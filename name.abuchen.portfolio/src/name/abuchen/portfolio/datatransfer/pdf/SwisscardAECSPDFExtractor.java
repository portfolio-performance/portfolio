package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.LineSpan;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.SplittingStrategy;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SwisscardAECSPDFExtractor extends AbstractPDFExtractor
{
    public SwisscardAECSPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Swisscard AECS GmbH");

        addCreditCardStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Swisscard AECS GmbH";
    }

    private void addCreditCardStatementTransaction()
    {
        final var type = new DocumentType("Datum Transaktion Betrag in [A-Z]{3}", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Datum Transaktion Betrag in CHF
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Datum Transaktion Betrag in (?<currency>[A-Z]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // Ihre Zahlungen
        // 01.05.2026 FXMGpMTP StGaE 600.00
        // Neue Transaktionen
        // @formatter:on
        var paymentBlock = new Block(sectionLineSplittingStrategy( //
                        "^Ihre Zahlungen$", "^Neue Transaktionen$", //
                        "^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .* [\\.'\\d]+$"));
        type.addBlock(paymentBlock);
        paymentBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // Neue Transaktionen
        // 20.04.2026 pSZgXQmP oPrZBxEWM AG, irsSh 69.50
        // EUR 74.51, Kurs: 0.9329398, 21.04.2026 / Betrag in CHF beinhaltet 0% Bearbeitungszuschlag
        // Total neue Transaktionen 193.40
        // @formatter:on
        var chargeBlock = new Block(sectionLineSplittingStrategy( //
                        "^Neue Transaktionen$", "^Total neue Transaktionen .*$", //
                        "^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?!GEB.HR).* \\-?[\\.'\\d]+$"));
        type.addBlock(chargeBlock);
        chargeBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.REMOVAL))

                        .section("date", "note", "sign", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<note>.*) (?<sign>\\-?)(?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            // Is sign --> "-" change from REMOVAL to DEPOSIT
                            if ("-".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.DEPOSIT);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(trim(v.get("note")));
                        })

                        // @formatter:off
                        // EUR 74.51, Kurs: 0.9329398, 21.04.2026 / Betrag in CHF beinhaltet 0% Bearbeitungszuschlag
                        // EUR -178.45, Kurs: 0.9198399, 15.03.2026 / Betrag in CHF beinhaltet 0% Bearbeitungszuschlag
                        // @formatter:on
                        .section("fxCurrency", "fxAmount", "exchangeRate").optional() //
                        .match("^(?<fxCurrency>[A-Z]{3}) (?<fxAmount>\\-?[\\.'\\d]+), Kurs: (?<exchangeRate>[\\.'\\d]+),.*$") //
                        .assign((t, v) -> t.setNote(trim(t.getNote() + " - " + v.get("fxCurrency") + " "
                                        + v.get("fxAmount") + " (Kurs " + v.get("exchangeRate") + ")")))

                        .wrap(TransactionItem::new));

        // @formatter:off
        // Diverses
        // 09.01.2026 GEBÜHR FÜR PAPIERRECHNUNG 1.50
        // @formatter:on
        var feeBlock = new Block(sectionLineSplittingStrategy( //
                        "^Neue Transaktionen$", "^Total neue Transaktionen .*$", //
                        "^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} GEB.HR .* [\\.'\\d]+$"));
        type.addBlock(feeBlock);
        feeBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.FEES))

                        .section("date", "note", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<note>GEB.HR .*) (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));
    }

    /**
     * Creates blocks for lines matching {@code lineStart} but only within the
     * section delimited by {@code sectionStart} and {@code sectionEnd}. Used
     * to distinguish lines that are identical in shape (date, description,
     * amount) but belong to different sections of the document (e.g.
     * "Ihre Zahlungen" vs. "Neue Transaktionen").
     */
    private SplittingStrategy sectionLineSplittingStrategy(String sectionStart, String sectionEnd, String lineStart)
    {
        var startPattern = Pattern.compile(sectionStart);
        var endPattern = Pattern.compile(sectionEnd);
        var linePattern = Pattern.compile(lineStart);

        return lines -> {
            var sectionStartLine = -1;
            var sectionEndLine = -1;

            for (var ii = 0; ii < lines.length; ii++)
            {
                if (sectionStartLine == -1 && startPattern.matcher(lines[ii]).matches())
                {
                    sectionStartLine = ii;
                }
                else if (sectionStartLine != -1 && endPattern.matcher(lines[ii]).matches())
                {
                    sectionEndLine = ii;
                    break;
                }
            }

            if (sectionStartLine == -1)
                return List.of();

            if (sectionEndLine == -1)
                sectionEndLine = lines.length - 1;

            var lineStarts = new ArrayList<Integer>();
            for (var ii = sectionStartLine; ii <= sectionEndLine; ii++)
                if (linePattern.matcher(lines[ii]).matches())
                    lineStarts.add(ii);

            var spans = new ArrayList<LineSpan>();
            for (var ii = 0; ii < lineStarts.size(); ii++)
            {
                var start = lineStarts.get(ii);
                var end = ii + 1 < lineStarts.size() ? lineStarts.get(ii + 1) - 1 : sectionEndLine;
                spans.add(new LineSpan(start, end));
            }
            return spans;
        };
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "de", "CH");
    }
}
