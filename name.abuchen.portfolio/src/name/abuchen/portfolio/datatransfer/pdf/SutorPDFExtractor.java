package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;

public class SutorPDFExtractor extends AbstractPDFExtractor
{

    public SutorPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Sutor"); //$NON-NLS-1$
        addBankIdentifier("Sutor Bank"); //$NON-NLS-1$

        addBuyTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return "Sutor Bank"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Sutor fairriester 2.0 | Umsätze");
        this.addDocumentTyp(type);

        Block block = new Block(".* Kauf .*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("name").match("^.* Kauf (?<name>[^\\d]*) .*")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("date").match("^(\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+) .*")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("amount")
                        .match("^(\\d+.\\d+.\\d{4}+) (\\d+.\\d+.\\d{4}+) -(?<amount>[\\.\\d]+[,\\d]*) .*")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            // Sutor always provides the amount in EUR, column
                            // "Betrag in EUR"
                            t.setCurrencyCode(CurrencyUnit.EUR);
                        })

                        .section("shares").match("^.* (?<shares>[\\.\\d]+[,\\d]*)$")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .wrap(BuySellEntryItem::new));
    }

    @Override
    public String getLabel()
    {
        return "Sutor Fairriester"; //$NON-NLS-1$
    }

}
