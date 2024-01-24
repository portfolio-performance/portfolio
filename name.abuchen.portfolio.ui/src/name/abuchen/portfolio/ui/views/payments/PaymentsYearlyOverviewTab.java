package name.abuchen.portfolio.ui.views.payments;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;

import jakarta.inject.Inject;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.TabularDataSource;
import name.abuchen.portfolio.ui.util.TabularDataSource.Builder;
import name.abuchen.portfolio.ui.util.TabularDataSource.Column;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel.Line;

public class PaymentsYearlyOverviewTab implements PaymentsTab
{
    @Inject
    private AbstractFinanceView view;

    @Inject
    private PaymentsViewModel model;

    private DateTimeFormatter formatterM = DateTimeFormatter.ofPattern("MMMM"); //$NON-NLS-1$
    private DateTimeFormatter formatterMY = DateTimeFormatter.ofPattern("MMMM yyyy"); //$NON-NLS-1$

    private Composite container;
    private ScrolledComposite scrolledComposite;

    @Override
    public String getLabel()
    {
        return Messages.PerformanceTabOverview;
    }

    @Override
    public Control createControl(Composite parent)
    {
        container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());

        createScrolledComposite(container);

        model.addUpdateListener(this::updateTable);

        return container;
    }

    private void updateTable()
    {
        if (scrolledComposite != null && !scrolledComposite.isDisposed())
            scrolledComposite.dispose();

        createScrolledComposite(container);

        container.layout();
    }

    private void createScrolledComposite(Composite parent)
    {
        scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);

        Composite dataComposite = new Composite(scrolledComposite, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(12).equalWidth(true).margins(10, 5).spacing(10, 5)
                        .applyTo(dataComposite);

        LocalDate date = LocalDate.of(LocalDate.now().getYear(), Month.JANUARY, 1);
        int index = (date.getYear() - model.getStartYear()) * 12;
        for (; index >= 0; index -= 12)
        {
            createYear(dataComposite, date, index);
            date = date.minusYears(1);
        }

        scrolledComposite.setContent(dataComposite);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        scrolledComposite.setMinSize(dataComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        final ControlListener listener = ControlListener.controlResizedAdapter(
                        e -> scrolledComposite.setMinSize(dataComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT)));
        parent.addControlListener(listener);
        scrolledComposite.addDisposeListener(e -> parent.removeControlListener(listener));
    }

    private void createYear(Composite composite, LocalDate date, int index)
    {
        ColoredLabel label = new ColoredLabel(composite, SWT.NONE);
        label.setBackdropColor(PaymentsColors.getColor(date.getYear()));
        label.setText(String.valueOf(date.getYear()));
        GridDataFactory.fillDefaults().indent(0, 15).align(SWT.FILL, SWT.END).applyTo(label);

        label = new ColoredLabel(composite, SWT.NONE);
        label.setBackdropColor(PaymentsColors.getColor(date.getYear()));
        label.setText(MessageFormat.format(Messages.LabelTransactionCount, sumTransactions(model.getSum(), index)));
        GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.END).applyTo(label);

        label = new ColoredLabel(composite, SWT.RIGHT);
        label.setBackdropColor(PaymentsColors.getColor(date.getYear()));
        label.setText(Values.Amount.format(sumValue(model.getSum(), index)));
        GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.END).applyTo(label);

        Label empty = new Label(composite, SWT.NONE);
        GridDataFactory.fillDefaults().span(7, 1).applyTo(empty);

        int month = 0;
        for (; month < 12 && index + month < model.getNoOfMonths(); month++)
        {
            Line line = model.getSum();

            LocalDate thisMonth = date.plusMonths(month);

            TabularDataSource source = new TabularDataSource(formatterMY.format(thisMonth),
                            builder -> build(builder, thisMonth));

            final MouseListener listener = MouseListener.mouseUpAdapter(e -> view.setInformationPaneInput(source));

            Composite monthComposite = new Composite(composite, SWT.BORDER);
            monthComposite.addMouseListener(listener);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(monthComposite);
            GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 2).applyTo(monthComposite);

            ColoredLabel l = new ColoredLabel(monthComposite, SWT.CENTER);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(l);
            l.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
            l.setText(formatterM.format(thisMonth));
            l.setBackdropColor(Colors.theme().defaultBackground());
            l.addMouseListener(listener);
            source.attachToolTipTo(l);

            l = new ColoredLabel(monthComposite, SWT.RIGHT);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(l);
            int numTransations = line.getNumTransations(index + month);
            l.setText(numTransations != 0 ? String.valueOf(numTransations) : ""); //$NON-NLS-1$
            l.setBackdropColor(Colors.theme().defaultBackground());
            l.addMouseListener(listener);
            source.attachToolTipTo(l);

            l = new ColoredLabel(monthComposite, SWT.RIGHT);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(l);
            String text = Values.Amount.formatNonZero(line.getValue(index + month));
            l.setText(text != null ? text : ""); //$NON-NLS-1$
            l.setBackdropColor(Colors.theme().defaultBackground());
            l.addMouseListener(listener);
            source.attachToolTipTo(l);
        }

        // make sure missing months are empty to keep the columns of the grid
        // layout filled
        if (month < 12)
            GridDataFactory.fillDefaults().span(12 - month, 1).applyTo(new Label(composite, SWT.NONE));
    }

    private void build(Builder builder, LocalDate month)
    {
        builder.addColumns(new Column(Messages.ColumnDate, SWT.LEFT, 100) //
                        .withFormatter(o -> Values.DateTime
                                        .format(((TransactionPair<?>) o).getTransaction().getDateTime())), //
                        new Column(Messages.ColumnTransactionType, SWT.LEFT), //
                        new Column(Messages.ColumnSecurity, SWT.LEFT, 220) //
                                        .withFormatter(o -> o instanceof Security s ? s.getName() : null).withLogo(), //
                        new Column(Messages.ColumnShares) //
                                        .withFormatter(o -> Values.Share.formatNonZero((Long) o)), //
                        new Column(Messages.ColumnAmount) //
                                        .withFormatter(o -> Values.Money.formatNonZero((Money) o,
                                                        view.getClient().getBaseCurrency())), //
                        new Column(Messages.ColumnFees) //
                                        .withFormatter(o -> Values.Money.formatNonZero((Money) o,
                                                        view.getClient().getBaseCurrency())), //
                        new Column(Messages.ColumnTaxes) //
                                        .withFormatter(o -> Values.Money.formatNonZero((Money) o,
                                                        view.getClient().getBaseCurrency())), //
                        new Column(Messages.ColumnTotal).withFormatter(
                                        o -> Values.Money.formatNonZero((Money) o, view.getClient().getBaseCurrency())), //
                        new Column(Messages.ColumnAccount, SWT.LEFT, 220)
                                        .withFormatter(o -> o instanceof Named n ? n.getName() : null).withLogo());

        model.getTransactions().stream()
                        .filter(pair -> pair.getTransaction().getDateTime().getYear() == month.getYear()
                                        && pair.getTransaction().getDateTime().getMonth() == month.getMonth())
                        .forEach(pair -> {

                            Object[] row = new Object[9];
                            row[0] = pair;
                            pair.withAccountTransaction()
                                            .ifPresent(t -> row[1] = t.getTransaction().getType().toString());
                            pair.withPortfolioTransaction()
                                            .ifPresent(t -> row[1] = t.getTransaction().getType().toString());
                            row[2] = pair.getTransaction().getSecurity();
                            row[3] = pair.getTransaction().getShares();
                            row[4] = pair.getTransaction().getGrossValue();
                            row[5] = pair.getTransaction().getUnitSum(Unit.Type.FEE);
                            row[6] = pair.getTransaction().getUnitSum(Unit.Type.TAX);
                            row[7] = pair.getTransaction().getMonetaryAmount();
                            row[8] = pair.getOwner();

                            builder.addRow(row);
                        });
    }

    private long sumValue(Line line, int index)
    {
        long value = 0;
        for (int ii = index; ii < index + 12 && ii < line.getNoOfMonths(); ii++)
            value += line.getValue(ii);
        return value;
    }

    private long sumTransactions(Line line, int index)
    {
        long value = 0;
        for (int ii = index; ii < index + 12 && ii < line.getNoOfMonths(); ii++)
            value += line.getNumTransations(ii);
        return value;
    }
}
