package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountCashLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellEdit;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerBuySellEditor;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCreationUnits;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioSecurityLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerSecurityQuantity;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerShareAdjustmentHelper;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerTransactionCreator;
import name.abuchen.portfolio.model.ledger.configuration.CashCompensationKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionSubtype;
import name.abuchen.portfolio.model.ledger.configuration.EventStage;
import name.abuchen.portfolio.model.ledger.configuration.FeeReason;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.model.ledger.nativeentry.LedgerNativeEntryAssembler;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeCashCompensation;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeCorporateActionEvent;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeEntryMetadata;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeFee;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeSecurityLeg;
import name.abuchen.portfolio.model.ledger.nativeentry.NativeTax;
import name.abuchen.portfolio.model.ledger.nativeentry.Ratio;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests ledger-native entry assembly for advanced transaction shapes.
 * These tests make sure structural facts can be represented without enabling unsupported UI workflows.
 */
@SuppressWarnings("nls")
public class LedgerSpinOffScenarioTest
{
    private static final Path XML_EXAMPLE = Path
                    .of("name.abuchen.portfolio.tests", "src", "name", "abuchen", "portfolio", "model", "ledger",
                                    "ledger-v6-spin-off-siemens-energy-example.xml");

    private static final LocalDateTime SPIN_OFF_DATE = LocalDateTime.of(2020, 9, 28, 0, 0);
    private static final LocalDateTime BUY_DATE = LocalDateTime.of(2020, 1, 2, 0, 0);
    private static final Instant UPDATED_AT = Instant.parse("2026-06-15T08:00:00Z");

