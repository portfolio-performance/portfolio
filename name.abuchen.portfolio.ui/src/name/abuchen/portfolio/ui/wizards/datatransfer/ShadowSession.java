package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;

/**
 * A short-lived shadow {@link Client} that drives the transaction dialogs for
 * manual entry.
 * <p>
 * Each dialog interaction creates a fresh session: the dialog writes one or
 * more transactions into the shadow client, {@link #harvest()} turns them into
 * {@link ExtractedEntry} copies that reference the <em>real</em> client's
 * accounts, portfolios and securities, and the session is then discarded.
 * Because every session is independent, transactions created in one dialog (or
 * on one manual page) can never leak into another.
 */
final class ShadowSession
{
    private final Client shadowClient = new Client();
    private final Map<Account, Account> accountToReal = new HashMap<>();
    private final Map<Portfolio, Portfolio> portfolioToReal = new HashMap<>();
    private final Map<Account, Account> accountToShadow = new HashMap<>();
    private final Map<Portfolio, Portfolio> portfolioToShadow = new HashMap<>();

    private ShadowSession(Client realClient, Collection<Security> additionalSecurities)
    {
        for (Account realAccount : realClient.getAccounts())
        {
            var shadow = new Account();
            shadow.setName(realAccount.getName());
            shadow.setCurrencyCode(realAccount.getCurrencyCode());
            shadowClient.addAccount(shadow);
            accountToReal.put(shadow, realAccount);
            accountToShadow.put(realAccount, shadow);
        }

        for (Portfolio realPortfolio : realClient.getPortfolios())
        {
            var shadow = new Portfolio();
            shadow.setName(realPortfolio.getName());
            shadowClient.addPortfolio(shadow);
            portfolioToReal.put(shadow, realPortfolio);
            portfolioToShadow.put(realPortfolio, shadow);

            // link shadow portfolio to shadow reference account
            var refAccount = realPortfolio.getReferenceAccount();
            if (refAccount != null && accountToShadow.containsKey(refAccount))
                shadow.setReferenceAccount(accountToShadow.get(refAccount));
        }

        // share existing securities by reference; safe because the dialogs only
        // reference, never edit them
        realClient.getSecurities().forEach(shadowClient::addSecurity);

        // add securities seen in auto-extracted results that are not yet part
        // of the real client
        for (Security security : additionalSecurities)
        {
            if (!shadowClient.getSecurities().contains(security))
                shadowClient.addSecurity(security);
        }
    }

    static ShadowSession create(Client realClient, Collection<Security> additionalSecurities)
    {
        return new ShadowSession(realClient, additionalSecurities);
    }

    Client getClient()
    {
        return shadowClient;
    }

    Account toShadow(Account realAccount)
    {
        return realAccount == null ? null : accountToShadow.get(realAccount);
    }

    Portfolio toShadow(Portfolio realPortfolio)
    {
        return realPortfolio == null ? null : portfolioToShadow.get(realPortfolio);
    }

    /**
     * Collects every transaction the dialog wrote into the shadow client and
     * returns them as {@link ExtractedEntry} copies that reference the real
     * client. A single dialog session may have produced more than one
     * transaction (e.g. via "Save and new").
     */
    List<ExtractedEntry> harvest()
    {
        var entries = new ArrayList<ExtractedEntry>();

        // scan shadow accounts for new transactions; the switch skips the
        // mirror side of each cross-entry, so no extra de-duplication is
        // required
        for (Account shadowAccount : shadowClient.getAccounts())
        {
            var realAccount = accountToReal.get(shadowAccount);

            for (AccountTransaction at : shadowAccount.getTransactions())
            {
                switch (at.getType())
                {
                    case BUY, SELL:
                        entries.add(extractBuySell(at, realAccount));
                        break;

                    case TRANSFER_OUT:
                        entries.add(extractAccountTransfer(at, realAccount));
                        break;

                    case TRANSFER_IN:
                        break;

                    default:
                        entries.add(extractAccountTransaction(at, realAccount));
                        break;
                }
            }
        }

        // scan shadow portfolios for delivery and transfer transactions
        for (Portfolio shadowPortfolio : shadowClient.getPortfolios())
        {
            var realPortfolio = portfolioToReal.get(shadowPortfolio);

            for (PortfolioTransaction pt : shadowPortfolio.getTransactions())
            {
                switch (pt.getType())
                {
                    case DELIVERY_INBOUND, DELIVERY_OUTBOUND:
                        entries.add(extractDelivery(pt, realPortfolio));
                        break;

                    case TRANSFER_OUT:
                        entries.add(extractPortfolioTransfer(pt, realPortfolio));
                        break;

                    case BUY, SELL, TRANSFER_IN:
                    default:
                        break;
                }
            }
        }

        return entries;
    }

    private ExtractedEntry extractBuySell(AccountTransaction at, Account realAccount)
    {
        var crossEntry = (BuySellEntry) at.getCrossEntry();

        var entry = new BuySellEntry();
        fillBuySell(entry, crossEntry.getPortfolioTransaction());

        var item = new Extractor.BuySellEntryItem(entry);
        item.setAccountPrimary(realAccount);

        var shadowPortfolio = crossEntry.getPortfolio();
        if (shadowPortfolio != null)
            item.setPortfolioPrimary(portfolioToReal.get(shadowPortfolio));

        return new ExtractedEntry(item);
    }

