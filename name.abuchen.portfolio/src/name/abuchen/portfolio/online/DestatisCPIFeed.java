package name.abuchen.portfolio.online;

import java.io.IOException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.ConsumerPriceIndex;

import org.htmlparser.Node;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.util.ParserException;

public class DestatisCPIFeed implements CPIFeed
{
    @Override
    public List<ConsumerPriceIndex> getConsumerPriceIndices() throws IOException
    {
        try
        {
            disableCertificateValidation();

            URL url = new URL(
                            "https://www.destatis.de/DE/ZahlenFakten/GesamtwirtschaftUmwelt/Preise/Verbraucherpreisindizes/Tabellen_/VerbraucherpreiseKategorien.html"); //$NON-NLS-1$
            Lexer lexer = new Lexer(url.openConnection());

            List<ConsumerPriceIndex> prices = new Visitor().visit(lexer);
            if (prices.isEmpty())
                throw new IOException(Messages.MsgResponseContainsNoIndices);

            return prices;
        }
        catch (ParserException e)
        {
            throw new IOException(e);
        }
    }

    static class Visitor
    {
        private List<ConsumerPriceIndex> prices;
        private ConsumerPriceIndex price;
        private int year = 0;

        private boolean insideTable = false;
        private boolean insideTBody = false;
        private boolean insideRow = false;
        private boolean insideColumn = false;
        private boolean insideAbbr = false;
        private boolean hasRowspan = false;
        private int columnIndex = -1;

        public boolean tag(Tag tag) throws IOException
        {
            return true;
        }

        public boolean table(Tag tag) throws IOException
        {
            if (!insideTable)
            {
                insideTable = true;
                return true;
            }

            insideTable = false;
            if (tag.isEndTag())
                return false;

            throw new IOException("Unexpected table tag " + tag); //$NON-NLS-1$
        }

        public boolean tbody(Tag tag) throws IOException
        {
            if (!insideTable)
                return true;

            if (!tag.isEndTag())
            {
                insideTBody = true;
            }
            else
            {
                insideTBody = false;
            }

            return true;
        }

        public boolean tr(Tag tag) throws IOException
        {
            if (!insideTBody)
                return true;

            if (!tag.isEndTag())
            {
                insideRow = true;
                price = new ConsumerPriceIndex();
            }
            else
            {
                insideRow = false;
                columnIndex = -1;

                if (price != null)
                    prices.add(price);
                price = null;
            }
            return true;
        }

        public boolean td(Tag tag) throws IOException
        {
            if (!insideRow)
                return true;

            insideColumn = !tag.isEndTag();
            if (insideColumn)
            {
                columnIndex++;

                if (columnIndex == 0)
                    this.hasRowspan = tag.getAttribute("ROWSPAN") != null; //$NON-NLS-1$
            }

            return true;
        }

        public boolean abbr(Tag tag) throws IOException
        {
            if (!insideColumn)
                return true;

            insideAbbr = !tag.isEndTag();
            return true;
        }

        public boolean text(Text text) throws IOException
        {
            if (!insideColumn)
                return true;

            if (columnIndex == 0 && hasRowspan)
                year = Integer.parseInt(text.getText());
            price.setYear(year);

            int col = hasRowspan ? columnIndex : columnIndex + 1;

            switch (col)
            {
                case 1:
                    if (insideAbbr)
                        price.setMonth(parseMonth(text.getText()).getValue());
                    break;
                case 2:
                    price.setIndex(parseIndex(text.getText()));
                    break;
            }

            return true;
        }

        private int parseIndex(String text) throws IOException
        {
            try
            {
                DecimalFormat fmt = new DecimalFormat("0.##", new DecimalFormatSymbols(Locale.GERMANY)); //$NON-NLS-1$
                Number q = fmt.parse(text);
                return (int) (q.doubleValue() * 100);
            }
            catch (ParseException e)
            {
                throw new IOException(e);
            }
        }

        @SuppressWarnings("nls")
        private Month parseMonth(String text) throws IOException
        {
            if ("Jan".equals(text))
                return Month.JANUARY;
            else if ("Feb".equals(text))
                return Month.FEBRUARY;
            else if ("Mär".equals(text))
                return Month.MARCH;
            else if ("Apr".equals(text))
                return Month.APRIL;
            else if ("Mai".equals(text))
                return Month.MAY;
            else if ("Jun".equals(text))
                return Month.JUNE;
            else if ("Jul".equals(text))
                return Month.JULY;
            else if ("Aug".equals(text))
                return Month.AUGUST;
            else if ("Sep".equals(text))
                return Month.SEPTEMBER;
            else if ("Okt".equals(text))
                return Month.OCTOBER;
            else if ("Nov".equals(text))
                return Month.NOVEMBER;
            else if ("Dez".equals(text))
                return Month.DECEMBER;

            throw new IOException("Unknown month: " + text);
        }

        public List<ConsumerPriceIndex> visit(Lexer lexer) throws ParserException, IOException
        {
            this.prices = new ArrayList<ConsumerPriceIndex>();

            boolean doContinue = true;

            Node node = lexer.nextNode();
            while (node != null && doContinue)
            {
                if (node instanceof Tag)
                {
                    Tag tag = (Tag) node;
                    String tagName = tag.getTagName();

                    if ("TABLE".equals(tagName)) //$NON-NLS-1$
                        doContinue = table(tag);
                    else if ("TBODY".equals(tagName)) //$NON-NLS-1$
                        doContinue = tbody(tag);
                    else if ("TR".equals(tagName)) //$NON-NLS-1$
                        doContinue = tr(tag);
                    else if ("TH".equals(tagName)) //$NON-NLS-1$
                        doContinue = td(tag);
                    else if ("TD".equals(tagName)) //$NON-NLS-1$
                        doContinue = td(tag);
                    else if ("ABBR".equals(tagName)) //$NON-NLS-1$
                        doContinue = abbr(tag);
                    else
                        doContinue = tag(tag);
                }
                else if (node instanceof Text)
                {
                    doContinue = text((Text) node);
                }
                node = lexer.nextNode();
            }

            return this.prices;
        }
    }

    private static boolean certificateValidationDisabled = false;

    private static void disableCertificateValidation()
    {
        if (certificateValidationDisabled)
            return;

        // http://stackoverflow.com/questions/875467/java-client-certificates-over-https-ssl/876785#876785

        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager()
        {
            public X509Certificate[] getAcceptedIssuers()
            {
                return new X509Certificate[0];
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType)
            {}

            public void checkServerTrusted(X509Certificate[] certs, String authType)
            {}
        } };

        // Install the all-trusting trust manager
        try
        {
            SSLContext sc = SSLContext.getInstance("SSL"); //$NON-NLS-1$
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        }
        catch (Exception ignore)
        {}

        certificateValidationDisabled = true;
    }
}
