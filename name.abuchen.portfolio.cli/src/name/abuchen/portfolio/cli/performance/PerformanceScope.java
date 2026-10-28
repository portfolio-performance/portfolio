package name.abuchen.portfolio.cli.performance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.command.Declaration;
import name.abuchen.portfolio.cli.command.ErrorCode;
import name.abuchen.portfolio.cli.command.Facet;
import name.abuchen.portfolio.cli.command.MasterData;
import name.abuchen.portfolio.cli.command.OptionSpec;
import name.abuchen.portfolio.cli.command.ReferenceOption;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.command.Warning;
import name.abuchen.portfolio.cli.command.WarningSpec;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.util.Interval;

/**
 * What <code>performance</code> is about: the whole client, or one investment
 * account, cash account or instrument, chosen with the reference options; and
 * whether it is broken down by instrument with <code>--by</code>, which only
 * the whole client and an investment account can be.
 */
final class PerformanceScope
{
    private static final ReferenceOption<Portfolio> INVESTMENT_ACCOUNT = ReferenceOption.investmentAccount(
                    "investment-account", "Report this investment account only. One scope option at most.");

    private static final ReferenceOption<Account> CASH_ACCOUNT = ReferenceOption.cashAccount("cash-account",
                    "Report this cash account only. One scope option at most.");

    private static final ReferenceOption<Security> INSTRUMENT = ReferenceOption.instrument("instrument",
                    "Report this instrument only. One scope option at most.");

    private static final OptionSpec WITH_REFERENCE_CASH_ACCOUNT = OptionSpec.flag("with-reference-cash-account",
                    "Report the investment account together with its reference cash account. Only with --investment-account; fails with no-reference-cash-account if the account has none.");

    private static final String BY_INSTRUMENT = "instrument";

    private static final OptionSpec BY = new OptionSpec("by", "dimension",
                    "Break the figures down: instrument is the only dimension. Returns one record per instrument active in the period, sorted by name, instead of the figures of the scope. With the whole client or an investment account.");

    private static final WarningSpec NO_ACTIVITY = new WarningSpec("no-activity-in-period",
                    "The instrument of --instrument was not held at the start or end of the period and had no transaction in it, so no figure is reported. Carries the uuid and name of the instrument.");

    /**
     * The scope as a facet: it owns the scope options, <code>--by</code>,
     * which only some scopes can be broken down by, and the rules between
     * them. <code>meta.scope</code> gets what it resolved to. It declares
     * <code>no-activity-in-period</code>, which the command warns about for a
     * quiet instrument.
     */
    static final Facet<PerformanceScope> FACET = Facet.of(Declaration.empty()
                    .options(INVESTMENT_ACCOUNT.spec(), CASH_ACCOUNT.spec(), INSTRUMENT.spec(),
                                    WITH_REFERENCE_CASH_ACCOUNT, BY)
                    .meta("scope", schema()).warnings(NO_ACTIVITY)
                    .codes(ErrorCode.CONFLICTING_OPTIONS, ErrorCode.OPTION_REQUIRED,
                                    ErrorCode.NO_REFERENCE_CASH_ACCOUNT)
                    .needingClient(), run -> resolve(run, run.client()), (scope, out) -> out.meta("scope", scope.meta()));

    /** The kinds of scope, as <code>meta.scope.kind</code> names them. */
    private enum Kind
    {
        CLIENT("client"), INVESTMENT_ACCOUNT("investment-account"), CASH_ACCOUNT("cash-account"), INSTRUMENT(
                        "instrument");

        private final String id;

        Kind(String id)
        {
            this.id = id;
        }
    }

    /** What the figures are about: each kind of scope knows how core reports it. */
    private sealed interface Target
    {
        Kind kind();

        PerformanceIndex index(Client client, CurrencyConverter converter, Interval interval);

        /** The client as far as the target covers it, if it can be broken down by instrument. */
        default Optional<Client> narrowed(Client client)
        {
            return Optional.empty();
        }

        /** The instrument the target is about, if it was not active in the period. */
        default Optional<Security> quietInstrument(Client client, CurrencyConverter converter, Interval interval)
        {
            return Optional.empty();
        }

        /** Adds what the target resolved to. */
        default void putInto(Map<String, Object> scope)
        {
        }
    }

    private record WholeClient() implements Target
    {
        @Override
        public Kind kind()
        {
            return Kind.CLIENT;
        }

