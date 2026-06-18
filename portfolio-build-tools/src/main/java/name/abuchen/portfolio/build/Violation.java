package name.abuchen.portfolio.build;

record Violation(String path, String message)
{
    @Override
    public String toString()
    {
        return "[" + path + "] " + message; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
