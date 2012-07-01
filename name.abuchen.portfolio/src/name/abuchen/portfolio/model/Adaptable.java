package name.abuchen.portfolio.model;

public interface Adaptable
{
    <T> T adapt(Class<T> type);
}
