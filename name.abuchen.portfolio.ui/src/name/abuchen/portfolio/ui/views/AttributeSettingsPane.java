package name.abuchen.portfolio.ui.views;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
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
import name.abuchen.portfolio.model.LimitPrice;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.ui.views.columns.LimitPriceSettings;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;

public class AttributeSettingsPane implements InformationPanePage
{

    @Override
    public String getLabel()
    {
        return Messages.AttributeSettings;
    }
    
    private Composite viewContainer;
    
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
            if(!control.isDisposed())
                control.dispose();
        }
        
        if(input instanceof AttributeType)
        {
            AttributeType attr = (AttributeType)input;
            createAttributSettingsView(attr, viewContainer);
        }        
                
        viewContainer.layout(true);
    }
    
    private void createAttributSettingsView(AttributeType attribute, Composite parent)
    {
        Class<?> type = attribute.getType();
        if(type == LimitPrice.class)
        {
            LimitPriceSettingsViewFactory.createView(attribute, parent);            
        }
        else
        {          
            Composite c = new Composite(parent, SWT.NONE);
            c.setLayout(new GridLayout(1, true));
            Label lb = new Label(c, SWT.NONE);
            lb.setText(Messages.AttributeSettings_NoSettingAvailable + attribute.getName());
            lb.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));           
        }        
    }  
    
    @Override
    public void onRecalculationNeeded()
    {
        // TODO Auto-generated method stub        
    }
    
    private static class LimitPriceSettingsViewFactory
    {
        
        private static void createView(AttributeType attribute, Composite parent)
        {       
            LimitPriceSettings settings = new LimitPriceSettings(attribute.getProperties());
            Composite container = new Composite(parent, SWT.NONE);
            RowLayout layout = new RowLayout(SWT.HORIZONTAL);
            container.setLayout(layout);

            createColorView(container, settings);
            createDistanceView(container, settings);          
            
        }
        
        
        private static void createDistanceView(Composite parent, LimitPriceSettings settings)
        {
          //---------------------------------------------------
            //-- Distance settings
            Group gpDistance = new Group(parent, SWT.NONE);
            gpDistance.setText(Messages.AttributeSettings_LimitPrice_ColumnSettings);
            gpDistance.setLayout(new RowLayout(SWT.VERTICAL));
            
            Button btn = new Button(gpDistance, SWT.CHECK);
            btn.setText(Messages.AttributeSettings_LimitPrice_ColumnSettings_ShowRelativeDiff);
            btn.setSelection(settings.getShowRelativeDiff());
            btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> settings.setShowRelativeDiff(((Button)event.getSource()).getSelection())));

            btn = new Button(gpDistance, SWT.CHECK);
            btn.setText(Messages.AttributeSettings_LimitPrice_ColumnSettings_ShowAbsoluteDiff);
            btn.setSelection(settings.getShowAbsoluteDiff());
            btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> settings.setShowAbsoluteDiff(((Button)event.getSource()).getSelection())));
        }
        
        private static void createColorView(Composite parent, LimitPriceSettings settings)
        {
          //---------------------------------------------------
            //-- Color settings
            Group gpColors = new Group(parent, SWT.NONE);
            gpColors.setText(Messages.AttributeSettings_LimitPrice_ColorSettings);
            gpColors.setLayout(new RowLayout(SWT.VERTICAL));
            
            
            Composite colorContainer = new Composite(gpColors, SWT.NONE);
            colorContainer.setLayout(new GridLayout(3, false));
            ColoredLabel exceedLabel = createColorButtons(colorContainer, settings::getLimitExceededColor, settings::setLimitExceededColor, Messages.AttributeSettings_LimitPrice_ColorSettings_LimitExceededColor, () -> Colors.theme().greenBackground());
            ColoredLabel undercutLabel = createColorButtons(colorContainer, settings::getLimitUndercutColor, settings::setLimitUndercutColor, Messages.AttributeSettings_LimitPrice_ColorSettings_LimitUndercutColor, () -> Colors.theme().redBackground());                      

            Composite buttonsContainer = new Composite(gpColors, SWT.NONE);
            RowLayout layout = new RowLayout(SWT.HORIZONTAL);
            layout.pack = false;
            buttonsContainer.setLayout(layout);
            
            // swap colors button
            Button button = new Button(buttonsContainer, SWT.PUSH);
            button.setText(Messages.AttributeSettings_LimitPrice_ColorSettings_SwapColors);
            button.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> 
            {
                // swap settings
                Color exceed = settings.getLimitExceededColor();
                Color undercut = settings.getLimitUndercutColor();
                settings.setLimitUndercutColor(exceed);
                settings.setLimitExceededColor(undercut);
                
                // swap label colors
                exceedLabel.setBackdropColor(undercut);
                exceedLabel.redraw();
                undercutLabel.setBackdropColor(exceed);
                undercutLabel.redraw();
            })) ;
            
            // reset to default colors button
            Button resetButton = new Button(buttonsContainer, SWT.PUSH);
            resetButton.setText(Messages.AttributeSettings_LimitPrice_ColorSettings_ResetColors);
            resetButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> 
            {
                // swap settings
                Color exceed = Colors.theme().greenBackground();
                Color undercut = Colors.theme().redBackground();
                settings.setLimitExceededColor(exceed);
                settings.setLimitUndercutColor(undercut);
                
                // swap label colors
                exceedLabel.setBackdropColor(exceed);
                exceedLabel.redraw();
                undercutLabel.setBackdropColor(undercut);
                undercutLabel.redraw();
            })) ;            
        }       
       
        
        private static ColoredLabel createColorButtons(Composite parent, Supplier<Color> getColor, Consumer<Color> setColor, String labelText, Supplier<Color> getThemeColor)
        {          
            CLabel textLabel = new CLabel(parent, SWT.CENTER);
            textLabel.setText(labelText); 
            
            ColoredLabel colorLabel = new ColoredLabel(parent, SWT.CENTER| SWT.BORDER); 
            Color color = getColor.get();
            if(color == null)
                color = getThemeColor.get();
            colorLabel.setBackground(color); 
            colorLabel.setBackdropColor(color);
            colorLabel.setSize(300, 100); 
            colorLabel.setLayoutData( new GridData( SWT.CENTER, SWT.CENTER, false, false ) );
            
            colorLabel.setText("            "); //$NON-NLS-1$
             
            Button button = new Button(parent, SWT.PUSH);
            button.setText("..."); //$NON-NLS-1$
            button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
            button.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> 
            {
             // Create the color-change dialog
                ColorDialog dlg = new ColorDialog(Display.getDefault().getActiveShell());

                // Set the selected color in the dialog from
                // user's selected color
                
                Color color1 = getColor.get();
                if(color1 == null)
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
                }
            })) ;
            
            return colorLabel;
        }
    }
}
