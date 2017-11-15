package name.abuchen.portfolio.model;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Dates;
import name.abuchen.portfolio.util.TradeCalendar;

public class InvestmentPlan implements Named, Adaptable
{

    private String name;
    private String note;
    private Security security;
    private Portfolio portfolio;
    private Account account;

    /**
     * Indicates whether the transactions of this investment plan are
     * automatically generated after opening the file (and updating the quotes).
     */
    private boolean autoGenerate = false;

    private LocalDateTime start;
    private int interval = 1;

    private long amount;
    private long fees;

    private List<Transaction> transactions = new ArrayList<>();

    public InvestmentPlan()
    {
        // needed for xstream de-serialization
    }

    public InvestmentPlan(String name)
    {
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String getNote()
    {
        return note;
    }

    @Override
    public void setNote(String note)
    {
        this.note = note;
    }

    public Security getSecurity()
    {
        return security;
    }

    public void setSecurity(Security security)
    {
        this.security = security;
    }

    public Portfolio getPortfolio()
    {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio)
    {
        this.portfolio = portfolio;
    }

    public Account getAccount()
    {
        return account;
    }

    public void setAccount(Account account)
    {
        this.account = account;
    }
    
    public boolean isAutoGenerate()
    {
        return autoGenerate;
    }

    public void setAutoGenerate(boolean autoGenerate)
    {
        this.autoGenerate = autoGenerate;
    }

    public LocalDate getStart()
    {
        return start.toLocalDate();
    }

    public void setStart(LocalDate start)
    {
        this.start = start.atStartOfDay();
    }
    
    public void setStart(LocalDateTime start)
    {
        this.start = start;
    }

    public int getInterval()
    {
        return interval;
    }

    public void setInterval(int interval)
    {
        this.interval = interval;
    }

    public long getAmount()
    {
        return amount;
    }

    public void setAmount(long amount)
    {
        this.amount = amount;
    }

    public long getFees()
    {
        return fees;
    }

    public void setFees(long fees)
    {
        this.fees = fees;
    }

    public List<Transaction> getTransactions()
    {
        return this.transactions;
    }

    @Deprecated
    public void removeTransaction(Transaction transaction)
    {
        transactions.remove(transaction);
    }

    public void removeTransaction(PortfolioTransaction portfolioTransaction)
    {
        this.transactions.remove(portfolioTransaction);
    }

    public void removeTransaction(AccountTransaction accountTransaction)
    {
        this.transactions.remove(accountTransaction);
    }

    public String getCurrencyCode()
    {
        return account != null ? account.getCurrencyCode() : portfolio.getReferenceAccount().getCurrencyCode();
    }

    @Override
    public <T> T adapt(Class<T> type)
    {
        if (type == Security.class)
            return type.cast(security);
        else if (type == Account.class)
            return type.cast(account);
        else if (type == Portfolio.class)
            return type.cast(portfolio);
        else
            return null;
    }

    /**
     * Returns the date of the last transaction generated
     */
    private LocalDate getLastDate()
    {
        LocalDate last = null;
        for (Transaction t : transactions)
        {
            LocalDate date = t.getDateTime().toLocalDate();
            if (last == null || last.isBefore(date))
                last = date;
        }

        return last;
    }

    /**
     * Returns the date for the next transaction to be generated based on the
     * interval
     */
    private LocalDate next(LocalDate transactionDate)
    {
        LocalDate previousDate = transactionDate;

        // the transaction date might be edited (or moved to the next months b/c
        // of public holidays) -> determine the "normalized" date by comparing
        // the three months around the current transactionDate

        if (transactionDate.getDayOfMonth() != start.getDayOfMonth())
        {
            int daysBetween = Integer.MAX_VALUE;

            LocalDate testDate = transactionDate.minusMonths(1);
            testDate = testDate.withDayOfMonth(Math.min(testDate.lengthOfMonth(), start.getDayOfMonth()));

            for (int ii = 0; ii < 3; ii++)
            {
                int d = Dates.daysBetween(transactionDate, testDate);
                if (d < daysBetween)
                {
                    daysBetween = d;
                    previousDate = testDate;
                }

                testDate = testDate.plusMonths(1);
                testDate = testDate.withDayOfMonth(Math.min(testDate.lengthOfMonth(), start.getDayOfMonth()));
            }
        }

        LocalDate next = previousDate.plusMonths(interval);

        // correct day of month (say the transactions are to be generated on the
        // 31st, but the month has only 30 days)
        next = next.withDayOfMonth(Math.min(next.lengthOfMonth(), start.getDayOfMonth()));

        // do not generate a investment plan transaction on a public holiday
        TradeCalendar tradeCalendar = new TradeCalendar();
        while (tradeCalendar.isHoliday(next))
            next = next.plusDays(1);

        return next;
    }

    public LocalDate getDateOfNextTransactionToBeGenerated()
    {
        LocalDate lastDate = getLastDate();
        return lastDate != null ? next(lastDate) : start.toLocalDate();
    }

    public List<Transaction> generateTransactions(CurrencyConverter converter)
    {
        LocalDate transactionDate = getDateOfNextTransactionToBeGenerated();
        List<Transaction> newlyCreated = new ArrayList<>();

        LocalDate now = LocalDate.now();

        while (!transactionDate.isAfter(now))
        {
            Transaction transaction = createTransaction(converter, transactionDate);

            transactions.add(transaction);
            newlyCreated.add(transaction);

            transactionDate = next(transactionDate);
        }

        return newlyCreated;
    }

    private Transaction createTransaction(CurrencyConverter converter, LocalDate tDate)
    {
        if (account == null && portfolio == null)
            return null;

        String targetCurrencyCode = getCurrencyCode();
        Transaction.Unit forex = null;
        long availableAmount = amount - fees;
        long shares          = 0L;

        long yourmilliseconds = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
        Date resultdate = new Date(yourmilliseconds);

        String note = MessageFormat.format(Messages.InvestmentPlanAutoNoteLabel, sdf.format(resultdate), name);

        if (security != null && portfolio != null)
        {
            long price = getSecurity().getSecurityPrice(tDate).getValue();
            boolean needsCurrencyConversion = !targetCurrencyCode.equals(security.getCurrencyCode());
            if (needsCurrencyConversion)
            {
                Money availableMoney = Money.of(targetCurrencyCode, amount - fees);
                availableAmount = converter.with(security.getCurrencyCode()).convert(tDate, availableMoney).getAmount();

                forex = new Transaction.Unit(Unit.Type.GROSS_VALUE, //
                            availableMoney, //
                            Money.of(security.getCurrencyCode(), availableAmount), //
                            converter.with(targetCurrencyCode).getRate(tDate, security.getCurrencyCode()).getValue());
            }

            shares = Math.round(availableAmount * Values.Share.factor() * Values.Quote.factorToMoney() / (double) price);
        }

        if (account != null)
        {
            if (portfolio != null)
            {
                // create buy transaction
                BuySellEntry entry = new BuySellEntry(portfolio, account);
                entry.setType(PortfolioTransaction.Type.BUY);
                entry.setDate(tDate.atStartOfDay());
                entry.setShares(shares);
                entry.setCurrencyCode(targetCurrencyCode);
                entry.setAmount(amount);
                entry.setSecurity(getSecurity());
                entry.setNote(note);

                if (fees != 0)
                    entry.getPortfolioTransaction()
                                    .addUnit(new Transaction.Unit(Unit.Type.FEE, Money.of(targetCurrencyCode, fees)));

                if (forex != null)
                    entry.getPortfolioTransaction().addUnit(forex);

                entry.insert();
                return (Transaction) entry.getPortfolioTransaction();
            }
            else
            {
                // create deposit transaction
                AccountTransaction transaction = new AccountTransaction();
                transaction.setDateTime(tDate.atStartOfDay());
                transaction.setType(AccountTransaction.Type.DEPOSIT);
                transaction.setCurrencyCode(targetCurrencyCode);
                transaction.setAmount(amount);
                transaction.setNote(note);

                if (fees != 0)
                    transaction.addUnit(new Transaction.Unit(Unit.Type.FEE, Money.of(targetCurrencyCode, fees)));

                if (forex != null)
                    transaction.addUnit(forex);
                account.addTransaction(transaction);
                return (Transaction) transaction;
            }
        }
        else
        {
            // create inbound delivery
            PortfolioTransaction transaction = new PortfolioTransaction();
            transaction.setDateTime(tDate.atStartOfDay());
            transaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            transaction.setSecurity(security);
            transaction.setCurrencyCode(targetCurrencyCode);
            transaction.setAmount(amount);
            transaction.setShares(shares);
            transaction.setNote(note);

            if (fees != 0)
                transaction.addUnit(new Transaction.Unit(Unit.Type.FEE, Money.of(targetCurrencyCode, fees)));

            if (forex != null)
                transaction.addUnit(forex);
            portfolio.addTransaction(transaction);
            return (Transaction) transaction;
        }
    }
}
