package name.abuchen.portfolio.cli.apply;

import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.cli.command.OperationSpec;
import name.abuchen.portfolio.cli.command.Outcome;
import name.abuchen.portfolio.cli.masterdata.CashAccounts;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;

/**
 * The write operations on cash accounts and investment accounts. Fields are
 * named as in the <code>cash-accounts</code> and
 * <code>investment-accounts</code> outputs.
 */
public final class AccountOperations
{
    private static final String REFERENCE = "referenceCashAccount";

    private static final EntityOperations<Account> CASH = new EntityOperations<>(
                    new EntityOperations.Definition<Account>(Outcome.Entity.CASH_ACCOUNT, //
                                    "Adds a cash account. The name and the currency are required.",
                                    "Updates a cash account by UUID. Only the fields present change; null or an empty string clears the note. The currency can't change once the account has transactions.",
                                    List.of(EntityOperations.Field.required("name", "Account name; never empty.",
                                                    "a name", Account::getName, Account::setName),
                                                    EntityOperations.Field.required("currencyCode",
                                                                    "ISO currency code of the account.",
                                                                    "a currency", Account::getCurrencyCode,
                                                                    Account::setCurrencyCode),
                                                    EntityOperations.Field.optional("note", "Note.",
                                                                    Account::getNote, Account::setNote),
                                                    EntityOperations.Field.retired("cash account",
                                                                    Account::isRetired, Account::setRetired)),
                                    Client::getAccounts, Account::new, Client::addAccount, Account::getUUID,
                                    AccountOperations::validateCash, null));

    private static final EntityOperations<Portfolio> INVESTMENT = new EntityOperations<>(
                    new EntityOperations.Definition<Portfolio>(Outcome.Entity.INVESTMENT_ACCOUNT, //
                                    "Adds an investment account. The name and a reference cash account that is already in the file are required.",
                                    "Updates an investment account by UUID. Only the fields present change; null or an empty string clears the note. A new reference cash account must already be in the file.",
                                    List.of(EntityOperations.Field.required("name", "Account name; never empty.",
                                                    "a name", Portfolio::getName, Portfolio::setName),
                                                    EntityOperations.Field.requiredWithContext(REFERENCE,
                                                                    "UUID of the cash account that settles this account; it must already be in the file.",
                                                                    "a reference cash account",
                                                                    AccountOperations::referenceUuid,
                                                                    AccountOperations::setReference),
                                                    EntityOperations.Field.optional("note", "Note.",
                                                                    Portfolio::getNote, Portfolio::setNote),
                                                    EntityOperations.Field.retired("investment account",
                                                                    Portfolio::isRetired, Portfolio::setRetired)),
                                    Client::getPortfolios, Portfolio::new, Client::addPortfolio, Portfolio::getUUID,
                                    AccountOperations::validateInvestment, null));

    static final OperationSpec<OperationContext> CASH_ADD = CASH.addOperation();
    static final OperationSpec<OperationContext> CASH_UPDATE = CASH.updateOperation();
    static final OperationSpec<OperationContext> INVESTMENT_ADD = INVESTMENT.addOperation();
    static final OperationSpec<OperationContext> INVESTMENT_UPDATE = INVESTMENT.updateOperation();

    static final List<OperationSpec<OperationContext>> ALL = List.of(CASH_ADD, CASH_UPDATE, INVESTMENT_ADD,
                    INVESTMENT_UPDATE);

    private AccountOperations()
    {
    }

    private static void validateCash(OperationContext context, Map<String, Object> values, Account existing)
    {
        EntityOperations.validateCurrency(context, values);

        if (existing != null)
            EntityOperations.validateCurrencyKept(context, "account", values, existing.getCurrencyCode(),
                            () -> !existing.getTransactions().isEmpty());
    }

    private static void validateInvestment(OperationContext context, Map<String, Object> values, Portfolio existing)
    {
        if (values.get(REFERENCE) instanceof String uuid)
            context.input().find(REFERENCE, uuid, CashAccounts.CASH_ACCOUNTS.entityKind());
    }

    private static Object referenceUuid(Portfolio portfolio)
    {
        return portfolio.getReferenceAccount() != null ? portfolio.getReferenceAccount().getUUID() : null;
    }

    /** The validator has already made sure that the cash account exists. */
    private static void setReference(OperationContext context, Portfolio portfolio, Object uuid)
    {
        portfolio.setReferenceAccount(CashAccounts.CASH_ACCOUNTS.entityKind()
                        .find(context.client(), context::existedBeforeBatch, (String) uuid).orElseThrow());
    }
}
