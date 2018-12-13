package name.abuchen.portfolio.ui.wizards.security;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import de.jollyday.HolidayCalendar;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.util.TradeCalendarCode;
import name.abuchen.portfolio.util.TradeCalendarProvinceCode;

public class SecurityMasterDataPage extends AbstractPage
{
    private final EditSecurityModel model;
    private final BindingHelper bindings;

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
            bindings.bindISINInput(container, Messages.ColumnISIN, "isin"); //$NON-NLS-1$
        bindings.bindStringInput(container, Messages.ColumnTicker, "tickerSymbol", SWT.NONE, 12); //$NON-NLS-1$
        if (!isExchangeRate)
            bindings.bindStringInput(container, Messages.ColumnWKN, "wkn", SWT.NONE, 12); //$NON-NLS-1$
        if (!isExchangeRate)
        {
            // empty cell
            new Label(container, SWT.NONE).setText(""); //$NON-NLS-1$
            Composite seperator1 = new Composite(container, SWT.NONE);
            seperator1.setLayout(new RowLayout());
            new Label(seperator1, SWT.SEPARATOR | SWT.HORIZONTAL);

            ComboViewer calendar = bindings.bindCalendarCombo(container, Messages.LabelSecurityCalendar, "calendar"); //$NON-NLS-1$
            calendar.getCombo().setToolTipText(Messages.LabelSecurityCalendarToolTip);

            IStructuredSelection calendarSelection = (IStructuredSelection) calendar.getSelection();
            TradeCalendarCode calendarSelectionCode = (TradeCalendarCode) calendarSelection.getFirstElement();
            ComboViewer calendarProvince = bindings.bindCalendarProvinceCombo(container,
                            Messages.LabelSecurityCalendarProvince, "calendarProvince", //$NON-NLS-1$
                            (String) calendarSelectionCode.getCalendarCode());
            calendarProvince.getCombo().setToolTipText(Messages.LabelSecurityCalendarProvinceToolTip);
            calendar.addSelectionChangedListener(new ISelectionChangedListener()
            {

                @Override
                public void selectionChanged(SelectionChangedEvent paramSelectionChangedEvent)
                {
                    IStructuredSelection calendarSelection = (IStructuredSelection) calendar.getSelection();
                    TradeCalendarCode calendarSelectionCode = (TradeCalendarCode) calendarSelection.getFirstElement();
                    List<TradeCalendarProvinceCode> calendarProvinceUpdate = new ArrayList<>();
                    calendarProvinceUpdate.add(TradeCalendarProvinceCode.EMPTY);
                    calendarProvinceUpdate.addAll(TradeCalendarProvinceCode
                                    .getAvailableCalendarProvinces(
                                                    HolidayCalendar.valueOf(calendarSelectionCode.getCalendarCode()))
                                    .stream().sorted().collect(Collectors.toList()));
                    calendarProvince.setInput(calendarProvinceUpdate);
                    if (calendarProvinceUpdate.size() == 1)
                    {
                        calendarProvince.setInput(null);
                        calendarProvince.getControl().setEnabled(false);
                    }
                    else
                        calendarProvince.getControl().setEnabled(true);
                    container.layout(true);
                }
            });

            // empty cell
            new Label(container, SWT.NONE).setText(""); //$NON-NLS-1$
            Composite seperator2 = new Composite(container, SWT.NONE);
            seperator2.setLayout(new RowLayout());
            new Label(seperator2, SWT.SEPARATOR | SWT.HORIZONTAL);
        }

        Control control = bindings.bindBooleanInput(container, Messages.ColumnRetired, "retired"); //$NON-NLS-1$
        Image image = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
                        .getImage();
        ControlDecoration deco = new ControlDecoration(control, SWT.TOP | SWT.LEFT);
        deco.setDescriptionText(Messages.MsgInfoRetiredSecurities);
        deco.setImage(image);
        deco.show();

        bindings.bindStringInput(container, Messages.ColumnNote, "note"); //$NON-NLS-1$
    }
}
