package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class SimpelPDFExtractor extends AbstractPDFExtractor
{
    private static final String EXCHANGE_RATE = "exchangeRate"; //$NON-NLS-1$
    private static final String FLAG_WITHHOLDING_TAX_FOUND = "isHoldingTax"; //$NON-NLS-1$

    public SimpelPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("LU32888126"); //$NON-NLS-1$

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Simpel S.A."; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Fondsabrechnung");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^ (Kauf|Verkauf)  .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
            //  Kauf  Standortfonds Österreich 10.00 €  140.59 €  0.071
            .section("type", "name", "amount", "kurs", "shares", "isin", "date", "shares1")
            .match("^ (?<type>(Kauf|Verkauf))  (?<name>.*) (?<amount>[\\-\\.,\\d]+) €  (?<kurs>[\\-\\.,\\d]+) €  (?<shares>[\\d.,]+)$")
            .match("^(?<isin>[\\w]{12}) (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}) (?<shares1>[\\d.,]+)$").assign((t, v) -> {
                if (v.get("type").equals("Verkauf"))
                {
                    t.setType(PortfolioTransaction.Type.SELL);
                }
                
                t.setSecurity(getOrCreateSecurity(v));
                t.setCurrencyCode("EUR");
                t.setShares(asShares(normalizeAmount(v.get("shares"))));
                
                t.setAmount(asAmount(normalizeAmount(v.get("amount"))));
                
                t.setDate(asDate(v.get("date")));
            })

            .wrap(BuySellEntryItem::new);

        /*
         * addTaxesSectionsTransaction(pdfTransaction, type);
         * addFeesSectionsTransaction(pdfTransaction, type);
         */
    }
    
    private static String normalizeAmount(String amount) {
        return amount.replace(",","").replace('.', ',');
    }
}
