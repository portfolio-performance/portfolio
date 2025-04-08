package name.abuchen.portfolio.util;

import java.util.ArrayList;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * Read (but not write) the internal Arrays$ArrayList collection. Starting with
 * Java 17, XStream does not have access to this class anymore (unless we
 * explicitly open the module). However, we anyway want to work with ArrayList
 * only, therefore we convert while reading.
 * 
 * <pre>
 * &lt;dimensions class="java.util.Arrays$ArrayList"&gt;
 * &lt;a class="string-array"&gt;
 *  &lt;string>category name&lt;/string&gt;
 * &lt;/a&gt;
 * &lt;/dimensions&gt;
 * </pre>
 */
public class XStreamArrayListConverter extends CollectionConverter
{
    public XStreamArrayListConverter(Mapper mapper)
    {
        super(mapper);
    }

    @Override
    public boolean canConvert(@SuppressWarnings("rawtypes") Class type)
    {
        return type.getName().equals("java.util.Arrays$ArrayList"); // NOSONAR //$NON-NLS-1$
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
    {
        // move to read
        reader.moveDown();
        var result = super.unmarshal(reader, context);
        reader.moveUp();
        return result;
    }

    @Override
    protected Object createCollection(@SuppressWarnings("rawtypes") Class type)
    {
        return new ArrayList<>();
    }
}
