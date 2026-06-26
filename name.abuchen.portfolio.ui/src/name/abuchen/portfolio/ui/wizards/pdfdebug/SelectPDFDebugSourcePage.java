package name.abuchen.portfolio.ui.wizards.pdfdebug;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.UnrecognizedPDFCache;
import name.abuchen.portfolio.ui.util.UnrecognizedPDFCache.Entry;

@SuppressWarnings("nls")
public class SelectPDFDebugSourcePage extends WizardPage
{
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
                    .ofLocalizedDateTime(FormatStyle.SHORT)
                    .withZone(ZoneId.systemDefault());

    private final UnrecognizedPDFCache cache;

    private TableViewer viewer;

    private Entry selectedEntry;
    private File selectedFile;

    public SelectPDFDebugSourcePage(UnrecognizedPDFCache cache)
    {
        super("selectPDFDebugSource");
        this.cache = cache;
        setTitle(Messages.PDFImportDebugSelectSourceTitle);
        setDescription(Messages.PDFImportDebugSelectSourceDescription);
        setPageComplete(false);
    }

    public Entry getSelectedEntry()
    {
        return selectedEntry;
    }

    public File getSelectedFile()
    {
        return selectedFile;
    }

    @Override
    public void createControl(Composite parent)
    {
        var container = new Composite(parent, SWT.NULL);
        setControl(container);
        GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(container);

        // table spans left column, grabs all available space
        var tableComposite = new Composite(container, SWT.NULL);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(tableComposite);
        var tableColumnLayout = new TableColumnLayout();
        tableComposite.setLayout(tableColumnLayout);

        viewer = new TableViewer(tableComposite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        var nameCol = new TableViewerColumn(viewer, SWT.NONE);
        nameCol.getColumn().setText(Messages.PDFImportDebugColumnDocument);
        nameCol.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return element instanceof Entry e ? e.getName() : "";
            }
        });
        tableColumnLayout.setColumnData(nameCol.getColumn(), new ColumnWeightData(70, 200));

        var capturedCol = new TableViewerColumn(viewer, SWT.NONE);
        capturedCol.getColumn().setText(Messages.PDFImportDebugColumnCapturedAt);
        capturedCol.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return element instanceof Entry e ? FORMATTER.format(e.getCapturedAt()) : "";
            }
        });
        tableColumnLayout.setColumnData(capturedCol.getColumn(), new ColumnWeightData(30, 120));

        viewer.setInput(cache.getEntries());

        viewer.addSelectionChangedListener(event -> {
            var sel = (IStructuredSelection) viewer.getSelection();
            selectedEntry = sel.isEmpty() ? null : (Entry) sel.getFirstElement();
            if (selectedEntry != null)
                selectedFile = null;
            updatePageComplete();
        });

        // right column: action buttons, top-aligned
        var buttonComposite = new Composite(container, SWT.NULL);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(buttonComposite);
        GridLayoutFactory.fillDefaults().applyTo(buttonComposite);

        var browseButton = new Button(buttonComposite, SWT.PUSH);
        browseButton.setText(Messages.PDFImportDebugBrowse);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(browseButton);
        browseButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            var dialog = new FileDialog(getShell(), SWT.OPEN | SWT.SINGLE);
            dialog.setFilterNames(new String[] { Messages.PDFImportFilterName });
            dialog.setFilterExtensions(new String[] { "*.pdf;*.PDF" });
            var path = dialog.open();
            if (path != null)
            {
                selectedFile = new File(path);
                selectedEntry = null;
                viewer.setSelection(StructuredSelection.EMPTY);
                updatePageComplete();

                // jump straight to the anonymization page; selecting a file is
                // an unambiguous choice, so no extra confirmation click is needed
                var nextPage = getWizard().getNextPage(this);
                if (nextPage != null)
                    getContainer().showPage(nextPage);
            }
        }));

        var clearButton = new Button(buttonComposite, SWT.PUSH);
        clearButton.setText(Messages.PDFImportDebugClearList);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(clearButton);
        clearButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            cache.clear();
            selectedEntry = null;
            selectedFile = null;
            viewer.setInput(cache.getEntries());
            updatePageComplete();
        }));
    }

    private void updatePageComplete()
    {
        setPageComplete(selectedEntry != null || selectedFile != null);
    }
}
