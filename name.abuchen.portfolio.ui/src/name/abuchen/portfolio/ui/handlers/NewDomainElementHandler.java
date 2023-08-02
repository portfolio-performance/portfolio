package name.abuchen.portfolio.ui.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.CoinGeckoSearchProvider;
import name.abuchen.portfolio.online.impl.EurostatHICPQuoteFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.ListSelectionDialog;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.editor.DomainElement;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.jobs.UpdateQuotesJob;
import name.abuchen.portfolio.ui.util.Colors;
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

            broker.post(UIConstants.Event.Domain.SECURITY_CREATED, newSecurity);
        }
    }

    private void createNewCryptocurrency(AbstractFinanceView view)
    {
        try
        {
            @SuppressWarnings("nls")
            final Set<String> popularCoins = Set.of("bitcoin", "ethereum", "aave", "algorand", "bitcoin-cash",
                            "cardano", "chainlink", "decentraland", "dogecoin", "litecoin", "polkadot", "matic-network",
                            "shiba-inu", "solana", "the-sandbox", "uniswap", "ripple");

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
                    SecuritySearchProvider.ResultItem item = (SecuritySearchProvider.ResultItem) element;
                    return popularCoins.contains(item.getExchange()) ? Colors.theme().warningBackground() : null;
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
                    SecuritySearchProvider.ResultItem item = (SecuritySearchProvider.ResultItem) element;
                    return popularCoins.contains(item.getExchange()) ? 0 : 1;
                }
            });
            dialog.setElements(Factory.getSearchProvider(CoinGeckoSearchProvider.class) //
                            .search("", SecuritySearchProvider.Type.ALL)); //$NON-NLS-1$

            if (dialog.open() == Window.OK)
            {
                Object[] result = dialog.getResult();

                for (Object object : result)
                {
                    ResultItem item = (ResultItem) object;
                    Security newSecurity = item.create(view.getClient().getSettings());
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
        Security newSecurity = new Security();
        newSecurity.setFeed(QuoteFeed.MANUAL);
        newSecurity.setCurrencyCode(view.getClient().getBaseCurrency());
        newSecurity.setTargetCurrencyCode(view.getClient().getBaseCurrency());
        openEditDialog(view, newSecurity);
    }

    private void createNewConsumerPriceIndex(AbstractFinanceView view)
    {
        LabelProvider labelProvider = LabelProvider.createTextProvider(o -> ((Exchange) o).getName());
        ListSelectionDialog dialog = new ListSelectionDialog(Display.getDefault().getActiveShell(), labelProvider);

        dialog.setTitle(Messages.SecurityMenuNewHICP);
        dialog.setMessage(Messages.SecurityMenuHICPMessage);
        dialog.setElements(new EurostatHICPQuoteFeed().getExchanges(new Security(), new ArrayList<>()));

        if (dialog.open() == Window.OK)
        {
            Object[] result = dialog.getResult();

            for (Object object : result)
            {
                Exchange region = (Exchange) object;

                Security newSecurity = new Security();
                newSecurity.setFeed(EurostatHICPQuoteFeed.ID);
                newSecurity.setLatestFeed(QuoteFeed.MANUAL);
                newSecurity.setCurrencyCode(null);
                newSecurity.setTickerSymbol(region.getId());
                newSecurity.setName(region.getName() + " " + Messages.LabelSuffix_HICP); //$NON-NLS-1$
                newSecurity.setCalendar(TradeCalendarManager.FIRST_OF_THE_MONTH_CODE);

                view.getClient().addSecurity(newSecurity);
                view.getClient().markDirty();
                new UpdateQuotesJob(view.getClient(), newSecurity).schedule();
                broker.post(UIConstants.Event.Domain.SECURITY_CREATED, newSecurity);
            }
        }
    }

    private void createNewTaxonomy(Client client)
    {
        InputDialog dlg = new InputDialog(Display.getDefault().getActiveShell(), Messages.LabelNewTaxonomy,
                        Messages.DialogTaxonomyNamePrompt, Messages.LabelNewTaxonomy, null);
        if (dlg.open() != Window.OK)
            return;

        String name = dlg.getValue();
        if (name == null)
            return;

        Taxonomy taxonomy = new Taxonomy(name);
        taxonomy.setRootNode(new Classification(UUID.randomUUID().toString(), name));
        client.addTaxonomy(taxonomy);
        client.touch();
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
