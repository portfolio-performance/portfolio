package name.abuchen.portfolio.ui.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;

public class IniFileManipulator
{
    public static final String SWT_AUTOSCALE = "swt.autoScale"; //$NON-NLS-1$
    public static final String SWT_AUTOSCALE_UPDATEONRUNTIME = "swt.autoScale.updateOnRuntime"; //$NON-NLS-1$

    private static final String VMARGS = "-vmargs"; //$NON-NLS-1$
    private static final String SWT_AUTOSCALE_QUARTER = "quarter"; //$NON-NLS-1$
    private static final String SWT_AUTOSCALE_EXACT = "exact"; //$NON-NLS-1$

    private List<String> lines = new ArrayList<>();
    private boolean isDirty = false;

    public void load() throws IOException
    {
        lines = Files.readAllLines(getIniFile(), Charset.defaultCharset());
    }

    public void save() throws IOException
    {
        Files.write(getIniFile(), lines, Charset.defaultCharset());
        isDirty = false;
    }

    public Path getIniFile()
    {
        return getIniFile(System.getProperty("eclipse.launcher"), Platform.getOS()); //$NON-NLS-1$
    }

    static Path getIniFile(String eclipseLauncher, String os)
    {
        var path = Paths.get(eclipseLauncher);

        var executable = path.getFileName().toString();
        var p = executable.lastIndexOf('.');
        var iniFileName = (p > 0 ? executable.substring(0, p) : executable) + ".ini"; //$NON-NLS-1$

        var directory = path.getParent();
        if (Platform.OS_MACOSX.equals(os))
            directory = directory.getParent().resolve("Eclipse"); //$NON-NLS-1$

        return directory.resolve(iniFileName);
    }

    private static boolean isMonitorSpecificCompatibleAutoScale(String value)
    {
        return value == null || SWT_AUTOSCALE_QUARTER.equals(value) || SWT_AUTOSCALE_EXACT.equals(value);
    }

    public void setSwtAutoScale(String value)
    {
        if (value == null)
        {
            removeVmProperty(SWT_AUTOSCALE);
            removeVmProperty(SWT_AUTOSCALE_UPDATEONRUNTIME);
        }
        else
        {
            setVmProperty(SWT_AUTOSCALE, value);
            if (!isMonitorSpecificCompatibleAutoScale(value))
                setVmProperty(SWT_AUTOSCALE_UPDATEONRUNTIME, Boolean.FALSE.toString());
        }
    }

    public String getVmProperty(String key)
    {
        var propertyPrefix = "-D" + key + "="; //$NON-NLS-1$ //$NON-NLS-2$
        var vmArgsIndex = getVmArgsIndex();
        if (vmArgsIndex < 0)
            return null;

        for (int ii = vmArgsIndex + 1; ii < lines.size(); ii++)
        {
            var trimmed = lines.get(ii).trim();
            if (trimmed.startsWith(propertyPrefix))
                return trimmed.substring(propertyPrefix.length());
        }

        return null;
    }

    public void setVmProperty(String key, String value)
    {
        var propertyPrefix = "-D" + key + "="; //$NON-NLS-1$ //$NON-NLS-2$
        var property = propertyPrefix + value;
        var vmArgsIndex = getOrCreateVmArgsIndex();

        var propertyIndex = -1;
        for (int ii = vmArgsIndex + 1; ii < lines.size(); ii++)
        {
            var trimmed = lines.get(ii).trim();
            if (!trimmed.startsWith(propertyPrefix))
                continue;

            if (propertyIndex < 0)
            {
                propertyIndex = ii;
                if (!trimmed.equals(property))
                {
                    lines.set(ii, property);
                    isDirty = true;
                }
            }
            else
            {
                lines.remove(ii);
                ii--;
                isDirty = true;
            }
        }

        if (propertyIndex < 0)
        {
            lines.add(property);
            isDirty = true;
        }
    }

    public void removeVmProperty(String key)
    {
        var iterator = lines.iterator();
        var vmArgsFound = false;
        var propertyPrefix = "-D" + key + "="; //$NON-NLS-1$ //$NON-NLS-2$

        while (iterator.hasNext())
        {
            var trimmed = iterator.next().trim();
            if (trimmed.equals(VMARGS))
            {
                vmArgsFound = true;
            }
            else if (vmArgsFound && trimmed.startsWith(propertyPrefix))
            {
                iterator.remove();
                isDirty = true;
            }
        }
    }

    public boolean isDirty()
    {
        return isDirty;
    }

    private int getOrCreateVmArgsIndex()
    {
        var vmArgsIndex = getVmArgsIndex();
        if (vmArgsIndex >= 0)
            return vmArgsIndex;

        lines.add(VMARGS);
        isDirty = true;
        return lines.size() - 1;
    }

    private int getVmArgsIndex()
    {
        for (int ii = 0; ii < lines.size(); ii++)
        {
            if (lines.get(ii).trim().equals(VMARGS))
                return ii;
        }

        return -1;
    }

}
