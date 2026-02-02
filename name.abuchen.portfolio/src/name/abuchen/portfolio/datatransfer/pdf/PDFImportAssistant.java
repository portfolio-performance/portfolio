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
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.math.NegativeValue;
import name.abuchen.portfolio.model.Client;

public class PDFImportAssistant
{
    private final Client client;
    private final List<File> files;
    private final List<Extractor> extractors = new ArrayList<>();

    public PDFImportAssistant(NegativeValue negativeValue, Client client, List<File> files)
    {
        this.client = client;
        this.files = files;

        extractors.add(new ABNAMROGroupPDFExtractor(negativeValue, client));
        extractors.add(new AdvanziaBankPDFExtractor(negativeValue, client));
        extractors.add(new AJBellSecuritiesLimitedPDFExtractor(negativeValue, client));
        extractors.add(new AkfBankPDFExtractor(negativeValue, client));
        extractors.add(new ArkeaDirectBankPDFExtractor(negativeValue, client));
        extractors.add(new AudiBankPDFExtractor(negativeValue, client));
        extractors.add(new AustrianAnadiBankPDFExtractor(client));
        extractors.add(new ApoBankPDFExtractor(negativeValue, client));
        extractors.add(new AvivaPLCPDFExtractor(negativeValue, client));
        extractors.add(new BaaderBankPDFExtractor(negativeValue, client));
        extractors.add(new Bank11PDFExtractor(negativeValue, client));
        extractors.add(new BancoBilbaoVizcayaArgentariaPDFExtractor(negativeValue, client));
        extractors.add(new BankSLMPDFExtractor(negativeValue, client));
        extractors.add(new BarclaysBankIrelandPLCPDFExtractor(negativeValue, client));
        extractors.add(new BasellandschaftlicheKantonalbankPDFExtractor(negativeValue, client));
        extractors.add(new BawagAGPDFExtractor(negativeValue, client));
        extractors.add(new BigbankPDFExtractor(negativeValue, client));
        extractors.add(new BisonPDFExtractor(negativeValue, client));
        extractors.add(new BondoraCapitalPDFExtractor(negativeValue, client));
        extractors.add(new BourseDirectPDFExtractor(negativeValue, client));
        extractors.add(new BoursoBankPDFExtractor(negativeValue, client));
        extractors.add(new BSDEXPDFExtractor(negativeValue, client));
        extractors.add(new C24BankGmbHPDFExtractor(negativeValue, client));
        extractors.add(new CetesDirectoPDFExtractor(negativeValue, client));
        extractors.add(new ComdirectPDFExtractor(negativeValue, client));
        extractors.add(new CommerzbankPDFExtractor(negativeValue, client));
        extractors.add(new CommSecPDFExtractor(negativeValue, client));
        extractors.add(new ComputersharePDFExtractor(negativeValue, client));
        extractors.add(new ConsorsbankPDFExtractor(negativeValue, client));
        extractors.add(new CreditMutuelAllianceFederalePDFExtractor(negativeValue, client));
        extractors.add(new CreditSuisseAGPDFExtractor(negativeValue, client));
        extractors.add(new CrowdestorPDFExtractor(negativeValue, client));
        extractors.add(new DABPDFExtractor(negativeValue, client));
        extractors.add(new DADATBankenhausPDFExtractor(negativeValue, client));
        extractors.add(new DebitumInvestmentsPDFExtractor(negativeValue, client));
        extractors.add(new DegiroPDFExtractor(negativeValue, client));
        extractors.add(new DekaBankPDFExtractor(negativeValue, client));
        extractors.add(new DeutscheBankPDFExtractor(negativeValue, client));
        extractors.add(new DirectaSimPDFExtractor(negativeValue, client));
        extractors.add(new Direkt1822BankPDFExtractor(negativeValue, client));
        extractors.add(new DkbPDFExtractor(negativeValue, client));
        extractors.add(new DreiBankenEDVPDFExtractor(negativeValue, client));
        extractors.add(new DZBankGruppePDFExtractor(negativeValue, client));
        extractors.add(new EasyBankAGPDFExtractor(negativeValue, client));
        extractors.add(new EbasePDFExtractor(negativeValue, client));
        extractors.add(new ErsteBankPDFExtractor(negativeValue, client));
        extractors.add(new EstateGuruPDFExtractor(negativeValue, client));
        extractors.add(new ETradePDFExtractor(negativeValue, client));
        extractors.add(new FidelityInternationalPDFExtractor(negativeValue, client));
        extractors.add(new FILFondbankPDFExtractor(negativeValue, client));
        extractors.add(new FindependentAGPDFExtractor(negativeValue, client));
        extractors.add(new FinTechGroupBankPDFExtractor(negativeValue, client));
        extractors.add(new FirstradeSecuritiesIncPDFExtractor(negativeValue, client));
        extractors.add(new FordMoneyPDFExtractor(negativeValue, client));
        extractors.add(new GenoBrokerPDFExtractor(negativeValue, client));
        extractors.add(new GinmonPDFExtractor(negativeValue, client));
        extractors.add(new GladbacherBankAGPDFExtractor(negativeValue, client));
        extractors.add(new HargreavesLansdownPlcExtractor(negativeValue, client));
        extractors.add(new HelloBankPDFExtractor(negativeValue, client));
        extractors.add(new HypothekarbankLenzburgAGPDFExtractor(negativeValue, client));
        extractors.add(new INGDiBaPDFExtractor(client));
        extractors.add(new JTDirektbankPDFExtractor(client));
        extractors.add(new KBCGroupNVPDFExtractor(client));
        extractors.add(new KeytradeBankPDFExtractor(client));
        extractors.add(new KFintechPDFExtractor(client));
        extractors.add(new MerkurPrivatBankPDFExtractor(client));
        extractors.add(new MeDirectBankPlcPDFExtractor(client));
        extractors.add(new MLPBankingAGPDFExtractor(client));
        extractors.add(new ModenaEstoniaPDFExtractor(client));
        extractors.add(new N26BankAGPDFExtractor(client));
        extractors.add(new NIBCBankPDFExtractor(client));
        extractors.add(new NordaxBankABPDFExtractor(client));
        extractors.add(new NorddeutscheLandesbankPDFExtractor(client));
        extractors.add(new OldenburgischeLandesbankAGPDFExtractor(client));
        extractors.add(new LGTBankPDFExtractor(client));
        extractors.add(new LiechtensteinischeLandesbankAGPDFExtractor(client));
        extractors.add(new LimeTradingCorpPDFExtractor(client));
        extractors.add(new OnvistaPDFExtractor(client));
        extractors.add(new OpenBankSAPDFExtractor(client));
        extractors.add(new OrangeBankPDFExtractor(client));
        extractors.add(new PictetCieGruppeSAPDFExtractor(client));
        extractors.add(new PostbankPDFExtractor(client));
        extractors.add(new PostfinancePDFExtractor(client));
        extractors.add(new QuestradeGroupPDFExtractor(client));
        extractors.add(new QuirinBankAGPDFExtractor(client));
        extractors.add(new RaiffeisenBankgruppePDFExtractor(client));
        extractors.add(new RaisinBankAGPDFExtractor(client));
        extractors.add(new RenaultBankDirektPDFExtractor(client));
        extractors.add(new RevolutLtdPDFExtractor(client));
        extractors.add(new SantanderConsumerBankPDFExtractor(client));
        extractors.add(new SaxoBankPDFExtractor(client));
        extractors.add(new SberbankEuropeAGPDFExtractor(client));
        extractors.add(new SBrokerPDFExtractor(client));
        extractors.add(new ScalableCapitalPDFExtractor(client));
        extractors.add(new SchelhammerCapitalBankAG(client));
        extractors.add(new ScorePriorityIncPDFExtractor(client));
        extractors.add(new SelfWealthPDFExtractor(client));
        extractors.add(new SimpelPDFExtractor(client));
        extractors.add(new SolarisbankAGPDFExtractor(client));
        extractors.add(new StakeshopPtyLtdPDFExtractor(client));
        extractors.add(new SunrisePDFExtractor(client));
        extractors.add(new SuresseDirektBankPDFExtractor(client));
        extractors.add(new SutorBankGmbHPDFExtractor(client));
        extractors.add(new SwissquotePDFExtractor(client));
        extractors.add(new SydbankASPDFExtractor(client));
        extractors.add(new TargobankPDFExtractor(client));
        extractors.add(new TigerBrokersPteLtdPDFExtractor(client));
        extractors.add(new TradegateAGPDFExtractor(client));
        extractors.add(new TradeRepublicPDFExtractor(client));
        extractors.add(new UBSAGBankingAGPDFExtractor(client));
        extractors.add(new UmweltbankAGPDFExtractor(client));
        extractors.add(new UnicreditPDFExtractor(client));
        extractors.add(new VanguardGroupEuropePDFExtractor(client));
        extractors.add(new VBankAGPDFExtractor(negativeValue, client));
        extractors.add(new VDKBankNVPDFExtractor(negativeValue, client));
        extractors.add(new VolkswagenBankPDFExtractor(negativeValue, client));
        extractors.add(new VZVermoegenszentrumAGPDFExtractor(negativeValue, client));
        extractors.add(new WealthsimpleInvestmentsIncPDFExtractor(negativeValue, client));
        extractors.add(new WirBankPDFExtractor(negativeValue, client));
        extractors.add(new WitheBoxGmbHPDFExtractor(negativeValue, client));
        extractors.add(new WeberbankPDFExtractor(negativeValue, client));
        extractors.add(new ZuercherKantonalbankPDFExtractor(negativeValue, client));
    }

