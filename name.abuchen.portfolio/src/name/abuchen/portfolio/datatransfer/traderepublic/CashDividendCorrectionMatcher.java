package name.abuchen.portfolio.datatransfer.traderepublic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiPredicate;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

/**
 * Pairs cash-dividend correction rows with their matching original dividend
 * rows within a single Trade Republic import (which may span multiple CSV
 * files). Corrections are emitted by {@code TradeRepublicCSVExtractor} as
 * failure {@code DIVIDENDS} items whose {@code amount}, tax Unit, fee Unit, and
 * FX Unit carry their CSV signs (negative). Originals are stored with absolute
 * values. A correction only pairs with an original booked on or before it.
 * <p>
 * Matching runs in two passes so that an exact reversal always wins:
 * <ol>
 * <li>every signed component is the exact inverse — the pair is removed</li>
 * <li>everything but the tax is the exact inverse — Trade Republic sometimes
 * reverses a dividend with a different tax than it originally withheld. The
 * pair is removed and the remaining net is booked as tax (refund) so that the
 * cash balance is unchanged.</li>
 * </ol>
 * In both passes an original without FX data matches a correction with FX data
 * (older exports omit the FX columns on some dividends). Unmatched corrections
 * stay in place so the wizard surfaces the failure message to the user.
 */
final class CashDividendCorrectionMatcher
{
    private CashDividendCorrectionMatcher()
    {
    }

    static void reconcile(List<Item> items)
    {
        var originals = new ArrayList<Candidate>();
        var corrections = new ArrayList<Candidate>();

        for (var item : items)
        {
            if (!(item instanceof TransactionItem))
                continue;
            if (!(item.getSubject() instanceof AccountTransaction at))
                continue;
            if (at.getType() != AccountTransaction.Type.DIVIDENDS)
                continue;

            var candidate = Candidate.of(item, at);
            if (item.isFailure())
                corrections.add(candidate);
            else
                originals.add(candidate);
        }

        if (corrections.isEmpty() || originals.isEmpty())
            return;

        var originalsByKey = new HashMap<Key, ArrayList<Candidate>>();
        for (var o : originals)
            originalsByKey.computeIfAbsent(o.key, k -> new ArrayList<>()).add(o);
        for (var list : originalsByKey.values())
            list.sort(Comparator.comparing(c -> c.date));

        corrections.sort(Comparator.comparing(c -> c.date));

        var toRemove = new HashSet<Item>();
        var toAdd = new ArrayList<Item>();

        // pass 1: exact reversal
        for (var correction : corrections)
        {
            var match = findMatch(originalsByKey, correction, (o, c) -> o.tax == -c.tax);
            if (match == null)
                continue;

            toRemove.add(match.item);
            toRemove.add(correction.item);
        }

        // pass 2: reversal with a different tax
        for (var correction : corrections)
        {
            if (toRemove.contains(correction.item))
                continue;

            var match = findMatch(originalsByKey, correction, (o, c) -> true);
            if (match == null)
                continue;

            toRemove.add(match.item);
            toRemove.add(correction.item);
            var difference = taxDifference(match, correction);
            if (difference != null)
                toAdd.add(difference);
        }

        if (!toRemove.isEmpty())
            items.removeIf(toRemove::contains);
        items.addAll(toAdd);
    }

    private static Candidate findMatch(HashMap<Key, ArrayList<Candidate>> originalsByKey, Candidate correction,
                    BiPredicate<Candidate, Candidate> predicate)
    {
        var candidates = originalsByKey.get(correction.key.invert());
        if (candidates == null)
            return null;

        for (int ii = candidates.size() - 1; ii >= 0; ii--)
        {
            var c = candidates.get(ii);
            if (!c.date.isAfter(correction.date) && (c.fx == null || c.fx.equals(Fx.invert(correction.fx)))
                            && predicate.test(c, correction))
                return candidates.remove(ii);
        }
        return null;
    }

    private static Item taxDifference(Candidate original, Candidate correction)
    {
        // gross and fee cancel out, so the remaining net is the difference
        // between the tax refunded by the correction and the tax withheld on
        // the original
        var difference = original.tx.getAmount() + correction.tx.getAmount();
        if (difference == 0)
            return null;

        var t = new AccountTransaction();
        t.setType(difference > 0 ? AccountTransaction.Type.TAX_REFUND : AccountTransaction.Type.TAXES);
        t.setSecurity(correction.tx.getSecurity());
        t.setDateTime(correction.tx.getDateTime());
        t.setCurrencyCode(correction.tx.getCurrencyCode());
        t.setShares(correction.tx.getShares());
        t.setAmount(Math.abs(difference));
        t.setNote(correction.tx.getNote());
        t.setSource(correction.tx.getSource());

        // a security in a foreign currency requires a gross value unit. Use
        // the correction's rate because the original might lack FX data.
        var fx = correction.fx;
        if (fx != null && !fx.forexCurrency.equals(t.getCurrencyCode()))
        {
            var forex = BigDecimal.valueOf(t.getAmount()).divide(fx.rate, 0, RoundingMode.HALF_UP).longValue();
            t.addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of(t.getCurrencyCode(), t.getAmount()),
                            Money.of(fx.forexCurrency, forex), fx.rate));
        }

        return new TransactionItem(t);
    }

    private static final class Candidate
    {
        final Item item;
        final AccountTransaction tx;
        final LocalDateTime date;
        final Key key;
        final long tax;
        final Fx fx;

        private Candidate(Item item, AccountTransaction tx, Key key, long tax, Fx fx)
        {
            this.item = item;
            this.tx = tx;
            this.date = tx.getDateTime();
            this.key = key;
            this.tax = tax;
            this.fx = fx;
        }

        static Candidate of(Item item, AccountTransaction tx)
        {
            var fx = tx.getUnit(Unit.Type.GROSS_VALUE).map(Fx::of).orElse(null);
            var tax = unitAmount(tx, Unit.Type.TAX);
            var fee = unitAmount(tx, Unit.Type.FEE);
            var key = new Key(tx.getSecurity(), tx.getShares(), tx.getAmount() + tax + fee, fee);
            return new Candidate(item, tx, key, tax, fx);
        }

        private static long unitAmount(AccountTransaction tx, Unit.Type type)
        {
            return tx.getUnit(type).map(Unit::getAmount).map(Money::getAmount).orElse(0L);
        }
    }

    private record Key(Security security, long shares, long gross, long fee)
    {
        Key invert()
        {
            return new Key(security, shares, -gross, -fee);
        }
    }

    private record Fx(String accountCurrency, long amount, String forexCurrency, long forexAmount, BigDecimal rate)
    {
        static Fx of(Unit u)
        {
            return new Fx(u.getAmount().getCurrencyCode(), u.getAmount().getAmount(), u.getForex().getCurrencyCode(),
                            u.getForex().getAmount(), u.getExchangeRate());
        }

        static Fx invert(Fx fx)
        {
            return fx == null ? null
                            : new Fx(fx.accountCurrency, -fx.amount, fx.forexCurrency, -fx.forexAmount, fx.rate);
        }
    }
}
