package name.abuchen.portfolio.cli;

import java.util.LinkedHashMap;
import java.util.Map;

import name.abuchen.portfolio.model.Account;

final class CashAccountsCommand
{
    private static final Schema ITEM = Schema.object("A cash account.")
                    .property("uuid", Schema.string("Canonical UUID."))
                    .property("name", Schema.string("Account name; empty if the file has none."))
                    .property("currencyCode", Schema.string("ISO currency code of the account."))
                    .optionalProperty("note", Schema.string("Note, if set."))
                    .property("retired", Schema.bool("Whether the account is retired (deactivated in the app)."));

    static final CommandSpec SPEC = new CommandSpec("cash-accounts",
                    "Cash accounts: UUID, name, currency, note and retired flag, ordered by name then UUID.", true,
                    Paging.OPTIONS, Paging.dataSchema("A page of cash accounts.", ITEM), ResponseMeta.PAGED_SCHEMA,
                    CashAccountsCommand::execute);

    private CashAccountsCommand()
    {
    }

    private static Response execute(CommandContext context) throws CliException
    {
        var paging = Paging.parse(context);

        var client = context.loadClient().client();

        return Paging.respond(context, client, paging, client.getAccounts(),
                        MasterData.byNameThenUuid(Account::getName, Account::getUUID),
                        CashAccountsCommand::render);
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
