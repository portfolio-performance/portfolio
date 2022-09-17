package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetGrossUnit;

import java.math.BigDecimal;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
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
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // 17'000 Inhaber-Aktien 
                // Nokia Corp
                // Valor: 472672
                // Total Kurswert EUR -74'120.00
                .section("name1", "name", "wkn", "currency")
                .match("^[\\.',\\d]+ (?<name1>.*)$")
                .match("^(?<name>.*)$")
                .match("^Valor: (?<wkn>.*)$")
                .match("^Total Kurswert (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> {
                    v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // 17'000 Inhaber-Aktien Nokia Corp
                .section("shares")
                .match("^(?<shares>[\\.',\\d]+) (?<name1>.*)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

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
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Total Kurswert EUR -74'120.00
                // Change EUR/CHF 1.241652 CHF -92'031.25
                .section("fxCurrency", "fxGross", "termCurrency", "baseCurrency", "exchangeRate", "currency", "gross").optional()
                .match("^Total Kurswert (?<fxCurrency>[\\w]{3}) (\\-)?(?<fxGross>[\\.',\\d]+)$")
                .match("^Change (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.',\\d]+) (?<currency>[\\w]{3}) (\\-)?(?<gross>[\\.',\\d]+)$")
                .assign((t, v) -> {
                    type.getCurrentContext().putType(asExchangeRate(v));

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                    checkAndSetGrossUnit(gross, fxGross, t, type);
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
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
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
                .section("name1", "name", "wkn", "isin", "currency")
                .match("^(?<name1>.*) Ex Datum: [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$")
                .match("^(?<name>.*)$")
                .match("^Valor: (?<wkn>.*)$")
                .match("^ISIN: (?<isin>.*)$")
                .match("^Brutto \\([\\.',\\d]+ \\* [\\w]{3} [\\.',\\d]+\\) (?<currency>[\\w]{3}) [\\.',\\d]+$")
                .assign((t, v) -> {
                    v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Brutto (1 * CHF 28.00) CHF 28.00
                .section("shares")
                .match("^Brutto \\((?<shares>[\\.',\\d]+) \\* .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Am 02.05.2016 wurde folgende Dividende gutgeschrieben:
                .section("date")
                .match("^Am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) wurde folgende Dividende gutgeschrieben:$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Netto CHF 18.20
                .section("amount", "currency")
                .match("^Netto (?<currency>[\\w]{3}) (?<amount>[\\.',\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Brutto (17'000 * EUR 0.26) EUR 4'420.00
                // Change EUR / CHF 1.067227 CHF 3'302.00
                .section("currency", "gross", "termCurrency", "baseCurrency", "fxCurrency", "exchangeRate").optional()
                .match("^Brutto \\([\\.',\\d]+ \\* [\\w]{3} [\\.',\\d]+\\) (?<currency>[\\w]{3}) (?<gross>[\\.',\\d]+)$")
                .match("^Change (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.',\\d]+) (?<fxCurrency>[\\w]{3}) (\\-)?[\\.',\\d]+$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(asExchangeRate(v));
                    
                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
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
