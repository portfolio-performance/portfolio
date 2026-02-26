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
    private static final String SWT_AUTOSCALE_UPDATEONRUNTIME = "-Dswt.autoScale.updateOnRuntime=true"; //$NON-NLS-1$

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
        var eclipseLauncher = System.getProperty("eclipse.launcher"); //$NON-NLS-1$

        var path = Paths.get(eclipseLauncher);

        var executable = path.getFileName().toString();
        var p = executable.lastIndexOf('.');
        var iniFileName = (p > 0 ? executable.substring(0, p) : executable) + ".ini"; //$NON-NLS-1$

        var directory = path.getParent();
        if (Platform.OS_MACOSX.equals(Platform.getOS()))
            directory = directory.getParent().resolve("Eclipse"); //$NON-NLS-1$

        return directory.resolve(iniFileName);
    }

    public boolean hasMonitorSpecificScaling()
    {
        boolean hasMonitorSpecificScaling = false;

        for (String line : lines)
        {
            String trimmed = line.trim();
            if (trimmed.equals(SWT_AUTOSCALE_UPDATEONRUNTIME))
                hasMonitorSpecificScaling = true;
        }

        return hasMonitorSpecificScaling;
    }

    public void addMonitorSpecificScaling()
    {
        boolean hasMonitorSpecificScaling = false;

        for (String line : lines)
        {
            String trimmed = line.trim();
            if (trimmed.equals(SWT_AUTOSCALE_UPDATEONRUNTIME))
                hasMonitorSpecificScaling = true;
        }

        if (!hasMonitorSpecificScaling)
        {
            lines.add(SWT_AUTOSCALE_UPDATEONRUNTIME);
            isDirty = true;
        }
    }

    public void removeMonitorSpecificScaling()
    {
        var iterator = lines.iterator();
        while (iterator.hasNext())
        {
            var trimmed = iterator.next().trim();
            if (trimmed.equals(SWT_AUTOSCALE_UPDATEONRUNTIME))
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
}
