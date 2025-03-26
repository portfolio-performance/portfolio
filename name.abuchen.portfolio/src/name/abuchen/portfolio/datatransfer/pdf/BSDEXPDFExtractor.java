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
        addBuySellTransaction();
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

//    private void addPurchaseAndSale(DocumentType type)
//    {
//        System.out.println("Hello from addPurchaseAndSale");
//        Block purchaseSale = new Block("(Kauf|Verkauf) .* EUR$");
//        type.addBlock(purchaseSale);
//        System.out.println("BSDEXPDFExtractor - Added purchase/sale block.");
//
//        purchaseSale.set(new Transaction<BuySellEntry>()
//
//                        .subject(() -> new BuySellEntry())
//
//                        .section("type", "date", "time", "shares", "asset", "amount", "fee")
//                        .match("(?<type>Kauf|Verkauf) (?<date>\\d{2}\\.\\d{2}\\.\\d{4}) (?<sharesin>[\\d\\.,]+) (?<assetin>[A-Z]+) (?<amount>[\\d\\.,]+) (?<assetout>[A-Z]+) (?<fee>[\\d\\.,]+) EUR$")
//                        .assign((t, v) -> {
//                            System.out.println("BSDEXPDFExtractor - Matched transaction: " + v);
//                            t.setType("Verkauf".equals(v.get("type")) ? PortfolioTransaction.Type.SELL : PortfolioTransaction.Type.BUY);
//                            t.setCurrencyCode("EUR");
//                            t.setSecurity(getOrCreateCryptoCurrency(v));
//                            t.setShares(asShares(v.get("shares")));
//                            t.setAmount(asAmount(v.get("amount")));
////                            t.setFee(asAmount(v.get("fee"))); // Handle fee
//                            t.setDate(asDate(v.get("date"), v.get("time")));
//                        })
//                        .wrap(BuySellEntryItem::new));
//    }
    
    private void addBuySellTransaction()
    {
        System.out.println("Hello from addProcessedTransactionHistory");

        // Define the document type for BSDEX transaction history
        DocumentType type = new DocumentType("Transaktionshistorie");
        this.addDocumentTyp(type);

        // Define a transaction block to process "Kauf" and "Verkauf" entries
//        Block transactionBlock = new Block("^[a-f0-9\\-]+ (?<type>Kauf|Verkauf) .*EUR$", "^([a-f0-9\\-]+)? ?(?<time>\\d{2}\\:\\d{2}\\:\\d{2})( \\d)?( \\d)?$");
        Block transactionBlock = new Block("^[a-f0-9\\-]+ (?<type>Kauf|Verkauf) .*EUR$", "^([a-f0-9\\-]+)? ?(?<time>\\d{2}\\:\\d{2}\\:\\d{2})$");
        type.addBlock(transactionBlock);

        // Define transaction matching logic for "Kauf" and "Verkauf"
        transactionBlock.set(new Transaction<BuySellEntry>()
            .subject(() -> {
                BuySellEntry transaction = new BuySellEntry();
                transaction.setType(PortfolioTransaction.Type.BUY); // Default to BUY
                return transaction;
            })
         
            // @formatter:off
            // d207c7f3-ebd2-416c-b451-34353cfcc23d Kauf 30.04.2024 0.0001283 BTC 7.18 EUR 0.01 EUR
            // 19:16:59
            // @formatter:on
            .section("type", "date", "shares", "tickerSymbol", "amount", "currency", "fee", "currencyFee", "time")
            .match("^[a-f0-9\\-]+ (?<type>Kauf) (?<date>\\d{2}\\.\\d{2}\\.\\d{4}) (?<shares>[\\d\\.,]+) (?<tickerSymbol>[A-Z]+) (?<amount>[\\d\\.,]+) (?<currency>[A-Z]{3}) (?<fee>[\\d\\.,]+) (?<currencyFee>[A-Z]{3})$")
//            .match("^([a-f0-9\\-]+)? ?(?<time>\\d{2}\\:\\d{2}\\:\\d{2})( \\d)?( \\d)?")
            .match("^([a-f0-9\\-]+)? ?(?<time>\\d{2}\\:\\d{2}\\:\\d{2})")
            .assign((t, v) -> {
                System.out.println("Matched values: " + v);
                System.out.println("Keys available: " + v.keySet());
                System.out.println("Type: " + v.get("type"));
                System.out.println("Date: " + v.get("date"));
                System.out.println("time: " + v.get("time"));
                System.out.println("shares: " + v.get("shares"));
                System.out.println("tickerSymbol: " + v.get("tickerSymbol"));
                System.out.println("Amount: " + v.get("amount"));
                System.out.println("fee: " + v.get("fee"));
                System.out.println("currency: " + v.get("currency"));
                System.out.println("currencyFee: " + v.get("currencyFee"));
                
                v.put("fee", v.get("fee").replace(".", ",")); // fix factor 1000 problem
                
                // Set transaction details
                t.setType(PortfolioTransaction.Type.BUY);
                t.setDate(asDate(v.get("date"), v.get("time")));
                t.setShares(asShares(v.get("shares").replace(".", ",")));
                t.setAmount(asAmount(v.get("amount").replace(".", ",")) + asAmount(v.get("fee")));
                t.setSecurity(getOrCreateCryptoCurrency(v));
                t.setCurrencyCode("EUR");
                processFeeEntries(t, v, type);
            })
            
            // @formatter:off
            // 750968db-6059-481b-9dd4- Verkauf 17.12.2024 37.8 EUR 15.0 XRP 0.08 EUR
            // f04544597f50 18:07:05
            // @formatter:on
            .section("type", "date", "shares", "tickerSymbol", "amount", "currency", "fee", "currencyFee", "time")
            .match("^[a-f0-9\\-]+ (?<type>Verkauf) (?<date>\\d{2}\\.\\d{2}\\.\\d{4}) (?<shares>[\\d\\.,]+) (?<tickerSymbol>[A-Z]+) (?<amount>[\\d\\.,]+) (?<currency>[A-Z]{3}) (?<fee>[\\d\\.,]+) (?<currencyFee>[A-Z]{3})$")
//            .match("^([a-f0-9\\-]+)? ?(?<time>\\d{2}\\:\\d{2}\\:\\d{2})( \\d)?( \\d)?")
            .match("^([a-f0-9\\-]+)? ?(?<time>\\d{2}\\:\\d{2}\\:\\d{2})")
            .assign((t, v) -> {
                System.out.println("Matched values: " + v);
                System.out.println("Keys available: " + v.keySet());
                System.out.println("Type: " + v.get("type"));
                System.out.println("Date: " + v.get("date"));
                System.out.println("time: " + v.get("time"));
                System.out.println("shares: " + v.get("shares"));
                System.out.println("tickerSymbol: " + v.get("tickerSymbol"));
                System.out.println("Amount: " + v.get("amount"));
                System.out.println("fee: " + v.get("fee"));
                System.out.println("currency: " + v.get("currency"));
                System.out.println("currencyFee: " + v.get("currencyFee"));
                
                v.put("fee", v.get("fee").replace(".", ",")); // fix factor 1000 problem
                
                // Set transaction details
                t.setType(PortfolioTransaction.Type.SELL);
                t.setDate(asDate(v.get("date"), v.get("time")));
                t.setShares(asShares(v.get("shares").replace(".", ",")));
                t.setAmount(asAmount(v.get("amount").replace(".", ",")) + asAmount(v.get("fee")));
                t.setSecurity(getOrCreateCryptoCurrency(v));
                t.setCurrencyCode("EUR");
                processFeeEntries(t, v, type);
            })
            .wrap(BuySellEntryItem::new));
        
//        addFeesSectionsTransaction(pdfTransaction, type);
    }
    
