package name.abuchen.portfolio.ui.wizards.security;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.AttributeType.ImageConverter;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.util.IValidatingConverter;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.wizards.security.EditSecurityModel.AttributeDesignation;
import name.abuchen.portfolio.util.ImageUtil;

public class AttributesPage extends AbstractPage implements IMenuListener
{
    private static final class ToAttributeStringConverter implements IConverter<Object, String>
    {
        private final AttributeDesignation attribute;

        private ToAttributeStringConverter(AttributeDesignation attribute)
        {
            this.attribute = attribute;
        }

        @Override
        public Object getToType()
        {
            return String.class;
        }

        @Override
        public Object getFromType()
        {
            return Object.class;
        }

        @Override
        public String convert(Object fromObject)
        {
            return attribute.getType().getConverter().toString(fromObject);
        }
    }

    private static final class ToAttributeObjectConverter implements IValidatingConverter<String, Object>
    {
        private final AttributeDesignation attribute;

        private ToAttributeObjectConverter(AttributeDesignation attribute)
        {
            this.attribute = attribute;
        }

        @Override
        public Object getToType()
        {
            return Object.class;
        }

        @Override
        public Object getFromType()
        {
            return String.class;
        }

        @Override
        public Object convert(String fromObject)
        {
            return attribute.getType().getConverter().fromString(fromObject);
        }
    }

    private final EditSecurityModel model;
    private final BindingHelper bindings;

    private Composite attributeContainer;
    private Menu menu;

    public AttributesPage(EditSecurityModel model, BindingHelper bindings)
    {
        this.model = model;
        this.bindings = bindings;
        setTitle(Messages.EditWizardAttributesTitle);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = new Composite(parent, SWT.NULL);
        setControl(composite);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(composite);

        attributeContainer = new Composite(composite, SWT.NULL);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(attributeContainer);
        GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5).applyTo(attributeContainer);

        for (AttributeDesignation attribute : model.getAttributes())
            addAttributeBlock(attributeContainer, attribute);

