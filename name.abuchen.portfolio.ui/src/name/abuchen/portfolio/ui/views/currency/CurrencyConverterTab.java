package name.abuchen.portfolio.ui.views.currency;

import static name.abuchen.portfolio.ui.util.FormDataFactory.startingWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.util.BindingHelper.Model;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.views.AbstractTabbedView;

public class CurrencyConverterTab implements AbstractTabbedView.Tab
{
    public static class CurrencyConverterModel extends Model
    {
        private ExchangeRateProviderFactory factory;
        private CurrencyConverter currencyConverter;

        private long baseValue = Values.Amount.factorize(100);
        private String baseCurrency = CurrencyUnit.EUR;
        private long termValue;
        private String termCurrency = CurrencyUnit.USD;
        private LocalDate date = LocalDate.now();

        @Override
        public void applyChanges()
        {
            throw new UnsupportedOperationException();
        }

        public CurrencyConverterModel(ExchangeRateProviderFactory factory)
        {
            this.factory = Objects.requireNonNull(factory);
            this.currencyConverter = new CurrencyConverterImpl(factory, getTermCurrency());
            convert();
        }

        private void convert()
        {
            Money base = Money.of(getBaseCurrency(), getBaseValue());
            Money term = currencyConverter.with(getTermCurrency()).convert(getDate(), base);
            setTermValue(term.getAmount());
        }

        public ExchangeRateTimeSeries getExchangeRateTimeSeries()
        {
            return factory.getTimeSeries(getBaseCurrency(), getTermCurrency());
        }

        public long getBaseValue()
        {
            return baseValue;
        }

        public void setBaseValue(long baseValue)
        {
            firePropertyChange("baseValue", this.baseValue, this.baseValue = baseValue); // NOSONAR //$NON-NLS-1$
            convert();
        }

        public String getBaseCurrency()
        {
            return baseCurrency;
        }

        public void setBaseCurrency(String baseCurrency)
        {
            firePropertyChange("baseCurrency", this.baseCurrency, this.baseCurrency = baseCurrency); // NOSONAR //$NON-NLS-1$
            convert();
        }

        public long getTermValue()
        {
            return termValue;
        }

        public void setTermValue(long termValue)
        {
            firePropertyChange("termValue", this.termValue, this.termValue = termValue); // NOSONAR //$NON-NLS-1$
        }

        public String getTermCurrency()
        {
            return termCurrency;
        }

        public void setTermCurrency(String termCurrency)
        {
            firePropertyChange("termCurrency", this.termCurrency, this.termCurrency = termCurrency); // NOSONAR //$NON-NLS-1$
            convert();
        }

        public LocalDate getDate()
        {
            return date;
        }

        public void setDate(LocalDate date)
        {
            firePropertyChange("date", this.date, this.date = date); // NOSONAR //$NON-NLS-1$
            convert();
        }
    }

    private static class ExchangeRateTimeSeriesContentProvider implements ITreeContentProvider
    {

        @Override
        public Object[] getElements(Object inputElement)
        {
            return new Object[] { ((CurrencyConverterModel) inputElement).getExchangeRateTimeSeries() };
        }

        @Override
        public Object[] getChildren(Object parentElement)
        {
            return ((ExchangeRateTimeSeries) parentElement).getComposition().toArray();
        }

        @Override
        public Object getParent(Object element)
        {
            return null;
        }

        @Override
        public boolean hasChildren(Object element)
        {
            return !((ExchangeRateTimeSeries) element).getComposition().isEmpty();
        }
    }

    @Inject
    private IPreferenceStore preferences;

    private CurrencyConverterModel model;
    private BindingHelper bindings;

    @PostConstruct
    private void createModel(ExchangeRateProviderFactory factory)
    {
        this.model = new CurrencyConverterModel(factory);
        this.bindings = new BindingHelper(model);
    }

    @Override
    public String getTitle()
    {
        return Messages.LabelCurrencyConverter;
    }

    @Override
    public Composite createTab(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(Colors.WHITE);

        FormLayout layout = new FormLayout();
        layout.marginLeft = 20;
        layout.spacing = 20;
        container.setLayout(layout);

        Composite editArea = new Composite(container, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(editArea);

        bindings.createErrorLabel(editArea);
        bindings.bindMandatoryAmountInput(editArea, Messages.ColumnAmount, "baseValue", SWT.NONE, 10); //$NON-NLS-1$
        bindings.bindCurrencyCodeCombo(editArea, Messages.ColumnBaseCurrency, "baseCurrency", false); //$NON-NLS-1$
        bindings.bindMandatoryAmountInput(editArea, Messages.ColumnConvertedAmount, "termValue", SWT.READ_ONLY, 10); //$NON-NLS-1$
        bindings.bindCurrencyCodeCombo(editArea, Messages.ColumnTermCurrency, "termCurrency", false); //$NON-NLS-1$
        bindings.bindDatePicker(editArea, Messages.ColumnDate, "date").setBackground(Colors.WHITE); //$NON-NLS-1$

        Composite tree = createTree(container);

        startingWith(editArea).thenBelow(tree).width(420).height(200);

        return container;
    }

    private Composite createTree(Composite container)
    {
        Composite area = new Composite(container, SWT.NONE);
        TreeColumnLayout layout = new TreeColumnLayout();
        area.setLayout(layout);

        TreeViewer nodeViewer = new TreeViewer(area, SWT.BORDER);

        ShowHideColumnHelper support = new ShowHideColumnHelper(getClass().getSimpleName(), preferences, nodeViewer,
                        layout);

        Column column = new Column("label", Messages.ColumnExchangeRate, SWT.NONE, 300); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                ExchangeRateTimeSeries series = (ExchangeRateTimeSeries) element;
                Optional<ExchangeRateProvider> provider = series.getProvider();
                return provider.isPresent() ? series.getLabel() + " " + provider.get().getName() : series.getLabel(); //$NON-NLS-1$
            }
        });
        support.addColumn(column);

        column = new Column("value", Messages.ColumnValue, SWT.RIGHT, 100); //$NON-NLS-1$
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                ExchangeRateTimeSeries series = (ExchangeRateTimeSeries) element;
                return Values.ExchangeRate.format(series.lookupRate(model.getDate())
                                .orElse(new ExchangeRate(model.getDate(), BigDecimal.ONE)).getValue());
            }
        });
        support.addColumn(column);

        support.createColumns();

        nodeViewer.getTree().setHeaderVisible(true);
        nodeViewer.getTree().setLinesVisible(true);

        nodeViewer.setContentProvider(new ExchangeRateTimeSeriesContentProvider());
        nodeViewer.setInput(model);

        model.addPropertyChangeListener(p -> nodeViewer.refresh(true));

        return area;
    }

}
