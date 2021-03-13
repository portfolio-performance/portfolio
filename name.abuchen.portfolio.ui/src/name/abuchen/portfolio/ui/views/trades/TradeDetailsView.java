package name.abuchen.portfolio.ui.views.trades;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.snapshot.trades.TradeCollector;
import name.abuchen.portfolio.snapshot.trades.TradeCollectorException;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.views.SecurityContextMenu;
import name.abuchen.portfolio.ui.views.TradesTableViewer;
import name.abuchen.portfolio.util.Interval;

public class TradeDetailsView extends AbstractFinanceView
{
    public static class Input
    {
        private final Interval interval;
        private final List<Trade> trades;
        private final List<TradeCollectorException> errors;

        public Input(Interval interval, List<Trade> trades, List<TradeCollectorException> errors)
        {
            this.interval = interval;
            this.trades = trades;
            this.errors = errors;
        }

        public Interval getInterval()
        {
            return interval;
        }

        public List<Trade> getTrades()
        {
            return trades;
        }

        public List<TradeCollectorException> getErrors()
        {
            return errors;
        }
    }

    /*
     * We need a reference to the value in the buttons but the native Boolean
     * class is immutable.
     */
    private class MutableBoolean
    {
        private boolean value;

        public MutableBoolean(boolean value)
        {
            this.value = value;
        }

        public void setValue(boolean value)
        {
            this.value = value;
        }

        public void invert()
        {
            value = !value;
        }

        public boolean isTrue()
        {
            return value;
        }
    }

    private class FilterAction extends Action
    {
        private MutableBoolean criterion;

        private DropDown theMenu;
        private FilterAction counterpart;

        public FilterAction(String label, MutableBoolean criterion, DropDown theMenu)
        {
            super(label);
            this.criterion = criterion;
            this.theMenu = theMenu;
            this.setChecked(criterion.isTrue());
        }

        public void setCounterpart(FilterAction counterpart)
        {
            this.counterpart = counterpart;
        }

        @Override
        public void run()
        {
            criterion.invert();

            if (counterpart != null && criterion.isTrue())
            {
                counterpart.criterion.setValue(false);
                counterpart.setChecked(false);
            }

            update();
            updateFilterButtonImage(theMenu);
        }
    }

    private static final String ID_WARNING_TOOL_ITEM = "warning"; //$NON-NLS-1$

    private Input input;

