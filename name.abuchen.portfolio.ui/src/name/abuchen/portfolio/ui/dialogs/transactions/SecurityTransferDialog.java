package name.abuchen.portfolio.ui.dialogs.transactions;

import static name.abuchen.portfolio.ui.util.FormDataFactory.startingWith;
import static name.abuchen.portfolio.ui.util.SWTHelper.amountWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.currencyWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.widest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
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
import name.abuchen.portfolio.ui.dialogs.transactions.AbstractTransactionDialog.ComboInput;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityTransferModel.Properties;
import name.abuchen.portfolio.ui.util.DateTimePicker;
import name.abuchen.portfolio.ui.util.SimpleDateTimeSelectionProperty;

public class SecurityTransferDialog extends AbstractTransactionDialog
{
    private final class PortfoliosMustBeDifferentValidator extends MultiValidator
    {
        IObservableValue source;
        IObservableValue target;

        public PortfoliosMustBeDifferentValidator(IObservableValue source, IObservableValue target)
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

    public static final class QuoteSuggestionSet
    {
        private final List<QuoteSuggestion> suggestionSet = new ArrayList<QuoteSuggestion>();

        public QuoteSuggestionSet()
        {
        }

        public void add(PortfolioTransferEntry.Suggestion suggestion, String label, boolean editable)
        {
            suggestionSet.add(new QuoteSuggestion (suggestion, label, editable));
        }

        public void remove(PortfolioTransferEntry.Suggestion suggestion)
        {
            suggestionSet.remove(suggestion);
        }

        public QuoteSuggestion[] get()
        {
            return suggestionSet.toArray(new QuoteSuggestion[0]);
        }

        public QuoteSuggestion get(PortfolioTransferEntry.Suggestion suggestion)
        {
            if (!suggestionSet.isEmpty())
            {
                for (QuoteSuggestion quoteSuggestion : suggestionSet)
                {
                    if (quoteSuggestion.suggestion.equals(suggestion))
                        return quoteSuggestion;
                }
            }
            return null;
        }

        public String toString()
        {
            return Arrays.toString(this.get());
        }
    }

    public static final class QuoteSuggestion
    {
        private final PortfolioTransferEntry.Suggestion suggestion;
        private final String label;
        private final boolean editable;

        public QuoteSuggestion(PortfolioTransferEntry.Suggestion suggestion, String label, boolean editable)
        {
            this.suggestion = suggestion;
            this.label = label;
            this.editable = editable;
        }

        public PortfolioTransferEntry.Suggestion getSuggestion()
        {
            return suggestion;
        }

        public String getLabel()
        {
            return label;
        }

        public boolean getEditable()
        {
            return editable;
        }

        @Override
        public String toString()
        {
            return "x " + getLabel();
        }
    }


    private Client client;
    private QuoteSuggestionSet quoteSuggestionSet = new QuoteSuggestionSet();

