package name.abuchen.portfolio.ui.views.dataseries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.ClientFilterMenu;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries.ClientDataSeries;
import name.abuchen.portfolio.util.ColorConversion;

/**
 * The set of available data series for a given presentation use case.
 */
public class DataSeriesSet
{
    private DataSeries.UseCase useCase;
    private final List<DataSeries> availableSeries = new ArrayList<>();

    public DataSeriesSet(Client client, IPreferenceStore preferences, DataSeries.UseCase useCase)
    {
        this.useCase = useCase;

        ColorWheel wheel = new ColorWheel(30);
        switch (useCase)
        {
            case STATEMENT_OF_ASSETS:
                buildStatementOfAssetsDataSeries();
                break;
            case PERFORMANCE:
                buildPerformanceDataSeries(client, preferences, wheel);
                break;
            case RETURN_VOLATILITY:
                buildReturnVolatilitySeries(client, preferences, wheel);
                break;
            default:
                throw new IllegalArgumentException(useCase.name());
        }

        buildCommonDataSeries(client, preferences, wheel);
    }

    public DataSeries.UseCase getUseCase()
    {
        return useCase;
    }

    public List<DataSeries> getAvailableSeries()
    {
        return availableSeries;
    }

    /**
     * Returns DataSeries matching the given UUID.
     */
    public DataSeries lookup(String uuid)
    {
        return availableSeries.stream().filter(d -> d.getUUID().equals(uuid)).findAny().orElse(null);
    }

    private void buildStatementOfAssetsDataSeries()
    {
        availableSeries.add(new DataSeries(DataSeries.Type.CLIENT, ClientDataSeries.TOTALS, Messages.LabelTotalSum,
                        Colors.TOTALS.getRGB()));

        DataSeries series = new DataSeries(DataSeries.Type.CLIENT, ClientDataSeries.TRANSFERALS,
                        Messages.LabelTransferals, Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY).getRGB());
        series.setLineChart(false);
        availableSeries.add(series);

        series = new DataSeries(DataSeries.Type.CLIENT, ClientDataSeries.INVESTED_CAPITAL,
                        Messages.LabelInvestedCapital, Display.getDefault().getSystemColor(SWT.COLOR_GRAY).getRGB());
        series.setShowArea(true);
        availableSeries.add(series);

        series = new DataSeries(DataSeries.Type.CLIENT, ClientDataSeries.ABSOLUTE_INVESTED_CAPITAL,
                        Messages.LabelAbsoluteInvestedCapital,
                        Display.getDefault().getSystemColor(SWT.COLOR_GRAY).getRGB());
        series.setShowArea(true);
        availableSeries.add(series);

        series = new DataSeries(DataSeries.Type.CLIENT, ClientDataSeries.ABSOLUTE_DELTA, Messages.LabelDelta,
                        Display.getDefault().getSystemColor(SWT.COLOR_GRAY).getRGB());
        availableSeries.add(series);

        series = new DataSeries(DataSeries.Type.CLIENT, ClientDataSeries.ABSOLUTE_DELTA_ALL_RECORDS,
                        Messages.LabelAbsoluteDelta, Display.getDefault().getSystemColor(SWT.COLOR_GRAY).getRGB());
        availableSeries.add(series);

        series = new DataSeries(DataSeries.Type.CLIENT, ClientDataSeries.TAXES, Messages.LabelAccumulatedTaxes,
                        Display.getDefault().getSystemColor(SWT.COLOR_RED).getRGB());
        availableSeries.add(series);

        series = new DataSeries(DataSeries.Type.CLIENT, ClientDataSeries.DIVIDENDS, Messages.LabelDividends,
                        Display.getDefault().getSystemColor(SWT.COLOR_DARK_MAGENTA).getRGB());
        series.setLineChart(false);
        availableSeries.add(series);

        series = new DataSeries(DataSeries.Type.CLIENT, ClientDataSeries.DIVIDENDS_ACCUMULATED,
                        Messages.LabelAccumulatedDividends,
                        Display.getDefault().getSystemColor(SWT.COLOR_DARK_MAGENTA).getRGB());
        availableSeries.add(series);

        series = new DataSeries(DataSeries.Type.CLIENT, ClientDataSeries.INTEREST, Messages.LabelInterest,
                        Colors.DARK_GREEN.getRGB());
        series.setLineChart(false);
        availableSeries.add(series);

        series = new DataSeries(DataSeries.Type.CLIENT, ClientDataSeries.INTEREST_ACCUMULATED,
                        Messages.LabelAccumulatedInterest, Colors.DARK_GREEN.getRGB());
        availableSeries.add(series);

        series = new DataSeries(DataSeries.Type.CLIENT, ClientDataSeries.INTEREST_CHARGE, Messages.LabelInterestCharge,
                        Colors.DARK_GREEN.getRGB());
        series.setLineChart(false);
        availableSeries.add(series);

        series = new DataSeries(DataSeries.Type.CLIENT, ClientDataSeries.INTEREST_CHARGE_ACCUMULATED,
                        Messages.LabelAccumulatedInterestCharge, Colors.DARK_GREEN.getRGB());
        availableSeries.add(series);

        series = new DataSeries(DataSeries.Type.CLIENT, ClientDataSeries.EARNINGS, Messages.LabelEarnings,
                        Colors.DARK_GREEN.getRGB());
        series.setLineChart(false);
        availableSeries.add(series);

