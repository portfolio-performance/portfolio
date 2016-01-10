package name.abuchen.portfolio.ui.util;

import java.util.Comparator;
import java.util.function.Function;

public final class AttributeComparator implements Comparator<Object>
{
    private Function<Object, Comparable<?>> provider;

    public AttributeComparator(Function<Object, Comparable<?>> attributeProvider)
    {
        super();
        this.provider = attributeProvider;
    }

    @Override
    public int compare(Object o1, Object o2)
    {
        @SuppressWarnings("unchecked")
        Comparable<Object> object1 = (Comparable<Object>) provider.apply(o1);
        @SuppressWarnings("unchecked")
        Comparable<Object> object2 = (Comparable<Object>) provider.apply(o2);

        if (object1 == null && object2 == null)
            return 0;
        else if (object1 == null)
            return -1;
        else if (object2 == null)
            return 1;

        return object1.compareTo(object2);
    }
}