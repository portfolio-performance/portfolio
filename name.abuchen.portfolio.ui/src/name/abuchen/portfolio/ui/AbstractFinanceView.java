package name.abuchen.portfolio.ui;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Client;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;

public abstract class AbstractFinanceView
{
    private ClientEditor clientEditor;

    private Composite top;
    private LocalResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources());
    private List<Menu> contextMenus = new ArrayList<Menu>();

    protected abstract String getTitle();

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
        GridLayoutFactory.fillDefaults().spacing(1, 1).applyTo(top);

        Control header = createHeader(top);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(header);

        Control body = createBody(top);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(body);
    }

    protected abstract Control createBody(Composite parent);

    private Control createHeader(Composite parent)
    {
        Composite header = new Composite(parent, SWT.NONE);

        header.setLayout(new RowLayout(SWT.VERTICAL));

        Font boldFont = resourceManager.createFont(FontDescriptor.createFrom(
                        JFaceResources.getFont(JFaceResources.DIALOG_FONT)).setStyle(SWT.BOLD));

        Label title = new Label(header, SWT.NONE);
        title.setText(getTitle());
        title.setFont(boldFont);

        return header;
    }

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
