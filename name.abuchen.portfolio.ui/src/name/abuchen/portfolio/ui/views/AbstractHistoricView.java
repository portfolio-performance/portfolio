package name.abuchen.portfolio.ui.views;

import java.text.MessageFormat;

import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/* package */abstract class AbstractHistoricView extends AbstractFinanceView
{
    private Combo reportingPeriod;

    private final int numberOfYears;
    private final int defaultSelection;

    public AbstractHistoricView(int numberOfYears)
    {
        this(numberOfYears, 2);
    }

    public AbstractHistoricView(int numberOfYears, int defaultSelection)
    {
        this.numberOfYears = numberOfYears;
        this.defaultSelection = defaultSelection;
    }

    protected abstract Control buildBody(Composite parent);

    protected abstract void reportingPeriodUpdated();

    @Override
    protected final Control createBody(Composite parent)
    {
        return buildBody(parent);
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        reportingPeriod = new Combo(toolBar, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);

        for (int ii = 0; ii < numberOfYears; ii++)
            reportingPeriod.add(MessageFormat.format(Messages.LabelReportingYears, ii + 1));

        reportingPeriod.select(Math.min(defaultSelection, numberOfYears));
        reportingPeriod.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                reportingPeriodUpdated();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {}
        });
        reportingPeriod.pack();

        ToolItem toolItem = new ToolItem(toolBar, SWT.SEPARATOR);
        toolItem.setWidth(reportingPeriod.getSize().x);
        toolItem.setControl(reportingPeriod);
    }

    protected final int getReportingYears()
    {
        return reportingPeriod.getSelectionIndex() + 1;
    }

}
