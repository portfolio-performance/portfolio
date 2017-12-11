package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.preference.IPreferenceStore;

import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;

public class DateUtils
{
    public static boolean useTransactionDateTime()
    {
        IPreferenceStore store = PortfolioPlugin.getDefault().getPreferenceStore();
        return store.getBoolean(UIConstants.Preferences.USE_DATE_TIME);
    }
    
    public static String formatTransactionDate(Transaction transaction)
    {
        if (useTransactionDateTime())
        {
            return Values.DateTime.format(transaction.getDateTime());
        }
        else
        {
            return Values.Date.format(transaction.getDate());
        }
    }

    public static int getTransactionDateColumnWidth()
    {
        return useTransactionDateTime() ? 120 : 80;
    }
}
