package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

public class TargobankPDFExtractor extends AbstractPDFExtractor
{
    private static final String regexName = "Wertpapier (?<name>.*)"; //$NON-NLS-1$
    private static final String regexWknAndIsin = "WKN / ISIN (?<wkn>\\S*) / (?<isin>\\S*)"; //$NON-NLS-1$
    private static final String regexAmountAndCurrency = "Konto-Nr. \\d* (?<amount>(\\d+\\.)?\\d+(,\\d+)?) (?<currency>\\w{3}+)"; //$NON-NLS-1$
    private static final String regexDate = "Schlusstag( / Handelszeit)? (?<date>\\d{2}.\\d{2}.\\d{4})( / (?<time>\\d{2}:\\d{2}:\\d{2}))?"; //$NON-NLS-1$
    private static final String regexTime = "(Schlusstag / )?Handelszeit ((?<date>\\d{2}.\\d{2}.\\d{4}) / )?(?<time>\\d{2}:\\d{2}:\\d{2})"; //$NON-NLS-1$
    private static final String regexShares = "St.ck (?<shares>\\d+(,\\d+)?)"; //$NON-NLS-1$
    
    public TargobankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("TARGO"); //$NON-NLS-1$
        addBankIdentifier("Targobank"); //$NON-NLS-1$
        
        addBuyTransaction();
        addSellTransaction();
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Kauf");
        this.addDocumentTyp(type);

        Block block = new Block("(Transaktionstyp )?Kauf"); 
        type.addBlock(block);   
        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("name", "wkn", "isin").optional()
                        .match(regexName)
                        .match(regexWknAndIsin)
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                        
                        .section("time").optional()
                        .match(regexTime)
                        .assign((t, v) -> {
                            type.getCurrentContext().put("time", v.get("time"));
                        })
                                                
                        .section("date").optional()
                        .match(regexDate)
                        .assign((t, v) -> {
                            if (type.getCurrentContext().get("time") != null)
                            {
                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                            }
                            else
                            {
                                t.setDate(asDate(v.get("date")));
                            }
                        })

                        .section("amount", "currency")
                        .match(regexAmountAndCurrency)
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })
                            
                        .section("shares").optional()
                        .match(regexShares)
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .wrap(t -> {
                            if (t.getPortfolioTransaction().getShares() == 0)
                                throw new IllegalArgumentException(Messages.PDFMsgMissingShares);
                            return new BuySellEntryItem(t);
                        });

        block.set(pdfTransaction);

    }
    
    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Verkauf");
        this.addDocumentTyp(type);

        Block block = new Block("(Transaktionstyp )?Verkauf");
        type.addBlock(block);   
        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("name", "wkn", "isin")
                        .match(regexName)
                        .match(regexWknAndIsin)
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("time").optional()
                        .match(regexTime)
                        .assign((t, v) -> {
                            type.getCurrentContext().put("time", v.get("time"));
                        })
                                                
                        .section("date").optional()
                        .match(regexDate)
                        .assign((t, v) -> {
                            if (type.getCurrentContext().get("time") != null)
                            {
                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                            }
                            else
                            {
                                t.setDate(asDate(v.get("date")));
                            }
                        })
                        
                        .section("amount", "currency")
                        .match(regexAmountAndCurrency)
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })
                            
                        .section("shares").optional()
                        .match(regexShares)
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .wrap(t -> {
                            if (t.getPortfolioTransaction().getShares() == 0)
                                throw new IllegalArgumentException(Messages.PDFMsgMissingShares);
                            return new BuySellEntryItem(t);
                        });

        block.set(pdfTransaction);

    }

    @Override
    public String getLabel()
    {
        return "TARGO"; //$NON-NLS-1$
    }

}
