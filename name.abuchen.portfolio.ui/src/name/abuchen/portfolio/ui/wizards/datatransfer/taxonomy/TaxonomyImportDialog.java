package name.abuchen.portfolio.ui.wizards.datatransfer.taxonomy;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.bootstrap.BundleMessages;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.TaxonomyJSONImporter;
import name.abuchen.portfolio.model.TaxonomyJSONImporter.ImportResult;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public class TaxonomyImportDialog extends TitleAreaDialog
{
    public static final int DIRTY = 42;

    private final IStylingEngine stylingEngine;

    private final IPreferenceStore preferences;
    private final Client client;
    private final Taxonomy taxonomy;

    private Text filePathText;
    private Button preserveNameDescriptionCheckbox;
    private Button pruneAbsentClassificationsCheckbox;

    private TaxonomyTabFolder taxonomyTabFolder;
    private ImportResult importResult;
    private String selectedFilePath;

    public TaxonomyImportDialog(Shell parentShell, IStylingEngine stylingEngine, IPreferenceStore preferences,
                    Client client, Taxonomy taxonomy)
    {
        super(parentShell);

        this.stylingEngine = stylingEngine;

        this.preferences = preferences;
        this.client = client;
        this.taxonomy = taxonomy;

        setTitleImage(Images.BANNER.image());
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return DialogSettings.getOrCreateSection(PortfolioPlugin.getDefault().getDialogSettings(),
                        TaxonomyImportDialog.class.getSimpleName());
    }

    @Override
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText(BundleMessages.getString(BundleMessages.Label.Command.importTaxonomy));
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    public void create()
    {
        super.create();
        setTitle(BundleMessages.getString(BundleMessages.Label.Command.importTaxonomy));
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        var area = (Composite) super.createDialogArea(parent);

        var container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

        createFileSelectionSection(container);

        taxonomyTabFolder = new TaxonomyTabFolder(client, stylingEngine);
        var tabFolder = taxonomyTabFolder.createTabFolder(container, taxonomy.getName());
        GridDataFactory.fillDefaults().grab(true, true).applyTo(tabFolder);

        return area;
    }

    private void createFileSelectionSection(Composite parent)
    {
        var fileSection = new Composite(parent, SWT.NONE);
        fileSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        fileSection.setLayout(new GridLayout(3, false));

        var fileLabel = new Label(fileSection, SWT.NONE);
        fileLabel.setText(Messages.LabelJSONFile);

        filePathText = new Text(fileSection, SWT.BORDER | SWT.READ_ONLY);
        filePathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        var browseButton = new Button(fileSection, SWT.PUSH);
        browseButton.setText(Messages.LabelPickFile);
        browseButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> selectFile()));

        var optionsComposite = new Composite(fileSection, SWT.NONE);
        optionsComposite.setLayout(new RowLayout());
        GridDataFactory.fillDefaults().span(3, 1).applyTo(optionsComposite);

        preserveNameDescriptionCheckbox = new Button(optionsComposite, SWT.CHECK);
        preserveNameDescriptionCheckbox.setText(Messages.LabelOptionPreserveNamesAndDescriptions);

        preserveNameDescriptionCheckbox
                        .setSelection(preferences.getBoolean(TaxonomyImportModel.PREF_PRESERVE_NAME_DESCRIPTION));

        preserveNameDescriptionCheckbox.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            preferences.setValue(TaxonomyImportModel.PREF_PRESERVE_NAME_DESCRIPTION,
                            preserveNameDescriptionCheckbox.getSelection());
            if (selectedFilePath != null)
                performDryRun();
        }));

        pruneAbsentClassificationsCheckbox = new Button(optionsComposite, SWT.CHECK);
        pruneAbsentClassificationsCheckbox.setText(Messages.LabelOptionPruneAbsentClassifications);

        pruneAbsentClassificationsCheckbox
                        .setSelection(preferences.getBoolean(TaxonomyImportModel.PREF_PRUNE_ABSENT_CLASSIFICATIONS));

        pruneAbsentClassificationsCheckbox.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            preferences.setValue(TaxonomyImportModel.PREF_PRUNE_ABSENT_CLASSIFICATIONS,
                            pruneAbsentClassificationsCheckbox.getSelection());
            if (selectedFilePath != null)
                performDryRun();
        }));
    }

    private void selectFile()
    {
        var dialog = new FileDialog(getShell(), SWT.OPEN);
        dialog.setText(BundleMessages.getString(BundleMessages.Label.Command.importTaxonomy));
        dialog.setFilterNames(new String[] { Messages.CSVConfigCSVImportLabelFileJSON });
        dialog.setFilterExtensions(new String[] { "*.json;*.JSON" }); //$NON-NLS-1$

        var filePath = dialog.open();
        if (filePath != null)
        {
            selectedFilePath = filePath;
            filePathText.setText(filePath);
            performDryRun();
        }
    }

    private void performDryRun()
    {
        if (selectedFilePath == null)
            return;

        try (FileInputStream fis = new FileInputStream(selectedFilePath))
        {
            var copy = taxonomy.copy();
            boolean preserveNameDescription = preserveNameDescriptionCheckbox.getSelection();
            boolean replaceMode = pruneAbsentClassificationsCheckbox.getSelection();
            TaxonomyJSONImporter importer = new TaxonomyJSONImporter(client, copy, preserveNameDescription,
                            replaceMode);
            importResult = importer.importTaxonomy(new InputStreamReader(fis, StandardCharsets.UTF_8));

            var hasChanges = importResult.hasChanges();

            taxonomyTabFolder.setImportResult(copy, importResult);

            Button okButton = getButton(IDialogConstants.OK_ID);
            if (okButton != null)
                okButton.setEnabled(hasChanges);

            if (!hasChanges)
            {
                setMessage(Messages.MsgNoChangesToBeApplied, IMessageProvider.WARNING);
                setErrorMessage(null);
            }
            else
            {
                setMessage(MessageFormat.format(Messages.LabelAdditionsAndModifications,
                                importResult.getCreatedObjects(), importResult.getModifiedObjects()));
                setErrorMessage(null);
            }

        }
        catch (IOException e)
        {
            setMessage(null);
            setErrorMessage(e.getMessage());
            importResult = null;
            taxonomyTabFolder.clearImportResult();

            var okButton = getButton(IDialogConstants.OK_ID);
            if (okButton != null)
                okButton.setEnabled(false);
        }
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);

        var okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null)
            okButton.setEnabled(false);
    }

    @Override
    protected void okPressed()
    {
        if (selectedFilePath == null || importResult == null || importResult.getChanges().isEmpty())
        {
            super.okPressed();
            return;
        }

        var preserveNameDescription = preserveNameDescriptionCheckbox.getSelection();
        var replaceMode = pruneAbsentClassificationsCheckbox.getSelection();

        // Perform the actual import
        try (FileInputStream fis = new FileInputStream(selectedFilePath))
        {
            var importer = new TaxonomyJSONImporter(client, taxonomy, preserveNameDescription, replaceMode);
            var result = importer.importTaxonomy(new InputStreamReader(fis, StandardCharsets.UTF_8));

            var hasChanges = result.hasChanges();

            MessageDialog.openInformation(getShell(), Messages.MsgImportCompletedSuccessfully,
                            Messages.MsgImportCompletedSuccessfully + "\n\n" //$NON-NLS-1$
                                            + MessageFormat.format(Messages.LabelAdditionsAndModifications,
                                                            importResult.getCreatedObjects(),
                                                            importResult.getModifiedObjects()));

            setReturnCode(hasChanges ? DIRTY : OK);
            close();
        }
        catch (IOException e)
        {
            MessageDialog.openError(getShell(), Messages.LabelError, e.getMessage());
        }
    }
}
