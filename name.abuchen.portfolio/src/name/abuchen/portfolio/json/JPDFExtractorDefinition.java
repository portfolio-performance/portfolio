package name.abuchen.portfolio.json;

import java.util.List;
import java.util.Map;

public class JPDFExtractorDefinition
{
    public static class JTransactionMatcher
    {
        private String startsWith;
        private String endsWith;
        private JTransaction.Type type;
        private List<JSection> sections;

        public String getStartsWith()
        {
            return startsWith;
        }

        public String getEndsWith()
        {
            return endsWith;
        }

        public JTransaction.Type getType()
        {
            return type;
        }

        public List<JSection> getSections()
        {
            return sections;
        }
    }

    public enum JSectionContext
    {
        SECURITY, UNIT
    }

    public static class JSection
    {
        private JSectionContext context;
        private List<String> pattern;
        private boolean isOptional;
        private Map<String, String> attributes;

        public JSectionContext getContext()
        {
            return context;
        }

        public List<String> getPattern()
        {
            return pattern;
        }

        public boolean isOptional()
        {
            return isOptional;
        }

        public Map<String, String> getAttributes()
        {
            return attributes;
        }
    }

    private int version;
    private String name;
    private String locale;
    private List<String> pattern;
    private List<JTransactionMatcher> transactions;

    public int getVersion()
    {
        return version;
    }

    public String getName()
    {
        return name;
    }

    public String getLocale()
    {
        return locale;
    }

    public List<String> getPattern()
    {
        return pattern;
    }

    public List<JTransactionMatcher> getTransactions()
    {
        return transactions;
    }

}
