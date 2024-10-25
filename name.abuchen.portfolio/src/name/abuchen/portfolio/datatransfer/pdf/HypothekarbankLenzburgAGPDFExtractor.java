package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.replaceMultipleBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
 * @implNote Hypothekarbank Lenzburg AG
 *
 * @implSpec The VALOR number is the WKN number with 5 to 9 letters.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class HypothekarbankLenzburgAGPDFExtractor extends AbstractPDFExtractor
{
    public HypothekarbankLenzburgAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Hypothekarbank Lenzburg AG");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Hypothekarbank Lenzburg AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wir haben am .* f.r Sie gekauft");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Transaktion .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Wir haben am 14.03.2024 an der BX Swiss für Sie gekauft
                                        //  720 Accum Shs USD Inve FTSE All Depotstelle  3500
                                        // Valor: 125615212 / IE000716YHJ7
                                        // Menge  720 Kurs CHF 5.484 CHF  3'948.48
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .find("Wir haben am .* f.r Sie gekauft") //)
                                                        .match("^([\\s]{1,})?[\\,'\\d]+ .* [A-Z]{3} (?<name>.*) Depotstelle.*$") //
                                                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .match("^Menge[\\s]{1,}[\\.'\\d]+ Kurs (?<currency>[\\w]{3})[\\s]{1,}[\\.'\\d]+[\\s]{1,}[\\w]{3}[\\s]{1,}[\\.'\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Wir haben am 03.09.2024 an der BX Swiss für Sie gekauft
                                        //  8 Ptg.Shs Van FTSE All Wr Depotstelle  3500
                                        // Valor: 18575459 / IE00B3RBWM25
                                        // Menge  8 Kurs CHF 115.471 CHF  923.77
                                        //
                                        // Wir haben am 03.09.2024 an der BX Swiss für Sie gekauft
                                        //  7 Shs SPDR S&P US Di Depotstelle  3500
                                        // Valor: 13976063 / IE00B6YX5D40
                                        // Menge  7 Kurs CHF 65.555 CHF  458.89
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .find("Wir haben am .* f.r Sie gekauft") //)
                                                        .match("^([\\s]{1,})?[\\,'\\d]+ ([A-Za-z]{3}\\.)?[A-Za-z]{3} (?<name>.*) Depotstelle.*$") //
                                                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .match("^Menge[\\s]{1,}[\\.'\\d]+ Kurs (?<currency>[\\w]{3})[\\s]{1,}[\\.'\\d]+[\\s]{1,}[\\w]{3}[\\s]{1,}[\\.'\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Wir haben am 09.10.2024 an der BX Swiss für Sie gekauft
                                        // 15 Registered Shs Microsoft Corp Depotstelle  3500
                                        // Valor: 951692 / US5949181045
                                        // Menge  15 Kurs CHF 355.865 CHF  5'337.98
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .find("Wir haben am .* f.r Sie gekauft") //)
                                                        .match("^([\\s]{1,})?[\\,'\\d]+ (?<name>.*) Depotstelle.*$") //
                                                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                                        .match("^Menge[\\s]{1,}[\\.'\\d]+ Kurs (?<currency>[\\w]{3})[\\s]{1,}[\\.'\\d]+[\\s]{1,}[\\w]{3}[\\s]{1,}[\\.'\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Menge  720 Kurs CHF 5.484 CHF  3'948.48
                        // @formatter:on
                        .section("shares") //
                        .match("^Menge[\\s]{1,}(?<shares>[\\.'\\d]+) Kurs.*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Wir haben am 14.03.2024 an der BX Swiss für Sie gekauft
                        // @formatter:on
                        .section("date") //
                        .match("^Wir haben am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Belastung 314.391.304 Valuta 18.03.2024 CHF  3'974.14
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Belastung .* (?<currency>[\\w]{3})[\\s]{1,}(?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Transaktion 61327806-0002
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Transaktion .*)$") //
                        .assign((t, v) -> t.setNote(trim(replaceMultipleBlanks(v.get("note")))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Ertragsaussch.ttung|Dividendenzahlung)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Ertragsaussch.ttung|Dividendenzahlung) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Aufgrund Ihres Bestandes schreiben wir Ihnen gut
                                        // 168 Ptg.Shs Van FTSE All Wr Depotstelle: 3500
                                        // USD Zahlbar Datum: 26.06.2024
                                        // Valor: 18575459 / IE00B3RBWM25 Ex Datum: 13.06.2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "wkn", "isin") //
                                                        .find("Aufgrund Ihres Bestandes.*") //
                                                        .match("^([\\s]{1,})?[\\,'\\d]+ ([A-Za-z]{3}\\.)?[A-Za-z]{3} (?<name>.*) Depotstelle.*$") //
                                                        .match("^(?<currency>[\\w]{3}) Zahlbar Datum.*$") //
                                                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Aufgrund Ihres Bestandes schreiben wir Ihnen gut
                                        //  25 Namen-Akt Allreal Holding AG Nom. CHF Depotstelle: 3500
                                        // 1.00 Zahlbar Datum: 25.04.2024
                                        // Valor: 883756 / CH0008837566 Ex Datum: 23.04.2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "wkn", "isin") //
                                                        .find("Aufgrund Ihres Bestandes.*") //
                                                        .match("^([\\s]{1,})?[\\,'\\d]+ (?<name>.*) (?<currency>[\\w]{3}) Depotstelle.*$") //
                                                        .match("^[\\.'\\d]+ Zahlbar Datum.*$") //
                                                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Aufgrund Ihres Bestandes schreiben wir Ihnen gut
                                        //  10 Namen-Akt Swisscom AG Nom. CHF 1.00 Depotstelle: 3500
                                        // Valor: 874251 / CH0008742519 Zahlbar Datum: 04.04.2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "wkn", "isin") //
                                                        .find("Aufgrund Ihres Bestandes.*") //
                                                        .match("^([\\s]{1,})?[\\,'\\d]+ (?<name>.*) (?<currency>[\\w]{3}) [\\.'\\d]+ Depotstelle.*$") //
                                                        .match("^Valor: (?<wkn>[A-Z0-9]{5,9}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // 168 Ptg.Shs Van FTSE All Wr Depotstelle: 3500
                        //  10 Namen-Akt Swisscom AG Nom. CHF 1.00 Depotstelle: 3500
                        // @formatter:on
                        .section("shares") //
                        .match("^([\\s]{1,})?(?<shares>[\\,'\\d]+) .* Depotstelle.*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))


                        // @formatter:off
                        // USD Zahlbar Datum: 26.06.2024
                        // @formatter:on
                        .section("date") //
                        .match("^.* Zahlbar Datum: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Gutschrift 351.413.308 Valuta 26.06.2024 CHF  116.96
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Gutschrift .* Valuta [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3})[\\s]{1,}(?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Brutto zu USD 0.788854 USD  132.53
                        // USD  132.53
                        // Devisenkurs 0.88255 CHF  116.96
                        // @formatter:on
                        .section("termCurrency", "exchangeRate", "fxGross", "baseCurrency", "gross").optional() //
                        .match("^Brutto zu (?<termCurrency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}[\\s]{1,}(?<fxGross>[\\.,\\d]+)$") //
                        .match("^Devisenkurs (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})[\\s]{1,}(?<gross>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                            ExtrExchangeRate rate = new ExtrExchangeRate(inverseRate, v.get("baseCurrency"), v.get("termCurrency"));
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            Money fxGross = Money.of(asCurrencyCode(v.get("termCurrency")), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Quartalsdividende Transaktion  65062408
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>.*) Transaktion.*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Quartalsdividende Transaktion  65062408
                        // Transaktion  62847906
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Transaktion.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(replaceMultipleBlanks(v.get("note"))), " | ")))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }


    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Verrechnungssteuer 35 % CHF -77.00
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Verrechnungssteuer .* (?<currency>[\\w]{3})[\\s]{1,}(\\-)?(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Eidg. Umsatzabgabe CHF  5.92
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Eidg\\. Umsatzabgabe (?<currency>[\\w]{3})[\\s]{1,}(\\-)?(?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Eigene Kommission (NEON) CHF  19.74
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Eigene Kommission.* (?<currency>[\\w]{3})[\\s]{1,}(?<fee>[\\.'\\d]+)$") //
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
