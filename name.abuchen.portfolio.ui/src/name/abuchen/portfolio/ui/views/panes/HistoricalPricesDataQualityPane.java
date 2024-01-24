package name.abuchen.portfolio.ui.views.panes;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;

import jakarta.inject.Inject;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.QuoteQualityMetrics;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.SecurityPriceDialog;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.views.SecurityQuoteQualityMetricsViewer;
import name.abuchen.portfolio.util.Holiday;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.Pair;
import name.abuchen.portfolio.util.TextUtil;
import name.abuchen.portfolio.util.TradeCalendar;
import name.abuchen.portfolio.util.TradeCalendarManager;

public class HistoricalPricesDataQualityPane implements InformationPanePage
{
    @Inject
    private IPreferenceStore preferences;

    @Inject
    Client client;

    private Label completeness;
    private Label tradeCalendar;
    private Label checkInterval;
    private TableViewer missing;
    private TableViewer unexpected;

    private Security security;
    private TradeCalendar calendar;

    @Override
    public String getLabel()
    {
        return Messages.GroupLabelDataQuality;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(Colors.WHITE);
        FormLayout layout = new FormLayout();
        layout.marginHeight = layout.marginWidth = 5;
        container.setLayout(layout);

        Label lCompleteness = new Label(container, SWT.NONE);
        lCompleteness.setText(Messages.ColumnMetricCompleteness);
        lCompleteness.setToolTipText(TextUtil.wordwrap(Messages.ColumnMetricCompleteness_Description));

        completeness = new Label(container, SWT.NONE);
        completeness.setToolTipText(TextUtil.wordwrap(Messages.ColumnMetricCompleteness_Description));

        Label lTradeCalendar = new Label(container, SWT.NONE);
        lTradeCalendar.setText(Messages.LabelSecurityCalendar);
        tradeCalendar = new Label(container, SWT.NONE);

        checkInterval = new Label(container, SWT.NONE);

        Label missingLabel = new Label(container, SWT.NONE);
        missingLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        missingLabel.setText(Messages.LabelMissingQuotes);
        missingLabel.setToolTipText(TextUtil.wordwrap(Messages.LabelMissingQuotes_Decsription));

        Pair<Composite, TableViewer> missingPair = createTable(container, "@missing"); //$NON-NLS-1$
        Composite missingTable = missingPair.getLeft();
        missing = missingPair.getRight();

        // add context menu to create missing price
        addContextMenuToAddPrices(missing);

        Label unexpectedLabel = new Label(container, SWT.NONE);
        unexpectedLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        unexpectedLabel.setText(Messages.LabelUnexpectedQuotes);
        unexpectedLabel.setToolTipText(TextUtil.wordwrap(Messages.LabelUnexpectedQuotes_Description));

        Pair<Composite, TableViewer> unexpectedPair = createTable(container, "@unexpected"); //$NON-NLS-1$
        Composite unexpectedTable = unexpectedPair.getLeft();
        unexpected = unexpectedPair.getRight();

        FormDataFactory.startingWith(completeness, lCompleteness).right(new FormAttachment(100))
                        .thenBelow(lTradeCalendar).left(new FormAttachment(0)) //
                        .thenRight(tradeCalendar).right(new FormAttachment(100)) //
                        .thenBelow(checkInterval).left(new FormAttachment(0)).right(new FormAttachment(100)) //
                        .thenBelow(missingLabel) //
                        .thenBelow(missingTable).bottom(new FormAttachment(100));

        FormDataFactory.startingWith(missingTable).right(new FormAttachment(50, -10)) //
                        .thenRight(unexpectedTable, 10).right(new FormAttachment(100)) //
                        .top(new FormAttachment(missingTable, 0, SWT.TOP)) //
                        .bottom(new FormAttachment(100)) //
                        .thenUp(unexpectedLabel);

        return container;
    }

    private void addContextMenuToAddPrices(TableViewer table)
    {
        new ContextMenu(table.getControl(), manager -> {

            var firstElement = table.getStructuredSelection().getFirstElement();
            if (firstElement == null)
                return;

            manager.add(new SimpleAction(Messages.SecurityMenuAddPrice, a -> {
                SecurityPriceDialog dialog = new SecurityPriceDialog(Display.getDefault().getActiveShell(), client,
                                security);
                dialog.setDate(((Interval) firstElement).getStart());

                if (dialog.open() != Window.OK)
                    return;

                client.markDirty();
            }));
        }).hook();
    }

    protected Pair<Composite, TableViewer> createTable(Composite parent, String showHideColumnHelperSuffix)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        TableViewer tableViewer = new TableViewer(container, SWT.FULL_SELECTION);
        CopyPasteSupport.enableFor(tableViewer);

        ShowHideColumnHelper support = new ShowHideColumnHelper(
                        SecurityQuoteQualityMetricsViewer.class.getSimpleName() + showHideColumnHelperSuffix,
                        preferences, tableViewer, layout);

        Column column = new Column(Messages.ColumnDate, SWT.None, 300);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Interval interval = (Interval) element;

                if (interval.getStart().equals(interval.getEnd()))
                    return formatDateWithHoliday(interval.getStart(), calendar);
                else
                    return MessageFormat.format(Messages.LabelDateXToY,
                                    formatDateWithHoliday(interval.getStart(), calendar),
                                    formatDateWithHoliday(interval.getEnd(), calendar));
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> ((Interval) e).getStart()), SWT.DOWN);
        support.addColumn(column);

        support.createColumns();

        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);

        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        return new Pair<>(container, tableViewer);
    }

    @Override
    public void setInput(Object input)
    {
        if (completeness == null || completeness.isDisposed())
            return;

        security = Adaptor.adapt(Security.class, input);

        if (security == null)
        {
            calendar = null;
            completeness.setText(""); //$NON-NLS-1$
            tradeCalendar.setText(""); //$NON-NLS-1$
            checkInterval.setText(""); //$NON-NLS-1$
            missing.setInput(new ArrayList<>());
            unexpected.setInput(new ArrayList<>());
        }
        else
        {
            QuoteQualityMetrics metrics = new QuoteQualityMetrics(security);
            calendar = TradeCalendarManager.getInstance(security);

            completeness.setText(Values.Percent2.format(metrics.getCompleteness()));
            tradeCalendar.setText(calendar != null ? calendar.getDescription() : ""); //$NON-NLS-1$
            checkInterval.setText(metrics.getCheckInterval()
                            .map(i -> MessageFormat.format(Messages.LabelMetricCheckInterval,
                                            Values.Date.format(i.getStart()), Values.Date.format(i.getEnd())))
                            .orElse("")); //$NON-NLS-1$

            missing.setInput(metrics.getMissingIntervals());
            unexpected.setInput(metrics.getUnexpectedIntervals());
        }
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (security != null)
            setInput(security);
    }

    private static String formatDateWithHoliday(LocalDate date, TradeCalendar calendar)
    {
        String result = Values.Date.format(date);
        if (calendar == null)
            return result;
        Holiday holiday = calendar.getHoliday(date);
        if (holiday != null)
            result += " (" + holiday.getLabel() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        return result;
    }
}
