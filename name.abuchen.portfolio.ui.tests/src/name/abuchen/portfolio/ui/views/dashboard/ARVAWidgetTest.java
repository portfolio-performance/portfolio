package name.abuchen.portfolio.ui.views.dashboard;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class ARVAWidgetTest
{
    @Test
    public void testArvaFactorWithThreePercentForSixtyYears()
    {
        assertThat(ARVAWidget.calculateArvaFactor(0.03, 60), closeTo(0.03613296, 0.00000001));
    }

    @Test
    public void testArvaFactorWithFourPercentForSixtyYears()
    {
        assertThat(ARVAWidget.calculateArvaFactor(0.04, 60), closeTo(0.04420185, 0.00000001));
    }

    @Test
    public void testArvaFactorWithZeroReturn()
    {
        assertThat(ARVAWidget.calculateArvaFactor(0.0, 60), closeTo(0.01666667, 0.00000001));
    }

    @Test
    public void testAnnualWithdrawal()
    {
        long withdrawal = ARVAWidget.calculateAnnualWithdrawal(1_000_000_00L, 0.03, 60,
                        ARVAWidget.PaymentTiming.END_OF_PERIOD);

        assertThat(withdrawal, is(36_132_96L));
    }

    @Test
    public void testAnnualWithdrawalWithReservedAmountAlreadySubtractedFromBasis()
    {
        long withdrawal = ARVAWidget.calculateAnnualWithdrawal(930_000_00L, 0.03, 60,
                        ARVAWidget.PaymentTiming.END_OF_PERIOD);

        assertThat(withdrawal, is(33_603_65L));
    }

    @Test
    public void testAnnualWithdrawalAtBeginningOfPeriod()
    {
        long withdrawal = ARVAWidget.calculateAnnualWithdrawal(1_000_000_00L, 0.03, 60,
                        ARVAWidget.PaymentTiming.BEGINNING_OF_PERIOD);

        assertThat(withdrawal, is(35_080_54L));
    }

    @Test
    public void testMonthlyWithdrawalAtEndOfPeriod()
    {
        long withdrawal = ARVAWidget.calculateMonthlyWithdrawal(1_000_000_00L, 0.03, 60,
                        ARVAWidget.PaymentTiming.END_OF_PERIOD);

        assertThat(withdrawal, is(2_970_45L));
    }

    @Test
    public void testMonthlyWithdrawalAtBeginningOfPeriod()
    {
        long withdrawal = ARVAWidget.calculateMonthlyWithdrawal(1_000_000_00L, 0.03, 60,
                        ARVAWidget.PaymentTiming.BEGINNING_OF_PERIOD);

        assertThat(withdrawal, is(2_963_15L));
    }

    @Test
    public void testMonthlyWithdrawalWithZeroReturn()
    {
        long withdrawal = ARVAWidget.calculateMonthlyWithdrawal(1_000_000_00L, 0.0, 60,
                        ARVAWidget.PaymentTiming.BEGINNING_OF_PERIOD);

        assertThat(withdrawal, is(1_388_89L));
    }

    @Test
    public void testMonthlyWithdrawalForSmallPortfolioIsLowerThanAnnualWithdrawalDividedByTwelve()
    {
        long annualWithdrawal = ARVAWidget.calculateAnnualWithdrawal(25_781_00L, 0.03, 60,
                        ARVAWidget.PaymentTiming.END_OF_PERIOD);
        long monthlyWithdrawal = ARVAWidget.calculateMonthlyWithdrawal(25_781_00L, 0.03, 60,
                        ARVAWidget.PaymentTiming.END_OF_PERIOD);

        assertThat(annualWithdrawal, is(931_54L));
        assertThat(monthlyWithdrawal, is(76_58L));
        assertThat(monthlyWithdrawal < Math.round(annualWithdrawal / 12.0), is(true));
    }

    @Test
    public void testWithdrawalRate()
    {
        assertThat(ARVAWidget.calculateWithdrawalRate(36_132_96L, 1_000_000_00L), closeTo(0.03613296, 0.00000001));
    }

    @Test
    public void testZeroBasisDoesNotThrow()
    {
        assertThat(ARVAWidget.calculateAnnualWithdrawal(0L, 0.03, 60, ARVAWidget.PaymentTiming.END_OF_PERIOD), is(0L));
        assertThat(ARVAWidget.calculateMonthlyWithdrawal(0L, 0.03, 60, ARVAWidget.PaymentTiming.END_OF_PERIOD), is(0L));
        assertThat(Double.isNaN(ARVAWidget.calculateWithdrawalRate(0L, 0L)), is(true));
    }

    @Test
    public void testInvalidRemainingYearsDoesNotThrow()
    {
        assertThat(ARVAWidget.calculateArvaFactor(0.03, 0), is(0.0));
        assertThat(ARVAWidget.calculateAnnualWithdrawal(1_000_000_00L, 0.03, 0,
                        ARVAWidget.PaymentTiming.END_OF_PERIOD), is(0L));
        assertThat(ARVAWidget.calculateMonthlyWithdrawal(1_000_000_00L, 0.03, 0,
                        ARVAWidget.PaymentTiming.END_OF_PERIOD), is(0L));
    }
}
