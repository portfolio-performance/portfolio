package name.abuchen.portfolio.model;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountOnlyTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryTransactionCreator;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Dates;
import name.abuchen.portfolio.util.TradeCalendar;
import name.abuchen.portfolio.util.TradeCalendarManager;

public class InvestmentPlan implements Named, Adaptable, Attributable
{
    public static final class UnsupportedLedgerGenerationException extends IOException
    {
        private static final long serialVersionUID = 1L;

        public UnsupportedLedgerGenerationException(String message)
        {
            super(message);
        }
    }

    public enum Type
    {
        PURCHASE_OR_DELIVERY, DEPOSIT, REMOVAL, INTEREST
    }

    /**
     * The magic number to distinguish between monthly and weekly intervals.
     */
    public static final int WEEKS_THRESHOLD = 100;

    private String name;
    private String note;
    private Security security;
    private Portfolio portfolio;
    private Account account;

    private Attributes attributes;

    /**
     * Indicates whether the transactions of this investment plan are
     * automatically generated after opening the file (and updating the quotes).
     */
    private boolean autoGenerate = false;

    private LocalDateTime start;

    /**
     * The interval in months or weeks.
     * <ul>
     * <li>Values > 0 and < 100 represent monthly intervals.</li>
     * <li>Values > 100 and < 200 represent weekly intervals (interval - 100 =
     * weeks).</li>
     * </ul>
     * <p/>
     * For monthly intervals, the day of the month is determined by the start
     * date. For weekly intervals, the day of the week is determined by the
     * start date.
     */
    private int interval = 1;

    private long amount;
    private long fees;
    private long taxes;

    private Type type;
    private String planKey;

    private List<Transaction> transactions = new ArrayList<>();
    private List<LedgerExecutionRef> ledgerExecutionRefs = new ArrayList<>();

    public InvestmentPlan()
    {
        // needed for xstream de-serialization
    }

    public InvestmentPlan(String name)
    {
        this.name = name;
    }

    public Type getPlanType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
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

    public long getTaxes()
    {
        return taxes;
    }

    public void setTaxes(long taxes)
    {
        this.taxes = taxes;
    }

    @Override
    public Attributes getAttributes()
    {
        if (attributes == null)
            attributes = new Attributes();
        return attributes;
    }

    @Override
    public void setAttributes(Attributes attributes)
    {
        this.attributes = attributes;
    }

    public String getPlanKey()
    {
        if (planKey == null || planKey.isBlank())
            planKey = "plan-" + UUID.randomUUID(); //$NON-NLS-1$

        return planKey;
    }

    public void setPlanKey(String planKey)
    {
        this.planKey = planKey;
    }

    public List<Transaction> getTransactions()
    {
        return this.transactions;
    }

    public List<LedgerExecutionRef> getLedgerExecutionRefs()
    {
        if (ledgerExecutionRefs == null)
            ledgerExecutionRefs = new ArrayList<>();

        return ledgerExecutionRefs;
    }

    public void addLedgerExecutionRef(LedgerExecutionRef executionRef)
    {
        getLedgerExecutionRefs().add(executionRef);
    }

    /**
     * Returns a list of transaction pairs, i.e. transaction and the owner
     * (account or portfolio). As the list of transactions is part of the XML
     * format, we cannot change the InvestmentPlan class.
     */
    public List<TransactionPair<?>> getTransactions(Client client)
    {
        migrateLegacyLedgerExecutionRefs(client);

        List<TransactionPair<?>> answer = new ArrayList<>();

        for (Transaction t : transactions)
        {
            if (t instanceof AccountTransaction at)
                answer.add(new TransactionPair<>(lookupOwner(client, at), at));
            else
                answer.add(new TransactionPair<>(lookupOwner(client, (PortfolioTransaction) t),
                                (PortfolioTransaction) t));
        }

        resolveGeneratedLedgerTransactions(client).forEach(answer::add);

        return answer;
    }

