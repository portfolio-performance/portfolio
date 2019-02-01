package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.databinding.conversion.IConverter;

import name.abuchen.portfolio.ui.Messages;

/* package */ final class Delimiter
{
    public static final class CharToDelimiterConverter implements IConverter<Character, Delimiter>
    {
        @Override
        public Object getToType()
        {
            return Delimiter.class;
        }

        @Override
        public Object getFromType()
        {
            return Character.class;
        }

        @Override
        public Delimiter convert(Character fromObject)
        {
            for (Delimiter delimiter : AVAILABLE)
            {
                if (fromObject.equals(delimiter.getCharacter()))
                    return delimiter;
            }
            return null;
        }
    }

    public static final class DelimiterToCharConverter implements IConverter<Delimiter, Character>
    {
        @Override
        public Object getToType()
        {
            return Character.class;
        }

        @Override
        public Object getFromType()
        {
            return Delimiter.class;
        }

        @Override
        public Character convert(Delimiter fromObject)
        {
            return fromObject.getCharacter();
        }
    }

    protected static final List<Delimiter> AVAILABLE = Arrays.asList(
                    new Delimiter(',', Messages.CSVImportSeparatorComma), //
                    new Delimiter(';', Messages.CSVImportSeparatorSemicolon), //
                    new Delimiter('\t', Messages.CSVImportSeparatorTab));

    private final char character;
    private final String label;

    Delimiter(char character, String label)
    {
        this.character = character;
        this.label = label;
    }

    public char getCharacter()
    {
        return character;
    }

    public String getLabel()
    {
        return label;
    }

    @Override
    public String toString()
    {
        return getLabel();
    }
}
