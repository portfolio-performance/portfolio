package name.abuchen.portfolio.ui.wizards.security;

import java.util.List;
import java.util.Objects;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import name.abuchen.portfolio.online.QuoteFeedData.RawResponse;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.SWTHelper;

public class RawResponsesDialog extends Dialog
{
    private final List<RawResponse> rawResponses;

    private Text rawText;

    public RawResponsesDialog(Shell parentShell, List<RawResponse> rawResponses)
    {
        super(parentShell);
        this.rawResponses = Objects.requireNonNull(rawResponses);
    }

    @Override
    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText(Messages.LabelInfo);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);

        Composite editArea = new Composite(composite, SWT.NONE);
        editArea.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
        editArea.setLayout(new FormLayout());

        ComboViewer comboURL = SWTHelper.createComboViewer(editArea);
        comboURL.setContentProvider(ArrayContentProvider.getInstance());
        comboURL.setLabelProvider(new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((RawResponse) element).getUrl();
            }
        });
        comboURL.setInput(rawResponses);
        comboURL.addSelectionChangedListener(event -> {
            RawResponse response = (RawResponse) event.getStructuredSelection().getFirstElement();
            rawText.setText(response.getContent() != null ? response.getContent() : ""); //$NON-NLS-1$
        });

        rawText = new Text(editArea, SWT.READ_ONLY | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);

        FormDataFactory.startingWith(comboURL.getControl()).left(new FormAttachment(0, 10))
                        .right(new FormAttachment(100, -10)).width(500) //
                        .thenBelow(rawText, 10).left(new FormAttachment(0, 10)).right(new FormAttachment(100, -10))
                        .width(500).height(300);

        if (!rawResponses.isEmpty())
            comboURL.setSelection(new StructuredSelection(rawResponses.get(0)));

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);

        Button button = createButton(parent, 9999, Messages.LabelCopyToClipboard, false);
        button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            if (rawText.isDisposed())
                return;
            Clipboard cb = new Clipboard(Display.getCurrent());
            TextTransfer textTransfer = TextTransfer.getInstance();
            cb.setContents(new Object[] { rawText.getText() }, new Transfer[] { textTransfer });
        }));

        button = createButton(parent, 9998, Messages.LabelFormatJSON, false);
        button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            if (rawText.isDisposed())
                return;

            try
            {
                JsonElement jsonElement = new JsonParser().parse(rawText.getText());
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                rawText.setText(gson.toJson(jsonElement));
            }
            catch (JsonSyntaxException ignore)
            {
                // text is not json, do nothing
            }
        }));

        button = createButton(parent, 9997, Messages.LabelCleanHTML, false);
        button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            if (rawText.isDisposed())
                return;

            rawText.setText(Jsoup.clean(rawText.getText(), Whitelist.relaxed()));
        }));

    }
}
