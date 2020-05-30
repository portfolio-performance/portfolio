package name.abuchen.portfolio.datatransfer.pdf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
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
        extractors.add(new ViacPDFExtractor(client));
        extractors.add(new TargobankPDFExtractor(client));
        extractors.add(new TradeRepublicPDFExtractor(client));
        extractors.add(new PostfinancePDFExtractor(client));
        extractors.add(new SutorPDFExtractor(client));
        extractors.add(new SwissquotePDFExtractor(client));
        extractors.add(new DZBankPDFExtractor(client));

        extractors.add(new JSONPDFExtractor(client, "deutsche-bank-purchase.json")); //$NON-NLS-1$
        extractors.add(new JSONPDFExtractor(client, "deutsche-bank-sale.json")); //$NON-NLS-1$
        extractors.add(new JSONPDFExtractor(client, "ffb-purchase.json")); //$NON-NLS-1$
        extractors.add(new JSONPDFExtractor(client, "trade-republic-dividends.json")); //$NON-NLS-1$
        extractors.add(new JSONPDFExtractor(client, "trade-republic-investmentplan.json")); //$NON-NLS-1$
        extractors.add(new JSONPDFExtractor(client, "ebase.json")); //$NON-NLS-1$
        extractors.add(new JSONPDFExtractor(client, "postbank-purchase.json")); //$NON-NLS-1$
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

        // post processing
        itemsByExtractor.entrySet().stream() //
                        .collect(Collectors.toMap(Entry<Extractor, List<Item>>::getKey,
                                        e -> e.getKey().postProcessing(e.getValue())));

        securityCache.addMissingSecurityItems(itemsByExtractor);

        return itemsByExtractor;
    }

    public List<Item> runWithPlainText(File file, List<Exception> errors) throws FileNotFoundException
    {
        String extractedText = null;
        try (Scanner scanner = new Scanner(file, StandardCharsets.UTF_8.name()))
        {
            extractedText = scanner.useDelimiter("\\A").next(); //$NON-NLS-1$
        }
        PDFInputFile inputFile = new PDFInputFile(file, extractedText);

        SecurityCache securityCache = new SecurityCache(client);

        List<Item> items = null;
        for (Extractor extractor : extractors)
        {
            List<Exception> warnings = new ArrayList<>();
            items = extractor.extract(securityCache, inputFile, warnings);

            if (!items.isEmpty())
            {
                // we extracted items; remove all errors from all other
                // extractors that
                // did not find any transactions in this text
                errors.clear();
                errors.addAll(warnings);
                
                items = extractor.postProcessing(items);
                
                break;
            }

            errors.addAll(warnings);
        }

        if (items == null || items.isEmpty())
            return Collections.emptyList();
        
        Map<Extractor, List<Item>> itemsByExtractor = new HashMap<>();
        itemsByExtractor.put(extractors.get(0), items);
        securityCache.addMissingSecurityItems(itemsByExtractor);

        return items;
    }

}
