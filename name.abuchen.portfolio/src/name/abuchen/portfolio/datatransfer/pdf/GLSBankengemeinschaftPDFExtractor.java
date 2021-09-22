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
public class GLSBankengemeinschaftPDFExtractor extends AbstractPDFExtractor
{
    private static final String EXCHANGE_RATE = "exchangeRate"; //$NON-NLS-1$
    private static final String FLAG_WITHHOLDING_TAX_FOUND = "exchangeRate"; //$NON-NLS-1$

    public GLSBankengemeinschaftPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("GLS Bank"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "GLS Gemeinschaftsbank eG"; //$NON-NLS-1$
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

        Block firstRelevantLine = new Block("^Wertpapier Abrechnung (Kauf|Verkauf).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional().match("Wertpapier Abrechnung (?<type>Verkauf).*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Stück 2.700 INTERNAT. CONS. AIRL. GROUP SA
                // ES0177542018 (A1H6AJ)
                // ACCIONES NOM. EO -,10
                // Handels-/Ausführungsplatz XETRA (gemäß Weisung)
                // Kurswert 5.047,65- EUR
                .section("shares", "name", "isin", "wkn", "name1", "currency")
                .match("^St.ck (?<shares>[.,\\d]+) (?<name>.*) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\)$")
                .match("^(?<name1>.*)$").match("^Kurswert [.,\\d]+- (?<currency>[\\w]{3})$").assign((t, v) -> {
                    if (!v.get("name1").startsWith("Handels-/Ausführungsplatz"))
                        v.put("name", v.get("name").trim() + " " + v.get("name1").trim());

                    v.put("name", v.get("name").trim());
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Schlusstag/-Zeit 17.02.2021 09:04:10 Auftraggeber Max
                // Mustermann
                .section("date", "time")
                .match("^Schlusstag\\/-Zeit (?<date>[\\d]+.[\\d]+.[\\d]{4}) (?<time>[\\d]+:[\\d]+:[\\d]+) .*$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Ausmachender Betrag 5.109,01- EUR
                // Ausmachender Betrag 1.109,01+ EUR
                .section("amount", "currency")
                .match("^Ausmachender Betrag (?<amount>[.,\\d]+)[-|+] (?<currency>[\\w]{3})$")
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
        DocumentType type = new DocumentType("Dividendengutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("Dividendengutschrift");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // Stück 17 CD PROJEKT S.A. PLOPTTC00011 (534356)
                // INHABER-AKTIEN C ZY 1
                // Zahlbarkeitstag 08.06.2021 Dividende pro Stück 5,00
                // PLN
                .section("shares", "name", "isin", "wkn", "name1", "currency")
                .match("^St.ck (?<shares>[.,\\d]+) (?<name>.*) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\)$")
                .match("^(?<name1>.*)$").match("^.* Dividende pro St.ck [.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                        v.put("name", v.get("name").trim() + " " + v.get("name1").trim());

                    v.put("name", v.get("name").trim());
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Ex-Tag 31.05.2021
                .section("date").match("^Ex-Tag (?<date>\\d+.\\d+.\\d{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Ausmachender Betrag 13,28+ EUR
                .section("amount", "currency").optional()
                .match("^Ausmachender Betrag (?<amount>[.,\\d]+)[-|+] (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Devisenkurs EUR / PLN 4,5044
                // Dividendengutschrift 85,00 PLN 18,87+ EUR
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match("^Devisenkurs (?<fxCurrency>[\\w]{3}) \\/ (?<currency>[\\w]{3}) ([\\s]+)? (?<exchangeRate>[.,\\d]+)$")
                .match("^Dividendengutschrift (?<fxAmount>[.,\\d]+) (?<fxCurrency>[\\w]{3}) (?<amount>[.,\\d]+)[+] (?<currency>[\\w]{3})")
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
                // Finanztransaktionssteuer 10,10- EUR
                .section("tax", "currency").optional()
                .match("^Finanztransaktionssteuer (?<tax>[.,\\d]+)- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kapitalertragsteuer 25 % auf 7,55 EUR 1,89- EUR
                .section("tax", "currency").optional()
                .match("^Kapitalertragsteuer [.,\\d]+ % .* (?<tax>[.,\\d]+)- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag 5,5 % auf 1,89 EUR 0,11- EUR
                .section("tax", "currency").optional()
                .match("^Solidarit.tszuschlag [.,\\d]+ % .* (?<tax>[.,\\d]+)- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kirchensteuer 7 % auf 2,89 EUR 2,11- EUR
                .section("tax", "currency").optional()
                .match("^Kirchensteuer [.,\\d]+ % .* (?<tax>[\\d.]+,\\d+)- (?<currency>\\w{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Einbehaltene Quellensteuer 19 % auf 85,00 PLN 3,59-
                // EUR
                .section("quellensteinbeh", "currency").optional()
                .match("^Einbehaltene Quellensteuer .* ([\\w]{3}) (?<quellensteinbeh>[.,\\d]+)- (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    type.getCurrentContext().put(FLAG_WITHHOLDING_TAX_FOUND, "true");
                    addTax(type, t, v, "quellensteinbeh");
                })

                // Anrechenbare Quellensteuer 15 % auf 18,87 EUR 2,83
                // EUR
                .section("quellenstanr", "currency").optional()
                .match("^Anrechenbare Quellensteuer .* ([\\w]{3}) (?<quellenstanr>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> addTax(type, t, v, "quellenstanr"))

                // 4 % rückforderbare Quellensteuer 3,40 PLN
                .section("quellenstrueck", "currency").optional()
                .match("^.* r.ckforderbare Quellensteuer (?<quellenstrueck>[.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> addTax(type, t, v, "quellenstrueck"));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Provision 1,0000 % vom Kurswert 50,48- EUR
                .section("fee", "currency").optional()
                .match("^Provision [.,\\d]+ % .* (?<fee>[.,\\d]+)- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Transaktionsentgelt Börse 0,71- EUR
                .section("fee", "currency").optional()
                .match("^Transaktionsentgelt B.rse (?<fee>[.,\\d]+)- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Übertragungs-/Liefergebühr 0,07- EUR
                .section("fee", "currency").optional()
                .match("^.bertragungs-\\/Liefergeb.hr (?<fee>[.,\\d]+)- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    private void addTax(DocumentType type, Object t, Map<String, String> v, String taxtype)
    {
        // Wenn es 'Einbehaltene Quellensteuer' gibt, dann die weiteren
        // Quellensteuer-Arten nicht berücksichtigen.
        // Die Berechnung der Gesamt-Quellensteuer anhand der anrechenbaren- und
        // der rückforderbaren Steuer kann ansonsten zu Rundungsfehlern führen.
        if (checkWithholdingTax(type, taxtype))
        {
            name.abuchen.portfolio.model.Transaction tx = getTransaction(t);

            String currency = asCurrencyCode(v.get("currency"));
            long amount = asAmount(v.get(taxtype));

            if (!currency.equals(tx.getCurrencyCode()) && type.getCurrentContext().containsKey(EXCHANGE_RATE))
            {
                BigDecimal rate = BigDecimal.ONE.divide(asExchangeRate(type.getCurrentContext().get(EXCHANGE_RATE)), 10,
                                RoundingMode.HALF_DOWN);

                currency = tx.getCurrencyCode();
                amount = rate.multiply(BigDecimal.valueOf(amount)).setScale(0, RoundingMode.HALF_DOWN).longValue();
            }

            tx.addUnit(new Unit(Unit.Type.TAX, Money.of(currency, amount)));
        }
    }

    private boolean checkWithholdingTax(DocumentType documentType, String taxtype)
    {
        if (Boolean.valueOf(documentType.getCurrentContext().get(FLAG_WITHHOLDING_TAX_FOUND)))
        {
            if ("quellenstanr".equalsIgnoreCase(taxtype) || ("quellenstrueck".equalsIgnoreCase(taxtype)))
            { 
                return false; 
            }
        }
        return true;
    }

    private name.abuchen.portfolio.model.Transaction getTransaction(Object t)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            return ((name.abuchen.portfolio.model.Transaction) t);
        }
        else
        {
            return ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction();
        }
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
            PDFExtractorUtils.checkAndSetFee(fee, (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee,
                            ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }
}
