package name.abuchen.portfolio.ui.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.StringTokenizer;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class CheckboxGroupFieldEditor extends FieldEditor
{
    private String[][] labelsAndValues;
    private List<Button> checkboxes = new ArrayList<>();

    public CheckboxGroupFieldEditor(String name, String labelText, String[][] labelsAndValues, Composite parent)
    {
        init(name, labelText);
        this.labelsAndValues = labelsAndValues;
        createControl(parent);
    }

    @Override
    protected void adjustForNumColumns(int numColumns)
    {
        Control control = getLabelControl();
        if (control != null)
        {
            ((GridData) control.getLayoutData()).horizontalSpan = numColumns;
        }

        for (var button : checkboxes)
        {
            ((GridData) button.getLayoutData()).horizontalSpan = numColumns;
        }
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns)
    {
        var control = getLabelControl(parent);
        GridDataFactory.fillDefaults().span(numColumns, 1).applyTo(control);

        for (String[] labelAndValue : labelsAndValues)
        {
            Button checkbox = new Button(parent, SWT.CHECK);
            checkbox.setText(labelAndValue[0]);
            checkbox.setData(labelAndValue[1]);
            checkboxes.add(checkbox);
            GridDataFactory.fillDefaults().span(numColumns, 1).applyTo(checkbox);
        }
    }

    @Override
    protected void doLoad()
    {
        if (checkboxes.isEmpty())
            return;
        String value = getPreferenceStore().getString(getPreferenceName());
        setCheckedFromString(value);
    }

    @Override
    protected void doLoadDefault()
    {
        if (checkboxes.isEmpty())
            return;
        String value = getPreferenceStore().getDefaultString(getPreferenceName());
        setCheckedFromString(value);
    }

    @Override
    protected void doStore()
    {
        StringJoiner joiner = new StringJoiner(","); //$NON-NLS-1$
        for (Button checkbox : checkboxes)
        {
            if (checkbox.getSelection())
                joiner.add((String) checkbox.getData());
        }
        getPreferenceStore().setValue(getPreferenceName(), joiner.toString());
    }

    @Override
    public int getNumberOfControls()
    {
        return labelsAndValues.length;
    }

    private void setCheckedFromString(String value)
    {
        List<String> selected = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(value, ","); //$NON-NLS-1$
        while (tokenizer.hasMoreTokens())
            selected.add(tokenizer.nextToken());

        for (Button checkbox : checkboxes)
        {
            String data = (String) checkbox.getData();
            checkbox.setSelection(selected.contains(data));
        }
    }
}
