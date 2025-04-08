package name.abuchen.portfolio.ui.views.dashboard;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.xml.sax.helpers.DefaultHandler;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SWTHelper;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.util.TextUtil;

public class LabelConfig implements WidgetConfig
{
    private static final class NotEmptyValidator implements IInputValidator
    {
        @Override
        public String isValid(String newText)
        {
            return newText == null || newText.trim().isEmpty()
                            ? MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnLabel)
                            : null;
        }
    }

    private static final class ValidXMLValidator implements IInputValidator
    {
        private SAXParserFactory spf;

        public ValidXMLValidator()
        {
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
        }

        @Override
        public String isValid(String newText)
        {
            if (newText == null || newText.trim().isEmpty())
                return MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnLabel);

            try
            {
                SAXParser parser = spf.newSAXParser();

                parser.parse(new ByteArrayInputStream(("<text>" + newText + "</text>") //$NON-NLS-1$ //$NON-NLS-2$
                                .getBytes(StandardCharsets.UTF_8)), new DefaultHandler());
            }
            catch (Exception e)
            {
                return e.getLocalizedMessage();
            }

            return null;
        }
    }

    private final WidgetDelegate<?> delegate;

    private boolean isMultiLine = false;
    private boolean containsTags = false;

    public LabelConfig(WidgetDelegate<?> delegate)
    {
        this.delegate = delegate;
    }

    public void setMultiLine(boolean isMultiLine)
    {
        this.isMultiLine = isMultiLine;
    }

    public void setContainsTags(boolean containsTags)
    {
        this.containsTags = containsTags;
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        var label = delegate.getWidget().getLabel();

        if (containsTags)
            label = TextUtil.stripTags(label);

        manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME,
                        new LabelOnly(TextUtil.tooltip(TextUtil.limit(label, 60))));

        manager.add(new SimpleAction(Messages.MenuRenameLabel, a -> {

            InputDialog dialog = isMultiLine ? createMultiLine() : createSingleLine();

            if (dialog.open() != Window.OK)
                return;

            delegate.getWidget().setLabel(dialog.getValue());

            delegate.update();
            delegate.getClient().touch();
        }));
    }

    private InputDialog createSingleLine()
    {
        return new InputDialog(Display.getCurrent().getActiveShell(), Messages.MenuRenameLabel, Messages.ColumnLabel,
                        delegate.getWidget().getLabel(), new NotEmptyValidator());
    }

    private InputDialog createMultiLine()
    {
        return new InputDialog(Display.getCurrent().getActiveShell(), Messages.MenuRenameLabel, Messages.ColumnLabel,
                        delegate.getWidget().getLabel(), new ValidXMLValidator())
        {
            @Override
            protected Control createDialogArea(Composite parent)
            {
                Control area = super.createDialogArea(parent);
                Text text = getText();

                GridDataFactory.fillDefaults().grab(true, true)
                                .hint(SWTHelper.stringWidth(text, "X") * 60, SWTHelper.lineHeight(text) * 4) //$NON-NLS-1$
                                .applyTo(text);

                return area;
            }

            @Override
            protected int getInputTextStyle()
            {
                return SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP;
            }

        };
    }

    @Override
    public String getLabel()
    {
        var label = delegate.getWidget().getLabel();

        if (containsTags)
            label = TextUtil.stripTags(label);

        return Messages.ColumnLabel + ": " + label; //$NON-NLS-1$
    }
}
