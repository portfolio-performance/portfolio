package name.abuchen.portfolio.ui.util.swt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.DesktopAPI;

public class PDFViewer extends Composite
{
    private static final double ZOOM_STEP = 1.5;
    private static final double MAX_ZOOM = 5.0;
    private static final int MAX_CACHED_PAGES = 3;

    private final PDFInputFile inputFile;

    private Composite pdfViewComposite;
    private Canvas pdfCanvas;
    private Label pageLabel;
    private Button prevButton;
    private Button nextButton;
    private Button zoomInButton;
    private Button zoomOutButton;

    private Text textWidget;

    /** the text with highlighting, shown instead of textWidget on demand */
    private StyledText highlightWidget;
    private Composite textStack;
    private StackLayout textStackLayout;
    private Button highlightButton;

    /** the highlighted parts of the text as {start, length} */
    private final List<int[]> highlights = new ArrayList<>();
    private Text searchField;
    private Button searchPrevButton;
    private Button searchNextButton;
    private Label searchLabel;

    /**
     * start offsets of the matches of the current search term. They are kept
     * for both text widgets because the offsets differ: Text returns its
     * content with the line delimiter of the platform, StyledText does not.
     */
    private List<Integer> matches = new ArrayList<>();
    private List<Integer> highlightMatches = new ArrayList<>();
    private int currentMatch = -1;
    private Composite pageNavComposite;
    private int currentPageIndex = 0;
    private int totalPages = 0;

    // The displayed image is owned by pageCache, never disposed independently.
    // Invariant: the current page is always the most-recently-accessed entry
    // (every get/render put touches it), so the access-order LRU below can
    // never evict and dispose the image currentSwtImage points to. Anything
    // that reads a cache entry for display must keep this true — e.g. a future
    // prefetch of neighbouring pages must not get()/put() them after the
    // current page, or it could make the visible image the eldest entry.
    private Image currentSwtImage;

    private double zoomLevel = 1.0;

    private boolean initialized = false;
    private boolean loading = false;
    private boolean loadFailed = false;

    // Serializes page rendering: one render at a time, which bounds the number
    // of concurrent PDF opens. Daemon thread so it never blocks JVM shutdown.
    private final ExecutorService renderExecutor = Executors.newSingleThreadExecutor(r -> {
        var t = new Thread(r, "PDFViewer-render"); //$NON-NLS-1$
        t.setDaemon(true);
        return t;
    });

    // Keeps the PDF document open across page turns so each render does not
    // re-parse the file. Only ever touched on the renderExecutor thread.
    private PDFInputFile.PageRenderer renderer;
    private volatile boolean disposed = false;

    // Bumped by release(); a render result is only applied if the generation
    // still matches, so a render in flight when the page is left cannot
    // repopulate the cleared cache. EDT-only.
    private int renderGeneration = 0;

