package name.abuchen.portfolio.ui.wizards;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.util.TaxonomyTemplate;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

public class AddTaxonomyPage extends AbstractWizardPage
{
    private Client client;

    public AddTaxonomyPage(Client client)
    {
        super(AddTaxonomyPage.class.getSimpleName());
        this.client = client;
        setTitle("Add Taxonomies");
        setDescription("Add taxonomies to analyze the portfolio along varios dimensions...");
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        setControl(container);

        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        final TableViewer viewer = new TableViewer(container);

        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        layout.setColumnData(column.getColumn(), new ColumnWeightData(90));
        column.getColumn().setText("Taxonomy");
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((TaxonomyTemplate) element).getName();
            }
        });

        column = new TableViewerColumn(viewer, SWT.NONE);
        layout.setColumnData(column.getColumn(), new ColumnWeightData(10));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return null;
            }

            @Override
            public Image getImage(Object element)
            {
                TaxonomyTemplate t = (TaxonomyTemplate) element;

                if (client.getTaxonomy(t.getId()) != null)
                    return PortfolioPlugin.image(PortfolioPlugin.IMG_CHECK);
                else
                    return null;
            }
        });

        viewer.addDoubleClickListener(new IDoubleClickListener()
        {
            @Override
            public void doubleClick(DoubleClickEvent event)
            {
                TaxonomyTemplate template = (TaxonomyTemplate) ((IStructuredSelection) event.getSelection())
                                .getFirstElement();

                if (template != null && client.getTaxonomy(template.getId()) == null)
                {
                    client.addTaxonomy(template.build());
                    viewer.refresh(template);
                }
            }
        });

        viewer.getTable().setHeaderVisible(true);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setInput(TaxonomyTemplate.list());

        setPageComplete(true);
    }

}
