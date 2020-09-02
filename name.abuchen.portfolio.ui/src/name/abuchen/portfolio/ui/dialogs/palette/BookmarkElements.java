package name.abuchen.portfolio.ui.dialogs.palette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import name.abuchen.portfolio.model.Bookmark;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.Element;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.ElementProvider;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.selection.SelectionService;
import name.abuchen.portfolio.ui.util.DesktopAPI;

/* package */ class BookmarkElements implements ElementProvider
{
    private static class BookmarkElement implements Element
    {
        private final Bookmark bookmark;
        private final SecuritySelection selection;

        public BookmarkElement(Bookmark bookmark, SecuritySelection selection)
        {
            this.bookmark = bookmark;
            this.selection = selection;
        }

        @Override
        public String getTitel()
        {
            return bookmark.getLabel();
        }

        @Override
        public String getSubtitle()
        {
            return selection.getSecurity().getName() + " " + Messages.BookmarksListView_bookmark; //$NON-NLS-1$
        }

        @Override
        public Images getImage()
        {
            return Images.BOOKMARK;
        }

        @Override
        public void execute()
        {
            DesktopAPI.browse(bookmark.constructURL(selection.getClient(), selection.getSecurity()));
        }
    }

    @Inject
    private Client client;

    @Inject
    private SelectionService selectionService;

    @Override
    public List<Element> getElements()
    {
        Optional<SecuritySelection> selection = selectionService.getSelection(client);

        if (!selection.isPresent())
            return Collections.emptyList();

        SecuritySelection security = selection.get();

        List<Element> answer = new ArrayList<>();

        client.getSettings().getBookmarks().stream() //
                        .filter(b -> !b.isSeparator()) //
                        .map(b -> new BookmarkElement(b, security)) //
                        .forEach(answer::add);

        security.getSecurity().getCustomBookmarks()//
                        .map(bm -> new BookmarkElement(bm, security)) //
                        .forEach(answer::add);

        return answer;
    }
}
