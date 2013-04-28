package name.abuchen.portfolio.snapshot;

import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;

public class AccountIndex extends PerformanceIndex
{
    public static AccountIndex forPeriod(Client client, Account account, ReportingPeriod reportInterval,
                    List<Exception> warnings)
    {
        AccountIndex index = new AccountIndex(client, account, reportInterval);
        index.calculate(warnings);
        return index;
    }

    private Account account;

    private AccountIndex(Client client, Account account, ReportingPeriod reportInterval)
    {
        super(client, reportInterval);
        this.account = account;
    }

    private void calculate(List<Exception> warnings)
    {
        Client pseudoClient = new Client();

        Account pseudoAccount = new Account();
        pseudoAccount.setName(""); //$NON-NLS-1$
        pseudoClient.addAccount(pseudoAccount);

        Portfolio pseudoPortfolio = new Portfolio();
        pseudoPortfolio.setReferenceAccount(pseudoAccount);
        pseudoClient.addPortfolio(pseudoPortfolio);

        for (AccountTransaction t : account.getTransactions())
        {
            switch (t.getType())
            {
                case BUY:
                case TRANSFER_IN:
                case DIVIDENDS:
                    pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getSecurity(),
                                    AccountTransaction.Type.DEPOSIT, t.getAmount()));
                    break;
                case SELL:
                case TRANSFER_OUT:
                    pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getSecurity(),
                                    AccountTransaction.Type.REMOVAL, t.getAmount()));
                    break;
                case DEPOSIT:
                case REMOVAL:
                case INTEREST:
                case TAXES:
                case FEES:
                    pseudoAccount.addTransaction(t);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        ClientIndex clientIndex = ClientIndex.forPeriod(pseudoClient, getReportInterval(), warnings);

        dates = clientIndex.getDates();
        totals = clientIndex.getTotals();
        accumulated = clientIndex.getAccumulatedPercentage();
        delta = clientIndex.getDeltaPercentage();
        transferals = clientIndex.getTransferals();
    }

}
