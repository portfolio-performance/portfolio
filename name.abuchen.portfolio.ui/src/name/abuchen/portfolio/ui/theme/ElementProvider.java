package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.dom.IElementProvider;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.dom.definition.ColorDefinitionElement;
import org.eclipse.e4.ui.css.swt.dom.definition.FontDefinitionElement;
import org.eclipse.e4.ui.css.swt.dom.definition.ThemesExtensionElement;
import org.eclipse.e4.ui.internal.css.swt.definition.IColorDefinitionOverridable;
import org.eclipse.e4.ui.internal.css.swt.definition.IFontDefinitionOverridable;
import org.eclipse.e4.ui.internal.css.swt.definition.IThemesExtension;
import org.swtchart.Chart;
import org.w3c.dom.Element;

import name.abuchen.portfolio.ui.editor.Sidebar;

@SuppressWarnings("restriction")
public class ElementProvider implements IElementProvider
{
    @Override
    public Element getElement(Object element, CSSEngine engine)
    {
        if (element instanceof IFontDefinitionOverridable)
            return new FontDefinitionElement((IFontDefinitionOverridable) element, engine);
        if (element instanceof IColorDefinitionOverridable)
            return new ColorDefinitionElement((IColorDefinitionOverridable) element, engine);
        if (element instanceof IThemesExtension)
            return new ThemesExtensionElement((IThemesExtension) element, engine);
        if (element instanceof Sidebar)
            return new SidebarElementAdapter((Sidebar<?>) element, engine);
        if (element instanceof Chart)
            return new ChartElementAdapter((Chart) element, engine);

        return null;
    }

}
