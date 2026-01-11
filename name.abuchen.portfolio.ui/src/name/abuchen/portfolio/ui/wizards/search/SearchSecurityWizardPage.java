package name.abuchen.portfolio.ui.wizards.search;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.nebula.widgets.chips.Chips;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.oauth.OAuthClient;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.PortfolioPerformanceFeed;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.LoginButton;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.swt.PaginatedTable;
import name.abuchen.portfolio.util.TextUtil;

public class SearchSecurityWizardPage extends WizardPage
{
    public static final String PAGE_ID = "searchpage"; //$NON-NLS-1$

    private final SearchSecurityDataModel model;

    private PaginatedTable table;
    private List<ResultItem> rawResults;
    private Map<String, Boolean> rawSources = new HashMap<>();

    private Set<String> filterByType = new HashSet<>();

    public SearchSecurityWizardPage(SearchSecurityDataModel model)
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

        var searchBox = new Text(container, SWT.BORDER | SWT.SINGLE);
        searchBox.setText(""); //$NON-NLS-1$
        searchBox.setFocus();
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(searchBox);

        var searchButton = new Button(container, SWT.PUSH);
        searchButton.setText(Messages.LabelSearch);
        searchButton.setEnabled(false);

        addFilterBar(container);

        table = new PaginatedTable();

        var control = table.createViewControl(container);
        GridDataFactory.fillDefaults().span(2, 1).applyTo(control);

        Consumer<SelectionEvent> onSearchEvent = e -> doSearch(searchBox.getText());

        // don't forward return to the default button
        searchBox.addTraverseListener(e -> {
            if (e.detail == SWT.TRAVERSE_RETURN)
                e.doit = false;
        });

