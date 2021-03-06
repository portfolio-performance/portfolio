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
import name.abuchen.portfolio.money.Values;

public class FinTechGroupBankPDFExtractor extends AbstractPDFExtractor
{

    public FinTechGroupBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("biw AG"); //$NON-NLS-1$
        addBankIdentifier("FinTech Group Bank AG"); //$NON-NLS-1$
        addBankIdentifier("flatex Bank AG"); //$NON-NLS-1$

        addBuySellTransaction();
        addBuyTransaction();
        addBuyfromSavingsplanTransaction();
        addDepositAndWithdrawalTransaction();
        addDividendTransaction();
        addForeignDividendTransaction();
        addSellTransaction();
        addTransferInOutTransaction();
        addTransferOutTransaction();
        addRemoveTransaction();
        addRemoveNewFormatTransaction();
        addOverdraftinterestTransaction();
        addTaxoptimisationTransaction();
        addVorabpauschaleTransaction();
    }

    @SuppressWarnings("nls")
    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Sammelabrechnung \\(Wertpapierkauf/-verkauf\\)");
        this.addDocumentTyp(type);

        Block block = new Block("Nr.(\\d*)/(\\d*) *Kauf.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("wkn", "isin", "name")
                        .match("Nr.[0-9A-Za-z]*/(\\d*) *Kauf *(?<name>.*) *\\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares", "date", "time")
                        .match("^davon ausgef\\. *: (?<shares>[.\\d]+,\\d*) St\\. *Schlusstag *: *(?<date>\\d+.\\d+.\\d{4}+), (?<time>\\d+:\\d+)(.+)?")
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares")));
                            if (v.get("time") != null)
                                t.setDate(asDate(v.get("date"), v.get("time")));
                            else
                                t.setDate(asDate(v.get("date")));
                        })

                        .section("amount", "currency") //
                        .match(".* Endbetrag *: *(?<amount>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fee", "currency").optional() //
                        .match(".* Provision *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* Eigene Spesen *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* \\*Fremde Spesen *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .wrap(BuySellEntryItem::new));

        block = new Block("Nr.(\\d*)/(\\d*) *Verkauf.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("wkn", "isin", "name")
                        .match("Nr.(\\d*)/(\\d*) *Verkauf *(?<name>.*) *\\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares", "date", "time")
                        .match("^davon ausgef\\. *: (?<shares>[.\\d]+,\\d*) St\\. *Schlusstag *: *(?<date>\\d+.\\d+.\\d{4}+), (?<time>\\d+:\\d+)(.+)?")
                        .assign((t, v) -> {
                            t.setShares(asShares(v.get("shares")));
                            if (v.get("time") != null)
                                t.setDate(asDate(v.get("date"), v.get("time")));
                            else
                                t.setDate(asDate(v.get("date")));
                        })

                        .section("amount", "currency") //
                        .match(".* Endbetrag *: *(?<amount>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fee", "currency").optional() //
                        .match(".* Provision *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* Eigene Spesen *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* \\*Fremde Spesen *: *(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        // read exchange rate and store in context
                        .section("exchangeRate").optional() //
                        .match("^Devisenkurs\\s*:\\s*(?<exchangeRate>\\d+,\\d+).*") //
                        .assign((t, v) -> {
                            type.getCurrentContext().put("exchangeRate", v.get("exchangeRate"));
                        })

                        .section("tax", "currency").optional() //
                        .match(".* \\*\\*Einbeh. Steuer *: *(?<tax>[\\d.]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {

                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));

                            // tax is given in EUR - if transaction currency is
                            // different, we need to calculate the value
                            if (!t.getPortfolioTransaction().getCurrencyCode().equals(tax.getCurrencyCode()))
                            {
                                BigDecimal exchangeRate = asExchangeRate(type.getCurrentContext().get("exchangeRate"));

                                Money txTax = Money.of(t.getPortfolioTransaction().getCurrencyCode(),
                                                BigDecimal.valueOf(tax.getAmount()).multiply(exchangeRate)
                                                                .setScale(0, RoundingMode.HALF_UP).longValue());

                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, txTax));
                            }
                            else
                            {
                                t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, tax));
                            }
                        })

                        .section("taxreturn", "currency").optional()
                        .match(".* \\*\\*Einbeh. Steuer *: *(?<taxreturn>-[\\d.]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> t.setAmount(
                                        t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxreturn"))))

                        .wrap(BuySellEntryItem::new));
        addTaxReturnBlock(type);
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierabrechnung Kauf Fonds/Zertifikate");
        this.addDocumentTyp(type);

        Block block = new Block(".*Auftragsdatum.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("time").optional().match(".*Ausführungszeit[\\s:]*(?<time>\\d+:\\d+).+") //
                        .assign((t, v) -> {
                            type.getCurrentContext().put("time", v.get("time"));
                        })

                        .section("date").match(".*[Schlusstag|Handelstag]\\s*(?<date>\\d+.\\d+.\\d{4}).*") //
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

                        .section("wkn", "isin", "name")
                        .match("Nr.[0-9A-Za-z]*/(\\d*) *Kauf *(?<name>.*) *\\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .match("^Ausgeführt[\\s:]*(?<shares>[\\.\\d]+(,\\d*)?) *St\\..*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        section -> section.attributes("amount", "currency") //
                                                        .match(".* Endbetrag[\\s:]*(?<currency>\\w{3}+) *(?<amount>[\\d.-]+,\\d+)") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        section -> section.attributes("amount", "currency") //
                                                        .match(".* Endbetrag[\\s:]*(?<amount>[\\d.-]+,\\d+)\\s(?<currency>\\w{3}+)") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))
                        .section("fee", "currency").optional() //
                        .match(".* Provision[\\s:]*(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* Provision[\\s:]*(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* Eigene Spesen[\\s:]*(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* Eigene Spesen[\\s:]*(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* \\*Fremde Spesen[\\s:]*(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* \\*Fremde Spesen[\\s:]*(?<fee>[\\d.-]+,\\d+) *(?<currency>\\w{3}+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addBuyfromSavingsplanTransaction()
    {
        DocumentType type = new DocumentType("Sammelabrechnung aus Sparplan");
        this.addDocumentTyp(type);

        Block block = new Block("Auftrags-Nr :.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("isin", "name").match("ISIN *: (?<isin>[^/]*)") //
                        .match("Bezeichnung *: (?<name>.*)").assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("date", "shares", "amount") //
                        .match("^Kauf *(\\d+.\\d+.\\d{4}) *(?<date>\\d+.\\d+.\\d{4}) *(?<shares>[\\.\\d]+(,\\d*)?) *([\\d.-]+,\\d+) *(\\w{3}+) *(?<amount>[\\d.-]+,\\d+) *(?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setShares(asShares(v.get("shares")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addDepositAndWithdrawalTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug Nr:", (context, lines) -> {
            Pattern pYear = Pattern.compile("Kontoauszug Nr:[ ]*\\d+/(\\d+).*");
            Pattern pCurrency = Pattern.compile("Kontow.hrung:[ ]+(\\w{3}+)");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pYear.matcher(line);
                if (m.matches())
                {
                    context.put("year", m.group(1));
                }
                m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group(1));
                }
            }
        });
        this.addDocumentTyp(type);

        // deposit, add value to account
        // 01.01. 01.01. xyz 123,45+
        Block block = new Block("\\d+\\.\\d+\\.[ ]+\\d+\\.\\d+\\.[ ]+.berweisung[ ]+[\\d.-]+,\\d+[+-]");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DEPOSIT);
                            return t;
                        })

                        .section("valuta", "amount", "sign")
                        .match("\\d+.\\d+.[ ]+(?<valuta>\\d+.\\d+.)[ ]+.berweisung[ ]+(?<amount>[\\d.-]+,\\d+)(?<sign>[+-])")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            String date = v.get("valuta");
                            if (date != null)
                            {
                                // create a long date from the year in the
                                // context
                                t.setDateTime(asDate(date + context.get("year")));
                            }
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            // check for withdrawals
                            String sign = v.get("sign");
                            if ("-".equals(sign))
                            {
                                // change type for withdrawals
                                t.setType(AccountTransaction.Type.REMOVAL);
                            }
                        })

                        .wrap(TransactionItem::new));

        // somedevelopment
        // Added "Lastschrift" as DEPOSIT option
        block = new Block("\\d+\\.\\d+\\.[ ]+\\d+\\.\\d+\\.[ ]+.Lastschrift[ ]+[\\d.-]+,\\d+[+-]");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.DEPOSIT);
            return t;
        })

                        .section("valuta", "amount", "sign")
                        .match("\\d+.\\d+.[ ]+(?<valuta>\\d+.\\d+.)[ ]+Lastschrift[ ]+(?<amount>[\\d.-]+,\\d+)(?<sign>[+-])")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            String date = v.get("valuta");
                            if (date != null)
                            {
                                // create a long date from the year in the
                                // context
                                t.setDateTime(asDate(date + context.get("year")));
                            }
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                        }).wrap(t -> new TransactionItem(t)));
        // somedevelopment

        // fees for foreign dividends, subtract value from account
        block = new Block("\\d+\\.\\d+\\.[ ]+\\d+\\.\\d+\\.[ ]+Geb.hr Kapitaltransaktion Ausland[ ]+[\\d.-]+,\\d+[-]");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.FEES);
                            return t;
                        })

                        .section("valuta", "amount", "isin")
                        .match("\\d+.\\d+.[ ]+(?<valuta>\\d+.\\d+.)[ ]+Geb.hr Kapitaltransaktion Ausland[ ]+(?<amount>[\\d.-]+,\\d+)[-]")
                        .match("\\s*(?<isin>\\w{12})").assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            Map<String, String> context = type.getCurrentContext();
                            String date = v.get("valuta");
                            if (date != null)
                            {
                                // create a long date from the year in the
                                // context
                                t.setDateTime(asDate(date + context.get("year")));
                            }
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            t.setNote("Gebühr Kapitaltransaktion Ausland");
                        }).wrap(TransactionItem::new));

        // account fees
        block = new Block(
                        "\\d+.\\d+.[ ]+\\d+.\\d+.[ ]+Depotgeb.hren[ ]+\\d{2}.\\d{2}.\\d{4}[ -]+\\d{2}.\\d{2}.\\d{4},[ ]+[\\d.-]+,\\d+[-]");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.FEES);
                            return t;
                        })

                        .section("valuta", "amount", "text")
                        .match("\\d+.\\d+.[ ]+(?<valuta>\\d+.\\d+.)[ ]+(?<text>Depotgeb.hren[ ]+\\d{2}.\\d{2}.\\d{4}[ -]+\\d{2}.\\d{2}.\\d{4}),[ ]+(?<amount>[\\d.-]+,\\d+)[-]")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            String date = v.get("valuta");
                            if (date != null)
                            {
                                // create a long date from the year in the
                                // context
                                t.setDateTime(asDate(date + context.get("year")));
                            }
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            t.setNote(v.get("text"));
                        }).wrap(TransactionItem::new));
    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type1 = new DocumentType(
                        "([^ ]Dividendengutschrift für inländische Wertpapiere)|(Dividendengutschrift[^ ])");
        DocumentType type2 = new DocumentType("Ertragsmitteilung");
        DocumentType type3 = new DocumentType("Zinsgutschrift");
        this.addDocumentTyp(type1);
        this.addDocumentTyp(type2);
        this.addDocumentTyp(type3);

        Block block = new Block("Ihre Depotnummer.*");
        type1.addBlock(block);
        type2.addBlock(block);
        type3.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DIVIDENDS);
                            return t;
                        })

                        .section("wkn", "isin", "name")
                        .match("Nr\\.(\\d*) * (?<name>.*) *\\((?<isin>[^/]*)/(?<wkn>[^)]*)\\).*") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .match("^St\\.[^:]+: *(?<shares>[\\.\\d]+(,\\d*)?).*")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .match(".* Endbetrag *: *(?<amount>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("tax", "currency").optional() //
                        .match("(.*)Einbeh. Steuer(.*):(\\s*)(?<tax>[\\d.]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .section("date") //
                        .match("Valuta * : *(?<date>\\d+.\\d+.\\d{4}+).*")
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // Devisenkurs : 1,173000 *Einbeh. Steuer : 12,99 EUR
                        // Quellenst.-satz : 15,00 % Gez. Quellensteuer : 18,28
                        // USD
                        .section("amountFx", "currencyFx", "exchangeRate").optional() //
                        .match(".*Devisenkurs *: *(?<exchangeRate>[.\\d]+,\\d+).*") //
                        .match(".*Gez. Quellenst.*: *(?<amountFx>[.\\d]+,\\d{2}) (?<currencyFx>\\w{3})") //
                        .assign((t, v) -> {

                            Money tax = Money.of(asCurrencyCode(v.get("currencyFx")), asAmount(v.get("amountFx")));

                            if (tax.getCurrencyCode().equals(t.getCurrencyCode()))
                            {
                                t.addUnit(new Unit(Unit.Type.TAX, tax));
                            }
                            else
                            {
                                BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                                RoundingMode.HALF_DOWN);

                                Money txTax = Money.of(t.getCurrencyCode(), BigDecimal.valueOf(tax.getAmount())
                                                .multiply(inverseRate).setScale(0, RoundingMode.HALF_UP).longValue());

                                // store tax value in both currencies, if
                                // security's currency
                                // is different to transaction currency
                                if (t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                                {
                                    t.addUnit(new Unit(Unit.Type.TAX, txTax));
                                }
                                else
                                {
                                    t.addUnit(new Unit(Unit.Type.TAX, txTax, tax, inverseRate));
                                }
                            }
                        })

                        .wrap(TransactionItem::new));
    }

    @SuppressWarnings("nls")
    private void addForeignDividendTransaction()
    {
        DocumentType type = new DocumentType("[^ ](Dividendengutschrift für ausländische Wertpapiere)",
                        (context, lines) -> {
                            Pattern pCurrencyFx = Pattern
                                            .compile(".* Bruttodividende *: *[.\\d]+,\\d{2} (?<currencyFx>\\w{3})");
                            Pattern pExchangeRate = Pattern
                                            .compile(".*Devisenkurs *: *(?<exchangeRate>[.\\d]+,\\d+).*");
                            // read the current context here
                            for (String line : lines)
                            {
                                Matcher m = pCurrencyFx.matcher(line);
                                if (m.matches())
                                {
                                    context.put("currencyFx", m.group(1));
                                }
                                m = pExchangeRate.matcher(line);
                                if (m.matches())
                                {
                                    context.put("exchangeRate", m.group(1));
                                }
                            }
                        });
        this.addDocumentTyp(type);

        Block block = new Block("Ihre Depotnummer.*");
        type.addBlock(block);

        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.DIVIDENDS);
                            return t;
                        })

                        // Nr.111111111 STARBUCKS CORP. (US8552441094/884437)
                        .section("wkn", "isin", "name") //
                        .match("Nr\\.(\\d*) * (?<name>.*) *\\((?<isin>[^/]*)/(?<wkn>[^)]*)\\).*") //
                        .assign((t, v) -> {

                            Map<String, String> context = type.getCurrentContext();

                            // the security must be in Fx units, otherwise,
                            // dividends and taxes cannot be in Fx units

                            v.put("currency", context.get("currencyFx"));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // St. : 105 Bruttodividende
                        .section("shares") //
                        .match("^St\\.[^:]+: *(?<shares>[\\.\\d]+(,\\d*)?).*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency") //
                        .match(".* Endbetrag *: *(?<amount>[\\d.-]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // Extag : 08.08.2017 Bruttodividende : 26,25 USD
                        .section("amountGrossFx", "currencyFx") //
                        .match(".* Bruttodividende *: *(?<amountGrossFx>[.\\d]+,\\d{2}) (?<currencyFx>\\w{3})") //
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            // set currency of transaction (should be in EUR)
                            String currencyCode = t.getCurrencyCode();

                            // create gross value unit only, if transaction
                            // currency is different to security currency
                            if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                            {

                                // get foreign currency (should be in Fx)
                                String currencyCodeFx = asCurrencyCode(v.get("currencyFx"));
                                // create a Unit only, if security and
                                // transaction currency are different
                                if (!currencyCode.equalsIgnoreCase(currencyCodeFx))
                                {
                                    // get exchange rate (in Fx/EUR) and
                                    // calculate
                                    // inverse exchange rate (in EUR/Fx)
                                    BigDecimal exchangeRate = asExchangeRate(context.get("exchangeRate"));
                                    BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                                    RoundingMode.HALF_DOWN);

                                    // get gross amount and calculate equivalent
                                    // in EUR
                                    Money mAmountGrossFx = Money.of(currencyCodeFx, asAmount(v.get("amountGrossFx")));
                                    BigDecimal amountGrossFxInEUR = BigDecimal.valueOf(mAmountGrossFx.getAmount())
                                                    .divide(exchangeRate, 10, RoundingMode.HALF_DOWN)
                                                    .setScale(0, RoundingMode.HALF_DOWN);
                                    Money mAmountGrossFxInEUR = Money.of(currencyCode, amountGrossFxInEUR.longValue());
                                    t.addUnit(new Unit(Unit.Type.GROSS_VALUE, mAmountGrossFxInEUR, mAmountGrossFx,
                                                    inverseRate));
                                }
                            }
                        })

                        // Quellenst.-satz : 30,00 % Gez. Quellenst. : 7,88 USD
                        .section("amountFx", "currencyFx").optional() //
                        .match(".*Gez. Quellenst.*: *(?<amountFx>[.\\d]+,\\d{2}) (?<currencyFx>\\w{3})") //
                        .assign((t, v) -> {

                            Money tax = Money.of(asCurrencyCode(v.get("currencyFx")), asAmount(v.get("amountFx")));

                            if (tax.getCurrencyCode().equals(t.getCurrencyCode()))
                            {
                                t.addUnit(new Unit(Unit.Type.TAX, tax));
                            }
                            else if (type.getCurrentContext().containsKey("exchangeRate"))
                            {
                                BigDecimal exchangeRate = asExchangeRate(type.getCurrentContext().get("exchangeRate"));
                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                                RoundingMode.HALF_DOWN);

                                Money txTax = Money.of(t.getCurrencyCode(), BigDecimal.valueOf(tax.getAmount())
                                                .multiply(inverseRate).setScale(0, RoundingMode.HALF_UP).longValue());

                                // store tax value in both currencies, if
                                // security's currency
                                // is different to transaction currency
                                if (t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                                {
                                    t.addUnit(new Unit(Unit.Type.TAX, txTax));
                                }
                                else
                                {
                                    t.addUnit(new Unit(Unit.Type.TAX, txTax, tax, inverseRate));
                                }
                            }
                        })

                        .section("amount", "currency").optional() //
                        .match("(.*)Einbeh. Steuer(.*):(\\s*)(?<amount>[\\d.]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> {

                            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                            // tax is given in EUR - if transaction currency is
                            // different, we need to calculate the value
                            if (!t.getCurrencyCode().equals(tax.getCurrencyCode()))
                            {
                                BigDecimal exchangeRate = asExchangeRate(type.getCurrentContext().get("exchangeRate"));

                                Money txTax = Money.of(t.getCurrencyCode(), BigDecimal.valueOf(tax.getAmount())
                                                .multiply(exchangeRate).setScale(0, RoundingMode.HALF_UP).longValue());

                                t.addUnit(new Unit(Unit.Type.TAX, txTax));
                            }
                            else
                            {
                                t.addUnit(new Unit(Unit.Type.TAX, tax));
                            }

                        })

                        .section("date") //
                        .match("Valuta * : *(?<date>\\d+.\\d+.\\d{4}+).*") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .wrap(TransactionItem::new));
    }

    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Wertpapierabrechnung Verkauf");
        this.addDocumentTyp(type);

        Block block = new Block(".*Auftragsdatum.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("date", "time").match(".*(Schlusstag|Handelstag) *(?<date>\\d+.\\d+.\\d{4}).*") //
                        .match(".*Ausführungszeit *(?<time>\\d+:\\d+).*") //
                        .assign((t, v) -> {
                            if (v.get("time") != null)
                                t.setDate(asDate(v.get("date"), v.get("time")));
                            else
                                t.setDate(asDate(v.get("date")));
                        })

                        .section("wkn", "isin", "name")
                        .match("Nr.(\\d*)/(\\d*) *Verkauf *(?<name>.*) *\\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares", "notation")
                        .match("^Ausgeführt[ :]*(?<shares>[\\.\\d]+(,\\d*)?) *(?<notation>St\\.|\\w{3}+).*") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("St."))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .oneOf( //
                                        section -> section.attributes("amount", "currency") //
                                                        .match(".* Endbetrag *(?<currency>\\w{3}+) *(?<amount>[\\d.-]+,\\d+)") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        section -> section.attributes("amount", "currency") //
                                                        .match(".* Endbetrag[ :]*(?<amount>[\\d.-]+,\\d+)\\s(?<currency>\\w{3}+)") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .section("fee", "currency").optional() //
                        .match(".* Provision[\\s:]*(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* Provision[\\s:]*(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* Eigene Spesen[\\s:]*(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* Eigene Spesen[\\s:]*(?<fee>[\\d.-]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* \\*Fremde Spesen[\\s:]*(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* \\*Fremde Spesen[\\s:]*(?<fee>[\\d.-]+,\\d+) *(?<currency>\\w{3}+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        // Gewinn/Verlust: 1.112,18 EUR **Einbeh. KESt : 305,85
                        // EUR
                        .section("tax", "currency").optional()
                        .match(".* \\*\\*Einbeh. KESt[\\s:]*(?<tax>[\\d.]+,\\d+) *(?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                        })

                        // Tax Refund (negative amount)
                        // Gewinn/Verlust: -68,61 EUR **Einbeh. KESt : -18,87
                        // EUR
                        .section("taxRefund", "currency").optional()
                        .match(".* \\*\\*Einbeh. KESt[\\s:]*(?<taxRefund>-[\\d.]+,\\d+) *(?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.setAmount(t.getPortfolioTransaction().getAmount() - asAmount(v.get("taxRefund")));
                        })

                        .wrap(BuySellEntryItem::new));

        addTaxRefundForSell(type);
    }

    // example is from 2016-12-01
    @SuppressWarnings("nls")
    private void addTransferInOutTransaction()
    {
        DocumentType type = new DocumentType("Gutschrifts-/Belastungsanzeige");
        this.addDocumentTyp(type);

        Block block = new Block("Depoteingang .*");
        type.addBlock(block);
        block.set(new Transaction<PortfolioTransaction>()

                        .subject(() -> {
                            PortfolioTransaction entry = new PortfolioTransaction();
                            entry.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            return entry;
                        })

                        // Datum : 16.03.2015
                        .section("date").match("Datum(\\s*):(\\s+)(?<date>\\d+.\\d+.\\d{4})") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // Depoteingang DEKAFONDS CF (DE0008474503)
                        .section("isin", "name").match("Depoteingang *(?<name>.*) *\\((?<isin>[^/]*)\\)") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // Stk./Nominale : 0,432000 Stk Einbeh. Steuer* : 0,00
                        // EUR
                        .section("shares", "notation")
                        .match("^Stk\\.\\/Nominale(\\s*):(\\s+)(?<shares>[\\.\\d]+(,\\d*)?) *(?<notation>St\\.|\\w{3}+)(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("Stk"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares(asShares(v.get("shares")) / 100);
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        // Kurs : 115,740741 EUR Devisenkurs : 1,000000
                        .section("rate", "currency")
                        .match("^Kurs(\\s*):(\\s+)(?<rate>[\\d.]+,\\d+)(\\s+)(?<currency>\\w{3}+)(\\s+)(.*)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("rate")) * t.getShares() / Values.Share.factor());
                        })

                        // Stk./Nominale : 0,052000 Stk Einbeh. Steuer* : 0,00
                        // EUR
                        .section("tax", "currency").optional() //
                        .match("(.*)Einbeh. Steuer(.*):(\\s*)(?<tax>[\\d.]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .wrap(TransactionItem::new));
    }

    @SuppressWarnings("nls")
    private void addTransferOutTransaction()
    {
        DocumentType type = new DocumentType("Depotausgang");
        this.addDocumentTyp(type);

        Block block = new Block("Depotausgang(.*)");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("date").match("Fälligkeitstag(\\s*):(\\s+)(?<date>\\d+.\\d+.\\d{4})(.*)") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("isin", "name").match("Depotausgang *(?<name>.*) *\\((?<isin>[^/]*)\\)") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares", "notation")
                        .match("^Stk\\.\\/Nominale(\\s*):(\\s+)(?<shares>[\\.\\d]+(,\\d*)?) *(?<notation>St\\.|\\w{3}+)(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("Stk"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("amount", "currency")
                        .match("(.*)Geldgegenwert\\*\\*(.*)(\\s*):(\\s*)(?<amount>[\\d.]+,\\d+)(\\s+)(?<currency>\\w{3}+)(.*)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section("fee", "currency").optional()
                        //
                        .match(".* Provision *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        //
                        .match(".* Eigene Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        //
                        .match(".* \\*Fremde Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("tax", "currency").optional() //
                        .match("(.*)Einbeh. Steuer(.*):(\\s*)(?<tax>[\\d.]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.TAX,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("tax"))))))

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addRemoveTransaction()
    {
        DocumentType type = new DocumentType("Bestandsausbuchung");
        this.addDocumentTyp(type);

        Block block = new Block("Bestandsausbuchung(.*)");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .section("date").match("Fälligkeitstag(\\s*):(\\s+)(?<date>\\d+.\\d+.\\d{4})(.*)") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("isin", "name").match("Bestandsausbuchung *(?<name>.*) *\\((?<isin>[^/]*)\\)") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares", "notation")
                        // Stk./Nominale**: 2.000,000000 Stk Einbeh. Steuer*:
                        // 0,00 EUR
                        .match("^Stk\\.\\/Nominale(.*):(\\s+)(?<shares>[\\.\\d]+(,\\d*)?)(\\s*)(?<notation>\\w{3}+)(.*)[Einbeh]+(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("Stk"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("amount", "currency").optional()
                        .match("(.*)Geldgegenwert\\*\\*(.*)(\\s*):(\\s*)(?<amount>[\\d.]+,\\d+)(\\s+)(?<currency>\\w{3}+)(.*)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section().optional() //
                        .match("(.*)Bestand haben wir wertlos ausgebucht(.*)").assign((t, v) -> {
                            t.setCurrencyCode(t.getAccountTransaction().getSecurity().getCurrencyCode());
                            t.setAmount(0L);
                            t.getPortfolioTransaction().setType(PortfolioTransaction.Type.TRANSFER_OUT);
                            t.setType(PortfolioTransaction.Type.TRANSFER_OUT);
                        })

                        .section("fee", "currency").optional() //
                        .match(".* Provision *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* Eigene Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional() //
                        .match(".* \\*Fremde Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("tax", "currency").optional() //
                        .match("(.*)Einbeh. Steuer(.*):(\\s*)(?<tax>[\\d.]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.TAX,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("tax"))))))

                        .wrap(BuySellEntryItem::new));
    }

    // since ~2015
    @SuppressWarnings("nls")
    private void addRemoveNewFormatTransaction()
    {
        DocumentType type = new DocumentType("Gutschrifts- / Belastungsanzeige");
        this.addDocumentTyp(type);

        Block block = new Block("Kundennummer(.*)");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        // WKN ISIN Wertpapierbezeichnung Anzahl
                        // SG0WRD DE000SG0WRD3 SG EFF. TURBOL ZS 83,00
                        // Sehr geehrter
                        .section("wkn", "isin", "name", "shares")
                        // .match("(?s)WKN(\\s+)ISIN(\\s+)Wertpapierbezeichnung(\\s+)Anzahl(.{1})(?<wkn>\\w{6}+)(\\s+)(?<isin>\\w{12}+)(\\s+)(?<name>.*?)(\\s+)(?<shares>[\\.\\d]+(,\\d*)?)(.*)(Sehr
                        // geehrte.*)") //
                        .match("(?s)(?<wkn>\\w{6}+)(\\s+)(?<isin>\\w{12}+)(\\s+)(?<name>.*?)(\\s+)(?<shares>[\\.\\d]+(,\\d*)?)(.*)") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        }).section("date").match("Fälligkeitstag(\\s*):(\\s+)(?<date>\\d+.\\d+.\\d{4})(.*)") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("amount", "currency").optional()
                        .match("(.*)Geldgegenwert(\\*{1,3})(.*)(\\s*):(\\s*)(?<amount>[\\d.]+,\\d+)(\\s+)(?<currency>\\w{3}+)(.*)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .section().optional().match("(.*)Bestand haben wir wertlos ausgebucht(.*)").assign((t, v) -> {
                            t.setCurrencyCode(t.getAccountTransaction().getSecurity().getCurrencyCode());
                            t.setAmount(0L);
                            t.getPortfolioTransaction().setType(PortfolioTransaction.Type.TRANSFER_OUT);
                            t.setType(PortfolioTransaction.Type.TRANSFER_OUT);
                        })

                        .section("fee", "currency").optional()
                        //
                        .match(".* Provision *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        //
                        .match(".* Eigene Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("fee", "currency").optional()
                        //
                        .match(".* \\*Fremde Spesen *(?<currency>\\w{3}+) *(?<fee>[\\d.-]+,\\d+)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fee"))))))

                        .section("tax", "currency").optional() //
                        .match("(.*)Einbeh. Steuer(.*):(\\s*)(?<tax>[\\d.]+,\\d+) (?<currency>\\w{3}+)") //
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.TAX,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("tax"))))))

                        .wrap(BuySellEntryItem::new));
    }

    @SuppressWarnings("nls")
    private void addOverdraftinterestTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug Nr:", (context, lines) -> {
            Pattern pYear = Pattern.compile("Kontoauszug Nr:[ ]*\\d+/(\\d+).*");
            Pattern pCurrency = Pattern.compile("Kontow.hrung:[ ]+(\\w{3}+)");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pYear.matcher(line);
                if (m.matches())
                {
                    context.put("year", m.group(1));
                }
                m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group(1));
                }
            }
        });
        this.addDocumentTyp(type);

        Block block = new Block("\\d+\\.\\d+\\.[ ]+\\d+\\.\\d+\\.[ ]+Zinsabschluss[ ]+(.*)");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.INTEREST_CHARGE);
                            return t;
                        })

                        .section("valuta", "amount", "sign", "text")
                        .match("\\d+.\\d+.[ ]+(?<valuta>\\d+.\\d+.)[ ]+(?<text>Zinsabschluss[ ]+(\\d+.\\d+.\\d{4})(\\s+)-(\\s+)(\\d+.\\d+.\\d{4}))(\\s+)(?<amount>[\\d.-]+,\\d+)(?<sign>[+-])")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            String date = v.get("valuta");
                            if (date != null)
                            {
                                // create a long date from the year in the
                                // context
                                t.setDateTime(asDate(date + context.get("year")));
                            }
                            t.setNote(v.get("text"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            String sign = v.get("sign");
                            if ("+".equals(sign))
                            {
                                // change type for payed Taxes
                                t.setType(AccountTransaction.Type.INTEREST);
                            }

                        }).wrap(t -> {
                            if (t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));
    }

    @SuppressWarnings("nls")
    private void addTaxoptimisationTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug Nr:", (context, lines) -> {
            Pattern pYear = Pattern.compile("Kontoauszug Nr:[ ]*\\d+/(\\d+).*");
            Pattern pCurrency = Pattern.compile("Kontow.hrung:[ ]+(\\w{3}+)");
            // read the current context here
            for (String line : lines)
            {
                Matcher m = pYear.matcher(line);
                if (m.matches())
                {
                    context.put("year", m.group(1));
                }
                m = pCurrency.matcher(line);
                if (m.matches())
                {
                    context.put("currency", m.group(1));
                }
            }
        });
        this.addDocumentTyp(type);

        Block block = new Block("\\d+\\.\\d+\\.[ ]+\\d+\\.\\d+\\.[ ]+Steuertopfoptimierung[ ]+(.*)");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.TAX_REFUND);
                            return t;
                        })

                        .section("valuta", "amount", "sign")
                        .match("\\d+.\\d+.[ ]+(?<valuta>\\d+.\\d+.)[ ]+Steuertopfoptimierung[ ]+(\\d{4})(\\s+)(?<amount>[\\d.-]+,\\d+)(?<sign>[+-])")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();
                            String date = v.get("valuta");
                            if (date != null)
                            {
                                // create a long date from the year in the
                                // context
                                t.setDateTime(asDate(date + context.get("year")));
                            }
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            String sign = v.get("sign");
                            if ("-".equals(sign))
                            {
                                // change type for payed Taxes
                                t.setType(AccountTransaction.Type.TAXES);
                            }
                        }).wrap(t -> {
                            if (t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));
    }

    @SuppressWarnings("nls")
    private void addTaxReturnBlock(DocumentType type)
    {

        // optional: Steuererstattung
        Block block = new Block("Nr.(\\d*)/(\\d*) *Verkauf.*");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.TAX_REFUND);
                            return entry;
                        })

                        .section("taxreturn").optional()
                        .match(".* \\*\\*Einbeh. Steuer *: *(?<taxreturn>-[\\d.]+,\\d+) (?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("taxreturn")));
                        })

                        .section("wkn", "isin", "name")
                        .match("Nr.(\\d*)/(\\d*) *Verkauf *(?<name>.*) *\\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("date")
                        .match("^davon ausgef\\. *: ([.\\d]+,\\d*) St\\. *Schlusstag *: *(?<date>\\d+.\\d+.\\d{4}+), \\d+:\\d+.+")
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));
    }

    @SuppressWarnings("nls")
    private void addVorabpauschaleTransaction()
    {
        final DocumentType type = new DocumentType("Wertpapierabrechnung Vorabpauschale", (context, lines) -> {
            Pattern pDate = Pattern.compile("Buchungsdatum +(?<date>\\d+.\\d+.\\d{4}+) *");
            for (String line : lines)
            {
                Matcher m = pDate.matcher(line);
                if (m.matches())
                    context.put("buchungsdatum", m.group("date"));
            }
        });
        this.addDocumentTyp(type);

        Block block = new Block("Wertpapierabrechnung Vorabpauschale .*");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction t = new AccountTransaction();
                            t.setType(AccountTransaction.Type.TAXES);
                            return t;
                        })

                        .section("wkn", "isin", "name") //
                        .match("^Depotinhaber: .*") //
                        .match("^(?<name>.*) +(?<isin>[^ ]+)/(?<wkn>[^ ]+) *$")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("amount", "currency")
                        .match(".* Einbeh. Steuer * (?<amount>[\\d.,]+) (?<currency>\\w{3}+) *") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode("currency"));
                        })

                        .section("date") //
                        .match("Gesamtbestand .*zum +(?<date>\\d+.\\d+.\\d{4}+) *").assign((t, v) -> {
                            // prefer "buchungsdatum" over "date" if available
                            String buchungsdatum = type.getCurrentContext().get("buchungsdatum");
                            t.setDateTime(asDate(buchungsdatum != null ? buchungsdatum : v.get("date")));
                        })

                        .section("shares") //
                        .match("Gesamtbestand * (?<shares>[\\d,.]+) St.*") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .wrap(t -> {
                            if (t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));
    }

    @SuppressWarnings("nls")
    private void addTaxRefundForSell(DocumentType type)
    {
        Block block = new Block(".*(Handelstag) *(?<date>\\d+.\\d+.\\d{4}).*");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction entry = new AccountTransaction();
                            entry.setType(AccountTransaction.Type.TAX_REFUND);
                            return entry;
                        })

                        // Nr.170997333/1 Verkauf DWS TOP DIVIDENDE LD
                        // (DE0009848119/984811)
                        .section("wkn", "isin", "name")
                        .match("Nr.(\\d*)/(\\d*) *Verkauf *(?<name>.*) *\\((?<isin>[^/]*)/(?<wkn>[^)]*)\\)")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // XXXXXXX XXXXXXX Handelstag 10.11.2020am
                        // XXXXXXX XXXXXX XXXXXXXXXXX Ausführungszeit 17:55 Uhr
                        .section("date", "time").match(".*(Schlusstag|Handelstag) *(?<date>\\d+.\\d+.\\d{4}).*")
                        .match(".*Ausführungszeit *(?<time>\\d+:\\d+).*").assign((t, v) -> {
                            if (v.get("time") != null)
                                t.setDateTime(asDate(v.get("date"), v.get("time")));
                            else
                                t.setDateTime(asDate(v.get("date")));
                        })

                        // Tax Refund (negative amount)
                        // Gewinn/Verlust: -68,61 EUR **Einbeh. KESt : -18,87
                        // EUR
                        .section("taxRefund", "currency").optional()
                        .match(".* \\*\\*Einbeh. KESt[\\s:]*(?<taxRefund>-[\\d.]+,\\d+) *(?<currency>\\w{3}+)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("taxRefund")));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));
    }

    @Override
    public String getLabel()
    {
        return "FinTech Group Bank AG / flatex / Whitebox"; //$NON-NLS-1$
    }
}
