package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

/**
 * Importer for "Info Reports" produced by the Bison App.
 * <p/>
 * Bison only supports EUR as currency. Therefore the extractor is always
 * defaulting to EUR.
 */
@SuppressWarnings("nls")
public class BisonPDFExtractor extends AbstractPDFExtractor
{
    public BisonPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("BISON");

        addInfoReport();
    }

    @Override
    public String getLabel()
    {
        return "BISON";
    }

    private void addInfoReport()
    {
        DocumentType type = new DocumentType("Info-Report");
        this.addDocumentTyp(type);

        addPurchaseAndSale(type);
        addVoucher(type);
        addDepositAndRemoval(type);
    }

    private void addPurchaseAndSale(DocumentType type)
    {
        // block must match line exactly to avoid other "Kauf" notices that are
        // only tax relevant
        Block purchaseSale = new Block("^(Kauf|Verkauf)\\*? [A-Z]* [\\.,\\d]+$");
        type.addBlock(purchaseSale);

        purchaseSale.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("type", "tickerSymbol", "shares", "date", "time", "amount") //
                        .match("^(?<type>(Kauf|Verkauf))\\*? (?<tickerSymbol>[A-Z]*) (?<shares>[\\.,\\d]+)$") //
                        .match("^(?<date>\\d{2}\\.\\d{2}\\.\\d{4}) (?<time>\\d{2}:\\d{2}) .* (?<amount>[\\.,\\d]+) €$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);

                            v.put("currency", "EUR");
                            t.setCurrencyCode("EUR");

                            t.setSecurity(getOrCreateCryptoCurrency(v));

                            t.setShares(asShares(v.get("shares")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setDate(asDate(v.get("date"), v.get("time")));
                        })

                        .wrap(BuySellEntryItem::new));
    }

    private void addVoucher(DocumentType type)
    {
        Block voucher = new Block("^(Gutschein)\\*? [A-Z]* [\\.,\\d]+$");
        type.addBlock(voucher);

        voucher.set(new Transaction<PortfolioTransaction>()

                        .subject(() -> {
                            PortfolioTransaction t = new PortfolioTransaction();
                            t.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            return t;
                        })

                        .section("tickerSymbol", "shares", "date", "time", "amount") //
                        .match("^Gutschein\\*? (?<tickerSymbol>[A-Z]*) (?<shares>[\\.,\\d]+)$") //
                        .match("^(?<date>\\d{2}\\.\\d{2}\\.\\d{4}) (?<time>\\d{2}:\\d{2}) .* (?<amount>[\\.,\\d]+) €$") //
                        .assign((t, v) -> {
                            v.put("currency", "EUR");
                            t.setCurrencyCode("EUR");

                            t.setSecurity(getOrCreateCryptoCurrency(v));

                            t.setShares(asShares(v.get("shares")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                        })

                        .wrap(TransactionItem::new));
    }

    private void addDepositAndRemoval(DocumentType type)
    {
        Block depositRemoval = new Block("^(Einzahlung|Auszahlung)$");
        type.addBlock(depositRemoval);

        depositRemoval.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DEPOSIT);
                            return t;
                        })

                        .section("type", "date", "time", "amount") //
                        .match("^(?<type>(Einzahlung|Auszahlung))$") //
                        .match("^(?<date>\\d{2}\\.\\d{2}\\.\\d{4}) (?<time>\\d{2}:\\d{2}) .* (?<amount>[\\.,\\d]+) €$") //
                        .assign((t, v) -> {

                            if ("Auszahlung".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            v.put("currency", "EUR");
                            t.setCurrencyCode("EUR");
                            t.setAmount(asAmount(v.get("amount")));
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                        })

                        .wrap(TransactionItem::new));
    }
}
