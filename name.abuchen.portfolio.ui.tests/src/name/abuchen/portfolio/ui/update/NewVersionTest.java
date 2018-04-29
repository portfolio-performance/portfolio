package name.abuchen.portfolio.ui.update;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Test;
import org.osgi.framework.Version;

import name.abuchen.portfolio.ui.update.NewVersion.ConditionalMessage;
import name.abuchen.portfolio.ui.update.NewVersion.Release;

public class NewVersionTest
{
    @Test
    public void testConditionalMessage()
    {
        String history = "-- 0.30.1\n" //$NON-NLS-1$
                        + "~~ (something=true)\n" //$NON-NLS-1$
                        + "~~ hello world\n" //$NON-NLS-1$
                        + "* text\n"; //$NON-NLS-1$

        NewVersion newVersion = new NewVersion("0.30.1"); //$NON-NLS-1$
        newVersion.setVersionHistory(history);

        assertThat(newVersion.getVersion(), is("0.30.1")); //$NON-NLS-1$
        assertThat(newVersion.getReleases(), hasSize(1));

        Release release = newVersion.getReleases().get(0);
        assertThat(release.getVersion(), is(new Version("0.30.1"))); //$NON-NLS-1$
        assertThat(release.getLines(), is(Arrays.asList(new String[] { "* text" }))); //$NON-NLS-1$
        assertThat(release.getMessages(), hasSize(1));

        ConditionalMessage message = release.getMessages().get(0);
        assertThat(message.getLines(), is(Arrays.asList(new String[] { "hello world" }))); //$NON-NLS-1$
        
        System.clearProperty("something"); //$NON-NLS-1$
        assertThat(message.isApplicable(), is(false));
        
        System.setProperty("something", Boolean.FALSE.toString()); //$NON-NLS-1$
        assertThat(message.isApplicable(), is(false));

        System.setProperty("something", Boolean.TRUE.toString()); //$NON-NLS-1$
        assertThat(message.isApplicable(), is(true));
    }
    
    @Test
    public void testIfBundleExists()
    {
        String history = "-- 0.30.1\n" //$NON-NLS-1$
                        + "~~ ($bundle.list=name.abuchen.portfolio.*)\n" //$NON-NLS-1$
                        + "~~ hello world\n" //$NON-NLS-1$
                        + "* text\n"; //$NON-NLS-1$

        NewVersion newVersion = new NewVersion("0.30.1"); //$NON-NLS-1$
        newVersion.setVersionHistory(history);

        ConditionalMessage message = newVersion.getReleases().get(0).getMessages().get(0);
        assertThat(message.getLines(), is(Arrays.asList(new String[] { "hello world" }))); //$NON-NLS-1$
        
        assertThat(message.isApplicable(), is(true));
    }

    @Test
    public void testIfBundleNotExists()
    {
        String history = "-- 0.30.1\n" //$NON-NLS-1$
                        + "~~ ($bundle.list=something.*)\n" //$NON-NLS-1$
                        + "~~ hello world\n" //$NON-NLS-1$
                        + "* text\n"; //$NON-NLS-1$

        NewVersion newVersion = new NewVersion("0.30.1"); //$NON-NLS-1$
        newVersion.setVersionHistory(history);

        ConditionalMessage message = newVersion.getReleases().get(0).getMessages().get(0);
        assertThat(message.getLines(), is(Arrays.asList(new String[] { "hello world" }))); //$NON-NLS-1$
        
        assertThat(message.isApplicable(), is(false));
    }

}
