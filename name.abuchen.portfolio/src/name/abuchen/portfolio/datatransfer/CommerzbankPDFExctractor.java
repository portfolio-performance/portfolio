package name.abuchen.portfolio.datatransfer;

import java.io.IOException;

import name.abuchen.portfolio.datatransfer.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

public class CommerzbankPDFExctractor extends AbstractPDFExtractor
{
    public CommerzbankPDFExctractor(Client client) throws IOException
    {
        super(client);

        addBankIdentifier("C O M M E R Z B A N K"); //$NON-NLS-1$

        addDividendTransaction();
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("E r t r a g s g u t s c h r i f t");
        this.addDocumentTyp(type);

        Block block = new Block("E r t r a g s g u t s c h r i f t");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        .section("wkn", "name", "shares", "isin")
                        //
                        .match(".*W e r t p a p i e r - B e z e i c h n u n g.*")
                        .match("p e r \\d \\d . \\d \\d . \\d \\d \\d \\d (?<name>.*) (?<wkn>\\S*)")
                        .match("^STK (?<shares>(\\d )*, (\\d )*).* (?<isin>\\S*)$").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(stripBlanks(v.get("shares"))));
                        })

                        .section("date", "amount")
                        //
                        .match(".*Zu I h r e n Gunsten.*")
                        .match("^.* (?<date>\\d \\d . \\d \\d . \\d \\d \\d \\d) (\\w{3}+)(?<amount>( \\d)* ,( \\d)*)$")
                        .assign((t, v) -> {
                            t.setDate(asDate(stripBlanks(v.get("date"))));
                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                        })

                        .wrap(t -> new TransactionItem(t)));
    }

    private String stripBlanks(String input)
    {
        return input.replaceAll("\\s", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public String getLabel()
    {
        return "Commerzbank"; //$NON-NLS-1$
    }
}
