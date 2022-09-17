package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class CommSecPDFExtractor extends AbstractPDFExtractor
{
    public CommSecPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Commonwealth Securities Limited"); //$NON-NLS-1$

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Commonwealth Securities Limited"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("WE HAVE (SOLD|BOUGHT)", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^CONSIDERATION \\((?<currency>[\\w]{3})\\): \\p{Sc}[\\.,\\d]+ .*$");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group("currency"));
                }
            }
        });
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^WE HAVE (SOLD|BOUGHT) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "SOLD" change from BUY to SELL
                .section("type").optional()
                .match("^WE HAVE (?<type>SOLD) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("SOLD"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // COMPANY: QANTAS AIRWAYS LIMITED
                // QAN
                // CONSIDERATION (AUD): $999.00 CONTRACT COMMENTS:
                .section("name", "tickerSymbol", "currency").optional()
                .match("^COMPANY: (?<name>.*)$")
                .match("^(?<tickerSymbol>[\\w]{3,4})$")
                .match("^CONSIDERATION \\((?<currency>[\\w]{3})\\): \\p{Sc}[\\.,\\d]+ .*$")
                .assign((t, v) -> {                    
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // COMPANY WISETECH GLOBAL LIMITED
                // SECURITY ORDINARY FULLY PAID WTC
                // CONSIDERATION (AUD): $28,060.00 PID XXXX HIN XXXXXXX
                .section("name", "tickerSymbol", "currency").optional()
                .match("^COMPANY (?<name>.*)$")
                .match("^SECURITY ORDINARY FULLY PAID (?<tickerSymbol>[\\w]{3,4})$")
                .match("^CONSIDERATION \\((?<currency>[\\w]{3})\\): \\p{Sc}[\\.,\\d]+ .*$")
                .assign((t, v) -> {                    
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // AS AT DATE: 20/04/2020 277 3.610000
                .section("date")
                .match("^AS AT DATE: (?<date>[\\d]+\\/[\\d]+\\/[\\d]{4}) .*$")
                .assign((t, v) -> {
                    t.setDate(asDate(v.get("date")));
                })

                .oneOf(
                                // AS AT DATE: 20/04/2020 277 3.610000
                                section -> section
                                        .attributes("shares")
                                        .match("^AS AT DATE: .* (?<shares>[\\.,\\d]+) [\\.,\\d]+$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // CONFIRMATION NO: XXXXXXX 1,000 28.060000
                                section -> section
                                        .attributes("shares")
                                        .match("^CONFIRMATION NO: .* (?<shares>[\\.,\\d]+) [\\.,\\d]+$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                // TOTAL COST: $1,092.92
                // NET PROCEEDS: $28,031.94
                .section("amount")
                .match("^(TOTAL COST|NET PROCEEDS): \\p{Sc}(?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setCurrencyCode(context.get("currency"));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // TOTAL GST: $2.72
                .section("tax").optional()
                .match("^TOTAL GST: \\p{Sc}(?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", context.get("currency"));

                    processTaxEntries(t, v, type);
                })

                // TOTAL GST: $2.55 105
                .section("tax").optional()
                .match("^TOTAL GST: \\p{Sc}(?<tax>[\\.,\\d]+) .*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", context.get("currency"));

                    processTaxEntries(t, v, type);
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // BROKERAGE & COSTS INCL GST: $29.95 55685147 0404181685
                .section("fee").optional()
                .match("^BROKERAGE & COSTS INCL GST: \\p{Sc}(?<fee>[\\.,\\d]+) .*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", context.get("currency"));

                    processFeeEntries(t, v, type);
                })

                // APPLICATION MONEY: $0.00
                .section("fee").optional()
                .match("^APPLICATION MONEY: \\p{Sc}(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", context.get("currency"));

                    processFeeEntries(t, v, type);
                });
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

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return PDFExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "en", "AU");
    }
}
