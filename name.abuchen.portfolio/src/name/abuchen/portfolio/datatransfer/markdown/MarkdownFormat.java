package name.abuchen.portfolio.datatransfer.markdown;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * Locale-independent formatting of values for the Markdown export. The output
 * must be identical regardless of the user's locale so that the document can
 * be parsed reliably by language models (ISO dates, decimal point, no
 * thousands separators).
 */
@SuppressWarnings("nls")
/* package */ final class MarkdownFormat
{
    /* package */ static final String NOT_AVAILABLE = "n/a";

    private static final int MAX_TEXT_LENGTH = 300;

    private MarkdownFormat()
    {
    }

    /* package */ static String date(LocalDate date)
    {
        return date == null ? "" : date.toString();
    }

    /**
     * Formats a monetary amount (stored in cents) with two decimals.
     */
    /* package */ static String amount(long amount)
    {
        return BigDecimal.valueOf(amount, Values.Amount.precision()).toPlainString();
    }

    /* package */ static String money(Money money)
    {
        return money == null ? "" : amount(money.getAmount());
    }

    /**
     * Formats a number of shares without trailing zeros.
     */
    /* package */ static String shares(long shares)
    {
        return stripZeros(BigDecimal.valueOf(shares, Values.Share.precision()), 0);
    }

    /**
     * Formats a security price (stored with eight decimals) with at least two
     * and at most eight decimals.
     */
    /* package */ static String quote(long quote)
    {
        return stripZeros(BigDecimal.valueOf(quote, Values.Quote.precision()), 2);
    }

    /**
     * Formats a price calculated as gross value divided by the number of
     * shares.
     */
    /* package */ static String pricePerShare(long grossAmount, long shares)
    {
        if (shares == 0)
            return "";

        var price = BigDecimal.valueOf(grossAmount, Values.Amount.precision())
                        .divide(BigDecimal.valueOf(shares, Values.Share.precision()), 6, RoundingMode.HALF_UP);
        return stripZeros(price, 2);
    }

    /**
     * Formats a fraction (0.1234) as percentage ("12.34%").
     */
    /* package */ static String percent(double fraction)
    {
        if (Double.isNaN(fraction) || Double.isInfinite(fraction))
            return NOT_AVAILABLE;

        return BigDecimal.valueOf(fraction * 100).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    /* package */ static String percent(Double fraction)
    {
        return fraction == null ? NOT_AVAILABLE : percent(fraction.doubleValue());
    }

    /**
     * Formats a classification weight (stored as 10000 = 100%) as percentage.
     */
    /* package */ static String weight(int weight)
    {
        return BigDecimal.valueOf(weight, Values.Weight.precision()).setScale(2, RoundingMode.HALF_UP)
                        .toPlainString() + "%";
    }

    /**
     * Makes arbitrary text safe to be used inside a table cell: no line
     * breaks, escaped backslashes and pipe characters, limited length. The
     * text is shortened before escaping so that an escape sequence is never
     * cut in half.
     */
    /* package */ static String text(String text)
    {
        if (text == null)
            return "";

        var cleaned = text.replaceAll("\\s+", " ").trim();

        if (cleaned.length() > MAX_TEXT_LENGTH)
            cleaned = cleaned.substring(0, MAX_TEXT_LENGTH - 3) + "...";

        return cleaned.replace("\\", "\\\\").replace("|", "\\|");
    }

    private static String stripZeros(BigDecimal value, int minScale)
    {
        var stripped = value.stripTrailingZeros();
        if (stripped.scale() < minScale)
            stripped = stripped.setScale(minScale);
        return stripped.toPlainString();
    }
}
