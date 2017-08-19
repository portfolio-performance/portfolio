package name.abuchen.portfolio.datatransfer.pdf;

import java.io.IOException;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class UnicreditPDFExtractor extends AbstractPDFExtractor
{

    public UnicreditPDFExtractor(Client client) throws IOException
    {
        super(client);
        
        addBankIdentifier("UniCredit Bank AG"); //$NON-NLS-1$

        addBuyTransaction();
        
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("UniCredit");
        this.addDocumentTyp(type);

        Block block = new Block("W e r t p a p i e r - A b r e c h n u n g K a u f .*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        })
                        // SANOFI S.A. 920657
                        // ST 22
                        // ACTIONS PORT. EO 2 FR0000120578
                        .section("name", "wkn", "isin")
                        .find("Nennbetrag Wertpapierbezeichnung Wertpapierkennnummer/ISIN")
                        .match("^(?<name>.*) (?<wkn>[^ ]*)$")
                        .match("^.* (?<isin>[A-Z]{2}[A-Z\\d]{9}\\d)$")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                        
                        // ST 22
                        .section("shares")
                        .match("^ST *(?<shares>[\\.\\d]+[,\\d]*)$")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        
                        // Belastung (vor Steuern) EUR 1.560,83
                        .section("amount", "currency")
                        .match("^Belastung.* (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d{2})$")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })
                        
                        // Valuta 17.02.2016 -----------------------
                        .section("date")
                        .match("^Valuta (?<date>\\d+\\.\\d+\\.\\d{4}) .*")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                        
                        // Brokerkommission* EUR 0,27
                        .section("fee", "currency").optional()
                        .match("^Brokerkommission.* (?<currency>\\w{3}) (?<fee>[\\d.]+,\\d{2})$")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                          Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))
                        
                        // Transaktionsentgelt* EUR 3,09
                        .section("fee", "currency").optional()
                        .match("^Transaktionsentgelt.* (?<currency>\\w{3}) (?<fee>[\\d.]+,\\d{2})$")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                          Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))
                        
                        // Provision EUR 10,21
                        .section("fee", "currency").optional()
                        .match("^Provision.* (?<currency>\\w{3}) (?<fee>[\\d.]+,\\d{2})$")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                          Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .wrap(t -> new BuySellEntryItem(t)));
    }
    
    @Override
    public String getLabel()
    {
        return "unicredit"; //$NON-NLS-1$
    }
}
