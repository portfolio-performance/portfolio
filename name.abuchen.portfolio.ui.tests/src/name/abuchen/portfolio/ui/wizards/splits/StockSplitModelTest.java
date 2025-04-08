package name.abuchen.portfolio.ui.wizards.splits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;

import org.junit.Test;

public class StockSplitModelTest
{

    @Test
    public void testCalculateNewQuoteAndStock()
    {
        StockSplitModel model = new StockSplitModel(null, null);
        // 2 shares for 1
        model.setNewShares(new BigDecimal(2));
        model.setOldShares(new BigDecimal(1));
        assertThat(model.calculateNewQuote(4), is(2L)); // quote is halved
        assertThat(model.calculateNewStock(4), is(8L)); // stock is doubled
        
        // 1 shares for 3
        model.setNewShares(new BigDecimal(1));
        model.setOldShares(new BigDecimal(3));
        assertThat(model.calculateNewQuote(5), is(15L)); // quote multiplied by 3
        assertThat(model.calculateNewStock(12), is(4L)); // stock is divided by 3
        
        // 3 shares for 1
        model.setNewShares(new BigDecimal(3));
        model.setOldShares(new BigDecimal(1));
        assertThat(model.calculateNewQuote(6), is(2L)); // quote is divided by 3
        assertThat(model.calculateNewStock(3), is(9L)); // stock is multiplied by 3        
    }

}
