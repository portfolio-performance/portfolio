package name.abuchen.portfolio.ui.dialogs.transactions;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellTransactionCreator;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;

/* package */class BuySellModel extends AbstractSecurityTransactionModel
{
    private BuySellEntry source;

    protected Account account;

    public BuySellModel(Client client, PortfolioTransaction.Type type)
    {
        super(client, type);

        if (!accepts(type))
            throw new IllegalArgumentException("type " + type + " not accepted for this model"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public boolean accepts(PortfolioTransaction.Type type)
    {
        return type == PortfolioTransaction.Type.BUY || type == PortfolioTransaction.Type.SELL;
    }

    @Override
    public void setSource(Object entry)
    {
        this.source = (BuySellEntry) entry;

        presetFromSource(entry);
    }

    @Override
    public void presetFromSource(Object entry)
    {
        var e = (BuySellEntry) entry;

        this.type = e.getPortfolioTransaction().getType();
        this.portfolio = (Portfolio) e.getOwner(e.getPortfolioTransaction());
        this.account = (Account) e.getOwner(e.getAccountTransaction());

        if (new LedgerBuySellTransactionCreator(client).isLedgerBacked(e))
            fillFromLedgerBackedTransaction(e.getPortfolioTransaction());
        else
            fillFromTransaction(e.getPortfolioTransaction());
    }

    private void fillFromLedgerBackedTransaction(PortfolioTransaction transaction)
    {
        this.security = transaction.getSecurity();

        var transactionDate = transaction.getDateTime();
        this.date = transactionDate.toLocalDate();
        this.time = transactionDate.toLocalTime();

        this.shares = transaction.getShares();
        this.total = transaction.getAmount();
        this.note = transaction.getNote();

        this.exchangeRate = BigDecimal.ONE;
        this.grossValue = 0;
        this.convertedGrossValue = 0;
        this.fees = 0;
        this.taxes = 0;
        this.forexFees = 0;
        this.forexTaxes = 0;

        transaction.getUnits().forEach(unit -> {
            switch (unit.getType())
            {
                case GROSS_VALUE:
                    this.exchangeRate = unit.getExchangeRate();
                    this.grossValue = unit.getForex() != null ? unit.getForex().getAmount()
                                    : unit.getAmount().getAmount();
                    break;
                case FEE:
                    if (unit.getForex() != null)
                        this.forexFees += unit.getForex().getAmount();
                    else
                        this.fees += unit.getAmount().getAmount();
                    break;
                case TAX:
                    if (unit.getForex() != null)
                        this.forexTaxes += unit.getForex().getAmount();
                    else
                        this.taxes += unit.getAmount().getAmount();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        });

        if (grossValue == 0)
        {
            this.convertedGrossValue = calculateConvertedGrossValue();
            this.grossValue = exchangeRate.compareTo(BigDecimal.ZERO) == 0 ? 0
                            : Math.round(convertedGrossValue / exchangeRate.doubleValue());
        }

        if (shares != 0)
            this.quote = BigDecimal.valueOf(grossValue * Values.Share.factor() / (shares * Values.Amount.divider()));

        setExchangeRate(exchangeRate);
    }

    @Override
    public boolean hasSource()
    {
        return source != null;
    }

    @Override
    public void applyChanges()
    {
        if (security == null)
            throw new UnsupportedOperationException(Messages.MsgMissingSecurity);
        if (account == null)
            throw new UnsupportedOperationException(Messages.MsgMissingReferenceAccount);

        var ledgerCreator = new LedgerBuySellTransactionCreator(client);
        var dateTime = LocalDateTime.of(date, time);
        var units = buildUnits();

        if (source != null && ledgerCreator.isLedgerBacked(source))
        {
            ledgerCreator.update(source, portfolio, account, type, dateTime, total, account.getCurrencyCode(), security,
                            shares, units, note, source.getSource());
            return;
        }

        if (source == null)
        {
            ledgerCreator.create(portfolio, account, type, dateTime, total, account.getCurrencyCode(), security, shares,
                            units, note, null);
            return;
        }

        BuySellEntry entry;

        if (source != null && source.getOwner(source.getPortfolioTransaction()).equals(portfolio)
                        && source.getOwner(source.getAccountTransaction()).equals(account))
        {
            entry = source;
        }
        else
        {
            entry = new BuySellEntry(portfolio, account);
            entry.setCurrencyCode(account.getCurrencyCode());
            entry.insert();

            if (source != null)
            {
                @SuppressWarnings("unchecked")
                TransactionOwner<Transaction> owner = (TransactionOwner<Transaction>) source
                                .getOwner(source.getPortfolioTransaction());
                owner.deleteTransaction(source.getPortfolioTransaction(), client);

                // preserve the source field from the original transaction
                entry.setSource(source.getSource());

                source = null;
            }
        }

        entry.setDate(dateTime);
        entry.setCurrencyCode(account.getCurrencyCode());
        entry.setSecurity(security);
        entry.setShares(shares);
        entry.setAmount(total);
        entry.setType(type);
        entry.setNote(note);

        writeToTransaction(entry.getPortfolioTransaction());
    }

    @Override
    public void resetToNewTransaction()
    {
        this.source = null;
        super.resetToNewTransaction();
    }

    @Override
    public void setPortfolio(Portfolio portfolio)
    {
        setAccount(portfolio.getReferenceAccount());
        super.setPortfolio(portfolio);
    }

    public Account getAccount()
    {
        return account;
    }

    public void setAccount(Account account)
    {
        String oldAccountCurrency = getTransactionCurrencyCode();
        String oldExchangeRateCurrencies = getExchangeRateCurrencies();
        String oldInverseExchangeRateCurrencies = getInverseExchangeRateCurrencies();

        firePropertyChange(Properties.account.name(), this.account, this.account = account); // NOSONAR

        firePropertyChange(Properties.transactionCurrencyCode.name(), oldAccountCurrency, getTransactionCurrencyCode());
        firePropertyChange(Properties.exchangeRateCurrencies.name(), oldExchangeRateCurrencies,
                        getExchangeRateCurrencies());
        firePropertyChange(Properties.inverseExchangeRateCurrencies.name(), oldInverseExchangeRateCurrencies,
                        getInverseExchangeRateCurrencies());

        if (getSecurity() != null)
        {
            updateSharesAndQuote();
            updateExchangeRate();
        }
    }

    @Override
    public String getTransactionCurrencyCode()
    {
        return account != null ? account.getCurrencyCode() : ""; //$NON-NLS-1$
    }

}
