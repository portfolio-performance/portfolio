package name.abuchen.portfolio.datatransfer.actions;

import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;

public class CheckCurrenciesAction implements ImportAction
{
    private static final Set<AccountTransaction.Type> TRANSACTIONS_WO_UNITS = EnumSet.of(AccountTransaction.Type.BUY,
                    AccountTransaction.Type.SELL, AccountTransaction.Type.TRANSFER_IN);

    @Override
    public Status process(Security security)
    {
        String currency = security.getCurrencyCode();
        CurrencyUnit unit = CurrencyUnit.getInstance(currency);
        return unit != null ? Status.OK_STATUS
                        : new Status(Status.Code.ERROR,
                                        MessageFormat.format(Messages.MsgCheckUnsupportedCurrency, currency));
    }

    @Override
    public Status process(AccountTransaction transaction, Account account)
    {
        if (!account.getCurrencyCode().equals(transaction.getCurrencyCode()))
            return new Status(Status.Code.ERROR,
                            MessageFormat.format(Messages.MsgCheckTransactionCurrencyDoesNotMatchAccount,
                                            transaction.getCurrencyCode(), account.getCurrencyCode()));

        if (transaction.getSecurity() != null)
        {
            if (TRANSACTIONS_WO_UNITS.contains(transaction.getType()))
            {
                // for buy/sell and transfer out transactions, the units are
                // maintained in the portfolio transaction, not the account
                // transaction.

                if (transaction.getUnits().findAny().isPresent())
                    return new Status(Status.Code.ERROR, MessageFormat
                                    .format(Messages.MsgCheckTransactionMustNotHaveGrossAmount, transaction.getType()));
            }
            else
            {
                Status status = checkGrossValueAndUnitsAgainstSecurity(transaction);
                if (status.getCode() != Status.Code.OK)
                    return status;
            }
        }

        return Status.OK_STATUS;
    }

    @Override
    public Status process(PortfolioTransaction transaction, Portfolio portfolio)
    {
        Security security = transaction.getSecurity();
        if (security == null)
            return new Status(Status.Code.ERROR,
                            MessageFormat.format(Messages.MsgCheckMissingSecurity, transaction.getType().toString()));

        Status status = checkGrossValueAndUnitsAgainstSecurity(transaction);
        if (status.getCode() != Status.Code.OK)
            return status;

        if (transaction.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                        || transaction.getType() == PortfolioTransaction.Type.BUY)
        {
            // tax + fees must be < than transaction amount
            Money taxAndFees = transaction.getUnits() //
                            .filter(u -> u.getType() == Unit.Type.TAX || u.getType() == Unit.Type.FEE) //
                            .map(Unit::getAmount) //
                            .collect(MoneyCollectors.sum(transaction.getCurrencyCode()));

            if (!transaction.getMonetaryAmount().isGreaterOrEqualTo(taxAndFees))
                return new Status(Status.Code.ERROR, MessageFormat.format(Messages.MsgCheckTaxAndFeesTooHigh,
                                Values.Money.format(transaction.getMonetaryAmount()), Values.Money.format(taxAndFees)));
        }

        return Status.OK_STATUS;
    }

    @Override
    public Status process(BuySellEntry entry, Account account, Portfolio portfolio)
    {
        AccountTransaction t = entry.getAccountTransaction();
        Status status = process(t, account);
        if (status.getCode() != Status.Code.OK)
            return status;
        return process(entry.getPortfolioTransaction(), portfolio);
    }

    @Override
    public Status process(AccountTransferEntry entry, Account source, Account target)
    {
        AccountTransaction t = entry.getSourceTransaction();
        Status status = process(t, source);
        if (status.getCode() != Status.Code.OK)
            return status;
        return process(entry.getTargetTransaction(), target);
    }

    @Override
    public Status process(PortfolioTransferEntry entry, Portfolio source, Portfolio target)
    {
        PortfolioTransaction t = entry.getSourceTransaction();
        Status status = process(t, source);
        if (status.getCode() != Status.Code.OK)
            return status;
        return process(entry.getTargetTransaction(), target);
    }

    private Status checkGrossValueAndUnitsAgainstSecurity(Transaction transaction)
    {
        String securityCurrency = transaction.getSecurity().getCurrencyCode();
        if (securityCurrency == null)
            return new Status(Status.Code.ERROR, Messages.MsgCheckSecurityWithoutCurrency);

        if (transaction.getCurrencyCode().equals(securityCurrency))
        {
            // then gross value unit must not be set
            Optional<Unit> grossValue = transaction.getUnit(Transaction.Unit.Type.GROSS_VALUE);
            if (grossValue.isPresent())
            {
                String grossValueCurrencyCode = grossValue.get().getForex() != null
                                ? grossValue.get().getForex().getCurrencyCode() : ""; //$NON-NLS-1$
                return new Status(Status.Code.ERROR, MessageFormat.format(Messages.MsgCheckGrossValueUnitNotValid,
                                grossValueCurrencyCode, securityCurrency));
            }

            // then other units must not have any forex information
            Optional<Unit> unit = transaction.getUnits().filter(u -> u.getForex() != null).findAny();
            if (unit.isPresent())
                return new Status(Status.Code.ERROR, MessageFormat.format(Messages.MsgCheckUnitForexNotValid,
                                Values.Money.format(unit.get().getForex())));
        }
        else
        {
            // then gross value must be set
            Optional<Unit> grossValue = transaction.getUnit(Transaction.Unit.Type.GROSS_VALUE);
            if (!grossValue.isPresent())
                return new Status(Status.Code.ERROR, MessageFormat.format(Messages.MsgCheckGrossValueUnitMissing,
                                transaction.getCurrencyCode(), securityCurrency));

            // then gross value forex must match security
            String forex = grossValue.get().getForex() != null ? grossValue.get().getForex().getCurrencyCode() : null;
            if (!securityCurrency.equals(forex))
                return new Status(Status.Code.ERROR, MessageFormat.format(Messages.MsgCheckGrossValueUnitForexMismatch,
                                forex, securityCurrency));

            // then other units must have matching currency (if they have forex)
            Optional<Unit> unit = transaction.getUnits() //
                            .filter(u -> u.getForex() != null) //
                            .filter(u -> !u.getForex().getCurrencyCode().equals(securityCurrency)) //
                            .findAny();
            if (unit.isPresent())
                return new Status(Status.Code.ERROR, MessageFormat.format(Messages.MsgCheckUnitForexMismatch,
                                Values.Money.format(unit.get().getForex()), securityCurrency));
        }

        return Status.OK_STATUS;
    }

}
