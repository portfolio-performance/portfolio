package name.abuchen.portfolio.datatransfer;

import java.io.IOException;

import name.abuchen.portfolio.datatransfer.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

public class DABPDFExctractor extends AbstractExtractor
{
    public DABPDFExctractor(Client client) throws IOException
    {
        super(client);

        addBuyTransaction();
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Kauf");
        this.addDocumentTyp(type);

        Block block = new Block("^Kauf .*$");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("isin", "name")
                        .find("Gattungsbezeichnung ISIN")
                        .match("^(?<name>.*) (?<isin>[^ ]*)$")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares")
                        //
                        .find("Nominal Kurs")
                        .match("^STK (?<shares>\\d+(,\\d+)?) (\\w{3}+) ([\\d.]+,\\d+)$")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount")
                        //
                        .find("Wert Konto-Nr. Betrag zu Ihren Lasten")
                        .match("^(\\d+.\\d+.\\d{4}+) ([0-9]*) (\\w{3}+) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> t.setAmount(asAmount(v.get("amount"))))

                        .section("date")
                        .match("^Handelstag (?<date>\\d+.\\d+.\\d{4}+) Kurswert (\\w{3}+) ([\\d.]+,\\d+)-$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @Override
    public String getLabel()
    {
        return "DAB Bank AG"; //$NON-NLS-1$
    }

}
