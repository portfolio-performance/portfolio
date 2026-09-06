package name.abuchen.portfolio.ui.preferences;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.rest.ClientStore;
import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.RejectedConnections;
import name.abuchen.portfolio.rest.RestApiConstants;
import name.abuchen.portfolio.rest.RestApiWorkspace;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.ClientInputFactory;
import name.abuchen.portfolio.ui.util.Colors;

/**
 * Configures both local front doors - the MCP server and the REST API - which
 * are one server on one port behind one switch, because the sixteen MCP tools
 * are the REST operations: global enable switch, port, the MCP URL, the
 * per-file opt-in with optional alias, and the authorized clients. Clients are
 * usually added through interactive pairing; "Add client" mints a token
 * manually, for a headless setup or for an MCP client that is handed one
 * (ADR 0005). Revocation takes effect immediately. Only files currently open in
 * the application are listed; unsaved files cannot be enabled because the API
 * identity is keyed by file path.
 */
public class RestApiPreferencePage extends PreferencePage
{
    /**
     * Shows a freshly minted token exactly once, and assembles the connector
     * settings around it.
     * <p/>
     * This is where the MCP snippet belongs, because this is the one moment the
     * token exists: it is stored as a hash and can never be shown again, so a
     * preference page that displayed the URL and the header together would be
     * displaying half of them. The page itself shows only what is not secret.
     */
    private static final class ShowTokenDialog extends Dialog
    {
        private final String token;
        private final int port;

        private ShowTokenDialog(Shell parentShell, String token, int port)
        {
            super(parentShell);
            this.token = token;
            this.port = port;
        }

        @Override
        protected void configureShell(Shell newShell)
        {
            super.configureShell(newShell);
            newShell.setText(Messages.PrefRestApiTitleNewToken);
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            var container = (Composite) super.createDialogArea(parent);
            GridLayoutFactory.swtDefaults().numColumns(3).margins(15, 15).applyTo(container);

            var hint = new Label(container, SWT.WRAP);
            hint.setText(Messages.PrefMsgRestApiTokenShownOnce);
            GridDataFactory.fillDefaults().span(3, 1).grab(true, false).hint(460, SWT.DEFAULT).applyTo(hint);

            addCopyRow(container, Messages.PrefRestApiLabelToken, token);

            var connector = new Label(container, SWT.WRAP);
            connector.setText(Messages.PrefMsgRestApiConnectorSettings);
            GridDataFactory.fillDefaults().span(3, 1).grab(true, false).indent(0, 10).hint(460, SWT.DEFAULT)
                            .applyTo(connector);

            addCopyRow(container, Messages.PrefRestApiLabelMcpUrl, mcpUrl(port));
            // the header value, named by its label: a client's connector dialog
            // asks for the two separately
            addCopyRow(container, Messages.PrefRestApiLabelAuthorizationHeader, "Bearer " + token); //$NON-NLS-1$

            return container;
        }

        private void addCopyRow(Composite container, String label, String value)
        {
            new Label(container, SWT.NONE).setText(label);

            var text = new Text(container, SWT.BORDER | SWT.READ_ONLY);
            text.setText(value);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(text);

            var button = new Button(container, SWT.PUSH);
            button.setText(Messages.LabelCopyToClipboard);
            button.addListener(SWT.Selection, event -> copyToClipboard(getShell(), value));
        }
    }

    private record Row(String path, String label)
    {
    }

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter
                    .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withZone(ZoneId.systemDefault());

    private final ClientInputFactory clientInputFactory;
    private final IEclipsePreferences preferences = RestApiWorkspace.preferences();
    private final FileAccessRegistry registry = RestApiWorkspace.createFileAccessRegistry();
    private final ClientStore clientStore = RestApiWorkspace.getClientStore();

    private Button enableButton;
    private Text portText;
    private CheckboxTableViewer filesViewer;
    private TableViewer clientsViewer;
    private final Map<String, String> aliases = new HashMap<>();

    public RestApiPreferencePage(ClientInputFactory clientInputFactory)
    {
        this.clientInputFactory = clientInputFactory;
        setTitle(Messages.PrefTitleRestApi);
        noDefaultAndApplyButton();
    }

