package name.abuchen.portfolio.ui.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.util.CurrencyToStringConverter;
import name.abuchen.portfolio.ui.util.StringToCurrencyConverter;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class SecurityTransferDialog extends AbstractDialog
{
    static class Model extends BindingHelper.Model
    {
        private Portfolio portfolioFrom;
        private Portfolio portfolioTo;
        private Security security;
        private long shares;
        private long price;
        private long amount;
        private Date date = Dates.today();

        public Model(Client client, Portfolio portfolioFrom)
        {
            super(client);
            this.portfolioFrom = portfolioFrom;

            for (Portfolio p : client.getActivePortfolios())
            {
                if (!p.equals(portfolioFrom))
                {
                    this.portfolioTo = p;
                    break;
                }
            }
        }

        public Portfolio getPortfolioFrom()
        {
            return portfolioFrom;
        }

        public void setPortfolioFrom(Portfolio portfolioFrom)
        {
            firePropertyChange("portfolioFrom", this.portfolioFrom, this.portfolioFrom = portfolioFrom); //$NON-NLS-1$
        }

        public Portfolio getPortfolioTo()
        {
            return portfolioTo;
        }

        public void setPortfolioTo(Portfolio portfolioTo)
        {
            firePropertyChange("portfolioTo", this.portfolioTo, this.portfolioTo = portfolioTo); //$NON-NLS-1$
        }

        public Security getSecurity()
        {
            return security;
        }

        public void setSecurity(Security security)
        {
            firePropertyChange("security", this.security, this.security = security); //$NON-NLS-1$
        }

        public long getShares()
        {
            return shares;
        }

        public void setShares(long shares)
        {
            firePropertyChange("shares", this.shares, this.shares = shares); //$NON-NLS-1$
            firePropertyChange("price", this.price, this.price = calculatePrice()); //$NON-NLS-1$
        }

        public long getPrice()
        {
            return price;
        }

        private long calculatePrice()
        {
            return shares == 0 ? 0 : Math.max(0, amount * Values.Share.factor() / shares);
        }

        public long getAmount()
        {
            return amount;
        }

        public void setAmount(long amount)
        {
            firePropertyChange("amount", this.amount, this.amount = amount); //$NON-NLS-1$
            firePropertyChange("price", this.price, this.price = calculatePrice()); //$NON-NLS-1$
        }

        public Date getDate()
        {
            return date;
        }

        public void setDate(Date date)
        {
            firePropertyChange("date", this.date, this.date = date); //$NON-NLS-1$
        }

        public void applyChanges()
        {
            if (portfolioFrom == null)
                throw new UnsupportedOperationException(Messages.MsgPortfolioFromMissing);
            if (portfolioTo == null)
                throw new UnsupportedOperationException(Messages.MsgPortfolioToMissing);

            PortfolioTransferEntry t = new PortfolioTransferEntry(portfolioFrom, portfolioTo);
            t.setDate(date);
            t.setSecurity(security);
            t.setShares(shares);
            t.setAmount(amount);
            t.insert();
        }
    }

    public SecurityTransferDialog(Shell parentShell, Client client, Portfolio from)
    {
        super(parentShell, Messages.SecurityMenuTransfer, new Model(client, from));
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        GridDataFactory gdf = GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false);

        // portfolio from
        Label label = new Label(editArea, SWT.NONE);
        label.setText(Messages.ColumnPortfolioFrom);
        ComboViewer comboFrom = new ComboViewer(editArea, SWT.READ_ONLY);
        comboFrom.setContentProvider(ArrayContentProvider.getInstance());
        comboFrom.setInput(getModel().getClient().getActivePortfolios().toArray());
        gdf.applyTo(comboFrom.getControl());
        final IViewerObservableValue observableFrom = ViewersObservables.observeSingleSelection(comboFrom);

        // portfolio to
        label = new Label(editArea, SWT.NONE);
        label.setText(Messages.ColumnPortfolioTo);
        ComboViewer comboTo = new ComboViewer(editArea, SWT.READ_ONLY);
        comboTo.setContentProvider(ArrayContentProvider.getInstance());
        comboTo.setInput(getModel().getClient().getActivePortfolios().toArray());
        gdf.applyTo(comboTo.getControl());
        final IViewerObservableValue observableTo = ViewersObservables.observeSingleSelection(comboTo);

        // security
        List<Security> securities = new ArrayList<Security>();
        securities.addAll(ClientSnapshot.create(getModel().getClient(), Dates.today()).getJointPortfolio()
                        .getPositionsBySecurity().keySet());
        Collections.sort(securities, new Security.ByName());

        bindings().bindComboViewer(editArea, Messages.ColumnSecurity, "security", new LabelProvider() //$NON-NLS-1$
                        {
                            @Override
                            public String getText(Object element)
                            {
                                return ((Security) element).getName();
                            }
                        }, securities.toArray());

        // shares
        bindings().bindMandatorySharesInput(editArea, Messages.ColumnShares, "shares").setFocus(); //$NON-NLS-1$

        // price
        label = new Label(editArea, SWT.NONE);
        label.setText(Messages.ColumnPrice);
        Label lblPrice = new Label(editArea, SWT.BORDER | SWT.READ_ONLY | SWT.NO_FOCUS);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(lblPrice);

        getBindingContext().bindValue(
                        SWTObservables.observeText(lblPrice),
                        BeansObservables.observeValue(getModel(), "price"), //$NON-NLS-1$
                        new UpdateValueStrategy(false, UpdateValueStrategy.POLICY_UPDATE)
                                        .setConverter(new StringToCurrencyConverter(Values.Amount)), //
                        new UpdateValueStrategy(false, UpdateValueStrategy.POLICY_UPDATE)
                                        .setConverter(new CurrencyToStringConverter(Values.Amount)));

        // amount
        bindings().bindMandatoryAmountInput(editArea, Messages.ColumnAmount, "amount"); //$NON-NLS-1$

        // date
        bindings().bindDatePicker(editArea, Messages.ColumnDate, "date"); //$NON-NLS-1$

        //
        // Bind UI
        //

        DataBindingContext context = getBindingContext();

        // multi validator (to and from portfolio must not be identical)
        MultiValidator validator = new MultiValidator()
        {
            @Override
            protected IStatus validate()
            {
                Object from = observableFrom.getValue();
                Object to = observableTo.getValue();

                return from != null && to != null && from != to ? ValidationStatus.ok() : ValidationStatus
                                .error(Messages.MsgPortfolioMustBeDifferent);
            }
        };
        context.addValidationStatusProvider(validator);

        context.bindValue(validator.observeValidatedValue(observableFrom), //
                        BeansObservables.observeValue(getModel(), "portfolioFrom")); //$NON-NLS-1$

        context.bindValue(validator.observeValidatedValue(observableTo), //
                        BeansObservables.observeValue(getModel(), "portfolioTo")); //$NON-NLS-1$

    }
}
