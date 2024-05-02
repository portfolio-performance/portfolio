package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/***
 * @formatter:off
 * @implNote The currency of Sunrise Securities GmbH is always EUR.
 *
 * @implSpec There is no information on the shares in the dividend transactions.
 *           In this section we calculate the shares for the respective transaction.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class SunrisePDFExtractor extends AbstractPDFExtractor
{
    public SunrisePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("info@meetsunrise.com");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Sunrise Securities GmbH";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Fondsabrechnung");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Kauf .* [\\.'\\d]+ \\p{Sc} [\\.'\\d]+ \\p{Sc} [\\.'\\d]+$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Kauf Standortfonds Österreich 10.00 € 140.59 € 0.071
                        // AT0000A1QA38 10.01.2022 5.123
                        // @formatter:on
                        .section("name", "currency", "isin") //
                        .match("^Kauf (?<name>.*) [\\.'\\d]+ \\p{Sc} [\\.'\\d]+ (?<currency>\\p{Sc}) [\\.'\\d]+$") //
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ['\\.\\d]+$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Kauf Standortfonds Österreich 10.00 € 140.59 € 0.071
                        // @formatter:on
                        .section("shares") //
                        .match("^Kauf .* [\\.'\\d]+ \\p{Sc} [\\.'\\d]+ \\p{Sc} (?<shares>[\\.\\d\\s]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // AT0000A1QA38 10.01.2022 5.123
                        // @formatter:on
                        .section("date") //
                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.'\\d]+$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Abrechnungsbetrag: 10.00 €
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Abrechnungsbetrag: (?<amount>['\\.\\d]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Auftrags-Nummer: 20220106123456789000000612345
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftrags\\-Nummer: [\\d]+)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Aussch.ttungsanzeige");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*Aussch.ttungsanzeige$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Fondsname: Standortfonds Österreich
                        // WKN/ISIN: AT0000A1QA38 Datum des Ertrags: 15.12.2023
                        // @formatter:on
                        .section("name", "isin") //
                        .match("^Fondsname: (?<name>.*)$") //
                        .match("^WKN\\/ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Datum des.*$") //
                        .assign((t, v) -> {
                            v.put("currency", CurrencyUnit.EUR);

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Ausschüttung je Anteil: 2.71
                        // Ausschüttung gesamt: 17.58
                        // @formatter:on
                        .section("amountPerShare", "gross") //
                        .match("^Aussch.ttung je Anteil: (?<amountPerShare>['\\.\\d]+)$") //
                        .match("^Aussch.ttung gesamt: (?<gross>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            BigDecimal amountPerShare = BigDecimal.valueOf(asAmount(v.get("amountPerShare")));
                            BigDecimal gross = BigDecimal.valueOf(asAmount(v.get("gross")));

                            BigDecimal shares = gross.divide(amountPerShare, Values.Share.precision(), RoundingMode.HALF_UP);
                            t.setShares(shares.movePointRight(Values.Share.precision()).longValue());
                        })

                        // @formatter:off
                        // WKN/ISIN: AT0000A1QA38 Datum des Ertrags: 15.12.2023
                        // @formatter:on
                        .section("date") //
                        .match("^WKN.*Datum des Ertrags: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausschüttung gesamt: 10.29
                        //  Zur Auszahlung kommender Betrag: 0.00
                        // @formatter:on
                        .section("gross", "amount") //
                        .match("^Aussch.ttung gesamt: (?<gross>[\\.'\\d]+)$") //
                        .match("^.*Zur Auszahlung kommender Betrag: (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(CurrencyUnit.EUR);

                            // @formatter:off
                            // If the dividend amount correspond to the tax amount to be paid,
                            // the taxes is recorded in a separate transaction.
                            // @formatter:on
                            if (t.getMonetaryAmount().isZero())
                            {
                                type.getCurrentContext().putBoolean("noTax", true);
                                t.setAmount(asAmount(v.get("gross")));
                            }
                        })

                        // @formatter:off
                        // WKN / ISIN: AT0000A1Z882 Turnus: jährlich
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.* (?<note>Turnus: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap((t, ctx) -> {
                            type.getCurrentContext().remove("noTax");

                            TransactionItem item = new TransactionItem(t);

                            return item;
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addDividendeTaxBlock(type);
    }

    private void addDividendeTaxBlock(DocumentType type)
    {
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("Aussch.ttungsanzeige");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Fondsname: Standortfonds Österreich
                        // WKN/ISIN: AT0000A1QA38 Datum des Ertrags: 15.12.2023
                        // @formatter:on
                        .section("name", "isin") //
                        .match("^Fondsname: (?<name>.*)$") //
                        .match("^WKN\\/ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Datum des.*$") //
                        .assign((t, v) -> {
                            v.put("currency", CurrencyUnit.EUR);

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // There is no information about the proportions in the
                        // documents. In this section we calculate the shares
                        // for the respective transaction.
                        // @formatter:on

                        // @formatter:off
                        // Ausschüttung je Anteil: 2.71
                        // Ausschüttung gesamt: 17.58
                        // @formatter:on
                        .section("amountPerShare", "gross") //
                        .match("^Aussch.ttung je Anteil: (?<amountPerShare>['\\.\\d]+)$") //
                        .match("^Aussch.ttung gesamt: (?<gross>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            BigDecimal amountPerShare = BigDecimal.valueOf(asAmount(v.get("amountPerShare")));
                            BigDecimal gross = BigDecimal.valueOf(asAmount(v.get("gross")));

                            BigDecimal shares = gross.divide(amountPerShare, Values.Share.precision(), RoundingMode.HALF_UP);
                            t.setShares(shares.movePointRight(Values.Share.precision()).longValue());
                        })

                        // @formatter:off
                        // WKN/ISIN: AT0000A1QA38 Datum des Ertrags: 15.12.2023
                        // @formatter:on
                        .section("date") //
                        .match("^WKN.*Datum des Ertrags: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausschüttung gesamt: 10.29
                        // Kapitalertragsteuer (KESt) gesamt: 10.29
                        // @formatter:on
                        .section("tax", "amount") //
                        .match("^Kapitalertragsteuer \\(KESt\\) gesamt: (?<tax>[\\.'\\d]+)$") //
                        .match("^.*Zur Auszahlung kommender Betrag: (?<amount>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            Money tax = Money.of(CurrencyUnit.EUR, asAmount(v.get("tax")));
                            Money amount = Money.of(CurrencyUnit.EUR, asAmount(v.get("amount")));

                            if (amount.isZero())
                            {
                                t.setMonetaryAmount(tax);
                            }
                            else
                            {
                                t.setAmount(0L);
                                t.setCurrencyCode(CurrencyUnit.EUR);
                            }
                        })

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                return null;

                            return item;
                        });
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // abgeführte Kapitalertragsteuer: 31.61 €
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^abgef.hrte Kapitalertragsteuer: (?<tax>[\\.'\\d]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kapitalertragsteuer (KESt) gesamt: 4.28
                        // @formatter:on
                        .section("tax").optional() //
                        .match("^Kapitalertragsteuer \\(KESt\\) gesamt: (?<tax>[\\.'\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                            {
                                v.put("currency", CurrencyUnit.EUR);
                                processTaxEntries(t, v, type);
                            }
                        });
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
