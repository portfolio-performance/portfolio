package name.abuchen.portfolio.ui.editor;

import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class PageBook extends Composite
{
    private StackLayout stackLayout;

    public PageBook(Composite parent, int style)
    {
        super(parent, style);

        this.stackLayout = new StackLayout();
        setLayout(stackLayout);
    }

    public void showPage(Control page)
    {
        if (page.isDisposed() || page.getParent() != this)
            return;

        stackLayout.topControl = page;
        layout(true);
    }
}
