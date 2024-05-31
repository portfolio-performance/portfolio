package name.abuchen.portfolio.ui.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.events.ChangeEventConstants;
import name.abuchen.portfolio.events.SecurityCreatedEvent;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.TaxonomyTemplate;
import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.CoinGeckoSearchProvider;
import name.abuchen.portfolio.online.impl.EurostatHICPQuoteFeed;
import name.abuchen.portfolio.online.impl.PortfolioReportNet;
import name.abuchen.portfolio.online.impl.PortfolioReportNetSearchProvider;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.ListSelectionDialog;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.editor.DomainElement;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.jobs.UpdateQuotesJob;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.wizards.security.EditSecurityDialog;
import name.abuchen.portfolio.ui.wizards.security.SearchSecurityWizardDialog;
import name.abuchen.portfolio.util.TradeCalendarManager;

public class NewDomainElementHandler
{
    @Inject
    private IEventBroker broker;

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(UIConstants.Parameter.TYPE) final String type)
    {
        Optional<PortfolioPart> portfolioPart = MenuHelper.getActivePortfolioPart(part, true);
        if (portfolioPart.isEmpty())
            return;

        DomainElement element = DomainElement.valueOf(type);
        switch (element)
        {
            case INVESTMENT_VEHICLE:
                // the creation dialogs must run in the context of the current
                // view otherwise the inject has not access to the full context
                // objects
                portfolioPart.get().getCurrentView().ifPresent(this::createNewInvestmentVehicle);
                break;
            case CRYPTO_CURRENCY:
                portfolioPart.get().getCurrentView().ifPresent(this::createNewCryptocurrency);
                break;
            case EXCHANGE_RATE:
                portfolioPart.get().getCurrentView().ifPresent(this::createNewExchangeRate);
                break;
            case CONSUMER_PRICE_INDEX:
                portfolioPart.get().getCurrentView().ifPresent(this::createNewConsumerPriceIndex);
                break;
            case TAXONOMY:
                createNewTaxonomy(portfolioPart.get().getClient());
                break;
            case WATCHLIST:
                createNewWatchlist(portfolioPart.get().getClient());
                break;
            default:
        }
    }

    private void createNewInvestmentVehicle(AbstractFinanceView view)
    {
        SearchSecurityWizardDialog dialog = new SearchSecurityWizardDialog(Display.getDefault().getActiveShell(),
                        view.getClient());
        if (dialog.open() == Window.OK)
        {
            Security newSecurity = dialog.getSecurity();

            openEditDialog(view, newSecurity);
        }

    }

    private void openEditDialog(AbstractFinanceView view, Security newSecurity)
    {
        EditSecurityDialog editSecurityDialog = view.make(EditSecurityDialog.class, newSecurity);

        if (editSecurityDialog.open() == Window.OK)
        {
            view.getClient().addSecurity(newSecurity);
            view.getClient().markDirty();
            new UpdateQuotesJob(view.getClient(), newSecurity).schedule();

            postSecurityCreatedEvent(view.getClient(), newSecurity);
        }
    }

    private void postSecurityCreatedEvent(Client client, Security security)
    {
        broker.post(ChangeEventConstants.Security.CREATED, new SecurityCreatedEvent(client, security));
    }

    private void createNewCryptocurrency(AbstractFinanceView view)
    {
        try
        {
            ILabelProvider labelProvider = new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    SecuritySearchProvider.ResultItem item = (SecuritySearchProvider.ResultItem) element;
                    return String.format("%s (%s)", item.getSymbol(), item.getName()); //$NON-NLS-1$
                }

                @Override
                public Color getBackground(Object element)
                {
                    return element instanceof PortfolioReportNet.OnlineItem ? Colors.theme().warningBackground() : null;
                }
            };

            ListSelectionDialog dialog = new ListSelectionDialog(Display.getDefault().getActiveShell(), labelProvider);

            dialog.setTitle(Messages.SecurityMenuNewCryptocurrency);
            dialog.setMessage(Messages.SecurityMenuNewCryptocurrencyMessage);
            dialog.setMultiSelection(false);
            dialog.setViewerComparator(new ViewerComparator()
            {
                @Override
                public int category(Object element)
                {
                    return element instanceof PortfolioReportNet.OnlineItem ? 0 : 1;
                }
            });

            var allCryptos = new ArrayList<ResultItem>();

            // add Portfolio Report cryptos
            allCryptos.addAll(Factory.getSearchProvider(PortfolioReportNetSearchProvider.class).getCoins());

            // add Coingecko unless the crypto already exists. Because the
            // symbol is not unique, we compare symbol and name

            var existingCryptos = allCryptos.stream()
                            .collect(Collectors.toMap(i -> i.getSymbol(), i -> i, (r, l) -> r));

            Factory.getSearchProvider(CoinGeckoSearchProvider.class) //
                            .search("", SecuritySearchProvider.Type.CRYPTO) //$NON-NLS-1$
                            .stream().filter(item -> {
                                var other = existingCryptos.get(item.getSymbol());
                                return other == null || !other.getName().equals(item.getName());
                            }).forEach(allCryptos::add);

            dialog.setElements(allCryptos);

            if (dialog.open() == Window.OK)
            {
                Object[] result = dialog.getResult();

                for (Object object : result)
                {
                    ResultItem item = (ResultItem) object;
                    Security newSecurity = item.create(view.getClient());
                    openEditDialog(view, newSecurity);
                }
            }
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError, e.getMessage());
        }
    }

    private void createNewExchangeRate(AbstractFinanceView view)
    {
        Security newSecurity = new Security(null, view.getClient().getBaseCurrency());
        newSecurity.setFeed(QuoteFeed.MANUAL);
        newSecurity.setTargetCurrencyCode(view.getClient().getBaseCurrency());
        openEditDialog(view, newSecurity);
    }

    private void createNewConsumerPriceIndex(AbstractFinanceView view)
    {
        LabelProvider labelProvider = LabelProvider.createTextProvider(o -> ((Exchange) o).getName());
        ListSelectionDialog dialog = new ListSelectionDialog(Display.getDefault().getActiveShell(), labelProvider);

        dialog.setTitle(Messages.SecurityMenuNewHICP);
        dialog.setMessage(Messages.SecurityMenuHICPMessage);
        dialog.setElements(new EurostatHICPQuoteFeed().getExchanges(null, new ArrayList<>()));

        if (dialog.open() == Window.OK)
        {
            Object[] result = dialog.getResult();

            for (Object object : result)
            {
                Exchange region = (Exchange) object;

                Security newSecurity = new Security(region.getName() + " " + Messages.LabelSuffix_HICP, null); //$NON-NLS-1$
                newSecurity.setFeed(EurostatHICPQuoteFeed.ID);
                newSecurity.setLatestFeed(QuoteFeed.MANUAL);
                newSecurity.setTickerSymbol(region.getId());
                newSecurity.setCalendar(TradeCalendarManager.FIRST_OF_THE_MONTH_CODE);

                view.getClient().addSecurity(newSecurity);
                view.getClient().markDirty();
                new UpdateQuotesJob(view.getClient(), newSecurity).schedule();
                postSecurityCreatedEvent(view.getClient(), newSecurity);
            }
        }
    }

    private void createNewTaxonomy(Client client)
    {
        TaxonomyCreationDialog dialog = new TaxonomyCreationDialog(Display.getDefault().getActiveShell());
        if (dialog.open() != Window.OK)
            return;

        String name = dialog.getName();
        if (name == null || name.isEmpty())
            return;

        final Optional<TaxonomyTemplate> template = dialog.getTemplate();
        if (template.isPresent())
        {
            Taxonomy taxonomy = template.get().build();
            taxonomy.setName(name);
            taxonomy.getRoot().setName(name);
            client.addTaxonomy(taxonomy);
            client.touch();
        }
        else
        {
            Taxonomy taxonomy = new Taxonomy(name);
            taxonomy.setRootNode(new Classification(UUID.randomUUID().toString(), name));
            client.addTaxonomy(taxonomy);
            client.touch();
        }
    }

    private void createNewWatchlist(Client client)
    {
        InputDialog dlg = new InputDialog(Display.getDefault().getActiveShell(), Messages.WatchlistNewLabel,
                        Messages.WatchlistEditDialogMsg, Messages.WatchlistNewLabel, null);
        if (dlg.open() != Window.OK)
            return;

        String name = dlg.getValue();
        if (name == null)
            return;

        Watchlist watchlist = new Watchlist();
        watchlist.setName(name);
        client.addWatchlist(watchlist);
        client.touch();
    }
}

