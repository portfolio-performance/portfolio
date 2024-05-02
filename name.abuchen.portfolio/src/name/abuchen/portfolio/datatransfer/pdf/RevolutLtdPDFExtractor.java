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
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote Revolut Trading Ltd. is a dollar-based financial services company.
 *           The currency of Revolut Trading Ltd. is always USD.
 *
 * @implNote We cannot import in the bank statement the purchases, sales, dividends, etc.
 *           because the amounts of price and number of shares are not correctly reported.
 *
 * @implSpec The date format is Locale.UK
 * @formatter:on
 */

@SuppressWarnings("nls")
public class RevolutLtdPDFExtractor extends AbstractPDFExtractor
{
    public RevolutLtdPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Revolut Trading Ltd");

        addBuySellTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Revolut Trading Ltd";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Order details");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Order details$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Sell" change from BUY to SELL
                        .section("type").optional() //
                        .match("^.* (?<type>Sell) .*$") //
                        .assign((t, v) -> {
                            if ("Sell".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // TSLA Tesla US88160R1014 Sell 2.1451261 $1,166.12 03 Nov 2021
                        // @formatter:on
                        .section("tickerSymbol", "name", "isin") //
                        .match("^(?<tickerSymbol>[A-Z0-9]{3,4}) (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Sell [\\.,\\d]+ \\p{Sc}[\\.,\\d]+ [\\d]{2} .* [\\d]{4}$") //
                        .assign((t, v) -> {
                            v.put("currency", CurrencyUnit.USD);

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // TSLA Tesla US88160R1014 Sell 2.1451261 $1,166.12 03 Nov 2021
                        // @formatter:on
                        .section("shares") //
                        .match("^[A-Z0-9]{3,4} .* [A-Z]{2}[A-Z0-9]{9}[0-9] Sell (?<shares>[\\.,\\d]+) \\p{Sc}[\\.,\\d]+ [\\d]{2} .* [\\d]{4}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // TSLA Tesla US88160R1014 Sell 2.1451261 $1,166.12 03 Nov 2021
                        // @formatter:on
                        .section("date") //
                        .match("^[A-Z0-9]{3,4} .* [A-Z]{2}[A-Z0-9]{9}[0-9] Sell [\\.,\\d]+ \\p{Sc}[\\.,\\d]+ (?<date>[\\d]{2} .* [\\d]{4})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), Locale.UK)))

                        // @formatter:off
                        // TSLA Tesla US88160R1014 Sell 2.1451261 $1,166.12 03 Nov 2021
                        // @formatter:on
                        .section("amount") //
                        .match("^[A-Z0-9]{3,4} .* [A-Z]{2}[A-Z0-9]{9}[0-9] Sell [\\.,\\d]+ \\p{Sc}(?<amount>[\\.,\\d]+) [\\d]{2} .* [\\d]{4}$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(CurrencyUnit.USD);
                        })

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

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
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "currency", "amount") //
                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} (?<currency>[\\w]{3}) .* Cash Disbursement \\- Wallet \\([\\w]{3}\\) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), Locale.UK));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Total Fee charged $0.02
                        // @formatter:on
                        .section("fee").optional() //
                        .match("^Total Fee charged \\p{Sc}(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("currency", CurrencyUnit.USD);

                            processFeeEntries(t, v, type);
                        });
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "UK");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "en", "UK");
    }
}
