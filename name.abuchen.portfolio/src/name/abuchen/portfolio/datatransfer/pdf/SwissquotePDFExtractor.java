package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SwissquotePDFExtractor extends AbstractPDFExtractor
{
    public SwissquotePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Swissquote"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendsTransaction();
        addAccountFeesTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Swissquote Bank AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("B.rsentransaktion: (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^B.rsentransaktion: (Kauf|Verkauf) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^B.rsentransaktion: (?<type>Verkauf) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // APPLE ORD ISIN: US0378331005 NASDAQ New York
                // 15 193 USD 2'895.00
                .section("name", "isin", "currency")
                .match("^(?<name>.*) ISIN: (?<isin>[\\w]{12}) .*$")
                .match("^[\\.'\\d]+ [\\.'\\d]+ (?<currency>[\\w]{3}) [\\.'\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // 15 193 USD 2'895.00
                .section("shares")
                .match("^(?<shares>[\\.'\\d]+) [\\.'\\d]+ [\\w]{3} [\\.'\\d]+$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Betrag belastet auf Kontonummer  99999901, Valutadatum 07.08.2019
                // Betrag gutgeschrieben auf Ihrer Kontonummer  99999900, Valutadatum 07.02.2018
                .section("date")
                .match("^Betrag (belastet|gutgeschrieben) (auf|auf Ihrer) Kontonummer([\\s]+)? [\\d]+, ([\\s]+)?Valutadatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Zu Ihren Lasten USD 2'900.60
                // Zu Ihren Gunsten CHF 8'198.70
                .section("currency", "amount")
                .match("^Zu Ihren (Lasten|Gunsten) (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Total DKK 35'410.5
                // Wechselkurs 14.9827
                // CHF 5'305.45
                .section("fxCurrency", "fxGross", "exchangeRate", "currency", "gross").optional()
                .match("^Total (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.'\\d]+)$")
                .match("^Wechselkurs (?<exchangeRate>[\\.'\\d]+)$")
                .match("^(?<currency>[\\w]{3}) (?<gross>[\\.'\\d]+)$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    if (t.getPortfolioTransaction().getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                    {
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                    }
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                    // Swissquote sometimes uses scaled exchanges rates (such as DKK/CHF 15.42),
                    // instead of 0.1542, hence we try to extract and if we fail,
                    // we calculate the exchange rate
                    if (fxGross.getCurrencyCode().equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                    {
                        try
                        {
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, fxGross, exchangeRate));
                        }
                        catch (IllegalArgumentException e)
                        {
                            exchangeRate = BigDecimal.valueOf(((double) gross.getAmount()) / fxGross.getAmount());                            
                            type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, fxGross, exchangeRate));
                        }
                    }
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendsTransaction()
    {
        DocumentType type = new DocumentType("(Dividende|Kapitalgewinn)");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividende|Kapitalgewinn) Unsere Referenz:(.*)$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.DIVIDENDS);
                    return entry;
                });

        pdfTransaction
                // HARVEST CAPITAL CREDIT ORD ISIN: US41753F1093NKN: 350
                // Dividende 0.08 USD
                .section("name", "isin", "currency")
                .match("^(?<name>.*) ISIN: (?<isin>[\\w]{12}).*$")
                .match("^(Dividende|Kapitalgewinn) ([\\.'\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Anzahl 350
                .section("shares")
                .match("^Anzahl (?<shares>[\\.'\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Ausführungsdatum 19.06.2019
                .section("date")
                .match("^Ausf.hrungsdatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Total USD 19.60
                .section("currency", "amount")
                .match("^Total (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addAccountFeesTransaction()
    {
        DocumentType type = new DocumentType("Depotgeb.hren");
        this.addDocumentTyp(type);

        Block block = new Block("^Depotgeb.hren Unsere Referenz(.*)$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction transaction = new AccountTransaction();
                    transaction.setType(AccountTransaction.Type.FEES);
                    return transaction;
                })

                .section("date", "amount", "currency")
                .match("^Valutadatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .match("^Betrag belastet (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setNote("Depotgebühren");
                })

                .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Abgabe (Eidg. Stempelsteuer) USD 4.75
                .section("currency", "tax").optional()
                .match("^Abgabe \\(Eidg\\. Stempelsteuer\\) (?<currency>[\\w]{3}) (?<tax>[\\.'\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Quellensteuer 15.00% (US) USD 4.20
                .section("currency", "withHoldingTax").optional()
                .match("^Quellensteuer [\\.'\\d]+% \\(.*\\) (?<currency>[\\w]{3}) (?<withHoldingTax>[\\.'\\d]+)$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Zusätzlicher Steuerrückbehalt 15% USD 4.20
                .section("currency", "tax").optional()
                .match("^Zus.tzlicher Steuerr.ckbehalt [\\.'\\d]+% (?<currency>[\\w]{3}) (?<tax>[\\.'\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Verrechnungssteuer 35% (CH) CHF 63.88
                .section("currency", "tax").optional()
                .match("^Verrechnungssteuer [\\.'\\d]+% \\(.*\\) (?<currency>[\\w]{3}) (?<tax>[\\.'\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Kommission Swissquote Bank AG USD 0.85
                .section("currency", "fee").optional()
                .match("^Kommission Swissquote Bank AG (?<currency>[\\w]{3}) (?<fee>[\\.'\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Börsengebühren CHF 1.00
                .section("currency", "fee").optional()
                .match("^B.rsengeb.hren (?<currency>[\\w]{3}) (?<fee>[\\.'\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return PDFExtractorUtils.convertToNumberLong(value, Values.Amount, "de", "CH");
    }

    @Override
    protected long asShares(String value)
    {
        return PDFExtractorUtils.convertToNumberLong(value, Values.Share, "de", "CH");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return PDFExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH");
    }
}
