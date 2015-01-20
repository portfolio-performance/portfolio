package name.abuchen.portfolio.ui.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Named;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.util.ProgressMonitorInputStream;
import name.abuchen.portfolio.util.TokenReplacingReader;
import name.abuchen.portfolio.util.TokenReplacingReader.ITokenResolver;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

public class OpenSampleHandler
{
    @Inject
    private UISynchronize sync;

    private static final ResourceBundle RESOURCES = ResourceBundle
                    .getBundle("name.abuchen.portfolio.ui.parts.samplemessages"); //$NON-NLS-1$

    @Execute
    public void execute(
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell, //
                    final MApplication app, //
                    final EPartService partService, final EModelService modelService,
                    @Named(UIConstants.Parameter.SAMPLE_FILE) final String sampleFile)
    {
        try
        {
            IRunnableWithProgress loadResourceOperation = new IRunnableWithProgress()
            {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try (InputStream in = this.getClass().getResourceAsStream(sampleFile))
                    {
                        InputStream inputStream = new ProgressMonitorInputStream(in, monitor);
                        Reader replacingReader = new TokenReplacingReader(new InputStreamReader(inputStream,
                                        StandardCharsets.UTF_8), buildResourcesTokenResolver());

                        final Client client = ClientFactory.load(replacingReader);

                        sync.asyncExec(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                MPart part = partService.createPart(UIConstants.Part.PORTFOLIO);
                                part.setLabel(sampleFile.substring(sampleFile.lastIndexOf('/') + 1));
                                part.getTransientData().put(Client.class.getName(), client);

                                MPartStack stack = (MPartStack) modelService.find(UIConstants.PartStack.MAIN, app);
                                stack.getChildren().add(part);

                                partService.showPart(part, PartState.ACTIVATE);
                            }
                        });
                    }
                    catch (IOException ignore)
                    {
                        PortfolioPlugin.log(ignore);
                    }
                }

            };
            new ProgressMonitorDialog(shell).run(true, true, loadResourceOperation);
        }
        catch (InvocationTargetException | InterruptedException e)
        {
            PortfolioPlugin.log(e);
        }
    }

    private static ITokenResolver buildResourcesTokenResolver()
    {
        return new ITokenResolver()
        {
            @Override
            public String resolveToken(String tokenName)
            {
                try
                {
                    return RESOURCES.getString(tokenName);
                }
                catch (MissingResourceException e)
                {
                    return tokenName;
                }
            }
        };
    }

}
