package name.abuchen.portfolio.snapshot.filter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy.Visitor;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Creates a Client that does only contain the securities and accounts included
 * in the specified classification. Because securities and accounts might only
 * be partially assigned to a classification, the transaction amount is adopted
 * if needed.
 */
public class ClientClassificationFilter implements ClientFilter
{
    private static class CalculationState
    {
        private final Map<Account, ReadOnlyAccount> account2readonly = new HashMap<>();
        private final Map<Portfolio, ReadOnlyPortfolio> portfolio2readonly = new HashMap<>();

        private final Set<Account> categorizedAccounts = new HashSet<>();
        private final Set<Security> categorizedSecurities = new HashSet<>();
        private final Map<InvestmentVehicle, BigDecimal> vehicle2weight = new HashMap<>();

        public CalculationState(Classification classification)
        {
            classification.accept(new Visitor()
            {
                @Override
                public void visit(Classification classification, Assignment assignment)
                {
                    InvestmentVehicle vehicle = assignment.getInvestmentVehicle();
                    BigDecimal weight = vehicle2weight.computeIfAbsent(vehicle, v -> BigDecimal.ZERO);
                    vehicle2weight.put(vehicle, weight.add(BigDecimal.valueOf(assignment.getWeight())));

                    if (vehicle instanceof Account)
                        categorizedAccounts.add((Account) vehicle);
                    else if (vehicle instanceof Security && ((Security) vehicle).getCurrencyCode() != null)
                        categorizedSecurities.add((Security) vehicle);
                }
            });
        }

        public boolean isCategorized(Security security)
        {
            return categorizedSecurities.contains(security);
        }

        public boolean isCategorized(Account account)
        {
            return categorizedAccounts.contains(account);
        }

        public BigDecimal getWeight(InvestmentVehicle vehicle)
        {
            BigDecimal w = vehicle2weight.get(vehicle);
            return w == null ? BigDecimal.ZERO : w;
        }

        public ReadOnlyAccount asReadOnly(Account account)
        {
            return account2readonly.get(account);
        }

        public void putReadOnly(Account account, ReadOnlyAccount readOnlyAccount)
        {
            account2readonly.put(account, readOnlyAccount);
        }

        public ReadOnlyPortfolio asReadOnly(Portfolio portfolio)
        {
            return portfolio2readonly.get(portfolio);
        }

        public void putReadOnly(Portfolio portfolio, ReadOnlyPortfolio readOnlyPortfolio)
        {
            portfolio2readonly.put(portfolio, readOnlyPortfolio);
        }
    }

    private final Classification classification;

    public ClientClassificationFilter(Classification classification)
    {
        this.classification = classification;
    }

    @Override
    public Client filter(Client client)
    {
        ReadOnlyClient pseudoClient = new ReadOnlyClient(client);

        CalculationState state = new CalculationState(classification);

        for (Account account : client.getAccounts())
        {
            ReadOnlyAccount pseudo = new ReadOnlyAccount(account);
            pseudoClient.internalAddAccount(pseudo);
            state.putReadOnly(account, pseudo);
        }

        for (Portfolio portfolio : client.getPortfolios())
        {
            ReadOnlyPortfolio pseudoPortfolio = new ReadOnlyPortfolio(portfolio);
            pseudoPortfolio.setReferenceAccount(state.asReadOnly(portfolio.getReferenceAccount()));
            pseudoClient.internalAddPortfolio(pseudoPortfolio);
            state.putReadOnly(portfolio, pseudoPortfolio);
        }

        for (Portfolio portfolio : client.getPortfolios())
            adaptPortfolioTransactions(state, portfolio);

        for (Account account : client.getAccounts())
        {
            if (state.isCategorized(account))
                adaptAccountTransactions(state, account);
            else
                collectSecurityRelatedTx(state, account);
        }

        for (Security security : state.categorizedSecurities)
            pseudoClient.internalAddSecurity(security);

        return pseudoClient;
    }

