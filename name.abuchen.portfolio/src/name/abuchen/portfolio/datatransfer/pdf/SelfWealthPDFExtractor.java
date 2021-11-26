package name.abuchen.portfolio.datatransfer.pdf;

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
import name.abuchen.portfolio.util.TextUtil;

@SuppressWarnings("nls")
public class SelfWealthPDFExtractor extends AbstractPDFExtractor
{
    private static final DateTimeFormatter australianDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    public SelfWealthPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("SelfWealth"); //$NON-NLS-1$

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "SelfWealth"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Buy|Sell) Confirmation");
        this.addDocumentTyp(type);

        Block firstRelevantLine = new Block("^(Buy|Sell) Confirmation$");
        type.addBlock(firstRelevantLine);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Sell" change from BUY to SELL
                .section("type").optional() //
                .match("^(?<type>Sell) Confirmation$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Sell"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // 1 LONG ROAD Trade Date: 1 Jul 2021
                .section("date")
                .match("^.* Trade Date: (?<date>[\\d]+ [\\D]{3} [\\d]{4})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // 25 UMAX BETA S&P500 YIELDMAX 12.40 $312.50 AUD
                .section("shares", "tickerSymbol", "name", "amount", "currency")
                .match("^(?<shares>[\\.,\\d]+) (?<tickerSymbol>[\\w]{3,4}) (?<name>.*) [\\.,\\d]+ \\D(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Net Value $322.00 AUD
                .section("amount", "currency")
                .match("^Net Value \\D(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // JOHN DOE A/C Reference No: T20210701123456­-1
                .section("note").optional()
                .match("^.* Reference No: (?<note>.*)$")
                .assign((t, v) -> t.setNote(TextUtil.strip(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Brokerage* $9.50 AUD
                .section("fee", "currency").optional()
                .match("^Brokerage\\* \\D(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Adviser Fee* $0.00 AUD
                .section("fee", "currency").optional()
                .match("^Adviser Fee\\* \\D(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
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
        return PDFExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "AU");
    }

    @Override
    protected long asShares(String value)
    {
        return PDFExtractorUtils.convertToNumberLong(value, Values.Share, "en", "AU");
    }
}
