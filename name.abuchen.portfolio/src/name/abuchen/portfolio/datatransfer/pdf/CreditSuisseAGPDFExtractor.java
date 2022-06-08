package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class CreditSuisseAGPDFExtractor extends AbstractPDFExtractor
{

    public CreditSuisseAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("CREDIT SUISSE"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Credit Suisse AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Ihr Kauf|Ihr Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Ihr (Kauf|Verkauf) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Ihr (?<type>(Kauf|Verkauf)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // 900 Registered Shs Iron Mountain Inc USD 0.01
                // Valor 26754105, IRM, ISIN US46284V1017
                // Kurswert USD 27,270.00
                .section("shares", "name", "isin", "currency").optional()
                .match("^(?<shares>[\\.,\\d]+) (?<name>.*) [\\w]{3} [\\.,\\d]+$")
                .match("^.* ISIN (?<isin>[\\w]{12})$")
                .match("^Kurswert (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // USD 200,000 6.25 % Fixed Rate Notes Norddeutsche
                // Landesbank Girozentrale 2014-10.4.24 Reg-S
                // Subord.
                // Valor 24160639, NDKH, ISIN XS1055787680
                // Kurswert USD 183,000.00
                .section("shares", "name", "isin", "currency").optional()
                .match("^[\\w]{3} (?<shares>[\\.,\\d]+) (?<name>.*)$")
                .match("^.* ISIN (?<isin>[\\w]{12})$")
                .match("^Kurswert (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    // Percentage quotation, workaround for bonds
                    t.setShares((asShares(v.get("shares")) / 100));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Ausführungszeit
                // 103 14800075391312 18:32:46 XNYS USD 30.30
                .section("time").optional()
                .find("Ausf.hrungszeit")
                .match("^.* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) [\\w]{3,4} .*$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                // Datum 08.06.2020
                .section("date")
                .match("^Datum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> {
                    if (type.getCurrentContext().get("time") != null)
                        t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Belastung USD 27,734.70
                // Gutschrift EUR 28,744.30
                .section("currency", "amount")
                .match("^(Belastung|Gutschrift) (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // If we have the "Internet-Vergünstigung",
                // we add up it from the amount and reset it.
                // The "Internet discount" is then posted as a fee refund.

                // Internet-Vergünstigung USD - 41.81
                // Internet-Vergünstigung USD 44.69
                .section("feeRefund", "currency").optional()
                .match("^Internet\\-Verg.nstigung (?<currency>[\\w]{3}) (\\- )?(?<feeRefund>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(t.getPortfolioTransaction().getAmount() + asAmount(v.get("feeRefund")));
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addFeeReturnBlock(type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Ertragsabrechnung");
        this.addDocumentTyp(type);

        Block block = new Block("^Ertragsabrechnung .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // 900 REGISTERED SHS IRON MOUNTAIN INC
                // Valor 26754105, IRM, ISIN US46284V1017
                // Bruttoertrag USD 556.65
                .section("shares", "name", "isin", "currency").optional()
                .match("^(?<shares>[\\.,\\d]+) (?<name>.*)$")
                .match("^.* ISIN (?<isin>[\\w]{12})$")
                .match("^Bruttoertrag (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // USD 200,000 6.25 % FIXED RATE NOTES NORDDEUTSCHE
                // LANDESBANK GIROZENTRALE 2014-10.4.24 REG-S
                // SUBORD.
                // Valor 24160639, NDKH, ISIN XS1055787680
                // Bruttoertrag USD 6,250.00
                .section("shares", "name", "isin", "currency").optional()
                .match("^[\\w]{3} (?<shares>[\\.,\\d]+) (?<name>.*)$")
                .match("^.* ISIN (?<isin>[\\w]{12})$")
                .match("^Bruttoertrag (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    // Percentage quotation, workaround for bonds
                    t.setShares((asShares(v.get("shares")) / 100));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Valuta 12.04.2021
                .section("date")
                .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Bruttoertrag USD 6,250.00
                .section("currency", "amount")
                .match("^Gutschrift (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Semesterzinsen
                // Quartalsdividende
                .section("note").optional()
                .match("^(?<note>(Semesterzinsen|Quartalsdividende))$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addFeeReturnBlock(DocumentType type)
    {
        Block block = new Block("^Ihr (Kauf|Verkauf) .*$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.FEES_REFUND);
                    return t;
                })

                // 900 Registered Shs Iron Mountain Inc USD 0.01
                // Valor 26754105, IRM, ISIN US46284V1017
                // Kurswert USD 27,270.00
                .section("shares", "name", "currency", "isin").optional()
                .match("^(?<shares>[\\.,\\d]+) (?<name>.*) [\\w]{3} [\\.,\\d]+$")
                .match("^.* ISIN (?<isin>[\\w]{12})$")
                .match("^Kurswert (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // USD 200,000 6.25 % Fixed Rate Notes Norddeutsche
                // Landesbank Girozentrale 2014-10.4.24 Reg-S
                // Subord.
                // Valor 24160639, NDKH, ISIN XS1055787680
                // Kurswert USD 183,000.00
                .section("shares", "name", "isin", "currency").optional()
                .match("^[\\w]{3} (?<shares>[\\.,\\d]+) (?<name>.*)$")
                .match("^.* ISIN (?<isin>[\\w]{12})$")
                .match("^Kurswert (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    // Percentage quotation, workaround for bonds
                    t.setShares((asShares(v.get("shares")) / 100));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Datum 08.06.2020
                .section("date")
                .match("^Datum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Internet-Vergünstigung USD - 41.81
                // Internet-Vergünstigung USD 44.69
                .section("currency", "amount").optional()
                .match("^Internet-Verg.nstigung (?<currency>[\\w]{3}) (-\\ )?(?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Eidgenössische Umsatzabgabe USD 40.91
                .section("currency", "tax").optional()
                .match("^Eidgen.ssische Umsatzabgabe (?<currency>[\\w]{3}) (\\- )?(?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Quellensteuer USD - 167.00
                .section("currency", "withHoldingTax").optional()
                .match("^Quellensteuer (?<currency>[\\w]{3}) (\\- )?(?<withHoldingTax>[\\.,\\d]+)$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Kommission Schweiz/Ausland USD 463.60
                .section("currency", "fee").optional()
                .match("^Kommission Schweiz\\/Ausland (?<currency>[\\w]{3}) (\\- )?(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Kommission Schweiz USD 1,413.65
                .section("currency", "fee").optional()
                .match("^Kommission Schweiz (?<currency>[\\w]{3}) (\\- )?(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Kosten und Abgaben Ausland USD 2.00
                .section("currency", "fee").optional()
                .match("^Kosten und Abgaben (?<currency>[\\w]{3}) (\\- )?(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return PDFExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "US");
    }

    @Override
    protected long asShares(String value)
    {
        return PDFExtractorUtils.convertToNumberLong(value, Values.Share, "en", "US");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return PDFExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "en", "US");
    }
}
