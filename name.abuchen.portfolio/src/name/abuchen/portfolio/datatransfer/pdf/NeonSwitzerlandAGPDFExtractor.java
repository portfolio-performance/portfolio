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

/**
 * @formatter:off
 * Extractor for 3a account documents from Neon Switzerland AG
 * 
 * Neon Switzerland AG partners with Hypothekarbank Lenzburg AG for their regular investment offering. 
 * However, their 3a accounts are a separate product held by Simply3a and managed by
 * Lienhardt & Partner Privatbank Zürich AG.
 * 
 * The PDF statements for 3a accounts from Neon thus cannot be extracted by
 * {@link HypothekarbankLenzburgAGPDFExtractor}.
 * 
 * @see <a href="https://www.neon-free.ch/en/saeule3a">Neon 3a Account Information</a>
 * @formatter:on
 */
@SuppressWarnings("nls")
public class NeonSwitzerlandAGPDFExtractor extends AbstractPDFExtractor
{
    public NeonSwitzerlandAGPDFExtractor(Client client)
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
                        .section("name", "wkn", "currency") //
                        .match("^(?!Unit |Rate )(?<name>[A-Za-z].*)$") //
                        .match("^Secur\\.Nr\\. 1\\/0*(?<wkn>[\\d,]+) Secur\\. Cur (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            v.put("wkn", v.get("wkn").replace(",", ""));
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
