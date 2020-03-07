package name.abuchen.portfolio.ui.wizards.security;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.util.SWTHelper;

public class SecurityMasterDataPage extends AbstractPage
{
    private final EditSecurityModel model;
    private final BindingHelper bindings;

    private Text isin;
    private Text wkn;

    protected SecurityMasterDataPage(EditSecurityModel model, BindingHelper bindings)
    {
        this.model = model;
        this.bindings = bindings;

        setTitle(Messages.EditWizardMasterDataTitle);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(container);

        boolean isExchangeRate = model.getSecurity().isExchangeRate();
        boolean isSyncedOnline = model.getOnlineId() != null;

        if (isSyncedOnline)
        {
            // empty cell
            new Label(container, SWT.NONE).setText(""); //$NON-NLS-1$

            Button unlink = new Button(container, SWT.PUSH);
            unlink.setText(Messages.EditWizardMasterDataUnlink);
            unlink.setToolTipText(Messages.EditWizardMasterDataUnlink_ToolTip);
            unlink.setImage(Images.ONLINE.image());
            unlink.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
                model.setOnlineId(null);
                isin.setEnabled(true);
                wkn.setEnabled(true);
                unlink.setEnabled(false);
            }));
        }

        ComboViewer currencyCode = bindings.bindCurrencyCodeCombo(container, Messages.ColumnCurrency, "currencyCode", //$NON-NLS-1$
                        !isExchangeRate);
        if (model.getSecurity().hasTransactions(model.getClient()))
        {
            currencyCode.getCombo().setEnabled(false);

            // empty cell
            new Label(container, SWT.NONE).setText(""); //$NON-NLS-1$

            Composite info = new Composite(container, SWT.NONE);
            info.setLayout(new RowLayout());

            Label l = new Label(info, SWT.NONE);
            l.setImage(Images.INFO.image());

            l = new Label(info, SWT.NONE);
            l.setText(Messages.MsgInfoChangingCurrencyNotPossible);

        }

        if (isExchangeRate)
        {
            ComboViewer targetCurrencyCode = bindings.bindCurrencyCodeCombo(container, Messages.ColumnTargetCurrency,
                            "targetCurrencyCode", false); //$NON-NLS-1$
            targetCurrencyCode.getCombo().setToolTipText(Messages.ColumnTargetCurrencyToolTip);
        }

        if (!isExchangeRate)
        {
            isin = bindings.bindISINInput(container, Messages.ColumnISIN, "isin"); //$NON-NLS-1$
            isin.setEnabled(!isSyncedOnline);
        }

        bindings.bindStringInput(container, Messages.ColumnTicker, "tickerSymbol", SWT.NONE, 12); //$NON-NLS-1$

        if (!isExchangeRate)
        {
            wkn = bindings.bindStringInput(container, Messages.ColumnWKN, "wkn", SWT.NONE, 12); //$NON-NLS-1$
            wkn.setEnabled(!isSyncedOnline);

            ComboViewer calendar = bindings.bindCalendarCombo(container, Messages.LabelSecurityCalendar, "calendar"); //$NON-NLS-1$
            calendar.getCombo().setToolTipText(Messages.LabelSecurityCalendarToolTip);
        }

        Control control = bindings.bindBooleanInput(container, Messages.ColumnRetired, "retired"); //$NON-NLS-1$

        int margin = 2;
        Image info = Images.INFO.image();
        Rectangle bounds = info.getBounds();

        GridDataFactory.fillDefaults().indent(bounds.width + margin, 0).applyTo(control);

        ControlDecoration deco = new ControlDecoration(control, SWT.CENTER | SWT.LEFT);
        deco.setDescriptionText(Messages.MsgInfoRetiredSecurities);
        deco.setImage(info);
        deco.setMarginWidth(margin);
        deco.show();

        Text valueNote = bindings.bindStringInput(container, Messages.ColumnNote, "note", //$NON-NLS-1$
                        SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, SWT.DEFAULT);
        GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, SWTHelper.lineHeight(valueNote) * 4)
                        .applyTo(valueNote);
    }
}
