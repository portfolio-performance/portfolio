package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class FidelityStockPlanPDFExtractor extends AbstractPDFExtractor
{
    public FidelityStockPlanPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("FIDELITY STOCK PLAN SERVICES, LLC"); //$NON-NLS-1$

        addBuySellTransaction();

    }

    @SuppressWarnings("nls")
    private void addBuySellTransaction()
    {
        // line must start with this!
        DocumentType type = new DocumentType("SECURITY DESCRIPTION");
        this.addDocumentTyp(type);

        Block block = new Block(
                        "CUSTOMER NO.   PARTICIPANT ID. TYPE REG.REP. TRADE DATE SETTLEMENT DATE TRANS NO. CUSIP NO. ORIG.");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("month", "day", "year") //
                        .match("^\\s*\\w\\d+.* (?<month>\\d+)-(?<day>\\d+)-(?<year>\\d+) \\d+-\\d+-\\d+.*$")
                        .assign((t, v) -> //
                        t.setDate(asDate(v.get("day") + "." + v.get("month") + ".20" + v.get("year"))))

                        .section("shares") //
                        .match("^YOU SOLD (?<shares>[\\.,\\d]+) AT.*$") //
                        .assign((t, v) -> t.setShares(asShares(convertFromUs(v.get("shares")))))

                        .section("gross", "tickerSymbol") //
                        .match("SECURITY DESCRIPTION SYMBOL: (?<tickerSymbol>[A-Z]{2,5}) Sale Proceeds\\s+\\$(?<gross>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode("USD"));
                            v.put("currency", "USD");
                            t.setSecurity(getOrCreateSecurity(v));
                            // t.setAmount(asAmount(convertFromUs(v.get("gross"))));
                            // t.getPortfolioTransaction().addUnit(new
                            // Unit(Unit.Type.GROSS_VALUE,
                            // Money.of(asCurrencyCode(asCurrencyCode("USD")),
                            // asAmount(convertFromUs(v.get("gross"))))));

                        })

                        .section("amount") //
                        .match(".*Net Cash Proceeds.*\\$(?<amount>[\\.,\\d]+)$")
                        .assign((t, v) -> t.setAmount(asAmount(convertFromUs(v.get("amount")))))

                        .section("fee") //
                        .optional() //
                        .match(".*Total Fees\\s+\\$(?<fee>[\\.,\\d]+)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(asCurrencyCode("USD")),
                                                                            asAmount(convertFromUs(v.get("fee"))))));
                        })

                        .wrap(BuySellEntryItem::new));
    }

    @Override
    public String getLabel()
    {
        return "Fidelity"; //$NON-NLS-1$
    }

    public String convertFromUs(String amount)
    {
        // 53,321.56 => 53.321,56
        String val = amount.replace(',', '#');
        val = val.replace('.', ',');
        val = val.replace('#', '.');
        return val;
    }
}
