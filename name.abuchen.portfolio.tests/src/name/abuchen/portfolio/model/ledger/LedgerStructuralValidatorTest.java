package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

/**
 * Tests structural validation for ledger entries.
 */
@SuppressWarnings("nls")
public class LedgerStructuralValidatorTest
{
    @Test
    public void testMissingLedgerIsRejected()
    {
        var result = LedgerStructuralValidator.validate(null);

        assertThat(result.format(), result.hasIssue(LedgerIssueCode.LEDGER_REQUIRED), is(true));
    }

    @Test
    public void testIssueCodesAreStableOnTheWire()
    {
        // the wire codes are part of the REST API: a client branches on them

        assertThat(LedgerIssueCode.ENTRY_TYPE_REQUIRED.getCode(), is("entry-type-required"));
        assertThat(LedgerIssueCode.SIGNED_FACT_NOT_ALLOWED.getCode(), is("signed-fact-not-allowed"));
        assertThat(LedgerIssueCode.SEMANTIC_PRIMARY_AMBIGUOUS.getCode(), is("semantic-primary-ambiguous"));

        for (var code : LedgerIssueCode.values())
            assertThat(code.getCode(), matchesPattern("[a-z0-9]+(-[a-z0-9]+)*"));
    }

    @Test
    public void testIssueCodeIsPartOfTheFormattedMessage()
    {
        var result = LedgerStructuralValidator.validate(null);

        assertThat(result.format(), containsString("[ledger-required]"));
    }

    @Test
    public void testEmptyLedgerIsValid()
    {
        assertOK(LedgerStructuralValidator.validate(new Ledger()));
    }

    @Test
    public void testValidSemanticEntriesPassValidation()
    {
        for (var type : LedgerEntryType.values())
            assertOK(LedgerStructuralValidator.validate(ledger(entry(type))));
    }

    @Test
    public void testMissingEntryTypeIsRejected()
    {
        var entry = entry(LedgerEntryType.DEPOSIT);
        entry.setType(null);

        assertIssue(entry, LedgerIssueCode.ENTRY_TYPE_REQUIRED);
    }

    @Test
    public void testMissingEntryDateTimeIsRejected()
    {
        var entry = entry(LedgerEntryType.DEPOSIT);
        entry.setDateTime(null);

        assertIssue(entry, LedgerIssueCode.ENTRY_DATE_TIME_REQUIRED);
    }

    @Test
    public void testMissingPostingTypeIsRejected()
    {
        var entry = entry(LedgerEntryType.DEPOSIT);
        entry.getPostings().get(0).setType(null);

        assertIssue(entry, LedgerIssueCode.POSTING_TYPE_REQUIRED);
    }

    @Test
    public void testMissingPostingCurrencyIsRejected()
    {
        var entry = entry(LedgerEntryType.DEPOSIT);
        entry.getPostings().get(0).setCurrency(null);

        assertIssue(entry, LedgerIssueCode.POSTING_CURRENCY_REQUIRED);
    }

    @Test
    public void testMissingPostingSecurityIsRejected()
    {
        var entry = entry(LedgerEntryType.DELIVERY_INBOUND);
        entry.getPostings().get(0).setSecurity(null);

        assertIssue(entry, LedgerIssueCode.POSTING_SECURITY_REQUIRED);
    }

    @Test
    public void testNegativeAmountIsRejected()
    {
        var entry = entry(LedgerEntryType.DEPOSIT);
        entry.getPostings().get(0).setAmount(-1);

        assertIssue(entry, LedgerIssueCode.SIGNED_FACT_NOT_ALLOWED);
    }

    @Test
    public void testNegativeSharesAreRejected()
    {
        var entry = entry(LedgerEntryType.DELIVERY_INBOUND);
        entry.getPostings().get(0).setShares(-1);

        assertIssue(entry, LedgerIssueCode.SIGNED_FACT_NOT_ALLOWED);
    }

    @Test
    public void testNonPositiveExchangeRateIsRejected()
    {
        var entry = entry(LedgerEntryType.DEPOSIT);
        entry.getPostings().get(0).setExchangeRate(BigDecimal.ZERO);

        assertIssue(entry, LedgerIssueCode.POSTING_EXCHANGE_RATE_POSITIVE);
    }

    @Test
    public void testDividendWithoutSecurityIsRejected()
    {
        var entry = entry(LedgerEntryType.DIVIDENDS);
        entry.getPostings().get(0).setSecurity(null);

        assertIssue(entry, LedgerIssueCode.DIVIDEND_SECURITY_REQUIRED);
    }

    @Test
    public void testMissingTransferSourceIsRejected()
    {
        var entry = entry(LedgerEntryType.CASH_TRANSFER);
        entry.removePosting(posting(entry, LedgerPostingDirection.OUTBOUND));

        assertIssue(entry, LedgerIssueCode.SEMANTIC_SOURCE_REQUIRED);
    }

