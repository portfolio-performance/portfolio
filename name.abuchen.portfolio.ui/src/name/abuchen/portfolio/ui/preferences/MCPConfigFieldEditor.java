package name.abuchen.portfolio.ui.preferences;

import java.util.function.IntSupplier;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.ui.Messages;

class MCPConfigFieldEditor extends FieldEditor
{
    private static final String HOST = "127.0.0.1"; //$NON-NLS-1$

    private final IntSupplier portSupplier;
    private Text configText;
    private Composite controlArea;

    MCPConfigFieldEditor(String label, Composite parent, IntSupplier portSupplier)
    {
        this.portSupplier = portSupplier;
        setLabelText(label);
        createControl(parent);
    }

    void refresh()
    {
        if (configText != null && !configText.isDisposed())
            configText.setText(buildMcpConfigJson(portSupplier.getAsInt()));
    }

    @Override
    protected void adjustForNumColumns(int numColumns)
    {
        ((GridData) controlArea.getLayoutData()).horizontalSpan = numColumns - 1;
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns)
    {
        getLabelControl(parent);

        controlArea = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).span(numColumns - 1, 1).applyTo(controlArea);
        GridLayoutFactory.fillDefaults().margins(0, 0).applyTo(controlArea);

        configText = new Text(controlArea, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL);
        GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 120).applyTo(configText);
        refresh();

        var copyButton = new Button(controlArea, SWT.PUSH);
        copyButton.setText(Messages.LabelCopyToClipboard);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(copyButton);
        copyButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            var clipboard = new Clipboard(Display.getCurrent());
            clipboard.setContents(new Object[] { configText.getText() }, new Transfer[] { TextTransfer.getInstance() });
            clipboard.dispose();
        }));
    }

    @Override
    protected void doLoad()
    {
        refresh();
    }

    @Override
    protected void doLoadDefault()
    {
        refresh();
    }

    @Override
    protected void doStore()
    {
        // read-only display
    }

    @Override
    public int getNumberOfControls()
    {
        return 2;
    }

    static String buildMcpConfigJson(int port)
    {
        var url = "http://" + HOST + ":" + port + "/mcp"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        var server = new JsonObject();
        server.addProperty("url", url); //$NON-NLS-1$

        var servers = new JsonObject();
        servers.add("portfolio-performance", server); //$NON-NLS-1$

        var root = new JsonObject();
        root.add("mcpServers", servers); //$NON-NLS-1$

        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }
}
