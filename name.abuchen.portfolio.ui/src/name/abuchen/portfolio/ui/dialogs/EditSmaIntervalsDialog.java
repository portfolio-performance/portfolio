package name.abuchen.portfolio.ui.dialogs;

import java.util.Arrays;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColorCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.IntervalSettings;
import name.abuchen.portfolio.model.IntervalSettings.IntervalSetting;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.util.ColorConversion;

public class EditSmaIntervalsDialog extends Dialog
{    
    private final IntervalSettings intervals;
    
    public EditSmaIntervalsDialog(Shell parentShell, IntervalSettings smaIntervals)
    {
        super(parentShell);

        // create copy of settings to work on them
        intervals = new IntervalSettings();        
        for(IntervalSettings.IntervalSetting i : smaIntervals.getAll())
            intervals.add(i.getInterval(), i.getRGB(), i.getIsActive());
    }

    @Override
    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText(Messages.LabelClientFilter);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }
    
    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite container = (Composite) super.createDialogArea(parent);
        
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        container.setLayout(gridLayout);
        
        final TableViewer table = new TableViewer(container, SWT.FULL_SELECTION);
        
        createIntervalColumn(table);
        createColorColumn(table);
                
        table.getTable().setHeaderVisible(true);
        table.getTable().setLinesVisible(true);
        table.setContentProvider(new ArrayContentProvider());
        
        updateTable(table);
        
        final Composite buttonArea = new Composite(container, SWT.NONE);
        buttonArea.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        final GridLayout gridLayoutButton = new GridLayout();
        gridLayoutButton.numColumns = 1;
        buttonArea.setLayout(gridLayoutButton);
        createAddButton(buttonArea, table);
        createRemoveButton(buttonArea, table);        
        
        return container;
    } 
        
    private void createAddButton(Composite container, TableViewer table)
    {
        final Button addButton = new Button(container, SWT.FLAT | SWT.PUSH);
        addButton.setImage(Images.ADD.image());
        addButton.setToolTipText(Messages.EditSmaIntervalDialog_AddIntervalLabel);
        addButton.addListener(SWT.MouseUp, e -> 
        {
            if (e.type == SWT.MouseUp) 
            {
                InputDialog dlg = new InputDialog(this.getShell(), Messages.EditSmaIntervalDialog_InsertIntervalLabel, Messages.EditSmaIntervalDialog_InsertIntervalLabel, "", new IntervalValidator(intervals.getAll())); //$NON-NLS-1$
                if(dlg.open() == Window.OK)
                {
                    intervals.add(Integer.parseInt(dlg.getValue()), new RGB(107, 179, 143), false);
                    updateTable(table);
                }
            }
        });
    }
    
    private class IntervalValidator implements IInputValidator
    {
        final IntervalSetting[] existingIntervals;
        
        public IntervalValidator(IntervalSetting[] intervalSettings)
        {
            this.existingIntervals = intervalSettings;
        }

        @Override
        public String isValid(String newText)
        {
            int tempInterval = -1;
            try
            {
                tempInterval = Integer.parseInt(newText);
            }
            catch(NumberFormatException ex)
            {
                return Messages.EditSmaIntervalDialog_ValidateEnterNumberLabel;
            }
            
            final int parsedInterval = tempInterval;            
            if(parsedInterval <= 0)
            {
                return Messages.EditSmaIntervalDialog_ValidateEnterPositiveNumberLabel;
            }
            
            if(Arrays.stream(existingIntervals).anyMatch(i -> i.getInterval() == parsedInterval))
            {
                return Messages.EditSmaIntervalDialog_ValidateIntervalAlreadyExistsLabel;
            }
            return null;
        }
    }
    
    private void createRemoveButton(Composite container, TableViewer table)
    {
        final Button removeButton = new Button(container, SWT.FLAT | SWT.PUSH);
        removeButton.setEnabled(false); // disable by default. will be enabled if row is selected
        removeButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        removeButton.setImage(Images.REMOVE.image());        
        removeButton.setToolTipText(Messages.EditSmaIntervalDialog_DeleteIntervalLabel);
        removeButton.addListener(SWT.MouseUp, e -> 
        {
            if (e.type == SWT.MouseUp) 
            {
                IntervalSettings.IntervalSetting interval = (IntervalSettings.IntervalSetting)table.getStructuredSelection().getFirstElement();
                table.remove(interval);
                intervals.remove(interval.getInterval());                
            }
        });
        
        table.addSelectionChangedListener(event -> removeButton.setEnabled(event.getStructuredSelection().size() == 1));
    }
       
    private void updateTable(TableViewer table)
    {
        table.getTable().removeAll();        
        
        IntervalSettings.IntervalSetting[] temp = intervals.getAll();
        Arrays.sort(temp, new IntervalSettings.SortIntervalSetting());
        table.setInput(temp);
    }

    private void createIntervalColumn(TableViewer table)
    {       
        TableViewerColumn colInterval = new TableViewerColumn(table, SWT.NONE);
        colInterval.getColumn().setWidth(80);
        colInterval.getColumn().setText(Messages.EditSmaIntervalDialog_IntervalLabel);
        colInterval.setLabelProvider(new ColumnLabelProvider() 
        {
            @Override
            public String getText(Object element) 
            {
                return String.valueOf(((IntervalSettings.IntervalSetting)element).getInterval());
            }
        });
    }
    
    private void createColorColumn(TableViewer table)
    {
        TableViewerColumn colColor = new TableViewerColumn(table, SWT.NONE);
        colColor.getColumn().setWidth(160);
        colColor.getColumn().setText(Messages.EditSmaIntervalDialog_ColorLabel);
        colColor.setEditingSupport(new ColorEditingSupport(table));
        colColor.setLabelProvider(new ColumnLabelProvider() 
        {
            @Override
            public String getText(Object element) 
            {
                return null;
            }
            
            @Override
            public String getToolTipText(Object element) 
            {
                IntervalSettings.IntervalSetting i = (IntervalSettings.IntervalSetting) element;
                return ColorConversion.toHex(i.getRGB());
            }
            
            @Override
            public Color getBackground(Object element) 
            {
                IntervalSettings.IntervalSetting i = (IntervalSettings.IntervalSetting) element;
                return new Color(i.getRGB());
            }
        });        
    }

    public IntervalSettings.IntervalSetting[] getIntervals()
    {
        return intervals.getAll();
    }

    
    private class ColorEditingSupport extends EditingSupport
    {
        private final TableViewer viewer;       
        private final CellEditor editor;

        public ColorEditingSupport(TableViewer viewer) 
        {
            super(viewer);
            this.viewer = viewer;
            this.editor = new ColorCellEditor(viewer.getTable());
        }

        @Override
        protected CellEditor getCellEditor(Object element) 
        {
            return editor;
        }

        @Override
        protected boolean canEdit(Object element) 
        {
            return true;
        }

        @Override
        protected Object getValue(Object element) 
        {
            return ((IntervalSettings.IntervalSetting) element).getRGB();
        }

        @Override
        protected void setValue(Object element, Object userInputValue) 
        {
            ((IntervalSettings.IntervalSetting) element).setRGB((RGB)userInputValue);
            viewer.update(element, null);
        }
        
    }
    
}
