package name.abuchen.portfolio.model.ledger.projection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerParameter;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerPostingDirection;
import name.abuchen.portfolio.model.ledger.LedgerPostingSemanticRole;
import name.abuchen.portfolio.model.ledger.LedgerPostingUnitRole;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerTransactionMetadata;
import name.abuchen.portfolio.model.ledger.ProjectionMembershipRole;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerAccountCashLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCashTransferLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerCreationUnits;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerDeliveryLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioSecurityLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferLeg;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerPortfolioTransferSecurity;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerSecurityQuantity;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerTransactionCreator;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Tests runtime-only projection descriptors derived from posting semantics.
 * Current projection refs remain the active materialization source in this phase.
 */
@SuppressWarnings("nls")
public class DerivedProjectionDescriptorServiceTest
{
    private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 1, 2, 0, 0);

    @Test
    public void testAccountOnlyDescriptorMatchesProjectionRef()
    {
        var client = new Client();
        var account = account("Cash");
        var entry = creator(client).createDeposit(metadata(), LedgerAccountCashLeg.of(account, money(100))).getEntry();

        var descriptors = descriptors(entry);

        assertThat(descriptors.size(), is(1));
        assertMatchesProjectionRef(entry, descriptors.get(0), entry.getProjectionRefs().get(0));
    }

    @Test
    public void testBuySellDescriptorsMatchAccountAndPortfolioProjectionRefs()
    {
        var client = new Client();
        var account = account("Cash");
        var portfolio = portfolio("Portfolio");
        var entry = creator(client).createBuy(metadata(), LedgerAccountCashLeg.of(account, money(100)),
                        LedgerPortfolioSecurityLeg.of(portfolio,
                                        LedgerSecurityQuantity.of(security("Security"), Values.Share.factorize(5)),
                                        money(100)),
                        LedgerCreationUnits.none()).getEntry();

        var descriptors = descriptors(entry);

        assertThat(roles(descriptors), is(Set.of(LedgerProjectionRole.ACCOUNT, LedgerProjectionRole.PORTFOLIO)));
        assertMatchesProjectionRef(entry, descriptor(descriptors, LedgerProjectionRole.ACCOUNT),
                        projection(entry, LedgerProjectionRole.ACCOUNT));
        assertMatchesProjectionRef(entry, descriptor(descriptors, LedgerProjectionRole.PORTFOLIO),
                        projection(entry, LedgerProjectionRole.PORTFOLIO));

        entry = creator(client).createSell(metadata(), LedgerAccountCashLeg.of(account, money(100)),
                        LedgerPortfolioSecurityLeg.of(portfolio,
                                        LedgerSecurityQuantity.of(security("Security"), Values.Share.factorize(5)),
                                        money(100)),
                        LedgerCreationUnits.none()).getEntry();

        descriptors = descriptors(entry);

        assertThat(roles(descriptors), is(Set.of(LedgerProjectionRole.ACCOUNT, LedgerProjectionRole.PORTFOLIO)));
        assertMatchesProjectionRef(entry, descriptor(descriptors, LedgerProjectionRole.ACCOUNT),
                        projection(entry, LedgerProjectionRole.ACCOUNT));
        assertMatchesProjectionRef(entry, descriptor(descriptors, LedgerProjectionRole.PORTFOLIO),
                        projection(entry, LedgerProjectionRole.PORTFOLIO));
    }

    @Test
    public void testDeliveryDescriptorMatchesProjectionRef()
    {
        var client = new Client();
        var portfolio = portfolio("Portfolio");
        var entry = creator(client).createInboundDelivery(metadata(),
                        LedgerDeliveryLeg.of(portfolio,
                                        LedgerSecurityQuantity.of(security("Security"), Values.Share.factorize(5)),
                                        money(100)))
                        .getEntry();

        var descriptors = descriptors(entry);

        assertThat(roles(descriptors), is(Set.of(LedgerProjectionRole.DELIVERY_INBOUND)));
        assertMatchesProjectionRef(entry, descriptors.get(0), projection(entry, LedgerProjectionRole.DELIVERY_INBOUND));

        entry = creator(client).createOutboundDelivery(metadata(),
                        LedgerDeliveryLeg.of(portfolio,
                                        LedgerSecurityQuantity.of(security("Security"), Values.Share.factorize(5)),
                                        money(100)))
                        .getEntry();

        descriptors = descriptors(entry);

        assertThat(roles(descriptors), is(Set.of(LedgerProjectionRole.DELIVERY_OUTBOUND)));
        assertMatchesProjectionRef(entry, descriptors.get(0),
                        projection(entry, LedgerProjectionRole.DELIVERY_OUTBOUND));
    }

    @Test
    public void testCashTransferDerivesSourceAndTargetWithoutPostingOrder()
    {
        var client = new Client();
        var source = account("Source");
        var target = account("Target");
        var entry = creator(client).createAccountTransfer(metadata(), LedgerCashTransferLeg.of(source, money(100)),
                        LedgerCashTransferLeg.of(target, money(100))).getEntry();

        moveFirstPostingToEnd(entry);

        var descriptors = descriptors(entry);

        assertSame(source, descriptor(descriptors, LedgerProjectionRole.SOURCE_ACCOUNT).getAccount());
        assertSame(target, descriptor(descriptors, LedgerProjectionRole.TARGET_ACCOUNT).getAccount());
        assertMatchesProjectionRef(entry, descriptor(descriptors, LedgerProjectionRole.SOURCE_ACCOUNT),
                        projection(entry, LedgerProjectionRole.SOURCE_ACCOUNT));
        assertMatchesProjectionRef(entry, descriptor(descriptors, LedgerProjectionRole.TARGET_ACCOUNT),
                        projection(entry, LedgerProjectionRole.TARGET_ACCOUNT));
    }

    @Test
    public void testSecurityTransferDerivesSourceAndTargetWithoutPostingOrder()
    {
        var client = new Client();
        var source = portfolio("Source");
        var target = portfolio("Target");
        var security = security("Security");
        var entry = creator(client).createPortfolioTransfer(metadata(),
                        LedgerPortfolioTransferSecurity.of(security, Values.Share.factorize(5)),
                        LedgerPortfolioTransferLeg.of(source, money(100)),
                        LedgerPortfolioTransferLeg.of(target, money(100))).getEntry();

        moveFirstPostingToEnd(entry);

        var descriptors = descriptors(entry);

        assertSame(source, descriptor(descriptors, LedgerProjectionRole.SOURCE_PORTFOLIO).getPortfolio());
        assertSame(target, descriptor(descriptors, LedgerProjectionRole.TARGET_PORTFOLIO).getPortfolio());
        assertMatchesProjectionRef(entry, descriptor(descriptors, LedgerProjectionRole.SOURCE_PORTFOLIO),
                        projection(entry, LedgerProjectionRole.SOURCE_PORTFOLIO));
        assertMatchesProjectionRef(entry, descriptor(descriptors, LedgerProjectionRole.TARGET_PORTFOLIO),
                        projection(entry, LedgerProjectionRole.TARGET_PORTFOLIO));
    }

    @Test
    public void testSiemensSpinOffDescriptorsMatchTargetedProjectionRefs()
    {
        var fixture = fixture();
        var entry = spinOffEntry(fixture);

        var descriptors = descriptors(entry);

        assertThat(roles(descriptors), is(Set.of(LedgerProjectionRole.OLD_SECURITY_LEG,
                        LedgerProjectionRole.DELIVERY_INBOUND, LedgerProjectionRole.NEW_SECURITY_LEG,
                        LedgerProjectionRole.CASH_COMPENSATION)));
        assertMatchesProjectionRef(entry, descriptor(descriptors, LedgerProjectionRole.OLD_SECURITY_LEG),
                        projection(entry, LedgerProjectionRole.OLD_SECURITY_LEG));
        assertMatchesProjectionRef(entry, descriptor(descriptors, LedgerProjectionRole.DELIVERY_INBOUND),
                        projection(entry, LedgerProjectionRole.DELIVERY_INBOUND));
        assertMatchesProjectionRef(entry, descriptor(descriptors, LedgerProjectionRole.NEW_SECURITY_LEG),
                        projection(entry, LedgerProjectionRole.NEW_SECURITY_LEG));
        assertMatchesProjectionRef(entry, descriptor(descriptors, LedgerProjectionRole.CASH_COMPENSATION),
                        projection(entry, LedgerProjectionRole.CASH_COMPENSATION));
    }

    @Test
    public void testNativeCorporateActionSmokeDescriptors()
    {
        assertThat(roles(descriptors(nativeSinglePortfolioEntry(LedgerEntryType.STOCK_DIVIDEND,
                        CorporateActionLeg.TARGET_SECURITY))), is(Set.of(LedgerProjectionRole.DELIVERY_INBOUND)));
        assertThat(roles(descriptors(nativeSinglePortfolioEntry(LedgerEntryType.BONUS_ISSUE,
                        CorporateActionLeg.TARGET_SECURITY))), is(Set.of(LedgerProjectionRole.DELIVERY_INBOUND)));
        assertThat(roles(descriptors(nativeSinglePortfolioEntry(LedgerEntryType.RIGHTS_DISTRIBUTION,
                        CorporateActionLeg.RIGHT_SECURITY))), is(Set.of(LedgerProjectionRole.NEW_SECURITY_LEG)));
        assertThat(roles(descriptors(bondConversionEntry())),
                        is(Set.of(LedgerProjectionRole.OLD_SECURITY_LEG, LedgerProjectionRole.NEW_SECURITY_LEG)));
    }

    @Test
    public void testDuplicateSemanticPrimaryIsReportedAsAmbiguous()
    {
        var entry = new LedgerEntry("entry-1");
        var account = account("Cash");

        entry.setType(LedgerEntryType.DEPOSIT);
        entry.setDateTime(DATE_TIME);
        entry.addPosting(primaryCash("posting-1", account));
        entry.addPosting(primaryCash("posting-2", account));

        var result = new DerivedProjectionDescriptorService().derive(entry);

        assertFalse(result.isOK());
        assertThat(result.getDescriptors().size(), is(0));
        assertThat(result.getDiagnostics().get(0).getCode(),
                        is(DerivedProjectionDescriptorService.Diagnostic.IssueCode.AMBIGUOUS_SEMANTIC_PRIMARY));
        assertThat(result.getDiagnostics().get(0).getPostingUUIDs(), is(List.of("posting-1", "posting-2")));
    }

    @Test
    public void testMissingSemanticPrimaryIsReported()
    {
        var entry = new LedgerEntry("entry-1");
        var account = account("Cash");
        var posting = primaryCash("posting-1", account);

        posting.setUnitRole(null);
        entry.setType(LedgerEntryType.DEPOSIT);
        entry.setDateTime(DATE_TIME);
        entry.addPosting(posting);

        var result = new DerivedProjectionDescriptorService().derive(entry);

        assertFalse(result.isOK());
        assertThat(result.getDescriptors().size(), is(0));
        assertThat(result.getDiagnostics().get(0).getCode(),
                        is(DerivedProjectionDescriptorService.Diagnostic.IssueCode.MISSING_SEMANTIC_PRIMARY));
    }

    private List<DerivedProjectionDescriptor> descriptors(LedgerEntry entry)
    {
        var result = new DerivedProjectionDescriptorService().derive(entry);

        assertTrue(result.formatDiagnostics(), result.isOK());

        return result.getDescriptors();
    }

    private void assertMatchesProjectionRef(LedgerEntry entry, DerivedProjectionDescriptor descriptor,
                    LedgerProjectionRef projectionRef)
    {
        assertSame(entry, descriptor.getEntry());
        assertThat(descriptor.getRole(), is(projectionRef.getRole()));
        assertSame(LedgerProjectionSupport.primaryPosting(entry, projectionRef), descriptor.getPrimaryPosting());

        if (descriptor.getViewKind() == DerivedProjectionViewKind.ACCOUNT)
            assertSame(projectionRef.getAccount(), descriptor.getAccount());
        else
            assertSame(projectionRef.getPortfolio(), descriptor.getPortfolio());
    }

    private Set<LedgerProjectionRole> roles(List<DerivedProjectionDescriptor> descriptors)
    {
        return descriptors.stream().map(DerivedProjectionDescriptor::getRole).collect(Collectors.toSet());
    }

    private DerivedProjectionDescriptor descriptor(List<DerivedProjectionDescriptor> descriptors,
                    LedgerProjectionRole role)
    {
        return descriptors.stream().filter(descriptor -> descriptor.getRole() == role).findFirst().orElseThrow();
    }

    private LedgerProjectionRef projection(LedgerEntry entry, LedgerProjectionRole role)
    {
        return entry.getProjectionRefs().stream().filter(ref -> ref.getRole() == role).findFirst().orElseThrow();
    }

    private void annotateFromProjectionRefs(LedgerEntry entry)
    {
        for (var ref : entry.getProjectionRefs())
        {
            var primary = LedgerProjectionSupport.primaryPosting(entry, ref);

            markPrimary(primary, ref.getRole());

            for (var membership : ref.getMemberships())
                markUnitPosting(entry, membership.getPostingUUID(), membership.getRole(), primary.getGroupKey());
        }
    }

    private void markPrimary(LedgerPosting posting, LedgerProjectionRole role)
    {
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        posting.setLocalKey(role.name());

        if (posting.getAccount() != null)
            posting.setSemanticRole(LedgerPostingSemanticRole.CASH);
        else if (posting.getPortfolio() != null)
            posting.setSemanticRole(LedgerPostingSemanticRole.SECURITY);

        posting.setDirection(direction(role));
        posting.setCorporateActionLeg(corporateActionLeg(posting));
    }

    private void markUnitPosting(LedgerEntry entry, String postingUUID, ProjectionMembershipRole role, String groupKey)
    {
        var posting = entry.getPostings().stream().filter(candidate -> postingUUID.equals(candidate.getUUID()))
                        .findFirst().orElseThrow();

        if (posting.getUnitRole() == LedgerPostingUnitRole.PRIMARY)
            return;

        posting.setGroupKey(groupKey);
        posting.setUnitRole(switch (role)
        {
            case FEE_UNIT -> LedgerPostingUnitRole.FEE;
            case TAX_UNIT -> LedgerPostingUnitRole.TAX;
            case GROSS_VALUE_UNIT -> LedgerPostingUnitRole.GROSS_VALUE;
            default -> posting.getUnitRole();
        });
    }

    private LedgerPostingDirection direction(LedgerProjectionRole role)
    {
        return switch (role)
        {
            case SOURCE_ACCOUNT, SOURCE_PORTFOLIO, OLD_SECURITY_LEG, DELIVERY_OUTBOUND -> LedgerPostingDirection.OUTBOUND;
            case TARGET_ACCOUNT, TARGET_PORTFOLIO, NEW_SECURITY_LEG, DELIVERY, DELIVERY_INBOUND ->
                LedgerPostingDirection.INBOUND;
            default -> LedgerPostingDirection.NEUTRAL;
        };
    }

    private CorporateActionLeg corporateActionLeg(LedgerPosting posting)
    {
        return posting.getParameters().stream() //
                        .filter(parameter -> parameter.getType() == LedgerParameterType.CORPORATE_ACTION_LEG) //
                        .map(LedgerParameter::getValue) //
                        .filter(String.class::isInstance) //
                        .map(String.class::cast) //
                        .map(this::corporateActionLeg) //
                        .findFirst().orElse(null);
    }

    private CorporateActionLeg corporateActionLeg(String code)
    {
        for (var leg : CorporateActionLeg.values())
            if (leg.getCode().equals(code))
                return leg;

        throw new IllegalArgumentException(code);
    }

    private void moveFirstPostingToEnd(LedgerEntry entry)
    {
        var posting = entry.getPostings().get(0);

        entry.removePosting(posting);
        entry.addPosting(posting);
    }

    private LedgerEntry spinOffEntry(Fixture fixture)
    {
        var entry = new LedgerEntry("spin-off");
        var oldLeg = portfolioPosting("old-siemens", fixture.portfolio, fixture.siemens, 10, 100,
                        CorporateActionLeg.SOURCE_SECURITY, LedgerProjectionRole.OLD_SECURITY_LEG);
        var retainedLeg = portfolioPosting("retained-siemens", fixture.portfolio, fixture.siemens, 10, 100,
                        CorporateActionLeg.TARGET_SECURITY, LedgerProjectionRole.DELIVERY_INBOUND);
        var newLeg = portfolioPosting("new-siemens-energy", fixture.portfolio, fixture.siemensEnergy, 5, 50,
                        CorporateActionLeg.TARGET_SECURITY, LedgerProjectionRole.NEW_SECURITY_LEG);
        var compensation = accountPosting("cash-compensation", fixture.account, 5,
                        CorporateActionLeg.CASH_COMPENSATION, LedgerProjectionRole.CASH_COMPENSATION);
        var fee = unitPosting("fee", LedgerPostingType.FEE, 2, LedgerPostingUnitRole.FEE);
        var tax = unitPosting("tax", LedgerPostingType.TAX, 1, LedgerPostingUnitRole.TAX);

        entry.setType(LedgerEntryType.SPIN_OFF);
        entry.setDateTime(DATE_TIME);
        entry.addPosting(oldLeg);
        entry.addPosting(retainedLeg);
        entry.addPosting(newLeg);
        entry.addPosting(compensation);
        entry.addPosting(fee);
        entry.addPosting(tax);
        entry.addProjectionRef(portfolioProjection(LedgerProjectionRole.OLD_SECURITY_LEG, fixture.portfolio, oldLeg));
        entry.addProjectionRef(portfolioProjection(LedgerProjectionRole.DELIVERY_INBOUND, fixture.portfolio,
                        retainedLeg));
        entry.addProjectionRef(portfolioProjection(LedgerProjectionRole.NEW_SECURITY_LEG, fixture.portfolio, newLeg));
        var cashProjection = accountProjection(LedgerProjectionRole.CASH_COMPENSATION, fixture.account, compensation);
        cashProjection.setPostingGroup(compensation);
        cashProjection.addMembership(fee.getUUID(), ProjectionMembershipRole.FEE_UNIT);
        cashProjection.addMembership(tax.getUUID(), ProjectionMembershipRole.TAX_UNIT);
        entry.addProjectionRef(cashProjection);
        fee.setGroupKey(compensation.getGroupKey());
        tax.setGroupKey(compensation.getGroupKey());

        return entry;
    }

    private LedgerEntry nativeSinglePortfolioEntry(LedgerEntryType type, CorporateActionLeg leg)
    {
        var fixture = fixture();
        var role = type == LedgerEntryType.RIGHTS_DISTRIBUTION ? LedgerProjectionRole.NEW_SECURITY_LEG
                        : LedgerProjectionRole.DELIVERY_INBOUND;
        var entry = new LedgerEntry(type.name());
        var posting = portfolioPosting("primary", fixture.portfolio, fixture.siemensEnergy, 5, 50, leg, role);

        entry.setType(type);
        entry.setDateTime(DATE_TIME);
        entry.addPosting(posting);

        return entry;
    }

    private LedgerEntry bondConversionEntry()
    {
        var fixture = fixture();
        var entry = new LedgerEntry("bond-conversion");
        var source = portfolioPosting("bond", fixture.portfolio, fixture.siemens, 10, 100,
                        CorporateActionLeg.CONVERSION_SOURCE, LedgerProjectionRole.OLD_SECURITY_LEG);
        var target = portfolioPosting("share", fixture.portfolio, fixture.siemensEnergy, 5, 50,
                        CorporateActionLeg.CONVERSION_TARGET, LedgerProjectionRole.NEW_SECURITY_LEG);

        entry.setType(LedgerEntryType.BOND_CONVERSION);
        entry.setDateTime(DATE_TIME);
        entry.addPosting(source);
        entry.addPosting(target);

        return entry;
    }

    private LedgerPosting primaryCash(String uuid, Account account)
    {
        var posting = new LedgerPosting(uuid);

        posting.setType(LedgerPostingType.CASH);
        posting.setAccount(account);
        posting.setAmount(Values.Amount.factorize(100));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(LedgerPostingSemanticRole.CASH);
        posting.setDirection(LedgerPostingDirection.NEUTRAL);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);

        return posting;
    }

    private LedgerPosting portfolioPosting(String uuid, Portfolio portfolio, Security security, int shares, int amount,
                    CorporateActionLeg leg, LedgerProjectionRole role)
    {
        var posting = new LedgerPosting(uuid);

        posting.setType(LedgerPostingType.SECURITY);
        posting.setPortfolio(portfolio);
        posting.setSecurity(security);
        posting.setShares(Values.Share.factorize(shares));
        posting.setAmount(Values.Amount.factorize(amount));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(LedgerPostingSemanticRole.SECURITY);
        posting.setDirection(direction(role));
        posting.setCorporateActionLeg(leg);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        posting.setGroupKey(role.name());
        posting.setLocalKey(role.name());
        posting.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_LEG, leg.getCode()));

        return posting;
    }

    private LedgerPosting accountPosting(String uuid, Account account, int amount, CorporateActionLeg leg,
                    LedgerProjectionRole role)
    {
        var posting = new LedgerPosting(uuid);

        posting.setType(LedgerPostingType.CASH_COMPENSATION);
        posting.setAccount(account);
        posting.setAmount(Values.Amount.factorize(amount));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(LedgerPostingSemanticRole.CASH_COMPENSATION);
        posting.setDirection(direction(role));
        posting.setCorporateActionLeg(leg);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        posting.setGroupKey(role.name());
        posting.setLocalKey(role.name());
        posting.addParameter(LedgerParameter.ofString(LedgerParameterType.CORPORATE_ACTION_LEG, leg.getCode()));

        return posting;
    }

    private LedgerPosting unitPosting(String uuid, LedgerPostingType type, int amount, LedgerPostingUnitRole role)
    {
        var posting = new LedgerPosting(uuid);

        posting.setType(type);
        posting.setAmount(Values.Amount.factorize(amount));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(type == LedgerPostingType.FEE ? LedgerPostingSemanticRole.FEE
                        : LedgerPostingSemanticRole.TAX);
        posting.setUnitRole(role);

        return posting;
    }

    private LedgerProjectionRef accountProjection(LedgerProjectionRole role, Account account, LedgerPosting posting)
    {
        var projection = new LedgerProjectionRef(role.name());

        projection.setRole(role);
        projection.setAccount(account);
        projection.setPrimaryPosting(posting);

        return projection;
    }

    private LedgerProjectionRef portfolioProjection(LedgerProjectionRole role, Portfolio portfolio,
                    LedgerPosting posting)
    {
        var projection = new LedgerProjectionRef(role.name());

        projection.setRole(role);
        projection.setPortfolio(portfolio);
        projection.setPrimaryPosting(posting);

        return projection;
    }

    private LedgerTransactionCreator creator(Client client)
    {
        return new LedgerTransactionCreator(client);
    }

    private LedgerTransactionMetadata metadata()
    {
        return LedgerTransactionMetadata.of(DATE_TIME).withNote("note").withSource("source");
    }

    private Account account(String name)
    {
        var account = new Account();

        account.setName(name);
        account.setCurrencyCode(CurrencyUnit.EUR);

        return account;
    }

    private Portfolio portfolio(String name)
    {
        var portfolio = new Portfolio();

        portfolio.setName(name);

        return portfolio;
    }

    private Security security(String name)
    {
        return new Security(name, CurrencyUnit.EUR);
    }

    private Money money(int amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    private Fixture fixture()
    {
        return new Fixture(account("Cash"), portfolio("Portfolio"), security("Siemens AG"),
                        security("Siemens Energy AG"));
    }

    private record Fixture(Account account, Portfolio portfolio, Security siemens, Security siemensEnergy)
    {
    }
}
