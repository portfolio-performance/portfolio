package name.abuchen.portfolio.ui.wizards.security;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.ValidationStatusProvider;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Spinner;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.wizards.security.EditSecurityModel.ClassificationLink;
import name.abuchen.portfolio.ui.wizards.security.EditSecurityModel.TaxonomyDesignation;

public class SecurityTaxonomyPage extends AbstractPage
{

    private static final class ClassificationNotTwiceValidator extends MultiValidator
    {
        private final List<IObservableValue<?>> observables;

        private ClassificationNotTwiceValidator(List<IObservableValue<?>> observables)
        {
            this.observables = observables;
        }

        @Override
        protected IStatus validate()
        {
            if (observables.isEmpty())
                return ValidationStatus.ok();

            Set<Classification> selected = new HashSet<>();

            for (IObservableValue<?> value : observables)
            {
                Classification classification = (Classification) value.getValue();
                if (!selected.add(classification))
                    return ValidationStatus.error(MessageFormat.format(
                                    Messages.EditWizardMasterDataMsgDuplicateClassification, classification.getName()));
            }
            return ValidationStatus.ok();
        }
    }

    private static final class WeightsAreGreaterThan100Validator extends MultiValidator
    {
        private final Label label;
        private final Taxonomy taxonomy;
        private final List<IObservableValue<?>> observables;

        private WeightsAreGreaterThan100Validator(Label label, Taxonomy taxonomy,
                        List<IObservableValue<?>> weightObservables)
        {
            this.label = label;
            this.taxonomy = taxonomy;
            this.observables = weightObservables;
        }

        @Override
        protected IStatus validate()
        {
            if (observables.isEmpty())
                return ValidationStatus.ok();

            int weights = 0;

            for (IObservableValue<?> value : observables)
                weights += (Integer) value.getValue();

            if (label != null)
                label.setText(Values.Weight.format(weights) + "%"); //$NON-NLS-1$

            if (Classification.ONE_HUNDRED_PERCENT >= weights)
                return ValidationStatus.ok();
            else
                return ValidationStatus.error(MessageFormat.format(Messages.EditWizardMasterDataMsgWeightNot100Percent,
                                taxonomy.getName(), Values.Weight.format(weights)));
        }
    }

    private static final class NotNullValidator implements IValidator
    {
        @Override
        public IStatus validate(Object value)
        {
            return value != null ? ValidationStatus.ok()
                            : ValidationStatus.error(Messages.EditWizardMasterDataMsgClassificationMissing);
        }
    }

    private static final class GreaterThanZeroValidator implements IValidator
    {
        @Override
        public IStatus validate(Object value)
        {
            int weight = (Integer) value;
            return weight > 0 ? ValidationStatus.ok()
                            : ValidationStatus.error(Messages.EditWizardMasterDataMsgWeightEqualsZero);
        }
    }

    public static final String PAGE_NAME = "taxonomies"; //$NON-NLS-1$
    private final EditSecurityModel model;
    private final BindingHelper bindings;
    private ScrolledComposite scrolledComposite;
    private Font boldFont;
    private List<ValidationStatusProvider> validators = new ArrayList<>();

    public SecurityTaxonomyPage(EditSecurityModel model, BindingHelper bindings)
    {
        this.model = model;
        this.bindings = bindings;
        setTitle(Messages.LabelTaxonomies);
    }

