package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
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

@SuppressWarnings("nls")
public class TargobankPDFExtractor extends AbstractPDFExtractor
{
    /**
     * @formatter:off
     * Information:
     * Targobank AG always creates two documents per transaction.
     *
     * 1. Transaction, e.g. sale or dividend
     * 2. tax statement
     *
     * To offset the taxes due with the transaction, we use the ex-tag as a
     * transaction date, which we later replace again with the payment date in
     * postProcessing().
     *
     * The reason for this is that sometimes the transaction
     * date is different between the taxes document and the transaction.
     *
     * @Override public List<Item> postProcessing(List<Item> items)
     * @formatter:on
     */

    private static final String TO_BE_DELETED = "to_be_deleted";
    private static final String ATTRIBUTE_PAY_DATE = "pay_date";

    public TargobankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("TARGO");
        addBankIdentifier("Targobank");
        addBankIdentifier("TARGOBANK AG");

        addBuySellTransaction();
        addTaxTreatmentForBuySellTransaction();
        addDividendeTransaction();
        addTaxTreatmentForDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Targobank AG";
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Effektenabrechnung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Transaktionstyp (Kauf|Verkauf)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Transaktionstyp (?<type>(Kauf|Verkauf))$")
                .assign((t, v) -> {
                    if ("Verkauf".equals(v.get("type")))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // @formatter:off
                // Wertpapier FanCy shaRe. nAmE X0-X0
                // WKN / ISIN ABC123 / DE0000ABC123
                // Kurs 12,34 EUR
                // @formatter:on
                .section("name", "wkn", "isin", "currency")
                .match("^Wertpapier (?<name>.*)$")
                .match("^WKN \\/ ISIN (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .match("^(Kurs|Preis vom) .* (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Stück 987,654
                // @formatter:on
                .section("shares")
                .match("^St.ck (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                .oneOf(
                                // @formatter:off
                                // Schlusstag / Handelszeit 02.01.2020 / 13:01:00
                                // @formatter:on
                                section -> section
                                        .attributes("date", "time")
                                        .match("^Schlusstag \\/ Handelszeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\/ (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // @formatter:off
                                // Schlusstag 10.01.2020
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))
                        )

                // @formatter:off
                // Konto-Nr. 0101753165 1.008,91 EUR
                // @formatter:on
                .section("amount", "currency")
                .match("^Konto\\-Nr\\. .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addTaxTreatmentForBuySellTransaction()
    {
        DocumentType type = new DocumentType("Effektenabrechnung \\(Steuerbeilage\\) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.TAXES);
            return entry;
        });

        Block firstRelevantLine = new Block("^Transaktionstyp Verkauf$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // @formatter:off
                // Wertpapier FanCy shaRe. nAmE X0-X0
                // WKN / ISIN ABC123 / DE0000ABC123
                // Kurs 12,34 EUR
                // @formatter:on
                .section("name", "wkn", "isin", "currency")
                .match("^Wertpapier (?<name>.*)$")
                .match("^WKN \\/ ISIN (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .match("^(Kurs|Preis vom) .* (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Stück 987,654
                // @formatter:on
                .section("shares")
                .match("^St.ck (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                .oneOf(
                                // @formatter:off
                                // Schlusstag / Handelszeit 26.05.2020 / 20:32:00
                                // @formatter:on
                                section -> section
                                        .attributes("date", "time")
                                        .match("^Schlusstag \\/ Handelszeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\/ (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"), v.get("time"))))
                                ,
                                // @formatter:off
                                // Schlusstag 10.01.2020
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                        )

                // @formatter:off
                // Gesamtsumme Steuern 823,76 EUR
                // @formatter:on
                .section("amount", "currency")
                .match("^Gesamtsumme Steuern (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(TransactionItem::new);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Ertragsgutschrift|Dividendengutschrift) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}");
        this.addDocumentTyp(type);

        Block block = new Block("^(Ertragsgutschrift|Dividendengutschrift) .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // @formatter:off
                // Wertpapier Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN
                // WKN / ISIN A12CX1 / IE00BKX55T58
                // Ausschüttung pro Stück 0,293466 USD
                // @formatter:on
                .section("name", "wkn", "isin", "currency")
                .match("^Wertpapier (?<name>.*)$")
                .match("^WKN \\/ ISIN (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .match("^(Aussch.ttung|Dividende) pro St.ck [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Stück 81
                // @formatter:on
                .section("shares")
                .match("^St.ck (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:of
                // Temporarily set the ex-day as the transaction day
                // and will be corrected to the payDay in the postProcessing().
                // (See information on top)
                // @formatter:on

                // @formatter:off
                // Ex-Tag 11.06.2020
                // @formatter:on
                .section("date")
                .match("^Ex\\-Tag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                // Zahlbar 24.06.2020
                // @formatter:on
                .section("payDate")
                .match("^Zahlbar (?<payDate>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> {
                    t.getSecurity().getAttributes() //
                                    .put(new AttributeType(ATTRIBUTE_PAY_DATE), asDate(v.get("payDate")));
                })

                // @formatter:off
                // Konto-Nr. 1234567890 21,18 EUR
                // @formatter:on
                .section("amount", "currency")
                .match("^Konto\\-Nr\\. .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // @formatter:off
                // Bruttoertrag 23,77 USD
                // Devisenkurs zur Handelswährung USD/EUR 1,1223
                // Bruttoertrag in EUR 21,18 EUR
                // @formatter:on
                .section("fxGross", "fxCurrency", "termCurrency", "baseCurrency", "exchangeRate", "gross", "currency").optional()
                .match("^Bruttoertrag (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                .match("^Devisenkurs zur Handelsw.hrung (?<termCurrency>[\\w]{3})\\/(?<baseCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$")
                .match("^Bruttoertrag in [\\w]{3} (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addTaxTreatmentForDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Ertragsgutschrift|Dividendengutschrift) \\(Steuerbeilage\\) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}");
        this.addDocumentTyp(type);

        Block block = new Block("^(Ertragsgutschrift|Dividendengutschrift) .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.TAXES);
            return entry;
        });

        pdfTransaction
                // @formatter:off
                // Wertpapier Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN
                // WKN / ISIN A12CX1 / IE00BKX55T58
                // Ausschüttung pro Stück 0,293466 USD
                // @formatter:on
                .section("name", "wkn", "isin")
                .match("^Wertpapier (?<name>.*)$")
                .match("^WKN \\/ ISIN (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // @formatter:off
                // Stück 81
                // @formatter:on
                .section("shares")
                .match("^St.ck (?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:of
                // Temporarily set the ex-day as the transaction day
                // and will be corrected to the payDay in the postProcessing().
                // (See information on top)
                // @Formatter:on

                // @formatter:off
                // Ex-Tag 11.06.2020
                // @formatter:on
                .section("date")
                .match("^Ex\\-Tag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                // Belastung Ihres Kontos NUMMER mit Wertstellung zum 24. Juni 2020.
                // @formatter:on
                .section("payDate").optional()
                .match("^Belastung Ihres Kontos .* (?<payDate>[\\d]{2}\\. .* [\\d]{4})\\.$")
                .assign((t, v) -> {
                    t.getSecurity().getAttributes() //
                                    .put(new AttributeType(ATTRIBUTE_PAY_DATE), asDate(v.get("payDate")));
                })

                // @formatter:off
                // Gesamtsumme Steuern 5,59 EUR
                // @formatter:on
                .section("amount", "currency")
                .match("^Gesamtsumme Steuern (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                // @formatter:off
                // Anrechenbare ausländische Quellensteuer 3,67 EUR
                // @formatter:on
                .section("amount", "currency").optional()
                .match("^Anrechenbare ausl.ndische Quellensteuer (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    if (t.getMonetaryAmount().isZero())
                    {
                        t.setAmount(asAmount(v.get("amount")));
                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    }
                    else
                    {
                        Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                        t.setMonetaryAmount(t.getMonetaryAmount().add(tax));
                    }
                })

                .wrap((t, ctx) -> {
                    TransactionItem item = new TransactionItem(t);
                    if (t.getCurrencyCode() != null && t.getAmount() == 0)
                        item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);
                    return item;
                });

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // 15 % Ausländische Quellensteuer (US) 3,67 EUR
                // @formatter:on
                .section("creditableWithHoldingTax", "currency").optional()
                .match("^[\\d]+ % Ausl.ndische Quellensteuer.* (?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Provision 8,90 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Provision (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    public void postProcessing(List<Item> items)
    {
        // Filter transactions by sell transactions
        List<Item> sellTransactionList = items.stream() //
                        .filter(item -> !item.isFailure()) //
                        .filter(BuySellEntryItem.class::isInstance) //
                        .map(BuySellEntryItem.class::cast) //
                        .filter(i -> i.getSubject() instanceof BuySellEntry) //
                        .filter(i -> PortfolioTransaction.Type.SELL //
                                        .equals((((BuySellEntry) i.getSubject()).getPortfolioTransaction().getType()))) //
                        .collect(Collectors.toList());

        // Filter transactions by taxes transactions
        List<Item> taxTransactionList = items.stream() //
                        .filter(item -> !item.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .map(TransactionItem.class::cast) //
                        .filter(i -> i.getSubject() instanceof AccountTransaction) //
                        .filter(i -> AccountTransaction.Type.TAXES //
                                        .equals((((AccountTransaction) i.getSubject()).getType()))) //
                        .collect(Collectors.toList());

        // Group sell and tax transactions together and group by date and
        // security
        Map<LocalDateTime, Map<Security, List<Item>>> sellTaxTransactions = Stream
                        .concat(sellTransactionList.stream(), taxTransactionList.stream())
                        .collect(Collectors.groupingBy(Item::getDate, Collectors.groupingBy(Item::getSecurity)));

        // Group dividend and taxes transactions together and group by date and
        // security
        Map<LocalDateTime, Map<Security, List<Item>>> dividendTaxTransactions = items.stream() //
                        .filter(item -> !item.isFailure()) //
                        .filter(TransactionItem.class::isInstance) //
                        .map(TransactionItem.class::cast) //
                        .filter(i -> i.getSubject() instanceof AccountTransaction) //
                        .filter(i -> AccountTransaction.Type.DIVIDENDS //
                                        .equals(((AccountTransaction) i.getSubject()).getType()) || //
                                        AccountTransaction.Type.TAXES //
                                                        .equals(((AccountTransaction) i.getSubject()).getType())) //
                        .collect(Collectors.groupingBy(Item::getDate, Collectors.groupingBy(Item::getSecurity)));

        sellTaxTransactions.forEach((k, v) -> {
            v.forEach((security, transactions) -> {

                // @formatter:off
                // It is possible that several sell transactions exist on
                // the same day without one or with several taxes transactions.
                //
                // We simplify here only one sell transaction with one
                // related taxes transaction.
                // @formatter:on

                if (transactions.size() == 2)
                {
                    BuySellEntry sellTransaction = null;
                    AccountTransaction taxTransaction = null;

                    // Which transaction is the taxes and which the sell?
                    if (transactions.get(0).getSubject() instanceof BuySellEntry entry
                                    && transactions.get(1).getSubject() instanceof AccountTransaction tx)
                    {
                        sellTransaction = entry;
                        taxTransaction = tx;
                    }
                    else if (transactions.get(1).getSubject() instanceof BuySellEntry entry
                                    && transactions.get(0).getSubject() instanceof AccountTransaction tx)
                    { // NOSONAR
                        sellTransaction = entry;
                        taxTransaction = tx;
                    }

                    // Check if there is a sell transaction and a tax
                    // transaction
                    if (AccountTransaction.Type.TAXES.equals(taxTransaction.getType()) && PortfolioTransaction.Type.SELL
                                    .equals(sellTransaction.getPortfolioTransaction().getType()))
                    {
                        // Subtract the taxes from the tax transaction from the
                        // total amount
                        sellTransaction.setMonetaryAmount(sellTransaction.getPortfolioTransaction().getMonetaryAmount()
                                        .subtract(taxTransaction.getMonetaryAmount()));

                        // Add taxes as tax unit
                        sellTransaction.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.TAX, taxTransaction.getMonetaryAmount()));

                        // Combine at sources file
                        sellTransaction.setSource(concatenate(sellTransaction.getSource(), taxTransaction.getSource(), "; "));

                        // Combine at notes
                        sellTransaction.setNote(concatenate(sellTransaction.getNote(), taxTransaction.getNote(), " | "));

                        // Set note that the tax transaction will be deleted
                        taxTransaction.setNote(TO_BE_DELETED);
                    }
                    else
                    {
                        // do nothing because no tax transaction is present
                    }
                }
            });
        });

        dividendTaxTransactions.forEach((k, v) -> {
            v.forEach((security, transactions) -> {
                AccountTransaction dividendTransaction = (AccountTransaction) transactions.get(0).getSubject();

                // @formatter:off
                // It is possible that several dividend transactions exist on
                // the same day without one or with several taxes transactions.
                //
                // We simplify here only one dividend transaction with one
                // related taxes transaction.
                // @formatter:on

                if (transactions.size() == 2)
                {
                    AccountTransaction taxTransaction = (AccountTransaction) transactions.get(1).getSubject();

                    // Which transaction is the taxes and which the dividend?
                    if (!AccountTransaction.Type.TAXES.equals(taxTransaction.getType()))
                    {
                        dividendTransaction = (AccountTransaction) transactions.get(1).getSubject();
                        taxTransaction = (AccountTransaction) transactions.get(0).getSubject();
                    }

                    // Check if there is a dividend transaction and a tax
                    // transaction
                    if (AccountTransaction.Type.TAXES.equals(taxTransaction.getType())
                                    && AccountTransaction.Type.DIVIDENDS.equals(dividendTransaction.getType()))
                    {
                        // @formatter:off
                        // Sometimes taxes (e.g., creditable withholding taxes)
                        // are included in the dividend document.
                        //
                        // Should that be the case, we subtract them from the taxes document.
                        // @formatter:on
                        if (dividendTransaction.getUnit(Unit.Type.TAX).isPresent())
                        {
                            Money tax = Money.of(dividendTransaction.getUnitSum(Unit.Type.TAX).getCurrencyCode(),
                                            dividendTransaction.getUnitSum(Unit.Type.TAX).getAmount());

                            if (tax.isGreaterOrEqualTo(taxTransaction.getMonetaryAmount()))
                                taxTransaction.setMonetaryAmount(taxTransaction.getMonetaryAmount().subtract(tax));
                        }

                        // Subtract the taxes from the tax transaction from the
                        // total amount
                        dividendTransaction.setMonetaryAmount(dividendTransaction.getMonetaryAmount()
                                        .subtract(taxTransaction.getMonetaryAmount()));

                        // Add taxes as tax unit
                        dividendTransaction.addUnit(new Unit(Unit.Type.TAX, taxTransaction.getMonetaryAmount()));

                        // Combine at sources file
                        dividendTransaction.setSource(concatenate(dividendTransaction.getSource(), taxTransaction.getSource(), "; "));

                        // Combine at notes
                        dividendTransaction.setNote(concatenate(dividendTransaction.getNote(), taxTransaction.getNote(), " | "));

                        // Set note that the tax transaction will be deleted
                        taxTransaction.setNote(TO_BE_DELETED);
                    }
                    else
                    {
                        // do nothing because no tax transaction is present
                    }
                }

                // Set the correct transaction date
                if (dividendTransaction.getSecurity().getAttributes()
                                .get(new AttributeType(ATTRIBUTE_PAY_DATE)) != null)
                {
                    dividendTransaction.setDateTime((LocalDateTime) dividendTransaction.getSecurity().getAttributes()
                                    .get(new AttributeType(ATTRIBUTE_PAY_DATE)));
                }
            });
        });

        // iterate list and remove items that are marked TO_BE_DELETED
        Iterator<Item> iter = items.iterator();
        while (iter.hasNext())
        {
            Object o = iter.next().getSubject();
            if (o instanceof AccountTransaction a)
            {
                if (TO_BE_DELETED.equals(a.getNote()))
                    iter.remove();
            }
        }
    }
}
