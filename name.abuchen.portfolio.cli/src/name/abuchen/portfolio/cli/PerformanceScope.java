package name.abuchen.portfolio.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.util.Interval;

/**
 * What <code>performance</code> is about: the whole client, or one investment
 * account, cash account or instrument, chosen with the reference options.
 */
final class PerformanceScope
{
    private static final OptionSpec INVESTMENT_ACCOUNT = new OptionSpec("investment-account", "ref",
                    "Report this investment account only: its UUID, or for convenience its exact name. Names are accepted on reads only. At most one of --investment-account, --cash-account and --instrument.");

    private static final OptionSpec CASH_ACCOUNT = new OptionSpec("cash-account", "ref",
                    "Report this cash account only: its UUID, or for convenience its exact name. Names are accepted on reads only. At most one of --investment-account, --cash-account and --instrument.");

    private static final OptionSpec INSTRUMENT = new OptionSpec("instrument", "ref",
                    "Report this instrument only: its UUID, or for convenience its ISIN, ticker or exact name. Name, ISIN and ticker references are accepted on reads only. At most one of --investment-account, --cash-account and --instrument.");

    private static final OptionSpec WITH_REFERENCE_CASH_ACCOUNT = OptionSpec.flag("with-reference-cash-account",
                    "Report the investment account together with its reference cash account. Only with --investment-account; fails with no-reference-cash-account if the account has none.");

    static final List<OptionSpec> OPTIONS = List.of(INVESTMENT_ACCOUNT, CASH_ACCOUNT, INSTRUMENT,
                    WITH_REFERENCE_CASH_ACCOUNT);

    private final Optional<Portfolio> investmentAccount;
    private final Optional<Account> cashAccount;
    private final Optional<Security> instrument;
    private final boolean withReferenceCashAccount;

    private PerformanceScope(Optional<Portfolio> investmentAccount, Optional<Account> cashAccount,
                    Optional<Security> instrument, boolean withReferenceCashAccount)
    {
        this.investmentAccount = investmentAccount;
        this.cashAccount = cashAccount;
        this.instrument = instrument;
        this.withReferenceCashAccount = withReferenceCashAccount;
    }

    /**
     * @throws CliException
     *             a usage error if the options conflict, or a failure if a
     *             reference does not resolve or the investment account has no
     *             reference cash account to add
     */
    static PerformanceScope resolve(CommandContext context, Client client) throws CliException
    {
        var given = new ArrayList<String>();
        for (var option : List.of(INVESTMENT_ACCOUNT, CASH_ACCOUNT, INSTRUMENT))
        {
            if (context.option(option.name()).isPresent())
                given.add("--" + option.name());
        }
        if (given.size() > 1)
            throw CliException.usage("conflicting-options", "Conflicting options",
                            "At most one of --" + INVESTMENT_ACCOUNT.name() + ", --" + CASH_ACCOUNT.name()
                                            + " and --" + INSTRUMENT.name() + " is allowed, but got "
                                            + String.join(" and ", given) + ".")
                            .with("options", given);

        var withReferenceCashAccount = context.flag(WITH_REFERENCE_CASH_ACCOUNT);
        if (withReferenceCashAccount && context.option(INVESTMENT_ACCOUNT.name()).isEmpty())
            throw CliException.usage("conflicting-options", "Conflicting options",
                            "Option '--" + WITH_REFERENCE_CASH_ACCOUNT.name() + "' needs '--"
                                            + INVESTMENT_ACCOUNT.name() + "'.")
                            .with("option", WITH_REFERENCE_CASH_ACCOUNT.name());

        Optional<Portfolio> investmentAccount = Optional.empty();
        var accountValue = context.option(INVESTMENT_ACCOUNT.name());
        if (accountValue.isPresent())
        {
            var account = References.investmentAccount(client, INVESTMENT_ACCOUNT.name(), accountValue.get());
            if (withReferenceCashAccount && account.getReferenceAccount() == null)
                throw CliException.failure("no-reference-cash-account", "No reference cash account",
                                "The investment account '" + MasterData.orEmpty(account.getName())
                                                + "' has no reference cash account.",
                                422).with("uuid", account.getUUID());
            investmentAccount = Optional.of(account);
        }

        Optional<Account> cashAccount = Optional.empty();
        var cashValue = context.option(CASH_ACCOUNT.name());
        if (cashValue.isPresent())
            cashAccount = Optional.of(References.cashAccount(client, CASH_ACCOUNT.name(), cashValue.get()));

        Optional<Security> instrument = Optional.empty();
        var instrumentValue = context.option(INSTRUMENT.name());
        if (instrumentValue.isPresent())
            instrument = Optional.of(References.instrument(client, INSTRUMENT.name(), instrumentValue.get()));

        return new PerformanceScope(investmentAccount, cashAccount, instrument, withReferenceCashAccount);
    }

