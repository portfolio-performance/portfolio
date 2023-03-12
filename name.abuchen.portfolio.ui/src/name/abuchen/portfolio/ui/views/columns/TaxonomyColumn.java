package name.abuchen.portfolio.ui.views.columns;

import java.text.MessageFormat;
import java.util.Comparator;
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
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.OptionLabelProvider;
import name.abuchen.portfolio.util.TextUtil;

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
        public List<Integer> getOptions()
        {
            // 1 --> skip taxonomy root node
            List<Integer> elements = IntStream.range(1, taxonomy.getHeigth()) // NOSONAR
                            .boxed().collect(Collectors.toList());
            elements.add(SHOW_FULL_CLASSIFICATION);
            return elements;
        }

        @Override
        public Integer valueOf(String s)
        {
            return Integer.parseInt(s);
        }

        @Override
        public String toString(Integer option)
        {
            return option.toString();
        }

        @Override
        public String getColumnLabel(Integer option)
        {
            int index = option;

            if (index == SHOW_FULL_CLASSIFICATION)
                return taxonomy.getName();

            List<String> labels = taxonomy.getDimensions();
            return labels != null && index < labels.size() ? labels.get(index - 1)
                            : MessageFormat.format(Messages.LabelLevelNameNumber, taxonomy.getName(), option);
        }

        @Override
        public String getMenuLabel(Integer option)
        {
            int index = option;

            if (index == SHOW_FULL_CLASSIFICATION)
                return Messages.LabelFullClassification;

            List<String> labels = taxonomy.getDimensions();
            return labels != null && index <= labels.size() ? labels.get(index - 1)
                            : MessageFormat.format(Messages.LabelLevelNumber, option);
        }

        @Override
        public String getDescription(Integer option)
        {
            return null;
        }

        @Override
        public boolean canCreateNewOptions()
        {
            return false;
        }

        @Override
        public Integer createNewOption(Shell shell)
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

                if (option == SHOW_FULL_CLASSIFICATION)
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

    private static final int SHOW_FULL_CLASSIFICATION = 100;

    public TaxonomyColumn(final Taxonomy taxonomy)
    {
        super(taxonomy.getId(), taxonomy.getName(), SWT.LEFT, 120);

        TaxonomyLabelProvider labelProvider = new TaxonomyLabelProvider(taxonomy);

        setGroupLabel(Messages.ColumnTaxonomy);
        setOptions(new TaxonomyOptions(taxonomy));
        setLabelProvider(labelProvider);
        setComparator(new TaxonomyComparator(labelProvider));
    }

    private class TaxonomyComparator implements Comparator<Object>
    {
        private final TaxonomyLabelProvider labelProvider;

        public TaxonomyComparator(TaxonomyLabelProvider labelProvider)
        {
            this.labelProvider = labelProvider;
        }

        @Override
        public int compare(Object o1, Object o2)
        {
            Integer option = (Integer) ColumnViewerSorter.SortingContext.getColumnOption();

            String s1 = labelProvider.getText(o1, option);
            String s2 = labelProvider.getText(o2, option);

            if (s1 == null && s2 == null)
                return 0;
            else if (s1 == null)
                return -1;
            else if (s2 == null)
                return 1;

            return TextUtil.compare(s1, s2);
        }
    }
}
