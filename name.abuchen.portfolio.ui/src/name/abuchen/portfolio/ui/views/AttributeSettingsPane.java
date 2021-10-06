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
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.LimitPrice;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.ui.views.columns.LimitPriceSettings;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;

public class AttributeSettingsPane implements InformationPanePage
{

    @Override
    public String getLabel()
    {
        return "Attribute Settings";
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
            createLimitPriceSettingsView(attribute, parent);            
        }
        else
        {          
            Label lb = new Label(parent, SWT.BORDER);
            lb.setText("no setting for " + attribute.getName());
        }
        
    }
    
    private void createLimitPriceSettingsView(AttributeType attribute, Composite parent)
    {       
        LimitPriceSettings settings = new LimitPriceSettings(attribute.getProperties());
        Composite container = new Composite(parent, SWT.NONE);
        RowLayout layout = new RowLayout(SWT.VERTICAL);
        container.setLayout(layout);

        Button btn = new Button(container, SWT.CHECK);
        btn.setText("ShowRelativeDiff"); //$NON-NLS-1$
        btn.setSelection(settings.getShowRelativeDiff());
        btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            settings.setShowRelativeDiff(((Button)event.getSource()).getSelection());
        }));
        container.setBackground(Colors.RED);


        btn = new Button(container, SWT.CHECK);
        btn.setText("ShowAbsoluteDiff"); //$NON-NLS-1$
        btn.setSelection(settings.getShowAbsoluteDiff());
        btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            settings.setShowAbsoluteDiff(((Button)event.getSource()).getSelection());
        }));

        ColoredLabel exceedLabel = createColorButtons(container, settings::getLimitExceededColor, settings::setLimitExceededColor, "LimitExceededColor: ", () -> Colors.theme().greenBackground()); //$NON-NLS-1$
        ColoredLabel undercutLabel = createColorButtons(container, settings::getLimitUndercutColor, settings::setLimitUndercutColor, "LimitUndercutColor: ", () -> Colors.theme().redBackground()); //$NON-NLS-1$                      

        // swap colors button
        Button button = new Button(container, SWT.PUSH);
        button.setText("swap colors"); //$NON-NLS-1$
        //button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
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
        
    }
    
    private ColoredLabel createColorButtons(Composite parent, Supplier<Color> getColor, Consumer<Color> setColor, String labelText, Supplier<Color> getThemeColor)
    {
        Composite colorContainer = new Composite(parent, SWT.NONE);
        colorContainer.setLayout(new GridLayout(SWT.HORIZONTAL, false));
        
        CLabel textLabel = new CLabel(colorContainer, SWT.CENTER);
        textLabel.setText(labelText); 
        
        ColoredLabel colorLabel = new ColoredLabel(colorContainer, SWT.CENTER| SWT.BORDER); 
        //colorLabel.setBackgroundMode(SWT.INHERIT_NONE);
        Color color = getColor.get();
        if(color == null)
            color = getThemeColor.get();
        colorLabel.setBackground(color); 
        colorLabel.setBackdropColor(color);
        colorLabel.setSize(300, 100); 
        colorLabel.setLayoutData( new GridData( SWT.CENTER, SWT.CENTER, false, false ) );
        
        colorLabel.setText("            "); //$NON-NLS-1$
         
        Button button = new Button(colorContainer, SWT.PUSH);
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

    @Override
    public void onRecalculationNeeded()
    {
        // TODO Auto-generated method stub
        
    }

}
