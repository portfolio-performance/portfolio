package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Locale;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class QuestradePDFExtractor extends AbstractPDFExtractor
{
    public QuestradePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Questrade, Inc.");

        addAccountStatementTransaction();
        addPurchaseTransaction();
        addDividendTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Questrade, Inc.";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("04\\. ACTIVITY DETAILS", //
                        documentContext -> documentContext //
                                        .section("currency") //
                                        .match(".*Combined in (?<currency>[\\w]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();
        var firstRelevantLine = new Block(".* Contribution .*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Matches lines like:
                        // 04-09-2025 04-09-2025 Contribution CONT 6263984218 - - - - 10,000.00 - - - -
                        // @formatter:on
                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) [\\d]{2}\\-[\\d]{2}\\-[\\d]{4} Contribution .* (?<amount>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            // date format is mm-dd-yyyy
                            t.setDateTime(asDate(v.get("date"), Locale.US));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote("Contribution");
                        })

                        .wrap(TransactionItem::new);
    }

    private void addPurchaseTransaction()
    {
        final var type = new DocumentType("04\\. ACTIVITY DETAILS", //
                        documentContext -> documentContext //
                                        .section("currency") //
                                        .match(".*Combined in (?<currency>[\\w]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();
        var firstRelevantLine = new Block(".* Buy .*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var tx = new BuySellEntry();
                            tx.setType(PortfolioTransaction.Type.BUY);
                            return tx;
                        })

                        // @formatter:off
                        // Matches lines like:
                        // 04-10-2025 04-11-2025 Buy .VEQT VANGUARD ALL-EQUITY ETF  PORTFOLIO
                        // 01-16-2023 01-18-2023 Buy .VEQT VANGUARD ALL-EQUITY ETF|PORTFOLIO ETF 
                        // @formatter:on
                        .section("date", "tickerSymbol", "name") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) [\\d]{2}\\-[\\d]{2}\\-[\\d]{4} Buy \\.(?<tickerSymbol>\\S+)\\s+(?<name>.*?)$") //
                        .assign((t, v) -> {
                            v.put("name", v.get("name").trim());
                            v.put("tickerSymbol", asTickerSymbol(v.get("tickerSymbol")));

                            // date format is mm-dd-yyyy
                            t.setDate(asDate(v.get("date"), Locale.US));
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Matches lines like:
                                        // ETF UNIT  WE ACTED AS AGENT 50.0000 40.930 (2,046.50) - (2,046.50) - - - -
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "gross", "amount") //
                                                        .match("^.+ UNIT\\s+WE ACTED AS AGENT (?<shares>[\\d\\.,]+) (?<price>[\\d\\.,]+) \\((?<gross>[\\d,\\.\\-]+)\\) - \\((?<amount>[\\d,\\.\\-]+)\\) .*$") //
                                                        .assign((t, v) -> {
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),

                                        // @formatter:off
                                        // Matches lines like:
                                        // UNIT|WE ACTED AS AGENT 50.0000 40.930 (2,046.50) (0.10) (2,046.60) - - - -
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "gross", "fee", "amount") //
                                                        .documentContext("currency") //
                                                        .match("^UNIT\\|WE ACTED AS AGENT (?<shares>[\\d\\.,]+) (?<price>[\\d\\.,]+) \\((?<gross>[\\d,\\.\\-]+)\\) \\((?<fee>[\\d,\\.\\-]+)\\) \\((?<amount>[\\d,\\.\\-]+)\\) .*$") //
                                                        .assign((t, v) -> {
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setAmount(asAmount(v.get("amount")));

                                                            processFeeEntries(t, v, type);
                                                        }))

                        .wrap(BuySellEntryItem::new);
    }

    private void addDividendTransaction()
    {
        final var type = new DocumentType("04\\. ACTIVITY DETAILS", //
                        documentContext -> documentContext //
                                        .section("currency") //
                                        .match(".*Combined in (?<currency>[\\w]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();
        var firstRelevantLine = new Block(".* UNIT DIST .*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //
                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .section("date", "tickerSymbol", "shares", "amount", "recNote") //
                        .documentContext("currency") //

                        // @formatter:off
                        // Matches lines like:
                        // 01-07-2025 01-07-2025    .VEQT UNIT DIST      ON      29 SHS REC 12/30/24 PAY - - - - 20.69 - - - -
                        // @formatter:on
                        .match("^(?<date>\\d{2}-\\d{2}-\\d{4}) \\d{2}-\\d{2}-\\d{4}\\s+\\.(?<tickerSymbol>\\S+) UNIT DIST\\s+ON\\s+(?<shares>[\\d,\\.]+) SHS (?<recNote>REC \\d{2}/\\d{2}/\\d{2}) PAY [\\s\\-]+(?<amount>[\\d,\\.\\-]+).*$") //
                        .assign((t, v) -> {
                            v.put("tickerSymbol", asTickerSymbol(v.get("tickerSymbol")));

                            // date format is mm-dd-yyyy
                            t.setDateTime(asDate(v.get("date"), Locale.US));
                            t.setCurrencyCode(v.get("currency"));
                            t.setShares(asShares(v.get("shares")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("recNote"));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .wrap(TransactionItem::new);
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "CA");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "en", "CA");
    }

    private String asTickerSymbol(String value)
    {
        // If the exchange designator is missing,
        // assume Toronto Stock Exchange (.TO)
        if (!value.contains("."))
        {
            value = value.trim() + ".TO";
        }
        return value;
    }
}
