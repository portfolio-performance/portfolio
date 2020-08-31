package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.ImportAction.Status.Code;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;

public class DetectDuplicatesActionTest
{
    @SuppressWarnings("nls")
    @Test
    public void testDuplicateDetection4AccountTransaction() throws IntrospectionException, ReflectiveOperationException
    {
        DetectDuplicatesAction action = new DetectDuplicatesAction(new Client());

        new PropertyChecker<AccountTransaction>(AccountTransaction.class, "note", "forex", "monetaryAmount").before(
                        (name, o, c) -> assertThat(name, action.process(o, account(c)).getCode(), is(Code.WARNING)))
                        .after((name, o, c) -> assertThat(name, action.process(o, account(c)).getCode(), is(Code.OK)))
                        .run();
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateDetection4PortfolioTransaction()
                    throws IntrospectionException, ReflectiveOperationException
    {
        DetectDuplicatesAction action = new DetectDuplicatesAction(new Client());

        new PropertyChecker<PortfolioTransaction>(PortfolioTransaction.class, "fees", "taxes", "note", "forex",
                        "monetaryAmount") //
                                        .before((name, o, c) -> assertThat(name,
                                                        action.process(o, portfolio(c)).getCode(), is(Code.WARNING)))
                                        .after((name, o, c) -> assertThat(name,
                                                        action.process(o, portfolio(c)).getCode(), is(Code.OK)))
                                        .run();
    }

    @SuppressWarnings("nls")
    @Test
    public void testDuplicateDetectionWithPurchaseAndDeliveryPairs()
                    throws IntrospectionException, ReflectiveOperationException
    {
        DetectDuplicatesAction action = new DetectDuplicatesAction(new Client());

        new PropertyChecker<PortfolioTransaction>(PortfolioTransaction.class, "type", "fees", "taxes", "note", "forex",
                        "monetaryAmount") //
                                        .before((name, o, c) -> {
                                            o.setType(PortfolioTransaction.Type.BUY);
                                            c.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                                            assertThat(name, action.process(o, portfolio(c)).getCode(),
                                                            is(Code.WARNING));
                                        }) //
                                        .after((name, o, c) -> assertThat(name,
                                                        action.process(o, portfolio(c)).getCode(), is(Code.OK)))
                                        .run();

        new PropertyChecker<PortfolioTransaction>(PortfolioTransaction.class, "type", "fees", "taxes", "note", "forex",
                        "monetaryAmount") //
                                        .before((name, o, c) -> {
                                            o.setType(PortfolioTransaction.Type.SELL);
                                            c.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                                            assertThat(name, action.process(o, portfolio(c)).getCode(),
                                                            is(Code.WARNING));
                                        }) //
                                        .after((name, o, c) -> assertThat(name,
                                                        action.process(o, portfolio(c)).getCode(), is(Code.OK)))
                                        .run();

    }

    private Account account(AccountTransaction t)
    {
        Account a = new Account();
        a.setCurrencyCode(t.getCurrencyCode());
        a.addTransaction(t);
        return a;
    }

    private Portfolio portfolio(PortfolioTransaction t)
    {
        Portfolio p = new Portfolio();
        p.addTransaction(t);
        return p;
    }

    @FunctionalInterface
    private interface Consumer<T>
    {
        void accept(String name, T original, T copy);
    }

    private static class PropertyChecker<T extends Transaction>
    {
        private Random random = new Random();

        private Class<T> type;
        private List<PropertyDescriptor> properties = new ArrayList<>();

        private Consumer<T> before;
        private Consumer<T> after;

        public PropertyChecker(Class<T> type, String... excludes) throws IntrospectionException
        {
            this.type = type;
            Set<String> excludedSet = new HashSet<>(Arrays.asList(excludes));

            BeanInfo info = Introspector.getBeanInfo(type);
            for (PropertyDescriptor p : info.getPropertyDescriptors()) // NOSONAR
            {
                if (excludedSet.contains(p.getName()))
                    continue;

                if (p.getWriteMethod() == null || p.getReadMethod() == null)
                    continue;

                properties.add(p);
            }
        }

        public PropertyChecker<T> before(Consumer<T> before)
        {
            this.before = before;
            return this;
        }

        public PropertyChecker<T> after(Consumer<T> after)
        {
            this.after = after;
            return this;
        }

        public void run() throws ReflectiveOperationException
        {
            for (PropertyDescriptor p : properties)
                check(p);
        }

        private void check(PropertyDescriptor change) throws ReflectiveOperationException
        {
            T instance = type.getDeclaredConstructor().newInstance();
            T other = type.getDeclaredConstructor().newInstance();

            for (PropertyDescriptor p : properties)
            {
                Object argument = arg(p.getPropertyType());
                p.getWriteMethod().invoke(instance, argument);
                p.getWriteMethod().invoke(other, argument);
            }

            before.accept(change.getName(), instance, other);

            change.getWriteMethod().invoke(other,
                            alternative(change.getPropertyType(), change.getReadMethod().invoke(other)));

            after.accept(change.getName(), instance, other);
        }

        private Object alternative(Class<?> propertyType, Object value)
        {
            if (propertyType == String.class)
            {
                return "x" + value; //$NON-NLS-1$
            }
            else if (propertyType == long.class)
            {
                return ((long) value) + 1;
            }
            else if (propertyType == LocalDate.class)
            {
                return LocalDate.of(1999, 1, 1);
            }
            else if (propertyType == LocalDateTime.class)
            {
                return LocalDateTime.of(1999, 1, 1, 0, 0, 0);
            }
            else if (propertyType == Security.class)
            {
                return new Security();
            }
            else if (propertyType == AccountTransaction.Type.class)
            {
                return AccountTransaction.Type.values()[(((AccountTransaction.Type) value).ordinal() + 1)
                                % AccountTransaction.Type.values().length];
            }
            else if (propertyType == PortfolioTransaction.Type.class)
            {
                return PortfolioTransaction.Type.values()[(((PortfolioTransaction.Type) value).ordinal() + 1)
                                % PortfolioTransaction.Type.values().length];
            }
            else
            {
                throw new UnsupportedOperationException(propertyType.getName());
            }
        }

        private Object arg(Class<?> propertyType)
        {
            if (propertyType == String.class)
            {
                char start = ' ';
                char end = 'z';
                int range = end - start;

                StringBuilder builder = new StringBuilder();
                for (int ii = 0; ii < 10; ii++)
                    builder.append((char) (random.nextInt(range) + start));

                return builder.toString();
            }
            else if (propertyType == long.class)
            {
                return random.nextLong();
            }
            else if (propertyType == LocalDate.class)
            {
                return LocalDate.of(2000 + random.nextInt(30), 1, 1);
            }
            else if (propertyType == LocalDateTime.class)
            {
                return LocalDateTime.of(2000 + random.nextInt(30), 1, 1, 0, 0, 0);
            }
            else if (propertyType == Security.class)
            {
                return new Security();
            }
            else if (propertyType == AccountTransaction.Type.class)
            {
                return AccountTransaction.Type.values()[random.nextInt(AccountTransaction.Type.values().length)];
            }
            else if (propertyType == PortfolioTransaction.Type.class)
            {
                return PortfolioTransaction.Type.values()[random.nextInt(PortfolioTransaction.Type.values().length)];
            }
            else
            {
                throw new UnsupportedOperationException(propertyType.getName());
            }
        }
    }
}
