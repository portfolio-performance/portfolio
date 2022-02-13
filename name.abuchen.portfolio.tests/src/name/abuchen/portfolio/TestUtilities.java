package name.abuchen.portfolio;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;

public class TestUtilities
{
    private TestUtilities()
    {
    }

    public static String read(Class<?> clazz, String resource)
    {
        try (Scanner scanner = new Scanner(clazz.getResourceAsStream(resource), StandardCharsets.UTF_8.name()))
        {
            return scanner.useDelimiter("\\A").next(); //$NON-NLS-1$
        }
    }

    @SuppressWarnings("nls")
    public static Collection<Object[]> availableLanguages()
    {
        return Arrays.asList(new String[] { "de" }, new String[] { "en" }, new String[] { "es" }, new String[] { "pt" },
                        new String[] { "nl" }, new String[] { "fr" }, new String[] { "it" }, new String[] { "cs" },
                        new String[] { "ru" });
    }
}
