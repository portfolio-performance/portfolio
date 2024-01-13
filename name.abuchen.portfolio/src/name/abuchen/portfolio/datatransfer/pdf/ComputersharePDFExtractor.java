/**
 * 
 */
package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;

/**
 * Extractor for Computershare (https://www.computershare.com/) depot documents.
 * Supported: * Import transaction purchase entries from Statement documents
 * (only document CS provides)
 */
@SuppressWarnings("nls")
public class ComputersharePDFExtractor extends AbstractPDFExtractor
{

    private static final String BANK_IDENTIFIER = "Computershare";
    private static final String BANK_LABEL = "Computershare Trust Company, N.A.";
    private static final String BLOCK_IDENTIFIER_PURCHASE = "^[\\d]{2} [\\w]{3} [\\d]{4} Purchase.*";
    private static final String DOCUMENT_IDENTIFIER_STATEMENT_DOC = ".*Plan Statement";
    private static final String PATTERN_KEY_AMOUNT = "amount";
    private static final String PATTERN_KEY_CURRENCY = "currency";
    private static final String PATTERN_KEY_DATE = "date";
    private static final String PATTERN_KEY_FEE = "fee";
    private static final String PATTERN_KEY_NAME = "name";
    private static final String PATTERN_KEY_SHARES = "shares";
    private static final String PATTERN_KEY_TICKERSYMBOL = "tickerSymbol";
    private static final String PATTERN_KEY_WKN = "wkn";
    private static final String PATTERN_PURCHASE_ENTRY = "^(?<date>[\\d]{2} [\\w]{3} [\\d]{4}) Purchase (?<amount>[\\.,\\d]+) (?<fee>([\\.,\\d]+)\\s)?(?<netAmount>[\\.,\\d]+) (?<grantDate>[\\d]{2} [\\w]{3} [\\d]{4}) (?<fmvGrant>[\\.,\\d]+) (?<purchaseDate>[\\d]{2} [\\w]{3} [\\d]{4}) (?<fmvPurchase>[\\.,\\d]+) (?<sharePrice>[\\.,\\d]+) (?<shares>[\\.,\\d]+) (?<totalShares>[\\.,\\d]+).*";
    private static final String PATTERN_STRING_CUSIP = "^(?<wkn>[A-Z0-9]{9})$";
    private static final String PATTERN_STRING_DOUBLE_DATE = "^[\\d]{2} [\\w]{3} [\\d]{4} [\\d]{2} [\\w]{3} [\\d]{4}$";
    private static final String PATTERN_STRING_SINGLE_DATE = "^[\\d]{2} [\\w]{3} [\\d]{4}$";
    private static final String PATTERN_STRING_TICKERSYMBOL = "^(?<tickerSymbol>\\S*)$";

    /**
     * @param client
     */
    public ComputersharePDFExtractor(Client client)
    {
        super(client);

        // Add bank identifier to be able identify the bank documents
        addBankIdentifier(BANK_IDENTIFIER);
        // register depot statement processing
        addDepotStatementTransaction();
    }

