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
public class SuresseDirektBankPDFExtractor extends AbstractPDFExtractor
{
    public SuresseDirektBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Suresse"); //$NON-NLS-1$

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Suresse Direkt Bank"; //$NON-NLS-1$
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Auszug [\\d]+([\\s]+)\\/[\\d]+", (context, lines) -> {
            Pattern pYear = Pattern.compile("^.* [\\d]{2}\\-[\\d]{2}\\-(?<year>[\\d]{4}) : ([\\-])?[\\.,\\d]+ [\\w]{3}$");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pYear.matcher(line);
                if (m.matches())
                    context.put("year", m.group("year"));
            }
        });
        this.addDocumentTyp(type);

        // 1 19-12 19-12 Uberweisung zu Ihren Gunsten 1,00 EUR
        // 2 20-12 20-12 Ihre U¨berweisung -1,00 EUR
        Block depositBlock = new Block("^[\\d]+ [\\d]{2}\\-[\\d]{2} [\\d]{2}\\-[\\d]{2} .*.berweisung.*([\\-])?[\\.,\\d]+ [\\w]{3}$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction transaction = new AccountTransaction();
                    transaction.setType(AccountTransaction.Type.DEPOSIT);
                    return transaction;
                })

                .section("date", "sign", "amount", "currency", "note")
                .match("^[\\d]+ (?<date>[\\d]{2}\\-[\\d]{2}) [\\d]{2}\\-[\\d]{2} .*.berweisung.*(?<sign>([\\s|\\-]))(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .match("^(?<note>Referenz : .*)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    // Is sign --> "-" change from DEPOSIT to REMOVAL
                    if (v.get("sign").equals("-"))
                        t.setType(AccountTransaction.Type.REMOVAL);
                    
                    t.setDateTime(asDate(v.get("date") + "-" + context.get("year")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        Block interestBlock = new Block("^Z I N S A B S C H L U S S$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction transaction = new AccountTransaction();
                    transaction.setType(AccountTransaction.Type.INTEREST);
                    return transaction;
                })

                .section("note1", "note2", "amount", "currency")
                .match("^Periode von: (?<note1>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) .* (?<note2>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) .*$")
                .match("^Habenzinsen (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("note2")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setNote(v.get("note1").replace("-", ".") + " - " + v.get("note2").replace("-", "."));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }
}
