package name.abuchen.portfolio.ui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.swt.SashLayout;
import name.abuchen.portfolio.ui.util.swt.SashLayoutData;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.util.TextUtil;

public abstract class AbstractFinanceView
{
    private final String identifier = getClass().getSimpleName() + "-newsash"; //$NON-NLS-1$

    @Inject
    private IEclipseContext context;

    @Inject
    private PortfolioPart part;

    private Composite top;
    private InformationPane pane;

    /**
     * The unescaped text of the title label.
     */
    private String titleText;
    private Label title;

    /**
     * Tool bar used for switching between different views. It displays an
     * abridged tool bar if the spaces does not allow to display all views.
     */
    private ToolBarManager viewToolBar;

    /**
     * Tool bar used for actions (new, columns, configuration).
     */
    private ToolBarManager actionToolBar;

    private LocalResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources());
    private List<Menu> contextMenus = new ArrayList<>();

    protected abstract String getDefaultTitle();
    
    SashLayout sashLayout;
    
    protected String getTitle()
    {
        return titleText;
    }

    protected final void updateTitle(String title)
    {
        if (!this.title.isDisposed())
        {
            String escaped = TextUtil.tooltip(title);
            boolean isEqual = escaped.equals(this.title.getText());

            this.titleText = title;
            this.title.setText(escaped);
            if (!isEqual)
                this.title.getParent().layout(true);
        }
    }

    public final void onRecalculationNeeded()
    {
        notifyModelUpdated();

        if (pane != null && pane.getControl() != null && !pane.getControl().isDisposed())
            pane.onRecalculationNeeded();
    }

    /** called when some other view modifies the model */
    public void notifyModelUpdated()
    {
    }

    /** called after the views has been fully created */
    protected void notifyViewCreationCompleted()
    {
    }

    public PortfolioPart getPart()
    {
        return part;
    }

    public IPreferenceStore getPreferenceStore()
    {
        return part.getPreferenceStore();
    }

    public Client getClient()
    {
        return part.getClient();
    }

    public void markDirty()
    {
        part.markDirty();
    }

    public void setInformationPaneInput(Object input)
    {
        if (pane != null)
            pane.setInput(input);
    }

    public Shell getActiveShell()
    {
        return Display.getDefault().getActiveShell();
    }

    public final void createViewControl(Composite parent, boolean hideInformationPane)
    {
        top = new Composite(parent, SWT.NONE);
        // on windows, add a spacing line as tables
        // have white top and need a border
        int spacing = Platform.OS_WIN32.equals(Platform.getOS()) ? 1 : 0;
        GridLayoutFactory.fillDefaults().spacing(spacing, spacing).applyTo(top);

        Control header = createHeader(top);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(header);

        Composite sash = new Composite(top, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(sash);

        SashLayout sl = new SashLayout(sash, SWT.VERTICAL | SWT.END);
        sl.setTag(UIConstants.Tag.INFORMATIONPANE);
        sash.setLayout(sl);

        createBody(sash);
        sashLayout = sl;

        pane = make(InformationPane.class);
        pane.createViewControl(sash);
        pane.setView(this);
        pane.setLayoutData(new SashLayoutData(-200));

        int size = getPreferenceStore().getInt(identifier);
        if (size == 0)
            size = hideInformationPane ? -200 : 200;
        pane.setLayoutData(new SashLayoutData(size));
        sash.addDisposeListener(e -> getPreferenceStore().setValue(identifier,
                        ((SashLayoutData) pane.getLayoutData()).getSize()));

        top.addDisposeListener(e -> dispose());

        // delay updating the tool bar as late as possible because otherwise we
        // see the tool bars flicker on Windows

        viewToolBar.update(false);
        actionToolBar.update(false);

        notifyViewCreationCompleted();
    }
    
    public void flipPane()
    {
        if (sashLayout != null)
            sashLayout.flip();
    }
    
    public boolean isPaneHidden()
    {
        if (sashLayout == null)
            return false;
            
        return sashLayout.isHidden();
        
    }

    protected abstract Control createBody(Composite parent);

    private final Control createHeader(Composite parent)
    {
        Composite header = new Composite(parent, SWT.NONE);
        header.setBackground(Colors.WHITE);

        titleText = getDefaultTitle();
        title = new Label(header, SWT.NONE);
        title.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING1);
        title.setText(TextUtil.tooltip(titleText));
        title.setForeground(Colors.SIDEBAR_TEXT);
        title.setBackground(header.getBackground());

        Composite wrapper = new Composite(header, SWT.NONE);
        wrapper.setBackground(header.getBackground());

        viewToolBar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
        ToolBar tb1 = viewToolBar.createControl(wrapper);
        tb1.setBackground(header.getBackground());
        // add buttons only after (!) creation of tool bar to avoid flickering
        addViewButtons(viewToolBar);

        // create layout *after* the toolbar to keep the tab order right
        wrapper.setLayout(new ToolBarPlusChevronLayout(wrapper, SWT.RIGHT));

        actionToolBar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
        ToolBar tb2 = actionToolBar.createControl(header);
        tb2.setBackground(header.getBackground());
        // add buttons only after (!) creation of tool bar to avoid flickering
        addButtons(actionToolBar);

        // layout
        GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5).applyTo(header);
        GridDataFactory.fillDefaults().applyTo(title);
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.CENTER).applyTo(wrapper);
        GridDataFactory.fillDefaults().applyTo(tb2);

        return header;
    }

    protected void addViewButtons(ToolBarManager toolBarManager)
    {
    }

    protected void addButtons(ToolBarManager toolBarManager)
    {
    }

    protected ToolBarManager getViewToolBarManager()
    {
        return this.viewToolBar;
    }

    protected ToolBarManager getToolBarManager()
    {
        return this.actionToolBar;
    }

    protected void addPanePages(List<InformationPanePage> pages)
    {
    }

    /**
     * Returns a given InformationPane page but only if the view control already
     * has been instantiated, i.e. the page actually has been shown previously.
     */
    protected <P extends InformationPanePage> Optional<P> lookup(Class<P> type)
    {
        return pane.lookup(type);
    }

    protected final void hookContextMenu(Control control, IMenuListener listener)
    {
        doCreateContextMenu(control, true, listener);
    }

    protected final Menu createContextMenu(Control control, IMenuListener listener)
    {
        return doCreateContextMenu(control, false, listener);
    }

    private final Menu doCreateContextMenu(Control control, boolean hook, IMenuListener listener)
    {
        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(listener);

        Menu contextMenu = menuMgr.createContextMenu(control);
        if (hook)
        {
            control.setData(ContextMenu.DEFAULT_MENU, contextMenu);
            control.setMenu(contextMenu);
        }

        contextMenus.add(contextMenu);

        return contextMenu;
    }

    public void dispose()
    {
        viewToolBar.dispose();
        actionToolBar.dispose();

        for (Menu contextMenu : contextMenus)
            if (!contextMenu.isDisposed())
                contextMenu.dispose();

        resourceManager.dispose();

        context.dispose();
    }

    public final Control getControl()
    {
        return top;
    }

    public void setFocus()
    {
        getControl().setFocus();
    }

    public final <T> T make(Class<T> type, Object... parameters)
    {
        if (parameters == null || parameters.length == 0)
            return ContextInjectionFactory.make(type, this.context);

        IEclipseContext c2 = EclipseContextFactory.create();
        for (Object param : parameters)
            c2.set(param.getClass().getName(), param);
        return ContextInjectionFactory.make(type, this.context, c2);
    }

    public final void inject(Object object)
    {
        ContextInjectionFactory.inject(object, context);
    }

    public final IEclipseContext getContext()
    {
        return context;
    }

    public <T> T getFromContext(Class<T> clazz)
    {
        return context.get(clazz);
    }

    public void setToContext(String key, Object value)
    {
        context.set(key, value);
    }

}
