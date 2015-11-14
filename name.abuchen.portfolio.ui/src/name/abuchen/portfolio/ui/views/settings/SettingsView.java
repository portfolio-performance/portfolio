package name.abuchen.portfolio.ui.views.settings;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;

public class SettingsView extends AbstractFinanceView
{
    public interface Tab
    {
        CTabItem createTab(CTabFolder folder);

        void showAddMenu(Shell shell);
    }

    private CTabFolder folder;
    private int initiallySelectedTab = 0;

    @Override
    protected String getTitle()
    {
        return Messages.LabelSettings;
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        Action config = new Action()
        {
            @Override
            public void run()
            {
                CTabItem item = folder.getSelection();
                Tab tab = (Tab) item.getData();
                tab.showAddMenu(getActiveShell());
            }
        };
        config.setImageDescriptor(Images.PLUS.descriptor());
        config.setToolTipText(Messages.MenuSettingsNew);

        new ActionContributionItem(config).fill(toolBar, -1);
    }

    @Override
    public void init(PortfolioPart part, Object parameter)
    {
        super.init(part, parameter);

        if (parameter != null && parameter instanceof Integer)
            initiallySelectedTab = ((Integer) parameter).intValue();
    }

    @Override
    protected Control createBody(Composite parent)
    {
        folder = new CTabFolder(parent, SWT.BORDER);

        Tab tab = make(BookmarksListTab.class);
        CTabItem item = tab.createTab(folder);
        item.setData(tab);

        tab = make(AttributeListTab.class);
        item = tab.createTab(folder);
        item.setData(tab);

        if (initiallySelectedTab >= 0 && initiallySelectedTab < folder.getItemCount())
            folder.setSelection(initiallySelectedTab);
        else
            folder.setSelection(0);

        return folder;
    }
}
