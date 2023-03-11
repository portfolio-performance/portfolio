package name.abuchen.portfolio.ui.survey;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.swt.StyledLabel;

public class SurveyReminder extends Composite
{

    public SurveyReminder(Composite parent)
    {
        super(parent, SWT.NONE);
        
        setData(UIConstants.CSS.DISABLE_CSS_STYLING, Boolean.TRUE);
        setBackground(Colors.theme().warningBackground());
        
        StyledLabel label = new StyledLabel(this, SWT.NONE);
        label.setText(Messages.SurveyReminder);
        label.setForeground(Colors.theme().defaultForeground());
        
        FillLayout layout = new FillLayout();
        layout.marginHeight = 5;
        layout.marginWidth = 5;
        setLayout(layout);
    }
}
