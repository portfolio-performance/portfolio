package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.dom.TreeElement;
import org.eclipse.swt.widgets.Tree;

@SuppressWarnings("restriction")
public class TreeElementAdapter extends TreeElement implements ColumnViewerElement
{

    public TreeElementAdapter(Tree tree, CSSEngine engine)
    {
        super(tree, engine);
    }

    @Override
    public void setLinesVisible(boolean show)
    {
        ((Tree) getControl()).setLinesVisible(show);
    }

    @Override
    public boolean isLinesVisible()
    {
        return ((Tree) getControl()).getLinesVisible();
    }
}
