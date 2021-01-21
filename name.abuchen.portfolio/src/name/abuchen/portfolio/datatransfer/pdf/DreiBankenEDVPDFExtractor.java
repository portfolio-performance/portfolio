package name.abuchen.portfolio.datatransfer.pdf;


import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;


public class DreiBankenEDVPDFExtractor extends AbstractPDFExtractor
{   
    public DreiBankenEDVPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("91810s/Klagenfurt"); //$NON-NLS-1$
 
        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return "3BankenEDV"; //$NON-NLS-1$
    }
    
    @Override
    public String getLabel()
    {
        return "3BankenEDV"; //$NON-NLS-1$
    }
    
    @SuppressWarnings("nls")
    private void addBuySellTransaction()
    {
        DocumentType newType = new DocumentType(".*(Kauf|Verkauf).*");
        this.addDocumentTyp(newType);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block(".*(Kauf|Verkauf).*");
        newType.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
    
            // Is type --> "Verkauf" change from BUY to SELL
            .section("type").optional()
            .match("Wertpapier-Abrechnu.*(?<type>Verkauf?).*") //
            .assign((t, v) -> {
                if (v.get("type").equals("Verkauf")) 
                {
                    t.setType(PortfolioTransaction.Type.SELL);
                }
            })
    
            // LU0675401409 Lyxor Emerg Market 2x Lev ETF Zugang Stk .               2,00
            // Inhaber-Anteile I o.N.
            .section("isin", "name", "shares", "nameContinued")
            .match("(?<isin>[\\w]{12}.*?) (?<name>.*?) (Zugang|Abgang).*(?<shares>[\\d.]+(,\\d+)).*")
            .match("(?<nameContinued>.*)")
            .assign((t, v) -> {
                v.put("isin", v.get("isin"));
                v.put("name", v.get("name"));                            
                t.setSecurity(getOrCreateSecurity(v));
                t.setShares(asShares(v.get("shares")));
            })
                            
            // Handelszeitpunkt: 04.01.2021 12:05:55
            .section("date", "time")
            .match("^(Handelszeitpunkt:).*(?<date>\\d+.\\d+.\\d{4}+) (?<time>\\d+:\\d+:\\d+).*")
            .assign((t, v) -> {
                if (v.get("time") != null)
                    t.setDate(asDate(v.get("date"), v.get("time")));
                else
                    t.setDate(asDate(v.get("date")));
            })
            
            // Wertpapierrechnung Wert 06.01.2021 EUR 205,30
            .section("currency", "amount")
            .match("^(Wertpapierrechn.* Wert) (\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) *(?<amount>[\\d.-]+,\\d+).*")
            .assign((t, v) -> {
                t.setAmount(asAmount(v.get("amount")));
                t.setCurrencyCode(v.get("currency"));
            })
    
            // Kursgewinn-KESt EUR                -3,37
            .section("tax", "currency").optional()
            .match("^(Kursgewinn-KESt) (?<currency>\\w{3}).*(?<tax>-[\\d.]+,\\d{2})")
            .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))
            
            // Dritt- und Börsengebühr EUR                0,02
            .section("fee", "currency").optional() //
            .match("^(Dritt.*B.*sengeb.*) (?<currency>\\w{3}+).*(?<fee>[\\d.-]+,\\d+).*")
            .assign((t, v) -> t.getPortfolioTransaction()
                            .addUnit(new Unit(Unit.Type.FEE,
                                            Money.of(asCurrencyCode(v.get("currency")),
                                                            asAmount(v.get("fee"))))))
    
            .wrap(BuySellEntryItem::new);
    }

    @SuppressWarnings("nls")
    private void addDividendeTransaction()
    {
        final DocumentType type = new DocumentType(".*(Ausschüttung|Dividende).*");
        this.addDocumentTyp(type);

        Block block = new Block(".*(Ausschüttung|Dividende).*");
        type.addBlock(block);
        Transaction<AccountTransaction> transaction = new Transaction<AccountTransaction>()

        .subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        })

        // IE00B0M63284 iShs Euro.Property Yield U.ETF Stk . 4,00
        // Registered Shares EUR (Dist)oN
        .section("isin", "name", "shares", "nameContinued")
        .match("(?<isin>[\\w]{12}.*?) (?<name>.*?) (Stk .).*(?<shares>[\\d.]+(,\\d+)).*")
        .match("(?<nameContinued>.*)")
        //.assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
        .assign((t, v) -> {
            v.put("isin", v.get("isin"));
            v.put("name", v.get("name"));                            
            t.setSecurity(getOrCreateSecurity(v));
            t.setShares(asShares(v.get("shares")));
        })
        
        .section("currency", "amount")
        .match("^(Wertpapierrechn.* Wert) (\\d+.\\d+.\\d{4}+) (?<currency>\\w{3}+) *(?<amount>[\\d.-]+,\\d+).*")
        .assign((t, v) -> {
            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
            t.setAmount(asAmount(v.get("amount")));
        })
        
        .section("date")
        .match("^Extag (?<date>\\d+.\\d+.\\d{4}+).*") //
        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

        .wrap(TransactionItem::new);
        
        addTaxesSectionsTransaction(transaction, type);
        
        block.set(transaction);
    }
    
    @SuppressWarnings("nls")
    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
            // KESt-Neu
            // Ertrag 0,0806 EUR Kurswert EUR 0,32KESt-Neu EUR
            // -0,02
            .section("tax", "currency").optional()
            .match(".*KESt-Neu.* (?<currency>\\w{3}).*")
            .match(".*(?<tax>-[\\d.]+,\\d+).*")
            .assign((t, v) -> {
                processTaxEntries(t, v, type);
            });
    }
    
    @SuppressWarnings("nls")
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
            PDFExtractorUtils.checkAndSetTax(tax,
                            ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }
}