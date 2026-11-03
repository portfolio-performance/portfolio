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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
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

    // The language can additionally be fixed in the ini file of the launcher.
    // The launcher turns this argument into the osgi.nl property before the
    // OSGi framework reads the config.ini of the configuration area. Therefore
    // the language also survives launch setups that write that file anew on
    // every start (for example the application launches from the workspace
    // during development) and packagings that reset the configuration area
    // (for example Flatpak).
    private static final String NLS_ARGUMENT = "-nl"; //$NON-NLS-1$
    private static final String VMARGS_ARGUMENT = "-vmargs"; //$NON-NLS-1$

    // command that is offered when the ini file of the launcher cannot be
    // written: the single quotes of the launcher path have to be doubled
    // because MessageFormat uses the single quote as its escape character
    private static final String START_COMMAND_FLATPAK = "flatpak run {0} {1} {2}"; //$NON-NLS-1$
    private static final String START_COMMAND_LAUNCHER = "''{0}'' {1} {2}"; //$NON-NLS-1$

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
        RUSSIAN("ru", "русский"), //$NON-NLS-1$ //$NON-NLS-2$
        SLOVAK("sk", "Slovenská"), //$NON-NLS-1$ //$NON-NLS-2$
        POLISH("pl", "Polski"), //$NON-NLS-1$ //$NON-NLS-2$
        CHINESE("zh", "中文"), //$NON-NLS-1$ //$NON-NLS-2$
        DANISH("da", "Dansk"), //$NON-NLS-1$ //$NON-NLS-2$
        TURKISH("tr", "Türk"), //$NON-NLS-1$ //$NON-NLS-2$
        VIETNAMESE("vi", "Tiếng Việt"), //$NON-NLS-1$ //$NON-NLS-2$
        CATALAN("ca", "Català"), //$NON-NLS-1$ //$NON-NLS-2$
        FINNISH("fi", "Suomi"); //$NON-NLS-1$ //$NON-NLS-2$

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
                if (l == Language.AUTOMATIC)
                {
                    countryCombo.getCombo().setEnabled(false);
                    countryCombo.setInput(Collections.emptyList());
                    countryCombo.setSelection(StructuredSelection.EMPTY);
                }
                else
                {
                    countryCombo.getCombo().setEnabled(true);
                    countryCombo.setInput(getCountriesForLanguage(l));
                    countryCombo.setSelection(new StructuredSelection(Locale.forLanguageTag(l.getCode())));
                }
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

        createSampleArea(area);

        updateSelection(userProperties.getProperty(OSGI_NL));

        return area;
    }

    private void createSampleArea(Composite area)
    {
        Label label = new Label(area, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridDataFactory.fillDefaults().span(2, 1).applyTo(label);

        label = new Label(area, SWT.NONE);
        label.setText("Java Locale"); //$NON-NLS-1$

        Label javaLocale = new Label(area, SWT.NONE);

        label = new Label(area, SWT.NONE);
        label.setText(Messages.ColumnDate);

        Label sampleDate = new Label(area, SWT.NONE);

        label = new Label(area, SWT.NONE);
        label.setText(Messages.LabelFirstDayOfWeek);

        Label firstDayOfWeek = new Label(area, SWT.NONE);

        label = new Label(area, SWT.NONE);
        label.setText(Messages.ColumnAmount);

        Label sampleAmount = new Label(area, SWT.NONE);

        countryCombo.addSelectionChangedListener(event -> {

            Locale locale = (Locale) countryCombo.getStructuredSelection().getFirstElement();

            if (locale == null)
            {
                // if the user currently uses the automatic locale, we can use
                // the locale to print the sample data. If the user overrides
                // the automatic locale, there is no way to know what it would
                // be

                String automaticLocale = userProperties.getProperty(OSGI_NL);
                if (automaticLocale == null)
                    locale = Locale.getDefault();
            }

            if (locale == null)
            {
                javaLocale.setText(""); //$NON-NLS-1$
                sampleDate.setText("---"); //$NON-NLS-1$
                firstDayOfWeek.setText("---"); //$NON-NLS-1$
                sampleAmount.setText("---"); //$NON-NLS-1$
            }
            else
            {
                javaLocale.setText(locale.toString());

                DateTimeFormatter formatter = DateTimeFormatter
                                .ofLocalizedDateTime(
                                                Locale.forLanguageTag("pt").getLanguage().equals(locale.getLanguage()) //$NON-NLS-1$
                                                                ? FormatStyle.SHORT
                                                                : FormatStyle.MEDIUM,
                                                FormatStyle.SHORT)
                                .withLocale(locale);

                sampleDate.setText(formatter.format(LocalDateTime.now()));

                firstDayOfWeek.setText(WeekFields.of(locale).getFirstDayOfWeek()
                                .getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()));

                DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
                DecimalFormat format = new DecimalFormat("#,##0.##", symbols); //$NON-NLS-1$

                sampleAmount.setText(format.format(12345.67));

                area.layout();
            }
        });
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
            Locale locale = Locale.forLanguageTag(language.code + "-" + country); //$NON-NLS-1$
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

    private List<Locale> getCountriesForLanguage(Language language)
    {
        List<Locale> regions = new ArrayList<>();
        if (language == Language.AUTOMATIC)
            return regions;

        for (Locale locale : Locale.getAvailableLocales())
        {
            if (locale.getLanguage().equals(language.code))
            {
                regions.add(locale);
            }
        }

        // Remove irrelevant variants
        regions.removeIf(l -> l.getDisplayVariant().length() > 0);
        regions.removeIf(l -> l.getDisplayScript().length() > 0);

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

        persistWithLauncherArgument(nlValue);

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

    /**
     * Writes the language into the ini file of the launcher so that it is set
     * before the framework reads the config.ini. Without this argument the
     * language of the user is lost whenever that config.ini is written anew.
     */
    private void persistWithLauncherArgument(String nlValue)
    {
        String launcherIniFile = getLauncherIniFileOrNull();

        if (launcherIniFile == null)
        {
            // no launcher and therefore no ini file: the argument has to be
            // added where this application is started from
            if (nlValue != null)
                MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.LabelInfo,
                                MessageFormat.format(Messages.MsgLanguageLaunchConfigurationHint, nlValue));

            return;
        }

        boolean written = writeNlArgument(launcherIniFile, nlValue);

        if (written && nlValue != null)
            MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.LabelInfo,
                            MessageFormat.format(Messages.PrefMsgLanguageStoredInLauncherIni, launcherIniFile,
                                            nlValue));
        else if (!written && nlValue != null)
            showStartCommandHint(launcherIniFile, nlValue);
    }

    private String getLauncherIniFileOrNull()
    {
        String launcherIniFile = System.getProperty("eclipse.launcher.ini"); //$NON-NLS-1$

        if (launcherIniFile == null || launcherIniFile.isEmpty())
        {
            String launcher = System.getProperty("eclipse.launcher"); //$NON-NLS-1$
            if (launcher == null || launcher.isEmpty())
                return null;

            launcherIniFile = launcher + ".ini"; //$NON-NLS-1$
        }

        return Files.isReadable(Paths.get(launcherIniFile)) ? launcherIniFile : null;
    }

    /**
     * Replaces an existing -nl argument of the launcher ini file, or removes it
     * if the language is set back to automatic. The argument and its value
     * occupy one line each and have to be placed in front of -vmargs.
     */
    private boolean writeNlArgument(String launcherIniFile, String nlValue)
    {
        try
        {
            Path launcherIni = Paths.get(launcherIniFile);
            List<String> lines = Files.readAllLines(launcherIni);
            List<String> updated = new ArrayList<>(lines.size());

            boolean inserted = nlValue == null;

            for (int i = 0; i < lines.size(); i++)
            {
                String line = lines.get(i);

                if (line.trim().equals(NLS_ARGUMENT) && i + 1 < lines.size())
                {
                    // skip the argument together with the value that follows it
                    i++;
                    continue;
                }

                if (!inserted && line.trim().equals(VMARGS_ARGUMENT))
                {
                    updated.add(NLS_ARGUMENT);
                    updated.add(nlValue);
                    inserted = true;
                }

                updated.add(line);
            }

            if (!inserted)
            {
                updated.add(NLS_ARGUMENT);
                updated.add(nlValue);
            }

            Files.write(launcherIni, updated);
            return true;
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            return false;
        }
    }

    private void showStartCommandHint(String launcherIniFile, String nlValue)
    {
        String startCommand = getStartCommandOrNull(nlValue);

        if (startCommand == null)
            MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.LabelInfo,
                            MessageFormat.format(Messages.MsgLanguageLaunchConfigurationHint, nlValue));
        else
            MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.LabelInfo,
                            MessageFormat.format(Messages.MsgLanguageCommandHint, launcherIniFile, startCommand));
    }

    /**
     * Command line that starts this installation in the desired language. It is
     * offered when the ini file of the launcher cannot be written, for example
     * in a sandbox such as Flatpak or in an installation owned by root.
     */
    private String getStartCommandOrNull(String nlValue)
    {
        String flatpakId = System.getenv("FLATPAK_ID"); //$NON-NLS-1$
        if (flatpakId != null && !flatpakId.isEmpty())
            return MessageFormat.format(START_COMMAND_FLATPAK, flatpakId, NLS_ARGUMENT, nlValue);

        String launcher = System.getProperty("eclipse.launcher"); //$NON-NLS-1$
        if (launcher == null || launcher.isEmpty())
            return null;

        return MessageFormat.format(START_COMMAND_LAUNCHER, launcher, NLS_ARGUMENT, nlValue);
    }

}
