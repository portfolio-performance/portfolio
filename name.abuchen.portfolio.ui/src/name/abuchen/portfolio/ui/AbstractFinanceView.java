package name.abuchen.portfolio.ui;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.util.Colors;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;

public abstract class AbstractFinanceView
{
    private ClientEditor clientEditor;

    private Composite top;
    private Label title;
    private LocalResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources());
    private List<Menu> contextMenus = new ArrayList<Menu>();

    protected abstract String getTitle();

    protected final void updateTitle()
    {
        this.title.setText(getTitle());
    }

    /** called when some other view modifies the model */
    public void notifyModelUpdated()
    {}

    public void init(ClientEditor clientEditor, Object parameter)
    {
        this.clientEditor = clientEditor;
    }

    public ClientEditor getClientEditor()
    {
        return clientEditor;
    }

    public Client getClient()
    {
        return clientEditor.getClient();
    }

    public void markDirty()
    {
        clientEditor.markDirty();
    }

    public final void createViewControl(Composite parent)
    {
        top = new Composite(parent, SWT.NONE);
        // on windows, add a spacing line as tables
        // have white top and need a border
        int spacing = Platform.OS_WIN32.equals(Platform.getOS()) ? 1 : 0;
        GridLayoutFactory.fillDefaults().spacing(spacing, spacing).applyTo(top);

        Control header = createHeader(top);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(header);

        Control body = createBody(top);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(body);

        top.addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                dispose();
            }
        });
    }

    protected abstract Control createBody(Composite parent);

    private Control createHeader(Composite parent)
    {
        Composite header = new Composite(parent, SWT.NONE);
        header.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        Font boldFont = resourceManager.createFont(FontDescriptor.createFrom(
                        JFaceResources.getFont(JFaceResources.HEADER_FONT)).setStyle(SWT.BOLD));

        title = new Label(header, SWT.NONE);
        title.setText(getTitle());
        title.setFont(boldFont);
        title.setForeground(resourceManager.createColor(Colors.HEADINGS.swt()));
        title.setBackground(header.getBackground());

        ToolBar toolBar = new ToolBar(header, SWT.FLAT | SWT.RIGHT);
        toolBar.setBackground(header.getBackground());
        addButtons(toolBar);

        // layout
        GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(header);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);
        GridDataFactory.fillDefaults().applyTo(toolBar);

        return header;
    }

    protected void addButtons(ToolBar toolBar)
    {}

    protected final void hookContextMenu(Control control, IMenuListener listener)
    {
        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(listener);

        Menu contextMenu = menuMgr.createContextMenu(control);
        control.setMenu(contextMenu);

        contextMenus.add(contextMenu);
    }

    public void dispose()
    {
        for (Menu contextMenu : contextMenus)
            if (!contextMenu.isDisposed())
                contextMenu.dispose();

        resourceManager.dispose();
    }

    public final Control getControl()
    {
        return top;
    }

}
