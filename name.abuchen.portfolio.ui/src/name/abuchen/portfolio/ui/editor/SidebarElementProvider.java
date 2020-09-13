package name.abuchen.portfolio.ui.editor;

import org.eclipse.e4.ui.css.core.dom.IElementProvider;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.w3c.dom.Element;

@SuppressWarnings("restriction")
public class SidebarElementProvider implements IElementProvider
{

    @Override
    public Element getElement(Object element, CSSEngine engine)
    {
        if (element instanceof Sidebar)
            return new SidebarElementAdapter((Sidebar<?>) element, engine);

        return null;
    }

}
