package name.abuchen.portfolio.ui.views.panes;

import java.time.LocalDate;

import javax.inject.Inject;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.filter.ClientFilter;
import name.abuchen.portfolio.snapshot.filter.EmptyFilter;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.views.StatementOfAssetsViewer;

public class StatementOfAssetsPane implements InformationPanePage
{
    @Inject
    private Client client;

    @Inject
    private AbstractFinanceView view;

    @Inject
    private ExchangeRateProviderFactory factory;

    private Portfolio portfolio;
    private StatementOfAssetsViewer viewer;

    @Override
    public String getLabel()
    {
        return Messages.LabelStatementOfAssets;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        viewer = view.make(StatementOfAssetsViewer.class);

        Control control = viewer.createControl(parent, true);

        new ContextMenu(viewer.getTableViewer().getControl(), manager -> viewer.hookMenuListener(manager, view)).hook();

        return control;
    }

    @Override
    public void addButtons(ToolBarManager toolBar)
    {
        toolBar.add(new SimpleAction(Messages.MenuExportData, Images.EXPORT,
                        a -> new TableViewerCSVExporter(viewer.getTableViewer()).export(getLabel(), portfolio)));

        toolBar.add(new DropDown(Messages.MenuShowHideColumns, Images.CONFIG, SWT.NONE,
                        manager -> viewer.getColumnHelper().menuAboutToShow(manager)));
    }

    @Override
    public void setInput(Object input)
    {
        portfolio = Adaptor.adapt(Portfolio.class, input);

        if (portfolio != null)
        {
            CurrencyConverter converter = new CurrencyConverterImpl(factory,
                            portfolio.getReferenceAccount().getCurrencyCode());

            ClientFilter clientFilter = new PortfolioClientFilter(portfolio);

            viewer.setInput(clientFilter, LocalDate.now(), converter);
        }
        else
        {
            CurrencyConverter converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
            viewer.setInput(new EmptyFilter(), LocalDate.now(), converter);
        }
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (portfolio != null)
            setInput(portfolio);
    }

}
