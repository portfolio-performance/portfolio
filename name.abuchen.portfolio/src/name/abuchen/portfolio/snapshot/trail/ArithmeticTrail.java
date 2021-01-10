package name.abuchen.portfolio.snapshot.trail;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.MutableMoney;

/* package */ class ArithmeticTrail implements TrailRecord
{
    public enum Operation
    {
        ADDITION, SUBTRACTION
    }

    private final Operation operation;
    private final String label;
    private final Money value;
    private final List<TrailRecord> children = new ArrayList<>();

    public ArithmeticTrail(Operation operation, String label, TrailRecord... inputs)
    {
        this.operation = operation;
        this.label = label;

        this.children.addAll(Arrays.asList(inputs));

        if (!this.children.isEmpty())
        {
            if (operation == Operation.ADDITION)
            {
                this.value = this.children.stream().map(TrailRecord::getValue).filter(Objects::nonNull)
                                .collect(MoneyCollectors.sum(this.children.get(0).getValue().getCurrencyCode()));
            }
            else if (operation == Operation.SUBTRACTION)
            {
                MutableMoney subtraction = MutableMoney.of(this.children.get(0).getValue());
                for (int index = 1; index < inputs.length; index++)
                    subtraction.subtract(children.get(index).getValue());
                this.value = subtraction.toMoney();
            }
            else
            {
                throw new UnsupportedOperationException();
            }
        }
        else
        {
            this.value = null;
        }
    }

    @Override
    public LocalDate getDate()
    {
        return null;
    }

    @Override
    public String getLabel()
    {
        return label;
    }

    @Override
    public Long getShares()
    {
        return null;
    }

    @Override
    public Money getValue()
    {
        return value;
    }

    @Override
    public List<TrailRecord> getInputs()
    {
        return Collections.unmodifiableList(children);
    }

    @Override
    public TrailRecord add(TrailRecord trail)
    {
        if (this.operation == Operation.ADDITION && trail instanceof ArithmeticTrail
                        && ((ArithmeticTrail) trail).operation.equals(this.operation))
        {
            List<TrailRecord> trails = new ArrayList<>();
            trails.addAll(this.children);
            trails.addAll(((ArithmeticTrail) trail).children);
            return new ArithmeticTrail(this.operation, label, trails.toArray(new TrailRecord[0]));
        }
        else if (this.operation == Operation.ADDITION)
        {
            List<TrailRecord> trails = new ArrayList<>();
            trails.addAll(this.children);
            trails.add(trail);
            return new ArithmeticTrail(this.operation, label, trails.toArray(new TrailRecord[0]));
        }
        else
        {
            return TrailRecord.super.add(trail);
        }
    }

    @Override
    public TrailRecord subtract(TrailRecord trail)
    {
        if (this.operation == Operation.SUBTRACTION && trail instanceof ArithmeticTrail
                        && ((ArithmeticTrail) trail).operation.equals(this.operation))
        {
            ArithmeticTrail answer = new ArithmeticTrail(this.operation, label);
            answer.children.addAll(this.children);
            answer.children.add(trail);
            return answer;
        }
        else
        {
            return TrailRecord.super.subtract(trail);
        }
    }
}
