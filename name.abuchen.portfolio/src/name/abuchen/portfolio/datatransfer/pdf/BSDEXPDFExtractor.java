package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Map;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

/**
 * Importer for "Transaktionshistorie" reports produced by BSDEX.
 */
@SuppressWarnings("nls")
public class BSDEXPDFExtractor extends AbstractPDFExtractor
{
    public BSDEXPDFExtractor(Client client)
    {
        super(client);

        // there is no "real" bank identifier available in the document,
        // therefore use the column headers
        addBankIdentifier("ID Typ Ausführung eingehender eingehendes ausgehender ausgehendes");

        addBuyTransaction();
        addSellTransaction();
        addDepositAndWithdrawal();
    }

    @Override
    public String getLabel()
    {
        return "BSDEX";
    }

    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Transaktionshistorie");
        this.addDocumentTyp(type);

        Block transactionBlock = new Block("^[a-f0-9\\-]+ Kauf .*EUR$",
                        "^([a-f0-9\\-]+)? ?(\\d{2}\\:\\d{2}\\:\\d{2})$");
        type.addBlock(transactionBlock);

        transactionBlock.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry transaction = new BuySellEntry();
            transaction.setType(PortfolioTransaction.Type.BUY);
            return transaction;
        })
        // @formatter:off
            // d54eb916-79c9-4eb2-b0ed- Kauf 18.12.2024 0.001 BTC 99.0 EUR 0.2 EUR
            // 041dd43be6cc 04:14:42
            // d207c7f3-ebd2-416c-b451-34353cfcc23d Kauf 30.04.2024 0.0001283 BTC 7.18 EUR 0.01 EUR
            // 19:16:59
            // @formatter:on
                        .section("date", "shares", "tickerSymbol", "amount", "currency", "fee", "feeCurrency", "time")
                        .match("^([a-f0-9\\-]+) Kauf (?<date>\\d{2}\\.\\d{2}\\.\\d{4}) (?<shares>[\\d\\.,]+) (?<tickerSymbol>[A-Z]+) (?<amount>[\\d\\.,]+) (?<currency>[A-Z]{3}) (?<fee>[\\d\\.,]+) (?<feeCurrency>[A-Z]{3})$")
                        .match("^([a-f0-9\\-]+)? ?(?<time>\\d{2}\\:\\d{2}\\:\\d{2})").assign((t, v) -> {
                            // fix factor 1000 problem
                            v.put("fee", v.get("fee").replace(".", ","));
                            // set crypto name for correct matching
                            v.put("name", getCryptoName(v.get("tickerSymbol")));

                            // Set transaction details
                            t.setDate(asDate(v.get("date"), v.get("time")));
                            t.setShares(asShares(v.get("shares").replace(".", ",")));
                            t.setAmount(asAmount(v.get("amount").replace(".", ",")) + asAmount(v.get("fee")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setSecurity(getOrCreateCryptoCurrency(v));

                            processFeeEntries(t, v, type);
                        }).wrap(BuySellEntryItem::new));
    }

    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Transaktionshistorie");
        this.addDocumentTyp(type);

        Block transactionBlock = new Block("^[a-f0-9\\-]+ Verkauf .*EUR$",
                        "^([a-f0-9\\-]+)? ?(\\d{2}\\:\\d{2}\\:\\d{2})$");
        type.addBlock(transactionBlock);

        transactionBlock.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry transaction = new BuySellEntry();
            transaction.setType(PortfolioTransaction.Type.SELL);
            return transaction;
        })
        // @formatter:off
            // 750968db-6059-481b-9dd4- Verkauf 17.12.2024 37.8 EUR 15.0 XRP 0.08 EUR
            // f04544597f50 18:07:05
            // @formatter:on
                        .section("date", "shares", "tickerSymbol", "amount", "currency", "fee", "feeCurrency", "time")
                        .match("^([a-f0-9\\-]+) Verkauf (?<date>\\d{2}\\.\\d{2}\\.\\d{4}) (?<amount>[\\d\\.,]+) (?<currency>[A-Z]+) (?<shares>[\\d\\.,]+) (?<tickerSymbol>[A-Z]{3}) (?<fee>[\\d\\.,]+) (?<feeCurrency>[A-Z]{3})$")
                        .match("^([a-f0-9\\-]+)? ?(?<time>\\d{2}\\:\\d{2}\\:\\d{2})").assign((t, v) -> {
                            // fix factor 1000 problem
                            v.put("fee", v.get("fee").replace(".", ","));
                            // set crypto name for correct matching
                            v.put("name", getCryptoName(v.get("tickerSymbol")));

                            // Set transaction details
                            t.setDate(asDate(v.get("date"), v.get("time")));
                            t.setShares(asShares(v.get("shares").replace(".", ",")));
                            t.setAmount(asAmount(v.get("amount").replace(".", ",")) - asAmount(v.get("fee")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setSecurity(getOrCreateCryptoCurrency(v));

                            processFeeEntries(t, v, type);
                        }).wrap(BuySellEntryItem::new));
    }

    private void addDepositAndWithdrawal()
    {
        DocumentType type = new DocumentType("Transaktionshistorie");
        this.addDocumentTyp(type);

        Block depositWithdrawal = new Block(".*(Einzahlung|Auszahlung).*",
                        "^([a-f0-9\\-]+)? ?(\\d{2}\\:\\d{2}\\:\\d{2}) \\d \\d$");
        type.addBlock(depositWithdrawal);

        depositWithdrawal.set(new Transaction<AccountTransaction>().subject(() -> new AccountTransaction())

        // @formatter:off
            // f5f27cba-6e3a-4120-8d96-c6be8859ce6f Einzahlung 14.06.2024 25.5 EUR IBAN-A IBAN-B
            // 06:00:26 4 6
            // c2c3eea6-cbee-4352-b87a- Auszahlung 17.12.2024 113.66 EUR IBAN-A IBAN-B
            // 87557f1886c3 06:30:23 6 4
            // @formatter:on
                        .section("type", "date", "amount", "currency", "time")
                        .match("^([a-f0-9\\-]+) (?<type>Einzahlung|Auszahlung) (?<date>\\d{2}\\.\\d{2}\\.\\d{4}) (?<amount>[\\d\\.,]+) (?<currency>[A-Z]+) [A-Z0-9]+ [A-Z0-9]+$")
                        .match("^([a-f0-9\\-]+)? ?(?<time>\\d{2}\\:\\d{2}\\:\\d{2}) \\d \\d").assign((t, v) -> {
                            t.setType("Auszahlung".equals(v.get("type")) ? AccountTransaction.Type.REMOVAL
                                            : AccountTransaction.Type.DEPOSIT);
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount").replace(".", ",")));
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                        }).wrap(TransactionItem::new));
    }

    /**
     * Maps a ticker symbol to the cryptocurrency name using a predefined map.
     * This helps identifying the correct crypto since there are for example
     * several ones with ticker "BTC".
     * 
     * @param tickerSymbol
     *            The ticker symbol to look up (e.g., "BTC").
     * @return The name of the cryptocurrency, or null if the ticker symbol is
     *         not found.
     */
    private String getCryptoName(String tickerSymbol)
    {
        // Predefined map for ticker symbols to cryptocurrency names that can be
        // traded on BSDEX (as of April 2025)
        Map<String, String> tickerToName = Map.of(
                        "BTC", "Bitcoin",
                        "ETH", "Ethereum",
                        "XRP", "XRP",
                        "LTC", "Litecoin",
                        "BCH", "Bitcoin Cash",
                        "UNI", "Uniswap",
                        "Chainlink", "LINK",
                        "ADA", "Cardano",
                        "DOT", "Polkadot",
                        "SOL", "Solana"
        );

        if (tickerSymbol == null || tickerSymbol.isEmpty())
        {
            return null;
        }

        // Fetch the name from the map (or return null if not found)
        return tickerToName.getOrDefault(tickerSymbol.toUpperCase(), null);
    }
}
