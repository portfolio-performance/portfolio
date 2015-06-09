package name.abuchen.portfolio.ui.views;

import name.abuchen.portfolio.model.Bookmark;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Column;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.SimpleListContentProvider;
import name.abuchen.portfolio.ui.util.StringEditingSupport;
import name.abuchen.portfolio.ui.util.ViewerHelper;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
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
        export.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_ADD));
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

        bookmarks = new TableViewer(container, SWT.FULL_SELECTION);

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

        hookContextMenu(bookmarks.getTable(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillContextMenu(manager);
            }

        });
        return container;
    }

    private void fillContextMenu(IMenuManager manager)
    {
        Action a = new Action(Messages.BookmarksListView_delete)
        {
            @Override
            public void run()
            {
                Bookmark index = (Bookmark) ((IStructuredSelection) bookmarks.getSelection()).getFirstElement();

                if (index == null)
                    return;

                getClient().getSettings().removeBookmark(index);
                markDirty();

                bookmarks.setInput(getClient().getSettings().getBookmarks());

            }
        };
        a.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_REMOVE));
        manager.add(a);

        a = new Action(Messages.BookmarksListView_insertBefore)
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
        };
        a.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_ADD));
        manager.add(a);

        a = new Action(Messages.BookmarksListView_insertAfter)
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
        };
        a.setImageDescriptor(PortfolioPlugin.descriptor(PortfolioPlugin.IMG_ADD));
        manager.add(a);
    }

}
