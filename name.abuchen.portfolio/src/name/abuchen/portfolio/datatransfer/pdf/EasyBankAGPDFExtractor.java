package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

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

@SuppressWarnings("nls")
public class EasyBankAGPDFExtractor extends AbstractPDFExtractor
{
    public EasyBankAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("easybank Service Center"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendTransaction();
        addDepotStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Easybank AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Gesch.ftsart: (Kauf|Verkauf|Kauf aus Dauerauftrag)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Wir haben f.r Sie am [\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4} unten angef.hrtes Gesch.ft abgerechnet:$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Gesch.ftsart: (?<type>(Kauf|Verkauf|Kauf aus Dauerauftrag)).*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // Titel: DE000A0F5UK5  i S h . S T . E u . 6 00 Bas.Res.U.ETF DE 
                // Inhaber-Anlageaktien               
                // Kurs: 66,88 EUR 
                .section("isin", "name", "name1", "currency")
                .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^Kurs: [\\-\\.,\\d]+ (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Kurs"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                .oneOf(
                                // Handelszeit: 13.6.2022 um 09:04:00 Uhr
                                section -> section
                                        .attributes("date", "time")
                                        .match("^Handelszeit: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}) .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // Schlusstag/-zeit: 05.07.2021 09:04:16
                                section -> section
                                        .attributes("date", "time")
                                        .match("^Schlusstag\\/\\-zeit: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // Handelszeit: 11.11.2022
                                section -> section
                                        .attributes("date")
                                        .match("^Handelszeit: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4})$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                        )

                .oneOf(
                                // Zugang: 6,94 Stk
                                section -> section
                                        .attributes("shares")
                                        .match("^(Zugang|Abgang): (?<shares>[\\.,\\d]+) Stk.*$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // Zugang: Stk 100
                                section -> section
                                        .attributes("shares")
                                        .match("^(Zugang|Abgang): Stk (?<shares>[\\.,\\d]+).*$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                // Zu Lasten IBAN AT00 0000 0000 0000 0000 -468,43 EUR 
                .section("amount", "currency")
                .match("^(Zu Lasten|Zu Gunsten) .* (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .section("termCurrency", "fxGross", "fxCurrency", "exchangeRate", "baseCurrency").optional()
                .match("^Kurs: [\\-\\.,\\d]+ (?<termCurrency>[\\w]{3}).*$")
                .match("^Kurswert: (?<fxGross>[\\-\\.,\\d]+) (?<fxCurrency>[\\w]{3}).*$")
                .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}\\) [\\-\\.,\\d]+ (?<baseCurrency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(asExchangeRate(v));

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .optionalOneOf(
                                //  Auftrags-Nr.: 00000000-3.5.2017
                                section -> section
                                        .attributes("note")
                                        .match("^(?<note>Auftrags-Nr\\.: .*)$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note"))))
                                ,
                                // Geschäftsart: Verkauf Auftrags-Nr.: 25866072 - 04.01.2022
                                section -> section
                                        .attributes("note")
                                        .match("^.* (?<note>Auftrags-Nr\\.: .*) \\- .*$")
                                        .assign((t, v) -> t.setNote(trim(v.get("note"))))
                        )

                .optionalOneOf(
                                //  Limit: Stoplimit: 2.725,000000
                                section -> section
                                        .attributes("note")
                                        .match("^Limit: (?<note>.*: .*)$")
                                        .assign((t, v) -> {
                                            if (t.getPortfolioTransaction().getNote() == null)
                                                t.setNote(trim(v.get("note")));
                                            else
                                                t.setNote(t.getPortfolioTransaction().getNote() + " | " + trim(v.get("note"))); //$NON-NLS-1$
                                        })
                                ,
                                // Limit: 42,500000
                                section -> section
                                        .attributes("note")
                                        .match("^(?<note>Limit: .*)$")
                                        .assign((t, v) -> {
                                            if (t.getPortfolioTransaction().getNote() == null)
                                                t.setNote(trim(v.get("note")));
                                            else
                                                t.setNote(t.getPortfolioTransaction().getNote() + " | " + trim(v.get("note"))); //$NON-NLS-1$
                                        })
                        )

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Gesch.ftsart: Ertrag");
        this.addDocumentTyp(type);

        Block block = new Block("^Wir haben f.r Sie am [\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4} unten angef.hrtes Gesch.ft abgerechnet:$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // Titel: AT0000APOST4  O E S T E R R E I C H ISCHE POST AG  
                // AKTIEN O.N.
                // Dividende: 1,9 EUR 
                .section("isin", "name", "name1", "currency")
                .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$")
                .match("^(?<name1>.*)$")
                .match("^(Dividende|Ertrag): [\\.,\\d]+ (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Dividende") || !v.get("name1").startsWith("Ertrag"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                .oneOf(
                                // 100 Stk
                                section -> section
                                        .attributes("shares")
                                        .match("^(?<shares>[\\.,\\d]+) Stk.*$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // Bestand: Stk 1.200
                                section -> section
                                        .attributes("shares")
                                        .match("^Bestand: Stk (?<shares>[\\.,\\d]+).*$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                .oneOf(
                                // Valuta 5.5.2022
                                section -> section
                                        .attributes("date")
                                        .match("^Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}).*$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                                ,
                                // Zu Gunsten IBAN AT11 1111 1111 1111 1111 Valuta 07.06.2022 478,50 EUR
                                section -> section
                                        .attributes("date")
                                        .match("^Zu Gunsten .* Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}).*$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                        )

                // Zu Gunsten IBAN AT12 1234 1234 1234 1234 123,75 EUR 
                .section("amount", "currency")
                .match("^Zu Gunsten .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .optionalOneOf(
                                // Ertrag: 0,279 USD 
                                // Bruttoertrag: 336,16 USD 
                                // Devisenkurs: 1,06145 (11.5.2022) 316,70 EUR
                                section -> section
                                        .attributes("termCurrency","fxGross", "fxCurrency", "exchangeRate", "currency")
                                        .match("^(Dividende|Ertrag): [\\.,\\d]+ (?<termCurrency>[\\w]{3}).*$")
                                        .match("^Bruttoertrag: (?<fxGross>[\\-\\.,\\d]+) (?<fxCurrency>[\\w]{3}).*$")
                                        .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}\\) [\\-\\.,\\d]+ (?<currency>[\\w]{3}).*$")
                                        .assign((t, v) -> {
                                            v.put("baseCurrency", asCurrencyCode(v.get("currency")));

                                            PDFExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(asExchangeRate(v));

                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                                            Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type);
                                        })
                                ,
                                // Dividende: 1 USD
                                // Ertrag: 16,00 USD
                                // Devisenkurs: 0,99975 (31.10.2022) 11,60 EUR
                                section -> section
                                        .attributes("termCurrency", "fxGross", "fxCurrency", "exchangeRate", "currency")
                                        .match("^Dividende: [\\.,\\d]+ (?<termCurrency>[\\w]{3}).*$")
                                        .match("^Ertrag: (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}).*$")
                                        .match("^Devisenkurs: (?<exchangeRate>[\\.,\\d]+) \\([\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}\\) [\\-\\.,\\d]+ (?<currency>[\\w]{3}).*$")
                                        .assign((t, v) -> {
                                            v.put("baseCurrency", asCurrencyCode(v.get("currency")));

                                            PDFExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(asExchangeRate(v));

                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                                            Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type);
                                        })
                        )

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addDepotStatementTransaction()
    {
        final DocumentType type = new DocumentType("KONTOAUSZUG", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("^.* (?<currency>[\\w]{3}) [\\.,\\d]+$");
            Pattern pYear = Pattern.compile("^Alter Saldo per [\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4}) [\\.,\\d]+$");

            for (String line : lines)
            {
                Matcher m = pCurrency.matcher(line);
                if (m.matches())
                    context.put("currency", m.group("currency"));

                m = pYear.matcher(line);
                if (m.matches())
                    context.put("year", m.group("year"));
            }
        });
        this.addDocumentTyp(type);

        Block depositBlock = new Block("^[\\d]{2}\\.[\\d]{2} .* [\\d]{2}\\.[\\d]{2} [\\.,\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.DEPOSIT);
                    return t;
                })

                // 28.06 Mustermann 28.06 2.000,00
                // IBAN: AT12 1234 1234 1234 1234
                // REF: 38000220627-5336530-0000561
                .section("date", "amount", "note").optional()
                .match("^(?<date>[\\d]{2}\\.[\\d]{2}) .* [\\d]{2}\\.[\\d]{2} (?<amount>[\\.,\\d]+)$")
                .match("^IBAN: .*$")
                .match("^(?<note>REF: .*)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date") + "." + context.get("year")));

                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        Block feeBlock = new Block("^[\\d]{2}\\.[\\d]{2} Abschluss [\\d]{2}\\.[\\d]{2} [\\.,\\d]+\\-$");
        type.addBlock(feeBlock);
        feeBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.FEES);
                    return t;
                })

                // 30.09 Abschluss 30.09 4,50-
                // Kontoführungsgebühr                          4,50-
                .section("date", "amount", "note").optional()
                .match("^(?<date>[\\d]{2}\\.[\\d]{2}) Abschluss [\\d]{2}\\.[\\d]{2} (?<amount>[\\.,\\d]+)\\-$")
                .match("^(?<note>Kontof.hrungsgeb.hr) .* [\\.,\\d]+\\-$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date") + "." + context.get("year")));

                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Kapitalertragsteuer: -52,25 EUR 
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer: \\-(?<tax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // KESt aus Neubestand: -93,23 EUR
                .section("tax", "currency").optional()
                .match("^KESt aus Neubestand: \\-(?<tax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Auslands-KESt neu: -0,83 EUR
                .section("tax", "currency").optional()
                .match("^Auslands\\-KESt neu: \\-(?<tax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                .section("tax", "currency").optional()
                .match("^KESt Ausl.ndische Dividende: \\-(?<tax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Quellensteuer: -327,58 EUR 
                .section("withHoldingTax", "currency").optional()
                .match("^Quellensteuer: \\-(?<withHoldingTax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Quellensteuer US-Emittent: -54,80 USD 
                .section("withHoldingTax", "currency").optional()
                .match("^Quellensteuer US\\-Emittent: \\-(?<withHoldingTax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Umsatzsteuer: -0,62 EUR 
                .section("tax", "currency").optional()
                .match("^Umsatzsteuer: \\-(?<tax>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Fremde Börsespesen: -3,47 EUR
                .section("fee", "currency").optional()
                .match("^Fremde B.rsespesen: \\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Fremde Settlementspesen: -0,24 EUR
                .section("fee", "currency").optional()
                .match("^Fremde Settlementspesen: \\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Fremde Spesen: -2,86 EUR 
                .section("fee", "currency").optional()
                .match("^Fremde Spesen: \\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Eigene Spesen: -1,28 EUR 
                .section("fee", "currency").optional()
                .match("^Eigene Spesen: \\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Devisenprovision: -0,04 EUR
                .section("fee", "currency").optional()
                .match("^Devisenprovision: \\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Grundgebühr: -3,-- EUR 
                // Grundgebühr: -7,95 EUR 
                .section("fee", "currency").optional()
                .match("^Grundgeb.hr: \\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Inkassoprovision: -3,11 EUR
                .section("fee", "currency").optional()
                .match("^Inkassoprovision: \\-(?<fee>[\\-\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
