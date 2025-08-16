package name.abuchen.portfolio.snapshot.security;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.snapshot.trail.TrailRecord;
import name.abuchen.portfolio.util.Interval;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;


/**
 * Calculates capital gains per day using FIFO logic.
 * This provides accurate realized and unrealized capital gains.
 */
public class DailyCapitalGainsCalculation
{
    private final Map<LocalDate, CapitalGainsRecord> realizedGainsPerDay = new HashMap<>();
    private final Map<LocalDate, CapitalGainsRecord> unrealizedGainsPerDay = new HashMap<>();
    private final Client client;
    private final CurrencyConverter converter;
    private final Interval interval;
    private final String termCurrency;

    public DailyCapitalGainsCalculation(Client client, CurrencyConverter converter, Interval interval)
    {
        this.client = client;
        this.converter = converter;
        this.interval = interval;
        this.termCurrency = converter.getTermCurrency();
    }

    /**
     * Data for tracking security gains calculations.
     */
    private static class SecurityCalculationData
    {
        private long previousGain;
        private long previousFxGain;
        private List<FifoItem> fifoQueue;

        public SecurityCalculationData()
        {
            this.previousGain = 0;
            this.previousFxGain = 0;
            this.fifoQueue = new ArrayList<>();
        }
    }

    /**
     * FIFO queue item for tracking buy transactions with full transaction details.
     */
    private static class FifoItem
    {
        private final LocalDate date;
        private long remainingShares;
        private long remainingValue;
        private final PortfolioTransaction transaction;

        public FifoItem(LocalDate date, long shares, long convertedGrossValueAmount, PortfolioTransaction transaction)
        {
            this.date = date;
            this.remainingShares = shares;
            this.remainingValue = convertedGrossValueAmount;
            this.transaction = transaction;
        }

        public long getRemainingShares()
        {
            return remainingShares;
        }

        public void reduceShares(long sharesToReduce)
        {
            this.remainingShares -= sharesToReduce;
        }

        public long getRemainingValue()
        {
            return remainingValue;
        }

        public void reduceValue(long valueToReduce)
        {
            this.remainingValue -= valueToReduce;
        }

        public boolean isFullyUsed()
        {
            return remainingShares <= 0;
        }
    }

    /**
     * Calculate daily capital gains for all securities.
     */
    public void calculate()
    {
        clear();
        
        // Get all securities from client
        List<Security> securities = client.getSecurities();
        if (securities.isEmpty())
        {
            return;
        }

        Map<Security, SecurityCalculationData> securitiesCalculationData = new HashMap<>();

        // Initialize current positions at interval start
        initializeCurrentPositions(securities, securitiesCalculationData);

        // Group transactions across all portfolios date range, sorted by date
        Map<LocalDate, List<PortfolioTransaction>> transactionsByDate = client.getPortfolios().stream()
            .flatMap(portfolio -> portfolio.getTransactions().stream())
            .filter(transaction -> transaction.getSecurity() != null)
            .filter(transaction -> securities.contains(transaction.getSecurity()))
            .filter(transaction -> {
                LocalDate transactionDate = transaction.getDateTime().toLocalDate();
                return !transactionDate.isBefore(interval.getStart()) && !transactionDate.isAfter(interval.getEnd());
            })
            .sorted((t1, t2) -> t1.getDateTime().compareTo(t2.getDateTime()))
            .collect(Collectors.groupingBy(
                transaction -> transaction.getDateTime().toLocalDate(),
                Collectors.toList()
            ));

        // Iterate through each day in the interval
        LocalDate currentDate = interval.getStart();
        
        while (!currentDate.isAfter(interval.getEnd()))
        {
            // 1. Get all transactions for this day and group by security
            List<PortfolioTransaction> dayTransactions = transactionsByDate.getOrDefault(currentDate, new ArrayList<>());
            Map<Security, List<PortfolioTransaction>> transactionsBySecurity = dayTransactions.stream()
                .collect(Collectors.groupingBy(PortfolioTransaction::getSecurity));

            // 2. Process each security: transactions first, then unrealized gains
            for (Security security : securities)
            {
                SecurityCalculationData securityData = securitiesCalculationData.computeIfAbsent(security, s -> new SecurityCalculationData());
                
                // Process all transactions for this security on this day
                for (PortfolioTransaction transaction : transactionsBySecurity.getOrDefault(security, new ArrayList<>()))
                {
                    processTransaction(transaction, currentDate, securityData);
                }
                
                // Calculate unrealized gains for this security on this day
                calculateUnrealizedGainsForSecurity(security, currentDate, securityData);
            }

            currentDate = currentDate.plusDays(1);
        }
    }

