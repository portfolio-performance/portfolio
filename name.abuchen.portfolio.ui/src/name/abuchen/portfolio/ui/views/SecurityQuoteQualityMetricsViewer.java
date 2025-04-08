package name.abuchen.portfolio.ui.views;

import java.text.MessageFormat;
import java.util.ArrayList;

import jakarta.inject.Inject;

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

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.QuoteQualityMetrics;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.util.Interval;

public class SecurityQuoteQualityMetricsViewer
{
    @Inject
    private IPreferenceStore preferences;

    private Label completeness;
    private Label checkInterval;
    private TableViewer missing;

    public Control createViewControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(Colors.WHITE);
        FormLayout layout = new FormLayout();
        layout.marginHeight = layout.marginWidth = 5;
        container.setLayout(layout);

        Label lCompleteness = new Label(container, SWT.NONE);
        lCompleteness.setText(Messages.ColumnMetricCompleteness);
        lCompleteness.setToolTipText(Messages.ColumnMetricCompleteness_Description);

        completeness = new Label(container, SWT.NONE);
        completeness.setToolTipText(Messages.ColumnMetricCompleteness_Description);

        checkInterval = new Label(container, SWT.NONE);

        Composite table = createTable(container);

        FormDataFactory.startingWith(completeness, lCompleteness).right(new FormAttachment(100))
                        .thenBelow(checkInterval).left(new FormAttachment(0)).thenBelow(table)
                        .bottom(new FormAttachment(100));

        return container;
    }

    protected Composite createTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        missing = new TableViewer(container, SWT.FULL_SELECTION);

        CopyPasteSupport.enableFor(missing);

        ShowHideColumnHelper support = new ShowHideColumnHelper(
                        SecurityQuoteQualityMetricsViewer.class.getSimpleName() + "@missing", //$NON-NLS-1$
                        preferences, missing, layout);

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
        column.setSorter(ColumnViewerSorter.create(e -> ((Interval) e).getStart()), SWT.DOWN);
        support.addColumn(column);

        support.createColumns();

        missing.getTable().setHeaderVisible(true);
        missing.getTable().setLinesVisible(true);

        missing.setContentProvider(ArrayContentProvider.getInstance());

        return container;
    }

    public void setInput(Security security)
    {
        if (completeness == null || completeness.isDisposed())
            return;

        if (security == null)
        {
            completeness.setText(""); //$NON-NLS-1$
            checkInterval.setText(""); //$NON-NLS-1$
            missing.setInput(new ArrayList<>());
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
        }
    }
}
