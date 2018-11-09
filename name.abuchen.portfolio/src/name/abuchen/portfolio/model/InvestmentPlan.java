package name.abuchen.portfolio.model;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    public Class<? extends Transaction> getPlanType()
    {
        if (portfolio != null && security != null)
            return PortfolioTransaction.class;
        else if (portfolio == null && account != null && security == null)
            return AccountTransaction.class;
        else
            throw new IllegalArgumentException();
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

    public void removeTransaction(PortfolioTransaction transaction)
    {
        this.transactions.remove(transaction);
    }

    public void removeTransaction(AccountTransaction transaction)
    {
        this.transactions.remove(transaction);
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

        if (next.isBefore(start.toLocalDate()))
        {
            // start date was recently changed, use this value instead
            next = start.toLocalDate();
        }

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
        Class<? extends Transaction> planType = getPlanType();

        if (planType == PortfolioTransaction.class)
            return createSecurityTx(converter, tDate);
        else if (planType == AccountTransaction.class)
            return createDepositTx(converter, tDate);
        else
            throw new IllegalArgumentException();
    }

    private Transaction createSecurityTx(CurrencyConverter converter, LocalDate tDate)
    {
        String targetCurrencyCode = getCurrencyCode();
        boolean needsCurrencyConversion = !targetCurrencyCode.equals(security.getCurrencyCode());

        Transaction.Unit forex = null;
        long price = getSecurity().getSecurityPrice(tDate).getValue();
        long availableAmount = amount - fees;

        if (needsCurrencyConversion)
        {
            Money availableMoney = Money.of(targetCurrencyCode, amount - fees);
            availableAmount = converter.with(security.getCurrencyCode()).convert(tDate, availableMoney).getAmount();

            forex = new Transaction.Unit(Unit.Type.GROSS_VALUE, //
                            availableMoney, //
                            Money.of(security.getCurrencyCode(), availableAmount), //
                            converter.with(targetCurrencyCode).getRate(tDate, security.getCurrencyCode()).getValue());
        }

        long shares = Math
                        .round(availableAmount * Values.Share.factor() * Values.Quote.factorToMoney() / (double) price);

        if (account != null)
        {
            // create buy transaction
            BuySellEntry entry = new BuySellEntry(portfolio, account);
            entry.setType(PortfolioTransaction.Type.BUY);
            entry.setDate(tDate.atStartOfDay());
            entry.setShares(shares);
            entry.setCurrencyCode(targetCurrencyCode);
            entry.setAmount(amount);
            entry.setSecurity(getSecurity());
            entry.setNote(MessageFormat.format(Messages.InvestmentPlanAutoNoteLabel,
                            Values.DateTime.format(LocalDateTime.now()), name));

            if (fees != 0)
                entry.getPortfolioTransaction()
                                .addUnit(new Transaction.Unit(Unit.Type.FEE, Money.of(targetCurrencyCode, fees)));

            if (forex != null)
                entry.getPortfolioTransaction().addUnit(forex);

            entry.insert();
            return entry.getPortfolioTransaction();
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
            transaction.setNote(MessageFormat.format(Messages.InvestmentPlanAutoNoteLabel,
                            Values.DateTime.format(LocalDateTime.now()), name));

            if (fees != 0)
                transaction.addUnit(new Transaction.Unit(Unit.Type.FEE, Money.of(targetCurrencyCode, fees)));

            if (forex != null)
                transaction.addUnit(forex);

            portfolio.addTransaction(transaction);
            return transaction;
        }
    }

    private Transaction createDepositTx(CurrencyConverter converter, LocalDate tDate)
    {
        Money deposit = Money.of(getCurrencyCode(), amount);

        boolean needsCurrencyConversion = !getCurrencyCode().equals(account.getCurrencyCode());
        if (needsCurrencyConversion)
            deposit = converter.with(account.getCurrencyCode()).at(tDate).apply(deposit);

        // create deposit transaction
        AccountTransaction transaction = new AccountTransaction();
        transaction.setDateTime(tDate.atStartOfDay());
        transaction.setType(AccountTransaction.Type.DEPOSIT);
        transaction.setMonetaryAmount(deposit);
        transaction.setNote(MessageFormat.format(Messages.InvestmentPlanAutoNoteLabel,
                        Values.DateTime.format(LocalDateTime.now()), name));

        account.addTransaction(transaction);
        return transaction;
    }
}
