package name.abuchen.portfolio.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

/**
 * The calculation slots come from a system property that nobody validates
 * before the server starts, so every value a user could type has to end in a
 * usable number.
 */
@SuppressWarnings("nls")
public class RestApiLimitsTest
{
    private static final int MANY_PROCESSORS = 16;

    @Test
    public void testWithoutThePropertyTheDefaultApplies()
    {
        assertThat(RestApiLimits.resolve(null, MANY_PROCESSORS, false), is(2));
        assertThat(RestApiLimits.resolve("", MANY_PROCESSORS, false), is(2));
        assertThat(RestApiLimits.resolve("  ", MANY_PROCESSORS, false), is(2));
    }

    @Test
    public void testAValueFromOneToEightIsTaken()
    {
        assertThat(RestApiLimits.resolve("1", MANY_PROCESSORS, false), is(1));
        assertThat(RestApiLimits.resolve("4", MANY_PROCESSORS, false), is(4));
        assertThat(RestApiLimits.resolve(" 8 ", MANY_PROCESSORS, false), is(8));
    }

    @Test
    public void testAValueOutsideTheRangeIsClamped()
    {
        assertThat(RestApiLimits.resolve("0", MANY_PROCESSORS, false), is(1));
        assertThat(RestApiLimits.resolve("-3", MANY_PROCESSORS, false), is(1));
        assertThat(RestApiLimits.resolve("9", MANY_PROCESSORS, false), is(8));
        assertThat(RestApiLimits.resolve("1000", MANY_PROCESSORS, false), is(8));
    }

    @Test
    public void testAValueThatIsNoNumberFallsBackToTheDefault()
    {
        assertThat(RestApiLimits.resolve("four", MANY_PROCESSORS, false), is(2));
        assertThat(RestApiLimits.resolve("2.5", MANY_PROCESSORS, false), is(2));
    }

    /** one processor stays free for the user interface */
    @Test
    public void testNeverMoreThanTheProcessorsLessOne()
    {
        assertThat(RestApiLimits.resolve("8", 4, false), is(3));
        assertThat(RestApiLimits.resolve(null, 2, false), is(1));
        assertThat(RestApiLimits.resolve("4", 1, false), is(1));
    }

    /** every running and every waiting calculation holds a request, and six more stay free */
    @Test
    public void testRequestsLeaveRoomForEveryCalculationAndTheReserve()
    {
        assertThat(RestApiLimits.calculationQueue(2), is(4));
        assertThat(RestApiLimits.maxConcurrentRequests(2), is(12));
        assertThat(RestApiLimits.maxConcurrentRequests(8), is(30));
    }
}
