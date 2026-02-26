package name.abuchen.portfolio.model;

import java.io.IOException;
import java.io.OutputStream;

public interface Exporter
{
    String getName();
    
    void export(OutputStream out) throws IOException;
}
