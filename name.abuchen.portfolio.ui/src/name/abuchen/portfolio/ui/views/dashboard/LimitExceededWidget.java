package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.LimitPrice;
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
import name.abuchen.portfolio.ui.views.settings.AttributeFieldType;
import name.abuchen.portfolio.ui.views.settings.SettingsView;

public class LimitExceededWidget extends AbstractSecurityListWidget<LimitExceededWidget.LimitItem>
{
    public static class LimitItem extends AbstractSecurityListWidget.Item
    {
        private LimitPrice limit;
        private SecurityPrice price;
        private AttributeType attributeType;

        public LimitItem(Security security, LimitPrice limit, SecurityPrice price, AttributeType type)
        {
            super(security);
            this.limit = limit;
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

            List<AttributeType> types = get(AttributesConfig.class).getTypes();

            List<LimitItem> items = new ArrayList<>();

            for (Security security : getClient().getSecurities())
            {
                for (AttributeType t : types)
                {
                    Object attribute = security.getAttributes().get(t);
                    if (!(attribute instanceof LimitPrice))
                        continue;

                    LimitPrice limit = (LimitPrice) attribute;

                    SecurityPrice latest = security.getSecurityPrice(LocalDate.now());
                    if (latest != null && limit.isExceeded(latest))
                    {
                        items.add(new LimitItem(security, limit, latest, t));
                    }
                }
            }

            Collections.sort(items, (r, l) -> Double.compare(AttributeColumn.LimitPriceComparator.calculateNormalizedDistance(l.limit, l.price), AttributeColumn.LimitPriceComparator.calculateNormalizedDistance(r.limit, r.price)));

            return items;
        };
    }

    @Override
    protected Composite createItemControl(Composite parent, LimitItem item)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FormLayout());

        Label logo = new Label(composite, SWT.NONE);
        logo.setImage(LogoManager.instance().getDefaultColumnImage(item.getSecurity(), getClient().getSettings()));

        Label name = new Label(composite, SWT.NONE);
        name.setText(item.getSecurity().getName());

        ColoredLabel price = new ColoredLabel(composite, SWT.RIGHT);

        // determine colors
        LimitPriceSettings settings = new LimitPriceSettings(item.attributeType.getProperties());
        price.setBackdropColor(item.limit.getRelationalOperator().isGreater()
                        ? settings.getLimitExceededPositivelyColor(Colors.theme().greenBackground())
                        : settings.getLimitExceededNegativelyColor(Colors.theme().redBackground()));

        price.setText(Values.Quote.format(item.getSecurity().getCurrencyCode(), item.price.getValue()));

        Label limit = new Label(composite, SWT.NONE);
        limit.setText(settings.getFullLabel(item.limit, item.price));

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

        title = new StyledLabel(parent, SWT.WRAP);
        title.setText(MessageFormat.format(Messages.MsgHintNoAttributesConfigured,
                        AttributeFieldType.LIMIT_PRICE.toString()));
        title.setOpenLinkHandler(d -> view.getPart().activateView(SettingsView.class, 1));
    }
}
