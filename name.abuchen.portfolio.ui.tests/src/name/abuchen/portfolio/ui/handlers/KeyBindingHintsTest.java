package name.abuchen.portfolio.ui.handlers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationFactory;
import org.eclipse.e4.ui.model.application.commands.MBindingTable;
import org.eclipse.e4.ui.model.application.commands.MCommandsFactory;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.commands.MCommandParameter;
import org.eclipse.e4.ui.model.application.commands.MKeyBinding;
import org.eclipse.e4.ui.model.application.commands.MParameter;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.ui.Messages;

/**
 * The model of the "Update quotes" menu items is rebuilt by hand as it is
 * defined in the application model (see Application.e4xmi of the bootstrap
 * bundle): one command, but several menu items that differ by their
 * parameters, and one binding per menu item.
 */
@SuppressWarnings("nls")
public class KeyBindingHintsTest
{
    private static final String COMMAND_ID = "name.abuchen.portfolio.ui.command.updatequotes";

    /** id of the parameter definition of the command (see Application.e4xmi) */
    private static final String PARAMETER_ID = "name.abuchen.portfolio.ui.param.filter";
    /**
     * the bindings and the menu items each carry their own parameter ids: the
     * application model numbers the parameter instances instead of repeating
     * the id of the parameter definition (the "1" of the menu item and the "1"
     * of the binding happen to be the same, "2" and "0" are not)
     */
    private static final String BINDING_PARAMETER_ID_ACTIVE = "name.abuchen.portfolio.ui.parameter.1";
    private static final String BINDING_PARAMETER_ID_HOLDINGS = "name.abuchen.portfolio.ui.parameter.2";
    private static final String ITEM_PARAMETER_ID_ACTIVE = "name.abuchen.portfolio.ui.parameter.1";
    private static final String ITEM_PARAMETER_ID_HOLDINGS = "name.abuchen.portfolio.ui.parameter.holdings";
    private static final String ITEM_PARAMETER_ID_SELECTED = "name.abuchen.portfolio.ui.parameter.2";

    private MApplication application;
    private MCommand command;
    private MCommandParameter filterParameter;
    private MBindingTable bindingTable;

    @Before
    public void setUpModel()
    {
        application = MApplicationFactory.INSTANCE.createApplication();

        command = MCommandsFactory.INSTANCE.createCommand();
        command.setElementId(COMMAND_ID);

        filterParameter = MCommandsFactory.INSTANCE.createCommandParameter();
        filterParameter.setElementId(PARAMETER_ID);
        filterParameter.setName(PARAMETER_ID);
        command.getParameters().add(filterParameter);

        bindingTable = MCommandsFactory.INSTANCE.createBindingTable();
        application.getBindingTables().add(bindingTable);

        addBinding("M1+U K", null, null);
        addBinding("M1+U A", BINDING_PARAMETER_ID_ACTIVE, "ACTIVE");
        addBinding("M1+U H", BINDING_PARAMETER_ID_HOLDINGS, "HOLDINGS");
    }

    private MKeyBinding addBinding(String keySequence, String bindingParameterId, String value)
    {
        MKeyBinding binding = MCommandsFactory.INSTANCE.createKeyBinding();
        binding.setKeySequence(keySequence);
        binding.setCommand(command);

        if (bindingParameterId != null)
        {
            MParameter bindingParameter = MCommandsFactory.INSTANCE.createParameter();
            bindingParameter.setElementId(bindingParameterId);
            bindingParameter.setName(filterParameter.getName());
            bindingParameter.setValue(value);
            binding.getParameters().add(bindingParameter);
        }

        bindingTable.getBindings().add(binding);
        return binding;
    }

    private MHandledMenuItem createMenuItem(String... parameterValues)
    {
        MHandledMenuItem item = MMenuFactory.INSTANCE.createHandledMenuItem();
        item.setCommand(command);

        for (int ii = 0; ii < parameterValues.length; ii = ii + 2)
        {
            MParameter itemParameter = MCommandsFactory.INSTANCE.createParameter();
            itemParameter.setElementId(parameterValues[ii]);
            itemParameter.setName(filterParameter.getName());
            itemParameter.setValue(parameterValues[ii + 1]);
            item.getParameters().add(itemParameter);
        }

        return item;
    }

    @Test
    public void testUnparameterizedBindingIsFound()
    {
        assertThat(KeyBindingHints.getKeySequence(application, createMenuItem()), is("M1+U K"));
    }

    @Test
    public void testParameterizedBindingIsFound()
    {
        assertThat(KeyBindingHints.getKeySequence(application,
                        createMenuItem(ITEM_PARAMETER_ID_ACTIVE, "ACTIVE")), is("M1+U A"));
        assertThat(KeyBindingHints.getKeySequence(application,
                        createMenuItem(ITEM_PARAMETER_ID_HOLDINGS, "HOLDINGS")), is("M1+U H"));
    }

    @Test
    public void testBindingIsMatchedByParameterNameAndNotById()
    {
        // the model gives the parameter of a binding and the parameter of a
        // menu item different ids, so only name and value may be compared
        assertThat(KeyBindingHints.getKeySequence(application,
                        createMenuItem("any.other.id", "ACTIVE")), is("M1+U A"));
    }

    @Test
    public void testBindingWithOtherParameterValueIsNotUsed()
    {
        assertThat(KeyBindingHints.getKeySequence(application,
                        createMenuItem(ITEM_PARAMETER_ID_SELECTED, "SECURITY")), is(nullValue()));
    }

    @Test
    public void testBindingWithOtherParametersIsNotUsed()
    {
        MHandledMenuItem item = createMenuItem(ITEM_PARAMETER_ID_ACTIVE, "WATCHLIST");

        assertThat(KeyBindingHints.getKeySequence(application, item), is(nullValue()));
    }

