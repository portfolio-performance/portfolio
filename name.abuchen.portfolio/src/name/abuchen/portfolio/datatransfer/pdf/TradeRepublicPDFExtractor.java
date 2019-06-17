package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class TradeRepublicPDFExtractor extends AbstractPDFExtractor
{
    public TradeRepublicPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("TRADE REPUBLIC"); //$NON-NLS-1$

        addBuyTransaction();
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Order Kauf");
        this.addDocumentTyp(type);

        Block block = new Block(".*Order Kauf.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("name", "isin", "shares") //
                        .find("POSITION ANZAHL KURS BETRAG") //
                        .match("(?<name>.*) (?<shares>[\\d+,.]*) Stk. ([\\d+,.]*) (\\w{3}+) ([\\d+,.]*) (\\w{3}+)$") //
                        .match(".*") //
                        .match("(?<isin>.*)").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("amount", "currency") //
                        .match("GESAMT -(?<amount>[\\d+,.]*) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("date", "time") //
                        .match(".*Order Kauf am (?<date>\\d+.\\d+.\\d{4}+), um (?<time>\\d+:\\d+) Uhr.*")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        .section("fee", "currency") //
                        .optional() //
                        .match("Fremdkostenzuschlag -(?<fee>[\\d+,.]*) (?<currency>\\w{3}+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .wrap(BuySellEntryItem::new));
    }

    @Override
    public String getLabel()
    {
        return "Trade Republic"; //$NON-NLS-1$
    }
}
