package name.abuchen.portfolio.ui.views;

import java.util.List;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.editor.PortfolioPart;

public abstract class AbstractTabbedView<T extends AbstractTabbedView.Tab> extends AbstractFinanceView
{
    public interface Tab
    {
        String getTitle();

        Composite createTab(Composite parent);

        default void addButtons(ToolBarManager toolBarManager)
        {}
    }

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

    private void updateToolBar()
    {
        ToolBarManager manager = getToolBarManager();
        manager.removeAll();

        getSelection().addButtons(getToolBarManager());

        manager.update(true);
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