        @Override
        public PerformanceIndex index(Client client, CurrencyConverter converter, Interval interval)
        {
            return PerformanceIndex.forClient(client, converter, interval, new ArrayList<>());
        }

        @Override
        public Optional<Client> narrowed(Client client)
        {
            return Optional.of(client);
        }
    }

    private record OneInvestmentAccount(Portfolio account, boolean withReferenceCashAccount) implements Target
    {
        @Override
        public Kind kind()
        {
            return Kind.INVESTMENT_ACCOUNT;
        }

        @Override
        public PerformanceIndex index(Client client, CurrencyConverter converter, Interval interval)
        {
            return withReferenceCashAccount
                            ? PerformanceIndex.forPortfolioPlusAccount(client, converter, account, interval,
                                            new ArrayList<>())
                            : PerformanceIndex.forPortfolio(client, converter, account, interval, new ArrayList<>());
        }

        @Override
        public Optional<Client> narrowed(Client client)
        {
            return Optional.of((withReferenceCashAccount
                            ? new PortfolioClientFilter(account, account.getReferenceAccount())
                            : new PortfolioClientFilter(account)).filter(client));
        }

        @Override
        public void putInto(Map<String, Object> scope)
        {
            MasterData.putIdentity(scope, account.getUUID(), account.getName());
            if (withReferenceCashAccount)
            {
                var reference = account.getReferenceAccount();
                var json = new LinkedHashMap<String, Object>();
                MasterData.putIdentity(json, reference.getUUID(), reference.getName());
                scope.put("referenceCashAccount", json);
            }
        }
    }

    private record OneCashAccount(Account account) implements Target
    {
        @Override
        public Kind kind()
        {
            return Kind.CASH_ACCOUNT;
        }

        @Override
        public PerformanceIndex index(Client client, CurrencyConverter converter, Interval interval)
        {
            return PerformanceIndex.forAccount(client, converter, account, interval, new ArrayList<>());
        }

        @Override
        public void putInto(Map<String, Object> scope)
        {
            MasterData.putIdentity(scope, account.getUUID(), account.getName());
        }
    }

    private record OneInstrument(Security security) implements Target
    {
        @Override
        public Kind kind()
        {
            return Kind.INSTRUMENT;
        }

        @Override
        public Optional<Security> quietInstrument(Client client, CurrencyConverter converter, Interval interval)
        {
            var active = LazySecurityPerformanceSnapshot.create(client, converter, interval).getRecord(security)
                            .isPresent();
            return active ? Optional.empty() : Optional.of(security);
        }

        @Override
        public PerformanceIndex index(Client client, CurrencyConverter converter, Interval interval)
        {
            return PerformanceIndex.forInvestment(client, converter, security, interval, new ArrayList<>());
        }

        @Override
        public void putInto(Map<String, Object> scope)
        {
            MasterData.putIdentity(scope, security.getUUID(), security.getName());
        }
    }

    private final Target target;
    private final Optional<Client> breakdown;

    private PerformanceScope(Target target, Optional<Client> breakdown)
    {
        this.target = target;
        this.breakdown = breakdown;
    }

    /**
     * @throws CliException
     *             a usage error if the options conflict, or a failure if a
     *             reference does not resolve or the investment account has no
     *             reference cash account to add
     */
    private static PerformanceScope resolve(CommandContext context, Client client) throws CliException
    {
        var target = target(context, client);
        if (!byInstrument(context))
            return new PerformanceScope(target, Optional.empty());

        var narrowed = target.narrowed(client).orElseThrow(PerformanceScope::unsupportedScope);
        return new PerformanceScope(target, Optional.of(narrowed));
    }

