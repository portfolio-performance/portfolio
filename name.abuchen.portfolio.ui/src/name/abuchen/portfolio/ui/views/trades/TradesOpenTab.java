package name.abuchen.portfolio.ui.views.trades;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.views.TradesTableViewer;

public class TradesOpenTab extends AbstractFinanceView implements TradeTab
{
    @Inject
    private TradeViewModel model;

    private TradesTableViewer table;
    private TableViewer tableViewer;

    @Override
    public String getLabel()
    {
        return Messages.LabelTradesOpen;
    }

    @Override
    public void addExportActions(IMenuManager manager)
    {
        manager.add(new Action(MessageFormat.format(Messages.LabelExport, Messages.LabelTradesOpen))
        {
            @Override
            public void run()
            {
                new TableViewerCSVExporter(tableViewer).export(Messages.LabelTradesOpen + ".csv"); //$NON-NLS-1$
            }
        });
    }

    @Override
    public Control createControl(Composite parent)
    {
        table = new TradesTableViewer(this);
        Control control = table.createViewControl(parent, TradesTableViewer.ViewMode.MULTIPLE_SECURITES);
        table.setInput(model.getTradesOpen());
        model.addUpdateListener(() -> table.setInput(model.getTradesOpen()));
        return control;
    }

    public void setInput(List<Trade> trades)
    {
        this.tableViewer.setInput(trades);
    }

    public TableViewer getTableViewer()
    {
        return tableViewer;
    }

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelTradesOpen;
    }

    @Override
    protected Control createBody(Composite parent)
    {
        return null;
    }
}
