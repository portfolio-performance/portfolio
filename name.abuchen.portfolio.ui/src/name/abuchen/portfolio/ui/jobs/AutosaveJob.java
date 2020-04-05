package name.abuchen.portfolio.ui.jobs;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.Messages;

public final class AutosaveJob extends AbstractClientJob
{
    private ClientInput clientInput;
    private Object clazz;
    private String identifier;
    private long   heartbeatPeriod;

    public AutosaveJob(ClientInput clientInput)
    {
        super(clientInput.getClient(), Messages.JobLabelAutosave);
        this.clientInput = clientInput;
    }

    public AutosaveJob setHeartbeat(long milliseconds)
    {
        heartbeatPeriod = milliseconds;
        return this;
    }

    public AutosaveJob repeatEvery(Object clazz, String identifier)
    {
        this.clazz      = clazz;
        this.identifier = identifier;
        return this;
    }

    private long getRepeatPeriod()
    {
        Object value;
        PropertyDescriptor descriptor = null;
        Object attributable = null;
        try
        {
            descriptor = descriptorFor(clazz.getClass(), identifier);
            attributable = Adaptor.adapt(clazz.getClass(), clazz);
            value = descriptor.getReadMethod().invoke(attributable);
        }
        catch (Exception e)
        {
            throw new RuntimeException(String.format("Descriptor failed with exception <%s>", e)); //$NON-NLS-1$
        }
        return (long) value * 1000 * 60; // value is given in minutes
    }

    private Boolean hasAutosaveDatestamp()
    {
        Object value;
        PropertyDescriptor descriptor = null;
        Object attributable = null;
        try
        {
            descriptor = descriptorFor(clazz.getClass(), "autosaveDatestamp"); //$NON-NLS-1$
            attributable = Adaptor.adapt(clazz.getClass(), clazz);
            value = descriptor.getReadMethod().invoke(attributable);
        }
        catch (Exception e)
        {
            throw new RuntimeException(String.format("Descriptor failed with exception <%s>", e)); //$NON-NLS-1$
        }
        return (Boolean) value;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor)
    {
        List<Security> toBeSynced = getClient().getSecurities().stream().filter(s -> s.getOnlineId() != null)
                        .collect(Collectors.toList());

        long   repeatPeriod = getRepeatPeriod();
        if (repeatPeriod > 0)
            schedule(repeatPeriod);
        else
        {
            schedule(heartbeatPeriod);
            return Status.OK_STATUS;
        }

        if (clientInput.isDirty() && clientInput.getFile() != null)
        {
            File file = clientInput.getFile();
            String filename = file.getName();
            LocalDate localDate = LocalDate.now();
            String suffix = "autosave"; //$NON-NLS-1$
            if (hasAutosaveDatestamp())
                suffix = suffix + "-" + DateTimeFormatter.ofPattern("yyyyMMdd").format(localDate);  //$NON-NLS-1$  //$NON-NLS-2$

            int l = filename.lastIndexOf('.');
            String autosaveName = l > 0 ? filename.substring(0, l) + '.' + suffix + filename.substring(l)
                            : filename + '.' + suffix;
            Path sourceFile = file.toPath();
            File autosaveFile = sourceFile.resolveSibling(autosaveName).toFile();
            try
            {
                ClientFactory.save(clientInput.getClient(), autosaveFile, null, null);
            }
            catch (IOException e)
            {
            	return new Status(IStatus.WARNING, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e));
            }
        }

        return Status.OK_STATUS;
    }

    protected PropertyDescriptor descriptorFor(Class<?> subjectType, String attributeName)
    {
        try
        {
            PropertyDescriptor[] properties = Introspector.getBeanInfo(subjectType).getPropertyDescriptors();
            for (PropertyDescriptor p : properties)
                if (attributeName.equals(p.getName()))
                    return p;
            throw new IllegalArgumentException(String.format("%s has no property named %s", subjectType //$NON-NLS-1$
                            .getName(), attributeName));
        }
        catch (IntrospectionException e)
        {
            throw new IllegalArgumentException(e);
        }
    }
}
