package name.abuchen.portfolio.datatransfer;

import java.io.IOException;

import name.abuchen.portfolio.datatransfer.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

public class FlatexPDFExctractor extends AbstractPDFExtractor
{

    public FlatexPDFExctractor(Client client) throws IOException
    {
        super(client);

        addBuyTransaction();
        addDividendTransaction();
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Sammelabrechnung (Wertpapierkauf/-verkauf)");
        this.addDocumentTyp(type);

        Block block = new Block("Nr.(\\d*)/(\\d*)  Kauf.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()
                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("wkn", "isin", "name")
                        .match("Nr.(\\d*)/(\\d*)  Kauf *(?<name>[^(]*) \\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares", "date")
                        .match("^davon ausgef.: (?<shares>\\d+,\\d*) St. *Schlusstag *:  (?<date>\\d+.\\d+.\\d{4}+), \\d+:\\d+ Uhr")
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares")));
                            t.setDate(asDate(v.get("date")));

                        })

                        .section("amount")
                        //
                        .match(".* Endbetrag *: *(?<amount>[\\d.-]+,\\d+) (\\w{3}+)")
                        .assign((t, v) -> t.setAmount(asAmount(v.get("amount"))))

                        .section("fee")
                        .optional()
                        //
                        .match(".* Provision *: *(?<fee>[\\d.-]+,\\d+) (\\w{3}+)")
                        .assign((t, v) -> t.setFees(asAmount(v.get("fee"))))

                        .section("fee")
                        .optional()
                        //
                        .match(".* Eigene Spesen *: *(?<fee>[\\d.-]+,\\d+) (\\w{3}+)")
                        .assign((t, v) -> t.setFees(t.getPortfolioTransaction().getFees() + asAmount(v.get("fee"))))

                        .section("fee").optional()
                        //
                        .match(".* \\*Fremde Spesen *: *(?<fee>[\\d.-]+,\\d+) (\\w{3}+)")
                        .assign((t, v) -> t.setFees(t.getPortfolioTransaction().getFees() + asAmount(v.get("fee"))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Dividendengutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("Dividendengutschrift.*");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()
                        //
                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DIVIDENDS);
                            return t;
                        })

                        .section("wkn", "isin", "name")
                        .match("Nr.(\\d*) * (?<name>[^(]*) *\\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares")
                        //
                        .match("^St. *: *(?<shares>[\\.\\d]+(,\\d*)?)")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount")
                        //
                        .match(".* Endbetrag *: *(?<amount>[\\d.-]+,\\d+) (\\w{3}+)")
                        .assign((t, v) -> t.setAmount(asAmount(v.get("amount"))))

                        .section("date")
                        //
                        .match("Valuta * : *(?<date>\\d+.\\d+.\\d{4}+) *")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .wrap(t -> new TransactionItem(t)));
    }

    @Override
    public String getLabel()
    {
        return "flatex"; //$NON-NLS-1$
    }
}
