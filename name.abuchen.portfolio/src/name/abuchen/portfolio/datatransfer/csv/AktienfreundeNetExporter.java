package name.abuchen.portfolio.datatransfer.csv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
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
                        .sorted(new Transaction.ByDate()) //
                        .collect(Collectors.toList());

        // write to file
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
        {
            CSVPrinter printer = new CSVPrinter(writer);
            printer.setStrategy(CSVExporter.STRATEGY);

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
    private void writeDividend(CSVPrinter printer, AccountTransaction transaction)
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
    private void writeBuySell(CSVPrinter printer, PortfolioTransaction transaction)
    {
        String type;

        switch (transaction.getType())
        {
            case BUY:
            case DELIVERY_INBOUND:
                type = "Kauf"; //$NON-NLS-1$
                break;
            case SELL:
            case DELIVERY_OUTBOUND:
                type = "Verkauf"; //$NON-NLS-1$
                break;
            default:
                // ignore all other transactions
                return;
        }

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
    private void writeHeader(CSVPrinter printer)
    {
        printer.print("Datum");
        printer.print("ISIN");
        printer.print("Name");
        printer.print("Typ");
        printer.print("Transaktion");
        printer.print("Preis");
        printer.print("Anzahl");
        printer.print("Kommission");
        printer.print("Steuern");
        printer.println();
    }
}
