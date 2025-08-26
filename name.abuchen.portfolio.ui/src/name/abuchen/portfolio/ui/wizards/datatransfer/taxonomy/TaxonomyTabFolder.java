package name.abuchen.portfolio.ui.wizards.datatransfer.taxonomy;

import java.util.ArrayList;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.layout.TreeColumnLayout;
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.TaxonomyJSONImporter.ChangeEntry;
import name.abuchen.portfolio.model.TaxonomyJSONImporter.ImportResult;
import name.abuchen.portfolio.model.TaxonomyJSONImporter.Operation;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.LogoManager;

/* package */ class TaxonomyTabFolder
{
    private final Client client;
    private ImportResult importResult;

    private IStylingEngine stylingEngine;
    private TreeViewer nodeViewer;
    private TableViewer changeViewer;

    public TaxonomyTabFolder(Client client, IStylingEngine stylingEngine)
    {
        this.client = client;
        this.stylingEngine = stylingEngine;
    }

    public void setImportResult(Taxonomy taxonomy, ImportResult importResult)
    {
        this.importResult = importResult;

        changeViewer.setInput(importResult.getChanges());
        nodeViewer.setInput(taxonomy);
    }

    public void clearImportResult()
    {
        this.importResult = new ImportResult();

        changeViewer.setInput(new Object[0]);
        nodeViewer.setInput(null);
    }

    public Composite createTabFolder(Composite parent, String name)
    {
        var tabFolder = new CTabFolder(parent, SWT.TOP | SWT.FLAT);
        tabFolder.setBorderVisible(true);

        var tree = createTaxonomyTree(tabFolder);
        var item = new CTabItem(tabFolder, SWT.NONE);
        item.setControl(tree);
        item.setText(name);

        var table = createMessagesTable(tabFolder);
        item = new CTabItem(tabFolder, SWT.NONE);
        item.setControl(table);
        item.setText(Messages.LabelDescription);

        tabFolder.setSelection(0);

        return tabFolder;
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

        changeViewer = new TableViewer(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL);
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
                var entry = (ChangeEntry) element;
                return entry.getOperation().toString();
            }

            @Override
            public Color getBackground(Object element)
            {
                var entry = (ChangeEntry) element;
                return entry.getOperation() == Operation.WARNING || entry.getOperation() == Operation.ERROR
                                ? Colors.theme().warningBackground()
                                : null;
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
                var entry = (ChangeEntry) element;
                return entry.getComment();
            }
        });

        changeViewer.setContentProvider(ArrayContentProvider.getInstance());

        return tableContainer;
    }
}
