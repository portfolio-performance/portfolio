package name.abuchen.portfolio.rest.internal;

import java.util.Collection;

import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;

/**
 * Which currencies a report's money had to be converted <em>from</em>.
 * <p/>
 * The currency of an instrument is not the whole story: a transaction can be
 * recorded in another currency than the security it belongs to, and its fees and
 * taxes are converted with that currency, not with the instrument's.
 * {@link name.abuchen.portfolio.money.CurrencyConverterImpl} looks a rate up for
 * exactly those amounts, and a pair with nothing to convert with silently
 * multiplies by 1 - which {@link FxWarnings} can only name if the pair was seen
 * here.
 * <p/>
 * An amount that carries its own exchange rate is not such a conversion. A
 * transaction whose security is already in the reporting currency uses the rate
 * stored on its gross-value unit, and a fee or tax whose forex amount is already
 * in the reporting currency uses that value. Neither asks the rate series
 * anything, so neither belongs in {@code fxWarnings}.
 */
final class ConvertedCurrencies
{
    private ConvertedCurrencies()
    {
    }

    /** adds every currency this transaction's amounts were converted from */
    /* package */ static void addFrom(PortfolioTransaction transaction, String termCurrency,
                    Collection<String> currencies)
    {
        var currency = transaction.getCurrencyCode();
        if (!currency.equals(termCurrency))
        {
            // PortfolioTransaction.getMonetaryAmount(): with the security already
            // in the term currency, a stored gross-value rate is used instead of
            // the historic rate; in every other case the amount is converted
            var rateStored = transaction.getSecurity().getCurrencyCode().equals(termCurrency)
                    && transaction.getUnit(Transaction.Unit.Type.GROSS_VALUE).isPresent();

            if (!rateStored)
                currencies.add(currency);
        }

        transaction.getUnits().forEach(unit -> {
            if (unit.getType() != Transaction.Unit.Type.FEE && unit.getType() != Transaction.Unit.Type.TAX)
                return;

            var amountCurrency = unit.getAmount().getCurrencyCode();
            if (amountCurrency.equals(termCurrency))
                return;

            // Transaction.getUnitSum(): a fee already spelled in the term currency
            // as its forex is taken as given, not converted
            var forexAlreadyInTerm = unit.getForex() != null
                    && termCurrency.equals(unit.getForex().getCurrencyCode());

            if (!forexAlreadyInTerm)
                currencies.add(amountCurrency);
        });
    }
}
