package name.abuchen.portfolio.datatransfer;

import java.io.IOException;

import name.abuchen.portfolio.datatransfer.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

public class SBrokerPDFExtractor extends AbstractPDFExtractor
{

    public SBrokerPDFExtractor(Client client) throws IOException
    {
        super(client);

        addBuyTransaction();
        addSellTransaction();
        addDividendTransaction();
    }

    @Override
    public String getLabel()
    {
        return "S Broker AG & Co. KG"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Kauf");
        this.addDocumentTyp(type);

        Block block = new Block("Kauf .*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

        .subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        })

                        .section("isin", "name")
                        //
                        .find("Gattungsbezeichnung ISIN")
                        .match("(?<name>.*) Inhaber-Anteile (?<isin>.*)").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares")
                        //
                        .match("^STK (?<shares>\\d+,\\d+?) .*")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("fee").optional() //
                        .match(".* Orderentgelt .* (?<fee>[\\d.]+,\\d+)-") //
                        .assign((t, v) -> t.setFees(t.getPortfolioTransaction().getFees() + asAmount(v.get("fee"))))

                        .section("fee").optional() //
                        .match(".* Börsengebühr .* (?<fee>[\\d.]+,\\d+)-") //
                        .assign((t, v) -> t.setFees(t.getPortfolioTransaction().getFees() + asAmount(v.get("fee"))))
                        
                        .section("date", "amount") //
                        .find("Wert Konto-Nr. Betrag zu Ihren Lasten")
                        .match("(?<date>\\d+.\\d+.\\d{4}) \\d{2}/\\d{4}/\\d{3} .+ (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                        })
                        
                        .wrap(t -> new BuySellEntryItem(t)));
    }
    
    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Verkauf");
        this.addDocumentTyp(type);

        Block block = new Block("Verkauf .*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

        .subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        })

                        .section("isin", "name")
                        //
                        .find("Gattungsbezeichnung ISIN")
                        .match("(?<name>.*) Inhaber-Anteile (?<isin>.*)").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares")
                        //
                        .match("^STK (?<shares>\\d+,\\d+?) .*")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("fee").optional() //
                        .match(".* Orderentgelt .* (?<fee>[\\d.]+,\\d+)-") //
                        .assign((t, v) -> t.setFees(t.getPortfolioTransaction().getFees() + asAmount(v.get("fee"))))

                        .section("fee").optional() //
                        .match(".* Börsengebühr .* (?<fee>[\\d.]+,\\d+)-") //
                        .assign((t, v) -> t.setFees(t.getPortfolioTransaction().getFees() + asAmount(v.get("fee"))))
                    
                        .section("date", "amount") //
                        .find("Wert Konto-Nr. Betrag zu Ihren Gunsten")
                        .match("(?<date>\\d+.\\d+.\\d{4}) \\d{2}/\\d{4}/\\d{3} .+ (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                        })
                        
                        .wrap(t -> new BuySellEntryItem(t)));
    }    

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Erträgnisgutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("Erträgnisgutschrift.*");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

        .subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DIVIDENDS);
            return transaction;
        })

                        .section("isin", "name")
                        //
                        .find("Gattungsbezeichnung ISIN")
                        .match("(?<name>.*) Inhaber-Anteile (?<isin>.*)").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares")
                        //
                        .match("^STK (?<shares>\\d+,\\d+?) .*")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "amount") //
                        .find("Wert Konto-Nr. Betrag zu Ihren Gunsten")
                        .match("(?<date>\\d+.\\d+.\\d{4}) \\d{2}/\\d{4}/\\d{3} .+ (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(t -> new TransactionItem(t)));
    }

}
