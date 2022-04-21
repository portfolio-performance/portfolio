package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class UnicreditPDFExtractor extends AbstractPDFExtractor
{

    public UnicreditPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("UniCredit Bank AG"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendTransaction();
    }

    @Override
    public String getLabel()
    {
        return "UniCredit Bank AG / HypoVereinsbank (HVB)"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(K a u f|V e r k a u f)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^W e r t p a p i e r \\- A b r e c h n u n g ([\\s]+)?(K a u f| V e r k a u f)( .*)?$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^W e r t p a p i e r \\- A b r e c h n u n g ([\\s]+)?(?<type>(K a u f| V e r k a u f))( .*)?$")
                .assign((t, v) -> {
                    if (stripBlanks(v.get("type")).equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Nennbetrag Wertpapierbezeichnung Wertpapierkennnummer/ISIN
                // SANOFI S.A. 920657
                // ST 22
                // ACTIONS PORT. EO 2 FR0000120578
                // Kurswert EUR 1.547,26
                .section("name", "wkn", "name1", "isin", "currency").optional()
                .find("Nennbetrag Wertpapierbezeichnung Wertpapierkennnummer\\/ISIN.*")
                .match("^(?<name>.*) (?<wkn>[\\w]{6})$")
                .match("^(?<name1>.*) (?<isin>[\\w]{12})$")
                .match("^Kurswert (?<currency>[\\w]{3}) [\\.,\\d]+.*$")
                .assign((t, v) -> {
                    v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Nennbetrag Wertpapierbezeichnung Wertpapierkennnummer/ISIN
                // ST 25 FIRST EAGLE AMUNDI-INTERNATIO. A1JQVV ACTIONS
                // NOM. AE-C O.N. LU0565135745
                // Kurswert EUR 4.803,50
                .section("name", "name1", "wkn", "isin").optional()
                .find("Nennbetrag Wertpapierbezeichnung ([\\s]+)?Wertpapierkennnummer\\/ISIN.*")
                .match("^ST [\\.,\\d]+ (?<name>.*) (?<wkn>[\\w]{6})[\\s]{2,}(?<name1>.*) (?<isin>[\\w]{12})$")
                .match("^Kurswert (?<currency>[\\w]{3}) [\\.,\\d]+.*$")
                .assign((t, v) -> {
                    v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // ST 22
                // ST 25 FIRST EAGLE AMUNDI-INTERNATIO. A1JQVV ACTIONS
                .section("shares")
                .match("^ST (?<shares>[\\.,\\d]+).*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Zum Kurs von Ausführungstag/Zeit Ausführungsort Verwahrart
                // EUR 192,14 20.04.2021 03.53.15 WP-Rechnung GS
                .section("time").optional()
                .find("Zum Kurs von .*")
                .match("^[\\w]{3} [\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?(?<time>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}) .*$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                // Zum Kurs von Ausf}hrungstag/Zeit Ausf}hrungsort Verwahrart
                // 15.02.2016
                // EUR 70,33 PARIS WP-Rechnung
                // 16.28.03
                .section("time").optional()
                .find("Zum Kurs von .*")
                .match("^[\\w]{3} [\\.,\\d]+ .*$")
                .match("^(?<time>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2})$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                .oneOf(
                                // Zum Kurs von Ausf}hrungstag/Zeit Ausf}hrungsort Verwahrart
                                // 15.02.2016
                                section -> section
                                        .attributes("date")
                                        .find("Zum Kurs von .*")
                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                        .assign((t, v) -> {
                                            if (type.getCurrentContext().get("time") != null)
                                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                                            else
                                                t.setDate(asDate(v.get("date")));
                                        })
                                ,
                                // Zum Kurs von Ausführungstag/Zeit Ausführungsort Verwahrart
                                // EUR 192,14 20.04.2021 03.53.15 WP-Rechnung GS
                                section -> section
                                        .attributes("date")
                                        .find("Zum Kurs von .*")
                                        .match("^[\\w]{3} [\\.,\\d]+ (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                                        .assign((t, v) -> {
                                            if (type.getCurrentContext().get("time") != null)
                                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                                            else
                                                t.setDate(asDate(v.get("date")));
                                        })
                        )

                // Belastung (vor Steuern) EUR 1.560,83
                // Gutschrift (vor Steuern) EUR 8.175,91
                .section("currency", "amount")
                .match("^(Belastung|Gutschrift) \\(vor Steuern\\) (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)( .*)?$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Umsatzreferenz: 20160215WAB1861426155
                .section("note").optional()
                .match("(?<note>Umsatzreferenz: .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Wertpapiermitteilung \\- Ertragszahlung");
        this.addDocumentTyp(type);

        Block block = new Block("^Wertpapiermitteilung \\- Ertragszahlung .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // ACATIS GANÉ VALUE EVENT FONDS Wertpapierkennnummer  A1T73W / DE000A1T73W9
                // INHABER-ANTEILE C
                // Geschäftsjahr 2020/2021
                // zahlbar mit EUR  15,00 Bruttobetrag   EUR 0,99
                .section("name", "wkn", "isin", "name1", "currency")
                .match("^(?<name>.*) Wertpapierkennnummer ([\\s]+)(?<wkn>.*) \\/ (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)$")
                .match("^zahlbar mit (?<currency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+ .*$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Geschäftsjahr"))
                        v.put("name", v.get("name") + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Stück 0,066
                .section("shares")
                .match("^St.ck (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Valuta 18.05.2021 Gutschrift     EUR 0,99
                .section("date")
                .match("^Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Valuta 18.05.2021 Gutschrift     EUR 0,99
                .section("currency", "amount")
                .match("^Valuta [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Gutschrift ([\\s]+)?(?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap(t -> new TransactionItem(t));

        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Brokerkommission* EUR 0,27
                .section("currency", "fee").optional()
                .match("^Brokerkommission\\* (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?( .*)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Transaktionsentgelt* EUR 3,09
                .section("currency", "fee").optional()
                .match("^Transaktionsentgelt\\* (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?( .*)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Provision EUR 10,21
                .section("currency", "fee").optional()
                .match("^Provision (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?( .*)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Wertpapierprovision* EUR 41,09- 
                .section("currency", "fee").optional()
                .match("^Wertpapierprovision\\* (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?( .*)?$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
