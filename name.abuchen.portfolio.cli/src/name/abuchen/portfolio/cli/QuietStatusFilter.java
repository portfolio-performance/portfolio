package name.abuchen.portfolio.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Drops the INFO and OK status lines that core prints to the console, such as
 * <code>Status INFO: ...ClientFactory code=0 Loaded ...</code>, and passes
 * everything else on, line by line. Outside OSGi, core logs by printing the
 * status to <code>System.err</code>, and the CLI module can't change that
 * ({@code docs/adr/0002}).
 */
final class QuietStatusFilter extends OutputStream
{
    private static final List<byte[]> DROPPED = List.of("Status INFO:".getBytes(StandardCharsets.US_ASCII),
                    "Status OK:".getBytes(StandardCharsets.US_ASCII));

    private final OutputStream out;
    private final ByteArrayOutputStream pending = new ByteArrayOutputStream();

    private boolean passing;
    private boolean dropping;

    QuietStatusFilter(OutputStream out)
    {
        this.out = out;
    }

    @Override
    public void write(int b) throws IOException
    {
        if (passing)
        {
            out.write(b);
            passing = b != '\n';
        }
        else if (dropping)
        {
            dropping = b != '\n';
        }
        else
        {
            pending.write(b);
            decide(b == '\n');
        }
    }

    /** Decides what to do with a line once enough of it has arrived to tell. */
    private void decide(boolean endOfLine) throws IOException
    {
        var start = pending.toByteArray();

        if (DROPPED.stream().anyMatch(prefix -> startsWith(start, prefix)))
        {
            pending.reset();
            dropping = !endOfLine;
        }
        else if (endOfLine || DROPPED.stream().noneMatch(prefix -> startsWith(prefix, start)))
        {
            emitPending();
            passing = !endOfLine;
        }
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix)
    {
        if (bytes.length < prefix.length)
            return false;
        for (int ii = 0; ii < prefix.length; ii++)
        {
            if (bytes[ii] != prefix[ii])
                return false;
        }
        return true;
    }

    private void emitPending() throws IOException
    {
        pending.writeTo(out);
        pending.reset();
    }

    @Override
    public void flush() throws IOException
    {
        out.flush();
    }

    /** Passes on a trailing partial line that never turned out to be a dropped one. */
    @Override
    public void close() throws IOException
    {
        emitPending();
        out.flush();
    }
}
