package name.abuchen.portfolio.ui.util.viewers;

import java.util.Optional;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

import name.abuchen.portfolio.snapshot.trail.Trail;
import name.abuchen.portfolio.ui.util.MoneyTrailDataSource;
import name.abuchen.portfolio.ui.util.TabularDataSource;

public class ToolTipCustomProviderSupport extends ColumnViewerToolTipSupport
{
    public interface TabularDataSourceProvider
    {
        Optional<TabularDataSource> getSource();
    }

    private ColumnViewer viewer;

    protected ToolTipCustomProviderSupport(ColumnViewer viewer, int style)
    {
        super(viewer, style, false);
        this.viewer = viewer;
    }

    public static final void enableFor(ColumnViewer viewer, int style)
    {
        new ToolTipCustomProviderSupport(viewer, style);
    }

    @Override
    protected boolean shouldCreateToolTip(Event event)
    {
        boolean answer = super.shouldCreateToolTip(event);

        if (answer)
            return true;

        // check if column has tooltip provider
        return Column.lookup(event.widget, viewer.getCell(new Point(event.x, event.y))) //
                        .map(Column::getToolTipProvider).isPresent();
    }

    @Override
    protected Composite createViewerToolTipContentArea(Event event, ViewerCell cell, Composite parent)
    {
        if (cell != null)
            return super.createViewerToolTipContentArea(event, cell, parent);

        cell = viewer.getCell(new Point(event.x, event.y));
        if (cell == null)
            return super.createViewerToolTipContentArea(event, cell, parent);

        var toolTipProvider = Column.lookup(event.widget, cell).map(Column::getToolTipProvider).orElseGet(() -> null);

        if (toolTipProvider != null)
        {
            int columnIndex = cell.getColumnIndex();

            Object option = null;

            if (cell.getControl() instanceof Table table)
                option = table.getColumn(columnIndex).getData(ShowHideColumnHelper.OPTIONS_KEY);
            else if (cell.getControl() instanceof Tree tree)
                option = tree.getColumn(columnIndex).getData(ShowHideColumnHelper.OPTIONS_KEY);

            Object toolTip = toolTipProvider.apply(cell.getElement(), option);
            if (toolTip != null)
                return buildToolTip(toolTip, parent);
        }

        return super.createViewerToolTipContentArea(event, cell, parent);
    }

    @Override
    public boolean isHideOnMouseDown()
    {
        return false;
    }

    private Composite buildToolTip(Object toolTip, Composite parent)
    {
        if (toolTip instanceof Optional<?> optional)
            toolTip = optional.orElse(null);

        if (toolTip instanceof String s)
        {
            Composite composite = new Composite(parent, SWT.NONE);
            composite.setLayout(new FillLayout());
            Label label = new Label(composite, SWT.NONE);
            label.setText(s);
            return composite;
        }
        else if (toolTip instanceof TabularDataSource source)
        {
            return source.createPlainComposite(parent);
        }
        else if (toolTip instanceof Trail trail)
        {
            return new MoneyTrailDataSource(trail).createPlainComposite(parent);
        }
        else if (toolTip instanceof MoneyTrailDataSource source)
        {
            return source.createPlainComposite(parent);
        }
        else
        {
            return null;
        }
    }
}
