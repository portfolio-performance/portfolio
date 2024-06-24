package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class ArkeaDirectBankPDFExtractor extends AbstractPDFExtractor
{
    public ArkeaDirectBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Arkéa Direct Bank");

        addBuySellTransaction();
        addDividendeTransaction();
        addTaxesTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Arkéa Direct Bank / Fortuneo Banque";
    }

    private void addBuySellTransaction()
    {
        var securityRange = new Block("^.* (ACTION|TRACKER) : .* \\([A-Z]{2}[A-Z0-9]{9}[0-9]\\)$") //
                        .asRange(section -> section //
                                        // @formatter:off
                                        // ¢ ACTION : ORANGE (FR0000133308)
                                        // Quantité 46 Cours 10,646 €
                                        //
                                        // ¢ TRACKER : AMUNDI MSCI WORLD UC.ETF EUR D (LU2655993207)
                                        // Quantité 7 Cours 30,24 €
                                        // @formatter:on
                                        .attributes("name", "isin", "currency") //
                                        .match("^.* (ACTION|TRACKER) : (?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\)$") //
                                        .match("^Quantit. [\\,\\d\\s]+ Cours [\\,\\d\\s]+ (?<currency>\\p{Sc})$"));

        final DocumentType type = new DocumentType("AVIS D.OP.RATIONS", securityRange);
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} R.f.rence .*$", "^Montant NET .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Quantité 46 Cours 10,646 €
                        // @formatter:on
                        .section("shares") //
                        .documentRange("name", "isin", "currency") //
                        .match("^Quantit. (?<shares>[\\,\\d\\s]+) Cours [\\,\\d\\s]+ \\p{Sc}$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        // @formatter:off
                        // 26-03-2024 Référence 94R6134018440990
                        // 16:27:35 Sens Achat - Exécution unique
                        // @formatter:on
                        .section("date", "time") //
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) R.f.rence .*$") //
                        .match("^(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .* \\- .*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Montant NET 491,67 € 491,67 €
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Montant NET [\\,\\d\\s]+ \\p{Sc} (?<amount>[\\,\\d\\s]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // 26-03-2024 Référence 94R6134018440990
                        // @formatter:on
                        .section("note").optional() //
                        .match("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} (?<note>R.f.rence .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("AVIS D.ENCAISSEMENT COUPON");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^AVIS D.ENCAISSEMENT COUPON$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        //   n       ACTION  ORANGE (FR0000133308)
                        // Montant unitaire 0,30 €
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^.* ACTION (?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\)$") //
                        .match("^Montant unitaire [\\,\\d\\s]+ (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Quantité 450
                        // @formatter:on
                        .section("shares") //
                        .match("^Quantit. (?<shares>[\\,\\d\\s]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 04/12/2023
                        // @formatter:on
                        .section("date") //
                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Montant Net 135,00 €
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Montant Net (?<amount>[\\,\\d\\s]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addTaxesTransaction()
    {
        DocumentType type = new DocumentType("AVIS RECAPITULATIF DE LA TAXE SUR LES");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^.* \\([A-Z]{2}[A-Z0-9]{9}[0-9]\\)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // ¾ ORANGE (FR0000133308)
                        // Montant global soumis à la TTF : 489,72 €
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^. (?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\)$") //
                        .match("^Montant global soumis à la TTF : [\\,\\d\\s]+ (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Quantité compensée soumise à la TTF : 46
                        // @formatter:on
                        .section("shares") //
                        .match("^Quantit. compens.e soumise . la TTF : (?<shares>[\\,\\d\\s]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 26-03-2024 Taxe Transaction Financière
                        // @formatter:on
                        .section("date") //
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) Taxe Transaction Financi.re$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Montant global soumis à la TTF : 489,72 €
                        // Taux de TTF : 0,30 %
                        // 1,47
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Montant global soumis à la TTF : [\\,\\d\\s]+ (?<currency>\\p{Sc})$") //
                        .find("Taux de TTF : [\\,\\d\\s]+ %")
                        .match("^(?<amount>[\\,\\d\\s]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // 26-03-2024 Référence 94R6134018440990
                        // @formatter:on
                        .section("note").optional() //
                        .match("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} (?<note>R.f.rence .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Prélèvement fiscal 0,00 €
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Pr.l.vement fiscal (?<tax>[\\,\\d\\s]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Prélèvements sociaux 0,00 €
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Pr.l.vements sociaux (?<tax>[\\,\\d\\s]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Courtage et Commission 1,95 €
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Courtage et Commission (?<fee>[\\,\\d\\s]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "fr", "FR");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "fr", "FR");
    }
}
