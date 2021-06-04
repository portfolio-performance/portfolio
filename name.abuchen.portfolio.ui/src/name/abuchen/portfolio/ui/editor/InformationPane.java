package name.abuchen.portfolio.ui.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.ToolBar;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.util.TextUtil;

public class InformationPane
{
    private static class PaneLayout extends Layout
    {
        @Override
        protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache)
        {
            return new Point(100, 100);
        }

        @Override
        protected void layout(Composite composite, boolean flushCache)
        {
            Control[] children = composite.getChildren();

            if (children.length < 3)
                return;

            Rectangle availableBounds = composite.getBounds();

            int y = 0;

            Point label = children[0].computeSize(SWT.DEFAULT, SWT.DEFAULT);
            children[0].setBounds(0, 0, Math.min(label.x, availableBounds.width), label.y);
            y += label.y + 2;

            Point toolbar = children[1].computeSize(SWT.DEFAULT, SWT.DEFAULT);
            children[1].setBounds(0, y, Math.min(toolbar.x, availableBounds.width), toolbar.y);
            y += toolbar.y + 2;

            children[2].setBounds(0, y, availableBounds.width, availableBounds.height - y);
        }
    }

    private Composite area;
    private CLabel label;
    private ToolBarManager toolBar;
    private PageBook pagebook;

    private Object currentInput;

    private Map<InformationPanePage, Control> page2control = new HashMap<>();

    /* package */ void createViewControl(Composite parent)
    {
        area = new Composite(parent, SWT.NONE);
        area.setLayout(new PaneLayout());

        label = new CLabel(area, SWT.NONE);
        Font newFont = FontDescriptor.createFrom(parent.getFont()).setStyle(SWT.BOLD).createFont(parent.getDisplay());
        label.addDisposeListener(e -> newFont.dispose());
        label.setFont(newFont);

        Composite wrapper = new Composite(area, SWT.NONE);
        wrapper.setBackground(area.getBackground());

        toolBar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);

        ToolBar tb = toolBar.createControl(wrapper);
        tb.setBackground(parent.getBackground());

        // create layout *after* the toolbar to keep the tab order right
        wrapper.setLayout(new ToolBarPlusChevronLayout(wrapper, SWT.LEFT));

        pagebook = new PageBook(area, SWT.NONE);
        pagebook.setBackground(area.getBackground());
    }

    /* package */ void setView(AbstractFinanceView view)
    {
        page2control.clear();

        List<InformationPanePage> pages = new ArrayList<>();
        view.addPanePages(pages);

        label.setText(view.getTitle());

        toolBar.removeAll();

        for (InformationPanePage page : pages)
        {
            DropDown dropdown = new DropDown(page.getLabel(), Images.VIEW, SWT.PUSH);
            dropdown.setDefaultAction(new SimpleAction(a -> {
                Control control = page2control.computeIfAbsent(page, p -> {
                    Control c = p.createViewControl(pagebook);
                    c.setData(InformationPanePage.class.getName(), p);
                    return c;
                });

                // important: refresh input when showing the page b/c pages
                // currently not visible are not updated if data changes

                page.setInput(currentInput);
                pagebook.showPage(control);

                for (IContributionItem item : toolBar.getItems())
                {
                    if (item == dropdown)
                        dropdown.setImage(Images.VIEW_SELECTED);
                    else if (item instanceof DropDown)
                        ((DropDown) item).setImage(Images.VIEW);
                }
            }));
            toolBar.add(dropdown);
        }

        toolBar.update(true);

        Control firstControl = null;
        if (!pages.isEmpty())
        {
            InformationPanePage firstPage = pages.get(0);
            firstControl = firstPage.createViewControl(pagebook);
            firstControl.setData(InformationPanePage.class.getName(), firstPage);
            page2control.put(firstPage, firstControl);

            ((DropDown) toolBar.getItems()[0]).setImage(Images.VIEW_SELECTED);
        }
        else
        {
            firstControl = new Composite(pagebook, SWT.NONE);
        }

        pagebook.showPage(firstControl);

        Control[] children = pagebook.getChildren();

        for (int ii = 0; ii < children.length; ii++)
        {
            if (children[ii] == firstControl)
                continue;

            children[ii].dispose();
        }

        pagebook.getParent().layout();
    }

    /* package */ void setInput(Object input)
    {
        currentInput = input;

        Named named = Adaptor.adapt(Named.class, input);
        label.setText(named != null ? TextUtil.tooltip(named.getName()) : ""); //$NON-NLS-1$

        InformationPanePage page = (InformationPanePage) pagebook.getPage()
                        .getData(InformationPanePage.class.getName());
        if (page != null)
            page.setInput(input);

        area.layout();
    }

    /* package */ void onRecalculationNeeded()
    {
        Named named = Adaptor.adapt(Named.class, currentInput);
        label.setText(named != null ? TextUtil.tooltip(named.getName()) : ""); //$NON-NLS-1$

        InformationPanePage page = (InformationPanePage) pagebook.getPage()
                        .getData(InformationPanePage.class.getName());
        if (page != null)
            page.onRecalculationNeeded();

        area.layout();
    }

    /* package */ <P extends InformationPanePage> Optional<P> lookup(Class<P> type)
    {
        for (InformationPanePage page : page2control.keySet())
        {
            if (type.isAssignableFrom(page.getClass()))
                return Optional.of(type.cast(page));
        }
        return Optional.empty();
    }

    /* package */ void setLayoutData(Object layoutData)
    {
        area.setLayoutData(layoutData);
    }

    /* package */ Object getLayoutData()
    {
        return area.getLayoutData();
    }

    /* package */ Control getControl()
    {
        return area;
    }
}
