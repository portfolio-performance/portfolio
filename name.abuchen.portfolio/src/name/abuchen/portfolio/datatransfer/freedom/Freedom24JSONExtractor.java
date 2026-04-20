package name.abuchen.portfolio.datatransfer.freedom;

import static name.abuchen.portfolio.util.TextUtil.concatenate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.impl.YahooFinanceQuoteFeed;

/**
 * @formatter:off
 * @implNote https://freedom24.com/tradernet-api/broker-report
 *
 * @implSpec Imports Freedom24 (Freedom Finance Europe) broker report JSON files
 *           into Portfolio Performance.
 *
 *           Processed sections of the JSON report:
 *           - trades.detailed        → BuySellEntry (stock trades)
 *                                    → AccountTransferEntry (currency conversions, instr_type == 6)
 *           - cash_flows.detailed    → AccountTransaction (deposits / withdrawals)
 *           - commissions.detailed   → AccountTransaction (FEES, merged into trades when possible)
 *           - in_outs_securities     → PortfolioTransaction DELIVERY_INBOUND (promo / gift shares)
 *
 *           Key JSON fields per trade:
 *           ┌─────────────────┬──────────────────────────────────────────────────┐
 *           │ JSON field      │ meaning                                          │
 *           ├─────────────────┼──────────────────────────────────────────────────┤
 *           │ trade_id        │ unique trade identifier                          │
 *           │ order_id        │ order identifier                                 │
 *           │ date            │ execution timestamp  "yyyy-MM-dd HH:mm:ss"       │
 *           │ instr_nm        │ ticker symbol  (e.g. "VLO.US", "USD/EUR")        │
 *           │ instr_type      │ 1 = stock/equity, 6 = currency pair              │
 *           │ issue_nb        │ ISIN (empty / "-" for currencies)                │
 *           │ operation       │ "buy" / "sell"                                   │
 *           │ p               │ price per share                                  │
 *           │ curr_c          │ trading currency  ("USD", "EUR", …)              │
 *           │ q               │ quantity (positive)                              │
 *           │ summ            │ gross amount  = p × q  (always positive)         │
 *           │ commission      │ commission amount (in commission_currency)       │
 *           │ commission_currency │ commission currency (usually "EUR")          │
 *           └─────────────────┴──────────────────────────────────────────────────┘
 *
 *           Currency conversion trades (instr_type == 6):
 *           - instr_nm is like "USD/EUR"
 *           - q = quantity of base currency bought/sold
 *           - summ = counter-currency amount
 *           - p = exchange rate (base / counter)
 *
 * @formatter:on
 */
@SuppressWarnings("nls")
public class Freedom24JSONExtractor implements Extractor
{
    // -----------------------------------------------------------------------
    // Date/time format used by Freedom24
    // -----------------------------------------------------------------------
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // -----------------------------------------------------------------------
    // Instrument type constants
    // -----------------------------------------------------------------------
    /** Stock / equity */
    private static final int INSTR_TYPE_STOCK = 1;
    /** Currency pair */
    private static final int INSTR_TYPE_CURRENCY = 6;

    // -----------------------------------------------------------------------
    // Cash-flow type IDs that represent actual deposits / withdrawals
    // -----------------------------------------------------------------------
    private static final String CASHFLOW_BANK = "bank";

    private static final Set<String> STOCK_AWARD_TYPES = Set.of(
                    "stock_award",
                    "Geschenkaktionen",       // DE
                    "Free stocks",            // EN
                    "أسهم الهدايا",           // AR
                    "Подаръчни акции",        // BG
                    "Dárkové akcie",          // CS
                    "Gratis aktier",          // DA
                    "Δωρεάν μετοχές",         // EL
                    "Las acciones gratuitas", // ES
                    "Tasuta aktsiad",         // ET
                    "Actions cadeaux",        // FR
                    "Նվեր բաժնետոսեր",        // HY
                    "Azioni in regalo",       // IT
                    "Сыйлық акциялары",       // KK
                    "Dovanojamos akcijos",    // LT
                    "Gratis aandelen",        // NL
                    "Akcje prezentowe",       // PL
                    "Ações gratuitas",        // PT
                    "Acțiuni libere",         // RO
                    "Подарочные акции",       // RU
                    "Саҳмияҳои тӯҳфавӣ",      // TG
                    "Подарункові акції",      // UK
                    "赠送股票");               // ZH

