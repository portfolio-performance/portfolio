package name.abuchen.portfolio.money;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class MoneyCollectors
{
    private abstract static class AbstractMoneyAdder<T> implements Collector<T, MutableMoney, Money>
    {
        private final String currencyCode;

        private AbstractMoneyAdder(String currencyCode)
        {
            this.currencyCode = currencyCode;
        }

        @Override
        public Supplier<MutableMoney> supplier()
        {
            return () -> MutableMoney.of(currencyCode);
        }

        @Override
        public BinaryOperator<MutableMoney> combiner()
        {
            return (left, right) -> left.add(right);
        }

        @Override
        public Function<MutableMoney, Money> finisher()
        {
            return MutableMoney::toMoney;
        }

        @Override
        public Set<java.util.stream.Collector.Characteristics> characteristics()
        {
            return EnumSet.of(Characteristics.UNORDERED);
        }
    }

    private MoneyCollectors()
    {
    }

    public static Collector<Money, MutableMoney, Money> sum(String currencyCode)
    {
        return new AbstractMoneyAdder<Money>(currencyCode)
        {
            @Override
            public BiConsumer<MutableMoney, Money> accumulator()
            {
                return (builder, money) -> builder.add(money);
            }
        };
    }

    public static <T> Collector<T, MutableMoney, Money> sum(String currencyCode, Function<T, Money> converter)
    {
        return new AbstractMoneyAdder<T>(currencyCode)
        {
            @Override
            public BiConsumer<MutableMoney, T> accumulator()
            {
                return (builder, type) -> builder.add(converter.apply(type));
            }
        };
    }
}
