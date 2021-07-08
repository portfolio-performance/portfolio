package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class SwissquotePDFExtractor extends SwissBasedPDFExtractor
{
    public SwissquotePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Swissquote"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendsTransaction();
        addFeeTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return ""; //$NON-NLS-1$
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
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })
        
                // APPLE ORD ISIN: US0378331005 NASDAQ New York
                // 15 193 USD 2'895.00
                .section("name", "isin", "shares", "currency")
                .match("^(?<name>.*) ISIN: (?<isin>[\\w]{12}) .*$")
                .match("^(?<shares>['.,\\d]+) ['.,\\d]+ (?<currency>[\\w]{3}) (['.,\\d]+)$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Betrag belastet auf Kontonummer  99999901, Valutadatum 07.08.2019
                // Betrag gutgeschrieben auf Ihrer Kontonummer  99999900, Valutadatum 07.02.2018
                .section("date")
                .match("^Betrag (belastet|gutgeschrieben) (auf|auf Ihrer) Kontonummer([\\s]+)? [\\d]+,([\\s]+)? Valutadatum (?<date>\\d+.\\d+.\\d{4})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Zu Ihren Lasten USD 2'900.60
                // Zu Ihren Gunsten CHF 8'198.70
                .section("currency", "amount")
                .match("^(Zu Ihren Lasten|Zu Ihren Gunsten) (?<currency>[\\w]{3}) (?<amount>['.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Total DKK 37'301.50
                // Wechselkurs 15.0198
                .section("fxcurrency", "fxamount", "exchangeRate").optional()
                .match("^Total (?<fxcurrency>[\\w]{3}) ([-\\s]+)?(?<fxamount>['.,\\d]+)$")
                .match("^Wechselkurs (?<exchangeRate>['.,\\d]+)$")
                .assign((t, v) -> {
                    // read the forex currency, exchange rate and gross
                    // amount in forex currency
                    String forex = asCurrencyCode(v.get("fxcurrency"));
                    if (t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                    {
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // gross given in forex currency
                        long fxAmount = asAmount(v.get("fxamount"));
                        long amount = reverseRate.multiply(BigDecimal.valueOf(fxAmount))
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();

                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                        Money.of(t.getPortfolioTransaction().getCurrencyCode(), amount),
                                        Money.of(forex, fxAmount), reverseRate);

                        t.getPortfolioTransaction().addUnit(grossValue);
                    }
                })

                // Total DKK 35'410.5
                // Wechselkurs 14.9827
                // CHF 5'305.45
                .section("amount", "currency", "exchangeRate", "forexCurrency", "forexAmount").optional()
                .match("^Total (?<forexCurrency>\\w{3}+) (?<forexAmount>[\\d+',.]*)$")
                .match("^Wechselkurs (?<exchangeRate>[\\d+',.]*)$")
                .match("^(?<currency>\\w{3}+) (?<amount>[\\d+',.]*)$")
                .assign((t, v) -> {
                    Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")),
                                    asAmount(v.get("forexAmount")));
                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                    
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    if (forex.getCurrencyCode()
                                    .equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                    {
                        Unit unit;
                        // Swissquote sometimes uses scaled exchanges
                        // rates (such as DKK/CHF 15.42, instead of
                        // 0.1542,
                        // hence we try to extract and if we fail, we
                        // calculate the exchange rate
                        try
                        {
                            unit = new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate);
                        }
                        catch (IllegalArgumentException e)
                        {
                            exchangeRate = BigDecimal.valueOf(((double) gross.getAmount()) / forex.getAmount());
                            type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                            unit = new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate);
                        }
                        t.getPortfolioTransaction().addUnit(unit);
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
                // Anzahl 350
                // Dividende 0.08 USD
                .section("name", "isin", "shares", "currency")
                .match("^(?<name>.*) ISIN: (?<isin>[\\w]{12}).*$")
                .match("^Anzahl (?<shares>['.,\\d]+)$")
                .match("^(Dividende|Kapitalgewinn) (['.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Ausführungsdatum 19.06.2019
                .section("date")
                .match("^Ausf.hrungsdatum (?<date>\\d+.\\d+.\\d{4})")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Total USD 19.60
                .section("currency", "amount").optional()
                .match("^Total (?<currency>[\\w]{3}) (?<amount>[.,\\d]+)")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Total USD 13.52
                // Wechselkurs USD / CHF : 0.94242
                .section("amount", "currency", "forexCurrency", "exchangeRate").optional()
                .match("^Total (?<currency>[\\w]{3}) (?<amount>['.,\\d]+)")
                .match("^Wechselkurs [\\w]{3} \\/ (?<forexCurrency>[\\w]{3}) : (?<exchangeRate>['.,\\d]+)")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate")).setScale(10,
                                    RoundingMode.HALF_DOWN);
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());                    
                    
                    BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                    Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")),
                                    Math.round(t.getAmount() / inverseRate.doubleValue()));
                    Unit unit = new Unit(Unit.Type.GROSS_VALUE, t.getMonetaryAmount(), forex, inverseRate);

                    if (unit.getForex().getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                        t.addUnit(unit);
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addFeeTransaction()
    {
        DocumentType type = new DocumentType("Depotgebühren");
        this.addDocumentTyp(type);

        Block block = new Block("^Depotgebühren Unsere Referenz(.*)$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction transaction = new AccountTransaction();
                    transaction.setType(AccountTransaction.Type.FEES);
                    return transaction;
                })

                .section("date", "amount", "currency")
                .match("^Valutadatum (?<date>\\d+.\\d+.\\d{4}+)$")
                .match("^Betrag belastet (?<currency>\\w{3}+) (?<amount>[\\d+',.]*)$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Abgabe (Eidg. Stempelsteuer) USD 4.75
                .section("currency", "tax").optional()
                .match("^Abgabe \\(Eidg. Stempelsteuer\\) (?<currency>[\\w]{3}) (?<tax>['.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Quellensteuer 15.00% (US) USD 4.20
                .section("currency", "tax").optional()
                .match("^Quellensteuer ['.,\\d]+% \\(.*\\) (?<currency>[\\w]{3}) (?<tax>['.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Zusätzlicher Steuerrückbehalt 15% USD 4.20
                .section("currency", "tax").optional()
                .match("^Zus.tzlicher Steuerr.ckbehalt ['.,\\d]+% (?<currency>[\\w]{3}) (?<tax>['.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Verrechnungssteuer 35% (CH) CHF 63.88
                .section("currency", "tax").optional()
                .match("^Verrechnungssteuer ['.,\\d]+% \\(.*\\) (?<currency>[\\w]{3}) (?<tax>['.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Kommission Swissquote Bank AG USD 0.85
                .section("currency", "fee").optional()
                .match("^Kommission Swissquote Bank AG (?<currency>[\\w]{3}) (?<fee>['.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Börsengebühren CHF 1.00
                .section("currency", "fee").optional()
                .match("^B.rsengeb.hren (?<currency>[\\w]{3}) (?<fee>['.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    private void processTaxEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax, 
                            (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax,
                            ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }

    private void processFeeEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee, 
                            (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee,
                            ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }
}
