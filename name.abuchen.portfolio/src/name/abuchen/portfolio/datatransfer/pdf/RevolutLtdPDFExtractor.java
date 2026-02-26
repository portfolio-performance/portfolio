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
public class RevolutLtdPDFExtractor extends AbstractPDFExtractor
{
    public RevolutLtdPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Revolut Trading Ltd");
        addBankIdentifier("Revolut Securities Europe");

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
        final var type = new DocumentType("Order details");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Order details$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Sell" change from BUY to SELL
                        .section("type").optional() //
                        .match("^.* (?<type>(Sell|Buy)) .*$") //
                        .assign((t, v) -> {
                            if ("Sell".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // TSLA Tesla US88160R1014 Sell 2.1451261 $1,166.12 03 Nov 2021
                                        // Zu Gunsten Konto 12345004 Valuta: 12.05.2017 EUR 75,92
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("tickerSymbol", "name", "isin", "currency") //
                                                        .match("^(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Sell [\\.,\\d]+ (?<currency>\\p{Sc})[\\.,\\d]+ [\\d]{2} .* [\\d]{4}$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // SPPW SPDR MSCI World UCITS ETF (Acc) IE00BFY0GT14 Buy 2.61848651 €38.19 €100 06 Jan 2025
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("tickerSymbol", "name", "isin", "currency") //
                                                        .match("^(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Buy [\\.,\\d]+ (?<currency>\\p{Sc})[\\.,\\d]+ \\p{Sc}[\\.,\\d]+ [\\d]{2} .* [\\d]{4}$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // TSLA Tesla US88160R1014 Sell 2.1451261 $1,166.12 03 Nov 2021
                        // SPPW SPDR MSCI World UCITS ETF (Acc) IE00BFY0GT14 Buy 2.61848651 €38.19 €100 06 Jan 2025
                        // @formatter:on
                        .section("shares") //
                        .match("^[A-Z0-9]{3,4} .* [A-Z]{2}[A-Z0-9]{9}[0-9] (Sell|Buy) (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // TSLA Tesla US88160R1014 Sell 2.1451261 $1,166.12 03 Nov 2021
                        // SPPW SPDR MSCI World UCITS ETF (Acc) IE00BFY0GT14 Buy 2.61848651 €38.19 €100 06 Jan 2025
                        // @formatter:on
                        .section("date") //
                        .match("^[A-Z0-9]{3,4} .* [A-Z]{2}[A-Z0-9]{9}[0-9] (Sell|Buy) .* (?<date>[\\d]{2} .* [\\d]{4})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), Locale.UK)))

                        // @formatter:off
                        // TSLA Tesla US88160R1014 Sell 2.1451261 $1,166.12 03 Nov 2021
                        // SPPW SPDR MSCI World UCITS ETF (Acc) IE00BFY0GT14 Buy 2.61848651 €38.19 €100 06 Jan 2025
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^[A-Z0-9]{3,4} .* [A-Z]{2}[A-Z0-9]{9}[0-9] (Sell|Buy) .* (?<currency>\\p{Sc})(?<amount>[\\.,\\d]+) [\\d]{2} .* [\\d]{4}$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Account Statement");
        this.addDocumentTyp(type);

        // @formatter:off
        // Formatting:
        // Trade Date | Settle Date | Currency | Activity Type | Symbol - Description | Quantity | Price Amount
        // -------------------------------------
        // 07/08/2020 07/08/2020 USD CDEP Cash Disbursement - Wallet (USD) 460.85
        // 07/15/2020 07/15/2020 USD CDEP Cash Disbursement - Wallet (USD) 204.15
        // @formatter:on
        var blockDeposit = new Block("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\w]{3} .* Cash Disbursement \\- Wallet \\([\\w]{3}\\) [\\.,\\d]+$");
        type.addBlock(blockDeposit);
        blockDeposit.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
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
                        .section("currency", "fee").optional() //
                        .match("^Total Fee charged (?<currency>\\p{Sc})(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
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
