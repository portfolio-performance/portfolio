package name.abuchen.portfolio.ui;

public interface UIConstants
{
    interface Part
    {
        String PORTFOLIO = "name.abuchen.portfolio.ui.part.portfolio"; //$NON-NLS-1$
        String ERROR_LOG = "name.abuchen.portfolio.ui.part.errorlog"; //$NON-NLS-1$
        String TEXT_VIEWER = "name.abuchen.portfolio.ui.part.textviewer"; //$NON-NLS-1$
    }

    interface PartStack
    {
        String MAIN = "name.abuchen.portfolio.ui.partstack.main"; //$NON-NLS-1$
    }

    interface Event
    {
        public interface Log
        {
            String CREATED = "errorlog/created"; //$NON-NLS-1$
            String CLEARED = "errorlog/cleared"; //$NON-NLS-1$
        }
    }

    interface Parameter
    {
        String FILE = "file"; //$NON-NLS-1$
    }
}