    @Override
    public void createControl(Composite parent)
    {
        LocalResourceManager resources = new LocalResourceManager(JFaceResources.getResources(), parent);

        boldFont = resources.createFont(FontDescriptor.createFrom(parent.getFont()).setStyle(SWT.BOLD));

        scrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        setControl(scrolledComposite);
        Composite container = new Composite(scrolledComposite, SWT.NULL);
        GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(container);

        scrolledComposite.setContent(container);
        scrolledComposite.setExpandVertical(true);
        scrolledComposite.setExpandHorizontal(true);

        createTaxonomyPicker(container);

        scrolledComposite.addControlListener(new ControlAdapter()
        {
            @Override
            public void controlResized(ControlEvent e)
            {
                scrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
        });
    }

    private void createTaxonomySection(final Composite taxonomyPicker, final TaxonomyDesignation designation)
    {
        // label
        Label label = new Label(taxonomyPicker, SWT.NONE);
        label.setFont(boldFont);
        label.setText(designation.getTaxonomy().getName());

        boolean isFirst = designation.equals(model.getDesignations().get(0));

        GridDataFactory.fillDefaults().grab(true, false).span(2, 1).align(SWT.BEGINNING, SWT.CENTER)
                        .indent(0, isFirst ? 0 : 20).applyTo(label);

        // drop-down selection block
        addBlock(taxonomyPicker, designation);

        // add button
        Link link = new Link(taxonomyPicker, SWT.UNDERLINE_LINK);
        link.setText(Messages.EditWizardMasterDataLinkNewCategory);
        GridDataFactory.fillDefaults().span(2, 1).indent(0, 5).align(SWT.BEGINNING, SWT.CENTER).applyTo(link);

        link.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                ClassificationLink link = new ClassificationLink();
                link.setWeight(designation.getLinks().isEmpty() ? Classification.ONE_HUNDRED_PERCENT : 0);
                designation.getLinks().add(link);

                recreateTaxonomyPicker(taxonomyPicker);
            }
        });
    }

    private void addBlock(final Composite taxonomyPicker, final TaxonomyDesignation designation)
    {
        Label sumOfWeights = null;
        final List<IObservableValue<?>> weightObservables = new ArrayList<>();
        final List<IObservableValue<?>> classificationObservables = new ArrayList<>();

        if (designation.getLinks().size() == 1
                        && designation.getLinks().get(0).getWeight() == Classification.ONE_HUNDRED_PERCENT)
        {
            addSimpleBlock(taxonomyPicker, designation, designation.getLinks().get(0), classificationObservables);
        }
        else if (!designation.getLinks().isEmpty())
        {
            for (ClassificationLink link : designation.getLinks())
                addFullBlock(taxonomyPicker, designation, link, weightObservables, classificationObservables);

            // add summary
            sumOfWeights = new Label(taxonomyPicker, SWT.NONE);
            sumOfWeights.setText(""); //$NON-NLS-1$
            GridDataFactory.fillDefaults().span(2, 1).indent(0, 5).align(SWT.BEGINNING, SWT.CENTER)
                            .applyTo(sumOfWeights);
        }

        setupWeightMultiValidator(sumOfWeights, designation, weightObservables);
        setupClassificationMultiValidator(designation, classificationObservables);
    }

    private void addSimpleBlock(Composite picker, TaxonomyDesignation designation, final ClassificationLink link,
                    List<IObservableValue<?>> classificationObservables)
    {
        Composite block = new Composite(picker, SWT.NONE);
        block.setBackground(picker.getBackground());
        block.setData(link);
        GridDataFactory.fillDefaults().span(2, 1).applyTo(block);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(block);

        addDropDown(block, designation, classificationObservables);
        addDeleteButton(block, designation, link);
    }

    private void addFullBlock(Composite picker, TaxonomyDesignation designation, final ClassificationLink link,
                    List<IObservableValue<?>> weightObservables, List<IObservableValue<?>> classificationObservables)
    {
        Composite block = new Composite(picker, SWT.NONE);
        block.setData(link);
        GridDataFactory.fillDefaults().span(2, 1).applyTo(block);
        GridLayoutFactory.fillDefaults().numColumns(3).applyTo(block);

        addSpinner(block, link, weightObservables);
        addDropDown(block, designation, classificationObservables);
        addDeleteButton(block, designation, link);
    }

    private void recreateTaxonomyPicker(final Composite taxonomyPicker)
    {
        // bindings must be removed explicitly otherwise the model keeps 'old'
        // invalid bindings and error messages
        for (ValidationStatusProvider validator : validators)
        {
            if (validator instanceof Binding)
                bindings.getBindingContext().removeBinding((Binding) validator);
            else
                bindings.getBindingContext().removeValidationStatusProvider(validator);
        }
        validators.clear();

        Composite parent = taxonomyPicker.getParent();
        taxonomyPicker.dispose();
        createTaxonomyPicker(parent);
        parent.layout();
    }

    private void setupWeightMultiValidator(Label sumOfWeights, TaxonomyDesignation designation,
                    final List<IObservableValue<?>> weightObservables)
    {
        MultiValidator multiValidator = new WeightsAreGreaterThan100Validator(sumOfWeights, designation.getTaxonomy(),
                        weightObservables);

        bindings.getBindingContext().addValidationStatusProvider(multiValidator);
        validators.add(multiValidator);

        for (int ii = 0; ii < weightObservables.size(); ii++)
        {
            IObservableValue<?> observable = weightObservables.get(ii);
            ClassificationLink link = designation.getLinks().get(ii);

            UpdateValueStrategy strategy = new UpdateValueStrategy();
            strategy.setAfterConvertValidator(new GreaterThanZeroValidator());

            @SuppressWarnings("unchecked")
            IObservableValue<?> weightObservable = BeanProperties.value("weight").observe(link); //$NON-NLS-1$
            validators.add(bindings.getBindingContext().bindValue(multiValidator.observeValidatedValue(observable),
                            weightObservable, strategy, null));
        }
    }

    private void setupClassificationMultiValidator(TaxonomyDesignation designation,
                    final List<IObservableValue<?>> classificationObservables)
    {
        MultiValidator multiValidator = new ClassificationNotTwiceValidator(classificationObservables);

        bindings.getBindingContext().addValidationStatusProvider(multiValidator);
        validators.add(multiValidator);

        for (int ii = 0; ii < classificationObservables.size(); ii++)
        {
            IObservableValue<?> observable = classificationObservables.get(ii);
            ClassificationLink link = designation.getLinks().get(ii);

            UpdateValueStrategy strategy = new UpdateValueStrategy();
            strategy.setAfterConvertValidator(new NotNullValidator());

            @SuppressWarnings("unchecked")
            IObservableValue<?> classificationObservable = BeanProperties.value("classification").observe(link); //$NON-NLS-1$
            validators.add(bindings.getBindingContext().bindValue(multiValidator.observeValidatedValue(observable),
                            classificationObservable, strategy, null));
        }
    }

    private void addDropDown(Composite block, TaxonomyDesignation designation,
                    List<IObservableValue<?>> classificationObservables)
    {
        final ComboViewer combo = new ComboViewer(block, SWT.READ_ONLY);
        combo.setContentProvider(ArrayContentProvider.getInstance());
        combo.setLabelProvider(new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Classification) element).getPathName(false, 80);
            }
        });
        combo.setInput(designation.getElements());
        GridDataFactory.fillDefaults().grab(false, false).applyTo(combo.getControl());

        classificationObservables.add(ViewersObservables.observeSingleSelection(combo));
    }

    private void addDeleteButton(final Composite block, final TaxonomyDesignation designation,
                    final ClassificationLink link)
    {
        final Button deleteButton = new Button(block, SWT.PUSH);
        deleteButton.setImage(Images.REMOVE.image());
        deleteButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                designation.getLinks().remove(link);
                recreateTaxonomyPicker(block.getParent());
            }
        });
    }

    private void addSpinner(Composite block, ClassificationLink link, List<IObservableValue<?>> observables)
    {
        final Spinner spinner = new Spinner(block, SWT.BORDER);
        spinner.setDigits(2);
        spinner.setMinimum(0);
        spinner.setValues(link.getWeight(), 0, Classification.ONE_HUNDRED_PERCENT, 2, 100, 1000);
        GridDataFactory.fillDefaults().applyTo(spinner);

        observables.add(WidgetProperties.selection().observe(spinner));
    }

    private void createTaxonomyPicker(Composite container)
    {
        final Composite taxonomyPicker = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(2, 1).grab(true, false).applyTo(taxonomyPicker);
        GridLayoutFactory.fillDefaults().numColumns(2).margins(0, 0).spacing(0, 0).applyTo(taxonomyPicker);

        for (TaxonomyDesignation designation : model.getDesignations())
            createTaxonomySection(taxonomyPicker, designation);

        scrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

}