    /** The instrument of an instrument scope. */
    Optional<Security> instrument()
    {
        return instrument;
    }

    /** Whether the scope can be broken down by instrument: the client or an investment account. */
    boolean canBreakDownByInstrument()
    {
        return cashAccount.isEmpty() && instrument.isEmpty();
    }

    /** The client as far as the scope covers it, on which per-instrument figures are calculated. */
    Client narrowed(Client client)
    {
        if (investmentAccount.isEmpty())
            return client;

        var account = investmentAccount.get();
        return (withReferenceCashAccount ? new PortfolioClientFilter(account, account.getReferenceAccount())
                        : new PortfolioClientFilter(account)).filter(client);
    }

    /** Core's performance index for the scope. */
    PerformanceIndex index(Client client, CurrencyConverter converter, Interval interval)
    {
        if (investmentAccount.isPresent())
        {
            var account = investmentAccount.get();
            return withReferenceCashAccount
                            ? PerformanceIndex.forPortfolioPlusAccount(client, converter, account, interval,
                                            new ArrayList<>())
                            : PerformanceIndex.forPortfolio(client, converter, account, interval, new ArrayList<>());
        }
        if (cashAccount.isPresent())
            return PerformanceIndex.forAccount(client, converter, cashAccount.get(), interval, new ArrayList<>());
        if (instrument.isPresent())
            return PerformanceIndex.forInvestment(client, converter, instrument.get(), interval, new ArrayList<>());
        return PerformanceIndex.forClient(client, converter, interval, new ArrayList<>());
    }

    /** Adds <code>scope</code>: the kind and what it resolved to. */
    void putInto(Map<String, Object> meta)
    {
        var scope = new LinkedHashMap<String, Object>();
        scope.put("kind", kind());
        investmentAccount.ifPresent(account -> {
            putIdentity(scope, account.getUUID(), account.getName());
            if (withReferenceCashAccount)
            {
                var reference = account.getReferenceAccount();
                var json = new LinkedHashMap<String, Object>();
                putIdentity(json, reference.getUUID(), reference.getName());
                scope.put("referenceCashAccount", json);
            }
        });
        cashAccount.ifPresent(account -> putIdentity(scope, account.getUUID(), account.getName()));
        instrument.ifPresent(security -> putIdentity(scope, security.getUUID(), security.getName()));
        meta.put("scope", scope);
    }

    private String kind()
    {
        if (investmentAccount.isPresent())
            return "investment-account";
        if (cashAccount.isPresent())
            return "cash-account";
        return instrument.isPresent() ? "instrument" : "client";
    }

    private static void putIdentity(Map<String, Object> scope, String uuid, String name)
    {
        scope.put("uuid", uuid);
        scope.put("name", MasterData.orEmpty(name));
    }

    /** Declares the members {@link #putInto} adds. */
    static Schema declareIn(Schema meta)
    {
        return meta.property("scope", Schema.object("What the figures are about.")
                        .property("kind", Schema.enumOf("The kind of scope.",
                                        List.of("client", "investment-account", "cash-account", "instrument")))
                        .optionalProperty("uuid", Schema.string("UUID of the account or instrument."))
                        .optionalProperty("name", Schema.string("Its name."))
                        .optionalProperty("referenceCashAccount", Schema.object(
                                        "The reference cash account reported together with the investment account. Present only with --with-reference-cash-account.")
                                        .property("uuid", Schema.string("Its UUID."))
                                        .property("name", Schema.string("Its name."))));
    }
}
