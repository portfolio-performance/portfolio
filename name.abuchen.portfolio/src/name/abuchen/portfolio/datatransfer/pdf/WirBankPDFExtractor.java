package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class WirBankPDFExtractor extends AbstractPDFExtractor
{
    public WirBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("WIR Bank");

        addDepositTransaction();
        addBuySellTransaction();
        addInterestTransaction();
        addFeeTransaction();
        addDividendTransaction();
    }

    @Override
    public String getLabel()
    {
        return "WIR Bank Genossenschaft";
    }

    private void addDepositTransaction()
    {
        DocumentType type = new DocumentType("(Einzahlung|Deposit) 3a");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Einzahlung|Deposit) 3a$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Einzahlung 3a
                        // Gutschrift: Valuta 15.01.2019 CHF 2'150.00
                        // @formatter:on
                        .section("date", "amount", "currency") //
                        .find("(Einzahlung|Deposit) 3a") //
                        .match("^(Gutschrift: Valuta|Credit: Value date) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Zahlungseingang von: Einzahlung ABCD
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(Zahlungseingang von|Incoming payment from): (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(B.rsenabrechnung|Exchange Settlement) \\- (Kauf|Verkauf|Buy|Sell)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(B.rsenabrechnung|Exchange Settlement) \\- (Kauf|Verkauf|Buy|Sell).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(B.rsenabrechnung|Exchange Settlement) \\- (?<type>(Kauf|Verkauf|Buy|Sell)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")) || "Sell".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Order: Kauf
                        // 1.369 Ant iShares Core S&P500
                        // ISIN: IE00B5BMR087
                        // Kurs: USD 262.51
                        // @formatter:on
                        .section("isin", "name", "currency") //
                        .find("Order: (Kauf|Verkauf|Buy|Sell)") //
                        .match("^[\\.,\\d]+ (Ant|Qty|Anteile|units) (?<name>.*)$") //
                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^(Kurs|Price): (?<currency>[\\w]{3}) .*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 1.369 Ant iShares Core S&P500
                        // 0.027 units Swisscanto Pacific ex Japan
                        // @formatter:on
                        .section("shares") //
                        .find("Order: (Kauf|Verkauf|Buy|Sell)") //
                        .match("^(?<shares>[\\.,\\d]+) (Ant|Qty|Anteile|units) (?<name>.*)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Verrechneter Betrag: Valuta 05.07.2018 CHF 360.43
                        // @formatter:on
                        .section("date") //
                        .match("^(Verrechneter Betrag: Valuta|Charged amount: Value date) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Verrechneter Betrag: Valuta 05.07.2018 CHF 360.43
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^(Verrechneter Betrag: Valuta|Charged amount: Value date) .* (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Betrag USD 359.27
                        // Umrechnungskurs CHF/USD 1.00195 CHF 359.98
                        // @formatter:on
                        .section("fxGross", "termCurrency", "baseCurrency", "exchangeRate", "gross").optional() //
                        .match("^(Betrag|Amount) [\\w]{3} (?<fxGross>[\\.,'\\d]+)$") //
                        .match("^(Umrechnungskurs|Exchange rate) (?<termCurrency>[\\w]{3})\\/(?<baseCurrency>[\\w]{3}) (?<exchangeRate>[\\.,'\\d]+) [\\w]{3} (?<gross>[\\.,'\\d]+)$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                            Money fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addInterestTransaction()
    {
        DocumentType type = new DocumentType("(Zins|Interest)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Zins|Interest)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Zins
                        // Am 31.03.2019 haben wir Ihrem Konto gutgeschrieben:
                        // Zinsgutschrift: CHF 0.04
                        // @formatter:on
                        .section("date", "amount", "currency") //
                        .find("(Zins|Interest)") //
                        .match("^(Am|On) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (haben wir (Ihrem Konto|Ihnen) gutgeschrieben|we have credited your account):$") //
                        .match("^(Zinsgutschrift|Interest credit): (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Zinssatz: 0.11%
                        // Zinsperiode: März
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^(?<note1>(Zinssatz|Interest rate): [\\.,\\d]+%)$") //
                        .match("^(?<note2>(Zinsperiode|Interest period): .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + " | " + trim(v.get("note2"))))

                        .wrap(TransactionItem::new);
    }

    private void addFeeTransaction()
    {
        DocumentType type = new DocumentType("(Belastung|Commission)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Belastung|Commission)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Belastung
                        // Verrechneter Betrag: Valuta 31.01.2019 CHF -1.11
                        // @formatter:on
                        .section("date", "amount", "currency") //
                        .find("(Belastung|Commission)") //
                        .match("^(Verrechneter Betrag: Valuta|Charged amount: Value date) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<currency>[\\w]{3}) (\\-)?(?<amount>[\\.,'\\d]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Effektive VIAC Verwaltungsgebühr: 0.123% p.a. CHF -1.11
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(Effektive|Effective) (?<note>(VIAC Verwaltungsgeb.hr|VIAC administration fee): [\\.,\\d]+%) .*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            return item;
                        });
    }

    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("(Dividendenaussch.ttung" //
                        + "|Dividend Payment" //
                        + "|R.ckerstattung Quellensteuer" //
                        + "|Refund withholding tax)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Dividendenaussch.ttung" //
                        + "|Dividend Payment" //
                        + "|Cancelation Dividend Payment" //
                        + "|R.ckerstattung Quellensteuer" //
                        + "|Refund withholding tax)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .section("type").optional() //
                        .match("^(Dividendenart|Type of dividend): (?<type>(R.ckerstattung Quellensteuer|Refund withholding tax))$") //
                        .assign((t, v) -> {
                            if ("Rückerstattung Quellensteuer".equals(v.get("type")) || "Refund withholding tax".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.TAX_REFUND);
                        })

                        // @formatter:off
                        // Cancelation Dividend Payment
                        // @formatter:on
                        .section("type").optional() //
                        .match("^(?<type>Cancelation Dividend Payment)$") //
                        .assign((t, v) -> v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported))

                        // @formatter:off
                        // 47.817 Ant UBS ETF MSCI USA SRI
                        // ISIN: LU0629460089
                        // Ausschüttung: USD 0.72
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^[\\.,\\d]+ (Ant|Qty|Anteile|units) (?<name>.*)$") //
                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^(Aussch.ttung|Dividend payment): (?<currency>[\\w]{3}) .*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 47.817 Ant UBS ETF MSCI USA SRI
                        // @formatter:on
                        .section("shares") //
                        .match("^(?<shares>[\\.,\\d]+) (Ant|Qty|Anteile|units) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Gutgeschriebener Betrag: Valuta 04.02.2022 CHF 31.44
                        // @formatter:on
                        .section("date") //
                        .match("^(Gutgeschriebener Betrag: Valuta|Amount (credited|debited): Value date) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Gutgeschriebener Betrag: Valuta 04.02.2022 CHF 31.44
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^(Gutgeschriebener Betrag: Valuta|Amount (credited|debited): Value date) .* (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Betrag CAD 0.20
                                        // Umrechnungskurs CHF/CAD 0.7466 CHF 0.15
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "termCurrency", "baseCurrency", "exchangeRate", "gross") //
                                                        .match("^(Betrag|Amount) [\\w]{3} (?<fxGross>[\\.,'\\d]+)$") //
                                                        .match("^(Umrechnungskurs|Exchange rate) (?<termCurrency>[\\w]{3})\\/(?<baseCurrency>[\\w]{3}) (?<exchangeRate>[\\.,'\\d]+) [\\w]{3} (?<gross>[\\.,'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Betrag USD 34.26
                                        // Umrechnungskurs CHF/USD
                                        // 0.91759 CHF 31.44
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "termCurrency", "baseCurrency", "exchangeRate", "gross") //
                                                        .match("^(Betrag|Amount) [\\w]{3} (?<fxGross>[\\.,'\\d]+)$") //
                                                        .match("^(Umrechnungskurs|Exchange rate) (?<termCurrency>[\\w]{3})\\/(?<baseCurrency>[\\w]{3}).*$") //
                                                        .match("^(?<exchangeRate>[\\.,'\\d]+) [\\w]{3} (?<gross>[\\.,'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // Dividendenart: Ordentliche Dividende
                        // Type of dividend: Ordinary dividend
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(Dividendenart|Type of dividend): (?<note>.*)") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //
                        // @formatter:off
                        // Stempelsteuer CHF 1.68
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^(Stempelsteuer|Stamp duty) (?<currency>[\\w]{3}) (?<tax>[\\.,'\\d]+)$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "de", "CH");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "de", "CH");
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "de", "CH");
    }
}
