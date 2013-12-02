package name.abuchen.portfolio.model;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import org.junit.Test;

public class SecurityTest
{

    @Test
    public void testThatDeepCopyIncludesAllProperties() throws IntrospectionException, IllegalArgumentException,
                    IllegalAccessException, InvocationTargetException
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
            else if (p.getPropertyType() == boolean.class)
                p.getWriteMethod().invoke(source, true);
            else
                skipped++;
        }

        assertThat(skipped, equalTo(4));

        Security target = source.deepCopy();

        assertThat(target.getUUID(), not(equalTo(source.getUUID())));

        // compare
        for (PropertyDescriptor p : info.getPropertyDescriptors())
        {
            if ("UUID".equals(p.getName())) //$NON-NLS-1$
                continue;

            if (p.getPropertyType() != String.class && p.getPropertyType() != boolean.class)
                continue;

            Object sourceValue = p.getReadMethod().invoke(source);
            Object targetValue = p.getReadMethod().invoke(target);

            assertThat(targetValue, equalTo(sourceValue));
        }
    }
}
