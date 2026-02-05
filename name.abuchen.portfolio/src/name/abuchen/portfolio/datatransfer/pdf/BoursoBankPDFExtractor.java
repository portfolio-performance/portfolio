package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

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
public class BoursoBankPDFExtractor extends AbstractPDFExtractor
{
    public BoursoBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Boursorama S.A.");
        addBankIdentifier("Compte PEA");
        addBankIdentifier("Résident Français");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Bourso Bank / ex-Boursorama Banque";
    }

    private void addBuySellTransaction()
    {
        var type = new DocumentType("(ACHAT|SOUSCRIPTION|VENTE|REPRISE)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^.*R.sident Fran.ais.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "VENTE" change from BUY to SELL
                        // Is type --> "REPRISE" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(?<type>(ACHAT|SOUSCRIPTION|VENTE|REPRISE)).*$") //
                        .assign((t, v) -> {
                            if ("VENTE".equals(v.get("type")) || "REPRISE".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // 29/05/2024 400 ISHS VI-ISMWSPE EOA
                                        // Cours demandé : 5,0220 EUR
                                        // Code ISIN : IE0002XZSHO1 Cours exécuté : 5,0216 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\,\\d\\s]+ (?<name>.*)$") //
                                                        .match("^Cours demand. : [\\,\\d\\s]+ (?<currency>[A-Z]{3})$") //
                                                        .match("^Code ISIN : (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // 27/02/2024 1,02 AXA COURT TERME A CAP.SI.2DEC
                                        // Code ISIN : FR0000288946 Valeur liquidative : 2 464,6553 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\,\\d\\s]+ (?<name>.*)$") //
                                                        .match("^Code ISIN : (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Valeur liquidative : [\\,\\d\\s]+ (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // 29/09/2023 42 AM.PEA MSCI EM.MKTS UC.ETF FCP Référence : 493029272303
                                        // Code ISIN : FR0013412020 Cours exécuté : 20,550 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\,\\d\\s]+ (?<name>.*) R.f.rence : .*$") //
                                                        .match("^Code ISIN : (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Cours ex.cu.. : [\\,\\d\\s]+ (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // 08/01/2024 7 AM.MSCI WORLD UCITS ETF EUR C
                                        // Code ISIN : LU1681043599 Cours exécuté : 447,5239 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\,\\d\\s]+ (?<name>.*)$") //
                                                        .match("^Code ISIN : (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Cours ex.cu.. : [\\,\\d\\s]+ (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // 29/05/2024 400 ISHS VI-ISMWSPE EOA
                        // @formatter:on
                        .section("shares") //
                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{4} (?<shares>[\\,\\d\\s]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 15:38:46
                        // @formatter:on
                        .section("time").optional() //
                        .match("^(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        // @formatter:off
                        // 29/05/2024 400 ISHS VI-ISMWSPE EOA
                        // @formatter:on
                        .section("date") //
                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) [\\,\\d\\s]+ .*$") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().get("time") != null)
                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                            else
                                t.setDate(asDate(v.get("date")));
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Montant net au débit de votre compte
                                        // 2 008,64 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("Montant .*")
                                                        .match("^(?<amount>[\\d\\s]+,[\\d]{2}) (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Montant brut Commission Frais (¨) Montant net au débit de votre compte
                                        // 965,02 EUR 4,83 EUR  969,85 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("Montant .*")
                                                        .match("^[\\d\\s]+,[\\d]{2} [A-Z]{3} [\\d\\s]+,[\\d]{2} [A-Z]{3} (?<amount>[\\d\\s]+,[\\d]{2}) (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Montant brut Droits d'entrée Frais H.T. T.V.A. Montant net au débit de votre compte
                                        // 2 513,95 EUR 0,00 EUR 0,00 EUR  2 513,95 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("Montant .*")
                                                        .match("^[\\d\\s]+,[\\d]{2} [A-Z]{3} [\\d\\s]+,[\\d]{2} [A-Z]{3} [\\d\\s]+,[\\d]{2} [A-Z]{3} (?<amount>[\\d\\s]+,[\\d]{2}) (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Montant transaction brut Intérêts total brut Courtages Montant transaction net
                                        // 200,40 USD 0,00 USD 200,40 USD 0,00 USD 200,40 USD
                                        // 000 jours
                                        // ACHAT DEVISES A TERME Cours de change Montant transaction net contrevalorisé
                                        // Contrevalorisation en EURO 1,06491900 188,18 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "termCurrency", "exchangeRate", "baseCurrency") //
                                                        .find("Montant transaction brut.*") //
                                                        .match("^(?<fxGross>[\\d\\s]+,[\\d]{2}) (?<termCurrency>[A-Z]{3}) .*$") //
                                                        .match("^Contrevalorisation en [\\w]+ (?<exchangeRate>[\\.,\\d]+) [\\d\\s]+,[\\d]{2} (?<baseCurrency>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            var gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Type d'ordre : à cours limite
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^Type d.ordre : (?<note>.*)$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))),
                                        // @formatter:off
                                        // 12:36:04 Type d'ordre : à cours limiteCours demandé : 20,5500 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*Type d.ordre : (?<note>.* [\\,\\d\\s]+ [A-Z]{3})$")
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))))

                        // @formatter:off
                        // Référence : 398406803961
                        // 29/09/2023 42 AM.PEA MSCI EM.MKTS UC.ETF FCP Référence : 493029272303
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>R.f.rence : .*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | ")))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        var type = new DocumentType("COUPONS");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^.* COUPONS$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // 15/02/2024 248 ISHS DEV MK PRO US (IE00B1FZS350) 40,33 12,09 28,24 28,24
                        // COUPONS : NETS FISCAUX  28,24 EUR
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\,\\d\\s]+ (?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\).*$") //
                        .match("^COUPONS : NETS FISCAUX [\\,\\d\\s]+ (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 15/02/2024 248 ISHS DEV MK PRO US (IE00B1FZS350) 40,33 12,09 28,24 28,24
                        // @formatter:on
                        .section("shares") //
                        .match("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{4} (?<shares>[\\,\\d\\s]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 15/02/2024 248 ISHS DEV MK PRO US (IE00B1FZS350) 40,33 12,09 28,24 28,24
                        // @formatter:on
                        .section("date") //
                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) [\\,\\d\\s]+ .*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // COUPONS : NETS FISCAUX  28,24 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^COUPONS : NETS FISCAUX (?<amount>[\\d\\s]+,[\\d]{2}) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Revenus d'actions étrangères sans abattement
                        // TOTAL EUR 0,00 12,09 28,24 0,00 28,24
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .find("Revenus.*")
                        .match("^TOTAL (?<currency>[A-Z]{3}) [\\,\\d\\s]+ (?<tax>[\\,\\d\\s]+) [\\,\\d\\s]+ [\\,\\d\\s]+ [\\,\\d\\s]+$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Montant brut Commission Frais (¨) Montant net au débit de votre compte
                        // 965,02 EUR 4,83 EUR  969,85 EU
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .find("Montant.*")
                        .match("^[\\d\\s]+,[\\d]{2} [A-Z]{3} (?<fee>[\\d\\s]+,[\\d]{2}) (?<currency>[A-Z]{3}) [\\d\\s]+,[\\d]{2} [A-Z]{3}$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Commission Frais divers Montant total des frais
                        // 6,95 EUR 0,00 EUR 6,95 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .find("Commission Frais divers.*")
                        .match("^(?<fee>[\\d\\s]+,[\\d]{2}) (?<currency>[A-Z]{3}) [\\d\\s]+,[\\d]{2} [A-Z]{3} [\\d\\s]+,[\\d]{2} [A-Z]{3}$") //
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