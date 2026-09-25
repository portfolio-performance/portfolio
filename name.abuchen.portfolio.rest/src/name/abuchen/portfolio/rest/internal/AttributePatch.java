package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.AttributeFieldType;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Attributes;
import name.abuchen.portfolio.model.Client;

/**
 * The nested {@code attributes} merge patch of an entity with custom
 * attributes (instrument, cash account, investment account): a present key
 * sets the attribute, an explicit null clears it, an absent key is left
 * untouched. Validation is separated from application so that a handler can
 * collect every violation before it touches the model.
 */
/* package */ final class AttributePatch
{
    /** one validated attribute change */
    /* package */ record Assignment(AttributeType type, boolean clear, Object value)
    {
    }

    private AttributePatch()
    {
    }

    /**
     * Validates the nested patch against the attribute types of the client
     * that apply to {@code target}; every violation is added to
     * {@code errors}. {@code targetLabel} names the entity kind in messages,
     * e.g. "instruments".
     */
    /* package */ static List<Assignment> stage(Client client, Class<? extends Attributable> target,
                    String targetLabel, Attributes current, JsonElement element, List<ApiException.FieldError> errors)
    {
        var assignments = new ArrayList<Assignment>();

        if (!element.isJsonObject())
        {
            errors.add(new ApiException.FieldError("attributes", "invalid-type", "attributes must be a JSON object")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return assignments;
        }

        for (var entry : element.getAsJsonObject().entrySet())
        {
            var id = entry.getKey();
            var field = "attributes." + id; //$NON-NLS-1$

            var type = client.getSettings().getAttributeTypes().filter(a -> id.equals(a.getId())).findAny()
                            .orElse(null);
            if (type == null)
            {
                errors.add(new ApiException.FieldError(field, "unknown-attribute", "no such attribute")); //$NON-NLS-1$ //$NON-NLS-2$
                continue;
            }
            if (!type.supports(target))
            {
                errors.add(new ApiException.FieldError(field, "attribute-not-applicable", //$NON-NLS-1$
                                "attribute does not apply to " + targetLabel)); //$NON-NLS-1$
                continue;
            }

            var fieldType = AttributeFieldType.of(type);
            if (fieldType == null || !AttributeCodec.isSupported(fieldType))
            {
                errors.add(new ApiException.FieldError(field, "unsupported-attribute-type", //$NON-NLS-1$
                                "attribute type is not supported by the API")); //$NON-NLS-1$
                continue;
            }

            var value = entry.getValue();
            if (value.isJsonNull())
            {
                if (current.exists(type))
                    assignments.add(new Assignment(type, true, null));
                continue;
            }

            try
            {
                assignments.add(new Assignment(type, false, AttributeCodec.decode(fieldType, value)));
            }
            catch (AttributeCodec.InvalidValueException e)
            {
                errors.add(new ApiException.FieldError(field, "invalid-value", e.getMessage())); //$NON-NLS-1$
            }
        }

        return assignments;
    }

    /**
     * Applies validated assignments and answers the changes, attributed by
     * the attribute's name and rendered the way the desktop shows the value.
     */
    /* package */ static List<ChangeLog.Change> apply(Attributes attributes, List<Assignment> assignments)
    {
        var changes = new ArrayList<ChangeLog.Change>();

        for (var assignment : assignments)
        {
            var type = assignment.type();
            var before = attributes.get(type);

            if (assignment.clear())
            {
                attributes.remove(type);
                if (before != null)
                    changes.add(new ChangeLog.Change(type.getName(), render(type, before), null));
            }
            else
            {
                var after = assignment.value();
                attributes.put(type, after);
                if (!Objects.equals(before, after))
                    changes.add(new ChangeLog.Change(type.getName(), before == null ? null : render(type, before),
                                    render(type, after)));
            }
        }

        return changes;
    }

    /**
     * Renders a stored attribute value the way the desktop UI shows it.
     * Guarded against a poisoned stored value (wrong runtime type from a
     * corrupted file) so that logging can never break a write.
     */
    /* package */ static String render(AttributeType type, Object value)
    {
        try
        {
            return type.getConverter().toString(value);
        }
        catch (RuntimeException e)
        {
            return String.valueOf(value);
        }
    }
}
