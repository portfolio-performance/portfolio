package name.abuchen.portfolio.ui.views.panes;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public interface InformationPanePage
{
    String getLabel();

    Control createViewControl(Composite parent);

    void setInput(Object input);

    void onRecalculationNeeded();
}
