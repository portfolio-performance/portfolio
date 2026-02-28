package name.abuchen.portfolio.tests;

import org.junit.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.fail;

public class StructureTest
{
    @Test
    public void validateTestStructure() throws Exception
    {
        // Scan the PDF importer test sources
        Path packagePath = Path.of("src/name/abuchen/portfolio/datatransfer/pdf");

        List<StructureError> errors = AssertionStructureValidator.validatePackage(packagePath);

        if (!errors.isEmpty())
        {
            StringBuilder message = new StringBuilder("\nStructure errors found\n\n");

            for (StructureError error : errors)
            {
                message.append(String.format("%s:%d -> %s%n", error.file(), error.line(), error.message()));
            }

            fail(message.toString());
        }
    }
}
