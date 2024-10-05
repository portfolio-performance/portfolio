package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class EstateGuruPDFExtractor extends AbstractPDFExtractor
{
    public EstateGuruPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Estateguru");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Estateguru";
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("(Transaktionsbericht|Transactions Report)");
        this.addDocumentTyp(type);

        Block depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} (Einzahlung|Principal) .*$");
        type.addBlock(depositBlock);
        depositBlock.setMaxSize(1);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 25.07.2023, 09:10 25.07.2023, 09:10 Einzahlung Genehmigt € 300,00 € 364,11
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "currency", "amount") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}), [\\d]{2}:[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} .*" //
                                                                        + "(?<currency>\\p{Sc}) (?<amount>[\\.,\\d]+) " //
                                                                        + "\\p{Sc} [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // 11.09.2024, 03:00 12.09.2024, 06:00 Principal Approved skredit - € 5.41 € 263.66
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "currency", "amount") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}), [\\d]{2}:[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} .*" //
                                                                        + "(?<currency>\\p{Sc}) (?<amount>[\\.,\\d]+) " //
                                                                        + "\\p{Sc} [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 02.02.2020, 00:10 03.02.2020, 17:59 Auszahlung Genehmigt (€ -51,70) € 51,72
        // @formatter:on
        Block removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} Auszahlung .*$");
        type.addBlock(removalBlock);
        removalBlock.setMaxSize(1);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })


                        .section("date", "currency", "amount") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}), [\\d]{2}:[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} .*" //
                                        + "\\((?<currency>\\p{Sc}) \\-(?<amount>[\\.,\\d]+)\\) " //
                                        + "\\p{Sc} [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 21.06.2024, 03:00 21.06.2024, 06:00 Zins Genehmigt Entwicklungskredit - 2.Stufe € 0,42 € 6,50
        // 23.04.2024, 03:00 24.04.2024, 06:00 Strafe Genehmigt Entwicklungskredit - 2.Stufe € 0,07 € 23,08
        // 10.04.2024, 00:00 11.04.2024, 06:00 Entschädigung Genehmigt skredit - € 0,01 € 53,12
        // 28.09.2024, 03:00 30.09.2024, 06:00 Interest Approved Entwicklungskredit - 3.Stufe € 0.42 € 267.63
        // @formatter:on
        Block interestBlock = new Block(
                        "^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} " //
                                        + "(Zins|Strafe|Entsch.digung|Interest).*$");
        type.addBlock(interestBlock);
        interestBlock.setMaxSize(1);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "note", "currency", "amount") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}), [\\d]{2}:[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} " //
                                        + "(?<note>(Zins" //
                                        + "|Strafe" //
                                        + "|Entsch.digung" //
                                        + "|Interest)).*" //
                                        + "(?<currency>\\p{Sc}) (?<amount>[\\.,\\d]+) " //
                                        + "\\p{Sc} [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 05.04.2024, 00:34 05.04.2024, 00:34 Vermögensverwaltungsgebühr Genehmigt (€ -0,61) € 63,00
        // 05.09.2024, 00:24 05.09.2024, 00:24 AUM fee Approved (€ -0.24) € 256.65
        // @formatter:on
        Block feesBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} "
                        + "(Verm.gensverwaltungsgeb.hr|AUM).*$");
        type.addBlock(feesBlock);
        feesBlock.setMaxSize(1);
        feesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .section("date", "note", "currency", "amount") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}), [\\d]{2}:[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} " //
                                        + "(?<note>(Verm.gensverwaltungsgeb.hr" //
                                        + "|AUM)).*" //
                                        + "\\((?<currency>\\p{Sc}) \\-(?<amount>[\\.,\\d]+)\\) " //
                                        + "\\p{Sc} [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));
    }

    @Override
    protected long asAmount(String value)
    {
        String language = "de";
        String country = "DE";

        int lastDot = value.lastIndexOf(".");
        int lastComma = value.lastIndexOf(",");

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en";
            country = "US";
        }

        return ExtractorUtils.convertToNumberLong(value, Values.Amount, language, country);
    }
}
