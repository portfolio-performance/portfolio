package name.abuchen.portfolio.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;

/**
 * Helper class for converting LocalDate to and from String. Throws DateTimeParseException
 */
public class LocalDateConverter extends AbstractSingleValueConverter {
    
    /**
     * {@inheritDoc}
     * @see com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter#canConvert(java.lang.Class)
     */
    public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
        return type.equals(LocalDate.class);
    }

    /**
     * {@inheritDoc}
     * @see com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter#toString(java.lang.Object)
     */
    public String toString(Object source) {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
        if (canConvert(source.getClass())) {
            return formatter.format((LocalDate) source);
        }
        throw new DateTimeParseException(source.toString(), source.toString(), 0);
    }

    /**
     * {@inheritDoc}
     * @see com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter#fromString(java.lang.String)
     */
    public Object fromString(String s) {
        DateTimeFormatter[] formatters = new DateTimeFormatter[] { 
                        DateTimeFormatter.ofPattern("d.M.yy"), //$NON-NLS-1$
                        DateTimeFormatter.ofPattern("d.M.y"), //$NON-NLS-1$
                        DateTimeFormatter.ofPattern("d. MMM y"), //$NON-NLS-1$
                        DateTimeFormatter.ofPattern("d. MMMM y"), //$NON-NLS-1$
                        DateTimeFormatter.ofPattern("d. MMM. y"), //$NON-NLS-1$
                        DateTimeFormatter.ofPattern("yyy-MM-dd") //$NON-NLS-1$
        };
        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDate date = LocalDate.parse(s, formatter);
                return date;
            } catch (DateTimeParseException e) {
                // continue with next formatter
            }
        }
        throw new DateTimeParseException(s, s, 0);
    }
}