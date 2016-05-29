package name.abuchen.portfolio.datatransfer.csv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVPrinter;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Values;

public class AktienfreundeNetExporter
{

    /**
     * Export all transactions in 'aktienfreunde.net' Format
     */
    public void exportAllTransactions(File file, Client client) throws IOException
    {
        // collect transactions
        List<Transaction> transactions = new ArrayList<Transaction>();

        for (Account a : client.getAccounts())
            transactions.addAll(a.getTransactions());
        for (Portfolio p : client.getPortfolios())
            transactions.addAll(p.getTransactions());

        Transaction.sortByDate(transactions);

        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy"); //$NON-NLS-1$

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
                {
                    AccountTransaction ta = (AccountTransaction) t;

                    if (ta.getSecurity() != null
                                    && (ta.getType() == AccountTransaction.Type.INTEREST || ta.getType() == AccountTransaction.Type.DIVIDENDS))
                        writeDividend(printer, ta, dateFormat);

                }
                else if (t instanceof PortfolioTransaction)
                {
                    PortfolioTransaction tp = (PortfolioTransaction) t;

                    switch (tp.getType())
                    {
                        case BUY:
                        case DELIVERY_INBOUND:
                            writeBuySell(printer, tp, "Kauf", dateFormat); //$NON-NLS-1$
                            break;
                        case SELL:
                        case DELIVERY_OUTBOUND:
                            writeBuySell(printer, tp, "Verkauf", dateFormat); //$NON-NLS-1$
                            break;
                        default:
                            // ignore all other transactions
                    }

                }
            }
        }
    }

    @SuppressWarnings("nls")
    private void writeDividend(CSVPrinter printer, AccountTransaction transaction, DateFormat dateFormat)
    {
        printer.print(dateFormat.format(transaction.getDate()));
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
    private void writeBuySell(CSVPrinter printer, PortfolioTransaction transaction, String type, DateFormat dateFormat)
    {
        printer.print(dateFormat.format(transaction.getDate()));
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