    private static Target target(CommandContext context, Client client) throws CliException
    {
        var given = new ArrayList<String>();
        for (var option : List.of(INVESTMENT_ACCOUNT.spec(), CASH_ACCOUNT.spec(), INSTRUMENT.spec()))
        {
            if (context.given(option))
                given.add("--" + option.name());
        }
        if (given.size() > 1)
            throw CliException.usage(ErrorCode.CONFLICTING_OPTIONS, "Conflicting options",
                            "At most one of --" + INVESTMENT_ACCOUNT.name() + ", --" + CASH_ACCOUNT.name()
                                            + " and --" + INSTRUMENT.name() + " is allowed, but got "
                                            + String.join(" and ", given) + ".")
                            .with("options", given);

        var withReferenceCashAccount = context.flag(WITH_REFERENCE_CASH_ACCOUNT);
        var investmentAccount = context.reference(INVESTMENT_ACCOUNT, client);
        if (withReferenceCashAccount && investmentAccount.isEmpty())
            throw CliException.usage(ErrorCode.OPTION_REQUIRED, "Option required",
                            "Option '--" + WITH_REFERENCE_CASH_ACCOUNT.name() + "' needs the option '--"
                                            + INVESTMENT_ACCOUNT.name() + " <" + INVESTMENT_ACCOUNT.spec().valueName()
                                            + ">'.")
                            .with("option", INVESTMENT_ACCOUNT.name());

        if (investmentAccount.isPresent())
        {
            var account = investmentAccount.get();
            if (withReferenceCashAccount && account.getReferenceAccount() == null)
                throw CliException.failure(ErrorCode.NO_REFERENCE_CASH_ACCOUNT, "No reference cash account",
                                "The investment account '" + MasterData.orEmpty(account.getName())
                                                + "' has no reference cash account.",
                                422).with("uuid", account.getUUID());
            return new OneInvestmentAccount(account, withReferenceCashAccount);
        }

        var cashAccount = context.reference(CASH_ACCOUNT, client);
        if (cashAccount.isPresent())
            return new OneCashAccount(cashAccount.get());

        var instrument = context.reference(INSTRUMENT, client);
        if (instrument.isPresent())
            return new OneInstrument(instrument.get());

        return new WholeClient();
    }

    /**
     * @return whether the figures are to be broken down by instrument
     * @throws CliException
     *             <code>invalid-option-value</code> for any other dimension
     */
    private static boolean byInstrument(CommandContext context) throws CliException
    {
        var value = context.text(BY);
        if (value.isPresent() && !BY_INSTRUMENT.equals(value.get()))
            throw BY.invalid(value.get(), "'" + BY_INSTRUMENT + "'");
        return value.isPresent();
    }

    /**
     * @return a usage error: the scope cannot be broken down by instrument
     */
    private static CliException unsupportedScope()
    {
        return CliException.usage(ErrorCode.CONFLICTING_OPTIONS, "Conflicting options",
                        "Option '--" + BY.name()
                                        + "' works with the whole client or an investment account, not with '--cash-account' or '--instrument'.")
                        .with("option", BY.name());
    }

    /**
     * With <code>--by instrument</code>, the client as far as the scope covers
     * it, on which per-instrument figures are calculated; empty without it.
     */
    Optional<Client> breakdown()
    {
        return breakdown;
    }

    /** The warning that the instrument of the scope was not active in the period. */
    static Warning noActivity(Security security)
    {
        return new Warning(NO_ACTIVITY.code(),
                        "The instrument '" + MasterData.orEmpty(security.getName())
                                        + "' was not held at the start or end of the period and had no transaction in it.")
                                                        .with("uuid", security.getUUID())
                                                        .with("name", MasterData.orEmpty(security.getName()));
    }

    /**
     * @return the instrument of an instrument scope that core's per-security
     *         snapshot has no record for, i.e. that was not active in the
     *         period
     */
    Optional<Security> quietInstrument(Client client, CurrencyConverter converter, Interval interval)
    {
        return target.quietInstrument(client, converter, interval);
    }

    /** Core's performance index for the scope. */
    PerformanceIndex index(Client client, CurrencyConverter converter, Interval interval)
    {
        return target.index(client, converter, interval);
    }

    /** <code>meta.scope</code>: the kind and what it resolved to. */
    private Map<String, Object> meta()
    {
        var scope = new LinkedHashMap<String, Object>();
        scope.put("kind", target.kind().id);
        target.putInto(scope);
        return scope;
    }

    private static Schema schema()
    {
        return Schema.object("What the figures are about.")
                        .property("kind", Schema.enumOf("The kind of scope.",
                                        Arrays.stream(Kind.values()).map(kind -> kind.id).toList()))
                        .optionalProperty("uuid", Schema.string("UUID of the account or instrument."))
                        .optionalProperty("name", Schema.string("Its name."))
                        .optionalProperty("referenceCashAccount", Schema.object(
                                        "The reference cash account reported together with the investment account. Present only with --with-reference-cash-account.")
                                        .property("uuid", Schema.string("Its UUID."))
                                        .property("name", Schema.string("Its name.")));
    }
}
