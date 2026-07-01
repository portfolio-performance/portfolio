package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;

public class MCPPreferencePage extends FieldEditorPreferencePage
{
    public static final int DEFAULT_MCP_PORT = 8090;

    private IntegerFieldEditor portEditor;
    private MCPConfigFieldEditor configEditor;

    public MCPPreferencePage()
    {
        super(GRID);

        setTitle(Messages.PrefTitleMCP);
        setDescription(Messages.PrefDescriptionMCP);
    }

    @Override
    protected void createFieldEditors()
    {
        addField(new BooleanFieldEditor(UIConstants.Preferences.MCP_ENABLED, //
                        Messages.PrefLabelMCPEnabled, getFieldEditorParent()));

        portEditor = new IntegerFieldEditor(UIConstants.Preferences.MCP_PORT, //
                        Messages.PrefLabelMCPPort, getFieldEditorParent());
        portEditor.setValidRange(1, 65535);
        addField(portEditor);

        addField(new BooleanFieldEditor(UIConstants.Preferences.MCP_AUTOSAVE, //
                        Messages.PrefLabelMCPAutosave, getFieldEditorParent()));

        configEditor = new MCPConfigFieldEditor(Messages.PrefLabelMCPConnectionConfig, getFieldEditorParent(),
                        this::readPort);
        addField(configEditor);
    }

    @Override
    protected void initialize()
    {
        ensureDefaultPort();
        super.initialize();
        configEditor.refresh();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        super.propertyChange(event);

        if (configEditor != null && FieldEditor.VALUE.equals(event.getProperty())
                        && portEditor.equals(event.getSource()))
            configEditor.refresh();
    }

    private void ensureDefaultPort()
    {
        IPreferenceStore store = getPreferenceStore();
        if (store.getInt(UIConstants.Preferences.MCP_PORT) <= 0)
            store.setValue(UIConstants.Preferences.MCP_PORT, DEFAULT_MCP_PORT);
    }

    private int readPort()
    {
        var textControl = portEditor.getTextControl(getFieldEditorParent());
        if (textControl != null && !textControl.isDisposed())
        {
            var text = textControl.getText().trim();
            if (!text.isEmpty())
            {
                try
                {
                    int port = Integer.parseInt(text);
                    if (port >= 1 && port <= 65535)
                        return port;
                }
                catch (NumberFormatException e)
                {
                    // fall through to preference store
                }
            }
        }

        int port = getPreferenceStore().getInt(UIConstants.Preferences.MCP_PORT);
        return port > 0 ? port : DEFAULT_MCP_PORT;
    }
}
