package name.abuchen.portfolio.datatransfer.csv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVPrinter;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Values;

/**
 * Special CSV exporter for rentablo.de
 * (formerly known as aktienfreunde.net)
 */
public class RentabloNetExporter
{
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy"); //$NON-NLS-1$

    /**
     * Export all transactions in 'aktienfreunde.net' Format
     */
    public void exportAllTransactions(File file, Client client) throws IOException
    {
        // collect transactions
        List<? extends Transaction> transactions = Stream
                        .concat(client.getAccounts().stream(), client.getPortfolios().stream())
                        .flatMap(l -> l.getTransactions().stream()) //
                        .sorted(Transaction.BY_DATE) //
                        .collect(Collectors.toList());

        // write to file
        try (CSVPrinter printer = new CSVPrinter(
                        new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8),
                        CSVExporter.STRATEGY))
        {
            writeHeader(printer);

            // only buy/sell/dividend transactions
            for (Transaction t : transactions)
            {
                if (t instanceof AccountTransaction)
                    writeDividend(printer, (AccountTransaction) t);
                else if (t instanceof PortfolioTransaction)
                    writeBuySell(printer, (PortfolioTransaction) t);
            }
        }
    }

    @SuppressWarnings("nls")
    private void writeDividend(CSVPrinter printer, AccountTransaction transaction) throws IOException
    {
        if (transaction.getSecurity() == null)
            return;
        if (transaction.getType() != AccountTransaction.Type.INTEREST
                        && transaction.getType() != AccountTransaction.Type.INTEREST_CHARGE
                        && transaction.getType() != AccountTransaction.Type.DIVIDENDS)
            return;

        printer.print(DATE_FORMAT.format(transaction.getDateTime().toLocalDate()));
        printer.print(CSVExporter.escapeNull(transaction.getSecurity().getIsin()));
        printer.print(CSVExporter.escapeNull(transaction.getSecurity().getName()));
        printer.print(Messages.CSVColumn_Security);
        printer.print(AccountTransaction.Type.DIVIDENDS.toString());
        printer.print(Values.Amount.format(transaction.getAmount()));
        printer.print(transaction.getShares() != 0 ? Values.Share.format(transaction.getShares()) : "1");
        printer.print("0");
        printer.print(transaction.getType() == AccountTransaction.Type.DIVIDENDS
                        ? Values.Amount.format(transaction.getUnitSum(Unit.Type.TAX).getAmount())
                        : "0"); //$NON-NLS-1$
        printer.println();
    }

    @SuppressWarnings("nls")
    private void writeBuySell(CSVPrinter printer, PortfolioTransaction transaction) throws IOException
    {
        String type;

        switch (transaction.getType())
        {
            case BUY:
            case DELIVERY_INBOUND:
                type = PortfolioTransaction.Type.BUY.toString(); //$NON-NLS-1$
                break;
            case SELL:
            case DELIVERY_OUTBOUND:
                type = PortfolioTransaction.Type.SELL.toString(); //$NON-NLS-1$
                break;
            default:
                // ignore all other transactions
                return;
        }

        printer.print(DATE_FORMAT.format(transaction.getDateTime().toLocalDate()));
        printer.print(CSVExporter.escapeNull(transaction.getSecurity().getIsin()));
        printer.print(CSVExporter.escapeNull(transaction.getSecurity().getName()));
        printer.print(Messages.CSVColumn_Security);
        printer.print(type);
        printer.print(Values.Quote.format(transaction.getGrossPricePerShare().getAmount()));
        printer.print(Values.Share.format(transaction.getShares()));
        printer.print(Values.Amount.format(transaction.getUnitSum(Unit.Type.FEE).getAmount()));
        printer.print(Values.Amount.format(transaction.getUnitSum(Unit.Type.TAX).getAmount()));
        printer.println();
    }

    @SuppressWarnings("nls")
    private void writeHeader(CSVPrinter printer) throws IOException
    {
        printer.print(Messages.CSVColumn_Date);
        printer.print(Messages.CSVColumn_ISIN);
        printer.print(Messages.CSVColumn_Name);
        printer.print(Messages.CSVColumn_Security);
        printer.print(Messages.CSVColumn_Type);
        printer.print(Messages.CSVColumn_GrossAmount);
        printer.print(Messages.CSVColumn_Shares);
        printer.print(Messages.CSVColumn_Fees);
        printer.print(Messages.CSVColumn_Taxes);
        printer.println();
    }
}
