package name.abuchen.portfolio.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class TransactionDetailsQuery
{
    private final Client client;
    private Predicate<TransactionDetails> predicate = details -> true;

    TransactionDetailsQuery(Client client)
    {
        this.client = Objects.requireNonNull(client);
    }

    public TransactionDetailsQuery legacyOnly()
    {
        return and(TransactionDetails::isLegacy);
    }

    public TransactionDetailsQuery ledgerOnly()
    {
        return and(TransactionDetails::isLedgerProjection);
    }

    public TransactionDetailsQuery withAccount(Account account)
    {
        return and(TransactionDetails.accountFilter(account));
    }

    public TransactionDetailsQuery withPortfolio(Portfolio portfolio)
    {
        return and(TransactionDetails.portfolioFilter(portfolio));
    }

    public TransactionDetailsQuery withSecurity(Security security)
    {
        return and(TransactionDetails.securityFilter(security));
    }

    public TransactionDetailsQuery withType(VisibleTransactionKind type)
    {
        return and(TransactionDetails.typeFilter(type));
    }

    public TransactionDetailsQuery between(LocalDate from, LocalDate to)
    {
        return and(TransactionDetails.betweenFilter(from, to));
    }

    public Stream<TransactionDetails> stream()
    {
        return TransactionDetails.stream(client).filter(predicate);
    }

    private TransactionDetailsQuery and(Predicate<TransactionDetails> filter)
    {
        predicate = predicate.and(filter);
        return this;
    }
}
