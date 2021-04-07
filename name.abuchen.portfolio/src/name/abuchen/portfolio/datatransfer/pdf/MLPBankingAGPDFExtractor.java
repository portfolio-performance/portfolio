package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Map;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
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
        DocumentType type = new DocumentType(".*Abrechnung Kauf.*");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block(".*Abrechnung Kauf.*");
        type.addBlock(firstRelevantLine);
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

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        //transaction
        // At this time there are no known tax or similar in the PDF debugs.
        // IF you found some, add this here like
        
        // Example
        // some taxes
        // .section("tax", "currency").optional()
        // .match("^ABC (?<tax>[.,\\d]+)[-]? (?<currency>[\\w]{3})$")
        // .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Ihr Ausgabeaufschlag betraegt:
                // 0,00 EUR (0,000 Prozent)
                .section("fee", "currency")
                .match("^(?<fee>[.,\\d]+) (?<currency>[\\w]{3}) \\([.,\\d]+ \\w+\\)$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    private void processTaxEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax, (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax, ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }

    private void processFeeEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee, 
                            (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee,
                            ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }
}
