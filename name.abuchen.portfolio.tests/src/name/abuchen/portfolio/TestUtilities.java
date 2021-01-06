package name.abuchen.portfolio;

import java.nio.charset.StandardCharsets;
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
}
