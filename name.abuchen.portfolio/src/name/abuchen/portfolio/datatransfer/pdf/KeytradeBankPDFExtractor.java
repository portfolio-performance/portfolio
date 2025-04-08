package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class KeytradeBankPDFExtractor extends AbstractPDFExtractor
{
    public KeytradeBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Keytrade Bank");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Keytrade Bank";
    }

    private void addBuySellTransaction()
    {
        final DocumentType type = new DocumentType("(Kauf" //
                        + "|Achat" //
                        + "|Aankoop" //
                        + "|Verkauf" //
                        + "|Vente"
                        + "|Verkoop)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Num.ro.*)?(Kauf" //
                        + "|Achat" //
                        + "|Aankoop" //
                        + "|Vente" //
                        + "|Verkauf"
                        + "|Verkoop) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        // Is type --> "Vente" change from BUY to SELL
                        // Is type --> "Verkoop" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(Num.ro.*)?(?<type>(Kauf" //
                                        + "|Achat" //
                                        + "|Aankoop" //
                                        + "|Verkauf" //
                                        + "|Vente" //
                                        + "|Verkoop)) .*$")
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")) //
                                            || "Vente".equals(v.get("type")) //
                                            || "Verkoop".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Kauf 168 LYXOR CORE WORLD (LU1781541179) für 11,7824 EUR
                                        // Achat 310 HOME24 SE  INH O.N. (DE000A14KEB5) à 15,9324 EUR
                                        // Vente 310 VERBIO VER.BIOENERGIE ON (DE000A0JL9W6) à 33,95 EUR
                                        // Verkauf 22 SARTORIUS AG O.N. (DE0007165607) für 247 EUR
                                        // Aankoop 20 SPDR ACWI IMI (IE00B3YLTY66) aan 178,3 EUR
                                        // Verkoop 40 AMAZON.COM INC (US0231351067) aan 128,48I5 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^(Kauf|Achat|Aankoop|Verkauf|Vente|Verkoop) [\\.,\\d]+ (?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\) .* (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Numéro de réf. : 7763C9846/01AAchat 15 KINEPOLIS GROUP (BE0974274061) à 46,3 EURType d' instrument: Actions
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^Num.ro.*(Kauf|Achat|Aankoop|Verkauf|Vente|Verkoop) [\\.,\\d]+ (?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\) .* [\\.,\\d]+ (?<currency>[\\w]{3}).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Kauf 168 LYXOR CORE WORLD (LU1781541179) für 11,7824 EUR
                        // Achat 310 HOME24 SE  INH O.N. (DE000A14KEB5) à 15,9324 EUR
                        // Vente 310 VERBIO VER.BIOENERGIE ON (DE000A0JL9W6) à 33,95 EUR
                        // Verkauf 22 SARTORIUS AG O.N. (DE0007165607) für 247 EUR
                        // Numéro de réf. : 7763C9846/01AAchat 15 KINEPOLIS GROUP (BE0974274061) à 46,3 EURType d' instrument: Actions
                        // Aankoop 20 SPDR ACWI IMI (IE00B3YLTY66) aan 178,3 EUR
                        // Verkoop 40 AMAZON.COM INC (US0231351067) aan 128,48I5 USD
                        // @formatter:on
                        .section("shares")
                        .match("^.*(Kauf|Achat|Aankoop|Verkauf|Vente|Verkoop) (?<shares>[\\.,\\d]+) .* \\([A-Z]{2}[A-Z0-9]{9}[0-9]\\) .*$")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Ausführungsdatum und -zeit : 15/03/2021 12:31:50 CET
                                        // Date et heure d'exécution : 19/10/2020 11:53:03 CET
                                        // Datum en tijdstip van uitvoering: 30/11/2023 16:37:39 CET
                                        // Datum en tijdstip van uitvoering: 04/07/2023L16:23:23 CET
                                        // Geldigheid: GTCTijdstip creatie order: 02/08/2023 20:03:48 CETDatum en tijdstip van uitvoering: 02/08/2023 20:03:49 EST
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^.*(Ausf.hrungsdatum und \\-zeit" //
                                                                        + "|Date et heure d.ex.cution" //
                                                                        + "|Datum en tijdstip van uitvoering)([\\s]+)?: " //
                                                                        + "(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}).*(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Ordre créé à: 26/P11/2021L10:05:13 CETDate et heure d'exécution: 26/11/2021 13:25:24 CETDate de comptabilisation: 26/11/2021Date valeur: 30/11/2021Lieu d'exécution: EURONEXT - EURONEXT BRUSS
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Ordre cr.. .([\\s]+)?: .*Date et heure d.ex.cution([\\s]+)?: (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Ordre créé à: 05/11/2020 11:25:43 CET
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Ordre cr.. .([\\s]+)?: (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))))

                        // @formatter:off
                        // Lastschrift 1.994,39 EUR Valutadatum 17/03/2021
                        // Gutschrift 5.409,05 EUR Valutadatum 03/07/2020
                        // Crédit 10.499,55 EUR Date valeur 12/02/2021
                        // Débit 4.963,99 EUR Date valeur 21/10/2020
                        // Crédit 908,64 EUR
                        // Débit 1.502,72 EUR
                        // Credit 5.091,46 USD
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^(Gutschrift|Cr.dit|Lastschrift|D.bit|Debet) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Bruttobetrag 924,00 USD
                                        // Wechselkurs EUR/USD 1.123000 849,47 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("gross", "termCurrency", "baseCurrency", "exchangeRate") //
                                                        .match("^Bruttobetrag (?<gross>[\\.,\\d]+) [\\w]{3}.*$") //
                                                        .match("^Wechselkurs (?<termCurrency>[\\w]{3})\\/(?<baseCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Plaats van uitvoering: UBATPS Z-EXCHANGEBrutobedrag + 5.139,40 USDTransactiekosten
                                        // Wisselkoers EUR/USD 1,097
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("gross", "termCurrency", "baseCurrency", "exchangeRate") //
                                                        .match("^.*Brutobedrag \\+ (?<gross>[\\.,\\d]+) [\\w]{3}.*$") //
                                                        .match("^Wisselkoers (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getBaseCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // Auftragstyp : Limit (16 EUR)
                        // Type d' ordre : Limit (16 EUR)
                        // Type d'ordre: Market
                        // Type d'ordre: Market I
                        // Ordertype: Limit (178,30 EUR)
                        // Ordertype: Limit (128,30LUSD)
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(Auftragstyp" //
                                        + "|Type d'([\\s]+)?ordre" //
                                        + "|Ordertype)([\\s]+)?: " //
                                        + "(?<note>(Limit \\([\\.,\\d]+([\\s])?[\\w]{3,4}\\)|Market)).*$") //
                        .assign((t, v) -> t.setNote(v.get("note")))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final DocumentType type = new DocumentType("(Einkommensart" //
                        + "|Nature des revenus" //
                        + "|Coupon)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Betrag:|Montant:|Coupon).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 22 SARTORIUS AG O.N. NR 200629 0,35 EUR
                                        // Wertschriftenkonto:4/260745 Wertpapier:79010788/DE0007165607 Belegnr.: CPN / 555091
                                        // Compte-titres:4/260745 Titre:79137418/DE000A0JL9W6 Nr. Quit.: CPN / 583554
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^(?<shares>[\\.,\\d]+) (?<name>.*) NR .* [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .match("^(Wertschriftenkonto|Compte\\-titres):.* (Wertpapier|Titre):[\\d]+\\/(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // BETALING COUPON NR 221220 : 81 VANECK VECTORS ETF TRUST JUNIOR GOLD MINERS USD (US92189F7915) aan
                                        // 0,1825 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^BETALING COUPON NR .*: [\\.,\\d]+ (?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\) aan$") //
                                                        .match("^[\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // CREDIT COUPON NR 210104 : 35 TOTALENERGIES (FR0000120271) à 0,66 EUR
                                        // BETALING COUPON NR 221220 : 81 VANECK VECTORS ETF TRUST JUNIOR GOLD MINERS USD (US92189F7915) aan
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^CREDIT COUPON NR .*: [\\.,\\d]+ (?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\) .* [\\.,\\d]+ (?<currency>[\\w]{3}).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // 22 SARTORIUS AG O.N. NR 200629 0,35 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(?<shares>[\\.,\\d]+) .* NR .* [\\.,\\d]+ [\\w]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // CREDIT COUPON NR 210104 : 35 TOTALENERGIES (FR0000120271) à 0,66 EUR
                                        // BETALING COUPON NR 221220 : 81 VANECK VECTORS ETF TRUST JUNIOR GOLD MINERS USD (US92189F7915) aan
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(CREDIT|BETALING) COUPON NR .*: (?<shares>[\\.,\\d]+) .* \\([A-Z]{2}[A-Z0-9]{9}[0-9]\\).*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Nettoguthaben 5,67 EUR Datum  01/07/2020
                                        // Net CREDIT 45,65 EUR Date  03/02/2021
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(Nettoguthaben|Net CREDIT) [\\.,\\d]+ [\\w]{3} (Datum|Date) ([\\s]+)?(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Date valeur: 11/01/2021
                                        // Date valeur: 17/11/2021 C
                                        // Valuta datum: 23/12/2022
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(Date valeur|Valuta datum): (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Nettoguthaben 5,67 EUR Datum  01/07/2020
                                        // Net CREDIT 45,65 EUR Date  03/02/2021
                                        // Netto credit 8,80 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^(Nettoguthaben|Net CREDIT|Netto credit) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Net CREDIT
                                        // 33,60 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("Net CREDIT") //
                                                        .match("^(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Bruttobetrag 1,25 USD
                                        // Wechselkurs 1,05 1,01 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("gross", "baseCurrency", "exchangeRate", "termCurrency") //
                                                        .match("^Bruttobetrag (?<gross>[\\.,\\d]+) (?<baseCurrency>[\\w]{3}).*$") //
                                                        .match("^Wechselkurs (?<exchangeRate>[\\.,\\d]+) [\\.,\\d]+ (?<termCurrency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Brutobedrag + 14,78 USD
                                        // Wisselkoers 1,0622 + 12,57 USD + 11,83 EUR
                                        // Steuerpflichtige Basis + 11,83 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("gross", "termCurrency", "baseCurrency", "exchangeRate") //
                                                        .match("^Brutobedrag \\+ (?<gross>[\\.,\\d]+) (?<termCurrency>[\\w]{3}).*$") //
                                                        .match("^Wisselkoers (?<exchangeRate>[\\.,\\d]+) \\+ [\\.,\\d]+ [\\w]{3} \\+ [\\.,\\d]+ (?<baseCurrency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(rate.getBaseCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // Wertschriftenkonto:4/260745 Wertpapier:79010788/DE0007165607 Belegnr.: CPN / 555091
                        // Compte-titres:4/260745 Titre:79137418/DE000A0JL9W6 Nr. Quit.: CPN / 583554
                        // Ref. nummer : 9445863/01
                        // Numéro de réf. : 4796214/01
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>(Belegnr\\." //
                                        + "|Nr. Quit\\." //
                                        + "|Ref\\. nummer"
                                        + "|Num.ro de r.f\\.)([\\s]+)?: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                        // @formatter:off
                        // Verrechnungssteuer 26,38 % 2,03 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Verrechnungssteuer [\\.,\\d]+ % (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Impôt à la source 26,38 % 16,35 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^.*Imp.t . la source [\\.,\\d]+ % (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Impôt à la source 26,50% - 6,12 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^.*Imp.t . la source [\\.,\\d]+% \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Beurstaks - 0,12% - 4,28 EUR
                        // Beurstaks D - 29,95 USD+ 5.109,45 USD- 0,35% - 16,40 EUR
                        // Plaats van uitvoering: UEURPONEXT PARISBrutobedrag + 5.722,85 EURD + 5.722,85 EURBeurstaks - 0,35% - 20,03 EURMakelaarsloon - 24,95 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^.*Beurstaks .*\\- [\\.,\\d]+% \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Précompte mobilier belge 30,00% - 5,09 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^.*Pr.compte mobilier .* [\\.,\\d]+% \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Précompte mobilier belge
                        // Net CREDIT
                        // Pièce justificative à conserver en vue de votre dUéclaration fiscaP
                        // L 30,00% - 14,40 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .find("Pr.compte mobilier .*") //
                        .match("^.* [\\.,\\d]+% \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Taxe boursière - 0,35% - 5,22 EUR
                        // Montant brut U - 694,50 EURD - 694,50 EURTaxe boursière - 0,35% - 2,43 EURCourtage - 7,50 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^.*Taxe boursi.re \\- [\\.,\\d]+% \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Bronheffing 15,00% - 2,22 USD
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Bronheffing [\\.,\\d]+% \\- (?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Belgische roerende voorheffing 30,00% - 3,55 EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Belgische roerende voorheffing [\\.,\\d]+% \\- (?<withHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                        // @formatter:off
                        // Transaktionskosten 14,95 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Transaktionskosten (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Plaats van uitvoering: UBATPS Z-EXCHANGEBrutobedrag + 5.139,40 USDTransactiekosten
                        // Beurstaks D - 29,95 USD+ 5.109,45 USD- 0,35% - 16,40 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .find(".*Transactiekosten") //
                        .match("^Beurstaks .* \\- (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).* [\\.,\\d]+%.*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Frais de transaction 24,95 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.*Frais de transaction (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Makelaarsloon - 14,95 EUR
                        // Plaats van uitvoering: UEURPONEXT PARISBrutobedrag + 5.722,85 EURD + 5.722,85 EURBeurstaks - 0,35% - 20,03 EURMakelaarsloon - 24,95 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.*Makelaarsloon \\- (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Courtage - 7,50 EUR
                        // Montant brut U - 694,50 EURD - 694,50 EURTaxe boursière - 0,35% - 2,43 EURCourtage - 7,50 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^.*Courtage \\- (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        String language = "de";
        String country = "DE";

        int lastDot = value.lastIndexOf(".");
        int lastComma = value.lastIndexOf(",");

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en";
            country = "US";
        }

        return ExtractorUtils.convertToNumberLong(value, Values.Amount, language, country);
    }

    @Override
    protected long asShares(String value)
    {
        String language = "de";
        String country = "DE";

        int lastDot = value.lastIndexOf(".");
        int lastComma = value.lastIndexOf(",");

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en";
            country = "US";
        }

        return ExtractorUtils.convertToNumberLong(value, Values.Share, language, country);
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        String language = "de";
        String country = "DE";

        int lastDot = value.lastIndexOf(".");
        int lastComma = value.lastIndexOf(",");

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en";
            country = "US";
        }

        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, language, country);
    }
}
