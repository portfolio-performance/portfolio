package name.abuchen.portfolio.datatransfer.pdf;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Annotated;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.Transaction.Unit.Type;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;

public class ComdirectPDFExtractor extends AbstractPDFExtractor
{
    public ComdirectPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("comdirect bank"); //$NON-NLS-1$

        addBuyTransaction();
        addDividendTransaction();
        addSellTransaction();
        addExpireTransaction();
        addVorabsteuerTransaction();
        addDividendTransactionFromSteuermitteilungPDF();
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierkauf");
        this.addDocumentTyp(type);

        Block block = new Block("^(\\* )?Wertpapierkauf *.*");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("time").optional() //
                        .match("Handelszeit *: (?<time>\\d+:\\d+) Uhr.*") //
                        .assign((t, v) -> {
                            type.getCurrentContext().put("time", v.get("time"));
                        })
                        
                        .section("date") //
                        .match("Geschäftstag *: (?<date>\\d+.\\d+.\\d{4}+) .*") //
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
                        
                        .section("isin", "name", "wkn", "nameContinued") //
                        .find("Wertpapier-Bezeichnung *WPKNR/ISIN *") //
                        .match("^(?<name>(\\S{1,} )*) *(?<wkn>\\S*) *$") //
                        .match("^(?<nameContinued>.*?)\\s{3,} *(?<isin>\\S*) *$") //assume 3 whitespaces as separator between name ans isin
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares").optional() //
                        .match("^St\\. *(?<shares>[\\d.]+(,\\d+)?) .*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("shares").optional() //
                        .match("^ Summe *St\\. *(?<shares>[\\d.]+(,\\d+)?) .*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .find(".*Zu Ihren Lasten( vor Steuern)? *") //
                        .match(".* \\d+.\\d+.\\d{4}+ *(?<currency>\\w{3}+) *(?<amount>[\\d.]+,\\d+).*") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("tax").optional() //
                        .match("^ *a *b *g *e *f *ü *h *r *t *e *S *t *e *u *e *r *n *(?<tax>.*)$") //
                        .assign((t, v) -> {
                            Unit unit = createTaxUnit(v.get("tax"));
                            if (unit == null || unit.getAmount().isZero())
                                return;

                            t.getPortfolioTransaction().addUnit(unit);

                            MutableMoney total = MutableMoney.of(t.getPortfolioTransaction().getCurrencyCode());
                            total.add(t.getPortfolioTransaction().getMonetaryAmount());
                            total.add(unit.getAmount());
                            t.setMonetaryAmount(total.toMoney());
                        })

                        .wrap(t -> {
                            if (t.getPortfolioTransaction().getShares() == 0)
                                throw new IllegalArgumentException(Messages.PDFMsgMissingShares);
                            return new BuySellEntryItem(t);
                        });

        addFeesSection(pdfTransaction);

        block.set(pdfTransaction);

        addTaxRefunds(type, "^(\\* )?Wertpapierkauf *.*");
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType dividende = new DocumentType("Abrechnung Dividendengutschrift");
        this.addDocumentTyp(dividende);

        DocumentType ertrag = new DocumentType("Abrechnung Ertragsgutschrift");
        this.addDocumentTyp(ertrag);

        Block block = new Block(
                        ".*G *u *t *s *c *h *r *i *f *t *f *ä *l *l *i *g *e *r *W *e *r *t *p *a *p *i *e *r *- *E *r *t *r *ä *g *e *");
        dividende.addBlock(block);
        ertrag.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DIVIDENDS);
                            return t;
                        })

                        .section("wkn", "name", "isin", "shares").optional() //
                        .match("^\\s*(p\\s*e\\s*r) *\\+?[\\d .]+  (?<name>.*)      (?<wkn>.*)") //
                        .match("^\\s*(S\\s*T\\s*K) *(?<shares>\\+?[\\d .]+,\\+?[\\d ]+).*    .* {4}(?<isin>.*)$") //
                        .assign((t, v) -> {
                            v.put("isin", stripBlanks(v.get("isin")));
                            v.put("wkn", stripBlanks(v.get("wkn")));
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(stripBlanks(v.get("shares"))));
                        })

                        .section("currency", "amount", "date") //
                        .find(".*Zu Ihren Gunsten vor Steuern *") //
                        .match("^.*(?<date>\\d{2}.\\d{2}.\\d{4}) *(?<currency>\\w{3}+) *(?<amount>[\\d.]+,\\d+) *$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setDateTime(asDate(v.get("date")));
                        })

                        .section("currency", "gross") //
                        .optional() //
                        .find("^Bruttobetrag: *(?<currency>\\w{3}+) *(?<gross>[\\d.]+,\\d+)").assign((t, v) -> {
                            long gross = asAmount(v.get("gross"));
                            long tax = gross - t.getAmount();
                            Unit unit = new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")), tax));
                            if (unit.getAmount().getCurrencyCode().equals(t.getCurrencyCode()))
                                t.addUnit(unit);
                        })

                        .wrap(TransactionItem::new));
    }
    
    
    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierverkauf");
        this.addDocumentTyp(type);

        Block block = new Block("^(\\* )?Wertpapierverkauf *.*");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("time").optional() //
                        .match("Handelszeit *: (?<time>\\d+:\\d+) Uhr.*") //
                        .assign((t, v) -> {
                            type.getCurrentContext().put("time", v.get("time"));
                        })
                        
                        .section("date") //
                        .match("Geschäftstag *: (?<date>\\d+.\\d+.\\d{4}+) .*") //
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

                        .section("isin", "name", "wkn", "nameContinued") //
                        .find("Wertpapier-Bezeichnung *WPKNR/ISIN *") //
                        .match("^(?<name>(\\S{1,} )*) *(?<wkn>\\S*) *$") //
                        .match("^(?<nameContinued>.*?)\\s{3,} *(?<isin>\\S*) *$") //assume 3 whitespaces as separator
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares").optional() //
                        .match("^St\\. *(?<shares>[\\d.]+(,\\d+)?) .*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("shares").optional() // teilausführung
                        .match("^ Summe *St\\. *(?<shares>[\\d.]+(,\\d+)?) .*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .find(".*(Zu Ihren Gunsten vor Steuern|Zu Ihren Lasten vor Steuern) *") //
                        .match(".* \\d+.\\d+.\\d{4}+ *(?<currency>\\w{3}+) *(?<amount>[\\d.]+,\\d+-?).*") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            if (v.get("amount").indexOf("-") != -1)
                            {
                                t.setAmount(-asAmount(v.get("amount").substring(0, v.get("amount").length() - 1)));
                            }
                            else 
                            {
                                t.setAmount(asAmount(v.get("amount")));
                            }
                        })

                        .section("tax").optional() //
                        .match("^ *a *b *g *e *f *ü *h *r *t *e *S *t *e *u *e *r *n *(?<tax>.*)$") //
                        .assign((t, v) -> {
                            Unit unit = createTaxUnit(v.get("tax"));
                            if (unit == null || unit.getAmount().isZero())
                                return;

                            t.getPortfolioTransaction().addUnit(unit);

                            MutableMoney total = MutableMoney.of(t.getPortfolioTransaction().getCurrencyCode());
                            total.add(t.getPortfolioTransaction().getMonetaryAmount());
                            total.subtract(unit.getAmount());
                            t.setMonetaryAmount(total.toMoney());
                        })

                        .wrap(BuySellEntryItem::new);

        addFeesSection(pdfTransaction);

        block.set(pdfTransaction);

        addTaxRefunds(type, "^(\\* )?Wertpapierverkauf *.*");
    }

    @SuppressWarnings("nls")
    private void addFeesSection(Transaction<BuySellEntry> pdfTransaction)
    {
        pdfTransaction.section("fee", "currency").optional()
                        .match(".*Provision *: *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)-? *") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        .match(".*B.rsenplatzabh.ng. Entgelt *: *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)-? *") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        .match(".*Abwickl.entgelt Clearstream *: *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)-? *") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        .match(".*Gesamtprovision *: *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)-? *") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        .match(".*Umschreibeentgelt *: *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)-? *") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")), 
                                                                        asAmount(v.get("fee"))))))
                        
                        .section("fee", "currency").optional()
                        .match(".*Variable B.rsenspesen *: *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)-? *") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")), 
                                                                        asAmount(v.get("fee"))))));
    }

    @SuppressWarnings("nls")
    private void addTaxRefunds(DocumentType type, String blockMarker)
    {
        // tax refunds --> separate transaction
        Block block = new Block(blockMarker);
        type.addBlock(block);

        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.TAX_REFUND);
                            return t;
                        })

                        .section("date") //
                        .match("Geschäftstag *: (?<date>\\d+.\\d+.\\d{4}+) .*") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .section("isin", "name", "wkn") //
                        .find("Wertpapier-Bezeichnung *WPKNR/ISIN *") //
                        .match("^(?<name>(\\S{1,} )*) *(?<wkn>\\S*) *$") //
                        .match("(\\S{1,} )* *(?<isin>\\S*) *$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("tax").optional() //
                        .match("^ *e *r *s *t *a *t *t *e *t *e *S *t *e *u *e *r *n *(?<tax>.*)$") //
                        .assign((t, v) -> {
                            Unit unit = createTaxUnit(v.get("tax"));
                            if (unit == null || unit.getAmount().isZero())
                                return;

                            t.setMonetaryAmount(unit.getAmount());
                        })

                        .wrap(t -> t.getAmount() == 0L ? null : new TransactionItem(t)));
    }
    
    @SuppressWarnings("nls")
    private void addExpireTransaction()
    {
        DocumentType type = new DocumentType("A *b *r *e *c *h *n *u *n *g *f *ä *l *l *i *g *e *r *W *e *r *t *p *a *p *i *e *r *e *");
        this.addDocumentTyp(type);

        Block block = new Block("^Einlösung *");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("date", "name", "nameContinued", "wkn", "shares", "isin")
                        .match("^ *p *e *r *(?<date> \\d *\\d *\\. *\\d *\\d *\\. *\\d *\\d *\\d *\\d)\\s{3,}(?<name>.*)\\s{3,}(?<wkn>.*) *$") //assume 3 whitespaces as separator
                        .match("^ *S *T *K *(?<shares>[\\d. ]+(,[\\d ]+)?) *(?<nameContinued>(\\S{1,} {1,2})*) *(?<isin>[\\S ]*) *$")
                        .assign((t, v) -> {
                            v.put("isin", stripBlanksAndUnderscores(v.get("isin")));
                            v.put("wkn", stripBlanksAndUnderscores(v.get("wkn")));
                            v.put("date", stripBlanksAndUnderscores(v.get("date")));
                            v.put("shares", stripBlanksAndUnderscores(v.get("shares")));
                            v.put("name", stripBlanksAndUnderscores(v.get("name")));
                            v.put("nameContinued", stripBlanksAndUnderscores(v.get("nameContinued")));
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setDate(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("amount", "currency") //
                        .match("^Kurswert Einl.sung *(?<currency>\\w{3}+) *(?<amount>[\\d,]*) *$")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(BuySellEntryItem::new);

        addFeesSection(pdfTransaction);
        
        block.set(pdfTransaction);
    }
    
    @SuppressWarnings("nls")
    private void addVorabsteuerTransaction()
    {
        DocumentType type = new DocumentType("Vorabpauschale");

        this.addDocumentTyp(type);

        Block block = new Block("^\\s*Steuerliche Behandlung:.*");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.TAXES);
                            return t;
                        })

                        .section("wkn", "name", "isin", "shares").optional() //
                        .match("^(Stk.)\\W*(?<shares>\\d[\\d .,]*)(?<name>.*),\\W*(WKN / ISIN:)(?<wkn>.*)/(?<isin>.*)$") //
                        .assign((t, v) -> {
                            v.put("isin", stripBlanks(v.get("isin")));
                            v.put("wkn", stripBlanks(v.get("wkn")));
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(stripBlanks(v.get("shares"))));
                        })

                        .section("date") //
                        .match("^.*Die Belastung erfolgt mit Valuta\\s+(?<date>\\d{2}.\\d{2}.\\d{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .section("tax", "currency").optional() // Kapitalertragsteuer
                        .match("^\\s*(K\\s*a\\s*p\\s*i\\s*t\\s*a\\s*l\\s*e\\s*r\\s*t\\s*r\\s*a\\s*g\\s*s\\s*t\\s*e\\s*u\\s*e\\s*r)"
                                        + //
                                        "(?<currency>[A-Z\\s]+)(?<tax>[\\d\\s,-]+)$") //
                        .assign((t, v) -> {
                            v.put("currency", stripBlanksAndUnderscores(v.get("currency")));
                            v.put("tax", stripBlanksAndUnderscores(v.get("tax")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                        })

                        .section("tax", "currency").optional() // Kirchensteuer
                        .match("^\\s*(K\\s*i\\s*r\\s*c\\s*h\\s*e\\s*n\\s*s\\s*t\\s*e\\s*u\\s*e\\s*r)"
                                        + "(?<currency>[A-Z\\s_]+)(?<tax>[\\d\\s,-_]+)$")
                        .assign((t, v) -> {
                            v.put("currency", stripBlanksAndUnderscores(v.get("currency")));
                            v.put("tax", stripBlanksAndUnderscores(v.get("tax")));
                            t.addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                        })

                        .section("tax", "currency").optional() // Solidaritätszuschlag
                        .match("^\\s*(S\\s*o\\s*l\\s*i\\s*d\\s*a\\s*r\\s*i\\s*t\\s*ä\\s*t\\s*s\\s*z\\s*u\\s*s\\s*c\\s*h\\s*l\\s*a\\s*g)"
                                        + "(?<currency>[A-Z\\s_]+)(?<tax>[\\d\\s,-_]+)$")
                        .assign((t, v) -> {
                            v.put("currency", stripBlanksAndUnderscores(v.get("currency")));
                            v.put("tax", stripBlanksAndUnderscores(v.get("tax")));
                            t.addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                        })

                        .section("tax", "currency") // abgeführte Steuern
                        .match("^\\s*(a\\s*b\\s*g\\s*e\\s*f\\s*ü\\s*h\\s*r\\s*t\\s*e\\s*S\\s*t\\s*e\\s*u\\s*er\\s*n)"
                                        + "(?<currency>[A-Z\\s_]+)(?<tax>[\\d\\s,-_]+)$")
                        .assign((t, v) -> {
                            v.put("currency", stripBlanksAndUnderscores(v.get("currency")));
                            v.put("tax", stripBlanksAndUnderscores(v.get("tax")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("tax")));
                        })

                        .wrap(TransactionItem::new));
    }
    
    

    @SuppressWarnings("nls")
    private void addDividendTransactionFromSteuermitteilungPDF()
    {
        
        DocumentType type = new DocumentType("Steuerliche Behandlung: (Aus|In)ländische (Dividende|Investment-Aussch.ttung)");

        this.addDocumentTyp(type);
        Block block = new Block("^\\s*Steuerliche Behandlung:.*");        
        
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DIVIDENDS);
                            return t;
                        })

                        .section("wkn", "name", "isin", "shares").optional() //
                        .match("^(Stk.)\\W*(?<shares>\\d[\\d .,]*)(?<name>.*),\\W*(WKN / ISIN:)(?<wkn>.*)/(?<isin>.*)$") //
                        .assign((t, v) -> {
                            v.put("isin", stripBlanks(v.get("isin")));
                            v.put("wkn", stripBlanks(v.get("wkn")));
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(stripBlanks(v.get("shares"))));
                        })

                        .section("currency", "amount")
                        .find(".*(Z\\s*u\\s*I\\s*h\\s*r\\s*e\\s*n\\s*G\\s*u\\s*n\\s*s\\s*t\\s*e\\s*n\\s*n\\s*a\\s*c\\s*h\\s*S\\s*t\\s*e\\s*u\\s*e\\s*r\\s*n\\s*:)\\s*(?<currency>[A-Z\\s]*)\\s*(?<amount>[\\d.\\s]*,[\\d\\s]+).*")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                        })

                        .section("currency", "gross1", "gross2")
                        .match(".*(Z\\s*u\\s*I\\s*h\\s*r\\s*e\\s*n\\s*G\\s*u\\s*n\\s*s\\s*t\\s*e\\s*n\\s*v\\s*o\\s*r\\s*S\\s*t\\s*e\\s*u\\s*e\\s*r\\s*n\\s*:)\\s*(?<currency>[A-Z\\s]*)\\s*(?<gross1>[\\d.\\s]*,[\\d\\s]+)")
                        .match(".*(S\\s*t\\s*e\\s*u\\s*e\\s*r\\s*b\\s*e\\s*m\\s*e\\s*s\\s*s\\s*u\\s*n\\s*g\\s*s\\s*g\\s*r\\s*u\\s*n\\s*d\\s*l\\s*a\\s*g\\s*e\\s*(v\\s*o\\s*r\\s*V\\s*e\\s*r\\s*l\\s*u\\s*s\\s*t\\s*v\\s*e\\s*r\\s*r\\s*e\\s*c\\s*h\\s*n\\s*u\\s*n\\s*g)?)\\s*(?<currency>[A-Z\\s]*)\\s*(?<gross2>[\\d.\\s]*,[\\d\\s]+)")
                        .assign((t, v) -> {
                            long amount = t.getAmount();
                            long gross1 = asAmount(stripBlanks(v.get("gross1")));
                            long gross2 = asAmount(stripBlanks(v.get("gross2")));
                            long tax = 0;
                            
                            if (gross1 > gross2)
                                tax = gross1 - amount; // Betrag vor Steuern > Steuerbemessungsgrundlage
                            else 
                                tax = gross2 - amount; // Betrag vor Steuern < Steuerbemessungsgrundlage

                            if (tax > 0)
                                t.addUnit(new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")), tax)));
                        })
                        
                        .section("date") //
                        .match("^(Die Gutschrift erfolgt mit Valuta) (?<date>\\d+.\\d+.\\d{4}+).*")
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                        
                        .wrap(TransactionItem::new));
    }

    @Override
    public List<Item> postProcessing(List<Item> items)
    {

        // group dividends into tax + nontax
        Map<LocalDateTime, Map<Security, List<Item>>> dividends = items.stream()
                        .filter(TransactionItem.class::isInstance) //
                        .map(TransactionItem.class::cast) //
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> AccountTransaction.Type.DIVIDENDS
                                        .equals(((AccountTransaction) i.getSubject()).getType()))
                        .collect(Collectors.groupingBy(Item::getDate, Collectors.groupingBy(Item::getSecurity)));

        // only remove non-tax-dividends, where additional tax-dividend exists
        // bugfix for tax-files without taxes (or both entries will be deleted)

        Map<LocalDateTime, Security> deleted = new HashMap<>();
        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext())
        {
            Item i = iterator.next();
            if (isDividendTransactionWithoutTax(i))
            {
                List<Item> similarTransactions = dividends.get(i.getDate()).get(i.getSecurity());
                if (similarTransactions.size() == 2)
                {
                    // already deleted once?
                    if (!(deleted.containsKey(i.getDate()) && i.getSecurity().equals(deleted.get(i.getDate()))))
                    {
                        // mark in hashMap and delete it
                        deleted.put(i.getDate(), i.getSecurity());
                        iterator.remove();
                    }
                }
            }
        }

        return items;
    }

    private boolean isDividendTransactionWithoutTax(Item i)
    {
        if (i instanceof TransactionItem)
        {
            Annotated s = ((TransactionItem) i).getSubject();
            if (s instanceof AccountTransaction)
            {
                AccountTransaction a = (AccountTransaction) s;
                return AccountTransaction.Type.DIVIDENDS.equals(a.getType()) && !a.getUnit(Type.TAX).isPresent();
            }
        }
        return false;
    }

    @SuppressWarnings("nls")
    private Unit createTaxUnit(String taxString)
    {
        String tax = taxString.replaceAll("[_ ]*", "");

        Pattern pattern = Pattern.compile("(?<currency>\\w{3}+)-?(?<amount>[\\d.]+,\\d+)");
        Matcher matcher = pattern.matcher(tax);
        if (!matcher.matches())
            return null;

        return new Unit(Unit.Type.TAX,
                        Money.of(asCurrencyCode(matcher.group("currency")), asAmount(matcher.group("amount"))));
    }

    @Override
    public String getLabel()
    {
        return "comdirect"; //$NON-NLS-1$
    }

    private String stripBlanks(String input)
    {
        return input.replaceAll("\\s", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String stripBlanksAndUnderscores(String input)
    {
        return input.replaceAll("[\\s_]", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

}
