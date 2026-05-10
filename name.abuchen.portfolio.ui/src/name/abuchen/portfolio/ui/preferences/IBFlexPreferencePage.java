package name.abuchen.portfolio.ui.preferences;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.preferences.IBFlexConfiguration.Credential;
import name.abuchen.portfolio.ui.preferences.IBFlexConfiguration.Cutoff;
import name.abuchen.portfolio.ui.util.DesktopAPI;

public class IBFlexPreferencePage extends PreferencePage
{
    private static final DateTimeFormatter IBFLEX_LAST_IMPORT_DATE_FORMAT = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss"); //$NON-NLS-1$

    private static final class EditCredentialDialog extends Dialog
    {
        private final List<Credential> existingCredentials;
        private final Credential originalCredential;
        private Text queryIdText;
        private Text tokenText;
        private Text nameText;
        private Credential credential;

        private EditCredentialDialog(Shell parentShell, List<Credential> existingCredentials,
                        Credential originalCredential)
        {
            super(parentShell);
            this.existingCredentials = existingCredentials;
            this.originalCredential = originalCredential;
        }

        @Override
        protected void configureShell(Shell shell)
        {
            super.configureShell(shell);
            shell.setText(Messages.PrefTitleIBFlex);
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            var container = (Composite) super.createDialogArea(parent);

            var composite = new Composite(container, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(2).margins(10, 10).applyTo(composite);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);

            var queryIdLabel = new Label(composite, SWT.NONE);
            queryIdLabel.setText(Messages.IBFlexQueryId);

            queryIdText = new Text(composite, SWT.BORDER);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(queryIdText);
            VerifyListener queryIdListener = event -> event.doit = event.text.chars().allMatch(Character::isDigit);
            queryIdText.addVerifyListener(queryIdListener);

            var tokenLabel = new Label(composite, SWT.NONE);
            tokenLabel.setText(Messages.IBFlexToken);

            tokenText = new Text(composite, SWT.BORDER | SWT.PASSWORD);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(tokenText);

            var nameLabel = new Label(composite, SWT.NONE);
            nameLabel.setText(Messages.ColumnName);

            nameText = new Text(composite, SWT.BORDER);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(nameText);

            if (originalCredential != null)
            {
                queryIdText.setText(originalCredential.queryId());
                tokenText.setText(originalCredential.token());
                nameText.setText(originalCredential.name() != null ? originalCredential.name() : ""); //$NON-NLS-1$
            }

            ModifyListener listener = event -> updateOkButton();
            queryIdText.addModifyListener(listener);
            tokenText.addModifyListener(listener);
            nameText.addModifyListener(listener);

            return container;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent)
        {
            super.createButtonsForButtonBar(parent);
            updateOkButton();
        }

        private void updateOkButton()
        {
            var okButton = getButton(IDialogConstants.OK_ID);
            if (okButton != null)
                okButton.setEnabled(queryIdText != null && tokenText != null && !queryIdText.getText().isBlank()
                                && queryIdText.getText().chars().allMatch(Character::isDigit)
                                && !tokenText.getText().isBlank() && !isDuplicateQueryId(queryIdText.getText().trim()));
        }

        private boolean isDuplicateQueryId(String queryId)
        {
            return existingCredentials.stream().filter(credential -> credential != originalCredential)
                            .anyMatch(credential -> credential.queryId().equals(queryId));
        }

        @Override
        protected void okPressed()
        {
            credential = new Credential(queryIdText.getText(), tokenText.getText(), nameText.getText());
            super.okPressed();
        }

        private Credential getCredential()
        {
            return credential;
        }
    }

    private final Optional<Client> client;
    private List<Credential> credentials;
    private List<Cutoff> cutoffs;
    private TableViewer credentialsTableViewer;
    private TableViewer cutoffsTableViewer;
    private Button editCredentialButton;
    private Button removeCredentialButton;
    private Button clearCutoffButton;
    private Button clearAllCutoffsButton;

    public IBFlexPreferencePage(Optional<Client> client)
    {
        this.client = client;
        setTitle(Messages.PrefTitleIBFlex);
        setDescription(Messages.PrefDescriptionIBFlex);
    }

    @Override
    protected Control createContents(Composite parent)
    {
        var composite = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);

