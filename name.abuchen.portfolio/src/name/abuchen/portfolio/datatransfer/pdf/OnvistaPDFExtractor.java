package name.abuchen.portfolio.datatransfer.pdf;

import java.io.IOException;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class OnvistaPDFExtractor extends AbstractPDFExtractor
{
    public OnvistaPDFExtractor(Client client) throws IOException
    {
        super(client);

        addBankIdentifier(""); //$NON-NLS-1$

        addBuyTransaction();
        addSellTransaction();
        addPayingTransaction();
        addDividendTransaction();
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Wir haben für Sie gekauft");
        this.addDocumentTyp(type);

        Block block = new Block("Wir haben für Sie gekauft");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("name", "isin")
                        .find("Gattungsbezeichnung ISIN")
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })
                        
                        .section("notation", "shares")
                        .find("Nominal Kurs")
                        .match("(?<notation>^EUR|^STK) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?)(.*)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("date", "amount", "currency")
                        .find("Wert Konto-Nr. Betrag zu Ihren Lasten")
                        //14.01.2015 172305047 EUR 59,55
                        .match("(?<date>\\d+.\\d+.\\d{4}+) (\\d{6,12}) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)$")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })
                        
                        .section("brokerage").optional()
                        .match("(^.*)(Orderprovision) (\\w{3}+) (?<brokerage>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("brokerage")) ))))
                        .section("stockfees").optional()
                        .match("(^.*) (B\\Drsengeb\\Dhr) (\\w{3}+) (?<stockfees>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("stockfees")) ))))
                        .section("agent").optional()                
                        .match("(^.*)(Maklercourtage)(\\s+)(\\w{3}+) (?<agent>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("agent")) ))))                
                                        
                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Wir haben für Sie verkauft");
        this.addDocumentTyp(type);

        Block block = new Block("Wir haben für Sie verkauft (.*)");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("name", "isin")
                        .find("Gattungsbezeichnung ISIN")
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })
                        
                        .section("notation", "shares")
                        .find("Nominal Kurs")
                        .match("(?<notation>^EUR|^STK) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?)(.*)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("date", "amount", "currency")
                        .find("Wert(\\s+)Konto-Nr. Betrag zu Ihren Gunsten(\\s+)$")
                        //12.04.2011 172305047 EUR 21,45
                        .match("(?<date>\\d+.\\d+.\\d{4}+) (\\d{6,12}) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })
                        
                        //Handelszeit 12:30 Maklercourtage              EUR 0,75-
                        .section("brokerage").optional()
                        .match("(^.*)(Orderprovision) (\\w{3}+) (?<brokerage>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("brokerage")) ))))
                        .section("stockfees").optional()
                        .match("(^.*) (B\\Drsengeb\\Dhr) (\\w{3}+) (?<stockfees>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("stockfees")) ))))
                        .section("agent").optional()                
                        .match("(^.*)(Maklercourtage)(\\s+)(\\w{3}+) (?<agent>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("agent")) ))))
                                        
                        .section("tax").optional()
                        //Kapitalertragsteuer EUR 4,22-
                        .match("^Kapitalertragsteuer (?<currency>\\w{3}+) (?<tax>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                        }) 
                        .section("soli").optional()
                        .match("^Solidaritätszuschlag (?<currency>\\w{3}+) (?<soli>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("soli")))));
                        })
                        .section("kirchenst").optional()
                        .match("^Kirchensteuer (?<currency>\\w{3}+) (?<kirchenst>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("kirchenst")))));
                        })

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addPayingTransaction()
    {
        DocumentType type = new DocumentType("Gutschriftsanzeige");
        this.addDocumentTyp(type);

        Block block = new Block("Gutschriftsanzeige(.*)");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("name", "isin")
                        .find("Gattungsbezeichnung (.*) ISIN")
                        .match("(?<name>.*) (.*) (?<isin>[^ ]\\S*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })
                        
                        .section("notation", "shares")
                        .find("Nominal Einlösung(.*)$")
                        .match("(?<notation>^EUR|^STK) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?)(.*)$")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })
                        
                        .section("date", "amount", "currency")
                        .find("Wert(\\s+)Konto-Nr. Betrag zu Ihren Gunsten$")
                        //17.11.2014 172305047 EUR 51,85
                        .match("(?<date>\\d+.\\d+.\\d{4}+) (\\d{6,12}) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })
                        
                        .section("tax").optional()
                        //Kapitalertragsteuer EUR 4,22-
                        .match("^Kapitalertragsteuer (?<currency>\\w{3}+) (?<tax>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                        })
                        .section("soli").optional()
                        .match("^Solidaritätszuschlag (?<currency>\\w{3}+) (?<soli>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("soli")))));
                        })
                        .section("kirchenst").optional()
                        .match("^Kirchensteuer (?<currency>\\w{3}+) (?<kirchenst>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("kirchenst")))));
                        })

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Erträgnisgutschrift");
        this.addDocumentTyp(type);

        //Dividendengutschrift|Kupongutschrift|Erträgnisgutschrift
        Block block = new Block("Erträgnisgutschrift(.*)");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        .section("name", "isin")
                        .find("Gattungsbezeichnung(.*) ISIN")
                        //Commerzbank AG Inhaber-Aktien o.N. DE000CBK1001
                        //5,5% TUI AG Wandelanl.v.2009(2014) 17.11.2014 17.11.2010 DE000TUAG117
                        .match("(?<name>.*?) (\\d+.\\d+.\\d{4} ){0,2}(?<isin>[^ ]\\S*)$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })
                        
                        .section("notation", "shares")
                        .find("Nominal (Ex-Tag )?Zahltag (.*etrag pro .*)?(Zinssatz.*)?")
                        //STK 50,000 21.04.2016 21.04.2016 EUR 0,200000
                        .match("(?<notation>^EUR|^STK) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?) (.*)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("date", "amount", "currency")
                        .find("Wert(\\s+)Konto-Nr. (Devisenkurs )?Betrag zu Ihren Gunsten(\\s*)$")
                        //21.04.2016 172305047 EUR 10,00
                        //18.12.2012 172305047 EUR/SGD 1,61567 EUR 1,56
                        .match("(?<date>\\d+.\\d+.\\d{4}+) (\\d{6,12}) (.{7,20} )?(?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })
                        .section("tax").optional()
                        .match("^Kapitalertragsteuer (?<currency>\\w{3}+) (?<tax>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.addUnit(new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                        })
                        .section("soli").optional()
                        .match("^Solidaritätszuschlag (?<currency>\\w{3}+) (?<soli>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.addUnit(new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("soli")))));
                        })
                        .section("kirchenst").optional()
                        .match("^Kirchensteuer (?<currency>\\w{3}+) (?<kirchenst>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.addUnit(new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("kirchenst")))));
                        })
                        
                        .wrap(t -> new TransactionItem(t)));
    }

    @Override
    public String getLabel()
    {
        return "Onvista"; //$NON-NLS-1$
    }
}
