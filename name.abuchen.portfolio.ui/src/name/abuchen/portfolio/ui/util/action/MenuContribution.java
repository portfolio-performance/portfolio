package name.abuchen.portfolio.ui.util.action;

import java.util.Objects;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.util.TextUtil;

/**
 * A specialized {@link ContributionItem} for adding menu entries to a
 * {@code MenuManager}.
 * <p>
 * This class is used instead of directly adding {@code Action} objects to a
 * {@code MenuManager} because Eclipse JFace interprets the '@' character as a
 * separator for accelerator keys. Since menu labels in Portfolio Performance
 * may contain user-created text (view names, bookmarks, classifications,
 * taxonomies) that include '@' characters, this class bypasses that special
 * character handling.
 */
public class MenuContribution extends ContributionItem
{
    private final String label;
    private final Images image;
    private final Runnable runnable;
    private final int style;
    private final boolean isChecked;

    private MenuItem menuItem;

    /**
     * Creates a disabled push-button menu item with the given label.
     */
    public MenuContribution(String label)
    {
        this(label, null, null, SWT.PUSH, false);
    }

    /**
     * Creates a push-button menu item with the given label and action.
     */
    public MenuContribution(String label, Runnable runnable)
    {
        this(label, null, runnable, SWT.PUSH, false);
    }

    /**
     * Creates a disabled push-button menu item with the given label and icon.
     */
    public MenuContribution(String label, Images image)
    {
        this(label, image, null, SWT.PUSH, false);
    }

    /**
     * Creates a checkable menu item with the given label, action, and initial
     * checked state.
     */
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
    public void fill(Composite parent)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void fill(ToolBar parent, int index)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void fill(CoolBar parent, int index)
    {
        throw new UnsupportedOperationException();
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
