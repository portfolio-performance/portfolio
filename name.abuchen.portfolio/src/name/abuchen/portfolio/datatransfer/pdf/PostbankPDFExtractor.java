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
public class PostbankPDFExtractor extends AbstractPDFExtractor
{
    private static final String FLAG_WITHHOLDING_TAX_FOUND  = "exchangeRate"; //$NON-NLS-1$

    public PostbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Postbank"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return "Deutsche Postbank AG"; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "Postbank"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung (Kauf|Verkauf|Ausgabe Investmentfonds)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("Wertpapier Abrechnung (Kauf|Verkauf|Ausgabe Investmentfonds).*");
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

                // Stück 158 XTR.(IE) - MSCI WORLD              IE00BJ0KDQ92 (A1XB5U)
                // REGISTERED SHARES 1C O.N.   
                .section("isin", "wkn", "name", "shares", "name1")
                .match("^St.ck (?<shares>[\\d.,]+) (?<name>.*) (?<isin>[\\w]{12}.*) (\\((?<wkn>.*)\\).*)")
                .match("(?<name1>.*)")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Handels-/Ausf.hrungsplatz"))
                        v.put("name", v.get("name").trim() + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Schlusstag 05.02.2020
                .section("date").optional()
                .match("^Schlusstag (?<date>\\d+.\\d+.\\d{4})")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Schlusstag/-Zeit 04.02.2020 08:00:04 Auftragserteilung/ -ort Online-Banking
                .section("date", "time").optional()
                .match("^Schlusstag\\/-Zeit (?<date>\\d+.\\d+.\\d{4}) (?<time>\\d+:\\d+:\\d+) .*")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Ausmachender Betrag 9.978,18- EUR
                .section("amount", "currency")
                .match("^Ausmachender Betrag (?<amount>[.,\\d]+)[\\+-]? (?<currency>[\\w]{3})")
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
        DocumentType type = new DocumentType("(Dividendengutschrift|Ausschüttung Investmentfonds|Ertragsgutschrift)");
        this.addDocumentTyp(type);

        Block block = new Block("(Dividendengutschrift|Ausschüttung Investmentfonds|Ertragsgutschrift .*)");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction

                // Stück 12 JOHNSON & JOHNSON  SHARES US4781601046 (853260)
                // REGISTERED SHARES DL 1
                .section("isin", "wkn", "name", "shares", "name1")
                .match("^St.ck (?<shares>[\\d.,]+) (?<name>.*) (?<isin>[\\w]{12}.*) (\\((?<wkn>.*)\\).*)")
                .match("(?<name1>.*)")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                        v.put("name", v.get("name").trim() + " " + v.get("name1"));
                    
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Ex-Tag 22.02.2021 Art der Dividende Quartalsdividende
                .section("date")
                .match("^Ex-Tag (?<date>\\d+.\\d+.\\d{4}).*")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                })

                // Ex-Tag 22.02.2021 Art der Dividende Quartalsdividende
                .section("note").optional()
                .match("^Ex-Tag \\d+.\\d+.\\d{4} Art der Dividende (?<note>.*)")
                .assign((t, v) -> {
                    t.setNote(v.get("note"));
                })

                // Ausmachender Betrag 8,64+ EUR
                .section("currency", "amount")
                .match("^Ausmachender Betrag (?<amount>[\\d.]+,\\d+)\\+ (?<currency>\\w{3})")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Devisenkurs EUR / USD  1,1920
                // Devisenkursdatum 09.03.2021
                // Dividendengutschrift 12,12 USD 10,17+ EUR
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match("^Devisenkurs .* (?<exchangeRate>[\\d.]+,\\d+)$")
                .match("^Devisenkursdatum .*")
                .match("^(Dividendengutschrift|Aussch.ttung) (?<fxAmount>[\\d.]+,\\d+) (?<fxCurrency>\\w{3}) (?<amount>[\\d.]+,\\d+)\\+ (?<currency>\\w{3})$")                        
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
                // Einbehaltene Quellensteuer 15 % auf 12,12 USD 1,53- EUR
                .section("quellensteinbeh", "currency").optional()
                .match("^Einbehaltende Quellensteuer [.,\\d]+ % .* (?<quellensteinbeh>[.,\\d]+)- (?<currency>[\\w]{3})$")
                .assign((t, v) ->  {
                    type.getCurrentContext().put(FLAG_WITHHOLDING_TAX_FOUND, "true");
                    addTax(type, t, v, "quellensteinbeh");
                })

                // Anrechenbare Quellensteuer 15 % auf 10,17 EUR 1,53 EUR
                .section("quellenstanr", "currency").optional()
                .match("^Anrechenbare Quellensteuer [.,\\d]+ % .* [.,\\d]+ \\w{3} (?<quellenstanr>[.,\\d]+) (?<currency>\\w{3})$")
                .assign((t, v) -> addTax(type, t, v, "quellenstanr"));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Provision 39,95- EUR
                .section("fee", "currency").optional()
                .match("^Provision (?<fee>[.,\\d]+)- (?<currency>[\\w]{3}).*")
                .assign((t, v) -> processFeeEntries(t, v, type))
        
                // Abwicklungskosten Börse 0,04- EUR
                .section("fee", "currency").optional()
                .match("^Abwicklungskosten B.rse (?<fee>[.,\\d]+)- (?<currency>[\\w]{3}).*")
                .assign((t, v) -> processFeeEntries(t, v, type))
        
                // Transaktionsentgelt Börse 11,82- EUR
                .section("fee", "currency").optional()
                .match("^Transaktionsentgelt B.rse (?<fee>[.,\\d]+)- (?<currency>[\\w]{3}).*")
                .assign((t, v) -> processFeeEntries(t, v, type))
        
                // Übertragungs-/Liefergebühr 0,65- EUR
                .section("fee", "currency").optional()
                .match("^.bertragungs-\\/Liefergeb.hr (?<fee>[.,\\d]+)- (?<currency>[\\w]{3}).*")
                .assign((t, v) -> processFeeEntries(t, v, type));
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
