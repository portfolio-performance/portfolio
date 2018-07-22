package name.abuchen.portfolio.ui.wizards.sync;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class InfoSyncPage extends AbstractWizardPage
{
    public InfoSyncPage()
    {
        super("infopage"); //$NON-NLS-1$

        setTitle("Stammdatenabgleich");
        setMessage("Fehlende ISIN, WKN, oder Ticker Symbole erweitern");
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        setControl(container);
        container.setLayout(new FormLayout());

        Label description = new Label(container, SWT.WRAP);
        description.setText("PP kann versuchen fehlende Wertpapierstammdaten abzugeleichen. "
                        + "Dazu wird mit der ISIN, WKN, Ticker und oder Namen auf dem Server nach aktualisierten Werten gesucht.\n\n"
                        + "Wenn kein Datenabgleich stattfinden soll, dann den Dialog jetzt mit 'Abbrechen' schliessen.");

        FormDataFactory.startingWith(description).width(500);

        container.pack();
        setPageComplete(true);
    }

}
