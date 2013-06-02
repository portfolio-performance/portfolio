package name.abuchen.portfolio.ui.util;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public final class CellEditorFactory
{
    public interface ModificationListener
    {
        void onModified(Object element, String property);
    }

    private final Class<?> subjectType;
    private final ColumnViewer viewer;

    private ModificationListener listener = new ModificationListener()
    {
        public void onModified(Object element, String property)
        {}
    };

    private final List<String> properties = new ArrayList<String>();
    private final List<CellEditor> cellEditors = new ArrayList<CellEditor>();
    private final List<Modifier> modifiers = new ArrayList<Modifier>();

    public CellEditorFactory(ColumnViewer viewer, Class<?> subject)
    {
        this.subjectType = subject;
        this.viewer = viewer;

        ColumnViewerEditorActivationStrategy support = new ColumnViewerEditorActivationStrategy(viewer)
        {
            protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event)
            {
                return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
                                || event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
                                || (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
                                || event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
            }
        };

        int feature = ColumnViewerEditor.TABBING_HORIZONTAL | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
                        | ColumnViewerEditor.TABBING_VERTICAL | ColumnViewerEditor.KEYBOARD_ACTIVATION;
        if (viewer instanceof TableViewer)
            TableViewerEditor.create((TableViewer) viewer, null, support, feature);
        else if (viewer instanceof TreeViewer)
            TreeViewerEditor.create((TreeViewer) viewer, support, feature);
    }

    public CellEditorFactory notify(ModificationListener listener)
    {
        this.listener = listener;
        return this;
    }

    public CellEditorFactory editable(String name)
    {
        PropertyDescriptor descriptor = descriptorFor(name);

        properties.add(name);
        cellEditors.add(cellEditorFor(descriptor));
        modifiers.add(modifierFor(descriptor));

        return this;
    }

    public CellEditorFactory month(String name)
    {
        final PropertyDescriptor descriptor = descriptorFor(name);
        if (descriptor.getPropertyType() != int.class)
            throw new UnsupportedOperationException(String.format(
                            "Property %s needs to be of type int to serve as month", name)); //$NON-NLS-1$

        // determine number of months (some calendars have 13)
        int numMonths = Calendar.getInstance().getActualMaximum(Calendar.MONTH) + 1;
        final String[] months = new String[numMonths];
        System.arraycopy(new DateFormatSymbols().getMonths(), 0, months, 0, numMonths);

        properties.add(name);
        cellEditors.add(new ComboBoxCellEditor(((Composite) viewer.getControl()), months, SWT.READ_ONLY));
        modifiers.add(new Modifier()
        {
            public Object getValue(Object element) throws Exception
            {
                return descriptor.getReadMethod().invoke(element);
            }

            public boolean modify(Object element, Object value) throws Exception
            {
                Integer newValue = (Integer) value;
                Integer oldValue = (Integer) descriptor.getReadMethod().invoke(element);

                if (!newValue.equals(oldValue))
                {
                    descriptor.getWriteMethod().invoke(element, newValue);
                    return true;
                }
                else
                {
                    return false;
                }
            }
        });

        return this;
    }

    public CellEditorFactory shares(String name)
    {
        return decimal(name, Values.Share);
    }

    public CellEditorFactory amount(String name)
    {
        return decimal(name, Values.Amount);
    }

    public CellEditorFactory index(String name)
    {
        return decimal(name, Values.Index);
    }

    public CellEditorFactory decimal(String name, Values<?> type)
    {
        final PropertyDescriptor descriptor = descriptorFor(name);
        if (Long.class.isAssignableFrom(descriptor.getPropertyType()))
            throw new UnsupportedOperationException(String.format(
                            "Property %s needs to be of type long to serve as decimal", name)); //$NON-NLS-1$

        properties.add(name);

        TextCellEditor textEditor = new TextCellEditor((Composite) viewer.getControl());
        ((Text) textEditor.getControl()).setTextLimit(20);
        ((Text) textEditor.getControl()).addVerifyListener(new NumberVerifyListener());
        cellEditors.add(textEditor);

        modifiers.add(new LongValueModifier(descriptor, type));

        return this;
    }

    private static final class LongValueModifier implements Modifier
    {
        private final StringToCurrencyConverter toCurrency;
        private final CurrencyToStringConverter fromCurrency;

        private final PropertyDescriptor descriptor;

        private LongValueModifier(PropertyDescriptor descriptor, Values<?> type)
        {
            this.descriptor = descriptor;
            this.toCurrency = new StringToCurrencyConverter(type);
            this.fromCurrency = new CurrencyToStringConverter(type);
        }

        public Object getValue(Object element) throws Exception
        {
            return fromCurrency.convert(descriptor.getReadMethod().invoke(element));
        }

        public boolean modify(Object element, Object value) throws Exception
        {
            Number newValue = (Number) toCurrency.convert(String.valueOf(value));

            if (int.class.isAssignableFrom(descriptor.getPropertyType())
                            || Integer.class.isAssignableFrom(descriptor.getPropertyType()))
                newValue = Integer.valueOf(newValue.intValue());

            Number oldValue = (Number) descriptor.getReadMethod().invoke(element);

            if (!newValue.equals(oldValue))
            {
                descriptor.getWriteMethod().invoke(element, newValue);
                return true;
            }
            else
            {
                return false;
            }
        }
    }

    private static final class NumberVerifyListener implements VerifyListener
    {
        private String allowedChars = ",.0123456789"; //$NON-NLS-1$

        public void verifyText(VerifyEvent e)
        {
            for (int ii = 0; e.doit && ii < e.text.length(); ii++)
                e.doit = allowedChars.indexOf(e.text.charAt(0)) >= 0;
        }
    }

    public CellEditorFactory combobox(String name, final List<?> items)
    {
        return combobox(name, items, false);
    }

    public CellEditorFactory combobox(String name, List<?> items, boolean includeEmpty)
    {
        final PropertyDescriptor descriptor = descriptorFor(name);

        properties.add(name);

        final List<Object> comboBoxItems = new ArrayList<Object>();
        if (includeEmpty)
            comboBoxItems.add(null);
        comboBoxItems.addAll(items);

        // cell editor
        String[] names = new String[comboBoxItems.size()];
        int index = 0;
        for (Object item : comboBoxItems)
            names[index++] = item == null ? "" : item.toString(); //$NON-NLS-1$

        cellEditors.add(new ComboBoxCellEditor((Composite) viewer.getControl(), names, SWT.READ_ONLY));

        modifiers.add(new Modifier()
        {
            public Object getValue(Object element) throws Exception
            {
                Object property = descriptor.getReadMethod().invoke(element);

                for (int ii = 0; ii < comboBoxItems.size(); ii++)
                {
                    Object item = comboBoxItems.get(ii);
                    if (item == property)
                        return ii;
                }

                return 0;
            }

            public boolean modify(Object element, Object value) throws Exception
            {
                Object newValue = comboBoxItems.get((Integer) value);
                Object oldValue = descriptor.getReadMethod().invoke(element);

                if (newValue != oldValue)
                {
                    descriptor.getWriteMethod().invoke(element, newValue);
                    return true;
                }
                else
                {
                    return false;
                }
            }

        });

        return this;
    }

    public CellEditorFactory readonly(String name)
    {
        properties.add(name);
        cellEditors.add(null);
        modifiers.add(null);

        return this;
    }

    public void apply()
    {
        viewer.setColumnProperties(properties.toArray(new String[0]));
        viewer.setCellEditors(cellEditors.toArray(new CellEditor[0]));
        viewer.setCellModifier(new CellModifiersImpl(subjectType, properties, modifiers, listener));
    }

    // //////////////////////////////////////////////////////////////
    // internal helpers
    // //////////////////////////////////////////////////////////////

    private interface Modifier
    {
        Object getValue(Object element) throws Exception;

        boolean modify(Object element, Object value) throws Exception;
    }

    private static class CellModifiersImpl implements ICellModifier
    {
        private Class<?> subjectType;
        private String[] names;
        private Modifier[] modifier;
        private ModificationListener listener;

        private CellModifiersImpl(Class<?> subjectType, List<String> names, List<Modifier> modifier,
                        ModificationListener listener)
        {
            this.subjectType = subjectType;
            this.names = names.toArray(new String[0]);
            this.modifier = modifier.toArray(new Modifier[0]);
            this.listener = listener;
        }

        public boolean canModify(Object element, String property)
        {
            return subjectType.isInstance(element) && modifier[indexOf(property)] != null;
        }

        public Object getValue(Object element, String property)
        {
            try
            {
                return modifier[indexOf(property)].getValue(element);
            }
            catch (Exception e)
            {
                ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                                Messages.CellEditor_Error, e.getMessage(), new Status(Status.ERROR,
                                                PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
                return null;
            }
        }

        public void modify(Object element, String property, Object value)
        {
            try
            {
                Object elem = ((Item) element).getData();
                if (modifier[indexOf(property)].modify(elem, value))
                    listener.onModified(elem, property);
            }
            catch (Exception e)
            {
                ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                                Messages.CellEditor_Error, e.getMessage(), new Status(Status.ERROR,
                                                PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
            }
        }

        private int indexOf(String property)
        {
            for (int ii = 0; ii < names.length; ii++)
            {
                if (names[ii].equals(property))
                    return ii;
            }
            throw new RuntimeException(String.format("Unknown property %s", property)); //$NON-NLS-1$
        }

    }

    private Modifier modifierFor(final PropertyDescriptor descriptor)
    {
        final Class<?> type = descriptor.getPropertyType();

        if (type == int.class)
        {
            return new Modifier()
            {
                public Object getValue(Object element) throws Exception
                {
                    Integer v = (Integer) descriptor.getReadMethod().invoke(element);
                    return String.format("%d", v.intValue()); //$NON-NLS-1$
                }

                public boolean modify(Object element, Object value) throws Exception
                {
                    Number n = new DecimalFormat("#").parse(String.valueOf(value)); //$NON-NLS-1$
                    Integer newValue = n.intValue();
                    Integer oldValue = (Integer) descriptor.getReadMethod().invoke(element);

                    if (!newValue.equals(oldValue))
                    {
                        descriptor.getWriteMethod().invoke(element, newValue);
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }
            };
        }
        else if (type == long.class)
        {
            return new Modifier()
            {
                public Object getValue(Object element) throws Exception
                {
                    Long v = (Long) descriptor.getReadMethod().invoke(element);
                    return String.format("%d", v.longValue()); //$NON-NLS-1$
                }

                public boolean modify(Object element, Object value) throws Exception
                {
                    Number n = new DecimalFormat("#").parse(String.valueOf(value)); //$NON-NLS-1$
                    Long newValue = n.longValue();
                    Long oldValue = (Long) descriptor.getReadMethod().invoke(element);

                    if (!newValue.equals(oldValue))
                    {
                        descriptor.getWriteMethod().invoke(element, newValue);
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }
            };
        }
        else if (type == Date.class)
        {
            return new Modifier()
            {
                public Object getValue(Object element) throws Exception
                {
                    Date v = (Date) descriptor.getReadMethod().invoke(element);
                    return String.format("%tF", v); //$NON-NLS-1$
                }

                public boolean modify(Object element, Object value) throws Exception
                {
                    Date newValue = new SimpleDateFormat("yyyy-MM-dd").parse(String.valueOf(value)); //$NON-NLS-1$
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(newValue);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    newValue = cal.getTime();

                    Date oldValue = (Date) descriptor.getReadMethod().invoke(element);

                    if (!newValue.equals(oldValue))
                    {
                        descriptor.getWriteMethod().invoke(element, newValue);
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }
            };
        }
        else if (type.isEnum()) //
        { //
            return new Modifier()
            {
                public Object getValue(Object element) throws Exception
                {
                    Enum<?> v = (Enum<?>) descriptor.getReadMethod().invoke(element);
                    return v.ordinal();
                }

                public boolean modify(Object element, Object value) throws Exception
                {
                    Enum<?> newValue = (Enum<?>) type.getEnumConstants()[(Integer) value];
                    Enum<?> oldValue = (Enum<?>) descriptor.getReadMethod().invoke(element);

                    if (!newValue.equals(oldValue))
                    {
                        descriptor.getWriteMethod().invoke(element, newValue);
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }
            };
        }
        else if (type == String.class) { return new Modifier()
        {
            public Object getValue(Object element) throws Exception
            {
                String v = (String) descriptor.getReadMethod().invoke(element);
                return v != null ? v : ""; //$NON-NLS-1$
            }

            public boolean modify(Object element, Object value) throws Exception
            {
                String oldValue = (String) descriptor.getReadMethod().invoke(element);

                if (!value.equals(oldValue))
                {
                    descriptor.getWriteMethod().invoke(element, value);
                    return true;
                }
                else
                {
                    return false;
                }
            }
        }; }

        throw new RuntimeException(String.format(
                        "No formatter available for property %s of type %s", descriptor.getName(), type //$NON-NLS-1$
                                        .getName()));
    }

    private CellEditor cellEditorFor(PropertyDescriptor descriptor)
    {
        Class<?> type = descriptor.getPropertyType();

        if (type == int.class || type == long.class)
        {
            TextCellEditor textEditor = new TextCellEditor((Composite) viewer.getControl());
            ((Text) textEditor.getControl()).setTextLimit(20);
            ((Text) textEditor.getControl()).addVerifyListener(new VerifyListener()
            {
                public void verifyText(VerifyEvent e)
                {
                    for (int ii = 0; e.doit && ii < e.text.length(); ii++)
                        e.doit = ",.0123456789".indexOf(e.text.charAt(0)) >= 0; //$NON-NLS-1$
                }
            });
            return textEditor;
        }
        else if (type == Date.class)
        {
            TextCellEditor textEditor = new TextCellEditor((Composite) viewer.getControl());
            ((Text) textEditor.getControl()).setTextLimit(10);
            ((Text) textEditor.getControl()).addVerifyListener(new VerifyListener()
            {
                public void verifyText(VerifyEvent e)
                {
                    for (int ii = 0; e.doit && ii < e.text.length(); ii++)
                        e.doit = "-0123456789".indexOf(e.text.charAt(0)) >= 0; //$NON-NLS-1$
                }
            });
            return textEditor;
        }
        else if (type.isEnum())
        {
            Object[] enumConstants = type.getEnumConstants();

            String[] names = new String[enumConstants.length];
            int index = 0;
            for (Object c : enumConstants)
                names[index++] = c.toString();

            ComboBoxCellEditor cellEditor = new ComboBoxCellEditor((Composite) viewer.getControl(), names,
                            SWT.READ_ONLY);
            cellEditor.getControl().setData(enumConstants);
            return cellEditor;
        }
        else if (type == String.class)
        {
            TextCellEditor textEditor = new TextCellEditor((Composite) viewer.getControl());
            ((Text) textEditor.getControl()).setTextLimit(60);
            return textEditor;
        }

        throw new RuntimeException(String.format(
                        "No cell editor available for property %s of type %s", descriptor.getName(), type //$NON-NLS-1$
                                        .getName()));
    }

    private PropertyDescriptor descriptorFor(String name)
    {
        try
        {
            PropertyDescriptor[] properties = Introspector.getBeanInfo(subjectType).getPropertyDescriptors();
            for (PropertyDescriptor p : properties)
                if (name.equals(p.getName()))
                    return p;
            throw new RuntimeException(String.format("%s has no property named %s", subjectType //$NON-NLS-1$
                            .getName(), name));
        }
        catch (IntrospectionException e)
        {
            throw new RuntimeException(e);
        }
    }
}
