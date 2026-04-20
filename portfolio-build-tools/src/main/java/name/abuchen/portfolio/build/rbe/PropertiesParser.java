package name.abuchen.portfolio.build.rbe;

import java.util.regex.Pattern;

public final class PropertiesParser
{
    private static final String LINE_SEPARATOR = "\n"; //$NON-NLS-1$
    private static final String KEY_VALUE_SEPARATORS = "=:"; //$NON-NLS-1$

    private static final Pattern PATTERN_LINE_BREAK = Pattern.compile("\r\n|\r|\n"); //$NON-NLS-1$
    private static final Pattern PATTERN_IS_REGULAR_LINE = Pattern.compile("^[^#!].*"); //$NON-NLS-1$
    private static final Pattern PATTERN_IS_COMMENTED_LINE = Pattern.compile("^##[^#].*"); //$NON-NLS-1$
    private static final Pattern PATTERN_LEADING_SPACE = Pattern.compile("^\\s*"); //$NON-NLS-1$
    private static final Pattern PATTERN_COMMENT_START = Pattern.compile("^##"); //$NON-NLS-1$
    private static final Pattern PATTERN_BACKSLASH_R = Pattern.compile("\\\\r"); //$NON-NLS-1$
    private static final Pattern PATTERN_BACKSLASH_N = Pattern.compile("\\\\n"); //$NON-NLS-1$

    private PropertiesParser()
    {}

    public static Bundle parse(String properties)
    {
        Bundle bundle = new Bundle();
        String[] lines = PATTERN_LINE_BREAK.split(properties);

        boolean doneWithFileComment = false;
        StringBuilder fileComment = new StringBuilder();
        StringBuilder lineComment = new StringBuilder();
        StringBuilder lineBuffer = new StringBuilder();

        for (int index = 0; index < lines.length; index++)
        {
            String line = lines[index];
            lineBuffer.setLength(0);
            lineBuffer.append(line);

            int equalPosition = findKeyValueSeparator(line);
            boolean isRegularLine = PATTERN_IS_REGULAR_LINE.matcher(line).matches();
            boolean isCommentedLine = doneWithFileComment && PATTERN_IS_COMMENTED_LINE.matcher(line).matches();

            if (equalPosition >= 1 && (isRegularLine || isCommentedLine))
            {
                doneWithFileComment = true;
                String comment = ""; //$NON-NLS-1$

                if (lineComment.length() > 0)
                {
                    comment = lineComment.toString();
                    lineComment.setLength(0);
                }

                if (isCommentedLine)
                {
                    lineBuffer.delete(0, 2);
                    equalPosition -= 2;
                }

                while (lineBuffer.lastIndexOf("\\") == lineBuffer.length() - 1) //$NON-NLS-1$
                {
                    int lineBreakPosition = lineBuffer.lastIndexOf("\\"); //$NON-NLS-1$
                    lineBuffer.replace(lineBreakPosition, lineBreakPosition + 1, ""); //$NON-NLS-1$

                    if (++index < lines.length)
                    {
                        String wrappedLine = PATTERN_LEADING_SPACE.matcher(lines[index]).replaceFirst(""); //$NON-NLS-1$
                        if (isCommentedLine)
                            lineBuffer.append(PATTERN_COMMENT_START.matcher(wrappedLine).replaceFirst("")); //$NON-NLS-1$
                        else
                            lineBuffer.append(wrappedLine);
                    }
                }

                String key = unescapeKey(lineBuffer.substring(0, equalPosition).trim());
                String value = PATTERN_LEADING_SPACE.matcher(lineBuffer.substring(equalPosition + 1)).replaceFirst(""); //$NON-NLS-1$

                if (value.startsWith("\\ ")) //$NON-NLS-1$
                    value = value.substring(1);

                if (Preferences.getConvertEncodedToUnicode())
                {
                    key = convertEncodedToUnicode(key);
                    value = convertEncodedToUnicode(value);
                }
                else
                {
                    value = PATTERN_BACKSLASH_R.matcher(value).replaceAll("\r"); //$NON-NLS-1$
                    value = PATTERN_BACKSLASH_N.matcher(value).replaceAll("\n"); //$NON-NLS-1$
                }

                bundle.addEntry(new BundleEntry(key, value, comment, isCommentedLine));
            }
            else if (lineBuffer.length() > 0 && (lineBuffer.charAt(0) == '#' || lineBuffer.charAt(0) == '!'))
            {
                if (!doneWithFileComment)
                {
                    fileComment.append(lineBuffer).append(LINE_SEPARATOR);
                }
                else
                {
                    lineComment.append(lineBuffer).append(LINE_SEPARATOR);
                }
            }
            else
            {
                doneWithFileComment = true;
            }
        }

        bundle.setComment(fileComment.toString());
        return bundle;
    }

    public static String convertEncodedToUnicode(String input)
    {
        char currentChar;
        int length = input.length();
        StringBuilder output = new StringBuilder(length);

        for (int index = 0; index < length;)
        {
            currentChar = input.charAt(index++);

            if (currentChar == '\\' && index + 1 <= length)
            {
                currentChar = input.charAt(index++);

                if (currentChar == 'u' && index + 4 <= length)
                {
                    int value = 0;
                    for (int ii = 0; ii < 4; ii++)
                    {
                        currentChar = input.charAt(index++);
                        switch (currentChar)
                        {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + currentChar - '0';
                                break;
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + currentChar - 'a';
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + currentChar - 'A';
                                break;
                            default:
                                value = currentChar;
                        }
                    }
                    output.append((char) value);
                }
                else
                {
                    if (currentChar == 't')
                        currentChar = '\t';
                    else if (currentChar == 'r')
                        currentChar = '\r';
                    else if (currentChar == 'n')
                        currentChar = '\n';
                    else if (currentChar == 'f')
                        currentChar = '\f';
                    else if (currentChar == 'u')
                        output.append('\\');

                    output.append(currentChar);
                }
            }
            else
            {
                output.append(currentChar);
            }
        }

        return output.toString();
    }

    private static int findKeyValueSeparator(String input)
    {
        int length = input.length();

        for (int index = 0; index < length; index++)
        {
            char current = input.charAt(index);
            if (current == '\\')
                index++;
            else if (KEY_VALUE_SEPARATORS.indexOf(current) != -1)
                return index;
        }

        return -1;
    }

    private static String unescapeKey(String key)
    {
        int length = key.length();
        StringBuilder buffer = new StringBuilder();

        for (int index = 0; index < length; index++)
        {
            char current = key.charAt(index);
            if (current == '\\' && index + 1 < length)
            {
                buffer.append(key.charAt(++index));
            }
            else
            {
                buffer.append(current);
            }
        }

        return buffer.toString();
    }
}
