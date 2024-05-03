package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.Locale;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;

/**
 * @implNote Bondora Capital does not have a specific bank identifier.
 */
@SuppressWarnings("nls")
public class BondoraCapitalPDFExtractor extends AbstractPDFExtractor
{
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
        final DocumentType type = new DocumentType("(?m)^(Zusammenfassung|Summary)$");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^([\\d]{1,2}.[\\d]{1,2}.[\\d]{4}|[\\d]{4}.[\\d]{1,2}.[\\d]{1,2}) (?!Automatische .berweisung).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.setMaxSize(1);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("type").optional()
                        .match("^([\\d]{1,2}.[\\d]{1,2}.[\\d]{4}|[\\d]{4}.[\\d]{1,2}.[\\d]{1,2}) " //
                                        + "(?<type>(.berweisen" //
                                        + "|SEPA\\-Bank.berweisung" //
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
                                            || "Abheben auf Bankkonto".equals(v.get("type")))
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
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "amount") //
                                                        .match("^(?<date>([\\d]{1,2}\\.[\\d]{2}\\.[\\d]{4}|[\\d]{4}\\.[\\d]{2}\\.[\\d]{2})) " //
                                                                        + "(?<note>(.berweisen" //
                                                                        + "|SEPA\\-Bank.berweisung" //
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

                                                            String language = "de";
                                                            String country = "DE";

                                                            int apostrophe = v.get("amount").indexOf("\'");
                                                            if (apostrophe >= 0)
                                                            {
                                                                language = "de";
                                                                country = "CH";
                                                            }

                                                            t.setAmount(asAmount(v.get("amount"), language, country));
                                                            t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                                                            t.setNote(trim(v.get("note")));
                                                        }),
                                        // @formatter:off
                                        // 02/19/2023 Go & Grow Zinsen €1.62 €9,056.75
                                        // 03/02/2023 Go & Grow Zinsen €1.62 €9,074.6
                                        // 03/03/2023 Go & Grow Zinsen €1.62 €9,076.22
                                        // 03/04/2023 Go & Grow Zinsen €1.63 €9,077.85
                                        // 4/1/2023 Go & Grow returns €0.84 €4,723.86
                                        // 4/6/2023 Transfer €50 €4,777.24
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "note", "amount") //
                                                        .match("^(?<date>([\\d]{1,2}\\/[\\d]{1,2}\\/[\\d]{4}|[\\d]{4}\\/[\\d]{1,2}\\/[\\d]{1,2})) " //
                                                                        + "(?<note>(.berweisen" //
                                                                        + "|SEPA\\-Bank.berweisung" //
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
                                                            t.setDateTime(asDate(v.get("date"), Locale.UK));

                                                            String language = "de";
                                                            String country = "DE";

                                                            int lastDot = v.get("amount").lastIndexOf(".");
                                                            int lastComma = v.get("amount").lastIndexOf(",");

                                                            // returns the
                                                            // greater of two
                                                            // int values
                                                            if (Math.max(lastDot, lastComma) == lastDot)
                                                            {
                                                                language = "en";
                                                                country = "US";
                                                            }

                                                            t.setAmount(asAmount(v.get("amount"), language, country));
                                                            t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
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
                                                            t.setAmount(asAmount(v.get("amount"), "de", "DE"));
                                                            t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                                                            t.setNote(trim(v.get("note")));
                                                        }))

                        .wrap(TransactionItem::new);
    }
}
