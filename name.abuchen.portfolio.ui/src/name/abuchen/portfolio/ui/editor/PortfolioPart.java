package name.abuchen.portfolio.ui.editor;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.di.UIEventTopic;
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
import name.abuchen.portfolio.ui.editor.ClientInput.ClientInputListener;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.swt.SashLayout;
import name.abuchen.portfolio.ui.util.swt.SashLayoutData;
import name.abuchen.portfolio.ui.views.ExceptionView;

public class PortfolioPart implements ClientInputListener
{
    // compatibility: the value used to be stored in the AbstractHistoricView
    private static final String REPORTING_PERIODS_KEY = "AbstractHistoricView"; //$NON-NLS-1$

    private ClientInput clientInput;
    private ExchangeRateProviderFactory exchangeRateProviderFacory;

    private Composite container;
    private ProgressBar progressBar;
    private PageBook book;
    private AbstractFinanceView view;

    private Control focus;

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
    public void createComposite(Composite parent, MPart part)
    {
        // is client available? (e.g. via new file wizard)
        clientInput = (ClientInput) part.getTransientData().get(ClientInput.class.getName());

        if (clientInput == null)
        {
            // is file name available? (e.g. load file, open on startup)
            String filename = part.getPersistedState().get(UIConstants.File.PERSISTED_STATE_KEY);
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
            part.getPersistedState().put(UIConstants.File.PERSISTED_STATE_KEY, clientInput.getFile().getAbsolutePath());

        clientInput.addListener(this);

        if (clientInput.getClient() != null)
        {
            setupClient(clientInput.getClient());
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
        int size = getPreferenceStore().getInt(sashIdentifier);
        navigationBar.setLayoutData(new SashLayoutData(size != 0 ? size : 180));
        sash.addDisposeListener(e -> getPreferenceStore().setValue(sashIdentifier,
                        ((SashLayoutData) navigationBar.getLayoutData()).getSize()));

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

    private void setupClient(Client client)
    {
        this.context.set(Client.class, client);

        // build factory for exchange rates once client is available
        this.exchangeRateProviderFacory = make(ExchangeRateProviderFactory.class);
        this.context.set(ExchangeRateProviderFactory.class, this.exchangeRateProviderFacory);
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
        setupClient(clientInput.getClient());
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

    }

    @Override
    public void onDirty(boolean isDirty)
    {
        dirty.setDirty(isDirty);

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
    }

    @Persist
    public void save(MPart part, @Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        this.clientInput.save();
        // FIXME
    }

    public void doSaveAs(MPart part, Shell shell, String extension, String encryptionMethod) // NOSONAR
    {
        // FIXME
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

    /* package */ void markDirty()
    {
        clientInput.markDirty();
    }

    @Inject
    @Optional
    public void onExchangeRatesLoaded(@UIEventTopic(UIConstants.Event.ExchangeRates.LOADED) Object obj)
    {
        if (exchangeRateProviderFacory != null)
            exchangeRateProviderFacory.clearCache();

        // update view w/o marking the model dirty
        if (view != null && view.getControl() != null && !view.getControl().isDisposed())
            view.notifyModelUpdated();
    }

    @SuppressWarnings("unchecked")
    public void activateView(String target, Object parameter)
    {
        disposeView();

        try
        {
            Class<?> clazz = getClass().getClassLoader()
                            .loadClass("name.abuchen.portfolio.ui.views." + target + "View"); //$NON-NLS-1$ //$NON-NLS-2$
            if (clazz == null)
                return;

            createView((Class<AbstractFinanceView>) clazz, parameter);
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
        viewContext.set(ExchangeRateProviderFactory.class, this.exchangeRateProviderFacory);

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
            view.getContext().dispose();

            if (!view.getControl().isDisposed())
                view.getControl().dispose();
            view = null;
        }
    }

    // //////////////////////////////////////////////////////////////
    // preference store functions
    // //////////////////////////////////////////////////////////////

    public LinkedList<ReportingPeriod> loadReportingPeriods() // NOSONAR
    {
        LinkedList<ReportingPeriod> answer = new LinkedList<>();

        String config = getPreferenceStore().getString(REPORTING_PERIODS_KEY);
        if (config != null && config.trim().length() > 0)
        {
            String[] codes = config.split(";"); //$NON-NLS-1$
            for (String c : codes)
            {
                try
                {
                    answer.add(ReportingPeriod.from(c));
                }
                catch (IOException | RuntimeException ignore)
                {
                    PortfolioPlugin.log(ignore);
                }
            }
        }

        if (answer.isEmpty())
        {
            for (int ii = 1; ii <= 5; ii++)
                answer.add(new ReportingPeriod.LastX(ii, 0));
        }

        return answer;
    }

    public void storeReportingPeriods(List<ReportingPeriod> periods)
    {
        StringBuilder buf = new StringBuilder();
        for (ReportingPeriod p : periods)
        {
            p.writeTo(buf);
            buf.append(';');
        }

        getPreferenceStore().setValue(REPORTING_PERIODS_KEY, buf.toString());
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
