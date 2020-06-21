package name.abuchen.portfolio.ui.views;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.math.CorrelationMatrix;
import name.abuchen.portfolio.math.CorrelationMatrix.Correlation;
import name.abuchen.portfolio.math.CorrelationMatrix.DataFrame;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesCache;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesConfigurator;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.util.TextUtil;

public class CorrelationListView extends AbstractHistoricView
{
    private static class Data implements DataFrame
    {
        DataSeries series;
        PerformanceIndex index;

        public Data(DataSeries series, PerformanceIndex index)
        {
            this.series = series;
            this.index = index;
        }

        @Override
        public LocalDate[] dates()
        {
            return index.getDates();
        }

        @Override
        public double[] values()
        {
            return index.getDeltaPercentage();
        }

        @Override
        public IntPredicate filter()
        {
            return index.filterReturnsForVolatilityCalculation();
        }
    }

    private DataSeriesConfigurator configurator;

    private CorrelationMatrix<Data> matrix;
    private DataSeriesCache cache;

    private TableViewer matrixViewer;
    private TableColumnLayout matrixLayout;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelCorrelationMatrix;
    }

    @Override
    public void notifyModelUpdated()
    {
        updateMatrix();
    }

    @Override
    public void reportingPeriodUpdated()
    {
        updateMatrix();
    }

    @Override
    protected void addButtons(ToolBarManager toolBar)
    {
        super.addButtons(toolBar);
        addExportButton(toolBar);
        toolBar.add(new DropDown(Messages.MenuConfigureChart, Images.CONFIG, SWT.NONE, manager -> {
            manager.add(new LabelOnly(Messages.LabelDataSeries));
            configurator.configMenuAboutToShow(manager);
        }));
    }

    private void addExportButton(ToolBarManager manager)
    {
        manager.add(new SimpleAction(Messages.MenuExportData, Images.EXPORT.descriptor(),
                        a -> new TableViewerCSVExporter(matrixViewer).export(getTitle() + ".csv"))); //$NON-NLS-1$
    }

    @Override
    protected Control createBody(Composite parent)
    {
        cache = make(DataSeriesCache.class);

        configurator = new DataSeriesConfigurator(this, DataSeries.UseCase.RETURN_VOLATILITY);
        configurator.addListener(this::updateMatrix);
        configurator.setToolBarManager(getViewToolBarManager());

        updateTitle(Messages.LabelCorrelationMatrix + " (" + configurator.getConfigurationName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$

        Composite container = new Composite(parent, SWT.NONE);

        matrixLayout = new TableColumnLayout();
        container.setLayout(matrixLayout);

        matrixViewer = new TableViewer(container, SWT.FULL_SELECTION);
        ColumnViewerToolTipSupport.enableFor(matrixViewer, ToolTip.NO_RECREATE);

        matrixViewer.getTable().setHeaderVisible(true);
        matrixViewer.getTable().setLinesVisible(true);

        matrixViewer.setContentProvider(ArrayContentProvider.getInstance());

        updateMatrix();

        return container;
    }

    private void updateMatrix()
    {
        Interval interval = getReportingPeriod().toInterval(LocalDate.now());

        List<Data> data = configurator.getSelectedDataSeries().stream().map(series -> {
            PerformanceIndex index = cache.lookup(series, interval);
            return new Data(series, index);
        }).collect(Collectors.toList());

        matrix = new CorrelationMatrix<>(data);

        updateTitle(Messages.LabelCorrelationMatrix + " (" + configurator.getConfigurationName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$

        updateBottomTableColumns();
    }

    private void createBottomTableColumns()
    {
        createBottomTableVehicleColumn();

        for (int index = 0; index < matrix.getDataFrames().size(); index++)
            createBottomTableCorrelationColumn(index);
    }

    private void createBottomTableVehicleColumn()
    {
        TableViewerColumn column = new TableViewerColumn(matrixViewer, SWT.NONE);
        column.getColumn().setText(Messages.ColumnSecurity);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public Image getImage(Object element)
            {
                return matrix.getDataFrames().get((Integer) element).series.getImage();
            }

            @Override
            public String getText(Object element)
            {
                return matrix.getDataFrames().get((Integer) element).series.getLabel();
            }
        });

        matrixLayout.setColumnData(column.getColumn(), new ColumnPixelData(200));
    }

    private void createBottomTableCorrelationColumn(int index)
    {
        TableViewerColumn column = new TableViewerColumn(matrixViewer, SWT.RIGHT);
        column.getColumn().setText(matrix.getDataFrames().get(index).series.getLabel());
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Integer x = (Integer) element;
                Optional<Correlation> data = matrix.getCorrelation(x, index);
                if (!data.isPresent())
                    return "-"; //$NON-NLS-1$
                else if (Double.isNaN(data.get().getR()))
                    return "n/a"; //$NON-NLS-1$
                else
                    return Values.PercentPlain.format(data.get().getR());
            }

            @Override
            public String getToolTipText(Object element)
            {
                Integer x = (Integer) element;
                Optional<Correlation> data = matrix.getCorrelation(x, index);

                if (!data.isPresent())
                    return null;

                Data frame1 = matrix.getDataFrames().get(x);
                DataSeries vehicle1 = frame1.series;
                Data frame2 = matrix.getDataFrames().get(index);
                DataSeries vehicle2 = frame2.series;

                if (Double.isNaN(data.get().getR()))
                    return TextUtil.tooltip(MessageFormat.format(Messages.ToolTipCorrelationNotAvailable,
                                    vehicle1.getLabel(), vehicle2.getLabel()));

                return TextUtil.tooltip(MessageFormat.format(Messages.ToolTipCorrelationData,
                                Values.PercentPlain.format(data.get().getR()), //
                                vehicle1.getLabel(), vehicle2.getLabel(), //
                                Values.Id.format(data.get().getCount()), //
                                Values.Date.format(data.get().getFirstDate()),
                                Values.Date.format(data.get().getLastDate())));
            }
        });

        matrixLayout.setColumnData(column.getColumn(), new ColumnPixelData(60));
    }

    private void updateBottomTableColumns()
    {
        try
        {
            // first add, then remove columns
            // (otherwise rendering of first column is broken)
            matrixViewer.getTable().setRedraw(false);

            int count = matrixViewer.getTable().getColumnCount();

            createBottomTableColumns();

            for (int ii = 0; ii < count; ii++)
                matrixViewer.getTable().getColumn(0).dispose();

            matrixViewer.setInput(IntStream.range(0, matrix.getDataFrames().size()).mapToObj(i -> Integer.valueOf(i))
                            .collect(Collectors.toList()));
        }
        finally
        {
            matrixViewer.getTable().getParent().layout(true);
            matrixViewer.getTable().setRedraw(true);
        }
    }

}
