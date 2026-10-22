package name.abuchen.portfolio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.junit.Test;

@SuppressWarnings("nls")
public class PortfolioLogTest
{
    @Test
    public void testMultipleDetailsProduceMultiStatusChildren()
    {
        var status = PortfolioLog.buildInfoStatus("pid", "summary", List.of("a", "b"));

        assertThat(status.isMultiStatus(), is(true));
        assertThat(status.getSeverity(), is(IStatus.INFO));
        assertThat(status.getMessage(), is("summary"));
        assertThat(status.getChildren().length, is(2));
        assertThat(status.getChildren()[0].getMessage(), is("a"));
        assertThat(status.getChildren()[1].getMessage(), is("b"));
        assertThat(status.getChildren()[0].getSeverity(), is(IStatus.INFO));
    }

    @Test
    public void testNoDetailsProduceSingleInfoStatus()
    {
        var status = PortfolioLog.buildInfoStatus("pid", "summary", List.of());

        assertThat(status.isMultiStatus(), is(false));
        assertThat(status.getSeverity(), is(IStatus.INFO));
        assertThat(status.getMessage(), is("summary"));
    }
}
