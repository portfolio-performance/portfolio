package name.abuchen.portfolio.ui.views;

import java.text.MessageFormat;

import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.ClientEditor;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.ToolBarDropdownMenu;

import org.eclipse.swt.widgets.ToolBar;

/* package */abstract class AbstractHistoricView extends AbstractFinanceView
{
    private String identifier;

    private final int numberOfYears;

    private int reportingPeriod;

    public AbstractHistoricView(int numberOfYears)
    {
        this(numberOfYears, 2);
    }

    public AbstractHistoricView(int numberOfYears, int defaultSelection)
    {
        this.numberOfYears = numberOfYears;
        this.reportingPeriod = defaultSelection + 1;

        identifier = this.getClass().getSimpleName() + "-REPORTING"; //$NON-NLS-1$
    }

    @Override
    public void init(ClientEditor clientEditor, Object parameter)
    {
        super.init(clientEditor, parameter);
        load();
    }

    @Override
    public void dispose()
    {
        getClientEditor().getPreferenceStore().setValue(identifier, String.valueOf(reportingPeriod) + 'Y');
        super.dispose();
    }

    private void load()
    {
        String config = getClientEditor().getPreferenceStore().getString(identifier);
        if (config == null || config.trim().length() == 0)
            return;

        this.reportingPeriod = Integer.parseInt(config.substring(0, config.length() - 1));
    }

    protected abstract void reportingPeriodUpdated();

    @Override
    protected void addButtons(final ToolBar toolBar)
    {
        final boolean[] active = new boolean[] { false };

        ToolBarDropdownMenu<Integer> menu = new ToolBarDropdownMenu<Integer>(toolBar)
        {
            @Override
            protected void itemSelected(Integer data)
            {
                if (!active[0])
                    return;
                reportingPeriod = data.intValue();
                reportingPeriodUpdated();
            }
        };

        for (int ii = 0; ii < numberOfYears; ii++)
            menu.add(Integer.valueOf(ii + 1), MessageFormat.format(Messages.LabelReportingYears, ii + 1));

        menu.select(Integer.valueOf(reportingPeriod));

        active[0] = true;
    }

    protected final int getReportingYears()
    {
        return reportingPeriod;
    }

}
