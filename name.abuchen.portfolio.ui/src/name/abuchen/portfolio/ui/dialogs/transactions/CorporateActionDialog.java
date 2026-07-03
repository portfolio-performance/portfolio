package name.abuchen.portfolio.ui.dialogs.transactions;

import static name.abuchen.portfolio.ui.util.FormDataFactory.startingWith;
import static name.abuchen.portfolio.ui.util.SWTHelper.amountWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.widest;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.conversion.text.NumberToStringConverter;
import org.eclipse.core.databinding.conversion.text.StringToNumberConverter;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CorporateActionEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.SpinOffModel.Properties;
import name.abuchen.portfolio.ui.util.IValidatingConverter;
import name.abuchen.portfolio.ui.util.SecurityNameLabelProvider;

public class CorporateActionDialog extends AbstractTransactionDialog // NOSONAR
{
    @Inject
    private IStylingEngine stylingEngine;

    @Inject
    private Client client;

    @Inject
    public CorporateActionDialog(@Named(IServiceConstants.ACTIVE_SHELL) Shell parentShell)
    {
        super(parentShell);
    }

    @PostConstruct
    private void createModel(ExchangeRateProviderFactory factory) // NOSONAR
    {
        var model = new SpinOffModel(client);
        model.setExchangeRateProviderFactory(factory);
        setModel(model);

        // set portfolio only if exactly one exists
        // (otherwise force user to choose)
        var activePortfolios = client.getActivePortfolios();
        if (activePortfolios.size() == 1)
            model.setPortfolio(activePortfolios.get(0));
    }

    private SpinOffModel model()
    {
        return (SpinOffModel) this.model;
    }

    private static String defaultString(String s)
    {
        return s == null ? "" : s;
    }

    private void bindInteger(Text text, String property)
    {
        IValidatingConverter<Object, Integer> converter = IValidatingConverter
                        .wrap(StringToNumberConverter.toInteger(true));

        var target = WidgetProperties.text(SWT.Modify).observe(text);
        var modelValue = BeanProperties.value(property, Integer.class).observe(model);
        context.bindValue(target, modelValue,
                        new UpdateValueStrategy<String, Integer>().setAfterGetValidator(converter)
                                        .setConverter(converter),
                        new UpdateValueStrategy<Integer, String>()
                                        .setConverter(NumberToStringConverter.fromInteger(true)));
    }

    @Override
    public void setSecurity(Security security)
    {
        model().setSourceSecurity(security);
    }

    @Override
    public void setPortfolio(Portfolio portfolio)
    {
        model().setPortfolio(portfolio);
    }

