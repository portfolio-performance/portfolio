package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.replaceMultipleBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class BarclaysBankIrelandPLCPDFExtractor extends AbstractPDFExtractor
{
    public BarclaysBankIrelandPLCPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Barclays Bank Ireland PLC");

        addCreditcardStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Barclays Bank Ireland PLC";
    }

    private void addCreditcardStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Beleg- Buchungs-/ Beschreibung Karte Betrag (EUR)
                                        // Gebucht am Wertstellung am Verwendungszweck Betrag (EUR)
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^.* \\((?<currency>[A-Z]{3})\\)$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 23.12.2023 25.12.2023 Lidl sagt Danke        Ort           DE Visa B A             60,78-
        // 23.12.2023 28.12.2023 Lidl sagt Danke        Ort           DE Visa B A             60,78+
        // @formatter:on
        Block depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                        + "(?!(Abgeltungsteuer|Solidarit.tszuschlag)).* [\\.,\\d]+[\\-|\\+]$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "type") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?<note>.{1,36})" //
                                        + ".* " //
                                        + "(?<amount>[\\.,\\d]+)" //
                                        + "(?<type>[\\-|\\+])$") //
                        .assign((t, v) -> {
                            // Is type --> "-" change from DEPOSIT to REMOVAL
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(trim(replaceMultipleBlanks(v.get("note"))));
                        })

                        .wrap(TransactionItem::new));

        Block interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Habenzinsen [\\.,\\d]+$");
        type.addBlock(interestBlock);
        interestBlock.setMaxSize(4);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 31.12.2023 31.12.2023 Habenzinsen 4,80
                        // @formatter:on
                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "Habenzinsen " //
                                        + "(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        // @formatter:off
                        // 31.12.2023 31.12.2023 Abgeltungsteuer -1,20
                        // @formatter:on
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Abgeltungsteuer \\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));

                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                        })

                        // @formatter:off
                        // 31.12.2023 31.12.2023 Solidaritätszuschlag -0,06
                        // @formatter:on
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Solidarit.tszuschlag \\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));

                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                        })

                        .wrap(TransactionItem::new));
    }
}
