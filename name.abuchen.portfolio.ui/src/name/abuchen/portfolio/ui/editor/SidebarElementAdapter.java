package name.abuchen.portfolio.ui.editor;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.dom.CompositeElement;

@SuppressWarnings("restriction")
public class SidebarElementAdapter extends CompositeElement
{

    public SidebarElementAdapter(Sidebar<?> composite, CSSEngine engine)
    {
        super(composite, engine);
    }

}
