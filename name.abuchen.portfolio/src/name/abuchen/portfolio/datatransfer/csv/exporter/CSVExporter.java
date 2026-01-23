package name.abuchen.portfolio.datatransfer.csv.exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.DuplicateHeaderMode;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
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
                    .setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL).get();

    public void exportTransactions(File file, List<? extends Transaction> transactions) throws IOException
    {
        try (var printer = new CSVPrinter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8),
                        STRATEGY))
        {
            writeHeader(printer);

            Collections.sort(transactions, Transaction.BY_DATE);

            for (var transaction : transactions)
            {
                if (transaction instanceof AccountTransaction accountTransaction)
                {
                    writeAccountTransaction(printer, accountTransaction);

                }
                else if (transaction instanceof PortfolioTransaction portfolioTransaction)
                {
                    writePortfolioTransaction(printer, portfolioTransaction);
                }
            }
        }
    }

    private void writeAccountTransaction(CSVPrinter printer, AccountTransaction accountTransaction) throws IOException
    {
        printer.print(accountTransaction.getDateTime().toString());
        printer.print(accountTransaction.getType().toString());
        printer.print(Values.Amount.format(accountTransaction.getType().isDebit() ? -accountTransaction.getAmount()
                        : accountTransaction.getAmount()));
        printer.print(accountTransaction.getCurrencyCode());

        var transaction = accountTransaction.getCrossEntry() instanceof BuySellEntry entry
                        ? entry.getPortfolioTransaction()
                        : accountTransaction;

        writeTransaction(printer, transaction);

        printer.println();
    }

    private void writeTransaction(CSVPrinter printer, Transaction transaction) throws IOException
    {
        // gross amount
        Optional<Unit> grossAmount = transaction.getUnit(Unit.Type.GROSS_VALUE);
        if (grossAmount.isPresent())
        {
            var forex = grossAmount.get().getForex();
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
        printer.print(Values.Amount.formatNonZero(transaction.getUnitSum(Unit.Type.FEE).getAmount()));
        printer.print(Values.Amount.formatNonZero(transaction.getUnitSum(Unit.Type.TAX).getAmount()));
        printer.print(Values.Share.formatNonZero(transaction.getShares()));

        printSecurityInfo(printer, transaction.getSecurity());

        printer.print(escapeNull(transaction.getNote()));
    }

    private void writeHeader(CSVPrinter printer) throws IOException
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
    }

    public void exportAccountTransactions(File directory, List<Account> accounts) throws IOException
    {
        for (var account : accounts)
            exportTransactions(new File(directory, TextUtil.sanitizeFilename(account.getName() + ".csv")), //$NON-NLS-1$
                            account.getTransactions());
    }

    private void writePortfolioTransaction(CSVPrinter printer, PortfolioTransaction transaction) throws IOException
    {
        printer.print(transaction.getDateTime().toString());
        printer.print(transaction.getType().toString());
        printer.print(Values.Amount.format(
                        transaction.getType().isLiquidation() ? -transaction.getAmount() : transaction.getAmount()));
        printer.print(transaction.getCurrencyCode());

        writeTransaction(printer, transaction);

        printer.println();
    }

    private void printSecurityInfo(CSVPrinter printer, Security security) throws IOException
    {
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
        for (var portfolio : portfolios)
            exportTransactions(new File(directory, TextUtil.sanitizeFilename(portfolio.getName() + ".csv")), //$NON-NLS-1$
                            portfolio.getTransactions());
    }

    public void exportSecurityMasterData(File file, List<Security> securities) throws IOException
    {
        try (var printer = new CSVPrinter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8),
                        STRATEGY))
        {
            printer.printRecord(Messages.CSVColumn_ISIN, //
                            Messages.CSVColumn_WKN, //
                            Messages.CSVColumn_TickerSymbol, //
                            Messages.CSVColumn_SecurityName, //
                            Messages.CSVColumn_Currency, //
                            Messages.CSVColumn_Note);

            for (var security : securities)
            {
                printer.printRecord(escapeNull(security.getIsin()), //
                                escapeNull(security.getWkn()), //
                                escapeNull(security.getTickerSymbol()), //
                                escapeNull(security.getName()), //
                                escapeNull(security.getCurrencyCode()), //
                                escapeNull(security.getNote()));
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
        try (var printer = new CSVPrinter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8),
                        STRATEGY))
        {
            printer.printRecord(Messages.CSVColumn_Date, Messages.CSVColumn_Quote);

            for (var price : security.getPrices())
            {
                printer.print(price.getDate().toString());

                long value = price.getValue();
                if (converter.isPresent())
                    value = converter.get().convert(price.getDate(), Money.of(security.getCurrencyCode(), value))
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
        for (var security : securities)
            exportSecurityPrices(converter, new File(directory, security.getIsin() + ".csv"), security); //$NON-NLS-1$
    }

    public void exportMergedSecurityPrices(Optional<CurrencyConverter> converter, File file, List<Security> securities)
                    throws IOException
    {
        // prepare: (a) find earliest date
        var earliestDate = securities.stream() //
                        .filter(security -> !security.getPrices().isEmpty()) //
                        .map(security -> security.getPrices().get(0).getDate()) //
                        .min(LocalDate::compareTo) //
                        .orElse(null);
        // ... (b) ignore securities w/o quotes
        var securitiesToExport = securities.stream() //
                        .filter(security -> !security.getPrices().isEmpty()).toList();

        try (var printer = new CSVPrinter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8),
                        STRATEGY))
        {
            // write header
            printer.print(Messages.CSVColumn_Date);
            for (Security security : securitiesToExport)
                printer.print(security.getExternalIdentifier());
            printer.println();

            // stop if no securities exist
            if (earliestDate == null)
                return;

            // write quotes
            var dayToExport = earliestDate;
            var today = LocalDate.now();

            while (dayToExport.compareTo(today) <= 0)
            {
                int[] prices = getSecurityPricesForDay(securitiesToExport, dayToExport);

                boolean hasValues = IntStream.of(prices).anyMatch(p -> p >= 0);
                if (hasValues)
                {
                    writePricesForDay(printer, dayToExport, securitiesToExport, prices, converter);
                }

                dayToExport = dayToExport.plusDays(1);
            }

        }
    }

    private void writePricesForDay(CSVPrinter printer, LocalDate dayToExport, List<Security> securitiesToExport,
                    int[] prices, Optional<CurrencyConverter> converter) throws IOException
    {
        printer.print(dayToExport.toString());

        for (int ii = 0; ii < prices.length; ii++)
        {
            if (prices[ii] < 0)
            {
                printer.print(""); //$NON-NLS-1$
            }
            else
            {
                var security = securitiesToExport.get(ii);
                long value = security.getPrices().get(prices[ii]).getValue();

                if (converter.isPresent())
                    value = converter.get().convert(dayToExport, Money.of(security.getCurrencyCode(), value))
                                    .getAmount();

                printer.print(Values.Quote.format(value));
            }
        }

        printer.println();
    }

    private int[] getSecurityPricesForDay(List<Security> securitiesToExport, LocalDate dayToExport)
    {
        int[] prices = new int[securitiesToExport.size()];

        for (int ii = 0; ii < securitiesToExport.size(); ii++)
        {
            var security = securitiesToExport.get(ii);
            prices[ii] = Collections.binarySearch(security.getPrices(), new SecurityPrice(dayToExport, 0));
        }

        return prices;
    }

    /* package */static String escapeNull(String value)
    {
        return value != null ? value : ""; //$NON-NLS-1$
    }
}
