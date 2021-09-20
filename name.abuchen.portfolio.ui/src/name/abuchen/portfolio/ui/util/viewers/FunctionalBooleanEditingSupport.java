package name.abuchen.portfolio.ui.util.viewers;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * Similar to {@link BooleanEditingSupport}, but not linked directly to a
 * property (and thus, unlike {@link BooleanEditingSupport}, not an instance of
 * {@link PropertyEditingSupport}).
 */
public class FunctionalBooleanEditingSupport extends ColumnEditingSupport
{
    private Function<Object, Boolean> readValueFunction;
    private BiConsumer<Object, Boolean> writeValueFunction;

    /**
     * @param readValueFunction
     *            A function that takes an object representing the column and
     *            returns the value to display for this column.
     * @param writeValueFunction
     *            A function that is called when the value of the cell was
     *            changed. The first argument represents the column, the second
     *            argument is the new value. A call to this function should change the behavior of {@code readValueFunction}
     */
    public FunctionalBooleanEditingSupport(Function<Object, Boolean> readValueFunction,
                    BiConsumer<Object, Boolean> writeValueFunction)
    {
        this.readValueFunction = readValueFunction;
        this.writeValueFunction = writeValueFunction;
    }

    @Override
    public CellEditor createEditor(Composite composite)
    {
        return new CheckboxCellEditor(composite);
    }

    @Override
    public Object getValue(Object element) throws Exception
    {
        Boolean v = readValueFunction.apply(element);
        return v != null ? v : ""; //$NON-NLS-1$
    }

    @Override
    public void setValue(Object element, Object value) throws Exception
    {
        Boolean newValue = (Boolean) value;
        Boolean oldValue = (Boolean) getValue(element);

        if (!value.equals(oldValue))
        {
            writeValueFunction.accept(element, newValue);
            notify(element, newValue, oldValue);
        }
    }
}
