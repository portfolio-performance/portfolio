package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class SBrokerPDFExtractor extends AbstractPDFExtractor
{

    public SBrokerPDFExtractor(Client client)
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

                        .section("isin", "name") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*)\\W+(?<isin>.+)")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("date", "amount", "currency") //
                        .find("Wert +Konto-Nr. Betrag zu Ihren Lasten *")
                        .match("(?<date>\\d+.\\d+.\\d{4}) \\d{2}/\\d{4}/\\d{3} (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("shares") //
                        .match("^STK (?<shares>\\d+,\\d+?) .*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("fee", "currency").optional() //
                        .match(".* Orderentgelt\\W+(?<currency>\\w{3}+) (?<fee>[\\d.]+,\\d+)-") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* Börsengebühr (?<currency>\\w{3}+) (?<fee>[\\d.]+,\\d+)-") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

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

                        .section("isin", "name") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*)\\W+(?<isin>.+)")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("date", "amount", "currency") //
                        .find("Wert Konto-Nr. Betrag zu Ihren Gunsten")
                        .match("(?<date>\\d+.\\d+.\\d{4}) \\d{2}/\\d{4}/\\d{3} (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("shares") //
                        .match("^STK (?<shares>\\d+,\\d+?) .*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("fee", "currency").optional() //
                        .match(".* Orderentgelt (?<currency>\\w{3}+) (?<fee>[\\d.]+,\\d+)-") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* Börsengebühr (?<currency>\\w{3}+) (?<fee>[\\d.]+,\\d+)-") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Erträgnisgutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("Erträgnisgutschrift.*EMAILVERSAND.*");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        .section("isin", "name") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*)\\W+(?<isin>.+)")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .match("^STK (?<shares>\\d+,\\d+?) .*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "amount", "currency") //
                        .find("Wert Konto-Nr. (Devisenkurs )?Betrag zu Ihren Gunsten")
                        .match("(?<date>\\d+.\\d+.\\d{4}).*(?<currency>\\w{3}) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> new TransactionItem(t)));
    }

}
