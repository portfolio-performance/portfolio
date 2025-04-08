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
import name.abuchen.portfolio.money.Values;

/**
 * Extractor for Computershare (https://www.computershare.com/) depot documents.
 * Supported:
 * <ul>
 * <li>Import transaction purchase entries from Statement documents (only
 * document CS provides)</li>
 * </ul>
 */
@SuppressWarnings("nls")
public class ComputersharePDFExtractor extends AbstractPDFExtractor
{

    private static final String TICKERSYMBOL = "tickerSymbol";
    private static final String WKN = "wkn";

    public ComputersharePDFExtractor(Client client)
    {
        super(client);

        // Add bank identifier to be able identify the bank documents
        addBankIdentifier("Computershare");
        // register depot statement processing
        addDepotStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Computershare Trust Company, N.A.";
    }

    private void addDepotStatementTransaction()
    {

        final DocumentType type = new DocumentType(".*Plan Statement", (context, lines) -> {
            // currently no parsable security information identifier in
            // statement document - needs line guessing

            // the logic: find the lines with 1 and 2 dates respectively that
            // are 2 lines apart. Relative to those lines we find ticker and
            // cusip

            // @formatter:off
            // [...]
            // AMZN                                    <- ticker = single date line - 3
            // 023135106                               <- cusip = single date line - 2
            // Acme Labs - Employee Plan Statement
            // 29 Sep 2023                             <- single date
            // 29 Sep 2023
            // 01 Jan 2023 29 Sep 2023                 <- double date
            // [...]
            // @formatter:on

            final Pattern singleDatePattern = Pattern.compile("^[\\d]{2} [\\w]{3} [\\d]{4}$");
            final Pattern doubleDatePattern = Pattern
                            .compile("^[\\d]{2} [\\w]{3} [\\d]{4} [\\d]{2} [\\w]{3} [\\d]{4}$");
            final Pattern cusipPattern = Pattern.compile("^(?<wkn>[A-Z0-9]{9})$");
            final Pattern tickerSymbolPattern = Pattern.compile("^(?<tickerSymbol>\\S*)$");

            int idxSingleDate = -1;
            int idxDoubleDate = -1;
            int max = lines.length;

            for (int ii = 0; ii < max && (idxDoubleDate == -1 || idxSingleDate == -1); ii++)
            {
                idxSingleDate = idxSingleDate == -1 && singleDatePattern.matcher(lines[ii]).matches() ? ii
                                : idxSingleDate;
                idxDoubleDate = idxDoubleDate == -1 && doubleDatePattern.matcher(lines[ii]).matches() ? ii
                                : idxDoubleDate;
            }

            if (idxSingleDate > 3 && idxDoubleDate > 4 && (idxDoubleDate - 2) == idxSingleDate)
            {

                final Matcher mCusip = cusipPattern.matcher(lines[idxSingleDate - 2]);
                final Matcher mTickerSymbol = tickerSymbolPattern.matcher(lines[idxSingleDate - 3]);
                if (mCusip.matches() && mTickerSymbol.matches())
                {
                    context.put(TICKERSYMBOL, mTickerSymbol.group(TICKERSYMBOL));
                    context.put(WKN, mCusip.group(WKN));
                }
            }

            if (!context.containsKey(TICKERSYMBOL))
                throw new IllegalArgumentException("Cannot retrieve security information");
        });

        this.addDocumentTyp(type);

        // define first block to process
        final Block purchaseBlocks = new Block("^[\\d]{2} [\\w]{3} [\\d]{4} Purchase.*");
        type.addBlock(purchaseBlocks);

        // create source
        final Transaction<BuySellEntry> purchaseTransaction = new Transaction<>();
        purchaseBlocks.set(purchaseTransaction);
        purchaseTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });
        purchaseTransaction.oneOf(

                        section -> section.attributes("date", "amount", "shares", "fee") //
                                        .documentContext(TICKERSYMBOL, WKN) //
                                        .match("^(?<date>[\\d]{2} [\\w]{3} [\\d]{4}) Purchase (?<amount>[\\.,\\d]+) (?<fee>[\\.,\\d]+) (?<netAmount>[\\.,\\d]+) (?<grantDate>[\\d]{2} [\\w]{3} [\\d]{4}) (?<fmvGrant>[\\.,\\d]+) (?<purchaseDate>[\\d]{2} [\\w]{3} [\\d]{4}) (?<fmvPurchase>[\\.,\\d]+) (?<sharePrice>[\\.,\\d]+) (?<shares>[\\.,\\d]+) (?<totalShares>[\\.,\\d]+).*") //
                                        .assign((t, v) -> {
                                            v.put("currency", "USD");
                                            t.setDate(asDate(v.get("date")));
                                            t.setCurrencyCode(asCurrencyCode("USD"));
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setShares(asShares(v.get("shares")));
                                            t.setSecurity(getOrCreateSecurity(v));
                                            processFeeEntries(t, v, type);
                                        }),

                        section -> section.attributes("date", "amount", "shares") //
                                        .documentContext(TICKERSYMBOL, WKN) //
                                        .match("^(?<date>[\\d]{2} [\\w]{3} [\\d]{4}) Purchase (?<amount>[\\.,\\d]+) (?<netAmount>[\\.,\\d]+) (?<grantDate>[\\d]{2} [\\w]{3} [\\d]{4}) (?<fmvGrant>[\\.,\\d]+) (?<purchaseDate>[\\d]{2} [\\w]{3} [\\d]{4}) (?<fmvPurchase>[\\.,\\d]+) (?<sharePrice>[\\.,\\d]+) (?<shares>[\\.,\\d]+) (?<totalShares>[\\.,\\d]+).*") //
                                        .assign((t, v) -> {
                                            v.put("currency", "USD");
                                            t.setDate(asDate(v.get("date")));
                                            t.setCurrencyCode(v.get("currency"));
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setShares(asShares(v.get("shares")));
                                            t.setSecurity(getOrCreateSecurity(v));
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

}
