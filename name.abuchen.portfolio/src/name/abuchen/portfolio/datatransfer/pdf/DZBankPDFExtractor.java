package name.abuchen.portfolio.datatransfer.pdf;

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

public class DZBankPDFExtractor extends AbstractPDFExtractor
{
    public DZBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Volksbank"); //$NON-NLS-1$
        addBankIdentifier("VRB Oberbayern"); //$NON-NLS-1$
        addBankIdentifier("NIBC Direct Depotservice"); //$NON-NLS-1$
        addBankIdentifier("Postfach 12 40 · 97755 Hammelburg"); //$NON-NLS-1$
        addBankIdentifier("Union Investment Service Bank AG"); //$NON-NLS-1$

        addBuySellTransaction();
        addAusschuettungTransaction();
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
    
    @SuppressWarnings("nls")
    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapier Abrechnung (Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Block block = new Block("Wertpapier Abrechnung (Kauf|Verkauf).*");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>()

            .subject(() -> {
                BuySellEntry entry = new BuySellEntry();
                entry.setType(PortfolioTransaction.Type.BUY);
                return entry;
            })

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
            .section("shares", "name", "isin", "wkn", "nameContinued")
            .match("(Nominale Wertpapierbezeichnung ISIN \\(WKN\\))")
            .match("(St.ck) (?<shares>[\\d.]+(,\\d+)?) (?<name>.*)\\s+(?<isin>.*) \\((?<wkn>.*)\\).*")
            .match("(?<nameContinued>.*)")
            .assign((t, v) -> {
                v.put("isin", v.get("isin"));
                v.put("wkn", v.get("wkn"));
                v.put("name", v.get("name"));                            
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
            
            // Provision 9,95- EUR
            .section("fee", "currency").optional()
            .match("(Provision)[ ]*(?<fee>[\\d+,.]*)- (?<currency>[\\w]{3}).*")
            .assign((t, v) -> t.getPortfolioTransaction()
                            .addUnit(new Unit(Unit.Type.FEE,
                                            Money.of(asCurrencyCode(v.get("currency")), 
                                                            asAmount(v.get("fee"))))))

            // Transaktionsentgelt Börse 2,49- EUR
            .section("fee", "currency").optional()
            .match("(Transaktionsentgelt B.rse)[ ]*(?<fee>[\\d+,.]*)- (?<currency>[\\w]{3}).*")
            .assign((t, v) -> t.getPortfolioTransaction()
                            .addUnit(new Unit(Unit.Type.FEE,
                                            Money.of(asCurrencyCode(v.get("currency")),
                                                            asAmount(v.get("fee"))))))

            // Übertragungs-/Liefergebühr 0,10- EUR
            .section("fee", "currency").optional()
            .match("(.bertragungs-\\/Liefergeb.hr)[ ]*(?<fee>[\\d+,.]*)- (?<currency>[\\w]{3}).*")
            .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))
            
            // Kapitalertragsteuer 25,00% auf 37,93 EUR 9,49- EUR
            .section("tax", "currency").optional()
            .match("(Kapitalertragsteuer) [\\d+,\\%]* auf [\\d,]* [\\w]{3} (?<tax>[\\d+,.]*)- (?<currency>[\\w]{3})")
            .assign((t, v) -> t.getPortfolioTransaction()
                            .addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")),
                                                            asAmount(v.get("tax"))))))

            // Solidaritätszuschlag 5,50% auf 9,49 EUR 0,52- EUR
            .section("tax", "currency").optional()
            .match("(Solidarit.tszuschlag) [\\d+,\\%]* auf [\\d,]* [\\w]{3} (?<tax>[\\d+,.]*)- (?<currency>[\\w]{3})")
            .assign((t, v) -> t.getPortfolioTransaction()
                            .addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")),
                                                            asAmount(v.get("tax"))))))

            .wrap(BuySellEntryItem::new);
        
        addTaxRefundForSell(type);
        
        block.set(pdfTransaction);
    }
    
    @SuppressWarnings("nls")
    private void addAusschuettungTransaction()
    {
        DocumentType dividende = new DocumentType("Ausschüttung Investmentfonds");
        this.addDocumentTyp(dividende);

        Block block = new Block(
                        "Aussch.ttung Investmentfonds.*");
        dividende.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DIVIDENDS);
                            return t;
                        })

                        .section("date")
                        .match(".*(Wertstellung) (?<date>\\d+.\\d+.\\d{4}+).*")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                        })
                        
                        .section("shares", "name", "isin", "wkn")
                        .match("(Nominale Wertpapierbezeichnung ISIN \\(WKN\\))")
                        .match("(St.ck) (?<shares>[\\d.]+(,\\d+)?) (?<name>.*)\\s+(?<isin>.*) \\((?<wkn>.*)\\).*")
                        .assign((t, v) -> {
                            v.put("isin", v.get("isin"));
                            v.put("wkn", v.get("wkn"));
                            v.put("name", v.get("name"));                            
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })
                        
                        .section("currency", "amount") //
                        .find("^(Aussch.ttung) (?<amountfx>[\\d.]+(,[\\d]+)?) (USD) (?<amount>[\\d.]+(,[\\d]+)?)\\+ (?<currency>.*)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })
                        
                        .section("currency", "amountaftertax") //
                        .find(".*(Ausmachender Betrag) (?<amountaftertax>[\\d.]+(,[\\d]+)?)\\+ (?<currency>.*)$")
                        .assign((t, v) -> {
                            long gross = t.getAmount();
                            long tax = gross - asAmount(v.get("amountaftertax"));
                            
                            if (tax > 0)
                                t.addUnit(new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")), tax)));
                        })

                        .wrap(TransactionItem::new));
    }
    
    @SuppressWarnings("nls")
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

        Block buyBlock = new Block("(\\d+.\\d+.\\d{4}) Kauf ([\\d.-]+,\\d+) (\\w{3})");
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

    @SuppressWarnings("nls")
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
            .section("shares", "name", "isin", "wkn", "nameContinued").optional()
            .match("(Nominale Wertpapierbezeichnung ISIN \\(WKN\\))")
            .match("(St.ck) (?<shares>[\\d.]+(,\\d+)?) (?<name>.*)\\s+(?<isin>.*) \\((?<wkn>.*)\\).*")
            .match("(?<nameContinued>.*)")
            .assign((t, v) -> {
                v.put("isin", v.get("isin"));
                v.put("wkn", v.get("wkn"));
                v.put("name", v.get("name"));                            
                t.setSecurity(getOrCreateSecurity(v));
                t.setShares(asShares(v.get("shares")));
            })

            // Datum 27.01.2021
            // Kapitalertragsteuer 25,00% auf 99,50 EUR 24,87 EUR
            // Solidaritätszuschlag 5,50% auf 24,87 EUR 1,36 EUR
            // Ausmachender Betrag 26,23 EUR
            .section("date", "taxRefund", "currency").optional()
            .match("(Datum) (?<date>\\d+.\\d+.\\d{4}+)$")
            .match("(Kapitalertragsteuer) [\\d+,\\%]* auf [\\d+,]+ [\\w]{3} (?<tax>[\\d+,.]*) (?<currency>[\\w]{3})")
            .match("(Solidarit.tszuschlag) [\\d+,\\%]* auf [\\d+,]+ [\\w]{3} (?<tax>[\\d+,.]*) (?<currency>[\\w]{3})")
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
}