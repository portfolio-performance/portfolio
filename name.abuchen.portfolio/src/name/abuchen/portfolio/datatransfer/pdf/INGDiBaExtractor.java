package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Map;
import java.util.function.BiConsumer;
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

public class INGDiBaExtractor extends AbstractPDFExtractor
{
    private static final String IS_JOINT_ACCOUNT = "isjointaccount"; //$NON-NLS-1$
    
    BiConsumer<Map<String, String>, String[]> isJointAccount = (context, lines) -> {
        Pattern pJointAccount = Pattern.compile("KapSt anteilig 50,00 %.*"); //$NON-NLS-1$
        Boolean bJointAccount = false;
        for (String line : lines)
        {
            Matcher m = pJointAccount.matcher(line);
            if (m.matches())
            {
                context.put(IS_JOINT_ACCOUNT, Boolean.TRUE.toString());
                bJointAccount = true;
                break;
            }
        }

        if (!bJointAccount)
            context.put(IS_JOINT_ACCOUNT, Boolean.FALSE.toString());

    };

    public INGDiBaExtractor(Client client)
    {
        super(client);

        addBuyTransaction();
        addSellTransaction();
        addErtragsgutschrift();
        addZinsgutschrift();
        addDividendengutschrift();
    }
    
    @Override
    public String getPDFAuthor()
    {
        return "ING-DiBa"; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "ING-DiBa"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierabrechnung Kauf");
        this.addDocumentTyp(type);

        Block block = new Block("Wertpapierabrechnung Kauf.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("wkn", "isin", "name")
                        //
                        .match("^ISIN \\(WKN\\) (?<isin>[^ ]*) \\((?<wkn>.*)\\)$")
                        .match("Wertpapierbezeichnung (?<name>.*)").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("shares")
                        //
                        .match("^Nominale( St.ck)? (?<shares>[\\d.]+(,\\d+)?).*")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("time").optional() //
                        .match("(Ausf.hrungstag . -zeit|Ausf.hrungstag|Schlusstag . -zeit|Schlusstag) .* (?<time>\\d+:\\d+:\\d+).*") //
                        .assign((t, v) -> {
                            type.getCurrentContext().put("time", v.get("time"));
                        })
                        
                        .section("date") //
                        .match("(Ausf.hrungstag . -zeit|Ausf.hrungstag|Schlusstag . -zeit|Schlusstag) (?<date>\\d+.\\d+.\\d{4}+).*") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().get("time") != null)
                            {
                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                            }
                            else
                            {
                                t.setDate(asDate(v.get("date")));
                            }
                        })
                        
                        .section("amount", "currency") //
                        .match("Endbetrag zu Ihren Lasten (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fee", "currency").optional() //
                        .match("Handelsplatzgeb.hr (?<currency>\\w{3}+) (?<fee>[\\d.]+,\\d+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match("Provision (?<currency>\\w{3}+) (?<fee>[\\d.]+,\\d+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match("Handelsentgelt (?<currency>\\w{3}+) (?<fee>[\\d.]+,\\d+)") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))
                        .wrap(t -> {
                            if (t.getPortfolioTransaction().getDateTime() == null)
                                throw new IllegalArgumentException("Missing date");
                            return new BuySellEntryItem(t);
                        }));
    }

    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierabrechnung Verkauf", isJointAccount);
        this.addDocumentTyp(type);

        Block block = new Block("Wertpapierabrechnung Verkauf.*");
        type.addBlock(block);
        Transaction<BuySellEntry> transaction = new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("wkn", "isin", "name")
                        //
                        .match("^ISIN \\(WKN\\) (?<isin>[^ ]*) \\((?<wkn>.*)\\)$")
                        .match("Wertpapierbezeichnung (?<name>.*)")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares")
                        //
                        .match("^Nominale St.ck (?<shares>[\\d.]+(,\\d+)?)")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date") //
                        .match("(Ausf.hrungstag . -zeit|Ausf.hrungstag|Schlusstag . -zeit|Schlusstag) (?<date>\\d+.\\d+.\\d{4}+).*") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                        
                        .section("date","time").optional() //
                        .match("(Ausf.hrungstag . -zeit|Ausf.hrungstag|Schlusstag . -zeit|Schlusstag) (?<date>\\d+.\\d+.\\d{4}+) .* (?<time>\\d+:\\d+:\\d+).*") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        .section("amount", "currency") //
                        .match("Endbetrag zu Ihren Gunsten (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fee", "currency").optional() //
                        .match("Handelsplatzgeb.hr (?<currency>\\w{3}+) (?<fee>[\\d.]+,\\d+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match("Provision (?<currency>\\w{3}+) (?<fee>[\\d.]+,\\d+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match("Handelsentgelt (?<currency>\\w{3}+) (?<fee>[\\d.]+,\\d+)") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .wrap(BuySellEntryItem::new);
        
        addTaxSectionToBuySellEntry(type, transaction);
        block.set(transaction);
    }

    @SuppressWarnings("nls")
    private void addErtragsgutschrift()
    {
        DocumentType type = new DocumentType("Ertragsgutschrift", isJointAccount);
        this.addDocumentTyp(type);

        Block block = new Block("Ertragsgutschrift");
        type.addBlock(block);
        Transaction<AccountTransaction> transaction = new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.DIVIDENDS);
                            return entry;
                        })

                        .section("wkn", "isin", "name")
                        //
                        .match("^ISIN \\(WKN\\) (?<isin>[^ ]*) \\((?<wkn>.*)\\)$")
                        .match("Wertpapierbezeichnung (?<name>.*)")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares")
                        //
                        .match("^Nominale (?<shares>[\\d.]+(,\\d+)?) .*")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date") //
                        .match("Zahltag (?<date>\\d+.\\d+.\\d{4}+)") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .section("amount", "currency") //
                        .match("Gesamtbetrag zu Ihren Gunsten (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);
        
        addTaxSectionToAccountTransaction(type, transaction);
        block.set(transaction);
    }

    @SuppressWarnings("nls")
    private void addZinsgutschrift()
    {
        DocumentType type = new DocumentType("Zinsgutschrift", isJointAccount);
        this.addDocumentTyp(type);

        Block block = new Block("Zinsgutschrift");
        type.addBlock(block);
        Transaction<AccountTransaction> transaction = new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.DIVIDENDS);
                            return entry;
                        })

                        .section("wkn", "isin", "name")
                        //
                        .match("^ISIN \\(WKN\\) (?<isin>[^ ]*) \\((?<wkn>.*)\\)$")
                        .match("Wertpapierbezeichnung (?<name>.*)")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("date") //
                        .match("Zahltag (?<date>\\d+.\\d+.\\d{4}+)") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .section("amount", "currency") //
                        .match("Gesamtbetrag zu Ihren Gunsten (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })
                        
                        .wrap(TransactionItem::new);
        
        addTaxSectionToAccountTransaction(type, transaction);
        block.set(transaction);
    }

    @SuppressWarnings("nls")
    private void addDividendengutschrift()
    {
        final DocumentType type = new DocumentType("Dividendengutschrift", isJointAccount);
        this.addDocumentTyp(type);

        Block block = new Block("Dividendengutschrift.*");
        type.addBlock(block);
        Transaction<AccountTransaction> transaction = new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.DIVIDENDS);
                            return entry;
                        })

                        .section("wkn", "isin", "name") //
                        .match("^ISIN \\(WKN\\) (?<isin>[^ ]*) \\((?<wkn>.*)\\)$")
                        .match("Wertpapierbezeichnung (?<name>.*)")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .match("^Nominale (?<shares>[\\d.]+(,\\d+)?) .*")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .match("Gesamtbetrag zu Ihren Gunsten (?<currency>\\w{3}+) (?<amount>[\\d.]+,\\d+)") //
                        .assign((t, v) -> {

                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })
                        
                        .section("date") //
                        .match("Valuta (?<date>\\d+.\\d+.\\d{4}+)") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                                                
                        .wrap(TransactionItem::new);
        
        addTaxSectionToAccountTransaction(type, transaction);
        block.set(transaction);
    }
    
    @SuppressWarnings("nls")
    private void addTaxSectionToBuySellEntry(DocumentType type, Transaction<BuySellEntry> transaction)
    {
                    // Kapitalerstragsteuer (Einzelkonto)
         transaction.section("tax", "currency").optional() //
                    .match("Kapitalertragsteuer \\d+,\\d+ ?% (?<currency>\\w{3}+) (?<tax>[\\d.]+,\\d+)")
                    .assign((t, v) -> 
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                    Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))
                    
                    // Kapitalerstragsteuer (Gemeinschaftskonto)
                    .section("tax1", "currency1", "tax2", "currency2").optional() //
                    .match("KapSt anteilig 50,00 ?% \\d+,\\d+ ?% (?<currency1>\\w{3}+) (?<tax1>[\\d.]+,\\d+)")
                    .match("KapSt anteilig 50,00 ?% \\d+,\\d+ ?% (?<currency2>\\w{3}+) (?<tax2>[\\d.]+,\\d+)")
                    .assign((t, v) -> {
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                    Money.of(asCurrencyCode(v.get("currency1")), asAmount(v.get("tax1")))));
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                    Money.of(asCurrencyCode(v.get("currency2")), asAmount(v.get("tax2")))));
                        })
                    
                    // Solidaritätszuschlag (ein Eintrag bei Einzelkonto)
                    .section("tax", "currency").optional() //
                    .match("Solidarit.tszuschlag \\d+,\\d+ ?% (?<currency>\\w{3}+) (?<tax>[\\d.]+,\\d+)")
                    .assign((t, v) -> {
                        if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,                    
                                    Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                        })
            
                    // Solidaritätszuschlag (zwei Einträge bei Gemeinschaftskonto)
                    .section("tax1", "currency1", "tax2", "currency2").optional() //
                    .match("Solidarit.tszuschlag \\d+,\\d+ ?% (?<currency1>\\w{3}+) (?<tax1>[\\d.]+,\\d+)")
                    .match("Solidarit.tszuschlag \\d+,\\d+ ?% (?<currency2>\\w{3}+) (?<tax2>[\\d.]+,\\d+)")
                    .assign((t, v) -> {
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,                    
                                    Money.of(asCurrencyCode(v.get("currency1")), asAmount(v.get("tax1")))));
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,                    
                                    Money.of(asCurrencyCode(v.get("currency2")), asAmount(v.get("tax2")))));
                        })
                    
                    // Kirchensteuer (ein Eintrag bei Einzelkonto) 
                    .section("tax", "currency").optional() //
                    .match("Kirchensteuer \\d+,\\d+ ?% (?<currency>\\w{3}+) (?<tax>[\\d.]+,\\d+)")
                    .assign((t, v) -> {
                        if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                    Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));                 
                        })

                    // Kirchensteuer (zwei Einträge bei Gemeinschaftskonten)     
                    .section("tax1", "currency1", "tax2", "currency2").optional() //
                    .match("Kirchensteuer \\d+,\\d+ ?% (?<currency1>\\w{3}+) (?<tax1>[\\d.]+,\\d+)")
                    .match("Kirchensteuer \\d+,\\d+ ?% (?<currency2>\\w{3}+) (?<tax2>[\\d.]+,\\d+)")
                    .assign((t, v) -> {
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,                    
                                    Money.of(asCurrencyCode(v.get("currency1")), asAmount(v.get("tax1")))));
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,                    
                                    Money.of(asCurrencyCode(v.get("currency2")), asAmount(v.get("tax2")))));
                        });
                    

    }
    
    @SuppressWarnings("nls")
    private void addTaxSectionToAccountTransaction(DocumentType type, Transaction<AccountTransaction> transaction)
    {
                    // Kapitalerstragsteuer (Einzelkonto)
         transaction.section("tax", "currency").optional() //
                    .match("Kapitalertragsteuer \\d+,\\d+ ?% (?<currency>\\w{3}+) (?<tax>[\\d.]+,\\d+)")
                    .assign((t, v) -> 
                            t.addUnit(new Unit(Unit.Type.TAX,
                                    Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))
                    
                    // Kapitalerstragsteuer (Gemeinschaftskonto)
                    .section("tax1", "currency1", "tax2", "currency2").optional() //
                    .match("KapSt anteilig 50,00 ?% \\d+,\\d+ ?% (?<currency1>\\w{3}+) (?<tax1>[\\d.]+,\\d+)")
                    .match("KapSt anteilig 50,00 ?% \\d+,\\d+ ?% (?<currency2>\\w{3}+) (?<tax2>[\\d.]+,\\d+)")
                    .assign((t, v) -> {
                            t.addUnit(new Unit(Unit.Type.TAX,
                                    Money.of(asCurrencyCode(v.get("currency1")), asAmount(v.get("tax1")))));
                            t.addUnit(new Unit(Unit.Type.TAX,
                                    Money.of(asCurrencyCode(v.get("currency2")), asAmount(v.get("tax2")))));
                        })
                    
                    // Solidaritätszuschlag (ein Eintrag bei Einzelkonto)
                    .section("tax", "currency").optional() //
                    .match("Solidarit.tszuschlag \\d+,\\d+ ?% (?<currency>\\w{3}+) (?<tax>[\\d.]+,\\d+)")
                    .assign((t, v) -> {
                        if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                            t.addUnit(new Unit(Unit.Type.TAX,                    
                                    Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                        })
            
                    // Solidaritätszuschlag (zwei Einträge bei Gemeinschaftskonto)
                    .section("tax1", "currency1", "tax2", "currency2").optional() //
                    .match("Solidarit.tszuschlag \\d+,\\d+ ?% (?<currency1>\\w{3}+) (?<tax1>[\\d.]+,\\d+)")
                    .match("Solidarit.tszuschlag \\d+,\\d+ ?% (?<currency2>\\w{3}+) (?<tax2>[\\d.]+,\\d+)")
                    .assign((t, v) -> {
                            t.addUnit(new Unit(Unit.Type.TAX,                    
                                    Money.of(asCurrencyCode(v.get("currency1")), asAmount(v.get("tax1")))));
                            t.addUnit(new Unit(Unit.Type.TAX,                    
                                    Money.of(asCurrencyCode(v.get("currency2")), asAmount(v.get("tax2")))));
                        })
                    
                    // Kirchensteuer (ein Eintrag bei Einzelkonto) 
                    .section("tax", "currency").optional() //
                    .match("Kirchensteuer \\d+,\\d+ ?% (?<currency>\\w{3}+) (?<tax>[\\d.]+,\\d+)")
                    .assign((t, v) -> {
                        if (!Boolean.parseBoolean(type.getCurrentContext().get(IS_JOINT_ACCOUNT)))
                            t.addUnit(new Unit(Unit.Type.TAX,
                                    Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));                 
                        })
            
                    // Kirchensteuer (zwei Einträge bei Gemeinschaftskonten)     
                    .section("tax1", "currency1", "tax2", "currency2").optional() //
                    .match("Kirchensteuer \\d+,\\d+ ?% (?<currency1>\\w{3}+) (?<tax1>[\\d.]+,\\d+)")
                    .match("Kirchensteuer \\d+,\\d+ ?% (?<currency2>\\w{3}+) (?<tax2>[\\d.]+,\\d+)")
                    .assign((t, v) -> {
                        t.addUnit(new Unit(Unit.Type.TAX,                    
                                Money.of(asCurrencyCode(v.get("currency1")), asAmount(v.get("tax1")))));
                        t.addUnit(new Unit(Unit.Type.TAX,                    
                                Money.of(asCurrencyCode(v.get("currency2")), asAmount(v.get("tax2")))));
                        })

                    // Quellensteuer
                    .section("tax", "currency", "taxTx", "currencyTx") //
                    .optional() //
                    .match("QuSt \\d+,\\d+ % \\((?<currencyTx>\\w{3}+) (?<taxTx>[\\d.,]*)\\) (?<currency>\\w{3}+) (?<tax>[\\d.,]*)")
                    .assign((t, v) -> {
                        String currency = asCurrencyCode(v.get("currency"));
                        String currencyTx = asCurrencyCode(v.get("currencyTx"));

                        if (currency.equals(t.getCurrencyCode()))
                            t.addUnit(new Unit(Unit.Type.TAX, Money.of(currency, asAmount(v.get("tax")))));
                        else
                            t.addUnit(new Unit(Unit.Type.TAX, Money.of(currencyTx, asAmount(v.get("taxTx")))));
                    })
                    // Quellensteuer ohne Fremdwährung
                    .section("tax", "currency") //
                    .optional() //
                    .match("QuSt \\d+,\\d+ % (?<currency>\\w{3}+) (?<tax>[\\d.,]*)")
                    .assign((t, v) -> {
                        String currency = asCurrencyCode(v.get("currency"));
                        t.addUnit(new Unit(Unit.Type.TAX, Money.of(currency, asAmount(v.get("tax")))));
                    });

    }

}
