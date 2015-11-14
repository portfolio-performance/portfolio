package name.abuchen.portfolio.ui.wizards.client;

import java.util.HashSet;
import java.util.Set;

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

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.TaxonomyTemplate;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class AddTaxonomyPage extends AbstractWizardPage
{
    private Client client;

    private Set<String> taxonomiesAdded = new HashSet<String>();

    public AddTaxonomyPage(Client client)
    {
        super(AddTaxonomyPage.class.getSimpleName());
        this.client = client;
        setTitle(Messages.NewFileWizardTaxonomyTitle);
        setDescription(Messages.NewFileWizardTaxonomyDescription);
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
        column.getColumn().setText(Messages.ColumnTaxonomy);
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

                if (taxonomiesAdded.contains(t.getId()))
                    return Images.CHECK.image();
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

                if (template != null && !taxonomiesAdded.contains(template.getId()))
                {
                    taxonomiesAdded.add(template.getId());
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
