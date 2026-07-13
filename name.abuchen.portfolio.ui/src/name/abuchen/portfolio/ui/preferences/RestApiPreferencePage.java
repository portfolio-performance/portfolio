package name.abuchen.portfolio.ui.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.RestApiConstants;
import name.abuchen.portfolio.rest.RestApiWorkspace;
import name.abuchen.portfolio.rest.TokenStore;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.ClientInputFactory;
import name.abuchen.portfolio.ui.util.Colors;

/**
 * Configures the local REST API: global enable switch, port, bearer token,
 * and the per-file opt-in with optional alias. Only files currently open in
 * the application are listed; unsaved files cannot be enabled because the
 * API identity is keyed by file path.
 */
public class RestApiPreferencePage extends PreferencePage
{
    private record Row(String path, String label)
    {
    }

    private final ClientInputFactory clientInputFactory;
    private final IEclipsePreferences preferences = RestApiWorkspace.preferences();
    private final FileAccessRegistry registry = RestApiWorkspace.createFileAccessRegistry();
    private final TokenStore tokenStore = RestApiWorkspace.createTokenStore();

    private Button enableButton;
    private Text portText;
    private Text tokenText;
    private CheckboxTableViewer filesViewer;
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

        new Label(container, SWT.NONE).setText(Messages.PrefLabelRestApiToken);
        var tokenRow = new Composite(container, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(3).applyTo(tokenRow);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(tokenRow);

        tokenText = new Text(tokenRow, SWT.BORDER | SWT.READ_ONLY);
        tokenText.setText(tokenStore.getOrCreate());
        GridDataFactory.fillDefaults().grab(true, false).applyTo(tokenText);

        var copyButton = new Button(tokenRow, SWT.PUSH);
        copyButton.setText(Messages.LabelCopyToClipboard);
        copyButton.addListener(SWT.Selection, event -> {
            var clipboard = new Clipboard(getShell().getDisplay());
            try
            {
                clipboard.setContents(new Object[] { tokenText.getText() },
                                new Transfer[] { TextTransfer.getInstance() });
            }
            finally
            {
                clipboard.dispose();
            }
        });

        var regenerateButton = new Button(tokenRow, SWT.PUSH);
        regenerateButton.setText(Messages.PrefRestApiBtnRegenerate);
        regenerateButton.addListener(SWT.Selection, event -> {
            if (MessageDialog.openConfirm(getShell(), Messages.PrefTitleRestApi,
                            Messages.PrefMsgRestApiRegenerateConfirm))
                tokenText.setText(tokenStore.regenerate());
        });

        var hasUnsavedFiles = clientInputFactory.listOpenClients().stream().anyMatch(input -> input.getFile() == null);
        if (hasUnsavedFiles)
        {
            var hint = new Label(container, SWT.WRAP);
            hint.setText(Messages.PrefMsgRestApiUnsavedFiles);
            hint.setForeground(Colors.theme().warningForeground());
            GridDataFactory.fillDefaults().span(2, 1).applyTo(hint);
        }

        createFilesTable(container);

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

        try
        {
            var input = (List<?>) filesViewer.getInput();
            for (Object element : input)
            {
                var row = (Row) element;
                registry.setEnabled(row.path(), filesViewer.getChecked(row));
                registry.setAlias(row.path(), aliases.get(row.path()));
            }
        }
        catch (IllegalArgumentException e)
        {
            setErrorMessage(e.getMessage());
            return false;
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
