package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.util.Locale;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class RevolutLtdPDFExtractor extends AbstractPDFExtractor
{
    /***
     * Information:
     * The currency of Revolut Trading Ltd. is always USD.
     */

    public RevolutLtdPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Revolut Trading Ltd"); //$NON-NLS-1$

        addBuySellTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Revolut Trading Ltd."; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Order details");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });
        
        Block firstRelevantLine = new Block("^Order details$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Sell" change from BUY to SELL
                .section("type").optional()
                .match("^.* (?<type>Sell) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Sell"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // Symbol Company Type Quantity Price Execution time Execution venue
                // TSLA Tesla Sell 2 1,166.121 01 Nov 2021 15:51:47 GMT XOFF
                .section("tickerSymbol", "name", "isin", "shares", "date")
                .find("Symbol Company ISIN Type Quantity Price Settlement date")
                .match("^(?<tickerSymbol>.*) (?<name>.*) (?<isin>[\\w]{12}) Sell (?<shares>[\\.,\\d]+) \\p{Sc}[\\.,\\d]+ (?<date>[\\d]{2} .* [\\d]{4})$")
                .assign((t, v) -> {
                    v.put("currency", CurrencyUnit.USD);
                    t.setShares(asShares(v.get("shares")));
                    t.setDate(asDate(v.get("date"), Locale.UK));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // TSLA Tesla US88160R1014 Sell 2.1451261 $1,166.12 03 Nov 2021
                .section("amount")
                .find("Symbol Company ISIN Type Quantity Price Settlement date")
                .match("^.* [\\.,\\d]+ \\p{Sc}(?<amount>[\\.,\\d]+) [\\d]{2} .* [\\d]{4}$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(CurrencyUnit.USD);
                })

                .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    /***
     * Information:
     * We cannot import in the bank statement the purchases, 
     * sales, dividends, etc. because the amounts of price and 
     * number of shares are not correctly reported.
     */
    private void addAccountStatementTransaction()
    {
        DocumentType type = new DocumentType("Account Statement");
        this.addDocumentTyp(type);

        // @formatter:off
        // Formatting:
        // Trade Date | Settle Date | Currency | Activity Type | Symbol - Description | Quantity | Price Amount
        // -------------------------------------
        // 07/08/2020 07/08/2020 USD CDEP Cash Disbursement - Wallet (USD) 460.85
        // 07/15/2020 07/15/2020 USD CDEP Cash Disbursement - Wallet (USD) 204.15
        // @formatter:on
        Block blockDeposit = new Block("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\w]{3} .* Cash Disbursement \\- Wallet \\([\\w]{3}\\) [\\.,\\d]+$");
        type.addBlock(blockDeposit);
        blockDeposit.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.DEPOSIT);
                            return entry;
                        })

                        .section("date", "currency", "amount")
                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} (?<currency>[\\w]{3}) .* Cash Disbursement \\- Wallet \\([\\w]{3}\\) (?<amount>[\\.,\\d]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), Locale.UK));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Total Fee charged $0.02
                .section("fee").optional()
                .match("^Total Fee charged \\p{Sc}(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    v.put("currency", CurrencyUnit.USD);
                    processFeeEntries(t, v, type);
                });
    }

    @Override
    protected long asAmount(String value)
    {
        return PDFExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "UK");
    }

    @Override
    protected long asShares(String value)
    {
        return PDFExtractorUtils.convertToNumberLong(value, Values.Share, "en", "UK");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return PDFExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "en", "UK");
    }
}
