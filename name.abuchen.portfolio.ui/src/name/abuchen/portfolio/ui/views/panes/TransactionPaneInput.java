package name.abuchen.portfolio.ui.views.panes;

import static name.abuchen.portfolio.util.CollectorsUtil.toMutableList;

import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.ui.selection.SecuritySelection;

/* package */ class TransactionPaneInput
{
    private final Object source;
    private final List<TransactionPair<?>> transactions;

    private TransactionPaneInput(Object source, List<TransactionPair<?>> transactions)
    {
        this.source = source;
        this.transactions = transactions;
    }

    public Object getSource()
    {
        return source;
    }

    public List<TransactionPair<?>> getTransactions()
    {
        return transactions;
    }

    public String getExportLabel()
    {
        return exportLabelFor(source);
    }

    public static String exportLabelFor(Object source)
    {
        if (source instanceof SecuritySelection sel)
            return sel.size() == 1 ? sel.getSecurity().getName() : sel.size() + " Securities";
        if (source instanceof Security sec)
            return sec.getName();
        if (source != null)
            return source.toString();
        return "";
    }

    public static TransactionPaneInput resolve(Object input, Client client)
    {
        var investmentPlan = Adaptor.adapt(InvestmentPlan.class, input);
        if (investmentPlan != null)
            return new TransactionPaneInput(investmentPlan, investmentPlan.getTransactions(client));

        var securitySelection = Adaptor.adapt(SecuritySelection.class, input);
        if (securitySelection != null && !securitySelection.isEmpty())
        {
            var allTransactions = securitySelection.getSecurities().stream()
                            .flatMap(s -> s.getTransactions(client).stream())
                            .collect(toMutableList());
            return new TransactionPaneInput(securitySelection, allTransactions);
        }

        var security = Adaptor.adapt(Security.class, input);
        if (security != null)
            return new TransactionPaneInput(security, security.getTransactions(client));

        var account = Adaptor.adapt(Account.class, input);
        if (account != null)
        {
            List<TransactionPair<?>> txList = account.getTransactions().stream()
                            .<TransactionPair<?>>map(t -> new TransactionPair<>(account, t))
                            .collect(toMutableList());
            return new TransactionPaneInput(account, txList);
        }

        var portfolio = Adaptor.adapt(Portfolio.class, input);
        if (portfolio != null)
        {
            List<TransactionPair<?>> txList = portfolio.getTransactions().stream()
                            .<TransactionPair<?>>map(t -> new TransactionPair<>(portfolio, t))
                            .collect(toMutableList());
            return new TransactionPaneInput(portfolio, txList);
        }

        return new TransactionPaneInput(null, Collections.emptyList());
    }
}
