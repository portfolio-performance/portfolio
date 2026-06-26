package name.abuchen.portfolio.ui.util.swt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
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
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
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

        // Text view
        var textScrolled = new ScrolledComposite(tabFolder, SWT.H_SCROLL | SWT.V_SCROLL);
        textScrolled.setExpandHorizontal(true);
        textScrolled.setExpandVertical(true);
        var textWidget = new Text(textScrolled, SWT.READ_ONLY | SWT.MULTI);
        textWidget.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.CODE);
        textWidget.setText(inputFile.getText() != null ? inputFile.getText() : ""); //$NON-NLS-1$
        textScrolled.setContent(textWidget);

        var textTab = new TabItem(tabFolder, SWT.NONE);
        textTab.setText(Messages.PDFImportWizardManualEntryTextView);
        textTab.setControl(textScrolled);

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
}
