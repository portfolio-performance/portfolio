package name.abuchen.portfolio.datatransfer.pdf;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.json.JClient;
import name.abuchen.portfolio.json.JTransaction;
import name.abuchen.portfolio.model.Client;

@RunWith(Parameterized.class)
public class JSONPDFTestCase
{
    public static final String EXT_JSON = ".json"; //$NON-NLS-1$
    public static final String EXT_TXT = ".txt"; //$NON-NLS-1$

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> getFiles() throws IOException, URISyntaxException
    {
        Path testDir = Paths.get(JSONPDFTestCase.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Collection<Object[]> params = new ArrayList<>();

        // look up test cases in such a way that it works in the Eclipse IDE,
        // with Infinitest, and on the Maven command line

        try (Stream<Path> fileWalker = Files.walk(testDir, 10))
        {
            fileWalker.filter(p -> p.toString().endsWith(EXT_JSON)) //
                            .filter(p -> p.toString().contains(File.separatorChar + "classes" + File.separatorChar)) //$NON-NLS-1$
                            .map(p -> {
                                String s = p.toString();
                                return s.substring(0, s.length() - EXT_JSON.length());
                            }) //
                            .forEach(p -> {
                                String label = p.substring(p.indexOf( //
                                                "name" + File.separatorChar //$NON-NLS-1$
                                                                + "abuchen" + File.separatorChar //$NON-NLS-1$
                                                                + "portfolio")); //$NON-NLS-1$
                                params.add(new Object[] { label, p });
                            });
        }

        if (params.isEmpty())
            throw new IOException("Error: no JSON test cases found"); //$NON-NLS-1$

        return params;
    }

    private String label;
    private String path;

    public JSONPDFTestCase(String label, String path)
    {
        this.label = label;
        this.path = path;
    }

    @Test
    public void test() throws IOException
    {
        List<File> files = new ArrayList<>();
        files.add(new File(path + EXT_TXT));
        PDFImportAssistant assistant = new PDFImportAssistant(new Client(), files);

        List<Exception> errors = new ArrayList<>();
        List<Extractor.Item> result = assistant.runWithPlainText(new File(path + EXT_TXT), errors);

        assertThat(errors, empty());

        new AssertImportActions().check(result);

        List<JTransaction> actualTransactions = result.stream().filter(i -> i.getData() instanceof JTransaction)
                        .map(i -> (JTransaction) i.getData()).collect(Collectors.toList());

        List<JTransaction> expectedTransactions = JClient.from(this.path + EXT_JSON).getTransactions()
                        .collect(Collectors.toList());

        assertThat("# of transactions must match", actualTransactions.size(), is(expectedTransactions.size())); //$NON-NLS-1$

        for (JTransaction tx : actualTransactions)
        {
            boolean foundMatch = false;

            for (JTransaction candidate : new ArrayList<>(expectedTransactions))
            {
                if (candidate.toJson().equals(tx.toJson()))
                {
                    foundMatch = true;
                    expectedTransactions.remove(candidate);
                    break;
                }
            }

            if (!foundMatch)
            {
                StringBuilder message = new StringBuilder();
                message.append(label);
                message.append("\n\nTransaction not found:\n"); //$NON-NLS-1$
                message.append(tx.toJson());
                message.append("\n\nExpected one of:"); //$NON-NLS-1$
                expectedTransactions.forEach(t -> message.append("\n").append(t.toJson())); //$NON-NLS-1$

                assertTrue(message.toString(), false);
            }
        }
    }
}
