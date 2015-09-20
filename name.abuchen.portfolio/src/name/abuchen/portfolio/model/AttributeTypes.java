package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.AttributeType.AmountPlainConverter;
import name.abuchen.portfolio.model.AttributeType.PercentPlainConverter;
import name.abuchen.portfolio.model.AttributeType.StringConverter;

public final class AttributeTypes
{
    /* package */static List<AttributeType> getDefaultTypes()
    {
        List<AttributeType> answer = new ArrayList<>();

        AttributeType ter = new AttributeType("ter"); //$NON-NLS-1$
        ter.setName(Messages.AttributesTERName);
        ter.setColumnLabel(Messages.AttributesTERColumn);
        ter.setTarget(Security.class);
        ter.setType(Double.class);
        ter.setConverter(PercentPlainConverter.class);
        answer.add(ter);

        AttributeType aum = new AttributeType("aum"); //$NON-NLS-1$
        aum.setName(Messages.AttributesAUMName);
        aum.setColumnLabel(Messages.AttributesAUMColumn);
        aum.setTarget(Security.class);
        aum.setType(Long.class);
        aum.setConverter(AmountPlainConverter.class);
        answer.add(aum);

        AttributeType vendor = new AttributeType("vendor"); //$NON-NLS-1$
        vendor.setName(Messages.AttributesVendorName);
        vendor.setColumnLabel(Messages.AttributesVendorColumn);
        vendor.setTarget(Security.class);
        vendor.setType(String.class);
        vendor.setConverter(StringConverter.class);
        answer.add(vendor);

        AttributeType fee = new AttributeType("acquisitionFee"); //$NON-NLS-1$
        fee.setName(Messages.AttributesAcquisitionFeeName);
        fee.setColumnLabel(Messages.AttributesAcquisitionFeeColumn);
        fee.setTarget(Security.class);
        fee.setType(Double.class);
        fee.setConverter(PercentPlainConverter.class);
        answer.add(fee);

        return answer;
    }

    private AttributeTypes()
    {}
}
