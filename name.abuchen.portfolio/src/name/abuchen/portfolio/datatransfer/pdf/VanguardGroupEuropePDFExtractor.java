package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

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
        DocumentType type = new DocumentType("Wertpapierabrechnung: (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Wertpapierabrechnung: (Kauf|Verkauf)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Wertpapierabrechnung: (?<type>(Kauf|Verkauf)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Wertpapierbezeichnung FDBL Vanguard LifeStrategy 60% Equity UCITS ETF
                        // (EUR) Distributing
                        // ISIN IE00BMVB5Q68
                        // Kurs (Stückpreis) EUR 25,0900
                        // @formatter:on
                        .section("name", "name1", "isin", "currency") //
                        .match("^Wertpapierbezeichnung (?<name>.*)") //
                        .match("^(?<name1>.*)$") //
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^Kurs \\(.*\\) (?<currency>[\\w]{3}) [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("ISIN"))
                                v.put("name", v.get("name") + " " + v.get("name1"));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Nominal / Stück 11,956954
                        // @formatter:on
                        .section("shares") //
                        .match("^Nominal \\/ St.ck (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Ausführungstag / -zeit 03.02.2023 um 11:11:17 Uhr
                        // @formatter:on
                        .section("date", "time") //
                        .match("^Ausf.hrungstag \\/ \\-zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Kurswert EUR 300,00
                                        // EUR 300,00
                                        // Abrechnungskonto DE01234567891234567890
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .find("Kurswert [\\w]{3} [\\.,\\d]+$") //
                                                        .match("^(?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .match("^Abrechnungskonto .*$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Abbuchungsbetrag EUR 300,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Abbuchungsbetrag (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // Ordernummer 3403175
                        // @formatter:on
                        .section("note") //
                        .match("^Ordernummer (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("Ord.-Nr.: " + trim(v.get("note"))))

                        // @formatter:off
                        // Referenznummer 12345678
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Referenznummer (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), "Ref.-Nr.: " + trim(v.get("note")), " | ")))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Bardividende");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Bardividende$", "^Steuerliche Informationen .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Wertpapierbezeichnung FDBL Vanguard LifeStrategy 60% Equity UCITS
                        // ETF (EUR) Distributing  (IE00BMVB5Q68)
                        // ISIN IE00BMVB5Q68
                        // Ausschüttung EUR 0,322898 pro Stück
                        // @formatter:on
                        .section("name", "name1", "isin", "currency") //
                        .match("^Wertpapierbezeichnung (?<name>.*)$") //
                        .match("^(?<name1>.*) \\([A-Z]{2}[A-Z0-9]{9}[0-9]\\)$") //
                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^(Original\\-)?Aussch.ttung (?<currency>[\\w]{3}) [\\.,\\d]+ .*$") //
                        .assign((t, v) -> {
                            if (!v.get("name1").startsWith("ISIN"))
                                v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Nominal/Stück 102,185154 ST
                        // @formatter:on
                        .section("shares") //
                        .match("^Nominal\\/St.ck (?<shares>[\\.,\\d]+) ST$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Der Abrechnungsbetrag wird mit Valuta 28.6.2023 über Ihr Konto 12345678  gebucht.
                        // @formatter:on
                        .section("date") //
                        .match("^Der Abrechnungsbetrag wird mit Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}) .*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Ausmachender Betrag EUR 26,91
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Ausmachender Betrag (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Original-Ausschüttung USD 0,267572 pro Stück
                        // Wechselkurs 1,066199
                        // Bruttobetrag EUR 0,64
                        // @formatter:on
                        .section("termCurrency", "exchangeRate", "baseCurrency", "gross").optional() //
                        .match("^(Original\\-)?Aussch.ttung (?<termCurrency>[\\w]{3}) [\\.,\\d]+ .*$") //
                        .match("^Wechselkurs (?<exchangeRate>[\\.,\\d]+)$") //
                        .match("^Bruttobetrag (?<baseCurrency>[\\w]{3}) (?<gross>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Dividendenart Ordentliche Dividende
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Dividendenart (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Kapitalertragsteuer EUR -5,78
                        // Kapitalertragsteuer EUR 0,28
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Kapitalertrags(s)?teuer (?<currency>[\\w]{3}) (\\-)?(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag EUR -0,31
                        // Solidaritätszuschlag EUR 0,01
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Solidarit.tszuschlag (?<currency>[\\w]{3}) (\\-)?(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer EUR 0,00
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^Kirchensteuer (?<currency>[\\w]{3}) (\\-)?(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }
}
