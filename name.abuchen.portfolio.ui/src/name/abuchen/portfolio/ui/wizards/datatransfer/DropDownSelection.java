package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Preselection of the dropdowns of the import wizard: the element of the
 * previous import if it is still available, otherwise the first element which
 * differs from a given element. For example, the offset portfolio of a
 * transfer should differ from the portfolio, otherwise the transfer cannot be
 * imported. The class has no user interface.
 */
final class DropDownSelection
{
    private DropDownSelection()
    {
    }

    /**
     * Returns the index of the element to preselect.
     *
     * @param elements
     *            the elements of the dropdown
     * @param isPrevious
     *            matches the element selected in the previous import
     * @param differentFrom
     *            the element the preselection should differ from, or null
     * @return the index of the previous element if it is available and differs
     *         from {@code differentFrom}; otherwise the index of the first
     *         element which differs from {@code differentFrom}; otherwise 0;
     *         -1 if there are no elements
     */
    static int indexOf(List<?> elements, Predicate<Object> isPrevious, Object differentFrom)
    {
        if (elements.isEmpty())
            return -1;

        for (var index = 0; index < elements.size(); index++)
        {
            var element = elements.get(index);
            if (isPrevious.test(element) && !Objects.equals(element, differentFrom))
                return index;
        }

        if (differentFrom != null)
        {
            for (var index = 0; index < elements.size(); index++)
            {
                if (!Objects.equals(elements.get(index), differentFrom))
                    return index;
            }
        }

        return 0;
    }
}
