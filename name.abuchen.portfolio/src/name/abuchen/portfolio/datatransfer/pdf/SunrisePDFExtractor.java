package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

/***
 * @formatter:off
 * @implNote The currency of Sunrise Securities GmbH is always EUR.
 *
 * @implSpec There is no information on the shares in the dividend transactions.
 *           In this section we calculate the shares for the respective transaction.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class SunrisePDFExtractor extends AbstractPDFExtractor
{
    private static final String EUR = "EUR";

    public SunrisePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("info@meetsunrise.com");
        addBankIdentifier("Sunrise Securities GmbH");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Sunrise Securities GmbH";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Fondsabrechnung");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Kauf .* [\\.'\\d]+ \\p{Sc} [\\.'\\d]+ \\p{Sc} [\\.'\\d]+$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // @formatter:off
                        // Kauf Standortfonds Österreich 10.00 € 140.59 € 0.071
                        // AT0000A1QA38 10.01.2022 5.123
                        // Kauf Standortfonds Deutschland 100.24 € 192.77 € 0.520
                        // AT0000A1Z882 02.07.2026
                        // @formatter:on
                        .section("name", "currency", "isin") //
                        .match("^Kauf (?<name>.*) [\\.'\\d]+ \\p{Sc} [\\.'\\d]+ (?<currency>\\p{Sc}) [\\.'\\d]+$") //
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}(?: [\\.'\\d]+)?$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Kauf Standortfonds Österreich 10.00 € 140.59 € 0.071
                        // @formatter:on
                        .section("shares") //
                        .match("^Kauf .* [\\.'\\d]+ \\p{Sc} [\\.'\\d]+ \\p{Sc} (?<shares>[\\.\\d\\s]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // AT0000A1QA38 10.01.2022 5.123
                        // AT0000A1Z882 02.07.2026
                        // @formatter:on
                        .section("date") //
                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})(?: [\\.'\\d]+)?$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Abrechnungsbetrag: 10.00 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Abrechnungsbetrag: (?<amount>[\\.'\\d]+) (?<currency>\\p{Sc})$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // AT0000A1Z882 02.07.2026
                                        //   100.24 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                                                        .match("^[\\s]+(?<amount>[\\.'\\d]+) (?<currency>\\p{Sc})$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Auftrags-Nummer: 20220106123456789000000612345
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^(?<note>Auftrags\\-Nummer: [\\d]+)$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))),
                                        // @formatter:off
                                        //  20260701123456789000001944747
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^[\\s]+(?<note>[\\d]{20,})$") //
                                                        .assign((t, v) -> t.setNote("Auftrags-Nummer: " + trim(v.get("note")))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final var type = new DocumentType("Aussch.ttungsanzeige");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^.*Aussch.ttungsanzeige$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        // @formatter:off
                        // Fondsname: Standortfonds Österreich
                        // WKN/ISIN: AT0000A1QA38 Datum des Ertrags: 15.12.2023
                        // @formatter:on
                        .section("name", "isin") //
                        .match("^Fondsname: (?<name>.*)$") //
                        .match("^WKN\\/ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Datum des.*$") //
                        .assign((t, v) -> {
                            v.put("currency", asCurrencyCode(EUR));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Ausschüttung je Anteil: 2.71
                        // Ausschüttung gesamt: 17.58
                        // @formatter:on
                        .section("amountPerShare", "gross") //
                        .match("^Aussch.ttung je Anteil: (?<amountPerShare>['\\.\\d]+)$") //
                        .match("^Aussch.ttung gesamt: (?<gross>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            var amountPerShare = BigDecimal.valueOf(asAmount(v.get("amountPerShare")));
                            var gross = BigDecimal.valueOf(asAmount(v.get("gross")));

                            var shares = gross.divide(amountPerShare, Values.Share.precision(), RoundingMode.HALF_UP);
                            t.setShares(shares.movePointRight(Values.Share.precision()).longValue());
                        })

                        // The date of the income is not imported as ex-date because
                        // it does not always match the ex-date of the fund.

                        // @formatter:off
                        // WKN/ISIN: AT0000A1QA38 Datum des Ertrags: 15.12.2023
                        // @formatter:on
                        .section("date") //
                        .match("^WKN.*Datum des Ertrags: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // If the tax equals the dividend, the amount paid out is
                        // zero. The dividend is imported with gross value and tax.

                        // @formatter:off
                        //  Zur Auszahlung kommender Betrag: 0.00
                        // @formatter:on
                        .section("amount") //
                        .match("^.*Zur Auszahlung kommender Betrag: (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(EUR));
                        })

                        // @formatter:off
                        // WKN / ISIN: AT0000A1Z882 Turnus: jährlich
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* (?<note>Turnus: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // abgeführte Kapitalertragsteuer: 31.61 €
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^abgef.hrte Kapitalertragsteuer: (?<tax>[\\.'\\d]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kapitalertragsteuer (KESt) gesamt: 4.28
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^Kapitalertragsteuer \\(KESt\\) gesamt: (?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("currency", asCurrencyCode(EUR));
                            processTaxEntries(t, v, type);
                        });
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
