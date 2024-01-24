package name.abuchen.portfolio.ui.util.swt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.DesktopAPI;

/**
 * Instances of this class represent a non-selectable user interface object that
 * displays a string or image that can be styled.
 * <p/>
 * Supported tags are:
 * <ul>
 * <li>green</li>
 * <li>red</li>
 * <li>strong</li>
 * <li>em</li>
 * <li>a</li>
 * </ul>
 */
public class StyledLabel extends Canvas // NOSONAR
{
    private static class Tag
    {
        private String tagName;
        private int start;
        private Map<String, String> attributes = new HashMap<>();

        public Tag(String tagName, int start)
        {
            this.tagName = tagName;
            this.start = start;
        }
    }

    private static final class StyleRangeParser extends DefaultHandler
    {
        private StringBuilder plainText = new StringBuilder();
        private List<StyleRange> styleRanges = new ArrayList<>();
        private LinkedList<Tag> stack = new LinkedList<>();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            if (!"text".equals(qName)) //$NON-NLS-1$
            {
                Tag tag = new Tag(qName, plainText.length());
                int l = attributes.getLength();
                for (int ii = 0; ii < l; ii++)
                    tag.attributes.put(attributes.getQName(ii), attributes.getValue(ii));
                stack.add(tag);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException
        {
            Tag tag = null;

            while (!stack.isEmpty())
            {
                Tag t = stack.removeLast();
                if (t.tagName.equals(qName))
                {
                    tag = t;
                    break;
                }
            }

            if (tag == null)
                return;

            if ("red".equals(qName)) //$NON-NLS-1$
            {
                styleRanges.add(new StyleRange(tag.start, plainText.length() - tag.start,
                                Colors.theme().redForeground(), null));
            }
            else if ("green".equals(qName)) //$NON-NLS-1$
            {
                styleRanges.add(new StyleRange(tag.start, plainText.length() - tag.start,
                                Colors.theme().greenForeground(), null));
            }
            else if ("strong".equals(qName)) //$NON-NLS-1$
            {
                styleRanges.add(new StyleRange(tag.start, plainText.length() - tag.start, null, null, SWT.BOLD));
            }
            else if ("em".equals(qName)) //$NON-NLS-1$
            {
                styleRanges.add(new StyleRange(tag.start, plainText.length() - tag.start, null, null, SWT.ITALIC));
            }
            else if ("a".equals(qName)) //$NON-NLS-1$
            {
                StyleRange style = new StyleRange();
                style.underline = true;
                style.underlineStyle = SWT.UNDERLINE_LINK;
                style.underlineColor = Colors.theme().hyperlink();
                style.foreground = Colors.theme().hyperlink();
                style.data = tag.attributes.get("href"); //$NON-NLS-1$
                style.start = tag.start;
                style.length = plainText.length() - tag.start;
                styleRanges.add(style);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException
        {
            plainText.append(ch, start, length);
        }

        public List<StyleRange> getStyleRanges()
        {
            return styleRanges;
        }

        public String getPlainText()
        {
            return plainText.toString();
        }
    }

    private TextLayout textLayout;
    private SAXParserFactory spf;
    private Consumer<String> openLinkHandler;

    private LocalResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources());

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
        addListener(SWT.MouseDown, this::openBrowser);
        addListener(SWT.MouseMove, this::updateCursor);

        initAccessibility();
    }

    public void setText(String text)
    {
        checkWidget();

        try
        {
            SAXParser parser = spf.newSAXParser();

            StyleRangeParser handler = new StyleRangeParser();

            parser.parse(new ByteArrayInputStream(("<text>" + text + "</text>") //$NON-NLS-1$ //$NON-NLS-2$
                            .getBytes(StandardCharsets.UTF_8)), handler);

            this.textLayout.setText(handler.getPlainText());
            handler.getStyleRanges().forEach(r -> {

                // TextLayout#setStyle : start and end offsets are "inclusive"

                if (r.fontStyle != SWT.NORMAL)
                {
                    Font font = resourceManager
                                    .create(FontDescriptor.createFrom(textLayout.getFont()).setStyle(r.fontStyle));
                    textLayout.setStyle(new TextStyle(font, r.foreground, r.background), r.start,
                                    r.start + r.length - 1);
                }
                else
                {
                    textLayout.setStyle(r, r.start, r.start + r.length - 1);
                }
            });
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

        // update existing styles with new font
        TextStyle[] styles = this.textLayout.getStyles();
        for (TextStyle style : styles)
        {
            if (style.font != null)
                style.font = resourceManager.create(FontDescriptor.createFrom(textLayout.getFont())
                                .setStyle(style.font.getFontData()[0].getStyle()));
        }

        redraw();
    }

    public void setOpenLinkHandler(Consumer<String> openLinkHandler)
    {
        this.openLinkHandler = openLinkHandler;
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed)
    {
        this.textLayout.setWidth(wHint == SWT.DEFAULT ? SWT.DEFAULT : Math.max(wHint - 4, 1));
        Rectangle bounds = this.textLayout.getBounds();
        return new Point(bounds.width + 4, bounds.height + 1);
    }

    private void handlePaint(Event e)
    {
        this.textLayout.draw(e.gc, 0, 0);
    }

    private void handleDispose(Event e)
    {
        this.textLayout.dispose();
    }

    private void openBrowser(Event event)
    {
        String linkUrl = this.getLinkUrlAtMousePointer(event);
        if (linkUrl != null && event.button == 1)
        {
            if (openLinkHandler != null)
                openLinkHandler.accept(linkUrl);
            else
                DesktopAPI.browse(linkUrl);
        }
    }

    private void updateCursor(Event event)
    {
        this.setCursor(this.getLinkUrlAtMousePointer(event) != null ? new Cursor(this.getDisplay(), SWT.CURSOR_HAND)
                        : null);
    }

    private String getLinkUrlAtMousePointer(Event event)
    {
        int offset = this.textLayout.getOffset(event.x, event.y, null);
        if (offset == -1)
            return null;

        Point mostLeftPoint = this.textLayout.getLocation(offset, false);
        Point mostRightPoint = this.textLayout.getLocation(offset, true);
        if (event.x < mostLeftPoint.x || event.x > mostRightPoint.x)
            return null;

        TextStyle style = this.textLayout.getStyle(offset);
        if (style == null || style.data == null)
            return null;

        return String.valueOf(style.data);
    }

    private void initAccessibility()
    {
        getAccessible().addAccessibleControlListener(new AccessibleControlAdapter()
        {
            @Override
            public void getRole(AccessibleControlEvent e)
            {
                e.detail = ACC.ROLE_LABEL;
            }

            @Override
            public void getValue(AccessibleControlEvent e)
            {
                e.result = textLayout.getText();
            }
        });
    }
}
