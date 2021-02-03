package name.abuchen.portfolio.datatransfer.pdf;

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

public class TradeRepublicPDFExtractor extends AbstractPDFExtractor
{
    public TradeRepublicPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("TRADE REPUBLIC"); //$NON-NLS-1$

        addBuyTransaction();
        addSellTransaction();
        addLiquidationTransaction();
        addAccountStatementTransaction();
        addTaxStatementTransaction();
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Order Kauf");
        this.addDocumentTyp(type);

        Block block = new Block(".*Order Kauf.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("name", "isin", "shares") //
                        .find("POSITION ANZAHL KURS BETRAG") //
                        .match("(?<name>.*) (?<shares>[\\d+,.]*) Stk. ([\\d+,.]*) (\\w{3}+) ([\\d+,.]*) (\\w{3}+)$") //
                        .match(".*") //
                        .match("(ISIN:)?(?<isin>.*)").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("amount", "currency") //
                        .match("GESAMT -(?<amount>[\\d+,.]*) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("date", "time") //
                        .match(".*Order Kauf am (?<date>\\d+\\.\\d+\\.\\d{4}|\\d{4}-\\d+-\\d+), um (?<time>\\d+:\\d+) Uhr.*")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        .section("fee", "currency") //
                        .optional() //
                        .match("Fremdkostenzuschlag -(?<fee>[\\d+,.]*) (?<currency>\\w{3}+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Order Verkauf");
        this.addDocumentTyp(type);

        Block block = new Block(".*Order Verkauf.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("name", "isin", "shares") //
                        .find("POSITION ANZAHL KURS BETRAG") //
                        .match("(?<name>.*) (?<shares>[\\d+,.]*) Stk. ([\\d+,.]*) (\\w{3}+) ([\\d+,.]*) (\\w{3}+)$") //
                        .match(".*") //
                        .match("(ISIN:)?(?<isin>.*)").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        // there might be two lines with "GESAMT" - one for gross and
                        // one for the net value - pick the second

                        .section("amount", "currency").optional() //
                        .match("GESAMT ([\\d+,.]*) (\\w{3}+)") //
                        .match("GESAMT (?<amount>[\\d+,.]*) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // in case there is no tax, only one line with "GESAMT"
                        // exists and we need to grab data from that line
                        .section("amount", "currency") //
                        .match("GESAMT (?<amount>[\\d+,.]*) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            // if amount is already set, we do nothing
                            if (t.getPortfolioTransaction().getAmount() == 0L)
                            {
                                t.setAmount(asAmount(v.get("amount")));
                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            }
                        })

                        .section("date", "time") //
                        .match(".*Order Verkauf am (?<date>\\d+\\.\\d+\\.\\d{4}|\\d{4}-\\d+-\\d+), um (?<time>\\d+:\\d+) Uhr.*")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        .section("fee", "currency") //
                        .optional() //
                        .match("Fremdkostenzuschlag -(?<fee>[\\d+,.]*) (?<currency>\\w{3}+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("tax", "currency") //
                        .optional() //
                        .match("Kapitalertragssteuer -(?<tax>[\\d+,.]*) (?<currency>\\w{3}+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.TAX,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("tax"))))))
                        // check for negative tax (optimization)
                        .section("tax", "currency").optional() //
                        .match("Kapitalertragssteuer Optimierung (?<tax>[\\d+,.]*) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("tax")));
                        })

                        .section("tax", "currency") //
                        .optional() //
                        .match("Solidaritätszuschlag -(?<tax>[\\d+,.]*) (?<currency>\\w{3}+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.TAX,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("tax"))))))

                        // check for negative tax (optimization)
                        .section("tax", "currency").optional() //
                        .match("Solidaritätszuschlag Optimierung (?<tax>[\\d+,.]*) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("tax")));
                        })

                        .section("tax", "currency").optional() //
                        .match("Kirchensteuer -(?<tax>[\\d+,.]*) (?<currency>\\w{3}+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.TAX,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("tax"))))))

                        // check for negative tax (optimization)
                        .section("tax", "currency").optional() //
                        .match("Kirchensteuer Optimierung (?<tax>[\\d+,.]*) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("tax")));
                        })

                        .wrap(BuySellEntryItem::new));

        Block taxBlock = new Block("Kapitalertragssteuer Optimierung.*");
        type.addBlock(taxBlock);
        taxBlock.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.TAX_REFUND);
            return t;
        })

