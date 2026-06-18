package name.abuchen.portfolio.datatransfer.traderepublic;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class CashDividendCorrectionMatcherTest
{
    private static final String FAILURE_MESSAGE = "unmatched correction";

    @Test
    public void pairsExact1to1MatchAndRemovesBoth()
    {
        var security = newSecurity("EUR");
        var original = originalItem(security, "2025-07-17T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(22.02), Values.Amount.factorize(7.56), Values.Amount.factorize(0));
        var correction = correctionItem(security, "2026-03-27T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(-22.02), Values.Amount.factorize(-7.56), Values.Amount.factorize(0));

        var items = new ArrayList<>(List.of(original, correction));
        CashDividendCorrectionMatcher.reconcile(items);

        assertThat(items.size(), is(0));
    }

    @Test
    public void keepsCorrectionWhenNoCandidateExists()
    {
        var security = newSecurity("EUR");
        var correction = correctionItem(security, "2026-03-27T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(-22.02), Values.Amount.factorize(-7.56), Values.Amount.factorize(0));

        var items = new ArrayList<>(List.of(correction));
        CashDividendCorrectionMatcher.reconcile(items);

        assertThat(items.size(), is(1));
        assertThat(items.get(0), is(correction));
    }

    @Test
    public void pairsWithNearestPriorOriginal()
    {
        var security = newSecurity("EUR");
        var earlyOriginal = originalItem(security, "2024-07-17T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(22.02), Values.Amount.factorize(7.56), Values.Amount.factorize(0));
        var laterOriginal = originalItem(security, "2025-07-17T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(22.02), Values.Amount.factorize(7.56), Values.Amount.factorize(0));
        var correction = correctionItem(security, "2026-03-27T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(-22.02), Values.Amount.factorize(-7.56), Values.Amount.factorize(0));

        var items = new ArrayList<>(List.of(earlyOriginal, laterOriginal, correction));
        CashDividendCorrectionMatcher.reconcile(items);

        assertThat(items.size(), is(1));
        assertThat(items.get(0), is(earlyOriginal));
    }

    @Test
    public void doesNotPairWhenCorrectionPrecedesAllCandidates()
    {
        var security = newSecurity("EUR");
        var original = originalItem(security, "2026-06-01T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(22.02), Values.Amount.factorize(7.56), Values.Amount.factorize(0));
        var correction = correctionItem(security, "2026-03-27T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(-22.02), Values.Amount.factorize(-7.56), Values.Amount.factorize(0));

        var items = new ArrayList<>(List.of(original, correction));
        CashDividendCorrectionMatcher.reconcile(items);

        assertThat(items.size(), is(2));
    }

    @Test
    public void pairsMultipleCorrectionsWithNearestPriorOriginals()
    {
        var security = newSecurity("EUR");
        var originalA = originalItem(security, "2024-07-17T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(22.02), Values.Amount.factorize(7.56), Values.Amount.factorize(0));
        var originalB = originalItem(security, "2025-01-17T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(22.02), Values.Amount.factorize(7.56), Values.Amount.factorize(0));
        var correctionC = correctionItem(security, "2025-07-17T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(-22.02), Values.Amount.factorize(-7.56), Values.Amount.factorize(0));
        var correctionD = correctionItem(security, "2026-03-27T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(-22.02), Values.Amount.factorize(-7.56), Values.Amount.factorize(0));

        var items = new ArrayList<>(List.of(originalA, originalB, correctionC, correctionD));
        CashDividendCorrectionMatcher.reconcile(items);

        assertThat(items.size(), is(0));
    }

    @Test
    public void differentSecuritiesDoNotPair()
    {
        var securityA = newSecurity("EUR");
        var securityB = newSecurity("EUR");
        var original = originalItem(securityA, "2025-07-17T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(22.02), Values.Amount.factorize(7.56), Values.Amount.factorize(0));
        var correction = correctionItem(securityB, "2026-03-27T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(-22.02), Values.Amount.factorize(-7.56), Values.Amount.factorize(0));

        var items = new ArrayList<>(List.of(original, correction));
        CashDividendCorrectionMatcher.reconcile(items);

        assertThat(items.size(), is(2));
    }

    @Test
    public void differentTaxDoesNotPair()
    {
        var security = newSecurity("EUR");
        var original = originalItem(security, "2025-07-17T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(21.78), Values.Amount.factorize(7.80), Values.Amount.factorize(0));
        var correction = correctionItem(security, "2026-03-27T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(-22.02), Values.Amount.factorize(-7.56), Values.Amount.factorize(0));

        var items = new ArrayList<>(List.of(original, correction));
        CashDividendCorrectionMatcher.reconcile(items);

        assertThat(items.size(), is(2));
    }

    @Test
    public void pairsWhenFxUnitsMirror()
    {
        var security = newSecurity("USD");
        var original = originalItemWithFx(security, "2025-07-17T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(22.02), Values.Amount.factorize(7.56), Values.Amount.factorize(0),
                        Values.Amount.factorize(29.58), Values.Amount.factorize(34.51), new BigDecimal("0.857265"));
        var correction = correctionItemWithFx(security, "2026-03-27T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(-22.02), Values.Amount.factorize(-7.56), Values.Amount.factorize(0),
                        Values.Amount.factorize(-29.58), Values.Amount.factorize(-34.51), new BigDecimal("0.857265"));

        var items = new ArrayList<>(List.of(original, correction));
        CashDividendCorrectionMatcher.reconcile(items);

        assertThat(items.size(), is(0));
    }

    @Test
    public void doesNotPairWhenFxRateDiffers()
    {
        var security = newSecurity("USD");
        var original = originalItemWithFx(security, "2025-07-17T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(22.02), Values.Amount.factorize(7.56), Values.Amount.factorize(0),
                        Values.Amount.factorize(29.58), Values.Amount.factorize(34.51), new BigDecimal("0.857265"));
        var correction = correctionItemWithFx(security, "2026-03-27T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(-22.02), Values.Amount.factorize(-7.56), Values.Amount.factorize(0),
                        Values.Amount.factorize(-29.58), Values.Amount.factorize(-34.51), new BigDecimal("0.860000"));

        var items = new ArrayList<>(List.of(original, correction));
        CashDividendCorrectionMatcher.reconcile(items);

        assertThat(items.size(), is(2));
    }

    @Test
    public void doesNotPairFxOriginalWithNonFxCorrection()
    {
        var security = newSecurity("USD");
        var original = originalItemWithFx(security, "2025-07-17T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(22.02), Values.Amount.factorize(7.56), Values.Amount.factorize(0),
                        Values.Amount.factorize(29.58), Values.Amount.factorize(34.51), new BigDecimal("0.857265"));
        var correction = correctionItem(security, "2026-03-27T10:00", Values.Share.factorize(181.638106),
                        Values.Amount.factorize(-22.02), Values.Amount.factorize(-7.56), Values.Amount.factorize(0));

        var items = new ArrayList<>(List.of(original, correction));
        CashDividendCorrectionMatcher.reconcile(items);

        assertThat(items.size(), is(2));
    }

    // -------- helpers --------

    private static Security newSecurity(String currency)
    {
        return new Security("Test Security", currency);
    }

    private static Item originalItem(Security security, String dateTime, long shares, long amount, long taxUnit,
                    long feeUnit)
    {
        var tx = newTx(security, dateTime, shares, amount);
        if (taxUnit != 0)
            tx.addUnit(new Unit(Unit.Type.TAX, Money.of("EUR", taxUnit)));
        if (feeUnit != 0)
            tx.addUnit(new Unit(Unit.Type.FEE, Money.of("EUR", feeUnit)));
        return new TransactionItem(tx);
    }

    private static Item correctionItem(Security security, String dateTime, long shares, long amount, long taxUnit,
                    long feeUnit)
    {
        var tx = newTx(security, dateTime, shares, amount);
        if (taxUnit != 0)
            tx.addUnit(new Unit(Unit.Type.TAX, Money.of("EUR", taxUnit)));
        if (feeUnit != 0)
            tx.addUnit(new Unit(Unit.Type.FEE, Money.of("EUR", feeUnit)));
        var item = new TransactionItem(tx);
        item.setFailureMessage(FAILURE_MESSAGE);
        return item;
    }

    private static Item originalItemWithFx(Security security, String dateTime, long shares, long amount, long taxUnit,
                    long feeUnit, long fxAccountAmount, long fxForexAmount, BigDecimal rate)
    {
        var tx = newTx(security, dateTime, shares, amount);
        tx.setCurrencyCode("EUR");
        if (taxUnit != 0)
            tx.addUnit(new Unit(Unit.Type.TAX, Money.of("EUR", taxUnit)));
        if (feeUnit != 0)
            tx.addUnit(new Unit(Unit.Type.FEE, Money.of("EUR", feeUnit)));
        tx.addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of("EUR", fxAccountAmount), Money.of("USD", fxForexAmount),
                        rate));
        return new TransactionItem(tx);
    }

    private static Item correctionItemWithFx(Security security, String dateTime, long shares, long amount, long taxUnit,
                    long feeUnit, long fxAccountAmount, long fxForexAmount, BigDecimal rate)
    {
        var tx = newTx(security, dateTime, shares, amount);
        tx.setCurrencyCode("EUR");
        if (taxUnit != 0)
            tx.addUnit(new Unit(Unit.Type.TAX, Money.of("EUR", taxUnit)));
        if (feeUnit != 0)
            tx.addUnit(new Unit(Unit.Type.FEE, Money.of("EUR", feeUnit)));
        tx.addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of("EUR", fxAccountAmount), Money.of("USD", fxForexAmount),
                        rate));
        var item = new TransactionItem(tx);
        item.setFailureMessage(FAILURE_MESSAGE);
        return item;
    }

    private static AccountTransaction newTx(Security security, String dateTime, long shares, long amount)
    {
        var tx = new AccountTransaction();
        tx.setType(AccountTransaction.Type.DIVIDENDS);
        tx.setSecurity(security);
        tx.setDateTime(LocalDateTime.parse(dateTime));
        tx.setCurrencyCode("EUR");
        tx.setShares(shares);
        tx.setAmount(amount);
        return tx;
    }

}
