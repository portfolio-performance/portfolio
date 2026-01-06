package name.abuchen.portfolio.ui;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import name.abuchen.portfolio.model.ImageManager;
import name.abuchen.portfolio.ui.util.Colors;

@SuppressWarnings("restriction")
public enum Images
{
    // logos

    LOGO_16("pp_16.png"), //$NON-NLS-1$
    LOGO_32("pp_32.png"), //$NON-NLS-1$
    LOGO_48("pp_48.png"), //$NON-NLS-1$
    LOGO_128("pp_128.png"), //$NON-NLS-1$
    LOGO_256("pp_256.png"), //$NON-NLS-1$
    LOGO_512("pp_512.png"), //$NON-NLS-1$

    // UX elements

    BANNER("banner.png"), //$NON-NLS-1$
    HANDLE_NS("handle_ns.png"), //$NON-NLS-1$
    HANDLE_WE("handle_we.png"), //$NON-NLS-1$

    // model items

    SECURITY("security.png"), //$NON-NLS-1$
    SECURITY_RETIRED("watchlist.png"), //$NON-NLS-1$
    ACCOUNT("account.png"), //$NON-NLS-1$
    PORTFOLIO("portfolio.png"), //$NON-NLS-1$
    GROUPEDACCOUNTS("groupedaccounts.svg"), //$NON-NLS-1$
    GROUPEDACCOUNTS_ON("groupedaccounts_on.svg"), //$NON-NLS-1$
    WATCHLIST("watchlist.png"), //$NON-NLS-1$
    INVESTMENTPLAN("investmentplan.png"), //$NON-NLS-1$
    NOTE("note.png"), //$NON-NLS-1$
    BOOKMARK("bookmark.png"), //$NON-NLS-1$
    BOOKMARK_OPEN("bookmark_open.png"), //$NON-NLS-1$

    // tool bar

    PLUS("plus.png"), //$NON-NLS-1$
    CONFIG("config.svg"), //$NON-NLS-1$
    EXPORT("export.svg"), //$NON-NLS-1$
    SAVE("save.png"), //$NON-NLS-1$
    FILTER_ON("filter_on.svg"), //$NON-NLS-1$
    FILTER_OFF("filter_off.svg"), //$NON-NLS-1$
    CALENDAR_ON("calendar_on.svg"), //$NON-NLS-1$
    CALENDAR_OFF("calendar_off.svg"), //$NON-NLS-1$
    CLOCK("clock.png"), //$NON-NLS-1$
    MEASUREMENT_ON("measurement_on.png"), //$NON-NLS-1$
    MEASUREMENT_OFF("measurement_off.png"), //$NON-NLS-1$
    CROSSHAIR_ON("crosshair_on.png"), //$NON-NLS-1$
    CROSSHAIR_OFF("crosshair_off.png"), //$NON-NLS-1$

    // views

    VIEW_TABLE("view_table.png"), //$NON-NLS-1$
    VIEW_TREEMAP("view_treemap.png"), //$NON-NLS-1$
    VIEW_PIECHART("view_piechart.png"), //$NON-NLS-1$
    VIEW_DONUT("view_donut.png"), //$NON-NLS-1$
    VIEW_REBALANCING("view_rebalancing.png"), //$NON-NLS-1$
    VIEW_STACKEDCHART("view_stackedchart_percentage.png"), //$NON-NLS-1$
    VIEW_STACKEDCHART_ACTUALVALUE("view_stackedchart.png"), //$NON-NLS-1$
    VIEW_BARCHART("view_barchart.png"), //$NON-NLS-1$
    VIEW_LINECHART("view_linechart.png"), //$NON-NLS-1$

