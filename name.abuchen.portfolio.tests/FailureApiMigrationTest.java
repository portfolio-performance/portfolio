package name.abuchen.portfolio.tests;

import org.junit.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.fail;

public class FailureApiMigrationTest
{
    @Test
    public void validateFailureApiMigration() throws Exception
    {
        // Scan the PDF importer production sources
        Path sourcePath = Path.of("../name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf");

        List<StructureError> errors = FailureApiUsageValidator.validatePackage(sourcePath);

        if (!errors.isEmpty())
        {
            StringBuilder message = new StringBuilder("\nDeprecated FAILURE API usage found\n\n");

            for (StructureError error : errors)
            {
                message.append(String.format("%s:%d -> %s%n", error.file(), error.line(), error.message()));
            }

            fail(message.toString());
        }
    }
}
