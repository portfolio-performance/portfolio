package name.abuchen.portfolio.ui.wizards;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.CellEditorFactory;
import name.abuchen.portfolio.ui.util.CellEditorFactory.ModificationListener;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.util.CSVImportDefinition;
import name.abuchen.portfolio.util.CSVImporter;
import name.abuchen.portfolio.util.CSVImporter.AmountField;
import name.abuchen.portfolio.util.CSVImporter.Column;
import name.abuchen.portfolio.util.CSVImporter.DateField;
import name.abuchen.portfolio.util.CSVImporter.EnumField;
import name.abuchen.portfolio.util.CSVImporter.EnumMapFormat;
import name.abuchen.portfolio.util.CSVImporter.Field;
import name.abuchen.portfolio.util.CSVImporter.FieldFormat;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public class ImportDefinitionPage extends AbstractWizardPage implements ISelectionChangedListener
{
    private static final class Delimiter
    {
        private final char delimiter;
        private final String label;

        private Delimiter(char delimiter, String label)
        {
            this.delimiter = delimiter;
            this.label = label;
        }

        public char getDelimiter()
        {
            return delimiter;
        }

        public String getLabel()
        {
            return label;
        }

        @Override
        public String toString()
        {
            return getLabel();
        }
    }

    private TableViewer tableViewer;

    private final CSVImporter importer;

    public ImportDefinitionPage(CSVImporter importer)
    {
        super("importdefinition"); //$NON-NLS-1$
        setTitle(Messages.CSVImportWizardTitle);
        setDescription(Messages.CSVImportWizardDescription);

        this.importer = importer;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new FormLayout());

        Label lblTarget = new Label(container, SWT.NONE);
        lblTarget.setText(Messages.CSVImportLabelTarget);
        Combo cmbTarget = new Combo(container, SWT.READ_ONLY);
        ComboViewer target = new ComboViewer(cmbTarget);
        target.setContentProvider(ArrayContentProvider.getInstance());
        target.setLabelProvider(new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                if (element instanceof CSVImportDefinition)
                    return element.toString();
                else
                    return "     " + element.toString(); //$NON-NLS-1$
            }
        });
        target.addSelectionChangedListener(this);

        Label lblDelimiter = new Label(container, SWT.NONE);
        lblDelimiter.setText(Messages.CSVImportLabelDelimiter);
        Combo cmbDelimiter = new Combo(container, SWT.READ_ONLY);
        ComboViewer delimiter = new ComboViewer(cmbDelimiter);
        delimiter.setContentProvider(ArrayContentProvider.getInstance());
        delimiter.setInput(new Delimiter[] { new Delimiter(',', Messages.CSVImportSeparatorComma), //
                        new Delimiter(';', Messages.CSVImportSeparatorSemicolon), //
                        new Delimiter('\t', Messages.CSVImportSeparatorTab) });
        cmbDelimiter.select(1);
        delimiter.addSelectionChangedListener(this);

        Label lblSkipLines = new Label(container, SWT.NONE);
        lblSkipLines.setText(Messages.CSVImportLabelSkipLines);
        final Spinner skipLines = new Spinner(container, SWT.BORDER);
        skipLines.setMinimum(0);
        skipLines.addModifyListener(new ModifyListener()
        {
            @Override
            public void modifyText(ModifyEvent event)
            {
                onSkipLinesChanged(skipLines.getSelection());
            }
        });

        Label lblEncoding = new Label(container, SWT.NONE);
        lblEncoding.setText(Messages.CSVImportLabelEncoding);
        Combo cmbEncoding = new Combo(container, SWT.READ_ONLY);
        ComboViewer encoding = new ComboViewer(cmbEncoding);
        encoding.setContentProvider(ArrayContentProvider.getInstance());
        encoding.setInput(Charset.availableCharsets().values().toArray());
        encoding.setSelection(new StructuredSelection(Charset.defaultCharset()));
        encoding.addSelectionChangedListener(this);

        final Button firstLineIsHeader = new Button(container, SWT.CHECK);
        firstLineIsHeader.setText(Messages.CSVImportLabelFirstLineIsHeader);
        firstLineIsHeader.setSelection(true);
        firstLineIsHeader.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent event)
            {
                onFirstLineIsHeaderChanged(firstLineIsHeader.getSelection());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent event)
            {}
        });

        Composite compositeTable = new Composite(container, SWT.NONE);

        //
        // form layout
        //

        Label biggest = maxWidth(lblTarget, lblDelimiter, lblEncoding);

        FormData data = new FormData();
        data.top = new FormAttachment(cmbTarget, 0, SWT.CENTER);
        lblTarget.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(biggest, 5);
        data.right = new FormAttachment(100);
        cmbTarget.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(cmbDelimiter, 0, SWT.CENTER);
        lblDelimiter.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(cmbTarget, 5);
        data.left = new FormAttachment(cmbTarget, 0, SWT.LEFT);
        data.right = new FormAttachment(50, -5);
        cmbDelimiter.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(cmbEncoding, 0, SWT.CENTER);
        lblEncoding.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(cmbDelimiter, 5);
        data.left = new FormAttachment(cmbDelimiter, 0, SWT.LEFT);
        data.right = new FormAttachment(50, -5);
        cmbEncoding.setLayoutData(data);

        data = new FormData();
        data.left = new FormAttachment(50, 5);
        data.top = new FormAttachment(skipLines, 0, SWT.CENTER);
        lblSkipLines.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(cmbTarget, 5);
        data.left = new FormAttachment(lblSkipLines, 5);
        data.right = new FormAttachment(100, 0);
        skipLines.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(skipLines, 5);
        data.left = new FormAttachment(lblSkipLines, 0, SWT.LEFT);
        data.right = new FormAttachment(100, 0);
        firstLineIsHeader.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(cmbEncoding, 10);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        data.bottom = new FormAttachment(100, 0);
        data.width = 100;
        data.height = 100;
        compositeTable.setLayoutData(data);

        //
        // table & columns
        //

        TableColumnLayout layout = new TableColumnLayout();
        compositeTable.setLayout(layout);

        tableViewer = new TableViewer(compositeTable, SWT.BORDER);
        final Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        tableViewer.setLabelProvider(new ImportLabelProvider(importer));
        tableViewer.setContentProvider(new SimpleListContentProvider());

        table.addMouseListener(new MouseListener()
        {
            @Override
            public void mouseUp(MouseEvent e)
            {}

            @Override
            public void mouseDown(MouseEvent e)
            {}

            @Override
            public void mouseDoubleClick(MouseEvent e)
            {
                TableItem item = table.getItem(0);
                if (item == null)
                    return;

                int columnIndex = -1;
                for (int ii = 0; ii < table.getColumnCount(); ii++)
                {
                    Rectangle bounds = item.getBounds(ii);
                    int width = table.getColumn(ii).getWidth();

                    if (e.x >= bounds.x && e.x <= bounds.x + width)
                        columnIndex = ii;
                }

                if (columnIndex >= 0)
                    onColumnSelected(columnIndex);
            }
        });

        //
        // setup form elements
        //
        List<Object> targets = new ArrayList<Object>();
        for (CSVImportDefinition def : importer.getDefinitions())
        {
            targets.add(def);
            targets.addAll(def.getTargets(importer.getClient()));
        }
        target.setInput(targets);
        target.setSelection(new StructuredSelection(target.getElementAt(0)));
    }

    private Label maxWidth(Label... labels)
    {
        int width = 0;
        Label answer = null;

        for (int ii = 0; ii < labels.length; ii++)
        {
            int w = labels[ii].computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
            if (w >= width)
                answer = labels[ii];
        }

        return answer;
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event)
    {
        Object element = ((IStructuredSelection) event.getSelectionProvider().getSelection()).getFirstElement();

        if (element instanceof CSVImportDefinition)
        {
            onTargetChanged((CSVImportDefinition) element, null);
        }
        else if (element instanceof Delimiter)
        {
            importer.setDelimiter(((Delimiter) element).getDelimiter());
            doProcessFile();
        }
        else if (element instanceof Charset)
        {
            importer.setEncoding((Charset) element);
            doProcessFile();
        }
        else
        {
            // find import definition above selected item
            ComboViewer comboViewer = (ComboViewer) event.getSelectionProvider();
            List<?> items = (List<?>) comboViewer.getInput();
            CSVImportDefinition def = null;
            for (Object object : items)
            {
                if (object instanceof CSVImportDefinition)
                    def = (CSVImportDefinition) object;
                if (object == element)
                    break;
            }
            onTargetChanged(def, element);
        }

    }

    private void onTargetChanged(CSVImportDefinition def, Object target)
    {
        if (!def.equals(importer.getDefinition()))
        {
            importer.setDefinition(def);
            doProcessFile();
        }

        if (target != importer.getImportTarget())
        {
            importer.setImportTarget(target);
            doUpdateErrorMessages();
        }
    }

    private void onSkipLinesChanged(int linesToSkip)
    {
        importer.setSkipLines(linesToSkip);
        doProcessFile();
    }

    private void onFirstLineIsHeaderChanged(boolean isFirstLineHeader)
    {
        importer.setFirstLineHeader(isFirstLineHeader);
        doProcessFile();
    }

    private void onColumnSelected(int columnIndex)
    {
        ColumnConfigDialog dialog = new ColumnConfigDialog(getShell(), importer.getDefinition(),
                        importer.getColumns()[columnIndex]);
        dialog.open();

        doUpdateTable();
    }

    private void doProcessFile()
    {
        try
        {
            importer.processFile();

            tableViewer.getTable().setRedraw(false);

            for (TableColumn column : tableViewer.getTable().getColumns())
                column.dispose();

            TableColumnLayout layout = (TableColumnLayout) tableViewer.getTable().getParent().getLayout();
            for (final CSVImporter.Column header : importer.getColumns())
            {
                TableColumn column = new TableColumn(tableViewer.getTable(), SWT.None);
                column.setText(header.getLabel());
                layout.setColumnData(column, new ColumnPixelData(80, true));
            }

            tableViewer.setInput(importer.getRawValues());
            tableViewer.refresh();
            tableViewer.getTable().pack();
            for (TableColumn column : tableViewer.getTable().getColumns())
                column.pack();

            doUpdateTable();
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            ErrorDialog.openError(getShell(), Messages.LabelError, e.getMessage(), new Status(Status.ERROR,
                            PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));

        }
        finally
        {
            tableViewer.getTable().setRedraw(true);
        }
    }

    private void doUpdateTable()
    {
        Table table = tableViewer.getTable();
        table.setRedraw(false);

        try
        {
            for (int ii = 0; ii < table.getColumnCount(); ii++)
            {
                Column column = importer.getColumns()[ii];
                boolean isNumber = column.getField() instanceof AmountField;
                table.getColumn(ii).setAlignment(isNumber ? SWT.RIGHT : SWT.LEFT);
            }

            tableViewer.refresh();

            doUpdateErrorMessages();
        }
        finally
        {
            table.setRedraw(true);
        }
    }

    private void doUpdateErrorMessages()
    {
        Set<Field> fieldsToMap = new HashSet<Field>(importer.getDefinition().getFields());
        for (Column column : importer.getColumns())
            fieldsToMap.remove(column.getField());

        if (fieldsToMap.isEmpty())
        {
            setErrorMessage(null);
            setPageComplete(importer.getImportTarget() != null);
        }
        else
        {
            setErrorMessage(MessageFormat.format(Messages.CSVImportErrorMissingFields,
                            Arrays.toString(fieldsToMap.toArray()), fieldsToMap.size()));
            setPageComplete(false);
        }
    }

    private static final class ImportLabelProvider extends LabelProvider implements ITableLabelProvider,
                    ITableColorProvider
    {
        private static final RGB GREEN = new RGB(152, 251, 152);
        private static final RGB RED = new RGB(255, 127, 80);

        private CSVImporter importer;

        private final LocalResourceManager resources;

        private ImportLabelProvider(CSVImporter importer)
        {
            this.importer = importer;
            this.resources = new LocalResourceManager(JFaceResources.getResources());
        }

        @Override
        public void dispose()
        {
            this.resources.dispose();
            super.dispose();
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex)
        {
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex)
        {
            String[] line = (String[]) element;

            if (line != null && columnIndex < line.length)
                return line[columnIndex];

            return null;
        }

        @Override
        public Color getForeground(Object element, int columnIndex)
        {
            return null;
        }

        @Override
        public Color getBackground(Object element, int columnIndex)
        {
            Column column = importer.getColumns()[columnIndex];
            if (column.getField() == null)
                return null;

            try
            {
                if (column.getFormat() != null)
                {
                    String text = getColumnText(element, columnIndex);
                    if (text != null)
                        column.getFormat().getFormat().parseObject(text);
                }
                return resources.createColor(GREEN);
            }
            catch (ParseException e)
            {
                return resources.createColor(RED);
            }
        }
    }

    private static class ColumnConfigDialog extends Dialog implements ISelectionChangedListener
    {
        private static final Field EMPTY = new Field("---"); //$NON-NLS-1$

        private CSVImportDefinition definition;
        private Column column;

        protected ColumnConfigDialog(Shell parentShell, CSVImportDefinition definition, Column column)
        {
            super(parentShell);
            setShellStyle(getShellStyle() | SWT.SHEET);

            this.definition = definition;
            this.column = column;
        }

        @Override
        protected void configureShell(Shell shell)
        {
            super.configureShell(shell);
            shell.setText(Messages.CSVImportLabelEditMapping);
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            Composite composite = (Composite) super.createDialogArea(parent);

            Label label = new Label(composite, SWT.NONE);
            label.setText(Messages.CSVImportLabelEditMapping);

            ComboViewer mappedTo = new ComboViewer(composite, SWT.READ_ONLY);
            mappedTo.setContentProvider(ArrayContentProvider.getInstance());
            List<Field> fields = new ArrayList<Field>();
            fields.add(EMPTY);
            fields.addAll(definition.getFields());
            mappedTo.setInput(fields);

            final Composite details = new Composite(composite, SWT.NONE);
            final StackLayout layout = new StackLayout();
            details.setLayout(layout);

            final Composite emptyArea = new Composite(details, SWT.NONE);

            GridLayoutFactory glf = GridLayoutFactory.fillDefaults().margins(0, 0);

            final Composite dateArea = new Composite(details, SWT.NONE);
            glf.applyTo(dateArea);
            label = new Label(dateArea, SWT.NONE);
            label.setText(Messages.CSVImportLabelFormat);
            final ComboViewer dateFormats = new ComboViewer(dateArea, SWT.READ_ONLY);
            dateFormats.setContentProvider(ArrayContentProvider.getInstance());
            dateFormats.setInput(DateField.FORMATS);
            dateFormats.getCombo().select(0);
            dateFormats.addSelectionChangedListener(this);

            final Composite valueArea = new Composite(details, SWT.NONE);
            glf.applyTo(valueArea);
            label = new Label(valueArea, SWT.NONE);
            label.setText(Messages.CSVImportLabelFormat);
            final ComboViewer valueFormats = new ComboViewer(valueArea, SWT.READ_ONLY);
            valueFormats.setContentProvider(ArrayContentProvider.getInstance());
            valueFormats.setInput(AmountField.FORMATS);
            valueFormats.getCombo().select(0);
            valueFormats.addSelectionChangedListener(this);

            final Composite keyArea = new Composite(details, SWT.NONE);
            glf.applyTo(keyArea);
            final TableViewer tableViewer = new TableViewer(keyArea, SWT.FULL_SELECTION);
            tableViewer.setContentProvider(new KeyMappingContentProvider());
            tableViewer.setLabelProvider(new KeyMappingLabelProvider());
            tableViewer.getTable().setLinesVisible(true);
            tableViewer.getTable().setHeaderVisible(true);
            GridDataFactory.fillDefaults().grab(false, true).applyTo(tableViewer.getTable());

            new CellEditorFactory(tableViewer, KeyMappingContentProvider.Entry.class) //
                            .notify(new ModificationListener()
                            {
                                @Override
                                public void onModified(Object element, String property)
                                {
                                    tableViewer.refresh(element);
                                }
                            }) //
                            .readonly("key") //$NON-NLS-1$
                            .editable("value") //$NON-NLS-1$
                            .apply();

            TableColumn col = new TableColumn(tableViewer.getTable(), SWT.NONE);
            col.setText(Messages.CSVImportLabelExpectedValue);
            col.setWidth(100);

            col = new TableColumn(tableViewer.getTable(), SWT.NONE);
            col.setText(Messages.CSVImportLabelProvidedValue);
            col.setWidth(100);

            layout.topControl = emptyArea;

            mappedTo.addSelectionChangedListener(new ISelectionChangedListener()
            {
                @Override
                public void selectionChanged(SelectionChangedEvent event)
                {
                    Field field = (Field) ((IStructuredSelection) event.getSelection()).getFirstElement();

                    if (field != column.getField())
                        column.setField(field != EMPTY ? field : null);

                    if (field instanceof DateField)
                    {
                        layout.topControl = dateArea;
                        if (column.getFormat() != null)
                            dateFormats.setSelection(new StructuredSelection(column.getFormat()));
                        else
                            dateFormats.setSelection(new StructuredSelection(dateFormats.getElementAt(0)));
                    }
                    else if (field instanceof AmountField)
                    {
                        layout.topControl = valueArea;
                        if (column.getFormat() != null)
                            valueFormats.setSelection(new StructuredSelection(column.getFormat()));
                        else
                            valueFormats.setSelection(new StructuredSelection(valueFormats.getElementAt(0)));
                    }
                    else if (field instanceof EnumField)
                    {
                        layout.topControl = keyArea;

                        EnumField<?> ef = (EnumField<?>) field;

                        FieldFormat f = column.getFormat();
                        if (f == null || !(f.getFormat() instanceof EnumMapFormat))
                        {
                            f = new FieldFormat(null, ef.createFormat());
                            column.setFormat(f);
                        }

                        tableViewer.setInput((EnumMapFormat<?>) f.getFormat());
                    }
                    else
                    {
                        layout.topControl = emptyArea;
                    }

                    details.layout();
                }
            });

            if (this.column.getField() != null)
            {
                mappedTo.setSelection(new StructuredSelection(this.column.getField()));
            }
            else
            {
                mappedTo.getCombo().select(0);
            }

            return composite;
        }

        @Override
        public void selectionChanged(SelectionChangedEvent event)
        {
            FieldFormat format = (FieldFormat) ((IStructuredSelection) event.getSelectionProvider().getSelection())
                            .getFirstElement();
            column.setFormat(format != null ? format : null);
        }
    }

    private static class KeyMappingContentProvider implements IStructuredContentProvider
    {
        /* Map.Entry#setValue is not backed by EnumMap :-( */
        public final static class Entry<M extends Enum<M>> implements Comparable<Entry<M>>
        {
            private EnumMap<M, String> map;
            private M key;

            private Entry(EnumMap<M, String> map, M key)
            {
                this.map = map;
                this.key = key;
            }

            public String getKey()
            {
                return key.name();
            }

            public String getValue()
            {
                return map.get(key);
            }

            @SuppressWarnings("unused")
            public void setValue(String value)
            {
                map.put(key, value);
            }

            @Override
            public int compareTo(Entry<M> other)
            {
                return key.name().compareTo(other.key.name());
            }
        }

        private EnumMapFormat<?> mapFormat;

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
            this.mapFormat = (EnumMapFormat<?>) newInput;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public Object[] getElements(Object inputElement)
        {
            if (mapFormat == null)
                return new Object[0];

            Entry<?>[] elements = new Entry[mapFormat.map().size()];

            int ii = 0;
            for (Enum<?> entry : mapFormat.map().keySet())
                elements[ii++] = new Entry(mapFormat.map(), entry);
            Arrays.sort(elements);
            return elements;
        }

        @Override
        public void dispose()
        {}
    }

    public static class KeyMappingLabelProvider extends BaseLabelProvider implements ITableLabelProvider
    {

        @Override
        public String getColumnText(Object element, int columnIndex)
        {
            @SuppressWarnings("unchecked")
            KeyMappingContentProvider.Entry<? extends Enum<?>> entry = (KeyMappingContentProvider.Entry<? extends Enum<?>>) element;
            return columnIndex == 0 ? entry.getKey() : entry.getValue();
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex)
        {
            return null;
        }
    }
}
