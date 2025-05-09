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
        final var type = new DocumentType("(Transaktionsbericht|Transactions Report)");
        this.addDocumentTyp(type);

        // @formatter:off
        // 25.07.2023, 09:10 25.07.2023, 09:10 Einzahlung Genehmigt € 300,00 € 364,11
        // 03.10.2023, 09:10 03.10.2023, 09:10 Deposit Approved € 30.00 € 79.94
        // 11.09.2024, 03:00 12.09.2024, 06:00 Principal Approved skredit - € 5.41 € 263.66
        // 02.12.2023, 12:57 02.12.2023, 12:57 Secondary Market Approved Entwicklungskredit - 3.Stufe € 13.54 € 276.09
        // 01.10.2024, 03:00 03.10.2024, 06:00 Hauptbetrag Genehmigt skredit - € 50,00 € 369,20
        // 02.12.2023, 12:57 02.12.2023, 12:57 Zweitmarkt Genehmigt Entwicklungskredit - 3.Stufe € 13,54 € 276,09
        // 23.08.2024, 09:30 23.08.2024, 09:30 Investition Aufgehoben Geschäftskredi € 64,00 € 131,77
        // 14.03.2023, 03:00 14.03.2023, 03:00 Cashback Genehmigt skredit - € 0,38 € 19,88
        // @formatter:on
        var depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} " //
                        + "(Einzahlung Genehmigt" //
                        + "|Deposit Approved" //
                        + "|Principal Approved" //
                        + "|Secondary Market Approved" //
                        + "|Hauptbetrag Genehmigt" //
                        + "|Zweitmarkt Genehmigt" //
                        + "|Investition Aufgehoben" //
                        + "|Cashback Genehmigt).*$");
        type.addBlock(depositBlock);
        depositBlock.setMaxSize(1);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "currency", "amount") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}), [\\d]{2}:[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} .*" //
                                        + "(?<currency>\\p{Sc}) (?<amount>[\\.,\\d]+) " //
                                        + "\\p{Sc} [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 02.02.2020, 00:10 03.02.2020, 17:59 Auszahlung Genehmigt (€ -51,70) € 51,72
        // 20.12.2023, 16:54 20.12.2023, 16:54 Investment Approved skredit - (€ -50.00) € 17.95
        // 04.12.2023, 15:16 04.12.2023, 15:16 Withdrawal Approved (€ -273.30) € 1.38
        // 20.12.2023, 16:54 20.12.2023, 16:54 Investition Genehmigt skredit - (€ -50,00) € 17,95
        // 10.12.2024, 16:21 10.12.2024, 16:21 Investition Ausstehend Entwicklungskredit - 10.Stufe (€ -53,00) € 0,70
        // 25.07.2024, 18:39 25.07.2024, 18:39 Investition Abgebrochen Geschäftskredi (€ -64,00) € 1,04
        // @formatter:on
        var removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} " //
                        + "(Auszahlung Genehmigt" //
                        + "|Investment Approved" //
                        + "|Withdrawal Approved" //
                        + "|Investition Approved" //
                        + "|Investition Genehmigt" //
                        + "|Investition Ausstehend" //
                        + "|Investition Abgebrochen).*$");
        type.addBlock(removalBlock);
        removalBlock.setMaxSize(1);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
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
        // 01.11.2023, 00:00 29.11.2023, 05:00 Indemnity Approved Entwicklungskredit - 3.Stufe € 0.01 € 242.49
        // 20.09.2023, 19:23 20.09.2023, 19:23 Secondary Market Profit Approved Entwicklungskredit - 22.Stufe € 0.27 € 74.60
        // 20.09.2023, 19:23 20.09.2023, 19:23 Gewinn auf dem Zweitmarkt Genehmigt Entwicklungskredit - 22.Stufe € 0,27 € 74,60
        // 10.02.2023, 03:00 06.03.2023, 05:00 Penalty Approved Entwicklungskredit - 1.Stufe € 0.36 € 21.01
        // 01.11.2023, 00:00 29.11.2023, 05:00 Entschädigung Genehmigt Entwicklungskredit - 3.Stufe € 0,01 € 242,49
        // @formatter:on
        var interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} " //
                        + "(Zins Genehmigt" //
                        + "|Strafe Genehmigt" //
                        + "|Entsch.digung Genehmigt" //
                        + "|Interest Approved" //
                        + "|Indemnity Approved" //
                        + "|Secondary Market Profit Approved" //
                        + "|Gewinn auf dem Zweitmarkt Genehmigt" //
                        + "|Penalty Approved" //
                        + "|Entsch.digung Genehmigt).*$");
        type.addBlock(interestBlock);
        interestBlock.setMaxSize(1);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "note", "currency", "amount") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}), [\\d]{2}:[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} " //
                                        + "(?<note>(Zins" //
                                        + "|Strafe" //
                                        + "|Entsch.digung" //
                                        + "|Interest" //
                                        + "|Indemnity" //
                                        + "|Secondary Market Profit"
                                        + "|Gewinn auf dem Zweitmarkt" //
                                        + "|Penalty" //
                                        + "|Entsch.digung)).*" //
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
        // 04.12.2023, 15:16 04.12.2023, 00:00 Withdraw fee Approved (€ -1.00) € 274.68
        // 02.12.2023, 12:57 02.12.2023, 12:57 Sale fee Approved Entwicklungskredit - 3.Stufe (€ -0.41) € 275.68
        // 04.12.2023, 15:16 04.12.2023, 00:00 Abhebegebühr Genehmigt (€ -1,00) € 274,68
        // 02.12.2023, 12:57 02.12.2023, 12:57 Verkaufsgebühr Genehmigt Entwicklungskredit - 3.Stufe (€ -0,41) € 275,68
        // @formatter:on
        var feesBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} " //
                        + "(Verm.gensverwaltungsgeb.hr Genehmigt" //
                        + "|AUM fee Approved" //
                        + "|Withdraw fee Approved" //
                        + "|Sale fee Approved" //
                        + "|Abhebegeb.hr Genehmigt" //
                        + "|Verkaufsgeb.hr Genehmigt).*$");
        type.addBlock(feesBlock);
        feesBlock.setMaxSize(1);
        feesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .section("date", "note", "currency", "amount") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}), [\\d]{2}:[\\d]{2} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}, [\\d]{2}:[\\d]{2} " //
                                        + "(?<note>(Verm.gensverwaltungsgeb.hr" //
                                        + "|AUM" //
                                        + "|Withdraw fee" //
                                        + "|Sale fee" //
                                        + "|Abhebegeb.hr" //
                                        + "|Verkaufsgeb.hr)).*" //
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
        var language = "de";
        var country = "DE";

        var lastDot = value.lastIndexOf(".");
        var lastComma = value.lastIndexOf(",");

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en";
            country = "US";
        }

        return ExtractorUtils.convertToNumberLong(value, Values.Amount, language, country);
    }
}
