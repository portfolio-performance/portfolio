package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        addBankIdentifier("comdirect"); //$NON-NLS-1$

        addBuyTransaction();
        addDividendTransaction();
        addSellTransaction();
        addExpireTransaction();
        addVorabsteuerTransaction();
        addDividendTransactionFromSteuermitteilungPDF();
        addFeesFromVerwahrentgeltPDF();
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
                        // assume 3 whitespaces as seperator
                        .match("^(?<nameContinued>.*?)\\s{3,} *(?<isin>\\S*) *$")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares").optional() //
                        .match("^St\\. *(?<shares>[\\d\\.]+(,\\d+)?) .*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("shares").optional() //
                        .match("^ Summe *St\\. *(?<shares>[\\d\\.]+(,\\d+)?) .*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .find(".*Zu Ihren Lasten( vor Steuern)? *") //
                        .match(".* \\d+.\\d+.\\d{4}+ *(?<currency>\\w{3}) *(?<amount>[\\d\\.]+,\\d+).*") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fxcurrency", "fxamount", "exchangeRate").optional() //
                        .match(".*Kurswert *: *(?<fxcurrency>\\w{3}) *(?<fxamount>[\\d\\.]+,\\d+).*")
                        .match(".*Umrechn. zum Dev. kurs * (?<exchangeRate>[\\d\\.]+,\\d+) .*") //
                        .assign((t, v) -> {

                            // read the forex currency, exchange rate and gross
                            // amount
                            // in forex currency
                            String forex = asCurrencyCode(v.get("fxcurrency"));
                            if (t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(forex))
                            {
                                BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                                RoundingMode.HALF_DOWN);

                                // gross given in forex currency
                                long fxAmount = asAmount(v.get("fxamount"));
                                long amount = reverseRate.multiply(BigDecimal.valueOf(fxAmount))
                                                .setScale(0, RoundingMode.HALF_DOWN).longValue();

                                Unit grossValue = new Unit(Unit.Type.GROSS_VALUE,
                                                Money.of(t.getPortfolioTransaction().getCurrencyCode(), amount),
                                                Money.of(forex, fxAmount), reverseRate);

                                t.getPortfolioTransaction().addUnit(grossValue);
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
                            total.add(unit.getAmount());
                            t.setMonetaryAmount(total.toMoney());
                        })

                        .wrap(t -> {
                            if (t.getPortfolioTransaction().getShares() == 0)
                                throw new IllegalArgumentException(Messages.PDFMsgMissingShares);
                            return new BuySellEntryItem(t);
                        });

        addFeesSection(pdfTransaction, type);

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
                        .match("^.*(?<date>\\d{2}.\\d{2}.\\d{4}) *(?<currency>\\w{3}) *(?<amount>[\\d\\.]+,\\d+) *$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setDateTime(asDate(v.get("date")));
                        })

                        .section("exchangeRate") //
                        .optional() //
                        .match(".*zum Devisenkurs: \\w{3}\\/\\w{3} *(?<exchangeRate>[\\d\\.]+,\\d+) .*") //
                        .assign((t, v) -> {

                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            dividende.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                        })

                        .section("currency", "gross") //
                        .optional() //
                        .match("^Bruttobetrag: *(?<currency>\\w{3}) *(?<gross>[\\d\\.]+,\\d+).*") //
                        .assign((t, v) -> {

                            String currency = asCurrencyCode(v.get("currency"));
                            long gross = asAmount(v.get("gross"));
                            long taxAmount = gross - t.getAmount();

                            if (!t.getCurrencyCode().equals(currency))
                            {
                                BigDecimal exchangeRate = new BigDecimal(
                                                dividende.getCurrentContext().get("exchangeRate"));
                                taxAmount = gross - exchangeRate.multiply(BigDecimal.valueOf(t.getAmount()))
                                                .setScale(0, RoundingMode.HALF_DOWN).longValue();
                            }
                            Money tax = Money.of(asCurrencyCode(v.get("currency")), taxAmount);
                            PDFExtractorUtils.checkAndSetTax(tax, t, dividende);

                            if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                            {
                                BigDecimal exchangeRate = new BigDecimal(
                                                dividende.getCurrentContext().get("exchangeRate"));
                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                                RoundingMode.HALF_DOWN);
                                Money grossFx = Money.of(currency, gross);
                                // convert gross to local currency using
                                // exchangeRate
                                gross = inverseRate
                                                .multiply(BigDecimal.valueOf(gross).setScale(0, RoundingMode.HALF_DOWN))
                                                .longValue();
                                Money grossTx = Money.of(t.getCurrencyCode(), gross);
                                t.addUnit(new Unit(Unit.Type.GROSS_VALUE, grossTx, grossFx, inverseRate));
                            }

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
                        // assume 3 whitespaces as separator
                        .match("^(?<nameContinued>.*?)\\s{3,} *(?<isin>\\S*) *$")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares").optional() //
                        .match("^St\\. *(?<shares>[\\d\\.]+(,\\d+)?) .*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("shares").optional() // teilausführung
                        .match("^ Summe *St\\. *(?<shares>[\\d\\.]+(,\\d+)?) .*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .find(".*(Zu Ihren Gunsten vor Steuern|Zu Ihren Lasten vor Steuern) *") //
                        .match(".* \\d+.\\d+.\\d{4}+ *(?<currency>\\w{3}) *(?<amount>[\\d\\.]+,\\d+-?).*") //
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

        addFeesSection(pdfTransaction, type);

        block.set(pdfTransaction);

        addTaxRefunds(type, "^(\\* )?Wertpapierverkauf *.*");
    }

    @SuppressWarnings("nls")
    private void addFeesSection(Transaction<BuySellEntry> pdfTransaction, DocumentType type)
    {
        pdfTransaction.section("fee", "currency").optional()
                        .match(".*Provision *: *(?<currency>\\w{3}) *(?<fee>[\\d\\.-]+,\\d+)-? *") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        .match(".*B.rsenplatzabh.ng. Entgelt *: *(?<currency>\\w{3}) *(?<fee>[\\d\\.-]+,\\d+)-? *") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        .match(".*Abwickl.entgelt Clearstream *: *(?<currency>\\w{3}) *(?<fee>[\\d\\.-]+,\\d+)-? *") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("exchangeRate").optional() //
                        .match(".*Umrechn. zum Dev. kurs * (?<exchangeRate>[\\d\\.]+,\\d+) .*") //
                        .assign((t, v) -> {
                            type.getCurrentContext().put("exchangeRate", v.get("exchangeRate"));
                        })

                        .section("fee", "currency").optional()
                        .match(".*Fremde Spesen *: *(?<currency>\\w{3}) *(?<fee>[\\d\\.-]+,\\d+)-? *") //
                        .assign((t, v) -> {
                            String currency = asCurrencyCode(v.get("currency"));
                            // fee is in transaction currency, just add it,
                            // convert to transaction currency otherwise
                            if (t.getPortfolioTransaction().getCurrencyCode().equals(currency))
                            {
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                                Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")))));
                            }
                            else
                            {
                                String rate = (String) type.getCurrentContext().get("exchangeRate");
                                BigDecimal exchangeRate = asExchangeRate(rate);
                                BigDecimal reverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                                RoundingMode.HALF_DOWN);

                                // fee in forex currency
                                long fxFee = asAmount(v.get("fee"));
                                long fee = reverseRate.multiply(BigDecimal.valueOf(fxFee))
                                                .setScale(0, RoundingMode.HALF_DOWN).longValue();

                                Unit feeUnit = null;
                                if (t.getPortfolioTransaction().getSecurity().getCurrencyCode().equals(currency))
                                {
                                    feeUnit = new Unit(Unit.Type.FEE,
                                                    Money.of(t.getPortfolioTransaction().getCurrencyCode(), fee),
                                                    Money.of(asCurrencyCode(v.get("currency")), fxFee), reverseRate);
                                }
                                else
                                {
                                    feeUnit = new Unit(Unit.Type.FEE,
                                                    Money.of(t.getPortfolioTransaction().getCurrencyCode(), fee));
                                }

                                t.getPortfolioTransaction().addUnit(feeUnit);

                            }
                        })

                        .section("fee", "currency").optional()
                        .match(".*Gesamtprovision *: *(?<currency>\\w{3}) *(?<fee>[\\d\\.-]+,\\d+)-? *") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        .match(".*Umschreibeentgelt *: *(?<currency>\\w{3}) *(?<fee>[\\d\\.-]+,\\d+)-? *") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        .match(".*Variable B.rsenspesen *: *(?<currency>\\w{3}) *(?<fee>[\\d\\.-]+,\\d+)-? *") //
                        .assign((t, v) -> t.getPortfolioTransaction() //
                                        .addUnit(new Unit(Unit.Type.FEE, //
                                                        Money.of(asCurrencyCode(v.get("currency")), //
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
        DocumentType type = new DocumentType(
                        "A *b *r *e *c *h *n *u *n *g *f *ä *l *l *i *g *e *r *W *e *r *t *p *a *p *i *e *r *e *");
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
                        // assume 3 whitespaces as separator
                        .match("^ *p *e *r *(?<date> \\d *\\d *\\. *\\d *\\d *\\. *\\d *\\d *\\d *\\d)\\s{3,}(?<name>.*)\\s{3,}(?<wkn>.*) *$")
                        .match("^ *S *T *K *(?<shares>[\\d\\. ]+(,[\\d ]+)?) *(?<nameContinued>(\\S{1,} {1,2})*) *(?<isin>[\\S ]*) *$")
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
                        .match("^Kurswert Einl.sung *(?<currency>\\w{3}) *(?<amount>[\\d,]*) *$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(BuySellEntryItem::new);

        addFeesSection(pdfTransaction, type);

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

        DocumentType type = new DocumentType(
                        "Steuerliche Behandlung: (Aus|In)ländische (Dividende|Investment-Aussch.ttung)");

        this.addDocumentTyp(type);
        Block block = new Block("^\\s*Steuerliche Behandlung:.*", "^Die Gutschrift erfolgt mit Valuta .*");

        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DIVIDENDS);
                            return t;
                        })

                        .section("wkn", "name", "isin", "shares").optional() //
                        .match("^(Stk.)\\W*(?<shares>\\d[\\d\\.,]*)\\W+(?<name>.*),\\W*(WKN / ISIN:)(?<wkn>.*)/(?<isin>.*)$") //
                        .assign((t, v) -> {
                            v.put("isin", stripBlanks(v.get("isin")));
                            v.put("wkn", stripBlanks(v.get("wkn")));
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(stripBlanks(v.get("shares"))));
                        })

                        .section("currency", "amount")
                        .find("^\\s*(Z\\s*u\\s*I\\s*h\\s*r\\s*e\\s*n\\s*G\\s*u\\s*n\\s*s\\s*t\\s*e\\s*n\\s*n\\s*a\\s*c\\s*h\\s*S\\s*t\\s*e\\s*u\\s*e\\s*r\\s*n\\s*:)\\s*(?<currency>[A-Z\\s]*)\\s*(?<amount>[\\d\\.\\s]*,[\\d\\s]+).*")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                        })

                        .section("currency", "gross1", "gross2")
                        .match("^\\s*(Z\\s*u\\s*I\\s*h\\s*r\\s*e\\s*n\\s*G\\s*u\\s*n\\s*s\\s*t\\s*e\\s*n\\s*v\\s*o\\s*r\\s*S\\s*t\\s*e\\s*u\\s*e\\s*r\\s*n\\s*:)\\s*(?<currency>[A-Z\\s]*)\\s*(?<gross1>[\\d\\.\\s]*,[\\d\\s]+)")
                        .match("^\\s*(S\\s*t\\s*e\\s*u\\s*e\\s*r\\s*b\\s*e\\s*m\\s*e\\s*s\\s*s\\s*u\\s*n\\s*g\\s*s\\s*g\\s*r\\s*u\\s*n\\s*d\\s*l\\s*a\\s*g\\s*e\\s*(v\\s*o\\s*r\\s*V\\s*e\\s*r\\s*l\\s*u\\s*s\\s*t\\s*v\\s*e\\s*r\\s*r\\s*e\\s*c\\s*h\\s*n\\s*u\\s*n\\s*g)?)\\s*(\\(\\s*1\\s*\\))?\\s*(?<currency>[A-Z\\s]*)\\s*(?<gross2>[\\d\\.\\s]*,[\\d\\s]+)")
                        .assign((t, v) -> {
                            long amount = t.getAmount();
                            long gross1 = asAmount(stripBlanks(v.get("gross1")));
                            long gross2 = asAmount(stripBlanks(v.get("gross2")));
                            long tax = 0;

                            if (gross1 > gross2)
                                // vor Steuern > Steuerbemessungsgrundlage
                                tax = gross1 - amount;
                            else
                                // vor Steuern < Steuerbemessungsgrundlage
                                tax = gross2 - amount;

                            if (tax > 0)
                                t.addUnit(new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currency")), tax)));
                        })

                        .section("date") //
                        .match("^(Die Gutschrift erfolgt mit Valuta) (?<date>\\d+\\.\\d+\\.\\d{4}+).*")
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .wrap(TransactionItem::new));
    }

    @SuppressWarnings("nls")
    private void addFeesFromVerwahrentgeltPDF()
    {

        DocumentType type = new DocumentType("Verwahrentgelt");

        this.addDocumentTyp(type);
        Block block = new Block("^.*Verwahrentgelt.*");

        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.FEES);
                            return t;
                        })

                        .section("wkn", "date").optional()
                        .match("^.*Verwahrentgelt .*, WKN (?<wkn>\\S+) (?<date>\\d+.\\d+.\\d{4}).*$") //
                        .assign((t, v) -> {
                            v.put("wkn", stripBlanks(v.get("wkn")));
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setDateTime(asDate(v.get("date")));
                        })

                        .section("currency", "amount")
                        .match("^.* Buchung von (?<amount>\\d+,\\d+) (?<currency>\\w+) .*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                        })

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

        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext())
        {
            Item i = iterator.next();

            if (isDividendTransaction(i))
            {
                List<Item> similarTransactions = dividends.get(i.getDate()).get(i.getSecurity());

                // exist multiple div transactions?
                if (similarTransactions.size() == 2)
                {
                    AccountTransaction a1 = (AccountTransaction) similarTransactions.get(0).getSubject();
                    AccountTransaction a2;
                    int ownIndex = 0;

                    // a1 = self, a2 = other
                    if (i.equals(similarTransactions.get(0)))
                    {
                        a2 = (AccountTransaction) similarTransactions.get(1).getSubject();
                    }
                    else
                    {
                        a1 = (AccountTransaction) similarTransactions.get(1).getSubject();
                        a2 = (AccountTransaction) similarTransactions.get(0).getSubject();
                        ownIndex = 1;
                    }

                    if (a2.getUnit(Type.TAX).isPresent())
                    {
                        // if tax of a1 < tax of a2
                        if (!a1.getUnit(Type.TAX).isPresent() || a2.getUnit(Type.TAX).get().getAmount()
                                        .isGreaterOrEqualThan(a1.getUnit(Type.TAX).get().getAmount()))
                        {

                            // store potential gross unit
                            Optional<Unit> unitGross = a1.getUnit(Unit.Type.GROSS_VALUE);
                            if (unitGross.isPresent())
                            {
                                a2.addUnit(unitGross.get());
                            }

                            // remove self and own divTransaction
                            iterator.remove();
                            dividends.get(i.getDate()).get(i.getSecurity()).remove(ownIndex);
                        }

                    } // else wait for a2's round

                }
            }
        }

        return items;
    }

    private boolean isDividendTransaction(Item i)
    {
        if (i instanceof TransactionItem)
        {
            Annotated s = ((TransactionItem) i).getSubject();
            if (s instanceof AccountTransaction)
            {
                AccountTransaction a = (AccountTransaction) s;
                return AccountTransaction.Type.DIVIDENDS.equals(a.getType());
            }
        }
        return false;
    }

    @SuppressWarnings("nls")
    private Unit createTaxUnit(String taxString)
    {
        String tax = taxString.replaceAll("[_ ]*", "");

        Pattern pattern = Pattern.compile("(?<currency>\\w{3})-?(?<amount>[\\d\\.]+,\\d+)");
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
