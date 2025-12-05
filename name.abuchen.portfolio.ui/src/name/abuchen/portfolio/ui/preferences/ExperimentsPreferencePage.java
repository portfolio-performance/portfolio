package name.abuchen.portfolio.ui.preferences;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.IniFileManipulator;

public class ExperimentsPreferencePage extends FieldEditorPreferencePage
{
    private Label statusLabel;
    private Button actionButton;
    private IniFileManipulator iniFileManipulator;
    private boolean hasProperty;
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

        var os = Platform.getOS();
        var showMonitorSpecificScaling = Platform.OS_WIN32.equals(os) || Platform.OS_LINUX.equals(os);

        if (showMonitorSpecificScaling)
        {
            var separator = new Label(getFieldEditorParent(), SWT.HORIZONTAL);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(separator);

            var statusComposite = new Composite(getFieldEditorParent(), SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(2).spacing(10, 5).applyTo(statusComposite);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(statusComposite);

            var statusTitleLabel = new Label(statusComposite, SWT.NONE);
            statusTitleLabel.setText("Monitor-specific scaling:"); //$NON-NLS-1$

            statusLabel = new Label(statusComposite, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(statusLabel);

            actionButton = new Button(getFieldEditorParent(), SWT.PUSH);
            GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(actionButton);
            actionButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> toggleProperty()));

            loadIniFile();
            updateUI();
        }
    }

    private void loadIniFile()
    {
        iniFileManipulator = new IniFileManipulator();
        try
        {
            iniFileManipulator.load();
            hasProperty = iniFileManipulator.hasMonitorSpecificScaling();
            iniFileAvailable = true;
        }
        catch (FileNotFoundException e)
        {
            PortfolioPlugin.log(e);
            hasProperty = false;
            iniFileAvailable = false;
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError, e.getMessage());
            hasProperty = false;
            iniFileAvailable = false;
        }
    }

    @SuppressWarnings("nls")
    private void updateUI()
    {
        if (!iniFileAvailable)
        {
            statusLabel.setText("unknown");
            actionButton.setText("not available");
            actionButton.setEnabled(false);
        }
        else if (hasProperty)
        {
            statusLabel.setText("enabled");
            actionButton.setText("Disable");
            actionButton.setEnabled(true);
        }
        else
        {
            statusLabel.setText("disabled");
            actionButton.setText("Enable");
            actionButton.setEnabled(true);
        }
        statusLabel.getParent().layout(true, true);
    }

    private void toggleProperty()
    {
        try
        {
            if (hasProperty)
            {
                iniFileManipulator.removeMonitorSpecificScaling();
            }
            else
            {
                iniFileManipulator.addMonitorSpecificScaling();
            }

            if (iniFileManipulator.isDirty())
            {
                iniFileManipulator.save();
                hasProperty = !hasProperty;
                updateUI();

                MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Success", //$NON-NLS-1$
                                Messages.MsgThemeRestartRequired);
            }
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError, e.getMessage());
        }
    }
}
