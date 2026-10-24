package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

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
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote VIAC (Terzo Vorsorgestiftung / Freizügigkeitsstiftung der WIR Bank) provides separate documents
 *           for the ordinary dividend and for the refund of the Swiss withholding tax of the same distribution.
 *
 *           The ordinary dividend contains the exchange rate if the security currency differs.
 *           The refund of the withholding tax is always booked in CHF and does not contain an exchange rate.
 *
 *           In postProcessing, the tax refund receives the gross value in the security currency
 *           from the matching dividend (same security, value date and shares) of the same import.
 *           If no matching dividend is found and the security currency differs, the tax refund is marked as failure.
 *
 *           The refund document always shows CHF. If the security was created by a refund document,
 *           the currency of the security is corrected in postProcessing from the dividend or buy/sell
 *           documents of the same import, so that the order of the documents does not matter.
 * @formatter:on
 */
@SuppressWarnings("nls")
public class WirBankPDFExtractor extends AbstractPDFExtractor
{
    private static final String ATTRIBUTE_SECURITY_CURRENCY = "security_currency";
    private static final String ATTRIBUTE_GROSS = "gross";
    private static final String ATTRIBUTE_FX_GROSS = "fx_gross";
    private static final String ATTRIBUTE_EXCHANGE_RATE = "exchange_rate";

