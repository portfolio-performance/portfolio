package name.abuchen.portfolio.ui.wizards.datatransfer;

import static name.abuchen.portfolio.ui.util.SWTHelper.widest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
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
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.csv.CSVConfig;
import name.abuchen.portfolio.datatransfer.csv.CSVConfigManager;
import name.abuchen.portfolio.datatransfer.csv.CSVExtractor;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.AmountField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Column;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.DateField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.EnumField;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.EnumMapFormat;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.Field;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.FieldFormat;
import name.abuchen.portfolio.datatransfer.csv.CSVImporter.ISINField;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupportWrapper;
import name.abuchen.portfolio.ui.util.viewers.StringEditingSupport;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;
import name.abuchen.portfolio.util.TextUtil;

public class CSVImportDefinitionPage extends AbstractWizardPage
{
    private final Client client;
    private final CSVImporter importer;

    /**
     * The label of any selected configuration. It is used (a) as indicator that
     * the number of columns should not be re-created and (b) as a proposal when
     * saving the configuration.
     */
    private String currentConfigLabel;

    private CSVConfigManager configManager;
    private final boolean onlySecurityPrices;

    private TableViewer tableViewer;
    private Menu configurationDropDownMenu;

    private DataBindingContext context;

    public CSVImportDefinitionPage(Client client, CSVImporter importer, CSVConfigManager configManager,
                    boolean onlySecurityPrices)
    {
        super("importdefinition"); //$NON-NLS-1$
        setTitle(Messages.CSVImportWizardTitle);
        setDescription(Messages.CSVImportWizardDescription);

        this.client = client;
        this.importer = importer;
        this.configManager = configManager;
        this.onlySecurityPrices = onlySecurityPrices;

        importer.setExtractor(
                        onlySecurityPrices ? importer.getSecurityPriceExtractor() : importer.getExtractors().get(0));

        this.context = new DataBindingContext();
    }

    public CSVImporter getImporter()
    {
        return importer;
    }

    public void setInitialConfiguration(CSVConfig config)
    {
        currentConfigLabel = Objects.requireNonNull(config).getLabel();
        config.writeTo(importer);
    }

    @Override
    public IWizardPage getNextPage()
    {
        if (onlySecurityPrices)
            return null;

        if (importer.getExtractor() == importer.getSecurityPriceExtractor())
            return getWizard().getPage(SelectSecurityPage.PAGE_ID);
        else
            return getWizard().getPage(CSVImportWizard.REVIEW_PAGE_ID);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new FormLayout());

        Label lblExtractor = new Label(container, SWT.RIGHT);
        lblExtractor.setText(Messages.CSVImportLabelTarget);
        ComboViewer extractor = createExtractorComboViewer(container);

