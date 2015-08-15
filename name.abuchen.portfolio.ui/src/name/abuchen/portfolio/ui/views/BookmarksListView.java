package name.abuchen.portfolio.ui.views;

import name.abuchen.portfolio.model.Bookmark;
import name.abuchen.portfolio.model.ClientSettings;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Column;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.DesktopAPI;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.StringEditingSupport;
import name.abuchen.portfolio.ui.util.ViewerHelper;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

public class BookmarksListView extends AbstractFinanceView implements ModificationListener
{
    private static final String DEFAULT_URL = "http://example.net/{tickerSymbol}?isin={isin}&wkn={wkn}&name={name}"; //$NON-NLS-1$
    private TableViewer bookmarks;

    @Override
    protected String getTitle()
    {
        return Messages.BookmarksListView_title;
    }

    protected void addButtons(ToolBar toolBar)
    {
        super.addButtons(toolBar);
        addAddButton(toolBar);
    }

    private void addAddButton(ToolBar toolBar)
    {
        Action export = new Action()
        {

            @Override
            public void run()
            {
                Bookmark wl = new Bookmark(Messages.BookmarksListView_NewBookmark, DEFAULT_URL);

                getClient().getSettings().getBookmarks().add(wl);
                markDirty();

                bookmarks.setInput(getClient().getSettings().getBookmarks());
                bookmarks.editElement(wl, 0);
            }
        };
        export.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_PLUS));
        export.setToolTipText(Messages.BookmarksListView_tooltip);

        new ActionContributionItem(export).fill(toolBar, -1);
    }

    @Override
    public void notifyModelUpdated()
    {
        bookmarks.setInput(getClient().getSettings().getBookmarks());

    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        markDirty();
    }

    @Override
    public void markDirty()
    {
        super.markDirty();
    }

    @Override
    protected Control createBody(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        bookmarks = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI);

        ColumnEditingSupport.prepare(bookmarks);

        ShowHideColumnHelper support = new ShowHideColumnHelper(
                        BookmarksListView.class.getSimpleName() + "@bottom", getPreferenceStore(), bookmarks, layout); //$NON-NLS-1$

        // Create Column for Bookmark
        Column column = new Column(Messages.BookmarksListView_bookmark, SWT.None, 150);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Bookmark) element).getLabel();
            }

            @Override
            public Image getImage(Object element)
            {
                return PortfolioPlugin.image(PortfolioPlugin.IMG_TEXT);
            }

        });

        new StringEditingSupport(Bookmark.class, "label").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        // Create Column for URL
        column = new Column(Messages.BookmarksListView_url, SWT.None, 500);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((Bookmark) element).getPattern();
            }
        });

        new StringEditingSupport(Bookmark.class, "pattern").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        support.createColumns();

        bookmarks.getTable().setHeaderVisible(true);
        bookmarks.getTable().setLinesVisible(true);

        bookmarks.setContentProvider(new SimpleListContentProvider());

        ViewerHelper.pack(bookmarks);

        bookmarks.setInput(getClient().getSettings().getBookmarks());
        bookmarks.refresh();

        hookContextMenu(bookmarks.getTable(), m -> fillContextMenu(m));
        return container;
    }

    private void fillContextMenu(IMenuManager manager)
    {
        Bookmark bookmark = (Bookmark) ((IStructuredSelection) bookmarks.getSelection()).getFirstElement();

        if (!bookmark.isSeparator())
            addTestMenu(manager, bookmark);

        addMoveUpAndDownActions(manager, bookmark);

        manager.add(new Separator());
        manager.add(new Action(Messages.BookmarksListView_insertBefore)
        {
            @Override
            public void run()
            {
                Bookmark index = (Bookmark) ((IStructuredSelection) bookmarks.getSelection()).getFirstElement();
                Bookmark wl = new Bookmark(Messages.BookmarksListView_NewBookmark, DEFAULT_URL);

                getClient().getSettings().insertBookmark(index, wl);
                markDirty();

                bookmarks.setInput(getClient().getSettings().getBookmarks());
                bookmarks.editElement(wl, 0);
            }
        });

        manager.add(new Action(Messages.BookmarksListView_insertAfter)
        {
            @Override
            public void run()
            {
                Bookmark index = (Bookmark) ((IStructuredSelection) bookmarks.getSelection()).getFirstElement();
                Bookmark wl = new Bookmark(Messages.BookmarksListView_NewBookmark, DEFAULT_URL);

                getClient().getSettings().insertBookmarkAfter(index, wl);
                markDirty();

                bookmarks.setInput(getClient().getSettings().getBookmarks());
                bookmarks.editElement(wl, 0);
            }
        });

        manager.add(new Action(Messages.BookmarksListView_addSeparator)
        {
            @Override
            public void run()
            {
                Bookmark index = (Bookmark) ((IStructuredSelection) bookmarks.getSelection()).getFirstElement();
                Bookmark wl = new Bookmark("-", ""); //$NON-NLS-1$ //$NON-NLS-2$

                getClient().getSettings().insertBookmarkAfter(index, wl);
                markDirty();

                bookmarks.setInput(getClient().getSettings().getBookmarks());
            }
        });

        manager.add(new Separator());
        manager.add(new Action(Messages.BookmarksListView_delete)
        {
            @Override
            public void run()
            {
                ClientSettings settings = getClient().getSettings();
                for (Object element : ((IStructuredSelection) bookmarks.getSelection()).toArray())
                    settings.removeBookmark((Bookmark) element);

                markDirty();
                bookmarks.setInput(settings.getBookmarks());
            }
        });

    }

    private void addTestMenu(IMenuManager manager, Bookmark bookmark)
    {
        MenuManager securities = new MenuManager(Messages.MenuOpenSecurityOnSite);
        for (Security security : getClient().getSecurities())
        {
            securities.add(new Action(security.getName())
            {
                @Override
                public void run()
                {
                    DesktopAPI.browse(bookmark.constructURL(security));
                }
            });
        }
        manager.add(securities);
        manager.add(new Separator());
    }

    private void addMoveUpAndDownActions(IMenuManager manager, Bookmark bookmark)
    {
        int index = getClient().getSettings().getBookmarks().indexOf(bookmark);
        if (index > 0)
        {
            manager.add(new Action(Messages.BookmarksListView_MoveUp)
            {
                @Override
                public void run()
                {
                    ClientSettings settings = getClient().getSettings();
                    settings.removeBookmark(bookmark);
                    settings.insertBookmark(index - 1, bookmark);
                    bookmarks.setInput(getClient().getSettings().getBookmarks());
                    markDirty();
                }
            });
        }

        if (index < getClient().getSettings().getBookmarks().size() - 1)
        {
            manager.add(new Action(Messages.BookmarksListView_MoveDown)
            {
                @Override
                public void run()
                {
                    ClientSettings settings = getClient().getSettings();
                    settings.removeBookmark(bookmark);
                    settings.insertBookmark(index + 1, bookmark);
                    bookmarks.setInput(getClient().getSettings().getBookmarks());
                    markDirty();
                }
            });
        }
    }
}
