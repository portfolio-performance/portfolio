package name.abuchen.portfolio.ui.dialogs.transactions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountOnlyTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountTransferTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDividendTransactionCreator;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferTransactionCreator;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.ui.Messages;

final class LedgerDialogCommitSupport
{
    private final Client client;

    LedgerDialogCommitSupport(Client client)
    {
        this.client = client;
    }

    boolean commitAccountTransaction(AccountTransaction sourceTransaction, Account account, AccountTransaction.Type type,
                    LocalDateTime dateTime, long total, String currencyCode, Security security, long shares,
                    LocalDateTime exDate, Money dividendCashForex, BigDecimal exchangeRate,
                    List<Transaction.Unit> units, String note)
    {
        var accountOnlyCreator = new LedgerAccountOnlyTransactionCreator(client);
        var dividendCreator = new LedgerDividendTransactionCreator(client);

        if (exDate != null && isLedgerAccountOnlyType(type)
                        && (sourceTransaction == null || accountOnlyCreator.canUpdate(sourceTransaction)))
            throw new UnsupportedOperationException(Messages.MsgExDateNotAllowed);

        if (sourceTransaction != null && dividendCreator.canUpdate(sourceTransaction))
        {
            dividendCreator.update(sourceTransaction, account, type, dateTime, total, currencyCode, security, shares,
                            exDate, dividendCashForex, dividendCashForex != null ? exchangeRate : null, units, note,
                            sourceTransaction.getSource());
            return true;
        }

        if (sourceTransaction != null && accountOnlyCreator.canUpdate(sourceTransaction))
        {
            accountOnlyCreator.update(sourceTransaction, account, type, dateTime, total, currencyCode, security, units,
                            note, sourceTransaction.getSource());
            return true;
        }

        if (sourceTransaction == null && isLedgerAccountOnlyType(type))
        {
            accountOnlyCreator.create(account, type, dateTime, total, currencyCode, security, units, note, null);
            return true;
        }

        if (sourceTransaction == null && type == AccountTransaction.Type.DIVIDENDS)
        {
            dividendCreator.create(account, dateTime, total, currencyCode, security, shares, exDate, dividendCashForex,
                            dividendCashForex != null ? exchangeRate : null, units, note, null);
            return true;
        }

        return false;
    }

    boolean commitBuySell(BuySellEntry source, Portfolio portfolio, Account account, PortfolioTransaction.Type type,
                    LocalDateTime dateTime, long total, Security security, long shares, List<Transaction.Unit> units,
                    String note)
    {
        var creator = new LedgerBuySellTransactionCreator(client);

        if (source != null && creator.isLedgerBacked(source))
        {
            creator.update(source, portfolio, account, type, dateTime, total, account.getCurrencyCode(), security,
                            shares, units, note, source.getSource());
            return true;
        }

        if (source == null)
        {
            creator.create(portfolio, account, type, dateTime, total, account.getCurrencyCode(), security, shares,
                            units, note, null);
            return true;
        }

        return false;
    }

    boolean commitAccountTransfer(AccountTransferEntry source, Account sourceAccount, Account targetAccount,
                    LocalDateTime dateTime, long sourceAmount, long targetAmount, Money sourceForex,
                    BigDecimal sourceExchangeRate, String note)
    {
        var creator = new LedgerAccountTransferTransactionCreator(client);

        if (source != null && creator.isLedgerBacked(source))
        {
            creator.update(source, sourceAccount, targetAccount, dateTime, sourceAmount, sourceAccount.getCurrencyCode(),
                            targetAmount, targetAccount.getCurrencyCode(), sourceForex, sourceExchangeRate, note,
                            source.getSource());
            return true;
        }

        if (source == null)
        {
            creator.create(sourceAccount, targetAccount, dateTime, sourceAmount, sourceAccount.getCurrencyCode(),
                            targetAmount, targetAccount.getCurrencyCode(), sourceForex, sourceExchangeRate, note, null);
            return true;
        }

        return false;
    }

    boolean commitDelivery(TransactionPair<PortfolioTransaction> source, Portfolio portfolio,
                    PortfolioTransaction.Type type, LocalDateTime dateTime, long total, String currencyCode,
                    Security security, long shares, List<Transaction.Unit> units, String note)
    {
        var creator = new LedgerDeliveryTransactionCreator(client);

        if (source != null && creator.canUpdate(source.getTransaction()))
        {
            creator.update(source.getTransaction(), portfolio, type, dateTime, total, currencyCode, security, shares,
                            null, null, units, note, source.getTransaction().getSource());
            return true;
        }

        if (source == null)
        {
            creator.create(portfolio, type, dateTime, total, currencyCode, security, shares, null, null, units, note,
                            null);
            return true;
        }

        return false;
    }

    boolean commitSecurityTransfer(PortfolioTransferEntry source, Portfolio sourcePortfolio, Portfolio targetPortfolio,
                    Security security, LocalDateTime dateTime, long shares, long amount, String note)
    {
        var creator = new LedgerPortfolioTransferTransactionCreator(client);

        if (source != null && creator.isLedgerBacked(source))
        {
            creator.update(source, sourcePortfolio, targetPortfolio, security, dateTime, shares, amount,
                            security.getCurrencyCode(), note, source.getSource());
            return true;
        }

        if (source == null)
        {
            creator.create(sourcePortfolio, targetPortfolio, security, dateTime, shares, amount,
                            security.getCurrencyCode(), note, null);
            return true;
        }

        return false;
    }

    private boolean isLedgerAccountOnlyType(AccountTransaction.Type type)
    {
        return switch (type)
        {
            case DEPOSIT, REMOVAL, INTEREST, INTEREST_CHARGE, FEES, FEES_REFUND, TAXES, TAX_REFUND -> true;
            case BUY, SELL, TRANSFER_IN, TRANSFER_OUT, DIVIDENDS -> false;
        };
    }
}
