package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

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
public class YuhPDFExtractor extends AbstractPDFExtractor
{
    public YuhPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Yuh"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendTransaction();
        addPaymentTransaction();
        addInterestTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Yuh powerd by Swissquote / Postfinance"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Kauf");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Kauf .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // @formatter:off
                // RICHEMONT N ISIN: CH0210483332 SIX Swiss Exchange 
                // 1.0269 97.38 CHF 100.00
                // @formatter:on
                .section("name", "isin", "currency")
                .match("^(?<name>.*) ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                .match("^[\\.'\\d]+ [\\.'\\d]+ (?<currency>[\\w]{3}) [\\.'\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // 1.0269 97.38 CHF 100.00
                // @formatter:on
                .section("shares")
                .match("^(?<shares>[\\.'\\d]+) [\\.'\\d]+ [\\w]{3} [\\.'\\d]+$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Gemäss Ihrem Kaufauftrag vom 31.10.2022 haben wir folgende Transaktionen vorgenommen:
                // @formatter:on
                .section("date")
                .match("^Gem.ss Ihrem Kaufauftrag vom (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // @formatter:off
                // Zu Ihren Lasten CHF 100.10
                // @formatter:on
                .section("currency", "amount")
                .match("^Zu Ihren Lasten (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // @formatter:off
                // Kauf * Unsere Referenz: 312345678
                // @formatter:on
                .section("note").optional()
                .match("^.* (?<note>Referenz: .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Dividende");
        this.addDocumentTyp(type);

        Block block = new Block("Dividende .* [\\d+].*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // @formatter:off
                // ROCHE GS ISIN: CH0012032048NKN: 1203204 4.0542
                // Anzahl 4.0542
                // Dividende 9.5 CHF
                // @formatter:on
                .section("name", "isin", "currency")
                .match("^(?<name>.*) ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                .match("^Dividende [\\.'\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Anzahl 4.0542
                // @formatter:on
                .section("shares")
                .match("^Anzahl (?<shares>[\\.,'\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Valutadatum 20.03.2023
                // @formatter:on
                .section("date")
                .match("^Valutadatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                // Total CHF 25.03
                // @formatter:on
                .section("currency", "amount")
                .match("^Total (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // @formatter:off
                // Dividende Unsere Referenz: 312345678
                // @formatter:on
                .section("note").optional()
                .match("^.* (?<note>Referenz: .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addPaymentTransaction()
    {
        DocumentType type = new DocumentType("Zahlungsverkehr");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DEPOSIT);
            return entry;
        });

        Block firstRelevantLine = new Block("^Zahlungsverkehr \\- (Gutschrift|Belastung) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                .section("type").optional()
                .match("^Zahlungsverkehr \\- (?<type>(Gutschrift|Belastung)) .*$")
                .assign((t, v) -> {
                    if ("Belastung".equals(v.get("type")))
                        t.setType(AccountTransaction.Type.REMOVAL);
                })

                // @formatter:off
                // Valutadatum 27.10.2022
                // @formatter:on
                .section("date")
                .match("^Valutadatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                // Total EUR 1'000.00
                // @formatter:on
                .section("currency", "amount")
                .match("^Total (?<currency>[\\w]{3}) (?<amount>[\\.'\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // @formatter:off
                // Zahlungsverkehr - Gutschrift Unsere Referenz: 312345678
                // @formatter:on
                .section("note").optional()
                .match("^.* (?<note>Referenz: .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(TransactionItem::new);
    }

    private void addInterestTransaction()
    {
        DocumentType type = new DocumentType("Zinsabrechnung");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.INTEREST);
            return entry;
        });

        Block firstRelevantLine = new Block("^IBAN : .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // @formatter:off
                // 30.12.2022 1.36 0.00 1.36 0.00 1.36
                // @formatter:on
                .section("date")
                .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                // IBAN : CH3308781000123456700 - Währung : CHF
                // Total 1.36 0.00 1.36 0.00 1.36
                // @formatter:on
                .section("currency", "amount")
                .match("^IBAN : .* W.hrung : (?<currency>[\\w]{3})$")
                .match("^Total .* (?<amount>[\\.'\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // @formatter:off
                // Zinsabrechnung vom 05.09.2022 bis zum 31.12.2022
                // @formatter:on
                .section("note1", "note2", "note3").optional()
                .match("^(?<note1>Zinsabrechnung) vom (?<note2>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* (?<note3>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> {
                    t.setNote(v.get("note1") + " " + v.get("note2") + " - " + v.get("note3"));
                })

                .wrap(TransactionItem::new);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Abgabe (Eidg. Stempelsteuer) CHF 0.10
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^Abgabe \\(Eidg\\. Stempelsteuer\\) (?<currency>[\\w]{3}) (?<tax>[\\.'\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Verrechnungssteuer 35% (CH) CHF 13.48
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^Verrechnungssteuer .* (?<currency>[\\w]{3}) (?<tax>[\\.'\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Kommission CHF 1.00
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^Kommission (?<currency>[\\w]{3}) (?<fee>[\\.'\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type));
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

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH");
    }
}
