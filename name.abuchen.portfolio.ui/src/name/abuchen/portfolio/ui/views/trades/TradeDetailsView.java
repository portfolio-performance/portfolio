package name.abuchen.portfolio.ui.views.trades;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.snapshot.trades.TradeCollector;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.TradesTableViewer;
import name.abuchen.portfolio.util.Interval;

public class TradeDetailsView extends AbstractFinanceView
{
    public static class Input
    {
        private final Interval interval;
        private final List<Trade> trades;

        public Input(Interval interval, List<Trade> trades)
        {
            this.interval = interval;
            this.trades = trades;
        }

        public Interval getInterval()
        {
            return interval;
        }

        public List<Trade> getTrades()
        {
            return trades;
        }
    }

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
    protected void addButtons(ToolBarManager toolBarManager)
    {
        DropDown dropDown = new DropDown(input.getInterval().toString(), Images.FILTER_ON, SWT.NONE);

        dropDown.setMenuListener(manager -> {
            manager.add(new SimpleAction(input.getInterval().toString(), a -> {
                table.setInput(input.getTrades());
                dropDown.setImage(Images.FILTER_ON);
            }));

            manager.add(new SimpleAction(Messages.LabelAllTrades, a -> {
                TradeCollector collector = new TradeCollector(getClient(), converter);
                List<Trade> trades = new ArrayList<>();
                getClient().getSecurities().forEach(s -> trades.addAll(collector.collect(s)));
                table.setInput(trades);
                dropDown.setImage(Images.FILTER_OFF);
            }));
        });

        toolBarManager.add(dropDown);

        super.addButtons(toolBarManager);
    }

    @Override
    protected Control createBody(Composite parent)
    {
        table = new TradesTableViewer(this);

        Control control = table.createViewControl(parent, TradesTableViewer.ViewMode.MULTIPLE_SECURITES);
        table.setInput(input.getTrades());

        return control;
    }
}
