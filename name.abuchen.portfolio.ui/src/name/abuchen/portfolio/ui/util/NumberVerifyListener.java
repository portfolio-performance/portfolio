package name.abuchen.portfolio.ui.util;

import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;

public final class NumberVerifyListener implements VerifyListener
{
    private String allowedChars = ",.0123456789"; //$NON-NLS-1$

    @Override
    public void verifyText(VerifyEvent e)
    {
        for (int ii = 0; e.doit && ii < e.text.length(); ii++)
            e.doit = allowedChars.indexOf(e.text.charAt(0)) >= 0;
    }
}
