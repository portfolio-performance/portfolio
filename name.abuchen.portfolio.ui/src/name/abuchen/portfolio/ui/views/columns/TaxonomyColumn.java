package name.abuchen.portfolio.ui.views.columns;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.OptionLabelProvider;

public class TaxonomyColumn extends Column
{
    private static final class TaxonomyOptions implements Options<Integer>
    {
        private final Taxonomy taxonomy;

        private TaxonomyOptions(Taxonomy taxonomy)
        {
            this.taxonomy = taxonomy;
        }

        @Override
        public List<Integer> getElements()
        {
            // 1 --> skip taxonomy root node
            List<Integer> elements = IntStream.range(1, taxonomy.getHeigth()) //
                            .boxed().collect(Collectors.toList());
            elements.add(100); // full classification
            return elements;
        }

        @Override
        public Integer valueOf(String s)
        {
            return Integer.parseInt(s);
        }

        @Override
        public String toString(Integer element)
        {
            return element.toString();
        }

        @Override
        public String getColumnLabel(Integer element)
        {
            int index = element;

            if (index == 100)
                return taxonomy.getName();

            List<String> labels = taxonomy.getDimensions();
            return labels != null && index < labels.size() ? labels.get(index - 1)
                            : MessageFormat.format(Messages.LabelLevelNameNumber, taxonomy.getName(), element);
        }

        @Override
        public String getMenuLabel(Integer element)
        {
            int index = element;

            if (index == 100)
                return Messages.LabelFullClassification;

            List<String> labels = taxonomy.getDimensions();
            return labels != null && index <= labels.size() ? labels.get(index - 1)
                            : MessageFormat.format(Messages.LabelLevelNumber, element);
        }

        @Override
        public String getDescription(Integer element)
        {
            return null;
        }

        @Override
        public boolean canCreateNewElements()
        {
            return false;
        }

        @Override
        public Integer createNewElement(Shell shell)
        {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TaxonomyLabelProvider extends OptionLabelProvider<Integer>
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
        setOptions(new TaxonomyOptions(taxonomy));
        setLabelProvider(new TaxonomyLabelProvider(taxonomy));
    }

}
