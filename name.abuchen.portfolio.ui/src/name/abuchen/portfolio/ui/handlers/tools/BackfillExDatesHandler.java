package name.abuchen.portfolio.ui.handlers.tools;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.google.common.base.Strings;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.impl.DivvyDiaryDividendFeed;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.handlers.MenuHelper;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;

public class BackfillExDatesHandler
{
    private static final long PAYMENT_DATE_TOLERANCE_DAYS = 5;

    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Optional @Preference(UIConstants.Preferences.DIVVYDIARY_API_KEY) String divvyDiaryApiKey)
    {
        if (Strings.isNullOrEmpty(divvyDiaryApiKey) || divvyDiaryApiKey.isBlank())
        {
            MessageDialog.openWarning(shell, Messages.LabelInfo, Messages.MsgBackfillExDatesMissingAPIKey);
            return;
        }

        var clientInput = MenuHelper.getActiveClientInput(part);
        if (clientInput.isEmpty())
            return;

        var client = clientInput.get().getClient();
        if (client == null)
            return;

        List<MatchedTransaction> matches = new ArrayList<>();

        IRunnableWithProgress operation = monitor -> collectMatches(client, divvyDiaryApiKey, matches, monitor);

        try
        {
            new ProgressMonitorDialog(shell).run(true, true, operation);
        }
        catch (InvocationTargetException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(shell, Messages.LabelError,
                            e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            return;
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return;
        }

        if (matches.isEmpty())
        {
            MessageDialog.openInformation(shell, Messages.LabelInfo, Messages.MsgBackfillExDatesNoCandidates);
            return;
        }

        BackfillExDatesDialog dialog = new BackfillExDatesDialog(shell, client, matches);
        if (dialog.open() != IDialogConstants.OK_ID)
            return;

        matches.forEach(match -> match.transaction().setExDate(match.exDate().atStartOfDay()));
        client.markDirty();

        MessageDialog.openInformation(shell, Messages.LabelInfo,
                        MessageFormat.format(Messages.MsgBackfillExDatesUpdated, matches.size()));
    }

    private void collectMatches(Client client, String divvyDiaryApiKey, List<MatchedTransaction> matches,
                    IProgressMonitor monitor) throws InterruptedException
    {
        var feed = (DivvyDiaryDividendFeed) Factory.getDividendFeed(DivvyDiaryDividendFeed.class);
        feed.setApiKey(divvyDiaryApiKey);

        var pendingTransactions = collectPendingTransactions(client);
        monitor.beginTask(Messages.LabelBackfillExDates, pendingTransactions.size());

        Map<Security, List<DividendEvent>> security2events = new HashMap<>();

        for (PendingTransaction pending : pendingTransactions)
        {
            if (monitor.isCanceled())
                throw new InterruptedException();

            var security = pending.transaction().getSecurity();
            monitor.subTask(security.getName());

            var events = security2events.get(security);
            if (events == null)
            {
                try
                {
                    events = feed.getDividendPayments(security);
                }
                catch (IOException e)
                {
                    PortfolioPlugin.log(security.getName(), e);
                    events = List.of();
                }
                security2events.put(security, events);
            }

            findBestMatch(pending.transaction(), events).ifPresent(exDate -> matches
                            .add(new MatchedTransaction(pending.account(), pending.transaction(), exDate)));

            monitor.worked(1);
        }

        monitor.done();
    }

    private List<PendingTransaction> collectPendingTransactions(Client client)
    {
        List<PendingTransaction> answer = new ArrayList<>();

        for (Account account : client.getAccounts())
        {
            for (AccountTransaction transaction : account.getTransactions())
            {
                if (transaction.getType() != AccountTransaction.Type.DIVIDENDS)
                    continue;

                if (transaction.getExDate() != null)
                    continue;

                var security = transaction.getSecurity();
                if (security == null || Strings.isNullOrEmpty(security.getIsin()))
                    continue;

                answer.add(new PendingTransaction(account, transaction));
            }
        }

        return answer;
    }

    private java.util.Optional<LocalDate> findBestMatch(AccountTransaction transaction,
                    List<DividendEvent> dividendEvents)
    {
        var txDate = transaction.getDateTime().toLocalDate();

        return dividendEvents.stream().filter(event -> event.getDate() != null && event.getPaymentDate() != null)
                        .filter(event -> Math.abs(ChronoUnit.DAYS.between(txDate,
                                        event.getPaymentDate())) <= PAYMENT_DATE_TOLERANCE_DAYS)
                        .min(Comparator.comparingLong((DividendEvent event) -> Math
                                        .abs(ChronoUnit.DAYS.between(txDate, event.getPaymentDate())))
                                        .thenComparing(DividendEvent::getPaymentDate))
                        .map(DividendEvent::getDate);
    }

    private record PendingTransaction(Account account, AccountTransaction transaction)
    {
    }

    private record MatchedTransaction(Account account, AccountTransaction transaction, LocalDate exDate)
    {
    }

    private static class BackfillExDatesDialog extends TitleAreaDialog
    {
        private final Client client;
        private final List<MatchedTransaction> matches;

        public BackfillExDatesDialog(Shell parentShell, Client client, List<MatchedTransaction> matches)
        {
            super(parentShell);
            setTitleImage(Images.BANNER.image());
            this.client = client;
            this.matches = new ArrayList<>(matches);
        }

        @Override
        protected void setShellStyle(int newShellStyle)
        {
            super.setShellStyle(newShellStyle | SWT.RESIZE);
        }

        @Override
        protected Control createContents(Composite parent)
        {
            Control contents = super.createContents(parent);
            setTitle(Messages.DialogBackfillExDatesTitle);
            setMessage(Messages.DialogBackfillExDatesMessage);
            return contents;
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            Composite composite = (Composite) super.createDialogArea(parent);

            Composite tableArea = new Composite(composite, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, true).hint(900, 400).applyTo(tableArea);
            tableArea.setLayout(new FillLayout());

            TableColumnLayout layout = new TableColumnLayout();
            tableArea.setLayout(layout);

            TableViewer tableViewer = new TableViewer(tableArea, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
            CopyPasteSupport.enableFor(tableViewer);
            tableViewer.setContentProvider(ArrayContentProvider.getInstance());
            tableViewer.getTable().setHeaderVisible(true);
            tableViewer.getTable().setLinesVisible(true);

            TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.NONE);
            column.getColumn().setText(Messages.ColumnDate);
            column.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    return Values.Date.format(((MatchedTransaction) element).transaction().getDateTime().toLocalDate());
                }
            });
            layout.setColumnData(column.getColumn(), new ColumnPixelData(90));
            ColumnViewerSorter.create(e -> ((MatchedTransaction) e).transaction().getDateTime()).attachTo(tableViewer,
                            column);

            column = new TableViewerColumn(tableViewer, SWT.NONE);
            column.getColumn().setText(Messages.ColumnAccount);
            column.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    return ((MatchedTransaction) element).account().getName();
                }
            });
            layout.setColumnData(column.getColumn(), new ColumnPixelData(140));
            ColumnViewerSorter.create(e -> ((MatchedTransaction) e).account().getName()).attachTo(tableViewer, column);

            column = new TableViewerColumn(tableViewer, SWT.NONE);
            column.getColumn().setText(Messages.ColumnSecurity);
            column.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    return ((MatchedTransaction) element).transaction().getSecurity().getName();
                }

                @Override
                public Image getImage(Object element)
                {
                    return LogoManager.instance().getDefaultColumnImage(
                                    ((MatchedTransaction) element).transaction().getSecurity(), client.getSettings());
                }
            });
            layout.setColumnData(column.getColumn(), new ColumnPixelData(260));
            ColumnViewerSorter.create(e -> ((MatchedTransaction) e).transaction().getSecurity().getName())
                            .attachTo(tableViewer, column);

            column = new TableViewerColumn(tableViewer, SWT.RIGHT);
            column.getColumn().setText(Messages.ColumnAmount);
            column.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    return Values.Money.format(((MatchedTransaction) element).transaction().getMonetaryAmount(),
                                    client.getBaseCurrency());
                }
            });
            layout.setColumnData(column.getColumn(), new ColumnPixelData(110));
            ColumnViewerSorter.create(e -> ((MatchedTransaction) e).transaction().getAmount()).attachTo(tableViewer,
                            column);

            column = new TableViewerColumn(tableViewer, SWT.NONE);
            column.getColumn().setText(Messages.ColumnExDate);
            column.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    return Values.Date.format(((MatchedTransaction) element).exDate());
                }
            });
            layout.setColumnData(column.getColumn(), new ColumnPixelData(90));
            ColumnViewerSorter.create(e -> ((MatchedTransaction) e).exDate()).attachTo(tableViewer, column);

            tableViewer.setInput(matches);

            return composite;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent)
        {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        }
    }
}
