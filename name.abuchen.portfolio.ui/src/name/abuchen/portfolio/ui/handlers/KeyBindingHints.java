package name.abuchen.portfolio.ui.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MBindingTable;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.commands.MKeyBinding;
import org.eclipse.e4.ui.model.application.commands.MParameter;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuItem;

import name.abuchen.portfolio.ui.Messages;

/**
 * Resolves the keyboard shortcut that the application model defines for a menu
 * item and adds it to the tooltip of that item.
 *
 * Issue #3308: on Linux (GTK) the menus do not show the accelerator text that
 * e4 appends after a tab character - and multi stroke sequences such as
 * "Ctrl+U A" could not be displayed by GTK anyway because a GTK menu item only
 * supports a single key. Therefore the shortcut is written into the tooltip,
 * which is displayed on every platform (except GTK4, where SWT cannot show
 * tooltips inside menus at all).
 *
 * The shortcut is looked up in the application model instead of using
 * {@code EBindingService.getBestSequenceFor(ParameterizedCommand)}: that helper
 * ignores parameters and would report the very same sequence for every variant
 * of the "Update quotes" command.
 */
public class KeyBindingHints
{
    private KeyBindingHints()
    {
    }

    /**
     * Adds the keyboard shortcut of the menu item to its tooltip, if a binding
     * is defined for it and if the tooltip does not already mention it.
     */
    public static void appendToTooltip(MApplication application, MMenuItem item)
    {
        setTooltipWithKeySequence(item, getKeySequence(application, item));
    }

    /**
     * @return the {@code keySequence} of the binding that matches the command
     *         and the parameters of the item, or <code>null</code> if the item
     *         has no command or if no unambiguous binding exists
     */
    public static String getKeySequence(MApplication application, MMenuItem item)
    {
        if (application == null || !(item instanceof MHandledItem handledItem))
            return null;

        MCommand command = handledItem.getCommand();
        if (command == null || command.getElementId() == null)
            return null;

        return findKeySequence(application.getBindingTables(), command.getElementId(), handledItem.getParameters());
    }

    /**
     * Turns a key sequence of the application model such as
     * {@code "M1+U M2+S"} into a human readable text such as
     * {@code "Ctrl+U Shift+S"}, using the modifier names of the current
     * language and the keyboard symbols of the current platform.
     */
    public static String formatKeySequence(String keySequence)
    {
        if (keySequence == null || keySequence.isBlank())
            return ""; //$NON-NLS-1$

        List<String> formatted = new ArrayList<>();

        for (String chord : keySequence.trim().split("\\s+")) //$NON-NLS-1$
        {
            // keep a trailing '+' (a modifier without a key) so that it can be
            // recognized as an incomplete sequence below
            List<String> pieces = new ArrayList<>(Arrays.asList(chord.split("\\+", -1))); //$NON-NLS-1$
            String key = pieces.remove(pieces.size() - 1);

            if (key.isEmpty())
            {
                // a modifier without a key ("M1+") cannot be shown
                continue;
            }

            StringBuilder sb = new StringBuilder();
            for (String piece : pieces)
                appendModifier(sb, piece);

            if (sb.length() > 0)
                sb.append('+');
            sb.append(key);

            formatted.add(sb.toString());
        }

        return String.join(" ", formatted); //$NON-NLS-1$
    }

    private static void setTooltipWithKeySequence(MMenuItem item, String keySequence)
    {
        if (item == null || keySequence == null)
            return;

        String shortcut = formatKeySequence(keySequence);
        if (shortcut.isEmpty())
            return;

        String current = item.getTooltip();
        if (current != null && current.contains(shortcut))
            return;

        String base = current == null ? "" : current; //$NON-NLS-1$
        // not MessageFormat: the existing tooltip is free text and may contain
        // apostrophes that MessageFormat would silently swallow
        String tooltip = Messages.KeyBindingHint.replace("{0}", shortcut).replace("{1}", base); //$NON-NLS-1$ //$NON-NLS-2$
        item.setTooltip(tooltip.trim());
    }

    private static String findKeySequence(List<MBindingTable> tables, String commandId, List<MParameter> itemParameters)
    {
        List<MKeyBinding> candidates = tables.stream() //
                        .flatMap(table -> table.getBindings().stream()) //
                        .filter(binding -> isForCommand(binding, commandId)) //
                        .filter(binding -> hasSameParameters(binding.getParameters(), itemParameters)) //
                        .toList();

        if (candidates.size() != 1)
        {
            // without exactly one matching binding the shortcut cannot be
            // determined - better show nothing than a wrong one
            return null;
        }

        return candidates.get(0).getKeySequence();
    }

    private static boolean isForCommand(MKeyBinding binding, String commandId)
    {
        return binding.getCommand() != null && commandId.equals(binding.getCommand().getElementId());
    }

    /**
     * A parameter of a binding and the parameter of a menu item address the
     * same parameter definition of the command through their <em>name</em>,
     * together with the value that is passed to the command. The element id
     * cannot be used: the application model gives every parameter its own
     * instance id, so the ids of a binding and of a menu item differ even when
     * both address the same parameter (see Application.e4xmi, for example the
     * "holdings" menu item and the binding of Ctrl+U H).
     */
    private static boolean hasSameParameters(List<MParameter> bindingParameters, List<MParameter> itemParameters)
    {
        int itemCount = itemParameters == null ? 0 : itemParameters.size();
        if (bindingParameters.size() != itemCount)
            return false;
        if (itemCount == 0)
            return true;

        return bindingParameters.stream().allMatch(bp -> itemParameters.stream() //
                        .anyMatch(ip -> Objects.equals(ip.getName(), bp.getName())
                                        && Objects.equals(ip.getValue(), bp.getValue())));
    }

    /**
     * SWT names the modifiers platform specific: on the Mac M1 is the command
     * key, elsewhere it is the control key. The prefixes are the ones used by
     * the key sequences of the application model.
     */
    private static void appendModifier(StringBuilder sb, String modifier)
    {
        if (modifier.isEmpty())
            return;

        String name = switch (modifier)
        {
            case "M1" -> isMac() ? Messages.KeyModifierCommand : Messages.KeyModifierControl; //$NON-NLS-1$
            case "M2" -> Messages.KeyModifierShift; //$NON-NLS-1$
            case "M3" -> Messages.KeyModifierAlt; //$NON-NLS-1$
            case "M4" -> Messages.KeyModifierCommand; //$NON-NLS-1$
            default -> null;
        };

        if (name == null)
            return;

        if (sb.length() > 0)
            sb.append('+');
        sb.append(name);
    }

    private static boolean isMac()
    {
        // SWT.getPlatform() names the window toolkit ("gtk", "win32", "cocoa")
        // and not the operating system, so ask the runtime for the OS
        return Platform.OS_MACOSX.equals(Platform.getOS());
    }
}