    private void addDepotStatementTransaction()
    {


        final DocumentType type = new DocumentType(DOCUMENT_IDENTIFIER_STATEMENT_DOC, 
                        (context, lines) -> {
                            // currently no parsable security information
                            // identifier in statement document - needs line
                            // guessing
                            final Pattern singleDatePattern = Pattern.compile(PATTERN_STRING_SINGLE_DATE);
                            final Pattern doubleDatePattern = Pattern.compile(PATTERN_STRING_DOUBLE_DATE);
                            final Pattern cusipPattern = Pattern.compile(PATTERN_STRING_CUSIP);
                            final Pattern tickerSymbolPattern = Pattern.compile(PATTERN_STRING_TICKERSYMBOL);
                            
                            int idxSingleDate = -1;
                            int idxDoubleDate = -1;
                            int max = lines.length;
                            
                            for (int i = 0; i < max && (idxDoubleDate == -1 || idxSingleDate == -1); i++)
                            {
                                idxSingleDate = idxSingleDate == -1 && singleDatePattern.matcher(lines[i]).matches() ? i
                                                : idxSingleDate;
                                idxDoubleDate = idxDoubleDate == -1 && doubleDatePattern.matcher(lines[i]).matches() ? i
                                                : idxDoubleDate;
                            }
                            if (idxSingleDate > 3 && idxDoubleDate > 4 && (idxDoubleDate - 2) == idxSingleDate)
                            {

                                final Matcher mCusip = cusipPattern.matcher(lines[idxSingleDate - 2]);
                                final Matcher mTickerSymbol = tickerSymbolPattern.matcher(lines[idxSingleDate - 3]);
                                if (mCusip.matches() && mTickerSymbol.matches())
                                {
                                    context.put(PATTERN_KEY_TICKERSYMBOL, mTickerSymbol.group(PATTERN_KEY_TICKERSYMBOL));
                                    context.put(PATTERN_KEY_WKN, mCusip.group(PATTERN_KEY_WKN));
                                }
                                else
                                {
                                    throw new IllegalArgumentException("Cannot retrieve security information");
                                }
                            
                            }
                        });

        this.addDocumentTyp(type);
        

        // define first block to process
        final Block purchaseBlocks = new Block(BLOCK_IDENTIFIER_PURCHASE);
        type.addBlock(purchaseBlocks);
        
        // create source
        final Transaction<BuySellEntry> purchaseTransaction = new Transaction<>();
        purchaseBlocks.set(purchaseTransaction);
        purchaseTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });
        purchaseTransaction.oneOf(section -> section
                        .attributes(PATTERN_KEY_DATE, PATTERN_KEY_AMOUNT, PATTERN_KEY_SHARES, PATTERN_KEY_FEE)
                                        .documentContext(PATTERN_KEY_TICKERSYMBOL, PATTERN_KEY_WKN)
                        .match(PATTERN_PURCHASE_ENTRY)//
                                        .assign((t, v) -> {
                            v.put(PATTERN_KEY_CURRENCY, "USD");
                            t.setDate(asDate(v.get(PATTERN_KEY_DATE)));
                                            t.setCurrencyCode(asCurrencyCode("USD"));
                            t.setAmount(asAmount(v.get(PATTERN_KEY_AMOUNT)));
                            t.setShares(asShares(v.get(PATTERN_KEY_SHARES)));
                                            final Security security = getOrCreateSecurity(v);
                                            security.setCurrencyCode(asCurrencyCode("USD"));
                                            t.setSecurity(security);
                                            processFeeEntries(t, v, type);
                                        }), section -> section.attributes(PATTERN_KEY_DATE, PATTERN_KEY_AMOUNT, PATTERN_KEY_SHARES)
                        .documentContext(PATTERN_KEY_TICKERSYMBOL, PATTERN_KEY_WKN)
                        .match(PATTERN_PURCHASE_ENTRY)//
                        .assign((t, v) -> {
                                                            v.put(PATTERN_KEY_CURRENCY, "USD");
                            t.setDate(asDate(v.get(PATTERN_KEY_DATE)));
                                                            t.setCurrencyCode(v.get(PATTERN_KEY_CURRENCY));
                            t.setAmount(asAmount(v.get(PATTERN_KEY_AMOUNT)));
                                            t.setShares(asShares(v.get(PATTERN_KEY_SHARES)));
                            final Security security = getOrCreateSecurity(v);
                            security.setCurrencyCode(asCurrencyCode("USD"));
                            t.setSecurity(security);

                                                        })
                            

                        ).wrap(BuySellEntryItem::new);

    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, Locale.ENGLISH.getLanguage(),
                        Locale.ENGLISH.getCountry());
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, Locale.ENGLISH.getLanguage(),
                        Locale.ENGLISH.getCountry());
    }

    @Override
    public String getLabel()
    {
        return BANK_LABEL;
    }

}