    // Prefixes of interest-charge type strings; covers all Freedom24 UI languages.
    // Prefix matching handles the trailing currency code (EUR / USD / …).
    private static final Set<String> INTEREST_CHARGE_PREFIXES = Set.of(
                    "Zinsen",       // DE
                    "Interest",     // EN + ES ("Intereses" startsWith "Interest")
                    "الفائدة",      // AR
                    "Лихва",        // BG
                    "Úroky",        // CS
                    "Interesse",    // DA + IT (same prefix)
                    "Ποσοστά",      // EL
                    "Protsendid",   // ET
                    "Intérêt",      // FR
                    "Դրամային",     // HY
                    "Ақша",         // KK
                    "Palūkanos",    // LT
                    "Procenten",    // NL
                    "Odsetki",      // PL
                    "Juros",        // PT
                    "Dobândă",      // RO
                    "Проценты",     // RU
                    "Фоизҳо",       // TG
                    "Відсотки",     // UK
                    "资金使用利息"); // ZH

    // Prefixes of trade-linked commission types; these are skipped in
    // processRemainingCommissions() because they are already embedded as FEE
    // units inside the corresponding BuySellEntry.
    private static final Set<String> TRADE_COMMISSION_PREFIXES = Set.of(
                    "Für eine Transaktion:", // DE
                    "For trade:",            // EN
                    "لكل معاملة:",           // AR
                    "За сделка:",            // BG
                    "Za transakce:",         // CS
                    "Til handel:",           // DA
                    "Για τη συναλλαγή:",     // EL
                    "Por trato",             // ES (no colon in Freedom24 export)
                    "Tehingu eest:",         // ET
                    "Pour l'offre:",         // FR
                    "Ըստ գործարքի՝",         // HY
                    "Per operazione:",       // IT
                    "Мәміле үшін:",          // KK
                    "Už sandorį:",           // LT
                    "Per transactie:",       // NL
                    "Za transakcję:",        // PL
                    "Para transação:",       // PT
                    "Pentru tranzacționare:", // RO
                    "За сделку:",            // RU
                    "Барои муомила:",        // TG
                    "За угоду:",             // UK
                    "交易手续费：");           // ZH

    // Extracts the commission exchange rate from the Freedom24 trade comment, e.g.:
    // "Currency exchange of trade (USD) to a currency exchange of the service plan (EUR) is 0.8494"
    // Captured group 1 = commission-currency per trade-currency (e.g. 0.8494 EUR per USD)
    private static final Pattern FX_RATE_PATTERN = Pattern.compile(
                    "Currency exchange of trade \\(\\w+\\) to a currency exchange of the service plan \\(\\w+\\) is (\\d+(?:\\.\\d+)?)");

    private final Client client;
    private List<Security> allSecurities = new ArrayList<>();

    /** Pre-selected depot for the import wizard, or {@code null}. */
    private Portfolio portfolio = null;

    // -----------------------------------------------------------------------

    public Freedom24JSONExtractor(Client client)
    {
        this.client = client;
        allSecurities.addAll(client.getSecurities());
    }

    @Override
    public String getLabel()
    {
        return "Freedom24 (JSON)";
    }

    // -----------------------------------------------------------------------
    // Main entry point
    // -----------------------------------------------------------------------

    @Override
    public List<Item> extract(SecurityCache securityCache, InputFile inputFile, List<Exception> errors)
    {
        try (FileInputStream in = new FileInputStream(inputFile.getFile()))
        {
            return importReport(in, errors);
        }
        catch (IOException e)
        {
            errors.add(e);
            return Collections.emptyList();
        }
    }

    /**
     * Parses the Freedom24 JSON broker report and returns all extracted items.
     *
     * @param stream JSON input stream
     * @param errors collector for non-fatal parse errors
     * @return list of extracted PP items
     */
    /* package */ List<Item> importReport(InputStream stream, List<Exception> errors)
    {
        List<Item> results = new ArrayList<>();

        try
        {
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            // 1. Stock & currency trades
            processTradesDetailed(root, results, errors);

            // 2. Cash flows (deposits / withdrawals only; block/unblock pairs are
            //    accounting artefacts and must be skipped)
            processCashFlows(root, results, errors);

            // 3. Interest / other fees from the commissions section that could
            //    NOT be merged into a trade (e.g. "Zinsen für die Mittelverwendung")
            processRemainingCommissions(root, results, errors);

            // 4. Gift / promo securities (in_outs_securities with type "stock_award")
            processSecurityInOuts(root, results, errors);
        }
        catch (Exception e)
        {
            errors.add(e);
        }

        return results;
    }

