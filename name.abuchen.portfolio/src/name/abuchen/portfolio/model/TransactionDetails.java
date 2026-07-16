package name.abuchen.portfolio.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.projection.DerivedProjectionDescriptor;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.money.Money;

public final class TransactionDetails
{
    private final Client client;
    private final Transaction transaction;
    private final Account account;
    private final Portfolio portfolio;
    private final LedgerBackedTransaction ledgerBacked;

    private TransactionDetails(Client client, Transaction transaction)
    {
        this.client = Objects.requireNonNull(client);
        this.transaction = Objects.requireNonNull(transaction);
        this.ledgerBacked = transaction instanceof LedgerBackedTransaction ledgerTransaction ? ledgerTransaction : null;
        this.account = resolveAccount();
        this.portfolio = resolvePortfolio();
    }

    public static TransactionDetails of(Client client, Transaction transaction)
    {
        return new TransactionDetails(client, transaction);
    }

    public static Stream<TransactionDetails> stream(Client client)
    {
        Set<Transaction> seen = Collections.newSetFromMap(new IdentityHashMap<>());

        return Stream.concat(client.getAccounts().stream().flatMap(account -> stream(client, account)),
                        client.getPortfolios().stream().flatMap(portfolio -> stream(client, portfolio)))
                        .filter(details -> seen.add(details.transaction()));
    }

    public static Stream<TransactionDetails> stream(Client client, Account account)
    {
        return account.getTransactions().stream().map(transaction -> new TransactionDetails(client, transaction));
    }

    public static Stream<TransactionDetails> stream(Client client, Portfolio portfolio)
    {
        return portfolio.getTransactions().stream().map(transaction -> new TransactionDetails(client, transaction));
    }

    public static TransactionDetailsQuery query(Client client)
    {
        return new TransactionDetailsQuery(client);
    }

    public Transaction transaction()
    {
        return transaction;
    }

    public TransactionDetailsSource source()
    {
        return ledgerBacked == null ? TransactionDetailsSource.LEGACY : TransactionDetailsSource.LEDGER_PROJECTION;
    }

    public boolean isLegacy()
    {
        return source() == TransactionDetailsSource.LEGACY;
    }

    public boolean isLedgerProjection()
    {
        return source() == TransactionDetailsSource.LEDGER_PROJECTION;
    }

    public Optional<AccountTransaction> accountTransaction()
    {
        return transaction instanceof AccountTransaction accountTransaction ? Optional.of(accountTransaction)
                        : Optional.empty();
    }

    public Optional<PortfolioTransaction> portfolioTransaction()
    {
        return transaction instanceof PortfolioTransaction portfolioTransaction ? Optional.of(portfolioTransaction)
                        : Optional.empty();
    }

    public Optional<Account> account()
    {
        return Optional.ofNullable(account);
    }

    public Optional<Portfolio> portfolio()
    {
        return Optional.ofNullable(portfolio);
    }

    public Optional<Security> security()
    {
        return Optional.ofNullable(transaction.getSecurity());
    }

    public Optional<Money> amount()
    {
        return Optional.ofNullable(transaction.getCurrencyCode())
                        .map(currencyCode -> Money.of(currencyCode, transaction.getAmount()));
    }

    public Optional<Long> shares()
    {
        long shares = transaction.getShares();
        return shares == 0 ? Optional.empty() : Optional.of(Long.valueOf(shares));
    }

    public List<Transaction.Unit> units()
    {
        return transaction.getUnits().toList();
    }

    public LocalDateTime dateTime()
    {
        return transaction.getDateTime();
    }

    public Optional<VisibleTransactionKind> visibleType()
    {
        return VisibleTransactionKind.of(transaction);
    }

    public Optional<LedgerEntry> ledgerEntry()
    {
        return Optional.ofNullable(ledgerBacked).map(LedgerBackedTransaction::getLedgerEntry);
    }

    public Optional<DerivedProjectionDescriptor> projectionDescriptor()
    {
        return Optional.ofNullable(ledgerBacked).map(LedgerBackedTransaction::getLedgerProjectionDescriptor);
    }

    public Stream<LedgerPosting> ledgerPostings()
    {
        return ledgerEntry().stream().flatMap(entry -> entry.getPostings().stream());
    }

    public Optional<LedgerPosting> primaryPosting()
    {
        return projectionDescriptor().map(DerivedProjectionDescriptor::getPrimaryPosting);
    }

    public Stream<LedgerPosting> unitPostings()
    {
        return projectionDescriptor().stream().flatMap(descriptor -> descriptor.getUnitPostings().stream());
    }

    public Optional<String> localKey()
    {
        return primaryPosting().map(LedgerPosting::getLocalKey);
    }

    public Optional<String> groupKey()
    {
        return primaryPosting().map(LedgerPosting::getGroupKey);
    }

    private Account resolveAccount()
    {
        if (ledgerBacked != null && ledgerBacked.getLedgerProjectionDescriptor().getAccount() != null)
            return ledgerBacked.getLedgerProjectionDescriptor().getAccount();

        if (transaction instanceof AccountTransaction)
            return client.getAccounts().stream().filter(candidate -> candidate.getTransactions().contains(transaction))
                            .findFirst().orElse(null);

        return null;
    }

    private Portfolio resolvePortfolio()
    {
        if (ledgerBacked != null && ledgerBacked.getLedgerProjectionDescriptor().getPortfolio() != null)
            return ledgerBacked.getLedgerProjectionDescriptor().getPortfolio();

        if (transaction instanceof PortfolioTransaction)
            return client.getPortfolios().stream()
                            .filter(candidate -> candidate.getTransactions().contains(transaction)).findFirst()
                            .orElse(null);

        return null;
    }

    static Predicate<TransactionDetails> accountFilter(Account account)
    {
        return details -> details.account().filter(account::equals).isPresent();
    }

    static Predicate<TransactionDetails> portfolioFilter(Portfolio portfolio)
    {
        return details -> details.portfolio().filter(portfolio::equals).isPresent();
    }

    static Predicate<TransactionDetails> securityFilter(Security security)
    {
        return details -> details.security().filter(security::equals).isPresent();
    }

    static Predicate<TransactionDetails> typeFilter(VisibleTransactionKind type)
    {
        return details -> details.visibleType().filter(type::equals).isPresent();
    }

    static Predicate<TransactionDetails> betweenFilter(LocalDate from, LocalDate to)
    {
        return details -> {
            LocalDate date = details.dateTime().toLocalDate();
            return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
        };
    }
}
