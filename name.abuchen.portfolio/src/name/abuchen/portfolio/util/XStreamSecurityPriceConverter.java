package name.abuchen.portfolio.util;

import java.time.LocalDate;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import name.abuchen.portfolio.model.SecurityPrice;

public class XStreamSecurityPriceConverter implements Converter
{

    @Override
    public boolean canConvert(@SuppressWarnings("rawtypes") Class object)
    {
        return object.equals(SecurityPrice.class);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context)
    {
        SecurityPrice price = (SecurityPrice) value;
        if (price.getDate() != null)
            writer.addAttribute("t", price.getDate().toString()); //$NON-NLS-1$
        writer.addAttribute("v", Long.toString(price.getValue())); //$NON-NLS-1$
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
    {
        SecurityPrice price = new SecurityPrice();
        String attribute = reader.getAttribute("t"); //$NON-NLS-1$
        if (attribute != null)
            price.setDate(LocalDate.parse(attribute));
        price.setValue(Long.parseLong(reader.getAttribute("v"))); //$NON-NLS-1$
        return price;
    }
}
