package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

@SuppressWarnings("nls")
public class WirBankPDFExtractor extends AbstractPDFExtractor
{
    public WirBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("WIR Bank"); //$NON-NLS-1$

        addDepositTransaction();
        addBuySellTransaction();
        addInterestTransaction();
        addFeeTransaction();
        addDividendsTransaction();
        addTaxRefundTransaction();
    }

    @Override
    public String getLabel()
    {
        return "WIR Bank Genossenschaft"; //$NON-NLS-1$
    }

    private void addDepositTransaction()
    {
        DocumentType type = new DocumentType("(Einzahlung|Deposit) 3a");
        this.addDocumentTyp(type);

        Block block = new Block("^(Einzahlung|Deposit) 3a$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DEPOSIT);
            return transaction;
        })

                        .section("date", "amount", "currency")
                        .find("(Einzahlung|Deposit) 3a")
                        .match("^(Gutschrift: Valuta|Credit: Value date) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("note").optional()
                        .match("^(Zahlungseingang von|Incoming payment from): (?<note>.*)$")
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new));
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(B.rsenabrechnung|Exchange Settlement) \\- (Kauf|Verkauf|Buy|Sell)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(B.rsenabrechnung|Exchange Settlement) \\- (Kauf|Verkauf|Buy|Sell)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(B.rsenabrechnung|Exchange Settlement) \\- (?<type>(Kauf|Verkauf|Buy|Sell))$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf") || v.get("type").equals("Sell"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                .section("isin", "name", "currency", "shares")
                .find("Order: (Kauf|Verkauf|Buy|Sell)")
                .match("^(?<shares>[\\.,\\d]+) (Ant|Qty) (?<name>.*)$")
                .match("^ISIN: (?<isin>[\\w]{12})$")
                .match("^(Kurs|Price): (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                .section("date", "amount", "currency")
                .match("^(Verrechneter Betrag: Valuta|Charged amount: Value date) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d]+)$")
                .assign((t, v) -> {
                    t.setDate(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .section("forex", "forexCurrency", "amount", "currency", "exchangeRate").optional()
                .match("^(Betrag|Amount) (?<forexCurrency>[\\w]{3}) (?<forex>[\\.,'\\d]+)$")
                .match("^(Umrechnungskurs|Exchange rate) [\\w]{3}\\/[\\w]{3} (?<exchangeRate>[\\.,'\\d]+) (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d]+)$")
                .assign((t, v) -> {
                    Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forex")));
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                    // only add gross value with forex if the security
                    // is actually denoted in the foreign currency
                    // (often users actually have the quotes in their
                    // home country currency)
                    if (forex.getCurrencyCode()
                                    .equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                    {
                        t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate));
                    }
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addInterestTransaction()
    {
        DocumentType type = new DocumentType("(Zins|Interest)");
        this.addDocumentTyp(type);

        Block block = new Block("^(Zins|Interest)$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.INTEREST);
            return transaction;
        })

                        .section("date", "amount", "currency")
                        .find("(Zins|Interest)")
                        .match("^(Am|On) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (haben wir (Ihrem Konto|Ihnen) gutgeschrieben|we have credited your account):$")
                        .match("^(Zinsgutschrift|Interest credit): (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("note1", "note2").optional()
                        .match("^(?<note1>(Zinssatz|Interest rate): [\\.,\\d]+%)$")
                        .match("^(?<note2>(Zinsperiode|Interest period): .*)$")
                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + " | " + trim(v.get("note2"))))

                        .wrap(TransactionItem::new));
    }

    private void addFeeTransaction()
    {
        DocumentType type = new DocumentType("(Belastung|Commission)");
        this.addDocumentTyp(type);

        Block block = new Block("^(Belastung|Commission)$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.FEES);
            return transaction;
        })

                        .section("date", "amount", "currency")
                        .find("(Belastung|Commission)")
                        .match("^(Verrechneter Betrag: Valuta|Charged amount: Value date) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<currency>[\\w]{3}) \\-(?<amount>[\\.,'\\d]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("note").optional()
                        .match("^(Effektive|Effective) (?<note>(VIAC Verwaltungsgeb.hr|VIAC administration fee): [\\.,\\d]+%) .*$")
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new));
    }

    private void addDividendsTransaction()
    {
        DocumentType type = new DocumentType("(Dividendenaussch.ttung|Dividend Payment)");
        this.addDocumentTyp(type);

        Block block = new Block("^(Dividendenart|Type of dividend): (Ordentliche Dividende|Ordinary dividend)$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                .section("shares", "name", "isin", "currency")
                .match("^(?<shares>[\\.,\\d]+) (Ant|Qty) (?<name>.*)$")
                .match("^ISIN: (?<isin>[\\w]{12})$")
                .match("^(Aussch.ttung|Dividend payment): (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                .section("date", "amount", "currency")
                .match("^(Gutgeschriebener Betrag: Valuta|Amount credited: Value date) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d]+)$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .section("currency", "amount", "exchangeRate", "fxCurrency", "fxAmount").optional()
                .match("^(Betrag|Amount) (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d]+)$")
                .match("^(Umrechnungskurs|Exchange rate) [\\w]{3}\\/[\\w]{3} (?<exchangeRate>[\\.,'\\d]+) (?<fxCurrency>[\\w]{3}) (?<fxAmount>[\\.,'\\d]+)$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                    {
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                    }
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // check, if forex currency is transaction
                        // currency or not and swap amount, if necessary
                        Unit grossValue;
                        if (!asCurrencyCode(v.get("fxCurrency")).equals(t.getCurrencyCode()))
                        {
                            Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(v.get("fxAmount")));
                            Money amount = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        else
                        {
                            Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")), 
                                            asAmount(v.get("fxAmount")));
                            Money fxAmount = Money.of(asCurrencyCode(v.get("currency")), 
                                            asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        t.addUnit(grossValue);
                    }
                })

                .section("currency", "amount", "exchangeRate", "fxCurrency", "fxAmount").optional()
                .match("^(Betrag|Amount) (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d]+)$")
                .match("^(Umrechnungskurs|Exchange rate) [\\w]{3}\\/[\\w]{3} $")
                .match("^(?<exchangeRate>[\\.,'\\d]+) (?<fxCurrency>[\\w]{3}) (?<fxAmount>[\\.,'\\d]+)$")
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                    {
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                    }
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // check, if forex currency is transaction
                        // currency or not and swap amount, if necessary
                        Unit grossValue;
                        if (!asCurrencyCode(v.get("fxCurrency")).equals(t.getCurrencyCode()))
                        {
                            Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(v.get("fxAmount")));
                            Money amount = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        else
                        {
                            Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")), 
                                            asAmount(v.get("fxAmount")));
                            Money fxAmount = Money.of(asCurrencyCode(v.get("currency")), 
                                            asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        t.addUnit(grossValue);
                    }
                })

                .wrap(TransactionItem::new);

        block.set(pdfTransaction);
    }

    private void addTaxRefundTransaction()
    {
        DocumentType type = new DocumentType("(Dividendenaussch.ttung|Dividend Payment)");
        this.addDocumentTyp(type);

        Block block = new Block("(Dividendenart|Type of dividend): (R.ckerstattung Quellensteuer|Refund withholding tax)");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.TAX_REFUND);
            return transaction;
        })

                        .section("shares", "name", "isin", "currency")
                        .match("^(?<shares>[\\.,\\d]+) (Ant|Qty) (?<name>.*)$")
                        .match("^ISIN: (?<isin>[\\w]{12})$")
                        .match("^(Aussch.ttung|Dividend payment): (?<currency>[\\w]{3}) .*$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .section("date", "amount", "currency")
                        .match("^(Gutgeschriebener Betrag: Valuta|Amount credited: Value date) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                            if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                            {
                                t.setNote(t.getSecurity().getName());
                                t.setSecurity(null);
                                t.setShares(0L);
                            }
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                .section("currency", "tax").optional()
                .match("^(Stempelsteuer|Stamp duty) (?<currency>[\\w]{3}) (?<tax>[\\.,'\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return PDFExtractorUtils.convertToNumberLong(value, Values.Amount, "de", "CH"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected long asShares(String value)
    {
        return PDFExtractorUtils.convertToNumberLong(value, Values.Share, "de", "CH"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return PDFExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