    @Inject
    public SecurityTransferDialog(@Named(IServiceConstants.ACTIVE_SHELL) Shell parentShell, Client client)
    {
        super(parentShell);
        this.client = client;
        setModel(new SecurityTransferModel(client));
        quoteSuggestionSet.add(PortfolioTransferEntry.Suggestion.goodwill, Messages.ColumnQuoteSuggestion_goodwill, true);
        quoteSuggestionSet.add(PortfolioTransferEntry.Suggestion.market, Messages.ColumnQuoteSuggestion_market, false);
        quoteSuggestionSet.add(PortfolioTransferEntry.Suggestion.purchase, Messages.ColumnQuoteSuggestion_purchase, false);
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
        IObservableValue sourceObservable = source.bindValue(Properties.sourcePortfolio.name(),
                        Messages.MsgPortfolioFromMissing);
        source.bindCurrency(Properties.sourcePortfolioLabel.name());

        // quote suggestion selection

        ComboInput quoteSuggestion = new ComboInput(editArea, "");
        quoteSuggestion.value.setInput(quoteSuggestionSet.get());
        IObservableValue quoteSuggestionObservable = quoteSuggestion.bindValue(Properties.quoteSuggestion.name(), Messages.MsgMissingSuggestion);
        quoteSuggestion.value.setSelection(new StructuredSelection(quoteSuggestionSet.get(model().getQuoteSuggestion())));

        // target portfolio

        ComboInput target = new ComboInput(editArea, Messages.ColumnAccountTo);
        target.value.setInput(including(client.getActivePortfolios(), model().getTargetPortfolio()));
        IObservableValue targetObservable = target.bindValue(Properties.targetPortfolio.name(),
                        Messages.MsgPortfolioToMissing);
        target.bindCurrency(Properties.targetPortfolioLabel.name());

        MultiValidator validator = new PortfoliosMustBeDifferentValidator(sourceObservable, targetObservable);
        context.addValidationStatusProvider(validator);

        // date

        Label lblDate = new Label(editArea, SWT.RIGHT);
        lblDate.setText(Messages.ColumnDate);
        DateTimePicker valueDate = new DateTimePicker(editArea);
        context.bindValue(new SimpleDateTimeSelectionProperty().observe(valueDate.getControl()),
                        BeanProperties.value(Properties.date.name()).observe(model));

        // amount

        Input shares = new Input(editArea, Messages.ColumnShares);
        shares.bindValue(Properties.shares.name(), Messages.ColumnShares, Values.Share, true);

        Input quote = new Input(editArea, "x " + Messages.ColumnQuote); //$NON-NLS-1$
        quote.bindBigDecimal(Properties.quote.name(), Values.Quote.pattern());
        quote.bindCurrency(Properties.securityCurrencyCode.name());
        quote.setEditable(quoteSuggestionSet.get(model().getQuoteSuggestion()).getEditable());

        Input amount = new Input(editArea, "="); //$NON-NLS-1$
        amount.bindValue(Properties.amount.name(), Messages.ColumnAmount, Values.Amount, true);
        amount.bindCurrency(Properties.securityCurrencyCode.name());

        // note

        Label lblNote = new Label(editArea, SWT.LEFT);
        lblNote.setText(Messages.ColumnNote);
        Text valueNote = new Text(editArea, SWT.BORDER);
        context.bindValue(WidgetProperties.text(SWT.Modify).observe(valueNote),
                        BeanProperties.value(Properties.note.name()).observe(model));

        //
        // form layout
        //

        int amountWidth = amountWidth(amount.value);
        int currencyWidth = currencyWidth(amount.currency);

        startingWith(securities.value.getControl(), securities.label).suffix(securities.currency)
                        .thenBelow(source.value.getControl()).label(source.label).suffix(source.currency)
                        .thenBelow(target.value.getControl()).label(target.label).suffix(target.currency)
                        .thenBelow(valueDate.getControl()).label(lblDate)
                        // shares - quote - amount
                        .thenBelow(shares.value).width(amountWidth).label(shares.label).thenRight(quoteSuggestion.value.getControl())
                        .thenRight(quote.value).width(amountWidth).thenRight(quote.currency).width(currencyWidth)
                        .thenRight(amount.label).thenRight(amount.value).width(amountWidth).thenRight(amount.currency)
                        .width(currencyWidth);

        startingWith(shares.value).thenBelow(valueNote).left(securities.value.getControl()).right(amount.value)
                        .label(lblNote);

        int widest = widest(securities.label, source.label, target.label, lblDate, amount.label, lblNote);
        startingWith(securities.label).width(widest);

        quoteSuggestion.value.addSelectionChangedListener(new ISelectionChangedListener()
        {
            @Override
            public void selectionChanged(SelectionChangedEvent event)
            {
                QuoteSuggestion selectedSuggestion = (QuoteSuggestion) ((IStructuredSelection) event.getSelection()).getFirstElement();
                boolean editable = selectedSuggestion.getEditable();
                quote.setEditable(editable);
                model().setQuoteSuggestion(selectedSuggestion.getSuggestion());
            }
        });
        model().updateQuote();
        WarningMessages warnings = new WarningMessages(this);
        warnings.add(() -> model().getDate().isAfter(LocalDate.now()) ? Messages.MsgDateIsInTheFuture : null);
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
