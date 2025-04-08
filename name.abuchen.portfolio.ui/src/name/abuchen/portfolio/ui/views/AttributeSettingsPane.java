package name.abuchen.portfolio.ui.views;

import java.text.MessageFormat;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LimitPrice;
import name.abuchen.portfolio.model.LimitPriceSettings;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.SWTHelper;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;
import name.abuchen.portfolio.util.Triple;

public class AttributeSettingsPane implements InformationPanePage
{

    @Inject
    private IStylingEngine stylingEngine;

    @Inject
    private Client client;

    private Composite viewContainer;

    @Override
    public String getLabel()
    {
        return Messages.AttributeSettings;
    }

    @Override
    public Control createViewControl(Composite parent)
    {
        viewContainer = new Composite(parent, SWT.NONE);
        viewContainer.setLayout(new FillLayout());
        return viewContainer;
    }

    @Override
    public void setInput(Object input)
    {
        for (Control control : viewContainer.getChildren())
        {
            if (!control.isDisposed())
                control.dispose();
        }

        if (input instanceof AttributeType attr)
        {
            createAttributSettingsView(attr, viewContainer);
        }

        viewContainer.layout(true);
    }

    private void createAttributSettingsView(AttributeType attribute, Composite parent)
    {
        Class<?> type = attribute.getType();
        if (type == LimitPrice.class)
        {
            LimitPriceSettingsViewFactory.createView(attribute, parent, stylingEngine, client);
        }
        else
        {
            Composite c = new Composite(parent, SWT.NONE);
            c.setLayout(new GridLayout(1, true));
            Label lb = new Label(c, SWT.NONE);
            lb.setText(MessageFormat.format(Messages.AttributeSettings_NoSettingAvailable, attribute.getName()));
            lb.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
        }
    }

    @Override
    public void onRecalculationNeeded()
    {
    }

    private static class LimitPriceSettingsViewFactory
    {

        private static void createView(AttributeType attribute, Composite parent, IStylingEngine stylingEngine,
                        Client client)
        {
            LimitPriceSettings settings = new LimitPriceSettings(attribute.getProperties());
            Composite container = new Composite(parent, SWT.NONE);
            RowLayout layout = new RowLayout(SWT.HORIZONTAL);
            container.setLayout(layout);

            createColorView(container, stylingEngine, settings, client);
            createDistanceView(container, settings, client);
        }

