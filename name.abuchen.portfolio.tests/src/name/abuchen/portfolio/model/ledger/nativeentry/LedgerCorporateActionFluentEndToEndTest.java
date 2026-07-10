package name.abuchen.portfolio.model.ledger.nativeentry;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.ProtobufTestUtilities;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerMutationContext;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.LedgerStructuralValidator;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionBasisMethod;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionBasisStatus;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionKind;
import name.abuchen.portfolio.model.ledger.configuration.CorporateActionLeg;
import name.abuchen.portfolio.model.ledger.configuration.LedgerLegRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerNativeEntryDefinitionValidator;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.projection.DerivedProjectionDescriptor;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedAccountTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedPortfolioTransaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionService;
import name.abuchen.portfolio.model.ledger.projection.LedgerProjectionSupport;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class LedgerCorporateActionFluentEndToEndTest
{
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 1, 2, 0, 0);
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");

    @Test
    public void testCashDistributionFluentCreateEditSaveLoadDelete() throws Exception
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.CASH_DISTRIBUTION) //
                        .date(DATE) //
                        .securityContext("context-1", "cash-1", fixture.portfolio, fixture.source) //
                        .cash("cash-1", "cash-1", fixture.account, money(10)) //
                        .cash("cash-2", "cash-2", fixture.secondAccount, money(20)) //
                        .fee("fee-1", fixture.account, money(2), "cash-1") //
                        .tax("tax-1", fixture.account, money(1), "cash-1") //
                        .buildAndAdd().getEntry();

        exerciseEndToEnd(fixture, entry,
                        (client, candidate) -> LedgerCorporateActionView.of(candidate).cash()
                                        .withAccount(account(client, "Second Cash Account")).single(),
                        LedgerLegRole.CASH_LEG, "cash-2", "cash-2",
                        posting -> setAmount(posting, 42),
                        reloaded -> {
                            assertAmount(reloaded, LedgerLegRole.CASH_LEG, "cash-2", "cash-2", 42);
                            assertProjection(reloaded, LedgerProjectionRole.ACCOUNT, "cash-1");
                            assertProjection(reloaded, LedgerProjectionRole.ACCOUNT, "cash-2");
                        });
    }

    @Test
    public void testStockDividendFluentCreateEditSaveLoadDelete() throws Exception
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.STOCK_DIVIDEND) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.source) //
                        .securityIn("target-1", "main", fixture.portfolio, fixture.target, shares(5)) //
                        .securityIn("target-2", "main", fixture.secondPortfolio, fixture.secondTarget, shares(3)) //
                        .cash("cash-1", "cash-1", fixture.account, money(1)) //
                        .basis(CorporateActionBasisStatus.UNKNOWN) //
                        .buildAndAdd().getEntry();

        exerciseEndToEnd(fixture, entry,
                        (client, candidate) -> LedgerCorporateActionView.of(candidate).securityIn()
                                        .withPortfolio(portfolio(client, "Second Portfolio"))
                                        .withSecurity(security(client, "Second Target AG")).single(),
                        LedgerLegRole.TARGET_SECURITY_LEG, "target-2", "main",
                        posting -> posting.setShares(shares(9)),
                        reloaded -> {
                            assertShares(reloaded, LedgerLegRole.TARGET_SECURITY_LEG, "target-2", "main", 9);
                            assertProjection(reloaded, LedgerProjectionRole.NEW_SECURITY_LEG, "target-1");
                            assertProjection(reloaded, LedgerProjectionRole.NEW_SECURITY_LEG, "target-2");
                            assertProjection(reloaded, LedgerProjectionRole.CASH_COMPENSATION, "cash-1");
                        });
    }

    @Test
    public void testSpinOffFluentCreateEditSaveLoadDelete() throws Exception
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.SPIN_OFF) //
                        .date(DATE) //
                        .effectiveDate(DATE.toLocalDate()) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.source) //
                        .securityOut("source-1", "main", fixture.portfolio, fixture.source, shares(10)) //
                        .securityIn("target-1", "main", fixture.portfolio, fixture.target, shares(4)) //
                        .securityIn("target-2", "main", fixture.secondPortfolio, fixture.secondTarget, shares(2)) //
                        .cash("cash-1", "cash-1", fixture.account, money(3)) //
                        .basis(CorporateActionBasisStatus.PROVIDED) //
                        .basisMethod(CorporateActionBasisMethod.PERCENTAGE_ALLOCATION) //
                        .basisPercentageAllocation(LedgerLegRole.SECURITY_CONTEXT_LEG, "context-1", "main",
                                        new BigDecimal("70")) //
                        .basisPercentageAllocation(LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "main",
                                        new BigDecimal("20")) //
                        .basisPercentageAllocation(LedgerLegRole.TARGET_SECURITY_LEG, "target-2", "main",
                                        new BigDecimal("10")) //
                        .buildAndAdd().getEntry();

        exerciseEndToEnd(fixture, entry,
                        (client, candidate) -> LedgerCorporateActionView.of(candidate).securityIn()
                                        .withPortfolio(portfolio(client, "Second Portfolio"))
                                        .withSecurity(security(client, "Second Target AG")).single(),
                        LedgerLegRole.TARGET_SECURITY_LEG, "target-2", "main",
                        posting -> posting.setShares(shares(8)),
                        reloaded -> {
                            assertShares(reloaded, LedgerLegRole.TARGET_SECURITY_LEG, "target-2", "main", 8);
                            assertBasisAllocationCount(reloaded, 3);
                            assertProjection(reloaded, LedgerProjectionRole.OLD_SECURITY_LEG, "source-1");
                            assertProjection(reloaded, LedgerProjectionRole.NEW_SECURITY_LEG, "target-1");
                            assertProjection(reloaded, LedgerProjectionRole.NEW_SECURITY_LEG, "target-2");
                            assertProjection(reloaded, LedgerProjectionRole.CASH_COMPENSATION, "cash-1");
                        });
    }

    @Test
    public void testBonusIssueFluentCreateEditSaveLoadDelete() throws Exception
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.BONUS_ISSUE) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.source) //
                        .securityIn("target-1", "main", fixture.portfolio, fixture.target, shares(6)) //
                        .cash("cash-1", "cash-1", fixture.account, money(2)) //
                        .fee("fee-1", fixture.account, money(1), "cash-1") //
                        .tax("tax-1", fixture.account, money(1), "cash-1") //
                        .buildAndAdd().getEntry();

        exerciseEndToEnd(fixture, entry,
                        (client, candidate) -> LedgerCorporateActionView.of(candidate).securityIn()
                                        .withPortfolio(portfolio(client, "Portfolio"))
                                        .withSecurity(security(client, "Target AG")).single(),
                        LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "main",
                        posting -> posting.setShares(shares(7)),
                        reloaded -> {
                            assertShares(reloaded, LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "main", 7);
                            assertProjection(reloaded, LedgerProjectionRole.NEW_SECURITY_LEG, "target-1");
                            assertProjection(reloaded, LedgerProjectionRole.CASH_COMPENSATION, "cash-1");
                        });
    }

    @Test
    public void testRightsDistributionFluentCreateEditSaveLoadDelete() throws Exception
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.RIGHTS_DISTRIBUTION) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.source) //
                        .rightIn("right-1", "main", fixture.portfolio, fixture.right, shares(11)) //
                        .cash("cash-1", "cash-1", fixture.account, money(2)) //
                        .buildAndAdd().getEntry();

        exerciseEndToEnd(fixture, entry,
                        (client, candidate) -> LedgerCorporateActionView.of(candidate).rightsIn()
                                        .withPortfolio(portfolio(client, "Portfolio"))
                                        .withSecurity(security(client, "Right AG")).single(),
                        LedgerLegRole.DISTRIBUTED_RIGHT_LEG, "right-1", "main",
                        posting -> posting.setShares(shares(13)),
                        reloaded -> {
                            assertShares(reloaded, LedgerLegRole.DISTRIBUTED_RIGHT_LEG, "right-1", "main", 13);
                            assertProjection(reloaded, LedgerProjectionRole.NEW_SECURITY_LEG, "right-1");
                            assertProjection(reloaded, LedgerProjectionRole.CASH_COMPENSATION, "cash-1");
                        });
    }

    @Test
    public void testCouponPaymentFluentCreateEditSaveLoadDelete() throws Exception
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.COUPON_PAYMENT) //
                        .date(DATE) //
                        .securityContext("context-1", "coupon-1", fixture.portfolio, fixture.source) //
                        .cash("cash-1", "coupon-1", fixture.account, money(12)) //
                        .accruedInterest("interest-1", "coupon-1", fixture.account, money(12)) //
                        .fee("fee-1", fixture.account, money(2), "coupon-1") //
                        .tax("tax-1", fixture.account, money(1), "coupon-1") //
                        .buildAndAdd().getEntry();

        exerciseEndToEnd(fixture, entry,
                        (client, candidate) -> LedgerCorporateActionView.of(candidate).cash()
                                        .withAccount(account(client, "Cash Account")).withGroupKey("coupon-1")
                                        .single(),
                        LedgerLegRole.CASH_LEG, "cash-1", "coupon-1",
                        posting -> setAmount(posting, 14),
                        reloaded -> {
                            assertAmount(reloaded, LedgerLegRole.CASH_LEG, "cash-1", "coupon-1", 14);
                            assertAmount(reloaded, LedgerLegRole.ACCRUED_INTEREST_LEG, "interest-1", "coupon-1", 12);
                            assertProjection(reloaded, LedgerProjectionRole.ACCOUNT, "cash-1");
                        });
    }

    @Test
    public void testPikInterestFluentCreateEditSaveLoadDelete() throws Exception
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.PIK_INTEREST) //
                        .date(DATE) //
                        .securityContext("context-1", "pik-1", fixture.portfolio, fixture.source) //
                        .securityIn("target-1", "pik-1", fixture.portfolio, fixture.target, shares(5)) //
                        .accruedInterest("interest-1", "pik-1", fixture.account, money(5)) //
                        .cash("cash-1", "cash-1", fixture.account, money(1)) //
                        .basis(CorporateActionBasisStatus.UNKNOWN) //
                        .buildAndAdd().getEntry();

        exerciseEndToEnd(fixture, entry,
                        (client, candidate) -> LedgerCorporateActionView.of(candidate).securityIn()
                                        .withPortfolio(portfolio(client, "Portfolio"))
                                        .withSecurity(security(client, "Target AG")).single(),
                        LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "pik-1",
                        posting -> posting.setShares(shares(6)),
                        reloaded -> {
                            assertShares(reloaded, LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "pik-1", 6);
                            assertAmount(reloaded, LedgerLegRole.ACCRUED_INTEREST_LEG, "interest-1", "pik-1", 5);
                            assertProjection(reloaded, LedgerProjectionRole.NEW_SECURITY_LEG, "target-1");
                            assertProjection(reloaded, LedgerProjectionRole.CASH_COMPENSATION, "cash-1");
                        });
    }

    @Test
    public void testDefaultedInterestFluentCreateEditSaveLoadDelete() throws Exception
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.DEFAULTED_INTEREST) //
                        .date(DATE) //
                        .securityContext("context-1", "claim-1", fixture.portfolio, fixture.source) //
                        .securityIn("claim-1", "claim-1", fixture.portfolio, fixture.claim, shares(3)) //
                        .cash("cash-1", "cash-1", fixture.account, money(4)) //
                        .buildAndAdd().getEntry();

        exerciseEndToEnd(fixture, entry,
                        (client, candidate) -> LedgerCorporateActionView.of(candidate).securityIn()
                                        .withPortfolio(portfolio(client, "Portfolio"))
                                        .withSecurity(security(client, "Claim AG")).single(),
                        LedgerLegRole.TARGET_SECURITY_LEG, "claim-1", "claim-1",
                        posting -> posting.setShares(shares(5)),
                        reloaded -> {
                            assertShares(reloaded, LedgerLegRole.TARGET_SECURITY_LEG, "claim-1", "claim-1", 5);
                            assertProjection(reloaded, LedgerProjectionRole.NEW_SECURITY_LEG, "claim-1");
                            assertProjection(reloaded, LedgerProjectionRole.ACCOUNT, "cash-1");
                        });
    }

    @Test
    public void testMaturityFluentCreateEditSaveLoadDelete() throws Exception
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.MATURITY) //
                        .date(DATE) //
                        .securityContext("context-1", "redemption-1", fixture.portfolio, fixture.bond) //
                        .securityOut("source-1", "redemption-1", fixture.portfolio, fixture.bond, shares(10)) //
                        .cash("cash-1", "redemption-1", fixture.account, money(100)) //
                        .principal("principal-1", "redemption-1", fixture.account, money(100)) //
                        .accruedInterest("interest-1", "redemption-1", fixture.account, money(5)) //
                        .buildAndAdd().getEntry();

        exerciseEndToEnd(fixture, entry,
                        (client, candidate) -> LedgerCorporateActionView.of(candidate).cash()
                                        .withAccount(account(client, "Cash Account")).withGroupKey("redemption-1")
                                        .single(),
                        LedgerLegRole.CASH_LEG, "cash-1", "redemption-1",
                        posting -> setAmount(posting, 105),
                        reloaded -> {
                            assertAmount(reloaded, LedgerLegRole.CASH_LEG, "cash-1", "redemption-1", 105);
                            assertAmount(reloaded, LedgerLegRole.PRINCIPAL_REDEMPTION_LEG, "principal-1",
                                            "redemption-1", 100);
                            assertProjection(reloaded, LedgerProjectionRole.DELIVERY_OUTBOUND, "source-1");
                            assertProjection(reloaded, LedgerProjectionRole.ACCOUNT, "cash-1");
                        });
    }

    @Test
    public void testPartialRedemptionFluentCreateEditSaveLoadDelete() throws Exception
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.PARTIAL_REDEMPTION) //
                        .date(DATE) //
                        .securityContext("context-1", "redemption-1", fixture.portfolio, fixture.bond) //
                        .securityOut("source-1", "redemption-1", fixture.portfolio, fixture.bond, shares(4)) //
                        .cash("cash-1", "redemption-1", fixture.account, money(40)) //
                        .principal("principal-1", "redemption-1", fixture.account, money(40)) //
                        .basis(CorporateActionBasisStatus.UNKNOWN) //
                        .buildAndAdd().getEntry();

        exerciseEndToEnd(fixture, entry,
                        (client, candidate) -> LedgerCorporateActionView.of(candidate).principal()
                                        .withAccount(account(client, "Cash Account")).withGroupKey("redemption-1")
                                        .single(),
                        LedgerLegRole.PRINCIPAL_REDEMPTION_LEG, "principal-1", "redemption-1",
                        posting -> setAmount(posting, 45),
                        reloaded -> {
                            assertAmount(reloaded, LedgerLegRole.PRINCIPAL_REDEMPTION_LEG, "principal-1",
                                            "redemption-1", 45);
                            assertProjection(reloaded, LedgerProjectionRole.DELIVERY_OUTBOUND, "source-1");
                            assertProjection(reloaded, LedgerProjectionRole.ACCOUNT, "cash-1");
                        });
    }

    @Test
    public void testCallFluentCreateEditSaveLoadDelete() throws Exception
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.CALL) //
                        .date(DATE) //
                        .securityContext("context-1", "redemption-1", fixture.portfolio, fixture.bond) //
                        .securityOut("source-1", "redemption-1", fixture.portfolio, fixture.bond, shares(7)) //
                        .cash("cash-1", "redemption-1", fixture.account, money(70)) //
                        .principal("principal-1", "redemption-1", fixture.account, money(70)) //
                        .fee("fee-1", fixture.account, money(1), "redemption-1") //
                        .buildAndAdd().getEntry();

        exerciseEndToEnd(fixture, entry,
                        (client, candidate) -> LedgerCorporateActionView.of(candidate).cash()
                                        .withAccount(account(client, "Cash Account")).withGroupKey("redemption-1")
                                        .single(),
                        LedgerLegRole.CASH_LEG, "cash-1", "redemption-1",
                        posting -> setAmount(posting, 71),
                        reloaded -> {
                            assertAmount(reloaded, LedgerLegRole.CASH_LEG, "cash-1", "redemption-1", 71);
                            assertProjection(reloaded, LedgerProjectionRole.DELIVERY_OUTBOUND, "source-1");
                            assertProjection(reloaded, LedgerProjectionRole.ACCOUNT, "cash-1");
                        });
    }

    @Test
    public void testPutFluentCreateEditSaveLoadDelete() throws Exception
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.PUT) //
                        .date(DATE) //
                        .securityContext("context-1", "redemption-1", fixture.portfolio, fixture.bond) //
                        .securityOut("source-1", "redemption-1", fixture.portfolio, fixture.bond, shares(8)) //
                        .cash("cash-1", "redemption-1", fixture.account, money(80)) //
                        .principal("principal-1", "redemption-1", fixture.account, money(80)) //
                        .tax("tax-1", fixture.account, money(1), "redemption-1") //
                        .buildAndAdd().getEntry();

        exerciseEndToEnd(fixture, entry,
                        (client, candidate) -> LedgerCorporateActionView.of(candidate).securityOut()
                                        .withPortfolio(portfolio(client, "Portfolio"))
                                        .withSecurity(security(client, "Bond AG")).single(),
                        LedgerLegRole.SOURCE_SECURITY_LEG, "source-1", "redemption-1",
                        posting -> posting.setShares(shares(6)),
                        reloaded -> {
                            assertShares(reloaded, LedgerLegRole.SOURCE_SECURITY_LEG, "source-1", "redemption-1", 6);
                            assertProjection(reloaded, LedgerProjectionRole.DELIVERY_OUTBOUND, "source-1");
                            assertProjection(reloaded, LedgerProjectionRole.ACCOUNT, "cash-1");
                        });
    }

    @Test
    public void testConversionFluentCreateEditSaveLoadDelete() throws Exception
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.CONVERSION) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.bond) //
                        .securityOut("source-1", "main", fixture.portfolio, fixture.bond, shares(10)) //
                        .securityIn("target-1", "main", fixture.portfolio, fixture.target, shares(5)) //
                        .cash("cash-1", "cash-1", fixture.account, money(2)) //
                        .basis(CorporateActionBasisStatus.PROVIDED) //
                        .basisMethod(CorporateActionBasisMethod.PERCENTAGE_ALLOCATION) //
                        .basisPercentageAllocation(LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "main",
                                        new BigDecimal("100")) //
                        .buildAndAdd().getEntry();

        exerciseEndToEnd(fixture, entry,
                        (client, candidate) -> LedgerCorporateActionView.of(candidate).securityIn()
                                        .withPortfolio(portfolio(client, "Portfolio"))
                                        .withSecurity(security(client, "Target AG")).single(),
                        LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "main",
                        posting -> posting.setShares(shares(6)),
                        reloaded -> {
                            assertShares(reloaded, LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "main", 6);
                            assertBasisAllocationCount(reloaded, 1);
                            assertProjection(reloaded, LedgerProjectionRole.DELIVERY_OUTBOUND, "source-1");
                            assertProjection(reloaded, LedgerProjectionRole.NEW_SECURITY_LEG, "target-1");
                            assertProjection(reloaded, LedgerProjectionRole.CASH_COMPENSATION, "cash-1");
                        });
    }

    @Test
    public void testExchangeFluentCreateEditSaveLoadDelete() throws Exception
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.EXCHANGE) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.bond) //
                        .securityOut("source-1", "main", fixture.portfolio, fixture.bond, shares(10)) //
                        .securityIn("target-1", "main", fixture.portfolio, fixture.target, shares(6)) //
                        .securityIn("target-2", "main", fixture.secondPortfolio, fixture.secondTarget, shares(4)) //
                        .cash("cash-1", "cash-1", fixture.account, money(3)) //
                        .basis(CorporateActionBasisStatus.PROVIDED) //
                        .basisMethod(CorporateActionBasisMethod.PERCENTAGE_ALLOCATION) //
                        .basisPercentageAllocation(LedgerLegRole.TARGET_SECURITY_LEG, "target-1", "main",
                                        new BigDecimal("60")) //
                        .basisPercentageAllocation(LedgerLegRole.TARGET_SECURITY_LEG, "target-2", "main",
                                        new BigDecimal("40")) //
                        .buildAndAdd().getEntry();

        exerciseEndToEnd(fixture, entry,
                        (client, candidate) -> LedgerCorporateActionView.of(candidate).securityIn()
                                        .withPortfolio(portfolio(client, "Second Portfolio"))
                                        .withSecurity(security(client, "Second Target AG")).single(),
                        LedgerLegRole.TARGET_SECURITY_LEG, "target-2", "main",
                        posting -> posting.setShares(shares(9)),
                        reloaded -> {
                            assertShares(reloaded, LedgerLegRole.TARGET_SECURITY_LEG, "target-2", "main", 9);
                            assertBasisAllocationCount(reloaded, 2);
                            assertProjection(reloaded, LedgerProjectionRole.DELIVERY_OUTBOUND, "source-1");
                            assertProjection(reloaded, LedgerProjectionRole.NEW_SECURITY_LEG, "target-1");
                            assertProjection(reloaded, LedgerProjectionRole.NEW_SECURITY_LEG, "target-2");
                            assertProjection(reloaded, LedgerProjectionRole.CASH_COMPENSATION, "cash-1");
                        });
    }

    @Test
    public void testRestructuringFluentCreateEditSaveLoadDelete() throws Exception
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.RESTRUCTURING) //
                        .date(DATE) //
                        .securityContext("context-1", "main", fixture.portfolio, fixture.source) //
                        .securityOut("source-1", "main", fixture.portfolio, fixture.source, shares(10)) //
                        .securityIn("target-1", "main", fixture.portfolio, fixture.target, shares(5)) //
                        .cash("cash-1", "main", fixture.account, money(9)) //
                        .principal("principal-1", "main", fixture.account, money(9)) //
                        .accruedInterest("interest-1", "main", fixture.account, money(1)) //
                        .basis(CorporateActionBasisStatus.UNKNOWN) //
                        .buildAndAdd().getEntry();

        exerciseEndToEnd(fixture, entry,
                        (client, candidate) -> LedgerCorporateActionView.of(candidate).cash()
                                        .withAccount(account(client, "Cash Account")).withGroupKey("main").single(),
                        LedgerLegRole.CASH_LEG, "cash-1", "main",
                        posting -> setAmount(posting, 10),
                        reloaded -> {
                            assertAmount(reloaded, LedgerLegRole.CASH_LEG, "cash-1", "main", 10);
                            assertAmount(reloaded, LedgerLegRole.PRINCIPAL_REDEMPTION_LEG, "principal-1", "main", 9);
                            assertAmount(reloaded, LedgerLegRole.ACCRUED_INTEREST_LEG, "interest-1", "main", 1);
                            assertProjection(reloaded, LedgerProjectionRole.DELIVERY_OUTBOUND, "source-1");
                            assertProjection(reloaded, LedgerProjectionRole.NEW_SECURITY_LEG, "target-1");
                            assertProjection(reloaded, LedgerProjectionRole.ACCOUNT, "cash-1");
                        });
    }

    @Test
    public void testDefaultFluentCreateEditSaveLoadDelete() throws Exception
    {
        var fixture = fixture();
        var entry = LedgerNativeEntryAssembler.corporateAction(fixture.client) //
                        .kind(CorporateActionKind.DEFAULT) //
                        .date(DATE) //
                        .securityContext("context-1", "claim-1", fixture.portfolio, fixture.source) //
                        .securityIn("claim-1", "claim-1", fixture.portfolio, fixture.claim, shares(3)) //
                        .cash("cash-1", "cash-1", fixture.account, money(5)) //
                        .buildAndAdd().getEntry();

        exerciseEndToEnd(fixture, entry,
                        (client, candidate) -> LedgerCorporateActionView.of(candidate).cash()
                                        .withAccount(account(client, "Cash Account")).single(),
                        LedgerLegRole.CASH_LEG, "cash-1", "cash-1",
                        posting -> setAmount(posting, 6),
                        reloaded -> {
                            assertAmount(reloaded, LedgerLegRole.CASH_LEG, "cash-1", "cash-1", 6);
                            assertProjection(reloaded, LedgerProjectionRole.NEW_SECURITY_LEG, "claim-1");
                            assertProjection(reloaded, LedgerProjectionRole.ACCOUNT, "cash-1");
                        });
    }

    private void exerciseEndToEnd(Fixture fixture, LedgerEntry entry, HandleLookup handleLookup,
                    LedgerLegRole expectedRole, String expectedLocalKey, String expectedGroupKey, PostingMutation edit,
                    EntryAssertion assertion) throws Exception
    {
        assertValid(fixture.client);
        assertNativeDefinitionValid(entry);

        LedgerProjectionService.materialize(fixture.client);
        LedgerProjectionService.materialize(fixture.client);
        assertProjectingMovementsAreMaterialized(entry);
        assertNonProjectingFacts(entry);

        var handle = assertHandle(fixture.client, entry, handleLookup, expectedRole, expectedLocalKey, expectedGroupKey);
        LedgerCorporateActionEditSupport.mutatePosting(fixture.client, handle, edit::mutate);
        var liveEntry = entryByUUID(fixture.client, entry.getUUID());

        assertion.assertEntry(liveEntry);
        assertValid(fixture.client);
        assertNativeDefinitionValid(liveEntry);
        assertProjectingMovementsAreMaterialized(liveEntry);
        assertNonProjectingFacts(liveEntry);

        var xml = saveXml(fixture.client);
        assertNoUuidSelectorFields(xml);

        var loadedFromXml = loadXml(xml);
        LedgerProjectionService.materialize(loadedFromXml);
        var xmlEntry = onlyLedgerEntry(loadedFromXml);
        assertHandle(loadedFromXml, xmlEntry, handleLookup, expectedRole, expectedLocalKey, expectedGroupKey);
        assertion.assertEntry(xmlEntry);
        assertValid(loadedFromXml);
        assertNativeDefinitionValid(xmlEntry);
        assertProjectingMovementsAreMaterialized(xmlEntry);
        assertNonProjectingFacts(xmlEntry);

        var loadedFromProtobuf = ProtobufTestUtilities.load(ProtobufTestUtilities.save(fixture.client));
        LedgerProjectionService.materialize(loadedFromProtobuf);
        var protobufEntry = onlyLedgerEntry(loadedFromProtobuf);
        assertHandle(loadedFromProtobuf, protobufEntry, handleLookup, expectedRole, expectedLocalKey, expectedGroupKey);
        assertion.assertEntry(protobufEntry);
        assertValid(loadedFromProtobuf);
        assertNativeDefinitionValid(protobufEntry);
        assertProjectingMovementsAreMaterialized(protobufEntry);
        assertNonProjectingFacts(protobufEntry);

        var securityCount = fixture.client.getSecurities().size();
        new LedgerMutationContext(fixture.client).removeEntry(liveEntry);

        assertThat(fixture.client.getLedger().getEntries().size(), is(0));
        assertThat(ledgerBackedProjectionCount(fixture.client), is(0L));
        assertThat(fixture.client.getSecurities().size(), is(securityCount));
    }

    private static LedgerCorporateActionLegHandle assertHandle(Client client, LedgerEntry entry,
                    HandleLookup handleLookup, LedgerLegRole expectedRole, String expectedLocalKey,
                    String expectedGroupKey)
    {
        var handle = handleLookup.find(client, entry);
        assertThat(handle.role(), is(expectedRole));
        assertThat(handle.localKey(), is(expectedLocalKey));
        assertThat(handle.groupKey(), is(expectedGroupKey));
        assertThat(handle.toSemanticKey().role(), is(expectedRole));
        assertThat(handle.toSemanticKey().localKey(), is(expectedLocalKey));
        assertThat(handle.toSemanticKey().groupKey(), is(expectedGroupKey));
        return handle;
    }

    private void assertProjectingMovementsAreMaterialized(LedgerEntry entry)
    {
        var descriptors = LedgerProjectionSupport.descriptors(entry);

        assertFalse(descriptors.isEmpty());
        assertTrue(descriptors.stream().allMatch(descriptor -> descriptor.getSemanticInstanceKey().isPresent()));
        for (var descriptor : descriptors)
            assertProjection(entry, descriptor.getRole(), descriptor.getSemanticInstanceKey().orElseThrow());
    }

    private void assertNonProjectingFacts(LedgerEntry entry)
    {
        assertFalse(LedgerProjectionSupport.descriptors(entry).stream()
                        .map(DerivedProjectionDescriptor::getPrimaryPosting)
                        .anyMatch(posting -> posting.getCorporateActionLeg() == CorporateActionLeg.SECURITY_CONTEXT));
        assertFalse(LedgerProjectionSupport.descriptors(entry).stream()
                        .map(DerivedProjectionDescriptor::getPrimaryPosting)
                        .anyMatch(posting -> posting.getType().name().contains("BASIS")));
    }

    private static void assertProjection(LedgerEntry entry, LedgerProjectionRole role, String semanticInstanceKey)
    {
        assertThat(LedgerProjectionSupport.descriptor(entry, role, semanticInstanceKey).getRole(), is(role));
    }

    private static void assertAmount(LedgerEntry entry, LedgerLegRole role, String localKey, String groupKey,
                    long amount)
    {
        assertThat(posting(entry, role, localKey, groupKey).getAmount(), is(money(amount).getAmount()));
    }

    private static void assertShares(LedgerEntry entry, LedgerLegRole role, String localKey, String groupKey,
                    long shares)
    {
        assertThat(posting(entry, role, localKey, groupKey).getShares(), is(shares(shares)));
    }

    private static void assertBasisAllocationCount(LedgerEntry entry, int expected)
    {
        assertThat(entry.getParameters().stream()
                        .filter(parameter -> parameter.getType() == LedgerParameterType.CORPORATE_ACTION_BASIS_ALLOCATION)
                        .count(), is((long) expected));
    }

    private static LedgerPosting posting(LedgerEntry entry, LedgerLegRole role, String localKey, String groupKey)
    {
        return LedgerCorporateActionEditSupport.postingBySemanticKey(entry, role, localKey, groupKey);
    }

    private static void setAmount(LedgerPosting posting, long amount)
    {
        posting.setAmount(money(amount).getAmount());
        posting.setCurrency(CurrencyUnit.EUR);
    }

    private static void assertValid(Client client)
    {
        var result = LedgerStructuralValidator.validate(client.getLedger());
        assertTrue(result.toString(), result.isOK());
    }

    private static void assertNativeDefinitionValid(LedgerEntry entry)
    {
        var result = LedgerNativeEntryDefinitionValidator.validate(entry);
        assertTrue(result.format(), result.isOK());
    }

    private static LedgerEntry onlyLedgerEntry(Client client)
    {
        assertThat(client.getLedger().getEntries().size(), is(1));
        return client.getLedger().getEntries().get(0);
    }

    private static LedgerEntry entryByUUID(Client client, String uuid)
    {
        return client.getLedger().getEntries().stream().filter(entry -> Objects.equals(entry.getUUID(), uuid))
                        .findFirst().orElseThrow();
    }

    private static long ledgerBackedProjectionCount(Client client)
    {
        return client.getAccounts().stream().flatMap(account -> account.getTransactions().stream())
                        .filter(LedgerBackedAccountTransaction.class::isInstance).count()
                        + client.getPortfolios().stream().flatMap(portfolio -> portfolio.getTransactions().stream())
                                        .filter(LedgerBackedPortfolioTransaction.class::isInstance).count();
    }

    private static void assertNoUuidSelectorFields(String xml)
    {
        assertFalse(xml.contains("ProjectionMembership"));
        assertFalse(xml.contains("primaryPostingUUID"));
        assertFalse(xml.contains("postingGroupUUID"));
    }

    private static String saveXml(Client client) throws IOException
    {
        var file = File.createTempFile("ledger-corporate-action-fluent-e2e", ".xml");

        try
        {
            ClientFactory.save(client, file);
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        }
        finally
        {
            Files.deleteIfExists(file.toPath());
        }
    }

    private static Client loadXml(String xml) throws IOException
    {
        return ClientFactory.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static long shares(long shares)
    {
        return Values.Share.factorize(shares);
    }

    private static Money money(long amount)
    {
        return Money.of(CurrencyUnit.EUR, Values.Amount.factorize(amount));
    }

    private static Fixture fixture()
    {
        var client = new Client();
        var account = new Account();
        var secondAccount = new Account();
        var portfolio = new Portfolio();
        var secondPortfolio = new Portfolio();
        var source = new Security("Source AG", CurrencyUnit.EUR);
        var target = new Security("Target AG", CurrencyUnit.EUR);
        var secondTarget = new Security("Second Target AG", CurrencyUnit.EUR);
        var right = new Security("Right AG", CurrencyUnit.EUR);
        var bond = new Security("Bond AG", CurrencyUnit.EUR);
        var claim = new Security("Claim AG", CurrencyUnit.EUR);

        account.setName("Cash Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        secondAccount.setName("Second Cash Account");
        secondAccount.setCurrencyCode(CurrencyUnit.EUR);
        portfolio.setName("Portfolio");
        portfolio.setReferenceAccount(account);
        secondPortfolio.setName("Second Portfolio");
        secondPortfolio.setReferenceAccount(secondAccount);

        account.setUpdatedAt(UPDATED_AT);
        secondAccount.setUpdatedAt(UPDATED_AT);
        portfolio.setUpdatedAt(UPDATED_AT);
        secondPortfolio.setUpdatedAt(UPDATED_AT);
        source.setUpdatedAt(UPDATED_AT);
        target.setUpdatedAt(UPDATED_AT);
        secondTarget.setUpdatedAt(UPDATED_AT);
        right.setUpdatedAt(UPDATED_AT);
        bond.setUpdatedAt(UPDATED_AT);
        claim.setUpdatedAt(UPDATED_AT);

        client.addAccount(account);
        client.addAccount(secondAccount);
        client.addPortfolio(portfolio);
        client.addPortfolio(secondPortfolio);
        client.addSecurity(source);
        client.addSecurity(target);
        client.addSecurity(secondTarget);
        client.addSecurity(right);
        client.addSecurity(bond);
        client.addSecurity(claim);

        return new Fixture(client, account, secondAccount, portfolio, secondPortfolio, source, target, secondTarget,
                        right, bond, claim);
    }

    private static Account account(Client client, String name)
    {
        return client.getAccounts().stream().filter(account -> name.equals(account.getName())).findFirst()
                        .orElseThrow();
    }

    private static Portfolio portfolio(Client client, String name)
    {
        return client.getPortfolios().stream().filter(portfolio -> name.equals(portfolio.getName())).findFirst()
                        .orElseThrow();
    }

    private static Security security(Client client, String name)
    {
        return client.getSecurities().stream().filter(security -> name.equals(security.getName())).findFirst()
                        .orElseThrow();
    }

    private interface HandleLookup
    {
        LedgerCorporateActionLegHandle find(Client client, LedgerEntry entry);
    }

    private interface PostingMutation
    {
        void mutate(LedgerPosting posting);
    }

    private interface EntryAssertion
    {
        void assertEntry(LedgerEntry entry);
    }

    private record Fixture(Client client, Account account, Account secondAccount, Portfolio portfolio,
                    Portfolio secondPortfolio, Security source, Security target, Security secondTarget, Security right,
                    Security bond, Security claim)
    {
    }
}
