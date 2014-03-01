package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
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

    @Override
    protected String getTitle()
    {
        return Messages.LabelStatementOfAssetsHoldings;
    }

    @Override
    protected Control createBody(Composite parent)
    {
        canvas = new PieChart(parent, SWT.NONE);

        ClientSnapshot snapshot = ClientSnapshot.create(getClient(), Dates.today());

        List<PieChart.Slice> slices = new ArrayList<PieChart.Slice>();

        for (AccountSnapshot a : snapshot.getAccounts())
            slices.add(new PieChart.Slice(a.getFunds(), a.getAccount().getName(), null));

        for (SecurityPosition position : snapshot.getJointPortfolio().getPositions())
            slices.add(new PieChart.Slice(position.calculateValue(), position.getSecurity().getName(), null));

        Collections.sort(slices, new Slice.ByValue());

        ColorWheel colors = new ColorWheel(canvas, slices.size());
        for (int ii = 0; ii < slices.size(); ii++)
            slices.get(ii).setColor(colors.getSegment(ii).getColor());

        canvas.setSlices(slices);

        canvas.redraw();

        return canvas;
    }
}
