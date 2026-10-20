package name.abuchen.portfolio.ui.preferences;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Optional;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.jobs.priceupdate.PriceUpdateConfig;
import name.abuchen.portfolio.ui.jobs.priceupdate.PriceUpdateStrategy;

public class HistoricalPricesPreferencePage extends FieldEditorPreferencePage
{
    private Optional<ClientInput> clientInput;

    private BooleanFieldEditor updateQuotesAfterFileOpen;
    private BooleanFieldEditor updateQuotesPeriodically;

    public HistoricalPricesPreferencePage(Optional<ClientInput> clientInput)
    {
        super(GRID);
        this.clientInput = clientInput;
        setTitle(Messages.JobLabelUpdateQuotes);
    }

    @Override
    protected void createFieldEditors()
    {
        this.updateQuotesAfterFileOpen = new BooleanFieldEditor(UIConstants.Preferences.UPDATE_QUOTES_AFTER_FILE_OPEN, //
                        Messages.PrefUpdateQuotesAfterFileOpen, getFieldEditorParent());
        addField(updateQuotesAfterFileOpen);

        this.updateQuotesPeriodically = new BooleanFieldEditor(UIConstants.Preferences.UPDATE_QUOTES_PERIODICALLY, //
                        Messages.PrefUpdateQuotesPeriodically, getFieldEditorParent());
        addField(updateQuotesPeriodically);

        Label separator = new Label(getFieldEditorParent(), SWT.SEPARATOR | SWT.HORIZONTAL);
        GridDataFactory.fillDefaults().span(2, 1).indent(0, 20).applyTo(separator);

        new DescriptionFieldEditor(Messages.PrefUpdateQuotesRecalculationIntervalInfo, getFieldEditorParent());

        addField(new ComboFieldEditor(UIConstants.Preferences.UPDATE_QUOTES_RECALCULATION_INTERVAL,
                        Messages.PrefUpdateQuotesRecalculationInterval, createRecalculationIntervalOptions(),
                        getFieldEditorParent()));

        if (clientInput.isPresent())
        {
            Label label = new Label(getFieldEditorParent(), SWT.SEPARATOR | SWT.HORIZONTAL);
            GridDataFactory.fillDefaults().span(2, 1).indent(0, 20).applyTo(label);

            new DescriptionFieldEditor(Messages.LabelPriceUpdateStrategyInfo, getFieldEditorParent());

            var elements = new ArrayList<PriceUpdateConfig>();

            elements.add(new PriceUpdateConfig(PriceUpdateStrategy.ACTIVE));
            elements.add(new PriceUpdateConfig(PriceUpdateStrategy.HOLDINGS));

            var client = clientInput.get().getClient();
            if (client != null)
            {
                for (var watchlist : client.getWatchlists())
                {
                    elements.add(new PriceUpdateConfig(PriceUpdateStrategy.HOLDINGS_AND_WATCHLIST,
                                    watchlist.getName()));
                }
            }

            var fieldEditor = new ComboFieldEditor(UIConstants.Preferences.UPDATE_QUOTES_STRATEGY,
                            Messages.LabelPriceUpdateStrategy,
                            elements.stream().map(c -> new String[] { c.getLabel(), c.getCode() })
                                            .toArray(String[][]::new),
                            getFieldEditorParent());
            fieldEditor.setPreferenceStore(clientInput.get().getPreferenceStore());

            addField(fieldEditor);

        }
    }

    private static String[][] createRecalculationIntervalOptions()
    {
        var format = NumberFormat.getNumberInstance();

        return new String[][] { //
                        { seconds(format, 0.3), "300" }, //$NON-NLS-1$
                        { seconds(format, 1), "1000" }, //$NON-NLS-1$
                        { seconds(format, 3), "3000" }, //$NON-NLS-1$
                        { seconds(format, 5), "5000" }, //$NON-NLS-1$
                        { seconds(format, 10), "10000" }, //$NON-NLS-1$
                        { seconds(format, 30), "30000" }, //$NON-NLS-1$
                        { Messages.PrefUpdateQuotesRecalculationIntervalOnlyAfterUpdate, "0" } }; //$NON-NLS-1$
    }

    private static String seconds(NumberFormat format, double seconds)
    {
        return format.format(seconds) + " s"; //$NON-NLS-1$
    }

    @Override
    protected void initialize()
    {
        super.initialize();

        updateQuotesPeriodically.setEnabled(updateQuotesAfterFileOpen.getBooleanValue(), getFieldEditorParent());
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        super.propertyChange(event);

        if (event.getSource() == updateQuotesAfterFileOpen && event.getProperty().equals(FieldEditor.VALUE))
        {
            updateQuotesPeriodically.setEnabled(updateQuotesAfterFileOpen.getBooleanValue(), getFieldEditorParent());
        }

    }
}
