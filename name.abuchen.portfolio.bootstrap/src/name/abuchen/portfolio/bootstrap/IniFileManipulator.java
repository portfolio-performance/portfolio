package name.abuchen.portfolio.bootstrap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IniFileManipulator
{
    private static final String CLEAR_PERSISTED_STATE = "-clearPersistedState"; //$NON-NLS-1$

    private List<String> lines = new ArrayList<String>();
    private boolean isDirty = false;

    public void load() throws IOException
    {
        lines = readAllLines(getIniFile());
    }

    public void save() throws IOException
    {
        write(getIniFile(), lines);
        isDirty = false;
    }

    public File getIniFile()
    {
        String eclipseLauncher = System.getProperty("eclipse.launcher"); //$NON-NLS-1$

        File path = new File(eclipseLauncher);

        String executable = path.getName().toString();
        int p = executable.lastIndexOf('.');
        String iniFileName = (p > 0 ? executable.substring(0, p) : executable) + ".ini"; //$NON-NLS-1$

        return new File(path.getParentFile(), iniFileName);
    }

    public void unsetClearPersistedState()
    {
        Iterator<String> iterator = lines.iterator();
        while (iterator.hasNext())
        {
            String line = iterator.next();
            if (line.trim().equals(CLEAR_PERSISTED_STATE))
            {
                iterator.remove();
                isDirty = true;
                return;
            }
        }
    }

    public boolean isDirty()
    {
        return isDirty;
    }

    private void write(File file, List<String> lines) throws IOException
    {
        BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(file), Charset.defaultCharset()));
        try
        {
            for (String line : lines)
            {
                out.write(line);
                out.write('\n');
            }
        }
        finally
        {
            out.close();
        }
    }

    private List<String> readAllLines(File file) throws IOException
    {
        List<String> answer = new ArrayList<String>();

        FileInputStream in = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charset.defaultCharset()));

        try
        {
            String line = null;
            while ((line = reader.readLine()) != null)
                answer.add(line);
        }
        finally
        {
            reader.close();
        }

        return answer;
    }
}
