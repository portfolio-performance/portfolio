package name.abuchen.portfolio.ui.util;

import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;

public final class NumberVerifyListener implements VerifyListener
{
    private static final String ALLOWED_CHARS = ",.0123456789"; //$NON-NLS-1$
    private static final String ALLOWED_NEGATIVE = "-" + ALLOWED_CHARS; //$NON-NLS-1$

    private String pattern;

    public NumberVerifyListener()
    {
        this(false);
    }

    public NumberVerifyListener(boolean allowNegativeValues)
    {
        this.pattern = allowNegativeValues ? ALLOWED_NEGATIVE : ALLOWED_CHARS;
    }

    @Override
    public void verifyText(VerifyEvent e)
    {
        for (int ii = 0; e.doit && ii < e.text.length(); ii++)
            e.doit = pattern.indexOf(e.text.charAt(0)) >= 0;
    }
}
