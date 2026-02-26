package name.abuchen.portfolio.ui.views.taxonomy;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleFunction;
import java.util.function.Function;

import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;

import name.abuchen.portfolio.model.ClientProperties;
import name.abuchen.portfolio.model.SecurityNameConfig;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.security.LazySecurityPerformanceSnapshot;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.swt.ColorSchema;
import name.abuchen.portfolio.util.ColorConversion;

/* package */class TaxonomyNodeRenderer
{
    static class PerformanceNodeRenderer extends TaxonomyNodeRenderer
    {
        private final ReportingPeriod reportingPeriod;
        private LazySecurityPerformanceSnapshot snapshot;
        private DoubleFunction<Color> colorSchema;

        public PerformanceNodeRenderer(TaxonomyModel model, LocalResourceManager resources,
                        ReportingPeriod reportingPeriod)
        {
            super(model, resources);
            this.reportingPeriod = reportingPeriod;
            this.snapshot = LazySecurityPerformanceSnapshot.create(model.getFilteredClient(),
                            model.getCurrencyConverter(), reportingPeriod.toInterval(LocalDate.now()));
        }

        public void setColorSchema(ColorSchema colorSchema)
        {
            this.colorSchema = colorSchema.buildColorFunction(resources);
        }

        @Override
        public String[] getLabel(TaxonomyNode node)
        {
            if (node.getBackingSecurity() == null)
            {
                return new String[] { node.getName() };
            }
            else
            {
                var r = snapshot.getRecord(node.getBackingSecurity());

                if (r.isPresent())
                {
                    var performance = r.get().getTrueTimeWeightedRateOfReturn().get();
                    return new String[] { node.getBackingSecurity().getName(nameConfig),
                                    Values.PercentWithSign.format(performance) };
                }
                else
                {
                    return new String[] { node.getBackingSecurity().getName(nameConfig) };
                }
            }
        }

        @Override
        public Color getColorFor(TaxonomyNode rootNode, TaxonomyNode node)
        {
            if (node.getBackingSecurity() == null)
            {
                return colorSchema.apply(0);
            }
            else
            {
                var r = snapshot.getRecord(node.getBackingSecurity());

                if (r.isPresent())
                {
                    var performance = r.get().getTrueTimeWeightedRateOfReturn().get();
                    return colorSchema.apply(performance);
                }
                else
                {
                    return colorSchema.apply(0);
                }
            }
        }

        public ReportingPeriod getReportingPeriod()
        {
            return reportingPeriod;
        }
    }

    protected final LocalResourceManager resources;
    protected final TaxonomyModel model;
    protected final SecurityNameConfig nameConfig;

    protected Map<String, Color> hex2color = new HashMap<>();
    protected Function<String, Color> colorFactory = color -> new Color(ColorConversion.hex2RGB(color));

    public TaxonomyNodeRenderer(TaxonomyModel model, LocalResourceManager resources)
    {
        this.model = model;
        this.resources = resources;
        this.nameConfig = new ClientProperties(model.getClient()).getSecurityNameConfig();
    }

    public String[] getLabel(TaxonomyNode node)
    {
        var label = node.getBackingSecurity() != null ? node.getBackingSecurity().getName(nameConfig) : node.getName();
        double total = model.getChartRenderingRootNode().getActual().getAmount();
        String info = String.format("%s (%s%%)", Values.Money.format(node.getActual()), //$NON-NLS-1$
                        Values.Percent.format(node.getActual().getAmount() / total));
        return new String[] { label, info };
    }

    public final void drawRectangle(TaxonomyNode rootNode, TaxonomyNode node, GC gc, Rectangle r)
    {
        var color = getColorFor(rootNode, node);

        gc.setBackground(color);
        gc.fillRectangle(r.x, r.y, r.width, r.height);

        gc.setForeground(Colors.darker(color));
        gc.drawLine(r.x, r.y + r.height - 1, r.x + r.width - 1, r.y + r.height - 1);
        gc.drawLine(r.x + r.width - 1, r.y, r.x + r.width - 1, r.y + r.height - 1);

        gc.setForeground(Colors.brighter(color));
        gc.drawLine(r.x, r.y, r.x + r.width, r.y);
        gc.drawLine(r.x, r.y, r.x, r.y + r.height);

        gc.setClipping(r);

        try
        {
            gc.setForeground(Colors.getTextColor(gc.getBackground()));

            var label = getLabel(node);

            var textExtents = new Point[label.length];
            var widestLabel = 0;
            for (int ii = 0; ii < label.length; ii++)
            {
                Point extent = gc.textExtent(label[ii]);
                textExtents[ii] = extent;
                if (extent.x > widestLabel)
                    widestLabel = extent.x;
            }

            int lineHeight = gc.getFontMetrics().getHeight();

            if (widestLabel <= r.width || r.width > r.height)
            {
                // horizontal
                for (int ii = 0; ii < label.length; ii++)
                {
                    gc.drawString(label[ii], r.x + 2, r.y + 2 + ii * lineHeight, true);
                }
            }
            else
            {
                // vertical
                final Transform transform = new Transform(gc.getDevice());
                try
                {
                    transform.translate(r.x, r.y);
                    transform.rotate(-90);
                    gc.setTransform(transform);

                    // drawing a multi-line label with transform does not
                    // work. Instead, we split the label into individual
                    // lines

                    for (int ii = 0; ii < label.length; ii++)
                    {
                        gc.drawString(label[ii], //
                                        Math.max(-textExtents[ii].x - 2, -r.height + 2), //
                                        2 + ii * lineHeight, true);
                    }
                }
                finally
                {
                    transform.dispose();
                    gc.setTransform(null);
                }
            }
        }
        finally
        {
            gc.setClipping((Rectangle) null);
        }

    }

    public final Color getColorFor(TaxonomyNode node)
    {
        return getColorFor(null, node);
    }

    protected Color getColorFor(TaxonomyNode rootNode, TaxonomyNode node)
    {
        if (rootNode == null || node.isRoot() || rootNode.equals(node))
            return hex2color.computeIfAbsent(node.getColor(), colorFactory);

        List<TaxonomyNode> path = node.getPath();
        int index = path.indexOf(rootNode);

        if (index < 0) // root not found!
            return hex2color.computeIfAbsent(node.getColor(), colorFactory);

        if (path.size() <= index + 1)
            return hex2color.computeIfAbsent(node.getColor(), colorFactory);

        TaxonomyNode reference = path.get(index + 1);
        return hex2color.computeIfAbsent(reference.getColor(), colorFactory);
    }

}
