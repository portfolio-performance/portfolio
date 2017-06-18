package name.abuchen.portfolio.datatransfer.pdf;

import java.io.IOException;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class CommerzbankPDFExctractor extends AbstractPDFExtractor
{
    public CommerzbankPDFExctractor(Client client) throws IOException
    {
        super(client);

        addBankIdentifier("C O M M E R Z B A N K"); //$NON-NLS-1$

        addBuyTransaction();
        addDividendTransaction();
    }

    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("W e r t p a p i e r k a u f");
        this.addDocumentTyp(type);

        Block block = new Block("W e r t p a p i e r k a u f");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("amount", "currency") //
                        .match(".*Zu I h r e n L a s t e n.*")
                        .match("^.* (\\d \\d . \\d \\d . \\d \\d \\d \\d) (?<currency>\\w{3}+)(?<amount>( \\d)*( \\.)?( \\d)* ,( \\d)*)$")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                        })

                        .section("date") //
                        .match("G e s c h ä f t s t a g : (?<date>\\d \\d . \\d \\d . \\d \\d \\d \\d) .*")
                        .assign((t, v) -> t.setDate(asDate(stripBlanks(v.get("date")))))

                        .section("shares") //
                        .match("S t . (?<shares>[\\d ,.]*) .*")
                        .assign((t, v) -> t.setShares(asShares(stripBlanks(v.get("shares")))))

                        .section("wkn", "name", "isin", "currency")
                        .match(".*W e r t p a p i e r - B e z e i c h n u n g.*").match("(?<name>.*) (?<wkn>\\S*)")
                        .match("^IBAN.*$") //
                        .match("^(?<isin>.*) (?<currency>\\w{3}+) (\\d \\d . \\d \\d . \\d \\d \\d \\d) (\\w{3}+)(( \\d)*( \\.)?( \\d)* ,( \\d)*)$")
                        .assign((t, v) -> {
                            v.put("isin", stripBlanks(v.get("isin")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .wrap(BuySellEntryItem::new));
    }

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

                        .section("date", "amount", "currency") //
                        .match(".*Zu I h r e n Gunsten.*")
                        .match("^.* (?<date>\\d \\d . \\d \\d . \\d \\d \\d \\d) (?<currency>\\w{3}+)(?<amount>( \\d)*( \\.)?( \\d)* ,( \\d)*)$")
                        .assign((t, v) -> {
                            t.setDate(asDate(stripBlanks(v.get("date"))));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                        })

                        .section("wkn", "name", "shares", "isin")
                        //
                        .match(".*W e r t p a p i e r - B e z e i c h n u n g.*")
                        .match("p e r \\d \\d . \\d \\d . \\d \\d \\d \\d (?<name>.*) (?<wkn>\\S*)")
                        .match("^STK (?<shares>(\\d )*(\\. )?(\\d )*, (\\d )*).* (?<isin>\\S*)$").assign((t, v) -> {
                            // if necessary, create the security with the
                            // currency of the transaction
                            v.put("currency", t.getCurrencyCode());
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(stripBlanks(v.get("shares"))));
                        })

                        .wrap(TransactionItem::new));
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
