package name.abuchen.portfolio.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import com.google.common.base.Objects;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientProperties;
import name.abuchen.portfolio.model.SecurityNameConfig;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants.Preferences;

public class FormattingPreferencePage extends FieldEditorPreferencePage
{
    private Optional<Client> client;

    public FormattingPreferencePage(Optional<Client> client)
    {
        super(GRID);
        this.client = client;
        setTitle(Messages.PrefTitleFormatting);
    }

    @Override
    protected void createFieldEditors()
    {
        IntegerFieldEditor sharesPrecisionEditor = new IntegerFieldEditor(Preferences.FORMAT_SHARES_DIGITS,
                        Messages.PrefLabelSharesDigits, getFieldEditorParent(), 1);
        sharesPrecisionEditor.setValidRange(0, Values.Share.precision());
        addField(sharesPrecisionEditor);
        IntegerFieldEditor quotePrecisionEditor = new IntegerFieldEditor(Preferences.FORMAT_CALCULATED_QUOTE_DIGITS,
                        Messages.PrefLabelQuoteDigits, getFieldEditorParent(), 1);
        quotePrecisionEditor.setValidRange(0, Values.Quote.precision());
        addField(quotePrecisionEditor);

        if (client.isPresent())
        {
            var elements = Arrays.stream(SecurityNameConfig.values())
                            .map(entry -> new String[] { entry.getLabel(), entry.name() }).toArray(String[][]::new);

            var fieldEditor = new ComboFieldEditor(ClientProperties.Keys.SECURITY_NAME_CONFIG,
                            Messages.PrefPrefixSecurityName, elements, getFieldEditorParent());
            fieldEditor.setPreferenceStore(new ClientPropertiesPreferenceStore(client.get()));

            addField(fieldEditor);
        }
    }
}

class ClientPropertiesPreferenceStore implements IPreferenceStore
{
    private final Client client;
    private final Map<String, String> defaultProperties;
    private final List<IPropertyChangeListener> listeners = new ArrayList<>();

    private boolean isDirty = false;

    public ClientPropertiesPreferenceStore(Client client)
    {
        this.client = client;
        this.defaultProperties = new HashMap<>();
    }

    @Override
    public void firePropertyChangeEvent(String name, Object oldValue, Object newValue)
    {
        var event = new PropertyChangeEvent(this, name, oldValue, newValue);
        listeners.forEach(listener -> listener.propertyChange(event));
    }

    @Override
    public void addPropertyChangeListener(IPropertyChangeListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removePropertyChangeListener(IPropertyChangeListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public void putValue(String name, String value)
    {
        // does not fire property change
        client.setProperty(name, value);
    }

    @Override
    public void setValue(String name, String value)
    {
        var oldValue = getString(name);
        if (!Objects.equal(oldValue, value))
        {
            client.setProperty(name, value);
            isDirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, long value)
    {
        var oldValue = getString(name);
        if (!Objects.equal(oldValue, value))
        {
            client.setProperty(name, Long.toString(value));
            isDirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }

    }

    @Override
    public void setValue(String name, int value)
    {
        var oldValue = getString(name);
        if (!Objects.equal(oldValue, value))
        {
            client.setProperty(name, Integer.toString(value));
            isDirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, float value)
    {
        var oldValue = getString(name);
        if (!Objects.equal(oldValue, value))
        {
            client.setProperty(name, Float.toString(value));
            isDirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, double value)
    {
        var oldValue = getString(name);
        if (!Objects.equal(oldValue, value))
        {
            client.setProperty(name, Double.toString(value));
            isDirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, boolean value)
    {
        var oldValue = getString(name);
        if (!Objects.equal(oldValue, value))
        {
            client.setProperty(name, Boolean.toString(value));
            isDirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setToDefault(String name)
    {
        if (!client.hasProperty(name))
            return;
        var oldValue = client.removeProperty(name);
        isDirty = true;
        var newValue = defaultProperties.get(name);
        firePropertyChangeEvent(name, oldValue, newValue);
    }

    @Override
    public void setDefault(String name, boolean value)
    {
        defaultProperties.put(name, Boolean.toString(value));
    }

    @Override
    public void setDefault(String name, String defaultObject)
    {
        defaultProperties.put(name, defaultObject);
    }

    @Override
    public void setDefault(String name, long value)
    {
        defaultProperties.put(name, Long.toString(value));
    }

    @Override
    public void setDefault(String name, int value)
    {
        defaultProperties.put(name, Integer.toString(value));
    }

    @Override
    public void setDefault(String name, float value)
    {
        defaultProperties.put(name, Float.toString(value));
    }

    @Override
    public void setDefault(String name, double value)
    {
        defaultProperties.put(name, Double.toString(value));
    }

    @Override
    public boolean needsSaving()
    {
        return isDirty;
    }

    @Override
    public boolean isDefault(String name)
    {
        return !client.hasProperty(name) && defaultProperties.containsKey(name);
    }

    @Override
    public String getString(String name)
    {
        return asString(client.getProperty(name));
    }

    @Override
    public long getLong(String name)
    {
        return asLong(client.getProperty(name));
    }

    @Override
    public int getInt(String name)
    {
        return getInt(client.getProperty(name));
    }

    @Override
    public float getFloat(String name)
    {
        return getFloat(client.getProperty(name));
    }

    @Override
    public double getDouble(String name)
    {
        return getDouble(client.getProperty(name));
    }

    @Override
    public boolean getBoolean(String name)
    {
        return getBoolean(client.getProperty(name));
    }

    @Override
    public String getDefaultString(String name)
    {
        return asString(defaultProperties.get(name));
    }

    @Override
    public long getDefaultLong(String name)
    {
        return asLong(defaultProperties.get(name));
    }

    @Override
    public int getDefaultInt(String name)
    {
        return asInt(defaultProperties.get(name));
    }

    @Override
    public float getDefaultFloat(String name)
    {
        return asFloat(defaultProperties.get(name));
    }

    @Override
    public double getDefaultDouble(String name)
    {
        return asDouble(defaultProperties.get(name));
    }

    @Override
    public boolean getDefaultBoolean(String name)
    {
        return asBoolean(defaultProperties.get(name));
    }

    @Override
    public boolean contains(String name)
    {
        return client.getProperty(name) != null;
    }

    private String asString(String value)
    {
        return value == null ? "" : value; //$NON-NLS-1$
    }

    private double asDouble(String value)
    {
        if (value == null)
            return 0;
        try
        {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    private float asFloat(String value)
    {
        if (value == null)
            return 0;
        try
        {
            return Float.parseFloat(value);
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    private int asInt(String value)
    {
        if (value == null)
            return 0;
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    private long asLong(String value)
    {
        if (value == null)
            return 0;
        try
        {
            return Long.parseLong(value);
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    private boolean asBoolean(String value)
    {
        if (value == null)
            return false;
        return Boolean.parseBoolean(value);
    }
}
