package name.abuchen.portfolio.ui.wizards.datatransfer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.model.AccountTransaction;

public class ReviewExtractedItemsPageTest
{
    @Test
    public void testGetExDateReturnsAccountTransactionExDate()
    {
        AccountTransaction transaction = new AccountTransaction();
        transaction.setType(AccountTransaction.Type.DIVIDENDS);
        transaction.setDateTime(LocalDateTime.parse("2024-03-19T00:00:00"));
        transaction.setExDate(LocalDateTime.parse("2024-03-14T00:00:00"));

        LocalDate exDate = ReviewExtractedItemsPage.getExDate(new Extractor.TransactionItem(transaction));

        assertThat(exDate, is(LocalDate.parse("2024-03-14")));
    }

    @Test
    public void testGetExDateReturnsNullIfTransactionHasNoExDate()
    {
        AccountTransaction transaction = new AccountTransaction();
        transaction.setType(AccountTransaction.Type.DIVIDENDS);
        transaction.setDateTime(LocalDateTime.parse("2024-03-19T00:00:00"));

        LocalDate exDate = ReviewExtractedItemsPage.getExDate(new Extractor.TransactionItem(transaction));

        assertThat(exDate, is((LocalDate) null));
    }
}
