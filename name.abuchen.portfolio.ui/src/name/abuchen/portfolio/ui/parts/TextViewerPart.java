package name.abuchen.portfolio.ui.parts;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.ui.UIConstants;

@SuppressWarnings("restriction")
public class TextViewerPart
{
    @Inject
    private Logger logger;

    @PostConstruct
    public void createComposite(Composite parent, MPart part)
    {
        String filename = part.getPersistedState().get(UIConstants.PersistedState.FILENAME);

        Text text = new Text(parent, SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
        part.setLabel(filename);

        try
        {
            byte[] encoded = Files.readAllBytes(Paths.get(filename));
            String content = Charset.defaultCharset().decode(ByteBuffer.wrap(encoded)).toString();
            text.setText(content);
        }
        catch (IOException e)
        {
            logger.error(e);
        }
    }
}
