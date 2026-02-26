package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class AdvanziaBankPDFExtractor extends AbstractPDFExtractor
{
    public AdvanziaBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Advanzia Bank S.A.");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Advanzia Bank S.A.";
    }


    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Kontoauszug Advanziakonto", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Datum Beschreibung Betrag (EUR)
                                        // Valutadatum Buchungstag Beschreibung Betrag (EUR)
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^.*Beschreibung Betrag \\((?<currency>[A-Z]{3})\\).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 30.06.2023 ZINSERTRAG 47,11
        // @formatter:on
        var interestBlock_Format01 = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ZINSERTRAG [\\.\\d]+,[\\d]{2}.*$");
        type.addBlock(interestBlock_Format01);
        interestBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) ZINSERTRAG (?<amount>[\\.\\d]+,[\\d]{2}).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 31.07.2025 31.07.2025 ZINSERTRAG 123,66
        // @formatter:on
        var interestBlock_Format02 = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ZINSERTRAG [\\.\\d]+,[\\d]{2}.*$");
        type.addBlock(interestBlock_Format02);
        interestBlock_Format02.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) ZINSERTRAG (?<amount>[\\.\\d]+,[\\d]{2}).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 14.06.2023 EINZAHLUNG 100,00
        // @formatter:on
        var depositBlock_Format01 = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} EINZAHLUNG [\\.\\d]+,[\\d]{2}.*$");
        type.addBlock(depositBlock_Format01);
        depositBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) EINZAHLUNG (?<amount>[\\.\\d]+,[\\d]{2}).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 03.07.2025 04.07.2025 EINZAHLUNG 190,00
        // @formatter:on
        var depositBlock_Format02 = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} EINZAHLUNG [\\.\\d]+,[\\d]{2}.*$");
        type.addBlock(depositBlock_Format02);
        depositBlock_Format02.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) EINZAHLUNG (?<amount>[\\.\\d]+,[\\d]{2}).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 15.06.2023 AUSZAHLUNG -100,00
        // @formatter:on
        var removalBlock_Format01 = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} AUSZAHLUNG \\-[\\.\\d]+,[\\d]{2}.*$");
        type.addBlock(removalBlock_Format01);
        removalBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) AUSZAHLUNG \\-(?<amount>[\\.\\d]+,[\\d]{2}).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 28.07.2025 28.07.2025 AUSZAHLUNG -340,00
        // @formatter:on
        var removalBlock_Format02 = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} AUSZAHLUNG \\-[\\.\\d]+,[\\d]{2}.*$");
        type.addBlock(removalBlock_Format02);
        removalBlock_Format02.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) AUSZAHLUNG \\-(?<amount>[\\.\\d]+,[\\d]{2}).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));
    }
}
