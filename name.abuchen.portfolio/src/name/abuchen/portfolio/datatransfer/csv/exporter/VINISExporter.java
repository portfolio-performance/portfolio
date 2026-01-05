package name.abuchen.portfolio.datatransfer.csv.exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import org.apache.commons.csv.CSVPrinter;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.AssetPosition;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceIndicator;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

/**
 * Special exporter for the VINIS-App
 */
public class VINISExporter
{
    /**
     * Export all values in 'VINIS-App' Format
     */
    public void exportAllValues(File file, Client client, ExchangeRateProviderFactory factory) throws IOException
    {
        final var baseCurrency = client.getBaseCurrency();
        var converter = new CurrencyConverterImpl(factory, baseCurrency);

        var lastYear = LocalDate.now().minusYears(1);
        var firstYear = LocalDate.now().minusYears(100);

        var periodCurrentYear = new ReportingPeriod.YearToDate();
        var periodLastYear = new ReportingPeriod.YearX(lastYear.getYear());
        var periodAllYears = new ReportingPeriod.FromXtoY(
                        firstYear.with(TemporalAdjusters.firstDayOfYear()), LocalDate.now());

        var performanceAllYears = new ClientPerformanceSnapshot(client, converter,
                        periodAllYears.toInterval(LocalDate.now()));
        var performanceCurrentYear = new ClientPerformanceSnapshot(client, converter,
                        periodCurrentYear.toInterval(LocalDate.now()));
        var performanceLastYear = new ClientPerformanceSnapshot(client, converter,
                        periodLastYear.toInterval(LocalDate.now()));

        var earningsCurrentYear = performanceCurrentYear.getValue(CategoryType.EARNINGS);
        var earningsLastYear = performanceLastYear.getValue(CategoryType.EARNINGS);
        var earningsAll = performanceAllYears.getValue(CategoryType.EARNINGS);

        var capitalGainsCurrentYear = performanceCurrentYear.getValue(CategoryType.CAPITAL_GAINS);
        var capitalGainsLastYear = performanceLastYear.getValue(CategoryType.CAPITAL_GAINS);
        var capitalGainsAll = performanceAllYears.getValue(CategoryType.CAPITAL_GAINS);

        var realizedCapitalGainsCurrentYear = performanceCurrentYear.getValue(CategoryType.REALIZED_CAPITAL_GAINS);
        var realizedCapitalGainsLastYear = performanceLastYear.getValue(CategoryType.REALIZED_CAPITAL_GAINS);
        var realizedCapitalGainsAll = performanceAllYears.getValue(CategoryType.REALIZED_CAPITAL_GAINS);

        var buySecurityValue = MutableMoney.of(baseCurrency);
        var currentSecurityValue = MutableMoney.of(baseCurrency);
        var buyTotalValue = MutableMoney.of(baseCurrency);
        var currentTotalValue = MutableMoney.of(baseCurrency);

        var securityPerformance = SecurityPerformanceSnapshot.create(client, converter,
                        Interval.of(LocalDate.MIN, LocalDate.now()), SecurityPerformanceIndicator.Costs.class);

        var assets = performanceCurrentYear.getEndClientSnapshot().getAssetPositions().toList();

        var toBaseCurrency = converter.at(LocalDate.now());

        for (AssetPosition asset : assets)
        {
            var valuation = asset.getValuation().with(toBaseCurrency);

            if (asset.getSecurity() != null)
            {
                var fifo = securityPerformance.getRecord(asset.getSecurity())
                                .map(SecurityPerformanceRecord::getFifoCost).orElse(Money.of(baseCurrency, 0))
                                .with(toBaseCurrency);
                buySecurityValue.add(fifo);
                currentSecurityValue.add(valuation);
                buyTotalValue.add(fifo);
            }
            else
            {
                buyTotalValue.add(valuation);
            }

            currentTotalValue.add(valuation);
        }

        var cash = performanceCurrentYear.getEndClientSnapshot().getAccounts().stream().map(AccountSnapshot::getFunds)
                        .collect(MoneyCollectors.sum(baseCurrency));

        // write to file
        try (var printer = new CSVPrinter(
                        new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8),
                        CSVExporter.STRATEGY))
        {
            writeHeader(printer);

            write(printer, Messages.VINISAppValueFundsSum, cash);

            write(printer, Messages.VINISAppValueSecuritiesPurchase, buySecurityValue.toMoney());
            write(printer, Messages.VINISAppValueSecuritiesMarket, currentSecurityValue.toMoney());

            write(printer, Messages.VINISAppValueTotalAssetsPurchase, buyTotalValue.toMoney());
            write(printer, Messages.VINISAppValueTotalAssetsMarket, currentTotalValue.toMoney());

            write(printer, Messages.VINISAppValueEarningsCurrentYear, earningsCurrentYear);
            write(printer, Messages.VINISAppValueEarningsLastYear, earningsLastYear);
            write(printer, Messages.VINISAppValueEarningsTotal, earningsAll);

            write(printer, Messages.VINISAppValueCapitalGainsCurrentYear, capitalGainsCurrentYear);
            write(printer, Messages.VINISAppValueCapitalGainsLastYear, capitalGainsLastYear);
            write(printer, Messages.VINISAppValueCapitalGainsTotal, capitalGainsAll);

            write(printer, Messages.VINISAppValueRealizedCapitalGainsCurrentYear, realizedCapitalGainsCurrentYear);
            write(printer, Messages.VINISAppValueRealizedCapitalGainsLastYear, realizedCapitalGainsLastYear);
            write(printer, Messages.VINISAppValueRealizedCapitalGainsTotal, realizedCapitalGainsAll);
        }
    }

    private void write(CSVPrinter printer, String description, Money value) throws IOException
    {
        printer.printRecord(description, //
                        Values.Amount.format(value.getAmount()), //
                        value.getCurrencyCode());
    }

    private void writeHeader(CSVPrinter printer) throws IOException
    {
        printer.printRecord(Messages.CSVColumn_Name, //
                        Messages.CSVColumn_Value, //
                        Messages.CSVColumn_Currency);
    }
}