        Button button = new Button(container, SWT.NONE);
        button.setImage(Images.CONFIG.image());
        button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> onConfigurationDropDown(button)));

        Label lblDelimiter = new Label(container, SWT.NONE);
        lblDelimiter.setText(Messages.CSVImportLabelDelimiter);
        ComboViewer delimiter = createDelimiterComboViewer(container);

        Label lblSkipLines = new Label(container, SWT.NONE);
        lblSkipLines.setText(Messages.CSVImportLabelSkipLines);
        final Spinner skipLines = new Spinner(container, SWT.BORDER);
        skipLines.setMinimum(0);

        IObservableValue<?> spinnerTarget = WidgetProperties.selection().observe(skipLines);
        @SuppressWarnings("unchecked")
        IObservableValue<String> spinnerModel = BeanProperties.value("skipLines").observe(importer); //$NON-NLS-1$
        context.bindValue(spinnerTarget, spinnerModel);

        skipLines.addModifyListener(event -> doProcessFile());

        Label lblEncoding = new Label(container, SWT.NONE);
        lblEncoding.setText(Messages.CSVImportLabelEncoding);
        Combo cmbEncoding = new Combo(container, SWT.READ_ONLY);
        ComboViewer encoding = new ComboViewer(cmbEncoding);
        encoding.setContentProvider(ArrayContentProvider.getInstance());
        encoding.setInput(Charset.availableCharsets().values().toArray());

        IObservableValue<?> encodingTarget = ViewersObservables.observeSingleSelection(encoding);
        @SuppressWarnings("unchecked")
        IObservableValue<?> encodingModel = BeanProperties.value("encoding").observe(importer); //$NON-NLS-1$
        context.bindValue(encodingTarget, encodingModel);
        encoding.addSelectionChangedListener(event -> doProcessFile());

        final Button firstLineIsHeader = new Button(container, SWT.CHECK);
        firstLineIsHeader.setText(Messages.CSVImportLabelFirstLineIsHeader);

        IObservableValue<?> targetObservable = WidgetProperties.selection().observe(firstLineIsHeader);
        @SuppressWarnings("unchecked")
        IObservableValue<?> modelObservable = BeanProperties.value("firstLineHeader").observe(importer); //$NON-NLS-1$
        context.bindValue(targetObservable, modelObservable);

        firstLineIsHeader.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> doProcessFile()));

        Composite compositeTable = new Composite(container, SWT.NONE);

        //
        // form layout
        //

        int width = widest(lblExtractor, lblDelimiter, lblEncoding);

        FormDataFactory.startingWith(lblExtractor).width(width).top(new FormAttachment(0, 5))
                        .thenRight(extractor.getCombo()).right(new FormAttachment(50, -5))
                        .thenBelow(delimiter.getCombo()).label(lblDelimiter).right(new FormAttachment(50, -5))
                        .thenBelow(cmbEncoding).label(lblEncoding).right(new FormAttachment(50, -5));

        FormDataFactory.startingWith(extractor.getCombo()).thenRight(button);

        FormDataFactory.startingWith(delimiter.getCombo()).thenRight(lblSkipLines).suffix(skipLines);

        FormDataFactory.startingWith(cmbEncoding).thenRight(firstLineIsHeader);

        FormData data = new FormData();
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

        tableViewer = new TableViewer(compositeTable, SWT.BORDER | SWT.FULL_SELECTION);
        final Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        tableViewer.setLabelProvider(new ImportLabelProvider(importer));
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());

        table.addMouseListener(MouseListener.mouseDoubleClickAdapter(e -> {
            TableItem item = table.getItem(0);
            if (item == null)
                return;

            int columnIndex = -1;
            for (int ii = 0; ii < table.getColumnCount(); ii++)
            {
                Rectangle bounds = item.getBounds(ii);
                int w = table.getColumn(ii).getWidth();

                if (e.x >= bounds.x && e.x <= bounds.x + w)
                    columnIndex = ii;
            }

            if (columnIndex >= 0)
                onColumnSelected(columnIndex);
        }));

        //
        // setup form elements
        //
        doProcessFile();
    }

    private ComboViewer createExtractorComboViewer(Composite container)
    {
        Combo cmbExtractor = new Combo(container, SWT.READ_ONLY);
        ComboViewer extractor = new ComboViewer(cmbExtractor);
        extractor.setContentProvider(ArrayContentProvider.getInstance());
        extractor.setInput(importer.getExtractors());
        extractor.getCombo().setEnabled(!onlySecurityPrices);

        extractor.setLabelProvider(new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Extractor) element).getLabel();
            }
        });

        IObservableValue<?> targetObservable = ViewersObservables.observeSingleSelection(extractor);
        @SuppressWarnings("unchecked")
        IObservableValue<?> modelObservable = BeanProperties.value("extractor").observe(importer); //$NON-NLS-1$
        context.bindValue(targetObservable, modelObservable);
        extractor.addSelectionChangedListener(e -> onTargetChanged());

        return extractor;
    }

    private ComboViewer createDelimiterComboViewer(Composite container)
    {
        Combo cmbDelimiter = new Combo(container, SWT.READ_ONLY);
        ComboViewer delimiter = new ComboViewer(cmbDelimiter);
        delimiter.setContentProvider(ArrayContentProvider.getInstance());
        delimiter.setInput(Delimiter.AVAILABLE);

        UpdateValueStrategy<Delimiter, Character> targetToModel = new UpdateValueStrategy<>();
        targetToModel.setConverter(new Delimiter.DelimiterToCharConverter());

        UpdateValueStrategy<Character, Delimiter> modelToTarget = new UpdateValueStrategy<>();
        modelToTarget.setConverter(new Delimiter.CharToDelimiterConverter());

        @SuppressWarnings("unchecked")
        IObservableValue<Delimiter> delimiterTarget = ViewersObservables.observeSingleSelection(delimiter);
        @SuppressWarnings("unchecked")
        IObservableValue<Character> delimiterModel = BeanProperties.value("delimiter").observe(importer); //$NON-NLS-1$
        context.bindValue(delimiterTarget, delimiterModel, targetToModel, modelToTarget);
        delimiter.addSelectionChangedListener(e -> doProcessFile());
        return delimiter;
    }

    private void onTargetChanged()
    {
        // important: by changing the extractor, there still might be old (now
        // invalid) column configurations. We could just force the
        // #doProcessFile to re-read the columns, however, we would loose
        // whatever the user has manually configured. By reading and writing via
        // CSVConfig, we keep only valid column configurations.

        CSVConfig config = new CSVConfig();
        config.readFrom(importer);
        config.writeTo(importer);

        doProcessFile(false);
    }

    private void onColumnSelected(int columnIndex)
    {
        ColumnConfigDialog dialog = new ColumnConfigDialog(client, getShell(), importer,
                        importer.getColumns()[columnIndex]);
        dialog.open();

        doUpdateTable();
    }

    private void onConfigurationDropDown(Button button)
    {
        if (configurationDropDownMenu == null)
        {
            MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
            menuMgr.setRemoveAllWhenShown(true);
            menuMgr.addMenuListener(manager -> {

                if (currentConfigLabel != null)
                {
                    manager.add(new SimpleAction(Messages.CSVConfigRereadColumnsFromFile, a -> onRereadColumns()));
                    manager.add(new Separator());
                }

                manager.add(new LabelOnly(Messages.CSVConfigLabelBuiltIn));
                configManager.getBuiltInConfigurations().forEach(config -> manager
                                .add(new SimpleAction(labelFor(config), a -> onConfigSelected(config))));

                manager.add(new Separator());
                manager.add(new LabelOnly(Messages.CSVConfigLabelUserSpecific));
                configManager.getUserSpecificConfigurations().forEach(config -> manager
                                .add(new SimpleAction(labelFor(config), a -> onConfigSelected(config))));

                manager.add(new Separator());
                manager.add(new SimpleAction(Messages.CSVConfigSave, a -> onConfigSaved()));
                MenuManager subMenu = new MenuManager(Messages.CSVConfigDelete);
                manager.add(subMenu);

                manager.add(new Separator());
                configManager.getUserSpecificConfigurations().forEach(config -> subMenu
                                .add(new SimpleAction(config.getLabel(), a -> onConfigDeleted(config))));
                manager.add(new SimpleAction(Messages.CSVConfigExport, a -> onConfigExport()));
                manager.add(new SimpleAction(Messages.CSVConfigImport, a -> onConfigImport()));

            });

            configurationDropDownMenu = menuMgr.createContextMenu(button);
            button.addDisposeListener(e -> configurationDropDownMenu.dispose());
        }

        configurationDropDownMenu.setVisible(true);
    }

    private String labelFor(CSVConfig config)
    {
        Optional<CSVExtractor> extractor = importer.getExtractorByCode(config.getTarget());
        return extractor.isPresent() ? config.getLabel() + " (" + extractor.get().getLabel() + ")" : config.getLabel(); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void onRereadColumns()
    {
        this.currentConfigLabel = null;
        doProcessFile();
    }

    private void onConfigSelected(CSVConfig config)
    {
        currentConfigLabel = config.getLabel();
        config.writeTo(importer);
        doProcessFile();
    }

    private void onConfigDeleted(CSVConfig config)
    {
        configManager.removeUserSpecificConfiguration(config);

        // reread columns if the current configuration is deleted. There might
        // be multiple configurations with the same name. It must be the same
        // label.
        if (config.getLabel() == currentConfigLabel) // NOSONAR
            onRereadColumns();
    }

    private void onConfigSaved()
    {
        InputDialog dlg = new InputDialog(Display.getDefault().getActiveShell(), Messages.CSVConfigDialogTitleSave,
                        Messages.ColumnName, currentConfigLabel != null ? currentConfigLabel : null, null);
        if (dlg.open() != InputDialog.OK)
            return;

        CSVConfig config = new CSVConfig();
        config.setLabel(dlg.getValue());
        config.readFrom(importer);

        configManager.addUserSpecificConfiguration(config);
        this.currentConfigLabel = config.getLabel();
    }

    private void onConfigExport()
    {
        FileDialog dialog = new FileDialog(getControl().getShell(), SWT.SAVE);

        String proposal = TextUtil.sanitizeFilename(currentConfigLabel != null ? currentConfigLabel : "csv-config"); //$NON-NLS-1$
        dialog.setFileName(proposal + ".json");//$NON-NLS-1$
        dialog.setOverwrite(true);
        String fileName = dialog.open();
        if (fileName == null)
            return;

        File file = new File(fileName);

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
        {
            CSVConfig config = new CSVConfig();
            config.setLabel(currentConfigLabel != null ? currentConfigLabel : file.getName());
            config.readFrom(importer);

            JSONValue.writeJSONString(config.toJSON(), writer);
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(getControl().getShell(), Messages.ExportWizardErrorExporting, e.getMessage());
        }
    }

    private void onConfigImport()
    {
        FileDialog fileDialog = new FileDialog(getControl().getShell(), SWT.OPEN);
        fileDialog.setFilterNames(
                        new String[] { Messages.CSVConfigCSVImportLabelFileJSON, Messages.CSVImportLabelFileAll }); // Messages.CSVImportLabelFileCSV
        fileDialog.setFilterExtensions(new String[] { "*.json", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
        String fileName = fileDialog.open();

        if (fileName == null)
            return;

        File file = new File(fileName);

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))
        {
            JSONObject jsonObject = (JSONObject) JSONValue.parseWithException(reader);

            CSVConfig config = new CSVConfig();
            config.fromJSON(jsonObject);

            configManager.addUserSpecificConfiguration(config);

            onConfigSelected(config);
        }
        catch (IOException | org.json.simple.parser.ParseException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(getControl().getShell(), Messages.ExportWizardErrorExporting, e.getMessage());
        }
    }

    private void doProcessFile()
    {
        doProcessFile(currentConfigLabel == null);
    }

    private void doProcessFile(boolean remap)
    {
        try
        {
            importer.processFile(remap);

            tableViewer.getTable().setRedraw(false);

            for (TableColumn column : tableViewer.getTable().getColumns())
                column.dispose();

            TableColumnLayout layout = (TableColumnLayout) tableViewer.getTable().getParent().getLayout();
            for (Column column : importer.getColumns())
            {
                TableColumn tableColumn = new TableColumn(tableViewer.getTable(), SWT.None);
                layout.setColumnData(tableColumn, new ColumnPixelData(80, true));
                setColumnLabel(tableColumn, column);
            }

            List<Object> input = new ArrayList<>();
            input.add(importer);
            input.addAll(importer.getRawValues());
            tableViewer.setInput(input);
            tableViewer.refresh();
            tableViewer.getTable().pack();

            // see #1723 and #1536: under Linux, the first column is extended to
            // the full size of the table

            if (!Platform.getWS().equals(Platform.WS_GTK))
                for (TableColumn column : tableViewer.getTable().getColumns())
                    column.pack();

            doUpdateErrorMessages();
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            ErrorDialog.openError(getShell(), Messages.LabelError, e.getMessage(),
                            new Status(Status.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));

        }
        finally
        {
            tableViewer.getTable().setRedraw(true);
        }
    }

    private void setColumnLabel(TableColumn tableColumn, Column column)
    {
        tableColumn.setText(column.getLabel());
        tableColumn.setAlignment(column.getField() instanceof AmountField ? SWT.RIGHT : SWT.LEFT);
    }

    private void doUpdateTable()
    {
        Table table = tableViewer.getTable();
        table.setRedraw(false);

        try
        {
            for (int ii = 0; ii < table.getColumnCount(); ii++)
                setColumnLabel(table.getColumn(ii), importer.getColumns()[ii]);

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
        Set<Field> fieldsToMap = new HashSet<>(importer.getExtractor().getFields());
        for (Column column : importer.getColumns())
            fieldsToMap.remove(column.getField());

        if (fieldsToMap.isEmpty())
        {
            setMessage(null);
            setPageComplete(true);
        }
        else
        {
            String required = fieldsToMap.stream().filter(f -> !f.isOptional()).map(Field::getName)
                            .collect(Collectors.joining(", ")); //$NON-NLS-1$

            String optional = fieldsToMap.stream().filter(Field::isOptional).map(Field::getName)
                            .collect(Collectors.joining(", ")); //$NON-NLS-1$

            boolean onlyOptional = required.length() == 0;

            setPageComplete(onlyOptional);

            StringBuilder message = new StringBuilder();
            if (required.length() > 0)
                message.append(MessageFormat.format(Messages.CSVImportErrorMissingFields, required)).append("\n"); //$NON-NLS-1$
            if (optional.length() > 0)
                message.append(MessageFormat.format(Messages.CSVImportInformationOptionalFields, optional));

            setMessage(message.toString(), onlyOptional ? IMessageProvider.INFORMATION : IMessageProvider.ERROR);
        }
    }

    private static final class ImportLabelProvider extends LabelProvider
                    implements ITableLabelProvider, ITableColorProvider
    {
        private static final Color GREEN = Colors.getColor(163, 215, 113);
        private static final Color LIGHTGREEN = Colors.getColor(188, 226, 158);
        private static final Color ERROR = Colors.getColor(255, 152, 89);

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
            if (element instanceof CSVImporter)
            {
                Column column = importer.getColumns()[columnIndex];

                if (column.getField() == null)
                    return Messages.CSVImportLabelDoubleClickHere;
                else
                    return MessageFormat.format(Messages.CSVImportLabelMappedToField, column.getField().getName());
            }
            else
            {
                String[] line = (String[]) element;

                if (line != null && columnIndex < line.length)
                    return line[columnIndex];
            }
            return null;
        }

        @Override
        public Color getForeground(Object element, int columnIndex)
        {
            return element instanceof CSVImporter ? Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY) : null;
        }

        @Override
        public Color getBackground(Object element, int columnIndex)
        {
            if (element instanceof CSVImporter)
                return null;

            Column column = importer.getColumns()[columnIndex];
            if (column.getField() == null)
                return null;

            try
            {
                if (column.getFormat() != null)
                {
                    String text = getColumnText(element, columnIndex);
                    if (text != null && !text.isEmpty())
                    {
                        column.getFormat().getFormat().parseObject(TextUtil.stripNonNumberCharacters(text));
                        return GREEN;
                    }
                }

                return column.getField().isOptional() ? LIGHTGREEN : GREEN;
            }
            catch (ParseException e)
            {
                return column.getField().isOptional() ? Colors.theme().warningBackground() : ERROR;
            }
        }
    }

    private static class ColumnConfigDialog extends Dialog implements ISelectionChangedListener
    {
        private static final Field EMPTY = new Field("#", "---"); //$NON-NLS-1$ //$NON-NLS-2$

        private CSVImporter importer;
        private Column column;
        private final Client client;

        protected ColumnConfigDialog(Client client, Shell parentShell, CSVImporter importer, Column column)
        {
            super(parentShell);
            setShellStyle(getShellStyle() | SWT.SHEET);

            this.client = client;
            this.importer = importer;
            this.column = column;
        }

        @Override
        protected void configureShell(Shell shell)
        {
            super.configureShell(shell);
            shell.setText(Messages.CSVImportLabelEditMapping);
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent)
        {
            // do not create a CANCEL button as it implies that the user could
            // cancel the operation. However, since we edit the original
            // configuration immediately, cancellation is not possible.
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            Composite composite = (Composite) super.createDialogArea(parent);

            Label label = new Label(composite, SWT.NONE);
            label.setText(Messages.CSVImportLabelEditMapping);

            ComboViewer mappedTo = new ComboViewer(composite, SWT.READ_ONLY);
            mappedTo.setContentProvider(ArrayContentProvider.getInstance());
            List<Field> fields = new ArrayList<>();
            fields.add(EMPTY);
            fields.addAll(importer.getExtractor().getFields());
            mappedTo.setInput(fields);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(mappedTo.getControl());

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
            dateFormats.getCombo().select(0);
            dateFormats.addSelectionChangedListener(this);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(dateFormats.getControl());

            final Composite valueArea = new Composite(details, SWT.NONE);
            glf.applyTo(valueArea);
            label = new Label(valueArea, SWT.NONE);
            label.setText(Messages.CSVImportLabelFormat);
            final ComboViewer valueFormats = new ComboViewer(valueArea, SWT.READ_ONLY);
            valueFormats.setContentProvider(ArrayContentProvider.getInstance());
            valueFormats.getCombo().select(0);
            valueFormats.addSelectionChangedListener(this);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(valueFormats.getControl());

            final Composite keyArea = new Composite(details, SWT.NONE);
            glf.applyTo(keyArea);
            final TableViewer tableViewer = new TableViewer(keyArea, SWT.FULL_SELECTION);
            tableViewer.setContentProvider(new KeyMappingContentProvider());
            tableViewer.getTable().setLinesVisible(true);
            tableViewer.getTable().setHeaderVisible(true);
            GridDataFactory.fillDefaults().grab(false, true).minSize(SWT.DEFAULT, 100).applyTo(tableViewer.getTable());

            TableViewerColumn col = new TableViewerColumn(tableViewer, SWT.NONE);
            col.getColumn().setText(Messages.CSVImportLabelExpectedValue);
            col.getColumn().setWidth(100);
            col.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    return ((KeyMappingContentProvider.Entry<?>) element).getKey();
                }
            });

            col = new TableViewerColumn(tableViewer, SWT.NONE);
            col.getColumn().setText(Messages.CSVImportLabelProvidedValue);
            col.getColumn().setWidth(100);
            col.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    return ((KeyMappingContentProvider.Entry<?>) element).getValue();
                }
            });

            ColumnEditingSupport.prepare(tableViewer);
            col.setEditingSupport(new ColumnEditingSupportWrapper(tableViewer,
                            new StringEditingSupport(KeyMappingContentProvider.Entry.class, "value"))); //$NON-NLS-1$

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
                        dateFormats.setInput(field.getAvailableFieldFormats());
                        if (column.getFormat() != null)
                            dateFormats.setSelection(new StructuredSelection(column.getFormat()));
                        else
                            dateFormats.setSelection(new StructuredSelection(
                                            field.guessFormat(client, importer.getFirstNonEmptyValue(column))));
                    }
                    else if (field instanceof AmountField)
                    {
                        layout.topControl = valueArea;
                        valueFormats.setInput(field.getAvailableFieldFormats());
                        if (column.getFormat() != null)
                            valueFormats.setSelection(new StructuredSelection(column.getFormat()));
                        else
                            valueFormats.setSelection(new StructuredSelection(
                                            field.guessFormat(client, importer.getFirstNonEmptyValue(column))));
                    }
                    else if (field instanceof ISINField)
                    {
                        column.setFormat(field.guessFormat(client, importer.getFirstNonEmptyValue(column)));
                    }
                    else if (field instanceof EnumField)
                    {
                        layout.topControl = keyArea;

                        FieldFormat f = column.getFormat();
                        if (f == null || !(f.getFormat() instanceof EnumMapFormat))
                        {
                            f = field.guessFormat(client, null);
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
        public static final class Entry<M extends Enum<M>>
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
                return key.toString();
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

            List<Entry<?>> elements = new ArrayList<>();

            for (Enum<?> entry : mapFormat.map().keySet())
                elements.add(new Entry(mapFormat.map(), entry));

            Collections.sort(elements, (e1, e2) -> e1.key.name().compareToIgnoreCase(e2.key.name()));

            return elements.toArray();
        }

        @Override
        public void dispose()
        {
            // nothing to do
        }
    }
}
