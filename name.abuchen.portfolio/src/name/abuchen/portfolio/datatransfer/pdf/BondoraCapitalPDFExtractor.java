package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.ParsedData;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

/**
 * @implNote Bondora Capital does not have a specific bank identifier.
 */
@SuppressWarnings("nls")
public class BondoraCapitalPDFExtractor extends AbstractPDFExtractor
{
    private static final String NUMBER_LOCALE_LANGUAGE = "numberLocaleLanguage";
    private static final String NUMBER_LOCALE_COUNTRY = "numberLocaleCountry";

    public BondoraCapitalPDFExtractor(Client client)
    {
        super(client);

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Bondora Capital";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("(?m)^(Zusammenfassung|Summary)$", (context, lines) -> {
            Locale documentLocale = null;
            var isConflicting = false;

            var summaryNumberLine = Pattern.compile(
                            ".*\\b(Opening|Zu Beginn|Payments in|Einzahlungen|Payments out|Auszahlungen|Ergebnis)\\b.*");
            var moneyValue = Pattern.compile( //
                            "(?:\\p{Sc}\\s*)?(?<amount>-?[\\d][\\.,'\\d\\s]*)(?:\\s*\\p{Sc})?");
            var transactionLine = Pattern.compile(
                            "^([\\d]{1,2}[\\.\\-/][\\d]{1,2}[\\.\\-/][\\d]{4}|[\\d]{4}[\\.\\-/][\\d]{1,2}[\\.\\-/][\\d]{1,2}) .*$");

            for (var line : lines)
            {
                if (transactionLine.matcher(line).matches())
                    break;

                if (!summaryNumberLine.matcher(line).matches())
                    continue;

                var matcher = moneyValue.matcher(line);
                while (matcher.find())
                {
                    var locale = ExtractorUtils.detectNumberLocale(matcher.group("amount"));
                    if (locale.isEmpty())
                        continue;

                    if (documentLocale == null)
                    {
                        documentLocale = locale.get();
                    }
                    else if (!documentLocale.equals(locale.get()))
                    {
                        isConflicting = true;
                        break;
                    }
                }

                if (isConflicting)
                    break;
            }

            if (!isConflicting && documentLocale != null)
            {
                context.put(NUMBER_LOCALE_LANGUAGE, documentLocale.getLanguage());
                context.put(NUMBER_LOCALE_COUNTRY, documentLocale.getCountry());
            }
        });
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block(
                        "^([\\d]{1,2}.[\\d]{1,2}.[\\d]{4}|[\\d]{4}.[\\d]{1,2}.[\\d]{1,2}) (?!Automatische .berweisung|Nicht\\s+zugeordneter\\s+Saldo).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.setMaxSize(1);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        .section("type").optional() //
                        .match("^([\\d]{1,2}.[\\d]{1,2}.[\\d]{4}|[\\d]{4}.[\\d]{1,2}.[\\d]{1,2}) " //
                                        + "(?<type>(.berweisen" //
                                        + "|SEPA\\-Bank.berweisung" //
                                        + "|SEPA payment" //
                                        + "|Transfer" //
                                        + "|Abheben" //
                                        + "|Abheben auf Bankkonto" //
                                        + "|Go & Grow Zinsen" //
                                        + "|Go & Grow returns" //
                                        + "|Withdrawal)" //
                                        + ") .*$") //
                        .assign((t, v) -> {
                            if ("Überweisen".equals(v.get("type")) //
                                            || "Transfer".equals(v.get("type")) //
                                            || "SEPA-Banküberweisung".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.DEPOSIT);
                            else if ("Abheben".equals(v.get("type")) //
                                            || "Withdrawal".equals(v.get("type")) //
                                            || "Abheben auf Bankkonto".equals(v.get("type"))
                                            || "SEPA payment".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);
                        })

                        .oneOf( //
                        // @formatter:off
                                        // 06.02.2022 Go & Grow Zinsen 0,22 € 1.228,18 €
                                        // 07.02.2022 Überweisen 1.000 € 2.228,18 €
                                        // 27.11.2023 SEPA-Banküberweisung 50 € 1.064,4 €
                                        // 27.04.2024 Abheben auf Bankkonto 1.500 € 181.295,79 €
                                        //
                                        // 25.10.2020 Go & Grow Zinsen 1 € 5'630,99 €
                                        // 26.10.2020 Go & Grow Zinsen 1,01 € 5'632 €
                                        // 02.05.2026 Go & Grow Zinsen 1,02 € 6.369,75 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "amount") //
                                                        .documentContextOptionally(NUMBER_LOCALE_LANGUAGE,
                                                                        NUMBER_LOCALE_COUNTRY) //
                                                        .match("^(?<date>([\\d]{1,2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\.[\\d]{2}\\.[\\d]{2})) " //
                                                                        + "(?<note>(.berweisen" //
                                                                        + "|SEPA\\-Bank.berweisung" //
                                                                        + "|SEPA payment" //
                                                                        + "|Transfer" //
                                                                        + "|Abheben" //
                                                                        + "|Abheben auf Bankkonto" //
                                                                        + "|Go & Grow Zinsen" //
                                                                        + "|Go & Grow returns" //
                                                                        + "|Withdrawal)) " //
                                                                        + "(\\p{Sc})?(\\W)?" //
                                                                        + "(?<amount>[\\.,'\\d\\s]+)" //
                                                                        + "(\\W)?(\\p{Sc})(\\W)?(\\-)?[\\.,'\\d\\s]+(\\W)?(\\p{Sc})?$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));

                                                            var locale = ExtractorUtils
                                                                            .detectNumberLocale(v.get("amount")) //
                                                                            .or(() -> localeFromDocumentContext(v)) //
                                                                            .orElse(Locale.GERMANY);

                                                            t.setAmount(asAmount(v.get("amount"), locale));
                                                            t.setCurrencyCode(asCurrencyCode("EUR"));
                                                            t.setNote(trim(v.get("note")));
                                                        }),
                                        // @formatter:off
                                        // 02/19/2023 Go & Grow Zinsen €1.62 €9,056.75
                                        // 03/02/2023 Go & Grow Zinsen €1.62 €9,074.6
                                        // 03/03/2023 Go & Grow Zinsen €1.62 €9,076.22
                                        // 03/04/2023 Go & Grow Zinsen €1.63 €9,077.85
                                        // 4/1/2023 Go & Grow returns €0.84 €4,723.86
                                        // 4/6/2023 Transfer €50 €4,777.24
                                        // 2/4/2025 SEPA payment €1,000 €1,000
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "amount", "balance") //
                                                        .documentContextOptionally(NUMBER_LOCALE_LANGUAGE,
                                                                        NUMBER_LOCALE_COUNTRY) //
                                                        .match("^(?<date>([\\d]{1,2}\\/[\\d]{1,2}\\/[\\d]{4}|[\\d]{4}\\/[\\d]{1,2}\\/[\\d]{1,2})) " //
                                                                        + "(?<note>(.berweisen" //
                                                                        + "|SEPA\\-Bank.berweisung" //
                                                                        + "|SEPA payment" //
                                                                        + "|Transfer" //
                                                                        + "|Abheben" //
                                                                        + "|Abheben auf Bankkonto" //
                                                                        + "|Go & Grow Zinsen" //
                                                                        + "|Go & Grow returns" //
                                                                        + "|Withdrawal)) " //
                                                                        + "(\\p{Sc})?(\\W)?" //
                                                                        + "(?<amount>[\\.,\\d]+)" //
                                                                        + "(\\W)?(\\p{Sc})(\\W)?(?<balance>\\-?[\\.,\\d]+)(\\W)?(\\p{Sc})?$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date"), Locale.UK));

                                                            var locale = ExtractorUtils
                                                                            .detectNumberLocale(v.get("amount")) //
                                                                            .or(() -> ExtractorUtils.detectNumberLocale(
                                                                                            v.get("balance"))) //
                                                                            .or(() -> localeFromDocumentContext(v)) //
                                                                            .orElse(Locale.UK);

                                                            t.setAmount(asAmount(v.get("amount"), locale));
                                                            t.setCurrencyCode(asCurrencyCode("EUR"));
                                                            t.setNote(trim(v.get("note")));
                                                        }),
                                        // @formatter:off
                                        // 06-10-2020 Überweisen 4,91 € 104,91 €
                                        // 06-10-2020 Go & Grow Zinsen 0,02 € 154,93 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "amount") //
                                                        .match("^(?<date>([\\d]{1,2}\\-[\\d]{1,2}\\-[\\d]{4}|[\\d]{4}\\-[\\d]{1,2}\\-[\\d]{1,2})) " //
                                                                        + "(?<note>(.berweisen" //
                                                                        + "|SEPA\\-Bank.berweisung" //
                                                                        + "|SEPA payment" //
                                                                        + "|Transfer" //
                                                                        + "|Abheben" //
                                                                        + "|Abheben auf Bankkonto" //
                                                                        + "|Go & Grow Zinsen" //
                                                                        + "|Go & Grow returns" //
                                                                        + "|Withdrawal)) " //
                                                                        + "(\\p{Sc})?(\\W)?" //
                                                                        + "(?<amount>[\\.,\\d]+)" //
                                                                        + "(\\W)?(\\p{Sc})(\\W)?(\\-)?[\\.,\\d]+(\\W)?(\\p{Sc})?$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount"), Locale.GERMANY));
                                                            t.setCurrencyCode(asCurrencyCode("EUR"));
                                                            t.setNote(trim(v.get("note")));
                                                        }))

                        .wrap(TransactionItem::new);
    }

    private Optional<Locale> localeFromDocumentContext(ParsedData values)
    {
        var language = values.get(NUMBER_LOCALE_LANGUAGE);
        var country = values.get(NUMBER_LOCALE_COUNTRY);

        if (language == null || country == null)
            return Optional.empty();

        return Optional.of(Locale.of(language, country));
    }
}
