package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class SberbankEuropeAGPDFExtractor extends AbstractPDFExtractor
{
    public SberbankEuropeAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Sberbank Europe AG");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Sberbank Europe AG";
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Tagesgeld", (context, lines) -> {
            Pattern pYear = Pattern.compile("^.* Kontoauszug Nr\\. ([\\s]+)?[\\d]+\\/(?<year>[\\d]{4})$");
            Pattern pCurrency = Pattern.compile("^(?<currency>[\\w]{3})\\-Konto Kontonummer .*$");

            for (String line : lines)
            {
                Matcher mYear = pYear.matcher(line);
                if (mYear.matches())
                    context.put("year", mYear.group("year"));

                Matcher mCurrency = pCurrency.matcher(line);
                if (mCurrency.matches())
                    context.put("currency", mCurrency.group("currency"));
            }
        });
        this.addDocumentTyp(type);

        // @formatter:off
        // 01.07. 01.07. Überweisungsgutschr. 123.456,78 H
        // @formatter:on
        Block depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. " //
                        + "(.berweisungsgutschr\\.)" //
                        + " .* [S|H]$");
        type.addBlock(depositBlock);

        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DEPOSIT);
                            return transaction;
                        })

                        .section("day", "month", "note", "amount", "sign") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\. " + "(?<day>[\\d]{2})\\.(?<month>[\\d]{2})\\. " //
                                        + "(?<note>.berweisungsgutschr\\.) " + "(?<amount>[\\.,\\d]+) " //
                                        + "(?<sign>[S|H])$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();

                            // Is sign --> "S" change from DEPOSIT to REMOVAL
                            if ("S".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            // create a long date from the year in the context
                            t.setDateTime(asDate(v.get("day") + "." + v.get("month") + "." + context.get("year")));

                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));

                            // Formatting some notes
                            if ("Überweisungsgutschr.".equals(v.get("note")))
                                v.put("note", "Überweisungsgutschrift");

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }
}
