package name.abuchen.portfolio.ui.dialogs.palette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.SimpleAction;

public class CommandPalettePopup extends PopupDialog
{
    public interface Element
    {
        String getTitel();

        default String getSubtitle()
        {
            return null;
        }

        default Images getImage()
        {
            return Images.VIEW;
        }

        default void execute()
        {
        }
    }

    public interface ElementProvider
    {
        List<Element> getElements();
    }

    private static class Item
    {
        private final Element element;

        /* package */ Item(Element element)
        {
            this.element = Objects.requireNonNull(element);
        }

        public void measure(Event event, TextLayout textLayout)
        {
            setup(event, textLayout);

            Image image = element.getImage().image();
            Rectangle imageBounds = image.getBounds();
            event.width += imageBounds.width + 2;
            event.height = Math.max(event.height, imageBounds.height + 2);

            Rectangle textBounds = textLayout.getBounds();
            event.width += textBounds.width + 2;
            event.height = Math.max(event.height, textBounds.height + 2);
        }

        public void paint(Event event, TextLayout textLayout)
        {
            setup(event, textLayout);

            Rectangle tableItemBounds = ((TableItem) event.item).getTextBounds(event.index);

            Image image = element.getImage().image();
            event.gc.drawImage(image, event.x + 1, event.y + 1);
            tableItemBounds.x += 1 + image.getBounds().width;

            Rectangle textBounds = textLayout.getBounds();
            textLayout.draw(event.gc, tableItemBounds.x,
                            tableItemBounds.y + (tableItemBounds.height - textBounds.height) / 2);
        }

        private void setup(Event event, TextLayout textLayout)
        {
            Table table = ((TableItem) event.item).getParent();
            textLayout.setFont(table.getFont());

            String title = element.getTitel();
            String subtitle = element.getSubtitle();

            if (subtitle == null)
            {
                textLayout.setText(title);
            }
            else
            {
                textLayout.setText(title + " " + subtitle); //$NON-NLS-1$
                textLayout.setStyle(new TextStyle(null, Colors.GRAY, null), title.length() + 1,
                                title.length() + 1 + subtitle.length());
            }
        }

        public void erase(Event event)
        {
            event.detail &= ~SWT.FOREGROUND;
        }
    }

    private List<Element> elements = new ArrayList<>();

    private List<Runnable> disposeListeners = new ArrayList<>();

    private Text filterText;
    private Table table;

    private TextLayout textLayout;

    public CommandPalettePopup(IEclipseContext context, String type)
    {
        super(Display.getDefault().getActiveShell(), SWT.TOOL, true, true, false, true, true, null,
                        Messages.LabelStartTyping);

        List<Class<? extends ElementProvider>> provider = new ArrayList<>();

        // for now: if a parameter is given, then it must be to show only new
        // domain element actions
        if (type == null)
        {
            provider.add(NavigationElements.class);
            provider.add(BookmarkElements.class);
            provider.add(TransactionElements.class);
            provider.add(ViewElements.class);
            provider.add(ActionElements.class);
        }

        provider.add(NewDomainElements.class);

        for (Class<? extends ElementProvider> clazz : provider)
            elements.addAll(ContextInjectionFactory.make(clazz, context).getElements());

        Collections.sort(elements, (r, l) -> r.getTitel().compareTo(l.getTitel()));

        create();
    }

    public void addDisposeListener(Runnable disposeListener)
    {
        disposeListeners.add(disposeListener);
    }

    @Override
    protected Color getForeground()
    {
        return Colors.BLACK;
    }

    @Override
    protected Color getBackground()
    {
        return Colors.INFO_TOOLTIP_BACKGROUND;
    }

    @Override
    protected Control createTitleControl(Composite parent)
    {
        filterText = new Text(parent, SWT.NONE);

        GC gc = new GC(parent);
        gc.setFont(parent.getFont());
        FontMetrics fontMetrics = gc.getFontMetrics();
        gc.dispose();

        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false)
                        .hint(SWT.DEFAULT, Dialog.convertHeightInCharsToPixels(fontMetrics, 1)).applyTo(filterText);

        filterText.addKeyListener(KeyListener.keyPressedAdapter(e -> {

            switch (e.keyCode)
            {
                case SWT.CR:
                    handleSelection();
                    return;
                case SWT.ARROW_DOWN:
                    if (table.getItemCount() > 0)
                    {
                        table.setSelection(0);
                        table.setFocus();
                    }
                    return;
                case SWT.ARROW_UP:
                    int ix = table.getSelectionIndex();
                    if (ix >= 1)
                    {
                        table.setSelection(ix - 1);
                        table.setFocus();
                    }
                    return;
                default:
            }

            if (e.character == SWT.ESC)
                close();
        }));

        filterText.addModifyListener(e -> refresh(((Text) e.widget).getText()));

