package name.abuchen.portfolio.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;

import org.junit.Test;

import com.google.common.base.Strings;

@SuppressWarnings("nls")
public class TextUtilTest
{

    @Test
    public void testStripNonNumberCharacters()
    {
        assertThat(TextUtil.stripNonNumberCharacters("+ 123,34 x"), is("123,34"));
        assertThat(TextUtil.stripNonNumberCharacters("abcd"), is(""));
        assertThat(TextUtil.stripNonNumberCharacters(",123"), is(",123"));
    }

    @Test
    public void testWordwrap()
    {
        String text = Strings.repeat("t ", 40) + "(test)";
        assertThat(TextUtil.wordwrap(text), is(endsWith("\n(test)")));
    }

}
