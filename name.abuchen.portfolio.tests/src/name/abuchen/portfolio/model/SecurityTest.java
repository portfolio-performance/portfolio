package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

import org.junit.Test;

public class SecurityTest
{

    @Test
    public void testThatDeepCopyIncludesAllProperties() throws IntrospectionException, IllegalAccessException,
                    InvocationTargetException
    {
        BeanInfo info = Introspector.getBeanInfo(Security.class);

        Security source = new Security();

        int skipped = 0;

        // set properties
        for (PropertyDescriptor p : info.getPropertyDescriptors())
        {
            if ("UUID".equals(p.getName())) //$NON-NLS-1$
                continue;

            if (p.getPropertyType() == String.class && p.getWriteMethod() != null)
                p.getWriteMethod().invoke(source, UUID.randomUUID().toString());
            else if (p.getPropertyType() == boolean.class && p.getWriteMethod() != null)
                p.getWriteMethod().invoke(source, true);
            else if (p.getPropertyType() == int.class && p.getWriteMethod() != null)
                p.getWriteMethod().invoke(source, new Random().nextInt());
            else
                skipped++;
        }

        assertThat(skipped, equalTo(9));

        Security target = source.deepCopy();

        assertThat(target.getUUID(), not(equalTo(source.getUUID())));

        // compare
        for (PropertyDescriptor p : info.getPropertyDescriptors())
        {
            if ("UUID".equals(p.getName())) //$NON-NLS-1$
                continue;

            if (p.getPropertyType() != String.class && p.getPropertyType() != boolean.class
                            && p.getPropertyType() != int.class)
                continue;

            Object sourceValue = p.getReadMethod().invoke(source);
            Object targetValue = p.getReadMethod().invoke(target);

            assertThat(targetValue, equalTo(sourceValue));
        }
    }

    @Test
    public void testSetLatest()
    {
        Security security = new Security();
        assertThat(security.setLatest(null), is(false));

        LatestSecurityPrice latest = new LatestSecurityPrice(LocalDate.now(), 1);
        assertThat(security.setLatest(latest), is(true));
        assertThat(security.setLatest(latest), is(false));
        assertThat(security.setLatest(null), is(true));
        assertThat(security.setLatest(null), is(false));

        LatestSecurityPrice second = new LatestSecurityPrice(LocalDate.now(), 2);
        assertThat(security.setLatest(latest), is(true));
        assertThat(security.setLatest(second), is(true));

        LatestSecurityPrice same = new LatestSecurityPrice(LocalDate.now(), 2);
        assertThat(security.setLatest(same), is(false));
    }

    @Test(expected = NullPointerException.class)
    public void testThatNullSecurityPriceIsNotAllowed()
    {
        Security security = new Security();
        security.addPrice(null);
    }
}
