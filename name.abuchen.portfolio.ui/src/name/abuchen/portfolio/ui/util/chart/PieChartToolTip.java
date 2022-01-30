package name.abuchen.portfolio.ui.util.chart;

import java.text.DecimalFormat;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.model.Node;

public class PieChartToolTip extends AbstractSWTChartToolTip
{
    private DecimalFormat defaultValueFormat = new DecimalFormat("#,##0.00"); //$NON-NLS-1$
    private IToolTipBuilder tooltipBuilder;

    public interface IToolTipBuilder
    {
        void build(Composite container, Node currentNode);
    }

    public class ToolTipBuilder implements IToolTipBuilder
    {
        @Override
        public void build(Composite container, Node currentNode)
        {
            RowLayout layout = new RowLayout(SWT.VERTICAL);
            layout.center = true;
            container.setLayout(layout);
            Composite data = new Composite(container, SWT.NONE);
            GridLayoutFactory.swtDefaults().numColumns(2).applyTo(data);
            Label left = new Label(data, SWT.NONE);
            left.setText(currentNode.getId());
            Label right = new Label(data, SWT.NONE);
            right.setText(defaultValueFormat.format(currentNode.getValue()));
        }
    }

    public PieChartToolTip(Chart chart)
    {
        super(chart);
        setToolTipBuilder(new ToolTipBuilder());
    }

    public void setToolTipBuilder(IToolTipBuilder builder)
    {
        tooltipBuilder = builder;
    }

    @Override
    protected Object getFocusObjectAt(Event event)
    {
        return ((PieChart) getChart()).getNodeAt(event.x, event.y);
    }

    @Override
    protected void createComposite(Composite parent, Object focus)
    {
        if (focus == null)
            return;

        Node currentNode = (Node) focus;
        final Composite container = new Composite(parent, SWT.NONE);
        tooltipBuilder.build(container, currentNode);
    }

    @Override
    protected boolean isHoverMode()
    {
        return true;
    }

    @Override
    void onFocusChanged(Object newFocus)
    {
        // Update highlighted node
        getChart().redraw();
    }
}
