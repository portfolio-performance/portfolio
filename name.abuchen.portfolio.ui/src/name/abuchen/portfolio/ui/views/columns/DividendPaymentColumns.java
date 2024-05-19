package name.abuchen.portfolio.ui.views.columns;

import java.time.LocalDate;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.DateLabelProvider;

public class DividendPaymentColumns
{
    private DividendPaymentColumns()
    {
    }

    static Column createNextDividendExDateColumn()
    {
        Column column = new Column("nextdivexdate", Messages.ColumnDividendsNextExDate, SWT.LEFT, 80); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnDividendsNextExDate_MenuLabel);
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setVisible(false);

        LocalDate now = LocalDate.now();

        Function<Object, LocalDate> dataProvider = element -> {
            Security security = Adaptor.adapt(Security.class, element);
            if (security == null)
                return null;

            return security.getEvents().stream() //
                            .filter(DividendEvent.class::isInstance) //
                            .map(e -> ((DividendEvent) e).getDate()) //
                            .sorted(Comparable::compareTo) //
                            .filter(d -> !now.isAfter(d)) //
                            .findFirst().orElse(null);
        };

        column.setLabelProvider(new DateLabelProvider(dataProvider)
        {
            @Override
            public Color getBackground(Object element)
            {
                // if ex-date is today, then use the warning background
                LocalDate date = dataProvider.apply(element);
                return date == null || !date.equals(LocalDate.now()) ? null : Colors.theme().warningBackground();
            }
        });
        column.setSorter(ColumnViewerSorter.create(dataProvider::apply));
        return column;
    }

    static Column createNextDividendPaymentDateColumn()
    {
        Column column = new Column("nextdivpaydate", Messages.ColumnDividendsNextPaymentDate, SWT.LEFT, 80); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnDividendsNextPaymentDate_MenuLabel);
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setVisible(false);

        LocalDate now = LocalDate.now();

        Function<Object, LocalDate> dataProvider = element -> {
            Security security = Adaptor.adapt(Security.class, element);
            if (security == null)
                return null;

            return security.getEvents().stream() //
                            .filter(DividendEvent.class::isInstance) //
                            .map(e -> ((DividendEvent) e).getPaymentDate()) //
                            .sorted(Comparable::compareTo) //
                            .filter(d -> !now.isAfter(d)) //
                            .findFirst().orElse(null);
        };

        column.setLabelProvider(new DateLabelProvider(dataProvider)
        {
            @Override
            public Color getBackground(Object element)
            {
                // if the payment date is today, then use the warning background
                LocalDate date = dataProvider.apply(element);
                return date == null || !date.equals(LocalDate.now()) ? null : Colors.theme().warningBackground();
            }
        });
        column.setSorter(ColumnViewerSorter.create(dataProvider::apply));
        return column;
    }

    static Column createNextDividendPaymentAmount(Client client)
    {
        Column column = new Column("nextdivpayment", Messages.ColumnDividendsNextPaymentAmount, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnDividendsNextPaymentAmount_MenuLabel);
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setVisible(false);

        LocalDate now = LocalDate.now();

        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Security security = Adaptor.adapt(Security.class, element);
                if (security == null)
                    return null;

                Money amount = getPaymentAmount(now, security);
                if (amount == null)
                    return null;

                return Values.Money.formatAlwaysVisible(amount, client.getBaseCurrency());
            }
        });
        column.setSorter(ColumnViewerSorter.create(element -> {
            Security security = Adaptor.adapt(Security.class, element);
            return security != null ? getPaymentAmount(now, security) : null;
        }));
        return column;
    }

    private static Money getPaymentAmount(LocalDate now, Security security)
    {
        return security.getEvents().stream() //
                        .filter(DividendEvent.class::isInstance) //
                        .map(e -> (DividendEvent) e) //
                        .sorted((e1, e2) -> e1.getPaymentDate().compareTo(e2.getPaymentDate())) //
                        .filter(d -> !now.isAfter(d.getPaymentDate())) //
                        .map(DividendEvent::getAmount).findFirst().orElse(null);
    }

    public static Stream<Column> createFor(Client client)
    {
        return Stream.of(createNextDividendExDateColumn(), //
                        createNextDividendPaymentDateColumn(), //
                        createNextDividendPaymentAmount(client));
    }

}
