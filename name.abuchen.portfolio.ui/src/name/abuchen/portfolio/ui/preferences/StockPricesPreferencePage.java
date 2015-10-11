package name.abuchen.portfolio.ui.preferences;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Label;

public class StockPricesPreferencePage extends FieldEditorPreferencePage
{

    public StockPricesPreferencePage()
    {
        super(GRID);
        setTitle(Messages.PrefStockPricesTitle);
        setDescription(Messages.PrefStockPricesHelpText);
    }

    public void createFieldEditors()
    {
        addField(new StringFieldEditor(PortfolioPlugin.Preferences.STOCK_PRICE_DATE, //
                        Messages.PrefStockPricesStartDate, getFieldEditorParent()));
        Label descriptionLabel1a = new Label(getFieldEditorParent(), SWT.WRAP);
        if (descriptionLabel1a != null)
        {
            descriptionLabel1a.setFont(getFieldEditorParent().getFont());
            descriptionLabel1a.setText(Messages.PrefStockPricesStartDateSupported1);
            descriptionLabel1a.setLayoutData(new GridData(GridData.BEGINNING));
        }
        Label descriptionLabel1b = new Label(getFieldEditorParent(), SWT.WRAP);
        if (descriptionLabel1b != null)
        {
            descriptionLabel1b.setFont(getFieldEditorParent().getFont());
            descriptionLabel1b.setText(Messages.PrefStockPricesStartDateSupported2);
            descriptionLabel1b.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }
        Label descriptionLabel2a = new Label(getFieldEditorParent(), SWT.WRAP);
        if (descriptionLabel2a != null)
        {
            descriptionLabel2a.setFont(getFieldEditorParent().getFont());
            descriptionLabel2a.setText("");
            descriptionLabel2a.setLayoutData(new GridData(GridData.BEGINNING));
        }
        Label descriptionLabel2b = new Label(getFieldEditorParent(), SWT.WRAP);
        if (descriptionLabel2b != null)
        {
            descriptionLabel2b.setFont(getFieldEditorParent().getFont());
            descriptionLabel2b.setText(Messages.PrefStockPricesStartDateSupported3);
            descriptionLabel2b.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }
    }
}
