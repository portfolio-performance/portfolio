package name.abuchen.portfolio.ui;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

@SuppressWarnings("restriction")
public enum Images
{
    LOGO_16("pp_16.png"), //$NON-NLS-1$
    LOGO_32("pp_32.png"), //$NON-NLS-1$
    LOGO_48("pp_48.png"), //$NON-NLS-1$
    LOGO_128("pp_128.png"), //$NON-NLS-1$
    LOGO_256("pp_256.png"), //$NON-NLS-1$
    LOGO_512("pp_512.png"), //$NON-NLS-1$

    BANNER("banner.png"), //$NON-NLS-1$

    // model items

    SECURITY("security.png"), //$NON-NLS-1$
    SECURITY_RETIRED("watchlist.png"), //$NON-NLS-1$
    ACCOUNT("account.png"), //$NON-NLS-1$
    PORTFOLIO("portfolio.png"), //$NON-NLS-1$
    WATCHLIST("watchlist.png"), //$NON-NLS-1$
    INVESTMENTPLAN("investmentplan.png"), //$NON-NLS-1$
    NOTE("note.png"), //$NON-NLS-1$

    // buttons

    PLUS("plus.png"), //$NON-NLS-1$
    CONFIG("config.png"), //$NON-NLS-1$
    EXPORT("export.png"), //$NON-NLS-1$
    SAVE("save.png"), //$NON-NLS-1$
    FILTER_ON("filter_on.png"), //$NON-NLS-1$
    FILTER_OFF("filter_off.png"), //$NON-NLS-1$
    CALENDAR_ON("calendar_on.png"), //$NON-NLS-1$
    CALENDAR_OFF("calendar_off.png"), //$NON-NLS-1$
    CLOCK("clock.png"), //$NON-NLS-1$

    // views

    VIEW_TABLE("view_table.png"), //$NON-NLS-1$
    VIEW_TREEMAP("view_treemap.png"), //$NON-NLS-1$
    VIEW_PIECHART("view_piechart.png"), //$NON-NLS-1$
    VIEW_DONUT("view_donut.png"), //$NON-NLS-1$
    VIEW_REBALANCING("view_rebalancing.png"), //$NON-NLS-1$
    VIEW_STACKEDCHART("view_stackedchart.png"), //$NON-NLS-1$
    VIEW_BARCHART("view_barchart.png"), //$NON-NLS-1$
    VIEW_LINECHART("view_linechart.png"), //$NON-NLS-1$

    CHECK("check.png"), //$NON-NLS-1$
    XMARK("xmark.png"), //$NON-NLS-1$
    QUICKFIX("quickfix.png"), //$NON-NLS-1$
    ADD("add.png"), //$NON-NLS-1$
    REMOVE("remove.png"), //$NON-NLS-1$
    CHEVRON("chevron.png"), //$NON-NLS-1$
    INTERVAL("interval.png"), //$NON-NLS-1$

    CATEGORY("category.png"), //$NON-NLS-1$
    UNASSIGNED_CATEGORY("unassigned.png"), //$NON-NLS-1$

    VIEW("view.png"), //$NON-NLS-1$
    VIEW_SELECTED("view_selected.png"), //$NON-NLS-1$
    VIEW_PLUS("view_plus.png"), //$NON-NLS-1$
    VIEW_SHARE("view_share.png"), //$NON-NLS-1$

    TEXT("text.png"), //$NON-NLS-1$

    ERROR("error.png"), //$NON-NLS-1$
    WARNING("warning.png"), //$NON-NLS-1$
    INFO("info.png"), //$NON-NLS-1$
    ONLINE("online.png"), //$NON-NLS-1$
    ERROR_NOTICE("error_notice.png"), //$NON-NLS-1$

    RED_ARROW("red_arrow.png"), //$NON-NLS-1$
    GREEN_ARROW("green_arrow.png"), //$NON-NLS-1$

    HANDLE_NS("handle_ns.png"), //$NON-NLS-1$
    HANDLE_WE("handle_we.png"); //$NON-NLS-1$

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

    public ImageDescriptor descriptor()
    {
        ImageDescriptor descriptor = imageRegistry.getDescriptor(file);
        if (descriptor == null)
        {
            Bundle bundle = FrameworkUtil.getBundle(Images.class);
            IPath path = new Path("icons/" + file); //$NON-NLS-1$
            URL url = FileLocator.find(bundle, path, null);
            descriptor = ImageDescriptor.createFromURL(url);
            imageRegistry.put(file, descriptor);
        }
        return descriptor;
    }

    public Image image()
    {
        Image image = imageRegistry.get(file);
        if (image == null)
        {
            descriptor(); // lazy loading
            image = imageRegistry.get(file);
        }
        return image;
    }
}
