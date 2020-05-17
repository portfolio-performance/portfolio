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

import name.abuchen.portfolio.Messages;
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
        ReportingPeriod periodLastYear = new ReportingPeriod.YearX(lastYear.getYear());
        ReportingPeriod periodCurrentYear = new ReportingPeriod.YearToDate();
        ReportingPeriod periodAllYears = new ReportingPeriod.FromXtoY(firstYear.with(TemporalAdjusters.firstDayOfYear()),
                        LocalDate.now());
        
        ClientPerformanceSnapshot performanceAllYears = new ClientPerformanceSnapshot(client, converter,
                        periodAllYears.toInterval(LocalDate.now()));
        ClientPerformanceSnapshot performanceCurrentYear = new ClientPerformanceSnapshot(client, converter,
                        periodCurrentYear.toInterval(LocalDate.now()));
        ClientPerformanceSnapshot performanceLastYear = new ClientPerformanceSnapshot(client, converter,
                        periodLastYear.toInterval(LocalDate.now()));
        
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
        
        List<AssetPosition> assets = performanceCurrentYear.getEndClientSnapshot().getAssetPositions()
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

        for (AccountSnapshot account : performanceCurrentYear.getEndClientSnapshot().getAccounts())
            cash = cash.add(account.getFunds());   
        
        
        // write to file
        try (CSVPrinter printer = new CSVPrinter(
                        new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8),
                        CSVExporter.STRATEGY))
        {
            writeHeader(printer);

            write(printer, Messages.VINISAppValueFundsSum, Values.Amount.format(cash.getAmount()), cash.getCurrencyCode());
            
            write(printer, Messages.VINISAppValueSecuritiesPurchase, Values.Amount.format(buySecurityValue.getAmount()), buySecurityValue.getCurrencyCode());
            write(printer, Messages.VINISAppValueSecuritiesMarket, Values.Amount.format(currentSecurityValue.getAmount()), currentSecurityValue.getCurrencyCode());
             
            write(printer, Messages.VINISAppValueTotalAssetsPurchase, Values.Amount.format(buyTotalValue.getAmount()), buyTotalValue.getCurrencyCode());
            write(printer, Messages.VINISAppValueTotalAssetsMarket, Values.Amount.format(currentTotalValue.getAmount()), currentTotalValue.getCurrencyCode());
            
            write(printer, Messages.VINISAppValueEarningsCurrentYear, Values.Amount.format(earningsCurrentYear.getAmount()), earningsCurrentYear.getCurrencyCode()); 
            write(printer, Messages.VINISAppValueEarningsLastYear, Values.Amount.format(earningsLastYear.getAmount()), earningsLastYear.getCurrencyCode()); 
            write(printer, Messages.VINISAppValueEarningsTotal, Values.Amount.format(earningsAll.getAmount()), earningsAll.getCurrencyCode());
            
            write(printer, Messages.VINISAppValueCapitalGainsCurrentYear, Values.Amount.format(capitalGainsCurrentYear.getAmount()), capitalGainsCurrentYear.getCurrencyCode()); 
            write(printer, Messages.VINISAppValueCapitalGainsLastYear, Values.Amount.format(capitalGainsLastYear.getAmount()), capitalGainsLastYear.getCurrencyCode()); 
            write(printer, Messages.VINISAppValueCapitalGainsTotal, Values.Amount.format(capitalGainsAll.getAmount()), capitalGainsAll.getCurrencyCode());
            
            write(printer, Messages.VINISAppValueRealizedCapitalGainsCurrentYear, Values.Amount.format(realizedCapitalGainsCurrentYear.getAmount()), realizedCapitalGainsCurrentYear.getCurrencyCode()); 
            write(printer, Messages.VINISAppValueRealizedCapitalGainsLastYear, Values.Amount.format(realizedCapitalGainsLastYear.getAmount()), realizedCapitalGainsLastYear.getCurrencyCode()); 
            write(printer, Messages.VINISAppValueRealizedCapitalGainsTotal, Values.Amount.format(realizedCapitalGainsAll.getAmount()), realizedCapitalGainsAll.getCurrencyCode());       
        }
    }

    private void write(CSVPrinter printer, String description, String value, String currency) throws IOException
    {
        printer.print(description);
        printer.print(value);
        printer.print(currency);
        printer.println();
    }



    @SuppressWarnings("nls")
    private void writeHeader(CSVPrinter printer) throws IOException
    {
        
        printer.print(Messages.CSVColumn_Name);
        printer.print(Messages.CSVColumn_Value);
        printer.print(Messages.CSVColumn_Currency);
        printer.println();
    }
}
