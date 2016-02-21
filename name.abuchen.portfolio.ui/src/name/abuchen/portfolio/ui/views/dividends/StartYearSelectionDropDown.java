package name.abuchen.portfolio.ui.views.dividends;

import java.time.LocalDate;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.ToolBar;

import name.abuchen.portfolio.ui.util.AbstractDropDown;

/* package */ class StartYearSelectionDropDown extends AbstractDropDown
{
    private DividendsViewModel model;

    public StartYearSelectionDropDown(ToolBar toolBar, DividendsViewModel model)
    {
        super(toolBar, String.valueOf(model.getStartYear()));
        this.model = model;
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        int now = LocalDate.now().getYear();

        for (int ii = 0; ii < 10; ii++)
        {
            int year = now - ii;

            Action action = new Action(String.valueOf(year))
            {
                @Override
                public void run()
                {
                    model.updateWith(year);
                    setLabel(String.valueOf(year));
                }
            };
            action.setChecked(year == model.getStartYear());
            manager.add(action);
        }
    }
}
