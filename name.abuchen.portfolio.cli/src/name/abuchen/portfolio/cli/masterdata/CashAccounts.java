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
                    .always("currencyCode", Schema.string("ISO currency code of the account."),
                                    Account::getCurrencyCode)
                    .ifSet("note", Schema.string("Note, if set."), Account::getNote)
                    .retired("Whether the account is retired.", Account::isRetired)
                    .updatedAt(Account::getUpdatedAt)
                    .lookupKeys("name")
                    .candidateDetails("currencyCode")
                    .list("Cash accounts: UUID, name, currency, note and retired flag",
                                    List.of(TableColumn.of("uuid", "UUID"), TableColumn.capped("name", "Name", 40),
                                                    TableColumn.of("currencyCode", "Currency"),
                                                    TableColumn.of("retired", "Retired")));

    private CashAccounts()
    {
    }
}
