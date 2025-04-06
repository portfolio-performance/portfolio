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

        addBankIdentifier("ID Typ Ausführung eingehender eingehendes ausgehender ausgehendes");

//        addTransactionHistory();
        addBuyTransaction();
        addSellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "BSDEX";
    }

    private void addTransactionHistory()
    {
        DocumentType type = new DocumentType("Transaktionshistorie");
        this.addDocumentTyp(type);
        System.out.println("Hello from addTransactionHistory");

//        addPurchaseAndSale(type);
        addDepositAndWithdrawal(type);
//        addBuySellTransaction(type);
    }
    
    private void addBuyTransaction()
    {
        // Define the document type for BSDEX transaction history
        DocumentType type = new DocumentType("Transaktionshistorie");
        this.addDocumentTyp(type);

        // Define a transaction block to process "Kauf" and "Verkauf" entries
        Block transactionBlock = new Block("^[a-f0-9\\-]+ Kauf .*EUR$", "^([a-f0-9\\-]+)? ?(\\d{2}\\:\\d{2}\\:\\d{2})$");
        type.addBlock(transactionBlock);

        // Define transaction matching logic for "Kauf" and "Verkauf"
        transactionBlock.set(new Transaction<BuySellEntry>()
            .subject(() -> {
                BuySellEntry transaction = new BuySellEntry();
                transaction.setType(PortfolioTransaction.Type.BUY);
                return transaction;
            })
            // @formatter:off
            // d207c7f3-ebd2-416c-b451-34353cfcc23d Kauf 30.04.2024 0.0001283 BTC 7.18 EUR 0.01 EUR
            // 041dd43be6cc 04:14:42 6 4
            // @formatter:on
            .section("idA", "date", "shares", "tickerSymbol", "amount", "currency", "fee", "currencyFee", "idB", "time")
            .match("^(?<idA>[a-f0-9\\-]+) Kauf (?<date>\\d{2}\\.\\d{2}\\.\\d{4}) (?<shares>[\\d\\.,]+) (?<tickerSymbol>[A-Z]+) (?<amount>[\\d\\.,]+) (?<currency>[A-Z]{3}) (?<fee>[\\d\\.,]+) (?<currencyFee>[A-Z]{3})$")
            .match("^(?<idB>[a-f0-9\\-]+)? ?(?<time>\\d{2}\\:\\d{2}\\:\\d{2})")
            .assign((t, v) -> {
                System.out.println("Keys available: " + v.keySet());
                System.out.println("Date: " + v.get("date"));
                System.out.println("time: " + v.get("time"));
                System.out.println("shares: " + v.get("shares"));
                System.out.println("tickerSymbol: " + v.get("tickerSymbol"));
                System.out.println("Amount: " + v.get("amount"));
                System.out.println("fee: " + v.get("fee"));
                System.out.println("currency: " + v.get("currency"));
                System.out.println("currencyFee: " + v.get("currencyFee"));
                System.out.println("idA: " + v.get("idA"));
                System.out.println("idB: " + v.get("idB"));
                System.out.println("ID: " + v.get("idA") + v.get("idB"));
                
                v.put("fee", v.get("fee").replace(".", ",")); // fix factor 1000 problem
                v.put("name", getCryptoName(v.get("tickerSymbol")));
                System.out.println("name: " + v.get("name"));
                
                // Set transaction details
                t.setDate(asDate(v.get("date"), v.get("time")));
                t.setShares(asShares(v.get("shares").replace(".", ",")));
                t.setAmount(asAmount(v.get("amount").replace(".", ",")) + asAmount(v.get("fee")));
                t.setCurrencyCode(v.get("currency"));
                t.setSecurity(getOrCreateCryptoCurrency(v));
                t.setNote("ID: " + v.get("idA")+v.get("idB"));

                processFeeEntries(t, v, type);
            })
            .wrap(BuySellEntryItem::new));
    }
    
    private void addSellTransaction()
    {
        // Define the document type for BSDEX transaction history
        DocumentType type = new DocumentType("Transaktionshistorie");
        this.addDocumentTyp(type);

        // Define a transaction block to process "Kauf" and "Verkauf" entries
        Block transactionBlock = new Block("^[a-f0-9\\-]+ Verkauf .*EUR$", "^([a-f0-9\\-]+)? ?(\\d{2}\\:\\d{2}\\:\\d{2})$");
        type.addBlock(transactionBlock);

        // Define transaction matching logic for "Kauf" and "Verkauf"
        transactionBlock.set(new Transaction<BuySellEntry>()
            .subject(() -> {
                BuySellEntry transaction = new BuySellEntry();
                transaction.setType(PortfolioTransaction.Type.SELL);
                return transaction;
            })
            // @formatter:off
            // 750968db-6059-481b-9dd4- Verkauf 17.12.2024 37.8 EUR 15.0 XRP 0.08 EUR
            // f04544597f50 18:07:05
            // @formatter:on
            .section("idA", "date", "shares", "tickerSymbol", "amount", "currency", "fee", "currencyFee", "idB", "time")
            .match("^(?<idA>[a-f0-9\\-]+) Verkauf (?<date>\\d{2}\\.\\d{2}\\.\\d{4}) (?<amount>[\\d\\.,]+) (?<currency>[A-Z]+) (?<shares>[\\d\\.,]+) (?<tickerSymbol>[A-Z]{3}) (?<fee>[\\d\\.,]+) (?<currencyFee>[A-Z]{3})$")
            .match("^(?<idB>[a-f0-9\\-]+)? ?(?<time>\\d{2}\\:\\d{2}\\:\\d{2})")
            .assign((t, v) -> {
                System.out.println("Keys available: " + v.keySet());
                System.out.println("Date: " + v.get("date"));
                System.out.println("time: " + v.get("time"));
                System.out.println("shares: " + v.get("shares"));
                System.out.println("tickerSymbol: " + v.get("tickerSymbol"));
                System.out.println("Amount: " + v.get("amount"));
                System.out.println("fee: " + v.get("fee"));
                System.out.println("currency: " + v.get("currency"));
                System.out.println("currencyFee: " + v.get("currencyFee"));
                System.out.println("idA: " + v.get("idA"));
                System.out.println("idB: " + v.get("idB"));
                System.out.println("ID: " + v.get("idA") + v.get("idB"));
                
                v.put("fee", v.get("fee").replace(".", ",")); // fix factor 1000 problem
                v.put("name", getCryptoName(v.get("tickerSymbol")));
                System.out.println("name: " + v.get("name"));
                
                // Set transaction details
                t.setDate(asDate(v.get("date"), v.get("time")));
                t.setShares(asShares(v.get("shares").replace(".", ",")));
                t.setAmount(asAmount(v.get("amount").replace(".", ",")) - asAmount(v.get("fee")));
                t.setCurrencyCode(v.get("currency"));
                t.setSecurity(getOrCreateCryptoCurrency(v));
                t.setNote("ID: " + v.get("idA")+v.get("idB"));

                processFeeEntries(t, v, type);
            })
            .wrap(BuySellEntryItem::new));
    }

    private void addDepositAndWithdrawal(DocumentType type)
    {
        Block depositWithdrawal = new Block("(Einzahlung|Auszahlung) .* EUR$");
        type.addBlock(depositWithdrawal);

        depositWithdrawal.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction())

                        .section("type", "date", "time", "amount")
                        .match("(?<type>Einzahlung|Auszahlung) (?<date>\\d{2}\\.\\d{2}\\.\\d{4}) (?<time>\\d{2}:\\d{2}:\\d{2}) (?<amount>[\\d\\.,]+) EUR$")
                        .assign((t, v) -> {
                            t.setType("Auszahlung".equals(v.get("type")) ? AccountTransaction.Type.REMOVAL : AccountTransaction.Type.DEPOSIT);
                            t.setCurrencyCode("EUR");
                            t.setAmount(asAmount(v.get("amount")));
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                        })

                        .wrap(TransactionItem::new));
    }
    
    /**
     * Maps a ticker symbol to the cryptocurrency name using a predefined map.
     * @param tickerSymbol The ticker symbol to look up (e.g., "BTC").
     * @return The name of the cryptocurrency, or null if the ticker symbol is not found.
     */
    private String getCryptoName(String tickerSymbol)
    {
        // Predefined map for ticker symbols to cryptocurrency names
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

        System.out.println("getCryptoName: tickerSymbol: " + tickerSymbol);

        if (tickerSymbol == null || tickerSymbol.isEmpty())
        {
            return null; // Handle null or empty ticker symbols gracefully
        }

        // Fetch the name from the map (or return null if not found)
        return tickerToName.getOrDefault(tickerSymbol.toUpperCase(), null);
    }
}
