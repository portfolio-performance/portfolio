package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.LineSpan;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.SplittingStrategy;
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
        addDeliveryInOutBoundTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Oberbank AG";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("(Wertpapier-Abrechnu\\s*n\\s*g\\s+" //
                        + "(Kauf" //
                        + "|Verkauf))");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

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
                        .section("type") //
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
                                        // Wertpapiernummer Bezeichnung Nominale/Stück
                                        // AT0000730007 ANDRITZ AG Abgang Stk.               95,00
                                        // AKTIEN O.N.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "nameContinued") //
                                                        .find("^Wertpapiernummer Bezeichnung Nominale/St.ck$") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*) (Zugang|Abgang) Stk\\s*\\.\\s+[\\.,\\d]+$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Wertpapiernummer Bezeichnung Nominale/Stück
                                        // AT000B127337 Oberbank AG Zugang EUR            8.000,00
                                        // Nachr. Anleihe 2023-2031
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "nameContinued") //
                                                        .find("^Wertpapiernummer Bezeichnung Nominale/St.ck$") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*) (Zugang|Abgang) [A-Z]{3}\\s+[\\.,\\d]+$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))
                        .oneOf( //
                        // @formatter:off
                                        // Wertpapiernummer Bezeichnung Nominale/Stück
                                        // CA09228F1036 BlackBerry Ltd. Zugang Stk .              14,00
                                        // Wertpapiernummer Bezeichnung Nominale/Stück
                                        // AT0000730007 ANDRITZ AG Abgang Stk.               95,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("local", "shares") //
                                                        .find("^Wertpapiernummer Bezeichnung Nominale/St.ck$") //
                                                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] .* (Zugang|Abgang) (?<local>Stk)\\s*\\.\\s+(?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setShares(asShares(v.get("shares")));
                                                        }),
                                        // @formatter:off
                                        // Wertpapiernummer Bezeichnung Nominale/Stück
                                        // AT000B127337 Oberbank AG Zugang EUR            8.000,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .find("^Wertpapiernummer Bezeichnung Nominale/St.ck$") //
                                                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] .* (Zugang|Abgang) [A-Z]{3}\\s+(?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            // Percentage
                                                            // quotation,
                                                            // workaround for
                                                            // bonds
                                                            var shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share
                                                                            .factorize(shares.doubleValue() / 100));
                                                        }))

                        // @formatter:off
                        // Handelszeitpunkt: 28.01.2021 09:16:17
                        // @formatter:on
                        .section("date", "time")
                        .match("^Handelszeitpunkt: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

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

    private void addDeliveryInOutBoundTransaction()
    {
        final var type = new DocumentType("(Durchf.hrungsanzeig\\s*e\\s+" //
                        + "(Freier Erhalt" //
                        + "|Freie Lieferung))");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<PortfolioTransaction>();

        // Delivery inbound and outbound documents have multiple pages. The
        // first two start with the same line,
        // e.g.: Durchführungsanzeig e Freier Erhalt
        //
        // Repeated occurrences must be ignored to prevent the creation of
        // duplicate blocks.
        var startsWith = Pattern.compile("^Durchf.hrungsanzeig\\s*e\\s+(Freie Lieferung|Freier Erhalt)$");
        var splittingStrategy = (SplittingStrategy) lines -> {
            var blockIdentifiers = new HashSet<String>();

            // first: find the start of the blocks
            var blockStarts = new ArrayList<Integer>();

            for (var ii = 0; ii < lines.length; ii++)
            {
                var matcher = startsWith.matcher(lines[ii]);
                if (matcher.matches() && blockIdentifiers.add(lines[ii]))
                    blockStarts.add(ii);
            }

            // second: convert to line spans
            var spans = new ArrayList<LineSpan>();
            for (var ii = 0; ii < blockStarts.size(); ii++)
            {
                int startLine = blockStarts.get(ii);
                var endLine = ii + 1 < blockStarts.size() ? blockStarts.get(ii + 1) - 1 : lines.length - 1;
                spans.add(new LineSpan(startLine, endLine));
            }
            return spans;
        };

        var firstRelevantLine = new Block(splittingStrategy);
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Is type --> "Einbuchung" change from DELIVERY_INBOUND to DELIVERY_OUTBOUND
                        // @formatter:on
                        .section("type").optional() //
                        .match("^Durchf.hrungsanzeig\\s*e\\s+" //
                                        + "(?<type>(Freier Erhalt" //
                                        + "|Freie Lieferung))$") //
                        .assign((t, v) -> {
                            if ("Freie Lieferung".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                        })

                        // @formatter:off
                        // Wertpapiernummer Bezeichnung Nominale/Stück
                        // IE00BYZK4552 iShsIV-Automation&Robot.U.ETF Zugang Stk.              123,00
                        // Registered Shares o.N.
                        // Wertpapiernummer Bezeichnung Nominale/Stück
                        // IE00BYZK4552 iShsIV-Automation&Robot.U.ETF Abgang Stk.              123,00
                        // Registered Shares o.N.
                        // @formatter:on
                        .section("isin", "name", "nameContinued") //
                        .find("^Wertpapiernummer Bezeichnung Nominale/St.ck$") //
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*) (Zugang|Abgang) Stk\\s*\\.\\s+[\\.,\\d]+$") //
                        .match("^(?<nameContinued>.*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Wertpapiernummer Bezeichnung Nominale/Stück
                        // IE00BYZK4552 iShsIV-Automation&Robot.U.ETF Zugang Stk.              123,00
                        // Wertpapiernummer Bezeichnung Nominale/Stück
                        // IE00BYZK4552 iShsIV-Automation&Robot.U.ETF Abgang Stk.              123,00
                        // @formatter:on
                        .section("local", "shares") //
                        .find("^Wertpapiernummer Bezeichnung Nominale/St.ck$") //
                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] .* (Zugang|Abgang) (?<local>Stk)\\s*\\.\\s+(?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares")));
                        })

                        // @formatter:off
                        // Schlusstag 05.02.2021
                        // @formatter:on
                        .section("date").match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        //   steuerlicher Anschaffungswert:                                                 1.199,76 EUR
                        // @formatter:on
                        .section("currency", "amount")
                        .match("^\\s*Anschaffungskurswert:\\s+(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Auftrags-Nr. 999999-28.01.2021
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftrags-Nr\\. \\d+)-[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);
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
