package name.abuchen.portfolio.ui.views.columns;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Column;
import name.abuchen.portfolio.ui.util.OptionLabelProvider;

import org.eclipse.swt.SWT;

public class TaxonomyColumn extends Column
{
    public static final class TaxonomyLabelProvider extends OptionLabelProvider
    {
        private final Taxonomy taxonomy;

        public TaxonomyLabelProvider(Taxonomy taxonomy)
        {
            this.taxonomy = taxonomy;
        }

        @Override
        public String getText(Object e, Integer option)
        {
            InvestmentVehicle vehicle = Adaptor.adapt(InvestmentVehicle.class, e);
            if (vehicle == null)
                return null;

            List<Classification> classifications = taxonomy.getClassifications(vehicle);
            if (classifications.isEmpty())
                return null;

            StringBuilder answer = new StringBuilder();

            for (Classification c : classifications)
            {
                if (answer.length() > 0)
                    answer.append(", "); //$NON-NLS-1$

                if (option == 100)
                {
                    answer.append(c.getPathName(false));
                }
                else
                {
                    List<Classification> path = c.getPathToRoot();
                    if (option < path.size())
                        answer.append(path.get(option).getName());
                }
            }

            return answer.toString();
        }
    }

    public TaxonomyColumn(final Taxonomy taxonomy)
    {
        super(taxonomy.getId(), taxonomy.getName(), SWT.LEFT, 120);

        setGroupLabel(Messages.ColumnTaxonomy);
        prepareOptions(taxonomy);

        this.setLabelProvider(new TaxonomyLabelProvider(taxonomy));
    }

    private void prepareOptions(final Taxonomy taxonomy)
    {
        List<String> labels = taxonomy.getDimensions();

        List<Integer> options = new ArrayList<Integer>();

        StringBuilder menuLabels = new StringBuilder("{0,choice,"); //$NON-NLS-1$
        StringBuilder columnLabels = new StringBuilder("{0,choice,"); //$NON-NLS-1$

        int heigth = taxonomy.getHeigth();
        for (int ii = 1; ii < heigth; ii++) // 1 --> skip taxonomy root node
        {
            options.add(ii);

            if (ii > 1)
            {
                menuLabels.append('|');
                columnLabels.append('|');
            }

            String label = null;
            if (labels != null && ii <= labels.size())
                label = labels.get(ii - 1);

            menuLabels.append(ii).append('#');
            columnLabels.append(ii).append('#');

            if (label != null)
            {
                menuLabels.append(label);
                columnLabels.append(label);
            }
            else
            {
                menuLabels.append(MessageFormat.format(Messages.LabelLevelNumber, ii));
                columnLabels.append(MessageFormat.format(Messages.LabelLevelNameNumber, taxonomy.getName(), ii));
            }
        }

        options.add(100);

        menuLabels.append("|100#" + Messages.LabelFullClassification + "}"); //$NON-NLS-1$ //$NON-NLS-2$
        columnLabels.append("|100#").append(taxonomy.getName()).append("}"); //$NON-NLS-1$ //$NON-NLS-2$

        setOptions(menuLabels.toString(), columnLabels.toString(), options.toArray(new Integer[0]));
    }
}
