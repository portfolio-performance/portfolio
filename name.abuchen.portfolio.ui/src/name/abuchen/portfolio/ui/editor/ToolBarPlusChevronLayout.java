package name.abuchen.portfolio.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;

/**
 * Display a chevron menu that contains the not visible toolbar items.
 */
/* package */ class ToolBarPlusChevronLayout extends Layout implements IMenuListener
{
    private int alignment = SWT.LEFT;
    private ImageHyperlink chevron;
    private Menu chevronMenu;

    private List<ContributionItem> invisible = new ArrayList<>();

    public ToolBarPlusChevronLayout(Composite host, int alignment)
    {
        if (alignment == SWT.RIGHT)
            this.alignment = SWT.RIGHT;

        this.chevron = new ImageHyperlink(host, SWT.PUSH);
        this.chevron.setImage(Images.CHEVRON.image());
        this.chevron.addHyperlinkListener(new HyperlinkAdapter()
        {
            @Override
            public void linkActivated(HyperlinkEvent e)
            {
                ImageHyperlink item = (ImageHyperlink) e.widget;

                if (chevronMenu == null)
                {
                    MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
                    menuMgr.setRemoveAllWhenShown(true);
                    menuMgr.addMenuListener(ToolBarPlusChevronLayout.this);

                    chevronMenu = menuMgr.createContextMenu(item.getParent());
                }

                Rectangle rect = item.getBounds();
                Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));

                chevronMenu.setLocation(pt.x, pt.y + rect.height);
                chevronMenu.setVisible(true);

                item.addDisposeListener(event -> chevronMenu.dispose());
            }
        });
    }

    @Override
    protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache)
    {
        ToolBar toolBar = getToolBar(composite);

        // return the preferred size given by the toolbar only
        return toolBar.computeSize(wHint, hHint, flushCache);
    }

    @Override
    protected void layout(Composite composite, boolean flushCache)
    {
        ToolBar toolBar = getToolBar(composite);

        Rectangle availableBounds = composite.getBounds();
        Point chevronSize = this.chevron.computeSize(SWT.DEFAULT, SWT.DEFAULT);

        // check which toolbar items are visible

        invisible = new ArrayList<>();

        int width = 0;

        ToolItem[] items = toolBar.getItems();
        for (int index = 0; index < items.length; index++)
        {
            ToolItem ti = items[index];
            Rectangle itemBounds = ti.getBounds();

            // tool items are not visible anymore if
            // a) there is no width available for the current item or
            // b) if the current item uses up the space for the chevron that
            // will have to be shown for the next item

            if ((width + itemBounds.width > availableBounds.width) // a
                            || ((index + 1 < items.length) && // b
                                            (width + itemBounds.width + chevronSize.x > availableBounds.width)))
            {
                // the tool item is not visible anymore
                for (int jj = index; jj < items.length; jj++)
                    invisible.add((ContributionItem) items[jj].getData());
                break;
            }
            else
            {
                width += itemBounds.width;
            }
        }

        if (invisible.isEmpty())
        {
            if (chevron.isVisible())
                chevron.setVisible(false);
        }
        else
        {
            chevron.setBounds(availableBounds.width - chevronSize.x, (availableBounds.height - chevronSize.y) / 2,
                            chevronSize.x, chevronSize.y);

            if (!chevron.isVisible())
                chevron.setVisible(true);

            availableBounds.width -= chevronSize.x;
        }

        if (alignment == SWT.LEFT)
            toolBar.setBounds(0, 0, width, availableBounds.height);
        else
            toolBar.setBounds(availableBounds.width - width, 0, width, availableBounds.height);
    }

    private ToolBar getToolBar(Composite composite)
    {
        ToolBar toolBar = null;
        for (Control child : composite.getChildren())
        {
            if (child instanceof ToolBar)
            {
                toolBar = (ToolBar) child;
                break;
            }
        }

        if (toolBar == null)
            throw new IllegalArgumentException();

        return toolBar;
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        for (ContributionItem item : invisible)
        {
            if (item instanceof DropDown)
            {
                DropDown dropDown = (DropDown) item;

                if (dropDown.getMenuListener() != null)
                {
                    MenuManager subMenu = new MenuManager(dropDown.getLabel());
                    if (dropDown.getImage() != null)
                        subMenu.setImageDescriptor(dropDown.getImage().descriptor());
                    dropDown.getMenuListener().menuAboutToShow(subMenu);
                    manager.add(subMenu);
                }
                else
                {
                    dropDown.fill(manager);
                }

            }
            else if (item instanceof ActionContributionItem)
            {
                ActionContributionItem action = (ActionContributionItem) item;

                // need to create a wrapper action because an action in the
                // toolbar typically has no text (only the icon)

                String label = action.getAction().getText();
                if (label == null || label.isEmpty())
                    label = action.getAction().getToolTipText();

                manager.add(new SimpleAction(label, action.getAction().getImageDescriptor(),
                                a -> action.getAction().run()));
            }
            else
            {
                manager.add(new LabelOnly(item.toString()));
            }
        }
    }
}
