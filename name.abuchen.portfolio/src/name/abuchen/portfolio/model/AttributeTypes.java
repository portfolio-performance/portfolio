package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.AttributeType.DoubleConverter;
import name.abuchen.portfolio.model.AttributeType.LongConverter;
import name.abuchen.portfolio.model.AttributeType.StringConverter;

public final class AttributeTypes
{
    private static final List<AttributeType> TYPES = Arrays.asList( //

                    new AttributeType("ter") //$NON-NLS-1$
                                    .name(Messages.AttributesTERName) //
                                    .columnLabel(Messages.AttributesTERColumn) //
                                    .target(Security.class) //
                                    .type(Double.class) //
                                    .converter(new DoubleConverter(Values.PercentPlain)), //

                    new AttributeType("aum") //$NON-NLS-1$
                                    .name(Messages.AttributesAUMName) //
                                    .columnLabel(Messages.AttributesAUMColumn) //
                                    .target(Security.class) //
                                    .type(Long.class) //
                                    .converter(new LongConverter(Values.AmountPlain)), //

                    new AttributeType("vendor") //$NON-NLS-1$
                                    .name(Messages.AttributesVendorName) //
                                    .columnLabel(Messages.AttributesVendorColumn) //
                                    .target(Security.class) //
                                    .type(String.class) //
                                    .converter(new StringConverter()) //
                    );

    public static List<AttributeType> available(Class<? extends Attributable> target)
    {
        List<AttributeType> answer = new ArrayList<AttributeType>();

        for (AttributeType type : TYPES)
        {
            if (type.supports(target))
                answer.add(type);
        }

        return answer;
    }

    private AttributeTypes()
    {}
}
