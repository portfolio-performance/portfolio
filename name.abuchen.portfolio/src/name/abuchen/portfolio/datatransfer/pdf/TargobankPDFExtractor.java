package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class TargobankPDFExtractor extends AbstractPDFExtractor
{
    private static final String regexName = "Wertpapier (?<name>.*)"; //$NON-NLS-1$
    private static final String regexWknAndIsin = "WKN / ISIN (?<wkn>\\S*) / (?<isin>\\S*)"; //$NON-NLS-1$
    private static final String regexAmountAndCurrency = "Konto-Nr. \\w* (?<amount>(\\d+\\.)?\\d+(,\\d+)?) (?<currency>\\w{3}+)"; //$NON-NLS-1$
    private static final String regexDate = "Schlusstag( / Handelszeit)? (?<date>\\d{2}.\\d{2}.\\d{4})( / (?<time>\\d{2}:\\d{2}:\\d{2}))?"; //$NON-NLS-1$
    private static final String regexTime = "(Schlusstag / )?Handelszeit ((?<date>\\d{2}.\\d{2}.\\d{4}) / )?(?<time>\\d{2}:\\d{2}:\\d{2})"; //$NON-NLS-1$
    private static final String regexShares = "St.ck (?<shares>\\d+(,\\d+)?)"; //$NON-NLS-1$
    private static final String regexFees = "Provision (?<fee>(\\d+\\.)?\\d+(,\\d+)?) (?<currency>\\w{3}+)"; //$NON-NLS-1$
    private static final String regexTaxes = "Gesamtsumme Steuern (?<tax>[\\d.]+,\\d+) (?<currency>\\w{3}+)$"; //$NON-NLS-1$

    private static final String TO_BE_DELETED = "to_be_deleted"; //$NON-NLS-1$
    private static final String ATTRIBUTE_PAY_DATE = "pay_date"; //$NON-NLS-1$

    private static final DateTimeFormatter SPECIAL_DATE_FORMAT = DateTimeFormatter.ofPattern("d. MMMM yyyy", //$NON-NLS-1$
                    Locale.GERMANY);

    public TargobankPDFExtractor(Client client) 
    {
        super(client);

        addBankIdentifier("TARGO"); //$NON-NLS-1$
        addBankIdentifier("Targobank"); //$NON-NLS-1$
        addBankIdentifier("TARGOBANK AG"); //$NON-NLS-1$

        addBuyTransaction();
        addSellTransaction();
        addDividendTransaction();
        addDividendTransactionFromTaxDocument();
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Kauf");
        this.addDocumentTyp(type);

        Block block = new Block("(Transaktionstyp )?Kauf");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("name", "wkn", "isin").optional()
                        .match(regexName)
                        .match(regexWknAndIsin)
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("time").optional()
                        .match(regexTime)
                        .assign((t, v) -> {
                            type.getCurrentContext().put("time", v.get("time"));
                        })

                        .section("date").optional()
                        .match(regexDate)
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

                        .section("amount", "currency")
                        .match(regexAmountAndCurrency)
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("fee", "currency").optional()
                        .match(regexFees)
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("shares").optional()
                        .match(regexShares)
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .wrap(t -> {
                            if (t.getPortfolioTransaction().getShares() == 0)
                                throw new IllegalArgumentException(Messages.PDFMsgMissingShares);
                            return new BuySellEntryItem(t);
                        });

        block.set(pdfTransaction);

    }

    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Verkauf");
        this.addDocumentTyp(type);

        Block block = new Block("(Transaktionstyp )?Verkauf");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("name", "wkn", "isin")
                        .match(regexName)
                        .match(regexWknAndIsin)
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("time").optional()
                        .match(regexTime)
                        .assign((t, v) -> {
                            type.getCurrentContext().put("time", v.get("time"));
                        })

                        .section("date").optional()
                        .match(regexDate)
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

                        .section("amount", "currency").optional()
                        .match(regexAmountAndCurrency)
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("fee", "currency").optional()
                        .match(regexFees)
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("tax", "currency").optional() //
                        .match(regexTaxes)
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        .section("shares").optional()
                        .match(regexShares)
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .wrap(t -> {
                            if (t.getPortfolioTransaction().getShares() == 0)
                                throw new IllegalArgumentException(Messages.PDFMsgMissingShares);
                            return new BuySellEntryItem(t);
                        });

        block.set(pdfTransaction);

    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType ertrag = new DocumentType("Ertragsgutschrift \\d.*");
        this.addDocumentTyp(ertrag);

        Block block = new Block("Ertragsgutschrift.*");
        ertrag.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DIVIDENDS);
                            return t;
                        })

                        .section("name", "wkn", "isin", "currency", "shares") //
                        .match("Wertpapier (?<name>.*)") //
                        .match("WKN / ISIN (?<wkn>\\S*) / (?<isin>\\S*)").match("St.ck (?<shares>[\\d.]+(,\\d+)?)")
                        .match("Aussch.ttung pro St.ck ([\\d.]+,\\d+) (?<currency>\\w{3}+).*") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("amount", "currency") //
