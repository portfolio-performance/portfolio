package name.abuchen.portfolio.ui.handlers.tools;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.bootstrap.BundleMessages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.impl.DivvyDiaryDividendFeed;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.handlers.MenuHelper;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;

public class BackfillExDatesHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        var clientInput = MenuHelper.getActiveClientInput(part);
        if (clientInput.isEmpty())
            return;

        var client = clientInput.get().getClient();
        if (client == null)
            return;

        List<MatchedTransaction> matches = new ArrayList<>();

        IRunnableWithProgress operation = monitor -> collectMatches(client, matches, monitor);

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

        var dialog = new BackfillExDatesDialog(shell, client, matches);
        if (dialog.open() != IDialogConstants.OK_ID)
            return;

        matches.forEach(match -> match.transaction.getTransaction().setExDate(match.exDate().atStartOfDay()));
        client.markDirty();

        MessageDialog.openInformation(shell, Messages.LabelInfo,
                        MessageFormat.format(Messages.MsgBackfillExDatesUpdated, matches.size()));
    }

    private void collectMatches(Client client, List<MatchedTransaction> matches, IProgressMonitor monitor)
                    throws InterruptedException
    {
        var feed = Factory.getDividendFeed(DivvyDiaryDividendFeed.class);

        var candidates = collectCandidateTransactions(client);
        monitor.beginTask(BundleMessages.getString(BundleMessages.Label.Command.backfillExDate), candidates.size());

        Map<Security, List<DividendEvent>> security2localEvents = new HashMap<>();
        Map<Security, List<DividendEvent>> security2onlineEvents = new HashMap<>();

        for (var candidate : candidates)
        {
            if (monitor.isCanceled())
                throw new InterruptedException();

            var security = candidate.getTransaction().getSecurity();
            monitor.subTask(security.getName());

            var bookingDate = candidate.getTransaction().getDateTime().toLocalDate();

            // Phase 1: try local dividend events
            var localEvents = security2localEvents.computeIfAbsent(security,
                            s -> s.getEvents().stream()
                                            .filter(e -> e.getType() == SecurityEvent.Type.DIVIDEND_PAYMENT)
                                            .map(DividendEvent.class::cast)
                                            .toList());

            var exDate = DividendEvent.findExDateByPaymentDate(bookingDate, localEvents);

            // Phase 2: if no local match, try DivvyDiary
            if (exDate.isEmpty())
            {
                var onlineEvents = security2onlineEvents.computeIfAbsent(security, s -> {
                    try
                    {
                        return feed.getDividendPayments(s);
                    }
                    catch (IOException e)
                    {
                        PortfolioPlugin.log(s.getName(), e);
                        return List.of();
                    }
                });

                exDate = DividendEvent.findExDateByPaymentDate(bookingDate, onlineEvents);
            }

            exDate.ifPresent(date -> matches.add(new MatchedTransaction(candidate, date)));

            monitor.worked(1);
        }

        monitor.done();
    }

    private List<TransactionPair<AccountTransaction>> collectCandidateTransactions(Client client)
    {
        var answer = new ArrayList<TransactionPair<AccountTransaction>>();

        for (var account : client.getAccounts())
        {
            for (var transaction : account.getTransactions())
            {
                if (transaction.getType() != AccountTransaction.Type.DIVIDENDS)
                    continue;

                if (transaction.getExDate() != null)
                    continue;

                answer.add(new TransactionPair<>(account, transaction));
            }
        }

        return answer;
    }

    private record MatchedTransaction(TransactionPair<AccountTransaction> transaction, LocalDate exDate)
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
            setTitle(BundleMessages.getString(BundleMessages.Label.Command.backfillExDate));
            return contents;
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            var composite = (Composite) super.createDialogArea(parent);

            var tableArea = new Composite(composite, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, true).hint(900, 400).applyTo(tableArea);

            var layout = new TableColumnLayout();
            tableArea.setLayout(layout);

            var tableViewer = new TableViewer(tableArea, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
            CopyPasteSupport.enableFor(tableViewer);
            tableViewer.setContentProvider(ArrayContentProvider.getInstance());
            tableViewer.getTable().setHeaderVisible(true);
            tableViewer.getTable().setLinesVisible(true);

            var column = new TableViewerColumn(tableViewer, SWT.NONE);
            column.getColumn().setText(Messages.ColumnDate);
            column.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    return Values.Date.format(((MatchedTransaction) element).transaction().getTransaction()
                                    .getDateTime().toLocalDate());
                }
            });
            layout.setColumnData(column.getColumn(), new ColumnPixelData(90));
            ColumnViewerSorter.create(e -> ((MatchedTransaction) e).transaction().getTransaction().getDateTime())
                            .attachTo(tableViewer, column);

            column = new TableViewerColumn(tableViewer, SWT.NONE);
            column.getColumn().setText(Messages.ColumnAccount);
            column.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    return ((Account) ((MatchedTransaction) element).transaction().getOwner()).getName();
                }
            });
            layout.setColumnData(column.getColumn(), new ColumnPixelData(140));
            ColumnViewerSorter.create(e -> ((Account) ((MatchedTransaction) e).transaction().getOwner()).getName())
                            .attachTo(tableViewer, column);

            column = new TableViewerColumn(tableViewer, SWT.NONE);
            column.getColumn().setText(Messages.ColumnSecurity);
            column.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    return ((MatchedTransaction) element).transaction().getTransaction().getSecurity().getName();
                }

                @Override
                public Image getImage(Object element)
                {
                    return LogoManager.instance().getDefaultColumnImage(
                                    ((MatchedTransaction) element).transaction().getTransaction().getSecurity(),
                                    client.getSettings());
                }
            });
            layout.setColumnData(column.getColumn(), new ColumnPixelData(260));
            ColumnViewerSorter.create(
                            e -> ((MatchedTransaction) e).transaction().getTransaction().getSecurity().getName())
                            .attachTo(tableViewer, column);

            column = new TableViewerColumn(tableViewer, SWT.RIGHT);
            column.getColumn().setText(Messages.ColumnAmount);
            column.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    return Values.Money.format(
                                    ((MatchedTransaction) element).transaction().getTransaction().getMonetaryAmount(),
                                    client.getBaseCurrency());
                }
            });
            layout.setColumnData(column.getColumn(), new ColumnPixelData(110));
            ColumnViewerSorter.create(e -> ((MatchedTransaction) e).transaction().getTransaction().getAmount())
                            .attachTo(tableViewer, column);

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
