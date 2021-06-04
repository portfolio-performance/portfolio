package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class DABPDFExtractor extends AbstractPDFExtractor
{
    public DABPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("DAB Bank"); //$NON-NLS-1$
        addBankIdentifier("BNP Paribas S.A. Niederlassung Deutschland"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendTransaction();
        addTransferOutInTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return "Computershare Communication Services GmbH"; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "DAB Bank"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Kauf|Verkauf) .*$", "Dieser Beleg wird .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(?<type>Verkauf) .*")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // ComStage-MSCI USA TRN UCIT.ETF Inhaber-Anteile I o.N. LU0392495700
                // STK 43,000 EUR 47,8310
                .section("isin", "name", "shares", "currency")
                .find("Gattungsbezeichnung ISIN")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("STK (?<shares>[.,\\d]+) (?<currency>[\\w]{3}) [.,\\d]+$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Handelstag 24.08.2015  Kurswert                    USD 5.205,00
                .section("date")
                .match("^Handelstag (?<date>\\d+.\\d+.\\d{4}) .*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Handelstag 24.08.2015  Kurswert                    USD 5.205,00
                // Handelszeit 16:38* Provision USD 13,01-
                .section("date", "time").optional()
                .match("^Handelstag (?<date>\\d+.\\d+.\\d{4}) .*$")
                .match("^Handelszeit (?<time>\\d+:\\d+).*$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // 24.11.2020  EUR 685,50
                // 08.01.2015 8022574001 EUR 150,00
                .section("amount", "currency").optional()
                .match("^\\d+.\\d+.\\d{4} .* (?<currency>[\\w]{3}) (?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // 03.08.2015 0000000000 EUR/USD 1,100297 EUR 4.798,86
                .section("amount", "currency", "fxCurrency", "exchangeRate").optional()
                .match("^\\d+.\\d+.\\d{4} [\\d]+ [\\w]{3}\\/(?<fxCurrency>[\\w]{3}) (?<exchangeRate>[.,\\d]+) (?<currency>[\\w]{3}) (?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                // Börse USA/NAN Ausmachender Betrag USD 5.280,17-
                // 03.08.2015 0000000000 EUR/USD 1,100297 EUR 4.798,86
                .section("fxcurrency", "fxamount", "exchangeRate").optional()
                .match("^.* (Ausmachender Betrag|Kurswert) (?<fxcurrency>[\\w]{3}) (?<fxamount>[.,\\d]+)[-]?$")
                .match("^\\d+.\\d+.\\d{4} [\\d]+ [\\w]{3}\\/[\\w]{3} (?<exchangeRate>[.,\\d]+) [\\w]{3} [.,\\d]+$")
                .assign((t, v) -> {
                    // read the forex currency, exchange rate and gross
                    // amount in forex currency
                    String forex = asCurrencyCode(v.get("fxcurrency"));
                    if (t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                    {
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // gross given in forex currency
                        long fxAmount = asAmount(v.get("fxamount"));
                        long amount = reverseRate.multiply(BigDecimal.valueOf(fxAmount))
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();

                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                        Money.of(t.getPortfolioTransaction().getCurrencyCode(), amount),
                                        Money.of(forex, fxAmount), reverseRate);

                        t.getPortfolioTransaction().addUnit(grossValue);
                    }
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        Block taxBlock = new Block("zu versteuern \\(negativ\\).*");
        type.addBlock(taxBlock);
        taxBlock.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.TAX_REFUND);
            return t;
        })

                .section("amount", "currency", "date")
                .match("Wert Konto-Nr. Abrechnungs-Nr. Betrag zu Ihren Gunsten")
                .match("^(?<date>\\d+.\\d+.\\d{4}+) ([\\d]+) ([\\d]+) (?<currency>[\\w]{3}) (?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setDateTime(asDate(v.get("date")));
                })

                .wrap(t -> new TransactionItem(t)));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("(Dividende|Ertr.gnisgutschrift)");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividendengutschrift|Erträgnisgutschrift (?!aus)).*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // Paychex Inc. Registered Shares DL -,01 US7043261079
                // STK 10,000 31.07.2020 27.08.2020 USD 0,620000
                .section("isin", "name", "shares", "currency").optional()
                .find("Gattungsbezeichnung ISIN")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("^STK (?<shares>[.,\\d]+) \\d+.\\d+.\\d{4} \\d+.\\d+.\\d{4} (?<currency>[\\w]{3}) [.,\\d]+$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Wertpapierbezeichnung WKN ISIN
                // HORMEL FOODS CORP. Registered Shares DL 0,01465 850875 US4404521001
                .section("name", "wkn", "isin", "currency").optional()
                .find("Wertpapierbezeichnung WKN ISIN")
                .match("^(?<name>.*) (?<wkn>.*) (?<isin>[\\w]{12})$")
                .match("^Dividende pro St.ck [.,\\d]+ (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // 1.500 Stück
                .section("shares").optional()
                .match("(?<shares>[.,\\d]+) St.ck")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Valuta 15.02.2018 BIC CSDBDE71XXX
                .section("date").optional()
                .match("Valuta (?<date>\\d+.\\d+.\\d{4}+).*")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // 08.01.2015 8022574001 EUR 150,00
                .section("date", "amount", "currency").optional()
                .find("Nominal Ex-Tag Zahltag .*")
                .match("^(?<date>\\d+.\\d+.\\d{4}) [\\d]+ (?<currency>[\\w]{3}) (?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Netto in USD zugunsten IBAN DE90 209,38 USD
                .section("amount", "currency").optional()
                .match("^Netto .* zugunsten IBAN .* (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(v.get("currency"));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // 27.08.2020  USD 4,64
                .section("date", "amount", "currency").optional()
                .find("Wert *Konto-Nr. *Betrag *zu *Ihren *Gunsten")
                .match("^(?<date>\\d+.\\d+.\\d{4})([\\s]+)?(?<currency>[\\w]{3}) (?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // 30.03.2015 0000000000 EUR/ZAR 13,195 EUR 586,80
                .section("date", "amount", "currency", "forexCurrency", "exchangeRate").optional()
                .find("Wert Konto-Nr. Devisenkurs Betrag zu Ihren Gunsten")
                .match("^(?<date>\\d+.\\d+.\\d{4}) [\\d]+ [\\w]{3}\\/(?<forexCurrency>[\\w]{3}) (?<exchangeRate>[.,\\d]+) (?<currency>[\\w]{3}) (?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate")).setScale(10,
                                    RoundingMode.HALF_DOWN);
                    BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")),
                                    Math.round(t.getAmount() / inverseRate.doubleValue()));
                    Unit unit = new Unit(Unit.Type.GROSS_VALUE, t.getMonetaryAmount(), forex, inverseRate);
                    if (unit.getForex().getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                        t.addUnit(unit);
                })

                /***
                 * this section is needed, if the dividend is payed in
                 * the forex currency to a account in forex curreny but
                 * the security is listed in local currency
                 */
                .section("forex", "localCurrency", "forexCurrency", "exchangeRate").optional() //
                .find("Wert Konto-Nr. Betrag zu Ihren Gunsten")
                .match("^(\\d{2}.\\d{2}\\.\\d{4}) ([0-9]*) (\\w{3}) (?<forex>[\\d.]+,\\d+)$")
                .match("^Devisenkurs: (?<localCurrency>\\w{3})/(?<forexCurrency>\\w{3}) (?<exchangeRate>[\\d.]+,\\d+)$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate")).setScale(10,
                                    RoundingMode.HALF_DOWN);
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forex")));
                    Money localAmount = Money.of(v.get("localCurrency"), Math.round(forex.getAmount()
                                    / Double.parseDouble(v.get("exchangeRate").replace(',', '.'))));

                    t.setAmount(forex.getAmount());
                    t.setCurrencyCode(forex.getCurrencyCode());

                    Unit unit = new Unit(Unit.Type.GROSS_VALUE, forex, localAmount, exchangeRate);

                    if (unit.getForex().getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                        t.addUnit(unit);
                })

                /***
                 * if gross dividend is given in document, we need to fix the unit.
                 * if security currency and transaction currency differ
                 */
                // ausländische Dividende EUR 5,25
                // Devisenkurs: EUR/USD 1,1814
                .section("fxCurrency", "fxAmount", "currency", "exchangeRate").optional()
                .match("^ausl.ndische Dividende [\\w]{3} (?<fxAmount>[.,\\d]+)$")
                .match("^Devisenkurs: (?<fxCurrency>[\\w]{3})\\/(?<currency>[\\w]{3}) (?<exchangeRate>[.,\\d]+)$")
                .assign((t, v) -> {
                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));

                        // check, if forex currency is transaction
                        // currency or not and swap amount, if necessary
                        Unit grossValue;
                        if (!asCurrencyCode(v.get("fxCurrency")).equals(t.getCurrencyCode()))
                        {
                            Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(v.get("fxAmount")));
                            long localAmount = exchangeRate.multiply(BigDecimal.valueOf(fxAmount.getAmount()))
                                            .longValue();
                            Money amount = Money.of(asCurrencyCode(v.get("currency")), localAmount);
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, exchangeRate);
                        }
                        else
                        {
                            Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(v.get("fxAmount")));
                            long forexAmount = exchangeRate.multiply(BigDecimal.valueOf(amount.getAmount()))
                                            .longValue();
                            Money fxAmount = Money.of(asCurrencyCode(v.get("currency")), forexAmount);
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, exchangeRate);
                        }
                        // remove existing unit to replace with new one
                        Optional<Unit> grossUnit = t.getUnit(Unit.Type.GROSS_VALUE);
                        if (grossUnit.isPresent())
                        {
                            t.removeUnit(grossUnit.get());
                        }
                        t.addUnit(grossValue);
                    }
                })

                // Brutto in USD 281,25 USD
                // Devisenkurs 1,249900 USD / EUR
                // Brutto in EUR 225,02 EUR
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match("Brutto in [\\w]{3} (?<fxAmount>[.,\\d]+) (?<fxCurrency>[\\w]{3})")
                .match("Devisenkurs (?<exchangeRate>[.,\\d]+) [\\w]{3} \\/ [\\w]{3}")
                .match("Brutto in [\\w]{3} (?<amount>[.,\\d]+) (?<currency>[\\w]{3})")
                .assign((t, v) -> {
                    // Example: Devisenkurs 1,249900 USD / EUR
                    // check which currency is transaction currency and
                    // use exchange rate accordingly
                    // if transaction currency is e.g. USD, we need to
                    // inverse the rate
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                    {
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                    }
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // check, if forex currency is transaction
                        // currency or not and swap amount, if necessary
                        Unit grossValue;
                        if (!asCurrencyCode(v.get("fxCurrency")).equals(t.getCurrencyCode()))
                        {
                            Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(v.get("fxAmount")));
                            Money amount = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        else
                        {
                            Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(v.get("fxAmount")));
                            Money fxAmount = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        t.addUnit(grossValue);
                    }
                })

                // Devisenkurs: EUR/USD 1,1814
                .section("exchangeRate", "fxCurrency").optional()
                .match("^Devisenkurs: [\\w]{3}\\/(?<fxCurrency>[\\w]{3}) (?<exchangeRate>[.,\\d]+)$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                    {
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                    }
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    @SuppressWarnings("nls")
    private void addTransferOutInTransaction()
    {
        DocumentType type = new DocumentType("Einbuchung|Ihr Depotbestand");
        this.addDocumentTyp(type);

        Transaction<PortfolioTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            PortfolioTransaction entry = new PortfolioTransaction();
            entry.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Einbuchung|Ihr Depotbestand:)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Gattungsbezeichnung ISIN
                // Solutiance AG Inhaber-Aktien o.N. DE0006926504
                .section("isin", "name", "name1", "shares", "currency").optional()
                .find("Gattungsbezeichnung ISIN")
                .match("^(?<name>.*) (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)")
                .match("STK (?<shares>[.,\\d]+) (?<currency>[\\w]{3}) [.,\\d]+$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Gattungsbezeichnung ISIN
                // BNP P.Easy-MSCI Pac.x.Jap.x.CW Nam.-Ant.UCITS ETF CAP EUR LU1291106356
                // o.N
                .section("isin", "name", "name1", "shares", "currency").optional()
                .find("Gattungsbezeichnung ISIN")
                .match("^(?<name>.*) (?<currency>[\\w]{3}) (?<isin>[\\w]{12})$")
                .match("^(?<name1>.*)")
                .match("STK (?<shares>[.,\\d]+)$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Nominal"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // 11.03.2021 3331111113 EUR 27,50
                .section("date", "amount", "currency").optional()
                .match("^(?<date>\\d+.\\d+.\\d{4}) [\\d]+ (?<currency>[\\w]{3}) (?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // STK 0,0722 04.12.2018
                .section("date").optional()
                .match("^STK [.,\\d]+ (?<date>\\d+.\\d+.\\d{4})$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                })

                /***
                 * Deposit without amount (share split)
                 */
                // Wir haben Ihrem Depot im Verhältnis 1 : 22 folgende Stücke zugebucht (hinzugefügt):
                .section("note").optional()
                .match("^Wir haben Ihrem Depot im (?<note>Verh.ltnis [\\d]+ : [\\d]+) .*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(t.getSecurity().getCurrencyCode());
                    t.setAmount(0L);
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    @SuppressWarnings("nls")
    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        // if we have a tax return, we set a flag and don't book tax below
        transaction
                .section("n").optional()
                .match("zu versteuern \\(negativ\\) (?<n>.*)")
                .assign((t, v) -> {
                    type.getCurrentContext().put("negative", "X");
                });

        transaction
                // davon anrechenbare US-Quellensteuer 15% EUR 0,79
                // davon anrechenbare Quellensteuer 15% ZAR 1.560,00
                // davon anrechenbare Quellensteuer Fondseingangsseite EUR 1,62
                .section("tax", "currency").optional()
                .match("(^(.*)?davon anrechenbare) (US-)?Quellensteuer .* ([\\s]+)?(?<currency>[\\w]{3})[\\s+]? (?<tax>[.,\\d]+)[-]?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Börse Sekunden-Handel Fonds L&S Kapitalertragsteuer EUR 45,88-
                .section("tax", "currency", "label").optional()
                .match("^(?<label>.*) Kapitalertragsteuer ([\\s]+)?(?<currency>[\\w]{3})([\\s]+)? (?<tax>[.,\\d]+)[-]?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")) 
                                    && !v.get("label").trim().startsWith("im laufenden Jahr einbehaltene"))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Kapitalertragsteuer EUR 14,51
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer (?<currency>[\\w]{3}) (?<tax>[.,\\d]+)[-]?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Verwahrart Girosammelverwahrung Solidaritätszuschlag EUR 2,52-
                .section("tax", "currency", "label").optional()
                .match("^(?<label>.*) Solidarit.tszuschlag ([\\s]+)?(?<currency>[\\w]{3})([\\s]+)? (?<tax>[.,\\d]+)[-]?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")) 
                                    && !v.get("label").trim().startsWith("im laufenden Jahr einbehaltene"))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Solidaritätszuschlag EUR 0,79
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag ([\\s]+)?(?<currency>[\\w]{3})([\\s]+)? (?<tax>[.,\\d]+)[-]?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // einbehaltene Kirchensteuer EUR 0,15
                .section("tax", "currency", "label").optional()
                .match("^(?<label>.*) Kirchensteuer ([\\s]+)?(?<currency>[\\w]{3})([\\s]+)? (?<tax>[.,\\d]+)[-]?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative"))
                                    && !v.get("label").trim().startsWith("im laufenden Jahr einbehaltene"))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Kirchensteuer EUR 4,12-
                .section("tax", "currency").optional()
                .match("^Kirchensteuer ([\\s]+)?(?<currency>[\\w]{3})([\\s]+)? (?<tax>[.,\\d]+)[-]?$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("negative")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                .section("tax", "currency").optional()
                .match("^abzgl. Quellensteuer .* (\\w{3}) (?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                })

                // abzgl. Kapitalertragsteuer 25,00 % von 90,02 EUR 22,51 EUR
                .section("tax", "currency").optional()
                .match("^abzgl. Kapitalertrags(s)?teuer .* (?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                })

                // abzgl. Solidaritätszuschlag 5,50 % von 22,51 EUR 1,23 EUR
                .section("tax", "currency").optional()
                .match("^abzgl. Solidarit.tszuschlag .* (?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                })

                .section("tax", "currency").optional()
                .match("^abzgl. Kirchensteuer .* (?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    processTaxEntries(t, v, type);
                });
    }

    @SuppressWarnings("nls")
    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Handelszeit 14:15* Registrierungsspesen EUR 0,58-
                .section("fee", "currency").optional()
                .match("^.* Registrierungsspesen (?<currency>[\\w]{3}) (?<fee>[.,\\d]+)-$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Handelszeit 16:38* Provision USD 18,78-
                .section("fee", "currency").optional()
                .match("^.* Provision (?<currency>[\\w]{3}) (?<fee>[.,\\d]+)-$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Börse USA/NAN Handelsplatzentgelt USD 17,44-
                .section("fee", "currency").optional()
                .match("^.* Handelsplatzentgelt (?<currency>[\\w]{3}) (?<fee>[.,\\d]+)-$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Verwahrart Wertpapierrechnung Abwicklungskosten Ausland USD 0,10-
                .section("fee", "currency").optional()
                .match("^.* Abwicklungskosten.* (?<currency>[\\w]{3}) (?<fee>[.,\\d]+)-$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Verwahrart Girosammelverwahrung Gebühr EUR 0,50-
                .section("fee", "currency").optional()
                .match("^Verwahrart Girosammelverwahrung Geb.hr (?<currency>[\\w]{3}) (?<fee>[.,\\d]+)-$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Börse außerbörslich Bezugspreis EUR 27,00-
                .section("fee", "currency").optional()
                .match("^B.rse außerb.rslich Bezugspreis (?<currency>[\\w]{3}) (?<fee>[.,\\d]+)-$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @SuppressWarnings("nls")
    private void processTaxEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax, (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax, ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }
    
    @SuppressWarnings("nls")
    private void processFeeEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee, 
                            (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee,
                            ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }
}