//                      .match("Konto-Nr. \\d* (?<amount>[\\d.]+,\\d+) (?<currency>\\w{3}+)$").assign((t, v) -> {
                        .match(regexAmountAndCurrency) //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // use document date to having matching dates from
                        // dividend and tax document
                        .section("date") //
                        .match("Ertragsgutschrift (?<date>\\d+.\\d+.\\d{4})$")
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // store real date in attribute
                        .section("date") //
                        .match("Zahlbar (?<date>\\d+.\\d+.\\d{4}+).*").assign((t, v) -> {
                            t.getSecurity().getAttributes().put(new AttributeType(ATTRIBUTE_PAY_DATE),
                                            asDate(v.get("date")));
                        })

                        .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional() //
                        .match("Bruttoertrag (?<fxAmount>[\\d.]+,\\d+) (?<fxCurrency>\\w{3}+)")
                        .match("Devisenkurs zur Handelsw.hrung (\\w{3}+)/(\\w{3}+) (?<exchangeRate>[\\d.]+,\\d+)") //
                        .match("Bruttoertrag in (\\w{3}+) (?<amount>[\\d.]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));

                            if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                            {
                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                                RoundingMode.HALF_DOWN);

                                Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                                asAmount(v.get("fxAmount")));
                                Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                                Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                                t.addUnit(grossValue);
                            }
                        })

                        .wrap(t -> t.getAmount() != 0 ? new TransactionItem(t) : null));

    }

    @SuppressWarnings("nls")
    private void addDividendTransactionFromTaxDocument()
    {

        DocumentType type = new DocumentType("Ertragsgutschrift \\(Steuerbeilage\\) .*");

        this.addDocumentTyp(type);
        Block block = new Block("Ertragsgutschrift \\(Steuerbeilage\\) .*");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DIVIDENDS);
                            return t;
                        })

                        .section("name", "wkn", "isin", "shares") //
                        .match("Wertpapier (?<name>.*)") //
                        .match("WKN / ISIN (?<wkn>\\S*) / (?<isin>\\S*)") //
                        .match("St.ck (?<shares>[\\d.]+(,\\d+)?)") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("amount", "currency") //
                        .match("Ertr.ge/Verluste (?<amount>[\\d.\\s]*,[\\d\\s]+) (?<currency>[A-Z\\s]*)$")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // if it exists, add Teilfreistellungsbetrag to amount
                        .section("amount").optional() //
                        .match("Teilfreistellung .* - (?<amount>[\\d.\\s]*,[\\d\\s]+) ([A-Z\\s]*)$").assign((t, v) -> {
                            t.setAmount(t.getAmount() + asAmount(v.get("amount")));
                        })

                        .section("tax", "currency") //
                        .match(regexTaxes).assign((t, v) -> {
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                            t.setAmount(t.getAmount() - asAmount(v.get("tax")));
                        })

                        // use document date to having matching dates from
                        // dividend and tax document
                        .section("date") //
                        .match("Ertragsgutschrift \\(Steuerbeilage\\) (?<date>\\d+.\\d+.\\d{4}+)$")
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // store real date in attribute
                        .section("date").optional() // example: "27. April 2020"
                        .match("Belastung Ihres Kontos .* mit Wertstellung zum (?<date>\\d+. \\w+ \\d{4}).$")
                        .assign((t, v) -> {
                            LocalDate date = LocalDate.parse(v.get("date"), SPECIAL_DATE_FORMAT);
                            t.getSecurity().getAttributes().put(new AttributeType(ATTRIBUTE_PAY_DATE),
                                            date.atStartOfDay());
                        })

                        .wrap(TransactionItem::new));

    }

    @Override
    public List<Item> postProcessing(List<Item> items)
    {

        // group dividends transactions by date and security
        Map<LocalDateTime, Map<Security, List<Item>>> dividends = items.stream()
                        .filter(TransactionItem.class::isInstance) //
                        .map(TransactionItem.class::cast) //
                        .filter(i -> i.getSubject() instanceof AccountTransaction)
                        .filter(i -> AccountTransaction.Type.DIVIDENDS
                                        .equals(((AccountTransaction) i.getSubject()).getType()))
                        .collect(Collectors.groupingBy(Item::getDate, Collectors.groupingBy(Item::getSecurity)));

        // iterate transactions and combine data from dividend and tax document
        // k is date and v is security
        dividends.forEach((k, v) -> {
            // key is security and transactions are the single transactions
            v.forEach((key, transactions) -> {
                if (transactions.size() == 1)
                {
                    // set correct date stored in attribute, if given in
                    // security, leave "wrong" date otherwise
                    AccountTransaction a1 = (AccountTransaction) transactions.get(0).getSubject();
                    if (a1.getSecurity().getAttributes().get(new AttributeType(ATTRIBUTE_PAY_DATE)) != null)
                    {
                        a1.setDateTime((LocalDateTime) a1.getSecurity().getAttributes()
                                        .get(new AttributeType(ATTRIBUTE_PAY_DATE)));
                    }
                }
                else if (transactions.size() == 2)
                {
                    // get both transactions and make the tax document a1
                    // the tax document must have a tax unit
                    AccountTransaction a1 = (AccountTransaction) transactions.get(0).getSubject();
                    AccountTransaction a2;

                    if (a1.getUnit(Unit.Type.TAX).isPresent())
                    {
                        a2 = (AccountTransaction) transactions.get(1).getSubject();
                    }
                    else
                    {
                        a1 = (AccountTransaction) transactions.get(1).getSubject();
                        a2 = (AccountTransaction) transactions.get(0).getSubject();
                    }

                    // the dividend document might have a gross_value unit,
                    // which needs to be copied over
                    Optional<Unit> unitGross = a2.getUnit(Unit.Type.GROSS_VALUE);
                    if (unitGross.isPresent())
                    {
                        a1.addUnit(unitGross.get());
                    }
                    // set correct date stored in attribute
                    a1.setDateTime((LocalDateTime) a1.getSecurity().getAttributes()
                                    .get(new AttributeType(ATTRIBUTE_PAY_DATE)));

                    // combine document notes and mark transaction 2 to be
                    // deleted
                    a1.setNote(a2.getNote().concat("; ").concat(a1.getNote())); //$NON-NLS-1$
                    a2.setNote(TO_BE_DELETED);
                }
            });
        });

        // group sell transactions by date and security
        Map<LocalDateTime, Map<Security, List<Item>>> sells = items.stream().filter(BuySellEntryItem.class::isInstance) //
                        .map(BuySellEntryItem.class::cast) //
                        .filter(i -> i.getSubject() instanceof BuySellEntry)
                        .filter(i -> PortfolioTransaction.Type.SELL
                                        .equals((((BuySellEntry) i.getSubject()).getPortfolioTransaction().getType())))
                        .collect(Collectors.groupingBy(Item::getDate, Collectors.groupingBy(Item::getSecurity)));

        // iterate transactions and combine data from dividend and tax document
        // k is date and v is security
        sells.forEach((k, v) -> {
            // key is security and transactions are the single transactions
            v.forEach((key, transactions) -> {
                if (transactions.size() == 2)
                {
                    // get both transactions and make the tax document a1
                    // the tax document must have a tax unit
                    BuySellEntry a1 = (BuySellEntry) transactions.get(0).getSubject();
                    BuySellEntry a2;

                    if (a1.getPortfolioTransaction().getUnit(Unit.Type.TAX).isPresent())
                    {
                        a2 = (BuySellEntry) transactions.get(1).getSubject();
                    }
                    else
                    {
                        a1 = (BuySellEntry) transactions.get(1).getSubject();
                        a2 = (BuySellEntry) transactions.get(0).getSubject();
                    }

                    // copy tax unit from a1 over to a2 and mark a1 to be deleted
                    // subtract tax from amount to have correct net amount
                    Optional<Unit> unitTax = a1.getPortfolioTransaction().getUnit(Unit.Type.TAX);
                    if (unitTax.isPresent())
                    {
                        Money tax = unitTax.get().getAmount();
                        a2.setAmount(a2.getPortfolioTransaction().getAmount() - tax.getAmount());
                        a2.getPortfolioTransaction().addUnit(unitTax.get());
                        
                    }
                    // combine document notes and mark transaction 1 to be
                    // deleted
                    a2.setNote(a2.getNote().concat("; ").concat(a1.getNote())); //$NON-NLS-1$
                    a1.setNote(TO_BE_DELETED);
                }
            });
        });
        
        // iterate list and remove items that are marked TO_BE_DELETED
        // Iterator<Item> iter = items.stream().filter(i -> i instanceof
        // BuySellEntryItem).iterator();

        Iterator<Item> iter = items.iterator();
        while (iter.hasNext())
        {
            Object o = iter.next().getSubject();
            if (o instanceof AccountTransaction)
            {
                AccountTransaction a = (AccountTransaction) o;
                if (a.getNote().equals(TO_BE_DELETED))
                {
                    iter.remove();
                }
            }
            else if (o instanceof BuySellEntry)
            {
                BuySellEntry a = (BuySellEntry) o;
                if (a.getNote().equals(TO_BE_DELETED))
                {
                    iter.remove();
                }
            }
        }
        return items;

    }

    @Override
    public String getLabel()
    {
        return "TARGO"; //$NON-NLS-1$
    }

}
