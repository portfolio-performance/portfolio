package name.abuchen.portfolio.ui.wizards.search;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.oauth.OAuthClient;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.MarketIdentifierCodes;
import name.abuchen.portfolio.online.impl.PortfolioPerformanceFeed;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.swt.PaginatedTable;
import name.abuchen.portfolio.util.TextUtil;

public class SelectMarketsWizardPage extends WizardPage
{
    public static final String PAGE_ID = "markets"; //$NON-NLS-1$

    private final SearchSecurityDataModel model;

    private PaginatedTable table;
    private ResultItem currentInstrument;

    public SelectMarketsWizardPage(SearchSecurityDataModel model)
    {
        super(PAGE_ID);
        this.model = model;

        setTitle(Messages.SecurityMenuAddNewSecurity);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);

        table = new PaginatedTable(12);
        var control = table.createViewControl(container);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(control);

        setControl(container);
    }

    @Override
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);

        if (visible && model.getSelectedInstrument() != null)
        {
            var previousSelection = (ResultItem) table.getSelection();

            currentInstrument = model.getSelectedInstrument();
            setTitle(currentInstrument.getName());

            var markets = doFilter(currentInstrument.getMarkets());
            setMarkets(currentInstrument, markets);

            // the table might still have a selected market which is not stored
            // in the model anymore (for example if the user navigated to an
            // instrument without markets in the meantime). Therefore we set the
            // selection if it is a valid market.

            if (markets.contains(previousSelection))
            {
                model.setSelectedMarket(previousSelection);
                table.setSelection(previousSelection);
                setPageComplete(true);
            }
            else
            {
                table.clearSelection();
                setPageComplete(false);
            }
        }
    }
    
    @Override
    public void setPageComplete(boolean complete)
    {
        super.setPageComplete(complete);

        var selectedItem = model.getSelectedInstrument();
        if (selectedItem != null && PortfolioPerformanceFeed.ID.equals(selectedItem.getFeedId())
                        && !OAuthClient.INSTANCE.isAuthenticated())
        {
            setErrorMessage(Messages.MsgHistoricalPricesRequireSignIn);
        }
        else
        {
            setErrorMessage(null);
        }
    }

    private void setMarkets(ResultItem parent, List<ResultItem> markets)
    {
        var labelProvider = new PaginatedTable.LabelProvider<ResultItem>()
        {
            @Override
            public String getText(ResultItem e)
            {
                if (e == parent)
                {
                    StringBuilder buffer = new StringBuilder();
                    buffer.append("<strong>").append(TextUtil.escapeHtml(e.getName())).append("</strong>"); //$NON-NLS-1$ //$NON-NLS-2$
                    if (e.getType() != null)
                        buffer.append("   <em>").append(e.getType()).append("</em>"); //$NON-NLS-1$ //$NON-NLS-2$
                    buffer.append("\n"); //$NON-NLS-1$

                    if (e.getIsin() != null)
                        buffer.append(e.getIsin()).append(" "); //$NON-NLS-1$
                    buffer.append("     <gray>[").append(e.getSource()).append("]</gray>"); //$NON-NLS-1$ //$NON-NLS-2$

                    return buffer.toString();
                }
                else
                {
                    StringBuilder buffer = new StringBuilder();

                    var exchange = MarketIdentifierCodes.getLabel(e.getExchange());
                    buffer.append("<strong>").append(exchange).append("</strong>"); //$NON-NLS-1$ //$NON-NLS-2$

                    if (e.getType() != null)
                        buffer.append("   <em>").append(e.getType()).append("</em>"); //$NON-NLS-1$ //$NON-NLS-2$
                    buffer.append("\n"); //$NON-NLS-1$

                    buffer.append("<strong>").append(TextUtil.limit(e.getCurrencyCode(), 20)).append("</strong> "); //$NON-NLS-1$ //$NON-NLS-2$

                    if (e.getSymbol() != null)
                        buffer.append(TextUtil.limit(e.getSymbol(), 20)).append(" "); //$NON-NLS-1$

                    return buffer.toString();
                }
            }

            @Override
            public Images getLeadingImage(ResultItem e)
            {
                return e == parent ? Images.ARROW_BACK : null;
            }
        };

        var selectionListener = new PaginatedTable.SelectionListener<ResultItem>()
        {
            @Override
            public void onSelection(ResultItem element)
            {
                if (element.getMarkets().isEmpty())
                {
                    model.setSelectedMarket(element);
                    setPageComplete(true);
                }
                else
                {
                    model.clearSelectedMarket();
                    setPageComplete(false);
                }
            }

            @Override
            public void onDoubleClick(ResultItem element)
            {
                if (element == parent)
                {
                    model.clearSelectedMarket();
                    setPageComplete(false);
                    getWizard().getContainer().showPage(getPreviousPage());
                }
                else
                {
                    model.setSelectedMarket(element);
                    setPageComplete(true);
                    getWizard().getContainer().showPage(getNextPage());
                }
            }
        };

        var elements = new ArrayList<ResultItem>();
        elements.add(parent);
        elements.addAll(markets);

        table.setInput(elements, labelProvider, selectionListener);
    }

    private List<ResultItem> doFilter(List<ResultItem> elements)
    {
        if (model.getCurrencies().isEmpty())
            return elements;

        var filtered = new ArrayList<ResultItem>();

        for (ResultItem item : elements)
        {
            var foundCurrency = false;
            for (CurrencyUnit currency : model.getCurrencies())
            {
                var c = item.getCurrencyCode();
                if (c != null && c.contains(currency.getCurrencyCode()))
                {
                    foundCurrency = true;
                    break;
                }
            }
            if (foundCurrency)
                filtered.add(item);
        }

        return filtered;
    }
}
