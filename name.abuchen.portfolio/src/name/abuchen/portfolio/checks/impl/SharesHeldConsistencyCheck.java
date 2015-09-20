package name.abuchen.portfolio.checks.impl;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.checks.Check;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.checks.QuickFix;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;

public class SharesHeldConsistencyCheck implements Check
{

    private static class SharesIssue implements Issue
    {
        private Portfolio portfolio;
        private Security security;
        private long shares;

        public SharesIssue(Portfolio portfolio, Security security, long shares)
        {
            super();
            this.portfolio = portfolio;
            this.security = security;
            this.shares = shares;
        }

        @Override
        public LocalDate getDate()
        {
            return null;
        }

        @Override
        public Object getEntity()
        {
            return portfolio;
        }

        @Override
        public Long getAmount()
        {
            return null;
        }

        @Override
        public String getLabel()
        {
            return MessageFormat.format(Messages.IssueInconsistentSharesHeld, security, portfolio,
                            Values.Share.format(shares));
        }

        @Override
        public List<QuickFix> getAvailableFixes()
        {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Issue> execute(Client client)
    {
        List<Issue> issues = new ArrayList<Issue>();
        List<Security> securities = client.getSecurities();

        for (Portfolio portfolio : client.getPortfolios())
        {
            long[] shares = new long[securities.size()];

            for (PortfolioTransaction t : portfolio.getTransactions())
            {
                int index = securities.indexOf(t.getSecurity());

                // negative index means either the security is not known to the
                // global collection or the security is null -> other checks
                if (index < 0)
                    continue;

                switch (t.getType())
                {
                    case BUY:
                    case TRANSFER_IN:
                    case DELIVERY_INBOUND:
                        shares[index] += t.getShares();
                        break;
                    case SELL:
                    case TRANSFER_OUT:
                    case DELIVERY_OUTBOUND:
                        shares[index] -= t.getShares();
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }

            for (int ii = 0; ii < shares.length; ii++)
            {
                if (shares[ii] < 0)
                {
                    Security security = securities.get(ii);
                    issues.add(new SharesIssue(portfolio, security, shares[ii]));
                }
            }
        }

        return issues;
    }
}
