package name.abuchen.portfolio.datatransfer.pdf;

import static org.junit.Assume.assumeTrue;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;

public class HeadlessPDFImportToolTest
{
    @Test
    public void inferredCashDepositIsDatedBeforeFundedTransaction()
    {
        AccountTransaction transaction = new AccountTransaction(AccountTransaction.Type.BUY);
        transaction.setDateTime(LocalDateTime.of(2020, 6, 25, 13, 37));

        assertEquals(LocalDateTime.of(2020, 6, 25, 13, 36),
                        HeadlessPDFImportTool.inferredCashDepositDateTime(transaction));
    }

    @Test
    public void inferredCashDepositIsWrittenBeforeFundedTransaction()
    {
        Account account = new Account("Test Account"); //$NON-NLS-1$

        AccountTransaction buy = new AccountTransaction(AccountTransaction.Type.BUY);
        buy.setDateTime(LocalDateTime.of(2020, 6, 25, 13, 37));
        buy.setCurrencyCode(account.getCurrencyCode());
        buy.setAmount(100_00);

        AccountTransaction deposit = new AccountTransaction(AccountTransaction.Type.DEPOSIT);
        deposit.setDateTime(LocalDateTime.of(2020, 6, 25, 13, 36));
        deposit.setCurrencyCode(account.getCurrencyCode());
        deposit.setAmount(100_00);
        deposit.setNote("Inferred cash funding to keep imported cash balance non-negative"); //$NON-NLS-1$

        account.addTransaction(buy);
        account.addTransaction(deposit);

        HeadlessPDFImportTool.sortAccountTransactions(account);

        assertEquals(deposit, account.getTransactions().get(0));
        assertEquals(buy, account.getTransactions().get(1));
    }

