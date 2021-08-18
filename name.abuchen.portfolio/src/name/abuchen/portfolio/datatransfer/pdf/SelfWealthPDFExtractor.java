package name.abuchen.portfolio.datatransfer.pdf;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class SelfWealthPDFExtractor extends AbstractPDFExtractor
{
    private static final DecimalFormat australianNumberFormat = (DecimalFormat) NumberFormat.getInstance(new Locale("en", "AU"));
    private static final DateTimeFormatter australianDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    public SelfWealthPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("SelfWealth"); //$NON-NLS-1$

        addBuySellTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "SelfWealth"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Buy|Sell)[\\W]Confirmation");
        this.addDocumentTyp(type);

        Block firstRelevantLine = new Block("^(Buy|Sell)[\\W]Confirmation$");
        type.addBlock(firstRelevantLine);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                /***
                 * Attention
                 * 
                 * There are spaces in some PDF documents that 
                 * are not directly visible. 
                 * (c2 a0 characters instead of 20) --> Fix with [\\W] patter
                 */

                // Is type --> "Sell" change from BUY to SELL
                .section("type").optional() //
                .match("^(?<type>Sell)[\\W]Confirmation$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Sell"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // 1 LONG ROAD Trade Date: 1 Jul 2021
                .section("date")
                .match(".*[\\W]Trade[\\W]Date:[\\W](?<date>\\d+[\\W][\\D]{3}[\\W][\\d]{4})$")
                .assign((t, v) -> {
                    v.put("date", v.get("date").replaceAll("[\\W]", " "));
                    t.setDate(asDate(v.get("date")));
                })

                // 25 UMAX BETA S&P500 YIELDMAX 12.40 $312.50 AUD
                .section("shares", "tickerSymbol", "name", "amount", "currency")
                .match("^(?<shares>[.,\\d]+)[\\W](?<tickerSymbol>[\\w]{3,4})[\\W](?<name>.*) [.,\\d]+[\\W][\\D](?<amount>[.,\\d]+)[\\W](?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Net Value $322.00 AUD
                .section("amount", "currency")
                .match("^Net[\\W]Value[\\W][\\D](?<amount>[.,\\d]+)[\\W](?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // JOHN DOE A/C Reference No: T20210701123456­-1
                .section("note").optional()
                .match("^.*[\\W]Reference[\\W]No:[\\W](?<note>.*)$")
                .assign((t, v) -> t.setNote(v.get("note")))

                .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Brokerage* $9.50 AUD
                .section("fee", "currency").optional()
                .match("^Brokerage\\*[\\W][\\D](?<fee>.*)[\\W](?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Adviser Fee* $0.00 AUD
                .section("fee", "currency").optional()
                .match("^Adviser[\\W]Fee\\*[\\W][\\D](?<fee>.*)[\\W](?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
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

    @Override
    protected LocalDateTime asDate(String value)
    {
        return LocalDate.parse(value, australianDateFormat).atStartOfDay();
    }

    @Override
    protected long asAmount(String value)
    {
        try
        {
            return Math.abs(Math.round(australianNumberFormat.parse(value).doubleValue() * Values.Amount.factor()));
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

}
