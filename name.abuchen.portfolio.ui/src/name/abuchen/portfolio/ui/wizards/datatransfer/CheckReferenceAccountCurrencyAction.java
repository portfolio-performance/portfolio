package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.text.MessageFormat;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.ImportAction.Status.Code;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.ui.Messages;

/**
 * Checks the currency of transactions which are booked into a portfolio
 * without an account (deliveries, portfolio transfers and buy/sell
 * transactions imported as deliveries). They are valid if the currency
 * matches the reference account of the portfolio or if an account in the
 * currency of the transaction is selected. Otherwise they get an error and
 * are not imported. The portfolios are resolved exactly as for the import.
 * <p>
 * Buy/sell transactions are booked on the chosen account whose currency
 * is checked by the CheckCurrenciesAction, therefore only a missing
 * reference account is reported for them.
 */
final class CheckReferenceAccountCurrencyAction implements ImportAction
{
    private final BooleanSupplier convertToDelivery;
    private final Function<String, Account> selectedAccount;

    /**
     * @param convertToDelivery
     *            whether buy/sell transactions are imported as deliveries
     * @param selectedAccount
     *            the account selected for a currency on the wizard page (or
     *            null if none is selected)
     */
    CheckReferenceAccountCurrencyAction(BooleanSupplier convertToDelivery, Function<String, Account> selectedAccount)
    {
        this.convertToDelivery = convertToDelivery;
        this.selectedAccount = selectedAccount;
    }

    @Override
    public Status process(PortfolioTransaction transaction, Portfolio portfolio)
    {
        return check(transaction.getCurrencyCode(), portfolio);
    }

    @Override
    public Status process(BuySellEntry entry, Account account, Portfolio portfolio)
    {
        if (convertToDelivery.getAsBoolean())
            return check(entry.getPortfolioTransaction().getCurrencyCode(), portfolio);

        if (portfolio != null && portfolio.getReferenceAccount() == null)
            return new Status(Code.ERROR, Messages.MsgMissingReferenceAccount);

        return Status.OK_STATUS;
    }

    @Override
    public Status process(PortfolioTransferEntry entry, Portfolio source, Portfolio target)
    {
        var status = check(entry.getSourceTransaction().getCurrencyCode(), source);
        if (status.getCode() != Code.OK)
            return status;

        return check(entry.getTargetTransaction().getCurrencyCode(), target);
    }

    private Status check(String currencyCode, Portfolio portfolio)
    {
        if (portfolio == null)
            return Status.OK_STATUS;

        var referenceAccount = portfolio.getReferenceAccount();
        if (referenceAccount == null)
            return new Status(Code.ERROR, Messages.MsgMissingReferenceAccount);

        if (currencyCode.equals(referenceAccount.getCurrencyCode()))
            return Status.OK_STATUS;

        // an account in the currency of the transaction is selected
        var account = selectedAccount.apply(currencyCode);
        if (account != null && currencyCode.equals(account.getCurrencyCode()))
            return Status.OK_STATUS;

        return new Status(Code.ERROR, MessageFormat.format(Messages.MsgErrorConvertToBuySellCurrencyMismatch,
                        currencyCode, referenceAccount.getCurrencyCode(), referenceAccount.getName()));
    }
}
