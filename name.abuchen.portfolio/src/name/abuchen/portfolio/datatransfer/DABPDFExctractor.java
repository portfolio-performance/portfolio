package name.abuchen.portfolio.datatransfer;

import java.io.IOException;

import name.abuchen.portfolio.datatransfer.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

public class DABPDFExctractor extends AbstractPDFExtractor
{
    public DABPDFExctractor(Client client) throws IOException
    {
        super(client);

        addBuyTransaction();
        addDividendTransaction();
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
                            entry.setCurrencyCode(CurrencyUnit.EUR);
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
                        .match("^Handelstag (?<date>\\d+.\\d+.\\d{4}+) .*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("fees")
                        .optional()
                        .match("^.* Provision (\\w{3}+) (?<fees>[\\d.]+,\\d+)-$")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(
                                        new Unit(Unit.Type.FEE, Money.of(t.getPortfolioTransaction().getCurrencyCode(),
                                                        asAmount(v.get("fees"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Dividende");
        this.addDocumentTyp(type);

        Block block = new Block("^Dividendengutschrift .*$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

        .subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            entry.setCurrencyCode(CurrencyUnit.EUR);
            return entry;
        })

        .section("isin", "name")
                        .find("Gattungsbezeichnung ISIN")
                        .match("^(?<name>.*) (?<isin>[^ ]*)$")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares")
                        //
                        .find("Nominal Ex-Tag Zahltag Dividenden-Betrag pro Stück")
                        .match("^STK (?<shares>\\d+(,\\d+)?) .*$")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "amount")
                        //
                        .find("Wert Konto-Nr. Betrag zu Ihren Gunsten")
                        .match("^(?<date>\\d+.\\d+.\\d{4}+) ([0-9]*) (\\w{3}+) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(t -> new TransactionItem(t)));
    }

    @Override
    public String getLabel()
    {
        return "DAB Bank AG"; //$NON-NLS-1$
    }

}
