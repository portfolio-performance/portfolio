package name.abuchen.portfolio.ui.wizards.events;

import java.time.LocalDate;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.ui.util.BindingHelper;

public class CustomEventModel extends BindingHelper.Model
{
    private Security security;
    private LocalDate exDate = LocalDate.now();
    private SecurityEvent.Type type = SecurityEvent.Type.CUSTOM;
    private String message = ""; //$NON-NLS-1$

    public CustomEventModel(Client client, Security security)
    {
        super(client);

        this.security = security;
    }

    public Security getSecurity()
    {
        return security;
    }

    public void setSecurity(Security security)
    {
        firePropertyChange("security", this.security, this.security = security); //$NON-NLS-1$
    }

    public LocalDate getExDate()
    {
        return exDate;
    }

    public void setExDate(LocalDate exDate)
    {
        firePropertyChange("exDate", this.exDate, this.exDate = exDate); //$NON-NLS-1$
    }

    public SecurityEvent.Type getType()
    {
        return type;
    }

    public void setType(SecurityEvent.Type type)
    {
        firePropertyChange("type", this.type, this.type = type); //$NON-NLS-1$
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        firePropertyChange("message", this.message, this.message = message); //$NON-NLS-1$
    }

    @Override
    public void applyChanges()
    {
        SecurityEvent event = new SecurityEvent(exDate, type, message);
        security.addEvent(event);
    }
}