    @Test
    public void testMissingTransferTargetIsRejected()
    {
        var entry = entry(LedgerEntryType.CASH_TRANSFER);
        entry.removePosting(posting(entry, LedgerPostingDirection.INBOUND));

        assertIssue(entry, LedgerIssueCode.SEMANTIC_TARGET_REQUIRED);
    }

    @Test
    public void testMissingSecurityTransferSourceIsRejected()
    {
        var entry = entry(LedgerEntryType.SECURITY_TRANSFER);
        entry.removePosting(posting(entry, LedgerPostingDirection.OUTBOUND));

        assertIssue(entry, LedgerIssueCode.SEMANTIC_SOURCE_REQUIRED);
    }

    @Test
    public void testDuplicateTransferSourceIsRejected()
    {
        var entry = entry(LedgerEntryType.CASH_TRANSFER);
        entry.addPosting(cashPosting(LedgerPostingDirection.OUTBOUND));

        assertIssue(entry, LedgerIssueCode.SEMANTIC_SOURCE_AMBIGUOUS);
    }

    @Test
    public void testDuplicateTransferTargetIsRejected()
    {
        var entry = entry(LedgerEntryType.CASH_TRANSFER);
        entry.addPosting(cashPosting(LedgerPostingDirection.INBOUND));

        assertIssue(entry, LedgerIssueCode.SEMANTIC_TARGET_AMBIGUOUS);
    }

    @Test
    public void testMissingPrimarySemanticRoleIsRejected()
    {
        var entry = entry(LedgerEntryType.BUY);
        posting(entry, LedgerPostingSemanticRole.CASH).setSemanticRole(null);

        assertIssue(entry, LedgerIssueCode.SEMANTIC_PRIMARY_REQUIRED);
    }

    @Test
    public void testDuplicatePrimaryPostingsAreRejected()
    {
        var entry = entry(LedgerEntryType.DEPOSIT);
        entry.addPosting(cashPosting(LedgerPostingDirection.NEUTRAL));

        assertIssue(entry, LedgerIssueCode.SEMANTIC_PRIMARY_AMBIGUOUS);
    }

    @Test
    public void testMissingPrimaryOwnerIsRejected()
    {
        var entry = entry(LedgerEntryType.DEPOSIT);
        entry.getPostings().get(0).setAccount(null);

        assertIssue(entry, LedgerIssueCode.SEMANTIC_OWNER_REQUIRED);
    }

    @Test
    public void testMissingPortfolioOwnerIsRejected()
    {
        var entry = entry(LedgerEntryType.DELIVERY_INBOUND);
        entry.getPostings().get(0).setPortfolio(null);

        assertIssue(entry, LedgerIssueCode.SEMANTIC_OWNER_REQUIRED);
    }

    @Test
    public void testUnitPostingIsAcceptedAlongsidePrimaries()
    {
        var entry = entry(LedgerEntryType.BUY);
        entry.addPosting(unitPosting(LedgerPostingType.FEE, LedgerPostingUnitRole.FEE));

        assertOK(LedgerStructuralValidator.validate(ledger(entry)));
    }

    @Test
    public void testExDateOnPostingWithoutSecurityIsRejected()
    {
        var entry = entry(LedgerEntryType.DEPOSIT);
        entry.getPostings().get(0).addParameter(LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE,
                        LocalDateTime.of(2020, 9, 28, 0, 0)));