    CHECK("check.png"), //$NON-NLS-1$
    XMARK("xmark.png"), //$NON-NLS-1$
    QUICKFIX("quickfix.png"), //$NON-NLS-1$
    ADD("add.png"), //$NON-NLS-1$
    REMOVE("remove.png"), //$NON-NLS-1$
    NEXT("next.png"), //$NON-NLS-1$
    PREVIOUS("previous.png"), //$NON-NLS-1$
    CHEVRON("chevron.png"), //$NON-NLS-1$
    DOT_HORIZONAL("dot_horizontal.png"), //$NON-NLS-1$
    INTERVAL("interval.png"), //$NON-NLS-1$
    NEW_TRANSACTION("new_transaction.png"), //$NON-NLS-1$
    INVERT_EXCHANGE_RATE("invert_exchange_rate.png"), //$NON-NLS-1$
    IMPORT_FILE("import_file.png"), //$NON-NLS-1$
    EXPORT_FILE("export_file.png"), //$NON-NLS-1$

    CATEGORY("category.png"), //$NON-NLS-1$
    UNASSIGNED_CATEGORY("unassigned.png"), //$NON-NLS-1$

    VIEW("view.png"), //$NON-NLS-1$
    VIEW_SELECTED("view_selected.png"), //$NON-NLS-1$
    VIEW_PLUS("view_plus.png"), //$NON-NLS-1$
    VIEW_SHARE("view_share.png"), //$NON-NLS-1$

    TEXT("text.png"), //$NON-NLS-1$

    OK("ok.png"), //$NON-NLS-1$
    ERROR("error.png"), //$NON-NLS-1$
    WARNING("warning.png"), //$NON-NLS-1$
    INFO("info.png"), //$NON-NLS-1$
    ONLINE("online.png"), //$NON-NLS-1$
    ERROR_NOTICE("error_notice.png"), //$NON-NLS-1$

    DOWN_ARROW("red_arrow.png"), //$NON-NLS-1$
    UP_ARROW("green_arrow.png"), //$NON-NLS-1$

    VISIBLE("visible.png"), //$NON-NLS-1$
    HIDDEN("hidden.png"), //$NON-NLS-1$

    ARROW_FORWARD("arrow_forward.png"), //$NON-NLS-1$
    ARROW_BACK("arrow_back.png"), //$NON-NLS-1$

    // 3rd party logos

    POEDITOR_LOGO("poeditor-logo.png"), //$NON-NLS-1$
    DIVVYDIARY_LOGO("logos/divvydiary.svg"), //$NON-NLS-1$
    MYDIVIDENDS24_LOGO("mydividends24.de-logo.png"), //$NON-NLS-1$
    EODHISTORICALDATA_LOGO("eodhistoricaldata-logo.png"), //$NON-NLS-1$
    PORTFOLIO_REPORT_LOGO("portfolio-report.net-logo.png"), //$NON-NLS-1$
    GITHUB_LOGO("github.com-logo.png"); //$NON-NLS-1$

    static
    {
        // Enable use of HiDPI icons as described here:
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=459412#c8
        // But: for now it needs enabling via JFace debug option, which in turn
        // are only set when using the non-e4 org.eclipse.ui bundle. Alas, we
        // enable it directly.

        // On Mac OS X and Windows it works for me, on Linux the wrong images
        // got loaded. Therefore I do not activate it for Linux
        if (!Platform.OS_LINUX.equals(Platform.getOS()))
            org.eclipse.jface.internal.InternalPolicy.DEBUG_LOAD_URL_IMAGE_DESCRIPTOR_2x = true;
    }

    private static ImageRegistry imageRegistry = new ImageRegistry();

    private final String file;

    private Images(String file)
    {
        this.file = file;
    }

    public static Image resolve(String file, boolean disabled)
    {
        String key = file + ":" + disabled; //$NON-NLS-1$
        Image image = imageRegistry.get(key);
        if (image != null)
            return image;

        descriptor(file); // lazy loading
        image = imageRegistry.get(file);

        if (disabled)
        {
            image = ImageManager.getDisabledVersion(image);
            imageRegistry.put(key, image); // $NON-NLS-1$
        }

        return image;
    }

