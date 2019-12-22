package name.abuchen.portfolio.checks;

public interface QuickFix
{
    QuickFix SEPARATOR = new QuickFix()
    {
        @Override
        public String getLabel()
        {
            return null;
        }

        @Override
        public String getDoneLabel()
        {
            return null;
        }

        @Override
        public void execute()
        {
            throw new UnsupportedOperationException();
        }
    };

    String getLabel();

    String getDoneLabel();

    void execute();
}
