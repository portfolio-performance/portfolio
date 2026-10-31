package name.abuchen.portfolio.snapshot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Calculates the valuation of a client for a given date.
 * <p/>
 * This class incrementally applies the transactions to the positions to
 * determine the current funds (for accounts) the the current shares (for
 * securities). Therefore, the method nextValuation must always be called with a
 * date later the date of the previous call.
 */
/* package */ final class ClientSnapshotIterator
{
    private static class AccountPosition
    {
        private Account account;

        /** all account transactions */
        private List<AccountTransaction> transactions;

        /** the current index into the transaction list */
        private int index;

        /** the current funds after applying the transactions */
        private long funds;

        public AccountPosition(Account account)
        {
            this.account = account;
            this.transactions = new ArrayList<>(account.getTransactions());
            this.transactions.sort(Transaction.BY_DATE);
            this.index = 0;
            this.funds = 0;
        }
    }

    private static class SecurityPosition
    {
        private Security security;

        /** looks up the prices for the increasing dates of the valuation */
        private PriceCursor prices;

        /** the current number of shares after applying the transactions */
        private long shares;

        /** the transactions that resulted in the given shares */
        private List<PortfolioTransaction> transactions;

        public SecurityPosition(Security security)
        {
            this.security = security;
            this.prices = new PriceCursor(security);
            this.transactions = new ArrayList<>();
            this.shares = 0;
        }
    }

    /**
     * Reuses the exchange rate per currency for the same date. The valuation
     * converts every position of a currency on the same day, but the rate
     * (binary search, inverse or chained exchange rate series) only needs to
     * be looked up once per currency and day. The conversion and rounding of
     * each position is unchanged.
     */
    /* package */ static final class SameDayRateConverter implements CurrencyConverter
    {
        private final CurrencyConverter delegate;
        private final Map<String, ExchangeRate> rates = new HashMap<>();
        private LocalDate date;

        /* package */ SameDayRateConverter(CurrencyConverter delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public String getTermCurrency()
        {
            return delegate.getTermCurrency();
        }

        @Override
        public ExchangeRate getRate(LocalDate requestedDate, String currencyCode)
        {
            if (!requestedDate.equals(date))
            {
                rates.clear();
                date = requestedDate;
            }

            ExchangeRate rate = rates.get(currencyCode);
            if (rate == null)
            {
                rate = delegate.getRate(requestedDate, currencyCode);
                rates.put(currencyCode, rate);
            }
            return rate;
        }

        @Override
        public CurrencyConverter with(String currencyCode)
        {
            return delegate.with(currencyCode);
        }
    }

    /**
     * Returns the same price as {@link Security#getSecurityPrice(LocalDate)}
     * but avoids the binary search per day: the valuation requests the prices
     * for increasing dates, therefore the cursor walks forward through the
     * historic prices.
     * <p/>
     * The latest (intraday) price is only relevant for dates on or after the
     * last historic price. These requests are delegated to
     * {@link Security#getSecurityPrice(LocalDate)} which answers them without a
     * search. For dates before the last historic price the latest price never
     * applies.
     * <p/>
     * The price list is read on every call. If prices are added or removed
     * while the cursor is used (e.g. by a running price update), the cursor
     * either walks forward or falls back to a binary search.
     */
    /* package */ static final class PriceCursor
    {
        private final Security security;
        private int index;

        /* package */ PriceCursor(Security security)
        {
            this.security = security;
        }

        /* package */ SecurityPrice getSecurityPrice(LocalDate date)
        {
            List<SecurityPrice> prices = security.getPrices();
            int size = prices.size();

            if (size == 0 || !prices.get(size - 1).getDate().isAfter(date))
                return security.getSecurityPrice(date);

            // reset if the list has shrunk or the date moved backwards
            if (index >= size || (index > 0 && prices.get(index).getDate().isAfter(date)))
                index = indexOf(prices, date);

            while (index + 1 < size && !prices.get(index + 1).getDate().isAfter(date))
                index++;

            // if the date is before the first historic price, the first price
            // is returned (same as Security#getSecurityPrice)
            return prices.get(index);
        }

        private static int indexOf(List<SecurityPrice> prices, LocalDate date)
        {
            int answer = Collections.binarySearch(prices, new SecurityPrice(date, 0));

            if (answer >= 0)
                return answer;
            else if (answer == -1)
                return 0;
            else
                return -answer - 2;
        }
    }

    private final CurrencyConverter converter;

    private final List<AccountPosition> accounts;

    private final Map<Security, SecurityPosition> securities = new HashMap<>();
    private final List<PortfolioTransaction> portfolioTransactions;
    private int portfolioTransactionIndex = 0;

    /* package */ ClientSnapshotIterator(Client client, CurrencyConverter converter)
    {
        this.converter = new SameDayRateConverter(converter);

        this.portfolioTransactions = client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream())
                        .sorted(Transaction.BY_DATE).toList();

        this.accounts = client.getAccounts().stream().map(AccountPosition::new).toList();
    }

    /* package */ long nextValuation(LocalDate date)
    {
        applyAccountTransactions(date);
        applyPortfolioTransactions(date);

        long valuation = 0;

        for (AccountPosition position : accounts)
        {
            if (position.funds == 0)
                continue;

            if (converter.getTermCurrency().equals(position.account.getCurrencyCode()))
            {
                valuation += position.funds;
            }
            else
            {
                valuation += converter.convert(date, Money.of(position.account.getCurrencyCode(), position.funds))
                                .getAmount();
            }
        }

        for (SecurityPosition position : securities.values())
        {
            if (position.shares == 0)
                continue;

            SecurityPrice price = position.prices.getSecurityPrice(date);

            if (price.getValue() == 0L)
            {
                // try to fallback to the price of the last // transaction
                PortfolioTransaction last = position.transactions.get(position.transactions.size() - 1);
                price = new SecurityPrice(last.getDateTime().toLocalDate(),
                                last.getGrossPricePerShare(converter.with(position.security.getCurrencyCode()))
                                                .getAmount());

            }

            long marketValue = BigDecimal.valueOf(position.shares) //
                            .movePointLeft(Values.Share.precision())
                            .multiply(BigDecimal.valueOf(price.getValue()), Values.MC)
                            .movePointLeft(Values.Quote.precisionDeltaToMoney()) //
                            .setScale(0, RoundingMode.HALF_UP).longValue();

            if (converter.getTermCurrency().equals(position.security.getCurrencyCode()))
            {
                valuation += marketValue;
            }
            else
            {
                valuation += converter.convert(date, Money.of(position.security.getCurrencyCode(), marketValue))
                                .getAmount();
            }
        }

        return valuation;
    }

    private void applyPortfolioTransactions(LocalDate date)
    {
        while (portfolioTransactionIndex < portfolioTransactions.size())
        {
            var tx = portfolioTransactions.get(portfolioTransactionIndex);

            if (!tx.getDateTime().toLocalDate().isAfter(date))
            {
                var s = securities.computeIfAbsent(tx.getSecurity(), SecurityPosition::new);
                s.transactions.add(tx);

                if (tx.getType().isPurchase())
                    s.shares += tx.getShares();
                else
                    s.shares -= tx.getShares();

                portfolioTransactionIndex++;
            }
            else
            {
                break;
            }
        }
    }

    private void applyAccountTransactions(LocalDate date)
    {
        for (AccountPosition account : accounts)
        {
            while (account.index < account.transactions.size())
            {
                var tx = account.transactions.get(account.index);

                if (!tx.getDateTime().toLocalDate().isAfter(date))
                {
                    if (tx.getType().isCredit())
                        account.funds += tx.getAmount();
                    else
                        account.funds -= tx.getAmount();

                    account.index++;
                }
                else
                {
                    break;
                }
            }
        }
    }
}
