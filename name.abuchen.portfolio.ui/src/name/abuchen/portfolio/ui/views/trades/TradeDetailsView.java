package name.abuchen.portfolio.ui.views.trades;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
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
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
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

    private static final String ID_WARNING_TOOL_ITEM = "warning"; //$NON-NLS-1$

    private Input input;

    private CurrencyConverter converter;
    private TradesTableViewer table;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelTrades;
    }

    @Inject
    @Optional
    public void setTrades(@Named(UIConstants.Parameter.VIEW_PARAMETER) Input input)
    {
        this.input = input;
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
        if (table.getInput() != input.getTrades())
            updateFrom(collectAllTrades());

        if (!table.getTableViewer().getTable().isDisposed())
            table.getTableViewer().refresh(true);
    }

    @Override
    protected void addButtons(ToolBarManager toolBarManager)
    {
        boolean hasPreselectedTrades = input != null;
        if (hasPreselectedTrades)
        {
            DropDown dropDown = new DropDown(input.getInterval().toString(), Images.FILTER_ON, SWT.NONE);

            dropDown.setMenuListener(manager -> {
                manager.add(new SimpleAction(input.getInterval().toString(), a -> {
                    updateFrom(input);
                    dropDown.setImage(Images.FILTER_ON);
                }));

                manager.add(new SimpleAction(Messages.LabelAllTrades, a -> {
                    updateFrom(collectAllTrades());
                    dropDown.setImage(Images.FILTER_OFF);
                }));
            });

            toolBarManager.add(dropDown);
        }

        toolBarManager.add(new DropDown(Messages.MenuExportData, Images.EXPORT, SWT.NONE, manager -> {
            manager.add(new SimpleAction(Messages.LabelTrades + " (CSV)", //$NON-NLS-1$
                            a -> new TableViewerCSVExporter(table.getTableViewer())
                                            .export(Messages.LabelTrades + ".csv"))); //$NON-NLS-1$
        }));

        toolBarManager.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE,
                        manager -> table.getShowHideColumnHelper().menuAboutToShow(manager)));
    }

    @Override
    protected Control createBody(Composite parent)
    {
        table = new TradesTableViewer(this);

        Control control = table.createViewControl(parent, TradesTableViewer.ViewMode.MULTIPLE_SECURITES);

        updateFrom(input != null ? input : collectAllTrades());

        return control;
    }

    private void updateFrom(Input data)
    {
        table.setInput(data.getTrades());

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
