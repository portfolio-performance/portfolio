package name.abuchen.portfolio.server.resources;

import java.time.LocalDate;

import name.abuchen.portfolio.json.JSecurityPerformanceRecord;
import name.abuchen.portfolio.json.JSecurityPosition;
import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.StatementOfAssetsViewer;
import name.abuchen.portfolio.util.Interval;

public class JAssetElement
{
    private JSecurityPosition position;
    private String classification;
    private Money valuation;
    private JSecurityPerformanceRecord performance;
    private String text;
    
    public JSecurityPosition getPosition()
    {
        return position;
    }
    
    public void setPosition(JSecurityPosition position)
    {
        this.position = position;
    }
    
    public String getClassification()
    {
        return classification;
    }
    
    public void setClassification(String classification)
    {
        this.classification = classification;
    }
    
    public static JAssetElement from(StatementOfAssetsViewer.Element element)
    {
        var allTime = Interval.of(LocalDate.MIN, LocalDate.now());
        
        JAssetElement e = new JAssetElement();
        
        if(element.isCategory()) 
        {
            e.classification = element.getCategory().getClassification().getName();
        
        } 
        else if(element.isPosition()) 
        {
            e.position = JSecurityPosition.from(element.getSecurityPosition());
        } 

        var performance = element.getPerformance(element.getGroupByTaxonomy().getCurrencyConverter().getTermCurrency(), allTime);
        
        if(performance != null) 
        {
            e.performance = JSecurityPerformanceRecord.from(performance);
        }
        
        if(element.isGroupByTaxonomy()) 
        {
            e.text = Messages.LabelTotalSum;
        } 
        else 
        {        
            Named n = Adaptor.adapt(Named.class, element);
            e.text = n != null ? n.getName() : null;
        }            

        e.valuation = element.getValuation();
        
        return e;
    }

    public Money getValuation()
    {
        return valuation;
    }

    public JSecurityPerformanceRecord getPerformance()
    {
        return performance;
    }

    public String getText()
    {
        return text;
    }
    
}
