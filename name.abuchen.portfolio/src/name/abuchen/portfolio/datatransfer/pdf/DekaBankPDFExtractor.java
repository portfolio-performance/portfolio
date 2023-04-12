package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

@SuppressWarnings("nls")
public class DekaBankPDFExtractor extends AbstractPDFExtractor
{
    public DekaBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("DekaBank"); //$NON-NLS-1$

        addBuySellTransaction();
        addSwapBuyTransaction();
        addSwapSellTransaction();
        addReinvestTransaction();
        addDividendeTransaction();
        addDepotStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "DekaBank Deutsche Girozentrale"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(LASTSCHRIFTEINZUG|VERKAUF|KAUF AUS ERTRAG)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(LASTSCHRIFTEINZUG|VERKAUF|KAUF AUS ERTRAG).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(?<type>(LASTSCHRIFTEINZUG|VERKAUF|KAUF AUS ERTRAG)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("VERKAUF"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                .oneOf(
                                // @formatter:off
                                // Bezeichnung: Deka-UmweltInvest TF
                                // ISIN: DE000DK0ECT0 Unterdepot: 00 Auftragsnummer: 8103 1017
                                // =Abrechnungsbetrag EUR 4.000,00 EUR 4.000,00 EUR 494,260000 Anteilumsatz: 8,093
                                // @formatter:on
                                section -> section
                                        .attributes("name", "isin", "currency")
                                        .match("^Bezeichnung: (?<name>.*)$")
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
                                        .match("^Bezeichnung: (?<name>.*)$")
                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .find("Kurs(\\/Kaufpreis)?.*")
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
                // @formatter:on
                .section("date")
                .match("^.*Abrechnungstag: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // @formatter:off
                // Einzugsbetrag EUR 4.000,00 Kurs/Kaufpreis Bestand alt: 11,291
                // Auszahlungsbetrag EUR 2.355,09
                // Abrechnungsbetrag EUR 1,00 EUR 1,00 EUR 96,576000 Anteilumsatz: 0,010
                // @formatter:on
                .section("currency", "amount")
                .match("^(Einzugsbetrag|Auszahlungsbetrag|Abrechnungsbetrag) (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
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
                                        .find("^zu Gunsten$")
                                        .match("^Bezeichnung: (?<name>.*)$")
                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                        .find("Kurs(\\/Kaufpreis)?.*")
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
                                        .find("^zu Gunsten$")
                                        .match("^Bezeichnung: (?<name>.*)$")
                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                        .find("Kurs(\\/Kaufpreis)?.*")
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
                .match("^.*Abrechnungstag: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

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
                                        .match("^Bezeichnung: (?<name>.*)$")
                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                        .find("Kurs(\\/Kaufpreis)?.*")
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
                                        .find("^zu Lasten$")
                                        .match("^Bezeichnung: (?<name>.*)$")
                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                                        .find("Kurs(\\/Kaufpreis)?.*")
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
                .match("^.*Abrechnungstag: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

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

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addReinvestTransaction()
    {
        DocumentType type = new DocumentType("ERTRAGSAUSSCH.TTUNG \\/ KAUF AUS ERTRAG");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^ERTRAGSAUSSCH.TTUNG \\/ KAUF AUS ERTRAG$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // @formatter:off
                // Ausschüttung (pro Anteil EUR 2,3300000): EUR 78,81
                // Bezeichnung: Deka-EuropaPotential TF
                // ISIN: DE0009786285 Unterdepot: 00 Auftragsnummer: 9457 2850
                // @formatter:on
                .section("currency", "name", "isin")
                .match("^Aussch.ttung .* (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .match("^Bezeichnung: (?<name>.*)$")
                .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

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
                .match("^.*Abrechnungstag: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // @formatter:off
                // =Wiederanlagebetrag EUR 63,47 Bestand neu: 34,280
                // @formatter:on
                .section("currency", "amount")
                .match("^.*Wiederanlagebetrag (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // @formatter:off
                // =Wiederanlagebetrag EUR 63,47 Bestand neu: 34,280
                // @formatter:on
                .section("note")
                .match("^.*(?<note>Wiederanlage).* [\\w]{3} [\\.,\\d]+.*$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("ERTRAGSAUSSCH.TTUNG");
        this.addDocumentTyp(type);

        Block block = new Block("^ERTRAGSAUSSCH.TTUNG.*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // @formatter:off
                // Ausschüttung (pro Anteil EUR 0,2292000): EUR 0,14
                // Bezeichnung: iShares J.P. Morgan USD EM Bond EUR Hedged UCITS ETF (Dist)
                // ISIN: IE00B9M6RS56 Unterdepot: 00 Auftragsnummer: 9302 2538
                // @formatter:on
                .section("currency", "name", "isin")
                .match("^Aussch.ttung .* (?<currency>[\\w]{3}) [\\.,\\d]+$")
                .match("^Bezeichnung: (?<name>.*)$")
                .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Anteilbestand am Ertragstermin: 0,619
                // @formatter:on
                .section("shares")
                .match("^Anteilbestand am Ertragstermin: (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Verwahrart: GiroSammel Abrechnungstag: 31.03.2022
                // @formatter:on
                .section("date")
                .match("^.*Abrechnungstag: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                // Ausschüttung EUR 0,14 Kurs Bestand alt: 0,739
                // @formatter:on
                .section("currency", "amount")
                .match("^Aussch.ttung (?<currency>[\\w]{3}) (?<amount>[.,\\d]+) .*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Ausschüttung EUR 78,81 Kurs/Kaufpreis Bestand alt: 33,823
                // - Verrechnete Steuern EUR 15,34 EUR 63,47 EUR 138,830000 Anteilumsatz: 0,457
                // =Wiederanlagebetrag EUR 63,47 Bestand neu: 34,280
                .section("currency", "tax").optional()
                .match("^\\- Verrechnete Steuern (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));

                    t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    public void addDepotStatementTransaction()
    {
        final DocumentType type = new DocumentType("Quartalsbericht per [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}", (context, lines) -> {
            Pattern pAccountCurrency = Pattern.compile("^depot in (?<accountCurrency>[\\w]{3}) in [\\w]{3}$");
            Pattern pSecurityCurrency = Pattern.compile("^(?<securityCurrency>[\\w]{3}) Fremdw.hrung [\\w]{3}$");
            Pattern pIsin = Pattern.compile("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$");

            int endBlock = lines.length;
            String securityCurrency = CurrencyUnit.EUR;

            for (int i = lines.length - 1; i >= 0; i--)
            {
                Matcher m = pAccountCurrency.matcher(lines[i]);
                if (m.matches())
                    context.put("accountCurrency", m.group("accountCurrency"));

                m = pIsin.matcher(lines[i]);
                if (m.matches())
                {
                    // Search the security currency in the block
                    for (int ii = i; ii < endBlock; ii++)
                    {
                        Matcher m1 = pSecurityCurrency.matcher(lines[ii]);
                        if (m1.matches())
                            securityCurrency = m1.group("securityCurrency");
                    }

                    // @formatter:off
                    // Stringbuilder:
                    // security_(security name)_(currency)_(start@line)_(end@line) = isin
                    // 
                    // Example:
                    // Deka-GlobalChampions TF
                    // ISIN: DE000DK0ECV6 Unterdepot: 00
                    // EUR Fremdwährung EUR Anteile tag nungstag
                    // @formatter:on
                    StringBuilder securityListKey = new StringBuilder("security_");
                    securityListKey.append(trim(lines[i - 1])).append("_");
                    securityListKey.append(securityCurrency).append("_");
                    securityListKey.append(Integer.toString(i)).append("_");
                    securityListKey.append(Integer.toString(endBlock));
                    context.put(securityListKey.toString(), m.group("isin"));

                    endBlock = i;
                }
            }
        });
        this.addDocumentTyp(type);

        // @formatter:off
        // Lastschrifteinzug 250,00 198,660000 +1,258 01.04.2021 01.04.2021
        // Verkauf 2.039,96 102,810000 -19,842 11.05.2021 11.05.2021
        // Kauf 36,00 216,100000 +0,167 18.07.2022 18.07.2022
        // @formatter:on
        Block buySellBlock = new Block("^(Lastschrifteinzug|Verkauf|Kauf)(?! aus Ertrag).*$");
        type.addBlock(buySellBlock);
        buySellBlock.set(new Transaction<BuySellEntry>()
            .subject(() -> {
                BuySellEntry entry = new BuySellEntry();
                entry.setType(PortfolioTransaction.Type.BUY);
                return entry;
        })

                .section("amount", "type", "shares", "date")
                .match("^(Lastschrifteinzug|Verkauf|Kauf) (?<amount>[\\.,\\d]+) [\\.,\\d]+ (?<type>[\\-|\\+])(?<shares>[\\.,\\d]+) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    // Is sign --> "-" change from BUY to SELL
                    if (v.get("type").equals("-"))
                        t.setType(PortfolioTransaction.Type.SELL);

                    Security securityData = getSecurity(context, v.getStartLineNumber());
                    if (securityData != null)
                    {
                        v.put("name", securityData.getName());
                        v.put("isin", securityData.getIsin());
                        v.put("currency", asCurrencyCode(securityData.getCurrency()));
                    }

                    t.setDate(asDate(v.get("date")));
                    t.setShares(asShares(v.get("shares")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("accountCurrency")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                .wrap(t -> {
                    if (t.getPortfolioTransaction().getCurrencyCode() != null)
                        return new BuySellEntryItem(t);
                    return null;
                }));

        // @formatter:off
        // Ausbuchung w/ Fusion -2,140 31.05.2021 28.05.2021
        // Einbuchung w/ Fusion +1,315 31.05.2021 28.05.2021
        // @formatter:on
        Block deliveryInOutbondblock = new Block("^(Einbuchung|Ausbuchung).*$");
        type.addBlock(deliveryInOutbondblock);
        deliveryInOutbondblock.set(new Transaction<PortfolioTransaction>()
            .subject(() -> {
                PortfolioTransaction transaction = new PortfolioTransaction();
                transaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                return transaction;
        })

                .section("type", "shares", "date")
                .match("^(Einbuchung|Ausbuchung) .* (?<type>[\\-|\\+])(?<shares>[\\.,\\d]+) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    // Is sign --> "-" change from DELIVERY_INBOUND to DELIVERY_OUTBOUND
                    if (v.get("type").equals("-"))
                        t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);

                    Security securityData = getSecurity(context, v.getStartLineNumber());
                    if (securityData != null)
                    {
                        v.put("name", securityData.getName());
                        v.put("isin", securityData.getIsin());
                        v.put("currency", asCurrencyCode(securityData.getCurrency()));
                    }

                    t.setDateTime(asDate(v.get("date")));
                    t.setShares(asShares(v.get("shares")));
                    t.setAmount(0L);
                    t.setCurrencyCode(asCurrencyCode(context.get("accountCurrency")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // @formatter:off
                // Ausbuchung w/ Fusion -2,140 31.05.2021 28.05.2021
                // Einbuchung w/ Fusion +1,315 31.05.2021 28.05.2021
                // @formatter:on
                .section("note").optional()
                .match("^(Einbuchung|Ausbuchung) .* (?<note>.*) [\\-|\\+][\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(t -> {
                    if (t.getCurrencyCode() != null)
                        return new TransactionItem(t);
                    return null;
                }));

        // @formatter:off
        // Verkauf aus Ertrag 33,43 177,600000 +0,000 20.05.2022 20.05.2022
        // @formatter:on
        Block dividendeBlock = new Block("^Verkauf aus Ertrag.*$");
        type.addBlock(dividendeBlock);
        dividendeBlock.set(new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
        })

                .section("amount", "shares", "date")
                .match("^Verkauf aus Ertrag (?<amount>[\\.,\\d]+) (?<shares>[\\.,\\d]+) \\+[\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    Security securityData = getSecurity(context, v.getStartLineNumber());
                    if (securityData != null)
                    {
                        v.put("name", securityData.getName());
                        v.put("isin", securityData.getIsin());
                        v.put("currency", asCurrencyCode(securityData.getCurrency()));
                    }

                    t.setDateTime(asDate(v.get("date")));
                    t.setShares(asShares(v.get("shares")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("accountCurrency")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null)
                        return new TransactionItem(t);
                    return null;
                }));

        // @formatter:off
        // per 31.12.2022* seit 31.12.2021 1
        // Depotpreis inkl. Mehrwertsteuer (MwSt): 
        // 12,50 EUR inkl. 2,00 EUR MwSt wurden für 2022 belastet
        // Depotübersicht
        // @formatter:on
        Block feesBlock = new Block("^Depotpreis inkl\\. Mehrwertsteuer.*$");
        type.addBlock(feesBlock);
        feesBlock.set(new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.FEES);
                return entry;
        })

                .section("note1", "amount", "currency", "note2", "date")
                .find("(?<note1>Depotpreis) inkl\\. Mehrwertsteuer.*")
                .match("^(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}+) .*(?<note2> f.r [\\d]{4}).*$")
                .match("^per (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .match("^Depot.bersicht$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("accountCurrency")));
                    t.setNote(v.get("note1") + v.get("note2"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null)
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
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^[\\-|\\+] Verrechnete Steuern (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+).*$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private Security getSecurity(Map<String, String> context, Integer entry)
    {
        for (String key : context.keySet())
        {
            String[] parts = key.split("_"); //$NON-NLS-1$
            if (parts[0].equalsIgnoreCase("security")) //$NON-NLS-1$
            {
                if (entry >= Integer.parseInt(parts[3]) && entry <= Integer.parseInt(parts[4]))
                    // returns security name, isin, security currency
                    return new Security(parts[1], context.get(key), parts[2]);
            }
        }
        return null;
    }

    private static class Security
    {
        public Security(String name, String isin, String currency)
        {
            this.name = name;
            this.isin = isin;
            this.currency = currency;
        }

        private String name;
        private String isin;
        private String currency;

        public String getName()
        {
            return name;
        }

        public String getIsin()
        {
            return isin;
        }

        public String getCurrency()
        {
            return currency;
        }
    }
}
