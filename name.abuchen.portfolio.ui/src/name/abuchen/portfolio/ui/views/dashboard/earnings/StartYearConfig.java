package name.abuchen.portfolio.ui.views.dashboard.earnings;

import java.text.MessageFormat;
import java.time.LocalDate;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.dashboard.DashboardView;
import name.abuchen.portfolio.ui.views.dashboard.WidgetConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;

public class StartYearConfig implements WidgetConfig
{
    private final WidgetDelegate<?> delegate;

    private int startYear;

    public StartYearConfig(WidgetDelegate<?> delegate, int defaultYearOffset)
    {
        this.delegate = delegate;

        LocalDate today = LocalDate.now();

        String year = delegate.getWidget().getConfiguration().get(Dashboard.Config.START_YEAR.name());
        try
        {
            if (year != null)
                startYear = Integer.parseInt(year);
        }
        catch (NumberFormatException ignore)
        {
            PortfolioPlugin.log(ignore);
        }

        if (startYear < 1900 || startYear > today.getYear())
            startYear = today.getYear() - defaultYearOffset;
    }

    public int getStartYear()
    {
        return startYear;
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME,
                        new LabelOnly(String.valueOf(Messages.LabelSelectYearSince + " " + startYear))); //$NON-NLS-1$

        MenuManager subMenu = new MenuManager(Messages.LabelSelectYearSince + " " + startYear); //$NON-NLS-1$
        manager.add(subMenu);

        int now = LocalDate.now().getYear();

        for (int ii = 0; ii < 10; ii++)
        {
            int year = now - ii;

            SimpleAction action = new SimpleAction(String.valueOf(year), a -> {
                this.startYear = year;

                delegate.getWidget().getConfiguration().put(Dashboard.Config.START_YEAR.name(), String.valueOf(year));

                delegate.update();
                delegate.getClient().touch();
            });
            action.setChecked(year == startYear);
            subMenu.add(action);
        }

        subMenu.add(new Separator());
        subMenu.add(new SimpleAction(Messages.LabelSelectYear, a -> {

            IInputValidator validator = newText -> {
                try
                {
                    int year = Integer.parseInt(newText);
                    return year >= 1900 && year <= now ? null
                                    : MessageFormat.format(Messages.MsgErrorDividendsYearBetween1900AndNow,
                                                    String.valueOf(now));
                }
                catch (NumberFormatException nfe)
                {
                    return MessageFormat.format(Messages.MsgErrorDividendsYearBetween1900AndNow, String.valueOf(now));
                }
            };

            InputDialog dialog = new InputDialog(Display.getDefault().getActiveShell(), Messages.LabelYear,
                            Messages.LabelPaymentsSelectStartYear, String.valueOf(startYear), validator);

            if (dialog.open() == Window.OK)
            {
                this.startYear = Integer.parseInt(dialog.getValue());

                delegate.getWidget().getConfiguration().put(Dashboard.Config.START_YEAR.name(),
                                String.valueOf(startYear));

                delegate.update();
                delegate.getClient().touch();
            }

        }));
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelSelectYearSince + " " + startYear; //$NON-NLS-1$
    }
}
