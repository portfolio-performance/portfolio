package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.LineSpan;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.SplittingStrategy;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class OberbankAGPDFExtractor extends AbstractPDFExtractor
{

    public OberbankAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Oberbank AG");

        addBuySellTransaction();
        addDividendTransaction();
        addDeliveryInOutBoundTransaction();
        addNonImportableTransaction();
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

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        // Is type --> "Rücknahme Fonds" change from BUY to SELL
                        .section("type") //
                        .match("^Wertpapier\\-.*" //
                                        + "(?<type>(Kauf" //
                                        + "|Verkauf" //
                                        + "|Ausgabe Fonds" //
                                        + "|Ausgabe Fonds aus Dauerauftrag" //
                                        + "|R.cknahme Fonds" //
                                        + "))$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")) || "Rücknahme Fonds".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

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
                                        // AT0000730007 ANDRITZ AG Abgang Stk.               95,00
                                        // AKTIEN O.N.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "nameContinued") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*) (Zugang|Abgang) Stk[\\s]*\\.[\\s]+[\\.,\\d]+$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // AT000B127337 Oberbank AG Zugang EUR            8.000,00
                                        // Nachr. Anleihe 2023-2031
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "nameContinued") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*) (Zugang|Abgang) [A-Z]{3}[\\s]+[\\.,\\d]+$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))
                        .oneOf( //
                        // @formatter:off
                                        // CA09228F1036 BlackBerry Ltd. Zugang Stk .              14,00
                                        // AT0000730007 ANDRITZ AG Abgang Stk.               95,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] .* (Zugang|Abgang) Stk[\\s]*\\.[\\s]+(?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setShares(asShares(v.get("shares")));
                                                        }),
                                        // @formatter:off
                                        // AT000B127337 Oberbank AG Zugang EUR            8.000,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] .* (Zugang|Abgang) [A-Z]{3}[\\s]+(?<shares>[\\.,\\d]+)$") //
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
                                        // SVK Sammelverwahrung Wert 09.08.2023
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
                        // 888888 - 02.03.2026
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
                        .match("^Kupon [\\.,\\d]+ % .* (?<note2>St.ckzinsen .* [\\d]+ Tage).* (?<note4>[A-Z]{3})[\\s]+(?<note3>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setNote(concatenate(t.getNote(), v.get("note2"), " | "));
                            t.setNote(concatenate(t.getNote(), v.get("note3"), ": "));
                            t.setNote(concatenate(t.getNote(), v.get("note4"), " "));
                        })

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendTransaction()
    {
        final var type = new DocumentType("(Wertpapier\\-.*" //
                        + "(Dividende" //
                        + "|Aussch.ttung" //
                        + "|Zinszahlung" //
                        + "|Aussch.ttungsgleicher Ertrag))");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Wertpapier\\-.*" //
                        + "(Dividende" //
                        + "|Aussch.ttung" //
                        + "|Zinszahlung" //
                        + "|Aussch.ttungsgleicher Ertrag)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // If we have a positive amount and a gross reinvestment,
                        // there is a tax refund.
                        // If the amount is negative, then it is taxes.
                        // @formatter:on
                        .section("type", "sign").optional() //
                        .match("^Wertpapier\\-.*" //
                                        + "(?<type>(Aussch.ttungsgleicher Ertrag" //
                                        + "))$") //
                        .match("^.*zu (?<sign>(Gunsten|Lasten)) Konto .*$") //
                        .assign((t, v) -> {
                            if ("Ausschüttungsgleicher Ertrag".equals(v.get("type")) && "Gunsten".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.TAX_REFUND);

                            if ("Ausschüttungsgleicher Ertrag".equals(v.get("type")) && "Lasten".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.TAXES);
                        })

                        .oneOf( //
                        // @formatter:off
                                        // US92826C8394 VISA Inc. Stk .              10,00
                                        // Reg. Shares Class A DL -,0001
                                        // Ertrag USD                6,70
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "nameContinued", "currency") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*) Stk[\\s]*\\.[\\s]+[\\.,\\d]+$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^Ertrag [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // AT000B127337 Oberbank AG EUR            8.000,00
                                        // Nachr. Anleihe 2023-2031
                                        // Ertrag f. 365 Tage EUR              364,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "nameContinued", "currency") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*) [A-Z]{3}[\\s]+[\\.,\\d]+$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^Ertrag f. [\\d]+ Tage (?<currency>[\\w]{3})[\\s]+[\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        .oneOf( //
                        // @formatter:off
                                        // US92826C8394 VISA Inc. Stk .              10,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] .*Stk[\\s]*\\.[\\s]+(?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setShares(asShares(v.get("shares")));
                                                        }),
                                        // @formatter:off
                                        // AT000B127337 Oberbank AG EUR            8.000,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] .* [A-Z]{3}[\\s]+(?<shares>[\\.,\\d]+)$") //
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
                        // Wertpapierrechnung Wert 02.03.2026 EUR                4,11
                        // SVK Sammelverwahrung Wert 16.06.2025 EUR              147,03
                        // @formatter:on
                        .section("date") //
                        .match("^(Wertpapierrechnung|SVK Sammelverwahrung) Wert (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [A-Z]{3}[\\s]+\\-?[\\.,\\d]+$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Extag 24.02.2024
                        // @formatter:on
                        .section("exDate").optional() //
                        .match("^Extag (?<exDate>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setExDate(asDate(v.get("exDate"))))

                        // @formatter:off
                        // Verwahrart, Positionsdaten
                        // Wertpapierrechnung Wert 02.03.2026 EUR                4,11
                        // Verwahrart, Positionsdaten
                        // SVK Sammelverwahrung Wert 16.06.2025 EUR              147,03
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^(Wertpapierrechnung|SVK Sammelverwahrung) Wert [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[A-Z]{3})[\\s]+\\-?(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // @formatter:off
                        // Ertrag 0,67 USD
                        // 15 % QUSt a 1,1797 v. 26.02.2026 EUR                4,11
                        // @formatter:on
                        .section("termCurrency", "fxGross", "exchangeRate", "baseCurrency").optional()
                        .match("^Ertrag (?<fxGross>[\\.,\\d]+) (?<termCurrency>[\\w]{3})$") //
                        .match("^.* a (?<exchangeRate>[\\.,\\d]+) v. [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<baseCurrency>[\\w]{3})[\\s]{1,}[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // 888888 - 02.03.2026
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>[\\d]+) - [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                        .assign((t, v) -> {
                            t.setNote(concatenate(t.getNote(), "Abrechnungs-Nr. " + v.get("note"), " | "));
                        })

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDeliveryInOutBoundTransaction()
    {
        final var type = new DocumentType("(Durchf.hrungsanzeig[\\s]*e[\\s]+" //
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
        var startsWith = Pattern.compile("^Durchf.hrungsanzeig[\\s]*e[\\s]+" //
                        + "(Freie Lieferung" //
                        + "|Freier Erhalt)$");
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
                        .match("^Durchf.hrungsanzeig[\\s]*e[\\s]+" //
                                        + "(?<type>(Freier Erhalt" //
                                        + "|Freie Lieferung))$") //
                        .assign((t, v) -> {
                            if ("Freie Lieferung".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                        })

                        // @formatter:off
                        // IE00BYZK4552 iShsIV-Automation&Robot.U.ETF Zugang Stk.              123,00
                        // Registered Shares o.N.
                        // IE00BYZK4552 iShsIV-Automation&Robot.U.ETF Abgang Stk.              123,00
                        // Registered Shares o.N.
                        // @formatter:on
                        .section("isin", "name", "nameContinued") //
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*) (Zugang|Abgang) Stk[\\s]*\\.[\\s]+[\\.,\\d]+$") //
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
                        .section("shares") //
                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] .* (Zugang|Abgang) Stk[\\s]*\\.[\\s]+(?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares")));
                        })

                        // @formatter:off
                        // Schlusstag 05.02.2021
                        // @formatter:on
                        .section("date") //
                        .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        //   steuerlicher Anschaffungswert:                                                 1.199,76 EUR
                        // @formatter:on
                        .section("currency", "amount")
                        .match("^[\\s]*Anschaffungskurswert:[\\s]+(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Auftrags-Nr. 999999-28.01.2021
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftrags-Nr\\. [\\d]+)-[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                        .assign((t, v) -> t.setNote(v.get("note")))

                        // @formatter:off
                        // 888888 - 02.03.2026
                        // @formatter:on
                        .section("note1").optional() //
                        .match("^(?<note1>[\\d]+) - [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                        .assign((t, v) -> {
                            t.setNote(concatenate(t.getNote(), "Abrechnungs-Nr. " + v.get("note1"), " | "));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addNonImportableTransaction()
    {
        final var type = new DocumentType("(Durchf.hrungsanzeig[\\s]*e[\\s]+" //
                        + "(Kapitalmaßnahme))", //
                        documentContext -> documentContext //
                                        .section("transaction") //
                                        .match("^(?<transaction>(Split))$") //
                                        .assign((ctx, v) -> ctx.put("transaction", v.get("transaction"))));
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<PortfolioTransaction>();

        var firstRelevantLine = new Block("^Durchf.hrungsanzeig[\\s]*e[\\s]+" //
                        + "(Einbuchung)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new PortfolioTransaction();
                            portfolioTransaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // US67066G1040 NVIDIA CORP. Zugang Stk.               12,00
                        // Registered Shares DL-,001
                        // 888888 - 20.07.2021
                        // @formatter:on    
                        .section("name", "nameContinued", "date", "isin", "shares") //
                        .documentContext("transaction") //
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*) Zugang Stk[\\s]*\\.[\\s]+(?<shares>[\\.,\\d]+)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .match("^[\\d]+ - (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> {
                            if ("Split".equals(v.get("transaction")))
                                v.markAsFailure(Messages.MsgErrorTransactionSplitUnsupported);
                            else
                                v.markAsFailure(Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            t.setDateTime(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setSecurity(getOrCreateSecurity(v));

                            t.setCurrencyCode(asCurrencyCode(t.getSecurity().getCurrencyCode()));
                            t.setAmount(0L);
                        })

                        // @formatter:off
                        // 888888 - 02.03.2026
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>[\\d]+) - [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                        .assign((t, v) -> {
                            t.setNote(concatenate(t.getNote(), "Abrechnungs-Nr. " + v.get("note"), " | "));
                        })

                        .wrap(TransactionItem::new);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //
        // @formatter:off
                        // Spesen EUR                7,25
                        // Spesen EUR               -14,93
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Spesen (?<currency>[A-Z]{3})[\\s]+\\-?(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // 24.02.2024 Spesen EUR               39,32
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Spesen (?<currency>[A-Z]{3})[\\s]+\\-?(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Kupon Spesen EUR                0,60
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Kupon Spesen (?<currency>[A-Z]{3})[\\s]+\\-?(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        // If we have a gross reinvestment,
        // we set a flag and don't book tax below.
        transaction //
                        .section("n").optional() //
                        .match("^Wertpapier\\-.*[\\s]+Aussch.ttungsgleicher Ertrag$") //
                        .match("^.*zu (?<n>(Gunsten|Lasten)) Konto .*$") //

                        .assign((t, v) -> type.getCurrentContext().putBoolean("noTax", true));

        transaction //
        // @formatter:off
                        // Kursgewinn-KESt EUR               -73,15
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kursgewinn-KESt (?<currency>[A-Z]{3})[\\s]+\\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // KESt EUR              -55,77
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^KESt (?<currency>[A-Z]{3})[\\s]+\\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Auslands-KESt USD               -0,26
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Auslands-KESt (?<currency>[\\w]{3})[\\s]+\\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Ausländische Steuern USD               -1,01
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Ausl.ndische Steuern (?<currency>[\\w]{3})[\\s]+\\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kupon jährlich KESt EUR             -100,10
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kupon jährlich KESt (?<currency>[\\w]{3})[\\s]+\\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processTaxEntries(t, v, type);
                        });
    }

}
