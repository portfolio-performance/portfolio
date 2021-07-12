package name.abuchen.portfolio.ui.views.panes;

import java.util.ArrayList;

import javax.inject.Inject;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.QuoteQualityMetrics;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.views.SecurityQuoteQualityMetricsViewer;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.Pair;
import name.abuchen.portfolio.util.TextUtil;

public class HistoricalPricesDataQualityPane implements InformationPanePage
{
    @Inject
    private IPreferenceStore preferences;

    private Label completeness;
    private Label checkInterval;
    private TableViewer missing;
    private TableViewer unexpected;

    private Security security;

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

        checkInterval = new Label(container, SWT.NONE);

        Label missingLabel = new Label(container, SWT.NONE);
        missingLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        missingLabel.setText(Messages.LabelMissingQuotes);
        missingLabel.setToolTipText(TextUtil.wordwrap(Messages.LabelMissingQuotes_Decsription));

        Pair<Composite, TableViewer> missingPair = createTable(container, "@missing"); //$NON-NLS-1$
        Composite missingTable = missingPair.getLeft();
        missing = missingPair.getRight();

        Label unexpectedLabel = new Label(container, SWT.NONE);
        unexpectedLabel.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        unexpectedLabel.setText(Messages.LabelUnexpectedQuotes);
        unexpectedLabel.setToolTipText(TextUtil.wordwrap(Messages.LabelUnexpectedQuotes_Description));

        Pair<Composite, TableViewer> unexpectedPair = createTable(container, "@unexpected"); //$NON-NLS-1$
        Composite unexpectedTable = unexpectedPair.getLeft();
        unexpected = unexpectedPair.getRight();

        FormDataFactory.startingWith(completeness, lCompleteness).right(new FormAttachment(100))
                        .thenBelow(checkInterval).left(new FormAttachment(0)).right(new FormAttachment(100))
                        .thenBelow(missingLabel) //
                        .thenBelow(missingTable).bottom(new FormAttachment(100));

        FormDataFactory.startingWith(missingTable) //
                        .thenRight(unexpectedTable, 20).top(new FormAttachment(missingTable, 0, SWT.TOP))
                        .bottom(new FormAttachment(100)) //
                        .thenUp(unexpectedLabel);

        return container;
    }

    protected Pair<Composite, TableViewer> createTable(Composite parent, String showHideColumnHelperSuffix)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        TableViewer tableViewer = new TableViewer(container, SWT.FULL_SELECTION);

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
                    return Values.Date.format(interval.getStart());
                else
                    return MessageFormat.format(Messages.LabelDateXToY, Values.Date.format(interval.getStart()),
                                    Values.Date.format(interval.getEnd()));
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> ((Interval) e).getStart()), SWT.UP);
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
            completeness.setText(""); //$NON-NLS-1$
            checkInterval.setText(""); //$NON-NLS-1$
            missing.setInput(new ArrayList<>());
            unexpected.setInput(new ArrayList<>());
        }
        else
        {
            QuoteQualityMetrics metrics = new QuoteQualityMetrics(security);

            completeness.setText(Values.Percent2.format(metrics.getCompleteness()));
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
}
