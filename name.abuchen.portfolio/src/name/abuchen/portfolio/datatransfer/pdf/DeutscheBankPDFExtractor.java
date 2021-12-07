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
public class DeutscheBankPDFExtractor extends AbstractPDFExtractor
{
    public DeutscheBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Deutsche Bank"); //$NON-NLS-1$
        addBankIdentifier("DB Privat- und Firmenkundenbank AG"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
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
            
            /***
             * If we have multiple entries in the document,
             * with fee and fee refunds,
             * then the "noProvision" flag must be removed.
             */
            type.getCurrentContext().remove("noProvision");

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
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // 123 1234567 00 BASF SE
                // WKN BASF11 Nominal ST 19
                // ISIN DE000BASF111 Kurs EUR 35,00
                .section("name", "wkn", "shares", "isin", "currency")
                .match("^[\\d]+ [ \\d]+ (?<name>.*)$")
                .match("^WKN (?<wkn>.*) Nominal ST (?<shares>[.,\\d]+)$")
                .match("^ISIN (?<isin>[\\w]{12}) Kurs (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Belegnummer 1694278628 / 24281 Schlusstag 20.08.2019
                .section("date").optional()
                .match("^Belegnummer .* Schlusstag (?<date>\\d+.\\d+.\\d{4})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Belegnummer 1234567890 / 123456 Schlusstag/-zeit MEZ 02.04.2015 / 09:04
                .section("date", "time").optional()
                .match("^Belegnummer .* Schlusstag\\/-zeit .* (?<date>\\d+.\\d+.\\d{4}) \\/ (?<time>\\d+:\\d+)$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })
                
                // Buchung auf Kontonummer 1234567 40 mit Wertstellung 08.04.2015 EUR 675,50
                .section("amount", "currency").optional()
                .match("^Buchung auf Kontonummer [ \\d]+ mit Wertstellung \\d+.\\d+.\\d{4} (?<currency>[\\w]{3}) (?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Dividendengutschrift|Ertragsgutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividendengutschrift|Ertragsgutschrift)$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // 380,000000 878841 US17275R1023
                // CISCO SYSTEMS INC.REGISTERED SHARES DL-,001
                // Dividende pro Stück 0,2600000000 USD Zahlbar 15.12.2014
                .section("shares", "wkn", "isin", "name", "currency")
                .match("^(?<shares>[.,\\d]+) (?<wkn>.*) (?<isin>[\\w]{12})$")
                .match("^(?<name>.*)$")
                .match("^(Dividende|Aussch.ttung) pro St.ck [.,\\d]+ (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Gutschrift mit Wert 15.12.2014 64,88 EUR
                .section("date").optional()
                .match("^Gutschrift mit Wert (?<date>\\d+.\\d+.\\d{4}) [.,\\d]+ [\\w]{3}$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Gutschrift mit Wert 15.12.2014 64,88 EUR
                .section("amount", "currency")
                .match("^Gutschrift mit Wert \\d+.\\d+.\\d{4} (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // Bruttoertrag 98,80 USD 87,13 EUR
                // Umrechnungskurs USD zu EUR 1,1339000000
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match("^Bruttoertrag (?<fxAmount>[.,\\d]+) (?<fxCurrency>[\\w]{3}) (?<amount>[.,\\d]+) (?<currency>[\\w]{3})$")
                .match("^Umrechnungskurs [\\w]{3} zu [\\w]{3} (?<exchangeRate>[.,\\d]+)$")
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

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Kapitalertragsteuer (KESt)  - 9,88 USD - 8,71 EUR
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer \\(KESt\\) ([\\s]+)?- [.,\\d]+ [\\w]{3} - (?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kapitalertragsteuer (KESt) - 4,28 EUR
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer \\(KESt\\) ([\\s]+)?- (?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kapitalertragsteuer EUR -122,94
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer (?<currency>[\\w]{3}) -(?<tax>[.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag auf KESt - 0,53 USD - 0,47 EUR
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag auf KESt ([\\s]+)?- [.,\\d]+ [\\w]{3} - (?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag auf KESt - 0,23 EUR
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag auf KESt ([\\s]+)?- (?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag auf Kapitalertragsteuer EUR -6,76
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag auf Kapitalertragsteuer (?<currency>[\\w]{3}) -(?<tax>[.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kirchensteuer auf Kapitalertragsteuer EUR -1,23
                .section("tax", "currency").optional()
                .match("^Kirchensteuer auf Kapitalertragsteuer (?<currency>[\\w]{3}) -(?<tax>[.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Anrechenbare ausländische Quellensteuer 13,07 EUR
                .section("tax", "currency").optional()
                .match("^Anrechenbare ausl.ndische Quellensteuer (?<tax>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        /***
         * If the provision fee and the fee refund are the same, 
         * then we set a flag and don't book provision fee
         */
        transaction
                // Provision EUR 1,26
                // Ausgekehrte Zuwendungen, die die Bank von DWS Xtrackers erhält EUR -1,26
                .section("currency", "fee", "feeRefund").optional()
                .match("^Provision (?<currency>[\\w]{3}) ([-])?(?<fee>[.,\\d]+)$")
                .match("^Ausgekehrte Zuwendungen, .* (?<currency>[\\w]{3}) -(?<feeRefund>[.,\\d]+)$")
                .assign((t, v) -> {
                    if (v.get("fee").equals(v.get("feeRefund")))
                    {
                        type.getCurrentContext().put("noProvision", "X");
                    }
                });

        transaction
                // Provision EUR 7,90
                // Provision EUR -7,90
                .section("currency", "fee").optional()
                .match("^Provision (?<currency>[\\w]{3}) ([-])?(?<fee>[.,\\d]+)$")
                .assign((t, v) -> {
                    if (!"X".equals(type.getCurrentContext().get("noProvision")))
                    {
                        processFeeEntries(t, v, type);
                    }
                })

                // Provision (0,25 %) EUR 8,78
                // Provision (0,25 %) EUR -8,78
                .section("currency", "fee").optional()
                .match("^Provision \\([.,\\d]+ %\\) (?<currency>[\\w]{3}) ([-])?(?<fee>[.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // XETRA-Kosten EUR 0,60
                // XETRA-Kosten EUR -0,60
                .section("currency", "fee").optional()
                .match("^XETRA-Kosten (?<currency>[\\w]{3}) ([-])?(?<fee>[.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Weitere Provision der Bank bei der börslichen Orderausführung EUR 2,00
                .section("currency", "fee").optional()
                .match("^Weitere Provision .* (?<currency>[\\w]{3}) ([-])?(?<fee>[.,\\d]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Fremde Spesen und Auslagen EUR -5,40
                .section("currency", "fee").optional()
                .match("^Fremde Spesen und Auslagen (?<currency>[\\w]{3}) ([-])?(?<fee>[.,\\d]+)$")
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
