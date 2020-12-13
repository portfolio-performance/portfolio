package name.abuchen.portfolio.model;

import java.time.LocalDateTime;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class SecurityPriceTest
{

    @Test public void testGetDateWithoutDate()
    {
        SecurityPrice price = new SecurityPrice();
        price.setDateTime(LocalDateTime.now());
        assertThat(price.getDate(), is(notNullValue()));
    }
}
