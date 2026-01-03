package name.abuchen.portfolio.ui.util.action;

import java.util.Objects;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.util.TextUtil;

public class MenuContribution extends ContributionItem
{
    private final String label;
    private final Images image;
    private final Runnable runnable;
    private final int style;
    private final boolean isChecked;

    private MenuItem menuItem;

    public MenuContribution(String label)
    {
        this(label, null, null, SWT.PUSH, false);
    }

    public MenuContribution(String label, Runnable runnable)
    {
        this(label, null, runnable, SWT.PUSH, false);
    }

    public MenuContribution(String label, Images image)
    {
        this(label, image, null, SWT.PUSH, false);
    }

    public MenuContribution(String label, Runnable runnable, boolean isChecked)
    {
        this(label, null, runnable, SWT.CHECK, isChecked);
    }

    private MenuContribution(String label, Images image, Runnable runnable, int style, boolean isChecked)
    {
        this.label = Objects.requireNonNull(label);
        this.image = image;
        this.runnable = runnable;
        this.style = style;
        this.isChecked = isChecked;
    }

    @Override
    public void fill(Menu menu, int index)
    {
        if (menuItem != null || menu == null)
            return;

        if (index >= 0)
            menuItem = new MenuItem(menu, style, index);
        else
            menuItem = new MenuItem(menu, style);

        menuItem.setText(TextUtil.tooltip(label));

        if (image != null)
            menuItem.setImage(image.image());

        if (style == SWT.CHECK)
            menuItem.setSelection(isChecked);

        if (runnable != null)
            menuItem.addListener(SWT.Selection, event -> runnable.run());
        else
            menuItem.setEnabled(false);

        menuItem.addListener(SWT.Dispose, event -> {
            if (event.widget == menuItem)
            {
                menuItem = null;
            }
        });
    }

    @Override
    public void dispose()
    {
        if (menuItem != null && !menuItem.isDisposed())
        {
            menuItem.dispose();
            menuItem = null;
        }
    }

}