    private CurrencyConverter converter;
    private TradesTableViewer table;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelTrades;
    }

    private MutableBoolean usePreselectedTrades = new MutableBoolean(false);

    private MutableBoolean onlyOpen = new MutableBoolean(false);
    private MutableBoolean onlyClosed = new MutableBoolean(false);

    private MutableBoolean onlyProfitable = new MutableBoolean(false);
    private MutableBoolean onlyLossMaking = new MutableBoolean(false);

    @Inject
    @Optional
    public void setTrades(@Named(UIConstants.Parameter.VIEW_PARAMETER) Input input)
    {
        this.input = input;
        this.usePreselectedTrades.setValue(true);
    }

    @PostConstruct
    protected void contruct(ExchangeRateProviderFactory factory)
    {
        converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
    }

    @Override
    public void notifyModelUpdated()
    {
        // the base currency might have changed
        this.converter = this.converter.with(getClient().getBaseCurrency());

        // only update the trades if it is *not* based on a pre-calculated set
        // of trades, for example when the user navigates from the dashboard to
        // the detailed trades
        if (input == null || !input.getTrades().equals(table.getInput()))
            update();

        if (!table.getTableViewer().getTable().isDisposed())
            table.getTableViewer().refresh(true);
    }

    @Override
    protected void addButtons(ToolBarManager toolBarManager)
    {
        addFilterButton(toolBarManager);

        toolBarManager.add(new DropDown(Messages.MenuExportData, Images.EXPORT, SWT.NONE,
                        manager -> manager.add(new SimpleAction(Messages.LabelTrades + " (CSV)", //$NON-NLS-1$
                                        a -> new TableViewerCSVExporter(table.getTableViewer())
                                                        .export(Messages.LabelTrades + ".csv"))) //$NON-NLS-1$
        ));

        toolBarManager.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE,
                        manager -> table.getShowHideColumnHelper().menuAboutToShow(manager)));
    }

    private void updateFilterButtonImage(DropDown dropDown)
    {
        boolean isOn = usePreselectedTrades.isTrue() || onlyOpen.isTrue() || onlyClosed.isTrue()
                        || onlyProfitable.isTrue() || onlyLossMaking.isTrue();
        dropDown.setImage(isOn ? Images.FILTER_ON : Images.FILTER_OFF);
    }

    private void addFilterButton(ToolBarManager manager)
    {
        boolean hasPreselectedTrades = input != null;

        DropDown filterDropDowMenu = new DropDown(Messages.MenuFilterTrades, Images.FILTER_OFF, SWT.NONE);
        updateFilterButtonImage(filterDropDowMenu);

        filterDropDowMenu.setMenuListener(mgr -> {

            if (hasPreselectedTrades)
            {
                mgr.add(new FilterAction(input.getInterval().toString(), usePreselectedTrades, filterDropDowMenu));
                mgr.add(new Separator());
            }

            FilterAction onlyOpenAction = new FilterAction(Messages.FilterOnlyOpenTrades, onlyOpen,
                            filterDropDowMenu);
            FilterAction onlyClosedAction = new FilterAction(Messages.FilterOnlyClosedTrades, onlyClosed,
                            filterDropDowMenu);

            onlyOpenAction.setCounterpart(onlyClosedAction);
            onlyClosedAction.setCounterpart(onlyOpenAction);

            FilterAction onlyProfitableAction = new FilterAction(Messages.FilterOnlyProfitableTrades, onlyProfitable,
                            filterDropDowMenu);
            FilterAction onlyLossMakingAction = new FilterAction(Messages.FilterOnlyLossMakingTrades, onlyLossMaking,
                            filterDropDowMenu);

            onlyProfitableAction.setCounterpart(onlyLossMakingAction);
            onlyLossMakingAction.setCounterpart(onlyProfitableAction);

            mgr.add(onlyOpenAction);
            mgr.add(onlyClosedAction);
            mgr.add(new Separator());
            mgr.add(onlyProfitableAction);
            mgr.add(onlyLossMakingAction);
        });

        manager.add(filterDropDowMenu);
    }

    @Override
    protected Control createBody(Composite parent)
    {
        table = new TradesTableViewer(this);

        Control control = table.createViewControl(parent, TradesTableViewer.ViewMode.MULTIPLE_SECURITES);

        update();

        new ContextMenu(table.getTableViewer().getControl(), this::fillContextMenu).hook();

        return control;
    }

    private void fillContextMenu(IMenuManager manager)
    {
        IStructuredSelection selection = table.getTableViewer().getStructuredSelection();

        if (selection.isEmpty() || selection.size() > 1)
            return;

        Trade trade = (Trade) selection.getFirstElement();
        new SecurityContextMenu(this).menuAboutToShow(manager, trade.getSecurity(), trade.getPortfolio());
    }

    private void update()
    {
        Input data = usePreselectedTrades.isTrue() ? input : collectAllTrades();

        Stream<Trade> filteredTrades = data.getTrades().stream();

        if (onlyClosed.isTrue())
            filteredTrades = filteredTrades.filter(Trade::isClosed);
        if (onlyOpen.isTrue())
            filteredTrades = filteredTrades.filter(t -> !t.isClosed());
        if (onlyLossMaking.isTrue())
            filteredTrades = filteredTrades.filter(Trade::isLoss);
        if (onlyProfitable.isTrue())
            filteredTrades = filteredTrades.filter(t -> !t.isLoss());

        table.setInput(filteredTrades.collect(Collectors.toList()));

        ToolBarManager toolBar = getToolBarManager();

        if (!data.getErrors().isEmpty())
        {
            if (toolBar.find(ID_WARNING_TOOL_ITEM) == null)
            {
                Action warning = new SimpleAction(Messages.MsgErrorTradeCollectionWithErrors,
                                Images.ERROR_NOTICE.descriptor(),
                                a -> MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError,
                                                data.getErrors().stream().map(TradeCollectorException::getMessage)
                                                                .collect(Collectors.joining("\n\n")))); //$NON-NLS-1$
                warning.setId(ID_WARNING_TOOL_ITEM);
                toolBar.insert(0, new ActionContributionItem(warning));
                toolBar.update(true);
            }
        }
        else
        {
            if (toolBar.remove(ID_WARNING_TOOL_ITEM) != null)
                toolBar.update(true);
        }
    }

    private Input collectAllTrades()
    {
        TradeCollector collector = new TradeCollector(getClient(), converter);
        List<Trade> trades = new ArrayList<>();
        List<TradeCollectorException> errors = new ArrayList<>();
        getClient().getSecurities().forEach(s -> {
            try
            {
                trades.addAll(collector.collect(s));
            }
            catch (TradeCollectorException e)
            {
                errors.add(e);
            }
        });

        return new Input(null, trades, errors);
    }
}