    private final Map<Integer, Image> pageCache = new LinkedHashMap<>(MAX_CACHED_PAGES + 1, 0.75f, true)
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Image> eldest)
        {
            if (size() > MAX_CACHED_PAGES)
            {
                // safe to dispose: the eldest is never the current page (see
                // the invariant on currentSwtImage)
                var img = eldest.getValue();
                if (img != null && !img.isDisposed())
                    img.dispose();
                return true;
            }
            return false;
        }
    };

    // drag-to-pan state
    private boolean dragging = false;
    private int dragStartX;
    private int dragStartY;
    private int scrollStartX;
    private int scrollStartY;

    public PDFViewer(Composite parent, int style, PDFInputFile inputFile)
    {
        super(parent, style);
        this.inputFile = inputFile;
        setLayout(new GridLayout(1, false));

        // File name header — click to open the document in the OS default
        // PDF viewer
        var fileLink = new Link(this, SWT.NONE);
        fileLink.setText("<a>" + inputFile.getName() + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
        fileLink.setToolTipText(inputFile.getFile().getAbsolutePath());
        fileLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        fileLink.addListener(SWT.Selection, e -> DesktopAPI.open(inputFile.getFile()));

        // Tab container for PDF/Text views
        var tabFolder = new TabFolder(this, SWT.NONE);
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // PDF view
        var pdfScrolled = new ScrolledComposite(tabFolder, SWT.H_SCROLL | SWT.V_SCROLL);
        pdfScrolled.setExpandHorizontal(true);
        pdfScrolled.setExpandVertical(true);
        pdfCanvas = new Canvas(pdfScrolled, SWT.DOUBLE_BUFFERED);
        pdfScrolled.setContent(pdfCanvas);
        pdfCanvas.addPaintListener(e -> {
            var canvasSize = pdfCanvas.getSize();

            if (loading || loadFailed || (currentSwtImage == null && initialized))
            {
                e.gc.setBackground(pdfCanvas.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
                e.gc.fillRectangle(0, 0, canvasSize.x, canvasSize.y);
                var text = loadFailed && !loading ? Messages.PDFImportWizardManualEntryPageRenderError
                                : Messages.StatusLoading;
                var extent = e.gc.textExtent(text);
                e.gc.drawText(text, (canvasSize.x - extent.x) / 2, (canvasSize.y - extent.y) / 2, true);
                return;
            }

            if (currentSwtImage != null && !currentSwtImage.isDisposed())
            {
                var imgBounds = currentSwtImage.getBounds();
                var scrolled = (ScrolledComposite) pdfViewComposite;
                var clientArea = scrolled.getClientArea();

                // compute fit scale (image fits viewport), then apply zoom
                double fitScaleX = (double) clientArea.width / imgBounds.width;
                double fitScaleY = (double) clientArea.height / imgBounds.height;
                double fitScale = Math.min(fitScaleX, fitScaleY);
                double scale = fitScale * zoomLevel;

                int destW = (int) (imgBounds.width * scale);
                int destH = (int) (imgBounds.height * scale);
                int destX = (canvasSize.x - destW) / 2;
                int destY = (canvasSize.y - destH) / 2;

                // fill background
                e.gc.setBackground(pdfCanvas.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
                e.gc.fillRectangle(0, 0, canvasSize.x, canvasSize.y);

                e.gc.setAntialias(SWT.ON);
                e.gc.setInterpolation(SWT.HIGH);
                e.gc.drawImage(currentSwtImage, 0, 0, imgBounds.width, imgBounds.height, destX, destY, destW, destH);
            }
        });

        // Double-click to zoom; left button = zoom in, right button = zoom out
        pdfCanvas.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseDoubleClick(MouseEvent e)
            {
                if (currentSwtImage == null || currentSwtImage.isDisposed())
                    return;

                if (e.button == 1)
                    zoomAtPoint(e.x, e.y, false);
                else if (e.button == 3)
                    zoomAtPoint(e.x, e.y, true);
            }

            @Override
            public void mouseDown(MouseEvent e)
            {
                if (zoomLevel > 1.0 && e.button == 1)
                {
                    dragging = true;
                    var cursorLoc = pdfCanvas.getDisplay().getCursorLocation();
                    dragStartX = cursorLoc.x;
                    dragStartY = cursorLoc.y;
                    var origin = ((ScrolledComposite) pdfViewComposite).getOrigin();
                    scrollStartX = origin.x;
                    scrollStartY = origin.y;
                }
            }

            @Override
            public void mouseUp(MouseEvent e)
            {
                dragging = false;
            }
        });

        // Press '0' to reset zoom
        pdfCanvas.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.character == '0')
                    resetZoom();
            }
        });

        pdfCanvas.addMouseMoveListener(e -> {
            if (dragging)
            {
                var cursorLoc = pdfCanvas.getDisplay().getCursorLocation();
                int deltaX = cursorLoc.x - dragStartX;
                int deltaY = cursorLoc.y - dragStartY;
                ((ScrolledComposite) pdfViewComposite).setOrigin(scrollStartX - deltaX, scrollStartY - deltaY);
            }
        });

        // Re-layout canvas when ScrolledComposite is resized
        pdfScrolled.addListener(SWT.Resize, e -> updateCanvasSize());
        pdfViewComposite = pdfScrolled;

        var pdfTab = new TabItem(tabFolder, SWT.NONE);
        pdfTab.setText(Messages.PDFImportWizardManualEntryPDFView);
        pdfTab.setControl(pdfScrolled);

        // Text view with a search field above the text
        var textComposite = new Composite(tabFolder, SWT.NONE);
        textComposite.setLayout(new GridLayout(1, false));

        var searchRow = new Composite(textComposite, SWT.NONE);
        searchRow.setLayout(new GridLayout(5, false));
        searchRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        searchField = new Text(searchRow, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
        searchField.setMessage(Messages.LabelSearch);
        searchField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        searchPrevButton = new Button(searchRow, SWT.PUSH);
        searchPrevButton.setText("<"); //$NON-NLS-1$
        searchPrevButton.setEnabled(false);
        searchPrevButton.addListener(SWT.Selection, e -> showMatch(currentMatch - 1));

        searchNextButton = new Button(searchRow, SWT.PUSH);
        searchNextButton.setText(">"); //$NON-NLS-1$
        searchNextButton.setEnabled(false);
        searchNextButton.addListener(SWT.Selection, e -> showMatch(currentMatch + 1));

        searchLabel = new Label(searchRow, SWT.NONE);
        searchLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

        highlightButton = new Button(searchRow, SWT.TOGGLE);
        highlightButton.setText("\u270E"); // pencil //$NON-NLS-1$
        highlightButton.setToolTipText(Messages.LabelHighlightText);

        // both text widgets show the same text: the plain one with the context
        // menu of the operating system, the other one with highlighting
        textStack = new Composite(textComposite, SWT.NONE);
        textStackLayout = new StackLayout();
        textStack.setLayout(textStackLayout);
        textStack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        var text = inputFile.getText() != null ? inputFile.getText() : ""; //$NON-NLS-1$

        textWidget = new Text(textStack, SWT.READ_ONLY | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        textWidget.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.CODE);
        textWidget.setText(text);

        highlightWidget = new StyledText(textStack, SWT.READ_ONLY | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        highlightWidget.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.CODE);
        highlightWidget.setText(text);

        textStackLayout.topControl = textWidget;

        highlightButton.addListener(SWT.Selection, e -> {
            textStackLayout.topControl = highlightButton.getSelection() ? highlightWidget : textWidget;
            textStack.layout();
            showMatch(currentMatch);
        });

        // highlight the word which has been double clicked
        highlightWidget.addListener(SWT.MouseDoubleClick, e -> toggleHighlight());

        var contextMenu = new Menu(highlightWidget);

        var copyItem = new MenuItem(contextMenu, SWT.PUSH);
        copyItem.setText(Messages.LabelCopyToClipboard);
        copyItem.addListener(SWT.Selection, e -> {
            if (highlightWidget.getSelectionCount() == 0)
                highlightWidget.selectAll();

            highlightWidget.copy();
        });

        var highlightItem = new MenuItem(contextMenu, SWT.PUSH);
        highlightItem.setText(Messages.LabelHighlightText);
        highlightItem.addListener(SWT.Selection, e -> toggleHighlight());

        var removeItem = new MenuItem(contextMenu, SWT.PUSH);
        removeItem.setText(Messages.LabelRemoveHighlighting);
        removeItem.addListener(SWT.Selection, e -> {
            highlights.clear();
            showMatch(currentMatch);
        });

        highlightWidget.setMenu(contextMenu);

        searchField.addListener(SWT.Modify, e -> search());
        searchField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.character == SWT.ESC)
                    searchField.setText(""); //$NON-NLS-1$
                else if (e.character == SWT.CR || e.character == SWT.LF)
                    showMatch(currentMatch + ((e.stateMask & SWT.SHIFT) != 0 ? -1 : 1));
            }
        });

        var textTab = new TabItem(tabFolder, SWT.NONE);
        textTab.setText(Messages.PDFImportWizardManualEntryTextView);
        textTab.setControl(textComposite);

        // Default to PDF tab
        tabFolder.setSelection(0);

        // Show page navigation only on the PDF tab
        tabFolder.addListener(SWT.Selection, e -> pageNavComposite.setVisible(tabFolder.getSelectionIndex() == 0));

        // Page navigation and zoom controls
        pageNavComposite = new Composite(this, SWT.NONE);
        pageNavComposite.setLayout(new GridLayout(5, false));
        pageNavComposite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

        prevButton = new Button(pageNavComposite, SWT.PUSH);
        prevButton.setText("<"); //$NON-NLS-1$
        prevButton.setEnabled(false);
        prevButton.addListener(SWT.Selection, e -> showPdfPage(currentPageIndex - 1));

        pageLabel = new Label(pageNavComposite, SWT.NONE);
        pageLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

        nextButton = new Button(pageNavComposite, SWT.PUSH);
        nextButton.setText(">"); //$NON-NLS-1$
        nextButton.setEnabled(false);
        nextButton.addListener(SWT.Selection, e -> showPdfPage(currentPageIndex + 1));

        zoomInButton = new Button(pageNavComposite, SWT.PUSH);
        zoomInButton.setText("+"); //$NON-NLS-1$
        zoomInButton.setEnabled(false);
        zoomInButton.addListener(SWT.Selection, e -> zoomIn());

        zoomOutButton = new Button(pageNavComposite, SWT.PUSH);
        zoomOutButton.setText("\u2013"); // en-dash as minus //$NON-NLS-1$
        zoomOutButton.setEnabled(false);
        zoomOutButton.addListener(SWT.Selection, e -> zoomOut());

        // Dispose all cached images when widget is disposed
        addDisposeListener(e -> {
            // signal queued renders to bail out; in-flight ones are no-ops once
            // the canvas is disposed (guarded in the asyncExec callbacks)
            disposed = true;

            closeRendererAsync();
            renderExecutor.shutdown();

            disposeCachedImages();
        });
    }

    public void initialize()
    {
        if (initialized)
            return;
        initialized = true;
        loading = true;
        pdfCanvas.redraw();

        var display = getDisplay();
        var generation = renderGeneration;
        renderExecutor.submit(() -> {
            if (disposed)
                return;

            int pages = 1;
            ImageData imageData = null;
            try
            {
                var r = renderer();
                pages = r.getPageCount();
                var pngBytes = r.renderPage(0, 150f);
                imageData = new ImageData(new ByteArrayInputStream(pngBytes));
            }
            catch (IOException | RuntimeException ex)
            {
                PortfolioPlugin.log(ex);
            }

            int finalPages = pages;
            ImageData finalImageData = imageData;
            display.asyncExec(() -> {
                if (pdfCanvas.isDisposed() || generation != renderGeneration)
                    return;

                totalPages = finalPages;

                if (finalImageData != null)
                {
                    currentSwtImage = new Image(display, finalImageData);
                    pageCache.put(0, currentSwtImage);
                }

                loading = false;
                loadFailed = finalImageData == null;
                currentPageIndex = 0;
                zoomLevel = 1.0;

                updateCanvasSize();
                pdfCanvas.redraw();
                updateCursor();
                updateZoomButtons();

                pageLabel.setText(MessageFormat.format(Messages.PDFImportWizardManualEntryPageOf, currentPageIndex + 1,
                                totalPages));
                prevButton.setEnabled(false);
                nextButton.setEnabled(totalPages > 1);
                pageNavComposite.layout();
            });
        });
    }

    /**
     * Releases the document and all cached page images so that only the
     * currently viewed document stays in memory. Safe to call repeatedly; a
     * later {@link #initialize()} reloads the document on demand. Call this
     * when the viewer is navigated away from (e.g. moving to the next
     * document).
     */
    public void release()
    {
        if (!initialized)
            return;

        // invalidate in-flight renders so their results are not applied to the
        // cache we are about to clear
        renderGeneration++;

        disposeCachedImages();

        currentSwtImage = null;
        totalPages = 0;
        currentPageIndex = 0;
        loading = false;
        loadFailed = false;
        initialized = false;

        closeRendererAsync();

        if (!pdfCanvas.isDisposed())
            pdfCanvas.redraw();
    }

    /**
     * Closes the open document on the render thread (it is not thread-safe),
     * after any in-flight render finishes, and drops it so a later
     * {@link #initialize()} reopens it. No-op if no document is open.
     */
    private void closeRendererAsync()
    {
        renderExecutor.execute(() -> {
            if (renderer != null)
            {
                try
                {
                    renderer.close();
                }
                catch (IOException ex)
                {
                    PortfolioPlugin.log(ex);
                }
                renderer = null;
            }
        });
    }

    private void disposeCachedImages()
    {
        for (Image img : pageCache.values())
        {
            if (img != null && !img.isDisposed())
                img.dispose();
        }
        pageCache.clear();
    }

    private void showPdfPage(int pageIndex)
    {
        if (pageIndex < 0 || pageIndex >= totalPages)
            return;

        currentPageIndex = pageIndex;
        zoomLevel = 1.0;

        // Check cache first
        var cached = pageCache.get(pageIndex);
        if (cached != null && !cached.isDisposed())
        {
            currentSwtImage = cached;
            loading = false;
            loadFailed = false;
            updateCanvasSize();
            pdfCanvas.redraw();
            updateCursor();
            updateZoomButtons();
            updateNavControls();
            return;
        }

        // Cache miss — render in background
        loading = true;
        loadFailed = false;
        currentSwtImage = null;
        pdfCanvas.redraw();

        var display = getDisplay();
        var generation = renderGeneration;
        renderExecutor.submit(() -> {
            if (disposed)
                return;

            ImageData imageData = null;
            try
            {
                var pngBytes = renderer().renderPage(pageIndex, 150f);
                imageData = new ImageData(new ByteArrayInputStream(pngBytes));
            }
            catch (IOException | RuntimeException ex)
            {
                PortfolioPlugin.log(ex);
            }

            ImageData finalImageData = imageData;
            display.asyncExec(() -> {
                if (pdfCanvas.isDisposed() || generation != renderGeneration)
                    return;

                // User may have navigated away; only apply if still on this
                // page
                if (currentPageIndex != pageIndex)
                    return;

                if (finalImageData != null)
                {
                    var img = new Image(display, finalImageData);
                    pageCache.put(pageIndex, img);
                    currentSwtImage = img;
                }

                loading = false;
                loadFailed = finalImageData == null;
                updateCanvasSize();
                pdfCanvas.redraw();
                updateCursor();
                updateZoomButtons();
                updateNavControls();
            });
        });
    }

    /**
     * Lazily opens the shared renderer. Must only be called on the
     * renderExecutor thread; the renderer is not thread-safe.
     */
    private PDFInputFile.PageRenderer renderer() throws IOException
    {
        if (renderer == null)
            renderer = inputFile.openRenderer();
        return renderer;
    }

    private void updateNavControls()
    {
        pageLabel.setText(MessageFormat.format(Messages.PDFImportWizardManualEntryPageOf, currentPageIndex + 1,
                        totalPages));
        prevButton.setEnabled(currentPageIndex > 0);
        nextButton.setEnabled(currentPageIndex < totalPages - 1);
        pageNavComposite.layout();
    }

    private void updateCanvasSize()
    {
        if (currentSwtImage == null || currentSwtImage.isDisposed())
            return;

        var scrolled = (ScrolledComposite) pdfViewComposite;
        var clientArea = scrolled.getClientArea();
        if (clientArea.width <= 0 || clientArea.height <= 0)
            return;

        var imgBounds = currentSwtImage.getBounds();
        double fitScaleX = (double) clientArea.width / imgBounds.width;
        double fitScaleY = (double) clientArea.height / imgBounds.height;
        double fitScale = Math.min(fitScaleX, fitScaleY);
        double effectiveScale = fitScale * zoomLevel;

        int canvasW = Math.max(clientArea.width, (int) (imgBounds.width * effectiveScale));
        int canvasH = Math.max(clientArea.height, (int) (imgBounds.height * effectiveScale));

        scrolled.setMinSize(canvasW, canvasH);
    }

    private void zoomIn()
    {
        double newZoom = zoomLevel * ZOOM_STEP;
        zoomLevel = Math.min(newZoom, MAX_ZOOM);
        updateCanvasSize();
        pdfCanvas.redraw();
        updateCursor();
        updateZoomButtons();
    }

    private void zoomOut()
    {
        double newZoom = zoomLevel / ZOOM_STEP;
        zoomLevel = Math.max(newZoom, 1.0);
        updateCanvasSize();
        pdfCanvas.redraw();
        updateCursor();
        updateZoomButtons();

        if (zoomLevel == 1.0)
            ((ScrolledComposite) pdfViewComposite).setOrigin(0, 0);
    }

    private void resetZoom()
    {
        zoomLevel = 1.0;
        updateCanvasSize();
        pdfCanvas.redraw();
        updateCursor();
        updateZoomButtons();
        ((ScrolledComposite) pdfViewComposite).setOrigin(0, 0);
    }

    private void zoomAtPoint(int mouseX, int mouseY, boolean out)
    {
        var scrolled = (ScrolledComposite) pdfViewComposite;
        var origin = scrolled.getOrigin();
        var oldSize = pdfCanvas.getSize();

        // viewport-relative click position
        int vpX = mouseX - origin.x;
        int vpY = mouseY - origin.y;

        // fractional position in canvas
        double fracX = oldSize.x > 0 ? (double) mouseX / oldSize.x : 0.5;
        double fracY = oldSize.y > 0 ? (double) mouseY / oldSize.y : 0.5;

        if (out)
        {
            zoomLevel = Math.max(zoomLevel / ZOOM_STEP, 1.0);
        }
        else
        {
            zoomLevel = Math.min(zoomLevel * ZOOM_STEP, MAX_ZOOM);
        }

        updateCanvasSize();
        scrolled.layout(true);

        var newSize = pdfCanvas.getSize();
        int newScrollX = (int) (fracX * newSize.x - vpX);
        int newScrollY = (int) (fracY * newSize.y - vpY);
        scrolled.setOrigin(newScrollX, newScrollY);

        pdfCanvas.redraw();
        updateCursor();
        updateZoomButtons();

        if (zoomLevel == 1.0)
            scrolled.setOrigin(0, 0);
    }

    private void updateCursor()
    {
        if (zoomLevel > 1.0)
            pdfCanvas.setCursor(pdfCanvas.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        else
            pdfCanvas.setCursor(null);
    }

    private void updateZoomButtons()
    {
        zoomInButton.setEnabled(currentSwtImage != null && zoomLevel < MAX_ZOOM);
        zoomOutButton.setEnabled(currentSwtImage != null && zoomLevel > 1.0);
    }

    /**
     * Returns the start offsets of all matches of the term within the text,
     * ignoring case. An empty term has no matches.
     */
    /* package */ static List<Integer> findMatches(String text, String term)
    {
        var offsets = new ArrayList<Integer>();

        if (text == null || term == null || term.isEmpty())
            return offsets;

        // search the original text: lower casing can change the length of
        // the text and therefore the offsets of the matches
        var index = 0;
        while (index + term.length() <= text.length())
        {
            if (text.regionMatches(true, index, term, 0, term.length()))
            {
                offsets.add(index);
                index += term.length();
            }
            else
            {
                index++;
            }
        }

        return offsets;
    }

    /**
     * Searches the text for the term of the search field, highlights all
     * matches and shows the first one.
     */
    private void search()
    {
        matches = findMatches(textWidget.getText(), searchField.getText());
        highlightMatches = findMatches(highlightWidget.getText(), searchField.getText());
        showMatch(0);
    }

    /**
     * Adds the current selection to the highlighted parts, or removes the
     * highlighting if the selection is already highlighted.
     */
    private void toggleHighlight()
    {
        var selection = highlightWidget.getSelection();
        if (selection.y <= selection.x)
            return;

        var range = new int[] { selection.x, selection.y - selection.x };

        var overlapping = highlights.stream().filter(h -> h[0] < range[0] + range[1] && range[0] < h[0] + h[1])
                        .toList();

        if (overlapping.isEmpty())
            highlights.add(range);
        else
            highlights.removeAll(overlapping);

        showMatch(currentMatch);
    }

    /**
     * Selects the match with the given index (wrapping around), scrolls it
     * into view and updates the highlighting.
     */
    private void showMatch(int index)
    {
        var length = searchField.getText().length();

        var count = textStackLayout.topControl == highlightWidget ? highlightMatches.size() : matches.size();
        currentMatch = count == 0 ? -1 : Math.floorMod(index, count);

        if (textStackLayout.topControl == highlightWidget)
            applyStyles(length);

        if (currentMatch >= 0)
        {
            if (textStackLayout.topControl == highlightWidget)
            {
                if (currentMatch < highlightMatches.size())
                {
                    var offset = highlightMatches.get(currentMatch);
                    highlightWidget.setSelection(offset, offset + length);
                    highlightWidget.showSelection();
                }
            }
            else
            {
                var offset = matches.get(currentMatch);
                textWidget.setSelection(offset, offset + length);
                textWidget.showSelection();
            }
        }
        else if (textStackLayout.topControl == textWidget)
        {
            textWidget.setSelection(0, 0);
        }

        searchLabel.setText(count == 0 ? "" : (currentMatch + 1) + "/" + count); //$NON-NLS-1$ //$NON-NLS-2$
        searchLabel.getParent().layout(true);

        searchPrevButton.setEnabled(count > 1);
        searchNextButton.setEnabled(count > 1);
    }

    /**
     * Shows the highlighted parts with a yellow background and the matches of
     * the search underlined, the current one with an orange background. The
     * matches win over the highlighting, therefore the highlighted parts are
     * split where they overlap a match.
     */
    private void applyStyles(int matchLength)
    {
        var ranges = new ArrayList<StyleRange>();

        var matchRanges = new ArrayList<int[]>();
        for (var ii = 0; ii < highlightMatches.size(); ii++)
        {
            var range = new int[] { highlightMatches.get(ii), matchLength };
            matchRanges.add(range);

            var style = new StyleRange(range[0], range[1], Colors.BLACK, null);
            if (ii == currentMatch)
                style.background = Colors.ICON_ORANGE;
            else
                style.underline = true;

            ranges.add(style);
        }

        for (var part : subtract(highlights, matchRanges))
            ranges.add(new StyleRange(part[0], part[1], Colors.BLACK, Colors.YELLOW));

        ranges.sort((left, right) -> Integer.compare(left.start, right.start));

        // be defensive: StyledText rejects ranges outside of the text
        var charCount = highlightWidget.getCharCount();
        ranges.removeIf(range -> range.start < 0 || range.start + range.length > charCount || range.length <= 0);

        highlightWidget.setStyleRanges(ranges.toArray(new StyleRange[0]));
    }

    /**
     * Removes the parts of the ranges which overlap one of the holes. Both are
     * given as {start, length}.
     */
    /* package */ static List<int[]> subtract(List<int[]> ranges, List<int[]> holes)
    {
        var result = new ArrayList<int[]>();

        for (var range : ranges)
        {
            var parts = new ArrayList<int[]>();
            parts.add(new int[] { range[0], range[0] + range[1] });

            for (var hole : holes)
            {
                var remaining = new ArrayList<int[]>();

                for (var part : parts)
                {
                    var holeStart = hole[0];
                    var holeEnd = hole[0] + hole[1];

                    if (holeEnd <= part[0] || holeStart >= part[1])
                    {
                        remaining.add(part);
                        continue;
                    }

                    if (part[0] < holeStart)
                        remaining.add(new int[] { part[0], holeStart });

                    if (holeEnd < part[1])
                        remaining.add(new int[] { holeEnd, part[1] });
                }

                parts = remaining;
            }

            for (var part : parts)
                result.add(new int[] { part[0], part[1] - part[0] });
        }

        return result;
    }
}
