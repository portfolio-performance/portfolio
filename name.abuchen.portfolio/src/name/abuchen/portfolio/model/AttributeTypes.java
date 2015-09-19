package name.abuchen.portfolio.model;

import java.util.Arrays;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.AttributeType.AmountPlainConverter;
import name.abuchen.portfolio.model.AttributeType.PercentPlainConverter;
import name.abuchen.portfolio.model.AttributeType.StringConverter;

public final class AttributeTypes
{
    private static final List<AttributeType> TYPES = Arrays.asList( //

                    new AttributeType("ter") //$NON-NLS-1$
                                    .name(Messages.AttributesTERName) //
                                    .columnLabel(Messages.AttributesTERColumn) //
                                    .target(Security.class) //
                                    .type(Double.class) //
                                    .converter(PercentPlainConverter.class), //

                    new AttributeType("aum") //$NON-NLS-1$
                                    .name(Messages.AttributesAUMName) //
                                    .columnLabel(Messages.AttributesAUMColumn) //
                                    .target(Security.class) //
                                    .type(Long.class) //
                                    .converter(AmountPlainConverter.class), //

                    new AttributeType("vendor") //$NON-NLS-1$
                                    .name(Messages.AttributesVendorName) //
                                    .columnLabel(Messages.AttributesVendorColumn) //
                                    .target(Security.class) //
                                    .type(String.class) //
                                    .converter(StringConverter.class), //

                    new AttributeType("acquisitionFee") //$NON-NLS-1$
                                    .name(Messages.AttributesAcquisitionFeeName) //
                                    .columnLabel(Messages.AttributesAcquisitionFeeColumn) //
                                    .target(Security.class) //
                                    .type(Double.class) //
                                    .converter(PercentPlainConverter.class) //
                    );

    /* package */static List<AttributeType> getDefaultTypes()
    {
        return TYPES;
    }

    private AttributeTypes()
    {}
}