    @Test
    public void testAmbiguousBindingsAreIgnored()
    {
        // two bindings for the same command and the same parameter value: the
        // shortcut cannot be determined, so nothing is shown
        addBinding("M1+U X", BINDING_PARAMETER_ID_HOLDINGS, "ACTIVE");

        assertThat(KeyBindingHints.getKeySequence(application,
                        createMenuItem(ITEM_PARAMETER_ID_ACTIVE, "ACTIVE")), is(nullValue()));
    }

    /**
     * The primary modifier of a key sequence (the "M1" of "M1+U") is the
     * command key on the Mac and the control key everywhere else. The expected
     * name is worked out here and not taken from the production code, so that a
     * wrong platform check in {@code KeyBindingHints} is noticed.
     */
    private static String expectedPrimaryModifier()
    {
        return isMacOnThisPlatform() ? Messages.KeyModifierCommand : Messages.KeyModifierControl;
    }

    private static String unexpectedPrimaryModifier()
    {
        return isMacOnThisPlatform() ? Messages.KeyModifierControl : Messages.KeyModifierCommand;
    }

    private static boolean isMacOnThisPlatform()
    {
        return Platform.OS_MACOSX.equals(Platform.getOS());
    }

    /**
     * A missing message is not handed out as the key: NLS fills the field with
     * "NLS missing message:&lt;key&gt; in:&lt;bundle&gt;" (see the method
     * computeMissingMessages of org.eclipse.osgi.util.NLS), so a test has to
     * reject that marker instead of the name of the key.
     */
    private static void assertTranslated(String key, String value)
    {
        assertThat("no value for " + key, value, not(nullValue()));
        assertThat("empty value for " + key, value, not(is("")));
        assertThat("missing message for " + key, value, not(containsString("NLS missing message")));
    }

    @Test
    public void testModifierNamesAreTranslated()
    {
        assertTranslated("KeyModifierControl", Messages.KeyModifierControl);
        assertTranslated("KeyModifierCommand", Messages.KeyModifierCommand);
        assertTranslated("KeyBindingHint", Messages.KeyBindingHint);

        String formatted = KeyBindingHints.formatKeySequence("M1+U A");

        assertThat(formatted, containsString(expectedPrimaryModifier()));
        assertThat(formatted, not(containsString(unexpectedPrimaryModifier())));
        assertThat(formatted, containsString("U"));
        assertThat(formatted, containsString("A"));
        assertThat(formatted, not(containsString("M1")));
    }

    @Test
    public void testFormatSequenceWithTwoModifierCombinations()
    {
        String formatted = KeyBindingHints.formatKeySequence("M1+O M2+S");

        assertThat(formatted.split(" ").length, is(2));
        assertThat(formatted, containsString(Messages.KeyModifierShift));
        assertThat(formatted, not(containsString("M1")));
        assertThat(formatted, not(containsString("M2")));
    }

    @Test
    public void testFormatKeepsAllModifiersOfOneChord()
    {
        String formatted = KeyBindingHints.formatKeySequence("M1+M2+S");

        assertThat(formatted, containsString(expectedPrimaryModifier()));
        assertThat(formatted, not(containsString(unexpectedPrimaryModifier())));
        assertThat(formatted, containsString(Messages.KeyModifierShift));
        assertThat(formatted, containsString("S"));
    }

    @Test
    public void testFormatIgnoresIncompleteSequence()
    {
        assertThat(KeyBindingHints.formatKeySequence("M1+"), is(""));
        assertThat(KeyBindingHints.formatKeySequence(null), is(""));
        assertThat(KeyBindingHints.formatKeySequence("  "), is(""));
    }

    @Test
    public void testTooltipGetsTheShortcutAppended()
    {
        MHandledMenuItem item = createMenuItem(ITEM_PARAMETER_ID_ACTIVE, "ACTIVE");
        item.setTooltip("only active securities");

        KeyBindingHints.appendToTooltip(application, item);

        String tooltip = item.getTooltip();
        assertThat(tooltip, containsString("only active securities"));
        assertThat(tooltip, containsString(KeyBindingHints.formatKeySequence("M1+U A")));
    }

    @Test
    public void testTooltipIsNotAppendedTwice()
    {
        MHandledMenuItem item = createMenuItem(ITEM_PARAMETER_ID_ACTIVE, "ACTIVE");
        item.setTooltip("only active securities");

        KeyBindingHints.appendToTooltip(application, item);
        String first = item.getTooltip();

        KeyBindingHints.appendToTooltip(application, item);

        assertThat(item.getTooltip(), is(first));
    }

    @Test
    public void testShortcutIsAlsoShownWithoutATooltip()
    {
        MHandledMenuItem item = createMenuItem();

        KeyBindingHints.appendToTooltip(application, item);

        assertThat(item.getTooltip(), containsString(KeyBindingHints.formatKeySequence("M1+U K")));
    }

    @Test
    public void testApostrophesOfTheExistingTooltipSurvive()
    {
        // the tooltip is free text: a MessageFormat would drop the apostrophes
        MHandledMenuItem item = createMenuItem(ITEM_PARAMETER_ID_ACTIVE, "ACTIVE");
        item.setTooltip("don't update the quotes of inactive securities");

        KeyBindingHints.appendToTooltip(application, item);

        assertThat(item.getTooltip(), containsString("don't"));
    }

    @Test
    public void testItemWithoutCommandLeavesTooltipAlone()
    {
        MHandledMenuItem item = MMenuFactory.INSTANCE.createHandledMenuItem();
        item.setTooltip("no command");

        KeyBindingHints.appendToTooltip(application, item);

        assertThat(item.getTooltip(), is("no command"));
    }
}
