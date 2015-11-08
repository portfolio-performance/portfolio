package name.abuchen.portfolio.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.core.services.statusreporter.StatusReporter;
import org.eclipse.e4.ui.internal.workbench.swt.IEventLoopAdvisor;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.e4.ui.workbench.lifecycle.PreSave;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

@SuppressWarnings("restriction")
public class LifeCycleManager
{
    @PostContextCreate
    public void doPostContextCreate(IEclipseContext context, Logger logger)
    {
        checkForJava8();
        setupEventLoopAdvisor(context, logger);
    }

    public void checkForJava8()
    {
        // if the java version is < 8, show a message dialog because otherwise
        // the application would silently not start

        double version = Double.parseDouble(System.getProperty("java.specification.version")); //$NON-NLS-1$

        if (version < 1.8)
        {
            MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.TitleJavaVersion,
                            Messages.MsgMinimumRequiredVersion);
            throw new UnsupportedOperationException("The minimum Java version required is Java 8"); //$NON-NLS-1$
        }
    }

    public void setupEventLoopAdvisor(final IEclipseContext context, final Logger logger)
    {
        // do not show an error popup if is the annoying NPE on El Capitan
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=434393

        context.set(IEventLoopAdvisor.class, new IEventLoopAdvisor()
        {
            @Override
            public void eventLoopIdle(final Display display)
            {
                display.sleep();
            }

            @Override
            public void eventLoopException(final Throwable exception)
            {
                boolean isAnnoyingNullPointerOnElCapitan = isAnnoyingNullPointerOnElCapitan(exception);

                StatusReporter statusReporter = (StatusReporter) context.get(StatusReporter.class.getName());
                if (!isAnnoyingNullPointerOnElCapitan && statusReporter != null)
                {
                    statusReporter.show(StatusReporter.ERROR, "Internal Error", exception); //$NON-NLS-1$
                }
                else if (logger != null)
                {
                    logger.error(exception);
                }
                else
                {
                    exception.printStackTrace();
                }
            }

            private boolean isAnnoyingNullPointerOnElCapitan(Throwable exception)
            {
                if (!(exception instanceof NullPointerException))
                    return false;

                StackTraceElement[] stackTrace = exception.getStackTrace();

                if (!"org.eclipse.swt.widgets.Control".equals(stackTrace[0].getClassName())) //$NON-NLS-1$
                    return false;

                if (!"internal_new_GC".equals(stackTrace[0].getMethodName())) //$NON-NLS-1$
                    return false;

                return true;
            }
        });
    }

    @PreSave
    public void removePortfolioPartsWithoutPersistedFile(MApplication app, EPartService partService,
                    EModelService modelService)
    {
        MPartStack stack = (MPartStack) modelService.find("name.abuchen.portfolio.ui.partstack.main", app); //$NON-NLS-1$

        List<MStackElement> toBeRemoved = new ArrayList<MStackElement>();

        for (MStackElement child : stack.getChildren())
        {
            if (!(child instanceof MPart))
                continue;

            if (!"name.abuchen.portfolio.ui.part.portfolio".equals(child.getElementId())) //$NON-NLS-1$
                continue;

            String filename = child.getPersistedState().get("file"); //$NON-NLS-1$
            if (filename == null)
                toBeRemoved.add(child);
        }

        if (!toBeRemoved.isEmpty())
        {
            if (toBeRemoved.contains(stack.getSelectedElement()))
                stack.setSelectedElement(null);
            stack.getChildren().removeAll(toBeRemoved);
        }
    }
}
