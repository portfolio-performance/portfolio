package name.abuchen.portfolio.ui.views;

import static name.abuchen.portfolio.ui.util.SWTHelper.EMPTY_LABEL;
import static name.abuchen.portfolio.ui.util.SWTHelper.clearLabel;
import static name.abuchen.portfolio.ui.util.SWTHelper.dateWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.placeBelow;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.model.Taxonomy.Visitor;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.util.TextUtil;

public class SecurityDetailsViewer
{
    private abstract static class SecurityFacet
    {
        abstract Control createViewControl(Composite parent);

        abstract void setInput(Security security);

        protected Label createHeading(Composite parent, String text)
        {
            Label heading = new Label(parent, SWT.NONE);
            heading.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
            heading.setText(text);
            return heading;
        }

        protected void below(Label referenceItem, Label value)
        {
            FormData data;
            data = new FormData();
            data.top = new FormAttachment(referenceItem, 5);
            data.left = new FormAttachment(referenceItem, 0, SWT.LEFT);
            data.right = new FormAttachment(100);
            value.setLayoutData(data);
        }

        protected String escape(String label)
        {
            return label != null ? TextUtil.tooltip(label) : EMPTY_LABEL;
        }

        protected String nonNullString(String label)
        {
            return label != null ? label : EMPTY_LABEL;
        }
    }

    private static class MasterDataFacet extends SecurityFacet
    {
        private Label valueName;
        private Label valueISIN;
        private Label valueTickerSymbol;
        private Label valueWKN;

        @Override
        Control createViewControl(Composite parent)
        {
            Composite composite = new Composite(parent, SWT.NONE);

            Label heading = createHeading(composite, Messages.ColumnSecurity);

            valueName = new Label(composite, SWT.NONE);
            valueISIN = new Label(composite, SWT.NONE);
            valueTickerSymbol = new Label(composite, SWT.NONE);
            valueWKN = new Label(composite, SWT.NONE);

            // layout

            FormLayout layout = new FormLayout();
            layout.marginLeft = 5;
            layout.marginRight = 5;
            composite.setLayout(layout);

            FormData data = new FormData();
            data.top = new FormAttachment(0, 5);
            heading.setLayoutData(data);

            data = new FormData();
            data.top = new FormAttachment(heading, 5);
            data.left = new FormAttachment(0);
            data.right = new FormAttachment(100);
            valueName.setLayoutData(data);

            below(valueName, valueISIN);
            below(valueISIN, valueTickerSymbol);
            below(valueTickerSymbol, valueWKN);

            return composite;
        }

        @Override
        void setInput(Security security)
        {
            if (security == null)
            {
                clearLabel(valueName, valueISIN, valueTickerSymbol, valueWKN);
            }
            else
            {
                valueName.setText(TextUtil.tooltip(security.getName()));
                valueISIN.setText(nonNullString(security.getIsin()));
                valueTickerSymbol.setText(nonNullString(security.getTickerSymbol()));
                valueWKN.setText(nonNullString(security.getWkn()));
            }
        }
    }

    private static class NoteFacet extends SecurityFacet
    {
        private Label valueNote;

        @Override
        Control createViewControl(Composite parent)
        {
            Composite composite = new Composite(parent, SWT.NONE);

            Label heading = createHeading(composite, Messages.ColumnNote);

            valueNote = new Label(composite, SWT.NONE);

            // layout

            FormLayout layout = new FormLayout();
            layout.marginLeft = 5;
            layout.marginRight = 5;
            composite.setLayout(layout);

            FormData data = new FormData();
            data.top = new FormAttachment(0, 5);
            heading.setLayoutData(data);

            data = new FormData();
            data.top = new FormAttachment(heading, 5);
            data.left = new FormAttachment(0);
            data.right = new FormAttachment(100);
            valueNote.setLayoutData(data);

            return composite;
        }

        @Override
        void setInput(Security security)
        {
            if (security == null || security.getNote() == null)
                valueNote.setText(EMPTY_LABEL);
            else
                valueNote.setText(escape(security.getNote()));
        }
    }

