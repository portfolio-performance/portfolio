package name.abuchen.portfolio.ui.wizards.security;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.ValidationStatusProvider;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.ListSelectionDialog;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.wizards.security.EditSecurityModel.ClassificationLink;
import name.abuchen.portfolio.ui.wizards.security.EditSecurityModel.SortOrder;
import name.abuchen.portfolio.ui.wizards.security.EditSecurityModel.TaxonomyDesignation;
import name.abuchen.portfolio.util.TextUtil;

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
                if (classification == null)
                    continue;
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
        private final List<IObservableValue<Integer>> observables;

        private WeightsAreGreaterThan100Validator(Label label, Taxonomy taxonomy,
                        List<IObservableValue<Integer>> weightObservables)
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

            for (IObservableValue<Integer> value : observables)
                weights += value.getValue();

            if (label != null)
                label.setText(Values.Weight.format(weights) + "%"); //$NON-NLS-1$

            if (Classification.ONE_HUNDRED_PERCENT >= weights)
                return ValidationStatus.ok();
            else
                return ValidationStatus.error(MessageFormat.format(Messages.EditWizardMasterDataMsgWeightNot100Percent,
                                taxonomy.getName(), Values.Weight.format(weights)));
        }
    }

    private static final class NotNullValidator implements IValidator<Object>
    {
        @Override
        public IStatus validate(Object value)
        {
            return value != null ? ValidationStatus.ok()
                            : ValidationStatus.error(Messages.EditWizardMasterDataMsgClassificationMissing);
        }
    }

    private static final class GreaterThanZeroValidator implements IValidator<Integer>
    {
        @Override
        public IStatus validate(Integer value)
        {
            int weight = value;
            return weight > 0 ? ValidationStatus.ok()
                            : ValidationStatus.error(Messages.EditWizardMasterDataMsgWeightEqualsZero);
        }
    }

    public static final String PAGE_NAME = "taxonomies"; //$NON-NLS-1$
    private final EditSecurityModel model;
    private final BindingHelper bindings;
    private ScrolledComposite scrolledComposite;
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
        scrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        setControl(scrolledComposite);
        Composite container = new Composite(scrolledComposite, SWT.NULL);
        GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(container);

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
        Composite heading = new Composite(taxonomyPicker, SWT.NONE);
        boolean isFirst = designation.equals(model.getDesignations().get(0));
        GridDataFactory.fillDefaults().indent(0, isFirst ? 0 : 20).applyTo(heading);

        GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(0, 0, 0, 5).applyTo(heading);

        Label label = new Label(heading, SWT.NONE);
        label.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING1);
        label.setText(designation.getTaxonomy().getName());
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

        if (designation.getLinks().size() > 1)
        {
            Composite sortBlock = new Composite(heading, SWT.NONE);
            GridDataFactory.fillDefaults().align(SWT.END, SWT.END).grab(true, true).applyTo(sortBlock);

            sortBlock.setLayout(new RowLayout());

            Link option = new Link(sortBlock, SWT.UNDERLINE_LINK);
            option.setText(designation.getSortOrder() != SortOrder.NATURAL ? "<a>" + Messages.ColumnTaxonomy + "</a>" //$NON-NLS-1$ //$NON-NLS-2$
                            : "\u25B2 " + Messages.ColumnTaxonomy); //$NON-NLS-1$
            option.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
                designation.setSortOrder(SortOrder.NATURAL);
                recreateTaxonomyPicker(taxonomyPicker);
            }));

            option = new Link(sortBlock, SWT.UNDERLINE_LINK);
            option.setText(designation.getSortOrder() != SortOrder.NAME ? "<a>" + Messages.ColumnName + "</a>" //$NON-NLS-1$ //$NON-NLS-2$
                            : "\u25B2 " + Messages.ColumnName); //$NON-NLS-1$
            option.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
                designation.setSortOrder(SortOrder.NAME);
                recreateTaxonomyPicker(taxonomyPicker);
            }));

            option = new Link(sortBlock, SWT.UNDERLINE_LINK);
            option.setText(designation.getSortOrder() != SortOrder.WEIGHT ? "<a>" + Messages.ColumnWeight + "</a>" //$NON-NLS-1$ //$NON-NLS-2$
                            : "\u25BC " + Messages.ColumnWeight); //$NON-NLS-1$
            option.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
                designation.setSortOrder(SortOrder.WEIGHT);
                recreateTaxonomyPicker(taxonomyPicker);
            }));
        }

        // drop-down selection block
        addBlock(taxonomyPicker, designation);

        // add button
        Link link = new Link(taxonomyPicker, SWT.UNDERLINE_LINK);
        link.setText(Messages.EditWizardMasterDataLinkNewCategory);
        GridDataFactory.fillDefaults().indent(0, 5).align(SWT.BEGINNING, SWT.CENTER).applyTo(link);

        link.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                Classification selected = selectClassification(taxonomyPicker.getShell(), designation, null);
                if (selected == null)
                    return;

                ClassificationLink link = new ClassificationLink();
                link.setClassification(selected);
                link.setWeight(designation.getLinks().isEmpty() ? Classification.ONE_HUNDRED_PERCENT : 0);
                designation.getLinks().add(link);

                recreateTaxonomyPicker(taxonomyPicker);
            }
        });
    }

    private void addBlock(final Composite taxonomyPicker, final TaxonomyDesignation designation)
    {
        Label sumOfWeights = null;
        final List<IObservableValue<Integer>> weightObservables = new ArrayList<>();
        final List<IObservableValue<?>> classificationObservables = new ArrayList<>();

        switch (designation.getSortOrder())
        {
            case NATURAL:
                designation.getLinks().sort((r, l) -> {
                    var right = r.getClassification();
                    var left = l.getClassification();

                    if (right == null && left == null)
                        return 0;
                    if (right == null)
                        return 1;
                    if (left == null)
                        return -1;

                    return designation.indexOf(right) - designation.indexOf(left);
                });
                break;
            case NAME:
                designation.getLinks().sort((r, l) -> {
                    var right = r.getClassification();
                    var left = l.getClassification();

                    if (right == null && left == null)
                        return 0;
                    if (right == null)
                        return 1;
                    if (left == null)
                        return -1;

                    return TextUtil.compare(right.getName(), left.getName());
                });
                break;
            case WEIGHT:
                designation.getLinks().sort((firstLink, secondLink) -> Integer.compare(secondLink.getWeight(),
                                firstLink.getWeight()));
                break;
            default:
        }

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
        GridDataFactory.fillDefaults().applyTo(block);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(block);

        addDropDown(block, designation, link, classificationObservables);
        addDeleteButton(block, designation, link);
    }

    private void addFullBlock(Composite picker, TaxonomyDesignation designation, final ClassificationLink link,
                    List<IObservableValue<Integer>> weightObservables,
                    List<IObservableValue<?>> classificationObservables)
    {
        Composite block = new Composite(picker, SWT.NONE);
        block.setData(link);
        GridDataFactory.fillDefaults().applyTo(block);
        GridLayoutFactory.fillDefaults().numColumns(3).applyTo(block);

        addSpinner(block, link, weightObservables);
        addDropDown(block, designation, link, classificationObservables);
        addDeleteButton(block, designation, link);
    }

    private void recreateTaxonomyPicker(final Composite taxonomyPicker)
    {
        // bindings must be removed explicitly otherwise the model keeps 'old'
        // invalid bindings and error messages. They must also be disposed as
        // their targets are not widget observables that go with the widgets.
        for (ValidationStatusProvider validator : validators)
        {
            if (validator instanceof Binding binding)
                bindings.getBindingContext().removeBinding(binding);
            else
                bindings.getBindingContext().removeValidationStatusProvider(validator);
            validator.dispose();
        }
        validators.clear();

        Composite parent = taxonomyPicker.getParent();
        taxonomyPicker.dispose();
        createTaxonomyPicker(parent);
        parent.layout();
    }

    private void setupWeightMultiValidator(Label sumOfWeights, TaxonomyDesignation designation,
                    final List<IObservableValue<Integer>> weightObservables)
    {
        MultiValidator multiValidator = new WeightsAreGreaterThan100Validator(sumOfWeights, designation.getTaxonomy(),
                        weightObservables);

        bindings.getBindingContext().addValidationStatusProvider(multiValidator);
        validators.add(multiValidator);

        for (int ii = 0; ii < weightObservables.size(); ii++)
        {
            IObservableValue<Integer> observable = weightObservables.get(ii);
            ClassificationLink link = designation.getLinks().get(ii);

            UpdateValueStrategy<Integer, Integer> strategy = new UpdateValueStrategy<>();
            strategy.setAfterConvertValidator(new GreaterThanZeroValidator());

            IObservableValue<Integer> targetObservable = multiValidator.observeValidatedValue(observable);
            IObservableValue<Integer> weightObservable = BeanProperties.value("weight", Integer.class).observe(link); //$NON-NLS-1$
            validators.add(bindings.getBindingContext().bindValue(targetObservable, weightObservable, strategy, null));
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

            UpdateValueStrategy<Object, Object> strategy = new UpdateValueStrategy<>();
            strategy.setAfterConvertValidator(new NotNullValidator());

            IObservableValue<Object> classificationObservable = BeanProperties.value("classification").observe(link); //$NON-NLS-1$
            validators.add(bindings.getBindingContext().bindValue(multiValidator.observeValidatedValue(observable),
                            classificationObservable, strategy, null));
        }
    }

    private void addDropDown(Composite block, TaxonomyDesignation designation, final ClassificationLink link,
                    List<IObservableValue<?>> classificationObservables)
    {
        Composite picker = new Composite(block, SWT.NONE);
        picker.setBackground(block.getBackground());
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(picker);
        GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).applyTo(picker);

        Label selectedClassification = new Label(picker, SWT.NONE);
        selectedClassification.setBackground(picker.getBackground());
        GridDataFactory.fillDefaults().hint(350, SWT.DEFAULT).align(SWT.FILL, SWT.CENTER)
                        .applyTo(selectedClassification);

        ImageHyperlink select = new ImageHyperlink(picker, SWT.NONE);
        select.setImage(Images.EDIT.image());
        select.setToolTipText(Messages.EditWizardMasterDataSelectClassification);
        select.setBackground(picker.getBackground());
        GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(select);

        var observable = new WritableValue<>(link.getClassification(), Classification.class);
        picker.addDisposeListener(e -> observable.dispose());
        updateLabel(selectedClassification, observable.getValue(), designation);
        observable.addValueChangeListener(e -> updateLabel(selectedClassification, e.diff.getNewValue(), designation));

        select.addHyperlinkListener(IHyperlinkListener.linkActivatedAdapter(e -> {
            Classification selected = selectClassification(block.getShell(), designation, observable.getValue());
            if (selected != null)
                observable.setValue(selected);
        }));

        classificationObservables.add(observable);
    }

    private Classification selectClassification(Shell shell, TaxonomyDesignation designation, Classification current)
    {
        ListSelectionDialog dialog = new ListSelectionDialog(shell, new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return designation.getPathName((Classification) element);
            }
        });

        dialog.setTitle(designation.getTaxonomy().getName());
        dialog.setMultiSelection(false);
        dialog.setElements(designation.getElements());
        dialog.setInitialSelection(current);
        dialog.setDisabledElements(designation.getLinks().stream() //
                        .map(ClassificationLink::getClassification) //
                        .filter(c -> c != null && c != current) //
                        .toList());
        dialog.setViewerComparator(new ViewerComparator()
        {
            @Override
            public int compare(Viewer viewer, Object e1, Object e2)
            {
                if (designation.getSortOrder() == SortOrder.NAME)
                    return TextUtil.compare(((Classification) e1).getName(), ((Classification) e2).getName());

                return Integer.compare(designation.indexOf((Classification) e1),
                                designation.indexOf((Classification) e2));
            }
        });

        if (dialog.open() != Window.OK)
            return null;

        Object[] result = dialog.getResult();
        return result.length > 0 ? (Classification) result[0] : null;
    }

    private void updateLabel(Label label, Classification classification, TaxonomyDesignation designation)
    {
        if (classification == null)
        {
            label.setText(""); //$NON-NLS-1$
            label.setToolTipText(null);
            return;
        }

        label.setText(designation.getPathName(classification));
        label.setToolTipText(TextUtil.tooltip(classification.getPathName(false)));
    }

    private void addDeleteButton(final Composite block, final TaxonomyDesignation designation,
                    final ClassificationLink link)
    {
        final ImageHyperlink deleteButton = new ImageHyperlink(block, SWT.NONE);
        deleteButton.setImage(Images.TRASH.image());
        deleteButton.setToolTipText(Messages.MenuTaxonomyAssignmentRemove);
        deleteButton.setBackground(block.getBackground());
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(deleteButton);
        deleteButton.addHyperlinkListener(IHyperlinkListener.linkActivatedAdapter(e -> {
            designation.getLinks().remove(link);
            recreateTaxonomyPicker(block.getParent());
        }));
    }

    private void addSpinner(Composite block, ClassificationLink link, List<IObservableValue<Integer>> observables)
    {
        final Spinner spinner = new Spinner(block, SWT.BORDER);
        spinner.setDigits(2);
        spinner.setMinimum(0);
        spinner.setValues(link.getWeight(), 0, Classification.ONE_HUNDRED_PERCENT, 2, 100, 1000);
        GridDataFactory.fillDefaults().applyTo(spinner);

        IObservableValue<Integer> observable = WidgetProperties.spinnerSelection().observe(spinner);
        observables.add(observable);
    }

    private void createTaxonomyPicker(Composite container)
    {
        final Composite taxonomyPicker = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(taxonomyPicker);
        GridLayoutFactory.fillDefaults().margins(0, 0).spacing(0, 0).applyTo(taxonomyPicker);

        for (TaxonomyDesignation designation : model.getDesignations())
            createTaxonomySection(taxonomyPicker, designation);

        scrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

}