        return filterText;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite) super.createDialogArea(parent);
        boolean isWin32 = Platform.OS_WIN32.equals(SWT.getPlatform());
        GridLayoutFactory.fillDefaults().extendedMargins(isWin32 ? 0 : 3, 3, 2, 2).applyTo(composite);

        Composite tableComposite = new Composite(composite, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(tableComposite);

        TableColumnLayout layout = new TableColumnLayout();
        tableComposite.setLayout(layout);

        table = new Table(tableComposite, SWT.SINGLE | SWT.FULL_SELECTION);

        textLayout = new TextLayout(table.getDisplay());
        textLayout.setOrientation(getDefaultOrientation());
        textLayout.setFont(table.getFont());

        layout.setColumnData(new TableColumn(table, SWT.NONE), new ColumnWeightData(100, 100));

        table.addSelectionListener(SelectionListener.widgetDefaultSelectedAdapter(e -> handleSelection()));

        table.addKeyListener(KeyListener.keyPressedAdapter(e -> {
            if (e.keyCode == SWT.ARROW_UP && table.getSelectionIndex() == 0)
                filterText.setFocus();
            else if (e.character == SWT.ESC)
                close();
            else if (e.keyCode == SWT.CR && e.stateMask != 0)
                handleSelection();
        }));

        Listener listener = event -> {
            Item entry = (Item) event.item.getData();
            switch (event.type)
            {
                case SWT.MeasureItem:
                    entry.measure(event, textLayout);
                    break;
                case SWT.PaintItem:
                    entry.paint(event, textLayout);
                    break;
                case SWT.EraseItem:
                    entry.erase(event);
                    break;
                default:
            }
        };

        table.addListener(SWT.MeasureItem, listener);
        table.addListener(SWT.EraseItem, listener);
        table.addListener(SWT.PaintItem, listener);

        // add all elements
        refresh(""); //$NON-NLS-1$

        return composite;
    }

    @Override
    protected Control getFocusControl()
    {
        return filterText;
    }

    @Override
    public boolean close()
    {
        for (Runnable l : disposeListeners)
            l.run();

        if (textLayout != null && !textLayout.isDisposed())
            textLayout.dispose();

        return super.close();
    }

    @Override
    protected IDialogSettings getDialogSettings()
    {
        final IDialogSettings settings = PortfolioPlugin.getDefault().getDialogSettings();
        IDialogSettings result = settings.getSection("command-palette"); //$NON-NLS-1$
        if (result == null)
            result = settings.addNewSection("command-palette"); //$NON-NLS-1$
        return result;
    }

    @Override
    protected Point getDefaultSize()
    {
        return new Point(400, 200);
    }

    @Override
    protected Point getDefaultLocation(Point initialSize)
    {
        Rectangle parentBounds = getParentShell().getBounds();
        int x = parentBounds.x + parentBounds.width / 2 - initialSize.x / 2;
        int y = parentBounds.y + parentBounds.height / 2 - initialSize.y / 2;
        return new Point(x, y);
    }

    @Override
    protected void fillDialogMenu(IMenuManager dialogMenu)
    {
        dialogMenu.add(new SimpleAction(Messages.LabelClose, a -> close()));
        dialogMenu.add(new Separator());
        super.fillDialogMenu(dialogMenu);
    }

    private void handleSelection()
    {
        if (table.getSelectionCount() == 1)
        {
            Item item = ((Item) table.getItem(table.getSelectionIndex()).getData());
            item.element.execute();
        }

        close();
    }

    private void refresh(String filter)
    {
        updateTableWith(match(filter));

        if (table.getItemCount() > 0)
            table.setSelection(0);

        if (filter.length() == 0)
            setInfoText(Messages.LabelStartTyping);
        else
            setInfoText(""); //$NON-NLS-1$
    }

    private void updateTableWith(List<Item> items)
    {
        TableItem[] tableItems = table.getItems();
        table.clearAll();

        int index = 0;
        for (Item item : items)
        {
            TableItem tableItem = index < tableItems.length ? tableItems[index] : new TableItem(table, SWT.NONE);
            tableItem.setData(item);
            tableItem.setText(0, item.element.getTitel());

            index++;
        }

        if (index < tableItems.length)
            table.remove(index, tableItems.length - 1);
    }

    private List<Item> match(String filter)
    {
        if (filter.isEmpty())
            return this.elements.stream().map(Item::new).toList();

        Pattern filterPattern = Pattern.compile(".*" + filter + ".*", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$ //$NON-NLS-2$

        List<Element> result = new ArrayList<>();

        // first: search title
        this.elements.stream().filter(e -> filterPattern.matcher(e.getTitel()).matches()).forEach(result::add);

        // second: search subtitle (but add only if not yet found)
        this.elements.stream().filter(e -> e.getSubtitle() != null && filterPattern.matcher(e.getSubtitle()).matches())
                        .filter(e -> !result.contains(e)).forEach(result::add);

        return result.stream().map(Item::new).toList();
    }
}
