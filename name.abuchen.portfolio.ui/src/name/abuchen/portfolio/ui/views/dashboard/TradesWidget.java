package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.snapshot.trades.TradeCollector;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.views.trades.TradeDetailsView;
import name.abuchen.portfolio.util.Interval;

public class TradesWidget extends WidgetDelegate<TradeDetailsView.Input>
{
    @Inject
    private PortfolioPart part;

    protected Label title;
    protected Label indicator;

    public TradesWidget(Widget widget, DashboardData dashboardData)
    {
        super(widget, dashboardData);

        addConfig(new ReportingPeriodConfig(this));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(parent.getBackground());
        GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(container);

        title = new Label(container, SWT.NONE);
        title.setText(getWidget().getLabel());
        title.setBackground(container.getBackground());
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(title);

        indicator = new Label(container, SWT.NONE);
        indicator.setFont(resources.getKpiFont());
        indicator.setBackground(container.getBackground());
        indicator.setText(""); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(indicator);

        ImageHyperlink button = new ImageHyperlink(container, SWT.NONE);
        button.setImage(Images.VIEW_SHARE.image());
        button.addHyperlinkListener(new HyperlinkAdapter()
        {
            @Override
            public void linkActivated(HyperlinkEvent e)
            {
                part.activateView("trades.TradeDetails", null, getUpdateTask().get()); //$NON-NLS-1$
            }
        });

        return container;
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    @Override
    public void update(TradeDetailsView.Input input)
    {
        this.title.setText(getWidget().getLabel());

        List<Trade> trades = input.getTrades();
        long positive = trades.stream().filter(t -> t.getIRR() > 0).count();
        this.indicator.setText(
                        MessageFormat.format("{0}   ↑{1} ↓{2}", trades.size(), positive, trades.size() - positive)); //$NON-NLS-1$
    }

    @Override
    public Supplier<TradeDetailsView.Input> getUpdateTask()
    {
        return () -> {
            TradeCollector collector = new TradeCollector(getClient(), getDashboardData().getCurrencyConverter());

            List<Trade> trades = new ArrayList<>();

            getClient().getSecurities().forEach(s -> trades.addAll(collector.collect(s)));

            Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());

            List<Trade> filteredTrades = trades.stream().filter(t -> t.getEnd().isPresent())
                            .filter(t -> interval.contains(t.getEnd().get())).collect(Collectors.toList());

            return new TradeDetailsView.Input(interval, filteredTrades);
        };
    }
}