    public Map<Extractor, List<Item>> run(IProgressMonitor monitor, Map<File, List<Exception>> errors)
    {
        monitor.beginTask(Messages.PDFMsgExtracingFiles, files.size());

        List<PDFInputFile> inputFiles = files.stream().map(PDFInputFile::new).collect(Collectors.toList());

        Map<Extractor, List<Item>> itemsByExtractor = new HashMap<>();

        var securityCache = new SecurityCache(client);

        for (PDFInputFile inputFile : inputFiles)
        {
            monitor.setTaskName(inputFile.getName());

            try
            {
                inputFile.convertPDFtoText();

                var extracted = false;

                List<Exception> warnings = new ArrayList<>();
                for (Extractor extractor : extractors)
                {
                    var items = extractor.extract(securityCache, inputFile, warnings);

                    if (!items.isEmpty())
                    {
                        extracted = true;
                        itemsByExtractor.computeIfAbsent(extractor, e -> new ArrayList<Item>()).addAll(items);
                        break;
                    }
                }

                if (!extracted)
                {
                    try
                    {
                        inputFile.convertLegacyPDFtoText();
                        for (Extractor extractor : extractors)
                        {
                            var items = extractor.extract(securityCache, inputFile, warnings);

                            if (!items.isEmpty())
                            {
                                extracted = true;
                                itemsByExtractor.computeIfAbsent(extractor, e -> new ArrayList<Item>()).addAll(items);
                                break;
                            }
                        }

                        if (extracted)
                        {
                            PortfolioLog.info("PDF successfully imported with PDFBox 1.8.x " + inputFile.getName()); //$NON-NLS-1$
                        }
                    }
                    catch (IOException ignore)
                    {
                        // ignore if the file cannot be read by PDFBox Version 1
                        PortfolioLog.error(ignore);
                    }
                }

                if (!extracted)
                {
                    Predicate<? super Exception> isNotUnsupportedOperation = e -> !(e instanceof UnsupportedOperationException);
                    var meaningfulExceptions = warnings.stream().filter(isNotUnsupportedOperation).toList();

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
        itemsByExtractor.entrySet().forEach(e -> e.getKey().postProcessing(e.getValue()));

        securityCache.addMissingSecurityItems(itemsByExtractor);

        return itemsByExtractor;
    }
}