    private static class LatestQuoteFacet extends SecurityFacet
    {
        private Label valueLatestPrices;
        private Label valueLatestTrade;
        private Label valueDaysHigh;
        private Label valueDaysLow;
        private Label valueVolume;

        @Override
        public Control createViewControl(Composite parent)
        {
            Composite composite = new Composite(parent, SWT.NONE);

            Label headingQuotes = createHeading(composite, Messages.ColumnLatestPrice);

            Label labelLatestPrice = new Label(composite, SWT.NONE);
            labelLatestPrice.setText(Messages.ColumnLatestPrice);
            valueLatestPrices = new Label(composite, SWT.RIGHT);

            Label labelLatestTrade = new Label(composite, SWT.NONE);
            labelLatestTrade.setText(Messages.ColumnLatestTrade);
            valueLatestTrade = new Label(composite, SWT.RIGHT);

            Label labelDaysHigh = new Label(composite, SWT.NONE);
            labelDaysHigh.setText(Messages.ColumnDaysHigh);
            valueDaysHigh = new Label(composite, SWT.RIGHT);

            Label labelDaysLow = new Label(composite, SWT.NONE);
            labelDaysLow.setText(Messages.ColumnDaysLow);
            valueDaysLow = new Label(composite, SWT.RIGHT);

            Label labelVolume = new Label(composite, SWT.NONE);
            labelVolume.setText(Messages.ColumnVolume);
            valueVolume = new Label(composite, SWT.RIGHT);

            // layout

            FormLayout layout = new FormLayout();
            layout.marginLeft = 5;
            layout.marginRight = 5;
            composite.setLayout(layout);

            FormData data = new FormData();
            data.top = new FormAttachment(0, 5);
            headingQuotes.setLayoutData(data);

            data = new FormData();
            data.top = new FormAttachment(valueLatestPrices, 0, SWT.CENTER);
            labelLatestPrice.setLayoutData(data);

            data = new FormData();
            data.top = new FormAttachment(headingQuotes, 5);
            data.left = new FormAttachment(50, 5);
            data.right = new FormAttachment(100);
            data.width = dateWidth(composite);
            valueLatestPrices.setLayoutData(data);

            placeBelow(valueLatestPrices, labelLatestTrade, valueLatestTrade);
            placeBelow(valueLatestTrade, labelDaysHigh, valueDaysHigh);
            placeBelow(valueDaysHigh, labelDaysLow, valueDaysLow);
            placeBelow(valueDaysLow, labelVolume, valueVolume);

            return composite;
        }

        @Override
        public void setInput(Security security)
        {
            if (valueLatestPrices.isDisposed())
                return;

            if (security == null || security.getLatest() == null)
            {
                clearLabel(valueLatestPrices, valueLatestTrade, valueDaysHigh, valueDaysLow, valueVolume);
            }
            else
            {
                LatestSecurityPrice p = security.getLatest();

                valueLatestPrices.setText(Values.Quote.format(p.getValue()));
                valueLatestTrade.setText(Values.Date.format(p.getDate()));
                long daysHigh = p.getHigh();
                valueDaysHigh.setText(daysHigh == LatestSecurityPrice.NOT_AVAILABLE ? Messages.LabelNotAvailable
                                : Values.Quote.format(daysHigh));
                long daysLow = p.getLow();
                valueDaysLow.setText(daysLow == LatestSecurityPrice.NOT_AVAILABLE ? Messages.LabelNotAvailable
                                : Values.Quote.format(daysLow));
                long volume = p.getVolume();
                valueVolume.setText(volume == LatestSecurityPrice.NOT_AVAILABLE ? Messages.LabelNotAvailable
                                : String.format("%,d", volume)); //$NON-NLS-1$
            }
        }

    }

    private static class TaxonomyFacet extends SecurityFacet
    {
        private Taxonomy taxonomy;

        private Label heading;
        private List<Label> labels = new ArrayList<>();

        public TaxonomyFacet(Taxonomy taxonomy)
        {
            this.taxonomy = taxonomy;
        }

