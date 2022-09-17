package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetGrossUnit;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        addBankIdentifier("Keytrade Bank"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Keytrade Bank"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        final DocumentType type = new DocumentType("(Kauf|Achat|Vente|Verkauf)", (context, lines) -> {
            Pattern pTransaction = Pattern.compile("^Num.ro.*(Achat|Vente) (?<shares>[\\.,\\d]+) (?<name>.*) \\((?<isin>[\\w]{12})\\) .* [\\.,\\d]+ (?<currency>[\\w]{3}).*$");
            // read the current context here

            // This is the section we need,
            // if the securities information and shares
            // are outside the block.
            for (String line : lines)
            {
                Matcher m = pTransaction.matcher(line);
                if (m.matches())
                {
                    context.put("shares", m.group("shares"));
                    context.put("name", m.group("name"));
                    context.put("isin", m.group("isin"));
                    context.put("currency", m.group("currency"));
                }
            }
        });
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Num.ro.*)?(Kauf|Achat|Vente|Verkauf) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(Num.ro.*)?(?<type>(Kauf|Achat|Vente|Verkauf)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf") || v.get("type").equals("Vente"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                .oneOf(
                                // Kauf 168 LYXOR CORE WORLD (LU1781541179) für 11,7824 EUR
                                // Achat 310 HOME24 SE  INH O.N. (DE000A14KEB5) à 15,9324 EUR
                                // Vente 310 VERBIO VER.BIOENERGIE ON (DE000A0JL9W6) à 33,95 EUR
                                // Verkauf 22 SARTORIUS AG O.N. (DE0007165607) für 247 EUR
                                section -> section
                                        .attributes("shares", "name", "isin", "currency")
                                        .match("^(Kauf|Achat|Verkauf|Vente) (?<shares>[\\.,\\d]+) (?<name>.*) \\((?<isin>[\\w]{12})\\) .* (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setShares(asShares(v.get("shares")));
                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                                ,
                                // Numéro de réf. : 7763C9846/01AAchat 15 KINEPOLIS GROUP (BE0974274061) à 46,3 EURType d' instrument: Actions
                                section -> section
                                        .attributes("securityIsOutsideTheBlock")
                                        .match("^(?<securityIsOutsideTheBlock>Num.ro.*)([\\s]+)?: .*$")
                                        .assign((t, v) -> {
                                            Map<String, String> context = type.getCurrentContext();

                                            if (t.getPortfolioTransaction().getShares() == 0L)
                                            {
                                                t.setShares(asShares(context.get("shares")));
                                                v.put("name", context.get("name"));
                                                v.put("isin", context.get("isin"));
                                                v.put("currency", context.get("currency"));
                                                t.setSecurity(getOrCreateSecurity(v));
                                            }
                                        })
                        )

                .oneOf(
                                // Ausführungsdatum und -zeit : 15/03/2021 12:31:50 CET
                                // Date et heure d'exécution : 19/10/2020 11:53:03 CET 
                                section -> section
                                        .attributes("date", "time")
                                        .match("^(Ausf.hrungsdatum und \\-zeit|Date et heure d.ex.cution)([\\s]+)?: (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // Ordre créé à: 26/P11/2021L10:05:13 CETDate et heure d'exécution: 26/11/2021 13:25:24 CETDate de comptabilisation: 26/11/2021Date valeur: 30/11/2021Lieu d'exécution: EURONEXT - EURONEXT BRUSS
                                section -> section
                                        .attributes("date", "time")
                                        .match("^Ordre cr.. .([\\s]+)?: .*Date et heure d.ex.cution([\\s]+)?: (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // Ordre créé à: 05/11/2020 11:25:43 CET
                                section -> section
                                        .attributes("date", "time")
                                        .match("^Ordre cr.. .([\\s]+)?: (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                        )

                // Lastschrift 1.994,39 EUR Valutadatum 17/03/2021
                // Gutschrift 5.409,05 EUR Valutadatum 03/07/2020
                // Crédit 10.499,55 EUR Date valeur 12/02/2021
                // Débit 4.963,99 EUR Date valeur 21/10/2020
                // Crédit 908,64 EUR
                // Débit 1.502,72 EUR
                .section("amount", "currency")
                .match("^(Gutschrift|Cr.dit|Lastschrift|D.bit) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Bruttobetrag 924,00 USD
                // Wechselkurs EUR/USD 1.123000 849,47 EUR
                .section("gross", "currency", "termCurrency", "baseCurrency", "exchangeRate", "fxCurrency").optional()
                .match("^Bruttobetrag (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .match("^Wechselkurs (?<termCurrency>[\\w]{3})\\/(?<baseCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+) [\\.,\\d]+ (?<fxCurrency>[\\w]{3})$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                // Auftragstyp : Limit (16 EUR)
                // Type d' ordre : Limit (16 EUR)
                // Type d'ordre: Market
                // Type d'ordre: Market I
                .section("note").optional()
                .match("^(Auftragstyp|Type d'([\\s]+)?ordre)([\\s]+)?: (?<note>(Limit \\([\\.,\\d]+ [\\w]{3}\\)|Market)).*$")
                .assign((t, v) -> t.setNote(v.get("note")))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final DocumentType type = new DocumentType("(Einkommensart|Nature des revenus)", (context, lines) -> {
            Pattern pTransaction = Pattern.compile("^CREDIT COUPON NR .*: (?<shares>[\\.,\\d]+) (?<name>.*) \\((?<isin>[\\w]{12})\\) .* [\\.,\\d]+ (?<currency>[\\w]{3}).*$");
            // read the current context here

            // This is the section we need,
            // if the securities information and shares
            // are outside the block.
            for (String line : lines)
            {
                Matcher m = pTransaction.matcher(line);
                if (m.matches())
                {
                    context.put("shares", m.group("shares"));
                    context.put("name", m.group("name"));
                    context.put("isin", m.group("isin"));
                    context.put("currency", m.group("currency"));
                }
            }
        });
        this.addDocumentTyp(type);

        Block block = new Block("^(Einkommensart|Nature des revenus)([\\s]+)?: .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                .oneOf(
                                // 22 SARTORIUS AG O.N. NR 200629 0,35 EUR
                                // Wertschriftenkonto:4/260745 Wertpapier:79010788/DE0007165607 Belegnr.: CPN / 555091
                                // Compte-titres:4/260745 Titre:79137418/DE000A0JL9W6 Nr. Quit.: CPN / 583554
                                section -> section
                                        .attributes("shares", "name", "isin", "currency")
                                        .match("^(?<shares>[\\.,\\d]+) (?<name>.*) NR .* [\\.,\\d]+ (?<currency>[\\w]{3})$")
                                        .match("^(Wertschriftenkonto|Compte\\-titres):.* (Wertpapier|Titre):[\\d]+\\/(?<isin>[\\w]{12}) .*$")
                                        .assign((t, v) -> {
                                            t.setShares(asShares(v.get("shares")));
                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                                ,
                                // CREDIT COUPON NR 210104 : 35 TOTALENERGIES (FR0000120271) à 0,66 EUR
                                section -> section
                                        .attributes("securityIsOutsideTheBlock")
                                        .match("^(?<securityIsOutsideTheBlock>Nature des revenus)([\\s]+)?: .*$")
                                        .assign((t, v) -> {
                                            Map<String, String> context = type.getCurrentContext();

                                            if (t.getShares() == 0L)
                                            {
                                                t.setShares(asShares(context.get("shares")));
                                                v.put("name", context.get("name"));
                                                v.put("isin", context.get("isin"));
                                                v.put("currency", context.get("currency"));
                                                t.setSecurity(getOrCreateSecurity(v));
                                            }
                                        })
                        )

                .oneOf(
                                // Nettoguthaben 5,67 EUR Datum  01/07/2020
                                // Net CREDIT 45,65 EUR Date  03/02/2021
                                section -> section
                                        .attributes("date")
                                        .match("^(Nettoguthaben|Net CREDIT) [\\.,\\d]+ [\\w]{3} (Datum|Date) ([\\s]+)?(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4})$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                                ,
                                // Date valeur: 11/01/2021
                                // Date valeur: 17/11/2021 C
                                section -> section
                                        .attributes("date")
                                        .match("^Date valeur([\\s]+)?: (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}).*$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                        )

                .oneOf(
                                // Nettoguthaben 5,67 EUR Datum  01/07/2020
                                // Net CREDIT 45,65 EUR Date  03/02/2021
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^(Nettoguthaben|Net CREDIT) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                                ,
                                // Net CREDIT
                                // 33,60 EUR
                                section -> section
                                        .attributes("amount", "currency")
                                        .find("Net CREDIT")
                                        .match("^(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                        )

                // Bruttobetrag 1,25 USD
                // Wechselkurs 1,05 1,01 EUR
                .section("gross", "currency", "exchangeRate", "fxCurrency").optional()
                .match("^Bruttobetrag (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .match("^Wechselkurs (?<exchangeRate>[\\.,\\d]+) [\\.,\\d]+ (?<fxCurrency>[\\w]{3})$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(v.get("currency")));
                    v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
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
                .match("(^.* .*|^)Imp.t . la source [\\.,\\d]+ % (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Impôt à la source 26,50% - 6,12 EUR
                .section("tax", "currency").optional()
                .match("(^.* .*|^)Imp.t . la source [\\.,\\d]+% \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Précompte mobilier belge 30,00% - 5,09 EUR
                .section("tax", "currency").optional()
                .match("(^.* .*|^)Pr.compte mobilier .* [\\.,\\d]+% \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Précompte mobilier belge
                // Net CREDIT
                // Pièce justificative à conserver en vue de votre dUéclaration fiscaP
                // L 30,00% - 14,40 EUR
                .section("tax", "currency").optional()
                .match("^Pr.compte mobilier .*$")
                .match("^.* [\\.,\\d]+% \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Taxe boursière - 0,35% - 5,22 EUR
                // Montant brut U - 694,50 EURD - 694,50 EURTaxe boursière - 0,35% - 2,43 EURCourtage - 7,50 EUR
                .section("tax", "currency").optional()
                .match("(^.* .*|^)Taxe boursi.re \\- [\\.,\\d]+% \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
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
                .match("(^.* .*|^)Frais de transaction (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Courtage - 7,50 EUR
                // Montant brut U - 694,50 EURD - 694,50 EURTaxe boursière - 0,35% - 2,43 EURCourtage - 7,50 EUR
                .section("fee", "currency").optional()
                .match("(^.* .*|^)Courtage \\- (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        String language = "de"; //$NON-NLS-1$
        String country = "DE"; //$NON-NLS-1$

        int lastDot = value.lastIndexOf("."); //$NON-NLS-1$
        int lastComma = value.lastIndexOf(","); //$NON-NLS-1$

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en"; //$NON-NLS-1$
            country = "US"; //$NON-NLS-1$
        }

        return PDFExtractorUtils.convertToNumberLong(value, Values.Amount, language, country);
    }

    @Override
    protected long asShares(String value)
    {
        String language = "de"; //$NON-NLS-1$
        String country = "DE"; //$NON-NLS-1$

        int lastDot = value.lastIndexOf("."); //$NON-NLS-1$
        int lastComma = value.lastIndexOf(","); //$NON-NLS-1$

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en"; //$NON-NLS-1$
            country = "US"; //$NON-NLS-1$
        }

        return PDFExtractorUtils.convertToNumberLong(value, Values.Share, language, country);
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        String language = "de"; //$NON-NLS-1$
        String country = "DE"; //$NON-NLS-1$

        int lastDot = value.lastIndexOf("."); //$NON-NLS-1$
        int lastComma = value.lastIndexOf(","); //$NON-NLS-1$

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en"; //$NON-NLS-1$
            country = "US"; //$NON-NLS-1$
        }

        return PDFExtractorUtils.convertToNumberBigDecimal(value, Values.Share, language, country);
    }
}
