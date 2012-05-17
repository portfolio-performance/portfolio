package name.abuchen.portfolio.ui.app;

import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.splash.BasicSplashHandler;

public class VersionNumberSplashHandler extends BasicSplashHandler
{

    @Override
    public void init(final Shell splash)
    {
        super.init(splash);

        setProgressRect(new Rectangle(5,194,445,10));
        setMessageRect(new Rectangle(193,120,257,20));
        setForeground(new RGB(255, 255, 255));

        getContent().addPaintListener(new PaintListener()
        {
            @Override
            public void paintControl(PaintEvent event)
            {
                String version = PortfolioPlugin.getDefault().getBundle().getVersion().toString();

                event.gc.setForeground(splash.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                event.gc.drawText("Version " + version, 193, 100, true); //$NON-NLS-1$
            }
        });
    }

}