    private List<TransactionPair<?>> resolveGeneratedLedgerTransactions(Client client)
    {
        var answer = new ArrayList<TransactionPair<?>>();

        client.getLedger().getEntries().stream() //
                        .filter(entry -> getPlanKey().equals(entry.getGeneratedByPlanKey())) //
                        .sorted(Comparator.comparing((LedgerEntry entry) -> Optional.ofNullable(
                                        entry.getPlanExecutionDate()).orElse(entry.getDateTime().toLocalDate()))
                                        .thenComparing(entry -> Optional.ofNullable(
                                                        entry.getPlanExecutionSequence()).orElse(0))) //
                        .map(entry -> resolveGeneratedLedgerTransaction(client, entry)) //
                        .forEach(answer::add);

        return answer;
    }

    private TransactionPair<?> resolveGeneratedLedgerTransaction(Client client, LedgerEntry entry)
    {
        var candidates = new ArrayList<Transaction>();

        for (var owner : client.getAccounts())
            owner.getTransactions().stream().filter(transaction -> isBackedBy(transaction, entry))
                            .forEach(candidates::add);

        for (var owner : client.getPortfolios())
            owner.getTransactions().stream().filter(transaction -> isBackedBy(transaction, entry))
                            .forEach(candidates::add);

        var preferred = entry.getPreferredViewKind();
        if (LedgerExecutionViewKind.ACCOUNT.name().equals(preferred))
            candidates.removeIf(candidate -> !(candidate instanceof AccountTransaction));
        else if (LedgerExecutionViewKind.PORTFOLIO.name().equals(preferred))
            candidates.removeIf(candidate -> !(candidate instanceof PortfolioTransaction));

        if (candidates.size() != 1)
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CORE_001
                            .message("Ambiguous ledger plan execution " + getPlanKey())); //$NON-NLS-1$

        var transaction = candidates.get(0);
        if (transaction instanceof AccountTransaction at)
            return new TransactionPair<>(lookupOwner(client, at), at);

