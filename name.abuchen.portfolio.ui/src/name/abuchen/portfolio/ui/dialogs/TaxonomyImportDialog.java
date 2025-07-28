package name.abuchen.portfolio.ui.dialogs;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.TaxonomyJSONImporter;
import name.abuchen.portfolio.model.TaxonomyJSONImporter.ChangeEntry;
import name.abuchen.portfolio.model.TaxonomyJSONImporter.ImportResult;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.LogoManager;

public class TaxonomyImportDialog extends TitleAreaDialog
{
    public static final int DIRTY = 42;
    private static final String PREF_PRESERVE_NAME_DESCRIPTION = TaxonomyImportDialog.class.getSimpleName()
                    + "-preserve.name.description"; //$NON-NLS-1$

    private final IStylingEngine stylingEngine;

    private final IPreferenceStore preferences;
    private final Client client;
    private final Taxonomy taxonomy;

    private Text filePathText;
    private TreeViewer nodeViewer;
    private TableViewer changeViewer;
    private Button preserveNameDescriptionCheckbox;
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
        newShell.setText(Messages.MenuImportTaxonomy);
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
        setTitle(Messages.MenuImportTaxonomy);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        var area = (Composite) super.createDialogArea(parent);

        var container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

        createFileSelectionSection(container);
        createPreviewSection(container);

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

        preserveNameDescriptionCheckbox = new Button(fileSection, SWT.CHECK);
        preserveNameDescriptionCheckbox.setText(Messages.LabelOptionPreserveNamesAndDescriptions);
        GridDataFactory.fillDefaults().span(3, 1).applyTo(preserveNameDescriptionCheckbox);

        preserveNameDescriptionCheckbox.setSelection(preferences.getBoolean(PREF_PRESERVE_NAME_DESCRIPTION));

        preserveNameDescriptionCheckbox.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            if (selectedFilePath != null)
                performDryRun();
        }));
    }

    private void createPreviewSection(Composite parent)
    {
        var tabFolder = new CTabFolder(parent, SWT.TOP | SWT.FLAT);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(tabFolder);
        tabFolder.setBorderVisible(true);

        var tree = createTaxonomyTree(tabFolder);
        var item = new CTabItem(tabFolder, SWT.NONE);
        item.setControl(tree);
        item.setText(taxonomy.getName());

        var table = createMessagesTable(tabFolder);
        item = new CTabItem(tabFolder, SWT.NONE);
        item.setControl(table);
        item.setText(Messages.LabelDescription);

        tabFolder.setSelection(0);
    }

    private Control createTaxonomyTree(Composite parent)
    {
        var treeContainer = new Composite(parent, SWT.NONE);
        var layout = new TreeColumnLayout();
        treeContainer.setLayout(layout);

        var tree = new Tree(treeContainer, SWT.BORDER | SWT.SINGLE);
        nodeViewer = new TreeViewer(tree);
        tree.setHeaderVisible(false);
        tree.setLinesVisible(true);

        // make sure to apply the styles before creating the fonts.
        stylingEngine.style(tree);

        var boldFont = JFaceResources.getResources()
                        .create(FontDescriptor.createFrom(tree.getFont()).setStyle(SWT.BOLD));

        var italicFont = JFaceResources.getResources()
                        .create(FontDescriptor.createFrom(tree.getFont()).setStyle(SWT.ITALIC));

        TreeColumn column = new TreeColumn(tree, SWT.None);
        layout.setColumnData(column, new ColumnWeightData(100));

        nodeViewer.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return switch (element)
                {
                    case Classification classification -> classification.getName();
                    case Assignment assignment -> assignment.getInvestmentVehicle().getName();
                    default -> null;
                };
            }

            @Override
            public Font getFont(Object element)
            {
                if (importResult == null)
                    return null;
                else if (importResult.isCreated(element))
                    return boldFont;
                else if (importResult.isModified(element))
                    return italicFont;
                else
                    return null;
            }

            @Override
            public Image getImage(Object element)
            {
                Named n = Adaptor.adapt(Named.class, element);
                return LogoManager.instance().getDefaultColumnImage(n, client.getSettings());
            }
        });

        nodeViewer.setContentProvider(new ITreeContentProvider()
        {
            @Override
            public boolean hasChildren(Object element)
            {
                if (element instanceof Classification classification)
                    return !classification.getChildren().isEmpty() || !classification.getAssignments().isEmpty();
                else
                    return false;
            }

            @Override
            public Object getParent(Object element)
            {
                if (element instanceof Classification classification)
                    return classification.getParent();
                else
                    return null;
            }

            @Override
            public Object[] getElements(Object inputElement)
            {
                return ((Taxonomy) inputElement).getRoot().getChildren().toArray();
            }

            @Override
            public Object[] getChildren(Object parentElement)
            {
                if (parentElement instanceof Classification classification)
                {
                    var children = new ArrayList<Object>();
                    children.addAll(classification.getChildren());
                    children.addAll(classification.getAssignments());
                    return children.toArray();
                }
                else
                {
                    return new Object[0];
                }
            }
        });

        return treeContainer;
    }

    private Control createMessagesTable(Composite parent)
    {
        var tableContainer = new Composite(parent, SWT.NONE);
        tableContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        var layout = new TableColumnLayout();
        tableContainer.setLayout(layout);

        changeViewer = new TableViewer(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        changeViewer.getTable().setHeaderVisible(true);
        changeViewer.getTable().setLinesVisible(true);

        var typeColumn = new TableViewerColumn(changeViewer, SWT.NONE);
        typeColumn.getColumn().setText(Messages.IntroLabelActions);
        layout.setColumnData(typeColumn.getColumn(), new ColumnWeightData(25));
        typeColumn.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                ChangeEntry entry = (ChangeEntry) element;
                return entry.getOperation().toString();
            }
        });

        var descriptionColumn = new TableViewerColumn(changeViewer, SWT.NONE);
        descriptionColumn.getColumn().setText(Messages.LabelDescription);
        layout.setColumnData(descriptionColumn.getColumn(), new ColumnWeightData(75));
        descriptionColumn.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                ChangeEntry entry = (ChangeEntry) element;
                return entry.getComment();
            }
        });

        changeViewer.setContentProvider(ArrayContentProvider.getInstance());

        return tableContainer;
    }

    private void selectFile()
    {
        var dialog = new FileDialog(getShell(), SWT.OPEN);
        dialog.setText(Messages.MenuImportTaxonomy);
        dialog.setFilterNames(new String[] { Messages.CSVConfigCSVImportLabelFileJSON });
        dialog.setFilterExtensions(new String[] { "*.json" }); //$NON-NLS-1$

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
            TaxonomyJSONImporter importer = new TaxonomyJSONImporter(client, copy, preserveNameDescription);
            importResult = importer.importTaxonomy(new InputStreamReader(fis, StandardCharsets.UTF_8));

            var hasChanges = importResult.hasChanges();

            changeViewer.setInput(importResult.getChanges());
            nodeViewer.setInput(copy);

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
            changeViewer.setInput(new Object[0]);
            nodeViewer.setInput(null);

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
        preferences.setValue(PREF_PRESERVE_NAME_DESCRIPTION, preserveNameDescription);

        // Perform the actual import
        try (FileInputStream fis = new FileInputStream(selectedFilePath))
        {
            var importer = new TaxonomyJSONImporter(client, taxonomy, preserveNameDescription);
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
