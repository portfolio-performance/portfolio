package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class NEON3aPDFExtractor extends AbstractPDFExtractor
{
    public NEON3aPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("neon Switzerland AG");

        addDailyStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "neon Switzerland AG";
    }

    private void addDailyStatementTransaction()
    {
        var type = new DocumentType("Investment account simply3a: Daily statement", //
                        contextProvider -> contextProvider //
                                        .section("currency") //
                                        .match("^Currency (?<currency>[A-Z]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        addFundBuySellBlock(type);
        addDepositBlock(type);
        addManagementFeeBlock(type);
    }

    private void addFundBuySellBlock(DocumentType type)
    {
        var pdfTransaction = new Transaction<BuySellEntry>();

        var block = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Fund (buy|sell) .*$");
        block.setMaxSize(5);
        type.addBlock(block);
        block.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        // @formatter:off
                        // 24.11.2025 Fund buy 24.11.2025  10.00  990.00
                        // 04.12.2025 Fund sell 04.12.2025  0.15 -0.32
                        // @formatter:on
                        .section("date", "type", "amount") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) Fund (?<type>buy|sell) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}  (?<amount>[\\.'\\d]+) .*$") //
                        .assign((t, v) -> {
                            if ("sell".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);

                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Sc Inv.II Mo.Re.Opp
                        // Secur.Nr. 1/039,462,806 Secur. Cur CHF
                        // @formatter:on
                        .section("name", "valor", "currency") //
                        .match("^(?!Secur\\.Nr\\.)(?!Unit )(?!Rate )(?<name>.+)$") //
                        .match("^Secur\\.Nr\\. 1\\/(?<valor>[\\d,]+) Secur\\. Cur (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            v.put("isin", valorToIsin(v.get("valor")));
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Unit 0.102347
                        // @formatter:on
                        .section("shares") //
                        .match("^Unit (?<shares>[\\.'\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .wrap(BuySellEntryItem::new);
    }

    private void addDepositBlock(DocumentType type)
    {
        var pdfTransaction = new Transaction<AccountTransaction>();

        var block = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Deposit .*$");
        block.setMaxSize(1);
        type.addBlock(block);
        block.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 18.11.2025 Deposit 18.11.2025  1'000.00  1'000.00
                        // 08.01.2026 Deposit 06.01.2026  6'258.00  6'258.00
                        // @formatter:on
                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) Deposit [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}  (?<amount>[\\.'\\d]+) .*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addManagementFeeBlock(DocumentType type)
    {
        var pdfTransaction = new Transaction<AccountTransaction>();

        var block = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Management fee .*$");
        block.setMaxSize(2);
        type.addBlock(block);
        block.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 03.12.2025 Management fee 0.45% 31.12.2025  0.47 -0.47
                        // @formatter:on
                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) Management fee [\\d]+\\.[\\d]+% [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}  (?<amount>[\\.'\\d]+) .*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Assessment from 24.11.25 - 31.12.25
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Assessment from .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);
    }

    /**
     * Converts a Swiss valor number to an ISIN using the Luhn mod N algorithm.
     *
     * @param valor
     *            the valor number with commas (e.g., "039,462,806")
     * @return the complete 12-character ISIN (e.g., "CH0394628066")
     */
    static String valorToIsin(String valor)
    {
        var clean = valor.replace(",", "");

        while (clean.length() < 9)
            clean = "0" + clean;

        var body = "CH" + clean;

        // Expand letters to digit sequences (A=10, B=11, ..., Z=35)
        var expanded = new StringBuilder();
        for (var c : body.toCharArray())
        {
            if (Character.isLetter(c))
                expanded.append(Character.getNumericValue(c));
            else
                expanded.append(c);
        }

        // Compute check digit using Luhn algorithm on expanded digit string
        var digits = expanded.toString();
        var sum = 0;
        var doubleDigit = true;

        for (var i = digits.length() - 1; i >= 0; i--)
        {
            var d = digits.charAt(i) - '0';

            if (doubleDigit)
            {
                d *= 2;
                if (d > 9)
                    d -= 9;
            }

            sum += d;
            doubleDigit = !doubleDigit;
        }

        var checkDigit = (10 - (sum % 10)) % 10;

        return body + checkDigit;
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "de", "CH");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "de", "CH");
    }
}
