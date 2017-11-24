package name.abuchen.portfolio.datatransfer.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.Transaction.Unit.Type;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;

public class ComdirectPDFExtractor extends AbstractPDFExtractor
{
    public ComdirectPDFExtractor(Client client) throws IOException
    {
        super(client);

        addBankIdentifier("comdirect bank"); //$NON-NLS-1$

        addBuyTransaction();
        addDividendTransaction();
        addTaxTransaction();
        addSellTransaction();
    }
    
    @Override
    protected List<Item> mergeItems(List<Item> items)
    {
        List<Item> results= new ArrayList<>();
        
        List<Item> idTransactions = items.stream()
                        .filter(i -> i instanceof TransactionItem && i.getSubject() instanceof AccountTransaction)
                        .filter(i -> ((AccountTransaction) i.getSubject()).getId() != null)
                        .collect(Collectors.toList());
        List<Item> idTransactionsRef = new ArrayList<>(idTransactions);
        List<Item> remainingTransactions = items.stream()
                        .filter(i -> !(
                                        i instanceof TransactionItem && 
                                        i.getSubject() instanceof AccountTransaction && 
                                        ((AccountTransaction) i.getSubject()).getId() != null
                                        ))
                        .collect(Collectors.toList());
        
        List<Item> mergedTransactions = idTransactions.stream()
                        .map(item -> ((AccountTransaction) item.getSubject()).getId())
                        .distinct()
                        .map(id -> idTransactionsRef.stream()
                                        .filter(item -> id.equals( ((AccountTransaction) item.getSubject()).getId()))
                                        .collect(Collectors.toList()))
                        .map(sameIdItemList -> {
                            if (sameIdItemList.size() == 2){
                                List<AccountTransaction> accountTransactions = sameIdItemList.stream()
                                                .map(item -> (AccountTransaction) item.getSubject())
                                                .collect(Collectors.toList());
                                Optional<AccountTransaction> oDiv = accountTransactions.stream()
                                                .filter(t -> AccountTransaction.Type.DIVIDENDS.equals(t.getType()))
                                                .findFirst();
                                Optional<AccountTransaction> oTax = accountTransactions.stream()
                                                .filter(t -> AccountTransaction.Type.TAXES.equals(t.getType()))
                                                .findFirst();
                                
                                if (oDiv.isPresent() && oTax.isPresent()){
                                    // update div-transaction with tax-transaction: add tax-unit, reduce amount
                                    AccountTransaction tDiv = oDiv.get();
                                    AccountTransaction tTax = oTax.get();
                                    Money internalTax = tDiv.getUnitSum(Type.TAX);
                                    Money externalTax = tTax.getUnitSum(Type.TAX);
                                    
                                    if (internalTax.getCurrencyCode().equals(externalTax.getCurrencyCode())){
                                        tDiv.addUnit(new Unit(Unit.Type.TAX, externalTax));
                                        tDiv.setAmount(tDiv.getAmount() - tTax.getAmount());
                                    }
                                    
                                    return Arrays.asList(new TransactionItem(tDiv));    
                                }
                            }
                            
                            return sameIdItemList;
                        })
                        // TODO: remove all single tax-entries?
                        .flatMap(l -> l.stream())
                        .collect(Collectors.toList());

        results.addAll(mergedTransactions);
        results.addAll(remainingTransactions);
        
        return results;
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

                        .section("date") //
                        .match("Geschäftstag *: (?<date>\\d+.\\d+.\\d{4}+) .*") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("isin", "name", "wkn") //
                        .find("Wertpapier-Bezeichnung *WPKNR/ISIN *") //
                        .match("^(?<name>(\\S{1,} )*) *(?<wkn>\\S*) *$") //
                        .match("(\\S{1,} )* *(?<isin>\\S*) *$") //
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
                        .match("^a *b *g *e *f *ü *h *r *t *e *S *t *e *u *e *r *n *(?<tax>.*)$") //
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
        DocumentType type = new DocumentType("G  u t s c h  ri f t fä  ll ig  e r W  e r t p a p i e r -E  r tr ä g e");
        this.addDocumentTyp(type);

        Block block = new Block(".*G  u t s c h  ri f t fä  ll ig  e r W  e r t p a p i e r -E  r tr ä g e *");
        type.addBlock(block);
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
                            t.setDate(asDate(v.get("date")));
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
                        
                        .section("reference") //
                        .optional() //
                        .find("^\\(Referenz-Nr.\\s+(?<reference>[^\\)]+).*").assign((t, v) -> {
                            t.setId(v.get("reference"));
                        })

                        .wrap(TransactionItem::new));
    }
    
    @SuppressWarnings("nls")
    private void addTaxTransaction()
    {
        // TODO 1: combine with dividend-transaction of other file
        // TODO 2: not matched by buy/sell with taxes...
        DocumentType type = new DocumentType("Steuerliche Behandlung: Ausländische Dividende");    // just char sequence
//        type.setMustExclude("Wertpapierkauf");
        this.addDocumentTyp(type);

        //Block block = new Block("^\\s*Steuerliche Behandlung:.*");
        Block block = new Block("^Kundennr.*"); // earlier for reference
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
                        .match("^.*Die Gutschrift erfolgt mit Valuta\\s+(?<date>\\d{2}.\\d{2}.\\d{4}).*$") //
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                        })

                         
                        .section("tax", "currency").optional()  // Kapitalertragsteuer
                        .match("^\\s*(K\\s*a\\s*p\\s*i\\s*t\\s*a\\s*l\\s*e\\s*r\\s*t\\s*r\\s*a\\s*g\\s*s\\s*t\\s*e\\s*u\\s*e\\s*r)" + //
                                        "(?<currency>[A-Z\\s]+)(?<tax>[\\d\\s,-]+)$") //
                        .assign((t, v) -> {
                            v.put("currency", stripBlanksAndUnderscores(v.get("currency")));
                            v.put("tax", stripBlanksAndUnderscores(v.get("tax")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                        })

                        .section("tax", "currency").optional()  // Kirchensteuer
                        .match("^\\s*(K\\s*i\\s*r\\s*c\\s*h\\s*e\\s*n\\s*s\\s*t\\s*e\\s*u\\s*e\\s*r)" +
                                        "(?<currency>[A-Z\\s_]+)(?<tax>[\\d\\s,-_]+)$")
                        .assign((t, v) -> {
                             v.put("currency", stripBlanksAndUnderscores(v.get("currency")));
                             v.put("tax", stripBlanksAndUnderscores(v.get("tax")));
                             t.addUnit(new Unit(Unit.Type.TAX,
                                             Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                        })
                        
                        .section("tax", "currency").optional() // Solidaritätszuschlag
                        .match("^\\s*(S\\s*o\\s*l\\s*i\\s*d\\s*a\\s*r\\s*i\\s*t\\s*ä\\s*t\\s*s\\s*z\\s*u\\s*s\\s*c\\s*h\\s*l\\s*a\\s*g)"+
                                        "(?<currency>[A-Z\\s_]+)(?<tax>[\\d\\s,-_]+)$")
                        .assign((t, v) -> {
                            v.put("currency", stripBlanksAndUnderscores(v.get("currency")));
                            v.put("tax", stripBlanksAndUnderscores(v.get("tax")));
                            t.addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                        })

                        .section("tax", "currency") // abgeführte Steuern
                        .match("^\\s*(a\\s*b\\s*g\\s*e\\s*f\\s*ü\\s*h\\s*r\\s*t\\s*e\\s*S\\s*t\\s*e\\s*u\\s*er\\s*n)" +
                                        "(?<currency>[A-Z\\s_]+)(?<tax>[\\d\\s,-_]+)$")
                        .assign((t, v) -> {
                             v.put("currency", stripBlanksAndUnderscores(v.get("currency")));
                             v.put("tax", stripBlanksAndUnderscores(v.get("tax")));
                             t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                             t.setAmount(asAmount(v.get("tax")));
                        })
                        
                        .section("reference") //
                        .optional() //  Referenznummer: horrible format a1 CONST: r e f a2 town - but fixed length (for now)
                        .match("^.*R\\s*e\\s*f\\s*e\\s*r\\s*e\\s*n\\s*z\\s*-\\s*N\\s*u\\s*m\\s*m\\s*e\\s*r\\s*:" +
                                        "(?<reference>.*)$").assign((t, v) -> {
                            t.setId(stripBlanks(v.get("reference")).substring(0, 16));
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

                        .section("date") //
                        .match("Geschäftstag *: (?<date>\\d+.\\d+.\\d{4}+) .*") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("isin", "name", "wkn") //
                        .find("Wertpapier-Bezeichnung *WPKNR/ISIN *") //
                        .match("^(?<name>(\\S{1,} )*) *(?<wkn>\\S*) *$") //
                        .match("(\\S{1,} )* *(?<isin>\\S*) *$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares").optional() //
                        .match("^St\\. *(?<shares>[\\d.]+(,\\d+)?) .*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("shares").optional() // teilausführung
                        .match("^ Summe *St\\. *(?<shares>[\\d.]+(,\\d+)?) .*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .find(".*Zu Ihren Gunsten vor Steuern *") //
                        .match(".* \\d+.\\d+.\\d{4}+ *(?<currency>\\w{3}+) *(?<amount>[\\d.]+,\\d+).*") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("tax").optional() //
                        .match("^a *b *g *e *f *ü *h *r *t *e *S *t *e *u *e *r *n *(?<tax>.*)$") //
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
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        .match(".*B.rsenplatzabh.ng. Entgelt *: *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)-? *") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        .match(".*Abwickl.entgelt Clearstream *: *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)-? *") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        .match(".*Gesamtprovision *: *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)-? *") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        .match(".*Umschreibeentgelt *: *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)-? *") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))));
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
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("isin", "name", "wkn") //
                        .find("Wertpapier-Bezeichnung *WPKNR/ISIN *") //
                        .match("^(?<name>(\\S{1,} )*) *(?<wkn>\\S*) *$") //
                        .match("(\\S{1,} )* *(?<isin>\\S*) *$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("tax").optional() //
                        .match("^e *r *s *t *a *t *t *e *t *e *S *t *e *u *e *r *n *(?<tax>.*)$") //
                        .assign((t, v) -> {
                            Unit unit = createTaxUnit(v.get("tax"));
                            if (unit == null || unit.getAmount().isZero())
                                return;

                            t.setMonetaryAmount(unit.getAmount());
                        })

                        .wrap(t -> t.getAmount() == 0L ? null : new TransactionItem(t)));
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
    
    private String stripBlanksAndUnderscores(String input){
        return input.replaceAll("[\\s_]", "");
    }

}
