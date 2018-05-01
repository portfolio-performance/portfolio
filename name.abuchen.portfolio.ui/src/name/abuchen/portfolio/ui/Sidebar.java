package name.abuchen.portfolio.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

import name.abuchen.portfolio.ui.util.Colors;

public final class Sidebar extends Composite
{
    public static final class Entry
    {
        private String id;
        private Sidebar bar;
        private Item item;
        private Action action;

        public Entry(Sidebar sidebar, String label)
        {
            id = label;
            bar = sidebar;
            item = bar.createItem(bar.entries.size(), this, label, 0);
        }

        public Entry(Entry parent, String label)
        {
            this.id = parent.getId() + label;
            this.bar = parent.bar;

            int index = this.bar.entries.indexOf(parent) + 1;
            for (; index < this.bar.entries.size(); index++)
                if (bar.entries.get(index).item.indent == parent.item.indent)
                    break;

            item = bar.createItem(index, this, label, parent.item.indent + STEP);
        }

        public Entry(Entry parent, Action action)
        {
            this(parent, action.getText());
            setAction(action);
        }

        public String getId()
        {
            return id;
        }

        public void setAction(Action action)
        {
            this.action = action;

            if (action.getImageDescriptor() != null)
                item.setImage(action.getImageDescriptor().createImage(true));
        }

        public Action getAction()
        {
            return action;
        }

        public void setContextMenu(MenuListener listener)
        {
            item.addContextMenu(listener);
        }

        public void addDropSupport(int operations, Transfer[] transferTypes, final DropTargetListener listener)
        {
            DropTarget dropTarget = new DropTarget(item, operations);
            dropTarget.setTransfer(transferTypes);
            dropTarget.addDropListener(new DropTargetListener()
            {
                @Override
                public void dropAccept(DropTargetEvent event)
                {
                    listener.dropAccept(event);
                }

                @Override
                public void drop(DropTargetEvent event)
                {
                    listener.drop(event);
                }

                @Override
                public void dragOver(DropTargetEvent event)
                {
                    listener.dragOver(event);
                }

                @Override
                public void dragOperationChanged(DropTargetEvent event)
                {
                    listener.dragOperationChanged(event);
                }

                @Override
                public void dragLeave(DropTargetEvent event)
                {
                    listener.dragLeave(event);
                    item.setIsDragTarget(false);
                    item.redraw();
                }

                @Override
                public void dragEnter(DropTargetEvent event)
                {
                    listener.dragEnter(event);
                    item.setIsDragTarget(true);
                    item.redraw();
                }
            });
        }

        /**
         * Finds either the neighbor above (SWT.ARROW_DOWN) or below
         * (SWT.ARROW_UP) the current entry.
         */
        public Entry findNeighbor(int direction)
        {
            int index = bar.entries.indexOf(this);

            if (direction == SWT.ARROW_DOWN)
                return index + 1 < bar.entries.size() ? bar.entries.get(index + 1) : null;
            else if (direction == SWT.ARROW_UP)
                return index > 0 ? bar.entries.get(index - 1) : null;

            return null;
        }

        public boolean isSelectable()
        {
            return item != null && item.indent > 0 && action != null;
        }

        public void setLabel(String label)
        {
            item.text = label;
            item.redraw();
        }

        public String getLabel()
        {
            return item.text;
        }

        public int getIndent()
        {
            return item.indent;
        }

        public void dispose()
        {
            Entry down = findNeighbor(SWT.ARROW_DOWN);

            if (down != null)
            {
                FormData data = (FormData) down.item.getLayoutData();

                Entry up = findNeighbor(SWT.ARROW_UP);
                if (up != null)
                    data.top = new FormAttachment(up.item, down.item.indent == 0 ? 20 : 0);
                else
                    data.top = new FormAttachment(0, 5);
            }

            bar.entries.remove(this);

            if (bar.selection == this)
                bar.selection = null;

            item.dispose();
            bar.layout();
        }

        public void select()
        {
            bar.select(this);
        }

