package name.abuchen.portfolio.ui.editor;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.swt.SashLayout;
import name.abuchen.portfolio.ui.util.swt.SashLayoutData;
import name.abuchen.portfolio.ui.views.ExceptionView;

public class PortfolioPart implements ClientInputListener
{
    private ClientInput clientInput;
    private ReportingPeriod selectedPeriod;

    private Composite container;
    private ProgressBar progressBar;
    private PageBook book;
    private AbstractFinanceView view;

    private Control focus;

    @Inject
    MPart part;

    @Inject
    MDirtyable dirty;

    @Inject
    IEclipseContext context;

    @Inject
    IEventBroker broker;

    @Inject
    ESelectionService selectionService;

    @Inject
    ClientInputFactory clientInputFactory;

    @PostConstruct
    public void createComposite(Composite parent)
    {
        // is client available? (e.g. via new file wizard)
        clientInput = (ClientInput) part.getTransientData().get(ClientInput.class.getName());

        if (clientInput == null)
        {
            // is file name available? (e.g. load file, open on startup)
            String filename = part.getPersistedState().get(UIConstants.PersistedState.FILENAME);
            if (filename != null)
            {
                clientInput = clientInputFactory.lookup(new File(filename));
                part.getTransientData().put(ClientInput.class.getName(), clientInput);

                broker.post(UIConstants.Event.File.OPENED, clientInput.getFile().getAbsolutePath());
            }
        }

        if (clientInput == null)
            throw new IllegalArgumentException();

        if (clientInput.getFile() != null)
            part.getPersistedState().put(UIConstants.PersistedState.FILENAME, clientInput.getFile().getAbsolutePath());

        clientInput.addListener(this);
        dirty.setDirty(clientInput.isDirty());

        if (clientInput.getClient() != null)
        {
            this.context.set(Client.class, clientInput.getClient());
            createContainerWithViews(parent);
        }
        else if (ClientFactory.isEncrypted(clientInput.getFile()))
        {
            createContainerWithMessage(parent,
                            MessageFormat.format(Messages.MsgOpenFile, clientInput.getFile().getName()), false, true);
        }
        else
        {
            this.progressBar = createContainerWithMessage(parent,
                            MessageFormat.format(Messages.MsgLoadingFile, clientInput.getFile().getName()), true,
                            false);
        }
    }

    private void createContainerWithViews(Composite parent)
    {
        container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());

        Composite sash = new Composite(container, SWT.NONE);
        SashLayout sashLayout = new SashLayout(sash, SWT.HORIZONTAL | SWT.BEGINNING);
        sash.setLayout(sashLayout);

        Composite navigationBar = new Composite(sash, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).margins(0, 0).applyTo(navigationBar);

        ClientEditorSidebar sidebar = new ClientEditorSidebar(this);
        Control control = sidebar.createSidebarControl(navigationBar);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(control);

        sashLayout.addQuickNavigation(sidebar::menuAboutToShow);

        Composite divider = new Composite(navigationBar, SWT.NONE);
        divider.setBackground(Colors.SIDEBAR_BORDER);
        GridDataFactory.fillDefaults().span(0, 2).hint(1, SWT.DEFAULT).applyTo(divider);

        ClientProgressProvider provider = make(ClientProgressProvider.class, clientInput.getClient(), navigationBar);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(provider.getControl());

        book = new PageBook(sash, SWT.NONE);

        // restore & save size of navigation bar
        final String sashIdentifier = PortfolioPart.class.getSimpleName() + "-newsash"; //$NON-NLS-1$
        int size = 0;

        try
        {
            String value = part.getPersistedState().get(sashIdentifier);
            if (value != null && !value.isEmpty())
                size = Integer.parseInt(value);
        }
        catch (NumberFormatException ignore)
        {
            PortfolioPlugin.log(ignore);
        }

        navigationBar.setLayoutData(new SashLayoutData(size != 0 ? size : 180));
        sash.addDisposeListener(e -> part.getPersistedState().put(sashIdentifier,
                        String.valueOf(((SashLayoutData) navigationBar.getLayoutData()).getSize())));

