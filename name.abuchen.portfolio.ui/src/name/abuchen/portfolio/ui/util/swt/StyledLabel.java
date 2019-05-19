package name.abuchen.portfolio.ui.util.swt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import name.abuchen.portfolio.ui.util.Colors;

/**
 * Instances of this class represent a non-selectable user interface object that
 * displays a string or image that can be styled.
 * <p/>
 * Supported tags are:
 * <ul>
 * <li>green</li>
 * <li>red</li>
 * </ul>
 */
public class StyledLabel extends Canvas // NOSONAR
{
    private TextLayout textLayout;
    private SAXParserFactory spf;

    public StyledLabel(Composite parent, int style)
    {
        super(parent, style);

        try
        {
            spf = SAXParserFactory.newInstance();
            spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); //$NON-NLS-1$
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException(e);
        }

        this.textLayout = new TextLayout(parent.getDisplay());
        this.textLayout.setFont(getFont());

        addListener(SWT.Paint, this::handlePaint);
        addListener(SWT.Dispose, this::handleDispose);
    }

    public void setText(String text)
    {
        checkWidget();

        try
        {
            SAXParser parser = spf.newSAXParser();

            StringBuilder plainText = new StringBuilder();
            List<StyleRange> styleRanges = new ArrayList<>();

            DefaultHandler handler = new DefaultHandler()
            {
                private int pos = -1;

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                                throws SAXException
                {
                    if (!"text".equals(qName)) //$NON-NLS-1$
                        pos = plainText.length();
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException
                {
                    if (pos < 0)
                        return;

                    if ("red".equals(qName)) //$NON-NLS-1$
                        styleRanges.add(new StyleRange(pos, plainText.length() - pos, Colors.DARK_RED, null));
                    else if ("green".equals(qName)) //$NON-NLS-1$
                        styleRanges.add(new StyleRange(pos, plainText.length() - pos, Colors.DARK_GREEN, null));

                    pos = -1;
                }

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException
                {
                    plainText.append(ch, start, length);
                }
            };

            parser.parse(new ByteArrayInputStream(("<text>" + text + "</text>") //$NON-NLS-1$ //$NON-NLS-2$
                            .getBytes(StandardCharsets.UTF_8)), handler);

            this.textLayout.setText(plainText.toString());
            styleRanges.forEach(r -> this.textLayout.setStyle(r, r.start, r.start + r.length));
        }
        catch (ParserConfigurationException | SAXException | IOException e)
        {
            throw new IllegalArgumentException(e);
        }

        redraw();
    }

    @Override
    public void setFont(Font font)
    {
        super.setFont(font);
        this.textLayout.setFont(font);
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed)
    {
        Rectangle bounds = this.textLayout.getBounds();
        return new Point(bounds.width + 4, bounds.height + 1);
    }

    private void handlePaint(Event e)
    {
        this.textLayout.draw(e.gc, 0, 0);
        e.type = SWT.None;
    }

    private void handleDispose(Event e)
    {
        this.textLayout.dispose();
        e.type = SWT.None;
    }
}
