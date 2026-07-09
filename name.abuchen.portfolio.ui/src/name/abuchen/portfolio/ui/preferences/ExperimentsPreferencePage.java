package name.abuchen.portfolio.ui.preferences;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.IniFileManipulator;

public class ExperimentsPreferencePage extends FieldEditorPreferencePage
{
    private static final String SWT_AUTOSCALE = "swt.autoScale"; //$NON-NLS-1$
    private static final String SWT_AUTOSCALE_DISABLED = "false"; //$NON-NLS-1$
    private static final String SWT_AUTOSCALE_75_PERCENT = "75"; //$NON-NLS-1$
    private static final String SWT_AUTOSCALE_125_PERCENT = "125"; //$NON-NLS-1$
    private static final String SWT_AUTOSCALE_150_PERCENT = "150"; //$NON-NLS-1$
    private static final String SWT_AUTOSCALE_175_PERCENT = "175"; //$NON-NLS-1$
    private static final String SWT_AUTOSCALE_200_PERCENT = "200"; //$NON-NLS-1$

    private Combo autoScaleCombo;
    private IniFileManipulator iniFileManipulator;
    private String autoScaleValue;
    private boolean iniFileAvailable;

    public ExperimentsPreferencePage()
    {
        super(GRID);
        setTitle(Messages.PrefTitleExperimentalFeatures);
    }

    @Override
    public void createFieldEditors()
    {
        String[][] features = java.util.Arrays.stream(Experiments.Feature.values())
                        .map(f -> new String[] { f.name(), f.name() }).toArray(String[][]::new);

        addField(new CheckboxGroupFieldEditor(UIConstants.Preferences.EXPERIMENTS,
                        Messages.PrefLabelEnableExperimentalFeatures, features, getFieldEditorParent()));

        loadIniFile();
        createAutoScaleSection();
    }

    private void createAutoScaleSection()
    {
        var separator = new Label(getFieldEditorParent(), SWT.HORIZONTAL);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(separator);

        var autoScaleComposite = new Composite(getFieldEditorParent(), SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).spacing(10, 5).applyTo(autoScaleComposite);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(autoScaleComposite);

        var autoScaleLabel = new Label(autoScaleComposite, SWT.NONE);
        autoScaleLabel.setText(Messages.PrefLabelSwtAutoScale);

        autoScaleCombo = new Combo(autoScaleComposite, SWT.READ_ONLY);
        autoScaleCombo.setItems(getAutoScaleLabels());
        autoScaleCombo.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> updateAutoScale()));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(autoScaleCombo);

        updateAutoScaleUI();
    }

    private void loadIniFile()
    {
        iniFileManipulator = new IniFileManipulator();
        try
        {
            iniFileManipulator.load();
            autoScaleValue = iniFileManipulator.getVmProperty(SWT_AUTOSCALE);
            iniFileAvailable = true;
        }
        catch (FileNotFoundException e)
        {
            PortfolioPlugin.log(e);
            autoScaleValue = null;
            iniFileAvailable = false;
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError, e.getMessage());
            autoScaleValue = null;
            iniFileAvailable = false;
        }
    }

    private void updateAutoScaleUI()
    {
        var autoScaleIndex = getAutoScaleIndex(autoScaleValue);
        if (autoScaleIndex >= 0)
            autoScaleCombo.select(autoScaleIndex);
        else
            autoScaleCombo.deselectAll();
        autoScaleCombo.setEnabled(iniFileAvailable);
    }

    private void updateAutoScale()
    {
        var selectedValue = getSelectedAutoScaleValue();
        if (!iniFileAvailable || java.util.Objects.equals(autoScaleValue, selectedValue))
            return;

        try
        {
            iniFileManipulator.setSwtAutoScale(selectedValue);

            if (iniFileManipulator.isDirty())
            {
                iniFileManipulator.save();
                autoScaleValue = selectedValue;

                MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Success", //$NON-NLS-1$
                                Messages.MsgThemeRestartRequired);
            }
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError, e.getMessage());
            updateAutoScaleUI();
        }
    }

    @SuppressWarnings("nls")
    private String[] getAutoScaleLabels()
    {
        return new String[] { Messages.PrefLabelSwtAutoScaleAutomatic, "75%", "100%", "125%", "150%", "175%", "200%" };
    }

    private String getSelectedAutoScaleValue()
    {
        return switch (autoScaleCombo.getSelectionIndex())
        {
            case 1 -> SWT_AUTOSCALE_75_PERCENT;
            case 2 -> SWT_AUTOSCALE_DISABLED;
            case 3 -> SWT_AUTOSCALE_125_PERCENT;
            case 4 -> SWT_AUTOSCALE_150_PERCENT;
            case 5 -> SWT_AUTOSCALE_175_PERCENT;
            case 6 -> SWT_AUTOSCALE_200_PERCENT;
            default -> null;
        };
    }

    private int getAutoScaleIndex(String value)
    {
        if (value == null)
            return 0;
        else if (SWT_AUTOSCALE_75_PERCENT.equals(value))
            return 1;
        else if (SWT_AUTOSCALE_DISABLED.equals(value))
            return 2;
        else if (SWT_AUTOSCALE_125_PERCENT.equals(value))
            return 3;
        else if (SWT_AUTOSCALE_150_PERCENT.equals(value))
            return 4;
        else if (SWT_AUTOSCALE_175_PERCENT.equals(value))
            return 5;
        else if (SWT_AUTOSCALE_200_PERCENT.equals(value))
            return 6;
        else
            return -1;
    }
}
