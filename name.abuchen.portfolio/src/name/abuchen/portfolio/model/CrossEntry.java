package name.abuchen.portfolio.model;

public interface CrossEntry
{
    public void updateFrom(Transaction t);

    public void delete();

    public Transaction getCrossTransaction(Transaction t);

    public Object getCrossEntity(Transaction t);
}
