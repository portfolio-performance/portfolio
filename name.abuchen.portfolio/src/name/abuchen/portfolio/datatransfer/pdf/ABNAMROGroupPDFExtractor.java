package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.ParsedData;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class ABNAMROGroupPDFExtractor extends AbstractPDFExtractor
{
    // 24.10.2011 19.10.2011 12030000-001 Zahlungseingang 100,00
    // 11.10.2012 11.10.2012 B2D11BI5S00A Ru¨ckzahlung Ihres Festgeldes
    // 50.000,00
    private static final String DEPOSIT = "^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* (Zahlungseingang|Ru.ckzahlung Ihres Festgeldes)[\\s]{1,}(?<amount>[\\.,\\d]+)";
    // 16.12.2011 16.12.2011 12030000-001 Zahlungsausgang 2.000,00
    // 11.04.2012 11.04.2012 B2D11BI5S00A Abschluss eines Festgeldes 50.000,00
    private static final String REMOVAL = "^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* (Zahlungsausgang|Abschluss eines Festgeldes)[\\s]{1,}(?<amount>[\\.,\\d]+)";
    // 28.06.2019 01.07.2019 DE5050324040 Ihre Zinsabrechnung 63,28
    // 30.12.2011 01.01.2012 5000510765 Ihre Tagesgeldzinsen 114,34
    // 11.10.2012 11.10.2012 B2D11BI5S00A Zinszahlung Festgeld 596,33
    private static final String INTEREST = "^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* (Ihre (Zinsabrechnung|Tagesgeldzinsen)|Zinszahlung Festgeld)[\\s]{1,}(?<amount>[\\.,\\d]+)";
    // 31.12.2012 01.01.2013 1000130541 Abgeltungssteuer 49,74
    // 31.12.2012 01.01.2013 1000130541 Solidarita¨tszuschlag 2,73
    private static final String TAXES = "^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* (Abgeltungssteuer|Solidarita.tszuschlag)[\\s]{1,}(?<amount>[\\.,\\d]+)";

    public ABNAMROGroupPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("ABN AMRO Bank N.V.");

        addTransactions();
    }

    @Override
    public String getLabel()
    {
        return "ABN AMRO Group / MoneYou";
    }

    private void addTransactions()
    {
        final DocumentType type = new DocumentType("Kontoauszug", //
                        documentContext -> documentContext //
                        .oneOf( //
                                        // @formatter:off
                                        // Tagesgeldkonto (alle Betra¨ge in EUR) 63,28 28.06.2019 85.288,02
                                        // @formatter:on
                                        section -> section.attributes("currency") //
                                        .match("^Tagesgeldkonto \\(alle Betr.* in (?<currency>[\\w]{3}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))
                                        ,
                                        // @formatter:off
                                        // Tagesgeldkonto 100,00 24.10.2011 100,00
                                        // @formatter:on
                                        section -> section.attributes("currency") //
                                        .match("^Tagesgeldkonto (?<currency>.*)$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode("EUR"))))
                        );

        this.addDocumentTyp(type);

        Block depositBlock = new Block(DEPOSIT);
        depositBlock.set(depositTransaction(type, DEPOSIT));
        type.addBlock(depositBlock);

        Block removalBlock = new Block(REMOVAL);
        removalBlock.set(removalTransaction(type, REMOVAL));
        type.addBlock(removalBlock);

        Block interestBlock = new Block(INTEREST);
        interestBlock.set(interestTransaction(type, INTEREST));
        type.addBlock(interestBlock);

        Block taxesBlock = new Block(TAXES);
        taxesBlock.set(taxesTransaction(type, TAXES));
        type.addBlock(taxesBlock);
    }

    private Transaction<AccountTransaction> depositTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match(regex) //
                        .assign((t, v) -> {
                            assignmentsProvider(t, v);
                        })

                        .wrap(TransactionItem::new);
    }

    private Transaction<AccountTransaction> removalTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match(regex) //
                        .assign((t, v) -> {
                            assignmentsProvider(t, v);
                        })

                        .wrap(TransactionItem::new);
    }

    private Transaction<AccountTransaction> interestTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match(regex) //
                        .assign((t, v) -> {
                            assignmentsProvider(t, v);
                        })

                        .wrap(TransactionItem::new);
    }

    private Transaction<AccountTransaction> taxesTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match(regex) //
                        .assign((t, v) -> {
                            assignmentsProvider(t, v);
                        })

                        .wrap(TransactionItem::new);
    }

    private void assignmentsProvider(AccountTransaction t, ParsedData v)
    {
        t.setDateTime(asDate(v.get("date")));
        t.setAmount(asAmount(v.get("amount")));
        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
    }
}