    /**
     * Initialize FIFO queues with existing positions from one day before the interval start date.
     * This ensures we have the correct starting point for unrealized gains calculation.
     */
    private void initializeCurrentPositions(List<Security> securities, Map<Security, SecurityCalculationData> securitiesCalculationData)
    {
        LocalDate oneDayBeforeInterval = interval.getStart();
        
        // Get client snapshot for the day before interval starts
        ClientSnapshot snapshot = ClientSnapshot.create(client, converter, oneDayBeforeInterval);
        
        for (Security security : securities)
        {
            SecurityCalculationData securityData = securitiesCalculationData.computeIfAbsent(security, s -> new SecurityCalculationData());
            
            // Get current position for this security from the snapshot
            // Use the joint portfolio to get positions by security
            SecurityPosition position = snapshot.getJointPortfolio().getPositionsBySecurity().get(security);
            
            if (position != null && position.getShares() != 0)
            {
                // Create a FifoItem representing the current position
                // We'll use the current market value as the basis for unrealized gains calculation
                long currentShares = position.getShares();
                
                // Calculate the current value using the position's price
                Money currentValue = position.calculateValue();
                Money convertedValue = converter.convert(oneDayBeforeInterval, currentValue);
                long currentValueAmount = convertedValue.getAmount();
                
                // Create FifoItem for the current position
                FifoItem currentPosition = new FifoItem(
                    oneDayBeforeInterval,    // buy date (one day before interval)
                    currentShares,           // original shares
                    currentValueAmount,      // original value
                    null);                   // no specific transaction for starting position
                
                securityData.fifoQueue.add(currentPosition);
            }
        }
    }

    /**
     * Process a single transaction and update the FIFO queue for its security.
     */
    private void processTransaction(PortfolioTransaction transaction, LocalDate transactionDate, SecurityCalculationData securityData)
    {
        Money grossValue = transaction.getGrossValue();
        Money convertedGrossValue = grossValue.with(converter.at(transaction.getDateTime()));

        switch (transaction.getType())
        {
            case BUY:
            case DELIVERY_INBOUND:
                // Add to FIFO queue with full transaction details
                securityData.fifoQueue.add(new FifoItem(transactionDate, transaction.getShares(), convertedGrossValue.getAmount(), transaction));
                break;

            case SELL:
            case DELIVERY_OUTBOUND:
                processRealizedGains(transaction, transactionDate, securityData, convertedGrossValue);
                break;
        }
    }

