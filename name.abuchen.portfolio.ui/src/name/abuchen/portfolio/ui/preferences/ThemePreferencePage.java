package name.abuchen.portfolio.ui.preferences;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.theme.ThemePreferences;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ValueColorScheme;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.ui.util.swt.ControlDecoration;
import name.abuchen.portfolio.util.ColorConversion;

@SuppressWarnings("restriction")
public final class ThemePreferencePage extends PreferencePage
{
    private static final ITheme AUTOMATIC_THEME = new ITheme()
    {
        @Override
        public String getId()
        {
            return "automatic"; //$NON-NLS-1$
        }

        @Override
        public String getLabel()
        {
            return Messages.LabelAutomaticDarkLightTheme;
        }
    };

    private IThemeEngine engine;
    private ComboViewer themeIdCombo;
    private String currentlyConfiguredThemeId;

    private int currentFontSize = -1;
    private ComboViewer fontSizeCombo;

    private List<Button> valueColorSchemeOptions = new ArrayList<>();
    private List<Runnable> previewBackgroundUpdaters = new ArrayList<>();
    private RGB customPositiveRGB;
    private RGB customPositiveArrowRGB;
    private RGB customNegativeRGB;
    private RGB customNegativeArrowRGB;

    public ThemePreferencePage(IThemeEngine themeEngine)
    {
        this.engine = themeEngine;

        setTitle(Messages.LabelTheme);

        this.currentFontSize = readFontSizeFromCSS();
        this.currentlyConfiguredThemeId = ThemePreferences.getConfiguredThemeId().orElse(AUTOMATIC_THEME.getId());
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite area = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(area);

        Label label = new Label(area, SWT.NONE);
        label.setText(Messages.LabelTheme);

        themeIdCombo = new ComboViewer(area, SWT.READ_ONLY);
        themeIdCombo.setLabelProvider(new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((ITheme) element).getLabel();
            }

        });
        themeIdCombo.setContentProvider(ArrayContentProvider.getInstance());
        var availableThemes = getCSSThemes();
        themeIdCombo.setInput(availableThemes);
        availableThemes.stream().filter(t -> t.getId().equals(currentlyConfiguredThemeId)).findAny()
                        .ifPresent(t -> themeIdCombo.setSelection(new StructuredSelection(t)));

        ControlDecoration themeComboDecorator = new ControlDecoration(themeIdCombo.getCombo(), SWT.RIGHT);
        themeComboDecorator.setDescriptionText(Messages.MsgThemeRestartRequired);
        themeComboDecorator.setImage(Images.WARNING.image());
        themeComboDecorator.hide();
        themeIdCombo.addSelectionChangedListener(event -> {
            ITheme selection = getSelectedTheme();

            var selectedId = selection != null ? selection.getId() : null;
            if (!Objects.equals(selectedId, currentlyConfiguredThemeId))
                themeComboDecorator.show();
            else
                themeComboDecorator.hide();
        });

        // font size

        label = new Label(area, SWT.NONE);
        label.setText(Messages.LabelFontSize);

        int systemFontSize = Display.getDefault().getSystemFont().getFontData()[0].getHeight();

        fontSizeCombo = new ComboViewer(area, SWT.READ_ONLY);
        fontSizeCombo.setLabelProvider(new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                int size = (Integer) element;
                String label = size == -1 ? Messages.LabelDefaultFontSize : String.valueOf(size) + "px"; //$NON-NLS-1$

                if (size == systemFontSize)
                    label += " (" + Messages.LabelDefaultFontSize + ")"; //$NON-NLS-1$ //$NON-NLS-2$

                return label;
            }

        });
        fontSizeCombo.setContentProvider(ArrayContentProvider.getInstance());
        fontSizeCombo.setInput(Stream.concat(Stream.of(-1), IntStream.range(8, 21).boxed()).toList());
        fontSizeCombo.setSelection(new StructuredSelection(currentFontSize));

        ControlDecoration fontSizeDecorator = new ControlDecoration(fontSizeCombo.getCombo(), SWT.RIGHT);
        fontSizeDecorator.setDescriptionText(Messages.MsgThemeRestartRequired);
        fontSizeDecorator.setImage(Images.WARNING.image());
        fontSizeDecorator.hide();
        fontSizeCombo.addSelectionChangedListener(event -> {

            int selectedFontSize = (Integer) fontSizeCombo.getStructuredSelection().getFirstElement();
            if (selectedFontSize != currentFontSize)
                fontSizeDecorator.show();
            else
                fontSizeDecorator.hide();
        });

        // color value scheme selection

        label = new Label(area, SWT.NONE);
        label.setText(Messages.LabelColorValueScheme);
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(label);

        String customPosHex = getPreferenceStore().getString(UIConstants.Preferences.VALUE_COLOR_SCHEME_CUSTOM_POSITIVE);
        try
        {
            customPositiveRGB = customPosHex != null && !customPosHex.isBlank() ? ColorConversion.hex2RGB(customPosHex)
                            : ValueColorScheme.getCustomScheme().positiveForeground().getRGB();
        }
        catch (Exception e)
        {
            customPositiveRGB = ValueColorScheme.getCustomScheme().positiveForeground().getRGB();
        }

        String customPosArrowHex = getPreferenceStore().getString(UIConstants.Preferences.VALUE_COLOR_SCHEME_CUSTOM_POSITIVE_ARROW);
        try
        {
            customPositiveArrowRGB = customPosArrowHex != null && !customPosArrowHex.isBlank()
                            ? ColorConversion.hex2RGB(customPosArrowHex)
                            : ValueColorScheme.getCustomScheme().getUpArrowColor();
            if (customPositiveArrowRGB == null)
                customPositiveArrowRGB = customPositiveRGB;
        }
        catch (Exception e)
        {
            customPositiveArrowRGB = customPositiveRGB;
        }

        String customNegHex = getPreferenceStore().getString(UIConstants.Preferences.VALUE_COLOR_SCHEME_CUSTOM_NEGATIVE);
        try
        {
            customNegativeRGB = customNegHex != null && !customNegHex.isBlank() ? ColorConversion.hex2RGB(customNegHex)
                            : ValueColorScheme.getCustomScheme().negativeForeground().getRGB();
        }
        catch (Exception e)
        {
            customNegativeRGB = ValueColorScheme.getCustomScheme().negativeForeground().getRGB();
        }

        String customNegArrowHex = getPreferenceStore().getString(UIConstants.Preferences.VALUE_COLOR_SCHEME_CUSTOM_NEGATIVE_ARROW);
        try
        {
            customNegativeArrowRGB = customNegArrowHex != null && !customNegArrowHex.isBlank()
                            ? ColorConversion.hex2RGB(customNegArrowHex)
                            : ValueColorScheme.getCustomScheme().getDownArrowColor();
            if (customNegativeArrowRGB == null)
                customNegativeArrowRGB = customNegativeRGB;
        }
        catch (Exception e)
        {
            customNegativeArrowRGB = customNegativeRGB;
        }

        var selectedSchemaId = getPreferenceStore().getString(UIConstants.Preferences.VALUE_COLOR_SCHEME);
        var schemes = ValueColorScheme.getAvailableSchemes();

        for (var scheme : schemes)
        {
            var radioButton = createSchemeOption(area, scheme);
            radioButton.setData(scheme);
            radioButton.setSelection(Objects.equals(selectedSchemaId, scheme.getIdentifier()));
            valueColorSchemeOptions.add(radioButton);
        }

        return area;
    }

    private List<ITheme> getCSSThemes()
    {
        ArrayList<ITheme> themes = new ArrayList<>();
        for (ITheme theme : engine.getThemes())
        {
            if (theme.getId().startsWith("name.abuchen.portfolio.")) //$NON-NLS-1$
                themes.add(theme);
        }
        themes.sort((ITheme t1, ITheme t2) -> t1.getLabel().compareTo(t2.getLabel()));
        themes.add(0, AUTOMATIC_THEME);
        return themes;
    }

    private ITheme getSelectedTheme()
    {
        if (themeIdCombo == null)
            return null;
        else
            return (ITheme) (themeIdCombo.getStructuredSelection().getFirstElement());
    }

    @Override
    public boolean performOk()
    {
        ITheme theme = getSelectedTheme();
        if (theme != null)
        {
            if (theme == AUTOMATIC_THEME)
            {
                setThemeToAutomatic();
            }
            else
            {
                engine.setTheme(theme, true);
            }
        }

        if (fontSizeCombo != null)
        {
            Integer selectedFontSize = (Integer) fontSizeCombo.getStructuredSelection().getFirstElement();
            if (selectedFontSize != null && selectedFontSize != currentFontSize)
                writeFrontSizeToCSS(selectedFontSize);
        }

        for (var button : valueColorSchemeOptions)
        {
            if (button.getSelection())
            {
                getPreferenceStore().setValue(UIConstants.Preferences.VALUE_COLOR_SCHEME,
                                ((ValueColorScheme) button.getData()).getIdentifier());
            }
        }

        if (customPositiveRGB != null)
        {
            getPreferenceStore().setValue(UIConstants.Preferences.VALUE_COLOR_SCHEME_CUSTOM_POSITIVE,
                            ColorConversion.toHex(customPositiveRGB));
            ValueColorScheme.getCustomScheme().setPositiveForeground(customPositiveRGB);
        }
        if (customPositiveArrowRGB != null)
        {
            getPreferenceStore().setValue(UIConstants.Preferences.VALUE_COLOR_SCHEME_CUSTOM_POSITIVE_ARROW,
                            ColorConversion.toHex(customPositiveArrowRGB));
            ValueColorScheme.getCustomScheme().setUpArrowColor(customPositiveArrowRGB);
        }
        if (customNegativeRGB != null)
        {
            getPreferenceStore().setValue(UIConstants.Preferences.VALUE_COLOR_SCHEME_CUSTOM_NEGATIVE,
                            ColorConversion.toHex(customNegativeRGB));
            ValueColorScheme.getCustomScheme().setNegativeForeground(customNegativeRGB);
        }
        if (customNegativeArrowRGB != null)
        {
            getPreferenceStore().setValue(UIConstants.Preferences.VALUE_COLOR_SCHEME_CUSTOM_NEGATIVE_ARROW,
                            ColorConversion.toHex(customNegativeArrowRGB));
            ValueColorScheme.getCustomScheme().setDownArrowColor(customNegativeArrowRGB);
        }

        Display.getDefault().asyncExec(() -> previewBackgroundUpdaters.forEach(Runnable::run));

        return super.performOk();
    }

    @Override
    protected void performDefaults()
    {
        setThemeToAutomatic();

        themeIdCombo.setSelection(new StructuredSelection(AUTOMATIC_THEME));

        fontSizeCombo.setSelection(new StructuredSelection(-1));

        for (var button : valueColorSchemeOptions)
        {
            var scheme = (ValueColorScheme) button.getData();
            button.setSelection(ValueColorScheme.STANDARD_SCHEME.equals(scheme.getIdentifier()));
        }

        try
        {
            customPositiveRGB = ColorConversion.hex2RGB("#00E000"); //$NON-NLS-1$
            customPositiveArrowRGB = ColorConversion.hex2RGB("#00E000"); //$NON-NLS-1$
            customNegativeRGB = ColorConversion.hex2RGB("#FF6B6B"); //$NON-NLS-1$
            customNegativeArrowRGB = ColorConversion.hex2RGB("#FF6B6B"); //$NON-NLS-1$
        }
        catch (Exception ignored)
        {
        }

        Display.getDefault().asyncExec(() -> previewBackgroundUpdaters.forEach(Runnable::run));

        super.performDefaults();
    }

    private Path getPathToCustomCSS() throws IOException, URISyntaxException
    {
        URL url = FileLocator.resolve(new URI("platform:/meta/name.abuchen.portfolio.ui/custom.css").toURL()); //$NON-NLS-1$ //NOSONAR
        return new File(url.getFile()).toPath();
    }

    private int readFontSizeFromCSS()
    {
        try
        {
            String customCSS = Files.readString(getPathToCustomCSS());

            Pattern p = Pattern.compile("font-size: (\\d+)px;"); //$NON-NLS-1$
            Matcher m = p.matcher(customCSS);

            if (!m.find())
                return -1;

            return Integer.parseInt(m.group(1));
        }
        catch (IOException | URISyntaxException | NumberFormatException e)
        {
            PortfolioPlugin.log(e);
            return -1;
        }
    }

    private void setThemeToAutomatic()
    {
        // clear the stored preference
        ThemePreferences.clearConfiguredThemeId();

        // set the current theme just like the CustomThemeEngine would set it
        boolean isDark = Display.isSystemDarkTheme();
        engine.setTheme(isDark ? UIConstants.Theme.DARK : UIConstants.Theme.LIGHT, false);
    }

    private void writeFrontSizeToCSS(int fontSize)
    {
        try
        {
            String css = ""; //$NON-NLS-1$

            if (fontSize > 0)
            {
                int[] delta = new int[] { 3, 10, -1 }; // windows
                if (Platform.OS_MACOSX.equals(Platform.getOS()))
                    delta = new int[] { 3, 10, -1 }; // mac
                else if (Platform.OS_LINUX.equals(Platform.getOS()))
                    delta = new int[] { 1, 10, -1 }; // linux

                css = String.format("* { font-size: %dpx;}%n" //$NON-NLS-1$
                                + ".heading1 { font-size: %dpx; }%n" //$NON-NLS-1$
                                + ".kpi { font-size: %dpx; }%n" //$NON-NLS-1$
                                + ".datapoint { font-size: %dpx; }", //$NON-NLS-1$
                                fontSize, fontSize + delta[0], fontSize + delta[1], fontSize + delta[2]);
            }

            Files.writeString(getPathToCustomCSS(), css, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (IOException | URISyntaxException e)
        {
            PortfolioPlugin.log(e);
        }
    }

    private Button createSchemeOption(Composite parent, ValueColorScheme scheme)
    {
        var radioButton = new Button(parent, SWT.RADIO);
        radioButton.setText(scheme.getName());
        GridDataFactory.fillDefaults().span(2, 1).applyTo(radioButton);

        // Color preview composite
        var previewComposite = new Composite(parent, SWT.NONE);
        previewComposite.setBackgroundMode(SWT.INHERIT_FORCE);
        int numColumns = scheme.isCustom() ? 5 : 4;
        GridLayoutFactory.fillDefaults().numColumns(numColumns).margins(20, 5).spacing(10, 5).applyTo(previewComposite);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(previewComposite);

        // Positive
        var positiveArrowLabel = new Label(previewComposite, SWT.NONE);
        positiveArrowLabel.setImage(scheme.isCustom() && customPositiveArrowRGB != null
                        ? ValueColorScheme.getArrowImage(customPositiveArrowRGB, true)
                        : scheme.upArrow());

        var positiveLabel = new ColoredLabel(previewComposite, SWT.NONE);
        positiveLabel.setText(Values.Amount.format(Values.Amount.factorize(1234.56)));
        positiveLabel.setTextColor(scheme.isCustom() ? Colors.getColor(customPositiveRGB) : scheme.positiveForeground());

        // Negative
        var negativeArrowLabel = new Label(previewComposite, SWT.NONE);
        negativeArrowLabel.setImage(scheme.isCustom() && customNegativeArrowRGB != null
                        ? ValueColorScheme.getArrowImage(customNegativeArrowRGB, false)
                        : scheme.downArrow());

        var negativeLabel = new ColoredLabel(previewComposite, SWT.NONE);
        negativeLabel.setText(Values.Amount.format(Values.Amount.factorize(-1234.56)));
        negativeLabel.setTextColor(scheme.isCustom() ? Colors.getColor(customNegativeRGB) : scheme.negativeForeground());

        if (scheme.isCustom())
        {
            var btnCustomize = new Button(previewComposite, SWT.PUSH);
            btnCustomize.setText(Messages.ValueColorSchemeCustomize);
            btnCustomize.addListener(SWT.Selection, event -> {
                ValueColorSchemeDialog dialog = new ValueColorSchemeDialog(getShell(),
                                customPositiveRGB, customPositiveArrowRGB,
                                customNegativeRGB, customNegativeArrowRGB);
                if (dialog.open() == Window.OK)
                {
                    customPositiveRGB = dialog.getPositiveRGB();
                    customPositiveArrowRGB = dialog.getPositiveArrowRGB();
                    customNegativeRGB = dialog.getNegativeRGB();
                    customNegativeArrowRGB = dialog.getNegativeArrowRGB();

                    positiveLabel.setTextColor(Colors.getColor(customPositiveRGB));
                    positiveLabel.redraw();
                    positiveArrowLabel.setImage(ValueColorScheme.getArrowImage(customPositiveArrowRGB, true));
                    positiveArrowLabel.redraw();

                    negativeLabel.setTextColor(Colors.getColor(customNegativeRGB));
                    negativeLabel.redraw();
                    negativeArrowLabel.setImage(ValueColorScheme.getArrowImage(customNegativeArrowRGB, false));
                    negativeArrowLabel.redraw();

                    previewComposite.layout(true, true);
                    previewComposite.redraw();
                    previewComposite.update();

                    selectScheme(radioButton);
                }
            });
        }

        Runnable updatePreviewBackground = () -> {
            if (previewComposite.isDisposed())
                return;
            var previewBackground = Colors.theme().defaultBackground();
            previewComposite.setBackground(previewBackground);
            positiveArrowLabel.setBackground(previewBackground);
            positiveArrowLabel.setImage(scheme.isCustom() && customPositiveArrowRGB != null
                            ? ValueColorScheme.getArrowImage(customPositiveArrowRGB, true)
                            : scheme.upArrow());
            positiveArrowLabel.redraw();
            negativeArrowLabel.setBackground(previewBackground);
            negativeArrowLabel.setImage(scheme.isCustom() && customNegativeArrowRGB != null
                            ? ValueColorScheme.getArrowImage(customNegativeArrowRGB, false)
                            : scheme.downArrow());
            negativeArrowLabel.redraw();
            positiveLabel.setBackdropColor(previewBackground);
            positiveLabel.setTextColor(scheme.isCustom() ? Colors.getColor(customPositiveRGB) : scheme.positiveForeground());
            positiveLabel.redraw();
            negativeLabel.setBackdropColor(previewBackground);
            negativeLabel.setTextColor(scheme.isCustom() ? Colors.getColor(customNegativeRGB) : scheme.negativeForeground());
            negativeLabel.redraw();
            previewComposite.redraw();
        };
        previewBackgroundUpdaters.add(updatePreviewBackground);
        Display.getDefault().asyncExec(updatePreviewBackground);

        return radioButton;
    }

    private void selectScheme(Button targetRadioButton)
    {
        for (var button : valueColorSchemeOptions)
            button.setSelection(button == targetRadioButton);
    }
}
