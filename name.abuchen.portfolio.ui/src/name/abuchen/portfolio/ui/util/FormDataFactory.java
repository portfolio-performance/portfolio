package name.abuchen.portfolio.ui.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class FormDataFactory
{
    public static FormDataFactory startingWith(Control reference, Label label)
    {
        FormDataFactory factory = new FormDataFactory(reference);

        factory.withLabel(label);
        FormData data = factory.from(reference);
        data.left = new FormAttachment(label, 5);

        return factory;
    }

    public static FormDataFactory startingWith(Control reference)
    {
        return new FormDataFactory(reference);
    }

    private Control reference;

    public FormDataFactory(Control reference)
    {
        this.reference = reference;
    }

    public FormDataFactory thenBelow(Control control)
    {
        FormData data = from(control);
        data.top = new FormAttachment(reference, 5);
        data.left = new FormAttachment(reference, 0, SWT.LEFT);
        return new FormDataFactory(control);
    }

    public FormDataFactory thenRight(Control control)
    {
        FormData data = from(control);
        data.top = new FormAttachment(reference, 0, SWT.CENTER);
        data.left = new FormAttachment(reference, 5);
        return new FormDataFactory(control);
    }

    public FormDataFactory withLabel(Label label)
    {
        FormData data = from(label);
        data.top = new FormAttachment(reference, 0, SWT.CENTER);
        data.right = new FormAttachment(reference, -5);

        return this;
    }

    public FormDataFactory withSuffix(Label label)
    {
        FormData data = from(label);
        data.top = new FormAttachment(reference, 0, SWT.CENTER);
        data.left = new FormAttachment(reference, 5);
        data.right = new FormAttachment(100);
        return this;
    }

    public FormDataFactory width(int width)
    {
        FormData data = from(reference);
        data.width = width;
        return this;
    }

    private FormData from(Control control)
    {
        FormData layoutData = (FormData) control.getLayoutData();
        if (layoutData == null)
        {
            layoutData = new FormData();
            control.setLayoutData(layoutData);
        }

        return layoutData;
    }
}
