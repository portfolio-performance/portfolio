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
public class VanguardGroupEuropePDFExtractor extends AbstractPDFExtractor
{
    public VanguardGroupEuropePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Vanguard Group Europe GmbH");

        addBuySellTransaction();
        addDividendTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Vanguard Group Europe GmbH";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierabrechnung: Kauf");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Wertpapierabrechnung: Kauf$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Wertpapierbezeichnung FDBL Vanguard LifeStrategy 60% Equity UCITS ETF
                // (EUR) Distributing
                // ISIN IE00BMVB5Q68
                // Kurs (Stückpreis) EUR 25,0900
                .section("name", "name1", "isin", "currency")
                .match("^Wertpapierbezeichnung (?<name>.*)")
                .match("^(?<name1>.*)$")
                .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .match("^Kurs \\(.*\\) (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("ISIN"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Nominal / Stück 11,956954
                .section("shares")
                .match("^Nominal \\/ St.ck (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Ausführungstag / -zeit 03.02.2023 um 11:11:17 Uhr
                .section("date", "time")
                .match("^Ausf.hrungstag \\/ \\-zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                .oneOf(
                                // Kurswert EUR 300,00
                                // EUR 300,00
                                // Abrechnungskonto DE01234567891234567890
                                section -> section
                                        .attributes("currency", "amount")
                                        .find("Kurswert [\\w]{3} [\\.,\\d]+$")
                                        .match("^(?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .match("^Abrechnungskonto .*$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                                ,
                                // Abbuchungsbetrag EUR 300,00
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^Abbuchungsbetrag (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                // Referenznummer 12345678
                .section("note").optional()
                .match("^(?<note>Referenznummer .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);
    }

    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Bardividende");
        this.addDocumentTyp(type);

        Block block = new Block("^Bardividende$", "^Steuerliche Informationen .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // @formatter:off
                // Wertpapierbezeichnung FDBL Vanguard LifeStrategy 60% Equity UCITS
                // ETF (EUR) Distributing  (IE00BMVB5Q68)
                // ISIN IE00BMVB5Q68
                // Währung EUR
                // @formatter:on
                .section("name", "name1", "isin", "currency")
                .match("^Wertpapierbezeichnung (?<name>.*)$")
                .match("^(?<name1>.*) \\([A-Z]{2}[A-Z0-9]{9}[0-9]\\)$")
                .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .match("^W.hrung (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("ISIN"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // @formatter:off
                // Nominal/Stück 102,185154 ST
                // @formatter:on
                .section("shares")
                .match("^Nominal\\/St.ck (?<shares>[\\.,\\d]+) ST$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Zahlungsdatum 28.06.2023
                // @formatter:on
                .section("date")
                .match("^Zahlungsdatum (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                // Ausmachender Betrag EUR 26,91
                // @formatter:on
                .section("currency", "amount")
                .match("^Ausmachender Betrag (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // @formatter:off
                // Dividendenart Ordentliche Dividende
                // @formatter:on
                .section("note").optional()
                .match("^Dividendenart (?<note>.*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Kapitalertragsteuer EUR -5,78
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^Kapitalertrag(s)?steuer (?<currency>[\\w]{3}) \\-(?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Solidaritätszuschlag EUR -0,31
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^Solidarit.tszuschlag (?<currency>[\\w]{3}) \\-(?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Kirchensteuer EUR 0,00
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^Kirchensteuer (?<currency>[\\w]{3}) \\-(?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }
}
