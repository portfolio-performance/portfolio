package name.abuchen.portfolio.ui.editor;

import static name.abuchen.portfolio.util.CollectorsUtil.toMutableList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import name.abuchen.portfolio.model.ConfigurationSet.Configuration;
import name.abuchen.portfolio.model.ConfigurationSet.WellKnownConfigurationSets;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public class ReportingPeriods
{
    private final ClientInput clientInput;

    private ArrayList<ReportingPeriod> periods;

    ReportingPeriods(ClientInput clientInput)
    {
        this.clientInput = clientInput;

        loadReportingPeriods();
    }

    public Stream<ReportingPeriod> stream()
    {
        return periods.stream();
    }

    public void add(ReportingPeriod p)
    {
        if (periods.contains(p))
            return;

        periods.add(p);

        storeReportingPeriods();
    }

    public void replaceAll(List<ReportingPeriod> reportingPeriods)
    {
        periods.clear();
        periods.addAll(reportingPeriods);

        storeReportingPeriods();
    }

    public boolean contains(ReportingPeriod p)
    {
        return periods.contains(p);
    }

    private void loadReportingPeriods()
    {
        periods = (ArrayList<ReportingPeriod>) clientInput.getClient().getSettings()
                        .getConfigurationSet(WellKnownConfigurationSets.REPORTING_PERIODS) //
                        .getConfigurations().map(c -> reportingPeriodStringToReportingPeriod(c.getData())) //
                        .filter(java.util.Optional::isPresent) //
                        .map(java.util.Optional::get).collect(toMutableList());

        // if periods not in client file, load them from settings file
        // (legacy)
        if (periods.isEmpty())
        {
            periods = legacyStringToReportingPeriods(
                            clientInput.getPreferenceStore().getString("AbstractHistoricView")); //$NON-NLS-1$

            // immediately store periods in case they were loaded from legacy
            if (!periods.isEmpty())
                storeReportingPeriods();
        }

        if (periods.isEmpty())
        {
            periods = defaultReportingPeriods();
        }
    }

    private static ArrayList<ReportingPeriod> defaultReportingPeriods()
    {
        var answer = new ArrayList<ReportingPeriod>();
        for (int ii = 1; ii <= 3; ii++)
            answer.add(new ReportingPeriod.LastX(ii, 0));
        return answer;
    }

    private static Optional<ReportingPeriod> reportingPeriodStringToReportingPeriod(String code)
    {
        try
        {
            return Optional.of(ReportingPeriod.from(code));
        }
        catch (IOException | RuntimeException ignore)
        {
            PortfolioPlugin.log(ignore);
        }
        return Optional.empty();
    }

    private ArrayList<ReportingPeriod> legacyStringToReportingPeriods(String string)
    {
        if (string == null || string.trim().length() <= 0)
            return new ArrayList<>();

        return (ArrayList<ReportingPeriod>) Arrays.stream(string.split(";")) //$NON-NLS-1$
                        .map(c -> reportingPeriodStringToReportingPeriod(c)) //
                        .filter(java.util.Optional::isPresent) //
                        .map(java.util.Optional::get).collect(toMutableList());
    }

    private void storeReportingPeriods()
    {
        var set = clientInput.getClient().getSettings()
                        .getConfigurationSet(WellKnownConfigurationSets.REPORTING_PERIODS);
        set.clear();
        periods.forEach(rp -> set.add(new Configuration("", "", rp.getCode()))); //$NON-NLS-1$ //$NON-NLS-2$
        clientInput.touch();
    }
}
