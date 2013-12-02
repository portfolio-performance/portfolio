package name.abuchen.portfolio.ui.views;

import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

public class StatementOfAssetsView extends AbstractFinanceView
{
    private StatementOfAssetsViewer assetViewer;

    @Override
    protected String getTitle()
    {
        return Messages.LabelStatementOfAssets;
    }

    @Override
    public void notifyModelUpdated()
    {
        ClientSnapshot snapshot = ClientSnapshot.create(getClient(), Dates.today());

        assetViewer.setInput(snapshot);
    }

    @Override
    protected void addButtons(final ToolBar toolBar)
    {
        Action export = new Action()
        {
            @Override
            public void run()
            {
                new TableViewerCSVExporter(assetViewer.getTableViewer()) //
                                .export(Messages.LabelStatementOfAssets + ".csv"); //$NON-NLS-1$
            }
        };
        export.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_EXPORT));
        export.setToolTipText(Messages.MenuExportData);
        new ActionContributionItem(export).fill(toolBar, -1);

        Action config = new Action()
        {
            @Override
            public void run()
            {
                assetViewer.showConfigMenu(toolBar.getShell());
            }
        };
        config.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_CONFIG));
        config.setToolTipText(Messages.MenuShowHideColumns);
        new ActionContributionItem(config).fill(toolBar, -1);
    }

    @Override
    protected Control createBody(Composite parent)
    {
        assetViewer = new StatementOfAssetsViewer(parent, this, getClient());
        hookContextMenu(assetViewer.getTableViewer().getControl(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                assetViewer.hookMenuListener(manager, StatementOfAssetsView.this);
            }
        });
        notifyModelUpdated();
        assetViewer.pack();

        return assetViewer.getControl();
    }

}