        /**
         * Moves the current item one up. Requires the composite to re-layout
         * afterwards.
         */
        public void moveUp()
        {
            int index = bar.entries.indexOf(this);
            if (index == 0)
                throw new IllegalArgumentException();

            bar.entries.remove(index);
            bar.entries.add(index - 1, this);

            for (int ii = index - 1; ii <= index + 1 && ii < bar.entries.size(); ii++)
                bar.setLayoutData(ii, bar.entries.get(ii).item);
        }
    }

    public interface MenuListener
    {
        void menuAboutToShow(Entry entry, IMenuManager manager);
    }

    public static final int STEP = 10;

    private Font regularFont;
    private Font boldFont;
    private Font sectionFont;

    private List<Entry> entries = new ArrayList<>();

    private Entry selection = null;

    public Sidebar(Composite parent, int style)
    {
        super(parent, SWT.NONE);

        setBackground(Colors.getColor(249, 250, 250));
        setLayout(new FormLayout());

        createColorsAndFonts(parent);
        registerListeners();
    }

    public void select(Entry entry)
    {
        Entry oldSelection = selection;
        selection = entry;

        if (oldSelection != null && oldSelection.item != null)
            oldSelection.item.redraw();

        if (selection.item != null)
            selection.item.redraw();

        entry.action.run();
    }

    public Entry selectById(String id)
    {
        for (Entry entry : entries)
        {
            if (id.equals(entry.getId()))
            {
                select(entry);
                return entry;
            }
        }

        return null;
    }

    public List<Entry> getEntries()
    {
        return entries;
    }

    //
    // listener implementations
    //

    private void registerListeners()
    {
        addDisposeListener(e -> Sidebar.this.widgetDisposed());

        addKeyListener(KeyListener.keyPressedAdapter(Sidebar.this::keyPressed));

        addTraverseListener(Sidebar.this::keyTraversed);
    }

    private void widgetDisposed()
    {
        regularFont.dispose();
        boldFont.dispose();
        sectionFont.dispose();
    }

    private void keyPressed(KeyEvent e)
    {
        if (selection != null && (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN))
        {
            Entry entry = selection.findNeighbor(e.keyCode);
            while (entry != null && !entry.isSelectable())
                entry = entry.findNeighbor(e.keyCode);

            if (entry != null)
                select(entry);
        }
        else
        {
            e.doit = false;
        }
    }

