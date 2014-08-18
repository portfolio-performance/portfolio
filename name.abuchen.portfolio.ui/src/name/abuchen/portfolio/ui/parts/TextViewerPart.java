package name.abuchen.portfolio.ui.parts;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.util.Files;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class TextViewerPart
{
    @Inject
    private Logger logger;

    @PostConstruct
    public void createComposite(Composite parent, MPart part)
    {
        String filename = part.getPersistedState().get(UIConstants.Parameter.FILE);

        Text text = new Text(parent, SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
        part.setLabel(filename);

        try
        {
            byte[] encoded = Files.readAllBytes(new File(filename));
            String content = Charset.defaultCharset().decode(ByteBuffer.wrap(encoded)).toString();
            text.setText(content);
        }
        catch (IOException e)
        {
            logger.error(e);
        }
    }
}
