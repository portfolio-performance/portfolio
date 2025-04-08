package name.abuchen.portfolio.util;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

@Component
public class ImageioSpiRegistration
{
    @Reference(cardinality = ReferenceCardinality.MULTIPLE)
    public void addImageReaderSpi(ImageReaderSpi readerProvider)
    {
        IIORegistry.getDefaultInstance().registerServiceProvider(readerProvider);
    }

}
