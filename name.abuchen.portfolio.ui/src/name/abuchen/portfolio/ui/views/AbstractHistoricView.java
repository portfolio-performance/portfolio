package name.abuchen.portfolio.ui.views;

import java.text.MessageFormat;

import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/* package */abstract class AbstractHistoricView extends AbstractFinanceView
{
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
    }

    protected abstract void reportingPeriodUpdated();

    @Override
    protected void addButtons(final ToolBar toolBar)
    {
        final ToolItem item = new ToolItem(toolBar, SWT.DROP_DOWN);
        item.setText(MessageFormat.format(Messages.LabelReportingYears, reportingPeriod));

        final Menu menu = new Menu(toolBar.getShell(), SWT.POP_UP);
        for (int ii = 0; ii < numberOfYears; ii++)
        {
            MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
            menuItem.setText(MessageFormat.format(Messages.LabelReportingYears, ii + 1));
            menuItem.setData(Integer.valueOf(ii + 1));
            menuItem.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent event)
                {
                    MenuItem selected = (MenuItem) event.widget;
                    item.setText(selected.getText());

                    // tool bar is packed & right-aligned
                    // if the "new" text is longer, the item will disappear
                    toolBar.getParent().layout();

                    reportingPeriod = ((Integer) selected.getData()).intValue();
                    reportingPeriodUpdated();
                }
            });
        }

        item.addListener(SWT.Selection, new Listener()
        {
            public void handleEvent(Event event)
            {
                Rectangle rect = item.getBounds();
                Point pt = new Point(rect.x, rect.y + rect.height);
                pt = toolBar.toDisplay(pt);
                menu.setLocation(pt.x, pt.y);
                menu.setVisible(true);
            }
        });

        toolBar.addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                if (!menu.isDisposed())
                    menu.dispose();
            }
        });
    }

    protected final int getReportingYears()
    {
        return reportingPeriod;
    }

}
