package name.abuchen.portfolio.ui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;

public final class Sidebar<I> extends Composite
{
    public interface Model<I>
    {
        Stream<I> getElements();

        Stream<I> getChildren(I item);

        String getLabel(I item);

        Optional<Images> getImage(I item);

        void select(I item);

        IMenuListener getActionMenu(I item);

        IMenuListener getContextMenu(I item);
    }

    public static final int STEP = 10;

    private Color selectionForeground = Colors.SIDEBAR_TEXT;
    private Color selectionBackground = Colors.SIDEBAR_BACKGROUND_SELECTED;

    private Font boldFont;
    private Font sectionFont;

    private final Model<I> model;

    private List<Entry> entries = new ArrayList<>();

    private Entry selection = null;
    private Entry focus = null;

    public Sidebar(Composite parent, Model<I> model)
    {
        super(parent, SWT.NONE);
        this.model = model;

        this.setData(UIConstants.CSS.CLASS_NAME, "sidebar"); //$NON-NLS-1$

        setLayout(new FormLayout());
        setFont(getFont());

        registerListeners();

        rebuild();
    }

    public void rebuild()
    {
        List<Entry> existingList = new ArrayList<>(entries);
        List<Entry> targetList = new ArrayList<>();

        model.getElements().forEach(item -> build(0, item, existingList, targetList));

        existingList.forEach(Entry::dispose);

        if (selection != null && selection.isDisposed())
            selection = null;

        this.entries = targetList;
    }

    private void build(int indent, I item, List<Entry> existingList, List<Entry> targetList)
    {
        Entry entry = existingList.stream().filter(i -> item.equals(i.getSubject())).findAny()
                        .orElseGet(() -> new Entry(this, item));

        entry.setIndent(indent);

        IMenuListener contextMenu = model.getContextMenu(item);
        if (contextMenu != null)
            entry.addContextMenu(contextMenu);

        FormData data = new FormData();
        data.left = new FormAttachment(0);
        data.right = new FormAttachment(100);
        int offset = indent == 0 ? 20 : 0;
        data.top = targetList.isEmpty() ? new FormAttachment(0, 5)
                        : new FormAttachment(targetList.get(targetList.size() - 1), offset);
        entry.setLayoutData(data);
        entry.redraw();

        existingList.remove(entry);
        targetList.add(entry);

        model.getChildren(item).forEach(child -> build(indent + STEP, child, existingList, targetList));
    }

    public void select(I subject)
    {
        Entry previous = selection;

        if (subject == null)
            selection = null;
        else
            selection = entries.stream().filter(e -> e.getSubject().equals(subject)).findAny().orElse(null);

        if (previous != null && !previous.isDisposed())
            previous.redraw();
        if (selection != null)
            selection.redraw();
    }

    public void addDropSupport(I subject, int operations, Transfer[] transferTypes, DropTargetListener listener)
    {
        Optional<Entry> entry = entries.stream().filter(e -> e.getSubject().equals(subject)).findAny();
        if (!entry.isPresent())
            return;

        Entry target = entry.get();

        if (target.getData(DND.DROP_TARGET_KEY) != null)
            return;

        DropTarget dropTarget = new DropTarget(target, operations);
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
                target.setIsDragTarget(false);
                target.redraw();
            }

