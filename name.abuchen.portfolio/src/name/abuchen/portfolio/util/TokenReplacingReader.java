//
// Thanks to Jakob Jenkov for his tutorial at
// http://tutorials.jenkov.com/java-howto/replace-strings-in-streams-arrays-files.html
//

package name.abuchen.portfolio.util;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;

public class TokenReplacingReader extends Reader
{
    public interface ITokenResolver
    {
        public String resolveToken(String tokenName) throws IOException;
    }

    protected PushbackReader pushbackReader = null;
    protected ITokenResolver tokenResolver = null;
    protected StringBuilder tokenNameBuffer = new StringBuilder();
    protected String tokenValue = null;
    protected int tokenValueIndex = 0;

    public TokenReplacingReader(Reader source, ITokenResolver resolver)
    {
        this.pushbackReader = new PushbackReader(source, 2);
        this.tokenResolver = resolver;
    }

    @Override
    public int read() throws IOException
    {
        if (this.tokenValue != null)
        {
            if (this.tokenValueIndex < this.tokenValue.length())
                return this.tokenValue.charAt(this.tokenValueIndex++);

            if (this.tokenValueIndex == this.tokenValue.length())
            {
                this.tokenValue = null;
                this.tokenValueIndex = 0;
            }
        }

        int data = this.pushbackReader.read();
        if (data != '$')
            return data;

        data = this.pushbackReader.read();
        if (data != '{')
        {
            this.pushbackReader.unread(data);
            return '$';
        }
        this.tokenNameBuffer.delete(0, this.tokenNameBuffer.length());

        data = this.pushbackReader.read();
        while (data != '}')
        {
            this.tokenNameBuffer.append((char) data);
            data = this.pushbackReader.read();
        }

        this.tokenValue = this.tokenResolver.resolveToken(this.tokenNameBuffer.toString());

        if (this.tokenValue == null)
            this.tokenValue = "${" + this.tokenNameBuffer.toString() + "}"; //$NON-NLS-1$ //$NON-NLS-2$

        if (this.tokenValue.length() == 0)
            return read();

        return this.tokenValue.charAt(this.tokenValueIndex++);
    }

    @Override
    public int read(char[] cbuf) throws IOException
    {
        return read(cbuf, 0, cbuf.length);
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException
    {
        int charsRead = 0;
        for (int i = 0; i < len; i++)
        {
            int nextChar = read();
            if (nextChar == -1)
            {
                if (charsRead == 0)
                {
                    charsRead = -1;
                }
                break;
            }
            charsRead = i + 1;
            cbuf[off + i] = (char) nextChar;
        }
        return charsRead;
    }

    @Override
    public void close() throws IOException
    {
        this.pushbackReader.close();
    }

    @Override
    public long skip(long n) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean ready() throws IOException
    {
        return this.pushbackReader.ready();
    }

    @Override
    public boolean markSupported()
    {
        return false;
    }

    @Override
    public void mark(int readAheadLimit) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() throws IOException
    {
        throw new UnsupportedOperationException();
    }
}
