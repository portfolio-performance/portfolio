package name.abuchen.portfolio.cli.query;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.cli.command.CommandSpec;
import name.abuchen.portfolio.cli.command.MasterData;
import name.abuchen.portfolio.cli.command.MasterDataList;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.command.TableColumn;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;

public final class CashAccountsCommand
{
    private static final Schema ITEM = Schema.object("A cash account.")
                    .property("uuid", Schema.string("Canonical UUID."))
                    .property("name", Schema.string("Account name; empty if the file has none."))
                    .property("currencyCode", Schema.string("ISO currency code of the account."))
                    .optionalProperty("note", Schema.string("Note, if set."))
                    .property("retired", Schema.bool("Whether the account is retired."))
                    .optionalProperty("updatedAt", MasterData.UPDATED_AT);

    public static final MasterDataList.Source<Account> SOURCE = new MasterDataList.Source<Account>(Client::getAccounts,
                    Account::getUUID, Account::getName, client -> CashAccountsCommand::render);

    public static final CommandSpec SPEC = MasterDataList.command("cash-accounts",
                    "Cash accounts: UUID, name, currency, note and retired flag", ITEM,
                    List.of(TableColumn.of("uuid", "UUID"), TableColumn.capped("name", "Name", 40),
                                    TableColumn.of("currencyCode", "Currency"), TableColumn.of("retired", "Retired")),
                    SOURCE, Account::getUpdatedAt);

    private CashAccountsCommand()
    {
    }

    private static Map<String, Object> render(Account account)
    {
        var json = new LinkedHashMap<String, Object>();
        json.put("uuid", account.getUUID());
        json.put("name", MasterData.orEmpty(account.getName()));
        json.put("currencyCode", account.getCurrencyCode());
        MasterData.putIfSet(json, "note", account.getNote());
        json.put("retired", account.isRetired());
        MasterData.putUpdatedAt(json, account.getUpdatedAt());
        return json;
    }
}
