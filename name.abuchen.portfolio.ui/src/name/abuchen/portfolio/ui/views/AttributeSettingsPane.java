package name.abuchen.portfolio.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.LimitPrice;
import name.abuchen.portfolio.ui.views.panes.InformationPanePage;

public class AttributeSettingsPane implements InformationPanePage
{

    @Override
    public String getLabel()
    {
        // TODO Auto-generated method stub
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

    Control currentSettingsView;
    
    @Override
    public void setInput(Object input)
    {
        if(currentSettingsView != null)
            currentSettingsView.dispose();
        // TODO entsprechend selektiertem Attribut eine View erzeugen und anzeigen
        if(input instanceof AttributeType)
        {
            
            AttributeType attr = (AttributeType)input;
            currentSettingsView = createAttributSettingsView(attr, viewContainer);
            //lb.setText(attr.getColumnLabel());
        }
        
                
        viewContainer.layout(true);
    }
    
    private Control createAttributSettingsView(AttributeType attribute, Composite parent)
    {
        Class<?> type = attribute.getType();
        if(type == LimitPrice.class)
        {
            // TODO: Limitprice settingsview erzeugen
            Label lb = new Label(parent, SWT.BORDER);
            lb.setText(attribute.getName());
            return lb;
        }
        else
        {
            
            Label lb = new Label(parent, SWT.BORDER);
            lb.setText("no setting for " + attribute.getName());
            return lb;
        }
        
    }

    @Override
    public void onRecalculationNeeded()
    {
        // TODO Auto-generated method stub
        
    }

}
