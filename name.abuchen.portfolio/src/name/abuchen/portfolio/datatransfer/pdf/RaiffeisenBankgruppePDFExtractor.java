package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Map;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class RaiffeisenBankgruppePDFExtractor extends AbstractPDFExtractor
{
    public RaiffeisenBankgruppePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Raiffeisenbank"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "Raiffeisenbank Bankgruppe"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Kauf|Verkauf");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Gesch.ftsart: (Kauf|Verkauf) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("Gesch.ftsart: (?<type>Verkauf) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Titel: DE000BAY0017 Bayer AG
                // Namens-Aktien o.N.
                // Kurs: 53,47 EUR
                .section("isin", "name", "name1", "currency")
                .match("^Titel: (?<isin>[\\w]{12}) (?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^Kurs: ([.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Kurs:"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Zugang: 2 Stk   
                // Abgang: 4.500 Stk   
                .section("shares")
                .match("^(Zugang|Abgang): (?<shares>[.,\\d]+)(.*)?$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Handelszeit: 03.05.2021 13:45:18 
                .section("date", "time")
                .match("^Handelszeit: (?<date>\\d+.\\d+.\\d{4}) (?<time>\\d+:\\d+:\\d+)$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Zu Lasten IBAN AT99 9999 9000 0011 1110 -107,26 EUR  
                // Zu Gunsten IBAN AT27 3284 2000 0011 1111 36.115,76 EUR 
                .section("amount", "currency")
                .match("^Zu (Lasten|Gunsten) .* ([-])?(?<amount>[.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Ertrag");
        this.addDocumentTyp(type);

        Block block = new Block("Gesch.ftsart: Ertrag");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // Titel: DE000BAY0017 Bayer AG
                // Namens-Aktien o.N.
                // Dividende: 2 EUR
                .section("isin", "name", "name1", "currency")
                .match("^Titel: (?<isin>[\\w]{12}) (?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^Dividende: ([.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Dividende:"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Geschäftsart: Ertrag
                // 90 Stk    
                .section("shares")
                .match("^Gesch.ftsart: .*$")
                .match("^(?<shares>[.,\\d]+) Stk(.*)?$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Extag: 28.04.2021
                .section("date")
                .match("^Extag: (?<date>\\d+.\\d+.\\d{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Zu Gunsten IBAN AT99 9999 9000 0011 1111 110,02 EUR 
                .section("amount", "currency")
                .match("^Zu Gunsten .* (?<amount>[.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Quellensteuer: -47,48 EUR 
                .section("tax", "currency").optional()
                .match("^Quellensteuer: -(?<tax>[.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Auslands-KESt: -22,50 EUR 
                .section("tax", "currency").optional()
                .match("^Auslands-KESt: -(?<tax>[.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kursgewinn-KESt: -696,65 EUR 
                .section("tax", "currency").optional()
                .match("^Kursgewinn-KESt: -(?<tax>[.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Serviceentgelt: -0,32 EUR 
                .section("fee", "currency").optional()
                .match("^Serviceentgelt: -(?<fee>[.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
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
