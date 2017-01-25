package name.abuchen.portfolio.ui.util;

import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

/**
 * Combined converter and validator that validates the object if it can be
 * converted without exception. With the latest data binding version, a
 * conversion error is only written to the log but does not produce a validation
 * error anymore. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=495789
 */
public interface IValidatingConverter extends IConverter, IValidator
{
    @Override
    default IStatus validate(Object value)
    {
        try
        {
            // test conversion
            convert(value);
            return ValidationStatus.ok();
        }
        catch (Exception e) // NOSONAR
        {
            return ValidationStatus.error(e.getMessage());
        }
    }

    public static IValidatingConverter wrap(IConverter converter)
    {
        return new IValidatingConverter()
        {
            @Override
            public Object getFromType()
            {
                return converter.getFromType();
            }

            @Override
            public Object getToType()
            {
                return converter.getToType();
            }

            @Override
            public Object convert(Object fromObject)
            {
                return converter.convert(fromObject);
            }
        };
    }
}
