package name.abuchen.portfolio.ui.wizards.sync;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.OnlineState;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;

public class OnlinePropertyColumn extends Column
{
    public static class NameColumnLabelProvider extends ColumnLabelProvider
    {
        private final OnlineState.Property property;

        public NameColumnLabelProvider(OnlineState.Property property)
        {
            this.property = property;
        }

        @Override
        public String getText(Object e)
        {
            OnlineProperty p = ((SecurityDecorator) e).getProperty(property);
            return p.getValue();
        }

        @Override
        public Color getBackground(Object e)
        {
            OnlineProperty p = ((SecurityDecorator) e).getProperty(property);

            if (p.isModified())
                return Display.getDefault().getSystemColor(SWT.COLOR_GREEN);
            else if (p.getSuggestedValue() != null && !p.getSuggestedValue().isEmpty()
                            && !p.getSuggestedValue().equals(p.getOriginalValue()))
                return Display.getDefault().getSystemColor(SWT.COLOR_YELLOW);
            else
                return null;
        }
    }

    private final OnlineState.Property property;

    public OnlinePropertyColumn(OnlineState.Property property, int defaultWidth)
    {
        super(property.name(), property.getLabel(), SWT.LEFT, defaultWidth);

        this.property = property;

        setLabelProvider(new NameColumnLabelProvider(property));
        setSorter(ColumnViewerSorter.create(e -> ((SecurityDecorator) e).getProperty(property).getValue()));
    }

    public OnlineState.Property getProperty()
    {
        return property;
    }
}
