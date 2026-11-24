package name.abuchen.portfolio.ui.views;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;

public final class MovingAveragePeriod
{
    public static final List<Integer> DEFAULT_PERIODS = List.of(5, 20, 30, 38, 50, 90, 100, 200);

    private MovingAveragePeriod()
    {
    }

    public static String format(int days)
    {
        return MessageFormat.format(Messages.LabelXDays, days);
    }

    public static List<Integer> merge(Collection<Integer> first, Collection<Integer> second)
    {
        TreeSet<Integer> periods = new TreeSet<>();
        periods.addAll(first);
        periods.addAll(second);
        return new ArrayList<>(periods);
    }

    public static List<Integer> parse(String value)
    {
        if (value == null || value.isBlank())
            return new ArrayList<>();

        List<Integer> periods = new ArrayList<>();
        Arrays.stream(value.split(",")) //$NON-NLS-1$
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(s -> {
                            try
                            {
                                int period = Integer.parseInt(s);
                                if (period > 0)
                                    periods.add(period);
                            }
                            catch (NumberFormatException ignore)
                            {
                                // ignore invalid values in old or hand-edited files
                            }
                        });
        return periods.stream().distinct().sorted().collect(Collectors.toCollection(ArrayList::new));
    }

    public static String serialize(Collection<Integer> periods)
    {
        return periods.stream().distinct().sorted().map(String::valueOf).collect(Collectors.joining(",")); //$NON-NLS-1$
    }

    public static Integer createNew(Shell shell, Collection<Integer> existingPeriods)
    {
        InputDialog dialog = new InputDialog(shell, Messages.LabelChartDetailMovingAverage,
                        Messages.LabelReportingDialogDays, "", value -> { //$NON-NLS-1$
                            if (value == null || value.isBlank())
                                return MessageFormat.format(Messages.MsgDialogInputRequired,
                                                Messages.LabelReportingDialogDays);

                            try
                            {
                                int period = Integer.parseInt(value.trim());
                                if (period <= 0)
                                    return MessageFormat.format(Messages.MsgDialogInputRequired,
                                                    Messages.LabelReportingDialogDays);
                                if (existingPeriods.contains(period))
                                    return MessageFormat.format(Messages.EditWizardMasterDataMsgDuplicateClassification,
                                                    MovingAveragePeriod.format(period));
                            }
                            catch (NumberFormatException e)
                            {
                                return MessageFormat.format(Messages.MsgDialogInputRequired,
                                                Messages.LabelReportingDialogDays);
                            }

                            return null;
                        });

        if (dialog.open() != Window.OK)
            return null;

        return Integer.parseInt(dialog.getValue().trim());
    }
}