    public WirBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("WIR Bank");
        addBankIdentifier("Banque WIR");
        addBankIdentifier("VIAC Invest AG");

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
        final var type = new DocumentType("(Einzahlung|Deposit|Versement|Avis de versement) (3a|VIAC Invest)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Einzahlung|Deposit|Versement|Avis de versement) (3a|VIAC Invest)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        // @formatter:off
                        // Einzahlung 3a
                        // Gutschrift: Valuta 15.01.2019 CHF 2'150.00
                        // @formatter:on
                        .section("date", "amount", "currency") //
                        .find("(Einzahlung|Deposit|Versement|Avis de versement) (3a|VIAC Invest)") //
                        .match("^(Gutschrift: Valuta|Credit: Value date|Montant cr.dit.: Valeur) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Zahlungseingang von: Einzahlung ABCD
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(Zahlungseingang von|Incoming payment from|Mode de versement|Deposit by): (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("(B.rsenabrechnung|Exchange Settlement|Op.ration de bourse) " //
                        + "\\- (Kauf|Zeichnung|Verkauf|R.cknahme|Buy|Subscription|Sell|Redemption|Achat|Vente)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(B.rsenabrechnung|Exchange Settlement|Op.ration de bourse) " //
                        + "\\- (Kauf|Zeichnung|Verkauf|R.cknahme|Buy|Subscription|Sell|Redemption|Achat|Vente).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(B.rsenabrechnung|Exchange Settlement|Op.ration de bourse) " //
                                        + "\\- (?<type>(Kauf|Zeichnung|Verkauf|R.cknahme|Buy|Subscription|Sell|Redemption|Achat|Vente)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type"))  //
                                            || "Rücknahme".equals(v.get("type")) //
                                            || "Sell".equals(v.get("type"))  //
                                            || "Redemption".equals(v.get("type"))  //
                                            || "Vente".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Order: Kauf
                        // 1.369 Ant iShares Core S&P500
                        // ISIN: IE00B5BMR087
                        // Kurs: USD 262.51
                        // @formatter:on
                        .section("isin", "name", "currency") //
                        .find("(Order|Ordre): (Kauf|Zeichnung|Verkauf|R.cknahme|Buy|Subscription|Sell|Redemption|Achat|Vente)") //
                        .match("^[\\.,\\d]+ (Anteile|Qty|Ant|units|Qt.|Quantit.|parts|actions)?(?<name>.*)$") //
                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^(Kurs|Price|Cours): (?<currency>[\\w]{3}) .*$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            v.getTransactionContext().put(ATTRIBUTE_SECURITY_CURRENCY,
                                            asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // 1.369 Ant iShares Core S&P500
                        // 0.027 units Swisscanto Pacific ex Japan
                        // @formatter:on
                        .section("shares") //
                        .find("(Order|Ordre): (Kauf|Zeichnung|Verkauf|R.cknahme|Buy|Subscription|Sell|Redemption|Achat|Vente)") //
                        .match("^(?<shares>[\\.,\\d]+) (Anteile|Qty|Ant|units|Qt.|Quantit.|parts|actions)?(?<name>.*)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Verrechneter Betrag: Valuta 05.07.2018 CHF 360.43
                        // @formatter:on
                        .section("date") //
                        .match("^(Verrechneter Betrag: Valuta|Charged amount: Value date|Montant comptabilis.: Valeur) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // Verrechneter Betrag: Valuta 05.07.2018 CHF 360.43
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^(Verrechneter Betrag: Valuta|Charged amount: Value date|Montant comptabilis.: Valeur) .* (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d]+).*$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Betrag USD 359.27
                        // Umrechnungskurs CHF/USD 1.00195 CHF 359.98
                        // @formatter:on
                        .section("fxGross", "termCurrency", "baseCurrency", "exchangeRate", "gross").optional() //
                        .match("^(Betrag|Amount|Montant) [\\w]{3} (?<fxGross>[\\.,'\\d]+)$") //
                        .match("^(Umrechnungskurs|Exchange rate|Taux de conversion) (?<termCurrency>[\\w]{3})\\/(?<baseCurrency>[\\w]{3}) (?<exchangeRate>[\\.,'\\d]+) [\\w]{3} (?<gross>[\\.,'\\d]+)$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                            Money fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                            storeExchangeRate(v.getTransactionContext(), gross, fxGross, rate);
                        })

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap((t, ctx) -> {
                            var item = new BuySellEntryItem(t);
                            storeItemData(item, ctx);
                            return item;
                        });

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    private void addInterestTransaction()
    {
        final var type = new DocumentType("(Zins|Interest|Int.r.ts)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Zins|Interest|Int.r.ts)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        // @formatter:off
                        // Zins
                        // Am 31.03.2019 haben wir Ihrem Konto gutgeschrieben:
                        // Zinsgutschrift: CHF 0.04
                        // @formatter:on
                        .section("date", "amount", "currency") //
                        .find("(Zins|Interest|Int.r.ts)") //
                        .match("^(Am|On|Nous avons cr.dit. le) ?" //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(haben wir (Ihrem Konto|Ihnen) gutgeschrieben|we have credited (you|your account)|les int.r.ts suivants):$") //
                        .match("^(Zinsgutschrift|Interest credit|Interest amount|Int.r.ts cr.diteurs): (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d]+)$") //
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
                        .match("^(?<note1>(Zinssatz|Interest rate|Taux d.int.r.t): [\\.,\\d]+%)$") //
                        .match("^(?<note2>(Zinsperiode|Interest period|P.riode d.int.r.t): .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + " | " + trim(v.get("note2"))))

                        .wrap(TransactionItem::new);
    }

    private void addFeeTransaction()
    {
        final var type = new DocumentType("(Belastung|Commission)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Belastung|Commission)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.FEES))

                        // @formatter:off
                        // Belastung
                        // Verrechneter Betrag: Valuta 31.01.2019 CHF -1.11
                        // @formatter:on
                        .section("date", "amount", "currency") //
                        .find("(Belastung|Commission)") //
                        .match("^(Verrechneter Betrag|Charged amount|Montant d.bit.): (Valuta|Value|Valeur)( date)? " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) " //
                                        + "(?<currency>[\\w]{3}) (\\-)?(?<amount>[\\.,'\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Effektive VIAC Verwaltungsgebühr: 0.123% p.a. CHF -1.11
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^(Effektive|Effective) " //
                                        + "(?<note1>(VIAC Verwaltungsgeb.hr" //
                                        + "|VIAC administration fee" //
                                        + "|VIAC p\\.a\\.)).*" //
                                        + ": (?<note2>[\\.,\\d]+%).*$") //
                        .assign((t, v) -> t.setNote(v.get("note1") + ": " + v.get("note2")))

                        // @formatter:off
                        // Commission de gestion effective VIAC: 0.26% p.a. CHF -3.10
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Commission de gestion effective VIAC.*: [\\.,\\d]+%).*$") //
                        .assign((t, v) -> t.setNote(v.get("note")))

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                return new SkippedItem(new TransactionItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            return new TransactionItem(t);
                        });
    }

    private void addDividendTransaction()
    {
        final var type = new DocumentType("(Dividendenaussch.ttung" //
                        + "|Dividend Payment" //
                        + "|Avis de dividende" //
                        + "|R.ckerstattung Quellensteuer" //
                        + "|Refund withholding tax" //
                        + "|Remboursement de l.imp.t . la source)");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Dividendenaussch.ttung" //
                        + "|Dividend Payment" //
                        + "|Cancelation Dividend Payment" //
                        + "|Avis de dividende" //
                        + "|R.ckerstattung Quellensteuer" //
                        + "|Refund withholding tax" //
                        + "|Remboursement de l.imp.t . la source)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        .section("type").optional() //
                        .match("^(Dividendenart|Aussch.ttungsart|Type of (dividend|distribution)|Type de dividende): " //
                                        + "(?<type>(R.ckerstattung Quellensteuer|R.ckerstattung Verrechnungssteuer|Refund (\\w+ )?withholding tax|Remboursement d.imp.t . la source))$") //
                        .assign((t, v) -> t.setType(AccountTransaction.Type.TAX_REFUND))

                        // @formatter:off
                        // Cancelation Dividend Payment
                        // @formatter:on
                        .section("type").optional() //
                        .match("^(?<type>Cancelation Dividend Payment)$") //
                        .assign((t, v) -> v.markAsFailure(Messages.MsgErrorTransactionOrderCancellationUnsupported))

                        // @formatter:off
                        // Dividendenart: Ordentliche Dividende
                        // 47.817 Ant UBS ETF MSCI USA SRI
                        // ISIN: LU0629460089
                        // Ausschüttung: USD 0.72
                        //
                        // Type of distribution: Refund Swiss withholding tax
                        // 0.196 units UBS Canada
                        // ISIN: CH0030849613
                        // Distribution: CHF 0.23
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .find("(Dividendenart|Aussch.ttungsart|Type of (dividend|distribution)|Type de dividende):.*") //
                        .match("^[\\.,\\d]+ (Anteile|Qty|Ant|parts|units|actions)?(?<name>.*)$") //
                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^(Aussch.ttung|Dividend payment|Distribution|Dividende distribu.): (?<currency>[\\w]{3}) .*$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            v.getTransactionContext().put(ATTRIBUTE_SECURITY_CURRENCY,
                                            asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Dividendenart: Ordentliche Dividende
                        // 47.817 Ant UBS ETF MSCI USA SRI
                        // Type of distribution: Refund Swiss withholding tax
                        // 0.196 units UBS Canada
                        // @formatter:on
                        .section("shares") //
                        .find("(Dividendenart|Aussch.ttungsart|Type of (dividend|distribution)|Type de dividende):.*") //
                        .match("^(?<shares>[\\.,\\d]+) (Anteile|Qty|Ant|parts|units|actions)?.*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Gutgeschriebener Betrag: Valuta 04.02.2022 CHF 31.44
                        // @formatter:on
                        .section("date") //
                        .match("^(Gutgeschriebener Betrag: Valuta|Amount (credited|debited): Value date|Montant cr.dit.: Valeur) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Gutgeschriebener Betrag: Valuta 04.02.2022 CHF 31.44
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^(Gutgeschriebener Betrag: Valuta|Amount (credited|debited): Value date|Montant cr.dit.: Valeur) .* (?<currency>[\\w]{3}) (?<amount>[\\.,'\\d]+)$") //
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
                                                        .match("^(Betrag|Amount|Montant) [\\w]{3} (?<fxGross>[\\.,'\\d]+)$") //
                                                        .match("^(Umrechnungskurs|Exchange rate|Taux de conversion) (?<termCurrency>[\\w]{3})\\/(?<baseCurrency>[\\w]{3}) (?<exchangeRate>[\\.,'\\d]+) [\\w]{3} (?<gross>[\\.,'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                            storeExchangeRate(v.getTransactionContext(), gross, fxGross, rate);
                                                        }),
                                        // @formatter:off
                                        // Betrag USD 34.26
                                        // Umrechnungskurs CHF/USD
                                        // 0.91759 CHF 31.44
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "termCurrency", "baseCurrency", "exchangeRate", "gross") //
                                                        .match("^(Betrag|Amount|Montant) [\\w]{3} (?<fxGross>[\\.,'\\d]+)$") //
                                                        .match("^(Umrechnungskurs|Exchange rate|Taux de conversion) (?<termCurrency>[\\w]{3})\\/(?<baseCurrency>[\\w]{3}).*$") //
                                                        .match("^(?<exchangeRate>[\\.,'\\d]+) [\\w]{3} (?<gross>[\\.,'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                                                            Money fxGross = Money.of(rate.getBaseCurrency(), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                            storeExchangeRate(v.getTransactionContext(), gross, fxGross, rate);
                                                        }))

                        // @formatter:off
                        // Dividendenart: Ordentliche Dividende
                        // Type of dividend: Ordinary dividend
                        // Type of distribution: Refund Swiss withholding tax
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(Dividendenart|Aussch.ttungsart|Type of (dividend|distribution)|Type de dividende): (?<note>.*)") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap((t, ctx) -> {
                            var item = new TransactionItem(t);
                            storeItemData(item, ctx);
                            return item;
                        });
    }

    private void storeExchangeRate(PDFParser.TransactionContext context, Money gross, Money fxGross,
                    ExtrExchangeRate rate)
    {
        context.put(ATTRIBUTE_GROSS, gross);
        context.put(ATTRIBUTE_FX_GROSS, fxGross);
        context.put(ATTRIBUTE_EXCHANGE_RATE, rate.getRate(gross.getCurrencyCode()).toPlainString());
    }

    private void storeItemData(Item item, PDFParser.TransactionContext context)
    {
        item.setData(ATTRIBUTE_SECURITY_CURRENCY, context.get(ATTRIBUTE_SECURITY_CURRENCY));
        item.setData(ATTRIBUTE_GROSS, context.get(ATTRIBUTE_GROSS));
        item.setData(ATTRIBUTE_FX_GROSS, context.get(ATTRIBUTE_FX_GROSS));
        item.setData(ATTRIBUTE_EXCHANGE_RATE, context.get(ATTRIBUTE_EXCHANGE_RATE));
    }

    /**
     * @formatter:off
     * Processes the extracted items of one import:
     *
     * 1. Corrects the currency of securities created during this import. The refund document
     *    of the withholding tax always shows CHF, the dividend and buy/sell documents show
     *    the currency of the security.
     *
     * 2. Adds the missing gross value to dividends and buy/sell transactions whose security
     *    currency was corrected.
     *
     * 3. Adds the gross value to the tax refund from the matching dividend
     *    (same security, value date and shares). If there is no matching dividend and
     *    the security currency differs, the tax refund is marked as failure.
     * @formatter:on
     */
    @Override
    public void postProcessing(List<Item> items)
    {
        var dividendAndBuySellItems = items.stream() //
                        .filter(i -> i.getData(ATTRIBUTE_SECURITY_CURRENCY) != null) //
                        .filter(i -> !isTaxRefund(i)) //
                        .toList();

        // correct the currency of securities created during this import
        for (var item : dividendAndBuySellItems)
        {
            var security = item.getSecurity();
            var securityCurrency = (String) item.getData(ATTRIBUTE_SECURITY_CURRENCY);

            if (security != null && !getClient().getSecurities().contains(security)
                            && !securityCurrency.equals(security.getCurrencyCode()))
                security.setCurrencyCode(securityCurrency);
        }

        // add missing gross values to dividend and buy/sell transactions
        for (var item : dividendAndBuySellItems)
            addMissingGrossValue(item);

        // add gross value from matching dividend to the tax refund
        items.stream() //
                        .filter(this::isTaxRefund) //
                        .filter(i -> !i.isFailure()) //
                        .forEach(refundItem -> {
                            var refund = (AccountTransaction) refundItem.getSubject();

                            if (refund.getSecurity() == null
                                            || refund.getCurrencyCode().equals(refund.getSecurity().getCurrencyCode())
                                            || refund.getUnit(Unit.Type.GROSS_VALUE).isPresent())
                                return;

                            var dividendGrossValue = items.stream() //
                                            .filter(i -> !i.isFailure()) //
                                            .filter(i -> i.getSubject() instanceof AccountTransaction tx
                                                            && tx.getType() == AccountTransaction.Type.DIVIDENDS
                                                            && tx.getSecurity() == refund.getSecurity()
                                                            && tx.getDateTime().equals(refund.getDateTime())
                                                            && tx.getShares() == refund.getShares()) //
                                            .map(i -> ((AccountTransaction) i.getSubject())
                                                            .getUnit(Unit.Type.GROSS_VALUE))
                                            .flatMap(Optional::stream) //
                                            .filter(u -> u.getForex().getCurrencyCode()
                                                            .equals(refund.getSecurity().getCurrencyCode())) //
                                            .findFirst();

                            if (dividendGrossValue.isPresent())
                            {
                                var exchangeRate = dividendGrossValue.get().getExchangeRate();
                                var fxAmount = BigDecimal.valueOf(refund.getAmount()) //
                                                .divide(exchangeRate, Values.MC) //
                                                .setScale(0, RoundingMode.HALF_EVEN).longValue();

                                refund.addUnit(new Unit(Unit.Type.GROSS_VALUE, refund.getMonetaryAmount(),
                                                Money.of(refund.getSecurity().getCurrencyCode(), fxAmount),
                                                exchangeRate));
                            }
                            else
                            {
                                refundItem.setFailureMessage(Messages.MsgErrorTransactionSkipIfDetailsMissing);
                            }
                        });
    }

    private boolean isTaxRefund(Item item)
    {
        return item.getSubject() instanceof AccountTransaction tx
                        && tx.getType() == AccountTransaction.Type.TAX_REFUND;
    }

    private void addMissingGrossValue(Item item)
    {
        var gross = (Money) item.getData(ATTRIBUTE_GROSS);
        var fxGross = (Money) item.getData(ATTRIBUTE_FX_GROSS);
        var exchangeRate = (String) item.getData(ATTRIBUTE_EXCHANGE_RATE);

        if (gross == null || fxGross == null || exchangeRate == null)
            return;

        name.abuchen.portfolio.model.Transaction tx;
        if (item.getSubject() instanceof BuySellEntry entry)
            tx = entry.getPortfolioTransaction();
        else if (item.getSubject() instanceof AccountTransaction accountTransaction)
            tx = accountTransaction;
        else
            return;

        if (tx.getSecurity() == null || tx.getCurrencyCode().equals(tx.getSecurity().getCurrencyCode())
                        || tx.getUnit(Unit.Type.GROSS_VALUE).isPresent()
                        || !fxGross.getCurrencyCode().equals(tx.getSecurity().getCurrencyCode()))
            return;

        tx.addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, fxGross, new BigDecimal(exchangeRate)));

        if (item.getSubject() instanceof BuySellEntry entry)
            ExtractorUtils.fixGrossValueBuySell().accept(entry);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //
                        // @formatter:off
                        // Stempelsteuer CHF 1.68
                        // @formatter:on
                        .section("currency", "tax").optional() //
                        .match("^(Stempelsteuer|Stamp duty|Droit de timbre) (?<currency>[\\w]{3}) (?<tax>[\\.,'\\d]+)$") //
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
