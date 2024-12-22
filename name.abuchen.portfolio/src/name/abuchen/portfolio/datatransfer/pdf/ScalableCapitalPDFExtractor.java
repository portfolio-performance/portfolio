package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class ScalableCapitalPDFExtractor extends AbstractPDFExtractor
{
    public ScalableCapitalPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Scalable Capital GmbH");

        addPurchaseTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Scalable Capital";
    }

    private void addPurchaseTransaction()
    {
        final DocumentType type = new DocumentType("Wertpapierabrechnung");

        this.addDocumentTyp(type);

        Block purchase = new Block(".* Kundenauftrag.*");
        type.addBlock(purchase);
        purchase.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .section("name", "currency", "isin") //
                        .find("Typ Wertpapier Anzahl Kurs Betrag") //
                        .match("^Kauf (?<name>.*) " //
                                        + "[.,\\d]+ Stk. " //
                                        + "[.,\\d]+ [A-Z]{3} " //
                                        + "[.,\\d]+ (?<currency>[A-Z]{3})")
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares", "amount", "currency") //
                        .find("Typ Wertpapier Anzahl Kurs Betrag") //
                        .match("^Kauf .* " //
                                        + "(?<shares>[.,\\d]+) Stk. " //
                                        + "[.,\\d]+ [A-Z]{3} " //
                                        + "(?<amount>[.,\\d]+) (?<currency>[A-Z]{3})")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("date", "time") //
                        .match("^Ausführung (?<date>[\\d]{2}\\.[\\w]{2}\\.[\\d]{4}) " //
                                        + "(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        .section("fee", "currency") //
                        .optional() //
                        .match("^Ordergebühren \\+(?<fee>[.,\\d]+) (?<currency>[A-Z]{3})$").assign((t, v) -> {
                            var currency = asCurrencyCode(v.get("currency"));
                            var fee = asAmount(v.get("fee"));
                            var tx = t.getPortfolioTransaction();

                            tx.addUnit(new Unit(Unit.Type.FEE, Money.of(currency, fee)));
                            t.setAmount(tx.getAmount() + fee);
                        })

                        .wrap(e -> e.getPortfolioTransaction().getSecurity() == null ? null : new BuySellEntryItem(e)));
    }

}
