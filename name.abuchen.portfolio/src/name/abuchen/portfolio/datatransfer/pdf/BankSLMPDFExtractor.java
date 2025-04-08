package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;

import java.math.BigDecimal;

import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote Bank SLM AG
 *
 * @implSpec The VALOR number is the WKN number with 5 to 9 letters.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class BankSLMPDFExtractor extends AbstractPDFExtractor
{
    public BankSLMPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Bank SLM AG");
        addBankIdentifier("Spar + Leihkasse");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Bank SLM AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("B.rsenabrechnung \\- (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^B.rsenabrechnung \\- (Kauf|Verkauf)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^B.rsenabrechnung \\- (?<type>(Kauf|Verkauf))$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Wir haben f�r Sie am 03.09.2013 gekauft.
                        // 17'000 Inhaber-Aktien
                        // Nokia Corp
                        // Valor: 472672
                        // Total Kurswert EUR -74'120.00
                        // @formatter:on
                        .section("name1", "name", "wkn", "currency") //
                        .find("Wir haben f.r.*") //)
                        .match("^[\\.'\\d]+ (?<name1>.*)$") //
                        .match("^(?<name>.*)$") //
                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9})$") //
                        .match("^Total Kurswert (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            v.put("name", v.get("name") + " " + v.get("name1"));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // 17'000 Inhaber-Aktien Nokia Corp
                        // @formatter:on
                        .section("shares") //
                        .find("Wir haben.*")
                        .match("^(?<shares>[\\.'\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Wir haben für Sie am 03.09.2013 gekauft.
                        // Wir haben für Sie am 22.08.2013 verkauft.
                        // @formatter:on
                        .section("date") //
                        .match("^Wir haben f.r Sie am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (gekauft|verkauft)\\.$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Netto CHF -43'412.10
                        // Netto CHF 142'359.40
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Netto (?<currency>[\\w]{3}) (\\-)?(?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Total Kurswert EUR -74'120.00
                        // Change EUR/CHF 1.241652 CHF -92'031.25
                        // @formatter:on
                        .section("fxGross", "termCurrency", "baseCurrency", "exchangeRate", "gross").optional() //
                        .match("^Total Kurswert [\\w]{3} (\\-)?(?<fxGross>[\\.'\\d]+)$") //
                        .match("^Change (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.'\\d]+) [\\w]{3} (\\-)?(?<gross>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                            Money fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Dividende");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Dividende$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Namen-Aktien nom CHF 100.00 Ex Datum: 02.05.2016
                        // Bank SLM AG
                        // Valor: 135186
                        // ISIN: CH0001351862
                        // Brutto (1 * CHF 28.00) CHF 28.00
                        // @formatter:on
                        .section("name1", "name", "wkn", "isin", "currency") //
                        .match("^(?<name1>.*) Ex Datum: [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                        .match("^(?<name>.*)$") //
                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9})$") //
                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^Brutto \\([\\.'\\d]+ \\* [\\w]{3} [\\.'\\d]+\\) (?<currency>[\\w]{3}) [\\.'\\d]+$") //
                        .assign((t, v) -> {
                            v.put("name", v.get("name") + " " + v.get("name1"));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Brutto (1 * CHF 28.00) CHF 28.00
                        // @formatter:on
                        .section("shares") //
                        .match("^Brutto \\((?<shares>[\\.'\\d]+) \\* .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Am 02.05.2016 wurde folgende Dividende gutgeschrieben:
                        // @formatter:on
                        .section("date") //
                        .match("^Am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) wurde folgende Dividende gutgeschrieben:$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Netto CHF 18.20
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Netto (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Brutto (17'000 * EUR 0.26) EUR 4'420.00
                        // Change EUR / CHF 1.067227 CHF 3'302.00
                        // @formatter:on
                        .section("gross", "termCurrency", "baseCurrency", "exchangeRate").optional() //
                        .match("^Brutto \\([\\.'\\d]+ \\* [\\w]{3} [\\.'\\d]+\\) >[\\w]{3} (?<gross>[\\.'\\d]+)$") //
                        .match("^Change (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.'\\d]+) [\\w]{3} (\\-)?[\\.'\\d]+$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Eidg. Umsatzabgabe CHF -138.05
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Eidg\\. Umsatzabgabe (?<currency>[\\w]{3}) \\-(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // 35% Verrechnungssteuer CHF -9.80
                        // @formatter:off
                        .section("currency", "tax").optional() //
                        .match("^[\\d]+% Verrechnungssteuer (?<currency>[\\w]{3}) \\-(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // 20% Quellensteuer EUR -884.00
                        // @formatter:on
                        .section("currency", "withHoldingTax").optional() //
                        .match("^[\\d]+% Quellensteuer (?<currency>[\\w]{3}) \\-(?<withHoldingTax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // 10% Nichtrückforderbare Steuern EUR -442.00
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^[\\d]+% Nicht r.ckforderbare Steuern (?<currency>[\\w]{3}) \\-(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Eigene Courtage CHF -489.15
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Eigene Courtage (?<currency>[\\w]{3}) \\-(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Börsengebühr Inland CHF -3.50
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^B.rsengeb.hr Inland (?<currency>[\\w]{3}) \\-(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Börsengebühr CHF -3.50
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^B.rsengeb.hr (?<currency>[\\w]{3}) \\-(?<fee>[\\.'\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "de", "CH");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "de", "CH");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH");
    }
}
