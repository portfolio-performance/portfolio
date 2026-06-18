package name.abuchen.portfolio.datatransfer;

import static name.abuchen.portfolio.util.CollectorsUtil.toMutableList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.datatransfer.Extractor.InputFile;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.model.Client;

public class TestExtractorHelper
{
    public static List<Item> runExtractor(Extractor extractor, Client client, List<InputFile> files,
                    List<Exception> errors)
    {
        var cache = new SecurityCache(client);

        var result = files.stream() //
                        .flatMap(f -> extractor.extract(cache, f, errors).stream()) //
                        .collect(toMutableList());

        extractor.postProcessing(result);

        Map<Extractor, List<Item>> map = new HashMap<>();
        map.put(extractor, result);
        cache.addMissingSecurityItems(map);

        return result;
    }

    /**
     * Loads a classpath resource alongside the given test class into a
     * temporary file and wraps it as an {@link InputFile} whose reported name
     * matches the original resource name.
     */
    public static InputFile loadResourceAsInputFile(Class<?> testClass, String resourceName) throws IOException
    {
        try (InputStream is = testClass.getResourceAsStream(resourceName))
        {
            if (is == null)
                throw new IOException("Test resource not found: " + resourceName); //$NON-NLS-1$

            var tempFile = Files.createTempFile("pp-test-", "-" + resourceName).toFile(); //$NON-NLS-1$ //$NON-NLS-2$
            tempFile.deleteOnExit();
            Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return new InputFile(tempFile)
            {
                @Override
                public String getName()
                {
                    return resourceName;
                }
            };
        }
    }

    /**
     * Convenience helper that loads a classpath resource, wraps it as an
     * {@link InputFile}, and runs the extractor against it.
     */
    public static List<Item> runExtractor(Extractor extractor, Client client, Class<?> testClass,
                    String resourceName, List<Exception> errors) throws IOException
    {
        var inputFile = loadResourceAsInputFile(testClass, resourceName);
        return runExtractor(extractor, client, List.of(inputFile), errors);
    }
}
