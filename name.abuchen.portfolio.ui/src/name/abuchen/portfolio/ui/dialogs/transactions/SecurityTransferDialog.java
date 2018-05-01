package name.abuchen.portfolio.ui.dialogs.transactions;

import static name.abuchen.portfolio.ui.util.FormDataFactory.startingWith;
import static name.abuchen.portfolio.ui.util.SWTHelper.amountWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.currencyWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.widest;

import java.time.LocalDateTime;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransferModel.Properties;

public class SecurityTransferDialog extends AbstractTransactionDialog
{
    private final class PortfoliosMustBeDifferentValidator extends MultiValidator
    {
        IObservableValue<?> source;
        IObservableValue<?> target;

        public PortfoliosMustBeDifferentValidator(IObservableValue<?> source, IObservableValue<?> target)
        {
            this.source = source;
            this.target = target;
        }

        @Override
        protected IStatus validate()
        {
            Object from = source.getValue();
            Object to = target.getValue();

            return from != null && to != null && from != to ? ValidationStatus.ok()
                            : ValidationStatus.error(Messages.MsgPortfolioMustBeDifferent);
        }
    }

    private Client client;

    @Inject
    public SecurityTransferDialog(@Named(IServiceConstants.ACTIVE_SHELL) Shell parentShell, Client client)
    {
        super(parentShell);
        this.client = client;
        setModel(new SecurityTransferModel(client));
    }

    private SecurityTransferModel model()
    {
        return (SecurityTransferModel) this.model;
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        //
        // input elements
        //

        // security

        ComboInput securities = new ComboInput(editArea, Messages.ColumnSecurity);
        securities.value.setInput(including(client.getActiveSecurities(), model().getSecurity()));
        securities.bindValue(Properties.security.name(), Messages.MsgMissingSecurity);
        securities.bindCurrency(Properties.securityCurrencyCode.name());

        // source portfolio

        ComboInput source = new ComboInput(editArea, Messages.ColumnAccountFrom);
        source.value.setInput(including(client.getActivePortfolios(), model().getSourcePortfolio()));
        IObservableValue<?> sourceObservable = source.bindValue(Properties.sourcePortfolio.name(),
                        Messages.MsgPortfolioFromMissing);
        source.bindCurrency(Properties.sourcePortfolioLabel.name());

        // target portfolio

        ComboInput target = new ComboInput(editArea, Messages.ColumnAccountTo);
        target.value.setInput(including(client.getActivePortfolios(), model().getTargetPortfolio()));
        IObservableValue<?> targetObservable = target.bindValue(Properties.targetPortfolio.name(),
                        Messages.MsgPortfolioToMissing);
        target.bindCurrency(Properties.targetPortfolioLabel.name());

        MultiValidator validator = new PortfoliosMustBeDifferentValidator(sourceObservable, targetObservable);
        context.addValidationStatusProvider(validator);

        // date + time

        DateTimeInput dateTime = new DateTimeInput(editArea, Messages.ColumnDate);
        dateTime.bindDate(Properties.date.name());
        dateTime.bindTime(Properties.time.name());
        dateTime.bindButton(() -> model().getTime(), time -> model().setTime(time));

        // amount

        Input shares = new Input(editArea, Messages.ColumnShares);
        shares.bindValue(Properties.shares.name(), Messages.ColumnShares, Values.Share, true);

        Input quote = new Input(editArea, "x " + Messages.ColumnQuote); //$NON-NLS-1$
        quote.bindBigDecimal(Properties.quote.name(), Values.Quote.pattern());
        quote.bindCurrency(Properties.securityCurrencyCode.name());

        Input amount = new Input(editArea, "="); //$NON-NLS-1$
        amount.bindValue(Properties.amount.name(), Messages.ColumnAmount, Values.Amount, true);
        amount.bindCurrency(Properties.securityCurrencyCode.name());

        // note

        Label lblNote = new Label(editArea, SWT.LEFT);
        lblNote.setText(Messages.ColumnNote);
        Text valueNote = new Text(editArea, SWT.BORDER);
        @SuppressWarnings("unchecked")
        IObservableValue<?> noteObservable = BeanProperties.value(Properties.note.name()).observe(model);
        context.bindValue(WidgetProperties.text(SWT.Modify).observe(valueNote), noteObservable);

        //
        // form layout
        //

        int amountWidth = amountWidth(amount.value);
        int currencyWidth = currencyWidth(amount.currency);

        startingWith(securities.value.getControl(), securities.label).suffix(securities.currency)
                        .thenBelow(source.value.getControl()).label(source.label).suffix(source.currency)
                        .thenBelow(target.value.getControl()).label(target.label).suffix(target.currency)
                        .thenBelow(dateTime.date.getControl()).label(dateTime.label).thenRight(dateTime.time)
                        .thenRight(dateTime.button, 0);

        // shares - quote - amount
        startingWith(dateTime.date.getControl()).thenBelow(shares.value).width(amountWidth).label(shares.label)
                        .thenRight(quote.label).thenRight(quote.value).width(amountWidth).thenRight(quote.currency)
                        .width(currencyWidth).thenRight(amount.label).thenRight(amount.value).width(amountWidth)
                        .thenRight(amount.currency).width(currencyWidth);

        startingWith(shares.value).thenBelow(valueNote).left(securities.value.getControl()).right(amount.value)
                        .label(lblNote);

        int widest = widest(securities.label, source.label, target.label, dateTime.label, shares.label, lblNote);
        startingWith(securities.label).width(widest);

        WarningMessages warnings = new WarningMessages(this);
        warnings.add(() -> LocalDateTime.of(model().getDate(), model().getTime()).isAfter(LocalDateTime.now())
                        ? Messages.MsgDateIsInTheFuture
                        : null);
        warnings.add(() -> new StockSplitWarning().check(model().getSecurity(), model().getDate()));
        model.addPropertyChangeListener(Properties.security.name(), e -> warnings.check());
        model.addPropertyChangeListener(Properties.date.name(), e -> warnings.check());
    }

    @Override
    public void setSecurity(Security security)
    {
        model().setSecurity(security);
    }

    @Override
    public void setPortfolio(Portfolio portfolio)
    {
        model().setSourcePortfolio(portfolio);
    }

    public void setEntry(PortfolioTransferEntry entry)
    {
        model().setSource(entry);
    }
}
