package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class SolarisbankAGPDFExtractor extends AbstractPDFExtractor
{
    public SolarisbankAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Solarisbank");
        addBankIdentifier("Solaris SE");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Solarisbank AG";
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Rechnungsabschluss");
        this.addDocumentTyp(type);

        // @formatter:off
        // 27.10.2022 26.10.2022 SEPA-Überweisunga141b0b25f9d etoken-google 150,00 EUR
        // 06.10.2022 06.04.2023 Kartenvorgang Visa Geld zurueck AktionVISACLP0324 GB 0,40 EUR
        // @formatter:on
        Block depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*(SEPA\\-.berweisung|.berweisung|Kartenvorgang).* [\\.,\\d]+ [\\w]{3}$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*(SEPA\\-.berweisung|.berweisung|Kartenvorgang)(?<note>.*) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 26.10.2022 26.10.2022 Überweisung an Peter Panzwischen Kunden DE11111111111111111111 -100,00 EUR
        // 28.10.2022 27.10.2022 Kartenzahlung Amazon.de AMAZON.DE LU -16,10 EUR
        // 19.10.2022 19.10.2022 SEPA-Lastschrift 11,99 EUR -11,99 EUR
        // 20.10.2022 20.04.2023 SEPA-Überweisungan Peter Pan -18,78 EUR
        // @formatter:on
        Block removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*(Kartenzahlung|.berweisung|SEPA\\-Lastschrift|SEPA\\-.berweisung).* \\-[\\.,\\d]+ [\\w]{3}$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*(Kartenzahlung|.berweisung|SEPA\\-Lastschrift|SEPA\\-.berweisung)(?<note>.*) \\-(?<amount>[\\.,\\d]+) (?<currency>\\w{3})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));
    }
}
