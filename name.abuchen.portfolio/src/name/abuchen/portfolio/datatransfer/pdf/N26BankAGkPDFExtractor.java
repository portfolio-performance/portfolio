package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class N26BankAGkPDFExtractor extends AbstractPDFExtractor
{
    public N26BankAGkPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("N26 Bank AG");
        addBankIdentifier("N26 Bank SE");

        addAccountStatementDepositRemovalTransaction();
        addAccountStatementInterestTransaction();
    }

    @Override
    public String getLabel()
    {
        return "N26 Bank AG";
    }

    private void addAccountStatementDepositRemovalTransaction()
    {
        final var type = new DocumentType("Kontoauszug");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^(?!(Zinsertrag|Abgeltungssteuer|Solidarit.tszuschlag)).* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\-|\\+][\\.,\\d]+\\p{Sc}$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Max Mustermann 19.06.2024 +5.000,00€
                        // An Hauptkonto 02.07.2024 -100,00€
                        // @formatter:on
                        .section("date", "type", "amount", "currency") //
                        .match("^(?!(Zinsertrag|Abgeltungssteuer|Solidarit.tszuschlag)).* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<type>[\\-|\\+])(?<amount>[\\.,\\d]+)(?<currency>\\p{Sc})$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type is "-" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);

                            return null;
                        });
    }

    private void addAccountStatementInterestTransaction()
    {
        final var type = new DocumentType("Kontoauszug");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} bis [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$", "^Gesamt \\+[\\.,\\d]+\\p{Sc}$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 01.06.2024 bis 30.06.2024
                                        // Gebühren 0,00€
                                        // Steuer
                                        // Abgeltungssteuer -63,04€
                                        // Solidaritätszuschlag -3,46€
                                        // Gesamt -66,50€
                                        // Zinsertrag +252,16€
                                        // Gesamt +252,16€
                                        //
                                        // 01.12.2023 bis 31.12.2023
                                        // Gebühren 0,00€
                                        // Abgeltungssteuer -0,14€
                                        // Gesamt -0,14€
                                        // Zinsertrag +0,56€
                                        // Zinssatz 2.26%
                                        // Gesamt +0,56€
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "tax", "taxCurrency", "amount", "currency") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) bis [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                                                        .match("^Gesamt (\\-)?(?<tax>[\\.,\\d]+)(?<taxCurrency>\\p{Sc})$") //
                                                        .match("^Zinsertrag \\+(?<amount>[\\.,\\d]+)(?<currency>\\p{Sc})$") //
                                                        .assign((t, v) -> {
                                                            var tax = Money.of(asCurrencyCode(v.get("taxCurrency")), asAmount(v.get("tax")));
                                                            var amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setMonetaryAmount(amount.subtract(tax));
                                                        }),
                                        // @formatter:off
                                        // 01.01.2025 bis 31.01.2025
                                        // Gebühren 0,00€
                                        // Steuer 0,00€
                                        // Zinsertrag +8,55€
                                        // Gesamt +8,55€
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "tax", "taxCurrency", "amount", "currency") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) bis [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                                                        .match("^Steuer (\\-)?(?<tax>[\\.,\\d]+)(?<taxCurrency>\\p{Sc})$") //
                                                        .match("^Zinsertrag \\+(?<amount>[\\.,\\d]+)(?<currency>\\p{Sc})$") //
                                                        .assign((t, v) -> {
                                                            var tax = Money.of(asCurrencyCode(v.get("taxCurrency")), asAmount(v.get("tax")));
                                                            var amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setMonetaryAmount(amount.subtract(tax));
                                                        }))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);

                            return null;
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Abgeltungssteuer -63,04€
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Abgeltungssteuer \\-(?<tax>[\\.,\\d]+)(?<currency>\\p{Sc})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag -3,46€
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag \\-(?<tax>[\\.,\\d]+)(?<currency>\\p{Sc})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }
}
