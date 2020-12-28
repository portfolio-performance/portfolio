package name.abuchen.portfolio.ui.update;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;

import org.junit.Test;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

import name.abuchen.portfolio.ui.update.NewVersion.ConditionalMessage;
import name.abuchen.portfolio.ui.update.NewVersion.Release;

@SuppressWarnings("nls")
public class NewVersionTest
{
    @Test
    public void testConditionalMessage()
    {
        String history = "-- 0.30.1\n" //
                        + "~~ (something=true)\n" //
                        + "~~ hello world\n" //
                        + "* text\n";

        NewVersion newVersion = new NewVersion("0.30.1");
        newVersion.setVersionHistory(history);

        assertThat(newVersion.getVersion(), is("0.30.1"));
        assertThat(newVersion.getReleases(), hasSize(1));

        Release release = newVersion.getReleases().get(0);
        assertThat(release.getVersion(), is(new Version("0.30.1")));
        assertThat(release.getLines(), is(Arrays.asList("* text")));
        assertThat(release.getMessages(), hasSize(1));

        ConditionalMessage message = release.getMessages().get(0);
        assertThat(message.getLines(), is(Arrays.asList("hello world")));

        System.clearProperty("something");
        assertThat(message.isApplicable(), is(false));

        System.setProperty("something", Boolean.FALSE.toString());
        assertThat(message.isApplicable(), is(false));

        System.setProperty("something", Boolean.TRUE.toString());
        assertThat(message.isApplicable(), is(true));
    }

    @Test
    public void testIfBundleExists()
    {
        String history = "-- 0.30.1\n" //
                        + "~~ ($bundle.list=name.abuchen.portfolio.*)\n" //
                        + "~~ hello world\n" //
                        + "* text\n";

        NewVersion newVersion = new NewVersion("0.30.1");
        newVersion.setVersionHistory(history);

        ConditionalMessage message = newVersion.getReleases().get(0).getMessages().get(0);
        assertThat(message.getLines(), is(Arrays.asList("hello world")));

        assertThat(message.isApplicable(), is(true));
    }

    @Test
    public void testIfBundleNotExists()
    {
        String history = "-- 0.30.1\n" //
                        + "~~ ($bundle.list=something.*)\n" //
                        + "~~ hello world\n" //
                        + "* text\n";

        NewVersion newVersion = new NewVersion("0.30.1");
        newVersion.setVersionHistory(history);

        ConditionalMessage message = newVersion.getReleases().get(0).getMessages().get(0);
        assertThat(message.getLines(), is(Arrays.asList("hello world")));

        assertThat(message.isApplicable(), is(false));
    }

    @Test
    public void testMultipleConditionalMessage()
    {
        String history = "-- 0.30.1\n" //
                        + "~~ ($bundle.list=name.abuchen.portfolio.*&something=true)\n" //
                        + "~~ hello world\n" //
                        + "~~ ($bundle.list=name.abuchen.portfolio.*&something=false)\n" //
                        + "~~ another day\n" //
                        + "* text\n";

        NewVersion newVersion = new NewVersion("0.30.1");
        newVersion.setVersionHistory(history);

        assertThat(newVersion.getVersion(), is("0.30.1"));
        assertThat(newVersion.getReleases(), hasSize(1));

        Release release = newVersion.getReleases().get(0);
        assertThat(release.getVersion(), is(new Version("0.30.1")));
        assertThat(release.getLines(), is(Arrays.asList("* text")));
        assertThat(release.getMessages(), hasSize(2));

        ConditionalMessage message1 = release.getMessages().get(0);
        assertThat(message1.getLines(), is(Arrays.asList("hello world")));

        ConditionalMessage message2 = release.getMessages().get(1);
        assertThat(message2.getLines(), is(Arrays.asList("another day")));

        System.clearProperty("something");
        assertThat(message1.isApplicable(), is(false));
        assertThat(message2.isApplicable(), is(false));

        System.setProperty("something", Boolean.FALSE.toString());
        assertThat(message1.isApplicable(), is(false));
        assertThat(message2.isApplicable(), is(true));

        System.setProperty("something", Boolean.TRUE.toString());
        assertThat(message1.isApplicable(), is(true));
        assertThat(message2.isApplicable(), is(false));
    }

    @Test
    public void testVersionConditionalMessage()
    {
        String history = "-- 0.44.2\n"
                        + "~~ ($version=" + FrameworkUtil.getBundle(NewVersionTest.class).getVersion() + ")\n"
                        + "~~ hello world\n" //
                        + "* text\n";

        NewVersion newVersion = new NewVersion("0.44.2");
        newVersion.setVersionHistory(history);

        assertThat(newVersion.getReleases(), hasSize(1));

        Release release = newVersion.getReleases().get(0);
        assertThat(release.getVersion(), is(new Version("0.44.2")));
        assertThat(release.getLines(), is(Arrays.asList("* text")));
        assertThat(release.getMessages(), hasSize(1));

        ConditionalMessage message1 = release.getMessages().get(0);
        assertThat(message1.getLines(), is(Arrays.asList("hello world")));
        assertThat(message1.isApplicable(), is(true));
    }

    @Test
    public void testVersionIsSmallerConditionalMessage()
    {
        String history = "-- 0.44.2\n" //
                        + "~~ (something=^0[.]([1-9]|[1-4][0-4])[.].*)\n" //
                        + "~~ hello world\n" //
                        + "* text\n";

        NewVersion newVersion = new NewVersion("0.44.2");
        newVersion.setVersionHistory(history);

        assertThat(newVersion.getReleases(), hasSize(1));

        Release release = newVersion.getReleases().get(0);
        assertThat(release.getVersion(), is(new Version("0.44.2")));
        assertThat(release.getLines(), is(Arrays.asList("* text")));
        assertThat(release.getMessages(), hasSize(1));

        ConditionalMessage message = release.getMessages().get(0);
        assertThat(message.getLines(), is(Arrays.asList("hello world")));

        System.setProperty("something", "0.7.1");
        assertThat(message.isApplicable(), is(true));

        System.setProperty("something", "0.44.1");
        assertThat(message.isApplicable(), is(true));

        System.setProperty("something", "0.45.1");
        assertThat(message.isApplicable(), is(false));

        System.setProperty("something", "1.5.1");
        assertThat(message.isApplicable(), is(false));
    }
}
