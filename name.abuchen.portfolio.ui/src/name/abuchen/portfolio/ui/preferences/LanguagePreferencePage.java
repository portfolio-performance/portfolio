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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
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
        FRENCH("fr", "Français"), //$NON-NLS-1$ //$NON-NLS-2$
        ITALIAN("it", "Italiano"), //$NON-NLS-1$ //$NON-NLS-2$
        DUTCH("nl", "Nederlands"), //$NON-NLS-1$ //$NON-NLS-2$
        PORTUGUESE("pt", "Português"), //$NON-NLS-1$ //$NON-NLS-2$
        CZECH("cs", "čeština"), //$NON-NLS-1$ //$NON-NLS-2$
        RUSSIAN("ru", "русский"); //$NON-NLS-1$ //$NON-NLS-2$

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
    private ComboViewer languageCombo;
    private ComboViewer countryCombo;

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

        languageCombo = new ComboViewer(area, SWT.READ_ONLY);
        languageCombo.setContentProvider(ArrayContentProvider.getInstance());
        languageCombo.setInput(Language.values());
        languageCombo.addSelectionChangedListener(event -> {
            Language l = (Language) event.getStructuredSelection().getFirstElement();
            if (l != null)
            {
                countryCombo.getCombo().setEnabled(!l.equals(Language.AUTOMATIC));
                countryCombo.setInput(getCountriesForLanguage(l));
                area.layout();
            }
        });

        label = new Label(area, SWT.NONE);
        label.setText(Messages.LabelCountry);

        countryCombo = new ComboViewer(area, SWT.READ_ONLY);
        countryCombo.setContentProvider(ArrayContentProvider.getInstance());
        countryCombo.setLabelProvider(new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Locale) element).getDisplayCountry();
            }
        });

        updateSelection(userProperties.getProperty(OSGI_NL));

        return area;
    }

    private void updateSelection(String nlValue)
    {
        Language language = Language.valueOfLocale(nlValue);
        languageCombo.setSelection(new StructuredSelection(language));

        if (language != Language.AUTOMATIC)
        {
            String country = ""; //$NON-NLS-1$
            if (nlValue.contains("_")) //$NON-NLS-1$
            {
                String[] split = nlValue.split("_"); //$NON-NLS-1$
                country = split[1];
            }
            Locale locale = new Locale(language.code, country);
            countryCombo.setSelection(new StructuredSelection(locale));
        }
    }

    private String getNlValueFromSelection()
    {
        Language language = (Language) languageCombo.getStructuredSelection().getFirstElement();
        if (language == null || language.equals(Language.AUTOMATIC))
        {
            return null;
        }
        else
        {
            Locale locale = (Locale) countryCombo.getStructuredSelection().getFirstElement();
            if (locale == null)
            {
                locale = (Locale) countryCombo.getElementAt(0);
            }
            return locale.toString();
        }
    }

    private List<Locale> getCountriesForLanguage(Language langauage)
    {
        List<Locale> regions = new ArrayList<>();
        if (langauage == Language.AUTOMATIC)
            return regions;

        for (Locale locale : Locale.getAvailableLocales())
        {
            if (locale.getLanguage().equals(langauage.code))
            {
                regions.add(locale);
            }
        }

        // Remove irrelevant variants
        regions.removeIf(l -> l.getDisplayVariant().length() > 0);

        Collections.sort(regions, (l1, l2) -> l1.getDisplayCountry().compareTo(l2.getDisplayCountry()));

        return regions;
    }

    @Override
    public boolean performOk()
    {
        // check if viewer is initialized at all
        if (languageCombo == null)
            return true;

        String nlValue = getNlValueFromSelection();
        if (nlValue == null)
        {
            userProperties.remove(OSGI_NL);
        }
        else
        {
            userProperties.setProperty(OSGI_NL, nlValue);
        }

        storeUserPreferences();

        return true;
    }

    @Override
    protected void performDefaults()
    {
        updateSelection(userProperties.getProperty(OSGI_NL));
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
