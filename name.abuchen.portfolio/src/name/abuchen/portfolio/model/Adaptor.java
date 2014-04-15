package name.abuchen.portfolio.model;

public final class Adaptor
{
    public static <T> T adapt(Class<T> type, Object subject)
    {
        if (subject == null)
            return null;

        if (type.isAssignableFrom(subject.getClass()))
            return type.cast(subject);

        if (subject instanceof Adaptable)
            return ((Adaptable) subject).adapt(type);

        return null;
    }

    private Adaptor()
    {}
}
