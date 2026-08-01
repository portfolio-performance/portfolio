package name.abuchen.portfolio.ui.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.ToolBar;

/**
 * A layout manager that adaptively allocates space between title, view toolbar,
 * and action toolbar. Priority order:
 * <ol>
 * <li>Action toolbar - always gets full preferred width (highest priority)</li>
 * <li>View toolbar minimum - always shows at least 1 item + chevron</li>
 * <li>Title - gets remaining space, truncated with ellipsis if needed</li>
 * <li>View toolbar expansion - expands into unused title space</li>
 * </ol>
 */
public class AdaptiveHeaderLayout extends Layout
{
    public static final String KEY_ORIGINAL_TITLE = "originalTitle"; //$NON-NLS-1$

    private static final int HORIZONTAL_SPACING = 5;
    private static final int VERTICAL_MARGIN = 5;
    private static final String ELLIPSIS = "…"; //$NON-NLS-1$

    private final int marginWidth;
    private final int marginHeight;

    public AdaptiveHeaderLayout()
    {
        this(HORIZONTAL_SPACING, VERTICAL_MARGIN);
    }

    public AdaptiveHeaderLayout(int marginWidth, int marginHeight)
    {
        this.marginWidth = marginWidth;
        this.marginHeight = marginHeight;
    }

