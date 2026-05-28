package name.abuchen.portfolio.ui.dialogs.transactions;

import static name.abuchen.portfolio.ui.util.FormDataFactory.startingWith;
import static name.abuchen.portfolio.ui.util.SWTHelper.amountWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.currencyWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.widest;

import java.time.LocalDateTime;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.FundTransferEntry;
import name.abuchen.portfolio.model.FundTransferEntry.CarriedLot;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.FundTransferModel.Properties;
import name.abuchen.portfolio.ui.util.SWTHelper;
import name.abuchen.portfolio.ui.util.SecurityNameLabelProvider;

public class FundTransferDialog extends AbstractTransactionDialog
{
    private Client client;

    @Inject
    public FundTransferDialog(@Named(org.eclipse.e4.ui.services.IServiceConstants.ACTIVE_SHELL) Shell parentShell,
                    Client client)
    {
        super(parentShell);
        this.client = client;
        setModel(new FundTransferModel(client));
    }

    private FundTransferModel model()
    {
        return (FundTransferModel) this.model;
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        //
        // input elements
        //

        ComboInput sourceSecurity = new ComboInput(editArea,
                        Messages.ColumnAccountFrom + " " + Messages.ColumnSecurity); //$NON-NLS-1$
        sourceSecurity.value.setInput(including(client.getActiveSecurities(), model().getSourceSecurity()));
        sourceSecurity.value.setLabelProvider(new SecurityNameLabelProvider(client));
        sourceSecurity.bindValue(Properties.sourceSecurity.name(), Messages.MsgMissingSecurity);
        sourceSecurity.bindCurrency(Properties.sourceSecurityCurrencyCode.name());

        ComboInput targetSecurity = new ComboInput(editArea,
                        Messages.ColumnAccountTo + " " + Messages.ColumnSecurity); //$NON-NLS-1$
        targetSecurity.value.setInput(including(client.getActiveSecurities(), model().getTargetSecurity()));
        targetSecurity.value.setLabelProvider(new SecurityNameLabelProvider(client));
        targetSecurity.bindValue(Properties.targetSecurity.name(), Messages.MsgMissingSecurity);
        targetSecurity.bindCurrency(Properties.targetSecurityCurrencyCode.name());

        ComboInput sourcePortfolio = new ComboInput(editArea,
                        Messages.ColumnAccountFrom + " " + Messages.ColumnPortfolio); //$NON-NLS-1$
        sourcePortfolio.value.setInput(including(client.getActivePortfolios(), model().getSourcePortfolio()));
        sourcePortfolio.bindValue(Properties.sourcePortfolio.name(), Messages.MsgPortfolioFromMissing);

        ComboInput targetPortfolio = new ComboInput(editArea,
                        Messages.ColumnAccountTo + " " + Messages.ColumnPortfolio); //$NON-NLS-1$
        targetPortfolio.value.setInput(including(client.getActivePortfolios(), model().getTargetPortfolio()));
        targetPortfolio.bindValue(Properties.targetPortfolio.name(), Messages.MsgPortfolioToMissing);

        DateTimeInput dateTime = new DateTimeInput(editArea, Messages.ColumnDate);
        dateTime.bindDate(Properties.date.name());
        dateTime.bindTime(Properties.time.name());
        dateTime.bindButton(() -> model().getTime(), time -> model().setTime(time));

        Input sourceShares = new Input(editArea, Messages.ColumnAccountFrom + " " + Messages.ColumnShares); //$NON-NLS-1$
        sourceShares.bindValue(Properties.sourceShares.name(), Messages.ColumnShares, Values.Share, true);

        Input sourceAmount = new Input(editArea, "="); //$NON-NLS-1$
        sourceAmount.bindValue(Properties.sourceAmount.name(), Messages.ColumnAmount, Values.Amount, true);
        sourceAmount.bindCurrency(Properties.sourceSecurityCurrencyCode.name());

        Input targetShares = new Input(editArea, Messages.ColumnAccountTo + " " + Messages.ColumnShares); //$NON-NLS-1$
        targetShares.bindValue(Properties.targetShares.name(), Messages.ColumnShares, Values.Share, true);

        Input targetAmount = new Input(editArea, "="); //$NON-NLS-1$
        targetAmount.bindValue(Properties.targetAmount.name(), Messages.ColumnAmount, Values.Amount, true);
        targetAmount.bindCurrency(Properties.targetSecurityCurrencyCode.name());

        Label previewLabel = new Label(editArea, SWT.LEFT);
        previewLabel.setText(Messages.ColumnPurchaseValue);
        Composite previewArea = createPreviewTable(editArea);

        Label lblNote = new Label(editArea, SWT.LEFT);
        lblNote.setText(Messages.ColumnNote);
        Text valueNote = new Text(editArea, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        IObservableValue<?> targetNote = WidgetProperties.text(SWT.Modify).observe(valueNote);
        IObservableValue<?> noteObservable = BeanProperties.value(Properties.note.name()).observe(model);
        context.bindValue(targetNote, noteObservable);

        //
        // form layout
        //

        int amountWidth = amountWidth(sourceAmount.value);
        int currencyWidth = currencyWidth(sourceAmount.currency);

        startingWith(sourceSecurity.value.getControl(), sourceSecurity.label).suffix(sourceSecurity.currency)
                        .thenBelow(targetSecurity.value.getControl()).label(targetSecurity.label)
                        .suffix(targetSecurity.currency).thenBelow(sourcePortfolio.value.getControl())
                        .label(sourcePortfolio.label).thenBelow(targetPortfolio.value.getControl())
                        .label(targetPortfolio.label).thenBelow(dateTime.date.getControl()).label(dateTime.label)
                        .thenRight(dateTime.time).thenRight(dateTime.button, 0);

        startingWith(dateTime.date.getControl()).thenBelow(sourceShares.value).width(amountWidth)
                        .label(sourceShares.label).thenRight(sourceAmount.label).thenRight(sourceAmount.value)
                        .width(amountWidth).thenRight(sourceAmount.currency).width(currencyWidth);

        startingWith(sourceShares.value).thenBelow(targetShares.value).width(amountWidth).label(targetShares.label)
                        .thenRight(targetAmount.label).thenRight(targetAmount.value).width(amountWidth)
                        .thenRight(targetAmount.currency).width(currencyWidth);

        startingWith(targetShares.value).thenBelow(previewArea).height(SWTHelper.lineHeight(valueNote) * 6)
                        .left(sourceSecurity.value.getControl()).right(targetAmount.currency).label(previewLabel);

        startingWith(previewArea).thenBelow(valueNote).height(SWTHelper.lineHeight(valueNote) * 3)
                        .left(sourceSecurity.value.getControl()).right(targetAmount.currency).label(lblNote);

        int widest = widest(sourceSecurity.label, targetSecurity.label, sourcePortfolio.label, targetPortfolio.label,
                        dateTime.label, sourceShares.label, targetShares.label, previewLabel, lblNote);
        startingWith(sourceSecurity.label).width(widest);

        WarningMessages warnings = new WarningMessages(this);
        warnings.add(() -> LocalDateTime.of(model().getDate(), model().getTime()).isAfter(LocalDateTime.now())
                        ? Messages.MsgDateIsInTheFuture
                        : null);
        warnings.add(() -> model().getSourceSecurity() != null
                        ? new StockSplitWarning().check(model().getSourceSecurity(), model().getDate())
                        : null);
        warnings.add(() -> model().getTargetSecurity() != null
                        ? new StockSplitWarning().check(model().getTargetSecurity(), model().getDate())
                        : null);
        model.addPropertyChangeListener(Properties.sourceSecurity.name(), e -> warnings.check());
        model.addPropertyChangeListener(Properties.targetSecurity.name(), e -> warnings.check());
        model.addPropertyChangeListener(Properties.date.name(), e -> warnings.check());
    }

    private Composite createPreviewTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        TableViewer viewer = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
        Table table = viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        addColumn(viewer, layout, Messages.ColumnDate, 100, SWT.NONE,
                        lot -> Values.Date.format(lot.getAcquisitionDate()));
        addColumn(viewer, layout, Messages.ColumnAccountFrom + " " + Messages.ColumnShares, 110, SWT.RIGHT, //$NON-NLS-1$
                        lot -> Values.Share.format(lot.getSourceShares()));
        addColumn(viewer, layout, Messages.ColumnAccountTo + " " + Messages.ColumnShares, 110, SWT.RIGHT, //$NON-NLS-1$
                        lot -> Values.Share.format(lot.getTargetShares()));
        addColumn(viewer, layout, Messages.ColumnPurchaseValue, 140, SWT.RIGHT, this::formatAcquisitionValue);

        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setInput(model().getCarriedLots());

        // The preview is not another market-value field. It shows the FIFO
        // acquisition lots that will be carried to the target fund for deferred
        // capital-gains calculations.
        model.addPropertyChangeListener(Properties.carriedLots.name(), e -> viewer.setInput(model().getCarriedLots()));

        return container;
    }

    private void addColumn(TableViewer viewer, TableColumnLayout layout, String label, int width, int align,
                    Function<CarriedLot, String> formatter)
    {
        TableViewerColumn column = new TableViewerColumn(viewer, align);
        column.getColumn().setText(label);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return formatter.apply((CarriedLot) element);
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(width, true));
    }

    private String formatAcquisitionValue(CarriedLot lot)
    {
        Money acquisitionValue = lot.getAcquisitionValue();
        return acquisitionValue != null ? Values.Money.format(acquisitionValue) : null;
    }

    @Override
    public void setSecurity(Security security)
    {
        model().setSourceSecurity(security);
    }

    @Override
    public void setPortfolio(Portfolio portfolio)
    {
        model().setSourcePortfolio(portfolio);
    }

    public void setEntry(FundTransferEntry entry)
    {
        model().setSource(entry);
    }

    public void presetEntry(FundTransferEntry entry)
    {
        model().presetFromSource(entry);
    }
}
