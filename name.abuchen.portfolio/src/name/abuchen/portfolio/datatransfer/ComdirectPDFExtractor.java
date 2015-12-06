package name.abuchen.portfolio.datatransfer;

import java.io.IOException;

import name.abuchen.portfolio.datatransfer.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

public class ComdirectPDFExtractor extends AbstractPDFExtractor
{
    public ComdirectPDFExtractor(Client client) throws IOException
    {
        super(client);

        addBankIdentifier("comdirect bank"); //$NON-NLS-1$

        addBuyTransaction();
        addDividendTransaction();
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierkauf");
        this.addDocumentTyp(type);

        Block block = new Block("Wertpapierkauf *");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

        .subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        })
        .section("date") //
        .match("Geschäftstag *: (?<date>\\d+.\\d+.\\d{4}+) .*") //
        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
        
        .section("isin", "name", "wkn") //
        .find("Wertpapier-Bezeichnung *WPKNR/ISIN *") //                                                
        .match("^(?<name>(\\S{1,} )*) *(?<wkn>\\S*) *$") //
        .match("(\\S{1,} )* *(?<isin>\\S*) *$") //
        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

        .section("shares") //
        .match("^St\\. *(?<shares>\\d+(,\\d+)?) .*") //
        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

        .section("amount") //
        .match(".* Kurswert *: (\\w{3}+) *(?<amount>[\\d.]+,\\d+).*") //
        .assign((t, v) -> t.setAmount(asAmount(v.get("amount"))))

        .wrap(t -> new BuySellEntryItem(t)));
        
        //FIXME not parsing fees, not present in the test file...
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Gutschrift fälliger Wertpapier-Erträge");
        this.addDocumentTyp(type);

        Block block = new Block("Ertragsgutschrift *");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

        .subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.DIVIDENDS);
            return t;
        })

        .section("wkn", "name", "isin", "date", "shares") //
        .match("^per (?<date>\\d+.\\d+.\\d{4}+) *(?<wkn>\\S*) *(?<name>(\\S{1,} )*) *$") //
        .match("^STK *(?<shares>[\\d.]+,\\d+) *(?<isin>\\S*) .*$") //
        .assign((t, v) -> {
            t.setSecurity(getOrCreateSecurity(v));
            t.setShares(asShares(v.get("shares")));
            t.setDate(asDate(v.get("date")));
        })

        .section("amount") //
        .find(".*Zu Ihren Gunsten vor Steuern *") //
        .match("^.*\\d+.\\d+.\\d{4}+ *EUR *(?<amount>[\\d.]+,\\d+) *$") //
        .assign((t, v) -> t.setAmount(asAmount(v.get("amount"))))

        .wrap(t -> new TransactionItem(t)));
    }

    @Override
    public String getLabel()
    {
        return "comdirect"; //$NON-NLS-1$
    }

}
