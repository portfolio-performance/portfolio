package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class FidelityPDFExtractor extends AbstractPDFExtractor
{
    public FidelityPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("FIDELITY BROKERAGE SERVICES LLC"); //$NON-NLS-1$

        addBuySellTransaction();
        // addSellTransaction();
        // addDividendTransaction();
        // addInboundDelivery();
    }

    @SuppressWarnings("nls")
    private void addBuySellTransaction()
    {
        // line must start with this!
        DocumentType type = new DocumentType("Transaction Confirmation");
        this.addDocumentTyp(type);

        Block block = new Block("REFERENCE NO\\. TYPE REG\\.REP\\. TRADE DATE SETTLEMENT DATE CUSIP NO\\. ORDER NO\\.",
                        "^(?!REFERENCE)(?!DESCRIPTION)[A-Z]{2,5}.*$");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })
                        /*
                         * REFERENCE NO. TYPE REG.REP. TRADE DATE SETTLEMENT
                         * DATE CUSIP NO. ORDER NO. 23348-SR3JMJ 1* WO# 12-14-23
                         * 12-18-23 02079K107 23348-I7W2H DESCRIPTION and
                         * DISCLOSURES You Bought ALPHABET INC CAP STK CL C
                         * Principal Amount 3,990.00 30 WE HAVE ACTED AS AGENT.
                         * Settlement Amount 3,990.00 at 133.0000 Symbol: GOOG
                         */

                        .section("month", "day", "year") //
                        .match("^\\d+-\\w+.* (?<month>\\d+)-(?<day>\\d+)-(?<year>\\d+) \\d+-\\d+-\\d+.*$")
                        .assign((t, v) -> //
                        t.setDate(asDate(v.get("day") + "." + v.get("month") + ".20" + v.get("year"))))

                        .section("type", "gross", "tickerSymbol", "name") //
                        .match("^You (?<type>Bought|Sold) (?<name>.*) Principal Amount\\s+(?<gross>[\\.,\\d]+)$") //
                        .match("^(?!REFERENCE)(?!DESCRIPTION)(?<tickerSymbol>[A-Z]{2,5}).*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode("USD"));
                            v.put("currency", "USD");
                            t.setSecurity(getOrCreateSecurity(v));
                            if ("Sold".equals(v.get("type")))
                            {
                                    t.setType(PortfolioTransaction.Type.SELL);
                            }
                            // t.setAmount(asAmount(convertFromUs(v.get("gross"))));
                            // t.getPortfolioTransaction().addUnit(new
                            // Unit(Unit.Type.GROSS_VALUE,
                            // Money.of(asCurrencyCode(asCurrencyCode("USD")),
                            // asAmount(convertFromUs(v.get("gross"))))));

                        })

                        .section("amount") //
                        .match(".*Settlement Amount\\s+(?<amount>[\\.,\\d]+)$")
                        .assign((t, v) -> t.setAmount(asAmount(convertFromUs(v.get("amount")))))

                        .section("fee") //
                        .optional() //
                        .match(".*Activity Assessmen\\s?t\\s+F\\s?e\\s?e\\s+(?<fee>[\\.,\\d]+)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(asCurrencyCode("USD")),
                                                                            asAmount(convertFromUs(v.get("fee"))))));
                        })

                        .section("shares") //
                        .match("^\\s+(?<shares>[\\.,\\d]+)\\s.*$") //
                        .assign((t, v) -> t.setShares(asShares(convertFromUs(v.get("shares")))))

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
