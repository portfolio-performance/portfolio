package name.abuchen.portfolio.ui.util.viewers;

import java.io.IOException;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.FirstTransactionSupplier;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.dialogs.ReportingPeriodDialog;

public class ReportingPeriodColumnOptions implements Column.Options<ReportingPeriod>
{
    private String columnLabel;
    private List<ReportingPeriod> defaultOptions;
    private FirstTransactionSupplier firstTransactionSupplier;

    public ReportingPeriodColumnOptions(FirstTransactionSupplier firstTransactionSupplier, String columnLabel, List<ReportingPeriod> defaultOptions)
    {
        this.firstTransactionSupplier = firstTransactionSupplier;
        this.columnLabel = columnLabel;
        this.defaultOptions = defaultOptions;
    }

    @Override
    public List<ReportingPeriod> getOptions()
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
    public String toString(ReportingPeriod option)
    {
        return option.getCode();
    }

    @Override
    public String getColumnLabel(ReportingPeriod option)
    {
        return MessageFormat.format(columnLabel, option.toString());
    }

    @Override
    public String getMenuLabel(ReportingPeriod option)
    {
        return option.toString();
    }

    @Override
    public String getDescription(ReportingPeriod option)
    {
        return null;
    }

    @Override
    public boolean canCreateNewOptions()
    {
        return true;
    }

    @Override
    public ReportingPeriod createNewOption(Shell shell)
    {
        ReportingPeriodDialog dialog = new ReportingPeriodDialog(shell, null, firstTransactionSupplier);
        if (dialog.open() == ReportingPeriodDialog.OK)
        {
            ReportingPeriod p = dialog.getReportingPeriod();
            defaultOptions.add(p);
            return p;
        }
        return null;
    }
}