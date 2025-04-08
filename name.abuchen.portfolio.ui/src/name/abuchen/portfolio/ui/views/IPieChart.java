package name.abuchen.portfolio.ui.views;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.snapshot.ClientSnapshot;

public interface IPieChart
{
    Control createControl(Composite parent);

    void refresh(ClientSnapshot snapshot);
}
