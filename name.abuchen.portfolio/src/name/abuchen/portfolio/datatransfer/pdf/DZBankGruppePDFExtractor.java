package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class DZBankGruppePDFExtractor extends AbstractPDFExtractor
{
    public DZBankGruppePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("GLS Bank"); //$NON-NLS-1$
        addBankIdentifier("Union Investment Service Bank AG"); //$NON-NLS-1$
        addBankIdentifier("Volksbank"); //$NON-NLS-1$
        addBankIdentifier("VR-Bank"); //$NON-NLS-1$
        addBankIdentifier("VRB"); //$NON-NLS-1$
        addBankIdentifier("Postfach 12 40"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
        addDepotStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "DZ Bank Gruppe (Volksbank/ Union Investment/ VR-Bank/ GLS Bank ...)"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        // Handshake for tax refund transaction
        Map<String, String> context = type.getCurrentContext();

        Block firstRelevantLine = new Block("^Wertpapier Abrechnung (Kauf|Verkauf).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Wertpapier Abrechnung (?<type>(Kauf|Verkauf)).*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // Stück 2.700 INTERNAT. CONS. AIRL. GROUP SA ES0177542018 (A1H6AJ)
                // ACCIONES NOM. EO -,10
                // Handels-/Ausführungsplatz XETRA (gemäß Weisung)
                // Kurswert 5.047,65- EUR
                .section("name", "isin", "wkn", "name1", "currency")
                .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\)$")
                .match("^(?<name1>.*)$")
                .match("^Kurswert [\\.,\\d]+(\\-)? (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));

                    // Handshake, if there is a tax refund
                    context.put("name", v.get("name"));
                    context.put("isin", v.get("isin"));
                    context.put("wkn", v.get("wkn"));
                })

                // Stück 2.700 INTERNAT. CONS. AIRL. GROUP SA ES0177542018 (A1H6AJ)
                .section("shares")
                .match("^St.ck (?<shares>[\\.,\\d]+) .*$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));

                    // Handshake, if there is a tax refund
                    context.put("shares", v.get("shares"));
                })

                // Schlusstag/-Zeit 17.02.2021 09:04:10 Auftraggeber Max Mustermann
                .section("date", "time")
                .match("^Schlusstag\\/\\-Zeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                // Ausmachender Betrag 5.109,01- EUR
                // Ausmachender Betrag 1.109,01+ EUR
                // Ausmachender Betrag 577,95 EUR
                .section("amount", "currency")
                .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)([\\-|\\+])? (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Limit billigst
                // Stoplimit 291,00 EUR Limit 290,00 EUR
                .section("note").optional()
                .match("^(?<note>(Limit|Stoplimit) .*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addTaxReturnBlock(context, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Dividendengutschrift|Aussch.ttung Investmentfonds|Ertragsgutschrift)");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividendengutschrift|Aussch.ttung Investmentfonds|Ertragsgutschrift .*)$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // Stück 17 CD PROJEKT S.A. PLOPTTC00011 (534356)
                // INHABER-AKTIEN C ZY 1
                // Zahlbarkeitstag 08.06.2021 Dividende pro Stück 5,00 PLN
                .section("name", "isin", "wkn", "name1", "currency")
                .match("^St.ck [\\.,\\d]+ (?<name>.*) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\)$")
                .match("^(?<name1>.*)$")
                .match("^.* ((Dividende|Ertrag) ([\\s]+)?pro St.ck|Aussch.ttung pro St\\.) [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                        v.put("name", trim(v.get("name")) + " " + trim(v.get("name1")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Stück 17 CD PROJEKT S.A. PLOPTTC00011 (534356)
                .section("shares")
                .match("^St.ck (?<shares>[\\.,\\d]+) .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Den Betrag buchen wir mit Wertstellung 10.06.2021 zu Gunsten des Kontos XXXX (IBAN DE88 4306 0967 1154
                .section("date")
                .match("^Den Betrag buchen wir mit Wertstellung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Ausmachender Betrag 13,28+ EUR
                .section("amount", "currency")
                .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+)\\+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Devisenkurs EUR / PLN 4,5044
                // Dividendengutschrift 85,00 PLN 18,87+ EUR
                .section("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "fxCurrency", "gross", "currency").optional()
                .match("^Devisenkurs (?<baseCurrency>[\\w]{3}) \\/ (?<termCurrency>[\\w]{3}) ([\\s]+)?(?<exchangeRate>[\\.,\\d]+)$")
                .match("^(Dividendengutschrift|Aussch.ttung) (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}) (?<gross>[\\.,\\d]+)\\+ (?<currency>[\\w]{3})")
                .assign((t, v) -> {
                    type.getCurrentContext().putType(asExchangeRate(v));

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                // Ex-Tag 26.02.2021 Art der Dividende Quartalsdividende
                .section("note").optional()
                .match("^.* Art der Dividende (?<note>.*)$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    public void addDepotStatementTransaction()
    {
        final DocumentType type = new DocumentType("Abrechnung Nr\\. [\\d]+", (context, lines) -> {
            Pattern pAccountingNumber = Pattern.compile("^(?<accountingNumber>Abrechnung Nr\\. [\\d]+)$");
            Pattern pBaseCurrency = Pattern.compile("^.* Preis\\/(?<baseCurrency>[\\w]{3}) .*$");
            Pattern pNameIsin = Pattern.compile("^(Fonds: )?(?<name>((?!MusterFonds).)*) ISIN: (?<isin>[\\w]{12}) .*$");

            // Set end of line of the securities transaction block
            int endOfLineOfSecurityTransactionBlock = lines.length;
            String baseCurrency = CurrencyUnit.EUR;

            // Create security list
            List<String[]> securityList = new ArrayList<String[]>();

            // Set patter of security names
            String patterOfSecurityNames = "";

            for (int i = lines.length - 1; i >= 1; i--)
            {
                Matcher m1 = pAccountingNumber.matcher(lines[i]);
                if (m1.matches())
                    context.put("accountingNumber", m1.group("accountingNumber"));

                m1 = pNameIsin.matcher(lines[i]);
                if (m1.matches())
                {
                    // Search the base currency in the block
                    for (int ii = i; ii < endOfLineOfSecurityTransactionBlock; ii++)
                    {
                        Matcher m2 = pBaseCurrency.matcher(lines[ii]);
                        if (m2.matches())
                            baseCurrency = m2.group("baseCurrency");
                    }

                    // @formatter:off
                    // Stringbuilder:
                    // security_(security name)_(currency)_(start@line)_(end@line) = isin
                    //  
                    // Example:
                    // Fonds: PrivatFonds: Kontrolliert pro ISIN: DE000A0RPAN3 Verwaltungsvergütung: 1,55 % p. a.
                    // Buchungs-/ Umsatzart Betrag/EUR Ausgabe- Preis/EUR Anteile
                    //  
                    // Fonds: UniGlobal ISIN: DE0008491051 Verwaltungsvergütung: 1,20 % p. a.
                    // Hinweis: UniProfiRente Altersvorsorgevertrag
                    //  - gefördert -
                    // Buchungs-/ Umsatzart Betrag/EUR Ausgabe- Preis/EUR Anteile
                    //  
                    // Fonds: UniMultiAsset: Exklusiv ISIN: DE000A2H9A01 Verwaltungsvergütung: 0,50 % p. a.
                    // UniMultiAsset: Chance I ISIN: DE000A2H9A19 Verwaltungsvergütung: 0,40 % p. a.
                    // LMGF-L.M.Mart.Cu.Gl.L.T.Uncon. Reg. ISIN: IE00BMDQ4622 Verwaltungsvergütung: 0,40 % p. a.
                    // @formatter:on
                    if (i != (endOfLineOfSecurityTransactionBlock - 1))
                    {
                        StringBuilder securityListKey = new StringBuilder("security_");
                        securityListKey.append(trim(m1.group("name"))).append("_");
                        securityListKey.append(baseCurrency).append("_");
                        securityListKey.append(Integer.toString(i + 1)).append("_");
                        securityListKey.append(Integer.toString(((endOfLineOfSecurityTransactionBlock))));
                        context.put(securityListKey.toString(), m1.group("isin"));

                        // Add security to securityList
                        String[] security = {m1.group("isin"), trim(m1.group("name"))};
                        securityList.add(security);
                    }
                    else
                    {
                        // @formatter:off
                        // Example:
                        // Fonds: UniMultiAsset: Exklusiv ISIN: DE000A2H9A01 Verwaltungsvergütung: 0,50 % p. a.
                        // UniMultiAsset: Chance I ISIN: DE000A2H9A19 Verwaltungsvergütung: 0,40 % p. a.
                        // LMGF-L.M.Mart.Cu.Gl.L.T.Uncon. Reg. ISIN: IE00BMDQ4622 Verwaltungsvergütung: 0,40 % p. a.
                        // iShares III- Core EO Govt Bond UCIT ISIN: IE00B4WXJJ64 Verwaltungsvergütung: 0,20 % p. a.
                        // J O H.C.M.U.Fd-Glob.Opport.Fd ISIN: IE00B7MR5575 Verwaltungsvergütung: 0,75 % p. a.
                        // SISF EURO Corporate Bond C Acc EUR ISIN: LU0113258742 Verwaltungsvergütung: 0,45 % p. a.
                        // BGF Euro Corporate Bond Fund D2 EUR ISIN: LU0368266499 Verwaltungsvergütung: 0,40 % p. a.
                        // UBS(L)F.S-MSCI EM.MKTS UC ETF A USD ISIN: LU0480132876 Verwaltungsvergütung: 0,225 % p. a.
                        // Xtrackers MSCI World Swap ISIN: LU0659579733 Verwaltungsvergütung: 0,42 % p. a
                        // DWS Inv.-Euro-Gov Bonds ISIN: LU1663883681 Verwaltungsvergütung: 0,35 % p. a.
                        // @formatter:on

                        // Add security to securityList
                        String[] security = {m1.group("isin"), trim(m1.group("name"))};
                        securityList.add(security);
                    }

                    endOfLineOfSecurityTransactionBlock = i;
                }
            }

            // Create patter of security names
            for (String[] security : securityList)
            {
                if (patterOfSecurityNames.isEmpty())
                    patterOfSecurityNames = security[1];
                else
                    patterOfSecurityNames = patterOfSecurityNames + "|" + security[1];
            }

            endOfLineOfSecurityTransactionBlock = lines.length;

            // Characters that have to be escaped in regular expressions
            patterOfSecurityNames = patterOfSecurityNames
                            .replaceAll("\\(", "\\\\(")
                            .replaceAll("\\)", "\\\\)")
                            .replaceAll("\\.", "\\\\.")
                            .replaceAll("\\-", "\\\\-")
                            .replaceAll("\\+", "\\\\+");

            for (int i = lines.length - 1; i >= 0; i--)
            {
                Pattern pSearchSecurity = Pattern.compile("^(?<name>(" + patterOfSecurityNames + "))$");
                Matcher m = pSearchSecurity.matcher(lines[i]);
                if (m.matches())
                {
                    for (String[] security : securityList) 
                    {
                        if (m.group("name").equals(security[1]))
                        {
                            // @formatter:off
                            // Stringbuilder:
                            // security_(security name)_(currency)_(start@line)_(end@line) = isin
                            // @formatter:on
                            StringBuilder securityListKey = new StringBuilder("security_");
                            securityListKey.append(trim(m.group("name"))).append("_");
                            securityListKey.append(baseCurrency).append("_");
                            securityListKey.append(Integer.toString(i + 1)).append("_");
                            securityListKey.append(Integer.toString(((endOfLineOfSecurityTransactionBlock))));
                            context.put(securityListKey.toString(), security[0]);
                        }
                    }
                    endOfLineOfSecurityTransactionBlock = i;
                }
            }
        });
        this.addDocumentTyp(type);

        // @formatter:off
        // Formatting:
        // Buchungsdatum
        // Preisdatum | Umsatzart | Betrag/EUR
        // Anlage | Betrag/EUR | Ausgabeaufschlag % | Preis/EUR | Anteile
        // -------------------------------------
        // 18.07.2017
        // 17.07.2017 Kauf 2.125,00
        // Anlage 2.125,00 0,00 148,75 14,286
        // 
        // 19.11.2020 Verkauf *1 18.103,67 63,38 -285,637
        //  
        // 27.11.2017
        // 2 4.11.2017 Wiederanlage 94,78 0,00 142,61 0,665
        // 
        // 01.03.2022 Anlage 4,24 25,5849 0,166
        // @formatter:on
        Transaction<BuySellEntry> pdfTransaction1 = new Transaction<>();
        pdfTransaction1.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine1 = new Block("^[\\s\\d]{2,3}\\.[\\d]{2}\\.[\\d]{4} (Kauf|Wiederanlage|Verkauf|Anlage) .*$");
        type.addBlock(firstRelevantLine1);
        firstRelevantLine1.set(pdfTransaction1);

        pdfTransaction1
                .oneOf(
                            section -> section
                                    .attributes("date", "amount", "shares")
                                    .match("^(?<date>[\\s\\d]{2,3}\\.[\\d]{2}\\.[\\d]{4}) Kauf (?<amount>[\\.,\\d]+)$")
                                    .match("^Anlage [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ (?<shares>[\\.,\\d]+)$")
                                    .assign((t, v) -> {
                                        Map<String, String> context = type.getCurrentContext();

                                        Security securityData = getSecurity(context, v.getStartLineNumber());
                                        if (securityData != null)
                                        {
                                            v.put("name", securityData.getName());
                                            v.put("isin", securityData.getIsin());
                                            v.put("currency", asCurrencyCode(securityData.getCurrency()));
                                        }
                                        t.setSecurity(getOrCreateSecurity(v));

                                        t.setDate(asDate(stripBlanks(v.get("date"))));
                                        t.setShares(asShares(v.get("shares")));
                                        t.setCurrencyCode(asCurrencyCode(context.get("baseCurrency")));
                                        t.setAmount(asAmount(v.get("amount")));
                                    })
                            ,
                            section -> section
                                    .attributes("date", "amount", "shares")
                                    .match("(?<date>[\\s\\d]{2,3}\\.[\\d]{2}\\.[\\d]{4}) Verkauf \\*[\\d]+ (?<amount>[\\.,\\d]+) [\\.,\\d]+ \\-(?<shares>[\\.,\\d]+)$")
                                    .assign((t, v) -> {
                                        Map<String, String> context = type.getCurrentContext();

                                        // We switch to SELL
                                        t.setType(PortfolioTransaction.Type.SELL);

                                        Security securityData = getSecurity(context, v.getStartLineNumber());
                                        if (securityData != null)
                                        {
                                            v.put("name", securityData.getName());
                                            v.put("isin", securityData.getIsin());
                                            v.put("currency", asCurrencyCode(securityData.getCurrency()));
                                        }
                                        t.setSecurity(getOrCreateSecurity(v));

                                        t.setDate(asDate(stripBlanks(v.get("date"))));
                                        t.setShares(asShares(v.get("shares")));
                                        t.setCurrencyCode(asCurrencyCode(context.get("baseCurrency")));
                                        t.setAmount(asAmount(v.get("amount")));
                                    })
                            ,
                            section -> section
                                    .attributes("date", "note", "amount", "shares")
                                    .match("^(?<date>[\\s\\d]{2,3}\\.[\\d]{2}\\.[\\d]{4}) (?<note>Wiederanlage) (?<amount>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+ (?<shares>[\\.,\\d]+)$")
                                    .assign((t, v) -> {
                                        Map<String, String> context = type.getCurrentContext();

                                        Security securityData = getSecurity(context, v.getStartLineNumber());
                                        if (securityData != null)
                                        {
                                            v.put("name", securityData.getName());
                                            v.put("isin", securityData.getIsin());
                                            v.put("currency", asCurrencyCode(securityData.getCurrency()));
                                        }
                                        t.setSecurity(getOrCreateSecurity(v));

                                        t.setDate(asDate(stripBlanks(v.get("date"))));
                                        t.setShares(asShares(v.get("shares")));
                                        t.setCurrencyCode(asCurrencyCode(context.get("baseCurrency")));
                                        t.setAmount(asAmount(v.get("amount")));
                                        t.setNote(v.get("note"));
                                    })
                            ,
                            section -> section
                                    .attributes("date", "note", "amount", "shares", "time")
                                    .match("^(?<date>[\\s\\d]{2,3}\\.[\\d]{2}\\.[\\d]{4}) (?<note>Anlage) (?<amount>[\\.,\\d]+) [\\.,\\d]+ (?<shares>[\\.,\\d]+)$")
                                    .match("^Handelszeit: (?<time>[\\d]{2}:[\\d]{2}) .*$")
                                    .assign((t, v) -> {
                                        Map<String, String> context = type.getCurrentContext();

                                        Security securityData = getSecurity(context, v.getStartLineNumber());
                                        if (securityData != null)
                                        {
                                            v.put("name", securityData.getName());
                                            v.put("isin", securityData.getIsin());
                                            v.put("currency", asCurrencyCode(securityData.getCurrency()));
                                        }
                                        t.setSecurity(getOrCreateSecurity(v));

                                        t.setDate(asDate(stripBlanks(v.get("date")), v.get("time")));
                                        t.setShares(asShares(v.get("shares")));
                                        t.setCurrencyCode(asCurrencyCode(context.get("baseCurrency")));
                                        t.setAmount(asAmount(v.get("amount")));
                                        t.setNote(v.get("note"));
                                    })
                            ,
                            section -> section
                                    .attributes("date", "note", "amount", "shares")
                                    .match("^(?<date>[\\s\\d]{2,3}\\.[\\d]{2}\\.[\\d]{4}) (?<note>Anlage) (?<amount>[\\.,\\d]+) [\\.,\\d]+ (?<shares>[\\.,\\d]+)$")
                                    .assign((t, v) -> {
                                        Map<String, String> context = type.getCurrentContext();

                                        Security securityData = getSecurity(context, v.getStartLineNumber());
                                        if (securityData != null)
                                        {
                                            v.put("name", securityData.getName());
                                            v.put("isin", securityData.getIsin());
                                            v.put("currency", asCurrencyCode(securityData.getCurrency()));
                                        }
                                        t.setSecurity(getOrCreateSecurity(v));

                                        t.setDate(asDate(stripBlanks(v.get("date"))));
                                        t.setShares(asShares(v.get("shares")));
                                        t.setCurrencyCode(asCurrencyCode(context.get("baseCurrency")));
                                        t.setAmount(asAmount(v.get("amount")));
                                        t.setNote(v.get("note"));
                                    })
                        )

                .wrap(t -> {
                    if (t.getPortfolioTransaction().getCurrencyCode() != null && t.getPortfolioTransaction().getAmount() != 0)
                        return new BuySellEntryItem(t);
                    return null;
                });

        addFeesSectionsTransaction(pdfTransaction1, type);
        addTaxesSectionsTransaction(pdfTransaction1, type);

        // @formatter:off
        // Formatting:
        // Buchungsdatum
        // Preisdatum | Umsatzart | Betrag/EUR
        // Anlage | Betrag/EUR | Ausgabeaufschlag % | Preis/EUR | Anteile
        // -------------------------------------
        // Gesamtausschüttung *1 94,78
        // abgeführte Kapitalertragsteuer 0,00
        // inklusive Solidaritätszuschlag
        // 27.11.2017
        // 2 4.11.2017 Wiederanlage 94,78 0,00 142,61 0,665
        //  
        // Ausschüttung *1 362,80
        // abgeführte Kapitalertragsteuer 0,00
        // inklusive Solidaritätszuschlag
        // 11.12.2020
        // 1 0.12.2020 Wiederanlage 362,80 0,00 53,91 6,730
        // @formatter:on
        Transaction<AccountTransaction> pdfTransaction2 = new Transaction<>();
        pdfTransaction2.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DIVIDENDS);
            return transaction;
        });

        Block firstRelevantLine2 = new Block("^(Gesamtaussch.ttung|Aussch.ttung) \\*[\\d]+ [\\.,\\d]+$", "^Bestand .*$");
        type.addBlock(firstRelevantLine2);
        firstRelevantLine2.set(pdfTransaction2);

        pdfTransaction2
                .section("amount", "date", "shares")
                .match("^(Gesamtaussch.ttung|Aussch.ttung) \\*[\\d]+ (?<amount>[\\.,\\d]+)$")
                .match("^(?<date>[\\s\\d]{2,3}\\.[\\d]{2}\\.[\\d]{4}) Wiederanlage [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    Security securityData = getSecurity(context, v.getStartLineNumber());
                    if (securityData != null)
                    {
                        v.put("name", securityData.getName());
                        v.put("isin", securityData.getIsin());
                        v.put("currency", asCurrencyCode(securityData.getCurrency()));
                    }

                    t.setDateTime(asDate(stripBlanks(v.get("date"))));
                    t.setShares(asShares(v.get("shares")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("baseCurrency")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });

        addFeesSectionsTransaction(pdfTransaction2, type);
        addTaxesSectionsTransaction(pdfTransaction2, type);

        // @formatter:off
        // Formatting:
        // Buchungsdatum
        // Preisdatum | Umsatzart | Betrag/EUR
        // Anlage | Betrag/EUR | Ausgabeaufschlag % | Preis/EUR | Anteile
        //  -------------------------------------
        // 22.11.2019
        // 21.11.2019 Ausgleichsbuchung Steuer*1 2,09
        // Anlage 2,09 0,00 241,88 0,009
        // @formatter:on
        Transaction<PortfolioTransaction> pdfTransaction3 = new Transaction<>();
        pdfTransaction3.subject(() -> {
            PortfolioTransaction transaction = new PortfolioTransaction();
            transaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            return transaction;
        });

        Block firstRelevantLine3 = new Block("^[\\s\\d]{2,3}\\.[\\d]{2}\\.[\\d]{4} Ausgleichsbuchung Steuer.* .*$");
        type.addBlock(firstRelevantLine3);
        firstRelevantLine3.set(pdfTransaction3);

        pdfTransaction3
                .section("date", "note", "amount", "shares")
                .match("^(?<date>[\\s\\d]{2,3}\\.[\\d]{2}\\.[\\d]{4}) (?<note>Ausgleichsbuchung Steuer).* (?<amount>[\\.,\\d]+)$")
                .match("^Anlage [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    Security securityData = getSecurity(context, v.getStartLineNumber());
                    if (securityData != null)
                    {
                        v.put("name", securityData.getName());
                        v.put("isin", securityData.getIsin());
                        v.put("currency", asCurrencyCode(securityData.getCurrency()));
                    }

                    t.setDateTime(asDate(stripBlanks(v.get("date"))));
                    t.setShares(asShares(v.get("shares")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("baseCurrency")));
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });

        addFeesSectionsTransaction(pdfTransaction3, type);
        addTaxesSectionsTransaction(pdfTransaction3, type);

        // @formatter:off
        // Formatting:
        // Buchungsdatum
        // Preisdatum | Umsatzart | Betrag/EUR
        // Anlage | Betrag/EUR | Ausgabeaufschlag % | Preis/EUR | Anteile
        // -------------------------------------
        // 11.02.2014
        // 10.02.2014 Umtausch 458,99
        // aus Unterdepot 1345674218
        // Anlage 458,99 0,00 37,77 12,152
        // @formatter:on
        Transaction<PortfolioTransaction> pdfTransaction4 = new Transaction<>();
        pdfTransaction4.subject(() -> {
            PortfolioTransaction transaction = new PortfolioTransaction();
            transaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            return transaction;
        });

        Block firstRelevantLine4 = new Block("^[\\s\\d]{2,3}\\.[\\d]{2}\\.[\\d]{4} Umtausch .*$");
        type.addBlock(firstRelevantLine4);
        firstRelevantLine4.set(pdfTransaction4);

        pdfTransaction4
                .oneOf(
                        section -> section
                                .attributes("date", "note1", "amount", "note2", "shares")
                                .match("^(?<date>[\\s\\d]{2,3}\\.[\\d]{2}\\.[\\d]{4}) (?<note1>Umtausch) (?<amount>[\\.,\\d]+)$")
                                .match("^(?<note2>aus Unterdepot [\\d]+)$")
                                .match("^Anlage [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ (?<shares>[\\.,\\d]+)$")
                                .assign((t, v) -> {
                                    Map<String, String> context = type.getCurrentContext();

                                    Security securityData = getSecurity(context, v.getStartLineNumber());
                                    if (securityData != null)
                                    {
                                        v.put("name", securityData.getName());
                                        v.put("isin", securityData.getIsin());
                                        v.put("currency", asCurrencyCode(securityData.getCurrency()));
                                    }

                                    t.setDateTime(asDate(stripBlanks(v.get("date"))));
                                    t.setShares(asShares(v.get("shares")));
                                    t.setAmount(asAmount(v.get("amount")));
                                    t.setCurrencyCode(asCurrencyCode(context.get("baseCurrency")));
                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setNote(v.get("note1") + " " + trim(v.get("note2")));
                                })
                        ,
                        section -> section
                                .attributes("date", "note1", "amount", "shares", "note2")
                                .match("(?<date>[\\s\\d]{2,3}\\.[\\d]{2}\\.[\\d]{4}) (?<note1>Umtausch) (?<amount>[\\.,\\d]+) [\\.,\\d]+ \\-(?<shares>[\\.,\\d]+)$")
                                .match("^(?<note2>zugunsten Unterdepot [\\d]+)$")
                                .assign((t, v) -> {
                                    Map<String, String> context = type.getCurrentContext();

                                    // We switch to DELIVERY_OUTBOUND
                                    t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);

                                    Security securityData = getSecurity(context, v.getStartLineNumber());
                                    if (securityData != null)
                                    {
                                        v.put("name", securityData.getName());
                                        v.put("isin", securityData.getIsin());
                                        v.put("currency", asCurrencyCode(securityData.getCurrency()));
                                    }

                                    t.setDateTime(asDate(stripBlanks(v.get("date"))));
                                    t.setShares(asShares(v.get("shares")));
                                    t.setAmount(asAmount(v.get("amount")));
                                    t.setCurrencyCode(asCurrencyCode(context.get("baseCurrency")));
                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setNote(v.get("note1") + " " + trim(v.get("note2")));
                                })
                        )

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });

        addFeesSectionsTransaction(pdfTransaction4, type);
        addTaxesSectionsTransaction(pdfTransaction4, type);

        // @formatter:off
        // Depotgebühr mit Nutzung der -32,55
        // Postbox
        // @formatter:on
        Transaction<AccountTransaction> pdfTransaction5 = new Transaction<>();
        pdfTransaction5.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.FEES);
            return transaction;
        });

        Block firstRelevantLine5 = new Block("^Depotgeb.hr .*$", "^Bestand .*$");
        type.addBlock(firstRelevantLine5);
        firstRelevantLine5.set(pdfTransaction5);

        pdfTransaction5
                .section("note", "amount", "date")
                .match("^(?<note>Depotgeb.hr) .* \\-(?<amount>[\\.,\\d]+)$")
                .match("^Bestand am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("baseCurrency")));
                    t.setNote(v.get("note"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });

        // @formatter:off
        // Erstattung Kapitalertragsteuer 16,8
        // inklusive Solidaritätszuschlag
        // @formatter:on
        Transaction<AccountTransaction> pdfTransaction6 = new Transaction<>();
        pdfTransaction6.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.TAX_REFUND);
            return transaction;
        });

        Block firstRelevantLine6 = new Block("^Erstattung Kapitalertragsteuer .*$", "^Bestand .*$");
        type.addBlock(firstRelevantLine6);
        firstRelevantLine6.set(pdfTransaction6);

        pdfTransaction6
                .section("amount", "date")
                .match("^Erstattung Kapitalertragsteuer (?<amount>[\\.,\\d]+)$")
                .match("^Bestand am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("baseCurrency")));
                    t.setNote(context.get("accountingNumber"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });

        // @formatter:off
        // Erstattung Kirchensteuer 1,27
        // @formatter:on
        Transaction<AccountTransaction> pdfTransaction7 = new Transaction<>();
        pdfTransaction7.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.TAX_REFUND);
            return transaction;
        });

        Block firstRelevantLine7 = new Block("^Erstattung Kirchensteuer .*$", "^Bestand .*$");
        type.addBlock(firstRelevantLine7);
        firstRelevantLine7.set(pdfTransaction7);

        pdfTransaction7
                .section("amount", "date")
                .match("^Erstattung Kirchensteuer (?<amount>[\\.,\\d]+)$")
                .match("^Bestand am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("baseCurrency")));
                    t.setNote(context.get("accountingNumber"));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                });
    }

    private void addTaxReturnBlock(Map<String, String> context, DocumentType type)
    {
        Block block = new Block("^Steuerliche Ausgleichrechnung$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.TAX_REFUND);
                    return t;
                })

                // Ausmachender Betrag 29,57 EUR
                // Den Gegenwert buchen wir mit Valuta 13.08.2019 zu Gunsten des Kontos 5052258000
                .section("amount", "currency", "date").optional()
                .match("^Ausmachender Betrag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .match("^Den Gegenwert buchen wir mit Valuta (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setShares(asShares(context.get("shares")));

                    t.setSecurity(getOrCreateSecurity(context));
                })

                // Verrechnete Aktienverluste 112,10- EUR
                .section("note").optional()
                .match("^(?<note>Verrechnete Aktienverluste .*)$")
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
                // Finanztransaktionssteuer 10,10- EUR
                .section("tax", "currency").optional()
                .match("^Finanztransaktionssteuer (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kapitalertragsteuer 25 % auf 7,55 EUR 1,89- EUR
                // Kapitalertragsteuer 25,00% auf 143,95 EUR 35,99- EUR
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer [\\.,\\d]+([\\s]+)?% .* (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag 5,5 % auf 1,89 EUR 0,11- EUR
                // Solidaritätszuschlag 5,50% auf 35,99 EUR 1,98- EUR
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag [\\.,\\d]+([\\s]+)?% .* (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kirchensteuer 7 % auf 2,89 EUR 2,11- EUR
                .section("tax", "currency").optional()
                .match("^Kirchensteuer [\\.,\\d]+([\\s]+)?% .* (?<tax>[\\d.]+,\\d+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Einbehaltene Quellensteuer 19 % auf 85,00 PLN 3,59- EUR
                .section("withHoldingTax", "currency").optional()
                .match("^Einbehaltene Quellensteuer [\\.,\\d]+([\\s]+)?% .* (?<withHoldingTax>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Anrechenbare Quellensteuer 15 % auf 18,87 EUR 2,83 EUR
                .section("creditableWithHoldingTax", "currency").optional()
                .match("^Anrechenbare Quellensteuer [\\.,\\d]+([\\s]+)?% .* (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type))

                // abgeführte Kapitalertragsteuer 0,00
                .section("tax").optional()
                .match("^abgef.hrte Kapitalertragsteuer (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    v.put("currency", asCurrencyCode(context.get("baseCurrency")));
                    processTaxEntries(t, v, type);
                })

                // abgeführte Kirchensteuer 0,00
                .section("tax").optional()
                .match("^abgef.hrte Kirchensteuer (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    v.put("currency", asCurrencyCode(context.get("baseCurrency")));
                    processTaxEntries(t, v, type);
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Provision 1,0000 % vom Kurswert 50,48- EUR
                .section("fee", "currency").optional()
                .match("^Provision [\\.,\\d]+ % .* (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Provision 9,95- EUR
                .section("fee", "currency").optional()
                .match("^Provision (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Transaktionsentgelt Börse 0,71- EUR
                .section("fee", "currency").optional()
                .match("^Transaktionsentgelt B.rse (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Übertragungs-/Liefergebühr 0,07- EUR
                .section("fee", "currency").optional()
                .match("^.bertragungs-\\/Liefergeb.hr (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Eigene Spesen 10,00- EUR
                .section("fee", "currency").optional()
                .match("^Eigene Spesen (?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Anlage 2.125,00 2,00 113,45 18,730
                .section("amount", "percentageFee").optional()
                .match("^Anlage (?<amount>[\\.,\\d]+) (?<percentageFee>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("baseCurrency")));

                    BigDecimal percentageFee = asBigDecimal(v.get("percentageFee"));
                    BigDecimal amount = asBigDecimal(v.get("amount"));

                    if (percentageFee.compareTo(BigDecimal.ZERO) != 0)
                    {
                        // fxFee = (amount / (1 + percentageFee / 100)) * (percentageFee / 100)
                        BigDecimal fxFee = amount
                                        .divide(percentageFee.divide(BigDecimal.valueOf(100))
                                                        .add(BigDecimal.ONE), Values.MC)
                                        .multiply(percentageFee, Values.MC);

                        Money fee = Money.of(asCurrencyCode(v.get("currency")),
                                        fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

                        checkAndSetFee(fee, t, type);
                    }
                })

                // 1 0.12.2020 Wiederanlage 362,80 0,00 53,91 6,730
                .section("amount", "percentageFee").optional()
                .match("^[\\s\\d]{2,3}\\.[\\d]{2}\\.[\\d]{4} Wiederanlage (?<amount>[\\.,\\d]+) (?<percentageFee>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();
                    v.put("currency", asCurrencyCode(context.get("baseCurrency")));

                    BigDecimal percentageFee = asBigDecimal(v.get("percentageFee"));
                    BigDecimal amount = asBigDecimal(v.get("amount"));

                    if (percentageFee.compareTo(BigDecimal.ZERO) != 0)
                    {
                        // fxFee = (amount / (1 + percentageFee / 100)) * (percentageFee / 100)
                        BigDecimal fxFee = amount
                                        .divide(percentageFee.divide(BigDecimal.valueOf(100))
                                                        .add(BigDecimal.ONE), Values.MC)
                                        .multiply(percentageFee, Values.MC);

                        Money fee = Money.of(asCurrencyCode(v.get("currency")),
                                        fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

                        checkAndSetFee(fee, t, type);
                    }
                });
    }

    private Security getSecurity(Map<String, String> context, Integer startTransactionLine)
    {
        for (String key : context.keySet())
        {
            String[] parts = key.split("_"); //$NON-NLS-1$
            if (parts[0].equalsIgnoreCase("security")) //$NON-NLS-1$
            {
                if (startTransactionLine >= Integer.parseInt(parts[3]) && startTransactionLine <= Integer.parseInt(parts[4]))
                {
                    // returns security name, isin, security currency
                    return new Security(parts[1], context.get(key), parts[2]);
                }
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
