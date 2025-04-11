package name.abuchen.portfolio.ui.util.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.util.Colors;

public class PaginatedTable
{
    public interface LabelProvider<T>
    {
        String getText(T element);

        default Images getLeadingImage(T element)
        {
            return null;
        }

        default Images getTrailingImage(T element)
        {
            return null;
        }
    }

    public interface SelectionListener<T>
    {
        default void onSelection(T element)
        {
        }

        default void onDoubleClick(T element)
        {
        }
    }

    static class Level<T>
    {
        private List<T> elements = new ArrayList<>();

        private LabelProvider<T> labelProvider;
        private SelectionListener<T> selectionListener;

        private int page = 0;
        private T selectedItem;

        public Level(List<T> elements, LabelProvider<T> labelProvider, SelectionListener<T> selectionListener)
        {
            this.elements = elements;
            this.labelProvider = labelProvider;
            this.selectionListener = selectionListener;
        }
    }

    static class Element extends Composite
    {
        private StyledLabel label;
        private ImageHyperlink leadingImage;
        private ImageHyperlink trailingImage;

        public Element(Composite parent, int style)
        {
            super(parent, style);
            createControls();
        }

        private void createControls()
        {
            GridLayoutFactory.fillDefaults().numColumns(3).spacing(0, 0).applyTo(this);

            leadingImage = new ImageHyperlink(this, SWT.NONE);
            leadingImage.setEnabled(false);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(leadingImage);

            label = new StyledLabel(this, SWT.NONE);
            label.setText("\n"); //$NON-NLS-1$
            GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(label);

            trailingImage = new ImageHyperlink(this, SWT.NONE);
            trailingImage.setEnabled(false);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(trailingImage);
        }

        public void setText(String text)
        {
            label.setText(text);
        }

        public void setLeadingImage(Images image)
        {
            if (image != null)
            {
                leadingImage.setImage(image.image());
                leadingImage.setVisible(true);
            }
            else
            {
                leadingImage.setImage(null);
                leadingImage.setVisible(false);
            }
        }

        public void setTrailingImage(Images image)
        {
            if (image != null)
            {
                trailingImage.setImage(image.image());
                trailingImage.setVisible(true);
            }
            else
            {
                trailingImage.setImage(null);
                trailingImage.setVisible(false);
            }
        }

        @Override
        public void setBackground(Color color)
        {
            super.setBackground(color);

            label.setBackground(color);
            leadingImage.setBackground(color);
            trailingImage.setBackground(color);
        }

        @Override
        public void addListener(int eventType, Listener listener)
        {
            super.addListener(eventType, listener);

            if (eventType == SWT.MouseDown || eventType == SWT.MouseDoubleClick)
            {
                label.addListener(eventType, listener);
                leadingImage.addListener(eventType, listener);
                trailingImage.addListener(eventType, listener);
            }
        }

        @Override
        public void setData(Object data)
        {
            super.setData(data);

            label.setData(data);
            leadingImage.setData(data);
            trailingImage.setData(data);
        }
    }

    private static final int PAGE_SIZE = 10;

