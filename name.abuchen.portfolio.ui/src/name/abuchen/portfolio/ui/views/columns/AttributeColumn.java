package name.abuchen.portfolio.ui.views.columns;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.AttributeType.ImageConverter;
import name.abuchen.portfolio.model.Attributes;
import name.abuchen.portfolio.model.Bookmark;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ImageManager;
import name.abuchen.portfolio.model.LimitPrice;
import name.abuchen.portfolio.model.LimitPriceSettings;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.DesktopAPI;
import name.abuchen.portfolio.ui.util.viewers.AttributeEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.BooleanAttributeEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.CellItemImageClickedListener;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.ImageAttributeEditingSupport;
import name.abuchen.portfolio.util.Pair;

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

    private static final class ImageLabelProvider extends ColumnLabelProvider
    {
        private final AttributeType attribute;

        private ImageLabelProvider(AttributeType attribute)
        {
            this.attribute = attribute;
        }

        @Override
        public String getText(Object element)
        {
            return ""; //$NON-NLS-1$
        }

        @Override
        public Image getImage(Object element)
        {
            if (attribute.getConverter() instanceof ImageConverter)
            {
                Attributable attributable = Adaptor.adapt(Attributable.class, element);
                return ImageManager.instance().getImage(attributable, attribute);
            }
            return null;
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

    public static final class LimitPriceComparator implements Comparator<Object>
    {
        private final AttributeType attribute;

        private LimitPriceComparator(AttributeType attribute)
        {
            this.attribute = attribute;
        }

        @Override
        public int compare(Object o1, Object o2)
        {
            Pair<String, Supplier<Double>> pair1 = getCompareValues(o1);
            Pair<String, Supplier<Double>> pair2 = getCompareValues(o2);

            if (pair1 == null && pair2 == null)
                return 0;
            else if (pair1 == null)
                return -1;
            else if (pair2 == null)
                return 1;

            // order by comparator...
            int operatorCompare = pair1.getLeft().compareTo(pair2.getLeft());
            if(operatorCompare != 0)
                return operatorCompare;

            // operators are the same, now calculate the normalized distance
            Double distance1 = pair1.getRight().get();
            Double distance2 = pair2.getRight().get();

            // checks in case one or both distance(s) not available
            if (distance1 == null && distance2 == null)
                return 0;
            else if (distance1 == null)
                return -1;
            else if (distance2 == null)
                return 1;

            // ...then by distance
            return Double.compare(distance1, distance2);
        }

        private Pair<String, Supplier<Double>> getCompareValues(Object o)
        {
            Security s = Adaptor.adapt(Security.class, o);
            if (s == null)
                return null;

            LimitPrice l = Adaptor.adapt(LimitPrice.class, s.getAttributes().get(attribute));
            if (l == null)
                return null;

            SecurityPrice p = s.getSecurityPrice(LocalDate.now());
            if (p == null)
                return new Pair<>(l.getRelationalOperator().getOperatorString(), () -> null); // no price, no distance

            return new Pair<>(l.getRelationalOperator().getOperatorString(), () -> calculateNormalizedDistance(l, p));
        }

        public static Double calculateNormalizedDistance(LimitPrice limit, SecurityPrice latest)
        {
            // "normalized relative distance": relative distance, but if exceeded then as positive value, otherwise as negative value
            double relativeDistanceAbs = Math.abs(limit.calculateRelativeDistance(latest.getValue()));
            return  limit.isExceeded(latest) ? relativeDistanceAbs : -relativeDistanceAbs;
        }
    }

    private static final class LimitPriceLabelProvider extends ColumnLabelProvider
    {
        private final AttributeType attribute;
        private final LimitPriceSettings settings;

        private LimitPriceLabelProvider(AttributeType attribute)
        {
            this.attribute = attribute;
            this.settings = new LimitPriceSettings(attribute.getProperties());
        }

        @Override
        public String getText(Object element)
        {
            Security security = Adaptor.adapt(Security.class, element);
            if (security == null)
                return null;

            Attributes attributes = security.getAttributes();
            if (attributes == null)
                return null;

            LimitPrice limit = (LimitPrice) attributes.get(attribute);
            if (limit == null)
                return null;

            // add relative/absolute difference to latest price if configured
            if (settings.getShowAbsoluteDiff() || settings.getShowRelativeDiff())
            {
                SecurityPrice latestSecurityPrice = security.getSecurityPrice(LocalDate.now());
                return settings.getFullLabel(limit, latestSecurityPrice);
            }
            else
            {
                return limit.toString();
            }
        }

        @Override
        public Color getBackground(Object element)
        {
            Security security = Adaptor.adapt(Security.class, element);
            if (security == null)
                return null;

            Attributes attributes = security.getAttributes();
            LimitPrice limit = (LimitPrice) attributes.get(attribute);
            if (limit == null)
                return null;

            SecurityPrice latestSecurityPrice = security.getSecurityPrice(LocalDate.now());
            if (latestSecurityPrice == null)
                return null;

            return getColor(limit, latestSecurityPrice);
        }

        private Color getColor(LimitPrice limit, SecurityPrice latestSecurityPrice)
        {
            if (!limit.isExceeded(latestSecurityPrice))
                return null;

            switch (limit.getRelationalOperator())
            {
                case GREATER_OR_EQUAL:
                case GREATER:
                    return settings.getLimitExceededPositivelyColor(Colors.theme().greenBackground());
                case SMALLER_OR_EQUAL:
                case SMALLER:
                    return settings.getLimitExceededNegativelyColor(Colors.theme().redBackground());
                default:
                    return null;
            }
        }

        @Override
        public Color getForeground(Object element)
        {
            Color background = getBackground(element);
            if (background == null)
                return null;

            return Colors.getTextColor(background);
        }
    }

    private static final class BookmarkLabelProvider extends StyledCellLabelProvider // NOSONAR
                    implements CellItemImageClickedListener
    {
        private final AttributeType attribute;

        private BookmarkLabelProvider(AttributeType attribute)
        {
            this.attribute = attribute;
        }

        private Optional<Bookmark> getValue(Object element)
        {
            Attributable attributable = Adaptor.adapt(Attributable.class, element);
            if (attributable == null)
                return Optional.empty();

            Attributes attributes = attributable.getAttributes();
            return Optional.ofNullable((Bookmark) attributes.get(attribute));
        }

        @Override
        public void update(ViewerCell cell)
        {
            String label = getValue(cell.getElement()).map(Bookmark::getLabel).orElse(""); //$NON-NLS-1$
            cell.setText(label);
            if (!label.isEmpty())
                cell.setImage(Images.BOOKMARK_OPEN.image());

            StyleRange styleRange = new StyleRange();
            styleRange.underline = true;
            styleRange.underlineStyle = SWT.UNDERLINE_SINGLE;
            styleRange.data = cell.getElement();
            styleRange.start = 0;
            styleRange.length = label.length();

            cell.setStyleRanges(new StyleRange[] { styleRange });
        }

        @Override
        public void onImageClicked(Object element)
        {
            getValue(element).ifPresent(bm -> DesktopAPI.browse(bm.getPattern()));
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
        else if (attribute.getType() == LimitPrice.class)
        {
            setStyle(SWT.RIGHT);
            setLabelProvider(new LimitPriceLabelProvider(attribute));
            setComparator(new LimitPriceComparator(attribute));
            new AttributeEditingSupport(attribute).attachTo(this);
        }
        else if (attribute.getType() == Bookmark.class)
        {
            // the bookmark label provider is a StyledCellLabelProvider that
            // cannot be used for more than one column. Always create a new
            // instance.

            setLabelProvider(() -> new BookmarkLabelProvider(attribute));
            new AttributeEditingSupport(attribute).attachTo(this);
        }
        else if (attribute.getConverter() instanceof ImageConverter)
        {
            setLabelProvider(new ImageLabelProvider(attribute));
            new ImageAttributeEditingSupport(attribute).attachTo(this);
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