    private ExtractedEntry extractAccountTransfer(AccountTransaction at, Account realAccount)
    {
        var crossEntry = (AccountTransferEntry) at.getCrossEntry();

        var entry = new AccountTransferEntry();
        fillAccountTransfer(entry, at);

        var item = new Extractor.AccountTransferItem(entry, true);
        item.setAccountPrimary(realAccount);

        var targetShadowAccount = crossEntry.getTargetAccount();
        if (targetShadowAccount != null)
            item.setAccountSecondary(accountToReal.get(targetShadowAccount));

        return new ExtractedEntry(item);
    }

    private ExtractedEntry extractAccountTransaction(AccountTransaction at, Account realAccount)
    {
        var newAt = new AccountTransaction();
        newAt.setType(at.getType());
        newAt.setDateTime(at.getDateTime());
        newAt.setCurrencyCode(at.getCurrencyCode());
        newAt.setAmount(at.getAmount());
        newAt.setShares(at.getShares());
        newAt.setSecurity(at.getSecurity());
        newAt.setNote(at.getNote());
        at.getUnits().forEach(newAt::addUnit);

        var item = new Extractor.TransactionItem(newAt);
        item.setAccountPrimary(realAccount);
        return new ExtractedEntry(item);
    }

    private ExtractedEntry extractDelivery(PortfolioTransaction pt, Portfolio realPortfolio)
    {
        var newPt = new PortfolioTransaction();
        newPt.setType(pt.getType());
        newPt.setDateTime(pt.getDateTime());
        newPt.setCurrencyCode(pt.getCurrencyCode());
        newPt.setAmount(pt.getAmount());
        newPt.setShares(pt.getShares());
        newPt.setSecurity(pt.getSecurity());
        newPt.setNote(pt.getNote());
        pt.getUnits().forEach(newPt::addUnit);

        var item = new Extractor.TransactionItem(newPt);
        item.setPortfolioPrimary(realPortfolio);
        return new ExtractedEntry(item);
    }

    private ExtractedEntry extractPortfolioTransfer(PortfolioTransaction pt, Portfolio realPortfolio)
    {
        var crossEntry = (PortfolioTransferEntry) pt.getCrossEntry();

        var entry = new PortfolioTransferEntry();
        fillPortfolioTransfer(entry, pt);

        var item = new Extractor.PortfolioTransferItem(entry);
        item.setPortfolioPrimary(realPortfolio);

        var targetShadowPortfolio = crossEntry.getTargetPortfolio();
        if (targetShadowPortfolio != null)
            item.setPortfolioSecondary(portfolioToReal.get(targetShadowPortfolio));

        return new ExtractedEntry(item);
    }

    /**
     * Rebuilds the subject of an already-harvested buy/sell entry in this
     * session's shadow coordinates, so the dialog can preselect the portfolio
     * and account when editing. The harvested item carries the owners in
     * <em>real</em> space; they are mapped back to the freshly created shadow
     * objects.
     */
    BuySellEntry shadowBuySell(Extractor.Item item)
    {
        var source = (BuySellEntry) item.getSubject();
        var entry = new BuySellEntry(toShadow(item.getPortfolioPrimary()), toShadow(item.getAccountPrimary()));
        fillBuySell(entry, source.getPortfolioTransaction());
        return entry;
    }

    AccountTransferEntry shadowAccountTransfer(Extractor.Item item)
    {
        var source = (AccountTransferEntry) item.getSubject();
        var entry = new AccountTransferEntry(toShadow(item.getAccountPrimary()), toShadow(item.getAccountSecondary()));
        fillAccountTransfer(entry, source.getSourceTransaction());
        return entry;
    }

    PortfolioTransferEntry shadowPortfolioTransfer(Extractor.Item item)
    {
        var source = (PortfolioTransferEntry) item.getSubject();
        var entry = new PortfolioTransferEntry(toShadow(item.getPortfolioPrimary()),
                        toShadow(item.getPortfolioSecondary()));
        fillPortfolioTransfer(entry, source.getSourceTransaction());
        return entry;
    }

    private static void fillBuySell(BuySellEntry target, PortfolioTransaction source)
    {
        target.setType(source.getType());
        target.setDate(source.getDateTime());
        target.setCurrencyCode(source.getCurrencyCode());
        target.setAmount(source.getAmount());
        target.setShares(source.getShares());
        target.setSecurity(source.getSecurity());
        target.setNote(source.getNote());
        source.getUnits().forEach(u -> target.getPortfolioTransaction().addUnit(u));
    }

    private static void fillAccountTransfer(AccountTransferEntry target, AccountTransaction source)
    {
        target.setDate(source.getDateTime());
        target.setCurrencyCode(source.getCurrencyCode());
        target.setAmount(source.getAmount());
        target.setNote(source.getNote());
    }

    private static void fillPortfolioTransfer(PortfolioTransferEntry target, PortfolioTransaction source)
    {
        target.setDate(source.getDateTime());
        target.setCurrencyCode(source.getCurrencyCode());
        target.setAmount(source.getAmount());
        target.setShares(source.getShares());
        target.setSecurity(source.getSecurity());
        target.setNote(source.getNote());
    }
}
