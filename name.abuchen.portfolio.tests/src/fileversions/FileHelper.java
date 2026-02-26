package fileversions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

public class FileHelper
{
    private static Path testDir = Paths.get(new File(
                    ReadingHistoricClientFilesTest.class.getProtectionDomain().getCodeSource().getLocation().getFile())
                                    .toURI());

    private FileHelper()
    {
    }

    @SuppressWarnings("nls")
    public static File find(String name) throws IOException
    {
        // depending on the environment (Eclipse, Maven, Infinitest) the files
        // can be located in different paths

        try (Stream<Path> fileWalker = Files.walk(testDir, 10))
        {
            Optional<Path> fullPath = fileWalker.filter(p -> p.getFileName().toString().equals(name)).findAny();

            if (fullPath.isPresent())
                return fullPath.get().toFile();
        }

        throw new IllegalArgumentException("entry with name '" + name + "' not found");
    }
}
