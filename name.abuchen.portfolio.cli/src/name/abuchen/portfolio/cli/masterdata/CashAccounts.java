package name.abuchen.portfolio.cli.masterdata;

import java.util.List;

import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.command.TableColumn;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;

/** The cash account kind. */
public final class CashAccounts
{
    public static final MasterDataKind<Account> CASH_ACCOUNTS = MasterDataKind
                    .declare("cash-account", "cash account", Client::getAccounts, Account::getUUID, Account::getName)
                    .item("A cash account.", "Account name; empty if the file has none.")
                    .write(Writer.required("Account name; never empty.", "a name", Account::setName))
                    .always("currencyCode", Schema.string("ISO currency code of the account."),
                                    Account::getCurrencyCode)
                    .write(Writer.required("ISO currency code of the account.", "a currency",
                                    Account::setCurrencyCode).check(Checks::isCurrency, "invalid-currency",
                                                    Checks::notACurrency, null))
                    .ifSet("note", Schema.string("Note, if set."), Account::getNote)
                    .write(Writer.optional("Note.", Account::setNote))
                    .retired("Whether the account is retired.", Account::isRetired)
                    .write(Writer.flag("Whether the cash account is retired", Account::setRetired))
                    .updatedAt(Account::getUpdatedAt)
                    .lookupKeys("name")
                    .candidateDetails("currencyCode")
                    .creates(Account::new, Client::addAccount, null)
                    .validator(CashAccounts::validate)
                    .operations("Adds a cash account. The name and the currency are required.",
                                    "Updates a cash account by UUID. Only the fields present change; null or an empty string clears the note. The currency can't change once the account has transactions.")
                    .list("Cash accounts: UUID, name, currency, note and retired flag",
                                    List.of(TableColumn.of("uuid", "UUID"), TableColumn.capped("name", "Name", 40),
                                                    TableColumn.of("currencyCode", "Currency"),
                                                    TableColumn.of("retired", "Retired")));

    private CashAccounts()
    {
    }

    private static void validate(Writes.Validation<Account> validation)
    {
        var existing = validation.existing();
        if (existing != null)
            Checks.currencyKept(validation, "account", existing.getCurrencyCode(),
                            () -> !existing.getTransactions().isEmpty());
    }
}