        private static void createDistanceView(Composite parent, LimitPriceSettings settings, Client client)
        {
            // ---------------------------------------------------
            // -- Distance settings
            Group gpDistance = new Group(parent, SWT.NONE);
            gpDistance.setText(Messages.AttributeSettings_LimitPrice_ColumnSettings);
            gpDistance.setLayout(new RowLayout(SWT.VERTICAL));

            Button btn = new Button(gpDistance, SWT.CHECK);
            btn.setText(Messages.AttributeSettings_LimitPrice_ColumnSettings_ShowRelativeDiff);
            btn.setSelection(settings.getShowRelativeDiff());
            btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
                settings.setShowRelativeDiff(((Button) event.getSource()).getSelection());
                client.touch();
            }));

            btn = new Button(gpDistance, SWT.CHECK);
            btn.setText(Messages.AttributeSettings_LimitPrice_ColumnSettings_ShowAbsoluteDiff);
            btn.setSelection(settings.getShowAbsoluteDiff());
            btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
                settings.setShowAbsoluteDiff(((Button) event.getSource()).getSelection());
                client.touch();
            }));
        }

        private static void createColorView(Composite parent, IStylingEngine stylingEngine, LimitPriceSettings settings,
                        Client client)
        {
            // ---------------------------------------------------
            // -- Color settings
            Group gpColors = new Group(parent, SWT.NONE);
            gpColors.setText(Messages.AttributeSettings_LimitPrice_ColorSettings);
            gpColors.setLayout(new FormLayout());

            Triple<CLabel, ColoredLabel, Button> positiveExceeded = createColorButtons(gpColors,
                            settings::getLimitExceededPositivelyColor, settings::setLimitExceededPositivelyColor,
                            Messages.AttributeSettings_LimitPrice_ColorSettings_LimitExceededPositively,
                            () -> Colors.theme().greenBackground(), client);

            Triple<CLabel, ColoredLabel, Button> negativelyExceeded = createColorButtons(gpColors,
                            settings::getLimitExceededNegativelyColor, settings::setLimitExceededNegativelyColor,
                            Messages.AttributeSettings_LimitPrice_ColorSettings_LimitExceededNegatively,
                            () -> Colors.theme().redBackground(), client);

            // swap colors button
            Button button = new Button(gpColors, SWT.PUSH);
            button.setText(Messages.AttributeSettings_LimitPrice_ColorSettings_SwapColors);
            button.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
                // swap settings
                Color oldPositive = settings.getLimitExceededPositivelyColor();
                Color oldNegative = settings.getLimitExceededNegativelyColor();

                // if needed, set default values
                if (oldPositive == null)
                    oldPositive = Colors.theme().greenBackground();
                if (oldNegative == null)
                    oldNegative = Colors.theme().redBackground();

                // check if we swapped back to default colors
                if (oldPositive.equals(Colors.theme().redBackground()))
                    oldPositive = null;

                if (oldNegative.equals(Colors.theme().greenBackground()))
                    oldNegative = null;

                settings.setLimitExceededNegativelyColor(oldPositive);
                settings.setLimitExceededPositivelyColor(oldNegative);
                client.touch();

                // swap label colors
                positiveExceeded.getSecond()
                                .setBackdropColor(oldNegative != null ? oldNegative : Colors.theme().greenBackground());
                positiveExceeded.getSecond().redraw();
                negativelyExceeded.getSecond()
                                .setBackdropColor(oldPositive != null ? oldPositive : Colors.theme().redBackground());
                negativelyExceeded.getSecond().redraw();
            }));

            // reset to default colors button
            Button resetButton = new Button(gpColors, SWT.PUSH);
            resetButton.setText(Messages.BtnLabelResetToDefault);
            resetButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
                // reset settings
                settings.setLimitExceededPositivelyColor(null);
                settings.setLimitExceededNegativelyColor(null);
                client.touch();

                // set label colors
                positiveExceeded.getSecond().setBackdropColor(Colors.theme().greenBackground());
                positiveExceeded.getSecond().redraw();
                negativelyExceeded.getSecond().setBackdropColor(Colors.theme().redBackground());
                negativelyExceeded.getSecond().redraw();
            }));

            // measuring the width requires that the font has been applied
            // before
            stylingEngine.style(gpColors);

            // Compute the widest element width in the group using
            int width = SWTHelper.widest(positiveExceeded.getFirst(), negativelyExceeded.getFirst(), button,
                            resetButton);

            // Set the width of the gpColors group to the widest element's width
            // plus padding
            gpColors.setSize(width, gpColors.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);

            FormDataFactory.startingWith(positiveExceeded.getFirst()).width(width + 20) //
                            .thenBelow(negativelyExceeded.getFirst()).width(width + 20) //
                            .thenBelow(button).left(new FormAttachment(0)).thenRight(resetButton);

            stylingEngine.style(gpColors);
        }

        private static Triple<CLabel, ColoredLabel, Button> createColorButtons(Composite parent,
                        Supplier<Color> getColor, Consumer<Color> setColor, String labelText,
                        Supplier<Color> getThemeColor, Client client)
        {
            CLabel textLabel = new CLabel(parent, SWT.NONE);
            textLabel.setText(labelText);

            ColoredLabel colorLabel = new ColoredLabel(parent, SWT.CENTER | SWT.BORDER);
            Color color = getColor.get();
            if (color == null)
                color = getThemeColor.get();
            colorLabel.setBackground(color);
            colorLabel.setBackdropColor(color);
            colorLabel.setSize(300, 100);

            colorLabel.setText("            "); //$NON-NLS-1$

            Button button = new Button(parent, SWT.PUSH);
            button.setText("..."); //$NON-NLS-1$
            button.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
                // Create the color-change dialog
                ColorDialog dlg = new ColorDialog(Display.getDefault().getActiveShell());

                // Set the selected color in the dialog from
                // user's selected color

                Color color1 = getColor.get();
                if (color1 == null)
                    color1 = getThemeColor.get();
                dlg.setRGB(color1.getRGB());

                // Change the title bar text
                dlg.setText("Choose a Color"); //$NON-NLS-1$

                // Open the dialog and retrieve the selected color
                RGB rgb = dlg.open();
                if (rgb != null)
                {
                    setColor.accept(new Color(rgb));
                    colorLabel.setBackground(getColor.get());
                    colorLabel.setBackdropColor(getColor.get());
                    client.touch();
                }
            }));

            FormDataFactory.startingWith(textLabel).thenRight(colorLabel).thenRight(button);

            return new Triple<>(textLabel, colorLabel, button);
        }
    }
}
