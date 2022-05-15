package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetFee;
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
public class DeutscheBankPDFExtractor extends AbstractPDFExtractor
{
    public DeutscheBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Deutsche Bank"); //$NON-NLS-1$
        addBankIdentifier("DB Privat- und Firmenkundenbank AG"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Deutsche Bank Privat- und Geschäftskunden AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Abrechnung: (Kauf|Verkauf) von Wertpapieren");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Abrechnung: (Kauf|Verkauf) von Wertpapieren$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Abrechnung: (?<type>Verkauf) von Wertpapieren$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // 123 1234567 00 BASF SE
                // WKN BASF11 Nominal ST 19
                // ISIN DE000BASF111 Kurs EUR 35,00
                .section("name", "wkn", "isin", "currency")
                .match("^[\\d]{3} [\\d]+ [\\d]{2} (?<name>.*)$")
                .match("^WKN (?<wkn>.*) Nominal ST [\\.,\\d]+$")
                .match("^ISIN (?<isin>[\\w]{12}) Kurs (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // WKN BASF11 Nominal ST 19
                .section("shares")
                .match("^.* Nominal ST (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                .oneOf(
                                // Belegnummer 1694278628 / 24281 Schlusstag 20.08.2019
                                section -> section
                                        .attributes("date")
                                        .match("^Belegnummer .* Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                                ,
                                // Belegnummer 1234567890 / 123456 Schlusstag/-zeit MEZ 02.04.2015 / 09:04
                                section -> section
                                        .attributes("date", "time")
                                        .match("^Belegnummer .* Schlusstag\\/\\-zeit .* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\/ (?<time>[\\d]{2}:[\\d]{2})$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                            )

                
                // Buchung auf Kontonummer 1234567 40 mit Wertstellung 08.04.2015 EUR 675,50
                .section("amount", "currency").optional()
                .match("^Buchung auf Kontonummer [\\s\\d]+ mit Wertstellung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(t -> {
                    // If we have multiple entries in the document, with
                    // fee and fee refunds, then the "noProvision" flag
                    // must be removed.
                    type.getCurrentContext().remove("noProvision");

                    return new BuySellEntryItem(t);
                });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Dividendengutschrift|Ertragsgutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividendengutschrift|Ertragsgutschrift)$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // 380,000000 878841 US17275R1023
                // CISCO SYSTEMS INC.REGISTERED SHARES DL-,001
                // Dividende pro Stück 0,2600000000 USD Zahlbar 15.12.2014
                .section("wkn", "isin", "name", "currency")
                .match("^[\\.,\\d]+ (?<wkn>.*) (?<isin>[\\w]{12})$")
                .match("^(?<name>.*)$")
                .match("^(Dividende|Aussch.ttung) pro St.ck [\\.,\\d]+ (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // 380,000000 878841 US17275R1023
                .section("shares")
                .match("^(?<shares>[\\.,\\d]+) .* [\\w]{12}$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Gutschrift mit Wert 15.12.2014 64,88 EUR
                .section("date", "amount", "currency")
                .match("^Gutschrift mit Wert (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Bruttoertrag 98,80 USD 87,13 EUR
                // Umrechnungskurs USD zu EUR 1,1339000000
                .section("fxGross", "fxCurrency", "gross", "currency", "baseCurrency", "termCurrency", "exchangeRate").optional()
                .match("^Bruttoertrag (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}) (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .match("^Umrechnungskurs (?<termCurrency>[\\w]{3}) zu (?<baseCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    type.getCurrentContext().putType(asExchangeRate(v));

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    
                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .conclude(PDFExtractorUtils.fixGrossValueA())
                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug vom", (context, lines) -> {
            Pattern pCurrency = Pattern.compile("[\\d]{3} [\\d]+ [\\d]{2} (?<currency>[\\w]{3}) [\\-|\\+] [\\.,\\d]+");
            Pattern pYear = Pattern.compile("Kontoauszug vom [\\d]{2}\\.[\\d]{2}\\.(?<year>[\\d]{4}) bis [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}");
            // read the current context here
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

        // @formatter:off
        // Formatting:
        // Buchung | Valuta | Vorgang | Soll | Haben
        //  
        // 01.12. 01.12. SEPA Dauerauftrag an - 40,00
        // Mustermann, Max
        // IBAN DE1111110000111111
        // BIC OSDDDE81XXX
        // 
        // 07.12. 07.12. SEPA Überweisung von + 562,00
        // Unser Sparverein
        // Verwendungszweck/ Kundenreferenz
        // 1111111111111111 1220 INKL. SONDERZAHLUNG
        // @formatter:on
        Block blockDepositRemoval = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. "
                        + "((SEPA )?"
                        + "(Dauerauftrag"
                        + "|.berweisung"
                        + "|Lastschrifteinzug"
                        + "|Echtzeit.berweisung) .*"
                        + "|Bargeldauszahlung GAA"
                        + "|Kartenzahlung"
                        + "|Verwendungszweck\\/ Kundenreferenz) "
                        + "(\\-|\\+) [\\.,\\d]+$");
        type.addBlock(blockDepositRemoval);
        blockDepositRemoval.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.DEPOSIT);
                    return entry;
                })

                .section("date", "note", "sign", "amount", "note1")
                .match("^[\\d]{2}\\.[\\d]{2}\\. (?<date>[\\d]{2}\\.[\\d]{2}\\.) "
                                + "(SEPA )?"
                                + "(?<note>(Dauerauftrag"
                                + "|.berweisung"
                                + "|Lastschrifteinzug"
                                + "|Echtzeit.berweisung) .*"
                                + "|Bargeldauszahlung GAA"
                                + "|Kartenzahlung"
                                + "|Verwendungszweck\\/ Kundenreferenz) "
                                + "(?<sign>(\\-|\\+)) (?<amount>[\\.,\\d]+)$")
                .match("^(?<note1>.*)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    // Is sign --> "-" change from DEPOSIT to REMOVAL
                    if (v.get("sign").equals("-"))
                        t.setType(AccountTransaction.Type.REMOVAL);

                    // Formatting some notes
                    if (!v.get("note1").startsWith("Verwendungszweck"))
                        v.put("note", trim(v.get("note")) + " " + trim(v.get("note1")));

                    if (v.get("note").startsWith("Lastschrifteinzug"))
                        v.put("note", trim(v.get("note1")));

                    if (v.get("note").startsWith("Verwendungszweck"))
                        v.put("note", "");

                    t.setDateTime(asDate(v.get("date") + context.get("year")));
                    t.setCurrencyCode(context.get("currency"));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setNote(v.get("note"));

                    // @formatter:off
                    // If we have fees, then we set the amount to 0.00
                    //  
                    // 31.12. 31.12. Verwendungszweck/ Kundenreferenz - 13,47
                    // Saldo der Abschlussposten
                    // @formatter:on
                    if (v.get("note1").equals("Saldo der Abschlussposten"))
                        t.setAmount(0L);
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        // @formatter:off
        // Formatting:
        // Buchung | Valuta | Vorgang | Soll | Haben
        //  
        // 31.12. 31.12. Verwendungszweck/ Kundenreferenz - 13,47
        // Saldo der Abschlussposten
        // @formatter:on
        Block blockFees = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. Verwendungszweck\\/ Kundenreferenz \\- [\\.,\\d]+$");
        type.addBlock(blockFees);
        blockFees.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.FEES);
                    return entry;
                })

                .section("date", "amount", "note").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\. (?<date>[\\d]{2}\\.[\\d]{2}\\.) Verwendungszweck\\/ Kundenreferenz \\- (?<amount>[\\.,\\d]+)$")
                .match("^(?<note>Saldo der Abschlussposten)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    t.setDateTime(asDate(v.get("date") + context.get("year")));
                    t.setCurrencyCode(context.get("currency"));
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
                // Kapitalertragsteuer (KESt)  - 9,88 USD - 8,71 EUR
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer \\(KESt\\) ([\\s]+)?\\- [\\.,\\d]+ [\\w]{3} \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kapitalertragsteuer (KESt) - 4,28 EUR
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer \\(KESt\\) ([\\s]+)?\\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kapitalertragsteuer EUR -122,94
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer (?<currency>[\\w]{3}) \\-(?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag auf KESt - 0,53 USD - 0,47 EUR
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag auf KESt ([\\s]+)?\\- [\\.,\\d]+ [\\w]{3} \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag auf KESt - 0,23 EUR
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag auf KESt ([\\s]+)?\\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag auf Kapitalertragsteuer EUR -6,76
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag auf Kapitalertragsteuer (?<currency>[\\w]{3}) \\-(?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kirchensteuer auf Kapitalertragsteuer EUR -1,23
                .section("tax", "currency").optional()
                .match("^Kirchensteuer auf Kapitalertragsteuer (?<currency>[\\w]{3}) \\-(?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kirchensteuer auf KESt - 5,28 EUR
                .section("tax", "currency").optional()
                .match("^Kirchensteuer auf KESt ([\\s]+)?\\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kirchensteuer auf KESt - 11,79 USD - 10,43 EUR
                .section("tax", "currency").optional()
                .match("^Kirchensteuer auf KESt ([\\s]+)?\\- [\\.,\\d]+ [\\w]{3} \\- (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Anrechenbare ausländische Quellensteuer 13,07 EUR
                .section("creditableWithHoldingTax", "currency").optional()
                .match("^Anrechenbare ausl.ndische Quellensteuer (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        // If the provision fee and the fee refund are the same, 
        // then we set a flag and don't book provision fee.
        transaction
                // Provision EUR 3,13
                // Provisions-Rabatt EUR -1,13
                // Ausgekehrte Zuwendungen, die die Bank von DWS Xtrackers erhält EUR -2,00
                .section("currency", "fee", "discountCurrency1", "discount1", "discountCurrency2", "discount2").optional()
                .match("^Provision (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,\\d]+)$")
                .match("^Provisions\\-Rabatt (?<discountCurrency1>[\\w]{3}) (\\-)?(?<discount1>[\\.,\\d]+)$")
                .match("^Ausgekehrte Zuwendungen, .* (?<discountCurrency2>[\\w]{3}) \\-(?<discount2>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
                    Money discount1 = Money.of(asCurrencyCode(v.get("discountCurrency1")), asAmount(v.get("discount1")));
                    Money discount2 = Money.of(asCurrencyCode(v.get("discountCurrency2")), asAmount(v.get("discount2")));

                    if (fee.subtract(discount1.add(discount2)).isPositive())
                    {
                        fee = fee.subtract(discount1.add(discount2));
                        checkAndSetFee(fee, t, type);
                    }

                    type.getCurrentContext().put("noProvision", "X");
                })

                // Provision EUR 3,13
                // Provisions-Rabatt EUR -3,13
                .section("currency", "fee", "discountCurrency", "discount").optional()
                .match("^Provision (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,\\d]+)$")
                .match("^Provisions\\-Rabatt (?<discountCurrency>[\\w]{3}) (\\-)?(?<discount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
                    Money discount = Money.of(asCurrencyCode(v.get("discountCurrency")), asAmount(v.get("discount")));

                    if (!"X".equals(type.getCurrentContext().get("noProvision")))
                    {
                        if (fee.subtract(discount).isPositive())
                        {
                            fee = fee.subtract(discount);
                            checkAndSetFee(fee, t, type);
                        }

                        type.getCurrentContext().put("noProvision", "X");
                    }
                })

                // Provision EUR 1,26
                // Ausgekehrte Zuwendungen, die die Bank von DWS Xtrackers erhält EUR -1,26
                .section("currency", "fee", "discountCurrency", "discount").optional()
                .match("^Provision (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,\\d]+)$")
                .match("^Ausgekehrte Zuwendungen, .* (?<discountCurrency>[\\w]{3}) \\-(?<discount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
                    Money discount = Money.of(asCurrencyCode(v.get("discountCurrency")), asAmount(v.get("discount")));

                    if (!"X".equals(type.getCurrentContext().get("noProvision")))
                    {
                        if (fee.subtract(discount).isPositive())
                        {
                            fee = fee.subtract(discount);
                            checkAndSetFee(fee, t, type);
                        }

                        type.getCurrentContext().put("noProvision", "X");
                    }
                });

        transaction
                // Provision EUR 7,90
                // Provision EUR -7,90
                .section("currency", "fee").optional()
                .match("^Provision (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("noProvision")))
                        processFeeEntries(t, v, type);
                })

                // Provision (0,25 %) EUR 8,78
                // Provision (0,25 %) EUR -8,78
                .section("currency", "fee").optional()
                .match("^Provision \\([\\.,\\d]+ %\\) (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // XETRA-Kosten EUR 0,60
                // XETRA-Kosten EUR -0,60
                .section("currency", "fee").optional()
                .match("^XETRA-Kosten (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Weitere Provision der Bank bei der börslichen Orderausführung EUR 2,00
                .section("currency", "fee").optional()
                .match("^Weitere Provision .* (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Fremde Spesen und Auslagen EUR -5,40
                .section("currency", "fee").optional()
                .match("^Fremde Spesen und Auslagen (?<currency>[\\w]{3}) (\\-)?(?<fee>[\\.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
