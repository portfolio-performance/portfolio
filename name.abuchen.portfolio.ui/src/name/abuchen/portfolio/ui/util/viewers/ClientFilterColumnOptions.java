package name.abuchen.portfolio.ui.util.viewers;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.util.ClientFilterMenu;

public class ClientFilterColumnOptions implements Column.Options<ClientFilterMenu.Item>
{
    private String columnLabel;
    private ClientFilterMenu clientFilterMenu;

    public ClientFilterColumnOptions(String columnLabel, ClientFilterMenu clientFilterMenu)
    {
        this.columnLabel = columnLabel;
        this.clientFilterMenu = clientFilterMenu;
    }

    @Override
    public List<ClientFilterMenu.Item> getOptions()
    {
        return clientFilterMenu.getAllItems().toList();
    }

    @Override
    public ClientFilterMenu.Item valueOf(String s)
    {
        return clientFilterMenu.getAllItems().filter(item -> item.getId().equals(s)).findAny().orElseThrow();
    }

    @Override
    public String toString(ClientFilterMenu.Item option)
    {
        return option.getId();
    }

    @Override
    public String getColumnLabel(ClientFilterMenu.Item option)
    {
        return MessageFormat.format(columnLabel, option.toString());
    }

    @Override
    public String getMenuLabel(ClientFilterMenu.Item option)
    {
        return option.toString();
    }

    @Override
    public String getDescription(ClientFilterMenu.Item option)
    {
        return null;
    }

    @Override
    public boolean canCreateNewOptions()
    {
        return false;
    }

    @Override
    public ClientFilterMenu.Item createNewOption(Shell shell)
    {
        throw new UnsupportedOperationException();
    }
}