        @Override
        Control createViewControl(Composite parent)
        {
            Composite composite = new Composite(parent, SWT.NONE);
            FormLayout layout = new FormLayout();
            layout.marginLeft = 5;
            layout.marginRight = 5;
            composite.setLayout(layout);

            heading = createHeading(composite, taxonomy.getName());
            FormData data = new FormData();
            data.top = new FormAttachment(0, 5);
            data.left = new FormAttachment(0);
            data.right = new FormAttachment(100);
            heading.setLayoutData(data);

            for (int ii = 0; ii < taxonomy.getHeigth() - 1; ii++)
            {
                Label label = new Label(composite, SWT.NONE);
                labels.add(label);

                if (ii == 0)
                {
                    data = new FormData();
                    data.top = new FormAttachment(heading, 5);
                    data.left = new FormAttachment(0);
                    data.right = new FormAttachment(100);
                    label.setLayoutData(data);
                }
                else
                {
                    below(labels.get(ii - 1), label);
                }
            }

            return composite;
        }

        @Override
        void setInput(final Security security)
        {
            final int[] count = new int[1];
            final int[] weight = new int[1];
            final Classification[] classification = new Classification[1];

            if (security != null)
            {
                taxonomy.foreach(new Visitor()
                {
                    @Override
                    public void visit(Classification thisClassification, Assignment assignment)
                    {
                        if (security.equals(assignment.getInvestmentVehicle()))
                        {
                            count[0]++;
                            if (assignment.getWeight() > weight[0])
                            {
                                weight[0] = assignment.getWeight();
                                classification[0] = thisClassification;
                            }
                        }
                    }
                });
            }

            if (count[0] > 1)
                heading.setText(taxonomy.getName() + ' ' + MessageFormat.format(Messages.LabelOneOfX, count[0]));
            else
                heading.setText(taxonomy.getName());

            List<Classification> path = classification[0] != null ? classification[0].getPathToRoot()
                            : new ArrayList<>();
            for (int ii = 0; ii < labels.size(); ii++)
                labels.get(ii).setText(path.size() > ii + 1 ? escape(path.get(ii + 1).getName()) : EMPTY_LABEL);
        }
    }

    private ScrolledComposite container;

    private List<SecurityFacet> children = new ArrayList<>();

    public SecurityDetailsViewer(Composite parent, int style, Client client)
    {
        this(parent, style, client, false);
    }

    public SecurityDetailsViewer(Composite parent, int style, Client client, boolean showMasterData)
    {
        container = new ScrolledComposite(parent, SWT.V_SCROLL);

        Composite container1 = new Composite(container, style);
        container.setContent(container1);
        container.setExpandHorizontal(true);
        container.setExpandVertical(true);
        // Needed to avoid artifact (at least on Linux) when scrollbar appears
        // and covers right edge (last digits of quotes, etc.) of the content
        // pane. With setAlwaysShowScrollBars(true) that issue doesn't happen.
        container.setAlwaysShowScrollBars(true);
        container1.setBackground(Colors.WHITE);
        container1.setBackgroundMode(SWT.INHERIT_FORCE);

        // facets

        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container1);

        if (showMasterData)
            children.add(new MasterDataFacet());

        children.add(new LatestQuoteFacet());

        for (Taxonomy taxonomy : client.getTaxonomies())
            children.add(new TaxonomyFacet(taxonomy));

        children.add(new NoteFacet());

        for (SecurityFacet child : children)
        {
            try
            {
                Control control = child.createViewControl(container1);
                GridDataFactory.fillDefaults().grab(true, false).applyTo(control);
            }
            catch (Exception e)
            {
                PortfolioPlugin.log(e);
            }
        }

        container.addControlListener(new ControlAdapter()
        {
            @Override
            public void controlResized(ControlEvent e)
            {
                container.setMinSize(container1.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
        });

        // After adding all children, explicitly calculate size and layout
        // ScrolledComposite's content container. This is again to avoid
        // artifact when scrollbar covers this container's right edge.
        container1.setSize(container1.computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
        container1.layout();
    }

    public Control getControl()
    {
        return container;
    }

    public void setInput(Security security)
    {
        for (SecurityFacet child : children)
            child.setInput(security);
    }
}
