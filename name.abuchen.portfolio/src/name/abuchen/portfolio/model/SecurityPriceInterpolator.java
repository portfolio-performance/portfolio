package name.abuchen.portfolio.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public interface SecurityPriceInterpolator
{
    public static SecurityPriceInterpolator getDefaultSecurityPriceInterpolator()
    {
        return TakeLastSecurityPriceInterpolator.getInstance();
    }
    
    /**
     * Calculate an interpolated security price to fill in gaps in the security
     * prices (i.e. due to holidays or weekends)
     * 
     * @param prices
     *            Ordered list of all security prices
     * @param lastIndexBefore
     *            The index such that {@code prices.get(lastIndexBefore)} is
     *            strictly before and {@code prices.get(lastIndexBefore + 1)} is strictly after
     *            {@code requestedDate}. Both
     *            {@code prices.get(lastIndexBefore)} and
     *            {@code prices.get(lastIndexBefore + 1)} must exist.
     * @param requestedDate The requested date
     * @return An interpoled security price for {@code requestedDate}.
     */
    public SecurityPrice interpolate(List<SecurityPrice> prices, int lastIndexBefore, LocalDate requestedDate);

    public static class TakeLastSecurityPriceInterpolator implements SecurityPriceInterpolator
    {
        private static TakeLastSecurityPriceInterpolator INSTANCE;
        
        public static TakeLastSecurityPriceInterpolator getInstance()
        {
            if (INSTANCE == null)
                INSTANCE = new TakeLastSecurityPriceInterpolator();
            return INSTANCE;
        }
        
        private TakeLastSecurityPriceInterpolator()
        {
        }
        
        @Override
        public SecurityPrice interpolate(List<SecurityPrice> prices, int lastIndexBefore, LocalDate requestedDate)
        {
            return prices.get(lastIndexBefore);
        }

    }
    
    public static class LinearSecurityPriceInterpolator implements SecurityPriceInterpolator
    {
        private static LinearSecurityPriceInterpolator INSTANCE;
        
        public static LinearSecurityPriceInterpolator getInstance()
        {
            if (INSTANCE == null)
                INSTANCE = new LinearSecurityPriceInterpolator();
            return INSTANCE;
        }
        
        private LinearSecurityPriceInterpolator()
        {
        }
        
        @Override
        public SecurityPrice interpolate(List<SecurityPrice> prices, int lastIndexBefore, LocalDate requestedDate)
        {
            int indexAfter = lastIndexBefore + 1;
            long gapDays = ChronoUnit.DAYS.between(prices.get(lastIndexBefore).getDate(), prices.get(indexAfter).getDate());
            long positionInGap = ChronoUnit.DAYS.between(prices.get(lastIndexBefore).getDate(), requestedDate);
            long interpolatedValue = prices.get(lastIndexBefore).getValue() + Math.round(((double) positionInGap) / ((double) gapDays)
                            * (double) (prices.get(indexAfter).getValue() - prices.get(lastIndexBefore).getValue()));
            return new SecurityPrice(requestedDate, interpolatedValue);
        }
    }
}
