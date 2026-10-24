package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetTax;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class VolksbankWienPDFExtractor extends AbstractPDFExtractor
{
    private static final String IS_NEGATIVE_AMOUNT = "isNegativeAmount";

    public VolksbankWienPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("VOLKSBANK WIEN AG");

        addBuySellTransaction();
        addDividendTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Volksbank Wien AG";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Gesch.ftsart: Kauf aus Dauerauftrag");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Wir haben f.r Sie am [\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4} unten angef.hrtes Gesch.ft abgerechnet:$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // @formatter:off
                        // Titel: DE0008491051  U n i G l o b a l
                        // Inh.-Ant. Ant.sch.kl.
                        // Kup. 38
                        // Fondsgesellschaft: Union Investment Privatfonds GmbH
                        // Kurs: 305,83 EUR
                        // @formatter:on
                        .section("isin", "name", "name1", "currency") //
                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                        .match("^(?<name1>.*)$") //
                        .match("^Kurs: [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            v.put("name", concatenate(collapseLetterSpacing(v.get("name")), trim(v.get("name1")), " "));
                            v.put("currency", asCurrencyCode(v.get("currency")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Zugang: 0,382 Stk
                        // @formatter:on
                        .section("shares") //
                        .match("^Zugang: (?<shares>[\\.,\\d]+) Stk$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Schlusstag: 12.10.2022
                        // @formatter:on
                        .section("date") //
                        .match("^Schlusstag: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Zu Lasten IBAN WN18 6137 8071 9059 7497 -119,75 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Zu Lasten .* \\-(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Auftrags-Nr.: 39534509-27.7.2021
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftrags\\-Nr\\.: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendTransaction()
    {
        final var type = new DocumentType("Gesch.ftsart: Ertrag");
        this.addDocumentTyp(type);

        // @formatter:off
        // If the taxes exceed the gross amount, the account is debited.
        // A dividend cannot be negative. In this case, the gross amount is
        // booked as dividend and the taxes as separate tax transaction.
        // @formatter:on

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Wir haben f.r Sie am [\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4} unten angef.hrtes Gesch.ft abgerechnet:$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        // @formatter:off
                        // Titel: DE0008491051  U n i G l o b a l
                        // Inh.-Ant. Ant.sch.kl.
                        // Kup. 38
                        // Fondsgesellschaft: Union Investment Privatfonds GmbH
                        // Ertrag: 2,8 EUR
                        // @formatter:on
                        .section("isin", "name", "name1", "currency") //
                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                        .match("^(?<name1>.*)$") //
                        .match("^Ertrag: [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            v.put("name", concatenate(collapseLetterSpacing(v.get("name")), trim(v.get("name1")), " "));
                            v.put("currency", asCurrencyCode(v.get("currency")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Geschäftsart: Ertrag
                        // 9,262 Stk
                        // @formatter:on
                        .section("shares") //
                        .find("Gesch.ftsart: Ertrag") //
                        .match("^(?<shares>[\\.,\\d]+) Stk$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Valuta 14.11.2022
                        // @formatter:on
                        .section("date") //
                        .match("^Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Extag: 10.11.2022
                        // @formatter:on
                        .section("exDate").optional() //
                        .match("^Extag: (?<exDate>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setExDate(asDate(v.get("exDate"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Zu Gunsten IBAN AT00 0000 0000 0000 0000 16,40 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Zu Gunsten .* (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Bruttoertrag: 25,93 EUR
                                        // Zu Lasten VYYK Kc38 5373 2681 3155 9279 -9,53 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Bruttoertrag: (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .match("^Zu Lasten .* \\-[\\.,\\d]+ [A-Z]{3}$") //
                                                        .assign((t, v) -> {
                                                            v.getTransactionContext().putBoolean(IS_NEGATIVE_AMOUNT, true);

                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);

        var taxesTransaction = new Transaction<AccountTransaction>();

        var taxesBlock = new Block("^Wir haben f.r Sie am [\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4} unten angef.hrtes Gesch.ft abgerechnet:$");
        type.addBlock(taxesBlock);
        taxesBlock.set(taxesTransaction);

        taxesTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAXES))

                        // @formatter:off
                        // Titel: DE0008491051  U n i G l o b a l
                        // Inh.-Ant. Ant.sch.kl.
                        // Kup. 38
                        // Fondsgesellschaft: Union Investment Privatfonds GmbH
                        // Ertrag: 2,8 EUR
                        // @formatter:on
                        .section("isin", "name", "name1", "currency") //
                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                        .match("^(?<name1>.*)$") //
                        .match("^Ertrag: [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            v.put("name", concatenate(collapseLetterSpacing(v.get("name")), trim(v.get("name1")), " "));
                            v.put("currency", asCurrencyCode(v.get("currency")));

                            t.setSecurity(getOrCreateSecurity(v));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        // @formatter:off
                        // Geschäftsart: Ertrag
                        // 9,262 Stk
                        // @formatter:on
                        .section("shares") //
                        .find("Gesch.ftsart: Ertrag") //
                        .match("^(?<shares>[\\.,\\d]+) Stk$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Valuta 14.11.2022
                        // @formatter:on
                        .section("date") //
                        .match("^Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Kapitalertragssteuer: -35,20 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer: \\-(?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> t.setAmount(t.getAmount() + asAmount(v.get("tax"))))

                        // @formatter:off
                        // KESt Ausländische Dividende: -0,26 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^KESt Ausl.ndische Dividende: \\-(?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> t.setAmount(t.getAmount() + asAmount(v.get("tax"))))

                        // @formatter:off
                        // Zu Lasten VYYK Kc38 5373 2681 3155 9279 -9,53 EUR
                        // @formatter:on
                        .section("negative").optional() //
                        .match("^(?<negative>Zu Lasten) .* \\-[\\.,\\d]+ [A-Z]{3}$") //
                        .assign((t, v) -> v.getTransactionContext().putBoolean(IS_NEGATIVE_AMOUNT, true))

                        // Only required if the taxes exceed the gross amount
                        .wrap((t, ctx) -> {
                            if (ctx.getBoolean(IS_NEGATIVE_AMOUNT))
                                return new TransactionItem(t);

                            return null;
                        });
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Kapitalertragssteuer: -35,20 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer: \\-(?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            // Taxes are booked separately if they exceed the gross amount
                            if (!v.getTransactionContext().getBoolean(IS_NEGATIVE_AMOUNT))
                            {
                                var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                                checkAndSetTax(tax, t, type.getCurrentContext());
                            }
                        })

                        // @formatter:off
                        // KESt Ausländische Dividende: -0,26 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^KESt Ausl.ndische Dividende: \\-(?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            // Taxes are booked separately if they exceed the gross amount
                            if (!v.getTransactionContext().getBoolean(IS_NEGATIVE_AMOUNT))
                            {
                                var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                                checkAndSetTax(tax, t, type.getCurrentContext());
                            }
                        });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Fondskaufspesen: -2,92 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Fondskaufspesen: \\-(?<fee>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            var fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
                            checkAndSetFee(fee, t, type.getCurrentContext());
                        });
    }

    /**
     * Removes the letter spacing of the security name, e.g.
     * "U n i G l o b a l" becomes "UniGlobal". Only spaces between two
     * single characters are removed.
     */
    private String collapseLetterSpacing(String value)
    {
        return trim(value).replaceAll("(?<=(^|\\s)\\S) (?=\\S(\\s|$))", "");
    }
}
