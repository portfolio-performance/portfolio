package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class KeytradeBankPDFExtractor extends AbstractPDFExtractor
{
    public KeytradeBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Keytrade Bank"); //$NON-NLS-1$

        addBuySellTransaction();
        addBuySellWithWatermarkTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Keytrade Bank"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Kauf|Achat|Vente|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Kauf|Achat|Vente|Verkauf) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(?<type>(Kauf|Achat|Vente|Verkauf)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf") || v.get("type").equals("Vente"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Kauf 168 LYXOR CORE WORLD (LU1781541179) für 11,7824 EUR
                // Achat 310 HOME24 SE  INH O.N. (DE000A14KEB5) à 15,9324 EUR
                // Vente 310 VERBIO VER.BIOENERGIE ON (DE000A0JL9W6) à 33,95 EUR
                // Verkauf 22 SARTORIUS AG O.N. (DE0007165607) für 247 EUR
                .section("isin", "name", "shares", "currency")
                .match("^(Kauf|Achat|Verkauf|Vente) (?<shares>[\\.,\\d]+) (?<name>.*) \\((?<isin>[\\w]{12})\\) .* (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Ausführungsdatum und -zeit : 15/03/2021 12:31:50 CET
                // Ordre créé à : 10/02/2021 15:23:42 CET
                // Ordre créé à: 27/01/2021 14:37:13 CET
                .section("date", "time")
                .match("^(Ausf.hrungsdatum und \\-zeit|Ordre cr.. .)(\\s)?: (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date").replaceAll("/", "."), v.get("time"))))

                // Lastschrift 1.994,39 EUR Valutadatum 17/03/2021
                // Gutschrift 5.409,05 EUR Valutadatum 03/07/2020
                // Crédit 10.499,55 EUR Date valeur 12/02/2021
                // Débit 4.963,99 EUR Date valeur 21/10/2020
                // Crédit 908,64 EUR
                // Débit 1.502,72 EUR
                .section("amount", "currency")
                .match("^(Gutschrift|Cr.dit|Lastschrift|D.bit) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Wechselkurs EUR/USD 1.123000 849,47 EUR
                // Lastschrift 953,95 USD Valutadatum 21/06/2016
                .section("amount", "currency", "exchangeRate", "forexCurrency", "forexAmount").optional()
                .match("^Wechselkurs [\\w]{3}\\/[\\w]{3} (?<exchangeRate>[\\.,\\d]+) (?<forexAmount>[\\.,\\d]+) (?<forexCurrency>[\\w]{3})$")
                .match("^(Gutschrift|Cr.dit|Lastschrift|D.bit) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> {
                     Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forexAmount")));
                     BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                     Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                    
                     // only add gross value with forex if the security
                     // is actually denoted in the foreign currency
                     // (often users actually have the quotes in their
                     // home country currency)
                     if (forex.getCurrencyCode()
                                     .equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                     {
                         t.getPortfolioTransaction()
                                         .addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate));
                     }
                })

                // Auftragstyp : Limit (16 EUR)
                // Type d' ordre : Limit (16 EUR)
                // Type d'ordre: Market
                .section("note").optional()
                .match("^(Auftragstyp|Type d'(\\s)?ordre)(\\s)?: (?<note>.*)$")
                .assign((t, v) -> t.setNote(v.get("note")))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addBuySellWithWatermarkTransaction()
    {
        DocumentType type = new DocumentType("Num.ro.*(Achat|Vente)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Num.ro.*(Achat|Vente) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Vente" change from BUY to SELL
                .section("type").optional()
                .match("^Num.ro.*(?<type>(Achat|Vente)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Vente"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Numéro de réf. : 7763C9846/01AAchat 15 KINEPOLIS GROUP (BE0974274061) à 46,3 EURType d' instrument: Actions
                .section("isin", "name", "shares", "currency").optional()
                .match("^Num.ro.*(Achat|Vente) (?<shares>[\\.,\\d]+) (?<name>.*) \\((?<isin>[\\w]{12})\\) .* (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                .oneOf(
                                // // Ordre créé à: 26/P11/2021L10:05:13 CETDate et heure d'exécution: 26/11/2021 13:25:24 CETDate de comptabilisation: 26/11/2021Date valeur: 30/11/2021Lieu d'exécution: EURONEXT - EURONEXT BRUSS
                                section -> section
                                        .attributes("date", "time")
                                        .match("^Ordre cr.. .(\\s)?: .*Date et heure d.ex.cution(\\s)?: (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date").replaceAll("/", "."), v.get("time"))))
                                ,
                                // Ordre créé à: 05/11/2020 11:25:43 CET
                                section -> section
                                        .attributes("date", "time")
                                        .match("^Ordre cr.. .(\\s)?: (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date").replaceAll("/", "."), v.get("time"))))
                        )

                // Débit 704,43 EUR
                .section("amount", "currency")
                .match("^(Cr.dit|D.bit) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Type d'ordre: Limit (46,30 EUR) I
                .section("note").optional()
                .match("^Type d'(\\s)?ordre(\\s)?: (?<note>.*) .*$")
                .assign((t, v) -> t.setNote(v.get("note")))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Einkommensart|Nature des revenus)");
        this.addDocumentTyp(type);

        Block block = new Block("^(Einkommensart|Nature des revenus): .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // 22 SARTORIUS AG O.N. NR 200629 0,35 EUR
                // Wertschriftenkonto:4/260745 Wertpapier:79010788/DE0007165607 Belegnr.: CPN / 555091
                // Compte-titres:4/260745 Titre:79137418/DE000A0JL9W6 Nr. Quit.: CPN / 583554
                .section("shares", "name", "isin", "currency")
                .match("^(?<shares>[\\.,\\d]+) (?<name>.*) NR .* [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .match("^(Wertschriftenkonto|Compte-titres):.* (Wertpapier|Titre):[\\d]+\\/(?<isin>[\\w]{12}) .*$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Guthaben Kupons Ex-Kupon 29/06/2020
                // CREDIT COUPONS Ex-coupon 01/02/2021
                .section("date")
                .match("^.* (Ex-Kupon|Ex-coupon|Kupons)(.*)? (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date").replaceAll("/", "."))))

                // Nettoguthaben 5,67 EUR Datum  01/07/2020
                .section("amount", "currency").optional()
                .match("^(Nettoguthaben|Net CREDIT) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })
                
                // Bruttobetrag 1,25 USD
                // Wechselkurs 1,05 1,01 EUR
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match("^Wechselkurs (?<exchangeRate>[\\.,\\d]+) (?<fxAmount>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                .match("^(Nettoguthaben|Net CREDIT) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> {
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
                            Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),asAmount(v.get("fxAmount")));
                            Money amount = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        else
                        {
                            Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));
                            Money fxAmount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        t.addUnit(grossValue);
                    }
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Verrechnungssteuer 26,38 % 2,03 EUR
                .section("tax", "currency").optional()
                .match("^Verrechnungssteuer [\\.,\\d]+ % (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Impôt à la source 26,38 % 16,35 EUR
                .section("tax", "currency").optional()
                .match("(^.* .*|^)Imp.t . la source [\\.,\\d]+ % (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Taxe boursière - 0,35% - 5,22 EUR
                // Montant brut U - 694,50 EURD - 694,50 EURTaxe boursière - 0,35% - 2,43 EURCourtage - 7,50 EUR
                .section("tax", "currency").optional()
                .match("(^.* .*|^)Taxe boursi.re \\- [\\.,\\d]+% \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Transaktionskosten 14,95 EUR
                .section("fee", "currency").optional()
                .match("^Transaktionskosten (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Frais de transaction 24,95 EUR
                .section("fee", "currency").optional()
                .match("(^.* .*|^)Frais de transaction (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Courtage - 7,50 EUR
                // Montant brut U - 694,50 EURD - 694,50 EURTaxe boursière - 0,35% - 2,43 EURCourtage - 7,50 EUR
                .section("fee", "currency").optional()
                .match("(^.* .*|^)Courtage \\- (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})(.*)?$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

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
