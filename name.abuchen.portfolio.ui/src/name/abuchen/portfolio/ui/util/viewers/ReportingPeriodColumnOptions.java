package name.abuchen.portfolio.ui.util.viewers;

import java.io.IOException;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.dialogs.ReportingPeriodDialog;

public class ReportingPeriodColumnOptions implements Column.Options<ReportingPeriod>
{
    private String columnLabel;
    private List<ReportingPeriod> defaultOptions;

    public ReportingPeriodColumnOptions(String columnLabel, List<ReportingPeriod> defaultOptions)
    {
        this.columnLabel = columnLabel;
        this.defaultOptions = defaultOptions;
    }

    @Override
    public List<ReportingPeriod> getElements()
    {
        return defaultOptions;
    }

    @Override
    public ReportingPeriod valueOf(String s)
    {
        try
        {
            return ReportingPeriod.from(s);
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String toString(ReportingPeriod element)
    {
        return element.getCode();
    }

    @Override
    public String getColumnLabel(ReportingPeriod element)
    {
        return MessageFormat.format(columnLabel, element.toString());
    }

    @Override
    public String getMenuLabel(ReportingPeriod element)
    {
        return element.toString();
    }

    @Override
    public String getDescription(ReportingPeriod element)
    {
        return null;
    }

    @Override
    public boolean canCreateNewElements()
    {
        return true;
    }

    @Override
    public ReportingPeriod createNewElement(Shell shell)
    {
        ReportingPeriodDialog dialog = new ReportingPeriodDialog(shell, null);
        if (dialog.open() == ReportingPeriodDialog.OK)
        {
            ReportingPeriod p = dialog.getReportingPeriod();
            defaultOptions.add(p);
            return p;
        }
        return null;
    }
}