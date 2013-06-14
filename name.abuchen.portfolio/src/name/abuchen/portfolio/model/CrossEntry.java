package name.abuchen.portfolio.model;

public interface CrossEntry
{
    void updateFrom(Transaction t);

    void delete();

    Object getEntity(Transaction t);

    Transaction getCrossTransaction(Transaction t);

    Object getCrossEntity(Transaction t);
}
