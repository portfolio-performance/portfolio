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
public class VBankAGPDFExtractor extends AbstractPDFExtractor
{
    public VBankAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("V-Bank AG"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "V-Bank AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Kauf|Verkauf)([\\s]+\\(Zeichnung\\))?$", "^Diese Mitteilung wurde maschinell .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(?<type>Verkauf)$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Wertpapierbezeichnung Deut. Börse Commodities GmbH Xetra-Gold IHS 2007(09/Und)
                // ISIN DE000A0S9GB0
                // WKN A0S9GB
                // Nominal / Stück 300 ST
                // Kurs EUR 36,906
                .section("name", "isin", "wkn", "shares", "currency")
                .match("^Wertpapierbezeichnung (?<name>.*)$")
                .match("^ISIN (?<isin>[\\w]{12})$")
                .match("^WKN (?<wkn>.*)$")
                .match("^Nominal \\/ St.ck (?<shares>[.,\\d]+) ST$")
                .match("^Kurs (?<currency>[\\w]{3}) [.,\\d]+$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Handelstag / Zeit 08.05.2019 09:36:23
                .section("date", "time")
                .match("^Handelstag \\/ Zeit (?<date>\\d+.\\d+.\\d{4}) (?<time>\\d+:\\d+:\\d+)$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Ausmachender Betrag EUR - 11.116,97
                // Ausmachender Betrag EUR 26.199,03
                .section("currency", "amount")
                .match("^Ausmachender Betrag (?<currency>[\\w]{3}) ([-\\s]+)?(?<amount>[.,\\d]+)")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Total USD - 16.169,50
                // Devisenkurs EUR/USD 1,116670
                .section("fxcurrency", "fxamount", "exchangeRate").optional()
                .match("^Total (?<fxcurrency>[\\w]{3}) ([-\\s]+)?(?<fxamount>[.,\\d]+)$")
                .match("^Devisenkurs [\\w]{3}\\/[\\w]{3} (?<exchangeRate>[.,\\d]+)$")
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

                // Devisenkurs EUR/USD 1,116670
                .section("exchangeRate").optional()
                .match("^Devisenkurs [\\w]{3}\\/[\\w]{3} (?<exchangeRate>[.,\\d]+)$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Ertr.gnisabrechnung");
        this.addDocumentTyp(type);

        Block block = new Block("^Ertr.gnisabrechnung$", "^Der Abrechnungsbetrag wird mit Valuta .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.DIVIDENDS);
                            return entry;
                        });

        pdfTransaction
                // Wertpapierbezeichnung OptoFlex Inhaber-Ant. P o.N.
                // ISIN LU0834815366
                // WKN A1J4YZ
                // Nominal/Stück 16 ST
                // Währung EUR
                .section("name", "isin", "wkn", "shares", "currency")
                .match("^Wertpapierbezeichnung (?<name>.*)$")
                .match("^ISIN (?<isin>[\\w]{12})$")
                .match("^WKN (?<wkn>.*)$")
                .match("^Nominal\\/St.ck (?<shares>[.,\\d]+) ST$")
                .match("^W.hrung (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Ex-Tag 06.12.2019
                .section("date")
                .match("^Ex-Tag (?<date>\\d+.\\d+.\\d{4})")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Ausmachender Betrag EUR 48,54
                .section("currency", "amount")
                .match("^Ausmachender Betrag (?<currency>[\\w]{3}) (?<amount>[.,\\d]+)")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Kapitalertragsteuer EUR - 386,08
                .section("currency", "tax").optional()
                .match("^Kapitalertragsteuer (?<currency>[\\w]{3}) ([-\\s]+)?(?<tax>[.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag EUR - 21,23
                .section("currency", "tax").optional()
                .match("^Solidaritätszuschlag (?<currency>[\\w]{3}) ([-\\s]+)?(?<tax>[.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kirchensteuer EUR - 11,11
                .section("currency", "tax").optional()
                .match("^Kirchensteuer (?<currency>[\\w]{3}) ([-\\s]+)?(?<tax>[.,\\d]+)")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Bank-Provision EUR - 30,00
                .section("currency", "fee").optional()
                .match("^Bank-Provision ([*\\s]+)?(?<currency>[\\w]{3}) ([-\\s]+)?(?<fee>[.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Abwicklungsgebühren * EUR - 2,00
                .section("currency", "fee").optional()
                .match("^Abwicklungsgebühren ([*\\s]+)?(?<currency>[\\w]{3}) ([-\\s]+)?(?<fee>[.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Spesen * EUR - 1,00
                .section("currency", "fee").optional()
                .match("^Spesen ([*\\s]+)?(?<currency>[\\w]{3}) ([-\\s]+)?(?<fee>[.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Gebühren USD - 1,00
                .section("currency", "fee").optional()
                .match("^Geb.hren ([*\\s]+)?(?<currency>[\\w]{3}) ([-\\s]+)?(?<fee>[.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Courtage * EUR - 13,17
                .section("currency", "fee").optional()
                .match("^Courtage \\* (?<currency>[\\w]{3}) ([-\\s]+)?(?<fee>[.,\\d]+)$")
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
