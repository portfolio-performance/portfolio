package name.abuchen.portfolio.ui.views;

import java.text.MessageFormat;

import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

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
        Composite top = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().spacing(1, 1).applyTo(top);

        Composite buttonBar = new Composite(top, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(buttonBar);
        buttonBar.setLayout(new RowLayout());
        addButtons(buttonBar);

        Control body = buildBody(top);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(body);
        return top;
    }

    protected void addButtons(Composite buttonBar)
    {
        reportingPeriod = new Combo(buttonBar, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);

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
    }

    protected final int getReportingYears()
    {
        return reportingPeriod.getSelectionIndex() + 1;
    }

}
