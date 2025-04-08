package name.abuchen.portfolio.model;

import java.text.MessageFormat;

public class Exchange
{
    public static final String DISPLAY_NAME_FORMAT_ID_AND_NAME = "{0} ({1})"; //$NON-NLS-1$
    public static final String DISPLAY_NAME_FORMAT_EXCHANGE_NAME_ONLY = "{1}"; //$NON-NLS-1$
    
    private String id;
    private String name;
    private String displayNameFormat;

    public Exchange()
    {
    }

    public Exchange(String id, String name)
    {
        this(id, name, DISPLAY_NAME_FORMAT_ID_AND_NAME);
    }

    public Exchange(String id, String name, String displayNameFormat)
    {
        this.id = id;
        this.name = name;
        this.displayNameFormat = displayNameFormat;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
    
    public String getDisplayName()
    {
        return MessageFormat.format(displayNameFormat, id, name);
    }
}