    private final Color selectionColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION);

    private List<Element> pageControls = new ArrayList<>();
    private CLabel pageLabel;

    private List<Level<?>> stack = new ArrayList<>();

    public PaginatedTable()
    {
        SelectionListener<Object> selectionListener = new SelectionListener<Object>()
        {
        };
        stack.add(new Level<>(new ArrayList<>(), Object::toString, selectionListener));
    }

    public <T> void setInput(List<T> elements, LabelProvider<T> labelProvider, SelectionListener<T> selectionListener)
    {
        stack.clear();
        stack.add(new Level<>(elements, labelProvider, selectionListener));

        renderPage();
    }

    public <T> void pushInput(List<T> elements, LabelProvider<T> labelProvider, SelectionListener<T> selectionListener)
    {
        stack.add(new Level<>(elements, labelProvider, selectionListener));

        // selected first element
        @SuppressWarnings("unchecked")
        var level = (Level<Object>) stack.getLast();
        level.selectedItem = level.elements.get(0);
        level.selectionListener.onSelection(level.selectedItem);

        renderPage();

        pageControls.get(0).forceFocus();
    }

    public void popInput()
    {
        if (!stack.isEmpty())
            stack.removeLast();

        renderPage();

        // focus the previous selected item
        @SuppressWarnings("unchecked")
        var level = (Level<Object>) stack.getLast();
        if (level.selectedItem != null)
        {
            updateSelectionColor();

            for (Element element : pageControls)
            {
                var item = element.getData();
                var isSelected = level.selectedItem == item;
                if (isSelected)
                    element.forceFocus();
            }
        }
    }

    public Control createViewControl(Composite parent)
    {
        var container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

        var table = new Composite(container, SWT.BORDER);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(table);

        table.setBackground(Colors.theme().defaultBackground());

        for (int ii = 0; ii < PAGE_SIZE; ii++)
        {
            Element element = createElement(table);
            pageControls.add(element);
        }

        addPageNavigator(container);

        return container;
    }

    private Element createElement(Composite table)
    {
        Element element = new Element(table, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(element);
        element.setBackground(table.getBackground());
        element.setText("\n"); //$NON-NLS-1$

        element.addListener(SWT.MouseDown, event -> {
            var item = event.widget.getData();
            if (item == null)
                return;

            @SuppressWarnings("unchecked")
            var level = (Level<Object>) stack.getLast();

            level.selectedItem = item;
            updateSelectionColor();
            element.forceFocus();

            level.selectionListener.onSelection(item);
        });

        element.addListener(SWT.MouseDoubleClick, event -> {
            var item = event.widget.getData();
            if (item == null)
                return;

            @SuppressWarnings("unchecked")
            var level = (Level<Object>) stack.getLast();

            level.selectedItem = item;
            updateSelectionColor();
            element.forceFocus();

            level.selectionListener.onDoubleClick(item);
        });

        element.addListener(SWT.Traverse, e -> {
            switch (e.detail)
            {
                case SWT.TRAVERSE_ESCAPE, SWT.TRAVERSE_RETURN, SWT.TRAVERSE_TAB_NEXT, SWT.TRAVERSE_TAB_PREVIOUS, SWT.TRAVERSE_PAGE_NEXT, SWT.TRAVERSE_PAGE_PREVIOUS:
                    e.doit = true;
                    break;
                default:
            }
        });

        element.addListener(SWT.FocusIn, e -> {
            var item = e.widget.getData();
            if (item == null)
            {
                e.doit = false;
                return;
            }

            @SuppressWarnings("unchecked")
            var level = (Level<Object>) stack.getLast();

            if (level.selectedItem != null && level.selectedItem == item)
                return;

            level.selectedItem = item;
            level.selectionListener.onSelection(item);
            updateSelectionColor();
        });

        element.addListener(SWT.KeyDown, e -> {
            if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR || e.keyCode == SWT.ARROW_RIGHT
                            || e.keyCode == SWT.ARROW_LEFT)
            {
                var item = e.widget.getData();
                if (item != null)
                {
                    @SuppressWarnings("unchecked")
                    var level = (Level<Object>) stack.getLast();
                    level.selectedItem = item;
                    updateSelectionColor();

                    level.selectionListener.onDoubleClick(item);
                }
            }
            else if (e.keyCode == SWT.ARROW_UP)
            {
                var nextControl = element.getParent().getDisplay().getFocusControl()
                                .traverse(SWT.TRAVERSE_TAB_PREVIOUS);
                if (!nextControl)
                    e.doit = true;
            }
            else if (e.keyCode == SWT.ARROW_DOWN)
            {
                var nextControl = element.getParent().getDisplay().getFocusControl().traverse(SWT.TRAVERSE_TAB_NEXT);

                if (!nextControl)
                    e.doit = true;
            }
        });

        // prevent the element from triggering the default button
        element.addTraverseListener(e -> {
            if (e.detail == SWT.TRAVERSE_RETURN)
                e.doit = false;
        });

        return element;
    }

    private void addPageNavigator(Composite container)
    {
        var buttons = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).applyTo(buttons);
        var layout = new RowLayout(SWT.HORIZONTAL);
        layout.fill = true;
        buttons.setLayout(layout);

        var prevButton = new Button(buttons, SWT.PUSH);
        prevButton.setText("<"); //$NON-NLS-1$
        prevButton.addSelectionListener(org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter(e -> {
            var page = stack.getLast().page;
            if (page == 0)
                return;

            stack.getLast().page = Math.max(0, page - 1);
            renderPage();
        }));

        pageLabel = new CLabel(buttons, SWT.CENTER);
        pageLabel.setText(""); //$NON-NLS-1$
        pageLabel.setLayoutData(new RowData(100, SWT.DEFAULT));

        var nextButton = new Button(buttons, SWT.PUSH);
        nextButton.setText(">"); //$NON-NLS-1$
        nextButton.addSelectionListener(org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter(e -> {
            var size = stack.getLast().elements.size();
            if (size == 0)
                return;

            var numPages = size / PAGE_SIZE + (size % PAGE_SIZE == 0 ? 0 : 1);
            stack.getLast().page = Math.min(numPages - 1, stack.getLast().page + 1);
            renderPage();
        }));
    }

    private void renderPage()
    {
        final int index = stack.getLast().page * PAGE_SIZE;

        for (int ii = 0; ii < PAGE_SIZE; ii++)
        {
            Element element = pageControls.get(ii);
            if (index + ii < stack.getLast().elements.size())
            {
                @SuppressWarnings("unchecked")
                var level = (Level<Object>) stack.getLast();
                var item = level.elements.get(index + ii);

                element.setText(level.labelProvider.getText(item));
                element.setLeadingImage(level.labelProvider.getLeadingImage(item));
                element.setTrailingImage(level.labelProvider.getTrailingImage(item));
                element.setData(item);
                element.layout();
            }
            else
            {
                element.setText("\n"); //$NON-NLS-1$
                element.setLeadingImage(null);
                element.setTrailingImage(null);
                element.setData(null);
                element.layout();
            }
        }

        updateSelectionColor();

        pageControls.getFirst().getParent()
                        .setTabList(pageControls
                                        .subList(0, Math.min(PAGE_SIZE, stack.getLast().elements.size() - index))
                                        .toArray(new Element[0]));

        int numPages = stack.getLast().elements.size() / PAGE_SIZE
                        + (stack.getLast().elements.size() % PAGE_SIZE == 0 ? 0 : 1);
        pageLabel.setText(String.valueOf(stack.getLast().page + 1) + "/" + numPages); //$NON-NLS-1$

        pageControls.getFirst().getParent().layout(true);
    }

    private void updateSelectionColor()
    {
        @SuppressWarnings("unchecked")
        var level = (Level<Object>) stack.getLast();

        for (Element element : pageControls)
        {
            var item = element.getData();
            var isSelected = level.selectedItem != null && level.selectedItem == item;
            var background = isSelected ? selectionColor : element.getParent().getBackground();
            element.setBackground(background);
            element.label.setForeground(Colors.getTextColor(background));
        }

    }
}
