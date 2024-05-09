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

public class DividendPaymentColumn
{

    static Column createNextDividendExDateColumn(Client client)
    {
        Column column = new Column("nextdivexdate", Messages.ColumnDividendsNextExDate, SWT.LEFT, 80); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnDividendsNextExDate_MenuLabel);
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setVisible(false);

        LocalDate now = LocalDate.now();

        Function<Object, LocalDate> dataProvider = element -> {
            Security sec = Adaptor.adapt(Security.class, element);
            if (sec == null)
            {
                return null; //
            }
            return sec.getEvents().stream() //
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
                LocalDate date = dataProvider.apply(element);
                if (date == null || !date.equals(LocalDate.now()))
                {
                    return null; //
                }

                return Colors.theme().redBackground();
            }
        });
        column.setSorter(ColumnViewerSorter.create(dataProvider::apply));
        return column;
    }

    static Column createNextDividendPaymentDateColumn(Client client)
    {
        Column column = new Column("nextdivpmtdate", Messages.ColumnDividendsNextPaymentDate, SWT.LEFT, 80); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnDividendsNextPaymentDate_MenuLabel);
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setVisible(false);

        LocalDate now = LocalDate.now();

        Function<Object, LocalDate> dataProvider = element -> {
            Security sec = Adaptor.adapt(Security.class, element);
            if (sec == null)
            {
                return null; //
            }
            return sec.getEvents().stream() //
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
                LocalDate date = dataProvider.apply(element);
                if (date == null || !date.equals(LocalDate.now()))
                {
                    return null; //
                }

                return Colors.theme().greenBackground();
            }
        });
        column.setSorter(ColumnViewerSorter.create(dataProvider::apply));
        return column;
    }

    static Column createNextDividendPaymentAmount(Client client)
    {
        Column column = new Column("nextdivpmtamt", Messages.ColumnDividendsNextPaymentAmount, SWT.RIGHT, 60); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnDividendsNextPaymentAmount_MenuLabel);
        column.setGroupLabel(Messages.GroupLabelDividends);
        column.setVisible(false);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Security sec = Adaptor.adapt(Security.class, element);
                if (sec == null)
                {
                    return null; //
                }
                LocalDate now = LocalDate.now();
                Money amount = getAmount(now, sec);

                if (amount == null)
                {
                    return null; //
                }
                return Values.Money.format(amount, client.getBaseCurrency(), false);
            }
        });
        column.setSorter(ColumnViewerSorter.create((o1, o2) -> {
            Money m1 = getAmount(LocalDate.now(), (Security) o1);
            Money m2 = getAmount(LocalDate.now(), (Security) o2);

            if (m1 == null)
                return m2 == null ? 0 : -1;
            if (m2 == null)
                return 1;

            return m1.compareTo(m2);
        }));
        return column;
    }

    static Money getAmount(LocalDate now, Security sec)
    {
        return sec.getEvents().stream() //
                        .filter(e -> e instanceof DividendEvent) //
                        .map(e -> (DividendEvent) e) //
                        .sorted((e1, e2) -> e1.getPaymentDate().compareTo(e2.getPaymentDate())) //
                        .filter(d -> !now.isAfter(d.getPaymentDate())) //
                        .map(DividendEvent::getAmount).findFirst().orElse(null);
    }

    public static Stream<Column> createFor(Client client)
    {
        return Stream.of(createNextDividendExDateColumn(client), //
                        createNextDividendPaymentDateColumn(client), //
                        createNextDividendPaymentAmount(client));
    }

}
