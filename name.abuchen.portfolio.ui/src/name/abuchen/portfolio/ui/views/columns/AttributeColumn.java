package name.abuchen.portfolio.ui.views.columns;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Attributes;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.AttributeEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.BooleanAttributeEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;

public class AttributeColumn extends Column
{
    private static final class AttributeComparator implements Comparator<Object>
    {
        private final AttributeType attribute;

        private AttributeComparator(AttributeType attribute)
        {
            this.attribute = attribute;
        }

        @Override
        public int compare(Object o1, Object o2)
        {
            Attributable a1 = Adaptor.adapt(Attributable.class, o1);
            Attributable a2 = Adaptor.adapt(Attributable.class, o2);

            if (a1 == null && a2 == null)
                return 0;
            else if (a1 == null)
                return -1;
            else if (a2 == null)
                return 1;

            Object v1 = a1.getAttributes().get(attribute);
            Object v2 = a2.getAttributes().get(attribute);

            return attribute.getComparator().compare(v1, v2);
        }
    }

    private static final class AttributeLabelProvider extends ColumnLabelProvider
    {
        private final AttributeType attribute;

        private AttributeLabelProvider(AttributeType attribute)
        {
            this.attribute = attribute;
        }

        @Override
        public String getText(Object element)
        {
            Attributable attributable = Adaptor.adapt(Attributable.class, element);
            if (attributable == null)
                return null;

            Attributes attributes = attributable.getAttributes();

            Object value = attributes.get(attribute);
            return attribute.getConverter().toString(value);
        }
    }

    private static final class BooleanLabelProvider extends ColumnLabelProvider
    {
        private final AttributeType attribute;

        private BooleanLabelProvider(AttributeType attribute)
        {
            this.attribute = attribute;
        }

        @Override
        public String getText(Object element)
        {
            return getValue(element).map(b -> Boolean.TRUE.equals(b) ? Messages.LabelYes : Messages.LabelNo).orElse(""); //$NON-NLS-1$
        }

        @Override
        public Image getImage(Object element)
        {
            return getValue(element).map(b -> Boolean.TRUE.equals(b) ? Images.CHECK.image() : Images.XMARK.image())
                            .orElse(null);
        }

        private Optional<Boolean> getValue(Object element)
        {
            Attributable attributable = Adaptor.adapt(Attributable.class, element);
            if (attributable == null)
                return Optional.empty();

            Attributes attributes = attributable.getAttributes();
            return Optional.ofNullable((Boolean) attributes.get(attribute));
        }
    }

    private static final String ID = "attribute$"; //$NON-NLS-1$

    private AttributeColumn(final AttributeType attribute)
    {
        super(ID + attribute.getId(), attribute.getColumnLabel(), // $NON-NLS-1$
                        attribute.isNumber() ? SWT.RIGHT : SWT.LEFT, 80);

        setMenuLabel(attribute.getName());
        setGroupLabel(Messages.GroupLabelAttributes);
        setSorter(ColumnViewerSorter.create(new AttributeComparator(attribute)));

        if (attribute.getType() == Boolean.class)
        {
            setLabelProvider(new BooleanLabelProvider(attribute));
            new BooleanAttributeEditingSupport(attribute).attachTo(this);
        }
        else
        {
            setLabelProvider(new AttributeLabelProvider(attribute));
            new AttributeEditingSupport(attribute).attachTo(this);
        }

    }

    public static Stream<Column> createFor(Client client, Class<? extends Attributable> target)
    {
        return client.getSettings() //
                        .getAttributeTypes() //
                        .filter(a -> a.supports(target)) //
                        .flatMap(attribute -> {

                            List<Column> columns = new ArrayList<>();

                            // primary column
                            Column column = new AttributeColumn(attribute);
                            column.setVisible(false);
                            columns.add(column);

                            // secondary column - if applicable
                            if (attribute.getType() == LocalDate.class)
                            {
                                column = createDaysLeftColumn(attribute);
                                columns.add(column);
                            }

                            return columns.stream();
                        });
    }

    private static Column createDaysLeftColumn(AttributeType attribute)
    {
        Column column;
        column = new Column(ID + attribute.getId() + "-daysbetween", //$NON-NLS-1$
                        attribute.getColumnLabel() + " - " + Messages.ColumnDaysBetweenPostfix, SWT.RIGHT, 80); //$NON-NLS-1$
        column.setMenuLabel(attribute.getName() + " - " + Messages.ColumnDaysBetweenPostfix); //$NON-NLS-1$
        column.setGroupLabel(Messages.GroupLabelAttributes);
        column.setSorter(ColumnViewerSorter.create(new AttributeComparator(attribute)));
        new AttributeEditingSupport(attribute).attachTo(column);

        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Attributable attributable = Adaptor.adapt(Attributable.class, element);
                if (attributable == null)
                    return null;

                LocalDate value = (LocalDate) attributable.getAttributes().get(attribute);
                return value == null ? null : String.valueOf(ChronoUnit.DAYS.between(value, LocalDate.now()));
            }
        });

        column.setVisible(false);
        return column;
    }
}
