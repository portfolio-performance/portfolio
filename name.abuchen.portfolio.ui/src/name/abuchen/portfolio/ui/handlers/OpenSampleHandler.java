package name.abuchen.portfolio.ui.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.parts.SampleMessages;
import name.abuchen.portfolio.ui.util.ReplaceFilterInputStream;

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
                    try
                    {
                        InputStream inputStream = new ReplaceFilterInputStream(this.getClass().getResourceAsStream(sampleFile), buildNlsReplaceMap());
                        final Client client = ClientFactory.load(inputStream, monitor);

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
        catch (InvocationTargetException e)
        {
            PortfolioPlugin.log(e);
        }
        catch (InterruptedException e)
        {
            PortfolioPlugin.log(e);
        }
    }
    
    private static Map<byte[], byte[]> buildNlsReplaceMap()
    {
        Map<byte[], byte[]> map = new HashMap<byte[], byte[]>();
        List<Field> fields = getAllNlsMessageFields();
        try 
        {
            for(Field f : fields)
            {
                String sval = (String) f.get(null);
                String skey = "${" + f.getName() + "}";
                map.put(skey.getBytes("UTF-8"), sval.getBytes("UTF-8"));
            }
        }
        catch (IllegalArgumentException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
        catch (UnsupportedEncodingException e)
        {
           throw new RuntimeException(e.getMessage(), e);
        }
        return map;
    }

    private static List<Field> getAllNlsMessageFields()
    {
        List<Field> fields = new ArrayList<>();
        for(Field f : SampleMessages.class.getFields())
        {
            if(Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers()) && f.getType().equals(String.class))
            {
                fields.add(f);
            }
        }
        return fields;
    }
}
