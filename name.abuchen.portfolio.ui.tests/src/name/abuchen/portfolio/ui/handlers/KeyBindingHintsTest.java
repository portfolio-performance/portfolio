package name.abuchen.portfolio.ui.handlers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

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
        filterParameter.setElementId("param.id.filter");
        filterParameter.setName("name.abuchen.portfolio.ui.param.filter");
        command.getParameters().add(filterParameter);

        bindingTable = MCommandsFactory.INSTANCE.createBindingTable();
        application.getBindingTables().add(bindingTable);

        addBinding("M1+U K", null, null);
        addBinding("M1+U A", filterParameter, "ACTIVE");
        addBinding("M1+U H", filterParameter, "HOLDINGS");
    }

    private MKeyBinding addBinding(String keySequence, MCommandParameter parameter, String value)
    {
        MKeyBinding binding = MCommandsFactory.INSTANCE.createKeyBinding();
        binding.setKeySequence(keySequence);
        binding.setCommand(command);

        if (parameter != null)
        {
            MParameter bindingParameter = MCommandsFactory.INSTANCE.createParameter();
            bindingParameter.setElementId(parameter.getElementId());
            bindingParameter.setName(parameter.getName());
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
                        createMenuItem(filterParameter.getElementId(), "ACTIVE")), is("M1+U A"));
        assertThat(KeyBindingHints.getKeySequence(application,
                        createMenuItem(filterParameter.getElementId(), "HOLDINGS")), is("M1+U H"));
    }

    @Test
    public void testBindingWithOtherParametersIsNotUsed()
    {
        MHandledMenuItem item = createMenuItem(filterParameter.getElementId(), "WATCHLIST");

        assertThat(KeyBindingHints.getKeySequence(application, item), is(nullValue()));
    }

    @Test
    public void testAmbiguousBindingsAreIgnored()
    {
        // two bindings for the same command and the same parameter value: the
        // shortcut cannot be determined, so nothing is shown
        addBinding("M1+U X", filterParameter, "ACTIVE");

        assertThat(KeyBindingHints.getKeySequence(application,
                        createMenuItem(filterParameter.getElementId(), "ACTIVE")), is(nullValue()));
    }

    @Test
    public void testModifierNamesAreTranslated()
    {
        // if a message is missing, NLS hands out the key itself
        assertThat(Messages.KeyModifierControl, not(is("KeyModifierControl")));
        assertThat(Messages.KeyBindingHint, not(is("KeyBindingHint")));

        String formatted = KeyBindingHints.formatKeySequence("M1+U A");

        assertThat(formatted, containsString(Messages.KeyModifierControl));
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

        assertThat(formatted, containsString(Messages.KeyModifierControl));
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
        MHandledMenuItem item = createMenuItem(filterParameter.getElementId(), "ACTIVE");
        item.setTooltip("only active securities");

        KeyBindingHints.appendToTooltip(application, item);

        String tooltip = item.getTooltip();
        assertThat(tooltip, containsString("only active securities"));
        assertThat(tooltip, containsString(KeyBindingHints.formatKeySequence("M1+U A")));
    }

    @Test
    public void testTooltipIsNotAppendedTwice()
    {
        MHandledMenuItem item = createMenuItem(filterParameter.getElementId(), "ACTIVE");
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
    public void testItemWithoutCommandLeavesTooltipAlone()
    {
        MHandledMenuItem item = MMenuFactory.INSTANCE.createHandledMenuItem();
        item.setTooltip("no command");

        KeyBindingHints.appendToTooltip(application, item);

        assertThat(item.getTooltip(), is("no command"));
    }
}
