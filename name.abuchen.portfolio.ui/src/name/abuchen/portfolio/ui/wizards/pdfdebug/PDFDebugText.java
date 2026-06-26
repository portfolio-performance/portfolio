package name.abuchen.portfolio.ui.wizards.pdfdebug;

import name.abuchen.portfolio.ui.PortfolioPlugin;

class PDFDebugText
{
    private PDFDebugText()
    {
    }

    @SuppressWarnings("nls")
    public static String build(String pdfBoxVersion, String extractedText)
    {
        var textBuilder = new StringBuilder();
        textBuilder.append("```").append("\n");
        textBuilder.append("PDFBox Version: ").append(String.valueOf(pdfBoxVersion)).append("\n");
        textBuilder.append("Portfolio Performance Version: ")
                        .append(PortfolioPlugin.getDefault().getBundle().getVersion().toString()) //
                        .append("\n");

        textBuilder.append("System: ") //
                        .append(System.getProperty("osgi.os", "unknown")).append(" | ")
                        .append(System.getProperty("osgi.arch", "unknown")).append(" | ")
                        .append(System.getProperty("java.vm.version", "unknown")).append(" | ")
                        .append(System.getProperty("java.vm.vendor", "unknown")).append("\n");

        textBuilder.append("-----------------------------------------\n");
        textBuilder.append(extractedText).append("\n");
        textBuilder.append("```");

        return textBuilder.toString();
    }
}