        assertIssue(entry, LedgerIssueCode.EX_DATE_SECURITY_REQUIRED);
    }

    @Test
    public void testExDateWithWrongValueKindIsRejected()
    {
        var entry = entry(LedgerEntryType.DIVIDENDS);
        entry.getPostings().get(0).addParameter(LedgerParameter.unchecked(LedgerParameterType.EX_DATE,
                        LedgerParameter.ValueKind.STRING, "2020-09-28"));

        assertIssue(entry, LedgerIssueCode.EX_DATE_VALUE_KIND_REQUIRED);
    }

    @Test
    public void testParameterValueKindMismatchIsRejected()
    {
        var entry = entry(LedgerEntryType.DIVIDENDS);
        entry.getPostings().get(0).addParameter(LedgerParameter.unchecked(LedgerParameterType.EX_DATE,
                        LedgerParameter.ValueKind.LOCAL_DATE_TIME, "not a date time"));

        assertIssue(entry, LedgerIssueCode.PARAMETER_VALUE_KIND_MISMATCH);
    }

    @Test
    public void testPrimaryPostingMustHaveThePostingTypeOfItsRule()
    {
        // a BUY whose cash primary is carried by a FEE posting: the roles all
        // line up, only the posting type is wrong

        var entry = entry(LedgerEntryType.BUY);
        entry.getPostings().get(0).setType(LedgerPostingType.FEE);

        assertIssue(entry, LedgerIssueCode.SEMANTIC_PRIMARY_REQUIRED);
    }

    private Ledger ledger(LedgerEntry entry)
    {
        var ledger = new Ledger();
        ledger.addEntry(entry);

        return ledger;
    }

    private LedgerEntry entry(LedgerEntryType type)
    {
        var entry = new LedgerEntry();
        entry.setType(type);
        entry.setDateTime(LocalDateTime.of(2026, 1, 1, 10, 0));

        switch (type)
        {
            case DEPOSIT, REMOVAL, INTEREST, INTEREST_CHARGE, DIVIDENDS ->
            {
                var posting = cashPosting(LedgerPostingDirection.NEUTRAL);
                if (type == LedgerEntryType.DIVIDENDS)
                    posting.setSecurity(security());
                entry.addPosting(posting);
            }
            case FEES, FEES_REFUND -> entry.addPosting(accountPosting(LedgerPostingType.FEE,
                            LedgerPostingSemanticRole.FEE, LedgerPostingDirection.NEUTRAL));
            case TAXES, TAX_REFUND -> entry.addPosting(accountPosting(LedgerPostingType.TAX,
                            LedgerPostingSemanticRole.TAX, LedgerPostingDirection.NEUTRAL));
            case BUY -> {
                entry.addPosting(cashPosting(LedgerPostingDirection.OUTBOUND));
                entry.addPosting(securityPosting(LedgerPostingDirection.INBOUND));
            }
            case SELL -> {
                entry.addPosting(cashPosting(LedgerPostingDirection.INBOUND));
                entry.addPosting(securityPosting(LedgerPostingDirection.OUTBOUND));
            }
            case DELIVERY_INBOUND -> entry.addPosting(securityPosting(LedgerPostingDirection.INBOUND));
            case DELIVERY_OUTBOUND -> entry.addPosting(securityPosting(LedgerPostingDirection.OUTBOUND));
            case CASH_TRANSFER -> {
                entry.addPosting(cashPosting(LedgerPostingDirection.OUTBOUND));
                entry.addPosting(cashPosting(LedgerPostingDirection.INBOUND));
            }
            case SECURITY_TRANSFER -> {
                entry.addPosting(securityPosting(LedgerPostingDirection.OUTBOUND));
                entry.addPosting(securityPosting(LedgerPostingDirection.INBOUND));
            }
            default -> throw new IllegalArgumentException(type.name());
        }

        return entry;
    }

    private LedgerPosting cashPosting(LedgerPostingDirection direction)
    {
        return accountPosting(LedgerPostingType.CASH, LedgerPostingSemanticRole.CASH, direction);
    }

    private LedgerPosting accountPosting(LedgerPostingType type, LedgerPostingSemanticRole role,
                    LedgerPostingDirection direction)
    {
        var posting = new LedgerPosting();
        posting.setType(type);
        posting.setAmount(Values.Amount.factorize(100));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setAccount(account());
        markPrimary(posting, role, direction);

        return posting;
    }

    private LedgerPosting securityPosting(LedgerPostingDirection direction)
    {
        var posting = new LedgerPosting();
        posting.setType(LedgerPostingType.SECURITY);
        posting.setAmount(Values.Amount.factorize(100));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setPortfolio(portfolio());
        posting.setSecurity(security());
        posting.setShares(Values.Share.factorize(10));
        markPrimary(posting, LedgerPostingSemanticRole.SECURITY, direction);

        return posting;
    }

    private LedgerPosting unitPosting(LedgerPostingType type, LedgerPostingUnitRole unitRole)
    {
        var posting = new LedgerPosting();
        posting.setType(type);
        posting.setAmount(Values.Amount.factorize(1));
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setSemanticRole(type == LedgerPostingType.FEE ? LedgerPostingSemanticRole.FEE
                        : LedgerPostingSemanticRole.TAX);
        posting.setUnitRole(unitRole);

        return posting;
    }

    private void markPrimary(LedgerPosting posting, LedgerPostingSemanticRole role, LedgerPostingDirection direction)
    {
        posting.setSemanticRole(role);
        posting.setDirection(direction);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);
    }

    private LedgerPosting posting(LedgerEntry entry, LedgerPostingDirection direction)
    {
        return entry.getPostings().stream().filter(posting -> posting.getDirection() == direction).findFirst()
                        .orElseThrow();
    }

    private LedgerPosting posting(LedgerEntry entry, LedgerPostingSemanticRole role)
    {
        return entry.getPostings().stream().filter(posting -> posting.getSemanticRole() == role).findFirst()
                        .orElseThrow();
    }

    private Account account()
    {
        var account = new Account();
        account.setName("Cash");
        return account;
    }

    private Portfolio portfolio()
    {
        var portfolio = new Portfolio();
        portfolio.setName("Portfolio");
        return portfolio;
    }

    private Security security()
    {
        return new Security("Security", CurrencyUnit.EUR);
    }

    private void assertOK(LedgerStructuralValidator.ValidationResult result)
    {
        assertTrue(result.format(), result.isOK());
    }

    private void assertIssue(LedgerEntry entry, LedgerIssueCode code)
    {
        var result = LedgerStructuralValidator.validate(ledger(entry));

        assertThat(result.format(), result.hasIssue(code), is(true));
    }
}
