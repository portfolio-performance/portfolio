package name.abuchen.portfolio.ui.preferences;

import java.util.ArrayList;
import java.util.List;

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

@SuppressWarnings("restriction")
public class ThemePreferencePage extends PreferencePage
{
    private IThemeEngine engine;
    private ComboViewer themeIdCombo;
    private ITheme currentTheme;
    private String defaultTheme = "name.abuchen.portfolio.light"; //$NON-NLS-1$

    public ThemePreferencePage(IThemeEngine themeEngine)
    {
        this.engine = themeEngine;

        setTitle(Messages.LabelTheme);
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

        super.performDefaults();
    }

}
