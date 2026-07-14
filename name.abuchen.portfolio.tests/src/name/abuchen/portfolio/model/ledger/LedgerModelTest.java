package name.abuchen.portfolio.model.ledger;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.ledger.LedgerParameter.ValueKind;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerParameterType;
import name.abuchen.portfolio.model.ledger.configuration.LedgerPostingType;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

/**
 * Tests low-level ledger model behavior used by the migration and projection
 * services. These tests make sure core model fields, the typed parameter
 * mechanism, and the stable enum vocabulary keep ledger data consistent.
 */
@SuppressWarnings("nls")
public class LedgerModelTest
{
    @Test
    public void testLedgerEntryCarriesIdentityAndMinimumFields()
    {
        var dateTime = LocalDateTime.of(2020, 9, 28, 0, 0);
        var updatedAt = Instant.parse("2026-01-02T03:04:05Z");
        var parameter = LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE, dateTime);
        var posting = new LedgerPosting("posting-1");
        var entry = new LedgerEntry("entry-1");

        entry.setType(LedgerEntryType.BUY);
        entry.setDateTime(dateTime);
        entry.setNote("note");
        entry.setSource("source");
        entry.addParameter(parameter);
        entry.addPosting(posting);
        entry.setUpdatedAt(updatedAt);

        assertThat(entry.getUUID(), is("entry-1"));
        assertThat(entry.getType(), is(LedgerEntryType.BUY));
        assertThat(entry.getDateTime(), is(dateTime));
        assertThat(entry.getNote(), is("note"));
        assertThat(entry.getSource(), is("source"));
        assertThat(entry.getUpdatedAt(), is(updatedAt));
        assertThat(entry.getParameters(), is(List.of(parameter)));
        assertThat(entry.getPostings(), is(List.of(posting)));
        assertThrows(UnsupportedOperationException.class, () -> entry.getParameters().add(parameter));
        assertThrows(UnsupportedOperationException.class, () -> entry.getPostings().add(new LedgerPosting()));
        assertTrue(entry.removeParameter(parameter));
        assertTrue(entry.getParameters().isEmpty());
    }

    @Test
    public void testGeneratedUUIDsAreIndependent()
    {
        var entry = new LedgerEntry();
        var otherEntry = new LedgerEntry();
        var posting = new LedgerPosting();

        assertNotEquals(entry.getUUID(), otherEntry.getUUID());
        assertNotEquals(entry.getUUID(), posting.getUUID());
    }

    @Test
    public void testLedgerCollectsEntriesWithoutOwningClientIntegration()
    {
        var ledger = new Ledger();
        var entry = new LedgerEntry("entry-1");

        ledger.addEntry(entry);

        assertThat(ledger.getEntries(), is(List.of(entry)));
        assertThrows(UnsupportedOperationException.class, () -> ledger.getEntries().add(new LedgerEntry()));
        assertTrue(ledger.removeEntry(entry));
        assertTrue(ledger.getEntries().isEmpty());
    }

    @Test
    public void testLedgerPostingCarriesFieldsAndForexAsPostingData()
    {
        var account = new Account();
        var portfolio = new Portfolio();
        var security = new Security("Siemens", CurrencyUnit.EUR);
        var exchangeRate = BigDecimal.valueOf(1.0875);
        var posting = new LedgerPosting("posting-1");

        posting.setType(LedgerPostingType.SECURITY);
        posting.setAmount(123456L);
        posting.setCurrency(CurrencyUnit.EUR);
        posting.setForexAmount(134259L);
        posting.setForexCurrency(CurrencyUnit.USD);
        posting.setExchangeRate(exchangeRate);
        posting.setSecurity(security);
        posting.setShares(500000L);
        posting.setAccount(account);
        posting.setPortfolio(portfolio);
        posting.setSemanticRole(LedgerPostingSemanticRole.SECURITY);
        posting.setDirection(LedgerPostingDirection.INBOUND);
        posting.setUnitRole(LedgerPostingUnitRole.PRIMARY);

        assertThat(posting.getUUID(), is("posting-1"));
        assertThat(posting.getType(), is(LedgerPostingType.SECURITY));
        assertThat(posting.getAmount(), is(123456L));
        assertThat(posting.getCurrency(), is(CurrencyUnit.EUR));
        assertThat(posting.getForexAmount(), is(134259L));
        assertThat(posting.getForexCurrency(), is(CurrencyUnit.USD));
        assertThat(posting.getExchangeRate(), is(exchangeRate));
        assertSame(security, posting.getSecurity());
        assertThat(posting.getShares(), is(500000L));
        assertSame(account, posting.getAccount());
        assertSame(portfolio, posting.getPortfolio());
        assertThat(posting.getSemanticRole(), is(LedgerPostingSemanticRole.SECURITY));
        assertThat(posting.getDirection(), is(LedgerPostingDirection.INBOUND));
        assertThat(posting.getUnitRole(), is(LedgerPostingUnitRole.PRIMARY));
    }

    @Test
    public void testLedgerPostingSemanticVocabularyIsTypedAndAdditive()
    {
        var posting = new LedgerPosting("posting-1");

        assertThat(posting.getSemanticRole(), is((LedgerPostingSemanticRole) null));
        assertThat(posting.getDirection(), is((LedgerPostingDirection) null));
        assertThat(posting.getUnitRole(), is((LedgerPostingUnitRole) null));

        assertThat(EnumSet.allOf(LedgerPostingSemanticRole.class),
                        is(EnumSet.of(LedgerPostingSemanticRole.CASH, LedgerPostingSemanticRole.SECURITY,
                                        LedgerPostingSemanticRole.FEE, LedgerPostingSemanticRole.TAX,
                                        LedgerPostingSemanticRole.GROSS_VALUE,
                                        LedgerPostingSemanticRole.FOREX_CONTEXT)));
        assertThat(EnumSet.allOf(LedgerPostingDirection.class),
                        is(EnumSet.of(LedgerPostingDirection.INBOUND, LedgerPostingDirection.OUTBOUND,
                                        LedgerPostingDirection.NEUTRAL)));
        assertThat(EnumSet.allOf(LedgerPostingUnitRole.class),
                        is(EnumSet.of(LedgerPostingUnitRole.PRIMARY, LedgerPostingUnitRole.FEE,
                                        LedgerPostingUnitRole.TAX, LedgerPostingUnitRole.GROSS_VALUE,
                                        LedgerPostingUnitRole.FOREX_CONTEXT)));
        assertThat(EnumSet.allOf(LedgerProjectionRole.class),
                        is(EnumSet.of(LedgerProjectionRole.ACCOUNT, LedgerProjectionRole.PORTFOLIO,
                                        LedgerProjectionRole.SOURCE_ACCOUNT, LedgerProjectionRole.TARGET_ACCOUNT,
                                        LedgerProjectionRole.SOURCE_PORTFOLIO, LedgerProjectionRole.TARGET_PORTFOLIO,
                                        LedgerProjectionRole.DELIVERY_INBOUND,
                                        LedgerProjectionRole.DELIVERY_OUTBOUND)));
    }

    @Test
    public void testExDateIsLocalDateTimeLedgerParameter()
    {
        var exDate = LocalDateTime.of(2020, 9, 28, 0, 0);
        var parameter = LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE, exDate);
        var posting = new LedgerPosting();

        posting.addParameter(parameter);

        assertFalse(Arrays.stream(LedgerEntry.class.getDeclaredFields()).anyMatch(f -> "exDate".equals(f.getName())));
        assertThat(posting.getParameters(), is(List.of(parameter)));
        assertThat(parameter.getType(), is(LedgerParameterType.EX_DATE));
        assertThat(parameter.getValueKind(), is(ValueKind.LOCAL_DATE_TIME));
        assertThat(parameter.getValue(), is(exDate));
        assertThrows(UnsupportedOperationException.class, () -> posting.getParameters().clear());
    }

    @Test
    public void testLedgerEntryAndPostingParametersAreIndependent()
    {
        var entryParameter = LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE,
                        LocalDateTime.of(2020, 9, 28, 0, 0));
        var postingParameter = LedgerParameter.ofLocalDateTime(LedgerParameterType.EX_DATE,
                        LocalDateTime.of(2021, 3, 1, 0, 0));
        var entry = new LedgerEntry();
        var posting = new LedgerPosting();

        entry.addParameter(entryParameter);
        posting.addParameter(postingParameter);
        entry.addPosting(posting);

        assertThat(entry.getParameters(), is(List.of(entryParameter)));
        assertThat(posting.getParameters(), is(List.of(postingParameter)));
        assertFalse(entry.getParameters().contains(postingParameter));
        assertFalse(posting.getParameters().contains(entryParameter));
    }

    @Test
    public void testLedgerParameterValueKindsAreExplicit()
    {
        var expectedKinds = EnumSet.of(ValueKind.STRING, ValueKind.DECIMAL, ValueKind.LONG, ValueKind.MONEY,
                        ValueKind.SECURITY, ValueKind.ACCOUNT, ValueKind.PORTFOLIO, ValueKind.BOOLEAN,
                        ValueKind.LOCAL_DATE, ValueKind.LOCAL_DATE_TIME);

        assertThat(EnumSet.allOf(ValueKind.class), is(expectedKinds));
        assertValueKindPolicy(ValueKind.STRING, String.class);
        assertValueKindPolicy(ValueKind.DECIMAL, BigDecimal.class);
        assertValueKindPolicy(ValueKind.LONG, Long.class);
        assertValueKindPolicy(ValueKind.MONEY, Money.class);
        assertValueKindPolicy(ValueKind.SECURITY, Security.class);
        assertValueKindPolicy(ValueKind.ACCOUNT, Account.class);
        assertValueKindPolicy(ValueKind.PORTFOLIO, Portfolio.class);
        assertValueKindPolicy(ValueKind.BOOLEAN, Boolean.class);
        assertValueKindPolicy(ValueKind.LOCAL_DATE, LocalDate.class);
        assertValueKindPolicy(ValueKind.LOCAL_DATE_TIME, LocalDateTime.class);
    }

    @Test
    public void testLedgerParameterFactoriesValidateParameterType()
    {
        var exception = assertThrows(IllegalArgumentException.class,
                        () -> LedgerParameter.ofString(LedgerParameterType.EX_DATE, "2020-09-28"));

        assertThat(exception.getMessage(), containsString("does not support STRING"));
        assertThat(exception.getMessage(), containsString("expected LOCAL_DATE_TIME"));
    }

    @Test
    public void testConfigurableLedgerEntryTypeCodesAreStable()
    {
        for (var type : LedgerEntryType.values())
            assertThat(LedgerEntryType.fromCode(type.getCode()), is(type));

        var exception = assertThrows(IllegalArgumentException.class, () -> LedgerEntryType.fromCode("UNKNOWN"));
        assertThat(exception.getMessage(), containsString("Unknown LedgerEntryType code: UNKNOWN"));

        for (String missing : new String[] { null, "", " " })
        {
            var missingException = assertThrows(IllegalArgumentException.class,
                            () -> LedgerEntryType.fromCode(missing));
            assertThat(missingException.getMessage(), containsString("Missing LedgerEntryType code"));
        }

        assertThat(EnumSet.allOf(LedgerEntryType.class),
                        is(EnumSet.of(LedgerEntryType.DEPOSIT, LedgerEntryType.REMOVAL, LedgerEntryType.INTEREST,
                                        LedgerEntryType.INTEREST_CHARGE, LedgerEntryType.FEES,
                                        LedgerEntryType.FEES_REFUND, LedgerEntryType.TAXES, LedgerEntryType.TAX_REFUND,
                                        LedgerEntryType.DIVIDENDS, LedgerEntryType.BUY, LedgerEntryType.SELL,
                                        LedgerEntryType.CASH_TRANSFER, LedgerEntryType.SECURITY_TRANSFER,
                                        LedgerEntryType.DELIVERY_INBOUND, LedgerEntryType.DELIVERY_OUTBOUND)));
    }

    @Test
    public void testConfigurableLedgerPostingTypeCodesAreStable()
    {
        for (var type : LedgerPostingType.values())
            assertThat(LedgerPostingType.fromCode(type.getCode()), is(type));

        var exception = assertThrows(IllegalArgumentException.class, () -> LedgerPostingType.fromCode("UNKNOWN"));
        assertThat(exception.getMessage(), containsString("Unknown LedgerPostingType code: UNKNOWN"));

        for (String missing : new String[] { null, "", " " })
        {
            var missingException = assertThrows(IllegalArgumentException.class,
                            () -> LedgerPostingType.fromCode(missing));
            assertThat(missingException.getMessage(), containsString("Missing LedgerPostingType code"));
        }

        assertThat(EnumSet.allOf(LedgerPostingType.class),
                        is(EnumSet.of(LedgerPostingType.CASH, LedgerPostingType.SECURITY, LedgerPostingType.FEE,
                                        LedgerPostingType.TAX, LedgerPostingType.GROSS_VALUE)));
    }

    @Test
    public void testConfigurableLedgerParameterTypeCodesAreStable()
    {
        for (var type : LedgerParameterType.values())
            assertThat(LedgerParameterType.fromCode(type.getCode()), is(type));

        var exception = assertThrows(IllegalArgumentException.class, () -> LedgerParameterType.fromCode("UNKNOWN"));
        assertThat(exception.getMessage(), containsString("Unknown LedgerParameterType code: UNKNOWN"));

        for (String missing : new String[] { null, "", " " })
        {
            var missingException = assertThrows(IllegalArgumentException.class,
                            () -> LedgerParameterType.fromCode(missing));
            assertThat(missingException.getMessage(), containsString("Missing LedgerParameterType code"));
        }

        assertThat(EnumSet.allOf(LedgerParameterType.class), is(EnumSet.of(LedgerParameterType.EX_DATE)));
    }

    private void assertValueKindPolicy(ValueKind kind, Class<?> valueType)
    {
        assertThat(kind.getValueType(), is(valueType));
        assertFalse(kind.supportsValue(null));
    }
}