            @Override
            public void dragEnter(DropTargetEvent event)
            {
                listener.dragEnter(event);
                target.setIsDragTarget(true);
                target.redraw();
            }
        });
    }

    //
    // listener implementations
    //

    private void registerListeners()
    {
        addDisposeListener(e -> Sidebar.this.widgetDisposed());

        addKeyListener(KeyListener.keyPressedAdapter(Sidebar.this::keyPressed));

        addTraverseListener(event -> event.doit = true);

        addFocusListener(new FocusListener()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                Entry previous = focus;
                focus = null;
                if (previous != null)
                    previous.redraw();
            }

            @Override
            public void focusGained(FocusEvent e)
            {
                focus = selection != null ? selection : entries.get(0);
                focus.redraw();
            }
        });
    }

    private void widgetDisposed()
    {
        boldFont.dispose();
        sectionFont.dispose();
    }

    private void keyPressed(KeyEvent e)
    {
        if (focus != null && (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN))
        {
            int pos = this.entries.indexOf(focus);
            pos = pos + (e.keyCode == SWT.ARROW_UP ? -1 : 1);

            if (pos >= 0 && pos < entries.size())
            {
                Entry previous = focus;

                focus = entries.get(pos);

                previous.redraw();
                focus.redraw();
            }
        }
        else if (focus != null && e.keyCode == SWT.SPACE)
        {
            model.select(focus.getSubject());
        }
        else
        {
            e.doit = false;
        }
    }

    @Override
    public void setFont(Font font)
    {
        super.setFont(font);

        if (boldFont != null && !boldFont.isDisposed())
            boldFont.dispose();
        if (sectionFont != null && !sectionFont.isDisposed())
            sectionFont.dispose();

        FontData fontData = font.getFontData()[0];
        fontData.setStyle(SWT.BOLD);
        boldFont = new Font(Display.getDefault(), fontData);

        if (Platform.OS_MACOSX.equals(Platform.getOS()))
            fontData.setHeight(fontData.getHeight() + 1);

        sectionFont = new Font(Display.getDefault(), fontData);
    }

    public Color getSelectionForeground()
    {
        return selectionForeground;
    }

    public void setSelectionForeground(Color selectionForeground)
    {
        this.selectionForeground = selectionForeground;
    }

    public Color getSelectionBackground()
    {
        return selectionBackground;
    }

    public void setSelectionBackground(Color selectionBackground)
    {
        this.selectionBackground = selectionBackground;
    }

    //
    // item widget
    //

    private class Entry extends Canvas // NOSONAR
    {
        private static final int MARGIN_X = 6;
        private static final int MARGIN_Y = 4;

        private final I subject;

        private int indent;

        private Image image;
        private Menu contextMenu;
        private Menu actionMenu;

        private boolean isDragTarget;

        public Entry(Composite parent, I subject)
        {
            super(parent, SWT.NO_FOCUS);
            this.subject = Objects.requireNonNull(subject);

            this.image = model.getImage(subject).map(Images::image).orElse(null);
            if (this.image == null && model.getActionMenu(subject) != null)
                this.image = Images.PLUS.image();

            addDisposeListener(Entry.this::handleDispose);
            addPaintListener(Entry.this::handlePaint);
            addMouseListener(MouseListener.mouseDownAdapter(Entry.this::handleMouseDown));
        }

        public I getSubject()
        {
            return subject;
        }

        public void addContextMenu(IMenuListener listener)
        {
            if (contextMenu != null)
                contextMenu.dispose();

            MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
            menuMgr.setRemoveAllWhenShown(true);
            menuMgr.addMenuListener(listener);

            contextMenu = menuMgr.createContextMenu(this);
            setMenu(contextMenu);
        }

        public void setIsDragTarget(boolean isDragTarget)
        {
            this.isDragTarget = isDragTarget;
        }

        public void setIndent(int indent)
        {
            this.indent = indent;
        }

        private void showMenu(IMenuListener listener)
        {
            if (actionMenu != null)
                actionMenu.dispose();

            MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
            menuMgr.setRemoveAllWhenShown(true);
            menuMgr.addMenuListener(listener);

            actionMenu = menuMgr.createContextMenu(this);
            actionMenu.setVisible(true);
        }

        private void handleDispose(DisposeEvent event) // NOSONAR
        {
            if (contextMenu != null)
                contextMenu.dispose();
            if (actionMenu != null)
                actionMenu.dispose();
        }

        private void handleMouseDown(MouseEvent event)
        {
            if (event.button == 1)
            {
                IMenuListener action = model.getActionMenu(subject);
                if (action != null)
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
                    {
                        showMenu(action);
                        return;
                    }
                }

                model.select(subject);
            }
        }

        @Override
        public Point computeSize(int wHint, int hHint, boolean changed)
        {
            int height = 0;

            String label = model.getLabel(subject);
            if (label != null)
            {
                GC gc = new GC(this);
                gc.setFont(Sidebar.this.getFont());
                Point extent = gc.stringExtent(label);
                gc.dispose();
                height = Math.max(height, extent.y);
            }

            return new Point(wHint, height + (2 * MARGIN_Y));
        }

        private void handlePaint(PaintEvent e)
        {
            GC gc = e.gc;

            Color oldBackground = gc.getBackground();
            Color oldForeground = gc.getForeground();

            Rectangle bounds = getClientArea();

            boolean hasFocus = Objects.equals(focus, this);

            if (Objects.equals(Sidebar.this.selection, this))
            {
                gc.setBackground(Sidebar.this.selectionBackground);
                gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);

                gc.setForeground(Sidebar.this.selectionForeground);
                gc.setFont(boldFont);
            }
            else
            {
                gc.setBackground(Sidebar.this.getBackground());
                gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);

                if (indent > 0)
                {
                    gc.setForeground(Sidebar.this.getForeground());
                    gc.setFont(isDragTarget ? boldFont : Sidebar.this.getFont());
                }
                else
                {
                    gc.setForeground(isDragTarget ? Colors.ICON_ORANGE : Sidebar.this.getForeground());
                    gc.setFont(sectionFont);
                }
            }

            String label = model.getLabel(subject);

            int x = bounds.x + MARGIN_X + indent;
            int width = bounds.width;

            if (image != null)
            {
                Rectangle imgBounds = image.getBounds();
                // center image relative to text
                int offset = (imgBounds.height - gc.stringExtent(label).y) / 2;

                if (indent == 0)
                {
                    gc.drawImage(image, bounds.width - imgBounds.width - MARGIN_X, bounds.y + MARGIN_Y - offset);
                    width = width - imgBounds.width - MARGIN_X - 1;
                }
                else
                {
                    gc.drawImage(image, x, bounds.y + MARGIN_Y - offset);
                    x += imgBounds.width + 5;
                }
            }

            gc.setClipping(new Rectangle(bounds.x, bounds.y, width, bounds.height));

            gc.drawText(label, x, bounds.y + MARGIN_Y, true);

            if (hasFocus)
            {
                Point extent = gc.textExtent(label);
                gc.drawLine(x - 1, bounds.y + MARGIN_Y + extent.y - 1, x + extent.x - 1,
                                bounds.y + MARGIN_Y + extent.y - 1);
            }

            gc.setClipping((Rectangle) null);

            gc.setBackground(oldBackground);
            gc.setForeground(oldForeground);
        }
    }
}
