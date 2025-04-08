package name.abuchen.portfolio.ui.util.viewers;

@FunctionalInterface
public interface ElementOptionFunction<R>
{
    R apply(Object element, Object option);
}
