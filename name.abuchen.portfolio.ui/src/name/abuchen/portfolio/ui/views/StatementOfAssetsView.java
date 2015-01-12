package name.abuchen.portfolio.ui.views;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.AbstractDropDown;
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

    @Inject
    private ExchangeRateProviderFactory factory;

    @Override
    protected String getTitle()
    {
        return Messages.LabelStatementOfAssets;
    }

    @Override
    public void notifyModelUpdated()
    {
        CurrencyConverter converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
        ClientSnapshot snapshot = ClientSnapshot.create(getClient(), converter, Dates.today());

        assetViewer.setInput(snapshot);
    }

    @Override
    protected void addButtons(final ToolBar toolBar)
    {
        AbstractDropDown dropdown = new AbstractDropDown(toolBar, getClient().getBaseCurrency())
        {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                List<CurrencyUnit> available = CurrencyUnit.getAvailableCurrencyUnits();
                Collections.sort(available);
                for (final CurrencyUnit unit : available)
                {
                    Action action = new Action(unit.getLabel())
                    {
                        @Override
                        public void run()
                        {
                            setLabel(unit.getCurrencyCode());
                            getClient().setBaseCurrency(unit.getCurrencyCode());
                        }
                    };
                    action.setChecked(getClient().getBaseCurrency().equals(unit.getCurrencyCode()));
                    manager.add(action);
                }
            }
        };
        getClient().addPropertyChangeListener("baseCurrency", e -> dropdown.setLabel(e.getNewValue().toString())); //$NON-NLS-1$

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

        Action save = new Action()
        {
            @Override
            public void run()
            {
                assetViewer.showSaveMenu(getActiveShell());
            }
        };
        save.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_SAVE));
        save.setToolTipText(Messages.MenuSaveColumns);
        new ActionContributionItem(save).fill(toolBar, -1);

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
