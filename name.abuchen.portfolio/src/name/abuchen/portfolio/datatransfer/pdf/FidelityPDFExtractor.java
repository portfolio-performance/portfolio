package name.abuchen.portfolio.datatransfer.pdf;

import java.time.LocalDateTime;
import java.util.Locale;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class FidelityPDFExtractor extends AbstractPDFExtractor
{
    public FidelityPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("FIDELITY"); //$NON-NLS-1$

        addSummaryStatementBuySellTransaction();
        addBuySellTransaction();
    }

    private void addSummaryStatementBuySellTransaction()
    {
        // line must start with this!
        DocumentType type = new DocumentType("Transaction Confirmation");
        this.addDocumentTyp(type);

        Block block = new Block("REFERENCE NO\\. TYPE REG\\.REP\\. TRADE DATE SETTLEMENT DATE CUSIP NO\\. ORDER NO\\.",
                        "^(?!REFERENCE)(?!DESCRIPTION)[A-Z]{2,5}.*$");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })
                        /* @formatter:off
                         * REFERENCE NO. TYPE REG.REP. TRADE DATE SETTLEMENT DATE CUSIP NO. ORDER NO.
                         * 20123-1XXXXX 1* WK# 01-04-21 01-06-21 46428Q109 20123-XXXXX
                         * DESCRIPTION and DISCLOSURES
                         * You Bought ISHARES SILVER TR ISHARES Principal Amount       1,011.60
                         *        40 WE HAVE ACTED AS AGENT. Settlement Amount       1,011.60
                         *               at    25.2900 FBS receives compensation from the fund's advisor or its affiliates in connection with an
                         * Symbol: exclusive, long-term marketing program that includes the promotion of this security and
                         * SLV other iShares funds, and the inclusion of iShares funds in certain platforms and investment
                         * @formatter:on
                         */

                        .section("date") //
                        .match("^.* \\w{2}# (?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{2}) .*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("type", "gross", "tickerSymbol", "name") //
                        .match("^You (?<type>Bought|Sold) (?<name>.*) Principal Amount\\s+(?<gross>[\\.,\\d]+)$") //
                        .match("^(?!REFERENCE)(?!DESCRIPTION)(?<tickerSymbol>[A-Z]{2,5}).*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode("USD"));
                            v.put("currency", "USD");
                            t.setSecurity(getOrCreateSecurity(v));
                            if ("Sold".equals(v.get("type")))
                            {
                                    t.setType(PortfolioTransaction.Type.SELL);
                            }
                        })

                        .section("amount") //
                        .match(".*Settlement Amount\\s+(?<amount>[\\.,\\d]+)$")
                        .assign((t, v) -> t.setAmount(asAmount(v.get("amount"))))

                        .section("fee") //
                        .optional() //
                        .match(".*Activity Assessmen\\s?t\\s+F\\s?e\\s?e\\s+(?<fee>[\\.,\\d]+)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(asCurrencyCode("USD")),
                                                                            asAmount(v.get("fee")))));
                        })

                        .section("shares") //
                        .match("^\\s+(?<shares>[\\.,\\d]+)\\s.*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .wrap(BuySellEntryItem::new));
    }

    private void addBuySellTransaction()
    {
        // line must start with this!
        DocumentType type = new DocumentType("SECURITY DESCRIPTION");
        this.addDocumentTyp(type);

        Block block = new Block(
                        "CUSTOMER NO\\.\\s+PARTICIPANT ID\\. TYPE REG\\.REP\\. TRADE DATE SETTLEMENT DATE TRANS NO\\. CUSIP NO\\. ORIG\\.");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })
                        /* @formatter:off
                         * CUSTOMER NO.   PARTICIPANT ID. TYPE REG.REP. TRADE DATE SETTLEMENT DATE TRANS NO. CUSIP NO. ORIG.
                         * I00123456 1 WI# 12-12-23 12-14-23 K9T1Q9 11135F101
                         * YOU SOLD 14 AT 173.1100
                         *       EXPLANATION OF PROCEEDS
                         * SECURITY DESCRIPTION SYMBOL: AAPL Sale Proceeds     $2,423.54
                         * APPLE INC
                         * DETAILS: Total Fees          $0.13
                         * Sale Date: DEC/12/2023
                         * Proceeds Available: DEC/14/2023
                         * Plan Type: COMPANY STOCK PLAN
                         * Net Cash Proceeds¹     -$2,423.41
                         * @formatter:on
                         */

                        .section("date") //
                        .match("^Sale Date: (?<date>[\\w]{3}\\/[\\d]{2}\\/[\\d]{4})$")
                        .assign((t, v) -> {
                            String date = v.get("date");
                            // convert month to camel case
                            if (Character.isLetter(date.charAt(0)))
                            {
                                date = date.charAt(0) + date.substring(1, 3).toLowerCase() + date.substring(3);
                            }
                            t.setDate(asDate(date));
                        })

                        .section("shares") //
                        .match("^YOU SOLD (?<shares>[\\.,\\d]+) AT.*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("gross", "tickerSymbol") //
                        .match("SECURITY DESCRIPTION SYMBOL: (?<tickerSymbol>[A-Z]{2,5}) Sale Proceeds\\s+\\$(?<gross>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode("USD"));
                            v.put("currency", "USD");
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("amount") //
                        .match(".*Net Cash Proceeds.*\\$(?<amount>[\\.,\\d]+)$")
                        .assign((t, v) -> t.setAmount(asAmount((v.get("amount")))))

                        .section("fee") //
                        .optional() //
                        .match(".*Total Fees\\s+\\$(?<fee>[\\.,\\d]+)").assign((t, v) -> {
                            t.getPortfolioTransaction().addUnit(
                                            new Unit(Unit.Type.FEE, Money.of(asCurrencyCode(asCurrencyCode("USD")),
                                                            asAmount((v.get("fee"))))));
                        })

                        .wrap(BuySellEntryItem::new));
    }

    @Override
    public String getLabel()
    {
        return "Fidelity"; //$NON-NLS-1$
    }

    @Override
    protected long asAmount(String value)
    {
        String language = "de";
        String country = "DE";

        int lastDot = value.lastIndexOf(".");
        int lastComma = value.lastIndexOf(",");

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en";
            country = "US";
        }

        return ExtractorUtils.convertToNumberLong(value, Values.Amount, language, country);
    }

    @Override
    protected long asShares(String value)
    {
        String language = "de";
        String country = "DE";

        int lastDot = value.lastIndexOf(".");
        int lastComma = value.lastIndexOf(",");

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en";
            country = "US";
        }

        return ExtractorUtils.convertToNumberLong(value, Values.Share, language, country);
    }

    @Override
    protected LocalDateTime asDate(String value, Locale... hints)
    {
        return ExtractorUtils.asDate(value, Locale.US);
    }

}
