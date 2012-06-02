package name.abuchen.portfolio.ui.app;

import java.net.MalformedURLException;
import java.net.URL;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.core.commands.common.CommandException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.handlers.IHandlerService;

public class IntroPart extends org.eclipse.ui.part.IntroPart implements IHyperlinkListener
{

    @Override
    public void createPartControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        container.setLayout(new FormLayout());

        FormToolkit toolkit = new FormToolkit(container.getDisplay());
        Form form = toolkit.createForm(container);

        FormData data = new FormData();
        data.top = new FormAttachment(0, 20);
        data.left = new FormAttachment(50, -100);
        data.right = new FormAttachment(50, 100);
        data.bottom = new FormAttachment(100, -20);

        form.setLayoutData(data);

        // form.setText(Messages.IntroTitle);

        FillLayout layout = new FillLayout();
        layout.marginHeight = layout.marginWidth = 5;
        form.getBody().setLayout(layout);

        FormText text = toolkit.createFormText(form.getBody(), true);

        StringBuilder buf = new StringBuilder();
        buf.append("<form>"); //$NON-NLS-1$
        buf.append("<p><img href=\"logo\"/></p>"); //$NON-NLS-1$
        buf.append("<p><span color=\"header\" font=\"header\">") //$NON-NLS-1$
                        .append(Messages.IntroTitle) //
                        .append("</span></p>"); //$NON-NLS-1$
        addLink(buf, "action:open", Messages.IntroOpenFile, Messages.IntroOpenFileText); //$NON-NLS-1$
        addLink(buf, "action:new", Messages.IntroNewFile, Messages.IntroNewFileText); //$NON-NLS-1$
        addLink(buf, "action:sample", Messages.IntroOpenSample, Messages.IntroOpenSampleText); //$NON-NLS-1$
        addLink(buf, "http://buchen.github.com/portfolio/new_and_noteworthy.html", //$NON-NLS-1$
                        Messages.IntroReadNews, Messages.IntroReadNewsText);
        buf.append("</form>"); //$NON-NLS-1$
        text.setText(buf.toString(), true, false);
        text.setImage("logo", PortfolioPlugin.getDefault().getImageRegistry().get(PortfolioPlugin.IMG_LOGO)); //$NON-NLS-1$
        text.setColor("header", toolkit.getColors().getColor(IFormColors.TITLE)); //$NON-NLS-1$
        text.setFont("header", JFaceResources.getHeaderFont()); //$NON-NLS-1$
        text.addHyperlinkListener(this);

    }

    private void addLink(StringBuilder buf, String target, String label, String subtext)
    {
        buf.append("<p><a href=\"").append(target).append("\">"); //$NON-NLS-1$ //$NON-NLS-2$
        buf.append(label).append("</a><br/>"); //$NON-NLS-1$
        buf.append(subtext).append("</p>"); //$NON-NLS-1$
    }

    @Override
    public void linkActivated(HyperlinkEvent event)
    {
        try
        {
            String target = (String) event.getHref();

            if ("action:open".equals(target)) //$NON-NLS-1$
            {
                IHandlerService handlerService = (IHandlerService) getIntroSite().getService(IHandlerService.class);
                Object result = handlerService.executeCommand(
                                "name.abuchen.portfolio.ui.commands.openFileCommand", null); //$NON-NLS-1$
                if (result != null)
                    PlatformUI.getWorkbench().getIntroManager().closeIntro(this);
            }
            else if ("action:new".equals(target)) //$NON-NLS-1$
            {
                IHandlerService handlerService = (IHandlerService) getIntroSite().getService(IHandlerService.class);
                handlerService.executeCommand("name.abuchen.portfolio.ui.commands.newFileCommand", null); //$NON-NLS-1$
                PlatformUI.getWorkbench().getIntroManager().closeIntro(this);
            }
            else if ("action:sample".equals(target)) //$NON-NLS-1$
            {
                IHandlerService handlerService = (IHandlerService) getIntroSite().getService(IHandlerService.class);
                handlerService.executeCommand("name.abuchen.portfolio.ui.commands.openSampleCommand", null); //$NON-NLS-1$
                PlatformUI.getWorkbench().getIntroManager().closeIntro(this);
            }
            else if (target.startsWith("http://")) //$NON-NLS-1$
            {
                IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
                IWebBrowser browser = support.getExternalBrowser();
                browser.openURL(new URL(target));
            }

        }
        catch (CommandException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(getIntroSite().getShell(), Messages.LabelError, e.getMessage());
        }
        catch (PartInitException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(getIntroSite().getShell(), Messages.LabelError, e.getMessage());
        }
        catch (MalformedURLException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(getIntroSite().getShell(), Messages.LabelError, e.getMessage());
        }
    }

    @Override
    public void linkEntered(HyperlinkEvent event)
    {}

    @Override
    public void linkExited(HyperlinkEvent event)
    {}

    @Override
    public void setFocus()
    {}

    @Override
    public void standbyStateChanged(boolean standby)
    {}

}
