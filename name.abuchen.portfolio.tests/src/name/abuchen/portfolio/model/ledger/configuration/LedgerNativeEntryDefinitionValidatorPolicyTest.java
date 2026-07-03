package name.abuchen.portfolio.model.ledger.configuration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerPosting;
import name.abuchen.portfolio.model.ledger.LedgerPostingDirection;
import name.abuchen.portfolio.model.ledger.LedgerPostingUnitRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerNativeEntryDefinitionValidator.IssueCode;

/**
 * Tests the definition-gated repeated primary leg policy without widening any
 * production native Corporate Action definitions.
 */
@SuppressWarnings("nls")
public class LedgerNativeEntryDefinitionValidatorPolicyTest
{
    @Test
    public void testAtLeastOneRepeatedPrimaryLegsAcceptDistinctLocalKeys()
    {
        var result = validate(LedgerLegCardinality.AT_LEAST_ONE, posting("target-1"), posting("target-2"));

        assertThat(result.format(), result.isOK(), is(true));
    }

    @Test
    public void testRepeatablePrimaryLegsAcceptDistinctLocalKeys()
    {
        var result = validate(LedgerLegCardinality.REPEATABLE, posting("target-1"), posting("target-2"));

        assertThat(result.format(), result.isOK(), is(true));
    }

    @Test
    public void testRepeatedPrimaryLegsRejectMissingLocalKey()
    {
        var result = validate(LedgerLegCardinality.AT_LEAST_ONE, posting(null), posting("target-2"));

        assertThat(result.format(), result.hasIssue(IssueCode.LEG_LOCAL_KEY_REQUIRED), is(true));
    }

    @Test
    public void testRepeatedPrimaryLegsRejectDuplicateLocalKey()
    {
        var result = validate(LedgerLegCardinality.REPEATABLE, posting("target-1"), posting("target-1"));

        assertThat(result.format(), result.hasIssue(IssueCode.LEG_LOCAL_KEY_DUPLICATE), is(true));
    }

    @Test
    public void testExactlyOneStillRejectsRepeatedPrimaryLegs()
    {
        var result = validate(LedgerLegCardinality.EXACTLY_ONE, posting("target-1"), posting("target-2"));

        assertThat(result.format(), result.hasIssue(IssueCode.LEG_CARDINALITY_VIOLATED), is(true));
    }

    @Test
    public void testOptionalStillRejectsRepeatedPrimaryLegs()
    {
        var result = validate(LedgerLegCardinality.OPTIONAL, posting("target-1"), posting("target-2"));

        assertThat(result.format(), result.hasIssue(IssueCode.AMBIGUOUS_LEG_MATCH), is(true));
    }

    private LedgerNativeEntryDefinitionValidator.ValidationResult validate(LedgerLegCardinality cardinality,
                    LedgerPosting... postings)
    {
        return LedgerNativeEntryDefinitionValidator.validateCardinalityForTesting(entry(), leg(cardinality),
                        List.of(postings));
    }

    private LedgerEntry entry()
    {
        var entry = new LedgerEntry("policy-entry");

        entry.setType(LedgerEntryType.SPIN_OFF);

        return entry;
    }

    private LedgerLegDefinition leg(LedgerLegCardinality cardinality)
    {
        return LedgerLegDefinition.of(LedgerLegRole.TARGET_SECURITY_LEG, LedgerPostingType.SECURITY, cardinality)
                        .projection(name.abuchen.portfolio.model.ledger.LedgerProjectionRole.NEW_SECURITY_LEG, true,
                                        false)
                        .build();
    }

    private LedgerPosting posting(String localKey)
    {
        var posting = new LedgerPosting("posting-" + localKey);

        posting.setType(LedgerPostingType.SECURITY);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
        posting.setDirection(LedgerPostingDirection.INBOUND);
        posting.setCorporateActionLeg(CorporateActionLeg.TARGET_SECURITY);
        posting.setLocalKey(localKey);
        posting.setGroupKey("main");

        return posting;
    }
}
