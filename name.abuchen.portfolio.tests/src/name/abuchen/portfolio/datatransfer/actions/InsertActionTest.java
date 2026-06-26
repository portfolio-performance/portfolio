package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.SaveFlag;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class InsertActionTest
{
    private Client client;
    private BuySellEntry entry;

    private final LocalDateTime transactionDate = LocalDateTime.now().withSecond(0).withNano(0);
    private final LocalDateTime exDate = transactionDate.minusDays(7);

    @Before
    public void prepare()
    {
        client = new Client();
        Security security = new Security("Security", CurrencyUnit.EUR);
        security.setUpdatedAt(Instant.now());
        client.addSecurity(security);
        Portfolio portfolio = new Portfolio("Portfolio");
        portfolio.setUpdatedAt(Instant.now());
        client.addPortfolio(portfolio);
        Account account = new Account("Account");
        account.setUpdatedAt(Instant.now());
        client.addAccount(account);

        entry = buySell(Type.BUY);
    }

    @Test
    public void testInsertOfBuySellEntryCreatesLedgerTruth()
    {
        Account account = client.getAccounts().get(0);
        Portfolio portfolio = client.getPortfolios().get(0);

        InsertAction action = new InsertAction(client);
        action.process(entry, account, portfolio);

        assertThat(account.getTransactions().size(), is(1));
        assertThat(portfolio.getTransactions().size(), is(1));
        assertThat(client.getLedger().getEntries().size(), is(1));

        AccountTransaction accountTransaction = account.getTransactions().get(0);
        PortfolioTransaction t = portfolio.getTransactions().get(0);
        assertThat(accountTransaction, instanceOf(LedgerBackedTransaction.class));
        assertThat(t, instanceOf(LedgerBackedTransaction.class));
        assertThat(t.getType(), is(Type.BUY));
        assertTransaction(t);
        assertThat(client.getAllTransactions().size(), is(1));
        assertValid(client);
    }

    @Test
    public void testConversionOfBuySellEntryCreatesLedgerDeliveryTruth()
    {
        Account account = client.getAccounts().get(0);
        Portfolio portfolio = client.getPortfolios().get(0);

        InsertAction action = new InsertAction(client);
        action.setConvertBuySellToDelivery(true);
        action.process(entry, account, portfolio);

        assertThat(account.getTransactions().isEmpty(), is(true));
        assertThat(portfolio.getTransactions().size(), is(1));
        assertThat(client.getLedger().getEntries().size(), is(1));

        PortfolioTransaction delivery = portfolio.getTransactions().get(0);
        assertThat(delivery, instanceOf(LedgerBackedTransaction.class));
        assertThat(delivery.getType(), is(Type.DELIVERY_INBOUND));
        assertTransaction(delivery);
        assertValid(client);
    }

    @Test
    public void testAccountOnlyImportsCreateLedgerTruthForSupportedFamilies()
    {
        Account account = client.getAccounts().get(0);
        Security security = client.getSecurities().get(0);
        var types = List.of(AccountTransaction.Type.DEPOSIT, AccountTransaction.Type.REMOVAL,
                        AccountTransaction.Type.INTEREST, AccountTransaction.Type.INTEREST_CHARGE,
                        AccountTransaction.Type.FEES, AccountTransaction.Type.FEES_REFUND,
                        AccountTransaction.Type.TAXES, AccountTransaction.Type.TAX_REFUND);

        InsertAction action = new InsertAction(client);
        for (AccountTransaction.Type type : types)
        {
            AccountTransaction transaction = new AccountTransaction(type);
            transaction.setDateTime(transactionDate);
            transaction.setAmount(Values.Amount.factorize(10));
            transaction.setCurrencyCode(CurrencyUnit.EUR);
            transaction.setSecurity(security);
            transaction.setNote("note " + type.name());
            transaction.setSource("source " + type.name());
            transaction.addUnit(new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1)),
                            Money.of(CurrencyUnit.USD, Values.Amount.factorize(2)), BigDecimal.valueOf(0.5)));

            action.process(transaction, account);
        }

        assertThat(client.getLedger().getEntries().size(), is(types.size()));
        assertThat(account.getTransactions().size(), is(types.size()));
        assertThat(client.getAllTransactions().size(), is(types.size()));
        assertTrue(account.getTransactions().stream().allMatch(LedgerBackedTransaction.class::isInstance));
        assertTrue(client.getLedger().getEntries().stream().anyMatch(entry -> entry.getPostings().stream()
                        .anyMatch(posting -> posting.getType() == LedgerPostingType.FEE
                                        && posting.getForexAmount() == Values.Amount.factorize(2)
                                        && CurrencyUnit.USD.equals(posting.getForexCurrency()))));
        assertValid(client);
    }

    @Test
    public void testDividendImportCreatesLedgerTruthAndPreservesFacts()
    {
        Account account = client.getAccounts().get(0);
        Security security = client.getSecurities().get(0);
        AccountTransaction dividend = dividend();

        new InsertAction(client).process(dividend, account);

        AccountTransaction projection = account.getTransactions().get(0);
        LedgerPosting cashPosting = client.getLedger().getEntries().get(0).getPostings().get(0);

        assertThat(projection, instanceOf(LedgerBackedTransaction.class));
        assertThat(client.getLedger().getEntries().get(0).getType(), is(LedgerEntryType.DIVIDENDS));
        assertThat(projection.getExDate(), is(exDate));
        assertThat(projection.getShares(), is(Values.Share.factorize(3)));
        assertSame(security, projection.getSecurity());
        assertThat(projection.getUnits().count(), is(2L));
        assertThat(exDate(cashPosting), is(exDate));
        assertValid(client);
    }

    @Test
    public void testDeliveryImportsCreateLedgerTruth()
    {
        Portfolio portfolio = client.getPortfolios().get(0);

        new InsertAction(client).process(delivery(Type.DELIVERY_INBOUND), portfolio);
        new InsertAction(client).process(delivery(Type.DELIVERY_OUTBOUND), portfolio);

        assertThat(client.getLedger().getEntries().size(), is(2));
        assertThat(client.getLedger().getEntries().get(0).getType(), is(LedgerEntryType.DELIVERY_INBOUND));
        assertThat(client.getLedger().getEntries().get(1).getType(), is(LedgerEntryType.DELIVERY_OUTBOUND));
        assertThat(portfolio.getTransactions().size(), is(2));
        assertTrue(portfolio.getTransactions().stream().allMatch(LedgerBackedTransaction.class::isInstance));
        assertThat(client.getAllTransactions().size(), is(2));
        assertValid(client);
    }

    @Test
    public void testAccountTransferImportCreatesOneLedgerEntryWithTwoProjections()
    {
        Account source = client.getAccounts().get(0);
        Account target = new Account("USD Account");
        target.setCurrencyCode(CurrencyUnit.USD);
        client.addAccount(target);
        AccountTransferEntry transfer = new AccountTransferEntry();
        transfer.setDate(transactionDate);
        transfer.getSourceTransaction().setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100)));
        transfer.getTargetTransaction().setMonetaryAmount(Money.of(CurrencyUnit.USD, Values.Amount.factorize(200)));
        transfer.getSourceTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE,
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100)),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(200)), BigDecimal.valueOf(0.5)));
        transfer.setNote("transfer note");
        transfer.setSource("transfer source");

        new InsertAction(client).process(transfer, source, target);

        AccountTransaction sourceProjection = source.getTransactions().get(0);
        AccountTransaction targetProjection = target.getTransactions().get(0);

        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(client.getLedger().getEntries().get(0).getType(), is(LedgerEntryType.CASH_TRANSFER));
        assertThat(sourceProjection, instanceOf(LedgerBackedTransaction.class));
        assertThat(targetProjection, instanceOf(LedgerBackedTransaction.class));
        assertThat(sourceProjection.getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(targetProjection.getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertThat(sourceProjection.getCrossEntry().getCrossTransaction(sourceProjection), is(targetProjection));
        assertThat(client.getAllTransactions().size(), is(1));
        assertValid(client);
    }

    @Test
    public void testPortfolioTransferImportCreatesOneLedgerEntryWithTwoProjections()
    {
        Portfolio source = client.getPortfolios().get(0);
        Portfolio target = new Portfolio("Target Portfolio");
        client.addPortfolio(target);
        PortfolioTransferEntry transfer = portfolioTransfer();

        new InsertAction(client).process(transfer, source, target);

        PortfolioTransaction sourceProjection = source.getTransactions().get(0);
        PortfolioTransaction targetProjection = target.getTransactions().get(0);

        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(client.getLedger().getEntries().get(0).getType(), is(LedgerEntryType.SECURITY_TRANSFER));
        assertThat(sourceProjection, instanceOf(LedgerBackedTransaction.class));
        assertThat(targetProjection, instanceOf(LedgerBackedTransaction.class));
        assertThat(sourceProjection.getType(), is(Type.TRANSFER_OUT));
        assertThat(targetProjection.getType(), is(Type.TRANSFER_IN));
        assertThat(sourceProjection.getCrossEntry().getCrossTransaction(sourceProjection), is(targetProjection));
        assertThat(client.getAllTransactions().size(), is(1));
        assertValid(client);
    }

    @Test
    public void testBuyAndSellImportsCreateLedgerTruth()
    {
        Account account = client.getAccounts().get(0);
        Portfolio portfolio = client.getPortfolios().get(0);
        BuySellEntry buy = buySell(Type.BUY);
        BuySellEntry sell = buySell(Type.SELL);

        InsertAction action = new InsertAction(client);
        action.process(buy, account, portfolio);
        action.process(sell, account, portfolio);

        assertThat(client.getLedger().getEntries().size(), is(2));
        assertThat(client.getLedger().getEntries().get(0).getType(), is(LedgerEntryType.BUY));
        assertThat(client.getLedger().getEntries().get(1).getType(), is(LedgerEntryType.SELL));
        assertThat(account.getTransactions().size(), is(2));
        assertThat(portfolio.getTransactions().size(), is(2));
        assertTrue(account.getTransactions().stream().allMatch(LedgerBackedTransaction.class::isInstance));
        assertTrue(portfolio.getTransactions().stream().allMatch(LedgerBackedTransaction.class::isInstance));
        assertThat(client.getAllTransactions().size(), is(2));
        assertValid(client);
    }

    @Test
    public void testDuplicateDetectionStillFindsImportedLedgerBackedProjection()
    {
        Account account = client.getAccounts().get(0);
        Portfolio portfolio = client.getPortfolios().get(0);
        InsertAction insert = new InsertAction(client);
        DetectDuplicatesAction duplicates = new DetectDuplicatesAction(client);

        insert.process(entry, account, portfolio);

        Status status = duplicates.process(buySell(Type.BUY), account, portfolio);

        assertThat(status.getCode(), is(Status.Code.WARNING));
        assertThat(status.getMessage(), is(Messages.LabelPotentialDuplicate));
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(client.getAllTransactions().size(), is(1));
    }

    @Test
    public void testInvestmentPlanGeneratedLedgerBuyIsUpdatedByImportedBuyInsteadOfDuplicated() throws Exception
    {
        Account account = client.getAccounts().get(0);
        Portfolio portfolio = client.getPortfolios().get(0);
        Security security = client.getSecurities().get(0);
        LocalDate planStart = transactionDate.toLocalDate().minusMonths(1);
        security.addPrice(new SecurityPrice(planStart, Values.Quote.factorize(10)));

        InvestmentPlan plan = new InvestmentPlan("Buy Plan");
        plan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
        plan.setAccount(account);
        plan.setPortfolio(portfolio);
        plan.setSecurity(security);
        plan.setAmount(Values.Amount.factorize(100));
        plan.setFees(Values.Amount.factorize(1));
        plan.setTaxes(Values.Amount.factorize(2));
        plan.setStart(planStart);
        plan.setInterval(12);
        client.addPlan(plan);

        PortfolioTransaction generated = (PortfolioTransaction) plan
                        .generateTransactions(client, new TestCurrencyConverter()).get(0).getTransaction();
        var ledgerBacked = (LedgerBackedTransaction) generated;
        String ledgerEntryUUID = ledgerBacked.getLedgerEntry().getUUID();
        String projectionUUID = ledgerBacked.getLedgerProjectionRef().getUUID();
        long importedShares = Math.round(generated.getShares() * 1.05d);

        BuySellEntry imported = buySell(Type.BUY);
        imported.setDate(generated.getDateTime());
        imported.setMonetaryAmount(generated.getMonetaryAmount());
        imported.setShares(importedShares);
        imported.setSecurity(security);
        imported.setNote("imported note");
        imported.setSource("imported source");
        imported.getPortfolioTransaction().clearUnits();
        imported.getPortfolioTransaction()
                        .addUnit(new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3))));
        imported.getPortfolioTransaction()
                        .addUnit(new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4))));

        Status duplicateStatus = new DetectDuplicatesAction(client).process(imported, account, portfolio);
        assertThat(duplicateStatus.getCode(), is(Status.Code.WARNING));
        assertThat(duplicateStatus.getMessage(), is(Messages.InvestmentPlanItemImportToolTip));

        InsertAction insert = new InsertAction(client);
        insert.setInvestmentPlanItem(true);
        insert.process(imported, account, portfolio);

        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(account.getTransactions().size(), is(1));
        assertThat(portfolio.getTransactions().size(), is(1));
        assertThat(client.getAllTransactions().size(), is(1));

        PortfolioTransaction updated = portfolio.getTransactions().get(0);
        AccountTransaction updatedAccountSide = (AccountTransaction) updated.getCrossEntry()
                        .getCrossTransaction(updated);
        var updatedLedgerBacked = (LedgerBackedTransaction) updated;

        assertThat(updatedLedgerBacked.getLedgerEntry().getUUID(), is(ledgerEntryUUID));
        assertThat(updated.getUUID(), is(projectionUUID));
        assertThat(updated.getShares(), is(importedShares));
        assertThat(updated.getNote(), is("imported note"));
        assertThat(updated.getSource(), is("imported source"));
        assertThat(updatedAccountSide.getNote(), is("imported note"));
        assertThat(updatedAccountSide.getSource(), is("imported source"));
        assertThat(updated.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3))));
        assertThat(updated.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4))));

        assertThat(plan.getLedgerExecutionRefs().size(), is(1));
        assertThat(plan.getLedgerExecutionRefs().get(0).getLedgerEntryUUID(), is(ledgerEntryUUID));
        assertThat(plan.getLedgerExecutionRefs().get(0).getProjectionUUID(), is(projectionUUID));
        assertThat(plan.getTransactions(client).get(0).getTransaction(), is(updated));

        Client xml = loadXml(saveXml(client));
        Client protobuf = loadProtobuf(saveProtobuf(client));
        assertUpdatedPlanImportRoundtrip(xml, importedShares);
        assertUpdatedPlanImportRoundtrip(protobuf, importedShares);
        assertValid(client);
    }

    @Test
    public void testInvestmentPlanGeneratedLedgerBuyRejectsImportedSellWithDiagnostic() throws Exception
    {
        Account account = client.getAccounts().get(0);
        Portfolio portfolio = client.getPortfolios().get(0);
        Security security = client.getSecurities().get(0);
        LocalDate planStart = transactionDate.toLocalDate().minusMonths(1);
        security.addPrice(new SecurityPrice(planStart, Values.Quote.factorize(10)));

        InvestmentPlan plan = new InvestmentPlan("Buy Plan");
        plan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
        plan.setAccount(account);
        plan.setPortfolio(portfolio);
        plan.setSecurity(security);
        plan.setAmount(Values.Amount.factorize(100));
        plan.setStart(planStart);
        plan.setInterval(12);
        client.addPlan(plan);

        PortfolioTransaction generated = (PortfolioTransaction) plan
                        .generateTransactions(client, new TestCurrencyConverter()).get(0).getTransaction();
        BuySellEntry imported = buySell(Type.SELL);
        imported.setDate(generated.getDateTime());
        imported.setSecurity(security);

        InsertAction insert = new InsertAction(client);
        var update = InsertAction.class.getDeclaredMethod("updateLedgerBackedInvestmentPlanTransaction",
                        Transaction.class, BuySellEntry.class);
        update.setAccessible(true);

        var exception = assertThrows(InvocationTargetException.class, () -> update.invoke(insert, generated, imported));

        assertThat(exception.getCause().getMessage(), is(LedgerDiagnosticCode.LEDGER_UI_006.message(
                        Messages.LedgerInsertActionGeneratedBuyTypeMismatch)));
    }

    @Test
    public void testNonMatchingInvestmentPlanImportStillCreatesNewLedgerBuy() throws Exception
    {
        Account account = client.getAccounts().get(0);
        Portfolio portfolio = client.getPortfolios().get(0);
        Security security = client.getSecurities().get(0);
        LocalDate planStart = transactionDate.toLocalDate().minusMonths(1);
        security.addPrice(new SecurityPrice(planStart, Values.Quote.factorize(10)));

        InvestmentPlan plan = new InvestmentPlan("Buy Plan");
        plan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
        plan.setAccount(account);
        plan.setPortfolio(portfolio);
        plan.setSecurity(security);
        plan.setAmount(Values.Amount.factorize(100));
        plan.setStart(planStart);
        plan.setInterval(12);
        client.addPlan(plan);
        plan.generateTransactions(client, new TestCurrencyConverter());

        Security otherSecurity = new Security("Other Security", CurrencyUnit.EUR);
        client.addSecurity(otherSecurity);
        BuySellEntry imported = buySell(Type.BUY);
        imported.setDate(transactionDate);
        imported.setSecurity(otherSecurity);

        Status duplicateStatus = new DetectDuplicatesAction(client).process(imported, account, portfolio);
        assertThat(duplicateStatus.getCode(), is(Status.Code.OK));

        new InsertAction(client).process(imported, account, portfolio);

        assertThat(client.getLedger().getEntries().size(), is(2));
        assertThat(account.getTransactions().size(), is(2));
        assertThat(portfolio.getTransactions().size(), is(2));
        assertThat(client.getAllTransactions().size(), is(2));
        assertThat(plan.getLedgerExecutionRefs().size(), is(1));
        assertValid(client);
    }

    @Test
    public void testAmbiguousInvestmentPlanGeneratedLedgerBuyIsNotUpdatedByImportedBuy() throws Exception
    {
        Account account = client.getAccounts().get(0);
        Portfolio portfolio = client.getPortfolios().get(0);
        Security security = client.getSecurities().get(0);
        LocalDate planStart = transactionDate.toLocalDate().minusMonths(1);
        security.addPrice(new SecurityPrice(planStart, Values.Quote.factorize(10)));

        InvestmentPlan firstPlan = buyPlan("First Buy Plan", account, portfolio, security, planStart);
        InvestmentPlan secondPlan = buyPlan("Second Buy Plan", account, portfolio, security, planStart);
        client.addPlan(firstPlan);
        client.addPlan(secondPlan);

        PortfolioTransaction firstGenerated = (PortfolioTransaction) firstPlan
                        .generateTransactions(client, new TestCurrencyConverter()).get(0).getTransaction();
        PortfolioTransaction secondGenerated = (PortfolioTransaction) secondPlan
                        .generateTransactions(client, new TestCurrencyConverter()).get(0).getTransaction();
        long firstShares = firstGenerated.getShares();
        long secondShares = secondGenerated.getShares();
        String firstNote = firstGenerated.getNote();
        String secondNote = secondGenerated.getNote();

        BuySellEntry imported = buySell(Type.BUY);
        imported.setDate(firstGenerated.getDateTime());
        imported.setMonetaryAmount(firstGenerated.getMonetaryAmount());
        imported.setShares(Math.round(firstGenerated.getShares() * 1.05d));
        imported.setSecurity(security);
        imported.setNote("ambiguous imported note");
        imported.setSource("ambiguous imported source");

        Status duplicateStatus = new DetectDuplicatesAction(client).process(imported, account, portfolio);
        assertThat(duplicateStatus.getCode(), is(Status.Code.WARNING));
        assertThat(duplicateStatus.getMessage(), is(Messages.LabelPotentialDuplicate));

        InsertAction insert = new InsertAction(client);
        insert.setInvestmentPlanItem(true);
        Status insertStatus = insert.process(imported, account, portfolio);

        assertThat(insertStatus.getCode(), is(Status.Code.WARNING));
        assertThat(insertStatus.getMessage(), is(Messages.LabelPotentialDuplicate));
        assertThat(client.getLedger().getEntries().size(), is(2));
        assertThat(account.getTransactions().size(), is(2));
        assertThat(portfolio.getTransactions().size(), is(2));
        assertThat(client.getAllTransactions().size(), is(2));
        assertThat(firstGenerated.getShares(), is(firstShares));
        assertThat(secondGenerated.getShares(), is(secondShares));
        assertThat(firstGenerated.getNote(), is(firstNote));
        assertThat(secondGenerated.getNote(), is(secondNote));
        assertThat(firstPlan.getLedgerExecutionRefs().size(), is(1));
        assertThat(secondPlan.getLedgerExecutionRefs().size(), is(1));
        assertValid(client);
    }

    @Test
    public void testPortfolioTransferDuplicatePipelineSkipsSecondLedgerInsertion()
    {
        Portfolio source = client.getPortfolios().get(0);
        Portfolio target = new Portfolio("Target Portfolio");
        client.addPortfolio(target);

        PortfolioTransferEntry first = portfolioTransfer();
        Status firstStatus = importIfNotDuplicate(first, source, target);

        PortfolioTransaction sourceProjection = source.getTransactions().get(0);
        PortfolioTransaction targetProjection = target.getTransactions().get(0);

        assertThat(firstStatus.getCode(), is(Status.Code.OK));
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(source.getTransactions().size(), is(1));
        assertThat(target.getTransactions().size(), is(1));
        assertThat(sourceProjection.getType(), is(Type.TRANSFER_OUT));
        assertThat(targetProjection.getType(), is(Type.TRANSFER_IN));

        PortfolioTransferEntry duplicate = portfolioTransfer();
        Status duplicateStatus = importIfNotDuplicate(duplicate, source, target);

        assertThat(duplicateStatus.getCode(), is(Status.Code.WARNING));
        assertThat(duplicateStatus.getMessage(), is(Messages.LabelPotentialDuplicate));
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(source.getTransactions().size(), is(1));
        assertThat(target.getTransactions().size(), is(1));
        assertSame(sourceProjection, source.getTransactions().get(0));
        assertSame(targetProjection, target.getTransactions().get(0));
        assertThat(source.getTransactions().get(0).getType(), is(Type.TRANSFER_OUT));
        assertThat(target.getTransactions().get(0).getType(), is(Type.TRANSFER_IN));
        assertThat(client.getAllTransactions().size(), is(1));
        assertValid(client);
    }

    @Test
    public void testAccountOnlyDuplicatePipelineSkipsSecondLedgerInsertion()
    {
        Account account = client.getAccounts().get(0);

        Status firstStatus = importIfNotDuplicate(accountOnly(AccountTransaction.Type.DEPOSIT), account);
        Status duplicateStatus = importIfNotDuplicate(accountOnly(AccountTransaction.Type.DEPOSIT), account);

        assertThat(firstStatus.getCode(), is(Status.Code.OK));
        assertThat(duplicateStatus.getCode(), is(Status.Code.WARNING));
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(account.getTransactions().size(), is(1));
        assertThat(client.getAllTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertValid(client);
    }

    @Test
    public void testDividendDuplicatePipelineSkipsSecondLedgerInsertion()
    {
        Account account = client.getAccounts().get(0);

        Status firstStatus = importIfNotDuplicate(dividend(), account);
        Status duplicateStatus = importIfNotDuplicate(dividend(), account);

        assertThat(firstStatus.getCode(), is(Status.Code.OK));
        assertThat(duplicateStatus.getCode(), is(Status.Code.WARNING));
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(account.getTransactions().size(), is(1));
        assertThat(client.getAllTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertThat(account.getTransactions().get(0).getExDate(), is(exDate));
        assertValid(client);
    }

    @Test
    public void testDeliveryDuplicatePipelineSkipsSecondLedgerInsertion()
    {
        Portfolio portfolio = client.getPortfolios().get(0);

        Status firstStatus = importIfNotDuplicate(delivery(Type.DELIVERY_INBOUND), portfolio);
        Status duplicateStatus = importIfNotDuplicate(delivery(Type.DELIVERY_INBOUND), portfolio);

        assertThat(firstStatus.getCode(), is(Status.Code.OK));
        assertThat(duplicateStatus.getCode(), is(Status.Code.WARNING));
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(portfolio.getTransactions().size(), is(1));
        assertThat(client.getAllTransactions().size(), is(1));
        assertThat(portfolio.getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertThat(portfolio.getTransactions().get(0).getType(), is(Type.DELIVERY_INBOUND));
        assertValid(client);
    }

    @Test
    public void testAccountTransferDuplicatePipelineSkipsSecondLedgerInsertion()
    {
        Account source = client.getAccounts().get(0);
        Account target = new Account("USD Account");
        target.setCurrencyCode(CurrencyUnit.USD);
        client.addAccount(target);

        Status firstStatus = importIfNotDuplicate(accountTransfer(), source, target);
        Status duplicateStatus = importIfNotDuplicate(accountTransfer(), source, target);

        assertThat(firstStatus.getCode(), is(Status.Code.OK));
        assertThat(duplicateStatus.getCode(), is(Status.Code.WARNING));
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(source.getTransactions().size(), is(1));
        assertThat(target.getTransactions().size(), is(1));
        assertThat(client.getAllTransactions().size(), is(1));
        assertThat(source.getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_OUT));
        assertThat(target.getTransactions().get(0).getType(), is(AccountTransaction.Type.TRANSFER_IN));
        assertValid(client);
    }

    @Test
    public void testBuySellDuplicatePipelineSkipsSecondLedgerInsertion()
    {
        Account account = client.getAccounts().get(0);
        Portfolio portfolio = client.getPortfolios().get(0);

        Status firstStatus = importIfNotDuplicate(buySell(Type.BUY), account, portfolio);
        Status duplicateStatus = importIfNotDuplicate(buySell(Type.BUY), account, portfolio);

        assertThat(firstStatus.getCode(), is(Status.Code.OK));
        assertThat(duplicateStatus.getCode(), is(Status.Code.WARNING));
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(account.getTransactions().size(), is(1));
        assertThat(portfolio.getTransactions().size(), is(1));
        assertThat(client.getAllTransactions().size(), is(1));
        assertThat(account.getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertThat(portfolio.getTransactions().get(0), instanceOf(LedgerBackedTransaction.class));
        assertValid(client);
    }

    @Test
    public void testXmlAndProtobufRoundtripAfterImportedLedgerEntries() throws Exception
    {
        Account account = client.getAccounts().get(0);
        Portfolio portfolio = client.getPortfolios().get(0);
        InsertAction action = new InsertAction(client);
        action.process(accountOnly(AccountTransaction.Type.DEPOSIT), account);
        action.process(dividend(), account);
        action.process(delivery(Type.DELIVERY_INBOUND), portfolio);
        action.process(buySell(Type.BUY), account, portfolio);

        Client xml = loadXml(saveXml(client));
        Client protobuf = loadProtobuf(saveProtobuf(client));

        assertThat(xml.getLedger().getEntries().size(), is(4));
        assertThat(protobuf.getLedger().getEntries().size(), is(4));
        assertThat(xml.getAllTransactions().size(), is(4));
        assertThat(protobuf.getAllTransactions().size(), is(4));
        assertTrue(xml.getAllTransactions().stream()
                        .allMatch(holder -> holder.getTransaction() instanceof LedgerBackedTransaction));
        assertTrue(protobuf.getAllTransactions().stream()
                        .allMatch(holder -> holder.getTransaction() instanceof LedgerBackedTransaction));
        assertValid(xml);
        assertValid(protobuf);
    }

    private InvestmentPlan buyPlan(String name, Account account, Portfolio portfolio, Security security, LocalDate start)
    {
        InvestmentPlan plan = new InvestmentPlan(name);
        plan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
        plan.setAccount(account);
        plan.setPortfolio(portfolio);
        plan.setSecurity(security);
        plan.setAmount(Values.Amount.factorize(100));
        plan.setStart(start);
        plan.setInterval(12);
        return plan;
    }

    private void assertTransaction(PortfolioTransaction t)
    {
        assertThat(t.getSecurity(), is(client.getSecurities().get(0)));
        assertThat(t.getMonetaryAmount(), is(Money.of(CurrencyUnit.EUR, 9_99)));
        assertThat(t.getNote(), is("note"));
        assertThat(t.getSource(), is("source"));
        assertThat(t.getDateTime(), is(transactionDate));
        assertThat(t.getShares(), is(99L));

        assertThat(t.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, 1_99)));
        assertThat(t.getUnits().count(), is(1L));
    }

    private void assertUpdatedPlanImportRoundtrip(Client client, long importedShares)
    {
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(client.getAccounts().get(0).getTransactions().size(), is(1));
        assertThat(client.getPortfolios().get(0).getTransactions().size(), is(1));
        assertThat(client.getAllTransactions().size(), is(1));

        PortfolioTransaction updated = client.getPortfolios().get(0).getTransactions().get(0);
        assertThat(updated, instanceOf(LedgerBackedTransaction.class));
        assertThat(updated.getShares(), is(importedShares));
        assertThat(updated.getNote(), is("imported note"));
        assertThat(updated.getSource(), is("imported source"));
        assertThat(updated.getUnitSum(Unit.Type.FEE), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(3))));
        assertThat(updated.getUnitSum(Unit.Type.TAX), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4))));
        assertThat(client.getPlans().get(0).getTransactions(client).get(0).getTransaction(), is(updated));
        assertValid(client);
    }

    private AccountTransaction accountOnly(AccountTransaction.Type type)
    {
        AccountTransaction transaction = new AccountTransaction(type);
        transaction.setDateTime(transactionDate);
        transaction.setAmount(Values.Amount.factorize(11));
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setNote("account-only note");
        transaction.setSource("account-only source");
        return transaction;
    }

    private AccountTransaction dividend()
    {
        AccountTransaction dividend = accountOnly(AccountTransaction.Type.DIVIDENDS);
        dividend.setSecurity(client.getSecurities().get(0));
        dividend.setShares(Values.Share.factorize(3));
        dividend.setExDate(exDate);
        dividend.addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(20)),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(40)), BigDecimal.valueOf(0.5)));
        dividend.addUnit(new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5))));
        return dividend;
    }

    private AccountTransferEntry accountTransfer()
    {
        AccountTransferEntry transfer = new AccountTransferEntry();
        transfer.setDate(transactionDate);
        transfer.getSourceTransaction().setMonetaryAmount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100)));
        transfer.getTargetTransaction().setMonetaryAmount(Money.of(CurrencyUnit.USD, Values.Amount.factorize(200)));
        transfer.setNote("transfer note");
        transfer.setSource("transfer source");
        return transfer;
    }

    private PortfolioTransaction delivery(Type type)
    {
        PortfolioTransaction transaction = new PortfolioTransaction(type);
        transaction.setDateTime(transactionDate);
        transaction.setSecurity(client.getSecurities().get(0));
        transaction.setAmount(Values.Amount.factorize(30));
        transaction.setCurrencyCode(CurrencyUnit.EUR);
        transaction.setShares(Values.Share.factorize(3));
        transaction.setNote("delivery note");
        transaction.setSource("delivery source");
        transaction.addUnit(new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2)),
                        Money.of(CurrencyUnit.USD, Values.Amount.factorize(4)), BigDecimal.valueOf(0.5)));
        return transaction;
    }

    private PortfolioTransferEntry portfolioTransfer()
    {
        PortfolioTransferEntry transfer = new PortfolioTransferEntry();
        transfer.setDate(transactionDate);
        transfer.setSecurity(client.getSecurities().get(0));
        transfer.setShares(Values.Share.factorize(7));
        transfer.setAmount(Values.Amount.factorize(70));
        transfer.setCurrencyCode(CurrencyUnit.EUR);
        transfer.setNote("portfolio transfer note");
        transfer.setSource("portfolio transfer source");
        return transfer;
    }

    private BuySellEntry buySell(Type type)
    {
        BuySellEntry buySell = new BuySellEntry();
        buySell.setType(type);
        buySell.setMonetaryAmount(Money.of(CurrencyUnit.EUR, 9_99));
        buySell.setShares(99);
        buySell.setDate(transactionDate);
        buySell.setSecurity(client.getSecurities().get(0));
        buySell.setNote("note");
        buySell.setSource("source");
        buySell.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, 1_99)));
        return buySell;
    }

    private Status importIfNotDuplicate(AccountTransaction transaction, Account account)
    {
        Status status = new DetectDuplicatesAction(client).process(transaction, account);
        if (status.getCode() == Status.Code.OK)
            new InsertAction(client).process(transaction, account);
        return status;
    }

    private Status importIfNotDuplicate(PortfolioTransaction transaction, Portfolio portfolio)
    {
        Status status = new DetectDuplicatesAction(client).process(transaction, portfolio);
        if (status.getCode() == Status.Code.OK)
            new InsertAction(client).process(transaction, portfolio);
        return status;
    }

    private Status importIfNotDuplicate(BuySellEntry buySell, Account account, Portfolio portfolio)
    {
        Status status = new DetectDuplicatesAction(client).process(buySell, account, portfolio);
        if (status.getCode() == Status.Code.OK)
            new InsertAction(client).process(buySell, account, portfolio);
        return status;
    }

    private Status importIfNotDuplicate(AccountTransferEntry transfer, Account source, Account target)
    {
        Status status = new DetectDuplicatesAction(client).process(transfer, source, target);
        if (status.getCode() == Status.Code.OK)
            new InsertAction(client).process(transfer, source, target);
        return status;
    }

    private Status importIfNotDuplicate(PortfolioTransferEntry transfer, Portfolio source, Portfolio target)
    {
        Status status = new DetectDuplicatesAction(client).process(transfer, source, target);
        if (status.getCode() == Status.Code.OK)
            new InsertAction(client).process(transfer, source, target);
        return status;
    }

    private LocalDateTime exDate(LedgerPosting posting)
    {
        return posting.getParameters().stream()
                        .filter(parameter -> parameter.getType() == LedgerParameterType.EX_DATE)
                        .findFirst()
                        .map(LedgerParameter::getValue)
                        .map(LocalDateTime.class::cast)
                        .orElse(null);
    }

    private void assertValid(Client client)
    {
        if (!LedgerStructuralValidator.validate(client.getLedger()).isOK())
            throw new AssertionError(LedgerStructuralValidator.validate(client.getLedger()).getIssues().toString());
    }

    private byte[] saveXml(Client client) throws Exception
    {
        File file = Files.createTempFile("ledger-import-insert-action", ".xml").toFile();
        try
        {
            ClientFactory.save(client, file);
            return Files.readAllBytes(file.toPath());
        }
        finally
        {
            Files.deleteIfExists(file.toPath());
        }
    }

    private Client loadXml(byte[] bytes) throws Exception
    {
        return ClientFactory.load(new ByteArrayInputStream(bytes));
    }

    private byte[] saveProtobuf(Client client) throws Exception
    {
        File file = Files.createTempFile("ledger-import-insert-action", ".portfolio").toFile();
        try
        {
            ClientFactory.saveAs(client, file, null, EnumSet.of(SaveFlag.BINARY, SaveFlag.COMPRESSED));
            return Files.readAllBytes(file.toPath());
        }
        finally
        {
            Files.deleteIfExists(file.toPath());
        }
    }

    private Client loadProtobuf(byte[] bytes) throws Exception
    {
        File file = Files.createTempFile("ledger-import-insert-action", ".portfolio").toFile();
        try
        {
            Files.write(file.toPath(), bytes);
            return ClientFactory.load(file, null, new NullProgressMonitor());
        }
        finally
        {
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test
    public void testPortfolioTransactionAttributes() throws IntrospectionException
    {
        // This test is only a "marker" that fails if the PortfolioTransaction
        // is structurally changed. If it is changed, then the ImportAction
        // needs to change too.

        BeanInfo info = Introspector.getBeanInfo(PortfolioTransaction.class);

        Set<String> properties = Arrays.stream(info.getPropertyDescriptors()).filter(p -> p.getWriteMethod() != null)
                        .map(PropertyDescriptor::getName).collect(Collectors.toSet());

        assertThat(properties, hasItem("security"));
        assertThat(properties, hasItem("monetaryAmount"));
        assertThat(properties, hasItem("currencyCode"));
        assertThat(properties, hasItem("amount"));
        assertThat(properties, hasItem("shares"));
        assertThat(properties, hasItem("dateTime"));
        assertThat(properties, hasItem("type"));
        assertThat(properties, hasItem("note"));
        assertThat(properties, hasItem("source"));
        assertThat(properties, hasItem("updatedAt"));

        assertThat(properties.size(), is(10));
    }
}
