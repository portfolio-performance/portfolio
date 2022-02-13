package name.abuchen.portfolio.ui.views.panes;

import java.util.Collections;
import java.util.Iterator;

import javax.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityEvent.DividendEvent;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.TableViewerCSVExporter;
import name.abuchen.portfolio.ui.util.swt.ActiveShell;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.util.viewers.DateEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.DateLabelProvider;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;
import name.abuchen.portfolio.ui.wizards.events.CustomEventWizard;

public class SecurityEventsPane implements InformationPanePage
{
    @Inject
    private Client client;

    @Inject
    private IPreferenceStore preferences;

    private Security security;
    private TableViewer events;

    @Override
    public String getLabel()
    {
        return Messages.SecurityTabEvents;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        return createEventsTable(parent);
    }

    @Override
    public void addButtons(ToolBarManager toolBar)
    {
        toolBar.add(new SimpleAction(Messages.MenuExportData, Images.EXPORT,
                        a -> new TableViewerCSVExporter(events).export(getLabel(), security)));
    }

    @Override
    public void setInput(Object input)
    {
        security = Adaptor.adapt(Security.class, input);
        events.setInput(security != null ? security.getEvents() : Collections.emptyList());
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (security != null)
            setInput(security);
    }

    protected Composite createEventsTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        events = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI);
        CopyPasteSupport.enableFor(events);

        ShowHideColumnHelper support = new ShowHideColumnHelper(SecurityEventsPane.class.getSimpleName(), // $NON-NLS-1$
                        preferences, events, layout);

        Column column = new Column(Messages.ColumnDate, SWT.None, 80);
        column.setLabelProvider(new DateLabelProvider(e -> ((SecurityEvent) e).getDate()));
        column.setSorter(ColumnViewerSorter.create(e -> ((SecurityEvent) e).getDate()), SWT.DOWN);
        column.setEditingSupport(new DateEditingSupport(SecurityEvent.class, "date") //$NON-NLS-1$
        {
            @Override
            public boolean canEdit(Object element)
            {
                return ((SecurityEvent) element).getType().isUserEditable();
            }
        });
        support.addColumn(column);

        column = new Column(Messages.ColumnTransactionType, SWT.None, 120);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((SecurityEvent) element).getType().toString();
            }
        });
        column.setSorter(ColumnViewerSorter.create(e -> ((SecurityEvent) e).getType()));
        support.addColumn(column);

        column = new Column(Messages.ColumnPaymentDate, SWT.NONE, 80);
        column.setLabelProvider(new DateLabelProvider(
                        e -> e instanceof DividendEvent ? ((DividendEvent) e).getPaymentDate() : null));
        column.setSorter(ColumnViewerSorter
                        .create(e -> e instanceof DividendEvent ? ((DividendEvent) e).getPaymentDate() : null));
        support.addColumn(column);

        column = new Column(Messages.ColumnAmount, SWT.NONE, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return element instanceof DividendEvent
                                ? Values.Money.format(((DividendEvent) element).getAmount(), client.getBaseCurrency())
                                : null;
            }
        });
        column.setSorter(ColumnViewerSorter
                        .create(e -> e instanceof DividendEvent ? ((DividendEvent) e).getAmount() : null));
        support.addColumn(column);

        column = new Column(Messages.ColumnDetails, SWT.None, 300);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((SecurityEvent) element).getDetails();
            }
        });
        column.setSorter(ColumnViewerSorter.createIgnoreCase(e -> ((SecurityEvent) e).getDetails()));
        column.setEditingSupport(new StringEditingSupport(SecurityEvent.class, "details") //$NON-NLS-1$
        {
            @Override
            public boolean canEdit(Object element)
            {
                return ((SecurityEvent) element).getType().isUserEditable();
            }
        });
        support.addColumn(column);

        support.createColumns();

        events.getTable().setHeaderVisible(true);
        events.getTable().setLinesVisible(true);

        events.setContentProvider(ArrayContentProvider.getInstance());

        new ContextMenu(events.getControl(), this::eventsMenuAboutToShow).hook();

        return container;
    }

    private void eventsMenuAboutToShow(IMenuManager manager) // NOSONAR
    {
        if (security == null)
            return;

        manager.add(new Action(Messages.SecurityMenuAddEvent)
        {
            @Override
            public void run()
            {
                CustomEventWizard wizard = new CustomEventWizard(client, security);
                WizardDialog dialog = new WizardDialog(ActiveShell.get(), wizard);
                if (dialog.open() == Window.OK)
                    client.markDirty();
            }
        });

        IStructuredSelection selection = events.getStructuredSelection();
        if (selection.isEmpty())
            return;

        manager.add(new Separator());

        manager.add(new Action(Messages.MenuTransactionDelete)
        {
            @Override
            public void run()
            {
                IStructuredSelection selection = events.getStructuredSelection();

                // allow deletion (but not creation) of stock split events.
                // Background: once we refactor stock splits, we might want to
                // rely on the fact that a stock split was created technically
                // in the program, but deletion should be possible anyway

                Iterator<?> iter = selection.iterator();
                while (iter.hasNext())
                    security.removeEvent((SecurityEvent) iter.next());

                client.markDirty();
            }
        });
    }

}