    private static ImageDescriptor descriptor(String file)
    {
        ImageDescriptor descriptor = imageRegistry.getDescriptor(file);
        if (descriptor == null)
        {
            // The arrow icon is used in multiple data rendering scenarios.
            // To support theme switching, we render it dynamically instead of
            // using pre-rendered static files.
            if (UP_ARROW.file.equals(file) || DOWN_ARROW.file.equals(file))
            {
                descriptor = createArrowDescriptor(UP_ARROW.file.equals(file));
            }
            else
            {
                Bundle bundle = FrameworkUtil.getBundle(Images.class);
                IPath path = new Path("icons/" + file); //$NON-NLS-1$
                URL url = FileLocator.find(bundle, path, null);
                if (url != null)
                    descriptor = ImageDescriptor.createFromURL(url);
            }

            if (descriptor != null)
                imageRegistry.put(file, descriptor);
        }
        return descriptor;
    }

    private static ImageDescriptor createArrowDescriptor(final boolean isUp)
    {
        return ImageDescriptor.createFromImageDataProvider(zoom -> {
            // Calculate dimensions based on zoom level
            final int imageSize = 16 * zoom / 100;
            final int triangleHeight = 9 * zoom / 100;
            final int triangleWidth = (int) (triangleHeight * 1.15);

            // Calculate centering offsets
            final int horizontalOffset = (imageSize - triangleWidth) / 2;
            final int verticalOffset = (imageSize - triangleHeight) / 2;

            // Create image and graphics context
            Image image = new Image(null, imageSize, imageSize);
            GC gc = new GC(image);

            try
            {
                // Fill background with white
                gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
                gc.fillRectangle(0, 0, imageSize, imageSize);

                // Set arrow color based on direction
                Color arrowColor = isUp ? Colors.ICON_UP : Colors.ICON_DOWN;
                gc.setBackground(arrowColor);

                // Draw triangle
                int[] trianglePoints = createTrianglePoints(isUp, imageSize, horizontalOffset, verticalOffset,
                                triangleHeight);
                gc.fillPolygon(trianglePoints);

                // Cleanup graphics resources
                gc.dispose();

                // Convert to ImageData with transparency
                ImageData imageData = image.getImageData();
                imageData.transparentPixel = imageData.palette.getPixel(new RGB(255, 255, 255));

                return imageData;
            }
            finally
            {
                image.dispose();
            }
        });
    }

    /**
     * Creates the point array for the triangle polygon.
     * 
     * @param isUp
     *            true for upward arrow, false for downward
     * @param imageSize
     *            total size of the image
     * @param horizontalOffset
     *            left offset for centering
     * @param verticalOffset
     *            top offset for centering
     * @param triangleHeight
     *            height of the triangle
     * @return array of x,y coordinates: [x1, y1, x2, y2, x3, y3]
     */
    private static int[] createTrianglePoints(boolean isUp, int imageSize, int horizontalOffset, int verticalOffset,
                    int triangleHeight)
    {
        int centerX = imageSize / 2;
        int topY = verticalOffset;
        int bottomY = verticalOffset + triangleHeight;
        int leftX = horizontalOffset;
        int rightX = imageSize - horizontalOffset;

        if (isUp)
        {
            // Triangle pointing up: apex at top center, base at bottom
            return new int[] { centerX, topY, // Top vertex (apex)
                            leftX, bottomY, // Bottom-left vertex
                            rightX, bottomY // Bottom-right vertex
            };
        }
        else
        {
            // Triangle pointing down: base at top, apex at bottom center
            return new int[] { leftX, topY, // Top-left vertex
                            rightX, topY, // Top-right vertex
                            centerX, bottomY // Bottom vertex (apex)
            };
        }
    }

    public ImageDescriptor descriptor()
    {
        return descriptor(file);
    }

    public Image image()
    {
        return resolve(file, false);
    }

    public Image image(boolean disabled)
    {
        return resolve(file, disabled);
    }

    public String getImageURI()
    {
        return "platform:/plugin/" + PortfolioPlugin.PLUGIN_ID + "/icons/" + file; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
