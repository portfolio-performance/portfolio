package name.abuchen.portfolio.util;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.Test;

/**
 * We can only test the fallback, since we cannot mock the static call to the
 * resource bundle *
 */
@SuppressWarnings("nls")
public class BuildInfoTest
{

    @Test
    public void testGetBuildTimeFallback()
    {
        LocalDate date = LocalDate.now();
        String today = DateTimeFormatter.ofPattern("MMM YYYY").format(date);
        String buildDate = DateTimeFormatter.ofPattern("MMM YYYY").format(BuildInfo.INSTANCE.getBuildTime());
        assertEquals(today, buildDate);
    }

}