    @Override
    protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache)
    {
        Control[] children = composite.getChildren();
        if (children.length != 3)
            throw new IllegalArgumentException(
                            "AdaptiveHeaderLayout expects exactly 3 children: title, viewToolbarWrapper, actionToolbar"); //$NON-NLS-1$

        var titleLabel = (Label) children[0];
        var viewToolbarWrapper = (Composite) children[1];
        var actionToolbar = (ToolBar) children[2];

        // Calculate preferred sizes
        var titleSize = titleLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
        var viewSize = viewToolbarWrapper.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
        var actionSize = actionToolbar.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);

        // Width is sum of all preferred widths plus margins
        var width = titleSize.x + viewSize.x + actionSize.x + 2 * marginWidth + 2 * HORIZONTAL_SPACING;

        // Height is maximum of all heights plus margins
        var height = Math.max(titleSize.y, Math.max(viewSize.y, actionSize.y)) + 2 * marginHeight;

        if (wHint != SWT.DEFAULT)
            width = Math.min(width, wHint);
        if (hHint != SWT.DEFAULT)
            height = Math.min(height, hHint);

        return new Point(width, height);
    }

    @Override
    protected void layout(Composite composite, boolean flushCache)
    {
        Control[] children = composite.getChildren();
        if (children.length != 3)
            throw new IllegalArgumentException(
                            "AdaptiveHeaderLayout expects exactly 3 children: title, viewToolbarWrapper, actionToolbar"); //$NON-NLS-1$

        var titleLabel = (Label) children[0];
        var viewToolbarWrapper = (Composite) children[1];
        var actionToolbar = (ToolBar) children[2];

        var bounds = composite.getClientArea();
        var availableWidth = bounds.width - 2 * marginWidth;
        var availableHeight = bounds.height - 2 * marginHeight;

        // Step 1: Reserve space for action toolbar (always gets full preferred
        // width)
        var actionSize = actionToolbar.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
        int actionWidth = actionSize.x;

        // Step 2: Calculate minimum space needed for view toolbar (1 item +
        // chevron)
        int viewMinWidth = calculateMinViewToolbarWidth(viewToolbarWrapper);

        // Step 3: Calculate remaining space available for title
        int spacingTotal = 2 * HORIZONTAL_SPACING;
        int remainingWidth = availableWidth - actionWidth - viewMinWidth - spacingTotal;

        // Step 4: Calculate actual title width (may be truncated)
        int titleWidth = calculateTitleWidth(titleLabel, Math.max(0, remainingWidth), flushCache);

        // Step 5: Give any leftover space back to view toolbar
        int extraSpace = remainingWidth - titleWidth;
        int viewActualWidth = viewMinWidth + Math.max(0, extraSpace);

        // Step 6: Position all components
        int y = marginHeight + (availableHeight - actionSize.y) / 2;

        // title at left
        titleLabel.setBounds(marginWidth, y, titleWidth, actionSize.y);

        // Action toolbar at right
        int actionX = bounds.width - marginWidth - actionWidth;
        actionToolbar.setBounds(actionX, y, actionWidth, actionSize.y);

        // View toolbar between title and action toolbar (right-aligned within
        // its space)
        int viewX = actionX - HORIZONTAL_SPACING - viewActualWidth;
        viewToolbarWrapper.setBounds(viewX, marginHeight, viewActualWidth, availableHeight);

        // Update the view toolbar wrapper's layout with the actual available
        // width
        if (viewToolbarWrapper.getLayout() instanceof ToolBarPlusChevronLayout layout)
        {
            layout.setMaxWidth(viewActualWidth);
        }
    }

    private int calculateMinViewToolbarWidth(Composite viewToolbarWrapper)
    {
        // Find the toolbar inside the wrapper
        ToolBar toolBar = findToolBar(viewToolbarWrapper);
        if (toolBar == null)
            return 50; // Fallback minimum width

        // Get the first toolbar item's width + chevron space
        var items = toolBar.getItems();
        if (items.length == 0)
            return 50;

        int firstItemWidth = items[0].getBounds().width;
        if (firstItemWidth == 0)
        {
            // if bounds aren't computed yet, use a reasonable estimate
            firstItemWidth = 50;
        }

        // add space for chevron (approximately 16px + padding)
        return firstItemWidth + 20;
    }

    private int calculateTitleWidth(Label titleLabel, int availableWidth, boolean flushCache)
    {
        if (availableWidth <= 0)
            return 0;

        var originalText = getTitleText(titleLabel);

        var currentLabel = titleLabel.getText();
        if (currentLabel.endsWith(ELLIPSIS))
            titleLabel.setText(originalText);

        var preferredSize = titleLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);

        // if the preferred size fits, use it
        if (preferredSize.x <= availableWidth)
            return preferredSize.x;

        // Otherwise, we need to truncate the text
        if (originalText == null || originalText.isEmpty())
            return 0;

        // Measure text width and truncate if necessary
        GC gc = new GC(titleLabel);
        try
        {
            int ellipsisWidth = gc.textExtent(ELLIPSIS).x;
            int availableForText = availableWidth - ellipsisWidth;

            if (availableForText <= 0)
                return 0;

            var truncatedText = truncateText(gc, originalText, availableForText);
            var displayText = truncatedText.isEmpty() ? "" : truncatedText + ELLIPSIS; //$NON-NLS-1$

            if (!displayText.equals(titleLabel.getText()))
            {
                titleLabel.setText(displayText);
                titleLabel.setToolTipText(originalText);
            }

            return gc.textExtent(displayText).x;
        }
        finally
        {
            gc.dispose();
        }
    }

    private String getTitleText(Label titleLabel)
    {
        var originalTitle = titleLabel.getData(KEY_ORIGINAL_TITLE);
        if (originalTitle instanceof String s)
            return s;
        else
            return titleLabel.getText();
    }

    private String truncateText(GC gc, String text, int availableWidth)
    {
        if (availableWidth <= 0)
            return ""; //$NON-NLS-1$

        var textWidth = gc.textExtent(text).x;
        if (textWidth <= availableWidth)
            return text;

        // binary search for the longest substring that fits
        int left = 0;
        int right = text.length();
        var bestFit = ""; //$NON-NLS-1$

        while (left <= right)
        {
            int mid = (left + right) / 2;
            String candidate = text.substring(0, mid);
            int candidateWidth = gc.textExtent(candidate).x;

            if (candidateWidth <= availableWidth)
            {
                bestFit = candidate;
                left = mid + 1;
            }
            else
            {
                right = mid - 1;
            }
        }

        return bestFit;
    }

    private ToolBar findToolBar(Composite composite)
    {
        for (Control child : composite.getChildren())
        {
            if (child instanceof ToolBar toolBar)
                return toolBar;
        }
        return null;
    }
}
