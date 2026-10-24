package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Random;
import java.util.TreeSet;

import org.junit.Test;

import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.util.Pair;

public class SecurityLatestTwoPricesTest
{
    /**
     * Reference implementation: the original algorithm working on a copy of
     * all prices including the latest price.
     */
    private static Optional<Pair<SecurityPrice, SecurityPrice>> reference(Security security)
    {
        var list = security.getPricesIncludingLatest();
        if (security.getPrices().isEmpty() || list.size() < 2)
            return Optional.empty();

        var now = LocalDate.now();

        var index = list.size() - 1;
        while (index >= 0)
        {
            if (!list.get(index).getDate().isAfter(now))
                break;
            index--;
        }

        return index > 0 ? Optional.of(new Pair<>(list.get(index), list.get(index - 1))) : Optional.empty();
    }

    @Test
    public void testSameResultAsReferenceImplementation()
    {
        var random = new Random(42);
        var now = LocalDate.now();

        for (int run = 0; run < 20_000; run++)
        {
            var security = new Security("S", CurrencyUnit.EUR); //$NON-NLS-1$

            // historic prices around today (incl. future dates)
            var dates = new TreeSet<LocalDate>();
            var count = random.nextInt(6);
            for (int ii = 0; ii < count; ii++)
                dates.add(now.plusDays(random.nextInt(9) - 6L));
            for (var date : dates)
                security.addPrice(new SecurityPrice(date, random.nextInt(5) + 1L));

            // latest price: none, same date as a historic price, or other date
            if (random.nextBoolean())
                security.setLatest(new LatestSecurityPrice(now.plusDays(random.nextInt(9) - 6L), //
                                random.nextInt(5) + 1L));

            var expected = reference(security);
            var actual = security.getLatestTwoSecurityPrices();

            assertThat(actual.isPresent(), is(expected.isPresent()));
            if (expected.isPresent())
            {
                assertThat(actual.get().getLeft(), is(sameInstance(expected.get().getLeft())));
                assertThat(actual.get().getRight(), is(sameInstance(expected.get().getRight())));
            }
        }
    }
}