    private void keyTraversed(TraverseEvent e)
    {
        if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS)
        {
            e.doit = true;
        }
    }

    //
    // item implementation
    //

    private void createColorsAndFonts(Composite parent)
    {
        FontData fontData = parent.getFont().getFontData()[0];
        regularFont = new Font(Display.getDefault(), fontData);

        fontData.setStyle(SWT.BOLD);
        boldFont = new Font(Display.getDefault(), fontData);

        if (Platform.OS_MACOSX.equals(Platform.getOS()))
            fontData.setHeight(fontData.getHeight() + 1);

        sectionFont = new Font(Display.getDefault(), fontData);
    }

    private Item createItem(int index, Entry entry, String label, int indent)
    {
        entries.add(index, entry);

        Item l = new Item(this, entry);
        l.setText(label);
        l.setIndent(indent);

        setLayoutData(index, l);

        if (index + 1 < entries.size())
        {
            // cannot use #setLayoutData because entry has no item reference yet
            Item item = entries.get(index + 1).item;
            FormData data = (FormData) item.getLayoutData();
            data.top = new FormAttachment(l, item.indent == 0 ? 20 : 0);
        }

        this.setTabList(new Control[0]);

        return l;
    }

    private void setLayoutData(int index, Item item)
    {
        FormData data = new FormData();
        data.left = new FormAttachment(0);
        data.right = new FormAttachment(100);
        int indent = item.indent == 0 ? 20 : 0;
        data.top = index == 0 ? new FormAttachment(0, 5) : new FormAttachment(entries.get(index - 1).item, indent);
        item.setLayoutData(data);
    }

    private void action(Entry entry)
    {
        if (entry.action != null)
            entry.action.run();
    }

    //
    // item widget
    //

    private class Item extends Canvas
    {
        private static final int MARGIN_X = 6;
        private static final int MARGIN_Y = 4;

        private final Entry entry;

        private int indent;
        private String text;
        private Image image;

        private Menu contextMenu;

        private boolean isDragTarget;

        public Item(Composite parent, Entry entry)
        {
            super(parent, SWT.NO_BACKGROUND | SWT.NO_FOCUS);
            this.entry = entry;

            addDisposeListener(e -> Item.this.widgetDisposed());

            addPaintListener(Item.this::paintControl);

            addKeyListener(KeyListener.keyPressedAdapter(e -> {
                if (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN)
                    Sidebar.this.keyPressed(e);
                else
                    e.doit = false;
            }));

            addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseDown(MouseEvent event)
                {
                    if (event.button == 1)
                    {
                        if (indent > 0)
                        {
                            Sidebar.this.select(Item.this.entry);
                        }
                        else if (indent == 0 && Item.this.entry.action != null)
                        {
                            boolean doIt = true;

                            if (image != null)
                            {
                                Rectangle clientArea = getClientArea();
                                Rectangle imgBounds = image.getBounds();

                                doIt = (event.x >= clientArea.width - imgBounds.width - MARGIN_X)
                                                && (event.x <= clientArea.width - MARGIN_X);
                            }

                            if (doIt)
                                Sidebar.this.action(Item.this.entry);
                        }
                    }
                }
            });
        }

        public void addContextMenu(final MenuListener listener)
        {
            if (contextMenu != null)
                contextMenu.dispose();

            MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
            menuMgr.setRemoveAllWhenShown(true);
            menuMgr.addMenuListener(m -> listener.menuAboutToShow(Item.this.entry, m));

            contextMenu = menuMgr.createContextMenu(this);
            setMenu(contextMenu);
        }

        private void widgetDisposed()
        {
            if (image != null)
                image.dispose();

            if (contextMenu != null)
                contextMenu.dispose();
        }

        public void setIsDragTarget(boolean isDragTarget)
        {
            this.isDragTarget = isDragTarget;
        }

        public void setIndent(int indent)
        {
            this.indent = indent;
        }

        public void setText(String text)
        {
            this.text = text;
        }

        public void setImage(Image image)
        {
            this.image = image;
        }

        @Override
        public Point computeSize(int wHint, int hHint, boolean changed)
        {
            int width = 0;
            int height = 0;
            if (text != null)
            {
                GC gc = new GC(this);
                Point extent = gc.stringExtent(text);
                gc.dispose();
                width += extent.x;
                height = Math.max(height, extent.y);
            }
            if (image != null)
            {
                width += image.getBounds().width + 5;
            }
            return new Point(width + (2 * MARGIN_X) + indent, height + (2 * MARGIN_Y));
        }

        private void paintControl(PaintEvent e)
        {
            GC gc = e.gc;

            Color oldBackground = gc.getBackground();
            Color oldForeground = gc.getForeground();

            Rectangle bounds = getClientArea();

            if (Sidebar.this.selection != null && this == Sidebar.this.selection.item)
            {
                gc.setBackground(Colors.SIDEBAR_BACKGROUND_SELECTED);
                gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);

                gc.setForeground(Colors.SIDEBAR_TEXT);
                gc.setFont(boldFont);
            }
            else
            {
                gc.setBackground(Colors.SIDEBAR_BACKGROUND);
                gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);

                if (indent > 0)
                {
                    gc.setForeground(Colors.SIDEBAR_TEXT);
                    gc.setFont(isDragTarget ? boldFont : regularFont);
                }
                else
                {
                    gc.setForeground(isDragTarget ? Colors.BLACK : Colors.SIDEBAR_TEXT);
                    gc.setFont(sectionFont);
                }
            }

            int x = bounds.x + MARGIN_X + indent;
            if (image != null)
            {
                Rectangle imgBounds = image.getBounds();
                // center image relative to text
                int offset = (imgBounds.height - gc.stringExtent(text).y) / 2;

                if (indent == 0)
                {
                    gc.drawImage(image, bounds.width - imgBounds.width - MARGIN_X, bounds.y + MARGIN_Y - offset);
                }
                else
                {
                    gc.drawImage(image, x, bounds.y + MARGIN_Y - offset);
                    x += imgBounds.width + 5;
                }
            }

            gc.drawText(text, x, bounds.y + MARGIN_Y, true);

            gc.setBackground(oldBackground);
            gc.setForeground(oldForeground);
        }
    }
}
