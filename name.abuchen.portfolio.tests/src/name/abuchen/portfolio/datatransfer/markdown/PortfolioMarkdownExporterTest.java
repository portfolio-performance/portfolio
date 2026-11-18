package name.abuchen.portfolio.datatransfer.markdown;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.markdown.PortfolioMarkdownExporter.TransactionScope;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TaxonomyBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.money.DiscreetMode;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class PortfolioMarkdownExporterTest
{
    private static final LocalDate TODAY = LocalDate.parse("2026-06-30");

    private Client createClient()
    {
        var client = new Client();

        var taxonomy = new TaxonomyBuilder() //
                        .addClassification("equity") //
                        .addClassification("bonds") //
                        .addTo(client);
        taxonomy.setName("Asset Classes");
        taxonomy.getClassificationById("equity").setWeight(7000);
        taxonomy.getClassificationById("bonds").setWeight(3000);

        var alpha = new SecurityBuilder() //
                        .addPrice("2025-01-02", 100_00000000L) //
                        .addPrice("2025-12-31", 110_00000000L) //
                        .addPrice("2026-06-30", 120_00000000L) //
                        .assign(taxonomy, "equity") //
                        .addTo(client);
        alpha.setName("Alpha | World ETF");
        alpha.setIsin("IE00ALPHA001");
        alpha.setTickerSymbol("ALPH");

        var beta = new SecurityBuilder() //
                        .addPrice("2025-02-01", 100_00000000L) //
                        .addPrice("2025-08-01", 120_00000000L) //
                        .addPrice("2026-06-30", 125_00000000L) //
                        .assign(taxonomy, "equity", 6000) //
                        .assign(taxonomy, "bonds", 4000) //
                        .addTo(client);
        beta.setName("Beta Fund");
        beta.setIsin("IE00BETA0001");
        beta.setTickerSymbol("BETA");

        var gamma = new SecurityBuilder() //
                        .addPrice("2026-06-29", 50_00000000L) //
                        .addTo(client);
        gamma.setName("Gamma Watch");

        var watchlist = new Watchlist();
        watchlist.setName("Ideas");
        watchlist.addSecurity(gamma);
        client.addWatchlist(watchlist);

        var cash = new AccountBuilder() //
                        .deposit_("2025-01-02", 10_000_00) //
                        .dividend("2025-09-01", 30_00, alpha) //
                        .addTo(client);
        cash.setName("Checking");

        var savings = new AccountBuilder().addTo(client);
        savings.setName("Savings");

        var transfer = new AccountTransferEntry(cash, savings);
        transfer.setDate(LocalDateTime.parse("2025-03-01T00:00"));
        transfer.setAmount(500_00);
        transfer.setCurrencyCode("EUR");
        transfer.insert();

        var depot = new PortfolioBuilder(cash) //
                        .buy(alpha, "2025-01-03", 50_00000000L, 5_010_00, 10_00, 0) //
                        .buy(beta, "2025-02-01", 10_00000000L, 1_000_00) //
                        .sell(beta, "2025-08-01", 10_00000000L, 1_200_00) //
                        .addTo(client);
        depot.setName("Main Depot");

        var plan = new InvestmentPlan("Monthly Alpha");
        plan.setSecurity(alpha);
        plan.setPortfolio(depot);
        plan.setAccount(cash);
        plan.setType(InvestmentPlan.Type.PURCHASE_OR_DELIVERY);
        plan.setStart(LocalDateTime.parse("2026-07-01T00:00"));
        plan.setInterval(1);
        plan.setAmount(100_00);
        client.addPlan(plan);

        return client;
    }

    private PortfolioMarkdownExporter exporter(Client client)
    {
        return new PortfolioMarkdownExporter(client, new TestCurrencyConverter(), "test").withValuationDate(TODAY);
    }

    @Test
    public void testDocumentStructure()
    {
        var markdown = exporter(createClient()).withGenerator("Portfolio Performance 1.2.3").generate();

        assertThat(markdown, containsString("# Portfolio report: test"));
        assertThat(markdown, containsString("- Valuation date: 2026-06-30"));
        assertThat(markdown, containsString("- Reporting currency: EUR"));
        assertThat(markdown, containsString("- First transaction: 2025-01-02"));
        assertThat(markdown, containsString("- Generated by: Portfolio Performance 1.2.3"));

        for (var heading : new String[] { "## How to read this document", "## Summary", "## Asset allocation",
                        "## Current holdings", "## Cash accounts", "## Portfolio breakdown", "## Performance",
                        "### By period", "### By calendar year", "## Security performance", "## Market data",
                        "## Investment plans", "## Watchlists", "## Transactions", "## Glossary" })
            assertThat(heading, markdown, containsString(heading));
    }

    @Test
    public void testHoldings()
    {
        var markdown = exporter(createClient()).generate();

        // 50 shares at 120.00 = 6000.00 of total 10_000 + 30 + ... assets; cost
        // includes the fees
        assertThat(markdown, containsString(
                        "| Alpha \\| World ETF | IE00ALPHA001 | ALPH | EUR | 50 | 120.00 | 2026-06-30 | 6000.00 |"));
        assertThat(markdown, containsString("| 5010.00 | 990.00 | 19.76% |"));

        // sold security is not listed as holding but in the performance table
        assertThat(markdown, not(containsString("| Beta Fund | IE00BETA0001 | BETA | EUR | 10 |")));
        assertThat(markdown, containsString("| Beta Fund | sold | 200.00 |"));
    }

    @Test
    public void testAllocationWithTargets()
    {
        var markdown = exporter(createClient()).generate();

        assertThat(markdown, containsString("### Asset Classes"));
        assertThat(markdown, containsString(
                        "| Category | Value (EUR) | Share | Target weight | Target value (EUR) | Difference (EUR) |"));

        // only the securities are assigned (6000.00); the targets of 70% /
        // 30% apply to this assigned value, cash is not assigned
        assertThat(markdown, containsString("| equity | 6000.00 | 53.48% | 70.00% | 4200.00 | 1800.00 |"));
        assertThat(markdown, containsString("| bonds | 0.00 | 0.00% | 30.00% | 1800.00 | -1800.00 |"));
        assertThat(markdown, containsString("| (unassigned) | 5220.00 | 46.52% |"));
        assertThat(markdown, containsString("| equity | Alpha \\| World ETF | 100.00% | 6000.00 |"));
    }

    @Test
    public void testTransactionsAreListedOnce()
    {
        var markdown = exporter(createClient()).generate();

        assertThat(markdown, containsString("| 2025-01-03 | BUY | Alpha \\| World ETF | 50 | 100.00 | 5000.00 | 10.00 |"));
        assertThat(markdown, containsString("| 2025-08-01 | SELL | Beta Fund | 10 | 120.00 |"));
        assertThat(markdown, containsString("| 2025-03-01 | TRANSFER |"));
        assertThat(markdown, containsString("| Checking | Savings |"));

        // one transfer, two buys/sells -> 'TRANSFER' only once, no TRANSFER_IN
        assertThat(markdown, not(containsString("TRANSFER_IN")));
        assertThat(count(markdown, "| TRANSFER |"), is(1));
    }

    @Test
    public void testTransactionScope()
    {
        var client = createClient();

        var none = exporter(client).withTransactionScope(TransactionScope.NONE).generate();
        assertThat(none, containsString("The transaction list was omitted"));
        assertThat(none, not(containsString("| BUY |")));

        var recent = exporter(client).withTransactionScope(TransactionScope.LAST_12_MONTHS).generate();
        assertThat(recent, not(containsString("| 2025-01-03 | BUY |")));
    }

    @Test
    public void testOutputDoesNotDependOnLocale()
    {
        var client = createClient();
        var defaultLocale = Locale.getDefault();

        try
        {
            Locale.setDefault(Locale.US);
            var us = exporter(client).generate();

            Locale.setDefault(Locale.GERMANY);
            var de = exporter(client).generate();

            org.junit.Assert.assertEquals(us, de);
        }
        finally
        {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void testEmptyClient() throws Exception
    {
        var out = new ByteArrayOutputStream();
        exporter(new Client()).export(out);

        var markdown = out.toString(StandardCharsets.UTF_8);
        assertThat(markdown, containsString("# Portfolio report: test"));
        assertThat(markdown, containsString("does not contain any transactions"));
    }

    @Test
    public void testPeriodTableIsConsistent()
    {
        var markdown = exporter(createClient()).generate();

        for (var label : new String[] { "Since first transaction", "Last 12 months", "2025", "2026 (to date)" })
        {
            var cells = markdown.lines().filter(l -> l.startsWith("| " + label + " |")).findFirst().orElseThrow()
                            .split("\\|");

            // cells[0] is empty, then period, from, to, start, ...
            var start = new BigDecimal(cells[4].trim());
            var deposits = new BigDecimal(cells[5].trim());
            var gains = new BigDecimal(cells[6].trim());
            var earnings = new BigDecimal(cells[7].trim());
            var fees = new BigDecimal(cells[8].trim());
            var taxes = new BigDecimal(cells[9].trim());
            var end = new BigDecimal(cells[10].trim());

            var calculated = start.add(deposits).add(gains).add(earnings).subtract(fees).subtract(taxes);
            assertThat(label, calculated, comparesEqualTo(end));
        }
    }

    @Test
    public void testForeignCurrency()
    {
        var client = new Client();

        var account = new AccountBuilder("USD").deposit_("2015-01-05", 1_000_00).addTo(client);
        account.setName("US Cash");

        var security = new SecurityBuilder("USD") //
                        .addPrice("2015-01-05", 100_00000000L) //
                        .addPrice("2015-01-16", 110_00000000L) //
                        .addTo(client);
        security.setName("US Stock");

        var portfolio = new PortfolioBuilder(account).addTo(client);

        var buy = new BuySellEntry(portfolio, account);
        buy.setType(PortfolioTransaction.Type.BUY);
        buy.setDate(LocalDateTime.parse("2015-01-05T00:00"));
        buy.setSecurity(security);
        buy.setShares(10_00000000L);
        buy.setMonetaryAmount(Money.of("USD", 1_000_00));
        buy.insert();

        var markdown = new PortfolioMarkdownExporter(client, new TestCurrencyConverter(), "fx")
                        .withValuationDate(LocalDate.parse("2015-01-16")).generate();

        // 1100.00 USD at 1.1588 USD per EUR
        assertThat(markdown, containsString("| US Stock | DE0001 | DAX.DE | USD | 10 | 110.00 | 2015-01-16 | 949.26 |"));
        assertThat(markdown, containsString("| US Cash | USD | 0.00 | 0.00 |"));
        assertThat(markdown, containsString("| 2015-01-05 | BUY | US Stock | 10 | 100.00 | 1000.00 |"));
    }

    @Test
    public void testDataQualityNotes()
    {
        var client = new Client();

        // no deposit -> negative balance
        var cash = new AccountBuilder().addTo(client);
        cash.setName("Overdrawn");

        var security = new SecurityBuilder().addPrice("2026-06-01", 10_00000000L).addTo(client);
        security.setName("Stale Stock");
        new PortfolioBuilder(cash).buy(security, "2026-05-01", 10_00000000L, 100_00).addTo(client);

        var markdown = exporter(client).generate();

        assertThat(markdown, containsString("## Data quality notes"));
        assertThat(markdown, containsString("The latest price of 'Stale Stock' is from 2026-06-01 (29 days old)."));
        assertThat(markdown, containsString("The cash account 'Overdrawn' has a negative balance (-100.00 EUR)."));
    }

    @Test
    public void testDuplicateSecurityNamesAreDisambiguated()
    {
        var client = new Client();
        var cash = new AccountBuilder().deposit_("2026-01-01", 1_000_00).addTo(client);

        var first = new SecurityBuilder().addPrice("2026-06-30", 10_00000000L).addTo(client);
        first.setName("Same Name");
        first.setTickerSymbol("AAA");
        var second = new SecurityBuilder().addPrice("2026-06-30", 10_00000000L).addTo(client);
        second.setName("Same Name");
        second.setTickerSymbol("BBB");

        new PortfolioBuilder(cash).buy(first, "2026-02-01", 1_00000000L, 10_00)
                        .buy(second, "2026-02-02", 1_00000000L, 10_00).addTo(client);

        var markdown = exporter(client).generate();

        assertThat(markdown, containsString("| Same Name [AAA] |"));
        assertThat(markdown, containsString("| Same Name [BBB] |"));
    }

    @Test
    public void testSecurityNotesAndNoTargets()
    {
        var client = new Client();
        var taxonomy = new TaxonomyBuilder().addClassification("equity").addTo(client);
        taxonomy.setName("Types");
        taxonomy.getClassificationById("equity").setWeight(0);

        var cash = new AccountBuilder().deposit_("2026-01-01", 1_000_00).addTo(client);
        var security = new SecurityBuilder().addPrice("2026-06-30", 10_00000000L).assign(taxonomy, "equity")
                        .addTo(client);
        security.setName("Noted");
        security.setNote("Long-term\nholding | do not sell");
        new PortfolioBuilder(cash).buy(security, "2026-02-01", 10_00000000L, 100_00).addTo(client);

        var markdown = exporter(client).generate();

        assertThat(markdown, containsString("- Noted: Long-term holding \\| do not sell"));

        // no targets configured: the target columns are not shown
        assertThat(markdown, containsString("| Category | Value (EUR) | Share |\n"));
        assertThat(markdown, not(containsString("Target weight")));
    }

    @Test
    public void testPortfolioTransferIsListedOnce()
    {
        var client = new Client();
        var cash = new AccountBuilder().deposit_("2026-01-01", 1_000_00).addTo(client);
        var security = new SecurityBuilder().addPrice("2026-06-30", 10_00000000L).addTo(client);
        security.setName("Moved");

        var from = new PortfolioBuilder(cash).buy(security, "2026-02-01", 10_00000000L, 100_00).addTo(client);
        from.setName("Broker A");
        var to = new PortfolioBuilder(cash).addTo(client);
        to.setName("Broker B");

        var transfer = new PortfolioTransferEntry(from, to);
        transfer.setDate(LocalDateTime.parse("2026-03-01T00:00"));
        transfer.setSecurity(security);
        transfer.setShares(10_00000000L);
        transfer.setAmount(100_00);
        transfer.setCurrencyCode("EUR");
        transfer.insert();

        var markdown = exporter(client).generate();

        assertThat(count(markdown, "| TRANSFER |"), is(1));
        assertThat(markdown, containsString("| 2026-03-01 | TRANSFER | Moved | 10 |"));
        assertThat(markdown, containsString("| Broker A |  | Broker B |"));
    }

    @Test
    public void testRealizedGainIncludesFeesAndTaxes()
    {
        var client = new Client();
        var cash = new AccountBuilder().deposit_("2025-01-02", 10_000_00).addTo(client);

        var security = new SecurityBuilder() //
                        .addPrice("2025-01-02", 100_00000000L) //
                        .addPrice("2026-06-30", 120_00000000L) //
                        .addTo(client);
        security.setName("Delta Stock");

        // gross 1000.00 + 10.00 fees; gross 1200.00 - 10.00 fees
        new PortfolioBuilder(cash) //
                        .buy(security, "2025-01-03", 10_00000000L, 1_010_00, 10_00, 0) //
                        .sell(security, "2026-02-02", 10_00000000L, 1_190_00, 10_00) //
                        .addTo(client);

        var markdown = exporter(client).generate();

        // total gain and realized gain are both after the fees of the buy
        // and the sell: 1190.00 - 1010.00
        assertThat(markdown, containsString("| Delta Stock | sold | 180.00 | 17.82% | 180.00 |  |"));
        assertThat(markdown, containsString("Total gain = realized gain + unrealized gain + dividends - fees and taxes"));
        assertThat(markdown, not(containsString("they are already reflected in the gains")));
    }

    @Test
    public void testTransactionsAfterValuationDate()
    {
        var client = createClient();
        var cash = client.getAccounts().get(0);
        cash.addTransaction(new AccountTransaction(LocalDateTime.parse("2026-07-15T00:00"), "EUR", 50_00, null,
                        AccountTransaction.Type.DEPOSIT));

        var recent = exporter(client).withTransactionScope(TransactionScope.LAST_12_MONTHS).generate();
        assertThat(recent, not(containsString("| 2026-07-15 |")));
        assertThat(recent, containsString("transactions dated after 2025-06-30 and up to 2026-06-30"));

        var all = exporter(client).withTransactionScope(TransactionScope.ALL).generate();
        assertThat(all, containsString("| 2026-07-15 | DEPOSIT |"));
        assertThat(all, containsString("- First transaction: 2025-01-02"));
    }

    @Test
    public void testOnlyTransactionsAfterValuationDate()
    {
        var client = new Client();
        new AccountBuilder().deposit_("2026-08-01", 1_000_00).addTo(client);

        var markdown = exporter(client).generate();

        assertThat(markdown, containsString("does not contain any transactions up to the valuation date"));
        assertThat(markdown, not(containsString("could not be created completely")));
        assertThat(markdown, not(containsString("- First transaction:")));
    }

    @Test
    public void testTransactionPriceIsInTransactionCurrency()
    {
        var markdown = exporter(createClient()).generate();

        assertThat(markdown, containsString(
                        "Amounts and prices in the transaction list are in the currency of the transaction (column 'Currency')."));
        assertThat(markdown, not(containsString("Prices are in the currency of the security.")));
    }

    @Test
    public void testAccountTransfers()
    {
        var client = new Client();

        var euro = new AccountBuilder().deposit_("2026-01-01", 1_000_00).addTo(client);
        euro.setName("Euro Cash");
        var dollar = new AccountBuilder("USD").addTo(client);
        dollar.setName("Dollar Cash");

        var transfer = new AccountTransferEntry(euro, dollar);
        transfer.setDate(LocalDateTime.parse("2026-02-01T00:00"));
        transfer.setAmount(100_00);
        transfer.setCurrencyCode("EUR");
        transfer.getTargetTransaction().setAmount(110_00);
        transfer.getTargetTransaction().setCurrencyCode("USD");
        transfer.insert();

        // incoming transfer without the outgoing side
        euro.addTransaction(new AccountTransaction(LocalDateTime.parse("2026-03-01T00:00"), "EUR", 20_00, null,
                        AccountTransaction.Type.TRANSFER_IN));

        var markdown = exporter(client).generate();

        assertThat(markdown, containsString("| 2026-02-01 | TRANSFER |"));
        assertThat(markdown, containsString("| Euro Cash | Dollar Cash (credited 110.00 USD) |"));
        assertThat(markdown, containsString("| 2026-03-01 | TRANSFER |"));
        assertThat(markdown, not(containsString("TRANSFER_IN")));
        assertThat(count(markdown, "| TRANSFER |"), is(2));
    }

    @Test
    public void testDuplicateSecurityNamesFallBackToIsinAndNumber()
    {
        var client = new Client();
        var taxonomy = new TaxonomyBuilder().addClassification("equity").addTo(client);
        taxonomy.setName("Types");

        var cash = new AccountBuilder().deposit_("2026-01-01", 1_000_00).addTo(client);

        var first = new SecurityBuilder().addPrice("2026-06-30", 10_00000000L).assign(taxonomy, "equity")
                        .addTo(client);
        first.setName("Same Name");
        first.setTickerSymbol("AAA");
        var second = new SecurityBuilder().addPrice("2026-06-30", 10_00000000L).assign(taxonomy, "equity")
                        .addTo(client);
        second.setName("Same Name");
        second.setTickerSymbol(null);
        second.setIsin("IE00SECOND01");
        var third = new SecurityBuilder().addPrice("2026-06-01", 10_00000000L).addTo(client);
        third.setName("Same Name");
        third.setTickerSymbol(null);
        third.setIsin(null);

        new PortfolioBuilder(cash).buy(first, "2026-02-01", 1_00000000L, 10_00)
                        .buy(second, "2026-02-02", 1_00000000L, 10_00)
                        .buy(third, "2026-02-03", 1_00000000L, 10_00).addTo(client);

        var markdown = exporter(client).generate();

        assertThat(markdown, containsString("| Same Name [AAA] |"));
        assertThat(markdown, containsString("| Same Name [IE00SECOND01] |"));
        assertThat(markdown, containsString("| Same Name [EUR #3] |"));

        // assignments and data quality notes use the same labels
        assertThat(markdown, containsString("| equity | Same Name [AAA] | 100.00% | 10.00 |"));
        assertThat(markdown, containsString("| equity | Same Name [IE00SECOND01] | 100.00% | 10.00 |"));
        assertThat(markdown, containsString("The latest price of 'Same Name [EUR #3]' is from 2026-06-01"));
    }

    @Test
    public void testAssignmentsToTaxonomyRoot()
    {
        var client = new Client();
        var taxonomy = new TaxonomyBuilder().addTo(client);
        taxonomy.setName("Flat");

        var cash = new AccountBuilder().deposit_("2026-01-01", 1_000_00).addTo(client);
        var security = new SecurityBuilder().addPrice("2026-06-30", 10_00000000L).addTo(client);
        security.setName("Rooted");
        taxonomy.getRoot().addAssignment(new Classification.Assignment(security, Classification.ONE_HUNDRED_PERCENT));
        new PortfolioBuilder(cash).buy(security, "2026-02-01", 10_00000000L, 100_00).addTo(client);

        var markdown = exporter(client).generate();

        assertThat(markdown, containsString("### Flat"));
        assertThat(markdown, containsString("| (taxonomy root) | 100.00 | 10.00% |"));
        assertThat(markdown, containsString("| (unassigned) | 900.00 | 90.00% |"));
        assertThat(markdown, containsString("| (taxonomy root) | Rooted | 100.00% | 100.00 |"));
    }

    @Test
    public void testSortingDoesNotDependOnLocale()
    {
        var client = new Client();
        var cash = new AccountBuilder().deposit_("2026-01-01", 1_000_00).addTo(client);

        var jFund = new SecurityBuilder().addPrice("2026-06-30", 10_00000000L).addTo(client);
        jFund.setName("J Fund");
        var iFund = new SecurityBuilder().addPrice("2026-06-30", 10_00000000L).addTo(client);
        iFund.setName("I Fund");

        new PortfolioBuilder(cash).buy(jFund, "2026-02-01", 1_00000000L, 10_00)
                        .buy(iFund, "2026-02-02", 1_00000000L, 10_00).addTo(client);

        var defaultLocale = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.US);
            var us = exporter(client).generate();

            // in Turkish, "I" is lower-cased to the dotless i which sorts
            // after "j"
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            var tr = exporter(client).generate();

            org.junit.Assert.assertEquals(us, tr);

            var marketData = us.substring(us.indexOf("## Market data"));
            assertThat(marketData.indexOf("| I Fund |") < marketData.indexOf("| J Fund |"), is(true));
        }
        finally
        {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void testTextEscapesBackslashAndPipe()
    {
        // a backslash in front of a pipe must not cancel the escaping
        assertThat(MarkdownFormat.text("C:\\| x"), is("C:\\\\\\| x"));
        assertThat(MarkdownFormat.text("a\nb\t|c"), is("a b \\|c"));

        // shortened before escaping: no dangling backslash before "..."
        var longText = "a".repeat(296) + "\\|" + "b".repeat(10);
        assertThat(MarkdownFormat.text(longText), is("a".repeat(296) + "\\\\..."));
    }

    @Test
    public void testDiscreetModeHidesAmountsSharesAndNotes()
    {
        var client = createClient();
        var alpha = client.getSecurities().get(0);
        alpha.setNote("bought for 5010 EUR");
        client.getAccounts().get(0).setNote("IBAN and balance");

        var markdown = exporter(client).withDiscreetMode(true).generate();

        assertThat(markdown, containsString("- Discreet mode: amounts and numbers of shares are hidden"));
        assertThat(markdown, containsString("The report was created in discreet mode"));

        // shares and value hidden; price, date and weights are real
        assertThat(markdown, containsString(
                        "| Alpha \\| World ETF | IE00ALPHA001 | ALPH | EUR | *** | 120.00 | 2026-06-30 | *** | 53.48% | *** | *** | 19.76% |"));
        assertThat(markdown, containsString("| equity | *** | 53.48% | 70.00% | *** | *** |"));
        assertThat(markdown, containsString("| Since first transaction | 2025-01-01 | 2026-06-30 | *** | *** |"));
        assertThat(markdown, containsString("| 2025-01-03 | BUY | Alpha \\| World ETF | *** | 100.00 | *** | *** |"));

        for (var amount : new String[] { "| 6000.00 |", "| 5010.00 |", "| 10000.00 |", "| 500.00 |", "| 30.00 |",
                        "| 50 |", "| 990.00 |" })
            assertThat(amount, markdown, not(containsString(amount)));

        assertThat(markdown, not(containsString("bought for 5010 EUR")));
        assertThat(markdown, not(containsString("IBAN and balance")));
    }

    @Test
    public void testGlobalDiscreetModeDoesNotChangeTheExport()
    {
        var client = createClient();
        try
        {
            DiscreetMode.setActive(true);
            var markdown = exporter(client).generate();

            assertThat(markdown, containsString("| 2026-06-30 | 6000.00 |"));
            assertThat(markdown, not(containsString("- Discreet mode:")));
        }
        finally
        {
            DiscreetMode.setActive(false);
        }
    }

    private static int count(String text, String token)
    {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0)
        {
            count++;
            index += token.length();
        }
        return count;
    }
}
