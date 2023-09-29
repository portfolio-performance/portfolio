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
        extractors.add(new BisonPDFExtractor(client));
        extractors.add(new BondoraCapitalPDFExtractor(client));
        extractors.add(new ComdirectPDFExtractor(client));
        extractors.add(new CommerzbankPDFExtractor(client));
        extractors.add(new CommSecPDFExtractor(client));
        extractors.add(new ConsorsbankPDFExtractor(client));
        extractors.add(new CreditSuisseAGPDFExtractor(client));
        extractors.add(new CrowdestorPDFExtractor(client));
        extractors.add(new DABPDFExtractor(client));
        extractors.add(new DADATBankenhausPDFExtractor(client));
        extractors.add(new DegiroPDFExtractor(client));
        extractors.add(new DekaBankPDFExtractor(client));
        extractors.add(new DeutscheBankPDFExtractor(client));
        extractors.add(new Direkt1822BankPDFExtractor(client));
        extractors.add(new DkbPDFExtractor(client));
        extractors.add(new DreiBankenEDVPDFExtractor(client));
        extractors.add(new DZBankGruppePDFExtractor(client));
        extractors.add(new EasyBankAGPDFExtractor(client));
        extractors.add(new EbasePDFExtractor(client));
        extractors.add(new ErsteBankPDFExtractor(client));
        extractors.add(new FILFondbankPDFExtractor(client));
        extractors.add(new FinTechGroupBankPDFExtractor(client));
        extractors.add(new GenoBrokerPDFExtractor(client));
        extractors.add(new HargreavesLansdownPlcExtractor(client));
        extractors.add(new HelloBankPDFExtractor(client));
        extractors.add(new INGDiBaPDFExtractor(client));
        extractors.add(new JustTradePDFExtractor(client));
        extractors.add(new KBCGroupNVPDFExtractor(client));
        extractors.add(new KeytradeBankPDFExtractor(client));
        extractors.add(new MLPBankingAGPDFExtractor(client));
        extractors.add(new NIBCBankPDFExtractor(client));
        extractors.add(new OldenburgischeLandesbankAGPDFExtractor(client));
        extractors.add(new LGTBankPDFExtractor(client));
        extractors.add(new LimeTradingCorpPDFExtractor(client));
        extractors.add(new MerkurPrivatBankPDFExtractor(client));
        extractors.add(new OnvistaPDFExtractor(client));
        extractors.add(new OpenBankSAPDFExtractor(client));
        extractors.add(new PictetCieGruppeSAPDFExtractor(client));
        extractors.add(new PostbankPDFExtractor(client));
        extractors.add(new PostfinancePDFExtractor(client));
        extractors.add(new QuirinBankAGPDFExtractor(client));
        extractors.add(new RaiffeisenBankgruppePDFExtractor(client));
        extractors.add(new RenaultBankDirektPDFExtractor(client));
        extractors.add(new RevolutLtdPDFExtractor(client));
        extractors.add(new SantanderConsumerBankPDFExtractor(client));
        extractors.add(new SberbankEuropeAGPDFExtractor(client));
        extractors.add(new SBrokerPDFExtractor(client));
        extractors.add(new ScorePriorityIncPDFExtractor(client));
        extractors.add(new SelfWealthPDFExtractor(client));
        extractors.add(new SimpelPDFExtractor(client));
        extractors.add(new SolarisbankAGPDFExtractor(client));
        extractors.add(new StakePDFExtractor(client));
        extractors.add(new SuresseDirektBankPDFExtractor(client));
        extractors.add(new SwissquotePDFExtractor(client));
        extractors.add(new TargobankPDFExtractor(client));
        extractors.add(new TigerBrokersPteLtdPDFExtractor(client));
        extractors.add(new TradeRepublicPDFExtractor(client));
        extractors.add(new UBSAGBankingAGPDFExtractor(client));
        extractors.add(new UnicreditPDFExtractor(client));
        extractors.add(new VanguardGroupEuropePDFExtractor(client));
        extractors.add(new VBankAGPDFExtractor(client));
        extractors.add(new WealthsimpleInvestmentsIncPDFExtractor(client));
        extractors.add(new WirBankPDFExtractor(client));
        extractors.add(new WeberbankPDFExtractor(client));
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

        return runWithInputFile(inputFile, errors);
    }

    public List<Item> runWithInputFile(PDFInputFile file, List<Exception> errors) throws FileNotFoundException
    {
        SecurityCache securityCache = new SecurityCache(client);

        List<Item> items = null;
        for (Extractor extractor : extractors)
        {
            List<Exception> warnings = new ArrayList<>();
            items = extractor.extract(securityCache, file, warnings);

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
