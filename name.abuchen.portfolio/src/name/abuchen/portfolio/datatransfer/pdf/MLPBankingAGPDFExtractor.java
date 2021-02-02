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
public class MLPBankingAGPDFExtractor extends AbstractPDFExtractor
{
    public MLPBankingAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("MLP Banking AG"); //$NON-NLS-1$

        addBuySellTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return "MLP Banking AG"; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "MLP Banking AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType newType = new DocumentType(".*Abrechnung Kauf.*");
        this.addDocumentTyp(newType);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block(".*Abrechnung Kauf.*");
        newType.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
            // Stück 4,929 SAUREN GLOBAL BALANCED LU0106280836 (930920)
            // INHABER-ANTEILE A O.N
            .section("isin", "wkn", "name", "shares", "nameContinued")
            .match("^(Stück) (?<shares>[\\d.,]+) (?<name>.*) (?<isin>[\\w]{12}.*) (\\((?<wkn>.*)\\).*)")
            .match("(?<nameContinued>.*)")
            .assign((t, v) -> {
                t.setSecurity(getOrCreateSecurity(v));
                t.setShares(asShares(v.get("shares")));
            })

            // Schlusstag 14.01.2021
            .section("date")
            .match("^(Schlusstag) (?<date>\\d+.\\d+.\\d{4}+).*")
            .assign((t, v) -> {
                    t.setDate(asDate(v.get("date")));
            })

            // Ausmachender Betrag 100,01- EUR
            .section("currency", "amount")
            .match("^(Ausmachender Betrag) (?<amount>[\\d.]+,\\d+)[?(-|\\+)] (?<currency>\\w{3}+)")
            .assign((t, v) -> {
                t.setAmount(asAmount(v.get("amount")));
                t.setCurrencyCode(v.get("currency"));
            })
            
            // Ihr Ausgabeaufschlag betraegt:
            // 0,00 EUR (0,000 Prozent)
            .section("fee", "currency")
            .match("^(?<fee>[\\d.-]+,\\d+) (?<currency>[\\w]{3}) \\([\\d.-]+,\\d+ \\w+\\)$")
            .assign((t, v) -> t.getPortfolioTransaction()
                            .addUnit(new Unit(Unit.Type.FEE,
                                            Money.of(asCurrencyCode(v.get("currency")),
                                                            asAmount(v.get("fee"))))))
            .wrap(BuySellEntryItem::new);
    }
}
