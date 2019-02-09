package name.abuchen.portfolio.datatransfer.pdf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.model.Client;

public class PDFImportAssistant
{
    private final Client client;
    private final List<File> files;
    private final List<Extractor> extractors = new ArrayList<>();

    public PDFImportAssistant(Client client, List<File> files)
    {
        this.client = client;
        this.files = files;

        extractors.add(new BaaderBankPDFExtractor(client));
        extractors.add(new BankSLMPDFExtractor(client));
        extractors.add(new ComdirectPDFExtractor(client));
        extractors.add(new CommerzbankPDFExtractor(client));
        extractors.add(new ConsorsbankPDFExtractor(client));
        extractors.add(new DABPDFExtractor(client));
        extractors.add(new DegiroPDFExtractor(client));
        extractors.add(new DeutscheBankPDFExtractor(client));
        extractors.add(new DkbPDFExtractor(client));
        extractors.add(new FinTechGroupBankPDFExtractor(client));
        extractors.add(new INGDiBaExtractor(client));
        extractors.add(new OnvistaPDFExtractor(client));
        extractors.add(new SBrokerPDFExtractor(client));
        extractors.add(new UnicreditPDFExtractor(client));
        extractors.add(new HelloBankPDFExtractor(client));
    }

    public Map<Extractor, List<Item>> run(IProgressMonitor monitor, Map<File, List<Exception>> errors)
    {
        monitor.beginTask(Messages.PDFMsgExtracingFiles, files.size());

        List<PDFInputFile> inputFiles = files.stream().map(PDFInputFile::new).collect(Collectors.toList());

        Map<Extractor, List<Item>> itemsByExtractor = new HashMap<>();

        SecurityCache securityCache = new SecurityCache(client);

        for (PDFInputFile inputFile : inputFiles)
        {
            monitor.setTaskName(inputFile.getName());

            try
            {
                inputFile.convertPDFtoText();

                boolean extracted = false;

                List<Exception> warnings = new ArrayList<>();
                for (Extractor extractor : extractors)
                {
                    List<Item> items = extractor.extract(securityCache, inputFile, warnings);

                    if (!items.isEmpty())
                    {
                        extracted = true;
                        itemsByExtractor.computeIfAbsent(extractor, e -> new ArrayList<Item>()).addAll(items);
                        break;
                    }
                }

                if (!extracted)
                {
                    Predicate<? super Exception> isNotUnsupportedOperation = e -> !(e instanceof UnsupportedOperationException);
                    List<Exception> meaningfulExceptions = warnings.stream().filter(isNotUnsupportedOperation)
                                    .collect(Collectors.toList());

                    errors.put(inputFile.getFile(), meaningfulExceptions.isEmpty() ? warnings : meaningfulExceptions);
                }
            }
            catch (IOException e)
            {
                errors.computeIfAbsent(inputFile.getFile(), f -> new ArrayList<>()).add(e);
            }

            monitor.worked(1);
        }

        securityCache.addMissingSecurityItems(itemsByExtractor);

        return itemsByExtractor;
    }
}