    private void adaptPortfolioTransactions(CalculationState state, Portfolio portfolio)
    {
        for (PortfolioTransaction t : portfolio.getTransactions())
        {
            // only transactions of securities assigned to the classification
            // are relevant when filtering
            if (!state.isCategorized(t.getSecurity()))
                continue;

            switch (t.getType())
            {
                case BUY:
                case SELL:
                    Account account = (Account) t.getCrossEntry().getCrossOwner(t);

                    if (!state.isCategorized(account))
                        addDeliveryT(state, portfolio, t,
                                        t.getType() == PortfolioTransaction.Type.BUY
                                                        ? PortfolioTransaction.Type.DELIVERY_INBOUND
                                                        : PortfolioTransaction.Type.DELIVERY_OUTBOUND,
                                        state.getWeight(t.getSecurity()));
                    else
                        addBuySellT(state, portfolio, t);
                    break;

                case DELIVERY_INBOUND:
                case DELIVERY_OUTBOUND:
                    addDeliveryT(state, portfolio, t, t.getType(), state.getWeight(t.getSecurity()));
                    break;

                case TRANSFER_OUT:
                    // handled via TRANSFER_IN
                    break;
                case TRANSFER_IN:
                    PortfolioTransferEntry entry = (PortfolioTransferEntry) t.getCrossEntry();
                    ClientFilterHelper.recreateTransfer(entry, state.asReadOnly(entry.getSourcePortfolio()),
                                    state.asReadOnly(entry.getTargetPortfolio()), state.getWeight(t.getSecurity()));
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    private void addBuySellT(CalculationState state, Portfolio portfolio, PortfolioTransaction t)
    {
        BigDecimal securityWeight = state.getWeight(t.getSecurity());

        long taxes = value(t.getUnitSum(Unit.Type.TAX).getAmount(), securityWeight);
        long securityAmount = value(t.getAmount(), securityWeight);
        securityAmount = t.getType() == PortfolioTransaction.Type.BUY ? securityAmount - taxes : securityAmount + taxes;

        Account account = (Account) t.getCrossEntry().getCrossOwner(t);
        BigDecimal accountWeight = state.getWeight(account);
        long accountAmount = value(t.getAmount(), accountWeight);

        long commonAmount = Math.min(securityAmount, accountAmount);
        BigDecimal commonWeight = BigDecimal.valueOf(commonAmount)
                        .divide(BigDecimal.valueOf(securityAmount), Values.MC)
                        .multiply(securityWeight, Values.MC);

        // create a buy/sell transactions with the amount shared by the account
        // assignment and the security assignment

        BuySellEntry copy = new BuySellEntry(state.asReadOnly(portfolio), state.account2readonly.get(account));
        copy.setDate(t.getDateTime());
        copy.setCurrencyCode(t.getCurrencyCode());
        copy.setSecurity(t.getSecurity());
        copy.setType(t.getType());
        copy.setNote(t.getNote());
        copy.setShares(value(t.getShares(), commonWeight));
        copy.setAmount(commonAmount);

        // copy all units (except for taxes) over to the new transaction
        t.getUnits().filter(u -> u.getType() != Unit.Type.TAX)
                        .forEach(u -> copy.getPortfolioTransaction().addUnit(value(u, commonWeight)));

        state.asReadOnly(portfolio).internalAddTransaction(copy.getPortfolioTransaction());
        state.asReadOnly(account).internalAddTransaction(copy.getAccountTransaction());

        // create a deposit or removal if the account has a higher weight

        if (accountAmount - commonAmount > 0)
        {
            AccountTransaction ta = new AccountTransaction(t.getDateTime(), t.getCurrencyCode(),
                            accountAmount - commonAmount, null,
                            t.getType() == PortfolioTransaction.Type.BUY ? AccountTransaction.Type.REMOVAL
                                            : AccountTransaction.Type.DEPOSIT);

            state.asReadOnly(account).internalAddTransaction(ta);
        }

        // create a inbound or outbound delivery if the security has a higher
        // weight and therefore an additional assignment is needed

        if (securityAmount - commonAmount > 0)
        {
            PortfolioTransaction tp = new PortfolioTransaction();
            tp.setDateTime(t.getDateTime());
            tp.setCurrencyCode(t.getCurrencyCode());
            tp.setSecurity(t.getSecurity());
            tp.setShares(value(t.getShares(), securityWeight.subtract(commonWeight)));
            tp.setType(t.getType() == PortfolioTransaction.Type.BUY ? PortfolioTransaction.Type.DELIVERY_INBOUND
                            : PortfolioTransaction.Type.DELIVERY_OUTBOUND);

            tp.setAmount(securityAmount - commonAmount);

            t.getUnits().filter(u -> u.getType() != Unit.Type.TAX)
                            .forEach(u -> tp.addUnit(value(u, securityWeight.subtract(commonWeight))));

            state.asReadOnly(portfolio).internalAddTransaction(tp);
        }
    }

    private void addDeliveryT(CalculationState state, Portfolio portfolio, PortfolioTransaction t,
                    PortfolioTransaction.Type targetType, BigDecimal weight)
    {
        PortfolioTransaction copy = new PortfolioTransaction();
        copy.setDateTime(t.getDateTime());
        copy.setCurrencyCode(t.getCurrencyCode());
        copy.setSecurity(t.getSecurity());
        copy.setShares(value(t.getShares(), weight));
        copy.setType(targetType);

        // calculation is without taxes -> remove any taxes & adapt total
        // accordingly

        long taxes = value(t.getUnitSum(Unit.Type.TAX).getAmount(), weight);
        long amount = value(t.getAmount(), weight);

        copy.setAmount(copy.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND ? amount - taxes : amount + taxes);

        // copy all units (except for taxes) over to the new transaction
        t.getUnits().filter(u -> u.getType() != Unit.Type.TAX).forEach(u -> copy.addUnit(value(u, weight)));

        state.asReadOnly(portfolio).internalAddTransaction(copy);
    }

    private void adaptAccountTransactions(CalculationState state, Account account)
    {
        BigDecimal accountWeight = state.getWeight(account);

        for (AccountTransaction t : account.getTransactions())
        {
            long amount = value(t.getAmount(), accountWeight);

            switch (t.getType())
            {
                case SELL:
                    // only if the security is not included (and therefore
                    // buy/sell transactions are handled by the
                    // #adaptPortfolioTransactions method), create a deposit or
                    // removal in the account
                    if (!state.isCategorized(t.getSecurity()))
                        state.asReadOnly(account).internalAddTransaction(new AccountTransaction(t.getDateTime(),
                                        t.getCurrencyCode(), amount, null, AccountTransaction.Type.DEPOSIT));
                    break;

                case BUY:
                    if (!state.isCategorized(t.getSecurity()))
                        state.asReadOnly(account).internalAddTransaction(new AccountTransaction(t.getDateTime(),
                                        t.getCurrencyCode(), amount, null, AccountTransaction.Type.REMOVAL));
                    break;

                case DIVIDENDS:
                    if (!state.isCategorized(t.getSecurity()))
                        state.asReadOnly(account).internalAddTransaction(new AccountTransaction(t.getDateTime(),
                                        t.getCurrencyCode(), amount, null, AccountTransaction.Type.DEPOSIT));
                    else
                        addSecurityRelatedAccountT(state, account, t);
                    break;

                case FEES_REFUND:
                    if (t.getSecurity() != null && state.isCategorized(t.getSecurity()))
                        addSecurityRelatedAccountT(state, account, t);
                    else if (t.getSecurity() != null)
                        state.asReadOnly(account).internalAddTransaction(new AccountTransaction(t.getDateTime(),
                                        t.getCurrencyCode(), amount, null, AccountTransaction.Type.DEPOSIT));
                    else
                        state.asReadOnly(account).internalAddTransaction(new AccountTransaction(t.getDateTime(),
                                        t.getCurrencyCode(), amount, null, t.getType()));
                    break;

                case FEES:
                    if (t.getSecurity() != null && state.isCategorized(t.getSecurity()))
                        addSecurityRelatedAccountT(state, account, t);
                    else if (t.getSecurity() != null)
                        state.asReadOnly(account).internalAddTransaction(new AccountTransaction(t.getDateTime(),
                                        t.getCurrencyCode(), amount, null, AccountTransaction.Type.REMOVAL));
                    else
                        state.asReadOnly(account).internalAddTransaction(new AccountTransaction(t.getDateTime(),
                                        t.getCurrencyCode(), amount, null, t.getType()));
                    break;

                case TRANSFER_IN:
                    // if the outbound account is not categorized, create a
                    // deposit right away. Otherwise a full transfer transaction
                    // is created.
                    if (!state.isCategorized((Account) t.getCrossEntry().getCrossOwner(t)))
                        state.asReadOnly(account).internalAddTransaction(new AccountTransaction(t.getDateTime(),
                                        t.getCurrencyCode(), amount, null, AccountTransaction.Type.DEPOSIT));
                    else
                        addTransferT(state, account, t);
                    break;

                case TRANSFER_OUT:
                    // transfer transactions are from the inbound account (see
                    // above); only if the inbound account is not categorized,
                    // then create a removal transaction
                    if (!state.isCategorized((Account) t.getCrossEntry().getCrossOwner(t)))
                        state.asReadOnly(account).internalAddTransaction(new AccountTransaction(t.getDateTime(),
                                        t.getCurrencyCode(), amount, null, AccountTransaction.Type.REMOVAL));
                    break;

                case TAX_REFUND:
                    // taxes are never included
                    state.asReadOnly(account).internalAddTransaction(new AccountTransaction(t.getDateTime(),
                                    t.getCurrencyCode(), amount, null, AccountTransaction.Type.DEPOSIT));
                    break;

                case TAXES:
                    // taxes are never included
                    state.asReadOnly(account).internalAddTransaction(new AccountTransaction(t.getDateTime(),
                                    t.getCurrencyCode(), amount, null, AccountTransaction.Type.REMOVAL));
                    break;

                case DEPOSIT:
                case REMOVAL:
                case INTEREST:
                case INTEREST_CHARGE:
                    state.asReadOnly(account).internalAddTransaction(new AccountTransaction(t.getDateTime(),
                                    t.getCurrencyCode(), amount, null, t.getType()));
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    private void addSecurityRelatedAccountT(CalculationState state, Account account, AccountTransaction t)
    {
        BigDecimal accountWeight = state.getWeight(account);
        BigDecimal securityWeight = state.getWeight(t.getSecurity());

        long taxes = value(t.getUnitSum(Unit.Type.TAX).getAmount(), securityWeight);
        long amount = value(t.getAmount(), securityWeight);

        AccountTransaction copy = new AccountTransaction(t.getDateTime(), t.getCurrencyCode(), amount + taxes,
                        t.getSecurity(), t.getType());
        t.getUnits().filter(u -> u.getType() != Unit.Type.TAX).forEach(u -> copy.addUnit(value(u, securityWeight)));
        state.asReadOnly(account).internalAddTransaction(copy);

        long accountAmount = value(t.getAmount(), accountWeight);

        long delta = accountAmount - amount - taxes;

        if (delta != 0)
        {
            AccountTransaction.Type deltaType = delta > 0 ^ t.getType().isDebit() ? AccountTransaction.Type.DEPOSIT
                            : AccountTransaction.Type.REMOVAL;
            state.asReadOnly(account).internalAddTransaction(new AccountTransaction(t.getDateTime(),
                            t.getCurrencyCode(), Math.abs(delta), null, deltaType));
        }
    }

    private void addTransferT(CalculationState state, Account inboundAccount, AccountTransaction t)
    {
        Account outboundAccount = (Account) t.getCrossEntry().getCrossOwner(t);

        BigDecimal inboundWeight = state.getWeight(inboundAccount);
        BigDecimal outboundWeight = state.getWeight(outboundAccount);

        if (inboundWeight.equals(outboundWeight) && inboundWeight.equals(Classification.ONE_HUNDRED_PERCENT_BD))
        {
            state.asReadOnly(inboundAccount).internalAddTransaction(t);
            state.asReadOnly(outboundAccount)
                            .internalAddTransaction((AccountTransaction) t.getCrossEntry().getCrossTransaction(t));
        }
        else if (inboundWeight.equals(outboundWeight))
        {
            AccountTransferEntry entry = createTransferEntry((AccountTransferEntry) t.getCrossEntry(), inboundWeight);

            // entry#insert does not work with ReadOnlyAccount
            state.asReadOnly(inboundAccount).internalAddTransaction(entry.getTargetTransaction());
            state.asReadOnly(outboundAccount).internalAddTransaction(entry.getSourceTransaction());
        }
        else if (inboundWeight.compareTo(outboundWeight) < 0)
        {
            AccountTransferEntry entry = createTransferEntry((AccountTransferEntry) t.getCrossEntry(), inboundWeight);

            // entry#insert does not work with ReadOnlyAccount
            state.asReadOnly(inboundAccount).internalAddTransaction(entry.getTargetTransaction());
            state.asReadOnly(outboundAccount).internalAddTransaction(entry.getSourceTransaction());

            AccountTransaction ot = (AccountTransaction) t.getCrossEntry().getCrossTransaction(t);

            state.asReadOnly(outboundAccount)
                            .internalAddTransaction(new AccountTransaction(ot.getDateTime(), ot.getCurrencyCode(),
                                            value(ot.getAmount(), outboundWeight.subtract(inboundWeight)), null,
                                            AccountTransaction.Type.REMOVAL));
        }
        else // inboundWeight > outboundWeight
        {
            AccountTransferEntry entry = createTransferEntry((AccountTransferEntry) t.getCrossEntry(), outboundWeight);

            // entry#insert does not work with ReadOnlyAccount
            state.asReadOnly(inboundAccount).internalAddTransaction(entry.getTargetTransaction());
            state.asReadOnly(outboundAccount).internalAddTransaction(entry.getSourceTransaction());

            state.asReadOnly(inboundAccount)
                            .internalAddTransaction(new AccountTransaction(t.getDateTime(), t.getCurrencyCode(),
                                            value(t.getAmount(), inboundWeight.subtract(outboundWeight)), null,
                                            AccountTransaction.Type.DEPOSIT));
        }
    }

    private AccountTransferEntry createTransferEntry(AccountTransferEntry entry, BigDecimal weight)
    {
        AccountTransferEntry copy = new AccountTransferEntry();
        copy.setDate(entry.getSourceTransaction().getDateTime());
        copy.setNote(entry.getSourceTransaction().getNote());

        copy.getSourceTransaction().setCurrencyCode(entry.getSourceTransaction().getCurrencyCode());
        copy.getTargetTransaction().setCurrencyCode(entry.getTargetTransaction().getCurrencyCode());
        copy.getSourceTransaction().setAmount(value(entry.getSourceTransaction().getAmount(), weight));
        copy.getTargetTransaction().setAmount(value(entry.getTargetTransaction().getAmount(), weight));

        return copy;
    }

    private void collectSecurityRelatedTx(CalculationState state, Account account)
    {
        ReadOnlyAccount readOnlyAccount = state.asReadOnly(account);

        for (AccountTransaction t : account.getTransactions())
        {
            if (t.getSecurity() == null || !state.isCategorized(t.getSecurity()))
                continue;

            BigDecimal weight = state.getWeight(t.getSecurity());

            switch (t.getType())
            {
                case DIVIDENDS:
                    long taxes = value(t.getUnitSum(Unit.Type.TAX).getAmount(), weight);
                    long amount = value(t.getAmount(), weight);

                    AccountTransaction copy = new AccountTransaction(t.getDateTime(), t.getCurrencyCode(),
                                    amount + taxes, t.getSecurity(), t.getType());

                    t.getUnits().filter(u -> u.getType() != Unit.Type.TAX).forEach(u -> copy.addUnit(value(u, weight)));

                    readOnlyAccount.internalAddTransaction(copy);
                    readOnlyAccount.internalAddTransaction(new AccountTransaction(t.getDateTime(), t.getCurrencyCode(),
                                    amount + taxes, null, AccountTransaction.Type.REMOVAL));
                    break;
                case FEES:
                    readOnlyAccount.internalAddTransaction(new AccountTransaction(t.getDateTime(), t.getCurrencyCode(),
                                    value(t.getAmount(), weight), t.getSecurity(), t.getType()));
                    readOnlyAccount.internalAddTransaction(new AccountTransaction(t.getDateTime(), t.getCurrencyCode(),
                                    value(t.getAmount(), weight), null, AccountTransaction.Type.DEPOSIT));
                    break;
                case FEES_REFUND:
                    readOnlyAccount.internalAddTransaction(new AccountTransaction(t.getDateTime(), t.getCurrencyCode(),
                                    value(t.getAmount(), weight), t.getSecurity(), t.getType()));
                    readOnlyAccount.internalAddTransaction(new AccountTransaction(t.getDateTime(), t.getCurrencyCode(),
                                    value(t.getAmount(), weight), null, AccountTransaction.Type.REMOVAL));
                    break;
                case TAXES:
                case TAX_REFUND:
                    // ignore taxes when calculating performance of
                    // securities
                case BUY:
                case TRANSFER_IN:
                case SELL:
                case TRANSFER_OUT:
                case DEPOSIT:
                case REMOVAL:
                case INTEREST:
                case INTEREST_CHARGE:
                    // do nothing
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    private static Unit value(Unit unit, BigDecimal weight)
    {
        if (weight.equals(Classification.ONE_HUNDRED_PERCENT_BD))
            return unit;
        else
            return new Unit(unit.getType(),
                            Money.of(unit.getAmount().getCurrencyCode(), value(unit.getAmount().getAmount(), weight)));
    }

    private static long value(long value, BigDecimal weight)
    {
        if (weight.equals(Classification.ONE_HUNDRED_PERCENT_BD))
            return value;
        else
            return BigDecimal.valueOf(value) //
                            .multiply(weight, Values.MC)
                            .divide(Classification.ONE_HUNDRED_PERCENT_BD, Values.MC)
                            .setScale(0, RoundingMode.HALF_EVEN).longValue();
    }
}
