package name.abuchen.portfolio.ui.views.panes;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.SecurityPriceDialog;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.DateEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.DateLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.ValueEditingSupport;
import name.abuchen.portfolio.ui.views.QuotesContextMenu;
import name.abuchen.portfolio.util.TradeCalendar;
import name.abuchen.portfolio.util.TradeCalendarManager;

public class HistoricalPricesPane implements InformationPanePage
{
    @Inject
    private Client client;

    @Inject
    private IPreferenceStore preferences;

    @Inject
    private AbstractFinanceView view;

    private Security security;
    private TableViewer prices;

    @Override
    public String getLabel()
    {
        return Messages.SecurityTabHistoricalQuotes;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        return createPricesTable(parent);
    }

    @Override
    public void addButtons(ToolBarManager toolBar)
    {
        toolBar.add(new SimpleAction(Messages.MenuExportData, Images.EXPORT,
                        a -> new TableViewerCSVExporter(prices).export(getLabel(), security)));
    }

    @Override
    public void setInput(Object input)
    {
        security = Adaptor.adapt(Security.class, input);
        prices.setInput(security != null ? security.getPrices() : Collections.emptyList());
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (security != null)
            prices.setInput(security.getPrices());
    }

    protected Composite createPricesTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        prices = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL);
        ColumnEditingSupport.prepare(prices);
        CopyPasteSupport.enableFor(prices);
        ShowHideColumnHelper support = new ShowHideColumnHelper(HistoricalPricesPane.class.getSimpleName(), preferences,
                        prices, layout);

        prices.setUseHashlookup(true);

        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new DateLabelProvider(e -> ((SecurityPrice) e).getDate())
        {
            @Override
            public Color getBackground(Object element)
            {
                SecurityPrice current = (SecurityPrice) element;
                List<?> all = (List<?>) prices.getInput();
                int index = all.indexOf(current);

                if (index == 0)
                    return null;

                SecurityPrice previous = (SecurityPrice) all.get(index - 1);

                TradeCalendar calendar = TradeCalendarManager.getInstance(security);

                boolean hasMissing = false;
                LocalDate date = current.getDate().minusDays(1);
                while (!hasMissing && date.isAfter(previous.getDate()))
                {
                    hasMissing = !calendar.isHoliday(date);
                    date = date.minusDays(1);
                }

                return hasMissing ? Colors.theme().warningBackground() : null;
            }
        });
        ColumnViewerSorter.create(SecurityPrice.class, "date").attachTo(column, SWT.DOWN); //$NON-NLS-1$
        new DateEditingSupport(SecurityPrice.class, "date") //$NON-NLS-1$
                        .addListener((e, o, n) -> {
                            SecurityPrice price = (SecurityPrice) e;
                            security.removePrice(price);
                            security.addPrice(price);

                            client.markDirty();
                        }).attachTo(column);
        support.addColumn(column);

        column = new Column(Messages.ColumnQuote, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                SecurityPrice price = (SecurityPrice) element;
                return Values.Quote.format(security.getCurrencyCode(), price.getValue(), client.getBaseCurrency());
            }
        });
        ColumnViewerSorter.create(SecurityPrice.class, "value").attachTo(column); //$NON-NLS-1$
        new ValueEditingSupport(SecurityPrice.class, "value", Values.Quote, number -> number.longValue() != 0) //$NON-NLS-1$
                        .addListener((e, o, n) -> client.markDirty()).attachTo(column);
        support.addColumn(column);

        support.createColumns();

        prices.getTable().setHeaderVisible(true);
        prices.getTable().setLinesVisible(true);

        prices.setContentProvider(ArrayContentProvider.getInstance());

        new ContextMenu(prices.getTable(), this::fillPricesContextMenu).hook();

        return container;
    }

    private void fillPricesContextMenu(IMenuManager manager)
    {
        if (security != null)
        {
            manager.add(new SimpleAction(Messages.SecurityMenuAddPrice, a -> {
                Dialog dialog = new SecurityPriceDialog(Display.getDefault().getActiveShell(), client, security);

                if (dialog.open() != Window.OK)
                    return;

                client.markDirty();
            }));
            manager.add(new Separator());
        }

        if (((IStructuredSelection) prices.getSelection()).getFirstElement() != null)
        {
            manager.add(new Action(Messages.SecurityMenuDeletePrice)
            {
                @Override
                public void run()
                {
                    if (security == null)
                        return;

                    Iterator<?> iter = ((IStructuredSelection) prices.getSelection()).iterator();
                    while (iter.hasNext())
                    {
                        SecurityPrice price = (SecurityPrice) iter.next();
                        if (price == null)
                            continue;

                        security.removePrice(price);
                    }

                    client.markDirty();
                }
            });
        }

        if (prices.getTable().getItemCount() > 0)
        {
            manager.add(new Action(Messages.SecurityMenuDeleteAllPrices)
            {
                @Override
                public void run()
                {
                    if (security == null)
                        return;

                    security.removeAllPrices();

                    client.markDirty();
                }
            });
        }

        if (security != null)
        {
            manager.add(new Separator());
            new QuotesContextMenu(view).menuAboutToShow(manager, security);
        }
    }
}
