package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class OberbankPDFExtractor extends AbstractPDFExtractor
{

    public OberbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Oberbank AG");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Oberbank AG";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("(Wertpapier-Abrechnu\\s*n\\s*g\\s*" //
                        + "(Kauf" //
                        + "|Verkauf))");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        // var firstRelevantLine = new
        // Block("^Wertpapier-Abrechnu\\s*n\\s*g.*$");
        var firstRelevantLine = new Block("^Wertpapier-Abrechnu\\s*n\\s*g\\s+(Kauf|Verkauf)$");

        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Wertpapier-Abrechnu\\s*n\\s*g\\s+" //
                                        + "(?<type>(Kauf" //
                                        + "|Verkauf))$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                        // @formatter:off
                                        // Wertpapiernummer Bezeichnung Nominale/Stück
                                        // CA09228F1036 BlackBerry Ltd. Zugang Stk .              14,00
                                        // Registered Shares o.N.
                                        // Kurs 19,098 EUR Kurswert EUR              267,37
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "nameContinued", "local", "shares") //
                                                        .find("^Wertpapiernummer Bezeichnung Nominale/St.ck$") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*) (Zugang|Abgang) (?<local>Stk)\\s*\\.\\s+(?<shares>[\\.,\\d]+)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .assign((t, v) -> {
                                                            // if
                                                            // (v.get("nameContinued").endsWith("p.STK"))
                                                            // v.put("nameContinued",
                                                            // v.get("nameContinued")
                                                            // .replace("p.STK",
                                                            // ""));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                            t.setShares(asShares(v.get("shares")));
                                                        }),
                                        // @formatter:off
                                        // Wertpapiernummer Bezeichnung Nominale/Stück
                                        // AT000B127337 Oberbank AG Zugang EUR            8.000,00
                                        // Nachr. Anleihe 2023-2031
                                        // Kurs 98,3 PROZ Kurswert EUR            7.864,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "nameContinued", "shares") //
                                                        .find("^Wertpapiernummer Bezeichnung Nominale/St.ck$") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*) (Zugang|Abgang) [A-Z]{3}\\s+(?<shares>[\\.,\\d]+)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                            // Percentage
                                                            // quotation,
                                                            // workaround for
                                                            // bonds
                                                            var shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share
                                                                            .factorize(shares.doubleValue() / 100));
                                                        }))

                        .oneOf( //
                        // @formatter:off
                                        // Handelszeitpunkt: 28.01.2021 09:16:17
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Handelszeitpunkt: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$") //
                                                        .assign((t, v) -> t
                                                                        .setDate(asDate(v.get("date"), v.get("time")))))

                        .oneOf( //
                        // @formatter:off
                                        // Verwahrart, Positionsdaten
                                        // Wertpapierrechnung Wert 01.02.2021 EUR              274,62
                                        // zu Lasten Konto AT11 1111 1111 1111 1111
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Wertpapierrechnung Wert [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[A-Z]{3})\\s+(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),

                                        // @formatter:off
                                        // Verwahrart, Positionsdaten EUR            8.068,87
                                        // SVK Sammelverwahrung Wert 09.08.2023
                                        // zu Lasten Konto AT11 1111 1111 1111 1111
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Verwahrart, Positionsdaten (?<currency>[A-Z]{3})\\s+(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // Auftrags-Nr. 999999-28.01.2021
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftrags-Nr\\. \\d+)-[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        // @formatter:off
                        // Kupon 4,55 % jährlich Stückzinsen f. 166 Tage EUR              165,55

                        // @formatter:on
                        .section("note1", "note2", "note3").optional() //
                        .match("^Kupon [\\.,\\d]+ % .* (?<note1>St.ckzinsen .* [\\d]+ Tage).* (?<note3>[A-Z]{3})\\s+(?<note2>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setNote(concatenate(t.getNote(), v.get("note1"), " | "));
                            t.setNote(concatenate(t.getNote(), v.get("note2"), ": "));
                            t.setNote(concatenate(t.getNote(), v.get("note3"), " "));
                        })

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //
        // @formatter:off
                        // Spesen EUR                7,25
                        // 24.02.2024 Spesen EUR               39,32
                        // Spesen EUR               -14,93
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^([\\d]{2}\\.[\\d]{2}\\.[\\d]{4} )?Spesen (?<currency>[A-Z]{3})\\s+\\-?(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));

    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

        // @formatter:off
                        // Kursgewinn-KESt EUR               -73,15
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kursgewinn-KESt (?<currency>[A-Z]{3})\\s+\\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));

    }

}
