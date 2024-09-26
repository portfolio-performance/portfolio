package name.abuchen.portfolio.ui.util.text;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.util.TextUtil;

public class FrenchKeypadSupport
{
    private static final boolean IS_FRENCH = Locale.getDefault().getLanguage().equals(Locale.FRENCH.getLanguage());

    private FrenchKeypadSupport()
    {
    }

    public static void configure(Text text)
    {
        if (IS_FRENCH)
        {
            text.addKeyListener(new KeyAdapter()
            {
                @Override
                public void keyPressed(KeyEvent e)
                {
                    if ((e.keyCode == SWT.KEYPAD_DECIMAL))
                    {
                        e.doit = false;
                        text.insert(String.valueOf(TextUtil.DECIMAL_SEPARATOR));
                    }
                }
            });
        }
    }
}
