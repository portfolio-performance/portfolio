package name.abuchen.portfolio.snapshot.advancelumpsum;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.snapshot.advancelumpsum.BaseInterestRate.Source;

@SuppressWarnings("nls")
public class BaseInterestRateTest
{
    @Test
    public void testManualRateTakesPrecedence()
    {
        var rate = BaseInterestRate.resolve(2024, Optional.of(new BigDecimal("1.00"))).orElseThrow();

        assertThat(rate, is(new BaseInterestRate(2024, new BigDecimal("1.00"), Source.MANUAL)));
    }

    @Test
    public void testOfficialRateIsUsedWhenNoManualRateExists()
    {
        var rate = BaseInterestRate.resolve(2024, Optional.empty()).orElseThrow();

        assertThat(rate, is(new BaseInterestRate(2024, new BigDecimal("2.29"), Source.OFFICIAL)));
    }

    @Test
    public void testRateIsUnavailableWhenNoOfficialOrManualRateExists()
    {
        assertThat(BaseInterestRate.resolve(2099, Optional.empty()).isPresent(), is(false));
    }
}
