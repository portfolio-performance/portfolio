package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import name.abuchen.portfolio.math.IRR;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.util.Helper;

public class DividendPerformanceSnapshot
{
    public static DividendPerformanceSnapshot create(Client client, ReportingPeriod period)
    {
        Map<Security, DivRecord> transactions = initRecords(client);

        Date startDate = period.getStartDate();
        Date endDate = period.getEndDate();

        for (Account account : client.getAccounts())
            extractSecurityRelatedAccountTransactions(account, startDate, endDate, transactions);
        for (Portfolio portfolio : client.getPortfolios())
        {
            extractSecurityRelatedPortfolioTransactions(portfolio, startDate, endDate, transactions);
            addPseudoValuationTansactions(portfolio, startDate, endDate, transactions);
        }

        return doCreateSnapshot(transactions, endDate);
    }

    public static DividendPerformanceSnapshot create(Client client, Portfolio portfolio, Date startDate, Date endDate)
    {
        Map<Security, DivRecord> transactions = initRecords(client);

        if (portfolio.getReferenceAccount() != null)
            extractSecurityRelatedAccountTransactions(portfolio.getReferenceAccount(), startDate, endDate, transactions);
        extractSecurityRelatedPortfolioTransactions(portfolio, startDate, endDate, transactions);
        addPseudoValuationTansactions(portfolio, startDate, endDate, transactions);

        return doCreateSnapshot(transactions, endDate);
    }

    private static Map<Security, DivRecord> initRecords(Client client)
    {
        Map<Security, DivRecord> transactions = new HashMap<Security, DivRecord>();

        for (Security s : client.getSecurities())
            transactions.put(s, new DivRecord(s));
        return transactions;
    }

    private static DividendPerformanceSnapshot doCreateSnapshot(Map<Security, DivRecord> transactions, Date endDate)
    {
        for (DivRecord c : transactions.values())
            c.prepare(endDate);

        for (Iterator<Map.Entry<Security, DivRecord>> iter = transactions.entrySet().iterator(); iter.hasNext();)
        {
            Map.Entry<Security, DivRecord> entry = iter.next();
            DivRecord d = entry.getValue();
            if (d.transactions.isEmpty())
                iter.remove();
            else if (d.getStockShares() == 0)
                iter.remove();
        }

        // prepare pseudo summarize

        DivRecord sum1 = null;

        for (DivRecord c : transactions.values())
        {
            if (c.security.getName().equalsIgnoreCase("_summe_"))
            {
                sum1 = c;
                break;
            }
        }

        if (sum1 != null)
        {

            DivRecord sum = sum1;
            // DivRecord sum = new DivRecord(sum1.getSecurity());
            // transactions.values().add(sum); // crasht mit new
            // DivRecord(sum1.getSecurity());

            for (DivRecord c : transactions.values())
            {
                if (c != sum)
                    sum.summarize(c);
            }

        }

        return new DividendPerformanceSnapshot(transactions.values());
    }

