package name.abuchen.portfolio.ui.util.viewers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.junit.Test;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.columns.AttributeColumn;

/**
 * Located in the viewers package to access the package-private accessors of
 * {@link Column}.
 */
@SuppressWarnings("nls")
public class AttributeColumnTest
{
    @Test
    public void testRenderingAttributesWithSameIdForDifferentTargets()
    {
        var client = new Client();
        var security = new Security();
        var account = new Account();

        for (var target : List.of(Security.class, Account.class))
        {
            var type = new AttributeType("shared");
            type.setName("Shared");
            type.setColumnLabel("Shared");
            type.setTarget(target);
            type.setType(String.class);
            type.setConverter(AttributeType.StringConverter.class);
            client.getSettings().addAttributeType(type);
            security.getAttributes().put(type, "security value");
            account.getAttributes().put(type, "account value");
        }

        var columns = AttributeColumn.createForSecuritiesAndAccounts(client).toList();
        var securityColumn = columns.stream().filter(c -> c.getId().equals("attribute$shared")).findFirst().orElseThrow();
        var accountColumn = columns.stream().filter(c -> c.getId().equals("attribute$account$shared")).findFirst()
                        .orElseThrow();
        var securityLabels = (ColumnLabelProvider) securityColumn.getLabelProvider().get();
        var accountLabels = (ColumnLabelProvider) accountColumn.getLabelProvider().get();

        assertThat(securityLabels.getText(security), is("security value"));
        assertThat(securityLabels.getText(account), is(nullValue()));
        assertThat(accountLabels.getText(account), is("account value"));
        assertThat(accountLabels.getText(security), is(nullValue()));
        assertThat(securityLabels.getText(null), is(nullValue()));
    }

    @Test
    public void testMixedColumnsHaveUniqueIds()
    {
        // the default logo attribute exists for securities and accounts with
        // the same id
        var client = new Client();
        addAccountDateAttribute(client);

        var columns = AttributeColumn.createForSecuritiesAndAccounts(client).toList();

        var ids = columns.stream().map(Column::getId).toList();
        assertThat(new HashSet<>(ids).size(), is(ids.size()));
    }

    @Test
    public void testSecurityColumnIdsAreUnchanged()
    {
        var client = new Client();

        var securityIds = AttributeColumn.createFor(client, Security.class).map(Column::getId).toList();
        var mixedIds = AttributeColumn.createForSecuritiesAndAccounts(client).map(Column::getId).toList();

        assertThat(mixedIds.subList(0, securityIds.size()), is(securityIds));
        assertThat(mixedIds.subList(securityIds.size(), mixedIds.size()), everyItem(startsWith("attribute$account$")));
    }

    @Test
    public void testHeadingsOnFirstColumnOfEachSection()
    {
        var client = new Client();
        addAccountDateAttribute(client);

        var columns = AttributeColumn.createForSecuritiesAndAccounts(client).toList();
        var securityCount = AttributeColumn.createFor(client, Security.class).count();

        var withHeading = columns.stream().filter(Column::hasHeading).toList();
        assertThat(withHeading, hasSize(2));

        assertThat(columns.get(0).getHeading(), is(Messages.LabelSecurities));
        assertThat(columns.get((int) securityCount).getHeading(), is(Messages.LabelAccounts));
    }

    @Test
    public void testNoHeadingForEmptySection()
    {
        var client = new Client();
        List<AttributeType> securityTypes = client.getSettings().getAttributeTypes()
                        .filter(t -> t.getTarget() == Security.class).toList();
        securityTypes.forEach(t -> client.getSettings().removeAttributeType(t));

        var columns = AttributeColumn.createForSecuritiesAndAccounts(client).toList();

        var withHeading = columns.stream().filter(Column::hasHeading).toList();
        assertThat(withHeading, hasSize(1));
        assertThat(columns.get(0).getHeading(), is(Messages.LabelAccounts));
    }

    @Test
    public void testDescriptionNamesTarget()
    {
        var client = new Client();
        addAccountDateAttribute(client);

        var accountColumns = AttributeColumn.createFor(client, Account.class).toList();

        // includes the secondary "days between" column of the date attribute
        assertThat(accountColumns.size() > 1, is(true));
        assertThat(accountColumns.stream().map(Column::getDescription).toList(), everyItem(
                        is(MessageFormat.format(Messages.ColumnAttributeDefinedFor, Messages.LabelAccounts))));
    }

    @Test
    public void testTargetLabel()
    {
        assertThat(AttributeColumn.getTargetLabel(Security.class), is(notNullValue()));
        assertThat(AttributeColumn.getTargetLabel(Account.class), is(Messages.LabelAccounts));
        assertThat(AttributeColumn.getTargetLabel(null), is(nullValue()));
    }

    private void addAccountDateAttribute(Client client)
    {
        var type = new AttributeType("date");
        type.setName("Date");
        type.setColumnLabel("Date");
        type.setTarget(Account.class);
        type.setType(LocalDate.class);
        type.setConverter(AttributeType.DateConverter.class);
        client.getSettings().addAttributeType(type);
    }
}
