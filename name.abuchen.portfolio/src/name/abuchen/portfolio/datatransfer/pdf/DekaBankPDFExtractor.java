package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.replaceMultipleBlanks;
import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.DocumentContext;
import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class DekaBankPDFExtractor extends AbstractPDFExtractor
{
    private static final DateTimeFormatter DATEFORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public DekaBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("DekaBank");

        addBuySellTransaction();
        addSwapBuyTransaction();
        addSwapSellTransaction();
        addReinvestTransaction();
        addDividendeTransaction();
        addDeliveryInOutBoundTransaction();
        addDepotStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "DekaBank Deutsche Girozentrale";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(LASTSCHRIFTEINZUG|VERKAUF|KAUF|KAUF AUS ERTRAG)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(LASTSCHRIFTEINZUG|VERKAUF|KAUF|KAUF AUS ERTRAG) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(?<type>(LASTSCHRIFTEINZUG|VERKAUF|KAUF|KAUF AUS ERTRAG)).*$")
                .assign((t, v) -> {
                    if ("VERKAUF".equals(v.get("type")))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                .oneOf(
                                // @formatter:off
                                // Bezeichnung: Deka-UmweltInvest TF
                                // ISIN: DE000DK0ECT0 Unterdepot: 00 Auftragsnummer: 8103 1017
                                // =Abrechnungsbetrag EUR 4.000,00 EUR 4.000,00 EUR 494,260000 Anteilumsatz: 8,093
                                //
                                // Fondsbezeichnung: Deka-GeldmarktPlan TF
                                // ISIN: LU0268059614 Unterkonto: 00 Auftragsnummer: 8101 2521
                                // = Abrechnungsbetrag EUR 400,00 EUR 400,00 EUR 996,160000 Anteilumsatz: 0,402
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "currency")
                                        .match("^(Bezeichnung|Fondsbezeichnung): (?<name>.*)$")
                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                        .match("^.*(?<currency>[\\w]{3}) [\\.,\\d]+ Anteilumsatz: (\\-)?[\\.,\\d]+$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // Bezeichnung: Deka-EuropaPotential TF
                                // ISIN: DE0009786285
                                // Kurs
                                // EUR 82,110000
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "currency")
                                        .match("^(Bezeichnung|Fondsbezeichnung): (?<name>.*)$")
                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                        )

                // @formatter:off
                // =Abrechnungsbetrag EUR 4.000,00 EUR 4.000,00 EUR 187,770000 Anteilumsatz: 21,303
                // Anteilumsatz: -28,939
                // @formatter:on
                .section("shares")
                .match("^.*Anteilumsatz: (\\-)?(?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Verwahrart: GiroSammel Abrechnungstag: 18.05.2021
                // Abrechnungstag: 14.10.2021
                // Verwahrart: GiroSammel Abrechnungstag: 1  4.12.2010
                // @formatter:on
                .section("date")
                .match("^.*Abrechnungstag: (?<date>[\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+)$")
                .assign((t, v) -> t.setDate(asDate(stripBlanks(v.get("date")))))

                // @formatter:off
                // Einzugsbetrag EUR 4.000,00 Kurs/Kaufpreis Bestand alt: 11,291
                // Auszahlungsbetrag EUR 2.355,09
                // Abrechnungsbetrag EUR 1,00 EUR 1,00 EUR 96,576000 Anteilumsatz: 0,010
                //  Einzugsbetrag EUR 400,00 Preis Bestand alt: 1,612
                // @formatter:on
                .section("currency", "amount")
                .match("^.*(Einzugsbetrag|Auszahlungsbetrag|Abrechnungsbetrag) (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // @formatter:off
                // ISIN: LU0268059614 Unterkonto: 00 Auftragsnummer: 8101 2521
                // @formatter:on
                .section("note").optional()
                .match("^.* (?<note>Auftragsnummer: .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addSwapBuyTransaction()
    {
        DocumentType type = new DocumentType("TAUSCH\\/KAUF");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^TAUSCH\\/KAUF.*$", "^Bestand neu:.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                .oneOf(
                                // @formatter:off
                                // zu Gunsten
                                // Bezeichnung: Deka-Liquidität: EURO TF
                                // ISIN: DE0009771824
                                // Kurs/Kaufpreis
                                // EUR 65,000000
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "currency")
                                        .find("zu Gunsten")
                                        .match("^(Bezeichnung|Fondsbezeichnung): (?<name>.*)$")
                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // zu Gunsten
                                // Bezeichnung: Deka Deutsche Börse EUROGOV Germany Money Market UCITS ETF
                                // ISIN: DE000ETFL227 Unterdepot: 00 Auftragsnummer: 9704 9385
                                // Kurs/Kaufpreis Bestand alt: 0,213
                                // Abrechnungsbetrag EUR 41,33 EUR 41,33 EUR 69,204000 Anteilumsatz: 0,597
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "currency")
                                        .find("zu Gunsten")
                                        .match("^(Bezeichnung|Fondsbezeichnung): (?<name>.*)$")
                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                        .match("^Abrechnungsbetrag .* (?<currency>[\\w]{3}) [\\.,\\d]+ Anteilumsatz: [\\.,\\d]+$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                        )

                // @formatter:off
                // Anteilumsatz: 5,112
                // Abrechnungsbetrag EUR 29,42 EUR 29,42 EUR 69,204000 Anteilumsatz: 0,425
                // @formatter:on
                .section("shares")
                .match("^.*Anteilumsatz: (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Verwahrart: GiroSammel Abrechnungstag: 18.05.2021
                // Abrechnungstag: 06.06.2018
                // @formatter:on
                .section("date")
                .match("^.*Abrechnungstag: (?<date>[\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+)$")
                .assign((t, v) -> t.setDate(asDate(stripBlanks(v.get("date")))))

                // @formatter:off
                // Abrechnungsbetrag EUR 332,31 EUR 332,31
                // @formatter:on
                .section("currency", "amount")
                .match("^Abrechnungsbetrag (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // @formatter:off
                // Abrechnungsbetrag EUR 6,22 USD 6,88 USD 102,155229 Anteilumsatz: 0,067
                // Devisenkurs Bestand neu: 1,027
                // USD 1,106630
                // @formatter:on
                .section("currency", "gross", "fxCurrency", "fxGross", "termCurrency", "exchangeRate").optional()
                .match("^Abrechnungsbetrag (?<currency>[\\w]{3}) (?<gross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.,\\d]+).*$")
                .find("Devisenkurs.*$")
                .match("^(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(v.get("currency")));

                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                // @formatter:off
                // ISIN: LU0268059614 Unterkonto: 00 Auftragsnummer: 8101 2521
                // @formatter:on
                .section("note").optional()
                .match("^.* (?<note>Auftragsnummer: .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addSwapSellTransaction()
    {
        DocumentType type = new DocumentType("TAUSCH\\/VERKAUF");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });

        Block firstRelevantLine = new Block("^TAUSCH\\/VERKAUF.*$", "^Bestand neu:.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                .oneOf(
                                // @formatter:off
                                // zu Lasten
                                // Bezeichnung: Deka-EuropaPotential TF
                                // ISIN: DE0009786285
                                // Kurs
                                // EUR 128,610000
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "currency")
                                        .find("zu Lasten")
                                        .match("^(Bezeichnung|Fondsbezeichnung): (?<name>.*)$")
                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                        .match("^(?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // zu Lasten
                                // Bezeichnung: Deka-Industrie 4.0 CF
                                // ISIN: LU1508359509 Unterdepot: 00 Auftragsnummer: 8108 0595
                                // Kurs Bestand alt: 0,277
                                // Abrechnungsbetrag EUR 14,74 EUR 14,74 EUR 177,620000 Anteilumsatz: -0,083
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "currency")
                                        .find("zu Lasten")
                                        .match("^(Bezeichnung|Fondsbezeichnung): (?<name>.*)$")
                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                        .match("^Abrechnungsbetrag .* (?<currency>[\\w]{3}) [\\.,\\d]+ Anteilumsatz: (\\-)?[\\.,\\d]+$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                        )

                // @formatter:off
                // Anteilumsatz: -2,598
                // Abrechnungsbetrag EUR 14,74 EUR 14,74 EUR 177,620000 Anteilumsatz: -0,083
                // @formatter:on
                .section("shares")
                .match("^.*Anteilumsatz: \\-(?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Verwahrart: GiroSammel Abrechnungstag: 18.05.2021
                // Abrechnungstag: 06.06.2018
                // @formatter:on
                .section("date")
                .match("^.*Abrechnungstag: (?<date>[\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+)$")
                .assign((t, v) -> t.setDate(asDate(stripBlanks(v.get("date")))))

                // @formatter:off
                // Auszahlungsbetrag EUR 206,74
                // Abrechnungsbetrag EUR 14,74 EUR 14,74 EUR 177,620000 Anteilumsatz: -0,083
                // @formatter:on
                .section("currency", "amount")
                .match("^(Auszahlungsbetrag|Abrechnungsbetrag) (?<currency>[\\w]{3}) (?<amount>[.,\\d]+).*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // @formatter:off
                // ISIN: LU0268059614 Unterkonto: 00 Auftragsnummer: 8101 2521
                // @formatter:on
                .section("note").optional()
                .match("^.* (?<note>Auftragsnummer: .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addReinvestTransaction()
    {
        DocumentType type = new DocumentType("(ERTRAGSAUSSCH.TTUNG \\/ KAUF AUS ERTRAG|ERTRAGSABRECHNUNG|ERTRAGSAUSSCH.TTUNG)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(ERTRAGSAUSSCH.TTUNG \\/ KAUF AUS ERTRAG|ERTRAGSABRECHNUNG|ERTRAGSAUSSCH.TTUNG)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                .oneOf(
                                // @formatter:off
                                // Ausschüttung (pro Anteil EUR 2,3300000): EUR 78,81
                                // Bezeichnung: Deka-EuropaPotential TF
                                // ISIN: DE0009786285 Unterdepot: 00 Auftragsnummer: 9457 2850
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "name", "isin")
                                        .match("^Aussch.ttung .* (?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .match("^(Bezeichnung|Fondsbezeichnung): (?<name>.*)$")
                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // Fondsbezeichnung: AriDeka CF
                                // ISIN: DE0008474511 Unterkonto: 00 Auftragsnummer: 9387 9103
                                // +Verrechnete Steuern EUR 1,43 EUR 29,15 EUR 33,420000 Anteilumsatz: 0,872
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "currency")
                                        .match("^(Bezeichnung|Fondsbezeichnung): (?<name>.*)$")
                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                        .match("^.*(?<currency>[\\w]{3}) [\\.,\\d]+ Anteilumsatz: [\\.,\\d]+$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                        )

                // @formatter:off
                // - Verrechnete Steuern EUR 15,34 EUR 63,47 EUR 138,830000 Anteilumsatz: 0,457
                // @formatter:on
                .section("shares")
                .match("^.* Anteilumsatz: (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Verwahrart: GiroSammel Abrechnungstag: 24.02.2023
                // @formatter:on
                .section("date")
                .match("^.*Abrechnungstag: (?<date>[\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+)$")
                .assign((t, v) -> t.setDate(asDate(stripBlanks(v.get("date")))))

                // @formatter:off
                // =Wiederanlagebetrag EUR 63,47 Bestand neu: 34,280
                // @formatter:on
                .section("currency", "amount").optional()
                .match("^.*Wiederanlagebetrag (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // @formatter:off
                // =Wiederanlagebetrag EUR 63,47 Bestand neu: 34,280
                // @formatter:on
                .section("note").optional()
                .match("^.* (?<note>Auftragsnummer: .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                // @formatter:off
                // =Wiederanlagebetrag EUR 63,47 Bestand neu: 34,280
                // @formatter:on
                .section("note").optional()
                .match("^.*(?<note>Wiederanlage).* [\\w]{3} [\\.,\\d]+.*$")
                .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                .wrap(t -> {
                    if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() != 0)
                        return new BuySellEntryItem(t);
                    return null;
                });
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(ERTRAGSAUSSCH.TTUNG|ERTRAGSABRECHNUNG)");
        this.addDocumentTyp(type);

        Block block = new Block("^(ERTRAGSAUSSCH.TTUNG|ERTRAGSABRECHNUNG).*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                .oneOf(
                                // @formatter:off
                                // Ausschüttung (pro Anteil EUR 0,2292000): EUR 0,14
                                // Bezeichnung: iShares J.P. Morgan USD EM Bond EUR Hedged UCITS ETF (Dist)
                                // ISIN: IE00B9M6RS56 Unterdepot: 00 Auftragsnummer: 9302 2538
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "name", "isin")
                                        .match("^Aussch.ttung .* (?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .match("^(Bezeichnung|Fondsbezeichnung): (?<name>.*)$")
                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // Fondsbezeichnung: AriDeka CF
                                // ISIN: DE0008474511 Unterkonto: 00 Auftragsnummer: 9387 9103
                                // +Verrechnete Steuern EUR 1,43 EUR 29,15 EUR 33,420000 Anteilumsatz: 0,872
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "currency")
                                        .match("^(Bezeichnung|Fondsbezeichnung): (?<name>.*)$")
                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                        .match("^.*(?<currency>[\\w]{3}) [\\.,\\d]+ Anteilumsatz: [\\.,\\d]+$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                        )

                .oneOf(
                                // @formatter:off
                                // Anteilbestand am Ertragstermin: 0,619
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^Anteilbestand am Ertragstermin: (?<shares>[\\.,\\d]+)$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // @formatter:off
                                //  Ausschüttung EUR 27,72 Preis Bestand alt: 29,811
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^.* Bestand alt: (?<shares>[\\.,\\d]+)$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                // @formatter:off
                // Verwahrart: GiroSammel Abrechnungstag: 31.03.2022
                // @formatter:on
                .section("date")
                .match("^.*Abrechnungstag: (?<date>[\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+)$")
                .assign((t, v) -> t.setDateTime(asDate(stripBlanks(v.get("date")))))

                // @formatter:off
                // Ausschüttung EUR 0,14 Kurs Bestand alt: 0,739
                //  Ausschüttung EUR 27,72 Preis Bestand alt: 29,811
                // @formatter:on
                .section("currency", "amount")
                .match("^.*Aussch.ttung (?<currency>[\\w]{3}) (?<amount>[.,\\d]+) .*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // @formatter:off
                // Ausschüttung EUR 78,81 Kurs/Kaufpreis Bestand alt: 33,823
                // - Verrechnete Steuern EUR 15,34 EUR 63,47 EUR 138,830000 Anteilumsatz: 0,457
                // =Wiederanlagebetrag EUR 63,47 Bestand neu: 34,280
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^\\-.*Verrechnete Steuern (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                    t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                })

                // @formatter:off
                // ISIN: DE0008474511 Unterkonto: 00 Auftragsnummer: 9387 9103
                // @formatter:on
                .section("note").optional()
                .match("^.* (?<note>Auftragsnummer: .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                // @formatter:off
                // If the taxes are positive, it is a tax refund.
                //
                // +Verrechnete Steuern EUR 1,43 EUR 29,15 EUR 33,420000 Anteilumsatz: 0,872
                // @formatter:on
                .section("noTax").optional()
                .match("^(?<noTax>\\+).*Verrechnete Steuern [\\w]{3} [\\.,\\d]+.*$")
                .assign((t, v) -> type.getCurrentContext().putBoolean("noTax", true))

                .wrap(t -> {
                    // If we have multiple entries in the document, then
                    // the "noTax" flag must be removed.
                    type.getCurrentContext().remove("noTax");

                    return new TransactionItem(t);
                });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addTaxReturnBlock(type);

        block.set(pdfTransaction);
    }

    private void addDeliveryInOutBoundTransaction()
    {
        DocumentType type = new DocumentType("AUSLIEFERUNG");
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
            return entry;
        });

        Block firstRelevantLine = new Block("^AUSLIEFERUNG.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // @formatter:off
                // Bezeichnung: DekaLux-PharmaTech TF
                // ISIN: LU0348413815 Unterdepot: 00 Auftragsnummer: 8101 8357
                // @formatter:on
                .section("name", "isin")
                .match("^(Bezeichnung|Fondsbezeichnung): (?<name>.*)$")
                .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));

                    v.getTransactionContext().put(FAILURE, Messages.MsgErrorTransactionTypeNotSupported);
                    t.setCurrencyCode(CurrencyUnit.EUR);
                    t.setAmount(0L);
                })

                // @formatter:off
                // Anteilumsatz: -16,000
                // @formatter:on
                .section("shares")
                .match("^.*Anteilumsatz: (\\-)?(?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Verwahrart: GiroSammel Abrechnungstag: 18.03.2020
                // @formatter:on
                .section("date")
                .match("^.*Abrechnungstag: (?<date>[\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+)$")
                .assign((t, v) -> t.setDateTime(asDate(stripBlanks(v.get("date")))))

                // @formatter:off
                // ISIN: LU0348413815 Unterdepot: 00 Auftragsnummer: 8101 8357
                // @formatter:on
                .section("note").optional()
                .match("^.* (?<note>Auftragsnummer: .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap((t, ctx) -> {
                    TransactionItem item = new TransactionItem(t);

                    if (ctx.getString(FAILURE) != null)
                        item.setFailureMessage(ctx.getString(FAILURE));

                    return item;
                });
    }

    public void addDepotStatementTransaction()
    {
        final DocumentType type = new DocumentType("(Quartalsbericht|Umsatz\\-Jahres.bersicht)", (context, lines) -> {

            Pattern pSecurity = Pattern.compile("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])( [\\d]+)? ([\\s]+)?[\\d]{2}( \\-)?( .*: )?(?<name>.*) ([\\s]+)?[\\.,\\d]+( [\\.,\\d]+ [\\w]{3})? ([\\-|\\+])?[\\.,\\d]+$");
            Pattern pISIN = Pattern.compile("^((?<name>.*)\\/ )?ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$");
            Pattern pDate = Pattern.compile("^((Jahresdepotauszug|Depot.bersicht|Depot\\-Auszug) )?(per|zum) (?<documentDate>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$");
            Pattern pShares = Pattern.compile("^.* ([\\.,\\d]+)?(?<addShare>(?<type>[\\-\\+\\s]+)[\\.,\\d]+) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$");
            Pattern pSharesTotal = Pattern.compile("^Bestand am [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}(?<name>.*) ([\\s]+)?([\\.,\\d]+) ([\\s\\*]+)?([\\.,\\d]+) ([\\s]+)?(?<sharesTotal>[\\.,\\d]+).*$");
            Pattern pSecurityName = Pattern.compile("^(Zulagenzahlung([\\s]+)?([\\d]{4})?"
                            + "|.*Ertrag|.*Tausch|.*zahlung|.*preis|.*buchung|.*verwendung|.*Verwendung|.*Aufl.sung|.*einzug|.*erstattung|.*Thesaurierung|.*forderung)?"
                            + "(?<name>.*) ([\\s])?([\\.,\\d]+) ([\\s])?([\\.,\\d]+) ([\\s])?([\\.,\\d]+)?(?<addShare>(?<type>[\\-\\+\\s]+)[\\.,\\d]+) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$");

            Pattern pDepotFeeDate = Pattern.compile("^Depotpreis(?! (inkl|incl)\\.) [\\.,\\d]+ [\\.,\\d]+[\\-\\+\\s]+[\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<depotFeeDate>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$");
            Pattern pContractFeeDate = Pattern.compile("^Vertragsgeb.hr .*[\\.,\\d]+ [\\.,\\d]+ [\\-\\+\\s]+[\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<contractFeeDate>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$");

            for (String line : lines)
            {
                Matcher mDate = pDate.matcher(line);
                if (mDate.matches())
                    context.put("documentDate", mDate.group("documentDate"));

                Matcher mDepotFeeDate = pDepotFeeDate.matcher(line);
                if (mDepotFeeDate.matches())
                    context.put("depotFeeDate", mDepotFeeDate.group("depotFeeDate"));

                Matcher mContractFeeDate = pContractFeeDate.matcher(line);
                if (mContractFeeDate.matches())
                    context.put("contractFeeDate", mContractFeeDate.group("contractFeeDate"));
            }

            // Create a helper to store the list of security items found in the document
            SecurityListHelper securityListHelper = new SecurityListHelper();
            context.putType(securityListHelper);

            // Extract security information using pSecurity pattern
            List<SecurityItem> securityItems = new ArrayList<>();

            for (String line : lines)
            {
                Matcher mSecurity = pSecurity.matcher(line);
                if (mSecurity.matches())
                {
                    SecurityItem securityItem = new SecurityItem();
                    securityItem.isin = mSecurity.group("isin");
                    securityItem.name = trim(mSecurity.group("name")) != null ? trim(mSecurity.group("name")) : "";
                    securityItems.add(securityItem);
                }
            }

            // Iterate through lines again to find ISIN patterns and update security items
            SecurityItem currentSecurityItem = null;
            for (int i = 0; i < lines.length; i++)
            {
                Matcher mISIN = pISIN.matcher(lines[i]);
                if (mISIN.matches())
                {
                    // Find the security item matching the ISIN and update its line numbers
                    for (SecurityItem securityItem : securityItems)
                    {
                        if (securityItem.isin.equals(mISIN.group("isin")) && securityItem.lineNoStart == 0)
                        {
                            securityItem.lineNoStart = i + 1;

                            if (currentSecurityItem != null)
                                currentSecurityItem.lineNoEnd = i;

                            currentSecurityItem = securityItem;
                            break;
                        }
                        else if (currentSecurityItem != null && currentSecurityItem == securityItem)
                        {
                            currentSecurityItem.lineNoEnd = i;
                        }
                    }

                    // If no matching SecurityItem is found, create a new one
                    if (currentSecurityItem == null || !currentSecurityItem.isin.equals(mISIN.group("isin")))
                    {
                        currentSecurityItem = new SecurityItem();
                        currentSecurityItem.isin = mISIN.group("isin");
                        currentSecurityItem.lineNoStart = i + 1;
                        currentSecurityItem.name = trim(mISIN.group("name")) != null ? trim(mISIN.group("name")) : "";
                        securityItems.add(currentSecurityItem);
                    }
                }
            }

            // If currentSecurityItem is not null and its lineNoEnd has not been updated, set it to the last line
            if (currentSecurityItem != null && currentSecurityItem.lineNoEnd == 0)
                currentSecurityItem.lineNoEnd = lines.length - 1;

            // Add security items to helper, ignoring those without names
            for (SecurityItem securityItem : securityItems)
            {
                if (!securityItem.name.isEmpty())
                    securityListHelper.items.add(securityItem);
            }

            // Create a helper to save the list of shares by securities transaction found in the document
            SharesListHelper sharesListHelper = new SharesListHelper();
            context.putType(sharesListHelper);

            // Extract shareItems information using pSharesTotal pattern
            List<ShareItem> shareItems = new ArrayList<>();

            for (SecurityItem securityItem : securityListHelper.items)
            {
                long shares = 0;

                // Searches for the regex pattern of sharesTotal in the current line
                for (int i = securityItem.lineNoEnd; i > securityItem.lineNoStart; i--)
                {
                    Matcher mSharesTotal = pSharesTotal.matcher(lines[i]);
                    if (mSharesTotal.matches())
                    {
                        shares = asShares(mSharesTotal.group("sharesTotal"));
                        break;
                    }
                }

                // Iterates through the lines within the current SecurityItem
                for (int i = securityItem.lineNoEnd - 1; i > securityItem.lineNoStart; i--)
                {
                    Matcher mShares = pShares.matcher(lines[i]);
                    if (mShares.matches())
                    {
                        boolean isPositive = !"-".equals(trim(mShares.group("type")));

                        ShareItem shareItem = new ShareItem();
                        shareItem.isin = securityItem.isin;
                        shareItem.lineNo = i;
                        shareItem.date = LocalDate.parse(mShares.group("date"), DATEFORMAT);
                        shareItem.addShare = asShares(trim(mShares.group("addShare").replace("+", "").replace("-", "")));

                        if (shares == 0)
                        {
                            shares += shareItem.addShare;
                            shareItem.shares = shares;
                        }
                        else
                        {
                            shares += isPositive ? -shareItem.addShare : shareItem.addShare;
                            shareItem.shares = shares;
                        }

                        shareItems.add(shareItem);
                    }
                }
            }

            for (int i = lines.length - 1 ; i >= 0; i--)
            {
                long shares = 0;

                Matcher mSharesTotal = pSharesTotal.matcher(lines[i]);
                if (mSharesTotal.matches())
                {
                    String trimmedName = trim(mSharesTotal.group("name"));

                    for (SecurityItem securityItem : securityItems)
                    {
                        if (securityItem.name.contains(trimmedName) && !trimmedName.isEmpty())
                        {
                            shares = asShares(mSharesTotal.group("sharesTotal"));

                            for (int ii = i + 1 ; ii >= 0; ii--)
                            {
                                Matcher mISIN = pISIN.matcher(lines[ii]);
                                if (mISIN.matches())
                                    break;

                                Matcher mSecurityName = pSecurityName.matcher(lines[ii]);
                                if (mSecurityName.matches() && securityItem.name.contains(trim(mSecurityName.group("name"))))
                                {
                                    boolean isPositive = !"-".equals(trim(mSecurityName.group("type")));

                                    ShareItem shareItem = new ShareItem();
                                    shareItem.isin = securityItem.isin;
                                    shareItem.lineNo = ii;
                                    shareItem.date = LocalDate.parse(mSecurityName.group("date"), DATEFORMAT);
                                    shareItem.addShare = asShares(trim(mSecurityName.group("addShare").replace("+", "").replace("-", "")));

                                    if (shares == 0)
                                    {
                                        shares += shareItem.addShare;
                                        shareItem.shares = shares;
                                    }
                                    else
                                    {
                                        shares += isPositive ? -shareItem.addShare : shareItem.addShare;
                                        shareItem.shares = shares;
                                    }

                                    shareItems.add(shareItem);
                                }
                            }
                            break;
                        }
                    }
                }
            }

            // Add shares of the securities transaction to the helper
            for (ShareItem shareItem : shareItems)
            {
                if (!shareItem.isin.isEmpty())
                    sharesListHelper.items.add(shareItem);
            }
        });
        this.addDocumentTyp(type);

        Block buySellBlock = new Block("^(Lastschrifteinzug"
                        + "|Storno Lastschrifteinzug"
                        + "|Verkauf( \\/ Tausch)?(?! aus Ertrag)"
                        + "|Zulagenr.ckforderung"
                        + "|Kauf( aus Ertrag| \\/ Tausch)?(?! (aus )?(Zulagenzahlung|Steuererstattung))"
                        + "|Thesaurierung \\/ Kauf aus Ertrag"
                        + "|Aussch.ttung \\/ Kauf aus Ertrag"
                        + "|Abrechnungsbetrag Aussch.ttung"
                        + "|Abrechnungsbetrag Thesaurierung"
                        + "|Depotpreis(?! (inkl|incl)\\.)"
                        + "|Vertragsgeb.hr(?! in [\\d]{4})"
                        + "|Sch.dliche Verwendung"
                        + "|Entgelt Aufl.sung"
                        + "|Vertragspreis(?! (\\(zu Lasten (Girokonto|Vertrag)\\)|gesamt|[\\-\\+\\.,\\d]+))"
                        + ")"
                        + " .*$");
        type.addBlock(buySellBlock);
        buySellBlock.set(new Transaction<BuySellEntry>().subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        })

            .oneOf(
                            // @formatter:off
                            // Lastschrifteinzug 250,00 198,660000 +1,258 01.04.2021 01.04.2021
                            // Lastschrifteinzug 1.000,00 59,320000 + 16,858 28.06.2005 28.06.2005
                            // Storno Lastschrifteinzug Deka-DividendenStrategi 805,00 159,850000 -5,036 21.12.2020 01.12.2020
                            // Verkauf 2.039,96 102,810000 -19,842 11.05.2021 11.05.2021
                            // Kauf 36,00 216,100000 +0,167 18.07.2022 18.07.2022
                            // Kauf 34,00 55,240000 + 0,615 28.02.2005 28.02.2005
                            // Kauf aus Ertrag 0,28 63,520000 + 0,004 03.01.2005 03.01.2005
                            // Abrechnungsbetrag Ausschüttung 54,72 438,170000  +0,125 20.11.2012 20.11.2012
                            // Depotpreis 10,00 69,740000 - 0,143 16.12.2005 16.12.2005
                            // Vertragsgebühr 5,00 49,040000 - 0,102 15.12.2006 15.12.2006
                            // Thesaurierung / Kauf aus Ertrag 100 0,00 72,270000 +0,000 12.01.2018 29.12.2017
                            // Zulagenrückforderung Deka-DividendenStrategi 475,00 176,250000 -2,695 02.01.2023 02.01.2023
                            // @formatter:on
                            section -> section
                                    .attributes("note", "amount", "amountPerShare", "type", "shares", "date")
                                    .match("^(?<note>(Lastschrifteinzug"
                                                    + "|Storno Lastschrifteinzug"
                                                    + "|Verkauf( \\/ Tausch)?(?! aus Ertrag)"
                                                    + "|Zulagenr.ckforderung"
                                                    + "|Kauf( aus Ertrag| \\/ Tausch)?(?! aus Steuererstattung)"
                                                    + "|Thesaurierung \\/ Kauf aus Ertrag"
                                                    + "|Aussch.ttung \\/ Kauf aus Ertrag"
                                                    + "|Abrechnungsbetrag Aussch.ttung"
                                                    + "|Abrechnungsbetrag Thesaurierung"
                                                    + "|Depotpreis(?! (inkl|incl)\\.)"
                                                    + "|Vertragsgeb.hr(?! in [\\d]{4})"
                                                    + "|Sch.dliche Verwendung"
                                                    + "|Entgelt Aufl.sung"
                                                    + "|Vertragspreis(?! (\\(zu Lasten (Girokonto|Vertrag)\\)|gesamt|[\\-\\+\\.,\\d]+)))) "
                                                    + "(?<amount>[\\.,\\d]+) "
                                                    + "(?<amountPerShare>[\\.,\\d]+)"
                                                    + "(?<type>[\\-\\+\\s]+)"
                                                    + "(?<shares>[\\.,\\d]+) "
                                                    + "[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                    .assign((t, v) -> {
                                        DocumentContext context = type.getCurrentContext();

                                        // Is type --> "-" change from BUY to SELL
                                        if ("-".equals(trim(v.get("type"))))
                                            t.setType(PortfolioTransaction.Type.SELL);

                                        if ("Storno Lastschrifteinzug".equals(v.get("type")))
                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported);

                                        SecurityListHelper securityListHelper = context.getType(SecurityListHelper.class).orElseGet(SecurityListHelper::new);
                                        Optional<SecurityItem> securityItem = securityListHelper.findItemByLineNoStart(v.getStartLineNumber());

                                        if (securityItem.isPresent())
                                        {
                                            v.put("name", securityItem.get().name);
                                            v.put("isin", securityItem.get().isin);
                                        }
                                        else
                                        {
                                            Optional<SecurityItem> securityItemByName = securityListHelper.findItemByName(trim(v.get("name")));
                                            if (securityItemByName.isPresent())
                                            {
                                                v.put("name", securityItemByName.get().name);
                                                v.put("isin", securityItemByName.get().isin);
                                            }
                                        }

                                        t.setSecurity(getOrCreateSecurity(v));
                                        t.setDate(asDate(v.get("date")));
                                        t.setShares(asShares(v.get("shares")));
                                        t.setAmount(asAmount(v.get("amount")));
                                        t.setCurrencyCode(CurrencyUnit.EUR);

                                        // @formatter:off
                                        // Deka indicates only up to 3 digits after the decimal point.
                                        // If the purchase or sale of shares is smaller, then we calculate the shares.
                                        //
                                        // Verkauf / Tausch Deka-Renten konservativ 0,02 48,370000 +0,000 09.06.2020 09.06.2020
                                        // Kauf / Tausch Deka-RentenStrategie 0,02 91,710000 +0,000 09.06.2020 09.06.2020
                                        //
                                        // @formatter:on
                                        if (t.getPortfolioTransaction().getShares() == 0 && t.getPortfolioTransaction().getAmount() != 0)
                                        {
                                            if (trim(v.get("note")).startsWith("Verkauf"))
                                                t.setType(PortfolioTransaction.Type.SELL);

                                            BigDecimal amountPerShare = BigDecimal.valueOf(asAmount(v.get("amountPerShare")));
                                            BigDecimal amount = BigDecimal.valueOf(asAmount(v.get("amount")));

                                            BigDecimal shares = amount.divide(amountPerShare, Values.Share.precision(), RoundingMode.HALF_UP);
                                            t.setShares(shares.movePointRight(Values.Share.precision()).longValue());
                                        }

                                        // Formatting some notes
                                        if ("Depotpreis".equals(trim(v.get("note"))))
                                            t.setNote(trim(v.get("note")) + " " + t.getPortfolioTransaction().getDateTime().getYear());

                                        if ("Vertragsgebühr".equals(trim(v.get("note"))))
                                            t.setNote(trim(v.get("note")) + " " + t.getPortfolioTransaction().getDateTime().getYear());

                                        if ("Schädliche Verwendung".equals(trim(v.get("note"))))
                                            t.setNote(trim(v.get("note")) + " " + t.getPortfolioTransaction().getDateTime().getYear());

                                        if ("Entgelt Auflösung".equals(trim(v.get("note"))))
                                            t.setNote(trim(v.get("note")) + " " + t.getPortfolioTransaction().getDateTime().getYear());

                                        if ("Vertragspreis".equals(trim(v.get("note"))))
                                            t.setNote(trim(v.get("note")) + " " + t.getPortfolioTransaction().getDateTime().getYear());
                                    })
                            ,
                            // @formatter:off
                            // Lastschrifteinzug Deka-BR 100 80,00 103,190000 +0,775 08.06.2022 08.06.2022
                            // Lastschrifteinzug Deka-BR 100 80,00 39,880000  +2,006 08.06.2012 08.06.2012
                            // Storno Lastschrifteinzug Deka-DividendenStrategi 805,00 159,850000 -5,036 21.12.2020 01.12.2020
                            // Abrechnungsbetrag Thesaurierung Deka-BR 100 4,39 39,790000  +0,110 02.07.2012 02.07.2012
                            // Thesaurierung / Kauf aus Ertrag Deka-BR 100 0,00 72,270000 +0,000 12.01.2018 29.12.2017
                            // Schädliche Verwendung Deka-BR 100 23.190,32 84,050000 -275,911 24.11.2020 24.11.2020
                            // Entgelt Auflösung Deka-BR 100 48,74 84,050000 -0,580 24.11.2020 24.11.2020
                            // Verkauf / Tausch Deka-Renten konservativ 340,80 47,890000 -7,116 16.04.2020 16.04.2020
                            // Zulagenrückforderung Deka-DividendenStrategi 475,00 176,250000 -2,695 02.01.2023 02.01.2023
                            //
                            // Vertragspreis Deka-DividendenStrategi 1,95 148,480000 -0,013 24.08.2020 24.08.2020
                            // e CF (A
                            // @formatter:on
                            section -> section
                                    .attributes("note", "name", "amount", "amountPerShare", "type", "shares", "date")
                                    .match("^(?<note>(Lastschrifteinzug"
                                                    + "|Storno Lastschrifteinzug"
                                                    + "|Verkauf( \\/ Tausch)?(?! aus Ertrag)"
                                                    + "|Zulagenr.ckforderung"
                                                    + "|Kauf( aus Ertrag| \\/ Tausch)?(?! aus Steuererstattung)"
                                                    + "|Thesaurierung \\/ Kauf aus Ertrag"
                                                    + "|Aussch.ttung \\/ Kauf aus Ertrag"
                                                    + "|Abrechnungsbetrag Aussch.ttung"
                                                    + "|Abrechnungsbetrag Thesaurierung"
                                                    + "|Depotpreis(?! (inkl|incl)\\.)"
                                                    + "|Vertragsgeb.hr(?! in [\\d]{4})"
                                                    + "|Sch.dliche Verwendung"
                                                    + "|Entgelt Aufl.sung"
                                                    + "|Vertragspreis(?! (\\(zu Lasten (Girokonto|Vertrag)\\)|gesamt|[\\-\\+\\.,\\d]+)))) "
                                                    + "(?<name>.*) "
                                                    + "(?<amount>[\\.,\\d]+) "
                                                    + "(?<amountPerShare>[\\.,\\d]+)"
                                                    + "(?<type>[\\-\\+\\s]+)"
                                                    + "(?<shares>[\\.,\\d]+) "
                                                    + "[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                    .assign((t, v) -> {
                                        DocumentContext context = type.getCurrentContext();

                                        // Is type --> "-" change from BUY to SELL
                                        if ("-".equals(trim(v.get("type"))))
                                            t.setType(PortfolioTransaction.Type.SELL);

                                        if ("Storno Lastschrifteinzug".equals(v.get("note")))
                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported);

                                        SecurityListHelper securityListHelper = context.getType(SecurityListHelper.class).orElseGet(SecurityListHelper::new);
                                        Optional<SecurityItem> securityItem = securityListHelper.findItemByLineNoStart(v.getStartLineNumber());

                                        if (securityItem.isPresent())
                                        {
                                            v.put("name", securityItem.get().name);
                                            v.put("isin", securityItem.get().isin);
                                        }
                                        else
                                        {
                                            Optional<SecurityItem> securityItemByName = securityListHelper.findItemByName(trim(v.get("name")));
                                            if (securityItemByName.isPresent())
                                            {
                                                v.put("name", securityItemByName.get().name);
                                                v.put("isin", securityItemByName.get().isin);
                                            }
                                        }

                                        t.setSecurity(getOrCreateSecurity(v));
                                        t.setDate(asDate(v.get("date")));
                                        t.setShares(asShares(v.get("shares")));
                                        t.setAmount(asAmount(v.get("amount")));
                                        t.setCurrencyCode(CurrencyUnit.EUR);

                                        // @formatter:off
                                        // Deka indicates only up to 3 digits after the decimal point.
                                        // If the purchase or sale of shares is smaller, then we calculate the shares.
                                        //
                                        // Verkauf / Tausch Deka-Renten konservativ 0,02 48,370000 +0,000 09.06.2020 09.06.2020
                                        // Kauf / Tausch Deka-RentenStrategie 0,02 91,710000 +0,000 09.06.2020 09.06.2020
                                        //
                                        // @formatter:on
                                        if (t.getPortfolioTransaction().getShares() == 0 && t.getPortfolioTransaction().getAmount() != 0)
                                        {
                                            if (trim(v.get("note")).startsWith("Verkauf"))
                                                t.setType(PortfolioTransaction.Type.SELL);

                                            BigDecimal amountPerShare = BigDecimal.valueOf(asAmount(v.get("amountPerShare")));
                                            BigDecimal amount = BigDecimal.valueOf(asAmount(v.get("amount")));

                                            BigDecimal shares = amount.divide(amountPerShare, Values.Share.precision(), RoundingMode.HALF_UP);
                                            t.setShares(shares.movePointRight(Values.Share.precision()).longValue());
                                        }

                                        // Formatting some notes
                                        if ("Depotpreis".equals(trim(v.get("note"))))
                                            t.setNote(trim(v.get("note")) + " " + t.getPortfolioTransaction().getDateTime().getYear());

                                        if ("Vertragsgebühr".equals(trim(v.get("note"))))
                                            t.setNote(trim(v.get("note")) + " " + t.getPortfolioTransaction().getDateTime().getYear());

                                        if ("Schädliche Verwendung".equals(trim(v.get("note"))))
                                            t.setNote(trim(v.get("note")));

                                        if ("Entgelt Auflösung".equals(trim(v.get("note"))))
                                            t.setNote(trim(v.get("note")));

                                        if ("Vertragspreis".equals(trim(v.get("note"))))
                                            t.setNote(trim(v.get("note")) + " " + t.getPortfolioTransaction().getDateTime().getYear());
                                    })
                    )

            .wrap((t, ctx) -> {
                BuySellEntryItem item = new BuySellEntryItem(t);

                if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() == 0)
                    item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                if (ctx.getString(FAILURE) != null)
                    item.setFailureMessage(ctx.getString(FAILURE));

                return item;
            }));

        Block deliveryInOutbondblock = new Block("^(Einbuchung"
                        + "|Ausbuchung|Auslieferung"
                        + "|Zulagenzahlung"
                        + "|Zulagenr.ckzahlung"
                        + "|Korrekturbuchung"
                        + "|Steuerr.ckzahlung"
                        + "|Steuererstattung"
                        + "|Kauf (aus )?(Zulagenzahlung|Steuererstattung))"
                        + " .*$");
        type.addBlock(deliveryInOutbondblock);
        deliveryInOutbondblock.set(new Transaction<PortfolioTransaction>().subject(() -> {
            PortfolioTransaction transaction = new PortfolioTransaction();
            transaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            return transaction;
        })
                .oneOf(
                            // @formatter:off
                            // Kauf aus Steuererstattung 12,29 47,355000 +0,260 19.08.2020 19.08.2020
                            // Kauf Zulagenzahlung 354,00 32,950000  +10,744 17.11.2009 17.11.2009
                            // @formatter:on
                            section -> section
                                    .attributes("note", "amount", "type", "shares", "date")
                                    .match("^(?<note>(Zulagenzahlung .* [\\d]{4}"
                                                    + "|Kauf Zulagenzahlung"
                                                    + "|Zulagenr.ckzahlung"
                                                    + "|Korrekturbuchung"
                                                    + "|Steuererstattung"
                                                    + "|Steuerr.ckzahlung"
                                                    + "|Kauf (aus )?(Zulagenzahlung|Steuererstattung))) "
                                                    + "(?<amount>[\\.,\\d]+) "
                                                    + "[\\.,\\d]+"
                                                    + "(?<type>[\\-\\+\\s]+)"
                                                    + "(?<shares>[\\.,\\d]+) "
                                                    + "[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                    .assign((t, v) -> {
                                        DocumentContext context = type.getCurrentContext();

                                        // Is type --> "-" change from BUY to SELL
                                        if ("-".equals(trim(v.get("type"))))
                                            t.setType(PortfolioTransaction.Type.SELL);

                                        SecurityListHelper securityListHelper = context.getType(SecurityListHelper.class).orElseGet(SecurityListHelper::new);
                                        Optional<SecurityItem> securityItem = securityListHelper.findItemByLineNoStart(v.getStartLineNumber());

                                        if (securityItem.isPresent())
                                        {
                                            v.put("name", securityItem.get().name);
                                            v.put("isin", securityItem.get().isin);
                                        }
                                        else
                                        {
                                            Optional<SecurityItem> securityItemByName = securityListHelper.findItemByName(trim(v.get("name")));
                                            if (securityItemByName.isPresent())
                                            {
                                                v.put("name", securityItemByName.get().name);
                                                v.put("isin", securityItemByName.get().isin);
                                            }
                                        }

                                        t.setSecurity(getOrCreateSecurity(v));
                                        t.setDateTime(asDate(v.get("date")));
                                        t.setShares(asShares(v.get("shares")));
                                        t.setAmount(asAmount(v.get("amount")));
                                        t.setCurrencyCode(CurrencyUnit.EUR);
                                        t.setNote(trim(replaceMultipleBlanks(v.get("note"))));
                                    })
                            ,
                            // @formatter:off
                            // Zulagenzahlung       2021 Deka-BR 100 14,99 101,840000 +0,147 17.05.2022 17.05.2022
                            // Zulagenzahlung      2011 Deka-BR 100 101,91 38,880000  +2,621 21.05.2012 21.05.2012
                            // Zulagenrückzahlung Deka-BR 100 1.960,93 84,050000 -23,331 24.11.2020 24.11.2020
                            // Korrekturbuchung Deka-BR 100 114,00 84,050000 -1,356 24.11.2020 24.11.2020
                            // Steuererstattung Deka-BR 100 1,57 98,050000 +0,016 06.01.2023 06.01.2023
                            //
                            // Steuerrückzahlung Deka-DividendenStrategi 42,60 148,480000 -0,287 24.08.2020 24.08.2020
                            // e CF (A
                            // @formatter:on
                            section -> section
                                    .attributes("note", "name", "amount", "type", "shares", "date")
                                    .match("^(?<note>(Zulagenzahlung .* [\\d]{4}"
                                                    + "|Zulagenr.ckzahlung"
                                                    + "|Korrekturbuchung"
                                                    + "|Steuererstattung"
                                                    + "|Steuerr.ckzahlung"
                                                    + "|Kauf (aus )?(Zulagenzahlung|Steuererstattung))) "
                                                    + "(?<name>.*) "
                                                    + "(?<amount>[\\.,\\d]+) "
                                                    + "[\\.,\\d]+"
                                                    + "(?<type>[\\-\\+\\s]+)"
                                                    + "(?<shares>[\\.,\\d]+) "
                                                    + "[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                    .assign((t, v) -> {
                                        DocumentContext context = type.getCurrentContext();

                                        // Is type --> "-" change from DELIVERY_INBOUND to DELIVERY_OUTBOUND
                                        if ("-".equals(trim(v.get("type"))))
                                            t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);

                                        SecurityListHelper securityListHelper = context.getType(SecurityListHelper.class).orElseGet(SecurityListHelper::new);
                                        Optional<SecurityItem> securityItem = securityListHelper.findItemByLineNoStart(v.getStartLineNumber());

                                        if (securityItem.isPresent())
                                        {
                                            v.put("name", securityItem.get().name);
                                            v.put("isin", securityItem.get().isin);
                                        }
                                        else
                                        {
                                            Optional<SecurityItem> securityItemByName = securityListHelper.findItemByName(trim(v.get("name")));
                                            if (securityItemByName.isPresent())
                                            {
                                                v.put("name", securityItemByName.get().name);
                                                v.put("isin", securityItemByName.get().isin);
                                            }
                                        }

                                        t.setSecurity(getOrCreateSecurity(v));
                                        t.setDateTime(asDate(v.get("date")));
                                        t.setShares(asShares(v.get("shares")));
                                        t.setAmount(asAmount(v.get("amount")));
                                        t.setCurrencyCode(CurrencyUnit.EUR);
                                        t.setNote(trim(replaceMultipleBlanks(v.get("note"))));
                                    })
                            ,
                            // @formatter:off
                            // Ausbuchung w/ Fusion -2,140 31.05.2021 28.05.2021
                            // Einbuchung w/ Fusion +1,315 31.05.2021 28.05.2021
                            // Auslieferung -16,000 20.03.2020 18.03.2020
                            // @formatter:on
                            section -> section
                                    .attributes("type", "shares", "date")
                                    .match("^(Einbuchung|Ausbuchung|Auslieferung) .*"
                                                    + "(?<type>[\\-|\\+])"
                                                    + "(?<shares>[\\.,\\d]+) "
                                                    + "[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                    .assign((t, v) -> {
                                        DocumentContext context = type.getCurrentContext();

                                        // Is type --> "-" change from DELIVERY_INBOUND to DELIVERY_OUTBOUND
                                        if ("-".equals(v.get("type")))
                                            t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);

                                        SecurityListHelper securityListHelper = context.getType(SecurityListHelper.class).orElseGet(SecurityListHelper::new);
                                        Optional<SecurityItem> securityItem = securityListHelper.findItemByLineNoStart(v.getStartLineNumber());

                                        if (securityItem.isPresent())
                                        {
                                            v.put("name", securityItem.get().name);
                                            v.put("isin", securityItem.get().isin);
                                        }
                                        else
                                        {
                                            Optional<SecurityItem> securityItemByName = securityListHelper.findItemByName(trim(v.get("name")));
                                            if (securityItemByName.isPresent())
                                            {
                                                v.put("name", securityItemByName.get().name);
                                                v.put("isin", securityItemByName.get().isin);
                                            }
                                        }

                                        t.setSecurity(getOrCreateSecurity(v));
                                        t.setDateTime(asDate(v.get("date")));
                                        t.setShares(asShares(v.get("shares")));
                                        t.setAmount(0L);
                                        t.setCurrencyCode(CurrencyUnit.EUR);
                                    })
                    )

                // @formatter:off
                // Ausbuchung w/ Fusion -2,140 31.05.2021 28.05.2021
                // Einbuchung w/ Fusion +1,315 31.05.2021 28.05.2021
                // @formatter:on
                .section("note").optional()
                .match("^(Einbuchung|Ausbuchung) .* (?<note>.*) ([\\-|\\+])[\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap((t, ctx) -> {
                    TransactionItem item = new TransactionItem(t);

                    if (t.getCurrencyCode() != null && t.getAmount() == 0)
                        item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                    return item;
                }));

        Block dividendeBlock = new Block("^(Verkauf aus Ertrag"
                        + "|Aussch.ttung \\/ Kauf aus Ertrag"
                        + "|Abrechnungsbetrag Ausschüttung"
                        + "|Abrechnungsbetrag Thesaurierung"
                        + "|Ertragsaussch.ttung\\-Storno"
                        + "|Kauf aus Ertrag"
                        + "|Thesaurierung \\/ Kauf aus Ertrag) .*$");
        type.addBlock(dividendeBlock);
        dividendeBlock.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        })

            .oneOf(
                            // @formatter:off
                            // Verkauf aus Ertrag 33,43 177,600000 +0,000 20.05.2022 20.05.2022
                            // Ausschüttung / Kauf aus Ertrag 1,46 179,310000 +0,008 15.02.2019 15.02.2019
                            // Abrechnungsbetrag Ausschüttung 54,72 438,170000  +0,125 20.11.2012 20.11.2012
                            // Abrechnungsbetrag Thesaurierung 0,00 64,500000 0,000 30.12.2005 30.12.2005
                            // Ertragsausschüttung-Storno 1,05 41,970000 - 0,025 07.09.2005 22.08.2005
                            // @formatter:on
                            section -> section
                                    .attributes("type", "amount", "shares", "date")
                                    .match("^(?<type>(Verkauf aus Ertrag"
                                                    + "|Aussch.ttung \\/ Kauf aus Ertrag"
                                                    + "|Abrechnungsbetrag Ausschüttung"
                                                    + "|Abrechnungsbetrag Thesaurierung"
                                                    + "|Ertragsaussch.ttung\\-Storno"
                                                    + "|Kauf aus Ertrag"
                                                    + "|Thesaurierung \\/ Kauf aus Ertrag)) "
                                                    + "(?<amount>[\\.,\\d]+) "
                                                    + "[\\.,\\d]+"
                                                    + "([\\+\\-\\s]+)?(?<shares>[\\.,\\d]+) "
                                                    + "[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                    .assign((t, v) -> {
                                        DocumentContext context = type.getCurrentContext();

                                        if ("Ertragsausschüttung-Storno".equals(v.get("type")))
                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported);

                                        SecurityListHelper securityListHelper = context.getType(SecurityListHelper.class).orElseGet(SecurityListHelper::new);
                                        SharesListHelper sharesListHelper = context.getType(SharesListHelper.class).orElseGet(SharesListHelper::new);

                                        Optional<SecurityItem> securityItem = securityListHelper.findItemByLineNoStart(v.getStartLineNumber());

                                        if (securityItem.isPresent())
                                        {
                                            v.put("name", securityItem.get().name);
                                            v.put("isin", securityItem.get().isin);

                                            Optional<ShareItem> shareItem = sharesListHelper.findItem(v.get("isin"), //
                                                            securityItem.get().lineNoEnd, //
                                                            LocalDate.parse(v.get("date"), DATEFORMAT), //
                                                            asShares(v.get("shares")));

                                            if (shareItem.isPresent())
                                                t.setShares(shareItem.get().shares);
                                        }
                                        else
                                        {
                                            Optional<SecurityItem> securityItemByName = securityListHelper.findItemByName(trim(v.get("name")));
                                            if (securityItemByName.isPresent())
                                            {
                                                v.put("name", securityItemByName.get().name);
                                                v.put("isin", securityItemByName.get().isin);

                                                Optional<ShareItem> shareItem = sharesListHelper.findItem(v.get("isin"), //
                                                                securityItemByName.get().lineNoEnd, //
                                                                LocalDate.parse(v.get("date"), DATEFORMAT), //
                                                                asShares(v.get("shares")));

                                                if (shareItem.isPresent())
                                                    t.setShares(shareItem.get().shares);
                                            }
                                        }

                                        t.setSecurity(getOrCreateSecurity(v));
                                        t.setDateTime(asDate(v.get("date")));
                                        t.setAmount(asAmount(v.get("amount")));
                                        t.setCurrencyCode(CurrencyUnit.EUR);
                                    })
                            ,
                            // @formatter:off
                            // Abrechnungsbetrag Thesaurierung Deka-BR 100 4,39 39,790000  +0,110 02.07.2012 02.07.2012
                            // Kauf aus Ertrag Deka-BR 100 42,80 73,530000 +0,582 12.01.2018 12.01.2018
                            // Thesaurierung / Kauf aus Ertrag Deka-BR 100 0,00 72,270000 +0,000 12.01.2018 29.12.2017
                            // @formatter:on
                            section -> section
                                    .attributes("name", "amount", "shares", "date")
                                    .match("^(?<type>(Verkauf aus Ertrag"
                                                    + "|Aussch.ttung \\/ Kauf aus Ertrag"
                                                    + "|Abrechnungsbetrag Ausschüttung"
                                                    + "|Abrechnungsbetrag Thesaurierung"
                                                    + "|Ertragsaussch.ttung\\-Storno"
                                                    + "|Kauf aus Ertrag"
                                                    + "|Thesaurierung \\/ Kauf aus Ertrag)) "
                                                    + "(?<name>.*) "
                                                    + "(?<amount>[\\.,\\d]+) "
                                                    + "[\\.,\\d]+"
                                                    + "([\\+\\-\\s]+)?(?<shares>[\\.,\\d]+) "
                                                    + "[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                    + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                    .assign((t, v) -> {
                                        DocumentContext context = type.getCurrentContext();

                                        if ("Ertragsausschüttung-Storno".equals(v.get("type")))
                                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported);

                                        SecurityListHelper securityListHelper = context.getType(SecurityListHelper.class).orElseGet(SecurityListHelper::new);
                                        SharesListHelper sharesListHelper = context.getType(SharesListHelper.class).orElseGet(SharesListHelper::new);

                                        Optional<SecurityItem> securityItem = securityListHelper.findItemByLineNoStart(v.getStartLineNumber());

                                        if (securityItem.isPresent())
                                        {
                                            v.put("name", securityItem.get().name);
                                            v.put("isin", securityItem.get().isin);

                                            Optional<ShareItem> shareItem = sharesListHelper.findItem(v.get("isin"), //
                                                            securityItem.get().lineNoEnd, //
                                                            LocalDate.parse(v.get("date"), DATEFORMAT), //
                                                            asShares(v.get("shares")));

                                            if (shareItem.isPresent())
                                                t.setShares(shareItem.get().shares);
                                        }
                                        else
                                        {

                                            Optional<SecurityItem> securityItemByName = securityListHelper.findItemByName(trim(v.get("name")));
                                            if (securityItemByName.isPresent())
                                            {
                                                v.put("name", securityItemByName.get().name);
                                                v.put("isin", securityItemByName.get().isin);

                                                Optional<ShareItem> shareItem = sharesListHelper.findItem(v.get("isin"), //
                                                                securityItemByName.get().lineNoEnd, //
                                                                LocalDate.parse(v.get("date"), DATEFORMAT), //
                                                                asShares(v.get("shares")));

                                                if (shareItem.isPresent())
                                                    t.setShares(shareItem.get().shares);
                                            }
                                        }

                                        t.setSecurity(getOrCreateSecurity(v));
                                        t.setDateTime(asDate(v.get("date")));
                                        t.setAmount(asAmount(v.get("amount")));
                                        t.setCurrencyCode(CurrencyUnit.EUR);
                                    })
                    )

                .wrap((t, ctx) -> {
                    TransactionItem item = new TransactionItem(t);

                    if (t.getCurrencyCode() != null && t.getAmount() == 0)
                        item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                    if (ctx.getString(FAILURE) != null)
                        item.setFailureMessage(ctx.getString(FAILURE));

                    return item;
                }));

        Block feesBlock = new Block("^.*(Vertragsgeb.hr in [\\d]{4}"
                        + "|.* (inkl|incl)\\.( [\\d]+%)? (Mehrwertsteuer \\(MwSt\\)|MwSt|MWSt)"
                        + "|Entgelt Aufl.sung "
                        + "|Vertragspreis(?!.*gesamt) "
                        + "|Weitere Preise(?!.*gesamt) "
                        + "|Abschluss\\- und Vertriebskosten(?!.*gesamt) "
                        + ").*$");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.FEES);
            return entry;
        })

                .oneOf(
                                // @formatter:off
                                // Vertragspreis (zu Lasten Girokonto) -10,00
                                // Vertragspreis (zu Lasten Vertrag)  0,00
                                // Weitere Preise (zu Lasten Girokonto) 0,00
                                // Weitere Preise (zu Lasten Vertrag)  0,00
                                // Vertragspreis -10,00
                                // Abschluss- und Vertriebskosten (Ausgabeaufschlag) -21,06
                                // Abschluss- und Vertriebskosten - Ausgabeaufschlag  -10,77
                                // @formatter:on
                                section -> section
                                        .attributes("note", "amount")
                                        .match("^(?<note>(Vertragspreis"
                                                        + "|Weitere Preise"
                                                        + "|Abschluss\\- und Vertriebskosten(?!.*gesamt).*)"
                                                        + "( \\(zu Lasten (Girokonto|Vertrag)\\))?)"
                                                        + " ([\\-\\s]+)?(?<amount>[\\.,\\d]+).*$")
                                        .assign((t, v) -> {
                                            Map<String, String> context = type.getCurrentContext();

                                            t.setDateTime(asDate(context.get("documentDate")));
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                            t.setNote(trim(v.get("note")));

                                            // Formatting some notes
                                            if (t.getNote().startsWith("Abschluss"))
                                                t.setNote("Abschluss-/ Vertriebskosten");

                                            t.setNote(t.getNote() + " " + t.getDateTime().getYear());
                                        })
                                ,
                                // @formatter:off
                                // Entgelt Auflösung Deka-BR 100 48,74 84,050000 -0,580 24.11.2020 24.11.2020
                                //
                                // Vertragspreis Deka-DividendenStrategi 1,95 148,480000 -0,013 24.08.2020 24.08.2020
                                // e CF (A
                                // @formatter:on
                                section -> section
                                        .attributes("note", "amount", "date")
                                        .match("^(?<note>Entgelt Aufl.sung "
                                                        + "|Vertragspreis ).* "
                                                        + "(?<amount>[\\.,\\d]+) "
                                                        + "[\\.,\\d]+ [\\-\\+\\s]+[\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} "
                                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                        .assign((t, v) -> {
                                            t.setDateTime(asDate(v.get("date")));
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                            t.setNote(trim(v.get("note")));

                                            // Formatting some note
                                            if ("Vertragspreis".equals(trim(v.get("note"))))
                                                t.setNote(trim(v.get("note")) + " " + t.getDateTime().getYear());
                                        })
                                ,
                                // @formatter:off
                                // Vertragsgebühr in 2006 5,00 Euro
                                // @formatter:on
                                section -> section
                                        .attributes("note", "amount")
                                        .match("^.*(?<note>Vertragsgeb.hr) in [\\d]{4} (\\-)?(?<amount>[\\.,\\d]+).*$")
                                        .assign((t, v) -> {
                                            Map<String, String> context = type.getCurrentContext();

                                            if (context.get("contractFeeDate") != null && "Vertragsgebühr".equals(v.get("note")))
                                                t.setDateTime(asDate(context.get("contractFeeDate")));
                                            else
                                                t.setDateTime(asDate(context.get("documentDate")));

                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                            t.setNote(v.get("note") + " " + t.getDateTime().getYear());
                                        })
                                ,
                                // @formatter:off
                                // Depotpreis incl. 16% MWSt: 10,00 belastet im Depot 0111111111 974587 00
                                // Depot-Auszug zum 31.12.2005 Seite 1
                                // @formatter:on
                                section -> section
                                        .attributes("note", "amount")
                                        .match("^.*(?<note>Depotpreis) (inkl|incl)\\.( [\\d]+%)? .*: (?<amount>[\\.,\\d]+) .*$")
                                        .assign((t, v) -> {
                                            Map<String, String> context = type.getCurrentContext();

                                            if (context.get("depotFeeDate") != null)
                                                t.setDateTime(asDate(context.get("depotFeeDate")));
                                            else
                                                t.setDateTime(asDate(context.get("documentDate")));

                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(CurrencyUnit.EUR);
                                            t.setNote(v.get("note") + " " + t.getDateTime().getYear());
                                        })
                                ,
                                // @formatter:off
                                // Depotpreis inkl. 16% MwSt: 10,00 EUR belastet im Depot 0111111111 974587 00
                                // Depotübersicht zum 31.12.2006
                                //
                                // Depotpreis inkl. 19% MwSt: 10,00 EUR belastet im Depot 0111111111 LU0268059614 00
                                // Jahresdepotauszug per 31.12.2008
                                // @formatter:on
                                section -> section
                                        .attributes("note", "amount", "currency")
                                        .match("^.*(?<note>Depotpreis) (inkl|incl)\\.( [\\d]+%)? .*: (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) .*$")
                                        .assign((t, v) -> {
                                            Map<String, String> context = type.getCurrentContext();

                                            if (context.get("depotFeeDate") != null)
                                                t.setDateTime(asDate(context.get("depotFeeDate")));
                                            else
                                                t.setDateTime(asDate(context.get("documentDate")));

                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setNote(v.get("note") + " " + t.getDateTime().getYear());
                                        })
                                ,
                                // @formatter:off
                                // Depotpreis inkl. 19% Mehrwertsteuer (MwSt):
                                // 12,50 EUR inkl. 2,00 EUR MwSt wurden für 2019 belastet
                                // per 31.12.2019
                                //
                                // D epotpreis inkl. 19% MwSt:
                                // 10,00 EUR wurden für 2012 belastet
                                // Jahresdepotauszug per 31.12.2012
                                //
                                // Depotpreis inkl. Mehrwertsteuer (MwSt):
                                // 0,00 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("note", "amount", "currency")
                                        .match("^(?<note>.*) (inkl|incl)\\. .*$")
                                        .match("^(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})( .* [\\d]{4} )?.*$")
                                        .assign((t, v) -> {
                                            Map<String, String> context = type.getCurrentContext();

                                            if (context.get("depotFeeDate") != null)
                                                t.setDateTime(asDate(context.get("depotFeeDate")));
                                            else
                                                t.setDateTime(asDate(context.get("documentDate")));

                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setNote(trim(stripBlanks(v.get("note"))) + " " + t.getDateTime().getYear());
                                        })
                        )

                .wrap(t -> {
                    TransactionItem item = new TransactionItem(t);
                    if (t.getCurrencyCode() != null && t.getAmount() == 0)
                        item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);
                    return item;
                }));
    }

    private void addTaxReturnBlock(DocumentType type)
    {
        Block block = new Block("^(ERTRAGSAUSSCH.TTUNG|ERTRAGSABRECHNUNG)$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.TAX_REFUND);
            return t;
        })

            .oneOf(
                            // @formatter:off
                            // Ausschüttung (pro Anteil EUR 0,2292000): EUR 0,14
                            // Bezeichnung: iShares J.P. Morgan USD EM Bond EUR Hedged UCITS ETF (Dist)
                            // ISIN: IE00B9M6RS56 Unterdepot: 00 Auftragsnummer: 9302 2538
                            // @formatter:on
                            section -> section
                                    .attributes("currency", "name", "isin")
                                    .match("^Aussch.ttung .* (?<currency>[\\w]{3}) [\\.,\\d]+$")
                                    .match("^Bezeichnung: (?<name>.*)$")
                                    .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                    .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                            ,
                            // @formatter:off
                            // Fondsbezeichnung: AriDeka CF
                            // ISIN: DE0008474511 Unterkonto: 00 Auftragsnummer: 9387 9103
                            // +Verrechnete Steuern EUR 1,43 EUR 29,15 EUR 33,420000 Anteilumsatz: 0,872
                            // @formatter:on
                            section -> section
                                    .attributes("name", "isin", "currency")
                                    .match("^Fondsbezeichnung: (?<name>.*)$")
                                    .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                    .match("^.*(?<currency>[\\w]{3}) [\\.,\\d]+ Anteilumsatz: [\\.,\\d]+$")
                                    .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                    )

            .oneOf(
                            // @formatter:off
                            // Anteilbestand am Ertragstermin: 0,619
                            // @formatter:on
                            section -> section
                                    .attributes("shares")
                                    .match("^Anteilbestand am Ertragstermin: (?<shares>[\\.,\\d]+)$")
                                    .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                            ,
                            // @formatter:off
                            //  Ausschüttung EUR 27,72 Preis Bestand alt: 29,811
                            // @formatter:on
                            section -> section
                                    .attributes("shares")
                                    .match("^.* Bestand alt: (?<shares>[\\.,\\d]+)$")
                                    .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                    )

            // @formatter:off
            // Verwahrart: GiroSammel Abrechnungstag: 31.03.2022
            // @formatter:on
            .section("date")
            .match("^.*Abrechnungstag: (?<date>[\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+)$")
            .assign((t, v) -> t.setDateTime(asDate(stripBlanks(v.get("date")))))

            // @formatter:off
            // Ausschüttung EUR 78,81 Kurs/Kaufpreis Bestand alt: 33,823
            // +Verrechnete Steuern EUR 1,43 EUR 29,15 EUR 33,420000 Anteilumsatz: 0,872
            // @formatter:on
            .section("currency", "amount").optional()
            .match("^\\+.*Verrechnete Steuern (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+).*$")
            .assign((t, v) -> {
                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                t.setAmount(asAmount(v.get("amount")));
            })

            // @formatter:off
            // ISIN: DE0008474511 Unterkonto: 00 Auftragsnummer: 9387 9103
            // @formatter:on
            .section("note").optional().match("^.* (?<note>Auftragsnummer: .*)$")
            .assign((t, v) -> t.setNote(trim(v.get("note"))))

            .wrap(t -> {
                if (t.getCurrencyCode() != null && t.getAmount() != 0)
                    return new TransactionItem(t);
                return null;
            }));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // + Verrechnete Steuern EUR 1,72
                // - Verrechnete Steuern EUR 15,34 EUR 63,47 EUR 138,830000 Anteilumsatz: 0,457
                // +Verrechnete Steuern EUR 1,43 EUR 29,15 EUR 33,420000 Anteilumsatz: 0,872
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^([\\-|\\+]).*Verrechnete Steuern (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    if (!type.getCurrentContext().getBoolean("noTax"))
                        processTaxEntries(t, v, type);
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Ausgabeaufschlag: 5,260%
                // Abrechnungsbetrag EUR 72,00 EUR 72,00 EUR 53,780000 Anteilumsatz: 1,339
                // @formatter:on
                .section("percentageFee", "amount", "currency").optional()
                .match("^Ausgabeaufschlag: (?<percentageFee>[\\.,\\d]+)%$")
                .match("^.*(Einzugsbetrag|Auszahlungsbetrag|Abrechnungsbetrag) [\\w]{3} [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    BigDecimal percentageFee = asBigDecimal(v.get("percentageFee"));
                    BigDecimal amount = asBigDecimal(v.get("amount"));

                    if (percentageFee.compareTo(BigDecimal.ZERO) != 0)
                    {
                        // @formatter:off
                        // fxFee = (amount / (1 + percentageFee / 100)) * (percentageFee / 100)
                        // @formatter:on
                        BigDecimal fxFee = amount
                                        .divide(percentageFee.divide(BigDecimal.valueOf(100))
                                                        .add(BigDecimal.ONE), Values.MC)
                                        .multiply(percentageFee, Values.MC);

                        Money fee = Money.of(asCurrencyCode(v.get("currency")),
                                        fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

                        checkAndSetFee(fee, t, type.getCurrentContext());
                    }
                });
    }

    private static class SecurityListHelper
    {
        private List<SecurityItem> items = new ArrayList<>();

        // Finds a SecurityItem in the list that starts on the specified line
        // number
        public Optional<SecurityItem> findItemByLineNoStart(int lineNo)
        {
            if (items.isEmpty())
                return Optional.empty();

            for (int i = items.size() - 1; i >= 0; i--) // NOSONAR
            {
                SecurityItem item = items.get(i);

                if ((item.lineNoStart > lineNo) || (item.lineNoEnd < lineNo))
                    continue;

                return Optional.of(item);
            }

            return Optional.empty();
        }

        // Finds a SecurityItem in the list that starts on the specified name
        public Optional<SecurityItem> findItemByName(String name)
        {
            if (items.isEmpty())
                return Optional.empty();

            for (int i = items.size() - 1; i >= 0; i--) // NOSONAR
            {
                SecurityItem item = items.get(i);

                // We cannot take "equals" because the security names continue
                // on the next line as well. Therefore we take "contains".
                if (!item.name.contains(name))
                    continue;

                return Optional.of(item);
            }

            return Optional.empty();
        }
    }

    private static class SecurityItem
    {
        String isin;
        String name;
        int lineNoStart;
        int lineNoEnd;

        @Override
        public String toString()
        {
            return "SecurityItem [isin=" + isin + ", name=" + name + ", lineNoStart=" + lineNoStart + ", lineNoEnd="
                            + lineNoEnd + "]";
        }
    }

    private static class SharesListHelper
    {
        private List<ShareItem> items = new ArrayList<>();

        public Optional<ShareItem> findItem(String isin, int securitylineNoEnd, LocalDate date, Long addShares)
        {
            if (items.isEmpty())
                return Optional.empty();

            for (int i = items.size() - 1; i >= 0; i--) // NOSONAR
            {
                ShareItem item = items.get(i);

                if (!item.isin.equals(isin))
                    continue;

                if (securitylineNoEnd != 0)
                {
                    if (item.lineNo >= securitylineNoEnd)
                        continue;
                }

                if (!item.date.equals(date))
                    continue;

                if (!item.addShare.equals(addShares))
                    continue;

                return Optional.of(item);
            }

            return Optional.empty();
        }
    }

    private static class ShareItem
    {
        String isin;
        int lineNo;
        LocalDate date;
        Long addShare;
        Long shares;

        @Override
        public String toString()
        {
            return "ShareItem [isin=" + isin + ", lineNo=" + lineNo + ", date=" + date + ", addShare="
                            + addShare + ", shares=" + shares + "]";
        }
    }
}
