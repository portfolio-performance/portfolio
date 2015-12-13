package name.abuchen.portfolio.datatransfer;

import java.io.IOException;

import name.abuchen.portfolio.datatransfer.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

public class INGDiBaExtractor extends AbstractPDFExtractor
{

    public INGDiBaExtractor(Client client) throws IOException
    {
        super(client);

        addBuyTransaction();
        addDividendTransaction();
    }

    @Override
    public String getLabel()
    {
        return "ING-DiBa"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierabrechnung Kauf");
        this.addDocumentTyp(type);

        Block block = new Block("Wertpapierabrechnung Kauf");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

        .subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        })

                        .section("wkn", "isin", "name")
                        //
                        .match("^ISIN \\(WKN\\) (?<isin>[^ ]*) \\((?<wkn>.*)\\)$")
                        .match("Wertpapierbezeichnung (?<name>.*)").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares")
                        //
                        .match("^Nominale St.ck (?<shares>\\d+(,\\d+)?)")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date") //
                        .match("Valuta (?<date>\\d+.\\d+.\\d{4}+)") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("amount") //
                        .match("Endbetrag zu Ihren Lasten (\\w{3}+) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> t.setAmount(asAmount(v.get("amount"))))

                        .section("fee").optional() //
                        .match("Handelsplatzgeb.hr (\\w{3}+) (?<fee>[\\d.]+,\\d+)") //
                        .assign((t, v) -> t.setFees(t.getPortfolioTransaction().getFees() + asAmount(v.get("fee"))))

                        .section("fee").optional() //
                        .match("Provision (\\w{3}+) (?<fee>[\\d.]+,\\d+)") //
                        .assign((t, v) -> t.setFees(t.getPortfolioTransaction().getFees() + asAmount(v.get("fee"))))

                        .section("fee").optional() //
                        .match("Handelsentgelt (\\w{3}+) (?<fee>[\\d.]+,\\d+)") //
                        .assign((t, v) -> t.setFees(t.getPortfolioTransaction().getFees() + asAmount(v.get("fee"))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Ertragsgutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("Ertragsgutschrift");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

        .subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DIVIDENDS);
            return transaction;
        })

                        .section("wkn", "isin", "name")
                        //
                        .match("^ISIN \\(WKN\\) (?<isin>[^ ]*) \\((?<wkn>.*)\\)$")
                        .match("Wertpapierbezeichnung (?<name>.*)").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares")
                        //
                        .match("^Nominale (?<shares>\\d+(,\\d+)?) .*")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date") //
                        .match("Valuta (?<date>\\d+.\\d+.\\d{4}+)") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("amount") //
                        .match("Gesamtbetrag zu Ihren Gunsten (\\w{3}+) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> t.setAmount(asAmount(v.get("amount"))))

                        .wrap(t -> new TransactionItem(t)));
    }

}
