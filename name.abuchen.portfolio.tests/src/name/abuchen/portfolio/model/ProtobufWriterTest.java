package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@SuppressWarnings("nls")
@RunWith(Parameterized.class)
public class ProtobufWriterTest
{
    private static final int CONTEXT_LENGTH = 100;

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> getFiles()
    {
        return Arrays.asList(new Object[][] { // NOSONAR
                        { "/issues/Issue1498FifoCrossPortfolio.xml" },
                        { "/issues/Issue1817CapitalGainsOnFilteredClientIfSecurityIsTransferred.xml" },
                        { "/issues/Issue1879DividendRateOfReturnPerYearWithSecurityInMultipleAccounts.xml" },
                        { "/issues/Issue1897AddFeesToDividendTransactions.xml" },
                        { "/issues/Issue1898TradeIRRwithPortfolioTransfers.xml" },
                        { "/issues/Issue1909FIFOCalculationOfSecurityPosition.xml" },
                        { "/issues/Issue371PurchaseValueWithTransfers.xml" },
                        { "/issues/Issue672CapitalGainsIfSecurityIsTransferred.xml" },
                        { "/issues/IssueCurrencyGainsRoundingError.xml" }, //
                        { "/scenarios/protobuf.xml" }, //
                        { "/scenarios/account_performance_tax_refund.xml" }, //
                        { "/scenarios/classification_test_case.xml" }, //
                        { "/scenarios/currency_sample.xml" }, //
                        { "/scenarios/security_performance_tax_refund_all_sold.xml" }, //
                        { "/scenarios/security_performance_tax_refund.xml" }, //
                        { "/scenarios/security_performance_with_missing_historical_quotes.xml" }, //
                        { "/scenarios/security_tax_and_fee_account_transactions.xml" }, //
                        { "/scenarios/volatility.xml" } });
    }

    private String file;

    public ProtobufWriterTest(String file)
    {
        this.file = file;
    }

    @Test
    public void compareAgainstXML() throws IOException
    {
        Client client = ClientFactory.load(ProtobufWriterTest.class.getResourceAsStream(file));

        // convert to binary format and back

        ProtobufWriter protobufWriter = new ProtobufWriter();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        protobufWriter.save(client, stream);
        stream.close();

        ByteArrayInputStream in = new ByteArrayInputStream(stream.toByteArray());
        Client newClient = protobufWriter.load(in);

        String expected = ClientTestUtilities.toString(client);
        String actual = ClientTestUtilities.toString(newClient);

        // output is too long for a quick analysis - reduce to CONTEXT_LENGTH
        // characters

        if (!expected.equals(actual))
        {
            int pos = ClientTestUtilities.indexOfDifference(expected, actual);

            // System.out.println("---- expected");
            // System.out.println(expected);
            // System.out.println("---- actual");
            // System.out.println(actual);

            assertThat("difference starting at character " + pos,
                            actual.substring(pos, Math.min(pos + CONTEXT_LENGTH, actual.length())),
                            is(expected.substring(pos, Math.min(pos + CONTEXT_LENGTH, expected.length()))));
        }
    }

}
