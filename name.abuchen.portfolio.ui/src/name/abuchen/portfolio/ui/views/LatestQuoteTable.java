package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.IndustryClassification;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

class LatestQuoteTable
{
    private static final String EMPTY_LABEL = ""; //$NON-NLS-1$

    private IndustryClassification taxonomy = new IndustryClassification();

    private Composite container;

    private Label valueLatestPrices;
    private Label valueLatestTrade;
    private Label valueDaysHigh;
    private Label valueDaysLow;
    private Label valueVolume;
    private Label valuePreviousClose;

    private Label valueSector;
    private Label valueIndustryGroup;
    private Label valueIndustry;
    private Label valueSubIndustry;

    public LatestQuoteTable(Composite parent)
    {
        container = new Composite(parent, SWT.BORDER);
        container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        // fonts

        LocalResourceManager resources = new LocalResourceManager(JFaceResources.getResources(), container);
        Font boldFont = resources.createFont(FontDescriptor.createFrom(container.getFont()).setStyle(SWT.BOLD));
        Color color = resources.createColor(Colors.HEADINGS.swt());

        // labels

        Label headingQuotes = new Label(container, SWT.NONE);
        headingQuotes.setText(Messages.ColumnLatestPrice);
        headingQuotes.setFont(boldFont);
        headingQuotes.setForeground(color);

        Label labelLatestPrice = new Label(container, SWT.NONE);
        labelLatestPrice.setText(Messages.ColumnLatestPrice);
        valueLatestPrices = new Label(container, SWT.RIGHT);

        Label labelLatestTrade = new Label(container, SWT.NONE);
        labelLatestTrade.setText(Messages.ColumnLatestTrade);
        valueLatestTrade = new Label(container, SWT.RIGHT);

        Label labelDaysHigh = new Label(container, SWT.NONE);
        labelDaysHigh.setText(Messages.ColumnDaysHigh);
        valueDaysHigh = new Label(container, SWT.RIGHT);

        Label labelDaysLow = new Label(container, SWT.NONE);
        labelDaysLow.setText(Messages.ColumnDaysLow);
        valueDaysLow = new Label(container, SWT.RIGHT);

        Label labelVolume = new Label(container, SWT.NONE);
        labelVolume.setText(Messages.ColumnVolume);
        valueVolume = new Label(container, SWT.RIGHT);

        Label labelPreviousClose = new Label(container, SWT.NONE);
        labelPreviousClose.setText(Messages.ColumnPreviousClose);
        valuePreviousClose = new Label(container, SWT.RIGHT);

        Label headingClassification = new Label(container, SWT.NONE);
        headingClassification.setText(taxonomy.getRootCategory().getLabel());
        headingClassification.setFont(boldFont);
        headingClassification.setForeground(color);

        valueSector = new Label(container, SWT.NONE);
        valueIndustryGroup = new Label(container, SWT.NONE);
        valueIndustry = new Label(container, SWT.NONE);
        valueSubIndustry = new Label(container, SWT.NONE);

        // layout

        FormLayout layout = new FormLayout();
        layout.marginLeft = 5;
        layout.marginRight = 5;
        container.setLayout(layout);

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
        valueLatestPrices.setLayoutData(data);

        below(valueLatestPrices, labelLatestTrade, valueLatestTrade);
        below(valueLatestTrade, labelDaysHigh, valueDaysHigh);
        below(valueDaysHigh, labelDaysLow, valueDaysLow);
        below(valueDaysLow, labelVolume, valueVolume);
        below(valueVolume, labelPreviousClose, valuePreviousClose);

        data = new FormData();
        data.top = new FormAttachment(labelPreviousClose, 30);
        data.left = new FormAttachment(0);
        data.right = new FormAttachment(100);
        headingClassification.setLayoutData(data);

        below(headingClassification, valueSector);
        below(valueSector, valueIndustryGroup);
        below(valueIndustryGroup, valueIndustry);
        below(valueIndustry, valueSubIndustry);

    }

    private void below(Label referenceItem, Label label, Label value)
    {
        FormData data;
        data = new FormData();
        data.top = new FormAttachment(value, 0, SWT.CENTER);
        label.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(referenceItem, 5);
        data.left = new FormAttachment(referenceItem, 0, SWT.LEFT);
        data.right = new FormAttachment(100);
        value.setLayoutData(data);
    }

    private void below(Label referenceItem, Label value)
    {
        FormData data;
        data = new FormData();
        data.top = new FormAttachment(referenceItem, 5);
        data.left = new FormAttachment(referenceItem, 0, SWT.LEFT);
        data.right = new FormAttachment(100);
        value.setLayoutData(data);
    }

    public Control getControl()
    {
        return container;
    }

    public void setInput(Security security)
    {
        if (security == null || security.getLatest() == null)
        {
            valueLatestPrices.setText(EMPTY_LABEL);
            valueLatestTrade.setText(EMPTY_LABEL);
            valueDaysHigh.setText(EMPTY_LABEL);
            valueDaysLow.setText(EMPTY_LABEL);
            valueVolume.setText(EMPTY_LABEL);
            valuePreviousClose.setText(EMPTY_LABEL);

            valueSector.setText(EMPTY_LABEL);
            valueIndustryGroup.setText(EMPTY_LABEL);
            valueIndustry.setText(EMPTY_LABEL);
            valueSubIndustry.setText(EMPTY_LABEL);
        }
        else
        {
            LatestSecurityPrice p = security.getLatest();

            valueLatestPrices.setText(Values.Amount.format(p.getValue()));
            valueLatestTrade.setText(Values.Date.format(p.getTime()));
            long daysHigh = p.getHigh();
            valueDaysHigh.setText(daysHigh == -1 ? "n/a" : Values.Amount.format(daysHigh)); //$NON-NLS-1$
            long daysLow = p.getLow();
            valueDaysLow.setText(daysLow == -1 ? "n/a" : Values.Amount.format(daysLow)); //$NON-NLS-1$
            long volume = p.getVolume();
            valueVolume.setText(volume == -1 ? "n/a" : String.format("%,d", volume)); //$NON-NLS-1$ //$NON-NLS-2$
            long prevClose = p.getPreviousClose();
            valuePreviousClose.setText(prevClose == -1 ? "n/a" : Values.Amount.format(prevClose)); //$NON-NLS-1$

            IndustryClassification.Category category = taxonomy.getCategoryById(security.getIndustryClassification());

            List<IndustryClassification.Category> path = category != null ? category.getPath()
                            : new ArrayList<IndustryClassification.Category>();

            valueSector.setText(path.size() >= 2 ? escape(path.get(1).getLabel()) : EMPTY_LABEL);
            valueIndustryGroup.setText(path.size() >= 3 ? escape(path.get(2).getLabel()) : EMPTY_LABEL);
            valueIndustry.setText(path.size() >= 4 ? escape(path.get(3).getLabel()) : EMPTY_LABEL);
            valueSubIndustry.setText(path.size() >= 5 ? escape(path.get(4).getLabel()) : EMPTY_LABEL);
        }
    }

    private String escape(String label)
    {
        int p = label.indexOf('&');
        if (p < 0)
            return label;

        return label.substring(0, p) + "&&" + escape(label.substring(p + 1)); //$NON-NLS-1$
    }
}
