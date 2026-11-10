package name.abuchen.portfolio.ui.views.panes;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.security.SnapshotCache;
import name.abuchen.portfolio.snapshot.trades.TradeCollector;
import name.abuchen.portfolio.snapshot.trades.TradeCollectorException;
import name.abuchen.portfolio.snapshot.trades.TradeGrouping;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.views.TradesTableViewer;

public class TradesPane implements InformationPanePage
{
    private static final String PREF_TRADE_GROUPING = TradesPane.class.getSimpleName() + "-tradeGrouping"; //$NON-NLS-1$

    @Inject
    @Named(UIConstants.Context.ACTIVE_CLIENT)
    private Client client;

    @Inject
    private AbstractFinanceView view;

    @Inject
    private ExchangeRateProviderFactory factory;

    private TradesTableViewer trades;

    private Security source;

    private TradeGrouping grouping;

    @Override
    public String getLabel()
    {
        return Messages.SecurityTabTrades;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        trades = new TradesTableViewer(view);
        return trades.createViewControl(parent, TradesTableViewer.ViewMode.SINGLE_SECURITY);
    }

    @Override
    public void addButtons(ToolBarManager toolBar)
    {
        toolBar.add(new SimpleAction(Messages.MenuExportData, Images.EXPORT,
                        a -> new TableViewerCSVExporter(trades.getTableViewer()).export(getLabel(), source)));

        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE, manager -> {
            trades.getShowHideColumnHelper().menuAboutToShow(manager);

            manager.add(new Separator());
            manager.add(new LabelOnly(Messages.LabelTradeGrouping));

            for (TradeGrouping tradeGrouping : TradeGrouping.values())
            {
                var action = new SimpleAction(tradeGrouping.getLabel(), a -> {
                    grouping = tradeGrouping;
                    view.getPreferenceStore().setValue(PREF_TRADE_GROUPING, tradeGrouping.name());
                    onRecalculationNeeded();
                });
                action.setChecked(tradeGrouping == getGrouping());
                manager.add(action);
            }
        }));
    }

    private TradeGrouping getGrouping()
    {
        if (grouping == null)
            grouping = TradeGrouping.fromString(view.getPreferenceStore().getString(PREF_TRADE_GROUPING));

        return grouping;
    }

    @Override
    public void setInput(Object input)
    {
        source = Adaptor.adapt(Security.class, input);

        if (trades == null)
            return;

        if (source != null)
        {

            try
            {
                CurrencyConverter converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());

                var collector = new TradeCollector(client, converter, getGrouping(), new SnapshotCache());

                trades.setInput(collector.collect(source));
            }
            catch (TradeCollectorException e)
            {
                // for now: do not show any trades (or rather: remove the
                // existing trades from the previous security). Need to
                // think of a better way to show the error message in the
                // GUI
                PortfolioPlugin.log(e);
                trades.setInput(null);
            }
        }
        else
        {
            trades.setInput(null);
        }

    }

    @Override
    public void onRecalculationNeeded()
    {
        if (source != null)
            setInput(source);
    }
}
