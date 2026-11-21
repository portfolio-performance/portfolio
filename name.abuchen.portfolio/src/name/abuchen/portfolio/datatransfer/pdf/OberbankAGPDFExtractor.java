package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.concatenate;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class OberbankAGPDFExtractor extends AbstractPDFExtractor
{
    public OberbankAGPDFExtractor(Client client)
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
        final var type = new DocumentType("(Wertpapier\\-.*" //
                        + "(Kauf" //
                        + "|Verkauf" //
                        + "|Ausgabe Fonds" //
                        + "|Ausgabe Fonds aus Dauerauftrag" //
                        + "|R.cknahme Fonds" //
                        + "))");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Wertpapier\\-.*" //
                        + "(Kauf" //
                        + "|Verkauf" //
                        + "|Ausgabe Fonds" //
                        + "|Ausgabe Fonds aus Dauerauftrag" //
                        + "|R.cknahme Fonds" //
                        + ")$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // Is type --> "Verkauf" or "Rücknahme Fonds" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Wertpapier\\-.*" //
                                        + "(?<type>(Verkauf" //
                                        + "|R.cknahme Fonds" //
                                        + "))$") //
                        .assign((t, v) -> t.setType(PortfolioTransaction.Type.SELL))

                        // @formatter:off
                        //     Storno 888888 - 13.04.2021
                        // @formatter:on
                        .section("type").optional() //
                        .match("^[\\s]*(?<type>Storno) [\\d]+ - [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                        .assign((t, v) -> v.markAsFailure(Messages.MsgErrorTransactionOrderCancellationUnsupported))

                        .oneOf( //
                                        // @formatter:off
                                        // CA09228F1036 BlackBerry Ltd. Zugang Stk .              14,00
                                        // Registered Shares o.N.
                                        // Kurs 19,098 EUR Kurswert EUR              267,37
                                        //
                                        // AT0000730007 ANDRITZ AG Abgang Stk.               95,00
                                        // AKTIEN O.N.
                                        // Kurs 44,9 EUR Kurswert EUR             4.265,50
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "nameContinued", "currency") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*) (Zugang|Abgang) Stk[\\s]*\\.[\\s]+[\\.,\\d]+$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^Kurs[\\s]+[\\.,\\d]+ [A-Z]+ Kurswert[\\s]+(?<currency>[A-Z]{3})[\\s]+[\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // AT000B127337 Oberbank AG Zugang EUR            8.000,00
                                        // Nachr. Anleihe 2023-2031
                                        // Kurs 98,3 PROZ Kurswert EUR            7.864,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "nameContinued", "currency") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*) (Zugang|Abgang) [A-Z]{3}[\\s]+[\\.,\\d]+$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^Kurs[\\s]+[\\.,\\d]+ [A-Z]+ Kurswert[\\s]+(?<currency>[A-Z]{3})[\\s]+[\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // CA09228F1036 BlackBerry Ltd. Zugang Stk .              14,00
                                        // AT0000730007 ANDRITZ AG Abgang Stk.               95,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] .* (Zugang|Abgang) Stk[\\s]*\\.[\\s]+(?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // AT000B127337 Oberbank AG Zugang EUR            8.000,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] .* (Zugang|Abgang) [A-Z]{3}[\\s]+(?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asBondNominal(v.get("shares")))))

                        // @formatter:off
                        // Handelszeitpunkt: 28.01.2021 09:16:17
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Handelszeitpunkt: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Wertpapierrechnung Wert 01.02.2021 EUR              274,62
                                        // SVK Sammelverwahrung Wert 11.03.2021 EUR              239,93
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^(Wertpapierrechnung|SVK Sammelverwahrung) Wert [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[A-Z]{3})[\\s]+(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Verwahrart, Positionsdaten EUR            8.068,87
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Verwahrart, Positionsdaten (?<currency>[A-Z]{3})[\\s]+(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // Auftrags-Nr. 999999-28.01.2021
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftrags-Nr\\. [\\d]+)-[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                        .assign((t, v) -> t.setNote(v.get("note")))

                        // @formatter:off
                        // 888888 - 28.01.2021
                        //     Storno 888888 - 13.04.2021
                        // @formatter:on
                        .section("note1").optional() //
                        .match("^([\\s]*Storno )?(?<note1>[\\d]+) - [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                        .assign((t, v) -> {
                            t.setNote(concatenate(t.getNote(), "Abrechnungs-Nr. " + v.get("note1"), " | "));
                        })

                        // @formatter:off
                        // Kupon 4,55 % jährlich Stückzinsen f. 166 Tage EUR              165,55
                        // @formatter:on
                        .section("note2", "note3", "note4").optional() //
                        .match("^Kupon [\\.,\\d]+ % .* St.ckzinsen (?<note2>f\\. [\\d]+ Tage).* (?<note4>[A-Z]{3})[\\s]+(?<note3>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setNote(concatenate(t.getNote(), "Stückzinsen " + v.get("note2"), " | "));
                            t.setNote(concatenate(t.getNote(), v.get("note3"), ": "));
                            t.setNote(concatenate(t.getNote(), v.get("note4"), " "));
                        })

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }
}
