package name.abuchen.portfolio.ui.views;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.editor.PortfolioPart;

public abstract class AbstractTabbedView<T extends AbstractTabbedView.Tab> extends AbstractFinanceView
{
    public interface Tab
    {
        String getTitle();

        Composite createTab(Composite parent);

        default void addButtons(ToolBar toolBar)
        {}
    }

    private ToolBar toolBar;
    private CTabFolder folder;
    private int initiallySelectedTab = 0;

    public AbstractTabbedView()
    {
        super();
    }

    protected abstract List<T> createTabs();

    @Override
    public void init(PortfolioPart part, Object parameter)
    {
        super.init(part, parameter);

        if (parameter instanceof Integer)
            initiallySelectedTab = ((Integer) parameter).intValue();
    }

    @Override
    protected final void addButtons(ToolBar toolBar)
    {
        this.toolBar = toolBar;
    }

    private void updateToolBar()
    {
        if (toolBar.isDisposed())
            return;

        for (ToolItem child : toolBar.getItems())
        {
            child.dispose();
        }

        getSelection().addButtons(toolBar);

        toolBar.getParent().layout(true);
    }

    @Override
    protected final Control createBody(Composite parent)
    {
        folder = new CTabFolder(parent, SWT.BORDER);

        for (T tab : createTabs())
        {
            Composite container = tab.createTab(folder);

            CTabItem item = new CTabItem(folder, SWT.NONE);
            item.setText(tab.getTitle());
            item.setControl(container);
            item.setData(tab);
        }

        folder.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateToolBar()));

        if (initiallySelectedTab >= 0 && initiallySelectedTab < folder.getItemCount())
            folder.setSelection(initiallySelectedTab);
        else
            folder.setSelection(0);

        updateToolBar();
        
        return folder;
    }

    @SuppressWarnings("unchecked")
    protected T getSelection()
    {
        CTabItem item = folder.getSelection();
        return (T) item.getData();
    }
}