    /**
     * Process a sell transaction and calculate realized gains using FIFO logic.
     */
    private void processRealizedGains(PortfolioTransaction transaction, LocalDate transactionDate, 
                                      SecurityCalculationData securityData, Money convertedGrossValue)
    {
        Security security = transaction.getSecurity();
        long shares = transaction.getShares();
        long sharesToSell = shares;
        
        // Get or create CapitalGainsRecord for this date
        CapitalGainsRecord realizedGainsForDate = realizedGainsPerDay.computeIfAbsent(transactionDate, 
            date -> new CapitalGainsRecord(security, termCurrency));

        // First, try to match against existing FIFO items
        for (FifoItem fifoItem : securityData.fifoQueue)
        {
            if (sharesToSell <= 0)
                break;

            if (fifoItem.getRemainingShares() == 0)
                continue;

            long sharesFromThisItem = Math.min(sharesToSell, fifoItem.getRemainingShares());
            
            // Calculate buy value for these shares using the same fraction logic as CapitalGainsCalculation
            long start = Math.round((double) sharesFromThisItem / fifoItem.remainingShares * fifoItem.remainingValue);
            
            // Calculate sell value for these shares using the same fraction logic
            long end = Math.round((double) sharesFromThisItem / shares * convertedGrossValue.getAmount());
            
            // Calculate forex gains for realized gains
            long forexGain = 0L;

            if (!termCurrency.equals(transaction.getSecurity().getCurrencyCode()))
            {
                // Calculate currency gains by converting the start value to forex and converting it back
                CurrencyConverter convert2forex = converter.with(transaction.getSecurity().getCurrencyCode());

                Money forex = convert2forex.convert(fifoItem.date, Money.of(termCurrency, start));
                Money back = forex.with(converter.at(transaction.getDateTime()));
                forexGain = back.getAmount() - start;
            }
            
            // Add to realized gains
            realizedGainsForDate.addCapitalGains(Money.of(termCurrency, end - start));
            realizedGainsForDate.addForexCaptialGains(Money.of(termCurrency, forexGain));

            // Update FIFO item
            fifoItem.reduceShares(sharesFromThisItem);
            fifoItem.reduceValue(start);

            sharesToSell -= sharesFromThisItem;
        }

        // Handle short selling (remaining shares after exhausting FIFO queue)
        if (sharesToSell > 0)
        {
            // Calculate short selling gain
            long shortSellingGain = Math.round((double) sharesToSell / shares * convertedGrossValue.getAmount());
            
            // Add to realized gains
            realizedGainsForDate.addCapitalGains(Money.of(termCurrency, shortSellingGain));
        }
    }

    /**
     * Calculate unrealized gains for a specific security on a specific date.
     */
    private void calculateUnrealizedGainsForSecurity(Security security, LocalDate currentDate, SecurityCalculationData securityData)
    {
        long currentGain = calculateCurrentGain(security, currentDate, securityData);
        long gainDiff = currentGain - securityData.previousGain;
        securityData.previousGain = currentGain;

        long currentFxGain = calculateCurrentFxGain(security, currentDate, securityData);
        long fxGainDiff = currentFxGain - securityData.previousFxGain;
        securityData.previousFxGain = currentFxGain;

        // Record relative (daily) unrealized gains
        CapitalGainsRecord unrealizedGainsForDate = unrealizedGainsPerDay.computeIfAbsent(currentDate, 
            date -> new CapitalGainsRecord(security, termCurrency));
        unrealizedGainsForDate.addCapitalGains(Money.of(termCurrency, gainDiff));
        unrealizedGainsForDate.addForexCaptialGains(Money.of(termCurrency, fxGainDiff));  
    }

    private long calculateCurrentGain(Security security, LocalDate currentDate, SecurityCalculationData securityData) {
        SecurityPrice currentPriceFx = security.getSecurityPrice(currentDate);
        long shares = securityData.fifoQueue.stream().mapToLong(item -> item.getRemainingShares()).sum();

        // Calculate the initial market value of the FIFO queue
        long initialMarketValue = securityData.fifoQueue.stream().mapToLong(item -> item.getRemainingValue()).sum();

        // Calculate the current market value of the FIFO queue in intervalEnd's termCurrency
        long currentMarketValue = converter.convert(interval.getEnd(), calculatePositionValue(security, currentPriceFx, shares)).getAmount();

        // Calculate the current gain
        return currentMarketValue - initialMarketValue;
    }

