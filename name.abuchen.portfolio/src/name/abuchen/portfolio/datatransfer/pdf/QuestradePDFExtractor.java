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

        addDepositTransaction();
        addBuyTransaction();
        addDividendTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Questrade, Inc.";
    }

    private void addDepositTransaction()
    {
        final var type = new DocumentType("04\\. ACTIVITY DETAILS", 
                        documentContext -> documentContext.
                                        section("currency") //
                                        .match(".*Combined in (?<currency>[\\w]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        // Matches lines like:
        // 04-09-2025 04-09-2025 Contribution CONT 6263984218 - - - - 10,000.00 - - - -
        var depositBlock = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}\\-[\\d]{2}\\-[\\d]{4} Contribution .* [\\.,\\d]+.*$");
        type.addBlock(depositBlock);

        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount")
                        .documentContext("currency")
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) [\\d]{2}\\-[\\d]{2}\\-[\\d]{4} Contribution .* (?<amount>[\\.,\\d]+).*$")
                        .assign((t, v) -> {
                            // date format is mm-dd-yyyy
                            t.setDateTime(asDate(v.get("date"), Locale.US));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote("Contribution");
                        })

                        .wrap(TransactionItem::new));
    }

    private void addBuyTransaction()
    {
        final var type = new DocumentType("04\\. ACTIVITY DETAILS",
                        documentContext -> documentContext.
                                        section("currency") //
                                        .match(".*Combined in (?<currency>[\\w]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        // Matches lines like:
        // 04-10-2025 04-11-2025 Buy .VEQT VANGUARD ALL-EQUITY ETF  PORTFOLIO 
        // ETF UNIT  WE ACTED AS AGENT 50.0000 40.930 (2,046.50) - (2,046.50) - - - -
        var buyBlock = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}\\-[\\d]{2}\\-[\\d]{4} Buy .*");
        buyBlock.setMaxSize(2);
        type.addBlock(buyBlock);

        buyBlock.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            var tx = new BuySellEntry();
                            tx.setType(PortfolioTransaction.Type.BUY);
                            return tx;
                        })

                        .section("date", "tickerSymbol", "name", "shares", "gross")
                        .documentContext("currency")
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) [\\d]{2}\\-[\\d]{2}\\-[\\d]{4} Buy \\.(?<tickerSymbol>\\S+)\\s+(?<name>.*?)$")
                        .match("^.+ UNIT  WE ACTED AS AGENT (?<shares>[\\d\\.,]+) (?<price>[\\d\\.,]+) \\((?<gross>[\\d,\\.\\-]+)\\) .*$")
                        .assign((t, v) -> {
                            v.put("name", v.get("name").trim());
                            v.put("tickerSymbol", asTickerSymbol(v.get("tickerSymbol")));

                            t.setDate(asDate(v.get("date"), Locale.US));
                            t.setCurrencyCode(v.get("currency"));
                            t.setShares(asShares(v.get("shares"), "en", "CA"));
                            t.setAmount(asAmount(v.get("gross")));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .wrap(BuySellEntryItem::new));
    }

    private void addDividendTransaction()
    {
        final var type = new DocumentType("04\\. ACTIVITY DETAILS",
                        documentContext -> documentContext.
                                        section("currency") //
                                        .match(".*Combined in (?<currency>[\\w]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        // Matches lines like:
        // 01-07-2025 01-07-2025    .VEQT UNIT DIST      ON      29 SHS REC 12/30/24 PAY - - - - 20.69 - - - -
        var dividendBlock = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}\\-[\\d]{2}\\-[\\d]{4}\\s+\\.VEQT UNIT DIST\\s+ON.+");
        type.addBlock(dividendBlock);

        dividendBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .section("date", "tickerSymbol", "shares", "amount", "recNote")
                        .documentContext("currency")

                        .match("^(?<date>\\d{2}-\\d{2}-\\d{4}) \\d{2}-\\d{2}-\\d{4}\\s+\\.(?<tickerSymbol>\\S+) UNIT DIST\\s+ON\\s+(?<shares>[\\d,\\.]+) SHS (?<recNote>REC \\d{2}/\\d{2}/\\d{2}) PAY [\\s\\-]+(?<amount>[\\d,\\.\\-]+).*$")
                        .assign((t, v) -> {
                            v.put("tickerSymbol", asTickerSymbol(v.get("tickerSymbol")));

                            t.setDateTime(asDate(v.get("date"), Locale.US));
                            t.setCurrencyCode(v.get("currency"));
                            t.setShares(asShares(v.get("shares"), "en", "CA"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("recNote"));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .wrap(TransactionItem::new));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "CA");
    }

    private String asTickerSymbol(String value)
    {
        if (!value.contains("."))
        {
            value = value.trim() + ".TO";
        }
        return value;
    }
}
