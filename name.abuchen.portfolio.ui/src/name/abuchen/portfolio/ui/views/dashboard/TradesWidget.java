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
import name.abuchen.portfolio.snapshot.trades.TradeCollectorException;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.util.swt.StyledLabel;
import name.abuchen.portfolio.ui.views.trades.TradeDetailsView;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TextUtil;

public class TradesWidget extends WidgetDelegate<TradeDetailsView.Input>
{
    @Inject
    private PortfolioPart part;

    protected Label title;
    protected StyledLabel indicator;

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
        title.setText(TextUtil.tooltip(getWidget().getLabel()));
        title.setBackground(container.getBackground());
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(title);

        indicator = new StyledLabel(container, SWT.NONE);
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
                part.activateView(TradeDetailsView.class, getUpdateTask().get());
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
        this.title.setText(TextUtil.tooltip(getWidget().getLabel()));

        List<Trade> trades = input.getTrades();
        long positive = trades.stream().filter(t -> t.getIRR() > 0).count();
        String text = MessageFormat.format("{0} <green>↑{1}</green> <red>↓{2}</red>", //$NON-NLS-1$
                        trades.size(), positive, trades.size() - positive);

        this.indicator.setText(text);
    }

    @Override
    public Supplier<TradeDetailsView.Input> getUpdateTask()
    {
        return () -> {
            TradeCollector collector = new TradeCollector(getClient(), getDashboardData().getCurrencyConverter());

            List<Trade> trades = new ArrayList<>();
            List<TradeCollectorException> errors = new ArrayList<>();

            getClient().getSecurities().forEach(s -> {
                try
                {
                    trades.addAll(collector.collect(s));
                }
                catch (TradeCollectorException error)
                {
                    errors.add(error);
                }
            });

            Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());

            List<Trade> filteredTrades = trades.stream().filter(t -> t.getEnd().isPresent())
                            .filter(t -> interval.contains(t.getEnd().get())).collect(Collectors.toList());

            return new TradeDetailsView.Input(interval, filteredTrades, errors);
        };
    }
}