        return new TransactionPair<>(lookupOwner(client, (PortfolioTransaction) transaction),
                        (PortfolioTransaction) transaction);
    }

    private boolean isBackedBy(Transaction transaction, LedgerEntry entry)
    {
        return transaction instanceof LedgerBackedTransaction ledgerBackedTransaction
                        && ledgerBackedTransaction.getLedgerEntry() == entry;
    }

    /**
     * Returns the owner of the transaction. Because an investment plan can be
     * updated, older transactions do not necessarily belong to the account that
     * is currently configured for by the plan.
     */
    private Account lookupOwner(Client client, AccountTransaction t)
    {
        if (account != null && account.getTransactions().contains(t))
            return account;

        return client.getAccounts().stream().filter(a -> a.getTransactions().contains(t)).findAny()
                        .orElseThrow(IllegalArgumentException::new);
    }

    /**
     * Returns the owner of the transaction. Because an investment plan can be
     * updated, older transactions do not necessarily belong to the portfolio
     * that is currently configured for the plan.
     */
    private Portfolio lookupOwner(Client client, PortfolioTransaction t)
    {
        if (portfolio != null && portfolio.getTransactions().contains(t))
            return portfolio;

        return client.getPortfolios().stream().filter(a -> a.getTransactions().contains(t)).findAny()
                        .orElseThrow(IllegalArgumentException::new);
    }

    public void removeTransaction(PortfolioTransaction transaction)
    {
        this.transactions.remove(transaction);
        removeLedgerExecutionRef(transaction);
    }

    public void removeTransaction(AccountTransaction transaction)
    {
        this.transactions.remove(transaction);
        removeLedgerExecutionRef(transaction);
    }

    private void removeLedgerExecutionRef(Transaction transaction)
    {
        if (!(transaction instanceof LedgerBackedTransaction ledgerBackedTransaction))
            return;

        clearPlanExecution(ledgerBackedTransaction.getLedgerEntry());
    }

    public void removeLedgerExecutionRefs(LedgerEntry entry)
    {
        Objects.requireNonNull(entry);

        clearPlanExecution(entry);
        getLedgerExecutionRefs().removeIf(ref -> entry.getUUID().equals(ref.getLedgerEntryUUID()));
    }

    public static final class LedgerExecutionRef
    {
        private String ledgerEntryUUID;
        private String projectionUUID;
        private LedgerProjectionRole projectionRole;

        public LedgerExecutionRef()
        {
            // needed for xstream de-serialization
        }

        public LedgerExecutionRef(String ledgerEntryUUID, String projectionUUID, LedgerProjectionRole projectionRole)
        {
            this.ledgerEntryUUID = ledgerEntryUUID;
            this.projectionUUID = projectionUUID;
            this.projectionRole = projectionRole;
        }

        public static LedgerExecutionRef of(LedgerBackedTransaction transaction)
        {
            return new LedgerExecutionRef(transaction.getLedgerEntry().getUUID(),
                            transaction.getLedgerProjectionRef().getUUID(),
                            transaction.getLedgerProjectionRef().getRole());
        }

        public String getLedgerEntryUUID()
        {
            return ledgerEntryUUID;
        }

        public String getProjectionUUID()
        {
            return projectionUUID;
        }

        public LedgerProjectionRole getProjectionRole()
        {
            return projectionRole;
        }

        private Transaction resolve(Client client)
        {
            var candidates = new ArrayList<Transaction>();

            for (var account : client.getAccounts())
                account.getTransactions().stream().filter(this::matches).forEach(candidates::add);

            for (var portfolio : client.getPortfolios())
                portfolio.getTransactions().stream().filter(this::matches).forEach(candidates::add);

            if (candidates.size() == 1)
                return candidates.get(0);

            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CORE_001
                            .message("Ambiguous ledger execution reference " + ledgerEntryUUID)); //$NON-NLS-1$
        }

        private boolean matches(Transaction transaction)
        {
            if (!(transaction instanceof LedgerBackedTransaction ledgerBackedTransaction))
                return false;

            return ledgerEntryUUID.equals(ledgerBackedTransaction.getLedgerEntry().getUUID())
                            && (projectionUUID == null
                                            || projectionUUID.equals(
                                                            ledgerBackedTransaction.getLedgerProjectionRef().getUUID()))
                            && (projectionRole == null
                                            || projectionRole == ledgerBackedTransaction.getLedgerProjectionRef()
                                                            .getRole());
        }
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
    public Optional<LocalDate> getLastDate()
    {
        LocalDate last = null;
        for (Transaction t : transactions)
        {
            LocalDate date = t.getDateTime().toLocalDate();
            if (last == null || last.isBefore(date))
                last = date;
        }

        return Optional.ofNullable(last);
    }

    public Optional<LocalDate> getLastDate(Client client)
    {
        migrateLegacyLedgerExecutionRefs(client);

        LocalDate last = getLastDate().orElse(null);

        for (var entry : client.getLedger().getEntries())
        {
            if (!getPlanKey().equals(entry.getGeneratedByPlanKey()))
                continue;

            LocalDate date = Optional.ofNullable(entry.getPlanExecutionDate())
                            .orElse(entry.getDateTime().toLocalDate());
            if (last == null || last.isBefore(date))
                last = date;
        }

        return Optional.ofNullable(last);
    }

    /**
     * Returns the date for the next transaction to be generated based on the
     * interval
     */
    private LocalDate next(LocalDate transactionDate)
    {
        LocalDate previousDate = transactionDate;
        LocalDate next;
        if (interval < WEEKS_THRESHOLD) // monthly intervals
        {
            // the transaction date might be edited (or moved to the next months
            // b/c of public holidays) -> determine the "normalized" date by
            // comparing the three months around the current transactionDate

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

            next = previousDate.plusMonths(interval);
            // correct day of month (say the transactions are to be generated on
            // the 31st, but the month has only 30 days)
            next = next.withDayOfMonth(Math.min(next.lengthOfMonth(), start.getDayOfMonth()));
        }
        else // weekly or bi weekly intervals
        {
            // the transaction date might be edited (or moved because of public
            // holidays). Revert back to the day of the week.

            if (transactionDate.getDayOfWeek() != start.getDayOfWeek())
            {
                int offset = transactionDate.getDayOfWeek().getValue() - start.getDayOfWeek().getValue();
                offset = offset > 0 ? offset : offset + 7;
                previousDate = transactionDate.minusDays(offset);
            }

            next = previousDate.plusWeeks((long) interval - WEEKS_THRESHOLD);
        }

        if (next.isBefore(start.toLocalDate()))
        {
            // start date was recently changed, use this value instead
            next = start.toLocalDate();
        }

        // do not generate a investment plan transaction on a public holiday
        TradeCalendar tradeCalendar = security != null ? TradeCalendarManager.getInstance(security)
                        : TradeCalendarManager.getDefaultInstance();
        while (tradeCalendar.isHoliday(next))
            next = next.plusDays(1);

        return next;
    }

    public LocalDate getDateOfNextTransactionToBeGenerated()
    {
        Optional<LocalDate> lastDate = getLastDate();

        if (lastDate.isPresent())
        {
            return next(lastDate.get());
        }
        else
        {
            LocalDate startDate = start.toLocalDate();

            // do not generate a investment plan transaction on a public holiday
            TradeCalendar tradeCalendar = security != null ? TradeCalendarManager.getInstance(security)
                            : TradeCalendarManager.getDefaultInstance();
            while (tradeCalendar.isHoliday(startDate))
                startDate = startDate.plusDays(1);

            return startDate;
        }
    }

    public List<TransactionPair<?>> generateTransactions(CurrencyConverter converter) throws IOException
    {
        LocalDate transactionDate = getDateOfNextTransactionToBeGenerated();
        List<TransactionPair<?>> newlyCreated = new ArrayList<>();

        LocalDate now = LocalDate.now();

        while (!transactionDate.isAfter(now))
        {
            TransactionPair<?> transaction = createTransaction(converter, transactionDate);

            transactions.add(transaction.getTransaction());
            newlyCreated.add(transaction);

            transactionDate = next(transactionDate);
        }

        return newlyCreated;
    }

    public LocalDate getDateOfNextTransactionToBeGenerated(Client client)
    {
        Optional<LocalDate> lastDate = getLastDate(client);

        if (lastDate.isPresent())
        {
            return next(lastDate.get());
        }
        else
        {
            LocalDate startDate = start.toLocalDate();

            TradeCalendar tradeCalendar = security != null ? TradeCalendarManager.getInstance(security)
                            : TradeCalendarManager.getDefaultInstance();
            while (tradeCalendar.isHoliday(startDate))
                startDate = startDate.plusDays(1);

            return startDate;
        }
    }

    public List<TransactionPair<?>> generateTransactions(Client client, CurrencyConverter converter) throws IOException
    {
        LocalDate transactionDate = getDateOfNextTransactionToBeGenerated(client);
        List<TransactionPair<?>> newlyCreated = new ArrayList<>();

        LocalDate now = LocalDate.now();

        while (!transactionDate.isAfter(now))
        {
            TransactionPair<?> transaction = createLedgerTransaction(client, converter, transactionDate);
            markLedgerExecution((LedgerBackedTransaction) transaction.getTransaction(), transactionDate);
            newlyCreated.add(transaction);

            transactionDate = next(transactionDate);
        }

        return newlyCreated;
    }

    void markLedgerExecution(LedgerBackedTransaction transaction)
    {
        markLedgerExecution(transaction, transaction.getLedgerEntry().getDateTime().toLocalDate());
    }

    private void markLedgerExecution(LedgerBackedTransaction transaction, LocalDate executionDate)
    {
        var entry = transaction.getLedgerEntry();
        entry.setGeneratedByPlanKey(getPlanKey());
        entry.setPlanExecutionDate(executionDate);
        entry.setPlanExecutionSequence(null);
        entry.setPreferredViewKind(viewKind(transaction).name());
    }

    void markLedgerExecution(LedgerEntry entry, LocalDate executionDate, LedgerExecutionViewKind preferredViewKind)
    {
        entry.setGeneratedByPlanKey(getPlanKey());
        entry.setPlanExecutionDate(executionDate);
        entry.setPlanExecutionSequence(null);
        entry.setPreferredViewKind(preferredViewKind != null ? preferredViewKind.name() : null);
    }

    private LedgerExecutionViewKind viewKind(LedgerBackedTransaction transaction)
    {
        if (transaction instanceof LedgerBackedPortfolioTransaction)
            return LedgerExecutionViewKind.PORTFOLIO;
        if (transaction instanceof LedgerBackedAccountTransaction)
            return LedgerExecutionViewKind.ACCOUNT;

        throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CORE_001
                        .message("Unsupported ledger backed plan transaction")); //$NON-NLS-1$
    }

    private void clearPlanExecution(LedgerEntry entry)
    {
        if (!getPlanKey().equals(entry.getGeneratedByPlanKey()))
            return;

        entry.setGeneratedByPlanKey(null);
        entry.setPlanExecutionDate(null);
        entry.setPlanExecutionSequence(null);
        entry.setPreferredViewKind(null);
    }

    private void migrateLegacyLedgerExecutionRefs(Client client)
    {
        if (getLedgerExecutionRefs().isEmpty())
            return;

        for (var ref : getLedgerExecutionRefs())
        {
            var transaction = ref.resolve(client);
            if (transaction instanceof LedgerBackedTransaction ledgerBackedTransaction)
                markLedgerExecution(ledgerBackedTransaction);
        }

        getLedgerExecutionRefs().clear();
    }

    private TransactionPair<?> createTransaction(CurrencyConverter converter, LocalDate tDate) throws IOException
    {
        Type planType = getPlanType();

        if (planType == Type.PURCHASE_OR_DELIVERY)
            return createSecurityTx(converter, tDate);
        else if (planType == Type.DEPOSIT || planType == Type.REMOVAL || planType == Type.INTEREST)
            return createAccountTx(converter, tDate);
        else
            throw new IllegalArgumentException("unsupported plan type " + planType.name()); //$NON-NLS-1$
    }

    private TransactionPair<?> createLedgerTransaction(Client client, CurrencyConverter converter, LocalDate tDate)
                    throws IOException
    {
        Type planType = getPlanType();

        if (planType == Type.PURCHASE_OR_DELIVERY)
            return createLedgerSecurityTx(client, converter, tDate);
        else if (planType == Type.DEPOSIT || planType == Type.REMOVAL || planType == Type.INTEREST)
            return createLedgerAccountTx(client, converter, tDate);
        else
            throw new IllegalArgumentException(LedgerDiagnosticCode.LEDGER_CORE_002
                            .message("unsupported plan type " + planType.name())); //$NON-NLS-1$
    }

    private TransactionPair<?> createSecurityTx(CurrencyConverter converter, LocalDate tDate) throws IOException
    {
        String targetCurrencyCode = getCurrencyCode();
        boolean needsCurrencyConversion = !targetCurrencyCode.equals(security.getCurrencyCode());

        Transaction.Unit forex = null;
        long price = getSecurity().getSecurityPrice(tDate).getValue();

        if (price == 0L)
            throw new IOException(MessageFormat.format(
                            Messages.MsgErrorInvestmentPlanMissingSecurityPricesToGenerateTransaction,
                            getSecurity().getName()));

        long availableAmount = amount - fees - taxes;

        if (needsCurrencyConversion)
        {
            Money availableMoney = Money.of(targetCurrencyCode, availableAmount);
            availableAmount = converter.with(security.getCurrencyCode()).convert(tDate, availableMoney).getAmount();

            forex = new Transaction.Unit(Unit.Type.GROSS_VALUE, //
                            availableMoney, //
                            Money.of(security.getCurrencyCode(), availableAmount), //
                            converter.with(targetCurrencyCode).getRate(tDate, security.getCurrencyCode()).getValue());
        }

        long shares = Math
                        .round((double) availableAmount * Values.Share.factor() * Values.Quote.factorToMoney() / price);

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

            if (taxes != 0)
                entry.getPortfolioTransaction()
                                .addUnit(new Transaction.Unit(Unit.Type.TAX, Money.of(targetCurrencyCode, taxes)));

            if (forex != null)
                entry.getPortfolioTransaction().addUnit(forex);

            entry.insert();
            return new TransactionPair<>(portfolio, entry.getPortfolioTransaction());
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

            if (taxes != 0)
                transaction.addUnit(new Transaction.Unit(Unit.Type.TAX, Money.of(targetCurrencyCode, taxes)));

            if (forex != null)
                transaction.addUnit(forex);

            portfolio.addTransaction(transaction);
            return new TransactionPair<>(portfolio, transaction);
        }
    }

    private TransactionPair<?> createLedgerSecurityTx(Client client, CurrencyConverter converter, LocalDate tDate)
                    throws IOException
    {
        var generated = generatedSecurityFacts(converter, tDate);
        var note = MessageFormat.format(Messages.InvestmentPlanAutoNoteLabel,
                        Values.DateTime.format(LocalDateTime.now()), name);

        if (account != null)
        {
            var entry = new LedgerBuySellTransactionCreator(client).create(portfolio, account,
                            PortfolioTransaction.Type.BUY, tDate.atStartOfDay(), amount, generated.currencyCode(),
                            getSecurity(), generated.shares(), generated.units(), note, null);
            return new TransactionPair<>(portfolio, entry.getPortfolioTransaction());
        }
        else
        {
            var transaction = new LedgerDeliveryTransactionCreator(client).create(portfolio,
                            PortfolioTransaction.Type.DELIVERY_INBOUND, tDate.atStartOfDay(), amount,
                            generated.currencyCode(), security, generated.shares(), null, null, generated.units(), note,
                            null);
            return new TransactionPair<>(portfolio, transaction);
        }
    }

    private GeneratedSecurityFacts generatedSecurityFacts(CurrencyConverter converter, LocalDate tDate)
                    throws IOException
    {
        String targetCurrencyCode = getCurrencyCode();
        boolean needsCurrencyConversion = !targetCurrencyCode.equals(security.getCurrencyCode());

        Transaction.Unit forex = null;
        long price = getSecurity().getSecurityPrice(tDate).getValue();

        if (price == 0L)
            throw new IOException(MessageFormat.format(
                            Messages.MsgErrorInvestmentPlanMissingSecurityPricesToGenerateTransaction,
                            getSecurity().getName()));

        long availableAmount = amount - fees - taxes;

        if (needsCurrencyConversion)
        {
            Money availableMoney = Money.of(targetCurrencyCode, availableAmount);
            availableAmount = converter.with(security.getCurrencyCode()).convert(tDate, availableMoney).getAmount();

            forex = new Transaction.Unit(Unit.Type.GROSS_VALUE, availableMoney,
                            Money.of(security.getCurrencyCode(), availableAmount),
                            converter.with(targetCurrencyCode).getRate(tDate, security.getCurrencyCode()).getValue());
        }

        long shares = Math
                        .round((double) availableAmount * Values.Share.factor() * Values.Quote.factorToMoney() / price);

        var units = new ArrayList<Transaction.Unit>();
        if (fees != 0)
            units.add(new Transaction.Unit(Unit.Type.FEE, Money.of(targetCurrencyCode, fees)));
        if (taxes != 0)
            units.add(new Transaction.Unit(Unit.Type.TAX, Money.of(targetCurrencyCode, taxes)));
        if (forex != null)
            units.add(forex);

        return new GeneratedSecurityFacts(targetCurrencyCode, shares, units);
    }

    private TransactionPair<?> createAccountTx(CurrencyConverter converter, LocalDate tDate)
    {
        AccountTransaction.Type transactionType;

        switch (type)
        {
            case DEPOSIT:
                transactionType = AccountTransaction.Type.DEPOSIT;
                break;
            case REMOVAL:
                transactionType = AccountTransaction.Type.REMOVAL;
                break;
            case INTEREST:
                transactionType = AccountTransaction.Type.INTEREST;
                break;
            default:
                throw new IllegalArgumentException();
        }

        Money monetaryAmount = Money.of(getCurrencyCode(), amount);

        boolean needsCurrencyConversion = !getCurrencyCode().equals(account.getCurrencyCode());
        if (needsCurrencyConversion)
            monetaryAmount = converter.with(account.getCurrencyCode()).at(tDate).apply(monetaryAmount);

        // create deposit transaction
        AccountTransaction transaction = new AccountTransaction();
        transaction.setDateTime(tDate.atStartOfDay());
        transaction.setType(transactionType);
        transaction.setMonetaryAmount(monetaryAmount);
        transaction.setNote(MessageFormat.format(Messages.InvestmentPlanAutoNoteLabel,
                        Values.DateTime.format(LocalDateTime.now()), name));

        if (taxes != 0)
            transaction.addUnit(new Transaction.Unit(Transaction.Unit.Type.TAX,
                            Money.of(account.getCurrencyCode(), taxes)));

        account.addTransaction(transaction);
        return new TransactionPair<>(account, transaction);
    }

    private TransactionPair<?> createLedgerAccountTx(Client client, CurrencyConverter converter, LocalDate tDate)
                    throws UnsupportedLedgerGenerationException
    {
        AccountTransaction.Type transactionType;

        switch (type)
        {
            case DEPOSIT:
                transactionType = AccountTransaction.Type.DEPOSIT;
                break;
            case REMOVAL:
                transactionType = AccountTransaction.Type.REMOVAL;
                break;
            case INTEREST:
                transactionType = AccountTransaction.Type.INTEREST;
                break;
            default:
                throw new IllegalArgumentException();
        }

        Money monetaryAmount = Money.of(getCurrencyCode(), amount);

        boolean needsCurrencyConversion = !getCurrencyCode().equals(account.getCurrencyCode());
        if (needsCurrencyConversion)
            monetaryAmount = converter.with(account.getCurrencyCode()).at(tDate).apply(monetaryAmount);

        var units = new ArrayList<Transaction.Unit>();
        if (taxes != 0)
            units.add(new Transaction.Unit(Transaction.Unit.Type.TAX, Money.of(account.getCurrencyCode(), taxes)));

        if (!units.isEmpty()
                        && (transactionType == AccountTransaction.Type.DEPOSIT
                                        || transactionType == AccountTransaction.Type.REMOVAL))
            throw new UnsupportedLedgerGenerationException(LedgerDiagnosticCode.LEDGER_CORE_003
                            .message("Ledger investment plan generation cannot preserve units for " + transactionType)); //$NON-NLS-1$

        var transaction = new LedgerAccountOnlyTransactionCreator(client).create(account, transactionType,
                        tDate.atStartOfDay(), monetaryAmount.getAmount(), monetaryAmount.getCurrencyCode(), null, units,
                        MessageFormat.format(Messages.InvestmentPlanAutoNoteLabel,
                                        Values.DateTime.format(LocalDateTime.now()), name),
                        null);

        return new TransactionPair<>(account, transaction);
    }

    private record GeneratedSecurityFacts(String currencyCode, long shares, List<Transaction.Unit> units)
    {
    }

    public enum LedgerExecutionViewKind
    {
        ACCOUNT, PORTFOLIO
    }

    @Override
    public String toString()
    {
        return name;
    }
}