class TaxonomyCreationDialog extends Dialog
{
    private String name = Messages.LabelNewTaxonomy;
    private TaxonomyTemplate template;

    public TaxonomyCreationDialog(Shell parentShell)
    {
        super(parentShell);
    }

    public String getName()
    {
        return name;
    }

    public Optional<TaxonomyTemplate> getTemplate()
    {
        return Optional.ofNullable(template);
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Control contents = super.createContents(parent);
        getShell().setText(Messages.LabelNewTaxonomy);
        return contents;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);

        Composite editArea = new Composite(composite, SWT.NONE);
        editArea.setLayout(new FormLayout());

        Label label = new Label(editArea, SWT.WRAP);
        label.setText(Messages.DialogTaxonomyNamePrompt);

        Text text = new Text(editArea, SWT.BORDER);
        text.setText(name);
        text.addModifyListener(e -> name = text.getText());

        Group group = new Group(editArea, SWT.SHADOW_IN);
        group.setText(Messages.LabelTemplate);
        group.setLayout(new RowLayout(SWT.VERTICAL));

        Button none = new Button(group, SWT.RADIO);
        none.setText(Messages.LabelEmptyTaxonomy);
        none.setSelection(true);
        none.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            template = null;
            text.setText(Messages.LabelNewTaxonomy);
        }));

        for (final TaxonomyTemplate t : TaxonomyTemplate.list())
        {
            Button radio = new Button(group, SWT.RADIO);
            radio.setText(t.getName());
            radio.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
                template = t;
                text.setText(t.getName());
            }));
        }

        FormDataFactory.startingWith(label).thenBelow(text).width(250).thenBelow(group, 10).right(text);

        return composite;
    }
}