    @Override
    protected Control createContents(Composite parent)
    {
        var container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);

        enableButton = new Button(container, SWT.CHECK);
        enableButton.setText(Messages.PrefLabelRestApiEnable);
        enableButton.setSelection(preferences.getBoolean(RestApiConstants.PREF_ENABLED, false));
        GridDataFactory.fillDefaults().span(2, 1).applyTo(enableButton);

        new Label(container, SWT.NONE).setText(Messages.PrefLabelRestApiPort);
        portText = new Text(container, SWT.BORDER);
        portText.setText(String.valueOf(preferences.getInt(RestApiConstants.PREF_PORT, RestApiConstants.DEFAULT_PORT)));
        GridDataFactory.fillDefaults().hint(80, SWT.DEFAULT).applyTo(portText);

        createMcpUrl(container);
        createLastRejection(container);

        var hasUnsavedFiles = clientInputFactory.listOpenClients().stream().anyMatch(input -> input.getFile() == null);
        if (hasUnsavedFiles)
        {
            var hint = new Label(container, SWT.WRAP);
            hint.setText(Messages.PrefMsgRestApiUnsavedFiles);
            hint.setForeground(Colors.theme().warningForeground());
            GridDataFactory.fillDefaults().span(2, 1).applyTo(hint);
        }

        createFilesTable(container);
        createClientsSection(container);