    private static void extractSecurityRelatedAccountTransactions(Account account, Date startDate, Date endDate,
                    Map<Security, DivRecord> transactions)
    {
        for (AccountTransaction t : account.getTransactions())
        {
            if (t.getDate().getTime() > startDate.getTime() && t.getDate().getTime() <= endDate.getTime())
            {
                switch (t.getType())
                {
                    case INTEREST:
                    case DIVIDENDS:
                        if (t.getSecurity() != null)
                        {
                            DividendTransaction dt = new DividendTransaction();
                            dt.setDate(t.getDate());
                            dt.setSecurity(t.getSecurity());
                            ;
                            dt.setAccount(account);
                            dt.setAmount(t.getAmount());
                            transactions.get(t.getSecurity()).add(dt);
                        }
                        break;
                    case FEES:
                    case TAXES:
                    case DEPOSIT:
                    case REMOVAL:
                    case BUY:
                    case SELL:
                    case TRANSFER_IN:
                    case TRANSFER_OUT:
                        // transactions.get(null).add(t);
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }
        }
    }

    private static void extractSecurityRelatedPortfolioTransactions(Portfolio portfolio, Date startDate, Date endDate,
                    Map<Security, DivRecord> transactions)
    {
        for (PortfolioTransaction t : portfolio.getTransactions())
        {
            if (t.getDate().getTime() > startDate.getTime() && t.getDate().getTime() <= endDate.getTime())
            {
                switch (t.getType())
                {
                    case TRANSFER_IN:
                    case TRANSFER_OUT:
                    case BUY:
                    case SELL:
                    case DELIVERY_INBOUND:
                    case DELIVERY_OUTBOUND:
                        transactions.get(t.getSecurity()).add(t);
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }

        }
    }

    private static void addPseudoValuationTansactions(Portfolio portfolio, Date startDate, Date endDate,
                    Map<Security, DivRecord> transactions)
    {
        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, startDate);
        for (SecurityPosition position : snapshot.getPositions())
        {
            transactions.get(position.getSecurity()).add(new DividendInitialTransaction(position, startDate));
        }

        snapshot = PortfolioSnapshot.create(portfolio, endDate);
        for (SecurityPosition position : snapshot.getPositions())
        {
            transactions.get(position.getSecurity()).add(new DividendFinalTransaction(position, endDate));
        }
    }

    private Collection<DivRecord> calculations;

    private DividendPerformanceSnapshot(Collection<DivRecord> calculations)
    {
        this.calculations = calculations;
    }

    public List<DivRecord> getRecords()
    {
        return new ArrayList<DivRecord>(calculations);
    }

    public static class DividendTransaction extends Transaction
    {
//        public enum Type
//        {
//            DEPOSIT, REMOVAL, INTEREST, DIVIDENDS, FEES, TAXES, BUY, SELL, TRANSFER_IN, TRANSFER_OUT;
    //
//            private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("name.abuchen.portfolio.model.labels"); //$NON-NLS-1$
    //
//            public String toString()
//            {
//                return RESOURCES.getString("account." + name()); //$NON-NLS-1$
//            }
//        }

//        private Type type;

        private long amount;
        private Account account;
        private long shares;
        private long dividendPerShare;
        private boolean isDiv12;
        private int divEventId;

        public DividendTransaction()
        {}

//        public DividendTransaction(Date date, Security security, Type type, long amount)
//        {
//            super(date, security);
//            this.type = type;
//            this.amount = amount;
//        }

//        public Type getType()
//        {
//            return type;
//        }
    //
//        public void setType(Type type)
//        {
//            this.type = type;
//        }

        
        public Account getAccount()
        {
            return account;
        }

        public void setAccount(Account account)
        {
            this.account = account;
        }
        
        @Override
        public long getAmount()
        {
            return amount;
        }

        public void setAmount(long amount)
        {
            this.amount = amount;
            updateDividendPerShare();
        }

        public long getShares()
        {
            return shares;
        }

        public void setShares(long shares)
        {
            this.shares = shares;
            updateDividendPerShare();
        }
        
        public long getDividendPerShare()
        {
            return dividendPerShare;
        }
        
        public boolean getIsDiv12 ()
        {
            return isDiv12;
        }

        public void setIsDiv12 (boolean isDiv12)
        {
            this.isDiv12 = isDiv12; 
        }
        
        public int getDivEventId ()
        {
            return divEventId;
        }

        public void setDivEventId (int divEventId)
        {
            this.divEventId = divEventId; 
        }
        
        private void updateDividendPerShare()
        {
            this.dividendPerShare = amountPerShare(amount, shares);
                
        }
        
        static public long amountPerShare (long amount, long shares)
        {
            if (shares != 0)
            {
                return Math.round ((double) amount / (double) shares * Values.Share.divider());          
            }
            else
            {
                return 0;            
            }
        }

        static public long amountTimesShares (long price, long shares)
        {
            if (shares != 0)
            {
                return Math.round ((double) price * (double) shares / Values.Share.divider());          
            }
            else
            {
                return 0;            
            }
        }

    }

    public static class DividendInitialTransaction extends Transaction
    {
        private SecurityPosition position;

        public DividendInitialTransaction(SecurityPosition position, Date time)
        {
            this.position = position;
            this.setSecurity(position.getSecurity());
            this.setDate(time);
        }

        @Override
        public long getAmount()
        {
            return position.calculateValue();
        }

        public SecurityPosition getPosition()
        {
            return position;
        }
    }

    public static class DividendFinalTransaction extends Transaction
    {
        private SecurityPosition position;

        public DividendFinalTransaction(SecurityPosition position, Date time)
        {
            this.position = position;
            this.setSecurity(position.getSecurity());
            this.setDate(time);
        }

        @Override
        public long getAmount()
        {
            return position.calculateValue();
        }

        public SecurityPosition getPosition()
        {
            return position;
        }
    }

    public static class DivRecord implements Adaptable
    {
        private final Security security;
        private List<Transaction> transactions = new ArrayList<Transaction>();

        private double irr;
        private double irrdiv;
        private long divAmount;
        private long div12Shares;
        private long div12Cost;
        private long div12Amount;
        private long div24Amount;
        private long div36Amount;
        private long stockAmount; // Wert im Bestand
        private long stockShares; // Stücke im Bestand
        private long poolAmount; // dto im Verrechnungpool
        private long poolShares; // dto im Verrechnungpool
        private Date dateFrom;
        private Date dateTo;
        private int divEventCount;
        private double divIncreasingRate;
        private int divIncreasingYears;

        public enum Periodicity
        {
            UNKNOWN, NONE, INDEFINITE, ANNUAL, SEMIANNUAL, QUARTERLY, IRREGULAR;

            private static final ResourceBundle RESOURCES = ResourceBundle
                            .getBundle("name.abuchen.portfolio.snapshot.labels"); //$NON-NLS-1$

            public String toString()
            {
                return RESOURCES.getString("dividends." + name()); //$NON-NLS-1$
            }
        }

        private Periodicity periodicity = Periodicity.UNKNOWN;

        /* package */DivRecord(Security security)
        {
            this.security = security;
        }

        public Security getSecurity()
        {
            return security;
        }

        public String getSecurityName()
        {
            if (getSecurity() != null)
                return getSecurity().getName();
            else
                return null;
        }

        public double getIrr()
        {
            return irr;
        }

        public double getIrrDiv()
        {
            return irrdiv;
        }

        public double getTrrDiv()
        {
            if (stockAmount > 0)
            {
                return (double) divAmount / (double) stockAmount;
            }
            else
            {
                return 0;
            }
        }

        public long getDivAmount()
        {
            return divAmount;
        }

        public long getDiv12Amount()
        {
            return div12Amount;
        }

        public long getDiv24Amount()
        {
            return div24Amount;
        }

        public long getDiv36Amount()
        {
            return div36Amount;
        }

        public long getDiv12PerShare()
        {
            return DividendTransaction.amountPerShare(div12Amount, div12Shares);
        }

        public long getCost12Amount()
        {
            return DividendTransaction.amountTimesShares(getStockPrice(), div12Shares);
        }

        public long getExpectedDiv12Amount()
        {
            return div24Amount;
        }

        public double getDivIncreasingRate()
        {
            return divIncreasingRate;
        }

        public long getLongTermDiv12Amount()
        {
            return div36Amount;
        }

        public long getDiv12MeanShares()
        {
            return div12Shares;
        }

        public long getStockAmount()
        {
            return stockAmount;
        }

        public long getStockShares()
        {
            return stockShares;
        }

        public long getStockPrice()
        {
            return DividendTransaction.amountPerShare(stockAmount, stockShares);
        }

        public long getPoolAmount()
        {
            return poolAmount;
        }

        public long getPoolShares()
        {
            return poolShares;
        }

        public long getPoolPrice()
        {
            return DividendTransaction.amountPerShare(poolAmount, poolShares);
        }

        public Date getDateFrom()
        {
            return dateFrom;
        }

        public Date getDateTo()
        {
            return dateTo;
        }

        public int getDivEventCount()
        {
            return divEventCount;
        }

        public double getPersonalDiv()
        {
            long amountPerShare = getStockPrice();
            if (amountPerShare > 0)
            {
                return (double) div12Amount / (double) div12Cost;
            }
            else
            {
                return 0;
            }

        }

        public Periodicity getPeriodicity()
        {
            return periodicity;
        }

        public int getPeriodicitySort()
        {
            return periodicity.ordinal();
        }

        public Boolean getHasDiv12()
        {
            switch (periodicity)
            {
                case UNKNOWN:
                case NONE:
                    return false;
                default:
                    return true;
            }
        }

        public List<Transaction> getTransactions()
        {
            return transactions;
        }

        @Override
        public <T> T adapt(Class<T> type)
        {
            return type == Security.class ? type.cast(security) : null;
        }

        public void summarize(DivRecord d)
        {
            this.periodicity = Periodicity.INDEFINITE;
            this.divEventCount += 1; // d.divEventCount;
            this.divAmount += d.divAmount;
            this.stockAmount += d.stockAmount;
            this.div12Cost += d.div12Cost;
            this.div12Amount += d.div12Amount;
            this.div24Amount += d.div24Amount;
            this.div36Amount += d.div36Amount;
            // private double irr;
            // private double irrdiv;
            // private long div12PerShare;
            // private long div12MeanShares;
            // private long stockShares; // Stücke im Bestand
            // private long poolAmount; // dto im Verrechnungpool
            // private long poolShares; // dto im Verrechnungpool
            // private Date dateFrom;
            // private Date dateTo;

        }

        void add(Transaction t)
        {
            transactions.add(t);
        }

        void prepare(Date endDate)
        {
            Collections.sort(transactions);

            if (!transactions.isEmpty())
            {
                calculateIRR();
                calculateIRRDiv();
                calculateDiv12(endDate);
            }
        }

        private void calculateIRR()
        {
            List<Date> dates = new ArrayList<Date>();
            List<Double> values = new ArrayList<Double>();

            for (Transaction t : transactions)
            {
                Calendar cal = Calendar.getInstance();
                cal.setTime(t.getDate());
                dates.add(t.getDate());

                if (t instanceof DividendInitialTransaction)
                {
                    values.add(-((DividendInitialTransaction) t).getAmount() / Values.Amount.divider());
                }
                else if (t instanceof DividendFinalTransaction)
                {
                    values.add(((DividendFinalTransaction) t).getAmount() / Values.Amount.divider());
                }
                else if (t instanceof DividendTransaction)
                {
                    values.add(((DividendTransaction) t).getAmount() / Values.Amount.divider());
                }
                else if (t instanceof AccountTransaction)
                {
                    values.add(((AccountTransaction) t).getAmount() / Values.Amount.divider());
                }
                else if (t instanceof PortfolioTransaction)
                {
                    PortfolioTransaction pt = (PortfolioTransaction) t;
                    switch (pt.getType())
                    {
                        case BUY:
                        case DELIVERY_INBOUND:
                        case TRANSFER_IN:
                            values.add(-pt.getAmount() / Values.Amount.divider());
                            break;
                        case SELL:
                        case DELIVERY_OUTBOUND:
                        case TRANSFER_OUT:
                            values.add(pt.getAmount() / Values.Amount.divider());
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }
                else
                {
                    throw new UnsupportedOperationException();
                }
            }

            this.irr = IRR.calculate(dates, values);
        }

        private void debugstop()
        {

        }

        private void calculateIRRDiv()
        {
            List<Date> dates = new ArrayList<Date>();
            List<Double> values = new ArrayList<Double>();

            Calendar cal = Calendar.getInstance();
            Date tDateFinal = cal.getTime();
            stockAmount = 0;
            stockShares = 0;
            poolAmount = 0;
            poolShares = 0;
            divEventCount = 0;

            Date dCurr, dLast = null;
            for (Transaction t : transactions)
            {

                cal.setTime(t.getDate());
                long tAmount = 0;
                Date tDate = t.getDate();

                if (t instanceof DividendInitialTransaction)
                {
                    DividendInitialTransaction dit = (DividendInitialTransaction) t;
                    // Eingangsbuchung wie buy.
                    // Kaufpreise und -mengen kumulieren, neuer
                    // durchschnittlicher Einstandspreis

                    stockAmount += dit.getAmount();
                    stockShares += dit.getPosition().getShares();
                    if (stockShares <= 0)
                    {
                        // wenn pt.getAmount immer > 0 ist, kann das nicht
                        // passieren
                        debugstop();
                        throw new UnsupportedOperationException();
                    }

                    tAmount = dit.getAmount(); // Achtung: dt.getAmount ist
                                               // schon negativ
                }
                else if (t instanceof DividendFinalTransaction)
                {
                    // Abschlussbuchungen ignorieren, statt dessen ganz am Ende
                    // stockamount buchen
                    // aber Datum für Abschlussbuchung merken
                    tDateFinal = tDate;
                }
                else if (t instanceof DividendTransaction)
                {
                    // "normale" Dividenden
                    DividendTransaction dt = (DividendTransaction) t;
                    tAmount = dt.getAmount();

                    // prüfen, ob die Dividende zum gleichen Zahlungstag gehört
                    dCurr = dt.getDate();
                    if ((dLast == null) || (Helper.daysBetween(dLast, dCurr) > 30))
                    {
                        divEventCount++;
                    }
                    dLast = dCurr;

                    dt.setShares(stockShares);
                    dt.setDivEventId(divEventCount);

                }
                else if (t instanceof AccountTransaction)
                {
                    // Zinsen ? ignorieren
                }
                else if (t instanceof PortfolioTransaction)
                {
                    // Bestandsveränderungen
                    PortfolioTransaction pt = (PortfolioTransaction) t;
                    long shares = pt.getShares();
                    long amount = pt.getAmount();
                    switch (pt.getType())
                    {
                        case BUY:
                            // Kaufpreise und -mengen kumulieren
                            if (poolShares > 0)
                            {
                                // der Kaufpreis wird anteilig um den im Pool
                                // gespeicherten Gewinn/Verlust reduziert
                                long ps = Math.min(shares, poolShares);
                                long pa = DividendTransaction.amountTimesShares(getPoolPrice(), ps);

                                poolShares -= ps;
                                poolAmount -= pa;
                                if (poolShares == 0)
                                {
                                    // Restwert komplett ausbuchen, sonst
                                    // bleiben die Rundungsdifferenzen auf
                                    // poolAmount stehen
                                    pa += poolAmount;
                                    poolAmount = 0;
                                }
                                amount -= pa;
                            }

                            stockShares += shares;
                            stockAmount += amount;
                            tAmount = -amount;
                            break;
                        case DELIVERY_INBOUND:
                        case TRANSFER_IN:
                            tAmount = 0; // -pt.getAmount();
                            // wie müssen Einlieferungen dividendenmäßig gebucht
                            // werden ?
                            break;
                        case SELL:
                            // Kaufpreise um mittleren Einstand reduzieren,
                            // Differenz zum Verkaufspreis im Pool bunkern
                            // Kaufmengen kumulieren
                            long cost = DividendTransaction.amountTimesShares(getStockPrice(), shares);
                            long profit = amount - cost;
                            stockShares -= shares;
                            poolShares += shares;
                            stockAmount -= cost;
                            poolAmount += profit;
                            if (stockShares == 0)
                            {
                                // Restwert komplett ausbuchen, sonst bleiben
                                // die Rundungsdifferenzen auf stockamount
                                // stehen
                                poolAmount += stockAmount;
                                stockAmount = 0;
                            }

                            if (stockShares < 0)
                            {
                                // wenn pt.getAmount immer > 0 ist, kann das
                                // nicht passieren
                                debugstop();
                                throw new UnsupportedOperationException();
                            }
                            tAmount = cost;
                            break;
                        case DELIVERY_OUTBOUND:
                        case TRANSFER_OUT:
                            // wie müssen Auslieferungen dividendenmäßig gebucht
                            // werden ?
                            tAmount = 0; // pt.getAmount ();
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }
                else
                {
                    throw new UnsupportedOperationException();
                }
                if (tAmount != 0)
                {
                    // nur relevante Positionen buchen
                    values.add(tAmount / Values.Amount.divider());
                    dates.add(tDate);
                }

            }

            // statt des Marktwertes Wert muss hier der Einstandswert gebucht
            // werden
            values.add(stockAmount / Values.Amount.divider());
            dates.add(tDateFinal);

            this.irrdiv = IRR.calculate(dates, values);
        }

        private int getEventsPerYear()
        {
            switch (periodicity)
            {
                case ANNUAL:
                    return 1;
                case SEMIANNUAL:
                    return 2;
                case QUARTERLY:
                    return 4;
                default:
                    return 0;
            }
        }

        private void calculateDIR()
        {

            divIncreasingRate = 0;
            divIncreasingYears = 0;
            int eBlock = getEventsPerYear();
            if (eBlock == 0)
                return;

            int eStart = 0;
            int nYears = 0;
            long divSum = 0;
            long divSum1 = 0;
            long divSum2 = 0;
            long divSumBreak = 0;

            for (Transaction t : transactions)
            {
                if (t instanceof DividendTransaction)
                {
                    DividendTransaction dt = (DividendTransaction) t;
                    int eCurr = dt.getDivEventId();

                    if (eCurr - eStart > eBlock)
                    {
                        if (eStart == 0)
                        {
                            divSum1 = divSum;
                        }
                        else if (divSum < divSumBreak)
                        {
                            // Dividende nicht gestiegen gegenüber dem Vorjahr
                            return;
                        }
                        else
                        {
                            divSum2 = divSum;
                        }
                        nYears++;
                        eStart = eCurr - 1;
                        divSumBreak = Math.round(divSum * 0.95);
                        divSum = 0;
                    }
                    divSum += dt.getDividendPerShare();
                }
            }
            if (nYears < 2)
                return; // periode nicht lang genug

            divIncreasingRate = (double) (divSum2 - divSum1) / (double) divSum1;
            divIncreasingRate = divIncreasingRate / nYears; // Zinseszins
                                                            // berücksichtigen
                                                            // !!
            divIncreasingYears = nYears;
        }

        private void calculateDiv12(Date endDate)
        {
            divAmount = 0;
            div12Cost = 0;
            div12Shares = 0;
            div12Amount = 0;
            div24Amount = 0;
            div36Amount = 0;
            dateFrom = null;
            dateTo = null;
            periodicity = Periodicity.NONE;
            divIncreasingRate = 0;

            // Dividenden summieren, dabei neueste Dividendenzahlung finden.
            // Da das Datum der Zahlungen von Jahr zu Jahr, von Bank zu Bank
            // variiert,
            // wird hier das Jahr um die Hälfte eines Quartals verlängert in der
            // Hoffnung,
            // das die Schwankungen nicht größer sind: 1 Jahr + 1½ Monate = 410
            // Tage
            // Beispiel: Zahlungen jeweils am 15.Feb, 15.Mai, 15.Aug. und
            // 15.Nov.2012, endDate ist 14.Feb.2013 unmittelbar vor der
            // nächsten geplanten Zahlung. Ist ein Zeitraum von 364 Tagen.
            // Vieleicht war erste Zahlung ja schon am 31.Jan.2012,
            // also lieber 6 Wochen Puffer berücksichtigen
            DividendTransaction dt = null;
            Date refDate = Helper.dateAddDays(endDate, -410);

            for (Transaction t : transactions)
            {
                if (t instanceof DividendTransaction)
                {
                    dt = (DividendTransaction) t;
                    divAmount += dt.getAmount();
                    Date dtd = dt.getDate();
                    if (dtd.before(refDate))
                    {
                        // alle Dividendenzahlungen deutlich älter als 1 Jahr
                        // ignorieren
                    }
                    else if ((dateTo == null) || (dtd.after(dateTo)))
                    {
                        dateTo = dtd;
                    }
                }
            }

            if (dateTo == null)
                return;
            if (stockAmount == 0)
                return;

            // Abgrenzungsdatum für ein Jahr: alle Zahlungen innerhalb der 10½
            // Monate = 365-45 = 320 Tage
            // beginnend mit der letzten Zahlung, die im vorigen Durchlauf
            // gefunden wurde.
            // Argumentation z.B. für Quartalszahlungen: eigentlich müssen die
            // ketzten 4 Zahlungen innerhalb von 9 Monaten
            // stattgefunden haben. Man benötigt jedoch ein Sicherheitspolster,
            // um Schwankungen auszugleichen.
            // Beispiel: Zahlungen jeweils am 15.Feb, 15.Mai, 15.Aug. und 15.Nov
            // 2012 ist ein Zeitraum von 270 Tagen
            dateFrom = Helper.dateAddDays(dateTo, -320);

            // Gesamtzeitraum für mittlere Stückzahl
            long medShare = 0;
            long medDays = 0;
            Date dCurr = null;
            Date dLast = Helper.dateAddDays(dateTo, -365);
            int bCurr = 0;
            int bLast = 0;

            for (Transaction t : transactions)
            {
                if (t instanceof DividendTransaction)
                {
                    dt = (DividendTransaction) t;
                    dCurr = dt.getDate();
                    bCurr = dt.getDivEventId();
                    if (dCurr.after(dateFrom))
                    {
                        div12Amount += dt.getAmount();
                        dt.setIsDiv12(true);
                        long days = Helper.daysBetween(dLast, dCurr);
                        medShare += days * dt.getShares();
                        medDays += days;
                        dLast = dCurr;
                    }
                    else
                    {
                        // letzte BlockId vor dem 12-Monatszeitraum merken
                        bLast = dt.getDivEventId();
                    }
                }
            }
            if (medDays > 0)
            {
                div12Shares = medShare / medDays;
                div12Cost = DividendTransaction.amountTimesShares(getStockPrice(), div12Shares);

                int nEvents = bCurr - bLast;
                if ((nEvents > 0) && (bLast == 0))
                {
                    // ohne Zahlung in der Vorperiode kann die Periodizität
                    // nicht sicher bestimmt werden
                    periodicity = Periodicity.INDEFINITE;
                }
                else
                {
                    switch (nEvents)
                    {
                        case 0:
                        {
                            periodicity = Periodicity.NONE;
                            break;
                        }
                        case 1:
                        {
                            periodicity = Periodicity.ANNUAL;
                            break;
                        }
                        case 2:
                        {
                            periodicity = Periodicity.SEMIANNUAL;
                            break;
                        }
                        case 4:
                        {
                            periodicity = Periodicity.QUARTERLY;
                            break;
                        }
                        default:
                        {
                            periodicity = Periodicity.IRREGULAR;
                            break;
                        }
                    }
                }
            }

            // calculations for expected values
            switch (periodicity)
            {
                case ANNUAL:
                case SEMIANNUAL:
                case QUARTERLY:
                case INDEFINITE:
                {
                    div24Amount = DividendTransaction.amountTimesShares(getDiv12PerShare(), stockShares);
                    break;
                }
                case IRREGULAR:
                    break; // could contain extraordinarily payments - so better
                           // ignore this
                default:
                    // UNKNOWN, NONE,
                    break; // no values;
            }

            // calculate dividend increasing rate
            calculateDIR();

            if (divIncreasingYears > 0)
            {
                div36Amount = Math.round((double) div24Amount * Math.pow(1 + divIncreasingRate, 10));
                // todo: dsr10
            }
            else
            {
                div36Amount = div24Amount;
            }
        }
    }
}
