package name.abuchen.portfolio.ui.views.dashboard.lists;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.AttributeFieldType;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.LimitPrice;
import name.abuchen.portfolio.model.LimitPriceResolver;
import name.abuchen.portfolio.model.LimitPriceSettings;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.ui.util.swt.StyledLabel;
import name.abuchen.portfolio.ui.views.columns.AttributeColumn;
import name.abuchen.portfolio.ui.views.dashboard.AttributesConfig;
import name.abuchen.portfolio.ui.views.dashboard.ChartHeightConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.settings.AttributeFieldTypeLabels;
import name.abuchen.portfolio.ui.views.settings.SettingsView;

public class LimitExceededWidget extends AbstractSecurityListWidget<LimitExceededWidget.LimitItem>
{
    public static class LimitItem extends AbstractSecurityListWidget.Item
    {
        private LimitPrice limit;
        private OptionalLong resolvedQuote;
        private SecurityPrice price;
        private AttributeType attributeType;

        public LimitItem(Security security, LimitPrice limit, SecurityPrice price, AttributeType type)
        {
            this(security, limit, limit.isStatic() ? OptionalLong.of(limit.getValue()) : OptionalLong.empty(), price,
                            type);
        }

        public LimitItem(Security security, LimitPrice limit, OptionalLong resolvedQuote, SecurityPrice price,
                        AttributeType type)
        {
            super(security);
            this.limit = limit;
            this.resolvedQuote = resolvedQuote;
            this.price = price;
            this.attributeType = type;
        }

    }

    public LimitExceededWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new AttributesConfig(this, t -> t.getTarget() == Security.class && t.getType() == LimitPrice.class));
        addConfig(new ChartHeightConfig(this));
    }

    @Override
    public Supplier<List<LimitItem>> getUpdateTask()
    {
        return () -> {

            var types = get(AttributesConfig.class).getTypes();
            var resolver = new LimitPriceResolver(getClient());

            var items = new ArrayList<LimitItem>();

            for (Security security : getClient().getSecurities())
            {
                for (AttributeType t : types)
                {
                    Object attribute = security.getAttributes().get(t);
                    if (!(attribute instanceof LimitPrice limit))
                        continue;

                    var latest = security.getSecurityPrice(LocalDate.now());
                    if (latest == null)
                        continue;

                    var resolved = limit.resolveValue(resolver, security);
                    if (resolved.isPresent()
                                    && LimitPrice.isExceeded(limit.getRelationalOperator(), resolved.getAsLong(),
                                                    latest))
                    {
                        items.add(new LimitItem(security, limit, resolved, latest, t));
                    }
                }
            }

            Collections.sort(items, (r, l) -> {
                double distL = Objects.requireNonNullElse(AttributeColumn.LimitPriceComparator
                                .calculateNormalizedDistance(l.limit, l.resolvedQuote, l.price),
                                Double.NEGATIVE_INFINITY);
                double distR = Objects.requireNonNullElse(AttributeColumn.LimitPriceComparator
                                .calculateNormalizedDistance(r.limit, r.resolvedQuote, r.price),
                                Double.NEGATIVE_INFINITY);
                return Double.compare(distL, distR);
            });

            return items;
        };
    }

    @Override
    protected Composite createItemControl(Composite parent, LimitItem item, LimitItem previous)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FormLayout());

        Label logo = createLabel(composite,
                        LogoManager.instance().getDefaultColumnImage(item.getSecurity(), getClient().getSettings()));

        Label name = createLabel(composite, item.getSecurity().getName(getClient().getSecurityNameConfig()));

        ColoredLabel price = new ColoredLabel(composite, SWT.RIGHT);

        // determine colors
        LimitPriceSettings settings = new LimitPriceSettings(item.attributeType.getProperties());
        Color bgColor = item.limit.getRelationalOperator().isGreater()
                        ? settings.getLimitExceededPositivelyColor(Colors.theme().greenBackground())
                        : settings.getLimitExceededNegativelyColor(Colors.theme().redBackground());
        price.setBackdropColor(bgColor);
        price.setForeground(Colors.getTextColor(bgColor));
        price.setText(Values.Quote.format(item.getSecurity().getCurrencyCode(), item.price.getValue()));

        Label limit = createLabel(composite, settings.getFullLabel(item.limit, item.resolvedQuote, item.price));

        composite.addMouseListener(mouseUpAdapter);
        name.addMouseListener(mouseUpAdapter);
        limit.addMouseListener(mouseUpAdapter);
        price.addMouseListener(mouseUpAdapter);

        FormDataFactory.startingWith(logo).thenRight(name).right(new FormAttachment(100)).thenBelow(price)
                        .thenRight(limit);

        return composite;
    }

    @Override
    protected void createEmptyControl(Composite parent)
    {
        if (get(AttributesConfig.class).hasTypes())
            return;

        var label = new StyledLabel(parent, SWT.WRAP);
        label.setText(MessageFormat.format(Messages.MsgHintNoAttributesConfigured,
                        AttributeFieldTypeLabels.label(AttributeFieldType.LIMIT_PRICE)));
        label.setOpenLinkHandler(d -> view.getPart().activateView(SettingsView.class, 1));
    }
}
