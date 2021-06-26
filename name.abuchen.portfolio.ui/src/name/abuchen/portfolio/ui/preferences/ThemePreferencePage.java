package name.abuchen.portfolio.ui.preferences;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

@SuppressWarnings("restriction")
public final class ThemePreferencePage extends PreferencePage
{
    private IThemeEngine engine;
    private ComboViewer themeIdCombo;
    private ITheme currentTheme;
    private String defaultTheme = "name.abuchen.portfolio.light"; //$NON-NLS-1$

    private int currentFontSize = -1;
    private ComboViewer fontSizeCombo;

    public ThemePreferencePage(IThemeEngine themeEngine)
    {
        this.engine = themeEngine;

        setTitle(Messages.LabelTheme);

        this.currentFontSize = readFontSizeFromCSS();
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
        themeIdCombo.setInput(getCSSThemes());
        themeIdCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        this.currentTheme = engine.getActiveTheme();
        if (this.currentTheme != null)
        {
            themeIdCombo.setSelection(new StructuredSelection(currentTheme));
        }
        ControlDecoration themeComboDecorator = new ControlDecoration(themeIdCombo.getCombo(), SWT.TOP | SWT.LEFT);
        themeIdCombo.addSelectionChangedListener(event -> {
            ITheme selection = getSelectedTheme();
            if (!selection.equals(currentTheme))
            {
                themeComboDecorator.setDescriptionText(Messages.MsgThemeRestartRequired);
                Image decorationImage = FieldDecorationRegistry.getDefault()
                                .getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage();
                themeComboDecorator.setImage(decorationImage);
                themeComboDecorator.show();
            }
            else
            {
                themeComboDecorator.hide();
            }
        });

        // font size

        label = new Label(area, SWT.NONE);
        label.setText(Messages.LabelFontSize);

        fontSizeCombo = new ComboViewer(area, SWT.READ_ONLY);
        fontSizeCombo.setLabelProvider(new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                int size = (Integer) element;
                return size == -1 ? Messages.LabelDefaultFontSize : String.valueOf(size) + "px"; //$NON-NLS-1$
            }

        });
        fontSizeCombo.setContentProvider(ArrayContentProvider.getInstance());
        fontSizeCombo.setInput(
                        Stream.concat(Stream.of(-1), IntStream.range(8, 21).boxed()).collect(Collectors.toList()));
        fontSizeCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        fontSizeCombo.setSelection(new StructuredSelection(currentFontSize));

        ControlDecoration fontSizeDecorator = new ControlDecoration(fontSizeCombo.getCombo(), SWT.TOP | SWT.LEFT);
        fontSizeCombo.addSelectionChangedListener(event -> {

            int selectedFontSize = (Integer) fontSizeCombo.getStructuredSelection().getFirstElement();
            if (selectedFontSize != currentFontSize)
            {
                fontSizeDecorator.setDescriptionText(Messages.MsgThemeRestartRequired);
                Image decorationImage = FieldDecorationRegistry.getDefault()
                                .getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage();
                fontSizeDecorator.setImage(decorationImage);
                fontSizeDecorator.show();
            }
            else
            {
                fontSizeDecorator.hide();
            }
        });

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
            engine.setTheme(theme, true);

        int selectedFontSize = (Integer) fontSizeCombo.getStructuredSelection().getFirstElement();
        if (selectedFontSize != currentFontSize)
            writeFrontSizeToCSS(selectedFontSize);

        return super.performOk();
    }

    @Override
    public boolean performCancel()
    {
        if (currentTheme != null)
            engine.setTheme(currentTheme, false);

        return super.performCancel();
    }

    @Override
    protected void performDefaults()
    {
        engine.setTheme(defaultTheme, true);
        if (engine.getActiveTheme() != null)
        {
            themeIdCombo.setSelection(new StructuredSelection(engine.getActiveTheme()));
        }

        fontSizeCombo.setSelection(new StructuredSelection(-1));

        super.performDefaults();
    }

    private Path getPathToCustomCSS() throws IOException
    {
        try
        {
            URL url = FileLocator.resolve(new URL("platform:/meta/name.abuchen.portfolio.ui/custom.css")); //$NON-NLS-1$
            return Path.of(url.toURI());
        }
        catch (URISyntaxException e)
        {
            throw new IOException(e);
        }
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
        catch (IOException | NumberFormatException e)
        {
            PortfolioPlugin.log(e);
            return -1;
        }
    }

    private void writeFrontSizeToCSS(int fontSize)
    {
        try
        {
            String css = ""; //$NON-NLS-1$

            if (fontSize > 0)
            {
                css = String.format("{ font-size: %dpx;}%n.heading1 { font-size: %dpx; }", fontSize, fontSize + 4); //$NON-NLS-1$
            }

            Files.writeString(getPathToCustomCSS(), css, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
        }
    }

}
