package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

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

@SuppressWarnings("nls")
public class FILFondbankPDFExtractor extends AbstractPDFExtractor
{
    public FILFondbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("FIL Fondsbank GmbH");

        addBuySellTransaction();
        addSellForAccountFeeTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "FIL Fondsbank GmbH (Fidelity Group)";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Fondsabrechnung|Steuerliche Informationen \\(Einzeltransaktion\\))");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Splittkauf|Splitkauf|Wiederanlage|Kauf|Verkauf|Steuerliche Informationen \\(Einzeltransaktion\\)).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(?<type>(Splittkauf|Splitkauf|Wiederanlage|Kauf|Verkauf|Steuerliche Informationen \\(Einzeltransaktion\\))).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                            {
                                t.setType(PortfolioTransaction.Type.SELL);
                                type.getCurrentContext().putBoolean("sale", true);
                            }
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}: (?<type>(Kauf( aus VL\\-Sparplan)|Gesamtverkauf)?)$") //
                        .assign((t, v) -> {
                            if ("Gesamtverkauf".equals(v.get("type")))
                            {
                                t.setType(PortfolioTransaction.Type.SELL);
                                type.getCurrentContext().putBoolean("sale", true);
                            }
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Fondsname (WKN / ISIN) Xtrackers MSCI World UCITS ETF 1C (A1XB5U / IE00BJ0KDQ92)
                                        // Abrechnungspreis 105,3600 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .match("^Fondsname \\(WKN \\/ ISIN\\) (?<name>.*) \\((?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\)$") //
                                                        .match("^Abrechnungspreis [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Kauf UBS Emer.Mkt.Soc.Resp.UETFADIS 89,56 EUR 12,6399 USD 7,728
                                        // 2540401213 A110QD / LU1048313891 1,090667 USD 02.10.2019 45,394
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "wkn", "isin") //
                                                        .match("^(Splittkauf|Splitkauf|Wiederanlage|Kauf|Verkauf)( Betrag)? (?<name>.*) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) ([\\-|\\+])?[\\.,\\d]+$") //
                                                        .match("^[\\d]+ (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Splittkauf Betrag hausInvest 20,00 EUR 42,2300 EUR 0,474
                                        // 2490116288 DE0009807016 / 980701 04.01.2016 3,818
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "wkn") //
                                                        .match("^(Splittkauf|Splitkauf|Wiederanlage|Kauf|Verkauf)( Betrag)? (?<name>.*) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) ([\\-|\\+])?[\\.,\\d]+$") //
                                                        .match("^[\\d]+ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\/ (?<wkn>[A-Z0-9]{6}) .*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Kauf UBS Emer.Mkt.Soc.Resp.UETFADIS 89,56 EUR 12,6399 USD 7,728
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(Splittkauf|Splitkauf|Wiederanlage|Kauf|Verkauf)( Betrag)? .* ([\\-|\\+])?(?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Anteile 0,263
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Anteile (?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        // @formatter:off
                        // Handelsuhrzeit 16:51:24
                        // @formatter:on
                        .section("time").optional() //
                        .match("^Handelsuhrzeit (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        .oneOf( //
                                        // @formatter:off
                                        // 2536717769 A0X97T / LU0446734526 1,125168 USD 16.04.2019 1,334
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^.*(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if (type.getCurrentContext().get("time") != null)
                                                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                                                            else
                                                                t.setDate(asDate(v.get("date")));
                                                        }),
                                        // @formatter:off
                                        // 30.04.2024: Kauf aus VL-Sparplan
                                        // 24.06.2024: Kauf
                                        // 17.07.2024: Gesamtverkauf
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}): (Kauf( aus VL\\-Sparplan)?|Gesamtverkauf)$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // Abrechnungsbetrag 1,77 EUR
                        // Auszahlungsbetrag 0,00 EUR
                        // Abrechnungsbetrag 26,00 EUR (inkl. Kosten)
                        // Abrechnungsbetrag 50,30 EUR (exkl. Kosten)
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^(Abrechnungsbetrag|Auszahlungsbetrag) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})( \\((inkl|exkl)\\. Kosten\\))?$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // In the fund financial statements, all amounts are reported exclusively as net amounts.
                        // If the net amount is stated in foreign currency, we convert this.
                        // @formatter:on

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Splittkauf Betrag UBS Msci Pacific exJap.U ETF A 1,77 EUR 44,4324 USD 0,045
                                        // 2536717769 A0X97T / LU0446734526 1,125168 USD 16.04.2019 1,334
                                        // UBS (Luxembourg) S.A. 1,99 USD 0,0000 EUR 1,379
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "exchangeRate", "fxGross", "termCurrency") //
                                                        .match("^(Splittkauf|Splitkauf|Wiederanlage|Kauf|Verkauf)( Betrag)? .* [\\.,\\d]+ (?<baseCurrency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} ([\\-|\\+])?[\\.,\\d]+$") //
                                                        .match("^[\\d]+ .* \\/ .* (?<exchangeRate>[\\.,\\d]+) [\\w]{3} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$") //
                                                        .match("^.* (?<fxGross>[\\.,\\d]+) (?<termCurrency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Abrechnungsbetrag 26,00 EUR (inkl. Kosten)
                                        // 27,66 USD
                                        // Devisenkurs 1,0637970
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("gross", "baseCurrency", "termCurrency", "exchangeRate") //
                                                        .match("^Abrechnungsbetrag (?<gross>[\\.,\\d]+) (?<baseCurrency>[\\w]{3})( \\(inkl\\. Kosten\\))?$") //
                                                        .match("^[\\.,\\d]+ (?<termCurrency>[\\w]{3})$") //
                                                        .match("^Devisenkurs (?<exchangeRate>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = rate.convert(asCurrencyCode(v.get("termCurrency")), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // 2540401213 A110QD / LU1048313891 1,090667 USD 02.10.2019 45,394
                                        // 2490116288 DE0009807016 / 980701 04.01.2016 3,818
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^(?<note>[\\d]+) .* \\/ .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$") //
                                                        .assign((t, v) -> t.setNote("Auftrags-Nr. " + v.get("note"))),
                                        // @formatter:off
                                        // Auftragsnummer 2616145223
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^Auftragsnummer (?<note>[\\d]+)$") //
                                                        .assign((t, v) -> t.setNote("Auftrags-Nr. " + v.get("note"))))

                        // @formatter:off
                        // Splittkauf Betrag UBS Msci Pacific exJap.U ETF A 1,77 EUR 44,4324 USD 0,045
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>(Splittkauf|Splitkauf|Wiederanlage)) .*$") //
                        .assign((t, v) -> {
                            if ("Splittkauf".equals(v.get("note")))
                                v.put("note", "Splitkauf");

                            t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | "));
                        })

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(t -> {
                            // @formatter:off
                            // If we have a sell transaction, we set a flag.
                            // So that in the case of a possible charge by sale, (Entgeltbelastung),
                            // taxes are not included if they are in the block.
                            // Otherwise the flag will be removed.
                            // @formatter:on
                            type.getCurrentContext().remove("sale");

                            return new BuySellEntryItem(t);
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addSellForAccountFeeTransaction()
    {
        DocumentType type = new DocumentType("Entgeltbelastung");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Entgeltbelastung .*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.SELL);
                            return portfolioTransaction;
                        })

                        // We have a sell transaction, we set a flag.
                        .section("type").optional() //
                        .match("^(?<type>Entgeltbelastung) .*$") //
                        .assign((t, v) -> type.getCurrentContext().putBoolean("sale", true))

                        .oneOf( //
                                        // @formatter:off
                                        // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                                        // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "wkn", "isin") //
                                                        .match("^(?<note>Entgeltbelastung) (?<name>.*) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) \\-[\\.,\\d]+$") //
                                                        .match("^[\\d]+ (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$")
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Entgeltbelastung Templeton Growth (Euro) Fund 25,00 EUR 15,3000 EUR -1,634
                                        // 2490658604 LU0114760746 / 941034 04.01.2016 10,848
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin", "wkn") //
                                                        .match("^(?<note>Entgeltbelastung) (?<name>.*) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) \\-[\\.,\\d]+$") //
                                                        .match("^[\\d]+ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\/ (?<wkn>[A-Z0-9]{6}) .*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Entgeltbelastung Templeton Growth (Euro) Fund 25,00 EUR 15,3000 EUR -1,634
                        // @formatter:on
                        .section("shares") //
                        .match("^Entgeltbelastung .* ([\\-|\\+])?(?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                        // @formatter:on
                        .section("date") //
                        .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Verwahrentgelt Fonds ohne Abschlussfolgeprovision 2019 1,00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Verwahrentgelt Fonds ohne Abschlussfolgeprovision .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Entgeltbelastung .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} \\-[\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                        // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                        // @formatter:on
                        .section("gross", "baseCurrency", "exchangeRate", "termCurrency").optional() //
                        .match("^Entgeltbelastung .* (?<gross>[\\.,\\d]+) (?<baseCurrency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} \\-[\\.,\\d]+$") //
                        .match("^[\\d]+ .* \\/ .* (?<exchangeRate>[\\.,\\d]+) (?<termCurrency>[\\w]{3}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // 540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                        // 2490658604 LU0114760746 / 941034 04.01.2016 10,848
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^(?<note1>Entgeltbelastung) .* (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ (?<fxCurrency>[\\w]{3}) \\-[\\.,\\d]+$") //
                        .match("^(?<note2>[\\d]+) .* \\/ .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$")
                        .assign((t, v) -> t.setNote("Auftrags-Nr. " + v.get("note2") + " | " + v.get("note1")))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addFeeForAccountBlock(type);
    }

    private void addFeeForAccountBlock(DocumentType type)
    {
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Entgeltbelastung .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                                        // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "wkn", "isin") //
                                                        .match("^Entgeltbelastung (?<name>.*) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) \\-[\\.,\\d]+$") //
                                                        .match("^[\\d]+ (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Entgeltbelastung Templeton Growth (Euro) Fund 25,00 EUR 15,3000 EUR -1,634
                                        // 2490658604 LU0114760746 / 941034 04.01.2016 10,848
                                        // @formatter:on
                                        section -> section.attributes("name", "currency", "isin", "wkn") //
                                                        .match("^Entgeltbelastung (?<name>.*) [\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3}) \\-[\\.,\\d]+$") //
                                                        .match("^[\\d]+ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) \\/ (?<wkn>[A-Z0-9]{6}) .*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Entgeltbelastung Templeton Growth (Euro) Fund 25,00 EUR 15,3000 EUR -1,634
                        // @formatter:on
                        .section("shares") //
                        .match("^Entgeltbelastung .* ([\\-|\\+])?(?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                        // @formatter:on
                        .section("date") //
                        .match("^.* (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\.,\\d]+$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( //
                                        // @formatter:off
                                        // lfd. Vermögensverwaltungsentgelt gem. separatem Auftrag 0,43 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^lfd\\. Verm.gensverwaltungsentgelt gem. separatem Auftrag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Entgeltbelastung .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} \\-[\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                        // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                        // @formatter:on
                        .section("gross", "baseCurrency", "exchangeRate", "termCurrency").optional() //
                        .match("^Entgeltbelastung .* (?<gross>[\\.,\\d]+) (?<baseCurrency>[\\w]{3}) [\\.,\\d]+ [\\w]{3} \\-[\\.,\\d]+$") //
                        .match("^[\\d]+ .* \\/ .* (?<exchangeRate>[\\.,\\d]+) (?<termCurrency>[\\w]{3}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            Money fxGross = rate.convert(rate.getTermCurrency(), gross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                        // 2490658604 LU0114760746 / 941034 04.01.2016 10,848
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>[\\d]+) .* \\/ .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$") //
                        .assign((t, v) -> t.setNote("Auftrags-Nr. " + v.get("note")))

                        // @formatter:off
                        // Depotführungsentgelt 2018 25,00 EUR
                        // Depotführungsentgelt 25,00 EUR
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Depotf.hrungsentgelt( [\\d]+)?) [\\.,\\d]+ [\\w]{3}$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        // @formatter:off
                        // Verwahrentgelt Fonds ohne Abschlussfolgeprovision 2019 1,00 EUR
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^(?<note1>Verwahrentgelt Fonds) .* (?<note2>[\\d]{4}) .*$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note1") + " " + v.get("note2"), " | ")))

                        // @formatter:off
                        // lfd. Vermögensverwaltungsentgelt gem. separatem Auftrag 0,09 EUR
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>lfd\\. Verm.gensverwaltungsentgelt) .*$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | ")))

                        .wrap(TransactionItem::new);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Aussch.ttungsanzeige|Datum der Aussch.ttung)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Fondsname .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Fondsname iShs S&P SmallCap 600 UCITS ET Datum der Ausschüttung 25.07.2018
                        // WKN / ISIN A0Q1YY / IE00B2QWCY14 Turnus halbjährlich
                        // Ausschüttung               0,289000000 USD 0,03 EUR
                        // @formatter:on
                        .section("name", "wkn", "isin", "currency") //
                        .match("^Fondsname (?<name>.*) Datum der Aussch.ttung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                        .match("^WKN \\/ ISIN (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*$") //
                        .match("^Aussch.ttung( vor Teilfreistellung)? ([\\s]+)?[\\.,\\d]+ [\\w]{3} [\\.,\\d]+ (?<currency>[\\w]{3})") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Fondsgesellschaft iShares Anteilsbestand per 12.07.2018 0,0930 St.
                        // @formatter:on
                        .section("shares") //
                        .match("^.* Anteilsbestand per [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<shares>[\\.,\\d]+) St\\.$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Fondsname iShs S&P SmallCap 600 UCITS ET Datum der Ausschüttung 25.07.2018
                        // @formatter:on
                        .section("date") //
                        .match("^Fondsname .* Datum der Aussch.ttung (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Folgender Betrag wurde zugunsten Ihrer Referenzbankverbindung überwiesen 0,03 EUR
                                        // Folgender Betrag wurde auf Ihr Abwicklungskonto überwiesen 15,46 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Folgender Betrag .* .berwiesen (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // zur Wiederanlage zur Verfügung stehend 15,67 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^zur Wiederanlage zur Verf.gung stehend (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // WKN / ISIN A1JSY2 / LU0731782826 Turnus monatlich
                        // @formatter:on
                        .section("note").optional() //
                        .match("^WKN \\/ ISIN .* \\/ .* (?<note>Turnus .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Kapitalertragsteuer               0,00 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("sale"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // abgeführte Kapitalertragsteuer 21,15 EUR
                        // Abgeführte Kapitalertragsteuer 0,06 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^(Abgef.hrte|abgef.hrte) Kapitalertrags(s)?teuer ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().getBoolean("sale"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Solidaritätszuschlag               0,00 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("sale"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // abgeführter Solidaritätszuschlag 1,16 EUR
                        // Abgeführter Solidaritätszuschlag 0,00 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^(Abgef.hrter|abgef.hrter) Solidarit.tszuschlag ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().getBoolean("sale"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // Kirchensteuer               0,00 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("sale"))
                                processTaxEntries(t, v, type);
                        })

                        // @formatter:off
                        // abgeführte Kirchensteuer 0,91 EUR
                        // Abgeführte Kirchensteuer 2 0,00 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^(Abgef.hrte|abgef.hrte) Kirchensteuer ([\\s]+)?(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().getBoolean("sale"))
                                processTaxEntries(t, v, type);
                        });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Ausgabeaufschlag / Provision (0,00 %) 0,00 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Ausgabeaufschlag \\/ Provision .* (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Additional Trading Costs 0,52 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Additional Trading Costs (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // ETF Transaktionskosten FFB 0,00 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^ETF Transaktionskosten FFB (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}