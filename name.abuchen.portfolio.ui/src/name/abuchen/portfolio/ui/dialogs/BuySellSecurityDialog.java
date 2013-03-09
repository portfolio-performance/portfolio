package name.abuchen.portfolio.ui.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.util.CurrencyToStringConverter;
import name.abuchen.portfolio.ui.util.StringToCurrencyConverter;
import name.abuchen.portfolio.util.Dates;

import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class BuySellSecurityDialog extends AbstractDialog
{
    static class Model extends BindingHelper.Model
    {
        private final PortfolioTransaction.Type type;

        private Portfolio portfolio;
        private Security security;
        private long shares;
        private long price;
        private long fees;
        private long total;
        private Date date = Dates.today();

        public Model(Client client, Security security, Type type)
        {
            super(client);

            this.security = security;
            this.type = type;

            if (type == PortfolioTransaction.Type.SELL && security != null)
            {
                ClientSnapshot snapshot = ClientSnapshot.create(client, Dates.today());
                for (PortfolioSnapshot portfolio : snapshot.getPortfolios())
                {
                    SecurityPosition position = portfolio.getPositionsBySecurity().get(security);
                    if (position != null)
                    {
                        setShares(position.getShares());
                        setPortfolio(portfolio.getSource());
                        setTotal(position.calculateValue());
                        break;
                    }
                }
            }
            else
            {
                if (!client.getPortfolios().isEmpty())
                    setPortfolio(client.getPortfolios().get(0));
                if (security == null && !client.getSecurities().isEmpty())
                    setSecurity(client.getSecurities().get(0));
            }
        }

        public long getPrice()
        {
            return price;
        }

        private long calculatePrice()
        {
            if (shares == 0)
            {
                return 0;
            }
            else
            {
                switch (type)
                {
                    case BUY:
                        return Math.max(0, (total - fees) * Values.Share.factor() / shares);
                    case SELL:
                        return Math.max(0, (total + fees) * Values.Share.factor() / shares);
                    default:
                        throw new RuntimeException("Unsupported transaction type for dialog " + type); //$NON-NLS-1$
                }
            }

        }

        public Portfolio getPortfolio()
        {
            return portfolio;
        }

        public void setPortfolio(Portfolio portfolio)
        {
            firePropertyChange("portfolio", this.portfolio, this.portfolio = portfolio); //$NON-NLS-1$
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

        public long getFees()
        {
            return fees;
        }

        public void setFees(long fees)
        {
            firePropertyChange("fees", this.fees, this.fees = fees); //$NON-NLS-1$
            firePropertyChange("price", this.price, this.price = calculatePrice()); //$NON-NLS-1$
        }

        public long getTotal()
        {
            return total;
        }

        public void setTotal(long total)
        {
            firePropertyChange("total", this.total, this.total = total); //$NON-NLS-1$
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

        @Override
        public void applyChanges()
        {
            if (security == null)
                throw new UnsupportedOperationException(Messages.MsgMissingSecurity);
            if (portfolio.getReferenceAccount() == null)
                throw new UnsupportedOperationException(Messages.MsgMissingReferenceAccount);

            BuySellEntry t = new BuySellEntry(portfolio, portfolio.getReferenceAccount());
            t.setDate(date);
            t.setSecurity(security);
            t.setShares(shares);
            t.setFees(fees);
            t.setAmount(total);
            t.setType(type);
            t.insert();
        }
    }

    private boolean allowSelectionOfSecurity = false;

    public BuySellSecurityDialog(Shell parentShell, Client client, Security security, PortfolioTransaction.Type type)
    {
        super(parentShell, security != null ? type.toString() + " " + security.getName() : type.toString(), //$NON-NLS-1$
                        new Model(client, security, type));

        if (!(type == PortfolioTransaction.Type.BUY || type == PortfolioTransaction.Type.SELL))
            throw new UnsupportedOperationException("dialog supports only BUY or SELL operation"); //$NON-NLS-1$

        this.allowSelectionOfSecurity = security == null;
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        // security selection
        if (!allowSelectionOfSecurity)
        {
            bindings().createLabel(editArea, ((Model) getModel()).getSecurity().getName());
        }
        else
        {
            List<Security> securities = new ArrayList<Security>();
            for (Security s : getModel().getClient().getSecurities())
                if (!s.isRetired())
                    securities.add(s);
            Collections.sort(securities, new Security.ByName());

            bindings().bindComboViewer(editArea, Messages.ColumnSecurity, "security", new LabelProvider() //$NON-NLS-1$
                            {
                                @Override
                                public String getText(Object element)
                                {
                                    return ((Security) element).getName();
                                }
                            }, securities.toArray());
        }

        // portfolio selection
        bindings().bindComboViewer(editArea, Messages.ColumnPortfolio, "portfolio", new LabelProvider() //$NON-NLS-1$
                        {
                            @Override
                            public String getText(Object element)
                            {
                                return ((Portfolio) element).getName();
                            }
                        }, getModel().getClient().getPortfolios().toArray());

        // shares
        bindings().bindMandatorySharesInput(editArea, Messages.ColumnShares, "shares").setFocus(); //$NON-NLS-1$

        // price
        Label label = new Label(editArea, SWT.NONE);
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

        // fee
        bindings().bindAmountInput(editArea, Messages.ColumnFees, "fees"); //$NON-NLS-1$

        // total
        bindings().bindMandatoryAmountInput(editArea, Messages.ColumnTotal, "total"); //$NON-NLS-1$

        // date
        bindings().bindDatePicker(editArea, Messages.ColumnDate, "date"); //$NON-NLS-1$
    }
}
