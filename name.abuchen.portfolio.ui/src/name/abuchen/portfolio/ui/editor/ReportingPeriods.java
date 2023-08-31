package name.abuchen.portfolio.ui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import name.abuchen.portfolio.snapshot.ReportingPeriod;

public class ReportingPeriods
{
    private final ArrayList<ReportingPeriod> periods;
    private final ClientInput clientInput;

    ReportingPeriods(ClientInput clientInput, ArrayList<ReportingPeriod> periods)
    {
        this.clientInput = clientInput;
        this.periods = clientInput.loadReportingPeriods();
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

        clientInput.storeReportingPeriods(this.stream());
        clientInput.touch();
    }

    public void replaceAll(List<ReportingPeriod> reportingPeriods)
    {
        periods.clear();
        periods.addAll(reportingPeriods);

        clientInput.storeReportingPeriods(this.stream());
        clientInput.touch();
    }

    public boolean contains(ReportingPeriod p)
    {
        return periods.contains(p);
    }

}
