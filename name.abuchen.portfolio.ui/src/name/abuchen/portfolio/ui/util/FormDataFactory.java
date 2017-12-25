package name.abuchen.portfolio.ui.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public final class FormDataFactory
{
    public static FormDataFactory startingWith(Control reference, Label label)
    {
        FormDataFactory factory = new FormDataFactory(reference);

        factory.label(label);
        FormData data = factory.from(reference);
        data.left = new FormAttachment(label, 5);

        return factory;
    }

    public static FormDataFactory startingWith(Control reference)
    {
        return new FormDataFactory(reference);
    }

    private Control reference;

    private FormDataFactory(Control reference)
    {
        this.reference = reference;
    }

    public FormDataFactory thenBelow(Control control)
    {
        return thenBelow(control, 5);
    }

    public FormDataFactory thenBelow(Control control, int offset)
    {
        FormData data = from(control);
        data.top = new FormAttachment(reference, offset);
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

    public FormDataFactory thenLeft(Control control)
    {
        FormData data = from(control);
        data.top = new FormAttachment(reference, 0, SWT.CENTER);
        data.right = new FormAttachment(reference, -5);
        return new FormDataFactory(control);
    }

    public FormDataFactory label(Label label)
    {
        FormData data = from(label);
        data.top = new FormAttachment(reference, 0, SWT.CENTER);
        data.right = new FormAttachment(reference, -5);
        return this;
    }

    public FormDataFactory suffix(Label label)
    {
        return suffix(label, SWT.DEFAULT);
    }

    public FormDataFactory suffix(Label label, int width)
    {
        FormData data = from(label);
        data.top = new FormAttachment(reference, 0, SWT.CENTER);
        data.left = new FormAttachment(reference, 5);
        data.right = new FormAttachment(100);
        data.width = width;
        return this;
    }

    public FormDataFactory suffix(Control suffix)
    {
        FormData data = from(suffix);
        data.top = new FormAttachment(reference, 0, SWT.CENTER);
        data.left = new FormAttachment(reference, 5);
        return this;
    }

    public FormDataFactory width(int width)
    {
        FormData data = from(reference);
        data.width = width;
        return this;
    }

    public FormDataFactory height(int height)
    {
        FormData data = from(reference);
        data.height = height;
        return this;
    }

    public FormDataFactory top(FormAttachment attachment)
    {
        FormData data = from(reference);
        data.top = attachment;
        return this;
    }

    public FormDataFactory left(Control control)
    {
        FormData data = from(reference);
        data.left = new FormAttachment(control, 0, SWT.LEFT);
        return this;
    }

    public FormDataFactory left(FormAttachment attachment)
    {
        FormData data = from(reference);
        data.left = attachment;
        return this;
    }

    public FormDataFactory right(Control control)
    {
        FormData data = from(reference);
        data.right = new FormAttachment(control, 0, SWT.RIGHT);
        return this;
    }

    public FormDataFactory right(FormAttachment attachment)
    {
        FormData data = from(reference);
        data.right = attachment;
        return this;
    }

    public FormDataFactory bottom(FormAttachment attachment)
    {
        FormData data = from(reference);
        data.bottom = attachment;
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
