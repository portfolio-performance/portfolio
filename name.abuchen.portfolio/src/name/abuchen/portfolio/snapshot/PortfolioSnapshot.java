package name.abuchen.portfolio.snapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.snapshot.filter.ReadOnlyPortfolio;

public class PortfolioSnapshot
{
    // //////////////////////////////////////////////////////////////
    // factory methods
    // //////////////////////////////////////////////////////////////

    public static PortfolioSnapshot create(Portfolio portfolio, CurrencyConverter converter, LocalDate date)
    {
        List<SecurityPosition> positions = portfolio.getTransactions() //
                        .stream() //
                        .filter(t -> !t.getDateTime().toLocalDate().isAfter(date)) //
                        .collect(Collectors.groupingBy(PortfolioTransaction::getSecurity)) //
                        .entrySet() //
                        .stream() //
                        .map(e -> {
                            SecurityPrice price = e.getKey().getSecurityPrice(date);

                            if (price.getValue() == 0L)
                            {
                                // try to fallback to the price of the last
                                // transaction
                                List<PortfolioTransaction> tx = e.getValue();

                                Optional<PortfolioTransaction> last = tx.stream()
                                                .sorted(Transaction.BY_DATE.reversed()).findFirst();

                                if (last.isPresent())
                                {
                                    PortfolioTransaction t = last.get();
                                    price = new SecurityPrice(t.getDateTime().toLocalDate(),
                                                    t.getGrossPricePerShare(
                                                                    converter.with(e.getKey().getCurrencyCode()))
                                                                    .getAmount());
                                }
                            }
                            return new SecurityPosition(e.getKey(), converter, price, e.getValue()); //
                        }) //
                        .filter(p -> p.getShares() != 0) //
                        .collect(Collectors.toList());

        return new PortfolioSnapshot(portfolio, converter, date, positions);
    }

    public static PortfolioSnapshot merge(List<PortfolioSnapshot> snapshots, CurrencyConverter converter)
    {
        if (snapshots.isEmpty())
            throw new IllegalArgumentException("Error: PortfolioSnapshots to be merged must not be empty"); //$NON-NLS-1$

        Portfolio portfolio = new Portfolio()
        {
            @Override
            public void shallowDeleteTransaction(PortfolioTransaction transaction, Client client)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void deleteTransaction(PortfolioTransaction transaction, Client client)
            {
                throw new UnsupportedOperationException();
            }
        };
        portfolio.setName(Messages.LabelJointPortfolio);
        Account referenceAccount = new Account(Messages.LabelJointPortfolio);
        referenceAccount.setCurrencyCode(converter.getTermCurrency());
        portfolio.setReferenceAccount(referenceAccount);

        snapshots.forEach(s -> portfolio.addAllTransaction(s.getPortfolio().getTransactions()));

        return create(portfolio, snapshots.get(0).getCurrencyConverter(), snapshots.get(0).getTime());
    }

    // //////////////////////////////////////////////////////////////
    // instance impl
    // //////////////////////////////////////////////////////////////

    private final Portfolio portfolio;
    private final CurrencyConverter converter;
    private final LocalDate date;
    private final List<SecurityPosition> positions;

    private PortfolioSnapshot(Portfolio source, CurrencyConverter converter, LocalDate date,
                    List<SecurityPosition> positions)
    {
        this.portfolio = source;
        this.converter = converter;
        this.date = date;
        this.positions = positions;
    }

    public Portfolio getPortfolio()
    {
        return portfolio;
    }

    /**
     * Returns the underlying portfolio which was the basis for the data even if
     * the portfolio was filtered.
     */
    public Portfolio unwrapPortfolio()
    {
        return portfolio instanceof ReadOnlyPortfolio readOnly ? readOnly.unwrap() : portfolio;
    }

    public CurrencyConverter getCurrencyConverter()
    {
        return converter;
    }

    public LocalDate getTime()
    {
        return date;
    }

    public List<SecurityPosition> getPositions()
    {
        return positions;
    }

    public Map<Security, SecurityPosition> getPositionsBySecurity()
    {
        return positions.stream().collect(Collectors.toMap(SecurityPosition::getSecurity, p -> p));
    }

    public Money getValue()
    {
        return positions.stream() //
                        .map(SecurityPosition::calculateValue) //
                        .map(money -> money.with(converter.at(date))) //
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));
    }

    public GroupByTaxonomy groupByTaxonomy(Taxonomy taxonomy)
    {
        return new GroupByTaxonomy(taxonomy, this);
    }
}
