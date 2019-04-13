package name.abuchen.portfolio.snapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.math.IRR;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConsumerPriceIndex;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Interval;

public class ClientIRRYield
{
    public static ClientIRRYield create(Client client, ClientSnapshot snapshotStart, ClientSnapshot snapshotEnd)
    {
        Interval interval = Interval.of(snapshotStart.getTime(), snapshotEnd.getTime());

        List<Transaction> transactions = new ArrayList<>();
        collectAccountTransactions(client, interval, transactions);
        collectPortfolioTransactions(client, interval, transactions);
        Collections.sort(transactions, new Transaction.ByDate());

        ArrayList<LocalDate> dates = new ArrayList<>();
        ArrayList<Double> values = new ArrayList<>();
        collectDatesAndValues(interval, snapshotStart, snapshotEnd, transactions, dates, values);

        double irr = IRR.calculate(dates, values);
        
        // collect consumer prices indices in order to calculate the inflation adjusted time series
        List<ConsumerPriceIndex> cpi_data = client.getConsumerPriceIndices();   
        
        List<Integer> indexes = new ArrayList<>();
        for (LocalDate date : dates) 
        {
            int year = date.getYear();
            int month = date.getMonthValue();
            ConsumerPriceIndex cpi = null;
            
            // search for last provided consumer price index
            while (cpi == null && !cpi_data.isEmpty() && year >= cpi_data.get(0).getYear()) 
            {
                cpi = ConsumerPriceIndex.findByDate(cpi_data, year, month);
                
                month--;
                if (month < 1) 
                {
                    year--;
                    month = 12;
                }
            }
            
            if (cpi != null) 
            {
                indexes.add(cpi.getIndex());
            }
            else 
            {
                indexes.add(0); // no previous consumer price index found -> zero produce an inflationAdjustedIrr of NaN
            }
        }
        
        // scale values according to its spending power
        for (int i = 0; i < values.size(); i++) 
        {
            values.set(i, values.get(i) / (double)indexes.get(i));
        }
        
        double inflationAdjustedIrr = IRR.calculate(dates, values);

        return new ClientIRRYield(irr, inflationAdjustedIrr);
    }

    private double irr;
    private double inflationAdjustedIrr;

    private ClientIRRYield(double irr, double inflationAdjustedIrr)
    {
        this.irr = irr;
        this.inflationAdjustedIrr = inflationAdjustedIrr;
    }

    public double getIrr()
    {
        return irr;
    }
    
    public double getInflationAdjustedIrr() 
    {
        return inflationAdjustedIrr;
    }

    private static void collectPortfolioTransactions(Client client, Interval interval, List<Transaction> transactions)
    {
        for (Portfolio portfolio : client.getPortfolios())
        {
            portfolio.getTransactions().stream() //
                            .filter(t -> interval.contains(t.getDateTime())) //
                            .forEach(t -> {
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
                            });
        }
    }

    private static void collectAccountTransactions(Client client, Interval interval, List<Transaction> transactions)
    {
        for (Account account : client.getAccounts())
        {
            account.getTransactions().stream() //
                            .filter(t -> interval.contains(t.getDateTime())) //
                            .forEach(t -> {
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
                                    case FEES_REFUND:
                                    case TAXES:
                                    case DIVIDENDS:
                                    case INTEREST:
                                    case INTEREST_CHARGE:
                                    case TAX_REFUND:
                                        break;
                                    default:
                                        throw new UnsupportedOperationException();
                                }
                            });
        }
    }

    private static void collectDatesAndValues(Interval interval, ClientSnapshot snapshotStart,
                    ClientSnapshot snapshotEnd, List<Transaction> transactions, List<LocalDate> dates,
                    List<Double> values)
    {
        CurrencyConverter converter = snapshotStart.getCurrencyConverter();

        dates.add(interval.getStart());
        // snapshots are always in target currency, no conversion needed
        values.add(-snapshotStart.getMonetaryAssets().getAmount() / Values.Amount.divider());

        for (Transaction t : transactions)
        {
            dates.add(t.getDateTime().toLocalDate());

            if (t instanceof AccountTransaction)
            {
                AccountTransaction at = (AccountTransaction) t;
                long amount = converter.convert(t.getDateTime(), t.getMonetaryAmount()).getAmount();
                if (at.getType() == Type.DEPOSIT || at.getType() == Type.TRANSFER_IN)
                    amount = -amount;
                values.add(amount / Values.Amount.divider());
            }
            else if (t instanceof PortfolioTransaction)
            {
                PortfolioTransaction pt = (PortfolioTransaction) t;
                long amount = converter.convert(t.getDateTime(), t.getMonetaryAmount()).getAmount();
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

        dates.add(interval.getEnd());
        values.add(snapshotEnd.getMonetaryAssets().getAmount() / Values.Amount.divider());
    }
}