    /**
     * Checks the Ledger-V6 scenario: share adjustment helper scales selected targeted spin off postings.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testShareAdjustmentHelperScalesSelectedTargetedSpinOffPostings() throws Exception
    {
        var fixture = fixture();
        var client = fixture.client();
        LedgerProjectionService.materialize(client);

        var entry = spinOffEntry(client);
        var entryUUID = entry.getUUID();
        var postingUUIDs = entry.getPostings().stream().map(LedgerPosting::getUUID).toList();
        var projectionUUIDs = name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).stream().map(descriptor -> descriptor.getRuntimeProjectionId()).toList();
        var selected = fixture.siemens().getTransactions(client).stream()
                        .map(pair -> (Transaction) pair.getTransaction())
                        .filter(transaction -> transaction.getDateTime().isBefore(SPIN_OFF_DATE.plusDays(1))).toList();

        LedgerShareAdjustmentHelper.plan(client, fixture.siemens(), selected, shares -> shares * 2).apply();
        LedgerProjectionService.materialize(client);

        var editedEntry = spinOffEntry(client);
        assertThat(editedEntry.getUUID(), is(entryUUID));
        assertThat(editedEntry.getPostings().stream().map(LedgerPosting::getUUID).toList(), is(postingUUIDs));
        assertThat(name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(editedEntry).stream().map(descriptor -> descriptor.getRuntimeProjectionId()).toList(),
                        is(projectionUUIDs));
        assertThat(buyProjection(client, fixture.siemens()).getShares(), is(Values.Share.factorize(20)));

        var oldLeg = portfolioProjection(fixture.portfolio(), LedgerProjectionRole.OLD_SECURITY_LEG);
        var retainedLeg = portfolioProjection(fixture.portfolio(), LedgerProjectionRole.DELIVERY_INBOUND,
                        fixture.siemens());
        var newLeg = portfolioProjection(fixture.portfolio(), LedgerProjectionRole.NEW_SECURITY_LEG);

        assertThat(oldLeg.getShares(), is(Values.Share.factorize(20)));
        assertThat(retainedLeg.getShares(), is(Values.Share.factorize(20)));
        assertThat(retainedLeg.getShares() - oldLeg.getShares(), is(0L));
        assertThat(newLeg.getShares(), is(Values.Share.factorize(5)));
        assertTrue(LedgerStructuralValidator.validate(client.getLedger()).isOK());

        var loaded = loadXml(saveXml(client));
        assertThat(buyProjection(loaded, siemens(loaded)).getShares(), is(Values.Share.factorize(20)));
        assertThat(portfolioProjection(loaded.getPortfolios().get(0), LedgerProjectionRole.OLD_SECURITY_LEG)
                        .getShares(), is(Values.Share.factorize(20)));
        assertThat(portfolioProjection(loaded.getPortfolios().get(0), LedgerProjectionRole.DELIVERY_INBOUND,
                        siemens(loaded)).getShares(), is(Values.Share.factorize(20)));
        assertThat(portfolioProjection(loaded.getPortfolios().get(0), LedgerProjectionRole.NEW_SECURITY_LEG)
                        .getShares(), is(Values.Share.factorize(5)));
    }

    /**
     * Checks the Ledger-V6 scenario: share adjustment helper rejects targeted projection without primary posting.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testShareAdjustmentHelperRejectsTargetedProjectionWithoutPrimaryPosting()
    {
        var client = new Client();
        var portfolio = portfolio("Targeted portfolio");
        var security = security("Targeted Security", "DE000TARGET0", "TGT.DE");
        var entry = new LedgerEntry();

        client.addPortfolio(portfolio);
        client.addSecurity(security);
        entry.setType(LedgerEntryType.CORPORATE_ACTION);
        entry.setDateTime(SPIN_OFF_DATE);
        entry.addParameter(LedgerParameter.ofCode(LedgerParameterType.CORPORATE_ACTION_KIND,
                        CorporateActionKind.SPIN_OFF));

        var posting = invalidTargetSecurityPosting(portfolio, security, Values.Share.factorize(10),
                        Values.Amount.factorize(100), CorporateActionLeg.TARGET_SECURITY.getCode(), security,
                        security);
        entry.addPosting(posting);
        client.getLedger().addEntry(entry);

        var exception = assertThrows(IllegalArgumentException.class,
                        () -> LedgerProjectionService.createProjection(entry, LedgerProjectionRole.NEW_SECURITY_LEG));

        assertFalse(exception.getMessage().isBlank());
        assertThat(posting.getShares(), is(Values.Share.factorize(10)));
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(portfolio.getTransactions().size(), is(0));
    }

    /**
     * Checks the Ledger-V6 scenario: edit loaded spin off example buy shares only without uuid literals.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testEditLoadedSpinOffExampleBuySharesOnlyWithoutUuidLiterals() throws Exception
    {
        var client = loadXmlExample();
        var siemens = siemens(client);
        var buy = buyProjection(client, siemens);
        var buyEntry = ((LedgerBackedTransaction) buy).getLedgerEntry();
        var entryUUID = buyEntry.getUUID();
        var projectionUUIDs = name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(buyEntry).stream().map(descriptor -> descriptor.getRuntimeProjectionId()).toList();

        assertThat(buy.getShares(), is(Values.Share.factorize(10)));

        new LedgerBuySellEditor().apply((LedgerBackedPortfolioTransaction) buy,
                        LedgerBuySellEdit.builder().shares(Values.Share.factorize(100)).build());
        LedgerProjectionService.materialize(client);

        var editedBuy = buyProjection(client, siemens);
        var editedEntry = ((LedgerBackedTransaction) editedBuy).getLedgerEntry();

        assertThat(editedEntry.getUUID(), is(entryUUID));
        assertThat(name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(editedEntry).stream().map(descriptor -> descriptor.getRuntimeProjectionId()).toList(),
                        is(projectionUUIDs));
        assertSame(siemens, editedBuy.getSecurity());
        assertThat(editedBuy.getShares(), is(Values.Share.factorize(100)));
        // This is a shares-only correction; the supporting BUY cash amount remains unchanged.
        assertThat(accountProjection(editedEntry).getAmount(), is(118640L));
        assertXmlRoundtripHasEditedBuy(client);
    }

    /**
     * Checks the Ledger-V6 scenario: edit loaded spin off example cash compensation without uuid literals.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testEditLoadedSpinOffExampleCashCompensationWithoutUuidLiterals() throws Exception
    {
        var client = loadXmlExample();
        var entry = spinOffEntry(client);
        var oldSiemensOut = securityPosting(entry, siemens(client), CorporateActionLeg.SOURCE_SECURITY.getCode(),
                        siemensEnergy(client));
        var siemensBackIn = securityPosting(entry, siemens(client), CorporateActionLeg.TARGET_SECURITY.getCode(),
                        siemens(client));
        var siemensEnergyIn = securityPosting(entry, siemensEnergy(client),
                        CorporateActionLeg.TARGET_SECURITY.getCode(), siemensEnergy(client));
        var compensation = primaryPosting(entry, LedgerProjectionRole.CASH_COMPENSATION);
        var entryUUID = entry.getUUID();
        var postingUUIDs = entry.getPostings().stream().map(LedgerPosting::getUUID).toList();
        var projectionUUIDs = name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).stream().map(descriptor -> descriptor.getRuntimeProjectionId()).toList();

        assertThat(compensation.getAmount(), is(Values.Amount.factorize(5)));

        new LedgerMutationContext(client).mutateEntry(entry,
                        edited -> primaryPosting(edited, LedgerProjectionRole.CASH_COMPENSATION)
                                        .setAmount(Values.Amount.factorize(100)));

        var edited = spinOffEntry(client);
        var editedCompensation = primaryPosting(edited, LedgerProjectionRole.CASH_COMPENSATION);

        assertThat(edited.getUUID(), is(entryUUID));
        assertThat(edited.getPostings().stream().map(LedgerPosting::getUUID).toList(), is(postingUUIDs));
        assertThat(name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(edited).stream().map(descriptor -> descriptor.getRuntimeProjectionId()).toList(),
                        is(projectionUUIDs));
        assertSecurityPostingUnchanged(edited, oldSiemensOut);
        assertSecurityPostingUnchanged(edited, siemensBackIn);
        assertSecurityPostingUnchanged(edited, siemensEnergyIn);
        assertThat(editedCompensation.getAmount(), is(Values.Amount.factorize(100)));
        assertThat(accountProjection(client.getAccounts().get(0), LedgerProjectionRole.CASH_COMPENSATION).getAmount(),
                        is(Values.Amount.factorize(100)));
        assertThat(accountProjection(client.getAccounts().get(0), LedgerProjectionRole.CASH_COMPENSATION)
                        .getUnit(Unit.Type.FEE).orElseThrow().getAmount().getAmount(), is(Values.Amount.factorize(2)));
        assertThat(accountProjection(client.getAccounts().get(0), LedgerProjectionRole.CASH_COMPENSATION)
                        .getUnit(Unit.Type.TAX).orElseThrow().getAmount().getAmount(), is(Values.Amount.factorize(1)));
        assertXmlRoundtripHasEditedCompensation(client);
    }

    /**
     * Checks the Ledger-V6 scenario: spin off documentation does not expose uuid construction.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    private SpinOffFixture fixture()
    {
        var client = new Client();
        var account = account("Spin-off cash account");
        var portfolio = portfolio("Corporate action portfolio");
        var siemens = security("Siemens AG", "DE0007236101", "SIE.DE");
        var siemensEnergy = security("Siemens Energy AG", "DE000ENER6Y0", "ENR.DE");

        client.addAccount(account);
        client.addPortfolio(portfolio);
        client.addSecurity(siemens);
        client.addSecurity(siemensEnergy);
        portfolio.setReferenceAccount(account);
        createStandardDeposit(client, account);
        createStandardBuy(client, account, portfolio, siemens);
        createSpinOffEntry(client, account, portfolio, siemens, siemensEnergy);

        return new SpinOffFixture(client, account, portfolio, siemens, siemensEnergy);
    }

    private LedgerEntry spinOffEntry(Client client)
    {
        return client.getLedger().getEntries().stream().filter(entry -> entry.getType() == LedgerEntryType.CORPORATE_ACTION)
                        .filter(entry -> SPIN_OFF_DATE.equals(entry.getDateTime())).findFirst().orElseThrow();
    }

    private LedgerEntry createSpinOffEntry(Client client, Account account, Portfolio portfolio, Security siemens,
                    Security siemensEnergy)
    {
        var entry = LedgerNativeEntryAssembler.forClient(client).spinOff()
                        .metadata(NativeEntryMetadata.of(SPIN_OFF_DATE)
                                        .note("Siemens Energy spin-off")
                                        .source("Ledger"))
                        .event(NativeCorporateActionEvent.builder()
                                        .kind(CorporateActionKind.SPIN_OFF)
                                        .subtype(CorporateActionSubtype.STANDARD)
                                        .reference("SIEMENS-ENERGY-2020")
                                        .stage(EventStage.SETTLED)
                                        .effectiveDate(SPIN_OFF_DATE.toLocalDate())
                                        .build())
                        .securityLeg(NativeSecurityLeg.source()
                                        .portfolio(portfolio)
                                        .security(siemens)
                                        .shares(Values.Share.factorize(10))
                                        .amount(Money.of(CurrencyUnit.EUR, 109960L))
                                        .sourceSecurity(siemens)
                                        .targetSecurity(siemensEnergy)
                                        .ratio(Ratio.of(BigDecimal.ONE, BigDecimal.valueOf(2)))
                                        .build())
                        .securityLeg(NativeSecurityLeg.context()
                                        .portfolio(portfolio)
                                        .security(siemens)
                                        .shares(0L)
                                        .amount(Money.of(CurrencyUnit.EUR, 0L))
                                        .groupKey("main")
                                        .localKey("context-1")
                                        .build())
                        .securityLeg(NativeSecurityLeg.target()
                                        .portfolio(portfolio)
                                        .security(siemens)
                                        .shares(Values.Share.factorize(10))
                                        .amount(Money.of(CurrencyUnit.EUR, 109960L))
                                        .sourceSecurity(siemens)
                                        .targetSecurity(siemens)
                                        .ratio(Ratio.of(BigDecimal.ONE, BigDecimal.valueOf(2)))
                                        .projectAs(LedgerProjectionRole.DELIVERY_INBOUND)
                                        .build())
                        .securityLeg(NativeSecurityLeg.target()
                                        .portfolio(portfolio)
                                        .security(siemensEnergy)
                                        .shares(Values.Share.factorize(5))
                                        .amount(Money.of(CurrencyUnit.EUR, 10605L))
                                        .sourceSecurity(siemens)
                                        .targetSecurity(siemensEnergy)
                                        .ratio(Ratio.of(BigDecimal.ONE, BigDecimal.valueOf(2)))
                                        .build())
                        .cashCompensation(NativeCashCompensation.builder()
                                        .account(account)
                                        .amount(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(5)))
                                        .kind(CashCompensationKind.CASH_IN_LIEU)
                                        .build())
                        .fee(NativeFee.of(account, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(2)),
                                        FeeReason.CORPORATE_ACTION_FEE))
                        .tax(NativeTax.withholding(account, Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1))))
                        .buildAndAdd()
                        .getEntry();

        entry.setUpdatedAt(UPDATED_AT);

        return entry;
    }

    private void createStandardDeposit(Client client, Account account)
    {
        var entry = new LedgerTransactionCreator(client)
                        .createDeposit(LedgerTransactionMetadata.of(LocalDateTime.of(2019, 12, 30, 0, 0)),
                                        LedgerAccountCashLeg.of(account,
                                                        Money.of(CurrencyUnit.EUR,
                                                                        Values.Amount.factorize(10000))))
                        .getEntry();

        entry.setUpdatedAt(Instant.parse("2026-06-15T10:41:50.210577100Z"));
    }

    private void createStandardBuy(Client client, Account account, Portfolio portfolio, Security siemens)
    {
        var entry = new LedgerTransactionCreator(client)
                        .createBuy(LedgerTransactionMetadata.of(BUY_DATE),
                                        LedgerAccountCashLeg.of(account, Money.of(CurrencyUnit.EUR, 118640L)),
                                        LedgerPortfolioSecurityLeg.of(portfolio,
                                                        LedgerSecurityQuantity.of(siemens,
                                                                        Values.Share.factorize(10)),
                                                        Money.of(CurrencyUnit.EUR, 118640L)),
                                        LedgerCreationUnits.none())
                        .getEntry();

        entry.setUpdatedAt(Instant.parse("2026-06-15T10:41:34.896212600Z"));
    }

    private LedgerPosting invalidTargetSecurityPosting(Portfolio portfolio, Security security, long shares, long amount,
                    String leg, Security sourceSecurity, Security targetSecurity)
    {
        var posting = new LedgerPosting();

        posting.setType(LedgerPostingType.SECURITY);
        posting.setPortfolio(portfolio);
        posting.setSecurity(security);
        posting.setShares(shares);
        posting.setAmount(amount);
        posting.setCurrency(CurrencyUnit.EUR);
        if (leg != null)
            posting.addParameter(LedgerParameter.ofString(
                            LedgerParameterType.CORPORATE_ACTION_LEG, leg));
        if (sourceSecurity != null)
            posting.addParameter(LedgerParameter.ofSecurity(LedgerParameterType.SOURCE_SECURITY,
                            sourceSecurity));
        if (targetSecurity != null)
            posting.addParameter(LedgerParameter.ofSecurity(LedgerParameterType.TARGET_SECURITY,
                            targetSecurity));
        if (sourceSecurity != null && targetSecurity != null)
        {
            posting.addParameter(LedgerParameter.ofDecimal(LedgerParameterType.RATIO_NUMERATOR,
                            BigDecimal.ONE));
            posting.addParameter(LedgerParameter.ofDecimal(LedgerParameterType.RATIO_DENOMINATOR,
                            BigDecimal.valueOf(2)));
        }

        return posting;
    }

    private void assertSpinOffSiemensPositionUnchanged(Portfolio portfolio, Security siemens)
    {
        var oldLeg = portfolioProjection(portfolio, LedgerProjectionRole.OLD_SECURITY_LEG);
        var retainedLeg = portfolioProjection(portfolio, LedgerProjectionRole.DELIVERY_INBOUND, siemens);

        assertThat(oldLeg.getShares(), is(Values.Share.factorize(10)));
        assertThat(retainedLeg.getShares(), is(Values.Share.factorize(10)));
        assertThat(retainedLeg.getShares() - oldLeg.getShares(), is(0L));
    }

    private LedgerPosting securityPosting(LedgerEntry entry, Security security,
                    String leg, Security targetSecurity)
    {
        return entry.getPostings().stream().filter(posting -> posting.getType() == LedgerPostingType.SECURITY)
                        .filter(posting -> posting.getSecurity() == security)
                        .filter(posting -> hasCorporateActionLeg(posting, leg))
                        .filter(posting -> targetSecurity == null || hasTargetSecurity(posting, targetSecurity))
                        .findFirst().orElseThrow();
    }

    private PortfolioTransaction portfolioProjection(Portfolio portfolio, LedgerProjectionRole role)
    {
        return portfolio.getTransactions().stream().filter(LedgerBackedTransaction.class::isInstance)
                        .filter(transaction -> ((LedgerBackedTransaction) transaction).getLedgerProjectionDescriptor()
                                        .getRole() == role)
                        .findFirst().orElseThrow();
    }

    private PortfolioTransaction portfolioProjection(Portfolio portfolio, LedgerProjectionRole role, Security security)
    {
        return portfolio.getTransactions().stream().filter(LedgerBackedTransaction.class::isInstance)
                        .filter(transaction -> ((LedgerBackedTransaction) transaction).getLedgerProjectionDescriptor()
                                        .getRole() == role)
                        .filter(transaction -> transaction.getSecurity() == security).findFirst().orElseThrow();
    }

    private AccountTransaction accountProjection(Account account, LedgerProjectionRole role)
    {
        return account.getTransactions().stream().filter(LedgerBackedTransaction.class::isInstance)
                        .filter(transaction -> ((LedgerBackedTransaction) transaction).getLedgerProjectionDescriptor()
                                        .getRole() == role)
                        .findFirst().orElseThrow();
    }

    private AccountTransaction accountProjection(LedgerEntry entry)
    {
        var accountProjection = projection(entry, LedgerProjectionRole.ACCOUNT);

        return accountProjection.getAccount().getTransactions().stream().filter(LedgerBackedTransaction.class::isInstance)
                        .filter(transaction -> ((LedgerBackedTransaction) transaction).getLedgerEntry() == entry)
                        .filter(transaction -> ((LedgerBackedTransaction) transaction).getLedgerProjectionDescriptor()
                                        .getRole() == LedgerProjectionRole.ACCOUNT)
                        .findFirst().orElseThrow();
    }

    private PortfolioTransaction buyProjection(Client client, Security siemens)
    {
        LedgerProjectionService.materialize(client);

        return client.getPortfolios().stream().flatMap(portfolio -> portfolio.getTransactions().stream())
                        .filter(LedgerBackedTransaction.class::isInstance)
                        .filter(transaction -> transaction.getType() == PortfolioTransaction.Type.BUY)
                        .filter(transaction -> BUY_DATE.equals(transaction.getDateTime()))
                        .filter(transaction -> transaction.getSecurity() == siemens).findFirst().orElseThrow();
    }

    private name.abuchen.portfolio.model.ledger.projection.DerivedProjectionDescriptor projection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return name.abuchen.portfolio.model.ledger.LedgerDescriptorTestSupport.descriptors(entry).stream().filter(ref -> ref.getRole() == role).findFirst().orElseThrow();
    }

    private LedgerPosting primaryPosting(LedgerEntry entry, LedgerProjectionRole role)
    {
        return projection(entry, role).getPrimaryPosting();
    }

    private boolean hasCorporateActionLeg(LedgerPosting posting, String leg)
    {
        return posting.getParameters().stream()
                        .filter(parameter -> parameter.getType() == LedgerParameterType.CORPORATE_ACTION_LEG)
                        .anyMatch(parameter -> parameter.getValue().equals(leg));
    }

    private boolean hasTargetSecurity(LedgerPosting posting, Security security)
    {
        return posting.getParameters().stream()
                        .filter(parameter -> parameter.getType() == LedgerParameterType.TARGET_SECURITY)
                        .anyMatch(parameter -> parameter.getValue() == security);
    }

    private void assertSecurityPostingUnchanged(LedgerEntry edited, LedgerPosting before)
    {
        var after = edited.getPostings().stream().filter(posting -> posting.getUUID().equals(before.getUUID()))
                        .findFirst().orElseThrow();

        assertThat(after.getType(), is(before.getType()));
        assertThat(after.getSecurity(), is(before.getSecurity()));
        assertThat(after.getShares(), is(before.getShares()));
        assertThat(after.getAmount(), is(before.getAmount()));
        assertThat(after.getCurrency(), is(before.getCurrency()));
    }

    private void assertXmlRoundtripHasEditedBuy(Client client) throws Exception
    {
        var loaded = loadXml(saveXml(client));
        var buy = buyProjection(loaded, siemens(loaded));

        assertThat(buy.getShares(), is(Values.Share.factorize(100)));
        assertFalse(saveXml(loaded).contains("<portfolio-transaction"));
        assertFalse(saveXml(loaded).contains("<account-transaction"));
    }

    private void assertXmlRoundtripHasEditedCompensation(Client client) throws Exception
    {
        var loaded = loadXml(saveXml(client));
        var compensation = accountProjection(loaded.getAccounts().get(0), LedgerProjectionRole.CASH_COMPENSATION);

        assertThat(compensation.getAmount(), is(Values.Amount.factorize(100)));
        assertThat(compensation.getUnit(Unit.Type.FEE).orElseThrow().getAmount().getAmount(),
                        is(Values.Amount.factorize(2)));
        assertThat(compensation.getUnit(Unit.Type.TAX).orElseThrow().getAmount().getAmount(),
                        is(Values.Amount.factorize(1)));
        assertFalse(saveXml(loaded).contains("<portfolio-transaction"));
        assertFalse(saveXml(loaded).contains("<account-transaction"));
    }

    private Client loadXmlExample() throws Exception
    {
        return ClientFactory.load(Files.newInputStream(xmlExample()));
    }

    private Security siemens(Client client)
    {
        return client.getSecurities().stream().filter(security -> "DE0007236101".equals(security.getIsin()))
                        .findFirst().orElseThrow();
    }

    private Security siemensEnergy(Client client)
    {
        return client.getSecurities().stream().filter(security -> "DE000ENER6Y0".equals(security.getIsin()))
                        .findFirst().orElseThrow();
    }

    private String saveXml(Client client) throws Exception
    {
        var file = Files.createTempFile("ledger-spin-off", ".xml");

        try
        {
            ClientFactory.save(client, file.toFile());
            return Files.readString(file, StandardCharsets.UTF_8);
        }
        finally
        {
            Files.deleteIfExists(file);
        }
    }

    private Client loadXml(String xml) throws Exception
    {
        return ClientFactory.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private Account account(String name)
    {
        var account = new Account();

        account.setName(name);
        account.setCurrencyCode(CurrencyUnit.EUR);
        account.setUpdatedAt(UPDATED_AT);

        return account;
    }

    private Portfolio portfolio(String name)
    {
        var portfolio = new Portfolio();

        portfolio.setName(name);
        portfolio.setUpdatedAt(UPDATED_AT);

        return portfolio;
    }

    private Security security(String name, String isin, String ticker)
    {
        var security = new Security(name, CurrencyUnit.EUR);

        security.setIsin(isin);
        security.setTickerSymbol(ticker);
        security.setUpdatedAt(UPDATED_AT);

        return security;
    }

    private Path xmlExample()
    {
        return findInWorkingTree(XML_EXAMPLE);
    }

    private Path findInWorkingTree(Path relativePath)
    {
        var current = Path.of("").toAbsolutePath();

        while (current != null)
        {
            var candidate = current.resolve(relativePath);

            if (Files.exists(candidate))
                return candidate;

            current = current.getParent();
        }

        return Path.of("").toAbsolutePath().resolve(relativePath);
    }

    private record SpinOffFixture(Client client, Account account, Portfolio portfolio, Security siemens,
                    Security siemensEnergy)
    {
    }
}
