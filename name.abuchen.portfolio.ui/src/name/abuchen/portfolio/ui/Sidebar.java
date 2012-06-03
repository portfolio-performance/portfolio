package name.abuchen.portfolio.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
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

public class Sidebar extends Composite
{
    private Color backgroundColor;
    private Color selectedColor;
    private Color lighterSelectedColor;
    private Color sectionColor;

    private Font regularFont;
    private Font boldFont;
    private Font sectionFont;

    private List<Item> items = new ArrayList<Item>();
    private Item selection = null;

    public Sidebar(Composite parent, int style)
    {
        super(parent, style);

        setLayout(new FormLayout());

        createColorsAndFonts(parent);
        registerListeners();
    }

    public void addSection(String label)
    {
        addItem(label.toUpperCase(), 0, null);
    }

    public void addItem(Action action)
    {
        addItem(action.getText(), 15, action);
    }

    public void addSubItem(Action action)
    {
        addItem(action.getText(), 30, action);
    }

    public void select(int index)
    {
        if (index < 0 || index >= items.size())
            throw new ArrayIndexOutOfBoundsException(index);

        select(items.get(index));
    }

    //
    // listener implementations
    //

    private void registerListeners()
    {
        addDisposeListener(new DisposeListener()
        {
            public void widgetDisposed(DisposeEvent e)
            {
                Sidebar.this.widgetDisposed(e);
            }
        });

        addPaintListener(new PaintListener()
        {
            public void paintControl(PaintEvent e)
            {
                Sidebar.this.paintControl(e);
            }
        });

        addKeyListener(new KeyListener()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {}

            @Override
            public void keyPressed(KeyEvent e)
            {
                Sidebar.this.keyPressed(e);
            }
        });

        addTraverseListener(new TraverseListener()
        {
            @Override
            public void keyTraversed(TraverseEvent e)
            {
                Sidebar.this.keyTraversed(e);
            }
        });
    }

    private void widgetDisposed(DisposeEvent e)
    {
        backgroundColor.dispose();
        selectedColor.dispose();
        lighterSelectedColor.dispose();
        sectionColor.dispose();

        regularFont.dispose();
        boldFont.dispose();
        sectionFont.dispose();
    }

    private void paintControl(PaintEvent e)
    {
        GC gc = e.gc;
        gc.setForeground(backgroundColor);
        gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        Rectangle r = getClientArea();
        gc.fillGradientRectangle(0, 0, r.width, r.height, false);
    }

    private void keyPressed(KeyEvent e)
    {
        if (e.keyCode == SWT.ARROW_UP)
        {
            int index = items.indexOf(selection);

            if (index == -1)
                index = items.size();

            index--;
            while (index >= 0)
            {
                Item item = items.get(index);
                if (item.getAction() != null)
                {
                    select(item);
                    break;
                }
                index--;
            }
        }
        else if (e.keyCode == SWT.ARROW_DOWN)
        {
            int index = items.indexOf(selection);

            index++;
            while (index < items.size())
            {
                Item item = items.get(index);
                if (item.getAction() != null)
                {
                    select(item);
                    break;
                }
                index++;
            }
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
        backgroundColor = new Color(null, 225, 231, 245);
        // selectedColor = new Color(null, 126, 124, 42);
        selectedColor = new Color(null, 115, 158, 227);
        // lighterSelectedColor = new Color(null, 168, 165, 55);
        lighterSelectedColor = new Color(null, 189, 208, 241);
        sectionColor = new Color(null, 137, 124, 130);

        FontData fontData = parent.getFont().getFontData()[0];
        regularFont = new Font(Display.getDefault(), fontData);
        fontData.setStyle(SWT.BOLD);
        boldFont = new Font(Display.getDefault(), fontData);

        if (!Platform.OS_MACOSX.equals(Platform.getOS()))
            fontData.setHeight(fontData.getHeight() - 1);

        sectionFont = new Font(Display.getDefault(), fontData);
    }

    private void addItem(String label, int indent, Action action)
    {
        Item l = new Item(this);
        l.setText(label);
        l.setIndent(indent);
        l.setAction(action);

        if (action != null && action.getImageDescriptor() != null)
            l.setImage(action.getImageDescriptor().createImage(true));

        if (items.isEmpty())
        {
            FormData data = new FormData();
            data.top = new FormAttachment(0, 5);
            data.left = new FormAttachment(0);
            data.right = new FormAttachment(100);
            l.setLayoutData(data);
        }
        else
        {
            Control previous = items.get(items.size() - 1);
            FormData data = new FormData();
            data.top = new FormAttachment(previous, indent == 0 ? 20 : 0);
            data.left = new FormAttachment(0);
            data.right = new FormAttachment(100);
            l.setLayoutData(data);
        }

        items.add(l);
    }

    private void select(Item item)
    {
        if (item.getAction() != null)
        {
            Item oldSelection = selection;
            selection = item;

            if (oldSelection != null)
                oldSelection.redraw();
            selection.redraw();

            item.getAction().run();
        }
    }

    //
    // item widget
    //

    private class Item extends Canvas
    {
        final static int marginX = 5;
        final static int marginY = 3;

        private int indent;
        private String text;
        private Image image;
        private Action action;

        public Item(Composite parent)
        {
            super(parent, SWT.NO_BACKGROUND);

            addDisposeListener(new DisposeListener()
            {
                public void widgetDisposed(DisposeEvent e)
                {
                    Item.this.widgetDisposed(e);
                }
            });

            addPaintListener(new PaintListener()
            {
                public void paintControl(PaintEvent e)
                {
                    Item.this.paintControl(e);
                }
            });

            addMouseListener(new MouseAdapter()
            {
                public void mouseDown(MouseEvent event)
                {
                    if (event.button == 1)
                    {
                        Sidebar.this.select(Item.this);
                    }
                }
            });
        }

        private void widgetDisposed(DisposeEvent e)
        {
            if (image != null)
                image.dispose();
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

        public Action getAction()
        {
            return action;
        }

        public void setAction(Action action)
        {
            this.action = action;
        }

        public Point computeSize(int wHint, int hHint, boolean changed)
        {
            int width = 0, height = 0;
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
            return new Point(width + 2 + (2 * marginX) + indent, height + 2 + (2 * marginY));
        }

        private void paintControl(PaintEvent e)
        {
            GC gc = e.gc;

            Color oldBackground = gc.getBackground();
            Color oldForeground = gc.getForeground();

            Rectangle bounds = getClientArea();

            if (this == Sidebar.this.selection)
            {
                gc.setForeground(lighterSelectedColor);
                gc.setBackground(selectedColor);
                gc.fillGradientRectangle(bounds.x, bounds.y, bounds.width, bounds.height, true);

                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
                gc.setFont(boldFont);
            }
            else
            {
                gc.setForeground(backgroundColor);
                gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
                gc.fillGradientRectangle(bounds.x, bounds.y, bounds.width, bounds.height, false);

                if (indent > 0)
                {
                    gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
                    gc.setFont(regularFont);
                }
                else
                {
                    gc.setForeground(sectionColor);
                    gc.setFont(sectionFont);
                }
            }

            int x = bounds.x + marginX + indent;
            if (image != null)
            {
                gc.drawImage(image, x, bounds.y + marginY);
                x += image.getBounds().width + 5;
            }

            gc.drawText(text, x, bounds.y + marginY, true);

            gc.setBackground(oldBackground);
            gc.setForeground(oldForeground);
        }
    }
}
