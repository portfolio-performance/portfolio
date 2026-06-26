package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

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
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;
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
                    .of("docs/ledger-v6/examples/ledger-v6-spin-off-siemens-energy-example.xml");

    private static final LocalDateTime SPIN_OFF_DATE = LocalDateTime.of(2020, 9, 28, 0, 0);
    private static final LocalDateTime BUY_DATE = LocalDateTime.of(2020, 1, 2, 0, 0);
    private static final Instant UPDATED_AT = Instant.parse("2026-06-15T08:00:00Z");

    /**
     * Checks the Ledger-V6 scenario: spin off uses ledger native targeted policy.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testSpinOffUsesLedgerNativeTargetedPolicy()
    {
        assertFalse(LedgerEntryType.SPIN_OFF.isLegacyFixedShape());
        assertTrue(LedgerEntryType.SPIN_OFF.isLedgerNativeTargeted());
        assertTrue(LedgerEntryType.SPIN_OFF.requiresTargetedProjectionRefs());
        assertTrue(LedgerEntryType.SPIN_OFF.usesSignedTargetedProjectionFacts());
    }

    /**
     * Checks the Ledger-V6 scenario: targeted projection ref can use posting objects.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testTargetedProjectionRefCanUsePostingObjects()
    {
        var primaryPosting = new LedgerPosting();
        var groupPosting = new LedgerPosting();
        var projection = new LedgerProjectionRef();

        projection.setPrimaryPosting(primaryPosting);
        projection.setPostingGroup(groupPosting);

        assertThat(projection.getPrimaryPostingUUID(), is(primaryPosting.getUUID()));
        assertThat(projection.getPostingGroupUUID(), is(groupPosting.getUUID()));

        assertThrows(NullPointerException.class, () -> projection.setPrimaryPosting(null));
        assertThrows(NullPointerException.class, () -> projection.setPostingGroup(null));
    }

    /**
     * Checks the Ledger-V6 scenario: creates targeted spin off shape.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testCreatesTargetedSpinOffShape()
    {
        var fixture = fixture();
        var client = fixture.client();
        var entry = spinOffEntry(client);

        assertThat(entry.getPostings().size(), is(6));
        assertThat(entry.getProjectionRefs().size(), is(4));
        var oldSiemensOut = securityPosting(entry, fixture.siemens(),
                        CorporateActionLeg.SOURCE_SECURITY.getCode(), fixture.siemensEnergy());
        var siemensBackIn = securityPosting(entry, fixture.siemens(),
                        CorporateActionLeg.TARGET_SECURITY.getCode(), fixture.siemens());
        var siemensEnergyIn = securityPosting(entry, fixture.siemensEnergy(),
                        CorporateActionLeg.TARGET_SECURITY.getCode(), fixture.siemensEnergy());
        var compensation = posting(entry, LedgerPostingType.CASH_COMPENSATION, fixture.account(),
                        Values.Amount.factorize(5));
        posting(entry, LedgerPostingType.FEE, fixture.account(), Values.Amount.factorize(2));
        posting(entry, LedgerPostingType.TAX, fixture.account(), Values.Amount.factorize(1));

        assertProjectionTargets(entry, LedgerProjectionRole.OLD_SECURITY_LEG, oldSiemensOut, null);
        assertProjectionTargets(entry, LedgerProjectionRole.DELIVERY_INBOUND, siemensBackIn, null);
        assertProjectionTargets(entry, LedgerProjectionRole.NEW_SECURITY_LEG, siemensEnergyIn, null);
        assertProjectionTargets(entry, LedgerProjectionRole.CASH_COMPENSATION, compensation, compensation);
        assertTrue(LedgerStructuralValidator.validate(client.getLedger()).isOK());
    }

    /**
     * Checks the Ledger-V6 scenario: materializes spin off compatibility projections.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testMaterializesSpinOffCompatibilityProjections()
    {
        var fixture = fixture();
        LedgerProjectionService.materialize(fixture.client());
        LedgerProjectionService.materialize(fixture.client());

        assertThat(fixture.portfolio().getTransactions().size(), is(4));
        assertThat(fixture.account().getTransactions().size(), is(3));

        var oldLeg = portfolioProjection(fixture.portfolio(), LedgerProjectionRole.OLD_SECURITY_LEG);
        var retainedLeg = portfolioProjection(fixture.portfolio(), LedgerProjectionRole.DELIVERY_INBOUND,
                        fixture.siemens());
        var newLeg = portfolioProjection(fixture.portfolio(), LedgerProjectionRole.NEW_SECURITY_LEG);
        var compensation = accountProjection(fixture.account(), LedgerProjectionRole.CASH_COMPENSATION);

        assertThat(oldLeg, instanceOf(LedgerBackedTransaction.class));
        assertThat(oldLeg.getType(), is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertSame(fixture.siemens(), oldLeg.getSecurity());
        assertThat(oldLeg.getShares(), is(Values.Share.factorize(10)));
        assertThat(oldLeg.getUnits().count(), is(0L));

        assertThat(retainedLeg, instanceOf(LedgerBackedTransaction.class));
        assertThat(retainedLeg.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertSame(fixture.siemens(), retainedLeg.getSecurity());
        assertThat(retainedLeg.getShares(), is(Values.Share.factorize(10)));
        assertThat(retainedLeg.getAmount(), is(oldLeg.getAmount()));
        assertThat(retainedLeg.getUnits().count(), is(0L));

        assertThat(newLeg, instanceOf(LedgerBackedTransaction.class));
        assertThat(newLeg.getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertSame(fixture.siemensEnergy(), newLeg.getSecurity());
        assertThat(newLeg.getShares(), is(Values.Share.factorize(5)));
        assertThat(newLeg.getUnits().count(), is(0L));

        assertThat(compensation, instanceOf(LedgerBackedTransaction.class));
        assertThat(compensation.getType(), is(AccountTransaction.Type.DEPOSIT));
        assertThat(compensation.getAmount(), is(Values.Amount.factorize(5)));
        assertThat(compensation.getUnit(Unit.Type.FEE).orElseThrow().getAmount().getAmount(),
                        is(Values.Amount.factorize(2)));
        assertThat(compensation.getUnit(Unit.Type.TAX).orElseThrow().getAmount().getAmount(),
                        is(Values.Amount.factorize(1)));
        assertThat(fixture.client().getAllTransactions().size(), is(6));
        assertSpinOffSiemensPositionUnchanged(fixture.portfolio(), fixture.siemens());
    }

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
        var projectionUUIDs = entry.getProjectionRefs().stream().map(LedgerProjectionRef::getUUID).toList();
        var selected = fixture.siemens().getTransactions(client).stream()
                        .map(pair -> (Transaction) pair.getTransaction())
                        .filter(transaction -> transaction.getDateTime().isBefore(SPIN_OFF_DATE.plusDays(1))).toList();

        LedgerShareAdjustmentHelper.plan(client, fixture.siemens(), selected, shares -> shares * 2).apply();
        LedgerProjectionService.materialize(client);

        var editedEntry = spinOffEntry(client);
        assertThat(editedEntry.getUUID(), is(entryUUID));
        assertThat(editedEntry.getPostings().stream().map(LedgerPosting::getUUID).toList(), is(postingUUIDs));
        assertThat(editedEntry.getProjectionRefs().stream().map(LedgerProjectionRef::getUUID).toList(),
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
        entry.setType(LedgerEntryType.SPIN_OFF);
        entry.setDateTime(SPIN_OFF_DATE);

        var posting = invalidTargetSecurityPosting(portfolio, security, Values.Share.factorize(10),
                        Values.Amount.factorize(100), CorporateActionLeg.TARGET_SECURITY.getCode(), security,
                        security);
        var projection = new LedgerProjectionRef();
        projection.setRole(LedgerProjectionRole.NEW_SECURITY_LEG);
        projection.setPortfolio(portfolio);
        entry.addPosting(posting);
        entry.addProjectionRef(projection);
        client.getLedger().addEntry(entry);

        var transaction = LedgerProjectionService.createProjection(entry, projection);

        assertThrows(IllegalArgumentException.class,
                        () -> LedgerShareAdjustmentHelper.plan(client, security, List.of(transaction),
                                        shares -> shares * 2));
        assertThat(posting.getShares(), is(Values.Share.factorize(10)));
        assertThat(client.getLedger().getEntries().size(), is(1));
        assertThat(portfolio.getTransactions().size(), is(0));
    }

    /**
     * Checks the Ledger-V6 scenario: xml roundtrip preserves targeted spin off shape.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testXmlRoundtripPreservesTargetedSpinOffShape() throws Exception
    {
        var client = fixture().client();
        LedgerProjectionService.materialize(client);

        var xml = saveXml(client);

        assertTrue(xml.contains("<ledger>"));
        assertFalse(xml.contains("<account-transaction"));
        assertFalse(xml.contains("<portfolio-transaction"));
        assertFalse(xml.contains("LedgerBacked"));

        var loaded = loadXml(xml);

        assertSpinOffScenarioClient(loaded);
    }

    /**
     * Checks the Ledger-V6 scenario: xml example read back.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testXmlExampleReadBack() throws Exception
    {
        var xmlExample = xmlExample();

        assertTrue("Missing committed XML example: " + xmlExample.toAbsolutePath(), Files.exists(xmlExample));
        assertSpinOffScenarioClient(ClientFactory.load(Files.newInputStream(xmlExample)));
    }

    /**
     * Checks the Ledger-V6 scenario: protobuf roundtrip preserves targeted spin off shape.
     * The result must keep ledger truth and visible runtime rows consistent.
     * This protects against duplicate truth or partial mutation.
     */
    @Test
    public void testProtobufRoundtripPreservesTargetedSpinOffShape() throws Exception
    {
        var client = fixture().client();
        LedgerProjectionService.materialize(client);

        assertSpinOffScenarioClient(loadProtobuf(saveProtobuf(client)));
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
        var projectionUUIDs = buyEntry.getProjectionRefs().stream().map(LedgerProjectionRef::getUUID).toList();

        assertThat(buy.getShares(), is(Values.Share.factorize(10)));

        new LedgerBuySellEditor().apply((LedgerBackedPortfolioTransaction) buy,
                        LedgerBuySellEdit.builder().shares(Values.Share.factorize(100)).build());
        LedgerProjectionService.materialize(client);

        var editedBuy = buyProjection(client, siemens);
        var editedEntry = ((LedgerBackedTransaction) editedBuy).getLedgerEntry();

        assertThat(editedEntry.getUUID(), is(entryUUID));
        assertThat(editedEntry.getProjectionRefs().stream().map(LedgerProjectionRef::getUUID).toList(),
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
        var projectionUUIDs = entry.getProjectionRefs().stream().map(LedgerProjectionRef::getUUID).toList();

        assertThat(compensation.getAmount(), is(Values.Amount.factorize(5)));

        new LedgerMutationContext(client).mutateEntry(entry,
                        edited -> primaryPosting(edited, LedgerProjectionRole.CASH_COMPENSATION)
                                        .setAmount(Values.Amount.factorize(100)));

        var edited = spinOffEntry(client);
        var editedCompensation = primaryPosting(edited, LedgerProjectionRole.CASH_COMPENSATION);

        assertThat(edited.getUUID(), is(entryUUID));
        assertThat(edited.getPostings().stream().map(LedgerPosting::getUUID).toList(), is(postingUUIDs));
        assertThat(edited.getProjectionRefs().stream().map(LedgerProjectionRef::getUUID).toList(),
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
    @Test
    public void testSpinOffDocumentationDoesNotExposeUuidConstruction() throws Exception
    {
        var markdown = Files.readString(xmlExample().resolveSibling("ledger-v6-spin-off-siemens-energy-example.md"),
                        StandardCharsets.UTF_8);

        assertFalse(markdown.contains("setPrimaryPostingUUID"));
        assertFalse(markdown.contains("setPostingGroupUUID"));
        assertFalse(markdown.contains("new LedgerPosting(\""));
        assertFalse(markdown.contains("new LedgerEntry(\""));
        assertFalse(markdown.contains("new LedgerProjectionRef(\""));
        assertFalse(Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
                        .matcher(markdown).find());
    }

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
        return client.getLedger().getEntries().stream().filter(entry -> entry.getType() == LedgerEntryType.SPIN_OFF)
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

    private void assertSpinOffScenarioClient(Client client)
    {
        LedgerProjectionService.materialize(client);

        var entry = spinOffEntry(client);

        assertThat(client.getLedger().getEntries().size(), is(3));
        assertThat(client.getSecurities().stream().filter(security -> "DE0007236101".equals(security.getIsin()))
                        .count(), is(1L));
        assertThat(client.getSecurities().stream().filter(security -> "Siemens Energy AG".equals(security.getName()))
                        .count(), is(1L));
        assertThat(client.getPortfolios().get(0).getReferenceAccount(), is(client.getAccounts().get(0)));
        assertThat(entry.getType(), is(LedgerEntryType.SPIN_OFF));
        assertThat(entry.getUpdatedAt(), is(UPDATED_AT));
        assertThat(entry.getPostings().size(), is(6));
        assertThat(entry.getProjectionRefs().size(), is(4));
        assertProjectionTargets(entry, LedgerProjectionRole.OLD_SECURITY_LEG,
                        securityPosting(entry, siemens(client), CorporateActionLeg.SOURCE_SECURITY.getCode(),
                                        siemensEnergy(client)),
                        null);
        assertProjectionTargets(entry, LedgerProjectionRole.DELIVERY_INBOUND,
                        securityPosting(entry, siemens(client), CorporateActionLeg.TARGET_SECURITY.getCode(),
                                        siemens(client)),
                        null);
        assertProjectionTargets(entry, LedgerProjectionRole.NEW_SECURITY_LEG,
                        securityPosting(entry, siemensEnergy(client), CorporateActionLeg.TARGET_SECURITY.getCode(),
                                        siemensEnergy(client)),
                        null);
        assertProjectionTargets(entry, LedgerProjectionRole.CASH_COMPENSATION,
                        primaryPosting(entry, LedgerProjectionRole.CASH_COMPENSATION),
                        primaryPosting(entry, LedgerProjectionRole.CASH_COMPENSATION));
        assertThat(client.getPortfolios().get(0).getTransactions().size(), is(4));
        assertThat(client.getAccounts().get(0).getTransactions().size(), is(3));
        assertThat(buyProjection(client, siemens(client)).getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(portfolioProjection(client.getPortfolios().get(0), LedgerProjectionRole.OLD_SECURITY_LEG).getType(),
                        is(PortfolioTransaction.Type.DELIVERY_OUTBOUND));
        assertSame(siemens(client),
                        portfolioProjection(client.getPortfolios().get(0), LedgerProjectionRole.OLD_SECURITY_LEG)
                                        .getSecurity());
        assertThat(portfolioProjection(client.getPortfolios().get(0), LedgerProjectionRole.OLD_SECURITY_LEG)
                        .getShares(),
                        is(Values.Share.factorize(10)));
        assertThat(portfolioProjection(client.getPortfolios().get(0), LedgerProjectionRole.DELIVERY_INBOUND,
                        siemens(client)).getType(), is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertSame(siemens(client),
                        portfolioProjection(client.getPortfolios().get(0), LedgerProjectionRole.DELIVERY_INBOUND,
                                        siemens(client)).getSecurity());
        assertThat(portfolioProjection(client.getPortfolios().get(0), LedgerProjectionRole.DELIVERY_INBOUND,
                        siemens(client)).getShares(),
                        is(Values.Share.factorize(10)));
        assertThat(portfolioProjection(client.getPortfolios().get(0), LedgerProjectionRole.DELIVERY_INBOUND,
                        siemens(client)).getAmount(), is(portfolioProjection(client.getPortfolios().get(0),
                                        LedgerProjectionRole.OLD_SECURITY_LEG).getAmount()));
        assertThat(portfolioProjection(client.getPortfolios().get(0), LedgerProjectionRole.NEW_SECURITY_LEG).getType(),
                        is(PortfolioTransaction.Type.DELIVERY_INBOUND));
        assertSame(siemensEnergy(client),
                        portfolioProjection(client.getPortfolios().get(0), LedgerProjectionRole.NEW_SECURITY_LEG)
                                        .getSecurity());
        assertThat(portfolioProjection(client.getPortfolios().get(0), LedgerProjectionRole.NEW_SECURITY_LEG)
                        .getShares(),
                        is(Values.Share.factorize(5)));
        assertThat(accountProjection(client.getAccounts().get(0), LedgerProjectionRole.CASH_COMPENSATION).getUnits()
                        .count(), is(2L));
        assertSpinOffSiemensPositionUnchanged(client.getPortfolios().get(0), siemens(client));
        assertTrue(LedgerStructuralValidator.validate(client.getLedger()).isOK());
    }

    private void assertSpinOffSiemensPositionUnchanged(Portfolio portfolio, Security siemens)
    {
        var oldLeg = portfolioProjection(portfolio, LedgerProjectionRole.OLD_SECURITY_LEG);
        var retainedLeg = portfolioProjection(portfolio, LedgerProjectionRole.DELIVERY_INBOUND, siemens);

        assertThat(oldLeg.getShares(), is(Values.Share.factorize(10)));
        assertThat(retainedLeg.getShares(), is(Values.Share.factorize(10)));
        assertThat(retainedLeg.getShares() - oldLeg.getShares(), is(0L));
    }

    private LedgerPosting posting(LedgerEntry entry, LedgerPostingType type, Account account, long amount)
    {
        return entry.getPostings().stream().filter(posting -> posting.getType() == type)
                        .filter(posting -> posting.getAccount() == account).filter(posting -> posting.getAmount() == amount)
                        .findFirst().orElseThrow();
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

    private void assertProjectionTargets(LedgerEntry entry, LedgerProjectionRole role, LedgerPosting primaryPosting,
                    LedgerPosting postingGroup)
    {
        var projection = projection(entry, role);
        assertThat(projection.getRole(), is(role));
        assertThat(primaryPostingUUID(projection), is(primaryPosting.getUUID()));
        assertThat(postingGroupUUID(projection), is(postingGroup != null ? postingGroup.getUUID() : null));
    }

    private String primaryPostingUUID(LedgerProjectionRef projection)
    {
        return projection.getPrimaryMembership().map(ProjectionMembership::getPostingUUID)
                        .orElse(projection.getPrimaryPostingUUID());
    }

    private String postingGroupUUID(LedgerProjectionRef projection)
    {
        return projection.getMembershipsByRole(ProjectionMembershipRole.GROUP_ANCHOR).stream().findFirst()
                        .map(ProjectionMembership::getPostingUUID).orElse(projection.getPostingGroupUUID());
    }

    private PortfolioTransaction portfolioProjection(Portfolio portfolio, LedgerProjectionRole role)
    {
        return portfolio.getTransactions().stream().filter(LedgerBackedTransaction.class::isInstance)
                        .filter(transaction -> ((LedgerBackedTransaction) transaction).getLedgerProjectionRef()
                                        .getRole() == role)
                        .findFirst().orElseThrow();
    }

    private PortfolioTransaction portfolioProjection(Portfolio portfolio, LedgerProjectionRole role, Security security)
    {
        return portfolio.getTransactions().stream().filter(LedgerBackedTransaction.class::isInstance)
                        .filter(transaction -> ((LedgerBackedTransaction) transaction).getLedgerProjectionRef()
                                        .getRole() == role)
                        .filter(transaction -> transaction.getSecurity() == security).findFirst().orElseThrow();
    }

    private AccountTransaction accountProjection(Account account, LedgerProjectionRole role)
    {
        return account.getTransactions().stream().filter(LedgerBackedTransaction.class::isInstance)
                        .filter(transaction -> ((LedgerBackedTransaction) transaction).getLedgerProjectionRef()
                                        .getRole() == role)
                        .findFirst().orElseThrow();
    }

    private AccountTransaction accountProjection(LedgerEntry entry)
    {
        var accountProjection = projection(entry, LedgerProjectionRole.ACCOUNT);

        return accountProjection.getAccount().getTransactions().stream().filter(LedgerBackedTransaction.class::isInstance)
                        .filter(transaction -> ((LedgerBackedTransaction) transaction).getLedgerEntry() == entry)
                        .filter(transaction -> ((LedgerBackedTransaction) transaction).getLedgerProjectionRef()
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

    private LedgerProjectionRef projection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream().filter(ref -> ref.getRole() == role).findFirst().orElseThrow();
    }

    private LedgerPosting primaryPosting(LedgerEntry entry, LedgerProjectionRole role)
    {
        return LedgerProjectionSupport.primaryPosting(entry, projection(entry, role));
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

    private byte[] saveProtobuf(Client client) throws Exception
    {
        var stream = new ByteArrayOutputStream();
        var writer = protobufWriter();
        var save = writer.getClass().getDeclaredMethod("save", Client.class, java.io.OutputStream.class);

        save.setAccessible(true);
        invoke(save, writer, client, stream);

        return stream.toByteArray();
    }

    private Client loadProtobuf(byte[] bytes) throws Exception
    {
        var writer = protobufWriter();
        var load = writer.getClass().getDeclaredMethod("load", java.io.InputStream.class);

        load.setAccessible(true);
        return (Client) invoke(load, writer, new ByteArrayInputStream(bytes));
    }

    private Object protobufWriter() throws Exception
    {
        var type = Class.forName("name.abuchen.portfolio.model.ProtobufWriter");
        var constructor = type.getDeclaredConstructor();

        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private Object invoke(java.lang.reflect.Method method, Object target, Object... args) throws Exception
    {
        try
        {
            return method.invoke(target, args);
        }
        catch (InvocationTargetException e)
        {
            var cause = e.getCause();

            if (cause instanceof Exception exception)
                throw exception;
            if (cause instanceof Error error)
                throw error;

            throw new AssertionError(cause);
        }
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
        var current = Path.of("").toAbsolutePath();

        while (current != null)
        {
            var candidate = current.resolve(XML_EXAMPLE);

            if (Files.exists(candidate))
                return candidate;

            current = current.getParent();
        }

        return Path.of("").toAbsolutePath().resolve(XML_EXAMPLE);
    }

    private record SpinOffFixture(Client client, Account account, Portfolio portfolio, Security siemens,
                    Security siemensEnergy)
    {
    }
}