        series = new DataSeries(DataSeries.Type.CLIENT, ClientDataSeries.EARNINGS_ACCUMULATED,
                        Messages.LabelAccumulatedEarnings, Colors.DARK_GREEN.getRGB());
        availableSeries.add(series);

    }

    private void buildPerformanceDataSeries(Client client, IPreferenceStore preferences, ColorWheel wheel)
    {
        // accumulated performance
        availableSeries.add(new DataSeries(DataSeries.Type.CLIENT, ClientDataSeries.TOTALS,
                        Messages.PerformanceChartLabelEntirePortfolio, Colors.TOTALS.getRGB()));

        DataSeries series = new DataSeries(DataSeries.Type.CLIENT, ClientDataSeries.DELTA_PERCENTAGE,
                        Messages.LabelAggregationDaily,
                        Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY).getRGB());
        series.setLineChart(false);
        availableSeries.add(series);

        // securities as benchmark
        for (Security security : client.getSecurities())
        {
            series = new DataSeries(DataSeries.Type.SECURITY_BENCHMARK, security, security.getName(), //
                            wheel.next());
            series.setBenchmark(true);
            availableSeries.add(series);
        }

        buildPreTaxDataSeries(client, preferences, wheel);
    }

    private void buildReturnVolatilitySeries(Client client, IPreferenceStore preferences, ColorWheel wheel)
    {
        // accumulated performance
        availableSeries.add(new DataSeries(DataSeries.Type.CLIENT, ClientDataSeries.TOTALS,
                        Messages.PerformanceChartLabelEntirePortfolio, Colors.TOTALS.getRGB()));

        // securities as benchmark
        for (Security security : client.getSecurities())
        {
            DataSeries series = new DataSeries(DataSeries.Type.SECURITY_BENCHMARK, security, security.getName(), //
                            wheel.next());

            series.setBenchmark(true);
            availableSeries.add(series);
        }

        buildPreTaxDataSeries(client, preferences, wheel);
    }

    private void buildPreTaxDataSeries(Client client, IPreferenceStore preferences, ColorWheel wheel)
    {
        availableSeries.add(new DataSeries(DataSeries.Type.CLIENT_PRETAX, ClientDataSeries.TOTALS,
                        Messages.PerformanceChartLabelEntirePortfolio + Messages.LabelSuffix_PreTax, wheel.next()));

        for (Portfolio portfolio : client.getPortfolios())
            availableSeries.add(new DataSeries(DataSeries.Type.PORTFOLIO_PRETAX, portfolio,
                            portfolio.getName() + Messages.LabelSuffix_PreTax, wheel.next()));

        for (Portfolio portfolio : client.getPortfolios())
            availableSeries.add(new DataSeries(DataSeries.Type.PORTFOLIO_PLUS_ACCOUNT_PRETAX, portfolio,
                            portfolio.getName() + " + " + portfolio.getReferenceAccount().getName() //$NON-NLS-1$
                                            + Messages.LabelSuffix_PreTax,
                            wheel.next()));

        for (Account account : client.getAccounts())
            availableSeries.add(new DataSeries(DataSeries.Type.ACCOUNT_PRETAX, account,
                            account.getName() + Messages.LabelSuffix_PreTax, wheel.next()));

        addCustomClientFilters(client, preferences, true, wheel);
    }

    private void addCustomClientFilters(Client client, IPreferenceStore preferences, boolean isPreTax, ColorWheel wheel)
    {
        // custom client filters
        ClientFilterMenu menu = new ClientFilterMenu(client, preferences);

        // quick fix: users can create duplicate client filters that end up to
        // have the same UUID. Avoid adding both violates the precondition that
        // every data series must have a unique id
        Set<String> addedSeries = new HashSet<>();
        for (ClientFilterMenu.Item item : menu.getCustomItems())
        {
            DataSeries series = new DataSeries(
                            isPreTax ? DataSeries.Type.CLIENT_FILTER_PRETAX : DataSeries.Type.CLIENT_FILTER, item,
                            isPreTax ? item.getLabel() + Messages.LabelSuffix_PreTax : item.getLabel(), wheel.next());

            if (addedSeries.add(series.getUUID()))
                availableSeries.add(series);
        }
    }

    private void buildCommonDataSeries(Client client, IPreferenceStore preferences, ColorWheel wheel)
    {
        for (Security security : client.getSecurities())
        {
            // securities w/o currency code (e.g. a stock index) cannot be added
            // as equity data series (only as benchmark)
            if (security.getCurrencyCode() == null)
                continue;

            availableSeries.add(new DataSeries(DataSeries.Type.SECURITY, security, security.getName(), //
                            wheel.next()));
        }

        for (Portfolio portfolio : client.getPortfolios())
            availableSeries.add(new DataSeries(DataSeries.Type.PORTFOLIO, portfolio, portfolio.getName(), //
                            wheel.next()));

        // portfolio + reference account
        for (Portfolio portfolio : client.getPortfolios())
        {
            DataSeries series = new DataSeries(DataSeries.Type.PORTFOLIO_PLUS_ACCOUNT, portfolio,
                            portfolio.getName() + " + " + portfolio.getReferenceAccount().getName(), //$NON-NLS-1$
                            wheel.next());
            availableSeries.add(series);
        }

        addCustomClientFilters(client, preferences, false, wheel);

        for (Account account : client.getAccounts())
            availableSeries.add(new DataSeries(DataSeries.Type.ACCOUNT, account, account.getName(), wheel.next()));

        for (Taxonomy taxonomy : client.getTaxonomies())
        {
            taxonomy.foreach(new Taxonomy.Visitor()
            {
                @Override
                public void visit(Classification classification)
                {
                    if (classification.getParent() == null)
                        return;

                    availableSeries.add(new DataSeries(DataSeries.Type.CLASSIFICATION, taxonomy, classification,
                                    classification.getName(), ColorConversion.hex2RGB(classification.getColor())));
                }
            });
        }
    }
}
