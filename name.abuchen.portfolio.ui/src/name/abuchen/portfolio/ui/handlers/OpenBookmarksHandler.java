package name.abuchen.portfolio.ui.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Bookmark;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.selection.SecuritySelection;
import name.abuchen.portfolio.ui.selection.SelectionService;
import name.abuchen.portfolio.ui.util.DesktopAPI;

public class OpenBookmarksHandler
{
    public static class BookmarkPopup<T> extends PopupDialog
    {
        private List<T> items;
        private Function<T, String> label;
        private Consumer<T> action;

        public BookmarkPopup(Shell shell, String title, List<T> items, Function<T, String> label, Consumer<T> action)
        {
            super(shell, SWT.NO_TRIM, true, false, false, false, false, title, null);

            this.items = items;
            this.label = label;
            this.action = action;
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            Composite composite = (Composite) super.createDialogArea(parent);
            boolean isWin32 = "win32".equals(SWT.getPlatform()); //$NON-NLS-1$
            GridLayoutFactory.fillDefaults().extendedMargins(isWin32 ? 0 : 3, 3, 2, 2).applyTo(composite);

            Composite tableComposite = new Composite(composite, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(tableComposite);

            TableColumnLayout tableColumnLayout = new TableColumnLayout();
            tableComposite.setLayout(tableColumnLayout);

            TableViewer tableViewer = new TableViewer(tableComposite, SWT.SINGLE | SWT.FULL_SELECTION | SWT.NO_SCROLL);
            tableViewer.setContentProvider(ArrayContentProvider.getInstance());
            tableViewer.getTable().setHeaderVisible(false);
            tableViewer.getTable().setLinesVisible(false);

            TableViewerColumn tableColumn = new TableViewerColumn(tableViewer, SWT.NONE);
            tableColumnLayout.setColumnData(tableColumn.getColumn(), new ColumnPixelData(200));
            tableColumn.setLabelProvider(new ColumnLabelProvider()
            {
                @SuppressWarnings("unchecked")
                @Override
                public String getText(Object element)
                {
                    return label.apply((T) element);
                }
            });

            tableViewer.getTable().addSelectionListener(SelectionListener
                            .widgetDefaultSelectedAdapter(e -> handleSelection(tableViewer.getStructuredSelection())));

            tableViewer.setInput(items);
            tableViewer.setSelection(new StructuredSelection(items.get(0)));

            return composite;
        }

        protected void handleSelection(IStructuredSelection selection)
        {
            @SuppressWarnings("unchecked")
            T item = (T) selection.getFirstElement();

            if (item != null)
            {
                action.accept(item);
            }

            close();
        }
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell, SelectionService selectionService)
    {
        MenuHelper.getActiveClient(part).ifPresent(client -> openPopup(shell, selectionService, client));
    }

    private void openPopup(Shell shell, SelectionService selectionService, Client client)
    {
        Optional<SecuritySelection> selection = selectionService.getSelection(client);
        if (!selection.isPresent())
            return;

        Security security = selection.get().getSecurity();

        List<Bookmark> bookmarks = new ArrayList<>();

        client.getSettings().getBookmarks().stream().filter(b -> !b.isSeparator()).forEach(bookmarks::add);

        security.getCustomBookmarks().forEach(bookmarks::add);

        BookmarkPopup<Bookmark> popup = new BookmarkPopup<>(shell, //
                        security.getName(), //
                        bookmarks, //
                        Bookmark::getLabel, //
                        bm -> DesktopAPI.browse(bm.constructURL(client, security)));
        popup.open();
    }
}