        return container;
    }

    /**
     * The URL to paste into an MCP client, and nothing secret. It tracks the
     * port field rather than the stored preference, because what the user is
     * about to copy is the address the server will listen on once they press
     * OK.
     */
    private void createMcpUrl(Composite container)
    {
        new Label(container, SWT.NONE).setText(Messages.PrefRestApiLabelMcpUrl);

        var row = new Composite(container, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(row);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(row);

        var urlText = new Text(row, SWT.BORDER | SWT.READ_ONLY);
        urlText.setText(mcpUrl(currentPort()));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(urlText);

        var copyButton = new Button(row, SWT.PUSH);
        copyButton.setText(Messages.LabelCopyToClipboard);
        copyButton.addListener(SWT.Selection, event -> copyToClipboard(getShell(), urlText.getText()));

        portText.addListener(SWT.Modify, event -> urlText.setText(mcpUrl(currentPort())));
    }

    /**
     * A client whose token is wrong never becomes an entry in the list below,
     * and an MCP client reports nothing at all on failure - no error, no retry,
     * no sign-in prompt. Without this, a mistyped token is invisible from both
     * ends and the user has nowhere to look.
     */
    private void createLastRejection(Composite container)
    {
        var rejection = RejectedConnections.last().orElse(null);
        if (rejection == null)
            return;

        var client = rejection.userAgent() != null ? rejection.userAgent()
                        : Messages.PrefRestApiLabelUnidentifiedClient;

        var label = new Label(container, SWT.WRAP);
        label.setText(MessageFormat.format(
                        rejection.tokenPresented() ? Messages.PrefMsgRestApiRejectedInvalidToken
                                        : Messages.PrefMsgRestApiRejectedNoToken,
                        format(rejection.when()), client));
        label.setForeground(Colors.theme().warningForeground());
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(label);
    }

    private int currentPort()
    {
        try
        {
            var port = Integer.parseInt(portText.getText().trim());
            if (port >= 1024 && port <= 65535)
                return port;
        }
        catch (NumberFormatException e) // NOSONAR - an unfinished edit, not an error
        {
            // fall through to what is actually stored
        }
        return preferences.getInt(RestApiConstants.PREF_PORT, RestApiConstants.DEFAULT_PORT);
    }

    private static String mcpUrl(int port)
    {
        return "http://127.0.0.1:" + port + RestApiConstants.MCP_ENDPOINT; //$NON-NLS-1$
    }

    private static void copyToClipboard(Shell shell, String value)
    {
        var clipboard = new Clipboard(shell.getDisplay());
        try
        {
            clipboard.setContents(new Object[] { value }, new Transfer[] { TextTransfer.getInstance() });
        }
        finally
        {
            clipboard.dispose();
        }
    }

    private void createFilesTable(Composite container)
    {
        var rows = new ArrayList<Row>();
        for (var input : clientInputFactory.listOpenClients())
        {
            if (input.getFile() == null)
                continue;
            var path = input.getFile().getAbsolutePath();
            rows.add(new Row(path, input.getLabel()));
            aliases.put(path, registry.byPath(path).map(FileAccessRegistry.FileAccess::alias).orElse(null));
        }

        var tableContainer = new Composite(container, SWT.NONE);
        var layout = new TableColumnLayout();
        tableContainer.setLayout(layout);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, true).hint(SWT.DEFAULT, 150).applyTo(tableContainer);

        filesViewer = CheckboxTableViewer.newCheckList(tableContainer,
                        SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        filesViewer.getTable().setHeaderVisible(true);
        filesViewer.setContentProvider(ArrayContentProvider.getInstance());

        var fileColumn = new TableViewerColumn(filesViewer, SWT.NONE);
        fileColumn.getColumn().setText(Messages.PrefRestApiColumnFile);
        layout.setColumnData(fileColumn.getColumn(), new ColumnWeightData(70));
        fileColumn.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Row) element).path();
            }
        });

        var aliasColumn = new TableViewerColumn(filesViewer, SWT.NONE);
        aliasColumn.getColumn().setText(Messages.PrefRestApiColumnAlias);
        layout.setColumnData(aliasColumn.getColumn(), new ColumnWeightData(30));
        aliasColumn.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                var alias = aliases.get(((Row) element).path());
                return alias != null ? alias : ""; //$NON-NLS-1$
            }
        });
        aliasColumn.setEditingSupport(new EditingSupport(filesViewer)
        {
            @Override
            protected CellEditor getCellEditor(Object element)
            {
                return new TextCellEditor(filesViewer.getTable());
            }

            @Override
            protected boolean canEdit(Object element)
            {
                return true;
            }

            @Override
            protected Object getValue(Object element)
            {
                var alias = aliases.get(((Row) element).path());
                return alias != null ? alias : ""; //$NON-NLS-1$
            }

            @Override
            protected void setValue(Object element, Object value)
            {
                var alias = String.valueOf(value).trim();
                aliases.put(((Row) element).path(), alias.isEmpty() ? null : alias);
                filesViewer.refresh(element);
            }
        });

        filesViewer.setInput(rows);

        for (Row row : rows)
        {
            var enabled = registry.byPath(row.path()).map(FileAccessRegistry.FileAccess::enabled).orElse(false);
            filesViewer.setChecked(row, enabled);
        }
    }

    private void createClientsSection(Composite container)
    {
        var label = new Label(container, SWT.NONE);
        label.setText(Messages.PrefRestApiLabelClients);
        GridDataFactory.fillDefaults().span(2, 1).indent(0, 10).applyTo(label);

        var tableContainer = new Composite(container, SWT.NONE);
        var layout = new TableColumnLayout();
        tableContainer.setLayout(layout);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).hint(SWT.DEFAULT, 120).applyTo(tableContainer);

        clientsViewer = new TableViewer(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        clientsViewer.getTable().setHeaderVisible(true);
        clientsViewer.setContentProvider(ArrayContentProvider.getInstance());

        var nameColumn = new TableViewerColumn(clientsViewer, SWT.NONE);
        nameColumn.getColumn().setText(Messages.PrefRestApiColumnClient);
        layout.setColumnData(nameColumn.getColumn(), new ColumnWeightData(50));
        nameColumn.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                var client = (ClientStore.ApiClient) element;
                return client.session() ? client.name() + " (" + Messages.PrefRestApiLabelSession + ")" //$NON-NLS-1$ //$NON-NLS-2$
                                : client.name();
            }
        });

        var createdColumn = new TableViewerColumn(clientsViewer, SWT.NONE);
        createdColumn.getColumn().setText(Messages.PrefRestApiColumnCreated);
        layout.setColumnData(createdColumn.getColumn(), new ColumnWeightData(25));
        createdColumn.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return format(((ClientStore.ApiClient) element).created());
            }
        });

        var lastUsedColumn = new TableViewerColumn(clientsViewer, SWT.NONE);
        lastUsedColumn.getColumn().setText(Messages.PrefRestApiColumnLastUsed);
        layout.setColumnData(lastUsedColumn.getColumn(), new ColumnWeightData(25));
        lastUsedColumn.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return format(((ClientStore.ApiClient) element).lastUsed());
            }
        });

        clientsViewer.setInput(clientStore.listClients());

        var buttons = new Composite(container, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(buttons);
        GridDataFactory.fillDefaults().span(2, 1).applyTo(buttons);

        var revokeButton = new Button(buttons, SWT.PUSH);
        revokeButton.setText(Messages.PrefRestApiBtnRevoke);
        revokeButton.setEnabled(false);
        revokeButton.addListener(SWT.Selection, event -> {
            var selected = (ClientStore.ApiClient) ((IStructuredSelection) clientsViewer.getSelection())
                            .getFirstElement();
            if (selected != null)
            {
                // takes effect immediately; a misclick is recoverable (re-pair)
                clientStore.revoke(selected.id());
                clientsViewer.setInput(clientStore.listClients());
                revokeButton.setEnabled(false);
            }
        });

        var addButton = new Button(buttons, SWT.PUSH);
        addButton.setText(Messages.PrefRestApiBtnAddClient);
        addButton.addListener(SWT.Selection, event -> addClient());

        clientsViewer.addSelectionChangedListener(
                        event -> revokeButton.setEnabled(!event.getSelection().isEmpty()));
    }

    /** the manual path for headless clients: mint a token and show it once */
    private void addClient()
    {
        // validate against the same sanitized name the store will actually keep
        var dialog = new InputDialog(getShell(), Messages.PrefTitleRestApi, Messages.PrefMsgRestApiEnterClientName,
                        "", value -> { //$NON-NLS-1$
                            var clean = ClientStore.sanitizeName(value);
                            return clean.isEmpty() || clean.length() > ClientStore.MAX_NAME_LENGTH ? "" : null; //$NON-NLS-1$
                        });
        if (dialog.open() != Window.OK)
            return;

        var token = clientStore.addPersistentClient(dialog.getValue());
        new ShowTokenDialog(getShell(), token, currentPort()).open();
        clientsViewer.setInput(clientStore.listClients());
    }

    private static String format(Instant instant)
    {
        return instant == null ? "-" : DATE_TIME.format(instant); //$NON-NLS-1$
    }

    @Override
    public boolean performOk()
    {
        int port;
        try
        {
            port = Integer.parseInt(portText.getText().trim());
        }
        catch (NumberFormatException e)
        {
            port = -1;
        }
        if (port < 1024 || port > 65535)
        {
            setErrorMessage(Messages.PrefMsgRestApiInvalidPort);
            return false;
        }

        var rows = ((List<?>) filesViewer.getInput()).stream().map(Row.class::cast).toList();

        try
        {
            // nothing is stored until every alias is known to be good: an
            // invalid one in the last row must not leave the earlier rows
            // applied and the dialog still open
            registry.validateAliases(aliases);
        }
        catch (IllegalArgumentException e)
        {
            setErrorMessage(e.getMessage());
            return false;
        }

        // clearing first lets one file hand its alias to another within the
        // same edit - setAlias checks against what is stored, and would
        // otherwise see the name as taken by the file that is giving it up
        for (Row row : rows)
            registry.setAlias(row.path(), null);

        for (Row row : rows)
        {
            registry.setEnabled(row.path(), filesViewer.getChecked(row));
            registry.setAlias(row.path(), aliases.get(row.path()));
        }

        preferences.putBoolean(RestApiConstants.PREF_ENABLED, enableButton.getSelection());
        preferences.putInt(RestApiConstants.PREF_PORT, port);
        try
        {
            preferences.flush();
        }
        catch (BackingStoreException e)
        {
            PortfolioLog.error(e);
        }

        return true;
    }
}