                        // check for negative tax (optimization)
                        .section("tax", "currency", "date").optional() //
                        .match("Kapitalertragssteuer Optimierung (?<tax>[\\d+,.]*) (?<currency>\\w{3}+)")
                        .match("VERRECHNUNGSKONTO VALUTA BETRAG")
                        .match(".* (?<date>\\d+\\.\\d+\\.\\d{4}|\\d{4}-\\d+-\\d+) (?<amount>[\\d+,.]*) (\\w{3}+)")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("tax")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                        })

                        .section("tax", "currency", "date").optional() //
                        .match("Solidaritätszuschlag Optimierung (?<tax>[\\d+,.]*) (?<currency>\\w{3}+)")
                        .match("VERRECHNUNGSKONTO VALUTA BETRAG")
                        .match(".* (?<date>\\d+\\.\\d+\\.\\d{4}|\\d{4}-\\d+-\\d+) (?<amount>[\\d+,.]*) (\\w{3}+)")
                        .assign((t, v) -> {
                            t.setAmount(t.getAmount() + asAmount(v.get("tax")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                        })

                        .section("tax", "currency", "date").optional() //
                        .match("Kirchensteuer Optimierung (?<tax>[\\d+,.]*) (?<currency>\\w{3}+)")
                        .match("VERRECHNUNGSKONTO VALUTA BETRAG")
                        .match(".* (?<date>\\d+\\.\\d+\\.\\d{4}|\\d{4}-\\d+-\\d+) (?<amount>[\\d+,.]*) (\\w{3}+)")
                        .assign((t, v) -> {
                            t.setAmount(t.getAmount() + asAmount(v.get("tax")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                        }).wrap(t -> new TransactionItem(t)));

    }

    @SuppressWarnings("nls")
    private void addLiquidationTransaction()
    {
        DocumentType type = new DocumentType("TILGUNG");
        this.addDocumentTyp(type);
    
        Block block = new Block("TILGUNG");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()
    
                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })
    
                        .section("name", "isin", "shares") //
                        .find("NR. BUCHUNG WERTPAPIER (ANZAHL|BETRAG).*") //
                        .match("\\d+ Tilgung (?<name>.*) (?<shares>[\\d+,.]*) Stk.$") //
                        .match(".*") //
                        .match("(ISIN:)?(?<isin>\\w{12})")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })
                        
                        .section("amount", "currency")//
                        .match("NR. POSITION (SUMME|BETRAG)") //
                        .match("\\d+ Kurswert .*") //
                        .match("(SUMME|GESAMT) (?<amount>[\\d+,.]*) (?<currency>\\w{3})") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })
    
                        .section("date") //
                        .match("VERRECHNUNGSKONTO VALUTA BETRAG")
                        .match("\\w* (?<date>\\d+\\.\\d+\\.\\d{4}|\\d{4}-\\d+-\\d+) .*")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
    
                        .wrap(BuySellEntryItem::new));
    
        Block taxBlock = new Block(".*Kapitalertragssteuer Optimierung.*");
        type.addBlock(taxBlock);
        taxBlock.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.TAX_REFUND);
            return t;
        })
    
                        // check for negative tax (optimization)
                        .section("tax", "currency", "date").optional() //
                        .match(".*Kapitalertragssteuer Optimierung (?<tax>[\\d+,.]*) (?<currency>\\w{3}+)")
                        .match("VERRECHNUNGSKONTO VALUTA BETRAG")
                        .match(".* (?<date>\\d+\\.\\d+\\.\\d{4}|\\d{4}-\\d+-\\d+) ([\\d+,.]*) (\\w{3}+)")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("tax")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                        })
    
                        .section("tax", "currency", "date").optional() //
                        .match("Solidaritätszuschlag Optimierung (?<tax>[\\d+,.]*) (?<currency>\\w{3}+)")
                        .match("VERRECHNUNGSKONTO VALUTA BETRAG")
                        .match(".* (?<date>\\d+\\.\\d+\\.\\d{4}|\\d{4}-\\d+-\\d+) ([\\d+,.]*) (\\w{3}+)")
                        .assign((t, v) -> {
                            t.setAmount(t.getAmount() + asAmount(v.get("tax")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                        })
    
                        .section("tax", "currency", "date").optional() //
                        .match("Kirchensteuer Optimierung (?<tax>[\\d+,.]*) (?<currency>\\w{3}+)")
                        .match("VERRECHNUNGSKONTO VALUTA BETRAG")
                        .match(".* (?<date>\\d+\\.\\d+\\.\\d{4}|\\d{4}-\\d+-\\d+) ([\\d+,.]*) (\\w{3}+)")
                        .assign((t, v) -> {
                            t.setAmount(t.getAmount() + asAmount(v.get("tax")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                        }).wrap(t -> new TransactionItem(t)));
    
    }

    @SuppressWarnings("nls")
    private void addAccountStatementTransaction()
    {

        DocumentType type = new DocumentType("KONTOAUSZUG", (context, lines) -> {
            Pattern currency = Pattern.compile("BUCHUNGSTAG / BUCHUNGSTEXT BETRAG IN (?<currency>\\w{3}+)");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = currency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group(1));
                }
            }
        });
        this.addDocumentTyp(type);

        Block payInBlock = new Block("\\d+.\\d+.\\d{4}+ Accepted PayIn.*");
        type.addBlock(payInBlock);
        payInBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DEPOSIT);
                            return t;
                        })

                        .section("date", "amount")
                        .match("(?<date>\\d+\\.\\d+\\.\\d{4}|\\d{4}-\\d+-\\d+) Accepted PayIn:.* to.* (?<amount>[\\d+,.]*)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                        })

                        .wrap(TransactionItem::new));

        Block taxRefundBlock = new Block("\\d+.\\d+.\\d{4}+ Steueroptimierung.*");
        type.addBlock(taxRefundBlock);
        taxRefundBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.TAX_REFUND);
                            return t;
                        })

                        .section("date", "amount")
                        .match("(?<date>\\d+\\.\\d+\\.\\d{4}|\\d{4}-\\d+-\\d+) Steueroptimierung null \\d{16} (?<amount>[\\d+,.]*)")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(type.getCurrentContext().get("currency")));
                        }).wrap(TransactionItem::new));
    }
    
    @SuppressWarnings("nls")
    private void addTaxStatementTransaction()
    {
        DocumentType type = new DocumentType("STEUERABRECHNUNG");
        this.addDocumentTyp(type);

        Block taxBlock = new Block("Kapitalertragsteuer Optimierung.*");
        type.addBlock(taxBlock);
        taxBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.TAX_REFUND);
                            return t;
                        })

                        .section("tax", "currency", "date")
                        .match("Kapitalertragsteuer Optimierung (?<tax>[\\d+,.]*) (?<currency>\\w{3}+)")
                        .match("VERRECHNUNGSKONTO VALUTA BETRAG")
                        .match(".* (?<date>\\d+\\.\\d+\\.\\d{4}|\\d{4}-\\d+-\\d+) ([\\d+,.]*)")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("tax")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                        })

                        .section("tax", "currency", "date")
                        .match("Solidaritätszuschlag Optimierung (?<tax>[\\d+,.]*) (?<currency>\\w{3}+)")
                        .match("VERRECHNUNGSKONTO VALUTA BETRAG")
                        .match(".* (?<date>\\d+\\.\\d+\\.\\d{4}|\\d{4}-\\d+-\\d+) ([\\d+,.]*)")
                        .assign((t, v) -> {
                            t.setAmount(t.getAmount() + asAmount(v.get("tax")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                        })

                        .section("tax", "currency", "date").optional()
                        .match("Kirchensteuer Optimierung (?<tax>[\\d+,.]*) (?<currency>\\w{3}+)")
                        .match("VERRECHNUNGSKONTO VALUTA BETRAG")
                        .match(".* (?<date>\\d+\\.\\d+\\.\\d{4}|\\d{4}-\\d+-\\d+) ([\\d+,.]*)")
                        .assign((t, v) -> {
                            t.setAmount(t.getAmount() + asAmount(v.get("tax")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                        }).wrap(TransactionItem::new));
    }

    @Override
    public String getLabel()
    {
        return "Trade Republic"; //$NON-NLS-1$
    }
}