    /**
     * Puts the dialog into edit mode for an existing spin-off group (Task
     * 6.6 wires this from {@code OpenDialogAction}).
     */
    public void setCorporateAction(CorporateActionEntry entry)
    {
        model().setSource(entry);
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        //
        // input elements
        //

        // source security (pre-filled from the security context menu)
        ComboInput source = new ComboInput(editArea, Messages.LabelSpinOffSource);
        source.value.setInput(including(client.getActiveSecurities(), model().getSourceSecurity()));
        source.value.setLabelProvider(new SecurityNameLabelProvider(client));
        source.bindValue(Properties.sourceSecurity.name(), Messages.MsgMissingSecurity);

        // target security (pick an existing security; the model transiently
        // adds a newly created Security to the client on apply, so a later
        // inline "+ new security" affordance can be layered without touching
        // the model -- kept minimal for this task, see brief)
        ComboInput target = new ComboInput(editArea, Messages.LabelSpinOffTarget);
        target.value.setInput(including(client.getActiveSecurities(), model().getTargetSecurity()));
        target.value.setLabelProvider(new SecurityNameLabelProvider(client));
        target.bindValue(Properties.targetSecurity.name(), Messages.MsgMissingSecurity);

        // portfolio (the event is per-portfolio, §10)
        ComboInput portfolio = new ComboInput(editArea, Messages.ColumnPortfolio);
        portfolio.value.setInput(including(client.getActivePortfolios(), model().getPortfolio()));
        portfolio.bindValue(Properties.portfolio.name(), Messages.MsgMissingPortfolio);

        // ex-date (date only; the model has no separate time-of-day)
        DateTimeInput exDate = new DateTimeInput(editArea, Messages.ColumnExDate);
        exDate.bindDate(Properties.exDate.name());
        exDate.time.setVisible(false);
        exDate.button.setVisible(false);

        // distribution ratio: N new shares for every M shares held (§10).
        // Seeds the entitlement in the model; entitlement stays editable.
        Input ratioNumerator = new Input(editArea, Messages.LabelSpinOffRatioNumerator);
        bindInteger(ratioNumerator.value, Properties.distributionNumerator.name());

        Input ratioDenominator = new Input(editArea, Messages.LabelSpinOffRatioDenominator);
        bindInteger(ratioDenominator.value, Properties.distributionDenominator.name());

        // entitlement (shares received; a persisted fact, §11)
        Input entitlement = new Input(editArea, Messages.LabelSpinOffEntitlement);
        entitlement.bindValue(Properties.entitlement.name(), Messages.ColumnShares, Values.Share, true);

        // FMV helper: enter parent + spinco fair-market-value/share, then
        // derive the basis ratio (only the ratio + reference price persist)
        Input parentFmv = new Input(editArea, Messages.LabelSpinOffParentFmv);
        parentFmv.bindValue(Properties.parentFmvPerShare.name(), Messages.LabelSpinOffParentFmv, Values.Quote,
                        false);

        Input spinOffFmv = new Input(editArea, Messages.LabelSpinOffSpinOffFmv);
        spinOffFmv.bindValue(Properties.spinOffFmvPerShare.name(), Messages.LabelSpinOffSpinOffFmv, Values.Quote,
                        false);

        Button computeRatio = new Button(editArea, SWT.PUSH);
        computeRatio.setText(Messages.LabelSpinOffComputeRatio);
        computeRatio.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> model().applyFmvHelper()));

        // basis ratio (0..1; also derivable via the FMV helper above)
        Input basisRatio = new Input(editArea, Messages.LabelSpinOffBasisRatio);
        basisRatio.bindBigDecimal(Properties.basisRatio.name(), "0.0000");

        // reference price (spinco FMV/share, §10) -- also seeded by the FMV helper
        Input referencePrice = new Input(editArea, Messages.LabelSpinOffReferencePrice);
        referencePrice.bindValue(Properties.spinOffPricePerShare.name(), Messages.LabelSpinOffReferencePrice,
                        Values.Quote, false);

        // cash-in-lieu (floored fraction -> cash; overridable to the broker figure)
        Input cashInLieu = new Input(editArea, Messages.LabelSpinOffCashInLieu);
        cashInLieu.bindValue(Properties.cashInLieuAmount.name(), Messages.LabelSpinOffCashInLieu, Values.Amount,
                        false);

        // exchange rate (shown only when source/target currencies differ, §6)
        ExchangeRateInput exchangeRate = new ExchangeRateInput(editArea, "x ");
        exchangeRate.bindBigDecimal(Properties.exchangeRate.name(), Values.ExchangeRate.pattern());
        exchangeRate.bindInvertAction(() -> model().setExchangeRate(
                        BigDecimal.ONE.divide(model().getExchangeRate(), 10, RoundingMode.HALF_DOWN)));

        // conservation summary (read-only, live-updated, §10)
        Label conservation = new Label(editArea, SWT.WRAP);
        conservation.setText(model().getConservationSummary());
        model().addPropertyChangeListener(Properties.conservationSummary.name(),
                        e -> conservation.setText(model().getConservationSummary()));

        // §13.2 soft-warn: shown when entitlement diverges from ratio x holdings.
        // A separate label -- NOT calculationStatus -- because OK is gated on
        // severity == OK (AbstractTransactionDialog), so a validation warning
        // would wrongly disable OK.
        Label entitlementWarning = new Label(editArea, SWT.WRAP);
        entitlementWarning.setText(defaultString(model().getEntitlementWarning()));
        model().addPropertyChangeListener(Properties.entitlementWarning.name(),
                        e -> entitlementWarning.setText(defaultString(model().getEntitlementWarning())));

        //
        // form layout
        //

        // measuring the width requires that the font has been applied before
        stylingEngine.style(editArea);

        int width = amountWidth(entitlement.value);
        int labelWidth = widest(source.label, target.label, portfolio.label, exDate.label, ratioNumerator.label,
                        ratioDenominator.label, entitlement.label, parentFmv.label, spinOffFmv.label,
                        basisRatio.label, referencePrice.label, cashInLieu.label, exchangeRate.label);

        startingWith(source.value.getControl(), source.label) //
                        .thenBelow(target.value.getControl()).label(target.label) //
                        .thenBelow(portfolio.value.getControl()).label(portfolio.label) //
                        .thenBelow(exDate.date.getControl()).label(exDate.label) //
                        .thenBelow(ratioNumerator.value).width(width).label(ratioNumerator.label) //
                        .thenBelow(ratioDenominator.value).width(width).label(ratioDenominator.label) //
                        .thenBelow(entitlement.value).width(width).label(entitlement.label) //
                        .thenBelow(parentFmv.value).width(width).label(parentFmv.label) //
                        .thenBelow(spinOffFmv.value).width(width).label(spinOffFmv.label) //
                        .thenRight(computeRatio);

        startingWith(spinOffFmv.value) //
                        .thenBelow(basisRatio.value).width(width).label(basisRatio.label) //
                        .thenBelow(referencePrice.value).width(width).label(referencePrice.label) //
                        .thenBelow(cashInLieu.value).width(width).label(cashInLieu.label) //
                        .thenBelow(exchangeRate.value).width(width).label(exchangeRate.label)
                        .thenRight(exchangeRate.buttonInvertExchangeRate, 0).thenRight(exchangeRate.currency)
                        .width(width);

        startingWith(exchangeRate.value) //
                        .thenBelow(conservation).left(source.value.getControl()).right(cashInLieu.value);

        startingWith(conservation) //
                        .thenBelow(entitlementWarning).left(source.value.getControl()).right(cashInLieu.value);

        startingWith(source.label).width(labelWidth);

        //
        // hide / show exchange rate depending on whether the source and
        // target securities' currencies differ (mirrors the
        // exchangeRateCurrencies listener in SecurityTransactionDialog;
        // SpinOffModel has no dedicated currency-pair property, so this
        // listens on both securities directly)
        //

        Runnable updateExchangeRateVisibility = () -> {
            Security sourceSecurity = model().getSourceSecurity();
            Security targetSecurity = model().getTargetSecurity();
            String sourceCurrency = sourceSecurity != null ? sourceSecurity.getCurrencyCode() : "";
            String targetCurrency = targetSecurity != null ? targetSecurity.getCurrencyCode() : "";

            boolean visible = sourceCurrency.length() > 0 && targetCurrency.length() > 0
                            && !sourceCurrency.equals(targetCurrency);

            exchangeRate.setVisible(visible);
            exchangeRate.currency.setText(visible ? sourceCurrency + "/" + targetCurrency : "");

            if (!visible)
                model().setExchangeRate(BigDecimal.ONE);
        };

        model().addPropertyChangeListener(Properties.sourceSecurity.name(), e -> updateExchangeRateVisibility.run());
        model().addPropertyChangeListener(Properties.targetSecurity.name(), e -> updateExchangeRateVisibility.run());
        updateExchangeRateVisibility.run();
    }
}
