package name.abuchen.portfolio.datatransfer.csv.exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVPrinter;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Values;

/**
 * Special exporter for Axtienfreunde.net
 */
public class AktienfreundeNetExporter
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
                        .toList();

        // write to file
        try (var printer = new CSVPrinter(
                        new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8),
                        CSVExporter.STRATEGY))
        {
            writeHeader(printer);

            // only buy/sell/dividend transactions
            for (var transaction : transactions)
            {
                if (transaction instanceof AccountTransaction at)
                    writeDividend(printer, at);
                else if (transaction instanceof PortfolioTransaction pt)
                    writeBuySell(printer, pt);
            }
        }
    }

    @SuppressWarnings("nls")
    private void writeDividend(CSVPrinter printer, AccountTransaction transaction) throws IOException
    {
        if (transaction.getSecurity() == null)
            return;
        if (transaction.getType() != AccountTransaction.Type.INTEREST
                        && transaction.getType() != AccountTransaction.Type.DIVIDENDS)
            return;

        printer.print(DATE_FORMAT.format(transaction.getDateTime().toLocalDate()));
        printer.print(CSVExporter.escapeNull(transaction.getSecurity().getIsin()));
        printer.print(CSVExporter.escapeNull(transaction.getSecurity().getName()));
        printer.print("Aktie");
        printer.print("Dividende");
        printer.print(Values.Amount.format(transaction.getAmount()));
        printer.print("1");
        printer.print("");
        printer.print("");
        printer.println();
    }

    @SuppressWarnings("nls")
    private void writeBuySell(CSVPrinter printer, PortfolioTransaction transaction) throws IOException
    {
        var type = switch (transaction.getType())
        {
            case BUY, DELIVERY_INBOUND -> "Kauf"; //$NON-NLS-1$
            case SELL, DELIVERY_OUTBOUND -> "Verkauf"; //$NON-NLS-1$
            default -> null;
        };

        // skip writing other transaction types
        if (type == null)
            return;

        printer.print(DATE_FORMAT.format(transaction.getDateTime().toLocalDate()));
        printer.print(CSVExporter.escapeNull(transaction.getSecurity().getIsin()));
        printer.print(CSVExporter.escapeNull(transaction.getSecurity().getName()));
        printer.print("Aktie");
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
        printer.printRecord("Datum", //
                        "ISIN", //
                        "Name", //
                        "Typ", //
                        "Transaktion", //
                        "Preis", //
                        "Anzahl", //
                        "Kommission", //
                        "Steuern");
    }
}
