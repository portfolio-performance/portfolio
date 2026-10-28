package name.abuchen.portfolio.cli.masterdata;

import java.util.List;

import name.abuchen.portfolio.cli.command.ErrorCode;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.command.TableColumn;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;

/** The investment account kind: accounts that hold instruments. */
public final class InvestmentAccounts
{
    private static final String REFERENCE = "referenceCashAccount";

    public static final MasterDataKind<Portfolio> INVESTMENT_ACCOUNTS = MasterDataKind
                    .declare("investment-account", "investment account", Client::getPortfolios, Portfolio::getUUID,
                                    Portfolio::getName)
                    .item("An investment account that holds instruments.", "Account name; empty if the file has none.")
                    .write(Writer.required("Account name; never empty.", "a name", Portfolio::setName))
                    .ifSet(REFERENCE, Schema.string("UUID of the cash account that settles this account, if any."),
                                    portfolio -> portfolio.getReferenceAccount() == null ? null
                                                    : portfolio.getReferenceAccount().getUUID())
                    .write(Writer.requiredExisting(
                                    "UUID of the cash account that settles this account; it must already be in the file.",
                                    "a reference cash account", CashAccounts.CASH_ACCOUNTS.entityKind(),
                                    Portfolio::setReferenceAccount))
                    .ifSet("note", Schema.string("Note, if set."), Portfolio::getNote)
                    .write(Writer.optional("Note.", Portfolio::setNote))
                    .retired("Whether the account is retired.", Portfolio::isRetired)
                    .write(Writer.flag("Whether the investment account is retired", Portfolio::setRetired))
                    .updatedAt(Portfolio::getUpdatedAt)
                    .lookupKeys("name")
                    .creates(Portfolio::new, Client::addPortfolio, null)
                    .validator(InvestmentAccounts::validate, ErrorCode.REFERENCE_NOT_FOUND)
                    .operations("Adds an investment account. The name and a reference cash account that is already in the file are required.",
                                    "Updates an investment account by UUID. Only the fields present change; null or an empty string clears the note. A new reference cash account must already be in the file.")
                    .list("Investment accounts: UUID, name, reference cash account, note and retired flag",
                                    List.of(TableColumn.of("uuid", "UUID"), TableColumn.capped("name", "Name", 40),
                                                    TableColumn.of(REFERENCE, "Reference cash account"),
                                                    TableColumn.of("retired", "Retired")));

    private InvestmentAccounts()
    {
    }

    /** The reference cash account must be an existing entity. */
    private static void validate(Writes.Validation<Portfolio> validation)
    {
        if (validation.values().get(REFERENCE) instanceof String uuid)
            validation.requireExisting(REFERENCE, uuid, CashAccounts.CASH_ACCOUNTS.entityKind());
    }
}
