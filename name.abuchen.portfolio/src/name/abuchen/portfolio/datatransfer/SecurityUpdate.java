package name.abuchen.portfolio.datatransfer;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Security;

public sealed interface SecurityUpdate permits SecurityUpdate.AttributeUpdate, SecurityUpdate.NoteUpdate
{
    String getLabel();

    void applyTo(Security security);

    record AttributeUpdate(AttributeType type, Object value) implements SecurityUpdate
    {
        @Override
        public String getLabel()
        {
            // name is nullable (AttributeType.java:499); fall back to the id so
            // SecurityUpdateItem#getTypeInformation's Collectors.joining, which
            // NPEs on a null element, is always fed a non-null label. Mirrors
            // the name-else-id first choice in SecurityAttributeColumns.columnNames
            // (kept local to avoid a datatransfer -> datatransfer.csv package cycle)
            return type.getName() != null && !type.getName().isBlank() ? type.getName() : type.getId();
        }

        @Override
        public void applyTo(Security security)
        {
            security.getAttributes().put(type, value);
        }
    }

    record NoteUpdate(String note) implements SecurityUpdate
    {
        @Override
        public String getLabel()
        {
            return Messages.CSVColumn_Note;
        }

        @Override
        public void applyTo(Security security)
        {
            security.setNote(note);
        }
    }
}
