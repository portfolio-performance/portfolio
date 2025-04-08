package name.abuchen.portfolio.ui.util;

import java.text.DecimalFormatSymbols;

import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;

public final class NumberVerifyListener implements VerifyListener
{
    private String pattern;

    public NumberVerifyListener()
    {
        this(false);
    }

    public NumberVerifyListener(boolean allowNegativeValues)
    {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();

        StringBuilder allowedChars = new StringBuilder();
        allowedChars.append("0123456789"); //$NON-NLS-1$
        allowedChars.append(symbols.getGroupingSeparator());
        allowedChars.append(symbols.getDecimalSeparator());
        if (allowNegativeValues)
            allowedChars.append("-"); //$NON-NLS-1$

        this.pattern = allowedChars.toString();
    }

    @Override
    public void verifyText(VerifyEvent e)
    {
        for (int ii = 0; e.doit && ii < e.text.length(); ii++)
            e.doit = pattern.indexOf(e.text.charAt(0)) >= 0;
    }
}
