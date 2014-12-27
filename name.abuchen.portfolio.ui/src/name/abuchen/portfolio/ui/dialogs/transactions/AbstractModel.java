package name.abuchen.portfolio.ui.dialogs.transactions;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public abstract class AbstractModel
{
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public AbstractModel()
    {}

    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    protected void firePropertyChange(String attribute, Object oldValue, Object newValue)
    {
        propertyChangeSupport.firePropertyChange(attribute, oldValue, newValue);
    }

    protected void firePropertyChange(String attribute, long oldValue, long newValue)
    {
        propertyChangeSupport.firePropertyChange(attribute, oldValue, newValue);
    }
}