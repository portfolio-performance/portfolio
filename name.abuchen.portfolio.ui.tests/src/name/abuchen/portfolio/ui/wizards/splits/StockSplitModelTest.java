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
    }

}
