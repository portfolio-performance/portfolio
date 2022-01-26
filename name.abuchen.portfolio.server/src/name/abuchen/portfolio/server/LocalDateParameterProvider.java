package name.abuchen.portfolio.server;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

public class LocalDateParameterProvider implements ParamConverterProvider
{
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations)
    {
        if (rawType != LocalDate.class)
            return null;

        return new ParamConverter<T>()
        {
            @Override
            public T fromString(String value)
            {
                if (value == null)
                    throw new IllegalArgumentException();

                try
                {
                    return rawType.cast(LocalDate.parse(value));
                }
                catch (DateTimeParseException ex)
                {
                    throw new WebApplicationException(Response.status(HttpServletResponse.SC_BAD_REQUEST).build());
                }
            }

            @Override
            public String toString(T value)
            {
                return value.toString();
            }
        };
    }
}
