package name.abuchen.portfolio.ui.preferences;

import java.io.IOException;
import java.text.MessageFormat;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.util.IniFileManipulator;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

public class LanguagePreferencePage extends PreferencePage
{
    public enum Language
    {
        AUTOMATIC(null, Messages.LabelLanguageAutomatic), //
        GERMAN("de", "Deutsch"), //$NON-NLS-1$ //$NON-NLS-2$
        ENGLISH("en", "English"); //$NON-NLS-1$ //$NON-NLS-2$

        private String code;
        private String label;

        private Language(String code, String label)
        {
            this.code = code;
            this.label = label;
        }

        public String getCode()
        {
            return code;
        }

        public static Language valueOfLocale(String locale)
        {
            if (locale == null)
                return AUTOMATIC;

            String code = locale.substring(0, 2);

            for (Language language : values())
            {
                if (code.equals(language.getCode()))
                    return language;
            }

            return AUTOMATIC;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    private IniFileManipulator iniFile;
    private ComboViewer viewer;

    public LanguagePreferencePage()
    {
        setTitle(Messages.PrefTitleLanguage);
        setDescription(Messages.PrefMsgLanguageConfig);

        try
        {
            iniFile = new IniFileManipulator();
            iniFile.load();
        }
        catch (IOException e)
        {
            if (!PortfolioPlugin.isDevelopmentMode())
                PortfolioPlugin.log(e);
        }
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite area = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(area);

        Label label = new Label(area, SWT.NONE);
        label.setText(Messages.LabelLanguage);

        viewer = new ComboViewer(area, SWT.READ_ONLY);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setInput(Language.values());
        viewer.setSelection(new StructuredSelection(Language.valueOfLocale(iniFile.getLanguage())));

        return area;
    }

    @Override
    public boolean performOk()
    {
        // check if viewer is initialized at all
        if (viewer == null)
            return true;

        Language language = (Language) ((IStructuredSelection) viewer.getSelection()).getFirstElement();

        switch (language)
        {
            case AUTOMATIC:
                iniFile.clearLanguage();
                break;
            default:
                iniFile.setLanguage(language.getCode());
                break;
        }

        if (iniFile.isDirty())
        {
            try
            {
                iniFile.save();
            }
            catch (IOException e)
            {
                MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.LabelError,
                                MessageFormat.format(Messages.MsgErrorSavingIniFile, e.getMessage()));
            }
        }

        return true;
    }

    @Override
    protected void performDefaults()
    {
        viewer.setSelection(new StructuredSelection(Language.valueOfLocale(iniFile.getLanguage())));
        super.performDefaults();
    }
}
