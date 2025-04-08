package name.abuchen.portfolio.datatransfer.csv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.DuplicateHeaderMode;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.TextUtil;

/* not thread safe */
public class CSVExporter
{
    /* package */ static final CSVFormat STRATEGY = CSVFormat.DEFAULT.builder() //
                    .setDelimiter(TextUtil.getListSeparatorChar()).setQuote('"').setRecordSeparator("\r\n") //$NON-NLS-1$
                    .setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL).build();

    public void exportAccountTransactions(File file, Account account) throws IOException
    {
        try (CSVPrinter printer = new CSVPrinter(
                        new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), STRATEGY))
        {
            printer.printRecord(Messages.CSVColumn_Date, //
                            Messages.CSVColumn_Type, //
                            Messages.CSVColumn_Value, //
                            Messages.CSVColumn_TransactionCurrency, //
                            Messages.CSVColumn_Taxes, //
                            Messages.CSVColumn_Shares, //
                            Messages.CSVColumn_ISIN, //
                            Messages.CSVColumn_WKN, //
                            Messages.CSVColumn_TickerSymbol, //
                            Messages.CSVColumn_SecurityName, //
                            Messages.CSVColumn_Note);

            for (AccountTransaction t : account.getTransactions())
            {
                printer.print(t.getDateTime().toString());
                printer.print(t.getType().toString());
                printer.print(Values.Amount.format(t.getType().isDebit() ? -t.getAmount() : t.getAmount()));
                printer.print(t.getCurrencyCode());
                printer.print(t.getType() == AccountTransaction.Type.DIVIDENDS
                                ? Values.Amount.format(t.getUnitSum(Unit.Type.TAX).getAmount())
                                : ""); //$NON-NLS-1$
                printer.print(t.getShares() != 0 ? Values.Share.format(t.getShares()) : ""); //$NON-NLS-1$

                printSecurityInfo(printer, t);

                printer.print(escapeNull(t.getNote()));
                printer.println();
            }
        }
    }

    public void exportAccountTransactions(File directory, List<Account> accounts) throws IOException
    {
        for (Account account : accounts)
            exportAccountTransactions(new File(directory, TextUtil.sanitizeFilename(account.getName() + ".csv")), //$NON-NLS-1$
                            account);
    }

    public void exportPortfolioTransactions(File file, Portfolio portfolio) throws IOException
    {
        try (CSVPrinter printer = new CSVPrinter(
                        new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), STRATEGY))
        {
            printer.printRecord(Messages.CSVColumn_Date, //
                            Messages.CSVColumn_Type, //
                            Messages.CSVColumn_Value, //
                            Messages.CSVColumn_TransactionCurrency, //
                            Messages.CSVColumn_GrossAmount, //
                            Messages.CSVColumn_CurrencyGrossAmount, //
                            Messages.CSVColumn_ExchangeRate, //
                            Messages.CSVColumn_Fees, //
                            Messages.CSVColumn_Taxes, //
                            Messages.CSVColumn_Shares, //
                            Messages.CSVColumn_ISIN, //
                            Messages.CSVColumn_WKN, //
                            Messages.CSVColumn_TickerSymbol, //
                            Messages.CSVColumn_SecurityName, //
                            Messages.CSVColumn_Note);

            for (PortfolioTransaction t : portfolio.getTransactions())
            {
                printer.print(t.getDateTime().toString());
                printer.print(t.getType().toString());
                printer.print(Values.Amount.format(t.getType().isLiquidation() ? -t.getAmount() : t.getAmount()));
                printer.print(t.getCurrencyCode());

                // gross amount
                Optional<Unit> grossAmount = t.getUnit(Unit.Type.GROSS_VALUE);
                if (grossAmount.isPresent())
                {
                    Money forex = grossAmount.get().getForex();
                    printer.print(Values.Amount.format(forex.getAmount()));
                    printer.print(forex.getCurrencyCode());
                    printer.print(Values.ExchangeRate.format(grossAmount.get().getExchangeRate()));
                }
                else
                {
                    printer.print(""); //$NON-NLS-1$
                    printer.print(""); //$NON-NLS-1$
                    printer.print(""); //$NON-NLS-1$
                }

                printer.print(Values.Amount.format(t.getUnitSum(Unit.Type.FEE).getAmount()));
                printer.print(Values.Amount.format(t.getUnitSum(Unit.Type.TAX).getAmount()));
                printer.print(Values.Share.format(t.getShares()));

                printSecurityInfo(printer, t);

                printer.print(escapeNull(t.getNote()));

                printer.println();
            }
        }
    }

    private void printSecurityInfo(CSVPrinter printer, Transaction t) throws IOException
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
            exportPortfolioTransactions(new File(directory, TextUtil.sanitizeFilename(portfolio.getName() + ".csv")), //$NON-NLS-1$
                            portfolio);
    }

    public void exportSecurityMasterData(File file, List<Security> securities) throws IOException
    {
        try (CSVPrinter printer = new CSVPrinter(
                        new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), STRATEGY))
        {
            printer.printRecord(Messages.CSVColumn_ISIN, //
                            Messages.CSVColumn_WKN, //
                            Messages.CSVColumn_TickerSymbol, //
                            Messages.CSVColumn_SecurityName, //
                            Messages.CSVColumn_Currency, //
                            Messages.CSVColumn_Note);

            for (Security s : securities)
            {
                printer.print(escapeNull(s.getIsin()));
                printer.print(escapeNull(s.getWkn()));
                printer.print(escapeNull(s.getTickerSymbol()));
                printer.print(escapeNull(s.getName()));
                printer.print(escapeNull(s.getCurrencyCode()));
                printer.print(escapeNull(s.getNote()));
                printer.println();
            }
        }
    }

    public void exportSecurityPrices(File file, Security security) throws IOException
    {
        exportSecurityPrices(Optional.empty(), file, security);
    }

    public void exportSecurityPrices(Optional<CurrencyConverter> converter, File file, Security security)
                    throws IOException
    {
        try (CSVPrinter printer = new CSVPrinter(
                        new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), STRATEGY))
        {
            printer.printRecord(Messages.CSVColumn_Date, Messages.CSVColumn_Quote);

            for (SecurityPrice p : security.getPrices())
            {
                printer.print(p.getDate().toString());
                long value = p.getValue();
                if (converter.isPresent())
                    value = converter.get().convert(p.getDate(), Money.of(security.getCurrencyCode(), value))
                                    .getAmount();
                printer.print(Values.Quote.format(value));
                printer.println();
            }
        }
    }

    public void exportSecurityPrices(File directory, List<Security> securities) throws IOException
    {
        exportSecurityPrices(Optional.empty(), directory, securities);
    }

    public void exportSecurityPrices(Optional<CurrencyConverter> converter, File directory, List<Security> securities)
                    throws IOException
    {
        for (Security security : securities)
            exportSecurityPrices(converter, new File(directory, security.getIsin() + ".csv"), security); //$NON-NLS-1$
    }

    public void exportMergedSecurityPrices(Optional<CurrencyConverter> converter, File file, List<Security> securities)
                    throws IOException
    {
        // prepare: (a) find earliest date (b) ignore securities w/o quotes
        LocalDate earliestDate = null;
        List<Security> export = new ArrayList<>(securities.size());

        for (Security s : securities)
        {
            List<SecurityPrice> prices = s.getPrices();
            if (!prices.isEmpty())
            {
                export.add(s);

                LocalDate quoteDate = prices.get(0).getDate();
                if (earliestDate == null)
                    earliestDate = quoteDate;
                else
                    earliestDate = earliestDate.isAfter(quoteDate) ? quoteDate : earliestDate;
            }
        }

        try (CSVPrinter printer = new CSVPrinter(
                        new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), STRATEGY))
        {
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
                    printer.print(pointer.toString());

                    for (ii = 0; ii < indices.length; ii++)
                    {
                        if (indices[ii] < 0)
                            printer.print(""); //$NON-NLS-1$
                        else
                        {
                            long value = export.get(ii).getPrices().get(indices[ii]).getValue();
                            if (converter.isPresent())
                                value = converter.get()
                                                .convert(pointer, Money.of(export.get(ii).getCurrencyCode(), value))
                                                .getAmount();
                            printer.print(Values.Quote.format(value));
                        }
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
