package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class DZBankPDFExtractor extends AbstractPDFExtractor
{
    private static final String EXCHANGE_RATE = "exchangeRate"; //$NON-NLS-1$
    private static final String FLAG_WITHHOLDING_TAX_FOUND  = "exchangeRate"; //$NON-NLS-1$
    
    public DZBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Volksbank"); //$NON-NLS-1$
        addBankIdentifier("VRB Oberbayern"); //$NON-NLS-1$
        addBankIdentifier("NIBC Direct Depotservice"); //$NON-NLS-1$
        addBankIdentifier("Postfach 12 40 · 97755 Hammelburg"); //$NON-NLS-1$
        addBankIdentifier("Union Investment Service Bank AG"); //$NON-NLS-1$
        addBankIdentifier("VR-Bank"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
        addSalesTurnoverTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return "DZBank"; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "DZBank"; //$NON-NLS-1$
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

                // Schlusstag/-Zeit 09.02.2021 19:48:50 Auftraggeber Max Muster
                .section("date", "time")
                .match("(Schlusstag\\/-Zeit) (?<date>\\d+.\\d+.\\d{4}+) (?<time>\\d+:\\d+:\\d+).*")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })

                // Nominale Wertpapierbezeichnung ISIN (WKN)
                // Stück 1 NETFLIX INC.                       US64110L1061 (552484)
                // REGISTERED SHARES DL -,001  
                .section("shares", "name", "isin", "wkn", "name1")
                .match("(Nominale Wertpapierbezeichnung ISIN \\(WKN\\))")
                .match("(St.ck) (?<shares>[\\d.]+(,\\d+)?)( Aktienname)? (?<name>.*) (?<isin>[\\w]{12}) \\((?<wkn>.*)\\).*")
                .match("(?<name1>.*)")
                .assign((t, v) -> {
                    v.put("name", v.get("name").trim());
                    if (!v.get("name1").startsWith("Handels-\\/Ausf.hrungsplatz"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Ausmachender Betrag 442,29 EUR
                .section("amount", "currency")
                .match("(Ausmachender Betrag)[ ]*(?<amount>[\\d.]+,[\\d-]+) (?<currency>[\\w]{3}).*")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addTaxRefundForSell(type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Dividendengutschrift|Aussch.ttung Investmentfonds)");
        this.addDocumentTyp(type);

        Block block = new Block("(Dividendengutschrift|Ausschüttung Investmentfonds)");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // Nominale Wertpapierbezeichnung ISIN (WKN)
                // Stück 100 VANGUARD FTSE ALL-WORLD U.ETF IE00B3RBWM25 (A1JX52)
                // REGISTERED SHARES USD DIS.ON
                .section("shares", "name", "isin", "wkn", "name1")
                .match("^Nominale Wertpapierbezeichnung ISIN \\(WKN\\)")
                .match("^St.ck (?<shares>[\\d.]+(,\\d+)?) (?<name>.*)\\s+(?<isin>[\\w]{12}) \\((?<wkn>.*)\\).*")
                .match("(?<name1>.*)")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                        v.put("name", v.get("name") + " " + v.get("name1"));
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Ex-Tag 12.12.2019 fonds) 0,208116300 USD
                .section("date")
                .match("^Ex-Tag (?<date>\\d+.\\d+.\\d{4}+).*")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                })
                
                // Ausmachender Betrag 9,92+ EUR
                .section("currency", "amount")
                .find("^Ausmachender Betrag (?<amount>[\\d.]+(,[\\d]+)?)\\+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Devisenkurs EUR / USD 1,2095
                // Devisenkursdatum 02.01.2018
                // Ausschüttung 113,16 USD 93,56+ EUR
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match("^Devisenkurs .* (?<exchangeRate>[\\d.]+,\\d+)$")
                .match("^Aussch.ttung (?<fxAmount>[\\d.]+,\\d+) (?<fxCurrency>[\\w]{3}) (?<amount>[\\d.]+,\\d+)\\+ (?<currency>[\\w]{3})$")
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
                            Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),asAmount(v.get("fxAmount")));
                            Money amount = Money.of(asCurrencyCode(v.get("currency")),asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        else
                        {
                            Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxAmount")));
                            Money fxAmount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
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

    private void addSalesTurnoverTransaction()
    {
        DocumentType type = new DocumentType("Umsatzübersicht", (context, lines) -> {
            Pattern pSecurityName = Pattern.compile("^(Produkt:) (?<name>.*) (Verf.*: .*)$");
            Pattern pSecurityNameContinued = Pattern.compile("^(([\\d+].) (?<nameContinued>.*))$");
            Pattern pSecurityISIN = Pattern.compile("^((?<isin>[\\w]{12}), .*)$");
            for (String line : lines)
            {
                Matcher m = pSecurityName.matcher(line);
                if (m.matches())
                {
                    context.put("name", m.group("name"));
                }
                m = pSecurityISIN.matcher(line);
                if (m.matches())
                {
                    context.put("isin", m.group("isin"));
                }
                m = pSecurityNameContinued.matcher(line);
                if (m.matches())
                {
                    context.put("nameContinued", m.group("nameContinued"));
                }
            }
        });
        this.addDocumentTyp(type);

        Block buyBlock = new Block("(\\d+.\\d+.\\d{4}) Kauf ([\\d.-]+,\\d+) ([\\w]{3})");
        type.addBlock(buyBlock);
        buyBlock.set(new Transaction<BuySellEntry>()
            .subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        })
            // 06.10.2017 Kauf 33,00 EUR
            // 1. UniGlobal Vorsorge
            // DE000A1C81G1, Kapitalverwaltungsgesellschaft: Union Investment Privatfonds GmbH, IBAN: DE1234567891, Bank: Volksbank in der
            // Ortenau eG
            // Anlagebetrag Preisdatum Ausgabepreis Rücknahme- Abrechnungs- Ausgabeauf- Gebühren Kurswert Anteile
            // in EUR in EUR preis in EUR preis in EUR schlag in EUR in EUR in EUR
            // 33,00 05.10.2017 207,24 197,37 207,24 -1,57 0,00 33,00 0,159
            .section("currency", "amount", "date", "fee", "fee2", "shares")
            .match("^(\\d+.\\d+.\\d{4}+) (Kauf) (?<amount>[\\d.-]+,\\d+) (?<currency>[\\w]{3})$")
            .match("^((?<isin>[\\w]{12}), .*)$")
            .match("^([\\d.-]+,\\d+) (?<date>\\d+.\\d+.\\d{4}) ([\\d.-]+,\\d+) ([\\d.-]+,\\d+) ([\\d.-]+,\\d+) "
                            + "[-]?(?<fee>[\\d.-]+,\\d+) "
                            + "[-]?(?<fee2>[\\d.-]+,\\d+) [-]?([\\d.-]+,\\d+) "
                            + "[-]?(?<shares>[\\d.,]+)$")
            .assign((t, v) -> {
                Map<String, String> context = type.getCurrentContext();
                t.setSecurity(getOrCreateSecurity(context));
                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                t.setAmount(asAmount(v.get("amount")));
                t.setDate(asDate(v.get("date")));
                t.setShares(asShares(v.get("shares")));
                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                Money.of(asCurrencyCode(v.get("currency")),
                                                asAmount(v.get("fee")))));
                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                Money.of(asCurrencyCode(v.get("currency")),
                                                asAmount(v.get("fee2")))));
            })
            .wrap(BuySellEntryItem::new));
    }

    private void addTaxRefundForSell(DocumentType type)
    {
        Block block = new Block("Wertpapier Abrechnung (Kauf|Verkauf).*");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.TAX_REFUND);
                return entry;
            })

            // Nominale Wertpapierbezeichnung ISIN (WKN)
            // Stück 5 ETSY INC.                          US29786A1060 (A14P98)
            // REGISTERED SHARES DL -,001  
            .section("shares", "name", "isin", "wkn", "name1").optional()
            .match("(Nominale Wertpapierbezeichnung ISIN \\(WKN\\))")
            .match("(St.ck) (?<shares>[\\d.]+(,\\d+)?) (?<name>.*)\\s+(?<isin>[\\w]{12}) \\((?<wkn>.*)\\).*")
            .match("(?<name1>.*)")
            .assign((t, v) -> {
                if (!v.get("name1").startsWith("Zahlbarkeitstag"))
                    v.put("name", v.get("name") + " " + v.get("name1"));
                t.setSecurity(getOrCreateSecurity(v));
                t.setShares(asShares(v.get("shares")));
            })

            // Datum 27.01.2021
            // Kapitalertragsteuer 25,00% auf 99,50 EUR 24,87 EUR
            // Solidaritätszuschlag 5,50% auf 24,87 EUR 1,36 EUR
            // Ausmachender Betrag 26,23 EUR
            .section("date", "taxRefund", "currency").optional()
            .match("(Datum) (?<date>\\d+.\\d+.\\d{4}+)$")
            .match("(Kapitalertragsteuer) [\\d+,\\%]* auf [\\d+,]+ [\\w]{3} [.,\\d]+ [\\w]{3}")
            .match("(Solidarit.tszuschlag) [\\d+,\\%]* auf [\\d+,]+ [\\w]{3} [.,\\d]+ [\\w]{3}")
            .match("(Ausmachender Betrag)[ ]*(?<taxRefund>[\\d.]+,[\\d-]+) (?<currency>[\\w]{3}).*")
            .assign((t, v) -> {
                t.setDateTime(asDate(v.get("date")));
                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                t.setAmount(asAmount(v.get("taxRefund")));
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
                // Kapitalertragsteuer 25 % auf 93,63 EUR 23,41- EUR
                .section("tax", "currency").optional()
                .match("^(Kapitalertragsteuer) [.,\\d]+ % .* (?<tax>[\\d.]+,\\d+)- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kapitalertragsteuer 25,00% auf 37,93 EUR 9,49- EUR
                .section("tax", "currency").optional()
                .match("(Kapitalertragsteuer) [\\d+,\\%]* auf [\\d,]* [\\w]{3} (?<tax>[\\d+,.]*)- (?<currency>[\\w]{3})")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag 5,5 % auf 23,41 EUR 1,28- EUR
                .section("tax", "currency").optional()
                .match("^(Solidaritätszuschlag) [.,\\d]+ % .* (?<tax>[\\d.]+,\\d+)- (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Solidaritätszuschlag 5,50% auf 9,49 EUR 0,52- EUR
                .section("tax", "currency").optional()
                .match("(Solidarit.tszuschlag) [\\d+,\\%]* auf [\\d,]* [\\w]{3} (?<tax>[\\d+,.]*)- (?<currency>[\\w]{3})")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Kirchensteuer 5,5 % auf 23,41 EUR 1,28- EUR
                .section("tax", "currency").optional()
                .match("^(Kirchensteuer) [.,\\d]+ % .* (?<tax>[\\d.]+,\\d+)- (?<currency>\\w{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Einbehaltene Quellensteuer 25 % auf 18,45 USD 3,85- EUR
                .section("quellensteinbeh", "currency").optional()
                .match("^Einbehaltene Quellensteuer(.*) (\\w{3}) (?<quellensteinbeh>[\\d.]+,\\d+)(-) (?<currency>\\w{3})")
                .assign((t, v) ->  {
                    type.getCurrentContext().put(FLAG_WITHHOLDING_TAX_FOUND, "true");
                    addTax(type, t, v, "quellensteinbeh");
                })

                // Anrechenbare Quellensteuer 15 % auf 15,39 EUR 2,31 EUR
                .section("quellenstanr", "currency").optional()
                .match("^Anrechenbare Quellensteuer(.*) (\\w{3}) (?<quellenstanr>[\\d.]+,\\d+) (?<currency>[\\w]{3})")
                .assign((t, v) -> addTax(type, t, v, "quellenstanr"))

                // 10 % rückforderbare Quellensteuer 1,85 USD
                .section("quellenstrueck", "currency").optional()
                .match("^(.*) r.ckforderbare Quellensteuer (?<quellenstrueck>[\\d.]+,\\d+) (?<currency>[\\w]{3})")
                .assign((t, v) -> addTax(type, t, v, "quellenstrueck"));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Provision 9,95- EUR
                .section("fee", "currency").optional()
                .match("(Provision)[ ]*(?<fee>[\\d+,.]*)- (?<currency>[\\w]{3}).*")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Transaktionsentgelt Börse 2,49- EUR
                .section("fee", "currency").optional()
                .match("(Transaktionsentgelt B.rse)[ ]*(?<fee>[\\d+,.]*)- (?<currency>[\\w]{3}).*")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Übertragungs-/Liefergebühr 0,10- EUR
                .section("fee", "currency").optional()
                .match("(.bertragungs-\\/Liefergeb.hr)[ ]*(?<fee>[\\d+,.]*)- (?<currency>[\\w]{3}).*")
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
                BigDecimal rate = BigDecimal.ONE.divide(
                                asExchangeRate(type.getCurrentContext().get(EXCHANGE_RATE)), 10,
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