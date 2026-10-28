package name.abuchen.portfolio.cli.masterdata;

import java.util.List;

import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.command.TableColumn;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;

/** The investment account kind: accounts that hold instruments. */
public final class InvestmentAccounts
{
    public static final MasterDataKind<Portfolio> INVESTMENT_ACCOUNTS = MasterDataKind
                    .declare("investment-account", "investment account", Client::getPortfolios, Portfolio::getUUID,
                                    Portfolio::getName)
                    .item("An investment account that holds instruments.", "Account name; empty if the file has none.")
                    .ifSet("referenceCashAccount",
                                    Schema.string("UUID of the cash account that settles this account, if any."),
                                    portfolio -> portfolio.getReferenceAccount() == null ? null
                                                    : portfolio.getReferenceAccount().getUUID())
                    .ifSet("note", Schema.string("Note, if set."), Portfolio::getNote)
                    .retired("Whether the account is retired.", Portfolio::isRetired)
                    .updatedAt(Portfolio::getUpdatedAt)
                    .lookupKeys("name")
                    .list("Investment accounts: UUID, name, reference cash account, note and retired flag",
                                    List.of(TableColumn.of("uuid", "UUID"), TableColumn.capped("name", "Name", 40),
                                                    TableColumn.of("referenceCashAccount", "Reference cash account"),
                                                    TableColumn.of("retired", "Retired")));

    private InvestmentAccounts()
    {
    }
}
