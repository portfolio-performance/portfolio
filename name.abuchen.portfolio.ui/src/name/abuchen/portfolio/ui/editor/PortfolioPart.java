package name.abuchen.portfolio.ui.editor;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.Persist;
import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.SaveFlag;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.Navigation.Item;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.swt.SashLayout;
import name.abuchen.portfolio.ui.util.swt.SashLayoutData;
import name.abuchen.portfolio.ui.views.ExceptionView;

public class PortfolioPart implements ClientInputListener
{
    private ClientInput clientInput;
    private ReportingPeriod selectedPeriod;
    private Navigation.Item selectedItem;

    private Composite container;
    private ProgressBar progressBar;
    private PageBook book;
    private ClientEditorSidebar sidebar;
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
    ClientInputFactory clientInputFactory;

    @Inject
    private IStylingEngine stylingEngine;

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
            throw new IllegalArgumentException("missing client info"); //$NON-NLS-1$

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

        clientInputFactory.incrementEditorCount(clientInput);
    }

    private void createContainerWithViews(Composite parent)
    {
        container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());

        Composite sash = new Composite(container, SWT.NONE);
        SashLayout sashLayout = new SashLayout(sash, SWT.HORIZONTAL | SWT.BEGINNING);
        sashLayout.setTag(UIConstants.Tag.SIDEBAR);
        sash.setLayout(sashLayout);

        Composite navigationBar = new Composite(sash, SWT.NONE);
        navigationBar.setData(UIConstants.CSS.CLASS_NAME, "sidebar"); //$NON-NLS-1$
        GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).margins(0, 0).applyTo(navigationBar);

        this.sidebar = new ClientEditorSidebar(this);
        Control control = sidebar.createSidebarControl(navigationBar);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(control);

        sashLayout.addQuickNavigation(
                        menuManager -> addToNavigationMenu(menuManager, 0, clientInput.getNavigation().getRoots()));

        Composite divider = new Composite(navigationBar, SWT.NONE);
        divider.setData(UIConstants.CSS.CLASS_NAME, "sidebarBorder"); //$NON-NLS-1$
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

        // open configured initial view, previously selected view, or default
        // view

        Optional<Navigation.Item> item = clientInput.getNavigation()
                        .findByIdentifier(part.getPersistedState().get(UIConstants.PersistedState.INITIAL_VIEW));

        if (!item.isPresent())
            item = clientInput.getNavigation()
                            .findByIdentifier(part.getPersistedState().get(UIConstants.PersistedState.VIEW));

        if (!item.isPresent())
            item = clientInput.getNavigation().findAll(Navigation.Tag.DEFAULT_VIEW).findAny();

        item.ifPresent(this::activateView);

        focus = book;
    }

    private void addToNavigationMenu(IMenuManager menuManager, int depth, Stream<Item> items)
    {
        items.forEach(item -> {

            if (item.getViewClass() == null)
            {
                MenuManager subMenu = new MenuManager(item.getLabel());
                menuManager.add(subMenu);

                addToNavigationMenu(subMenu, depth + 1, item.getChildren());
            }
            else
            {
                String label = depth > 1 ? "- " + item.getLabel() : item.getLabel(); //$NON-NLS-1$
                SimpleAction menuAction = new SimpleAction(label, a -> activateView(item));
                if (item.getImage() != null)
                    menuAction.setImageDescriptor(item.getImage().descriptor());
                menuManager.add(menuAction);

                addToNavigationMenu(menuManager, depth + 1, item.getChildren());
            }
        });
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
        container.setBackground(Colors.WHITE);
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
        if (message != null)
            label.setText(message);

        data = new FormData();
        data.top = new FormAttachment(image, 40);
        data.left = new FormAttachment(0, 10);
        data.right = new FormAttachment(100, -10);
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
    }

    @Override
    public void onRecalculationNeeded()
    {
        if (view != null && view.getControl() != null && !view.getControl().isDisposed())
            view.onRecalculationNeeded();
    }

    @Inject
    public void setQuotePrecision(
                    @Preference(value = UIConstants.Preferences.FORMAT_CALCULATED_QUOTE_DIGITS) int quotePrecision)
    {
        onRecalculationNeeded();
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
        Navigation navigation = getClientInput().getNavigation();
        if (navigation != null)
            part.getPersistedState().put(UIConstants.PersistedState.VIEW,
                            selectedItem != null ? navigation.getIdentifier(selectedItem) : ""); //$NON-NLS-1$

        this.clientInput.removeListener(this);
        this.clientInput.savePreferences();

        clientInputFactory.decrementEditorCount(clientInput);

        this.clientInput = null;
    }

    @Persist
    public void save(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        this.clientInput.save(shell);
    }

    public void doSaveAs(Shell shell, String extension, Set<SaveFlag> flags)
    {
        this.clientInput.doSaveAs(shell, extension, flags);
    }

    public void doExportAs(Shell shell, String extension, Set<SaveFlag> flags)
    {
        this.clientInput.doExportAs(shell, extension, flags);
    }

    public ClientInput getClientInput()
    {
        return clientInput;
    }

    public Client getClient()
    {
        return clientInput.getClient();
    }

    /**
     * Returns the preferences store per data file.
     */
    public IPreferenceStore getPreferenceStore()
    {
        return clientInput.getPreferenceStore();
    }

    /**
     * Returns the eclipse preferences which exist per installation.
     */
    public IEclipsePreferences getEclipsePreferences()
    {
        return clientInput.getEclipsePreferences();
    }

    public ReportingPeriods getReportingPeriods()
    {
        return clientInput.getReportingPeriods();
    }

    public Optional<AbstractFinanceView> getCurrentView()
    {
        return Optional.ofNullable(view);
    }

    public Optional<Navigation.Item> getSelectedItem()
    {
        return Optional.ofNullable(selectedItem);
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
        {
            selectedPeriod = clientInput.getReportingPeriods().stream().findFirst()
                            .orElseGet(() -> new ReportingPeriod.LastX(1, 0));
        }

        return selectedPeriod;
    }

    public void setSelectedPeriod(ReportingPeriod selectedPeriod)
    {
        this.selectedPeriod = Objects.requireNonNull(selectedPeriod);
        part.getPersistedState().put(UIConstants.PersistedState.REPORTING_PERIOD, selectedPeriod.getCode());
    }

    /* package */ void markDirty()
    {
        if (clientInput != null)
            clientInput.markDirty();
    }

    public void activateView(Class<? extends AbstractFinanceView> view, Object parameter)
    {
        getClientInput().getNavigation().findAll(i -> view.equals(i.getViewClass())).findAny()
                        .ifPresent(item -> activateView(item, parameter));
    }

    public void activateView(Navigation.Item item)
    {
        activateView(item, item.getParameter());
    }

    public void activateView(Navigation.Item item, Object parameter)
    {
        if (item.getViewClass() == null)
            return;

        disposeView();

        try
        {
            createView(item.getViewClass(), parameter, item.hideInformationPane());

            this.selectedItem = item;

            this.sidebar.select(item);
        }
        catch (Exception e)
        {
            PortfolioPlugin.log(e);
            createView(ExceptionView.class, e, true);
        }
    }

    private void createView(Class<? extends AbstractFinanceView> clazz, Object parameter, boolean hideInformationPane)
    {
        IEclipseContext viewContext = this.context.createChild(clazz.getName());
        viewContext.set(Client.class, this.clientInput.getClient());
        viewContext.set(IPreferenceStore.class, this.clientInput.getPreferenceStore());
        viewContext.set(PortfolioPart.class, this);
        viewContext.set(ExchangeRateProviderFactory.class, this.clientInput.getExchangeRateProviderFacory());
        viewContext.set(PartPersistedState.class, new PartPersistedState(part.getPersistedState()));

        ContextFunction lookup = new ContextFunction()
        {
            @Override
            public Object compute(IEclipseContext context, String contextKey)
            {
                Object filteredClient = context.get(UIConstants.Context.FILTERED_CLIENT);
                if (filteredClient != null)
                    return filteredClient;
                else
                    return context.get(Client.class);
            }
        };
        viewContext.set(UIConstants.Context.ACTIVE_CLIENT, lookup);

        if (parameter != null)
            viewContext.set(UIConstants.Parameter.VIEW_PARAMETER, parameter);

        // assign to 'view' only *after* creating the view control to avoid
        // dirty listeners to trigger the view while it is under construction
        AbstractFinanceView underConstruction = ContextInjectionFactory.make(clazz, viewContext);
        viewContext.set(AbstractFinanceView.class, underConstruction);

        underConstruction.createViewControl(book, hideInformationPane);

        // explicitly style control after creation because on Windows the styles
        // are not always applied immediately
        stylingEngine.style(underConstruction.getControl());

        view = underConstruction;
        book.showPage(view.getControl());
        view.setFocus();
    }

    private void disposeView()
    {
        if (view != null)
        {
            AbstractFinanceView toBeDisposed = view;

            // null view first to avoid dirty listener to notify view
            view = null;

            if (!toBeDisposed.getControl().isDisposed())
                toBeDisposed.getControl().dispose();
        }
    }

    public <T> T make(Class<T> type, Object... parameters)
    {
        IEclipseContext c2 = EclipseContextFactory.create();
        if (parameters != null)
            for (Object param : parameters)
                c2.set(param.getClass().getName(), param);
        return ContextInjectionFactory.make(type, this.context, c2);
    }

    public void inject(Object object)
    {
        ContextInjectionFactory.inject(object, context);
    }
}
