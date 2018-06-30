package name.abuchen.portfolio.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Based on org.eclipse.team.internal.core.streams.ProgressMonitorInputStream
 */
public class ProgressMonitorInputStream extends FilterInputStream
{
    private IProgressMonitor monitor;
    private int updateIncrement;
    private long bytesRead = 0;
    private long lastUpdate = -1;
    private long nextUpdate = 0;

    public ProgressMonitorInputStream(InputStream in, int updateIncrement, IProgressMonitor monitor)
    {
        super(in);
        this.updateIncrement = updateIncrement;
        this.monitor = monitor;
    }

    /**
     * Constructs a ProgressMonitorInputStream by using
     * {@link java.io.InputStream#available()} to determine the total length of
     * the stream.
     */
    public ProgressMonitorInputStream(InputStream in, IProgressMonitor monitor) throws IOException
    {
        this(in, (int) Math.min(in.available() / 20L, Integer.MAX_VALUE), monitor);
    }

    @Override
    public void close() throws IOException
    {
        try
        {
            in.close();
        }
        finally
        {
            monitor.done();
        }
    }

    @Override
    public int read() throws IOException
    {
        int b = in.read();
        if (b != -1)
        {
            bytesRead += 1;
            update();
        }
        return b;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException
    {
        try
        {
            int count = in.read(buffer, offset, length);
            if (count != -1)
            {
                bytesRead += count;
                update();
            }
            return count;
        }
        catch (InterruptedIOException e)
        {
            bytesRead += e.bytesTransferred;
            update();
            throw e;
        }
    }

    @Override
    public long skip(long amount) throws IOException
    {
        try
        {
            long count = in.skip(amount);
            bytesRead += count;
            update();
            return count;
        }
        catch (InterruptedIOException e)
        {
            bytesRead += e.bytesTransferred;
            update();
            throw e;
        }
    }

    @Override
    public boolean markSupported()
    {
        return false;
    }

    private void update()
    {
        if (bytesRead >= nextUpdate)
        {
            nextUpdate = bytesRead - (bytesRead % updateIncrement);
            if (nextUpdate != lastUpdate)
                monitor.worked(1);
            lastUpdate = nextUpdate;
            nextUpdate += updateIncrement;
        }
    }
}
