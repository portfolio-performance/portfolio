package name.abuchen.portfolio.datatransfer.csv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVPrinter;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.ReportingPeriod;

/**
 * Special exporter for the VINIS-App
 */
public class VINISExporter
{
    
    /**
     * Export all values in 'VINIS-App' Format
     */
    public void exportAllValues(File file, Client client) throws IOException
    {
        final String baseCurrency = client.getBaseCurrency();
        ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(client);
        CurrencyConverter converter = new CurrencyConverterImpl(factory, baseCurrency);
        
        LocalDate lastYear = LocalDate.now().minusYears(1);
        LocalDate firstYear = LocalDate.now().minusYears(100);
        ReportingPeriod periodLastYear = new ReportingPeriod.FromXtoY(lastYear.with(TemporalAdjusters.firstDayOfYear()),
                        lastYear.with(TemporalAdjusters.lastDayOfYear()));
        ReportingPeriod periodCurrentYear = new ReportingPeriod.FromXtoY(LocalDate.now().with(TemporalAdjusters.firstDayOfYear()),
                        LocalDate.now());
        ReportingPeriod periodAllYears = new ReportingPeriod.FromXtoY(firstYear.with(TemporalAdjusters.firstDayOfYear()),
                        LocalDate.now());
        ReportingPeriod periodToday = new ReportingPeriod.FromXtoY(LocalDate.now(),
                        LocalDate.now());
        
        ClientPerformanceSnapshot performanceAllYears = new ClientPerformanceSnapshot(client, converter,
                        periodAllYears.toInterval(LocalDate.now()));
        ClientPerformanceSnapshot performanceCurrentYear = new ClientPerformanceSnapshot(client, converter,
                        periodCurrentYear.toInterval(LocalDate.now()));
        ClientPerformanceSnapshot performanceLastYear = new ClientPerformanceSnapshot(client, converter,
                        periodLastYear.toInterval(LocalDate.now()));
        ClientPerformanceSnapshot performanceToday = new ClientPerformanceSnapshot(client, converter,
                        periodToday.toInterval(LocalDate.now()));
        
        
        Money earningsCurrentYear = performanceCurrentYear.getValue(CategoryType.EARNINGS);
        Money earningsLastYear = performanceLastYear.getValue(CategoryType.EARNINGS);
        Money earningsAll = performanceAllYears.getValue(CategoryType.EARNINGS);
        
        Money capitalGainsCurrentYear = performanceCurrentYear.getValue(CategoryType.CAPITAL_GAINS);
        Money capitalGainsLastYear = performanceLastYear.getValue(CategoryType.CAPITAL_GAINS);
        Money capitalGainsAll = performanceAllYears.getValue(CategoryType.CAPITAL_GAINS);
        
        Money realizedCapitalGainsCurrentYear = performanceCurrentYear.getValue(CategoryType.REALIZED_CAPITAL_GAINS);
        Money realizedCapitalGainsLastYear = performanceLastYear.getValue(CategoryType.REALIZED_CAPITAL_GAINS);
        Money realizedCapitalGainsAll = performanceAllYears.getValue(CategoryType.REALIZED_CAPITAL_GAINS); 
        
        Money buySecurityValue = Money.of(baseCurrency, 0);
        Money currentSecurityValue = Money.of(baseCurrency, 0);
        Money buyTotalValue = Money.of(baseCurrency, 0);
        Money currentTotalValue = Money.of(baseCurrency, 0);
        Money cash = Money.of(baseCurrency, 0);
        
        List<AssetPosition> assets =  performanceToday.getEndClientSnapshot().getAssetPositions()
                                        .collect(Collectors.toList());

        for (AssetPosition asset : assets)
        {
            //calc security values
            if(asset.getPosition() != null && asset.getSecurity() != null)
            {
                if (!asset.getFIFOPurchaseValue().getCurrencyCode().equals(baseCurrency))
                {
                    buySecurityValue = buySecurityValue.add(converter.convert(LocalDate.now(), asset.getFIFOPurchaseValue()));
                    currentSecurityValue = currentSecurityValue.add(converter.convert(LocalDate.now(), asset.getValuation()));
                }
                else
                {
                    buySecurityValue = buySecurityValue.add(asset.getFIFOPurchaseValue());
                    currentSecurityValue = currentSecurityValue.add(asset.getValuation());
                }   
            }
            
            //calc total values
            if (!asset.getFIFOPurchaseValue().getCurrencyCode().equals(baseCurrency))
            {
                //use purchaseValue only for securities, on accounts we only have the valuation 
                if(asset.getPosition() != null && asset.getSecurity() != null)
                    buyTotalValue = buyTotalValue.add(converter.convert(LocalDate.now(), asset.getFIFOPurchaseValue()));
                else
                    buyTotalValue = buyTotalValue.add(converter.convert(LocalDate.now(), asset.getValuation())); 
                
                currentTotalValue = currentTotalValue.add(converter.convert(LocalDate.now(), asset.getValuation()));
            }
            else
            {
              //use purchaseValue only for securities, on accounts we only have the valuation  
                if(asset.getPosition() != null && asset.getSecurity() != null)
                    buyTotalValue = buyTotalValue.add(asset.getFIFOPurchaseValue());
                else
                    buyTotalValue = buyTotalValue.add(asset.getValuation());
                
                currentTotalValue = currentTotalValue.add(asset.getValuation());
            }
            
        }

        for (AccountSnapshot account : performanceToday.getEndClientSnapshot().getAccounts())
            cash = cash.add(account.getFunds());   
        
        
        // write to file
        try (CSVPrinter printer = new CSVPrinter(
                        new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8),
                        CSVExporter.STRATEGY))
        {
            writeHeader(printer);
            
            write(printer, "Kontostände Summe", Values.Amount.format(cash.getAmount()));
            
            write(printer, "Aktien Kaufwert", Values.Amount.format(buySecurityValue.getAmount()));
            write(printer, "Aktien Marktwert", Values.Amount.format(currentSecurityValue.getAmount()));
            
            write(printer, "Gesamtvermögen Kaufwert", Values.Amount.format(buyTotalValue.getAmount()));
            write(printer, "Gesamtvermögen Marktwert", Values.Amount.format(currentTotalValue.getAmount()));
            
            write(printer, "Erträge aktuelles Jahr", Values.Amount.format(earningsCurrentYear.getAmount())); 
            write(printer, "Erträge letztes Jahr", Values.Amount.format(earningsLastYear.getAmount())); 
            write(printer, "Erträge gesamt", Values.Amount.format(earningsAll.getAmount()));
            
            write(printer, "Kurserfolge aktuelles Jahr", Values.Amount.format(capitalGainsCurrentYear.getAmount())); 
            write(printer, "Kurserfolge letztes Jahr", Values.Amount.format(capitalGainsLastYear.getAmount())); 
            write(printer, "Kurserfolge gesamt", Values.Amount.format(capitalGainsAll.getAmount()));
            
            write(printer, "Realisierte Kurserfolge aktuelles Jahr", Values.Amount.format(realizedCapitalGainsCurrentYear.getAmount())); 
            write(printer, "Realisierte Kurserfolge letztes Jahr", Values.Amount.format(realizedCapitalGainsLastYear.getAmount())); 
            write(printer, "Realisierte Kurserfolge gesamt", Values.Amount.format(realizedCapitalGainsAll.getAmount()));       
        }
    }

    private void write(CSVPrinter printer, String description, String value) throws IOException
    {
        printer.print(description);
        printer.print(value);
        printer.println();
    }



    @SuppressWarnings("nls")
    private void writeHeader(CSVPrinter printer) throws IOException
    {
        printer.print("Bezeichnung");
        printer.print("Wert");
        printer.println();
    }
}
