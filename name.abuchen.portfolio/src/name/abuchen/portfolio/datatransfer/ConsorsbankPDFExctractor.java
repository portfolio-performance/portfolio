package name.abuchen.portfolio.datatransfer;

import java.io.IOException;

import name.abuchen.portfolio.datatransfer.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

public class ConsorsbankPDFExctractor extends AbstractExtractor
{
    public ConsorsbankPDFExctractor(Client client) throws IOException
    {
        super(client);

        addBankIdentifier("Consorsbank"); //$NON-NLS-1$
        addBankIdentifier("Cortal Consors"); //$NON-NLS-1$

        addBuyTransaction();
        addDividendTransaction();
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("KAUF");
        this.addDocumentTyp(type);

        Block block = new Block("^KAUF AM .*$");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

        .subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        })

        .section("wkn", "isin", "name") //
                        .find("Wertpapier WKN ISIN") //
                        .match("^(?<name>.*) (?<wkn>[^ ]*) (?<isin>[^ ]*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .find("Einheit Umsatz") //
                        .match("^ST (?<shares>\\d+(,\\d+)?)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "amount")
                        //
                        .match("Wert (?<date>\\d+.\\d+.\\d{4}+) (\\w{3}+) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setDate(asDate(v.get("date")));
                        })

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("DIVIDENDENGUTSCHRIFT");
        this.addDocumentTyp(type);

        Block block = new Block("DIVIDENDENGUTSCHRIFT.*");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

        .subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.DIVIDENDS);
            return t;
        })

        .section("wkn", "name", "shares")
                        //
                        .match("ST *(?<shares>\\d+(,\\d*)?) *WKN: *(?<wkn>\\S*) *")
                        //
                        .match("^(?<name>.*)$").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("date", "amount")
                        //
                        .match("WERT (?<date>\\d+.\\d+.\\d{4}+) *(\\w{3}+) *(?<amount>[\\d.]+,\\d+) *")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setDate(asDate(v.get("date")));
                        })

                        .wrap(t -> new TransactionItem(t)));
    }

    @Override
    public String getLabel()
    {
        return "Consorsbank"; //$NON-NLS-1$
    }

}
