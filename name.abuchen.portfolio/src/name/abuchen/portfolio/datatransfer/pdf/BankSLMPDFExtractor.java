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
public class BankSLMPDFExtractor extends AbstractPDFExtractor
{
    public BankSLMPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Bank SLM AG"); //$NON-NLS-1$
        addBankIdentifier("Spar + Leihkasse"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Bank SLM AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("B.rsenabrechnung \\- (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
                pdfTransaction.subject(() -> {
                    BuySellEntry entry = new BuySellEntry();
                    entry.setType(PortfolioTransaction.Type.BUY);
                    return entry;
        });

        Block firstRelevantLine = new Block("^B.rsenabrechnung \\- (Kauf|Verkauf)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                .section("type").optional()
                .match("^B.rsenabrechnung \\- (?<type>(Kauf|Verkauf))$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // 17'000 Inhaber-Aktien Nokia Corp
                // Valor: 472672
                // Total Kurswert EUR -74'120.00
                .section("shares", "name1", "name", "wkn", "currency")
                .match("^(?<shares>[\\.',\\d]+) (?<name1>.*)$")
                .match("^(?<name>.*)$")
                .match("^Valor: (?<wkn>.*)$")
                .match("^Total Kurswert (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> {
                    v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Wir haben für Sie am 03.09.2013 gekauft.
                // Wir haben für Sie am 22.08.2013 verkauft.
                .section("date")
                .match("^Wir haben f.r Sie am (?<date>[\\d]{2}+\\.[\\d]{2}+\\.[\\d]{4}) (gekauft|verkauft)\\.$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Netto CHF -43'412.10
                // Netto CHF 142'359.40
                .section("amount", "currency")
                .match("^Netto (?<currency>[\\w]{3}) (\\-)?(?<amount>[\\.',\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Total Kurswert EUR -74'120.00
                // Change EUR/CHF 1.241652 CHF -92'031.25
                .section("fxCurrency", "fxAmount", "exchangeRate").optional()
                .match("^Total Kurswert (?<fxCurrency>[\\w]{3}) (\\-)?(?<fxAmount>[\\.',\\d]+)$")
                .match("^Change [\\w]{3}/[\\w]{3} (?<exchangeRate>[\\.',\\d]+) [\\w]{3} (\\-)?[\\.',\\d]+$")
                .assign((t, v) -> {
                    // read the forex currency, exchange rate and gross
                    // amount in forex currency
                    String forex = asCurrencyCode(v.get("fxCurrency"));
                    if (t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                    {
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // gross given in forex currency
                        long fxAmount = asAmount(v.get("fxAmount"));
                        long amount = reverseRate.multiply(BigDecimal.valueOf(fxAmount))
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();

                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                        Money.of(t.getPortfolioTransaction().getCurrencyCode(), amount),
                                        Money.of(forex, fxAmount), reverseRate);

                        t.getPortfolioTransaction().addUnit(grossValue);
                    }
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Dividende");
        this.addDocumentTyp(type);

        Block block = new Block("^Dividende$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // Namen-Aktien nom CHF 100.00 Ex Datum: 02.05.2016
                // Bank SLM AG
                // Valor: 135186
                // ISIN: CH0001351862
                // Brutto (1 * CHF 28.00) CHF 28.00
                .section("name1", "name", "wkn", "isin", "shares", "currency")
                .match("^(?<name1>.*) Ex Datum: [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$")
                .match("^(?<name>.*)$")
                .match("^Valor: (?<wkn>.*)$")
                .match("^ISIN: (?<isin>.*)$")
                .match("^Brutto \\((?<shares>[\\.',\\d]+) \\* [\\w]{3} [\\.',\\d]+\\) (?<currency>[\\w]{3}) [\\.',\\d]+$")
                .assign((t, v) -> {
                    v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Am 02.05.2016 wurde folgende Dividende gutgeschrieben:
                .section("date")
                .match("^Am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) wurde folgende Dividende gutgeschrieben:$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Netto CHF 18.20
                .section("amount", "currency")
                .match("^Netto (?<currency>[\\w]{3}) (?<amount>[\\.',\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Brutto (17'000 * EUR 0.26) EUR 4'420.00
                // Netto EUR 3'094.00
                // Change EUR / CHF 1.067227 CHF 3'302.00
                .section("fxCurrency", "fxAmount", "currency", "amount", "exchangeRate").optional()
                .match("^Brutto \\([\\.',\\d]+ \\* [\\w]{3} [\\.',\\d]+\\) (?<fxCurrency>[\\w]{3}) (?<fxAmount>[\\.',\\d]+)$")
                .match("^Netto (?<currency>[\\w]{3}) (?<amount>[\\.',\\d]+)$")
                .match("^Change [\\w]{3}\\/[\\w]{3} (?<exchangeRate>[\\.',\\d]+) [\\w]{3} (\\-)?[\\.',\\d]+$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                    {
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                    }
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // check, if forex currency is transaction
                        // currency or not and swap amount, if necessary
                        Unit grossValue;
                        if (!asCurrencyCode(v.get("fxCurrency")).equals(t.getCurrencyCode()))
                        {
                            Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(v.get("fxAmount")));
                            Money amount = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        else
                        {
                            Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(v.get("fxAmount")));
                            Money fxAmount = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        t.addUnit(grossValue);
                    }
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Eidg. Umsatzabgabe CHF -138.05
                .section("currency", "tax").optional()
                .match("^Eidg\\. Umsatzabgabe (?<currency>[\\w]{3}) \\-(?<tax>[\\.',\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // 35% Verrechnungssteuer CHF -9.80
                .section("currency", "tax").optional()
                .match("^[\\d]+% Verrechnungssteuer (?<currency>[\\w]{3}) \\-(?<tax>[\\.',\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // 20% Quellensteuer EUR -884.00
                .section("currency", "withHoldingTax").optional()
                .match("^[\\d]+% Quellensteuer (?<currency>[\\w]{3}) \\-(?<withHoldingTax>[\\.',\\d]+)$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // 10% Nichtrückforderbare Steuern EUR -442.00
                .section("currency", "tax").optional()
                .match("^[\\d]+% Nicht r.ckforderbare Steuern (?<currency>[\\w]{3}) \\-(?<tax>[\\.',\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Eigene Courtage CHF -489.15
                .section("currency", "fee").optional()
                .match("^Eigene Courtage (?<currency>[\\w]{3}) \\-(?<fee>[\\.',\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Börsengebühr Inland CHF -3.50
                .section("currency", "fee").optional()
                .match("^B.rsengeb.hr Inland (?<currency>[\\w]{3}) \\-(?<fee>[\\.',\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Börsengebühr CHF -3.50
                .section("currency", "fee").optional()
                .match("^B.rsengeb.hr (?<currency>[\\w]{3}) \\-(?<fee>[\\.',\\d]+)$")
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
