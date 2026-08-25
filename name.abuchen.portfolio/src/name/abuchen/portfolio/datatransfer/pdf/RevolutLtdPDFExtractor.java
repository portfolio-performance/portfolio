package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.replaceMultipleBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
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

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

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
        // @formatter:off
        // The "Account Statement" document type covers two layouts:
        // - the old Revolut Trading Ltd layout (US date format, "Cash Disbursement - Wallet")
        // - the Revolut Securities Europe UAB layout (English dates with time, one statement section per currency)
        //
        // In the Revolut Securities Europe UAB layout, transaction lines only
        // contain the ticker symbol. Name and ISIN are taken from the portfolio
        // breakdown table, which only lists positions still held at the end of
        // the reporting period. For securities sold before the period end, the
        // security is created with the ticker symbol only.
        // @formatter:on
        final var type = new DocumentType("Account Statement", //
                        (context, lines) -> {
                            // @formatter:off
                            // XDWI Xtrackers MSCI World Industrials UCITS ETF (Acc) IE00BM67HV82 0.27961238 €77.61 €21.70 2.97%
                            // WELK Amundi S&P Global Financials ESG Acc UCITS ETF IE000KYX7IP4 1.13596698 €19.56 €22.22 3.04%
                            // @formatter:on
                            var pSecurity = Pattern.compile("^(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) (?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) [\\.,\\d]+ (?:US)?\\p{Sc}[\\.,\\d]+ (?:US)?\\p{Sc}[\\.,\\d]+ [\\.,\\d]+%$");

                            // Create a helper to store the list of security items found in the document
                            var securityListHelper = new SecurityListHelper();
                            context.putType(securityListHelper);

                            for (var line : lines)
                            {
                                var m = pSecurity.matcher(line);
                                if (m.matches())
                                {
                                    var securityItem = new SecurityItem();
                                    securityItem.tickerSymbol = m.group("tickerSymbol");
                                    securityItem.name = m.group("name");
                                    securityItem.isin = m.group("isin");
                                    securityListHelper.items.add(securityItem);
                                }
                            }
                        });
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

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "currency", "amount") //
                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} (?<currency>[\\w]{3}) .* Cash Disbursement \\- Wallet \\([\\w]{3}\\) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), Locale.UK));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 07/06/2020 07/08/2020 USD BUY V - VISA INC    COM CL A - TRD V B 0.4 at 196.19 Principal. 0.4 196.19 (78.48)
        // 07/13/2020 07/15/2020 USD BUY NET - CLOUDFLARE INC   CL A COM - TRD NET B 1 at 40.59 Agency. 1 40.59 (41.78)
        // @formatter:on
        var blockBuy = new Block("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\w]{3} BUY .* \\- TRD .*$");
        type.addBlock(blockBuy);
        blockBuy.set(new Transaction<BuySellEntry>()

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        .section("date", "currency", "tickerSymbol", "name", "shares", "amount") //
                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} (?<currency>[\\w]{3}) BUY (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) \\- (?<name>.*) \\- TRD .* (?<shares>[\\.,\\d]+) [\\.,\\d]+ \\((?<amount>[\\.,\\d]+)\\)$") //
                        .assign((t, v) -> {
                            v.put("name", trim(replaceMultipleBlanks(v.get("name"))));

                            t.setSecurity(getOrCreateSecurity(v));
                            t.setDate(asDate(v.get("date"), Locale.UK));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(BuySellEntryItem::new));

        // @formatter:off
        // 12 May 2026 09:00:01 GMT EMBH Trade - Market 0.01925701 €65.95 Buy €1.27 €0.02 €0.01
        // 11 May 2026 13:45:00 GMT LYMS Trade - Market 0.0571061 €101.04 Sell €5.77 €0.02 €0.01
        // @formatter:on
        var buySellBlock = new Block("^[\\d]{2} [\\w]{3} [\\d]{4} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} GMT [A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})? Trade \\- Market [\\.,\\d]+ (?:US)?\\p{Sc}[\\.,\\d]+ (Buy|Sell) .*$");
        type.addBlock(buySellBlock);
        buySellBlock.set(new Transaction<BuySellEntry>()

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        .section("date", "time", "tickerSymbol", "shares", "type", "currency", "gross", "fee", "commission") //
                        .match("^(?<date>[\\d]{2} [\\w]{3} [\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) GMT (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) Trade \\- Market (?<shares>[\\.,\\d]+) (?:US)?\\p{Sc}[\\.,\\d]+ (?<type>(Buy|Sell)) (?:US)?(?<currency>\\p{Sc})(?<gross>[\\.,\\d]+) (?:US)?\\p{Sc}(?<fee>[\\.,\\d]+) (?:US)?\\p{Sc}(?<commission>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "Sell" change from BUY to SELL
                            // @formatter:on
                            if ("Sell".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);

                            var context = type.getCurrentContext();
                            var securityItem = context.getType(SecurityListHelper.class).get()
                                            .findItem(v.get("tickerSymbol"));

                            securityItem.ifPresent(s -> {
                                v.put("name", s.name);
                                v.put("isin", s.isin);
                            });

                            t.setSecurity(getOrCreateSecurity(v));
                            t.setDate(asDate(v.get("date"), v.get("time")));
                            t.setShares(asShares(v.get("shares")));

                            var gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                            var fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
                            var commission = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("commission")));

                            // @formatter:off
                            // The "Value" column is the gross value (quantity × price).
                            // The total amount is gross plus fees for purchases and gross minus fees for sales.
                            // @formatter:on
                            if (PortfolioTransaction.Type.SELL == t.getPortfolioTransaction().getType())
                                t.setMonetaryAmount(gross.subtract(fee).subtract(commission));
                            else
                                t.setMonetaryAmount(gross.add(fee).add(commission));

                            ExtractorUtils.checkAndSetFee(fee, t, context);
                            ExtractorUtils.checkAndSetFee(commission, t, context);
                        })

                        .wrap(BuySellEntryItem::new));

        // @formatter:off
        // 29 Nov 2024 17:47:56 GMT QDVY Dividend €0.05 €0 €0
        // @formatter:on
        var dividendBlock = new Block("^[\\d]{2} [\\w]{3} [\\d]{4} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} GMT [A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})? Dividend (?:US)?\\p{Sc}[\\.,\\d]+ .*$");
        type.addBlock(dividendBlock);
        dividendBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        .section("date", "time", "tickerSymbol", "currency", "amount") //
                        .match("^(?<date>[\\d]{2} [\\w]{3} [\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) GMT (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) Dividend (?:US)?(?<currency>\\p{Sc})(?<amount>[\\.,\\d]+) (?:US)?\\p{Sc}[\\.,\\d]+ (?:US)?\\p{Sc}[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            var context = type.getCurrentContext();
                            var securityItem = context.getType(SecurityListHelper.class).get()
                                            .findItem(v.get("tickerSymbol"));

                            securityItem.ifPresent(s -> {
                                v.put("name", s.name);
                                v.put("isin", s.isin);
                            });

                            t.setSecurity(getOrCreateSecurity(v));
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 29 Sep 2024 10:03:54 GMT Cash top-up €0.10 €0 €0
        // 07 May 2026 00:20:41 GMT Cash top-up €1 €0 €0
        // @formatter:on
        var topUpBlock = new Block("^[\\d]{2} [\\w]{3} [\\d]{4} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} GMT Cash top\\-up (?:US)?\\p{Sc}[\\.,\\d]+ .*$");
        type.addBlock(topUpBlock);
        topUpBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "time", "currency", "amount") //
                        .match("^(?<date>[\\d]{2} [\\w]{3} [\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) GMT Cash top\\-up (?:US)?(?<currency>\\p{Sc})(?<amount>[\\.,\\d]+) (?:US)?\\p{Sc}[\\.,\\d]+ (?:US)?\\p{Sc}[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 29 Oct 2024 00:54:48 GMT Robo management fee -€0.01 €0 €0
        // @formatter:on
        var feeBlock = new Block("^[\\d]{2} [\\w]{3} [\\d]{4} [\\d]{2}\\:[\\d]{2}\\:[\\d]{2} GMT Robo management fee \\-(?:US)?\\p{Sc}[\\.,\\d]+ .*$");
        type.addBlock(feeBlock);
        feeBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.FEES))

                        .section("date", "time", "currency", "amount") //
                        .match("^(?<date>[\\d]{2} [\\w]{3} [\\d]{4}) (?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2}) GMT Robo management fee \\-(?:US)?(?<currency>\\p{Sc})(?<amount>[\\.,\\d]+) (?:US)?\\p{Sc}[\\.,\\d]+ (?:US)?\\p{Sc}[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote("Robo management fee");
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

    private static class SecurityItem
    {
        String tickerSymbol;
        String name;
        String isin;

        @Override
        public String toString()
        {
            return "SecurityItem [tickerSymbol=" + tickerSymbol + ", name=" + name + ", isin=" + isin + "]";
        }
    }

    private static class SecurityListHelper
    {
        private final List<SecurityItem> items = new ArrayList<>();

        // Finds a SecurityItem in the list
        public Optional<SecurityItem> findItem(String tickerSymbol)
        {
            if (items.isEmpty())
                return Optional.empty();

            for (SecurityItem item : items) // NOSONAR
            {
                if (!item.tickerSymbol.equals(tickerSymbol))
                    continue;

                return Optional.of(item);
            }

            return Optional.empty();
        }
    }
}
