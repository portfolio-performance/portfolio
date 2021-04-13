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
public class WeberbankPDFExtractor extends AbstractPDFExtractor
{
    private static final String FLAG_WITHHOLDING_TAX_FOUND  = "exchangeRate"; //$NON-NLS-1$

    public WeberbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("BLZ 101 201 00"); //$NON-NLS-1$
        addBankIdentifier("BLZ 10120100"); //$NON-NLS-1$
        addBankIdentifier("BIC WELADED1WBB"); //$NON-NLS-1$

        addBuySellTransaction();
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
        return "Weberbank"; //$NON-NLS-1$
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

        Block firstRelevantLine = new Block("Wertpapier Abrechnung (Kauf|Verkauf).*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("Wertpapier Abrechnung (?<type>Verkauf).*")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Stück 4.440 NEL ASA NO0010081235 (A0B733)
                // NAVNE-AKSJER NK -,20 
                .section("shares", "name", "isin", "wkn", "name1")
                .match("^St.ck (?<shares>[\\d.,]+) (?<name>.*) (?<isin>[\\w]{12}) (\\((?<wkn>.*)\\).*)")
                .match("(?<name1>.*)")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Handels-/Ausf.hrungsplatz"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Schlusstag/-Zeit 26.03.2021 15:14:29 Auftraggeber XXXXXXXXXXXXX
                .section("date", "time")
                .match("^Schlusstag\\/-Zeit (?<date>\\d+.\\d+.\\d{4}) (?<time>\\d+:\\d+:\\d+) .*$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date").replaceAll("/", "."), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date").replaceAll("/", ".")));
                })

                // Ausmachender Betrag 9.978,18- EUR
                // Ausmachender Betrag 2.335,30 EUR
                .section("amount", "currency")
                .match("^Ausmachender Betrag (?<amount>[.,\\d.]+)[-]? (?<currency>[\\w]{3})")
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
        DocumentType type = new DocumentType("(Dividendengutschrift)");
        this.addDocumentTyp(type);

        Block block = new Block("(Dividendengutschrift)");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // Nominale Wertpapierbezeichnung ISIN (WKN)
                // Stück 107 APPLE INC. US0378331005 (865985)
                .section("shares", "name", "isin", "wkn", "name1")
                .match("^St.ck (?<shares>[.,\\d]+) (?<name>.*) (?<isin>\\w{12}) \\((?<wkn>.*)\\)$")
                .match("(?<name1>.*)")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Ex-Tag 07.08.2020 Art der Dividende Quartalsdividende
                .section("date")
                .match("^Ex-Tag (?<date>\\d+.\\d+.\\d{4}).*")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                })

                // Ex-Tag 07.08.2020 Art der Dividende Quartalsdividende
                .section("note").optional()
                .match("^Ex-Tag \\d+.\\d+.\\d{4} Art der Dividende (?<note>.*)")
                .assign((t, v) -> {
                    t.setNote(v.get("note"));
                })

                // Ausmachender Betrag 55,14+ EUR
                .section("currency", "amount")
                .match("^Ausmachender Betrag (?<amount>[\\d.]+,\\d+)\\+ (?<currency>\\w{3})")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Devisenkurs EUR / USD 1,1848
                // Devisenkursdatum 14.08.2020
                // Dividendengutschrift 87,74 USD 74,05+ EUR
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match("^Devisenkurs .* (?<exchangeRate>[\\d.]+,\\d+)$")
                .match("^Devisenkursdatum .*")
                .match("^Dividendengutschrift (?<fxAmount>[\\d.]+,\\d+) (?<fxCurrency>\\w{3}) (?<amount>[\\d.]+,\\d+)\\+ (?<currency>\\w{3})$")                        
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
                // Kapitalertragsteuer 25 % auf 29,61 EUR 7,40- EUR
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer [.,\\d]+ % .* (?<tax>[.,\\d]+)- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))
        
                // Solidaritätszuschlag 5,5 % auf 7,40 EUR 0,40- EUR
                .section("tax", "currency").optional()
                .match("^Solidaritätszuschlag [.,\\d]+ % .* (?<tax>[.,\\d]+)- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))
        
                // Einbehaltene Quellensteuer 15 % auf 87,74 USD 11,11- EUR
                .section("quellensteinbeh", "currency").optional()
                .match("^Einbehaltende Quellensteuer [.,\\d]+ % .* (?<quellensteinbeh>[.,\\d]+)- (?<currency>[\\w]{3})$")
                .assign((t, v) ->  {
                    type.getCurrentContext().put(FLAG_WITHHOLDING_TAX_FOUND, "true");
                    addTax(type, t, v, "quellensteinbeh");
                })
        
                // Anrechenbare Quellensteuer 15 % auf 74,05 EUR 11,11 EUR
                .section("quellenstanr", "currency").optional()
                .match("^Anrechenbare Quellensteuer [.,\\d]+ % .* (?<quellenstanr>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> addTax(type, t, v, "quellenstanr"));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        //transaction
            // At this time there are no known fees or similar in the PDF debugs.
            // IF you found some, add this here like
            
            // Example
            // some fees
            // .section("fee", "currency").optional()
            // .match("^ABC (?<fee>[.,\\d]+)[-]? (?<currency>[\\w]{3})$")
            // .assign((t, v) -> processFeeEntries(t, v, type));
    }

    private void addTax(DocumentType type, Object t, Map<String, String> v, String taxtype)
    {
        // Wenn es 'Einbehaltene Quellensteuer' gibt, dann die weiteren
        // Quellensteuer-Arten nicht berücksichtigen.
        if (checkWithholdingTax(type, taxtype))
        {
            ((name.abuchen.portfolio.model.Transaction) t)
                    .addUnit(new Unit(Unit.Type.TAX, 
                                    Money.of(asCurrencyCode(v.get("currency")), 
                                                    asAmount(v.get(taxtype)))));
        }
    }

    private boolean checkWithholdingTax(DocumentType type, String taxtype)
    {
        if (Boolean.valueOf(type.getCurrentContext().get(FLAG_WITHHOLDING_TAX_FOUND)))
        {
            if ("quellenstanr".equalsIgnoreCase(taxtype))
            { 
                return false; 
            }
        }
        return true;
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
