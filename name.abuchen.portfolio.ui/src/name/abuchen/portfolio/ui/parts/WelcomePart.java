package name.abuchen.portfolio.ui.parts;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.DesktopAPI;

public class WelcomePart
{

    @Inject
    private ECommandService commandService;

    @Inject
    private EHandlerService handlerService;

    @PostConstruct
    public void createComposite(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        RowLayout layout = new RowLayout();
        layout.spacing = 20;
        layout.marginHeight = layout.marginWidth = 10;
        container.setLayout(layout);

        // create fonts
        FontData[] fD = container.getFont().getFontData();
        fD[0].setStyle(SWT.BOLD);
        final Font boldFont = new Font(container.getDisplay(), fD[0]);

        fD[0].setStyle(SWT.NORMAL);
        fD[0].setHeight(fD[0].getHeight() * 2);
        final Font bigFont = new Font(container.getDisplay(), fD[0]);

        container.addDisposeListener(e -> {
            boldFont.dispose();
            bigFont.dispose();
        });

        // first column: logo
        Label image = new Label(container, SWT.NONE);
        image.setBackground(container.getBackground());
        image.setImage(Images.LOGO_128.image());

        // second column: actions
        Composite actions = new Composite(container, SWT.NONE);
        actions.setBackground(container.getBackground());
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(actions);

        Label title = new Label(actions, SWT.NONE);
        title.setText(Messages.LabelPortfolioPerformance);
        title.setFont(bigFont);

        Label version = new Label(actions, SWT.NONE);
        version.setText(PortfolioPlugin.getDefault().getBundle().getVersion().toString());

        addSectionLabel(boldFont, actions, Messages.IntroLabelActions);
        addLink(actions, "action:open", Messages.IntroOpenFile, Messages.IntroOpenFileText); //$NON-NLS-1$
        addLink(actions, "action:new", Messages.IntroNewFile, Messages.IntroNewFileText); //$NON-NLS-1$

        addSectionLabel(boldFont, actions, Messages.IntroLabelSamples);
        addLink(actions, "action:sample", Messages.IntroOpenSample, Messages.IntroOpenSampleText); //$NON-NLS-1$
        addLink(actions, "action:daxsample", Messages.IntroOpenDaxSample, Messages.IntroOpenDaxSampleText); //$NON-NLS-1$

        addSectionLabel(boldFont, actions, Messages.IntroLabelHelp);
        addLink(actions, "https://github.com/buchen/portfolio/wiki", //$NON-NLS-1$
                        Messages.IntroOpenWIKI, Messages.IntroOpenWIKIText);
        addLink(actions, "http://buchen.github.com/portfolio/new_and_noteworthy.html", //$NON-NLS-1$
                        Messages.IntroReadNews, Messages.IntroReadNewsText);
        addLink(actions, "http://www.wertpapier-forum.de/topic/38306-portfolio-performance-mein-neues-programm/", //$NON-NLS-1$
                        Messages.IntroOpenForum, Messages.IntroOpenForumText);

    }

    private void addSectionLabel(Font boldFont, Composite actions, String label)
    {
        Label l = new Label(actions, SWT.NONE);
        l.setText(label);
        l.setFont(boldFont);
        GridDataFactory.fillDefaults().indent(0, 20).applyTo(l);
    }

    private void addLink(Composite container, final String target, String label, String subtext)
    {
        Link link = new Link(container, SWT.UNDERLINE_LINK);
        link.setText("<a>" + label + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
        link.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                linkActivated(target);
            }
        });

        Label l = new Label(container, SWT.WRAP);
        l.setText(subtext);
        GridDataFactory.fillDefaults().indent(0, -3).applyTo(l);
    }

    public void linkActivated(String target)
    {
        if ("action:open".equals(target)) //$NON-NLS-1$
        {
            executeCommand("name.abuchen.portfolio.ui.command.open"); //$NON-NLS-1$
        }
        else if ("action:new".equals(target)) //$NON-NLS-1$
        {
            executeCommand("name.abuchen.portfolio.ui.command.newclient"); //$NON-NLS-1$
        }
        else if ("action:sample".equals(target)) //$NON-NLS-1$
        {
            executeCommand("name.abuchen.portfolio.ui.command.openSample", //$NON-NLS-1$
                            UIConstants.Parameter.SAMPLE_FILE, //
                            "/" + getClass().getPackage().getName().replace('.', '/') + "/kommer.xml"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else if ("action:daxsample".equals(target)) //$NON-NLS-1$
        {
            executeCommand("name.abuchen.portfolio.ui.command.openSample", //$NON-NLS-1$
                            UIConstants.Parameter.SAMPLE_FILE, //
                            "/" + getClass().getPackage().getName().replace('.', '/') + "/dax.xml"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else if (target.startsWith("http")) //$NON-NLS-1$
        {
            DesktopAPI.browse(target);
        }
    }

    private void executeCommand(String command, String... parameters)
    {
        try
        {
            Command cmd = commandService.getCommand(command);

            List<Parameterization> parameterizations = new ArrayList<Parameterization>();
            if (parameters != null)
            {
                for (int ii = 0; ii < parameters.length; ii++)
                {
                    IParameter p = cmd.getParameter(parameters[ii]);
                    parameterizations.add(new Parameterization(p, parameters[++ii]));
                }
            }

            ParameterizedCommand pCmd = new ParameterizedCommand(cmd,
                            parameterizations.toArray(new Parameterization[0]));
            if (handlerService.canExecute(pCmd))
                handlerService.executeHandler(pCmd);
        }
        catch (NotDefinedException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError, e.getMessage());
        }
    }
}
