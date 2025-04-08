package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.dom.TableElement;
import org.eclipse.swt.widgets.Table;

@SuppressWarnings("restriction")
public class TableElementAdapter extends TableElement implements ColumnViewerElement
{

    public TableElementAdapter(Table table, CSSEngine engine)
    {
        super(table, engine);
    }

    @Override
    public void setLinesVisible(boolean show)
    {
        ((Table) getControl()).setLinesVisible(show);
    }

    @Override
    public boolean isLinesVisible()
    {
        return ((Table) getControl()).getLinesVisible();
    }
}
