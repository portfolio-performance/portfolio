package name.abuchen.portfolio.datatransfer.traderepublic;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
 * values. A correction pairs with an original when every signed component is
 * the exact inverse and the correction's booking date is on or after the
 * original's. Paired items are removed; unmatched corrections stay in place so
 * the wizard surfaces the failure message to the user.
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
        for (var correction : corrections)
        {
            var candidates = originalsByKey.get(correction.key.invert());
            if (candidates == null || candidates.isEmpty())
                continue;

            Candidate match = null;
            for (int ii = candidates.size() - 1; ii >= 0; ii--)
            {
                var c = candidates.get(ii);
                if (!c.date.isAfter(correction.date))
                {
                    match = c;
                    candidates.remove(ii);
                    break;
                }
            }
            if (match == null)
                continue;

            toRemove.add(match.item);
            toRemove.add(correction.item);
        }

        if (!toRemove.isEmpty())
            items.removeIf(toRemove::contains);
    }

    private static final class Candidate
    {
        final Item item;
        final LocalDateTime date;
        final Key key;

        private Candidate(Item item, LocalDateTime date, Key key)
        {
            this.item = item;
            this.date = date;
            this.key = key;
        }

        static Candidate of(Item item, AccountTransaction tx)
        {
            var fx = tx.getUnit(Unit.Type.GROSS_VALUE).map(Fx::of).orElse(null);
            var key = new Key(tx.getSecurity(), tx.getShares(), tx.getAmount(), unitAmount(tx, Unit.Type.TAX),
                            unitAmount(tx, Unit.Type.FEE), fx);
            return new Candidate(item, tx.getDateTime(), key);
        }

        private static long unitAmount(AccountTransaction tx, Unit.Type type)
        {
            return tx.getUnit(type).map(Unit::getAmount).map(Money::getAmount).orElse(0L);
        }
    }

    private record Key(Security security, long shares, long amount, long tax, long fee, Fx fx)
    {
        Key invert()
        {
            return new Key(security, shares, -amount, -tax, -fee, fx == null ? null : fx.invert());
        }
    }

    private record Fx(String accountCurrency, long amount, String forexCurrency, long forexAmount, BigDecimal rate)
    {
        static Fx of(Unit u)
        {
            return new Fx(u.getAmount().getCurrencyCode(), u.getAmount().getAmount(), u.getForex().getCurrencyCode(),
                            u.getForex().getAmount(), u.getExchangeRate());
        }

        Fx invert()
        {
            return new Fx(accountCurrency, -amount, forexCurrency, -forexAmount, rate);
        }
    }
}
