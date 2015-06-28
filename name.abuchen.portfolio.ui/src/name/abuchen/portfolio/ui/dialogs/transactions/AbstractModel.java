package name.abuchen.portfolio.ui.dialogs.transactions;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import name.abuchen.portfolio.money.ExchangeRateProviderFactory;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

public abstract class AbstractModel
{
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private ExchangeRateProviderFactory factory;

    public abstract String getHeading();

    public abstract void applyChanges();

    public IStatus getCalculationStatus()
    {
        return ValidationStatus.ok();
    }

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

    public void setExchangeRateProviderFactory(ExchangeRateProviderFactory factory)
    {
        this.factory = factory;
    }

    public ExchangeRateProviderFactory getExchangeRateProviderFactory()
    {
        return factory;
    }
}