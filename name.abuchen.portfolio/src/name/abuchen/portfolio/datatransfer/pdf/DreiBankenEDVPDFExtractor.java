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
public class DreiBankenEDVPDFExtractor extends AbstractPDFExtractor
{
    public DreiBankenEDVPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("91810s/Klagenfurt"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return "3BankenEDV"; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "3BankenEDV"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType(".*(Kauf|Verkauf).*");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block(".*(Kauf|Verkauf).*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional().match("Wertpapier-Abrechnu.*(?<type>Verkauf?).*") //
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // LU0675401409 Lyxor Emerg Market 2x Lev ETF Zugang Stk
                // . 2,00
                // Inhaber-Anteile I o.N.
                .section("isin", "name", "shares", "nameContinued")
                .match("(?<isin>[\\w]{12}.*?) (?<name>.*?) (Zugang|Abgang).*(?<shares>[\\d.]+(,\\d+)).*")
                .match("(?<nameContinued>.*)").assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Handelszeitpunkt: 04.01.2021 12:05:55
                .section("date", "time")
                .match("^(Handelszeitpunkt:).*(?<date>\\d+.\\d+.\\d{4}+) (?<time>\\d+:\\d+:\\d+).*")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Wertpapierrechnung Wert 06.01.2021 EUR 205,30
                .section("currency", "amount")
                .match("^(Wertpapierrechn.* Wert) (\\d+.\\d+.\\d{4}+) (?<currency>[\\w]{3}) *(?<amount>[\\d.-]+,\\d+).*")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType(".*(Ausschüttung|Dividende).*");
        this.addDocumentTyp(type);

        Block block = new Block(".*(Ausschüttung|Dividende).*");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // IE00B0M63284 iShs Euro.Property Yield U.ETF Stk .
                // 4,00
                // Registered Shares EUR (Dist)oN
                .section("isin", "name", "shares", "nameContinued")
                .match("(?<isin>[\\w]{12}.*?) (?<name>.*?) (Stk .).*(?<shares>[\\d.]+(,\\d+)).*")
                .match("(?<nameContinued>.*)").assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Wertpapierrechnung Wert 23.12.2020 EUR 0,30
                .section("currency", "amount")
                .match("^(Wertpapierrechn.* Wert) (\\d+.\\d+.\\d{4}+) (?<currency>[\\w]{3}) *(?<amount>[\\d.-]+,\\d+).*")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Extag 10.12.2020
                .section("date") //
                .match("^Extag (?<date>\\d+.\\d+.\\d{4}+).*")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Ertrag 0,68 USD Kurswert USD 2,04Quellensteuer USD
                // -0,31
                // Auslands-KESt USD -0,26
                // Zwischensumme USD 1,47
                // 15 % QUSt a 1,224 v. 28.12.2020 EUR 1,21
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match(".*Kurswert.*(?<fxCurrency>[\\w]{3}).*(?<fxAmount>[\\d.]+,\\d+).*\\w+.*([\\w]{3}).*([\\d.]+,\\d+)")
                .match("\\w.*")
                .match("\\w.*")
                .match(".*(?<exchangeRate>[\\d.]+,\\d+) v. (\\d+.\\d+.\\d{4}).*(?<currency>[\\w]{3}).*(?<amount>[\\d.]+,\\d+).*")
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

                // Ertrag 0,09 USD Kurswert USD                0,54KESt-Neu USD               -0,15
                // Zwischensumme USD                0,39
                // a 1,214 v. 09.12.2020 EUR                0,32
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match(".*Kurswert.*(?<fxCurrency>[\\w]{3}).*(?<fxAmount>[\\d.]+,\\d+).*\\w+.*([\\w]{3}).*([\\d.]+,\\d+)")
                .match("\\w.*")
                .match(".*(?<exchangeRate>[\\d.]+,\\d+) v. (\\d+.\\d+.\\d{4}).*(?<currency>[\\w]{3}).*(?<amount>[\\d.]+,\\d+).*")
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
                // Auslands-KESt USD -0,26
                .section("tax", "currency").optional()
                .match("Auslands-KESt.*(?<currency>[\\w]{3}).*-(?<tax>[\\d.]+,\\d+).*")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // 0,0204 EUR KESt
                .section("tax", "currency").optional()
                .match(".*(?<tax>[\\d.]+,\\d+).*(?<currency>[\\w]{3}).*KESt")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Ertrag 0,0806 EUR Kurswert EUR 0,32KESt-Neu EUR -0,02
                .section("tax", "currency").optional()
                .match(".*KESt-Neu.*(?<currency>[\\w]{3}).*-(?<tax>[\\d.]+,\\d+).*")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Ertrag 0,68 USD Kurswert USD 2,04Quellensteuer USD
                // -0,31
                .section("tax", "currency").optional()
                .match(".*Quellensteuer.*(?<currency>[\\w]{3}).*-(?<tax>[\\d.]+,\\d+).*")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kursgewinn-KESt EUR -3,37
                .section("tax", "currency").optional()
                .match("^(Kursgewinn-KESt) (?<currency>[\\w]{3}).*(?<tax>-[\\d.]+,\\d{2})")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Dritt- und Börsengebühr EUR 0,02
                .section("fee", "currency").optional()
                .match("^(Dritt.*B.*sengeb.*) (?<currency>[\\w]{3}).*(?<fee>[\\d.-]+,\\d+).*")
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
            PDFExtractorUtils.checkAndSetTax(tax,
                            ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
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
