package name.abuchen.portfolio.util;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class Iban
{
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"; //$NON-NLS-1$
    private static final String DEFAULT_CHECK_DIGIT = "00";
    public  static final String PATTERN = "[A-Z]{2}[0-9]{20}"; //$NON-NLS-1$
    public  static final int IBANNUMBER_MIN_SIZE = 22;
    public  static final int IBANNUMBER_MAX_SIZE = 22;
    public  static final BigInteger IBANNUMBER_MAGIC_NUMBER = new BigInteger("97");
    public  static final String IBANNUMBER_DUMMY = "NA68012345678901234567";
    public  static final String IBANNUMBER_ANY   = "NA08765432109876543210";
    
    public Iban()
    {
    }

    public static final boolean isValid(String iban) // NOSONAR
    {
        System.err.println(">>>> Iban::isValid iban: " + iban + "[" + iban.substring(2, 3) + "] => [" + calculateCheckDigit(iban) + "]");
        // taken from: https://gist.github.com/DandyDev/5394643
        String newAccountNumber = iban.trim();

        // Check that the total IBAN length is correct as per the country. If not, the IBAN is invalid. We could also check
        // for specific length according to country, but for now we won't
        if (newAccountNumber.length() < IBANNUMBER_MIN_SIZE || newAccountNumber.length() > IBANNUMBER_MAX_SIZE)
            return false;

        // Move the four initial characters to the end of the string.
        newAccountNumber = newAccountNumber.substring(4) + newAccountNumber.substring(0, 4);

        // Replace each letter in the string with two digits, thereby expanding the string, where A = 10, B = 11, ..., Z = 35.
        StringBuilder numericAccountNumber = new StringBuilder();
        int numericValue;
        for (int i = 0;i < newAccountNumber.length();i++)
        {
            numericValue = Character.getNumericValue(newAccountNumber.charAt(i));
            if(-1 >= numericValue)
                return false;
            else
                numericAccountNumber.append(numericValue);
        }
        // Interpret the string as a decimal integer and compute the remainder of that number on division by 97.
        BigInteger ibanNumber = new BigInteger(numericAccountNumber.toString());
        int i = ibanNumber.mod(IBANNUMBER_MAGIC_NUMBER).intValue();
        return i == 1;
    }
 
    // taken from: https://github.com/arturmkrtchyan/iban4j/blob/master/src/main/java/org/iban4j/IbanUtil.java
    /**
     * Calculates
     * <a href="http://en.wikipedia.org/wiki/ISO_13616#Modulo_operation_on_IBAN">Iban Modulo</a>.
     *
     * @param iban String value
     * @return modulo 97
     */
    private static int calculateMod(final String iban)
    {
        final String reformattedIban = iban.substring(4) + iban.substring(0, 4);
        long total = 0;
        for (int i = 0; i < reformattedIban.length(); i++)
        {
            final int numericValue = Character.getNumericValue(reformattedIban.charAt(i));
            total = (numericValue > 9 ? total * 100 : total * 10) + numericValue;

            if (total > 999999999) 
                total = (total % IBANNUMBER_MAGIC_NUMBER.intValue());

        }
        return (int) (total % IBANNUMBER_MAGIC_NUMBER.intValue());
    }

    // taken from: https://github.com/arturmkrtchyan/iban4j/blob/master/src/main/java/org/iban4j/IbanUtil.java
    /**
     * Calculates Iban
     * <a href="http://en.wikipedia.org/wiki/ISO_13616#Generating_IBAN_check_digits">Check Digit</a>.
     *
     * @param iban string value
     * @throws IbanFormatException if iban contains invalid character.
     *
     * @return check digit as String
     */
    public static String calculateCheckDigit(final String iban)
    {
        Pattern pattern = Pattern.compile(PATTERN); //$NON-NLS-1$ //$NON-NLS-2$
        Matcher matcher = pattern.matcher(iban);
        if (matcher.matches())
        {
            final String reformattedIban =  String.format("%2s%2s%18s", iban.substring(0, 2), Iban.DEFAULT_CHECK_DIGIT, StringUtils.leftPad(iban.substring(4), 18, '0'));
            final int modResult = calculateMod(reformattedIban);
            final int checkDigitIntValue = (IBANNUMBER_MAGIC_NUMBER.intValue() + 1 - modResult);
            final String checkDigit = Integer.toString(checkDigitIntValue);
            return checkDigitIntValue > 9 ? checkDigit : "0" + checkDigit;
        }
        else
            System.err.println(">>>> iban::calculateCheckDigit() matcher : " + matcher.toString()); // TODO: still needed for debug?
            return "XX";
    }

    public static final String DEconvert(String blz, String account) // NOSONAR
    {
        String country = "DE";
        String iban = String.format("%2s%2s%8s%10s", country, "XX", StringUtils.leftPad(blz, 8, '0'), StringUtils.leftPad(account, 10, '0'));
        iban = iban.substring(0, 2) + calculateCheckDigit(iban) + iban.substring(4);
        return iban;
    }
}
