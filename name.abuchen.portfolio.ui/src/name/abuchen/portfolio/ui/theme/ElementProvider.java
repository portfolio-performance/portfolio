package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.dom.IElementProvider;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.dom.definition.ColorDefinitionElement;
import org.eclipse.e4.ui.css.swt.dom.definition.FontDefinitionElement;
import org.eclipse.e4.ui.css.swt.dom.definition.ThemesExtensionElement;
import org.eclipse.e4.ui.internal.css.swt.definition.IColorDefinitionOverridable;
import org.eclipse.e4.ui.internal.css.swt.definition.IFontDefinitionOverridable;
import org.eclipse.e4.ui.internal.css.swt.definition.IThemesExtension;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swtchart.Chart;
import org.w3c.dom.Element;

import name.abuchen.portfolio.ui.editor.Sidebar;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.views.PortfolioBalanceChart;
import name.abuchen.portfolio.ui.views.SecuritiesChart;

@SuppressWarnings("restriction")
public class ElementProvider implements IElementProvider
{
    @Override
    public Element getElement(Object element, CSSEngine engine)
    {
        if (element instanceof IFontDefinitionOverridable fontDefinition)
            return new FontDefinitionElement(fontDefinition, engine);
        if (element instanceof IColorDefinitionOverridable colorDefinition)
            return new ColorDefinitionElement(colorDefinition, engine);
        if (element instanceof IThemesExtension themesExtension)
            return new ThemesExtensionElement(themesExtension, engine);
        if (element instanceof Sidebar)
            return new SidebarElementAdapter((Sidebar<?>) element, engine);
        if (element instanceof Chart chart)
            return new ChartElementAdapter(chart, engine);
        if (element instanceof Colors.Theme colorsTheme)
            return new ColorsThemeElementAdapter(colorsTheme, engine);
        if (element instanceof Table table)
            return new TableElementAdapter(table, engine);
        if (element instanceof Tree tree)
            return new TreeElementAdapter(tree, engine);
        if (element instanceof SecuritiesChart securitiesChart)
            return new SecuritiesChartElementAdapter(securitiesChart, engine);
        if (element instanceof PortfolioBalanceChart portfolioBalanceChart)
            return new PortfolioBalanceChartElementAdapter(portfolioBalanceChart, engine);

        return null;
    }

}
