package name.abuchen.portfolio.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;

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

    @Test
    public void testStripCallbacksJsonObjects()
    {
        String json = "{\"name\"=\"value\"}";

        assertThat(TextUtil.stripJavaScriptCallback(json), is(json));
        assertThat(TextUtil.stripJavaScriptCallback("callback(" + json + ");"), is(json));
        assertThat(TextUtil.stripJavaScriptCallback(null), is(nullValue()));
        assertThat(TextUtil.stripJavaScriptCallback("something"), is("something"));
        assertThat(TextUtil.stripJavaScriptCallback("}something{"), is("}something{"));

        json = Strings.repeat("texttext", 100);
        assertThat(TextUtil.stripJavaScriptCallback(json), is(json));
    }

    @Test
    public void testStripCallbacksJsonArrays()
    {
        String json = "[{\"name\"=\"value\"}]";

        assertThat(TextUtil.stripJavaScriptCallback(json), is(json));
        assertThat(TextUtil.stripJavaScriptCallback("callback(" + json + ");"), is(json));
        assertThat(TextUtil.stripJavaScriptCallback("angular.callbacks._u(" + json + ")"), is(json));
        assertThat(TextUtil.stripJavaScriptCallback("sd" + json), is("sd" + json));
        assertThat(TextUtil.stripJavaScriptCallback("]something["), is("]something["));
    }

    @Test
    public void testSanitizeFilename()
    {
        assertThat(TextUtil.sanitizeFilename("?a\\b/c:d|e<f>g//h*i"), is("_a_b_c_d_e_f_g_h_i"));
        assertThat(TextUtil.sanitizeFilename("a    b"), is("a_b"));
        assertThat(TextUtil.sanitizeFilename("äöüÄÖÜß"), is("äöüÄÖÜß"));
        assertThat(TextUtil.sanitizeFilename("Акти"), is("Акти"));
    }

}
