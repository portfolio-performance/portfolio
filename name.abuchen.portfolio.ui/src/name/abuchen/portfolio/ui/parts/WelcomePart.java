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
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
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
import name.abuchen.portfolio.ui.util.RecentFilesCache;

@SuppressWarnings("restriction")
public class WelcomePart
{
    private static final String OPEN = "open:"; //$NON-NLS-1$

    @Inject
    private ECommandService commandService;

    @Inject
    private EHandlerService handlerService;

    @Inject
    private EPartService partService;

    @Inject
    private RecentFilesCache recentFiles;

    private Composite recentFilesComposite;
    private Font boldFont;

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
        boldFont = new Font(container.getDisplay(), fD[0]);

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
        GridLayoutFactory.fillDefaults().numColumns(2).spacing(20, 20).applyTo(actions);

        Label title = new Label(actions, SWT.NONE);
        title.setText(Messages.LabelPortfolioPerformance);
        title.setFont(bigFont);
        GridDataFactory.fillDefaults().span(2, 1).applyTo(title);

        Label version = new Label(actions, SWT.NONE);
        version.setText(PortfolioPlugin.getDefault().getBundle().getVersion().toString());
        GridDataFactory.fillDefaults().span(2, 1).applyTo(version);

        Composite links = new Composite(actions, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(links);
        GridLayoutFactory.fillDefaults().applyTo(links);

        addSectionLabel(boldFont, links, Messages.IntroLabelActions);
        addLink(links, "action:open", Messages.IntroOpenFile, Messages.IntroOpenFileText); //$NON-NLS-1$
        addLink(links, "action:new", Messages.IntroNewFile, Messages.IntroNewFileText); //$NON-NLS-1$

        addSectionLabel(boldFont, links, Messages.IntroLabelSamples);
        addLink(links, "action:sample", Messages.IntroOpenSample, Messages.IntroOpenSampleText); //$NON-NLS-1$
        addLink(links, "action:daxsample", Messages.IntroOpenDaxSample, Messages.IntroOpenDaxSampleText); //$NON-NLS-1$

        addSectionLabel(boldFont, links, Messages.IntroLabelHelp);
        addLink(links, "https://forum.portfolio-performance.info", //$NON-NLS-1$
                        Messages.IntroOpenForum, Messages.IntroOpenForumText);
        addLink(links, "https://forum.portfolio-performance.info/c/how-to", //$NON-NLS-1$
                        Messages.IntroOpenHowtos, Messages.IntroOpenHowtosText);
        addLink(links, "https://forum.portfolio-performance.info/c/faq", //$NON-NLS-1$
                        Messages.IntroOpenFAQ, Messages.IntroOpenFAQText);

        createRecentFilesComposite(actions);
    }

    private void createRecentFilesComposite(Composite actions)
    {
        recentFilesComposite = new Composite(actions, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(recentFilesComposite);
        GridLayoutFactory.fillDefaults().applyTo(recentFilesComposite);

        addSectionLabel(boldFont, recentFilesComposite, Messages.IntroLabelRecentlyUsedFiles);

        for (String file : recentFiles.getRecentFiles())
        {
            String name = Path.fromOSString(file).lastSegment();
            addLink(recentFilesComposite, OPEN + file, name, null);
        }
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

        if (subtext != null)
        {
            Label l = new Label(container, SWT.WRAP);
            l.setText(subtext);
            GridDataFactory.fillDefaults().indent(0, -3).applyTo(l);
        }
    }

    public void linkActivated(String target)
    {
        if (target.startsWith(OPEN))
        {
            String file = target.substring(OPEN.length());

            // check if file is already opened somewhere

            java.util.Optional<MPart> part = partService.getParts().stream()
                            .filter(p -> UIConstants.Part.PORTFOLIO.equals(p.getElementId())) //
                            .filter(p -> file.equals(p.getPersistedState().get(UIConstants.File.PERSISTED_STATE_KEY)))
                            .findAny();

            if (part.isPresent())
                partService.activate(part.get());
            else
                executeCommand(UIConstants.Command.OPEN_RECENT_FILE, UIConstants.Parameter.FILE, file);
        }
        else if ("action:open".equals(target)) //$NON-NLS-1$
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

            List<Parameterization> parameterizations = new ArrayList<>();
            if (parameters != null)
            {
                for (int ii = 0; ii < parameters.length; ii = ii + 2)
                {
                    IParameter p = cmd.getParameter(parameters[ii]);
                    parameterizations.add(new Parameterization(p, parameters[ii + 1]));
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

    @Inject
    @Optional
    public void subscribeFileTopic(@UIEventTopic(UIConstants.Event.File.ALL_SUB_TOPICS) String file)
    {
        if (recentFilesComposite == null)
            return;

        Composite parent = recentFilesComposite.getParent();
        recentFilesComposite.dispose();
        createRecentFilesComposite(parent);
        recentFilesComposite.getParent().getParent().layout(true);
    }
}