        sidebar.selectDefaultView();

        focus = book;
    }

    /**
     * Creates window with logo and message. Optional a progress bar (while
     * loading) or a password input field (if encrypted).
     */
    private ProgressBar createContainerWithMessage(Composite parent, String message, boolean showProgressBar,
                    boolean showPasswordField)
    {
        ProgressBar bar = null;

        container = new Composite(parent, SWT.NONE);
        container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        container.setLayout(new FormLayout());

        Label image = new Label(container, SWT.NONE);
        image.setBackground(container.getBackground());
        image.setImage(Images.LOGO_48.image());

        FormData data = new FormData();
        data.top = new FormAttachment(50, -50);
        data.left = new FormAttachment(50, -24);
        image.setLayoutData(data);

        if (showPasswordField)
        {
            Text pwd = createPasswordField(container);

            data = new FormData();
            data.top = new FormAttachment(image, 10);
            data.left = new FormAttachment(image, 0, SWT.CENTER);
            data.width = 100;
            pwd.setLayoutData(data);

            focus = pwd;
        }
        else if (showProgressBar)
        {
            bar = new ProgressBar(container, SWT.SMOOTH);

            data = new FormData();
            data.top = new FormAttachment(image, 10);
            data.left = new FormAttachment(50, -100);
            data.width = 200;
            bar.setLayoutData(data);
        }

        Label label = new Label(container, SWT.CENTER | SWT.WRAP);
        label.setBackground(container.getBackground());
        label.setText(message);

        data = new FormData();
        data.top = new FormAttachment(image, 40);
        data.left = new FormAttachment(50, -100);
        data.width = 200;
        label.setLayoutData(data);

        return bar;
    }

    private Text createPasswordField(Composite container)
    {
        final Text pwd = new Text(container, SWT.PASSWORD | SWT.BORDER);
        pwd.setFocus();
        pwd.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
                final String password = pwd.getText();

                new LoadClientThread(clientInput, broker, new ProgressProvider(clientInput), password.toCharArray())
                                .start();
            }
        });

        return pwd;
    }

    private void rebuildContainer(Consumer<Composite> builder)
    {
        if (container != null && !container.isDisposed())
        {
            Composite parent = container.getParent();
            parent.setRedraw(false);
            try
            {
                container.dispose();
                builder.accept(parent);
                parent.layout(true);
            }
            finally
            {
                parent.setRedraw(true);
            }
        }
    }

    @Override
    public void onLoading(int totalWork, int worked)
    {
        if (this.progressBar == null || this.progressBar.isDisposed())
        {
            rebuildContainer(parent -> { // NOSONAR
                progressBar = createContainerWithMessage(parent, MessageFormat.format(Messages.MsgLoadingFile,
                                PortfolioPart.this.clientInput.getFile().getName()), true, false);
            });
        }

        int max = progressBar.getMaximum();
        if (max != totalWork)
            this.progressBar.setMaximum(totalWork);
        this.progressBar.setSelection(worked);
    }

    @Override
    public void onLoaded()
    {
        this.context.set(Client.class, clientInput.getClient());
        rebuildContainer(this::createContainerWithViews);
    }

    @Override
    public void onError(String message)
    {
        rebuildContainer(parent -> createContainerWithMessage(parent, message, false,
                        ClientFactory.isEncrypted(clientInput.getFile())));
    }

    @Override
    public void onSaved()
    {
        part.getPersistedState().put(UIConstants.PersistedState.FILENAME, clientInput.getFile().getAbsolutePath());
        part.setLabel(clientInput.getLabel());
        part.setTooltip(clientInput.getFile().getAbsolutePath());
    }

    @Override
    public void onDirty(boolean isDirty)
    {
        dirty.setDirty(isDirty);

        if (isDirty)
            onRecalculationNeeded();
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (view != null && view.getControl() != null && !view.getControl().isDisposed())
            view.notifyModelUpdated();
    }

    @Focus
    public void setFocus()
    {
        if (focus != null && !focus.isDisposed())
            focus.setFocus();
    }

    @PreDestroy
    public void destroy()
    {
        this.clientInput.removeListener(this);
        this.clientInput.savePreferences();
    }

    @Persist
    public void save(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        this.clientInput.save(shell);
    }

    public void doSaveAs(Shell shell, String extension, String encryptionMethod)
    {
        this.clientInput.doSaveAs(shell, extension, encryptionMethod);
    }

    public ClientInput getClientInput()
    {
        return clientInput;
    }

    public Client getClient()
    {
        return clientInput.getClient();
    }

    public IPreferenceStore getPreferenceStore()
    {
        return clientInput.getPreferenceStore();
    }

    public LinkedList<ReportingPeriod> getReportingPeriods() // NOSONAR
    {
        return clientInput.getReportingPeriods();
    }

    public ReportingPeriod getSelectedPeriod()
    {
        if (selectedPeriod != null)
            return selectedPeriod;

        try
        {
            String code = part.getPersistedState().get(UIConstants.PersistedState.REPORTING_PERIOD);
            if (code != null)
                selectedPeriod = ReportingPeriod.from(code);
        }
        catch (IOException ignore)
        {
            PortfolioPlugin.log(ignore);
        }

        if (selectedPeriod == null)
            selectedPeriod = clientInput.getReportingPeriods().getFirst();

        return selectedPeriod;
    }

    public void setSelectedPeriod(ReportingPeriod selectedPeriod)
    {
        this.selectedPeriod = Objects.requireNonNull(selectedPeriod);
        part.getPersistedState().put(UIConstants.PersistedState.REPORTING_PERIOD, selectedPeriod.getCode());
    }

    public String getSelectedViewId()
    {
        return part.getPersistedState().get(UIConstants.PersistedState.VIEW);
    }

    public void setSelectedViewId(String viewId)
    {
        part.getPersistedState().put(UIConstants.PersistedState.VIEW, viewId);
    }

    /* package */ void markDirty()
    {
        clientInput.markDirty();
    }

    public void activateView(String target, String id)
    {
        this.activateView(target, id, null);
    }

    @SuppressWarnings("unchecked")
    public void activateView(String target, String id, Object parameter)
    {
        disposeView();

        try
        {
            Class<?> clazz = getClass().getClassLoader()
                            .loadClass("name.abuchen.portfolio.ui.views." + target + "View"); //$NON-NLS-1$ //$NON-NLS-2$
            if (clazz == null)
                return;

            createView((Class<AbstractFinanceView>) clazz, parameter);

            if (id != null)
                setSelectedViewId(id);
        }
        catch (Exception e)
        {
            PortfolioPlugin.log(e);

            createView(ExceptionView.class, e);
        }
    }

    private void createView(Class<? extends AbstractFinanceView> clazz, Object parameter)
    {
        IEclipseContext viewContext = this.context.createChild();
        viewContext.set(Client.class, this.clientInput.getClient());
        viewContext.set(IPreferenceStore.class, this.clientInput.getPreferenceStore());
        viewContext.set(PortfolioPart.class, this);
        viewContext.set(ESelectionService.class, selectionService);
        viewContext.set(ExchangeRateProviderFactory.class, this.clientInput.getExchangeRateProviderFacory());

        view = ContextInjectionFactory.make(clazz, viewContext);
        viewContext.set(AbstractFinanceView.class, view);
        view.setContext(viewContext);
        view.init(this, parameter);
        view.createViewControl(book);

        book.showPage(view.getControl());
        view.setFocus();
    }

    private void disposeView()
    {
        if (view != null)
        {
            AbstractFinanceView toBeDisposed = view;

            view = null;

            toBeDisposed.getContext().dispose();

            if (!toBeDisposed.getControl().isDisposed())
                toBeDisposed.getControl().dispose();
        }
    }

    private <T> T make(Class<T> type, Object... parameters)
    {
        IEclipseContext c2 = EclipseContextFactory.create();
        if (parameters != null)
            for (Object param : parameters)
                c2.set(param.getClass().getName(), param);
        return ContextInjectionFactory.make(type, this.context, c2);
    }
}
