package name.abuchen.portfolio.ui.wizards.datatransfer.taxonomy;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.bootstrap.BundleMessages;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.TaxonomyJSONImporter;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.action.MenuContribution;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;
import name.abuchen.portfolio.ui.wizards.datatransfer.taxonomy.TaxonomyImportModel.ImportAction;
import name.abuchen.portfolio.ui.wizards.datatransfer.taxonomy.TaxonomyImportModel.ImportItem;

/**
 * Combined page for file selection and taxonomy action configuration. Displays
 * file selection at the top and taxonomy table at the bottom.
 */
public class TaxonomyFileSelectionPage extends AbstractWizardPage
{
    private final TaxonomyImportModel importModel;

    private Text filePathText;
    private TableViewer tableViewer;

    private String selectedFilePath;

    public TaxonomyFileSelectionPage(TaxonomyImportModel importModel)
    {
        super("taxonomyFileSelection"); //$NON-NLS-1$
        this.importModel = importModel;

        setTitle(BundleMessages.getString(BundleMessages.Label.Command.importTaxonomy));
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        // File selection section at the top
        createFileSelectionSection(container);

        // Taxonomy selection section at the bottom (initially hidden)
        createTaxonomySection(container);

        setControl(container);
        setPageComplete(false); // Initially not complete until file is selected
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

        var preserveNameDescriptionCheckbox = new Button(optionsComposite, SWT.CHECK);
        preserveNameDescriptionCheckbox.setText(Messages.LabelOptionPreserveNamesAndDescriptions);

        preserveNameDescriptionCheckbox.setSelection(importModel.isPreserveNameAndDescription());

        preserveNameDescriptionCheckbox.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            importModel.setPreserveNameAndDescription(preserveNameDescriptionCheckbox.getSelection());
            rerunDryRun();
        }));

        var pruneAbsentClassificationsCheckbox = new Button(optionsComposite, SWT.CHECK);
        pruneAbsentClassificationsCheckbox.setText(Messages.LabelOptionPruneAbsentClassifications);

        pruneAbsentClassificationsCheckbox.setSelection(importModel.doPruneAbsentClassifications());

        pruneAbsentClassificationsCheckbox.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            importModel.setPruneAbsentClassifications(pruneAbsentClassificationsCheckbox.getSelection());
            rerunDryRun();
        }));
    }

    private void createTaxonomySection(Composite parent)
    {
        Composite tableContainer = new Composite(parent, SWT.NONE);
        tableContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        TableColumnLayout layout = new TableColumnLayout();
        tableContainer.setLayout(layout);

        Table table = new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        tableViewer = new TableViewer(table);
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        // Taxonomy Name column
        var nameColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        nameColumn.getColumn().setText(Messages.ColumnName);
        layout.setColumnData(nameColumn.getColumn(), new ColumnWeightData(50));
        nameColumn.setLabelProvider(new CellLabelProvider()
        {
            @Override
            public void update(ViewerCell cell)
            {
                var item = (ImportItem) cell.getElement();
                cell.setText(item.getName());
            }
        });

        // Import Action column
        var actionColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        actionColumn.getColumn().setText(Messages.ColumnAction);
        layout.setColumnData(actionColumn.getColumn(), new ColumnWeightData(50));
        actionColumn.setLabelProvider(new CellLabelProvider()
        {
            @Override
            public void update(ViewerCell cell)
            {
                var item = (ImportItem) cell.getElement();
                cell.setText(getActionDisplayText(item));
                cell.setImage(Images.QUICKFIX.image());
            }
        });

        // Result column
        var resultColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        resultColumn.getColumn().setText(Messages.ColumnStatus);
        layout.setColumnData(resultColumn.getColumn(), new ColumnWeightData(50));
        resultColumn.setLabelProvider(new CellLabelProvider()
        {
            @Override
            public void update(ViewerCell cell)
            {
                var item = (ImportItem) cell.getElement();
                cell.setText(item.getChangesText());
            }
        });

        hookContextMenu();
    }

    private void hookContextMenu()
    {
        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(this::fillContextMenu);

        Menu contextMenu = menuMgr.createContextMenu(tableViewer.getTable());
        tableViewer.getTable().setMenu(contextMenu);
        tableViewer.getTable().setData(ContextMenu.DEFAULT_MENU, contextMenu);

        tableViewer.getTable().addDisposeListener(e -> {
            if (contextMenu != null)
                contextMenu.dispose();
        });
    }

    private void fillContextMenu(IMenuManager manager)
    {
        var selection = tableViewer.getStructuredSelection();
        if (selection.isEmpty())
            return;

        manager.add(new SimpleAction(ImportAction.NEW.getLabel(), a -> {
            for (var element : selection)
            {
                if (element instanceof ImportItem item)
                {
                    item.setImportAction(ImportAction.NEW);
                    item.setTargetTaxonomy(null);
                    rerunDryRun(item);
                }
            }
        }));

        manager.add(new SimpleAction(ImportAction.SKIP.getLabel(), a -> {
            for (var element : selection)
            {
                if (element instanceof ImportItem item)
                {
                    item.setImportAction(ImportAction.SKIP);
                    item.setTargetTaxonomy(null);
                    rerunDryRun(item);
                }
            }
        }));

        if (selection.size() == 1)
        {
            var item = (ImportItem) selection.getFirstElement();

            for (var taxonomy : importModel.getClient().getTaxonomies())
            {
                manager.add(new MenuContribution(MessageFormat.format(Messages.LabelColonSeparated,
                                ImportAction.UPDATE.getLabel(), taxonomy.getName()), () -> {
                                    item.setImportAction(ImportAction.UPDATE);
                                    item.setTargetTaxonomy(taxonomy);
                                    rerunDryRun(item);
                                }));
            }
        }
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
            parseAndValidateFile();
        }
    }

    private void parseAndValidateFile()
    {
        if (selectedFilePath == null)
            return;

        try (FileInputStream fis = new FileInputStream(selectedFilePath);
                        InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8))
        {
            var newItems = parseFile(reader);

            for (var item : newItems)
            {
                // pre-select existing taxonomy if name matches
                var existingTaxonomy = importModel.getClient().getTaxonomies().stream()
                                .filter(t -> t.getName().equals(item.getName())).findFirst().orElse(null);
                if (existingTaxonomy != null)
                {
                    item.setImportAction(ImportAction.UPDATE);
                    item.setTargetTaxonomy(existingTaxonomy);
                }

                performDryRun(item);
            }

            this.importModel.setImportItems(newItems);

            if (tableViewer != null)
            {
                tableViewer.setInput(newItems);
                tableViewer.refresh();
            }

            // notify wizard that import items have changed
            if (getWizard() instanceof MultiTaxonomyImportWizard wizard)
                wizard.resetDetailPages();

            setPageComplete(isPageComplete());
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            setErrorMessage(MessageFormat.format(Messages.LabelColonSeparated, Messages.LabelError, e.getMessage()));
            setPageComplete(false);
        }
    }

    @SuppressWarnings("unchecked")
    private ArrayList<ImportItem> parseFile(InputStreamReader reader)
    {
        var newItems = new ArrayList<ImportItem>();

        var gson = new Gson();
        var root = JsonParser.parseReader(reader);

        if (root.isJsonArray())
        {
            for (var element : root.getAsJsonArray())
                newItems.add(new ImportItem(gson.fromJson(element, Map.class)));
        }
        else if (root.isJsonObject())
        {
            newItems.add(new ImportItem(gson.fromJson(root, Map.class)));
        }

        return newItems;
    }

    private void performDryRun(ImportItem item)
    {
        item.setErrorMessage(null);

        if (item.getImportAction() == ImportAction.SKIP)
        {
            item.setImportResult(null, null);
            return;
        }

        try
        {
            var dryrunTaxonomy = createDryrunTaxonomy(item);

            var importer = new TaxonomyJSONImporter(importModel.getClient(), dryrunTaxonomy,
                            importModel.isPreserveNameAndDescription(), importModel.doPruneAbsentClassifications());

            var dryrunResult = importer.importTaxonomy(item.getJsonData());
            item.setImportResult(dryrunResult, dryrunTaxonomy);
        }
        catch (IOException e)
        {
            item.setErrorMessage(e.getMessage());
        }
    }

    /**
     * Re-run dry runs for the given item, e.g., when its action changed.
     */
    private void rerunDryRun(ImportItem item)
    {
        performDryRun(item);

        if (tableViewer != null)
            tableViewer.refresh(item);

        // notify wizard that import items have changed
        if (getWizard() instanceof MultiTaxonomyImportWizard wizard)
            wizard.resetDetailPages();

        setPageComplete(isPageComplete());
    }

    /**
     * Re-run dry runs for all items when one the option to keep names or the
     * replace mode changes
     */
    private void rerunDryRun()
    {
        for (var item : importModel.getImportItems())
            performDryRun(item);
        if (tableViewer != null)
            tableViewer.refresh();

        // notify wizard that import items have changed
        if (getWizard() instanceof MultiTaxonomyImportWizard wizard)
            wizard.resetDetailPages();

        setPageComplete(isPageComplete());
    }

    private Taxonomy createDryrunTaxonomy(ImportItem item)
    {
        if (item.getImportAction() == ImportAction.UPDATE && item.getTargetTaxonomy() != null)
        {
            // For dry run on existing taxonomy, create a copy
            return item.getTargetTaxonomy().copy();
        }
        else
        {
            // For new taxonomy, create a temporary one
            Taxonomy taxonomy = new Taxonomy(item.getName());
            taxonomy.setRootNode(new Classification(UUID.randomUUID().toString(), item.getName()));
            return taxonomy;
        }
    }

    private String getActionDisplayText(ImportItem item)
    {
        if (item.getImportAction() == ImportAction.UPDATE)
        {
            return MessageFormat.format(Messages.LabelColonSeparated, ImportAction.UPDATE.getLabel(),
                            item.getTargetTaxonomy().getName());
        }
        else
        {
            return item.getImportAction().getLabel();
        }
    }

    @Override
    public boolean isPageComplete()
    {
        return importModel.getImportItems().stream() //
                        .filter(item -> item.getImportAction() != ImportAction.SKIP) //
                        .count() > 0;
    }
}
