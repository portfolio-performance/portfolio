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
 * @implNote Extractor for 3a account documents from Neon Switzerland AG
 *
 *           Neon Switzerland AG partners with Hypothekarbank Lenzburg AG for their regular investment offering.
 *           However, their 3a accounts are a separate product held by Simply3a and managed by
 *           Lienhardt & Partner Privatbank Zürich AG.
 *
 * @see href="https://www.neon-free.ch/en/saeule3a">Neon 3a Account Information</a>
 *
 * @implSpec The VALOR number is the WKN number.
 * @formatter:on
 */
@SuppressWarnings("nls")
public class NeonSwitzerlandAGPDFExtractor extends AbstractPDFExtractor
{
    public NeonSwitzerlandAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("neon Switzerland AG");

        addBuySellTransaction();
        addDepositTransaction();
        addFeeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "neon Switzerland AG";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Investment account simply3a: Daily statement", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Currency CHF
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Currency (?<currency>[A-Z]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Fund (buy|sell) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Sell" change from BUY to SELL
                        .section("type").optional() //
                        .match("^.* Fund (?<type>(buy|sell)) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*$") //
                        .assign((t, v) -> {
                            if ("sell".equals(v.get("type"))) //
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // 25.11.2025 Fund buy 25.11.2025  80.00  310.00
                        // Sc F. V Eq Emer. M.
                        // Secur.Nr. 1/011,704,497 Secur. Cur CHF
                        // @formatter:on
                        .section("name", "wkn", "currency") //
                        .find("[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Fund (buy|sell).*$") //
                        .match("^(?<name>.*)$") //
                        .match("^Secur\\.Nr\\. 1\\/0(?<wkn>[\\d]{2},[\\d]{3},[\\d]{3}) Secur\\. Cur (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            v.put("wkn", v.get("wkn").replace(",", ""));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // 24.11.2025 Fund buy 24.11.2025  10.00  990.00
                        // 04.12.2025 Fund sell 04.12.2025  0.15 -0.32
                        // @formatter:on
                        .section("date") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) Fund (buy|sell).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Unit 0.102347
                        // @formatter:on
                        .section("shares") //
                        .match("^Unit (?<shares>[\\.'\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 24.11.2025 Fund buy 24.11.2025  10.00  990.00
                        // 04.12.2025 Fund sell 04.12.2025  0.15 -0.32
                        // @formatter:on
                        .section("amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Fund (buy|sell) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}[\\s]{1,}(?<amount>[\\.'\\d]+).*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(BuySellEntryItem::new);
    }

    private void addDepositTransaction()
    {
        final var type = new DocumentType("Investment account simply3a: Daily statement", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Currency CHF
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Currency (?<currency>[A-Z]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Deposit .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

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
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) Deposit [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}[\\s]{1,}(?<amount>[\\.'\\d]+).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addFeeTransaction()
    {
        final var type = new DocumentType("Investment account simply3a: Daily statement", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Currency CHF
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Currency (?<currency>[A-Z]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Management fee .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

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
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) Management fee [\\d]+\\.[\\d]+% [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}[\\s]{1,}(?<amount>[\\.'\\d]+).*$") //
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