    @Test
    public void runFromMaven() throws Exception
    {
        String input = propertyOrEnv("portfolio.import.input"); //$NON-NLS-1$
        String manualDelivery = propertyOrEnv("portfolio.import.manualDelivery"); //$NON-NLS-1$
        String manualPortfolioTransfer = propertyOrEnv("portfolio.import.manualPortfolioTransfer"); //$NON-NLS-1$
        String manualShareUpdate = propertyOrEnv("portfolio.import.manualShareUpdate"); //$NON-NLS-1$
        String manualAccountUpdate = propertyOrEnv("portfolio.import.manualAccountUpdate"); //$NON-NLS-1$
        String manualAccountTransaction = propertyOrEnv("portfolio.import.manualAccountTransaction"); //$NON-NLS-1$
        String normalizeOnly = propertyOrEnv("portfolio.import.normalizeOnly"); //$NON-NLS-1$
        String fillZeroTransferValues = propertyOrEnv("portfolio.import.fillZeroTransferValues"); //$NON-NLS-1$
        String reportHoldings = propertyOrEnv("portfolio.import.reportHoldings"); //$NON-NLS-1$
        assumeTrue(
                        "Set -Dportfolio.import.input, -Dportfolio.import.manualDelivery, -Dportfolio.import.manualPortfolioTransfer, -Dportfolio.import.manualShareUpdate, -Dportfolio.import.manualAccountUpdate, -Dportfolio.import.manualAccountTransaction, -Dportfolio.import.normalizeOnly, -Dportfolio.import.fillZeroTransferValues, or -Dportfolio.import.reportHoldings to run the headless importer", //$NON-NLS-1$
                        input != null && !input.isBlank() || manualDelivery != null && !manualDelivery.isBlank()
                                        || manualPortfolioTransfer != null && !manualPortfolioTransfer.isBlank()
                                        || manualShareUpdate != null && !manualShareUpdate.isBlank()
                                        || manualAccountUpdate != null && !manualAccountUpdate.isBlank()
                                        || manualAccountTransaction != null && !manualAccountTransaction.isBlank()
                                        || Boolean.parseBoolean(normalizeOnly)
                                        || Boolean.parseBoolean(fillZeroTransferValues)
                                        || Boolean.parseBoolean(reportHoldings));

        String output = required("portfolio.import.output"); //$NON-NLS-1$

        List<String> args = new ArrayList<>();
        if (input != null && !input.isBlank())
            add(args, "--input", input); //$NON-NLS-1$
        addIfSet(args, "--client", "portfolio.import.client"); //$NON-NLS-1$ //$NON-NLS-2$
        add(args, "--output", output); //$NON-NLS-1$
        addIfSet(args, "--report", "portfolio.import.report"); //$NON-NLS-1$ //$NON-NLS-2$
        addIfSet(args, "--base-currency", "portfolio.import.baseCurrency"); //$NON-NLS-1$ //$NON-NLS-2$
        addIfSet(args, "--account", "portfolio.import.account"); //$NON-NLS-1$ //$NON-NLS-2$
        addIfSet(args, "--portfolio", "portfolio.import.portfolio"); //$NON-NLS-1$ //$NON-NLS-2$
        addIfSet(args, "--secondary-account", "portfolio.import.secondaryAccount"); //$NON-NLS-1$ //$NON-NLS-2$
        addIfSet(args, "--secondary-portfolio", "portfolio.import.secondaryPortfolio"); //$NON-NLS-1$ //$NON-NLS-2$
        addFlagIfSet(args, "--allow-warnings", "portfolio.import.allowWarnings"); //$NON-NLS-1$ //$NON-NLS-2$
        addFlagIfSet(args, "--convert-buy-sell-to-delivery", //$NON-NLS-1$
                        "portfolio.import.convertBuySellToDelivery"); //$NON-NLS-1$
        addFlagIfSet(args, "--remove-dividends", "portfolio.import.removeDividends"); //$NON-NLS-1$ //$NON-NLS-2$
        addFlagIfSet(args, "--infer-cash-deposits", "portfolio.import.inferCashDeposits"); //$NON-NLS-1$ //$NON-NLS-2$
        addFlagIfSet(args, "--normalize-only", "portfolio.import.normalizeOnly"); //$NON-NLS-1$ //$NON-NLS-2$
        addFlagIfSet(args, "--fill-zero-transfer-values", //$NON-NLS-1$
                        "portfolio.import.fillZeroTransferValues"); //$NON-NLS-1$
        addFlagIfSet(args, "--report-holdings", "portfolio.import.reportHoldings"); //$NON-NLS-1$ //$NON-NLS-2$
        addRepeatedIfSet(args, "--manual-share-update", "portfolio.import.manualShareUpdate"); //$NON-NLS-1$ //$NON-NLS-2$
        addRepeatedIfSet(args, "--manual-account-update", "portfolio.import.manualAccountUpdate"); //$NON-NLS-1$ //$NON-NLS-2$
        addRepeatedIfSet(args, "--manual-account-transaction", //$NON-NLS-1$
                        "portfolio.import.manualAccountTransaction"); //$NON-NLS-1$
        addRepeatedIfSet(args, "--manual-delivery", "portfolio.import.manualDelivery"); //$NON-NLS-1$ //$NON-NLS-2$
        addRepeatedIfSet(args, "--manual-portfolio-transfer", //$NON-NLS-1$
                        "portfolio.import.manualPortfolioTransfer"); //$NON-NLS-1$

        HeadlessPDFImportTool.main(args.toArray(String[]::new));
    }

    private static String required(String property)
    {
        String value = propertyOrEnv(property);
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Missing required property: " + property); //$NON-NLS-1$
        return value;
    }

    private static String propertyOrEnv(String property)
    {
        String value = System.getProperty(property);
        if (value != null && !value.isBlank())
            return value;

        return System.getenv(property.toUpperCase().replace('.', '_'));
    }

    private static void add(List<String> args, String option, String value)
    {
        args.add(option);
        args.add(value);
    }

    private static void addIfSet(List<String> args, String option, String property)
    {
        String value = propertyOrEnv(property);
        if (value != null && !value.isBlank())
            add(args, option, value);
    }

    private static void addFlagIfSet(List<String> args, String option, String property)
    {
        String value = propertyOrEnv(property);
        if (Boolean.parseBoolean(value))
            args.add(option);
    }

    private static void addRepeatedIfSet(List<String> args, String option, String property)
    {
        String value = propertyOrEnv(property);
        if (value == null || value.isBlank())
            return;

        for (String item : value.split("\\R")) //$NON-NLS-1$
        {
            if (!item.isBlank())
                add(args, option, item.trim());
        }
    }
}