    private long calculateCurrentFxGain(Security security, LocalDate currentDate, SecurityCalculationData securityData) {
        CurrencyConverter convert2forex = converter.with(security.getCurrencyCode());

        // Calculate the initial market value of the FIFO queue in the security's currency
        long initialMarketValueFX = securityData.fifoQueue.stream().mapToLong(item -> convert2forex.convert(item.date, Money.of(termCurrency, item.getRemainingValue())).getAmount()).sum();

        // Calculate the initial value of the FIFO queue in termCurrency
        long initialValueAtInitialConversion = securityData.fifoQueue.stream().mapToLong(item -> item.getRemainingValue()).sum();

        // Calculate the current value of the FIFO queue in termCurrency
        long initialValueAtCurrentConversion = converter.convert(currentDate, Money.of(security.getCurrencyCode(), initialMarketValueFX)).getAmount();

        // Calculate the current FX gain by subtracting the initial value from the current value
        return initialValueAtCurrentConversion - initialValueAtInitialConversion;
    }

    /**
     * Calculate the value of a position in the security's currency.
     */
    public Money calculatePositionValue(Security security, SecurityPrice price,  long shares)
    {
        long marketValue = BigDecimal.valueOf(shares) //
                        .movePointLeft(Values.Share.precision())
                        .multiply(BigDecimal.valueOf(price.getValue()), Values.MC)
                        .movePointLeft(Values.Quote.precisionDeltaToMoney()) //
                        .setScale(0, RoundingMode.HALF_UP).longValue();
        return Money.of(security.getCurrencyCode(), marketValue);
    }

    /**
     * Get the realized gains for a specific date.
     */
    public Money getRealizedGains(LocalDate date)
    {
        CapitalGainsRecord record = realizedGainsPerDay.get(date);
        if (record != null)
        {
            return record.getCapitalGains();
        }
        return Money.of(termCurrency, 0L);
    }

    /**
     * Get the unrealized gains for a specific date.
     */
    public Money getUnrealizedGains(LocalDate date)
    {
        CapitalGainsRecord record = unrealizedGainsPerDay.get(date);
        if (record != null)
        {
            return record.getCapitalGains();
        }
        return Money.of(termCurrency, 0L);
    }

    /**
     * Get all dates that have realized gains.
     */
    public List<LocalDate> getDatesWithRealizedGains()
    {
        return new ArrayList<>(realizedGainsPerDay.keySet());
    }

    /**
     * Get all dates that have unrealized gains.
     */
    public List<LocalDate> getDatesWithUnrealizedGains()
    {
        return new ArrayList<>(unrealizedGainsPerDay.keySet());
    }

    /**
     * Get the total realized gains across all dates.
     */
    public Money getTotalRealizedGains()
    {
        long total = realizedGainsPerDay.values().stream()
            .mapToLong(record -> record.getCapitalGains().getAmount())
            .sum();
        return Money.of(termCurrency, total);
    }

    /**
     * Get the total unrealized gains across all dates.
     */
    public Money getTotalUnrealizedGains()
    {
        long total = unrealizedGainsPerDay.values().stream()
            .mapToLong(record -> record.getCapitalGains().getAmount())
            .sum();
        return Money.of(termCurrency, total);
    }

    /**
     * Get the total unrealized gains up to a specific date (inclusive).
     * This sums all daily unrealized gains from the start of the interval up to the given date.
     */
    public Money getTotalUnrealizedGainsUpTo(LocalDate date)
    {
        long total = unrealizedGainsPerDay.entrySet().stream()
            .filter(entry -> !entry.getKey().isAfter(date))
            .mapToLong(entry -> entry.getValue().getCapitalGains().getAmount())
            .sum();
        return Money.of(termCurrency, total);
    }

    /**
     * Get the total realized gains up to a specific date (inclusive).
     * This sums all daily realized gains from the start of the interval up to the given date.
     */
    public Money getTotalRealizedGainsUpTo(LocalDate date)
    {
        long total = realizedGainsPerDay.entrySet().stream()
            .filter(entry -> !entry.getKey().isAfter(date))
            .mapToLong(entry -> entry.getValue().getCapitalGains().getAmount())
            .sum();
        return Money.of(termCurrency, total);
    }

    /**
     * Clear all calculated gains.
     */
    public void clear()
    {
        realizedGainsPerDay.clear();
        unrealizedGainsPerDay.clear();
    }
} 