        searchBox.addSelectionListener(SelectionListener.widgetDefaultSelectedAdapter(onSearchEvent));
        searchBox.addModifyListener(e -> {
            if (e.widget instanceof Text txt)
            {
                searchButton.setEnabled(!txt.getText().isBlank());
            }
        });
        searchButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(onSearchEvent));

        setControl(container);
    }

    private void addFilterBar(Composite container)
    {
        var filterBar = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().span(2, 1).applyTo(filterBar);

        GridLayoutFactory.fillDefaults().numColumns(3).applyTo(filterBar);

        var chips = new Composite(filterBar, SWT.NONE);
        chips.setLayout(new RowLayout(SWT.HORIZONTAL));

        final Listener listener = event -> {
            var chip = (Chips) event.widget;
            var data = chip.getData();

            if (data instanceof CurrencyUnit currency)
            {
                if (chip.getSelection())
                    model.addCurrency(currency);
                else
                    model.removeCurrency(currency);
            }
            else if (data instanceof String type)
            {
                if (chip.getSelection())
                    filterByType.add(type);
                else
                    filterByType.remove(type);
            }

            if (rawResults != null)
                setSearchResults(rawResults);
        };

        for (CurrencyUnit currency : model.getClient().getUsedCurrencies())
        {
            var chip = new Chips(chips, SWT.TOGGLE);
            chip.setData(currency);
            chip.setText(currency.getCurrencyCode());
            chip.setPushedStateBackground(Colors.EQUITY);
            chip.setChipsBackground(Colors.theme().chipBackground());
            chip.addListener(SWT.Selection, listener);
        }

        for (String type : List.of(name.abuchen.portfolio.Messages.LabelSearchShare,
                        name.abuchen.portfolio.Messages.LabelCryptocurrency,
                        name.abuchen.portfolio.Messages.LabelCommodity))
        {
            var chip = new Chips(chips, SWT.TOGGLE);
            chip.setData(type);
            chip.setText(type);
            chip.setPushedStateBackground(Colors.ICON_ORANGE);
            chip.setChipsBackground(Colors.theme().chipBackground());
            chip.addListener(SWT.Selection, listener);
        }

        var spacer = new Label(filterBar, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(spacer);

        var buttons = new Composite(filterBar, SWT.NONE);
        buttons.setLayout(new RowLayout(SWT.HORIZONTAL));

        Button button = new Button(buttons, SWT.PUSH);
        button.setText(Messages.LabelQuoteFeedProvider + " ▼"); //$NON-NLS-1$

        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> {
            for (String source : rawSources.keySet().stream().sorted().toList())
            {
                var item = new SimpleAction(source, SWT.CHECK, a -> {
                    rawSources.put(source, !rawSources.get(source));
                    if (rawResults != null)
                        setSearchResults(rawResults);
                });
                item.setChecked(rawSources.get(source));
                manager.add(item);
            }
        });

        Menu contextMenu = menuMgr.createContextMenu(button);
        button.addDisposeListener(e -> {
            if (!contextMenu.isDisposed())
                contextMenu.dispose();
        });

        button.addListener(SWT.Selection, e -> {
            Point location = button.toDisplay(0, button.getSize().y);
            contextMenu.setLocation(location);
            contextMenu.setVisible(true);
        });

        // trigger setPageComplete to add or remove the error message
        LoginButton.create(buttons, () -> setPageComplete(isPageComplete()));
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

    @Override
    public IWizardPage getNextPage()
    {
        if (getWizard() == null || model.getSelectedInstrument() == null)
            return null;

        return getWizard().getPage(Objects.equals(model.getSelectedMarket(), model.getSelectedInstrument())
                        ? SearchSecurityPreviewPricesWizardPage.PAGE_ID
                        : SelectMarketsWizardPage.PAGE_ID);
    }

    @Override
    public IWizardPage getPreviousPage()
    {
        return null;
    }

    private void setSearchResults(List<ResultItem> elements)
    {
        var filtered = doFilter(elements);

        var labelProvider = new PaginatedTable.LabelProvider<ResultItem>()
        {
            @Override
            public String getText(ResultItem e)
            {
                StringBuilder buffer = new StringBuilder();
                buffer.append("<strong>").append(TextUtil.escapeHtml(e.getName())).append("</strong>"); //$NON-NLS-1$ //$NON-NLS-2$
                if (e.getType() != null)
                    buffer.append("   <em>").append(e.getType()).append("</em>"); //$NON-NLS-1$ //$NON-NLS-2$
                buffer.append("\n"); //$NON-NLS-1$

                if (e.getCurrencyCode() != null)
                    buffer.append(TextUtil.limit(e.getCurrencyCode(), 20)).append(" "); //$NON-NLS-1$

                if (e.getIsin() != null)
                    buffer.append(e.getIsin()).append(" "); //$NON-NLS-1$
                else if (e.getSymbol() != null)
                    buffer.append(TextUtil.limit(e.getSymbol(), 20)).append(" "); //$NON-NLS-1$

                buffer.append("     <gray>[").append(e.getSource()).append("]</gray>"); //$NON-NLS-1$ //$NON-NLS-2$

                return buffer.toString();
            }

            @Override
            public Images getTrailingImage(ResultItem e)
            {
                return e.getMarkets().isEmpty() ? null : Images.ARROW_FORWARD;
            }
        };

        var selectionListener = new PaginatedTable.SelectionListener<ResultItem>()
        {
            @Override
            public void onSelection(ResultItem element)
            {
                model.setSelectedInstrument(element);
                model.setSelectedMarket(element.getMarkets().isEmpty() ? element : null);
                setPageComplete(true);
            }

            @Override
            public void onDoubleClick(ResultItem element)
            {
                model.setSelectedInstrument(element);
                model.setSelectedMarket(element.getMarkets().isEmpty() ? element : null);
                setPageComplete(true);
                getWizard().getContainer().showPage(getNextPage());
            }
        };

        table.setInput(filtered, labelProvider, selectionListener);
    }

    private List<ResultItem> doFilter(List<ResultItem> elements)
    {
        var skippedProvider = rawSources.entrySet().stream() //
                        .filter(entry -> !entry.getValue()) //
                        .map(Map.Entry::getKey) //
                        .collect(Collectors.toSet());

        if (skippedProvider.isEmpty() && model.getCurrencies().isEmpty() && filterByType.isEmpty())
            return elements;

        var filtered = new ArrayList<ResultItem>();

        for (ResultItem item : elements)
        {
            if (skippedProvider.contains(item.getSource()))
                continue;

            var foundCurrency = model.getCurrencies().isEmpty();
            for (CurrencyUnit currency : model.getCurrencies())
            {
                var c = item.getCurrencyCode();
                if (c != null && c.contains(currency.getCurrencyCode()))
                {
                    foundCurrency = true;
                    break;
                }
            }
            if (!foundCurrency)
                continue;

            var foundType = filterByType.isEmpty();
            for (String type : filterByType)
            {
                var t = item.getType();
                if (t != null && t.contains(type))
                {
                    foundType = true;
                    break;
                }
            }

            if (foundType)
                filtered.add(item);
        }

        return filtered;
    }

    private void doSearch(String query)
    {
        try
        {
            if (query.isBlank())
                return;

            // after searching, selection required to enable finish button
            setPageComplete(false);

            getContainer().run(true, false, progressMonitor -> {

                List<SecuritySearchProvider> providers = Factory.getSearchProvider();

                progressMonitor.beginTask(Messages.SecurityMenuSearch4Securities, providers.size());

                List<ResultItem> result = new ArrayList<>();
                List<String> errors = new ArrayList<>();

                for (SecuritySearchProvider provider : providers)
                {
                    try
                    {
                        progressMonitor.setTaskName(provider.getName());
                        result.addAll(provider.search(query));
                    }
                    catch (IOException e)
                    {
                        PortfolioPlugin.log(e);
                        errors.add(provider.getName() + ": " + e.getMessage()); //$NON-NLS-1$
                    }
                    progressMonitor.worked(1);
                }

                var sources = result.stream() //
                                .map(ResultItem::getSource) //
                                .filter(Objects::nonNull).distinct()
                                .collect(Collectors.toMap(source -> source, source -> true));

                // keep unchecked sources to remember the user decision
                rawSources.entrySet().stream() //
                                .filter(entry -> !entry.getValue())
                                .forEach(entry -> sources.put(entry.getKey(), false));

                Display.getDefault().asyncExec(() -> {
                    model.clearSelectedInstrument();
                    this.rawResults = result;
                    this.rawSources = sources;
                    setSearchResults(result);

                    if (!errors.isEmpty())
                        setErrorMessage(String.join(", ", errors)); //$NON-NLS-1$
                    else
                        setErrorMessage(null);
                });

            });
        }
        catch (InvocationTargetException | InterruptedException e)
        {
            PortfolioPlugin.log(e);
        }
    }
}
