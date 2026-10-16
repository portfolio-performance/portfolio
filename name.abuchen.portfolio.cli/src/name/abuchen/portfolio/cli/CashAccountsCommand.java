package name.abuchen.portfolio.cli;

import java.util.LinkedHashMap;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;

final class CashAccountsCommand
{
    private static final Schema ITEM = Schema.object("A cash account.")
                    .property("uuid", Schema.string("Canonical UUID."))
                    .property("name", Schema.string("Account name; empty if the file has none."))
                    .property("currencyCode", Schema.string("ISO currency code of the account."))
                    .optionalProperty("note", Schema.string("Note, if set."))
                    .property("retired", Schema.bool("Whether the account is retired."));

    static final CommandSpec SPEC = MasterDataList.command("cash-accounts",
                    "Cash accounts: UUID, name, currency, note and retired flag", ITEM,
                    new MasterDataList.Source<Account>(Client::getAccounts, Account::getUUID, Account::getName,
                                    client -> CashAccountsCommand::render));

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
        return json;
    }
}
