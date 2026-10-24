package name.abuchen.portfolio.cli;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;

final class InvestmentAccountsCommand
{
    private static final Schema ITEM = Schema.object("An investment account that holds instruments.")
                    .property("uuid", Schema.string("Canonical UUID."))
                    .property("name", Schema.string("Account name; empty if the file has none."))
                    .optionalProperty("referenceCashAccount",
                                    Schema.string("UUID of the cash account that settles this account, if any."))
                    .optionalProperty("note", Schema.string("Note, if set."))
                    .property("retired", Schema.bool("Whether the account is retired."))
                    .optionalProperty("updatedAt", MasterData.UPDATED_AT);

    static final MasterDataList.Source<Portfolio> SOURCE = new MasterDataList.Source<Portfolio>(Client::getPortfolios,
                    Portfolio::getUUID, Portfolio::getName, client -> InvestmentAccountsCommand::render);

    static final CommandSpec SPEC = MasterDataList.command("investment-accounts",
                    "Investment accounts: UUID, name, reference cash account, note and retired flag", ITEM,
                    List.of(TableColumn.of("uuid", "UUID"), TableColumn.capped("name", "Name", 40),
                                    TableColumn.of("referenceCashAccount", "Reference cash account"),
                                    TableColumn.of("retired", "Retired")),
                    SOURCE, Portfolio::getUpdatedAt);

    private InvestmentAccountsCommand()
    {
    }

    private static Map<String, Object> render(Portfolio portfolio)
    {
        var json = new LinkedHashMap<String, Object>();
        json.put("uuid", portfolio.getUUID());
        json.put("name", MasterData.orEmpty(portfolio.getName()));
        if (portfolio.getReferenceAccount() != null)
            json.put("referenceCashAccount", portfolio.getReferenceAccount().getUUID());
        MasterData.putIfSet(json, "note", portfolio.getNote());
        json.put("retired", portfolio.isRetired());
        MasterData.putUpdatedAt(json, portfolio.getUpdatedAt());
        return json;
    }
}
