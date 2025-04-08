package name.abuchen.portfolio.util;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class CollectorsUtil
{
    private CollectorsUtil()
    {
    }

    /**
     * This method returns a collector that gathers the elements into a mutable
     * ArrayList. It's useful in cases where we need to modify the resulting
     * list, which is not possible with the immutable list returned by the
     * Stream#toList method. We use this method to differentiate between cases
     * where a mutable list is necessary and to prevent the IDE from suggesting
     * the use of Stream#toList.
     */
    public static <T> Collector<T, ?, List<T>> toMutableList()
    {
        return Collectors.toList();
    }
}