        // add button
        final Button addButton = new Button(composite, SWT.PUSH);
        addButton.setImage(Images.ADD.image());
        addButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                showAdditionalAttributes();
            }
        });

        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(addButton);

        parent.addDisposeListener(e -> {
            if (menu != null && !menu.isDisposed())
                menu.dispose();
        });
    }

    private void addAttributeBlock(Composite container, final AttributeDesignation attribute)
    {
        // label
        final Label label = new Label(container, SWT.NONE);
        label.setText(attribute.getType().getName());

        final Control value;
        final Binding binding;

        // input
        if (attribute.getType().getType() == Boolean.class)
        {
            value = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(value);

            IObservableValue<Boolean> attributeModel = BeanProperties.value("value", Boolean.class).observe(attribute); //$NON-NLS-1$
            IObservableValue<Boolean> attributeTarget = WidgetProperties.buttonSelection().observe((Button) value);
            binding = bindings.getBindingContext().bindValue(attributeTarget, attributeModel);
        }
        else if (attribute.getType().getConverter() instanceof ImageConverter)
        {
            value = new Composite(container, SWT.PUSH);
            GridLayoutFactory.fillDefaults().numColumns(1).applyTo((Composite) value);
            final Button preview = new Button((Composite) value, SWT.PUSH);
            final String previewPlaceholderText = "..."; //$NON-NLS-1$
            preview.setText(previewPlaceholderText);

            ImageConverter conv = (ImageConverter) attribute.getType().getConverter();
            Image img = ImageUtil.toImage(conv.toString(attribute.getValue()), 16, 16);
            if (img != null)
                preview.setImage(img);

            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(value);

            ToAttributeObjectConverter input2model = new ToAttributeObjectConverter(attribute);
            IObservableValue<Object> attributeModel = BeanProperties.value("value").observe(attribute); //$NON-NLS-1$
            IObservableValue<String> attributeTarget = WidgetProperties.tooltipText().observe(preview);
            binding = bindings.getBindingContext().bindValue( //
                            attributeTarget, attributeModel,
                            new UpdateValueStrategy<String, Object>().setAfterGetValidator(input2model)
                                            .setConverter(input2model),
                            new UpdateValueStrategy<Object, String>()
                                            .setConverter(new ToAttributeStringConverter(attribute))
                                            .setBeforeSetValidator(new IValidator<Object>()
                                            {
                                                @Override
                                                public IStatus validate(Object value)
                                                {
                                                    String s = conv.toString(value);
                                                    if (s == null || s.length() == 0)
                                                    {
                                                        updatePreview(null);
                                                        return Status.OK_STATUS;
                                                    }

                                                    Image img = ImageUtil.toImage(s, 16, 16);

                                                    updatePreview(img);
                                                    return img == null ? Status.CANCEL_STATUS : Status.OK_STATUS;
                                                }

                                                private void updatePreview(Image img)
                                                {
                                                    preview.setImage(img);
                                                    if (img == null)
                                                        preview.setText(previewPlaceholderText);
                                                    else
                                                        preview.setText(""); //$NON-NLS-1$
                                                    value.getParent().getParent().layout(true);
                                                }
                                            }));

            preview.addMouseListener(MouseListener.mouseDownAdapter(e -> {
                FileDialog dial = new FileDialog(container.getShell());
                String filename = dial.open();
                if (filename != null)
                {
                    try
                    {
                        String b64 = ImageUtil.loadAndPrepare(filename, ImageConverter.MAXIMUM_SIZE_EMBEDDED_IMAGE,
                                        ImageConverter.MAXIMUM_SIZE_EMBEDDED_IMAGE);
                        if (b64 == null)
                            MessageDialog.openError(getShell(), Messages.MsgInvalidImage,
                                            MessageFormat.format(Messages.MsgInvalidImageDetail, filename));
                        else
                            attributeModel.setValue(b64);
                    }
                    catch (IOException ex)
                    {
                        PortfolioPlugin.log(ex);
                    }
                }
            }));
        }
        else
        {
            value = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(value);

            ToAttributeObjectConverter input2model = new ToAttributeObjectConverter(attribute);
            IObservableValue<Object> attributeModel = BeanProperties.value("value").observe(attribute); //$NON-NLS-1$
            IObservableValue<String> attributeTarget = WidgetProperties.text(SWT.Modify).observe(value);
            binding = bindings.getBindingContext().bindValue( //
                            attributeTarget, attributeModel,
                            new UpdateValueStrategy<String, Object>().setAfterGetValidator(input2model)
                                            .setConverter(input2model),
                            new UpdateValueStrategy<Object, String>()
                                            .setConverter(new ToAttributeStringConverter(attribute)));
        }

        // delete button
        final Button deleteButton = new Button(container, SWT.PUSH);
        deleteButton.setImage(Images.REMOVE.image());

        // delete selection listener
        deleteButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                model.getAttributes().remove(attribute);
                bindings.getBindingContext().removeBinding(binding);

                Composite parent = deleteButton.getParent();
                label.dispose();
                value.dispose();
                deleteButton.dispose();
                parent.getParent().layout(true);
            }
        });
    }

    protected void showAdditionalAttributes()
    {
        if (menu == null)
        {
            MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
            menuMgr.setRemoveAllWhenShown(true);
            menuMgr.addMenuListener(this);

            menu = menuMgr.createContextMenu(getShell());
        }

        menu.setVisible(true);
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.add(new LabelOnly(Messages.LabelAvailableAttributes));

        Set<AttributeType> existing = new HashSet<>();
        for (AttributeDesignation d : model.getAttributes())
            existing.add(d.getType());

        model.getClient() //
                        .getSettings() //
                        .getAttributeTypes() //
                        .filter(a -> !existing.contains(a)) //
                        .filter(a -> a.supports(Security.class)) //
                        .forEach(attribute -> addMenu(manager, attribute));
    }

    private void addMenu(IMenuManager manager, final AttributeType attribute)
    {
        manager.add(new Action(attribute.getName())
        {
            @Override
            public void run()
            {
                AttributeDesignation a = new AttributeDesignation(attribute, null);
                model.getAttributes().add(a);
                addAttributeBlock(attributeContainer, a);
                attributeContainer.getParent().layout(true);
            }
        });
    }
}
