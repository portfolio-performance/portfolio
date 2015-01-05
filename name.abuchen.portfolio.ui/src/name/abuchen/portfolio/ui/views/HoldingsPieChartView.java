package name.abuchen.portfolio.ui.views;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.PieChart;
import name.abuchen.portfolio.ui.util.PieChart.Slice;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class HoldingsPieChartView extends AbstractFinanceView
{
    private PieChart canvas;

    @Inject
    private ExchangeRateProviderFactory factory;

    @Override
    protected String getTitle()
    {
        return Messages.LabelStatementOfAssetsHoldings;
    }

    @Override
    protected Control createBody(Composite parent)
    {
        canvas = new PieChart(parent, SWT.NONE);

        CurrencyConverter converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
        ClientSnapshot snapshot = ClientSnapshot.create(getClient(), converter, Dates.today());

        List<PieChart.Slice> slices = snapshot.getPositionsByVehicle().values().stream()
                        .map(p -> new PieChart.Slice(p.getValuation().getAmount(), p.getInvestmentVehicle().getName()))
                        .collect(Collectors.toList());

        Collections.sort(slices, new Slice.ByValue());

        ColorWheel colors = new ColorWheel(canvas, slices.size());
        for (int ii = 0; ii < slices.size(); ii++)
            slices.get(ii).setColor(colors.getSegment(ii).getColor());

        canvas.setSlices(slices);

        canvas.redraw();

        return canvas;
    }
}