    // -----------------------------------------------------------------------
    // Section processors
    // -----------------------------------------------------------------------

    /**
     * Processes {@code trades.detailed} array.
     * <p>
     * Each trade is either:
     * <ul>
     *   <li>a stock trade (instr_type == 1) → {@link BuySellEntry}</li>
     *   <li>a currency conversion (instr_type == 6) → skipped for now; TODO AccountTransferEntry</li>
     * </ul>
     * The commission is embedded directly into the trade as a {@link Unit}
     * of type FEE so no separate fee transaction is needed.
     */
    private void processTradesDetailed(JsonObject root, List<Item> results, List<Exception> errors)
    {
        JsonArray trades = getArray(root, "trades", "detailed");
        if (trades == null)
            return;

        for (JsonElement el : trades)
        {
            try
            {
                JsonObject trade = el.getAsJsonObject();
                int instrType = getInt(trade, "instr_type", 0);

                if (instrType == INSTR_TYPE_STOCK)
                    processStockTrade(trade, results);
                else if (instrType == INSTR_TYPE_CURRENCY)
                    processCurrencyTrade(trade, results);
                // other instrument types (bonds, futures, …) are ignored for now
            }
            catch (Exception e)
            {
                errors.add(e);
            }
        }
    }

    /**
     * Maps a single stock trade to a {@link BuySellEntry}.
     *
     * <p>Amount calculation:
     * <pre>
     *   gross  = q × p            (summ field)
     *   fees   = commission       (in commission_currency, usually EUR)
     *   net    = gross + fees     (buy: gross + fees, sell: gross − fees)
     * </pre>
     * PP convention: the monetary amount on a BuySellEntry is the *total*
     * account debit/credit including fees.  Fees are additionally stored as a
     * separate {@link Unit} so PP can derive the gross value.
     */
    private void processStockTrade(JsonObject trade, List<Item> results)
    {
        BuySellEntry entry = new BuySellEntry();

        // --- operation type ------------------------------------------------
        String op = getString(trade, "operation", "").toLowerCase();
        switch (op)
        {
            case "buy":
                entry.setType(PortfolioTransaction.Type.BUY);
                break;
            case "sell":
                entry.setType(PortfolioTransaction.Type.SELL);
                break;
            default:
                throw new IllegalArgumentException("Unknown operation type: " + op);
        }

        // --- date/time -----------------------------------------------------
        entry.setDate(parseDateTime(getString(trade, "date", "")));

        // --- security ------------------------------------------------------
        entry.setSecurity(getOrCreateSecurity(trade, results));

        // --- amount & currency ---------------------------------------------
        double grossAmount = getDouble(trade, "summ", 0.0);
        String tradeCurrency = asCurrencyCode(getString(trade, "curr_c", ""));

        // commission (usually in EUR, which may differ from the trade currency)
        double commissionAmt = getDouble(trade, "commission", 0.0);
        String commissionCurrency = asCurrencyCode(getString(trade, "commission_currency", tradeCurrency));

        // gross money in trading currency
        Money gross = Money.of(tradeCurrency, Values.Amount.factorize(grossAmount));
        Money commission = Money.of(commissionCurrency, Values.Amount.factorize(commissionAmt));

        // For the *account* side of the transaction we need EUR amounts.
        // Freedom24 always charges commissions in EUR (the account base currency).
        // The trade amount (summ) is in the trading currency (e.g. USD).
        // PP stores the portfolio-transaction amount in the trading currency,
        // the account-transaction amount in the account currency.
        //
        // Simple approach: set both sides to the gross amount in trading currency.
        // PP's fixGrossValueBuySell will detect the FEE unit and adjust totals.
        entry.setMonetaryAmount(gross);

        // --- shares --------------------------------------------------------
        long shares = Math.round(getDouble(trade, "q", 0.0) * Values.Share.factor());
        entry.setShares(shares);

        // --- fee -----------------------------------------------------------
        // PP requires FEE units to be in the same currency as the portfolio
        // transaction.  Freedom24 always charges commissions in EUR even when
        // the trade is in USD, so a cross-currency unit would be rejected.
        // In that case the commission is emitted as a separate FEES transaction.
        if (commissionAmt > 0.0)
        {
            if (commissionCurrency.equals(tradeCurrency))
            {
                entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, commission));
            }
            else
            {
                AccountTransaction feeTx = new AccountTransaction();
                feeTx.setType(AccountTransaction.Type.FEES);
                feeTx.setDateTime(entry.getPortfolioTransaction().getDateTime());
                feeTx.setMonetaryAmount(commission);
                feeTx.setSecurity(entry.getPortfolioTransaction().getSecurity());
                feeTx.setNote(buildTradeNote(trade));

                // PP requires a GROSS_VALUE unit when the fee transaction
                // references a security whose currency differs from the
                // account currency.  The exchange rate is embedded by
                // Freedom24 in the trade comment field.
                String tradeComment = getString(trade, "comment", "");
                Matcher m = FX_RATE_PATTERN.matcher(tradeComment);
                if (m.find())
                {
                    double commCcyPerTradeCcy = Double.parseDouble(m.group(1)); // e.g. 0.8494 EUR/USD
                    double feeInTradeCcy = commissionAmt / commCcyPerTradeCcy;
                    Money feeInTradeCurrency = Money.of(tradeCurrency,
                                    Values.Amount.factorize(feeInTradeCcy));
                    // PP validates: forex × rate = amount  →  USD × rate = EUR
                    // so rate = commissionCurrency per tradeCurrency (e.g. EUR/USD)
                    BigDecimal rate = BigDecimal.valueOf(commCcyPerTradeCcy)
                                    .setScale(10, RoundingMode.HALF_UP);
                    feeTx.addUnit(new Unit(Unit.Type.GROSS_VALUE, commission, feeInTradeCurrency, rate));
                }

                results.add(new TransactionItem(feeTx));
            }
        }

        // --- note (trade-id / order-id) ------------------------------------
        entry.setNote(buildTradeNote(trade));

        ExtractorUtils.fixGrossValueBuySell().accept(entry);

        results.add(new BuySellEntryItem(entry));
    }

    /**
     * Maps a Freedom24 currency conversion trade (instr_type == 6) to an
     * {@link AccountTransferEntry} between two accounts.
     *
     * <h3>Field semantics</h3>
     * <pre>
     *   instr_nm  "USD/EUR"   BASE / COUNTER
     *   curr_c    "EUR"       always the COUNTER currency
     *   p         0.86167     COUNTER per BASE  (EUR per USD)
     *   q         4641.83     BASE amount
     *   summ      3999.73     COUNTER amount  (≈ q × p)
     *
     *   operation "buy"  → buy BASE, pay COUNTER
     *                       source = COUNTER account (EUR out)
     *                       target = BASE account    (USD in)
     *
     *   operation "sell" → sell BASE, receive COUNTER
     *                       source = BASE account    (USD out)
     *                       target = COUNTER account (EUR in)
     * </pre>
     *
     * <p>The {@link Unit.Type#GROSS_VALUE} unit on the source transaction carries
     * the exchange rate.  PP validates {@code forex × rate = amount}, so:
     * <ul>
     *   <li>buy:  source=EUR, forex=USD → rate = EUR/USD = p</li>
     *   <li>sell: source=USD, forex=EUR → rate = USD/EUR = 1/p</li>
     * </ul>
     */
    private void processCurrencyTrade(JsonObject trade, List<Item> results)
    {
        String instrNm = getString(trade, "instr_nm", "");
        String[] parts = instrNm.split("/");
        if (parts.length != 2)
            return;

        String baseCurrency    = asCurrencyCode(parts[0].trim()); // e.g. USD
        String counterCurrency = asCurrencyCode(parts[1].trim()); // e.g. EUR

        double qBase    = Math.abs(getDouble(trade, "q",    0.0));
        double qCounter = Math.abs(getDouble(trade, "summ", 0.0));

        if (qBase == 0.0 || qCounter == 0.0)
            return;

        double p = getDouble(trade, "p", 0.0);
        if (p == 0.0)
            p = qCounter / qBase;

        String op = getString(trade, "operation", "").toLowerCase();

        final String   sourceCurrency;
        final String   targetCurrency;
        final long     sourceAmount;
        final long     targetAmount;
        final BigDecimal grossValueRate;

        switch (op)
        {
            case "buy":
                // pay COUNTER (EUR), receive BASE (USD)
                sourceCurrency = counterCurrency;
                targetCurrency = baseCurrency;
                sourceAmount   = Values.Amount.factorize(qCounter);
                targetAmount   = Values.Amount.factorize(qBase);
                // source=EUR, forex=USD → rate = EUR/USD = p
                grossValueRate = BigDecimal.valueOf(p).setScale(10, RoundingMode.HALF_UP);
                break;
            case "sell":
                // pay BASE (USD), receive COUNTER (EUR)
                sourceCurrency = baseCurrency;
                targetCurrency = counterCurrency;
                sourceAmount   = Values.Amount.factorize(qBase);
                targetAmount   = Values.Amount.factorize(qCounter);
                // source=USD, forex=EUR → rate = USD/EUR = 1/p
                grossValueRate = BigDecimal.valueOf(1.0 / p).setScale(10, RoundingMode.HALF_UP);
                break;
            default:
                return;
        }

        Money sourceMoney = Money.of(sourceCurrency, sourceAmount);
        Money targetMoney = Money.of(targetCurrency, targetAmount);

        AccountTransferEntry transfer = new AccountTransferEntry();
        transfer.setDate(parseDateTime(getString(trade, "date", "")));
        transfer.getSourceTransaction().setMonetaryAmount(sourceMoney);
        transfer.getTargetTransaction().setMonetaryAmount(targetMoney);

        transfer.getSourceTransaction().addUnit(
                        new Unit(Unit.Type.GROSS_VALUE, sourceMoney, targetMoney, grossValueRate));

        transfer.setNote(buildTradeNote(trade));

        results.add(new AccountTransferItem(transfer, true));

        // commission (FX trades at Freedom24 are typically free, but handle defensively)
        double commissionAmt = getDouble(trade, "commission", 0.0);
        if (commissionAmt > 0.0)
        {
            String commCcy = asCurrencyCode(getString(trade, "commission_currency", sourceCurrency));
            AccountTransaction feeTx = new AccountTransaction();
            feeTx.setType(AccountTransaction.Type.FEES);
            feeTx.setDateTime(transfer.getSourceTransaction().getDateTime());
            feeTx.setMonetaryAmount(Money.of(commCcy, Values.Amount.factorize(commissionAmt)));
            feeTx.setNote("FX commission: " + instrNm);
            results.add(new TransactionItem(feeTx));
        }
    }

    /**
     * Processes {@code cash_flows.detailed} and creates deposit / withdrawal
     * transactions.
     * <p>
     * Only entries with {@code type_id == "bank"} represent actual external
     * money movements.  All other types ("block", "unblock", "block_commission",
     * "unblock_commission") are internal Freedom24 accounting entries that must
     * be skipped to avoid double-counting.
     */
    private void processCashFlows(JsonObject root, List<Item> results, List<Exception> errors)
    {
        // Prefer cash_in_outs over cash_flows.detailed because it carries
        // individual transaction_ids and precise timestamps.
        JsonArray cashInOuts = getArray(root, "cash_in_outs");
        if (cashInOuts != null)
        {
            for (JsonElement el : cashInOuts)
            {
                try
                {
                    processCashInOut(el.getAsJsonObject(), results);
                }
                catch (Exception e)
                {
                    errors.add(e);
                }
            }
            return;
        }

        // Fallback: use cash_flows.detailed
        JsonArray flows = getArray(root, "cash_flows", "detailed");
        if (flows == null)
            return;

        for (JsonElement el : flows)
        {
            try
            {
                JsonObject flow = el.getAsJsonObject();
                String typeId = getString(flow, "type_id", "");
                if (!CASHFLOW_BANK.equals(typeId))
                    continue; // skip internal block/unblock entries

                processCashFlowEntry(flow, results);
            }
            catch (Exception e)
            {
                errors.add(e);
            }
        }
    }

    /**
     * Creates a deposit or withdrawal from a {@code cash_in_outs} entry.
     * Only entries with {@code type == "bank"} and no {@code offbalance} account
     * represent actual external money movements.
     */
    private void processCashInOut(JsonObject entry, List<Item> results)
    {
        String type = getString(entry, "type", "");
        if (!CASHFLOW_BANK.equals(type))
            return;

        // offbalance != null means this entry belongs to a non-trading sub-account
        if (!entry.get("offbalance").isJsonNull())
            return;

        double amount = getDouble(entry, "amount", 0.0);
        if (amount == 0.0)
            return;

        String currency = asCurrencyCode(getString(entry, "currency", ""));
        String dateStr = getString(entry, "datetime", getString(entry, "pay_d", ""));

        AccountTransaction tx = new AccountTransaction();
        tx.setDateTime(parseDateTime(dateStr));
        tx.setMonetaryAmount(Money.of(currency, Values.Amount.factorize(Math.abs(amount))));
        tx.setType(amount > 0.0 ? AccountTransaction.Type.DEPOSIT : AccountTransaction.Type.REMOVAL);

        String comment = getString(entry, "comment", "");
        String txId = getString(entry, "transaction_id", "");
        tx.setNote(concatenate(
            txId.isEmpty() ? null : "Transaction-ID: " + txId,
            comment.isEmpty() ? null : comment.trim(),
            " | "));

        results.add(new TransactionItem(tx));
    }

    /**
     * Creates a deposit or withdrawal from a {@code cash_flows.detailed} entry.
     */
    private void processCashFlowEntry(JsonObject flow, List<Item> results)
    {
        double amount = getDouble(flow, "amount", 0.0);
        if (amount == 0.0)
            return;

        String currency = asCurrencyCode(getString(flow, "currency", ""));
        String dateStr = getString(flow, "date", "");

        AccountTransaction tx = new AccountTransaction();
        tx.setDateTime(parseDateTime(dateStr));
        tx.setMonetaryAmount(Money.of(currency, Values.Amount.factorize(Math.abs(amount))));
        tx.setType(amount > 0.0 ? AccountTransaction.Type.DEPOSIT : AccountTransaction.Type.REMOVAL);

        String comment = getString(flow, "comment", "");
        if (!comment.isBlank())
            tx.setNote(comment.trim());

        results.add(new TransactionItem(tx));
    }

    /**
     * Processes {@code commissions.detailed} for entries that are NOT directly
     * linked to a trade (e.g. periodic interest charges for negative balances,
     * withdrawal fees).
     * <p>
     * Commissions linked to trades via their trade_id are already included as
     * {@link Unit} fee units inside the {@link BuySellEntry} – they must NOT be
     * created as separate fee transactions to avoid double-counting.
     * <p>
     * Freedom24 uses the {@code type} field to link commissions to trades:
     * "Für eine Transaktion:  &lt;trade_id&gt;".  All other commission types
     * (e.g. "Zinsen für die Mittelverwendung EUR") are emitted as stand-alone
     * fee transactions.
     */
    private void processRemainingCommissions(JsonObject root, List<Item> results, List<Exception> errors)
    {
        JsonArray commissions = getArray(root, "commissions", "detailed");
        if (commissions == null)
            return;

        for (JsonElement el : commissions)
        {
            try
            {
                JsonObject commission = el.getAsJsonObject();

                // Skip commissions that are already embedded in a trade
                String type = getString(commission, "type", "");
                if (TRADE_COMMISSION_PREFIXES.stream().anyMatch(type::startsWith))
                    continue;

                double sum = getDouble(commission, "sum", 0.0);
                if (sum == 0.0)
                    continue;

                String currency = asCurrencyCode(getString(commission, "currency", ""));
                String dateStr = getString(commission, "datetime", "");
                String comment = getString(commission, "comment", "");

                AccountTransaction tx = new AccountTransaction();
                tx.setDateTime(parseDateTime(dateStr));
                tx.setMonetaryAmount(Money.of(currency, Values.Amount.factorize(sum)));
                tx.setType(INTEREST_CHARGE_PREFIXES.stream().anyMatch(type::startsWith)
                                ? AccountTransaction.Type.INTEREST_CHARGE
                                : AccountTransaction.Type.FEES);

                String note = concatenate(type.isBlank() ? null : type.trim(),
                    comment.isBlank() ? null : comment.trim(), " | ");
                tx.setNote(note);

                results.add(new TransactionItem(tx));
            }
            catch (Exception e)
            {
                errors.add(e);
            }
        }
    }

    /**
     * Processes {@code securities_in_outs} (also called {@code in_outs_securities}).
     * <p>
     * Entries with {@code type == "stock_award"} / {@code "Geschenkaktionen"} represent
     * promotional / gift shares credited to the account at zero cost.  They are imported
     * as {@link PortfolioTransaction.Type#DELIVERY_INBOUND} with a zero monetary amount.
     * <p>
     * {@code securities_in_outs} carries rich metadata (transaction_id) but no ISIN.
     * {@code in_outs_securities.detailed} carries the ISIN but less metadata.
     * Both sections contain the same events and are joined via {@code ticker + datetime}
     * so that the ISIN is available for reliable security lookup.
     */
    private void processSecurityInOuts(JsonObject root, List<Item> results, List<Exception> errors)
    {
        // Build ISIN lookup from in_outs_securities.detailed (ticker|date -> isin)
        Map<String, String> isinByTickerDate = new HashMap<>();
        JsonArray isinSource = getArray(root, "in_outs_securities", "detailed");
        if (isinSource != null)
        {
            for (JsonElement el : isinSource)
            {
                JsonObject e = el.getAsJsonObject();
                String t = getString(e, "ticker", "");
                String d = getString(e, "date", "");
                String isin = getString(e, "isin", "");
                if (!t.isEmpty() && !d.isEmpty() && !isin.isEmpty())
                    isinByTickerDate.put(t + "|" + d, isin);
            }
        }

        // Primary source: securities_in_outs (has transaction_id, balance_currency)
        // Fallback: in_outs_securities.detailed (when the other key is absent)
        JsonArray inOuts = getArray(root, "securities_in_outs");
        if (inOuts == null)
            inOuts = isinSource;
        if (inOuts == null)
            return;

        for (JsonElement el : inOuts)
        {
            try
            {
                JsonObject entry = el.getAsJsonObject();

                String type = getString(entry, "type", "");
                if (!STOCK_AWARD_TYPES.contains(type))
                    continue;

                String ticker = getString(entry, "ticker", "");
                String dateStr = getString(entry, "datetime", getString(entry, "date", ""));
                // Prefer ISIN from the detailed section; fall back to whatever the entry provides
                String isin = isinByTickerDate.getOrDefault(ticker + "|" + dateStr,
                                getString(entry, "isin", ""));
                double qty = getDouble(entry, "quantity", 0.0);
                String comment = getString(entry, "comment", "");
                String currency = asCurrencyCode(getString(entry, "balance_currency", "USD"));

                Security security = getOrCreateSecurityByTickerIsin(ticker, isin, currency, results);

                PortfolioTransaction tx = new PortfolioTransaction();
                tx.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                tx.setDateTime(parseDateTime(dateStr));
                tx.setShares(Math.round(qty * Values.Share.factor()));
                tx.setSecurity(security);
                tx.setMonetaryAmount(Money.of(currency, 0));
                tx.setNote(comment.isBlank() ? null : comment.trim());

                TransactionItem txItem = new TransactionItem(tx);
                txItem.setPortfolioPrimary(portfolio);
                results.add(txItem);
            }
            catch (Exception e)
            {
                errors.add(e);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Security lookup / creation
    // -----------------------------------------------------------------------

    /**
     * Looks up or creates a security from a trade JSON object.
     * <p>
     * Matching priority (mirrors the IB extractor):
     * <ol>
     *   <li>Exact ISIN + currency match</li>
     *   <li>Exact ticker symbol match</li>
     *   <li>Create new security</li>
     * </ol>
     */
    private Security getOrCreateSecurity(JsonObject trade, List<Item> results)
    {
        String ticker = getString(trade, "instr_nm", "");
        String isin = getString(trade, "issue_nb", "");
        String currency = asCurrencyCode(getString(trade, "curr_c", ""));

        // Normalise "no ISIN" placeholders used by Freedom24
        if ("-".equals(isin) || isin.isBlank())
            isin = "";

        return getOrCreateSecurityByTickerIsin(ticker, isin, currency, results);
    }

    /**
     * Core security lookup / creation.
     *
     * @param ticker   Freedom24 ticker symbol (e.g. "VLO.US")
     * @param isin     ISIN or empty string
     * @param currency ISO currency code
     * @param results  item list to append a {@link SecurityItem} if a new
     *                 security is created
     */
    private Security getOrCreateSecurityByTickerIsin(String ticker, String isin, String currency,
                    List<Item> results)
    {
        // --- try to find an existing security ------------------------------
        // The stored tickerSymbol is in Yahoo format (e.g. "CLSK"), while the
        // Freedom24 ticker carries an exchange suffix (e.g. "CLSK.US").  Both
        // forms must be tried so that promo-share securities (no ISIN) are
        // recognised on re-import and PP duplicate detection works correctly.
        String yahooTicker = toYahooTicker(ticker);
        for (Security s : allSecurities)
        {
            if (!isin.isEmpty() && isin.equals(s.getIsin()) && currency.equals(s.getCurrencyCode()))
                return s;
            if (!ticker.isEmpty() && ticker.equals(s.getTickerSymbol()))
                return s;
            if (!yahooTicker.isEmpty() && !yahooTicker.equals(ticker) && yahooTicker.equals(s.getTickerSymbol()))
                return s;
        }

        // --- create new security -------------------------------------------
        // Freedom24 tickers end with ".US" for NYSE/NASDAQ, ".EU" etc.
        // Strip the exchange suffix for the Yahoo ticker so historical prices
        // can be fetched automatically.

        Security security = new Security(ticker, isin, yahooTicker, YahooFinanceQuoteFeed.ID);
        security.setCurrencyCode(currency);

        allSecurities.add(security);
        results.add(new SecurityItem(security));

        return security;
    }

    /**
     * Converts a Freedom24 ticker to a Yahoo Finance ticker where possible.
     * <p>
     * Freedom24 uses US-style tickers like "VLO.US", "SOFI.US" – for US stocks
     * Yahoo Finance uses the bare symbol without the ".US" suffix.
     * European tickers would need exchange-specific suffixes (not handled here).
     */
    private static String toYahooTicker(String ticker)
    {
        if (ticker == null || ticker.isEmpty())
            return ticker;
        // Remove ".US" suffix – US stocks on Yahoo have no suffix
        if (ticker.endsWith(".US"))
            return ticker.substring(0, ticker.length() - 3);
        return ticker;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Parses a Freedom24 date/time string.
     * Accepts full timestamp ("2026-01-09 10:52:19") as well as date-only
     * strings ("2026-01-09").
     */
    static LocalDateTime parseDateTime(String dateTime)
    {
        if (dateTime == null || dateTime.isBlank())
            return null;

        String s = dateTime.trim();

        // Full timestamp
        try
        {
            return LocalDateTime.parse(s, DATE_FORMAT);
        }
        catch (Exception ignore)
        {
            // fall through
        }

        // Date only – treat as start of day
        try
        {
            return LocalDateTime.parse(s + " 00:00:00", DATE_FORMAT);
        }
        catch (Exception ignore)
        {
            // fall through
        }

        return null;
    }

    /**
     * Builds a note string from the trade / order identifiers.
     */
    private static String buildTradeNote(JsonObject trade)
    {
        String tradeId = getString(trade, "trade_id", "");
        String orderId = getString(trade, "order_id", "");

        if (tradeId.isEmpty() && orderId.isEmpty())
            return null;

        if (orderId.isEmpty())
            return "Trade-ID: " + tradeId;
        if (tradeId.isEmpty())
            return "Order-ID: " + orderId;
        return "Trade/Order-ID: " + tradeId + " / " + orderId;
    }

    /** Converts a currency string to a valid ISO 4217 code. */
    protected String asCurrencyCode(String currency)
    {
        if (currency == null || currency.isBlank())
            return client.getBaseCurrency();
        CurrencyUnit unit = CurrencyUnit.getInstance(currency.trim());
        return unit == null ? client.getBaseCurrency() : unit.getCurrencyCode();
    }

    // -----------------------------------------------------------------------
    // JSON accessor utilities
    // -----------------------------------------------------------------------

    /** Returns a nested JSON array via dotted path segments, or null. */
    private static JsonArray getArray(JsonObject root, String... path)
    {
        JsonObject current = root;
        for (int i = 0; i < path.length - 1; i++)
        {
            JsonElement el = current.get(path[i]);
            if (el == null || !el.isJsonObject())
                return null;
            current = el.getAsJsonObject();
        }
        JsonElement last = current.get(path[path.length - 1]);
        if (last == null || !last.isJsonArray())
            return null;
        return last.getAsJsonArray();
    }

    private static String getString(JsonObject obj, String key, String defaultValue)
    {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull())
            return defaultValue;
        return el.getAsString();
    }

    private static double getDouble(JsonObject obj, String key, double defaultValue)
    {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull())
            return defaultValue;
        try
        {
            return el.getAsDouble();
        }
        catch (Exception e)
        {
            return defaultValue;
        }
    }

    private static int getInt(JsonObject obj, String key, int defaultValue)
    {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull())
            return defaultValue;
        try
        {
            return el.getAsInt();
        }
        catch (Exception e)
        {
            return defaultValue;
        }
    }
}