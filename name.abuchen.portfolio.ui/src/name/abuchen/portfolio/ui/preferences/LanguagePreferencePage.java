package name.abuchen.portfolio.ui.preferences;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Properties;

import org.eclipse.core.runtime.Platform;
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

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public class LanguagePreferencePage extends PreferencePage
{
    private static final String OSGI_NL = "osgi.nl"; //$NON-NLS-1$

    public enum Language
    {
        AUTOMATIC(null, Messages.LabelLanguageAutomatic), //
        GERMAN("de", "Deutsch"), //$NON-NLS-1$ //$NON-NLS-2$
        ENGLISH("en", "English"), //$NON-NLS-1$ //$NON-NLS-2$
        SPANISH("es", "Español"), //$NON-NLS-1$ //$NON-NLS-2$
        DUTCH("nl", "Nederlands"), //$NON-NLS-1$ //$NON-NLS-2$
        PORTUGUESE("pt", "Português"); //$NON-NLS-1$ //$NON-NLS-2$

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

    private Properties userProperties = new Properties();
    private ComboViewer viewer;

    public LanguagePreferencePage()
    {
        setTitle(Messages.PrefTitleLanguage);
        setDescription(Messages.PrefMsgLanguageConfig);
        loadUserPreferences();
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
        viewer.setSelection(new StructuredSelection(Language.valueOfLocale(userProperties.getProperty(OSGI_NL))));

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
                userProperties.remove(OSGI_NL);
                break;
            default:
                userProperties.setProperty(OSGI_NL, language.getCode());
                break;
        }

        storeUserPreferences();

        return true;
    }

    @Override
    protected void performDefaults()
    {
        viewer.setSelection(new StructuredSelection(Language.valueOfLocale(userProperties.getProperty(OSGI_NL))));
        super.performDefaults();
    }

    private Path getUserConfigFile() throws URISyntaxException
    {
        // path can contain spaces (.../Application Support/...)
        URL configArea = Platform.getConfigurationLocation().getURL();
        URI uri = new URI(configArea.toExternalForm().replace(" ", "%20")); //$NON-NLS-1$ //$NON-NLS-2$
        return Paths.get(uri).resolve("config.ini"); //$NON-NLS-1$
    }

    private void loadUserPreferences()
    {
        try
        {
            Path userConfigFile = getUserConfigFile();
            if (Files.exists(userConfigFile))
            {
                try (InputStream input = new FileInputStream(userConfigFile.toFile()))
                {
                    userProperties.load(input);
                }
            }
        }
        catch (IOException | URISyntaxException e)
        {
            PortfolioPlugin.log(e);
        }
    }

    private void storeUserPreferences()
    {
        try
        {
            Path userConfigFile = getUserConfigFile();

            try (OutputStream out = new FileOutputStream(userConfigFile.toFile()))
            {
                userProperties.store(out, null);
            }
        }
        catch (IOException | URISyntaxException e)
        {
            MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.LabelError,
                            MessageFormat.format(Messages.MsgErrorSavingIniFile, e.getMessage()));
        }
    }

}
