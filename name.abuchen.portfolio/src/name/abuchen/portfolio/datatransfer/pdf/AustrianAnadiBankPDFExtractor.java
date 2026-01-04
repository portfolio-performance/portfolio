package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class AustrianAnadiBankPDFExtractor extends AbstractPDFExtractor
{
    public AustrianAnadiBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("HAABAT2K");

        addDepositRemoveAccountTransaction();
        addSubAccountInterestTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Austrian Anadi Bank AG";
    }

    private void addDepositRemoveAccountTransaction()
    {
        final var type = new DocumentType("KONTOAUSZUG", //
                        documentContext -> documentContext //
                        // @formatter:off
                        // Neuer Saldo zu Ihren Gunsten
                        // Max Mustermann EUR 4.348,95
                        // @formatter:on
                                        .section("currency") //
                                        .find("^Neuer Saldo zu Ihren Gunsten.*$") //
                                        .match("^.*(?<currency>[\\w]{3}).*$") //
                                        .assign((ctx, v) -> {
                                            ctx.put("currency", asCurrencyCode(v.get("currency")));
                                        })
                                        // @formatter:off
                        // Inglitschstraße 5A, 050202-0 vom 30.05.2025
                        // @formatter:on
                                        .section("year") //
                                        .match("^.*[\\d]{6}-[\\d] vom [\\d]{2}.[\\d]{2}.(?<year>[\\d]{4})$") //
                                        .assign((ctx, v) -> {
                                            ctx.put("year", v.get("year"));
                                        }));

        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^[\\d]{1,2}\\.[\\d]{1,2}.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .oneOf( //
                                // @formatter:off
                                // 17.11 ABRECHNUNG ZU KONTO 00123456678 17.11 2.522,75
                                // VERANLAGUNGSBETRAG             2.500,00
                                // LAUFZEIT        2025-05-15 - 2025-11-17
                                // Habenzinsen
                                // AB 2025-05-15          2,4000%              30,33
                                // KESt                                         7,58-
                                // @formatter:on
                                section -> section //
                                                .documentContext("year", "currency") //
                                                .attributes("date", "amount", "account", "interest", "tax") //
                                                .match("^(?i)(?<date>[\\d]{1,2}\\.[\\d]{1,2}) ABRECHNUNG ZU KONTO (?<account>[\\d]+) [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+$") //
                                                .match("^(?i)VERANLAGUNGSBETRAG[^\\w]*(?<amount>[\\.,\\d]+)$") //
                                                .find("^Habenzinsen$") //
                                                .match("^AB [\\d]{4}-[\\d]{2}-[\\d]{2} .* (?<interest>[\\.,\\d]+)$") //
                                                .match("^(?i)KESt[^\\w]*(?<tax>[\\.,\\d]+)-$")
                                                .assign((t, v) -> { //
                                                        t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                                                        t.setCurrencyCode(v.get("currency"));
                                                        t.setType(AccountTransaction.Type.DEPOSIT);
                                                        t.setAmount(asAmount(v.get("amount")));
                                                        t.setNote("ABRECHNUNG ZU KONTO " + v.get("account"));
                                                }), //
                                //
                                // @formatter:off
                                // 31.12 Abschluss 31.12 123,91
                                // Habenzinsen                                165,21
                                // KESt                                        41,30-
                                // @formatter:on
                                section -> section //
                                                .documentContext("year", "currency") //
                                                .attributes("date", "note", "amount", "tax") //
                                                .match("^(?i)(?<date>[\\d]{1,2}\\.[\\d]{1,2}) Abschluss [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)$") //
                                                .match("^(?i)(?<note>Habenzinsen)[^\\w]*[\\.,\\d]+$") //
                                                .match("^(?i)KESt[^\\w]*(?<tax>[\\.,\\d]+)-$") //
                                                .assign((t, v) -> { //
                                                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setType(AccountTransaction.Type.INTEREST);
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(v.get("note"));
                                                            processTaxEntries(t, v, type);
                                                }), //
                                // @formatter:off
                                // 15.05 ONLINE-FESTGELD / KONTO 12345555555 15.05 2.500,00-
                                // VERANLAGUNGSBETRAG             2.500,00
                                // LAUFZEIT        2025-05-15 - 2025-11-17
                                // Habenzinsen
                                // AB 2025-05-15          2,4000%              30,33
                                // KESt                                         7,58-
                                //@formatter:on
                                section -> section //
                                                .documentContext("year", "currency") //
                                                .attributes("date", "amount", "note") //
                                                .match("^(?i)(?<date>[\\d]{1,2}\\.[\\d]{1,2}) (?<note>ONLINE-FESTGELD / KONTO \\d+).* [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)-$") //
                                                .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setType(AccountTransaction.Type.REMOVAL);
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(v.get("note"));
                                                }),
                                // @formatter:off
                                // 09.12 Max Mustermann 05.12 3.500,00
                                // IBAN: AT12 1234 1234 1234 1234
                                // REF: 1111111111111EB1111111
                                //@formatter:on
                                section -> section //
                                                .documentContext("year", "currency") //
                                                .attributes("date", "amount") //
                                                .match("^(?i)(?<date>[\\d]{1,2}\\.[\\d]{1,2}) .* [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)$") //
                                                // .match("^(?i)(?<note>IBAN
                                                // .*)$") //
                                                .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setType(AccountTransaction.Type.DEPOSIT);
                                                            t.setAmount(asAmount(v.get("amount")));
                                                }),

                                // @formatter:off
                                // 09.12 Max Mustermann 05.12 3.500,00-
                                // IBAN: AT12 1234 1234 1234 1234
                                // REF: 1111111111111EB1111111
                                //@formatter:on
                                section -> section //
                                                .documentContext("year", "currency") //
                                                .attributes("date", "amount") //
                                                .match("^(?i)(?<date>[\\d]{1,2}\\.[\\d]{1,2}) .* [\\d]{1,2}\\.[\\d]{1,2} (?<amount>[\\.,\\d]+)-$") //
                                                .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setType(AccountTransaction.Type.REMOVAL);
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            // t.setNote(v.get("note"));
                                                })
                        ) //

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                                
                            return null;
                        });
    }

    private void addSubAccountInterestTransaction()
    {
        final var type = new DocumentType("KONTOAUSZUG", //
                        documentContext -> documentContext //
                        // @formatter:off
                        // Neuer Saldo zu Ihren Gunsten
                        // Max Mustermann EUR 4.348,95
                        // @formatter:on
                                        .section("currency") //
                                        .find("^Neuer Saldo zu Ihren Gunsten.*$") //
                                        .match("^.*(?<currency>[\\w]{3}).*$") //
                                        .assign((ctx, v) -> {
                                            ctx.put("currency", asCurrencyCode(v.get("currency")));
                                        })
                                        // @formatter:off
                        // Inglitschstraße 5A, 050202-0 vom 30.05.2025
                        // @formatter:on
                                        .section("year") //
                                        .match("^.*[\\d]{6}-[\\d] vom [\\d]{2}.[\\d]{2}.(?<year>[\\d]{4})$") //
                                        .assign((ctx, v) -> {
                                            ctx.put("year", v.get("year"));
                                        }));

        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^[\\d]{1,2}\\.[\\d]{1,2}.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .optionalOneOf( //
                                // @formatter:off
                                // 17.11 ABRECHNUNG ZU KONTO 00123456678 17.11 2.522,75
                                // VERANLAGUNGSBETRAG             2.500,00
                                // LAUFZEIT        2025-05-15 - 2025-11-17
                                // Habenzinsen
                                // AB 2025-05-15          2,4000%              30,33
                                // KESt                                         7,58-
                                // @formatter:on
                                section -> section //
                                                .documentContext("year", "currency") //
                                                .attributes("date", "amount", "account", "interest", "tax") //
                                                .match("^(?i)(?<date>[\\d]{1,2}\\.[\\d]{1,2}) ABRECHNUNG ZU KONTO (?<account>[\\d]+) [\\d]{1,2}\\.[\\d]{1,2} [\\.,\\d]+$") //
                                                .match("^(?i)VERANLAGUNGSBETRAG[^\\w]*(?<amount>[\\.,\\d]+)$") //
                                                .find("^Habenzinsen$") //
                                                .match("^AB [\\d]{4}-[\\d]{2}-[\\d]{2} .* (?<interest>[\\.,\\d]+)$") //
                                                .match("^(?i)KESt[^\\w]*(?<tax>[\\.,\\d]+)-$")
                                                .assign((t, v) -> { //
                                                        t.setDateTime(asDate(v.get("date") + "." + v.get("year")));
                                                        t.setCurrencyCode(v.get("currency"));
                                                        t.setType(AccountTransaction.Type.INTEREST);
                                                        t.setAmount(asAmount(v.get("interest")));
                                                        t.setNote("Habenzinsen ABRECHNUNG ZU KONTO " + v.get("account"));
                                                        
                                                        var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                                                        t.addUnit(new Unit(Unit.Type.TAX, tax));
                                                        t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                                                }) //
                        ) //

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                                
                            return null;
                        });
    }
}
