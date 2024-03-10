package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.dom.CompositeElement;
import org.eclipse.swtchart.Chart;

@SuppressWarnings("restriction")
public class ChartSWTElementAdapter extends CompositeElement
{

    public ChartSWTElementAdapter(Chart chart, CSSEngine engine)
    {
        super(chart, engine);
    }

}
