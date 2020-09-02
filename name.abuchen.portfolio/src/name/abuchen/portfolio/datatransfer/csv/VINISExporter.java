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
import name.abuchen.portfolio.money.MonetaryOperator;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.MutableMoney;
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
    public void exportAllValues(File file, Client client, ExchangeRateProviderFactory factory) throws IOException
    {
        final String baseCurrency = client.getBaseCurrency();
        CurrencyConverter converter = new CurrencyConverterImpl(factory, baseCurrency);

        LocalDate lastYear = LocalDate.now().minusYears(1);
        LocalDate firstYear = LocalDate.now().minusYears(100);

        ReportingPeriod periodCurrentYear = new ReportingPeriod.YearToDate();
        ReportingPeriod periodLastYear = new ReportingPeriod.YearX(lastYear.getYear());
        ReportingPeriod periodAllYears = new ReportingPeriod.FromXtoY(
                        firstYear.with(TemporalAdjusters.firstDayOfYear()), LocalDate.now());

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

        MutableMoney buySecurityValue = MutableMoney.of(baseCurrency);
        MutableMoney currentSecurityValue = MutableMoney.of(baseCurrency);
        MutableMoney buyTotalValue = MutableMoney.of(baseCurrency);
        MutableMoney currentTotalValue = MutableMoney.of(baseCurrency);

        List<AssetPosition> assets = performanceCurrentYear.getEndClientSnapshot().getAssetPositions()
                        .collect(Collectors.toList());

        MonetaryOperator toBaseCurrency = converter.at(LocalDate.now());

        for (AssetPosition asset : assets)
        {
            Money fifo = asset.getFIFOPurchaseValue().with(toBaseCurrency);
            Money valuation = asset.getValuation().with(toBaseCurrency);

            if (asset.getSecurity() != null)
            {
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

        Money cash = performanceCurrentYear.getEndClientSnapshot().getAccounts().stream().map(AccountSnapshot::getFunds)
                        .collect(MoneyCollectors.sum(baseCurrency));

        // write to file
        try (CSVPrinter printer = new CSVPrinter(
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
        printer.print(description);
        printer.print(Values.Amount.format(value.getAmount()));
        printer.print(value.getCurrencyCode());
        printer.println();
    }

    private void writeHeader(CSVPrinter printer) throws IOException
    {
        printer.print(Messages.CSVColumn_Name);
        printer.print(Messages.CSVColumn_Value);
        printer.print(Messages.CSVColumn_Currency);
        printer.println();
    }
}
