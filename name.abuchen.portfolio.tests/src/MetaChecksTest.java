package name.abuchen.portfolio.tests;

import org.junit.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.fail;

public class MetaChecksTest
{
    private static final Path PDF_IMPORTER_TEST_SOURCES =
            Path.of("src/name/abuchen/portfolio/datatransfer/pdf");

    private static final Path PDF_IMPORTER_PRODUCTION_SOURCES =
            Path.of("../name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf");

    @Test
    public void validateTestStructure() throws Exception
    {
        assertNoErrors(
                "Structure errors found",
                () -> AssertionStructureValidator.validatePackage(PDF_IMPORTER_TEST_SOURCES)
        );
    }

    @Test
    public void validateFailureApiMigration() throws Exception
    {
        assertNoErrors(
                "Deprecated FAILURE API usage found",
                () -> FailureApiUsageValidator.validatePackage(PDF_IMPORTER_PRODUCTION_SOURCES)
        );
    }

    @FunctionalInterface
    private interface Validation
    {
        List<StructureError> run() throws Exception;
    }

    private static void assertNoErrors(String headline, Validation validation) throws Exception
    {
        List<StructureError> errors = validation.run();

        if (errors == null || errors.isEmpty())
            return;

        StringBuilder message = new StringBuilder()
                .append("\n")
                .append(headline)
                .append("\n\n");

        for (StructureError error : errors)
        {
            message.append(String.format("%s:%d -> %s%n",
                    error.file(), error.line(), error.message()));
        }

        fail(message.toString());
    }
}
