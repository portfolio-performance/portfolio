package name.abuchen.portfolio.ui.preferences;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Collectors;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.junit.Test;

import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class FormattingPreferenceStoreTest
{

    @Test
    public void testAsString()
    {
        var store = new ClientPropertiesPreferenceStore(new Client());

        assertThat(store.asString(null), is(""));
        assertThat(store.asString("  "), is("  "));
        assertThat(store.asString(" abc "), is(" abc "));
    }

    @Test
    public void testAsDouble()
    {
        var store = new ClientPropertiesPreferenceStore(new Client());

        assertThat(store.asDouble(null), is(0d));
        assertThat(store.asDouble("  "), is(0d));
        assertThat(store.asDouble(" abc "), is(0d));
        assertThat(store.asDouble(" 123.45 "), is(123.45d));
        assertThat(store.asDouble(" -123.45 "), is(-123.45d));
    }

    @Test
    public void testAsFloat()
    {
        var store = new ClientPropertiesPreferenceStore(new Client());

        assertThat(store.asFloat(null), is(0f));
        assertThat(store.asFloat("  "), is(0f));
        assertThat(store.asFloat(" abc "), is(0f));
        assertThat(store.asFloat(" 123.45 "), is(123.45f));
        assertThat(store.asFloat(" -123.45 "), is(-123.45f));
    }

    @Test
    public void testAsInt()
    {
        var store = new ClientPropertiesPreferenceStore(new Client());

        assertThat(store.asInt(null), is(0));
        assertThat(store.asInt("  "), is(0));
        assertThat(store.asInt(" abc "), is(0));
        assertThat(store.asInt(" 123.45 "), is(0));
        assertThat(store.asInt(" -123.45 "), is(0));
        assertThat(store.asInt(" 123 "), is(123));
        assertThat(store.asInt(" -123 "), is(-123));
    }

    @Test
    public void testAsLong()
    {
        var store = new ClientPropertiesPreferenceStore(new Client());

        assertThat(store.asLong(null), is(0L));
        assertThat(store.asLong("  "), is(0L));
        assertThat(store.asLong(" abc "), is(0L));
        assertThat(store.asLong(" 123.45 "), is(0L));
        assertThat(store.asLong(" -123.45 "), is(0L));
        assertThat(store.asLong(" 123 "), is(123L));
        assertThat(store.asLong(" -123 "), is(-123L));
    }

    @Test
    public void testAsBoolean()
    {
        var store = new ClientPropertiesPreferenceStore(new Client());

        assertThat(store.asBoolean(null), is(false));
        assertThat(store.asBoolean("  "), is(false));
        assertThat(store.asBoolean(" abc "), is(false));
        assertThat(store.asBoolean(" true "), is(true));
        assertThat(store.asBoolean(" false "), is(false));
        assertThat(store.asBoolean(" 1 "), is(false));
        assertThat(store.asBoolean(" -1 "), is(false));
    }

    @Test
    public void testSetGetValues()
    {
        var messages = new LinkedList<String>();
        var store = new ClientPropertiesPreferenceStore(new Client());
        IPropertyChangeListener listener = e -> {
            messages.add("event triggered: " + e.getProperty() + " " + e.getOldValue() + " -> " + e.getNewValue());
        };
        store.addPropertyChangeListener(listener);

        checkInitialValues(store);

        checkAndResetNeedSaving(store, false);

        store.setDefault("dfltbool", true);
        store.setDefault("dfltint", 1);
        store.setDefault("dfltlong", 2L);
        store.setDefault("dfltfloat", 3.4);
        store.setDefault("dfltdouble", 5.6);
        store.setDefault("dfltstring", " some string ");

        checkAndResetNeedSaving(store, false);

        setNonDefaultValues(store, true);
        var msgs = messages.stream().collect(Collectors.joining("\n"));
        messages.clear();
        assertThat(msgs, is("""
                        event triggered: bool false -> true
                        event triggered: int 0 -> 7
                        event triggered: long 0 -> 8
                        event triggered: float 0.0 -> 9.1
                        event triggered: double 0.0 -> 10.2
                        event triggered: string  ->  some non-default string \
                        """));

        checkChangedValues(store);

        setNonDefaultValues(store, false);
        assertThat(messages.stream().collect(Collectors.joining("\n")), is(""));
        checkChangedValues(store);

        store.putValue("somekey", "some value");
        assertThat(messages.stream().collect(Collectors.joining("\n")), is(""));
        assertThat(store.getString("somekey"), is("some value"));

        store.removePropertyChangeListener(listener);
        store.setValue("somekey", "some new value");
        assertThat(messages.stream().collect(Collectors.joining("\n")), is(""));
        assertThat(store.getString("somekey"), is("some new value"));
    }

    private void checkChangedValues(ClientPropertiesPreferenceStore store)
    {
        assertThat(store.getDefaultBoolean("dfltbool"), is(true));
        assertThat(store.getDefaultInt("dfltint"), is(1));
        assertThat(store.getDefaultLong("dfltlong"), is(2L));
        assertThat(store.getDefaultFloat("dfltfloat"), is(3.4f));
        assertThat(store.getDefaultDouble("dfltdouble"), is(5.6d));
        assertThat(store.getDefaultString("dfltstring"), is(" some string "));

        assertThat(store.getBoolean("bool"), is(true));
        assertThat(store.getInt("int"), is(7));
        assertThat(store.getLong("long"), is(8L));
        assertThat(store.getFloat("float"), is(9.1f));
        assertThat(store.getDouble("double"), is(10.2d));
        assertThat(store.getString("string"), is(" some non-default string "));
    }

    private void checkInitialValues(ClientPropertiesPreferenceStore store)
    {
        assertThat(store.getDefaultBoolean("dfltbool"), is(false));
        assertThat(store.getDefaultInt("dfltint"), is(0));
        assertThat(store.getDefaultLong("dfltlong"), is(0L));
        assertThat(store.getDefaultFloat("dfltfloat"), is(0f));
        assertThat(store.getDefaultDouble("dfltdouble"), is(0d));
        assertThat(store.getDefaultString("dfltstring"), is(""));

        assertThat(store.getBoolean("bool"), is(false));
        assertThat(store.getInt("int"), is(0));
        assertThat(store.getLong("long"), is(0L));
        assertThat(store.getFloat("float"), is(0f));
        assertThat(store.getDouble("double"), is(0d));
        assertThat(store.getString("string"), is(""));
    }

    private void setNonDefaultValues(ClientPropertiesPreferenceStore store, boolean expDirty)
    {
        store.setValue("bool", true);
        checkAndResetNeedSaving(store, expDirty);
        store.setValue("int", 7);
        checkAndResetNeedSaving(store, expDirty);
        store.setValue("long", 8L);
        checkAndResetNeedSaving(store, expDirty);
        store.setValue("float", 9.1f);
        checkAndResetNeedSaving(store, expDirty);
        store.setValue("double", 10.2d);
        checkAndResetNeedSaving(store, expDirty);
        store.setValue("string", " some non-default string ");
        checkAndResetNeedSaving(store, expDirty);
    }

    private void checkAndResetNeedSaving(ClientPropertiesPreferenceStore store, boolean expValue)
    {
        assertThat(store.needsSaving(), is(expValue));
        store.resetDirty();
    }

    @Test
    public void testIsDefault()
    {
        var store = new ClientPropertiesPreferenceStore(new Client());
        store.setDefault("dfltbool", true);
        store.setDefault("dfltint", 1);
        store.setDefault("dfltlong", 2L);
        store.setDefault("dfltfloat", 3.4f);
        store.setDefault("dfltdouble", 5.6d);
        store.setDefault("dfltstring", " some string ");

        store.setValue("bool", true);
        store.setValue("int", 7);
        store.setValue("long", 8L);
        store.setValue("float", 9.1f);
        store.setValue("double", 10.2d);
        store.setValue("string", " some non-default string ");

        String[] defaultIdents = { "dfltbool", "dfltint", "dfltlong", "dfltfloat", "dfltdouble", "dfltstring" };
        String[] nonDefaultIdents = { "bool", "int", "long", "float", "double", "string" };

        Arrays.stream(defaultIdents) //
                        .forEach(ident -> assertThat(ident, store.isDefault(ident), is(true)));
        Arrays.stream(nonDefaultIdents) //
                        .forEach(ident -> assertThat(ident, store.isDefault(ident), is(false)));
        assertThat(store.isDefault("dummy"), is(false));

        store.setValue("dfltbool", true);
        store.setValue("dfltint", 2);
        store.setValue("dfltlong", 3);
        store.setValue("dfltfloat", 5.1);
        store.setValue("dfltdouble", 6.2);
        store.setValue("dfltstring", " some other non-default string ");

        Arrays.stream(defaultIdents) //
                        .forEach(ident -> {
                            assertThat(ident, store.isDefault(ident), is(false));
                            store.setToDefault(ident);
                            assertThat(ident, store.isDefault(ident), is(true));
                        });

        store.setToDefault("dummy");
        assertThat(store.isDefault("dummy"), is(false));
    }
}
