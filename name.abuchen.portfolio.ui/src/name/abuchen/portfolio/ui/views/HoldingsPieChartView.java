package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.snapshot.AccountSnapshot;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.PieChart;
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

        List<SecurityPosition> positions = new ArrayList<SecurityPosition>();

        long cash = 0;
        for (AccountSnapshot a : snapshot.getAccounts())
            cash += a.getFunds();
        positions.add(new SecurityPosition(null, new SecurityPrice(snapshot.getTime(), cash), 1));

        positions.addAll(snapshot.getJointPortfolio().getPositions());

        Collections.sort(positions, new Comparator<SecurityPosition>()
        {
            @Override
            public int compare(SecurityPosition pos1, SecurityPosition pos2)
            {
                return -Long.valueOf(pos1.calculateValue()).compareTo(pos2.calculateValue());
            }
        });

        List<PieChart.Slice> slices = new ArrayList<PieChart.Slice>();

        for (SecurityPosition p : positions)
        {
            slices.add(new PieChart.Slice(p.calculateValue(), //
                            p.getSecurity() == null ? Security.AssetClass.CASH.toString() : p.getSecurity().getName(), //
                            p.getSecurity() == null ? Colors.CASH : Colors.valueOf(p.getSecurity().getType().name())));
        }

        canvas.setSlices(slices);

        canvas.redraw();
        
        return canvas;
    }
}
