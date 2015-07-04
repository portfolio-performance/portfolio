package name.abuchen.portfolio.datatransfer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Values;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVStrategy;

/* not thread safe */
public class CSVExporter
{
    /* package */static final CSVStrategy STRATEGY = new CSVStrategy(';', '"', CSVStrategy.COMMENTS_DISABLED,
                    CSVStrategy.ESCAPE_DISABLED, false, false, false, false);

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$
    private NumberFormat currencyFormat = new DecimalFormat("#,##0.00"); //$NON-NLS-1$

    public void exportAccountTransactions(File file, Account account) throws IOException
    {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
        {
            CSVPrinter printer = new CSVPrinter(writer);
            printer.setStrategy(STRATEGY);

            printer.println(new String[] { Messages.CSVColumn_Date, //
                            Messages.CSVColumn_Type, //
                            Messages.CSVColumn_Value, //
                            Messages.CSVColumn_ISIN, //
                            Messages.CSVColumn_WKN, //
                            Messages.CSVColumn_TickerSymbol, //
                            Messages.CSVColumn_Description });

            for (AccountTransaction t : account.getTransactions())
            {
                printer.print(dateFormat.format(t.getDate()));
                printer.print(t.getType().toString());
                printer.print(currencyFormat.format(t.getAmount() / Values.Amount.divider()));

                printSecurityInfo(printer, t);

                printer.println();
            }
        }
    }

    public void exportAccountTransactions(File directory, List<Account> accounts) throws IOException
    {
        for (Account account : accounts)
            exportAccountTransactions(new File(directory, account.getName() + ".csv"), account); //$NON-NLS-1$
    }

    public void exportPortfolioTransactions(File file, Portfolio portfolio) throws IOException
    {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
        {
            CSVPrinter printer = new CSVPrinter(writer);
            printer.setStrategy(STRATEGY);

            printer.println(new String[] { Messages.CSVColumn_Date, //
                            Messages.CSVColumn_Type, //
                            Messages.CSVColumn_Value, //
                            Messages.CSVColumn_Fees, //
                            Messages.CSVColumn_Taxes, //
                            Messages.CSVColumn_Shares, //
                            Messages.CSVColumn_ISIN, //
                            Messages.CSVColumn_WKN, //
                            Messages.CSVColumn_TickerSymbol, //
                            Messages.CSVColumn_Description });

            for (PortfolioTransaction t : portfolio.getTransactions())
            {
                printer.print(dateFormat.format(t.getDate()));
                printer.print(t.getType().toString());
                printer.print(currencyFormat.format(t.getAmount() / Values.Amount.divider()));
                printer.print(currencyFormat.format(t.getFees() / Values.Amount.divider()));
                printer.print(currencyFormat.format(t.getTaxes() / Values.Amount.divider()));
                printer.print(Values.Share.format(t.getShares()));

                printSecurityInfo(printer, t);

                printer.println();
            }
        }
    }

    private void printSecurityInfo(CSVPrinter printer, Transaction t)
    {
        Security security = t.getSecurity();
        if (security != null)
        {
            printer.print(escapeNull(security.getIsin()));
            printer.print(escapeNull(security.getWkn()));
            printer.print(escapeNull(security.getTickerSymbol()));
            printer.print(escapeNull(security.getName()));
        }
        else
        {
            printer.print(""); //$NON-NLS-1$
            printer.print(""); //$NON-NLS-1$
            printer.print(""); //$NON-NLS-1$
            printer.print(""); //$NON-NLS-1$
        }
    }

    public void exportPortfolioTransactions(File directory, List<Portfolio> portfolios) throws IOException
    {
        for (Portfolio portfolio : portfolios)
            exportPortfolioTransactions(new File(directory, portfolio.getName() + ".csv"), portfolio); //$NON-NLS-1$
    }

    public void exportSecurityMasterData(File file, List<Security> securities) throws IOException
    {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
        {
            CSVPrinter printer = new CSVPrinter(writer);
            printer.setStrategy(STRATEGY);

            printer.println(new String[] { Messages.CSVColumn_ISIN, //
                            Messages.CSVColumn_WKN, //
                            Messages.CSVColumn_TickerSymbol, //
                            Messages.CSVColumn_Description, //
                            Messages.CSVColumn_TickerSymbol });

            for (Security s : securities)
            {
                printer.print(escapeNull(s.getIsin()));
                printer.print(escapeNull(s.getWkn()));
                printer.print(escapeNull(s.getTickerSymbol()));
                printer.print(escapeNull(s.getName()));
                printer.print(escapeNull(s.getTickerSymbol()));
                printer.println();
            }
        }
    }

    public void exportSecurityPrices(File file, Security security) throws IOException
    {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
        {
            CSVPrinter printer = new CSVPrinter(writer);
            printer.setStrategy(STRATEGY);

            printer.println(new String[] { Messages.CSVColumn_Date, Messages.CSVColumn_Quote });

            for (SecurityPrice p : security.getPrices())
            {
                printer.print(dateFormat.format(p.getTime()));
                printer.print(currencyFormat.format(p.getValue() / Values.Quote.divider()));
                printer.println();
            }
        }
    }

    public void exportSecurityPrices(File directory, List<Security> securities) throws IOException
    {
        for (Security security : securities)
            exportSecurityPrices(new File(directory, security.getIsin() + ".csv"), security); //$NON-NLS-1$
    }

    public void exportMergedSecurityPrices(File file, List<Security> securities) throws IOException
    {
        // prepare: (a) find earliest date (b) ignore securities w/o quotes
        LocalDate earliestDate = null;
        List<Security> export = new ArrayList<Security>(securities.size());

        for (Security s : securities)
        {
            List<SecurityPrice> prices = s.getPrices();
            if (!prices.isEmpty())
            {
                export.add(s);

                LocalDate quoteDate = prices.get(0).getTime();
                if (earliestDate == null)
                    earliestDate = quoteDate;
                else
                    earliestDate = earliestDate.isAfter(quoteDate) ? quoteDate : earliestDate;
            }
        }

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
        {
            CSVPrinter printer = new CSVPrinter(writer);
            printer.setStrategy(STRATEGY);

            // write header
            printer.print(Messages.CSVColumn_Date);
            for (Security security : export)
                printer.print(security.getExternalIdentifier());
            printer.println();

            // stop if no securities exist
            if (earliestDate == null)
                return;

            // write quotes
            LocalDate pointer = earliestDate;
            LocalDate today = LocalDate.now();

            while (pointer.compareTo(today) <= 0)
            {
                // check if any quotes exist for that day at all
                int[] indices = new int[export.size()];

                int ii = 0;
                for (Security security : export)
                {
                    SecurityPrice p = new SecurityPrice(pointer, 0);
                    indices[ii] = Collections.binarySearch(security.getPrices(), p);
                    ii++;
                }

                boolean hasValues = false;
                for (ii = 0; ii < indices.length && !hasValues; ii++)
                    hasValues = indices[ii] >= 0;

                if (hasValues)
                {
                    printer.print(Values.Date.format(pointer));

                    for (ii = 0; ii < indices.length; ii++)
                    {
                        if (indices[ii] < 0)
                            printer.print(""); //$NON-NLS-1$
                        else
                            printer.print(Values.Quote.format(export.get(ii).getPrices().get(indices[ii]).getValue()));
                    }

                    printer.println();
                }

                pointer = pointer.plusDays(1);
            }

        }
    }

    /* package */static String escapeNull(String value)
    {
        return value != null ? value : ""; //$NON-NLS-1$
    }
}
