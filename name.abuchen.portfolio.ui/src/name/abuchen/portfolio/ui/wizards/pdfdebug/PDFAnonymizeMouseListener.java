package name.abuchen.portfolio.ui.wizards.pdfdebug;

import java.util.Random;

import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
class PDFAnonymizeMouseListener extends MouseAdapter
{
    private final Random random = new Random();
    private final Text widget;

    public PDFAnonymizeMouseListener(Text widget)
    {
        this.widget = widget;
    }

    @Override
    public void mouseDoubleClick(MouseEvent e)
    {
        var selectedText = widget.getSelectionText();

        // Check if selectedText is not empty and does not contain forbidden
        // characters or currency codes
        if (!selectedText.isEmpty() && !containsForbiddenCharacters(selectedText)
                        && !CurrencyUnit.containsCurrencyCode(selectedText.trim()))
        {
            // Generate a new string of random characters to replace the
            // selected text
            var replacementTextBuilder = new StringBuilder();
            for (int i = 0; i < selectedText.length(); i++)
            {
                char c = selectedText.charAt(i);
                replacementTextBuilder.append(switch (Character.valueOf(c))
                {
                    case Character ch when Character.isLetter(ch) -> generateRandomLetter();
                    case Character ch when Character.isDigit(ch) -> generateRandomNumber();
                    default -> c;
                });

            }
            var replacementText = replacementTextBuilder.toString();

            // Replace selectedText with replacementText
            int startIndex = widget.getSelection().x;
            widget.insert(replacementText);
            widget.setSelection(startIndex, startIndex + replacementText.length());
        }
    }

    private boolean containsForbiddenCharacters(String text)
    {
        // we don't want to replace special characters and an ISIN
        return text.matches(".*[\\-\\.,':\\/].*")
                        || text.matches("[A-Z]{2}[A-Z0-9]{9}[0-9]");
    }

    private char generateRandomLetter()
    {
        boolean isUpperCase = random.nextBoolean();
        int offset = isUpperCase ? 'A' : 'a';
        return (char) (random.nextInt(26) + offset);
    }

    /** Generates a random digit between 0 and 9 */
    private char generateRandomNumber()
    {
        return (char) (random.nextInt(10) + '0');
    }
}
