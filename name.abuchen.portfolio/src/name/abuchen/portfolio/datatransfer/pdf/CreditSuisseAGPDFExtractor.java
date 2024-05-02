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

/**
 * @formatter:off
 * @implNote Credit Suisse AG
 *
 * @implSpec The VALOR number is the WKN number with 5 to 9 letters.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class CreditSuisseAGPDFExtractor extends AbstractPDFExtractor
{

    public CreditSuisseAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("CREDIT SUISSE");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Credit Suisse AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Ihr Kauf|Ihr Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Ihr (Kauf|Verkauf) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Ihr (?<type>(Kauf|Verkauf)) .*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 900 Registered Shs Iron Mountain Inc USD 0.01
                                        // Valor 26754105, IRM, ISIN US46284V1017
                                        // zum Kurs von USD 30.30
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .match("^[\\.,\\d]+ (?<name>.*) [\\w]{3} [\\.,\\d]+$") //
                                                        .match("^Valor (?<wkn>[A-Z0-9]{5,9}),.* ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^zum Kurs von (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // USD 200,000 6.25 % Fixed Rate Notes Norddeutsche
                                        // Landesbank Girozentrale 2014-10.4.24 Reg-S
                                        // Subord.
                                        // Valor 24160639, NDKH, ISIN XS1055787680
                                        // Kurswert USD 183,000.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .match("^[\\w]{3} [\\.,\\d]+ (?<name>.*)$") //
                                                        .match("^Valor (?<wkn>[A-Z0-9]{5,9}),.* ISIN (?<isin>[\\w]{12})$") //
                                                        .match("^Kurswert (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // USD 200,000 6.25 % Fixed Rate Notes Norddeutsche
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\w]{3} (?<shares>[\\.,\\d]+) [\\.,\\d]+ %.*$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            BigDecimal shares = asExchangeRate(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }),
                                        // @formatter:off
                                        // 900 Registered Shs Iron Mountain Inc USD 0.01
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(?<shares>[\\.,\\d]+) (?<name>.*) [\\w]{3} [\\.,\\d]+$")
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        // @formatter:off
                        // Ausführungszeit
                        // 103 14800075391312 18:32:46 XNYS USD 30.30
                        // @formatter:on
                        .section("time").optional() //
                        .find("Ausf.hrungszeit") //
                        .match("^.* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) [\\w]{3,4} .*$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        // @formatter:off
                        // Datum 08.06.2020
                        // @formatter:on
                        .section("date") //
                        .match("^Datum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().get("time") != null)
                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                            else
                                t.setDate(asDate(v.get("date")));
                        })

                        // @formatter:off
                        // Belastung USD 27,734.70
                        // Gutschrift EUR 28,744.30
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^(Belastung|Gutschrift) (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // If we have the "Internet-Vergünstigung",
                        // we add up it from the amount and reset it.
                        // The "Internet discount" is then posted as a fee refund.
                        // @formatter:on

                        // @formatter:off
                        // Internet-Vergünstigung USD - 41.81
                        // Internet-Vergünstigung USD 44.69
                        // @formatter:on
                        .section("feeRefund", "currency").optional() //
                        .match("^Internet\\-Verg.nstigung (?<currency>[\\w]{3}) (\\- )?(?<feeRefund>[\\.,\\d]+)$") //
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

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        
        Block firstRelevantLine = new Block("^Ertragsabrechnung .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 900 REGISTERED SHS IRON MOUNTAIN INC
                                        // Valor 26754105, IRM, ISIN US46284V1017
                                        // Dividende USD 0.6185
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .match("^[\\.,\\d]+ (?<name>.*)$") //
                                                        .match("^Valor (?<wkn>[A-Z0-9]{5,9}),.* ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Dividende (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // USD 200,000 6.25 % FIXED RATE NOTES NORDDEUTSCHE
                                        // LANDESBANK GIROZENTRALE 2014-10.4.24 REG-S
                                        // SUBORD.
                                        // Valor 24160639, NDKH, ISIN XS1055787680
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "name", "wkn", "isin") //
                                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+ (?<name>.*)$") //
                                                        .match("^Valor (?<wkn>[A-Z0-9]{5,9}),.* ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // USD 200,000 6.25 % Fixed Rate Notes Norddeutsche
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\w]{3} (?<shares>[\\.,\\d]+) [\\.,\\d]+ %.*$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            BigDecimal shares = asExchangeRate(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }),
                                        // @formatter:off
                                        // 900 Registered Shs Iron Mountain Inc USD 0.01
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(?<shares>[\\.,\\d]+) .*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        // @formatter:off
                        // Valuta 12.04.2021
                        // @formatter:on
                        .section("date") //
                        .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Bruttoertrag USD 6,250.00
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Gutschrift (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Semesterzinsen
                        // Quartalsdividende
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>(Semesterzinsen|Quartalsdividende))$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addFeeReturnBlock(DocumentType type)
    {
        Block block = new Block("^Ihr (Kauf|Verkauf) .*$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES_REFUND);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 900 Registered Shs Iron Mountain Inc USD 0.01
                                        // Valor 26754105, IRM, ISIN US46284V1017
                                        // zum Kurs von USD 30.30
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .match("^[\\.,\\d]+ (?<name>.*) [\\w]{3} [\\.,\\d]+$") //
                                                        .match("^Valor (?<wkn>[A-Z0-9]{5,9}),.* ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^zum Kurs von (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // USD 200,000 6.25 % Fixed Rate Notes Norddeutsche
                                        // Landesbank Girozentrale 2014-10.4.24 Reg-S
                                        // Subord.
                                        // Valor 24160639, NDKH, ISIN XS1055787680
                                        // Kurswert USD 183,000.00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .match("^[\\w]{3} [\\.,\\d]+ (?<name>.*)$") //
                                                        .match("^Valor (?<wkn>[A-Z0-9]{5,9}),.* ISIN (?<isin>[\\w]{12})$") //
                                                        .match("^Kurswert (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // USD 200,000 6.25 % Fixed Rate Notes Norddeutsche
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^[\\w]{3} (?<shares>[\\.,\\d]+) [\\.,\\d]+ % (?<name>.*)$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            BigDecimal shares = asExchangeRate(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }),
                                        // @formatter:off
                                        // 900 Registered Shs Iron Mountain Inc USD 0.01
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(?<shares>[\\.,\\d]+) (?<name>.*) [\\w]{3} [\\.,\\d]+$")
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        // @formatter:off
                        // Datum 08.06.2020
                        // @formatter:on
                        .section("date") //
                        .match("^Datum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Internet-Vergünstigung USD - 41.81
                        // Internet-Vergünstigung USD 44.69
                        // @formatter:on
                        .section("currency", "amount").optional() //
                        .match("^Internet-Verg.nstigung (?<currency>[\\w]{3}) (-\\ )?(?<amount>[\\.,\\d]+)$") //
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
        transaction //
        
                        // @formatter:off
                        // Eidgenössische Umsatzabgabe USD 40.91
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Eidgen.ssische Umsatzabgabe (?<currency>[\\w]{3}) (\\- )?(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Quellensteuer USD - 167.00
                        // @formatter:on
                        .section("currency", "withHoldingTax").optional() //
                        .match("^Quellensteuer (?<currency>[\\w]{3}) (\\- )?(?<withHoldingTax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //
        
                        // @formatter:off
                        // Kommission Schweiz/Ausland USD 463.60
                        // Kommission Schweiz USD 1,413.65
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Kommission .* (?<currency>[\\w]{3}) (\\- )?(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Kosten und Abgaben Ausland USD 2.00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Kosten und Abgaben (?<currency>[\\w]{3}) (\\- )?(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "US");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "en", "US");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "en", "US");
    }
}
