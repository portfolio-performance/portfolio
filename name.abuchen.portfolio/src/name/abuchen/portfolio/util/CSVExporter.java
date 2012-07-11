package name.abuchen.portfolio.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Values;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVStrategy;

/* not thread safe */
public class CSVExporter
{
    private static final CSVStrategy strategy = new CSVStrategy(';', '"', CSVStrategy.COMMENTS_DISABLED,
                    CSVStrategy.ESCAPE_DISABLED, false, false, false, false);

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$
    private NumberFormat currencyFormat = new DecimalFormat("#,##0.00"); //$NON-NLS-1$

    public void exportAccountTransactions(File file, Account account) throws IOException
    {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")); //$NON-NLS-1$

        try
        {
            CSVPrinter printer = new CSVPrinter(writer);
            printer.setStrategy(strategy);

            printer.println(new String[] { Messages.CSVColumn_Date, //
                            Messages.CSVColumn_Type, //
                            Messages.CSVColumn_Value, //
                            Messages.CSVColumn_ISIN, //
                            Messages.CSVColumn_Description });

            for (AccountTransaction t : account.getTransactions())
            {
                printer.print(dateFormat.format(t.getDate()));
                printer.print(t.getType().name());
                printer.print(currencyFormat.format(t.getAmount() / Values.Amount.divider()));
                if (t.getSecurity() != null)
                {
                    printer.print(t.getSecurity().getIsin());
                    printer.print(t.getSecurity().getName());
                }
                else
                {
                    printer.print(""); //$NON-NLS-1$
                    printer.print(""); //$NON-NLS-1$
                }
                printer.println();
            }
        }
        finally
        {
            writer.close();
        }
    }

    public void exportAccountTransactions(File directory, List<Account> accounts) throws IOException
    {
        for (Account account : accounts)
            exportAccountTransactions(new File(directory, account.getName() + ".csv"), account); //$NON-NLS-1$
    }

    public void exportPortfolioTransactions(File file, Portfolio portfolio) throws IOException
    {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")); //$NON-NLS-1$

        try
        {
            CSVPrinter printer = new CSVPrinter(writer);
            printer.setStrategy(strategy);

            printer.println(new String[] { Messages.CSVColumn_Date, //
                            Messages.CSVColumn_Type, //
                            Messages.CSVColumn_Value, //
                            Messages.CSVColumn_Fees, //
                            Messages.CSVColumn_Shares, //
                            Messages.CSVColumn_ISIN, //
                            Messages.CSVColumn_Description });

            for (PortfolioTransaction t : portfolio.getTransactions())
            {
                printer.print(dateFormat.format(t.getDate()));
                printer.print(t.getType().name());
                printer.print(currencyFormat.format(t.getAmount() / Values.Amount.divider()));
                printer.print(currencyFormat.format(t.getFees() / Values.Amount.divider()));
                printer.print(Values.Share.format(t.getShares()));

                if (t.getSecurity() != null)
                {
                    printer.print(t.getSecurity().getIsin());
                    printer.print(t.getSecurity().getName());
                }
                else
                {
                    printer.print(""); //$NON-NLS-1$
                    printer.print(""); //$NON-NLS-1$
                }
                printer.println();
            }
        }
        finally
        {
            writer.close();
        }
    }

    public void exportPortfolioTransactions(File directory, List<Portfolio> portfolios) throws IOException
    {
        for (Portfolio portfolio : portfolios)
            exportPortfolioTransactions(new File(directory, portfolio.getName() + ".csv"), portfolio); //$NON-NLS-1$
    }

    public void exportSecurityMasterData(File file, List<Security> securities) throws IOException
    {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")); //$NON-NLS-1$

        try
        {
            CSVPrinter printer = new CSVPrinter(writer);
            printer.setStrategy(strategy);

            printer.println(new String[] { Messages.CSVColumn_ISIN, //
                            Messages.CSVColumn_Description, //
                            Messages.CSVColumn_TickerSymbol, //
                            Messages.CSVColumn_Type });

            for (Security s : securities)
            {
                printer.print(s.getIsin());
                printer.print(s.getName());
                printer.print(s.getTickerSymbol());
                printer.print(s.getType().toString());
                printer.println();
            }
        }
        finally
        {
            writer.close();
        }
    }

    public void exportSecurityPrices(File file, Security security) throws IOException
    {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")); //$NON-NLS-1$

        try
        {
            CSVPrinter printer = new CSVPrinter(writer);
            printer.setStrategy(strategy);

            printer.println(new String[] { Messages.CSVColumn_Date, Messages.CSVColumn_Quote });

            for (SecurityPrice p : security.getPrices())
            {
                printer.print(dateFormat.format(p.getTime()));
                printer.print(currencyFormat.format(p.getValue() / Values.Quote.divider()));
                printer.println();
            }
        }
        finally
        {
            writer.close();
        }
    }

    public void exportSecurityPrices(File directory, List<Security> securities) throws IOException
    {
        for (Security security : securities)
            exportSecurityPrices(new File(directory, security.getIsin() + ".csv"), security); //$NON-NLS-1$
    }

}