//    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
//    {
//        transaction //
//
//                        // @formatter:off
//                        // Fremdkostenzuschlag -1,00 EUR
//                        // @formatter:on
//                        .section("fee", "currency").optional() //
//                        .match("^Fremdkostenzuschlag \\-(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
//                        .assign((t, v) -> {
//                            if (!type.getCurrentContext().getBoolean("negative"))
//                                processFeeEntries(t, v, type);
//                        })
//    }
    
//    private void addBuySellTransaction(DocumentType type)
//    {
//        Block hardcodedBlock = new Block(".*"); // Matches any line
//        type.addBlock(hardcodedBlock);
//
//        hardcodedBlock.set(new Transaction<BuySellEntry>()
//
//            .subject(() -> {
//                BuySellEntry entry = new BuySellEntry();
//                entry.setType(PortfolioTransaction.Type.BUY);
//                return entry;
//            })
//
//            // Define sections even though they're not used
//            .section("tickerSymbol", "shares", "date", "time", "amount")
//
//            // Dummy matchers (just placeholders, as we're hardcoding)
//            .match("^.*$") 
//
//            .assign((t, v) -> {
//                // Hardcoded values
//                t.setSecurity(getOrCreateSecurity(Map.of("tickerSymbol", "BTC", "currency", "EUR")));
//                t.setShares(asShares("0.01")); // Always 0.01 BTC
//                t.setAmount(asAmount("500.00")); // Always 500 EUR
//                t.setDate(asDate("20.03.2025", "12:00"));
//            })
//
//            .wrap(BuySellEntryItem::new)
//        );
//    }


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
    
//    @Override
//    public List<Item> extract(PDFInputFile pdf, List<Exception> errors)
//    {
//        try
//        {
//            System.out.println("BSDEXPDFExtractor - Starting extraction for file: " + pdf.getFileName());
//            System.out.println("BSDEXPDFExtractor - File content: " + pdf.getText());
//
//            if (!matchesDocument(pdf))
//            {
//                String errorMsg = "BSDEX: Datei '" + pdf.getFileName() + "' ist kein unterstütztes Dokument";
//                System.out.println("BSDEXPDFExtractor - Error: " + errorMsg);
//                throw new UnsupportedOperationException(errorMsg);
//            }
//
//            List<Item> items = super.extract(pdf, errors);
//            System.out.println("BSDEXPDFExtractor - Extraction completed. Items extracted: " + items.size());
//            return items;
//        }
//        catch (Exception e)
//        {
//            System.err.println("BSDEXPDFExtractor - Exception occurred: " + e.getMessage());
//            errors.add(e);
//            return new ArrayList<>();
//        }
//    }
}
