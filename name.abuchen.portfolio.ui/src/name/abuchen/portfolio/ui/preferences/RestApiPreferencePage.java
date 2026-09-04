package name.abuchen.portfolio.ui.preferences;

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
import name.abuchen.portfolio.rest.RestApiConstants;
import name.abuchen.portfolio.rest.RestApiWorkspace;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.ClientInputFactory;
import name.abuchen.portfolio.ui.util.Colors;

/**
 * Configures the local REST API: global enable switch, port, the per-file
 * opt-in with optional alias, and the authorized clients. Clients are usually
 * added through interactive pairing; "Add client" mints a token manually for
 * headless use. Revocation takes effect immediately. Only files currently open
 * in the application are listed; unsaved files cannot be enabled because the
 * API identity is keyed by file path.
 */
public class RestApiPreferencePage extends PreferencePage
{
    /** shows a freshly minted token exactly once, with a copy button */
    private static final class ShowTokenDialog extends Dialog
    {
        private final String token;

        private ShowTokenDialog(Shell parentShell, String token)
        {
            super(parentShell);
            this.token = token;
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
            GridLayoutFactory.swtDefaults().numColumns(2).margins(15, 15).applyTo(container);

            var hint = new Label(container, SWT.WRAP);
            hint.setText(Messages.PrefMsgRestApiTokenShownOnce);
            GridDataFactory.fillDefaults().span(2, 1).grab(true, false).hint(400, SWT.DEFAULT).applyTo(hint);

            var tokenText = new Text(container, SWT.BORDER | SWT.READ_ONLY);
            tokenText.setText(token);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(tokenText);

            var copyButton = new Button(container, SWT.PUSH);
            copyButton.setText(Messages.LabelCopyToClipboard);
            copyButton.addListener(SWT.Selection, event -> {
                var clipboard = new Clipboard(getShell().getDisplay());
                try
                {
                    clipboard.setContents(new Object[] { token }, new Transfer[] { TextTransfer.getInstance() });
                }
                finally
                {
                    clipboard.dispose();
                }
            });

            return container;
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
        new ShowTokenDialog(getShell(), token).open();
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
