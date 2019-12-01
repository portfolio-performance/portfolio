package name.abuchen.portfolio.util;

import java.util.Objects;

/**
 * An immutable pair consisting of two non-null <code>Object</code> elements.
 */
public class Pair<L, R>
{
    private final L left;
    private final R right;

    public Pair(L left, R right)
    {
        this.left = Objects.requireNonNull(left);
        this.right = Objects.requireNonNull(right);
    }

    public L getLeft()
    {
        return left;
    }

    public R getRight()
    {
        return right;
    }

    @Override
    public int hashCode()
    {
        return left.hashCode() ^ right.hashCode();
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

        Pair<?, ?> other = (Pair<?, ?>) obj;
        return left.equals(other.left) && right.equals(other.right);
    }
}
