package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class OldenburgischeLandesbankAGPDFExtractor extends AbstractPDFExtractor
{
    public OldenburgischeLandesbankAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Oldenburgische Landesbank AG");

        addBuySellTransaction();
        addDividendeTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Oldenburgische Landesbank AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("WERTPAPIERABRECHNUNG");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*Depotnummer .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Kauf - iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile
                        // DE000A0H0785 (A0H078) 0,033037 10,0350 EUR 1,47 EUR
                        //
                        // Kauf: AIS-AM.WORLD SRI PAB Act.Nom. UCITS ETF DR (C)o.N.
                        // LU1861134382 (A2JSDA) 9,727757 93,4642 EUR 982,07 EUR
                        // @formatter:on
                        .section("name", "isin", "wkn", "currency") //
                        .match("^Kauf( \\-|:) (?<name>.*)$") //
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\) [\\.,\\d]+ [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // DE000A0H0785 (A0H078) 0,033037 10,0350 EUR 1,47 EUR
                        // @formatter:on
                        .section("shares") //
                        .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\) (?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Depotnummer 0195923225 Ausführung 17.10.2023 18:11:56
                        // @formatter:on
                        .section("time").optional() //
                        .match("^.* Ausf.hrung .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        // @formatter:off
                        // Depotnummer 50000000 Ausführung 17.05.2023
                        // @formatter:on
                        .section("date") //
                        .match("^.* Ausf.hrung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().get("time") != null)
                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                            else
                                t.setDate(asDate(v.get("date")));
                        })

                        // @formatter:off
                        // Ausmachender Betrag: 1,48 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Ausmachender Betrag: (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Orderreferenz 100000
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Orderreferenz (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("Ord.-Ref.: " + v.get("note")))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("ERTRAGSAUSSCH.TTUNG");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.*Depotnummer .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                .subject(() -> {
                    AccountTransaction accountTransaction = new AccountTransaction();
                    accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                    return accountTransaction;
                })

                // @formatter:off
                // Ausschüttung – iS.EO G.B.C.1.5-10.5y.U.ETF DE Inhaber-Anteile
                // DE000A0H0785 (A0H078) 71,851808 8,895903 EUR
                // @formatter:on
                .section("name", "isin", "wkn", "currency") //
                .match("^Aussch.ttung \\– (?<name>.*)$") //
                .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\((?<wkn>[A-Z0-9]{6})\\) [\\.,\\d]+ [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // DE000A0H0785 (A0H078) 71,851808 8,895903 EUR
                // @formatter:on
                .section("shares") //
                .match("^[A-Z]{2}[A-Z0-9]{9}[0-9] \\([A-Z0-9]{6}\\) (?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\w]{3}$") //
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Zahlbarkeitstag 15.05.2023
                // @formatter:on
                .section("date") //
                .match("^Zahlbarkeitstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                // Ausmachender Betrag + 7,44 EUR
                // @formatter:on
                .section("amount", "currency") //
                .match("^Ausmachender Betrag \\+ (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap(TransactionItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Alter Saldo                                       EUR                  0,00+
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Alter Saldo.* (?<currency>[\\w]{3}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 03.08.23 03.08. CORE-LA-EV   EINGANG VORBEHALTEN                      10,00+
        // @formatter:on
        Block depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} [\\d]{2}\\.[\\d]{2}\\. .* EINGANG VORBEHALTEN.* [\\.,\\d]+[\\+|\\-].*$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("year", "date", "amount", "type") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{2}) (?<date>[\\d]{2}\\.[\\d]{2}\\.) .* EINGANG VORBEHALTEN.* (?<amount>[\\.,\\d]+)(?<type>[\\+|\\-]).*$") //
                        .assign((t, v) -> {
                            // Is type is "-" change from DEPOSIT to REMOVAL
                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Orderentgelt: 0,01 EUR
                        // @formatter:on
                        .section("fee", "currency").optional()
                        .match("^Orderentgelt: (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
