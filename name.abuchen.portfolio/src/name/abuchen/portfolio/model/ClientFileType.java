package name.abuchen.portfolio.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public enum ClientFileType
{
    XML("xml", SaveFlag.XML), //$NON-NLS-1$
    XML_ID("xml", SaveFlag.XML, SaveFlag.ID_REFERENCES), //$NON-NLS-1$
    XML_ZIP("zip", SaveFlag.XML, SaveFlag.COMPRESSED), //$NON-NLS-1$
    XML_AES256("portfolio", SaveFlag.XML, SaveFlag.ENCRYPTED, SaveFlag.AES256), //$NON-NLS-1$
    XML_AES128("portfolio", SaveFlag.XML, SaveFlag.ENCRYPTED, SaveFlag.AES128), //$NON-NLS-1$
    BINARY("portfolio", SaveFlag.BINARY, SaveFlag.COMPRESSED), //$NON-NLS-1$
    BINARY_AES256("portfolio", SaveFlag.BINARY, SaveFlag.ENCRYPTED, SaveFlag.AES256); //$NON-NLS-1$

    private final String extension;
    private final EnumSet<SaveFlag> flags;

    private ClientFileType(String extension, SaveFlag... flags)
    {
        this.extension = extension;
        this.flags = EnumSet.copyOf(Arrays.asList(flags));
    }

    public String getExtension()
    {
        return extension;
    }

    public Set<SaveFlag> getFlags()
    {
        return flags;
    }

}
