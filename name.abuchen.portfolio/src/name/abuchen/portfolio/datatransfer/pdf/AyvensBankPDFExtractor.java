package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class AyvensBankPDFExtractor extends AbstractPDFExtractor
{
    public AyvensBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Ayvens Bank");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Ayvens Bank N.V.";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Kontoauszug");

        this.addDocumentTyp(type);

        // @formatter:off
        // 01-04-2026 Zinsen dazu 19,23 €
        // 01-03-2026 Zinsen dazu 19,20 €
        // @formatter:on
        var interestBlock = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} Zinsen dazu [\\.,\\d]+ \\p{Sc}$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "amount", "currency") //
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) Zinsen dazu (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 06-01-2026 ND21837570459852711862 dazu 9.999,00 €
        // 02-01-2026 dH76100077742295069568 Initiale Uberweisung dazu 1,00 €
        // 02-04-2026 dI53998518932366313094 von Adva gesamt inkl. April Zins dazu 78.683,12 €
        // 31-03-2026 BC60230308373922159806 R ex Kasse 190 dazu 190,00 €
        // 31-03-2026 rQ26236415973132438367 monatl. Haeufchen 588,- dazu 588,00 €
        // @formatter:on
        var depositBlock = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} (?!Zinsen).*dazu [\\.,\\d]+ \\p{Sc}$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "currency") //
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<note>(?!Zinsen).*) dazu (?<amount>[\\.,\\d]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 31-03-2026 8912317415 Ab -64.283,94 €
        // @formatter:on
        var removalBlock = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} .* Ab \\-[\\.,\\d]+ \\p{Sc}$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "currency") //
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<note>.*) Ab \\-(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));
    }
}
