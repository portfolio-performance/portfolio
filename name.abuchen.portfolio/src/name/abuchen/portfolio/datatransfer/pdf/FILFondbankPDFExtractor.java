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
public class FILFondbankPDFExtractor extends AbstractPDFExtractor
{
    public FILFondbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("FIL Fondsbank GmbH"); //$NON-NLS-1$

        addBuySellTransaction();
        addSellForAccountFeeTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "FIL Fondsbank GmbH (Fidelity Group)"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(Splittkauf|Wiederanlage|Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(Splittkauf|Wiederanlage|Kauf|Verkauf) .*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(?<type>(Splittkauf|Wiederanlage|Kauf|Verkauf)) .*$")
                .assign((t, v) -> {
                    /***
                     * If we have a sell transaction, we set a flag.
                     * 
                     * So that in the case of a possible charge by sale, (Entgeltbelastung),
                     * taxes are not included if they are in the block.
                     * 
                     * Otherwise the flag will be removed.
                     */
                    type.getCurrentContext().remove("sale");
                    
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                        type.getCurrentContext().put("sale", "X");
                    }
                })

                // Kauf UBS Emer.Mkt.Soc.Resp.UETFADIS 89,56 EUR 12,6399 USD 7,728
                // 2540401213 A110QD / LU1048313891 1,090667 USD 02.10.2019 45,394
                .section("name", "currency", "shares", "wkn", "isin")
                .match("^(Splittkauf|Wiederanlage|Kauf|Verkauf)( Betrag)? (?<name>.*) [.,\\d]+ [\\w]{3} [.,\\d]+ (?<currency>[\\w]{3}) ([-|+])?(?<shares>[.,\\d]+)$")
                .match("^[\\d]+ (?<wkn>.*) \\/ (?<isin>[\\w]{12}) .*$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // 2536717769 A0X97T / LU0446734526 1,125168 USD 16.04.2019 1,334
                .section("date").optional()
                .match("^.* (?<date>\\d+.\\d+.\\d{4}) [.,\\d]+$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // 2536717769 A0X97T / LU0446734526 1,125168 USD 16.04.2019 1,334
                // Handelsuhrzeit 16:51:24
                .section("date", "time").optional()
                .match("^.* (?<date>\\d+.\\d+.\\d{4}) [.,\\d]+$")
                .match("^Handelsuhrzeit (?<time>\\d+:\\d+:\\d+)$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Abrechnungsbetrag 1,77 EUR
                // Auszahlungsbetrag 0,00 EUR
                .section("amount", "currency").optional()
                .match("^(Abrechnungsbetrag|Auszahlungsbetrag) (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Splittkauf Betrag UBS Msci Pacific exJap.U ETF A 1,77 EUR 44,4324 USD 0,045
                // 2536717769 A0X97T / LU0446734526 1,125168 USD 16.04.2019 1,334
                .section("forex", "exchangeRate", "currency", "amount").optional()
                .match("^(Splittkauf|Wiederanlage|Kauf|Verkauf)( Betrag)? .* (?<amount>[.,\\d]+) (?<currency>[\\w]{3}) [.,\\d]+ (?<forex>[\\w]{3}) ([-|+])?[.,\\d]+$")
                .match("^[\\d]+ .* \\/ [\\w]{12} (?<exchangeRate>[.,\\d]+) [\\w]{3} \\d+.\\d+.\\d{4} [.,\\d]+$")
                .assign((t, v) -> {
                    // read the forex currency, exchange rate, account
                    // currency and gross amount in account currency
                    String forex = asCurrencyCode(v.get("forex"));

                    if (t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                    {
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // gross given in account currency
                        long amount = asAmount(v.get("amount"));
                        long fxAmount = exchangeRate.multiply(BigDecimal.valueOf(amount))
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();

                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                        Money.of(asCurrencyCode(v.get("currency")), amount),
                                        Money.of(forex, fxAmount), reverseRate);

                        t.getPortfolioTransaction().addUnit(grossValue);
                    }
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addSellForAccountFeeTransaction()
    {
        DocumentType type = new DocumentType("Entgeltbelastung");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });

        Block firstRelevantLine = new Block("^Entgeltbelastung .*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                /***
                 * We have a sell transaction, we set a flag.
                 */
                .section("type").optional()
                .match("^(?<type>Entgeltbelastung) .*$")
                .assign((t, v) -> {
                    type.getCurrentContext().put("sale", "X");
                })

                // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                .section("name", "currency", "shares", "wkn", "isin")
                .match("^Entgeltbelastung (?<name>.*) [.,\\d]+ [\\w]{3} [.,\\d]+ (?<currency>[\\w]{3}) -(?<shares>[.,\\d]+)$")
                .match("^[\\d]+ (?<wkn>.*) \\/ (?<isin>[\\w]{12}) .*$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                .section("date")
                .match("^.* (?<date>\\d+.\\d+.\\d{4}) [.,\\d]+$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                .section("amount", "currency").optional()
                .match("^Entgeltbelastung .* (?<amount>[.,\\d]+) (?<currency>[\\w]{3}) [.,\\d]+ [\\w]{3} -[.,\\d]+$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Verwahrentgelt Fonds ohne Abschlussfolgeprovision 2019 1,00 EUR
                .section("amount", "currency").optional()
                .match("^Verwahrentgelt Fonds ohne Abschlussfolgeprovision .* (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                .section("forex", "exchangeRate", "currency", "amount").optional()
                .match("^Entgeltbelastung .* (?<amount>[.,\\d]+) (?<currency>[\\w]{3}) [.,\\d]+ (?<forex>[\\w]{3}) -[.,\\d]+$")
                .match("^[\\d]+ .* \\/ [\\w]{12} (?<exchangeRate>[.,\\d]+) [\\w]{3} \\d+.\\d+.\\d{4} [.,\\d]+$")
                .assign((t, v) -> {
                    // read the forex currency, exchange rate, account
                    // currency and gross amount in account currency
                    String forex = asCurrencyCode(v.get("forex"));

                    if (t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                    {
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // gross given in account currency
                        long amount = asAmount(v.get("amount"));
                        long fxAmount = exchangeRate.multiply(BigDecimal.valueOf(amount))
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();

                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                        Money.of(asCurrencyCode(v.get("currency")), amount),
                                        Money.of(forex, fxAmount), reverseRate);

                        t.getPortfolioTransaction().addUnit(grossValue);
                    }
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addFeeForAccountBlock(type);
    }

    private void addFeeForAccountBlock(DocumentType type)
    {
        /***
         * If changes are made in this area,
         * the sell transaction function must be adjusted.
         * addSellForAccountFeeTransaction();
         */
        Block block = new Block("^Entgeltbelastung .*$");
        type.addBlock(block);       
        block.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.FEES);
            return t;
        })

                // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                .section("name", "currency", "shares", "wkn", "isin")
                .match("^Entgeltbelastung (?<name>.*) [.,\\d]+ [\\w]{3} [.,\\d]+ (?<currency>[\\w]{3}) -(?<shares>[.,\\d]+)$")
                .match("^[\\d]+ (?<wkn>.*) \\/ (?<isin>[\\w]{12}) .*$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                .section("date")
                .match("^.* (?<date>\\d+.\\d+.\\d{4}) [.,\\d]+$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                .section("amount", "currency").optional()
                .match("^Entgeltbelastung (?<name>.*) (?<amount>[.,\\d]+) (?<currency>[\\w]{3}) [.,\\d]+ [\\w]{3} -(?<shares>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // lfd. Vermögensverwaltungsentgelt gem. separatem Auftrag 0,43 EUR
                .section("amount", "currency").optional()
                .match("^lfd. Verm.gensverwaltungsentgelt gem. separatem Auftrag (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Entgeltbelastung UBS-MSCI WLD.SOC.RESP.A.D.E. 1,01 EUR 98,1753 USD -0,011
                // 2540818151 A1JA1R / LU0629459743 1,113832 USD 15.10.2019 17,187
                .section("forex", "exchangeRate", "currency", "amount").optional()
                .match("^Entgeltbelastung .* (?<amount>[.,\\d]+) (?<currency>[\\w]{3}) [.,\\d]+ (?<forex>[\\w]{3}) ([-|+])?[.,\\d]+$")
                .match("^[\\d]+ .* \\/ [\\w]{12} (?<exchangeRate>[.,\\d]+) [\\w]{3} \\d+.\\d+.\\d{4} [.,\\d]+$")
                .assign((t, v) -> {
                    // read the forex currency, exchange rate, account
                    // currency and gross amount in account currency
                    String forex = asCurrencyCode(v.get("forex"));

                    if (t.getSecurity().getCurrencyCode().equals(forex))
                    {
                        BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                        BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // gross given in account currency
                        long amount = asAmount(v.get("amount"));
                        long fxAmount = exchangeRate.multiply(BigDecimal.valueOf(amount))
                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();

                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                        Money.of(asCurrencyCode(v.get("currency")), amount),
                                        Money.of(forex, fxAmount), reverseRate);

                        t.addUnit(grossValue);
                    }
                })

                .wrap(TransactionItem::new));
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Ausschüttungsanzeige|Datum der Ausschüttung)");
        this.addDocumentTyp(type);

        Block block = new Block("^Fondsname .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);

                /***
                 * If we have multiple entries in the document,
                 * then the "sale" flag must be removed.
                 */
                type.getCurrentContext().remove("sale");

                return entry;
            });

        pdfTransaction
                // Fondsname iShs S&P SmallCap 600 UCITS ET Datum der Ausschüttung 25.07.2018
                // WKN / ISIN A0Q1YY / IE00B2QWCY14 Turnus halbjährlich
                // Ausschüttung               0,289000000 USD 0,03 EUR
                .section("name", "wkn", "isin", "shares", "currency")
                .match("^Fondsname (?<name>.*) Datum der Aussch.ttung \\d+.\\d+.\\d{4}$")
                .match("^WKN \\/ ISIN (?<wkn>.*) \\/ (?<isin>[\\w]{12}) .*$")
                .match("^Fondsgesellschaft .* Anteilsbestand per \\d{2}.\\d{2}.\\d{4} (?<shares>[\\d.,]+) St.$")
                .match("^Aussch.ttung( vor Teilfreistellung)? ([\\s]+)?[.,\\d]+ [\\w]{3} [.,\\d]+ (?<currency>[\\w]{3})")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Fondsname iShs S&P SmallCap 600 UCITS ET Datum der Ausschüttung 25.07.2018
                .section("date").optional()
                .match("^Fondsname (?<name>.*) Datum der Aussch.ttung (?<date>\\d+.\\d+.\\d{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Folgender Betrag wurde zugunsten Ihrer Referenzbankverbindung überwiesen 0,03 EUR
                .section("amount", "currency").optional()
                .match("^Folgender Betrag wurde zugunsten Ihrer Referenzbankverbindung .berwiesen (?<amount>[\\d.,]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // zur Wiederanlage zur Verfügung stehend 15,67 EUR
                .section("amount", "currency").optional()
                .match("^zur Wiederanlage zur Verf.gung stehend (?<amount>[\\d.,]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Kapitalertragsteuer               0,00 EUR
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer ([\\s]+)?(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("sale")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // abgeführte Kapitalertragsteuer 21,15 EUR
                .section("tax", "currency").optional()
                .match("^abgef.hrte Kapitalertragsteuer ([\\s]+)?(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if ("X".equals(type.getCurrentContext().get("sale")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Solidaritätszuschlag               0,00 EUR
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag ([\\s]+)(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("sale")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // abgeführter Solidaritätszuschlag 1,16 EUR
                .section("tax", "currency").optional()
                .match("^abgeführter Solidarit.tszuschlag ([\\s]+)?(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if ("X".equals(type.getCurrentContext().get("sale")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // Kirchensteuer               0,00 EURR
                .section("tax", "currency").optional()
                .match("^Kirchensteuer ([\\s]+)(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("sale")))
                    {
                        processTaxEntries(t, v, type);
                    }
                })

                // abgeführte Kirchensteuer 0,91 EUR
                .section("tax", "currency").optional()
                .match("^abgef.hrte Kirchensteuer ([\\s]+)?(?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if ("X".equals(type.getCurrentContext().get("sale")))
                    {
                        processTaxEntries(t, v, type);
                    }
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Ausgabeaufschlag / Provision (0,00 %) 0,00 EUR
                .section("currency", "fee").optional()
                .match("^Ausgabeaufschlag \\/ Provision .* (?<fee>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Additional Trading Costs 0,52 EUR
                .section("currency", "fee").optional()
                .match("^Additional Trading Costs (?<fee>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // ETF Transaktionskosten FFB 0,00 EUR
                .section("currency", "fee").optional()
                .match("^ETF Transaktionskosten FFB (?<fee>[.,\\d]+) (?<currency>[\\w]{3})$")
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