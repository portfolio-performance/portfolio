package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.math.IRR;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Values;

public class ClientIRRYield
{

    public static ClientIRRYield create(Client client, CurrencyConverter converter, Date start, Date end)
    {
        ClientSnapshot snapshotStart = ClientSnapshot.create(client, converter, start);
        ClientSnapshot snapshotEnd = ClientSnapshot.create(client, converter, end);

        return create(client, snapshotStart, snapshotEnd);
    }

    public static ClientIRRYield create(Client client, ClientSnapshot snapshotStart, ClientSnapshot snapshotEnd)
    {
        Date start = snapshotStart.getTime();
        Date end = snapshotEnd.getTime();

        List<Transaction> transactions = new ArrayList<Transaction>();
        collectAccountTransactions(client, start, end, transactions);
        collectPortfolioTransactions(client, start, end, transactions);
        Collections.sort(transactions, new Transaction.ByDate());

        List<Date> dates = new ArrayList<Date>();
        List<Double> values = new ArrayList<Double>();

        collectDatesAndValues(start, end, snapshotStart, snapshotEnd, transactions, dates, values);

        double irr = IRR.calculate(dates, values);

        return new ClientIRRYield(snapshotStart, snapshotEnd, transactions, irr * 100);
    }

    private ClientSnapshot snapshotStart;
    private ClientSnapshot snapshotEnd;
    private List<Transaction> transactions;
    private double irr;

    private ClientIRRYield(ClientSnapshot snapshotStart, ClientSnapshot snapshotEnd, List<Transaction> transactions,
                    double irr)
    {
        this.snapshotStart = snapshotStart;
        this.snapshotEnd = snapshotEnd;
        this.transactions = transactions;
        this.irr = irr;
    }

    public ClientSnapshot getSnapshotStart()
    {
        return snapshotStart;
    }

    public ClientSnapshot getSnapshotEnd()
    {
        return snapshotEnd;
    }

    public List<Transaction> getTransactions()
    {
        return transactions;
    }

    public double getIrr()
    {
        return irr;
    }

    private static void collectPortfolioTransactions(Client client, Date start, Date end, List<Transaction> transactions)
    {
        for (Portfolio portfolio : client.getPortfolios())
        {
            for (PortfolioTransaction t : portfolio.getTransactions())
            {
                if (t.getDate().getTime() > start.getTime() && t.getDate().getTime() <= end.getTime())
                {
                    switch (t.getType())
                    {
                        case TRANSFER_IN:
                        case TRANSFER_OUT:
                        case DELIVERY_INBOUND:
                        case DELIVERY_OUTBOUND:
                            transactions.add(t);
                            break;
                        case BUY:
                        case SELL:
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }

            }
        }
    }

    private static void collectAccountTransactions(Client client, Date start, Date end, List<Transaction> transactions)
    {
        for (Account account : client.getAccounts())
        {
            for (AccountTransaction t : account.getTransactions())
            {
                if (t.getDate().getTime() > start.getTime() && t.getDate().getTime() <= end.getTime())
                {
                    switch (t.getType())
                    {
                        case DEPOSIT:
                        case REMOVAL:
                        case TRANSFER_IN:
                        case TRANSFER_OUT:
                            transactions.add(t);
                            break;
                        case BUY:
                        case SELL:
                        case FEES:
                        case TAXES:
                        case DIVIDENDS:
                        case INTEREST:
                        case TAX_REFUND:
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }

            }
        }
    }

    private static void collectDatesAndValues(Date start, Date end, ClientSnapshot snapshotStart,
                    ClientSnapshot snapshotEnd, List<Transaction> transactions, List<Date> dates, List<Double> values)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(start);
        dates.add(cal.getTime());
        values.add(-(snapshotStart.getAssets()) / Values.Amount.divider());

        for (Transaction t : transactions)
        {
            cal.setTime(t.getDate());
            dates.add(cal.getTime());

            if (t instanceof AccountTransaction)
            {
                AccountTransaction at = (AccountTransaction) t;
                long amount = at.getAmount();
                if (at.getType() == Type.DEPOSIT || at.getType() == Type.TRANSFER_IN)
                    amount = -amount;
                values.add(amount / Values.Amount.divider());
            }
            else if (t instanceof PortfolioTransaction)
            {
                PortfolioTransaction pt = (PortfolioTransaction) t;

                long amount = pt.getAmount();
                if (pt.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND
                                || pt.getType() == PortfolioTransaction.Type.TRANSFER_IN)
                    amount = -amount;
                values.add(amount / Values.Amount.divider());
            }
            else
            {
                throw new UnsupportedOperationException();
            }
        }

        cal.setTime(end);
        dates.add(cal.getTime());
        values.add(snapshotEnd.getAssets() / Values.Amount.divider());
    }
}