        Link link = new Link(composite, SWT.NONE);
        link.setText("<a>https://www.interactivebrokers.com/campus/ibkr-api-page/flex-web-service/</a>"); //$NON-NLS-1$
        GridDataFactory.fillDefaults().span(2, 1).applyTo(link);
        link.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> DesktopAPI
                        .browse("https://www.interactivebrokers.com/campus/ibkr-api-page/flex-web-service/"))); //$NON-NLS-1$

        credentials = new ArrayList<>(IBFlexConfiguration.getCredentials());
        cutoffs = client.map(
                        activeClient -> new ArrayList<>(IBFlexConfiguration.getLastImportDates(activeClient)))
                        .orElseGet(ArrayList::new);

        credentialsTableViewer = new TableViewer(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        Table credentialsTable = credentialsTableViewer.getTable();
        credentialsTable.setHeaderVisible(true);
        credentialsTable.setLinesVisible(true);
        GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 150).applyTo(credentialsTable);

        var queryIdColumn = new TableViewerColumn(credentialsTableViewer, SWT.NONE);
        queryIdColumn.getColumn().setText(Messages.IBFlexQueryId);
        queryIdColumn.getColumn().setWidth(120);
        queryIdColumn.setLabelProvider(ColumnLabelProvider.createTextProvider(e -> ((Credential) e).queryId()));

        var tokenColumn = new TableViewerColumn(credentialsTableViewer, SWT.NONE);
        tokenColumn.getColumn().setText(Messages.IBFlexToken);
        tokenColumn.getColumn().setWidth(220);
        tokenColumn.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                var token = ((Credential) element).token();
                if (token.length() <= 6)
                    return "\u2022".repeat(token.length()); //$NON-NLS-1$
                return token.substring(0, 3) + "\u2022".repeat(token.length() - 6) //$NON-NLS-1$
                                + token.substring(token.length() - 3);
            }
        });

        var nameColumn = new TableViewerColumn(credentialsTableViewer, SWT.NONE);
        nameColumn.getColumn().setText(Messages.ColumnName);
        nameColumn.getColumn().setWidth(180);
        nameColumn.setLabelProvider(ColumnLabelProvider.createTextProvider(e -> {
            var name = ((Credential) e).name();
            return name != null ? name : ""; //$NON-NLS-1$
        }));

        credentialsTableViewer.setContentProvider(ArrayContentProvider.getInstance());
        credentialsTableViewer.setInput(credentials);
        credentialsTableViewer.addSelectionChangedListener(event -> updateCredentialButtons());

        var buttonComposite = new Composite(composite, SWT.NONE);
        GridLayoutFactory.fillDefaults().applyTo(buttonComposite);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(buttonComposite);

        var addButton = new Button(buttonComposite, SWT.PUSH);
        addButton.setText(Messages.ConsumerPriceIndexMenuAdd);
        addButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        addButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> addCredential()));

        editCredentialButton = new Button(buttonComposite, SWT.PUSH);
        editCredentialButton.setText(Messages.IBFlexEdit);
        editCredentialButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        editCredentialButton
                        .addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> editSelectedCredential()));

        removeCredentialButton = new Button(buttonComposite, SWT.PUSH);
        removeCredentialButton.setText(Messages.ChartSeriesPickerRemove);
        removeCredentialButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        removeCredentialButton.addSelectionListener(
                        SelectionListener.widgetSelectedAdapter(e -> removeSelectedCredentials()));

        var separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).indent(0, 10).applyTo(separator);

        var cutoffsLabel = new Label(composite, SWT.NONE);
        cutoffsLabel.setText(Messages.IBFlexCurrentPortfolioCutoffs);
        GridDataFactory.fillDefaults().span(2, 1).applyTo(cutoffsLabel);

        cutoffsTableViewer = new TableViewer(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        Table cutoffsTable = cutoffsTableViewer.getTable();
        cutoffsTable.setHeaderVisible(true);
        cutoffsTable.setLinesVisible(true);
        GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 120).applyTo(cutoffsTable);

        var cutoffQueryIdColumn = new TableViewerColumn(cutoffsTableViewer, SWT.NONE);
        cutoffQueryIdColumn.getColumn().setText(Messages.IBFlexQueryId);
        cutoffQueryIdColumn.getColumn().setWidth(200);
        cutoffQueryIdColumn.setLabelProvider(ColumnLabelProvider.createTextProvider(e -> ((Cutoff) e).queryId()));

        var cutoffDateColumn = new TableViewerColumn(cutoffsTableViewer, SWT.NONE);
        cutoffDateColumn.getColumn().setText(Messages.IBFlexLastImportDate);
        cutoffDateColumn.getColumn().setWidth(180);
        cutoffDateColumn.setLabelProvider(
                        ColumnLabelProvider.createTextProvider(e -> formatIBFlexLastImportDate(((Cutoff) e).date())));

        cutoffsTableViewer.setContentProvider(ArrayContentProvider.getInstance());
        cutoffsTableViewer.setInput(cutoffs);
        cutoffsTableViewer.addSelectionChangedListener(event -> updateCutoffButtons());

        var cutoffButtonComposite = new Composite(composite, SWT.NONE);
        GridLayoutFactory.fillDefaults().applyTo(cutoffButtonComposite);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(cutoffButtonComposite);

        clearCutoffButton = new Button(cutoffButtonComposite, SWT.PUSH);
        clearCutoffButton.setText(Messages.IBFlexClearLastImportDate);
        clearCutoffButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        clearCutoffButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> removeSelectedCutoff()));

        clearAllCutoffsButton = new Button(cutoffButtonComposite, SWT.PUSH);
        clearAllCutoffsButton.setText(Messages.IBFlexClearAllLastImportDates);
        clearAllCutoffsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        clearAllCutoffsButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            cutoffs.clear();
            refreshCutoffs();
        }));

        refreshCredentials();
        refreshCutoffs();
        return composite;
    }

    private void addCredential()
    {
        var dialog = new EditCredentialDialog(getShell(), credentials, null);
        if (dialog.open() != Window.OK)
            return;

        credentials.add(dialog.getCredential());
        refreshCredentials();
    }

    private void editSelectedCredential()
    {
        var selection = (IStructuredSelection) credentialsTableViewer.getSelection();
        if (selection.size() != 1)
            return;

        var originalCredential = (Credential) selection.getFirstElement();
        var dialog = new EditCredentialDialog(getShell(), credentials, originalCredential);
        if (dialog.open() != Window.OK)
            return;

        int index = credentials.indexOf(originalCredential);
        if (index >= 0)
        {
            credentials.set(index, dialog.getCredential());
            refreshCredentials();
        }
    }

    private void removeSelectedCredentials()
    {
        var selection = (IStructuredSelection) credentialsTableViewer.getSelection();
        if (selection.isEmpty())
            return;

        for (Object element : selection.toList())
            credentials.remove(element);

        refreshCredentials();
    }

    private void refreshCredentials()
    {
        credentialsTableViewer.refresh();
        updateCredentialButtons();
    }

    private void updateCredentialButtons()
    {
        if (credentialsTableViewer == null || editCredentialButton == null || removeCredentialButton == null)
            return;

        var selection = (IStructuredSelection) credentialsTableViewer.getSelection();
        editCredentialButton.setEnabled(selection.size() == 1);
        removeCredentialButton.setEnabled(!selection.isEmpty());
    }

    private void removeSelectedCutoff()
    {
        var selection = (IStructuredSelection) cutoffsTableViewer.getSelection();
        if (selection.isEmpty())
            return;

        for (Object element : selection.toList())
            cutoffs.remove(element);

        refreshCutoffs();
    }

    private void refreshCutoffs()
    {
        cutoffs.sort(Comparator.comparing(Cutoff::queryId));
        cutoffsTableViewer.refresh();
        updateCutoffButtons();
    }

    private void updateCutoffButtons()
    {
        if (clearCutoffButton == null || clearAllCutoffsButton == null || cutoffsTableViewer == null)
            return;

        clearCutoffButton.setEnabled(!cutoffsTableViewer.getSelection().isEmpty());
        clearAllCutoffsButton.setEnabled(!cutoffs.isEmpty());
    }

    @Override
    public boolean performOk()
    {
        getPreferenceStore().setValue(UIConstants.Preferences.IBFLEX_CREDENTIALS,
                        IBFlexConfiguration.serialize(credentials));
        client.ifPresent(activeClient -> IBFlexConfiguration.setLastImportDates(activeClient, cutoffs));
        return true;
    }

    @Override
    protected void performDefaults()
    {
        credentials.clear();
        refreshCredentials();
        cutoffs.clear();
        refreshCutoffs();
        super.performDefaults();
    }

    private static String formatIBFlexLastImportDate(LocalDateTime value)
    {
        if (value == null)
            return null;

        return IBFLEX_LAST_IMPORT_DATE_FORMAT.format(value);
    }
}
