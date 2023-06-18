package name.abuchen.portfolio.ui.preferences;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.DesktopAPI;

public class MyDividends24PreferencePage extends FieldEditorPreferencePage
{

    public MyDividends24PreferencePage()
    {
        super(GRID);

        setTitle(Messages.PrefTitleMyDividends24);
        setDescription(Messages.PrefDescriptionMyDividends24);
        setImageDescriptor(Images.MYDIVIDENDS24_LOGO.descriptor());
    }

    @Override
    public void createFieldEditors()
    {
        Link link = new Link(getFieldEditorParent(), SWT.NONE);
        link.setText("<a>http://mydividends24.de/de_de</a>"); //$NON-NLS-1$
        link.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));
        link.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(final SelectionEvent event)
            {
                DesktopAPI.browse("http://mydividends24.de/de_de"); //$NON-NLS-1$
            }
        });

        addField(new TextAreaFieldEditor(UIConstants.Preferences.MYDIVIDENDS24_API_KEY,
                        Messages.PrefMyDividends24APIKey, getFieldEditorParent()));
    }

    private class TextAreaFieldEditor extends FieldEditor
    {

        private Text textField;

        public TextAreaFieldEditor(String name, String labelText, Composite parent)
        {
            super(name, labelText, parent);
        }

        @Override
        protected void adjustForNumColumns(int numColumns)
        {
            ((GridData) textField.getLayoutData()).horizontalSpan = numColumns - 1;
        }

        @Override
        protected void doFillIntoGrid(Composite parent, int numColumns)
        {
            Label label = getLabelControl(parent);
            label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
            textField = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
            GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, numColumns - 1, 1);
            gridData.heightHint = textField.getLineHeight();
            textField.setLayoutData(gridData);
            textField.addModifyListener(e -> {
                int contentHeight = textField.computeSize(textField.getSize().x, SWT.DEFAULT).y;
                int lineHeight = textField.getLineHeight();
                ((GridData) textField.getLayoutData()).heightHint = Math.max(lineHeight, contentHeight);
                textField.getParent().layout();
            });
        }

        @Override
        protected void doLoad()
        {
            String value = getPreferenceStore().getString(getPreferenceName());
            if (value.length() > 100)
            {
                int thirdLength = value.length() / 3;
                String firstLine = value.substring(0, thirdLength);
                String secondLine = value.substring(thirdLength, 2 * thirdLength);
                String thirdLine = value.substring(2 * thirdLength);
                textField.setText(firstLine + "\n" + secondLine + "\n" + thirdLine); //$NON-NLS-1$ //$NON-NLS-2$
            }
            else
            {
                textField.setText(value);
            }
        }

        @Override
        protected void doLoadDefault()
        {
            textField.setText(getPreferenceStore().getDefaultString(getPreferenceName()));
        }

        @Override
        protected void doStore()
        {
            String value = textField.getText().replaceAll("\\r\\n|\\r|\\n", ""); //$NON-NLS-1$ //$NON-NLS-2$
            getPreferenceStore().setValue(getPreferenceName(), value);
        }

        @Override
        public int getNumberOfControls()
        {
            return 2;
        }

    }
}
