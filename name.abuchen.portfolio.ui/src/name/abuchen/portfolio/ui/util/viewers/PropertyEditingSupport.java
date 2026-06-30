package name.abuchen.portfolio.ui.util.viewers;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.text.MessageFormat;
import java.util.function.Predicate;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerInlineEditingField;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerInlineEditingPolicy;
import name.abuchen.portfolio.ui.Messages;

public abstract class PropertyEditingSupport extends ColumnEditingSupport
{
    private Class<?> subjectType;
    private PropertyDescriptor descriptor;
    private Predicate<Object> canEditCheck;
    private LedgerInlineEditingField ledgerInlineEditingField;

    public PropertyEditingSupport(Class<?> subjectType, String attributeName)
    {
        this.subjectType = subjectType;
        this.descriptor = descriptorFor(subjectType, attributeName);
        this.ledgerInlineEditingField = ledgerInlineEditingField(attributeName);
    }

    public PropertyEditingSupport setCanEditCheck(Predicate<Object> canEditCheck)
    {
        this.canEditCheck = canEditCheck;
        return this;
    }

    protected PropertyDescriptor descriptor()
    {
        return descriptor;
    }

    @Override
    public boolean canEdit(Object element)
    {
        return !isLedgerNativeTargetedProjection(element) && adapt(element) != null
                        && canEditLedgerInlineField(element)
                        && (canEditCheck == null || canEditCheck.test(element));
    }

    protected final void checkLedgerInlineField(Object element)
    {
        if (!canEditLedgerInlineField(element))
            throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_UI_011
                            .message(MessageFormat.format(Messages.LedgerPropertyEditingSupportUnsupportedInlineEdit,
                                            ledgerInlineEditingField)));
    }

    private boolean canEditLedgerInlineField(Object element)
    {
        return ledgerInlineEditingField == null || LedgerInlineEditingPolicy.isEditable(element, ledgerInlineEditingField);
    }

    protected Object adapt(Object element)
    {
        return Adaptor.adapt(subjectType, element);
    }

    private LedgerInlineEditingField ledgerInlineEditingField(String attributeName)
    {
        return switch (attributeName)
        {
            case "dateTime" -> LedgerInlineEditingField.DATE; //$NON-NLS-1$
            case "note" -> LedgerInlineEditingField.NOTE; //$NON-NLS-1$
            case "source" -> LedgerInlineEditingField.SOURCE; //$NON-NLS-1$
            case "shares" -> LedgerInlineEditingField.SHARES; //$NON-NLS-1$
            default -> null;
        };
    }

    protected PropertyDescriptor descriptorFor(Class<?> subjectType, String attributeName)
    {
        try
        {
            PropertyDescriptor[] properties = Introspector.getBeanInfo(subjectType).getPropertyDescriptors();
            for (PropertyDescriptor p : properties)
                if (attributeName.equals(p.getName()))
                    return p;
            throw new IllegalArgumentException(String.format("%s has no property named %s", subjectType //$NON-NLS-1$
                            .getName(), attributeName));
        }
        catch (IntrospectionException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

}
