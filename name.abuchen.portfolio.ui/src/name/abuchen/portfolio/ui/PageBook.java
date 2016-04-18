package name.abuchen.portfolio.ui;

import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class PageBook extends Composite
{
    private StackLayout layout;

    public PageBook(Composite parent, int style)
    {
        super(parent, style);

        this.layout = new StackLayout();
        setLayout(layout);
    }

    public void showPage(Control page)
    {
        if (page.isDisposed() || page.getParent() != this)
            return;

        layout.topControl = page;
        layout(true);
    }
}
