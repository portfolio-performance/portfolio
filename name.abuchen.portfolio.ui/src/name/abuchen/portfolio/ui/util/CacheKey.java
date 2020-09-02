package name.abuchen.portfolio.ui.util;

import java.util.Arrays;
import java.util.Objects;

public class CacheKey
{
    private final Object[] objects;

    public CacheKey(Object... objects)
    {
        for (int ii = 0; ii < objects.length; ii++)
            if (objects[ii] == null)
                throw new NullPointerException();

        this.objects = objects;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(objects);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        CacheKey other = (CacheKey) obj;
        return Arrays.equals(objects, other.objects);
    }
}