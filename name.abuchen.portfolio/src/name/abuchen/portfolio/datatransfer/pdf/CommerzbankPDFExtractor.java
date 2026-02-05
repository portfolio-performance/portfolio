package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.replaceMultipleBlanks;
import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
import static name.abuchen.portfolio.util.TextUtil.stripBlanksAndUnderscores;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Pair;

/**
 * @formatter:off
 * @implNote Commerzbank provides two documents for the transaction.
 *           The security transaction and the taxes treatment.
 *           Both documents are provided as one PDF or as two PDFs.
 *
 *           The security transaction includes the fees, but not the correct taxes
 *           and the taxes treatment includes all taxes (including withholding tax),
 *           but not all fees.
 *
 *           Therefore, we use the documents based on their function and merge both documents, if possible, as one transaction.
 *           {@code
 *              matchTransactionPair(List<Item> transactionList,List<Item> taxesTreatmentList)
 *           }
 *
 *           The separate taxes treatment does only contain taxes in the account currency.
 *           However, if the security currency differs, we need to provide the currency conversion.
 *           {@code
 *              applyMissingCurrencyConversionBetweenTaxesAndPurchaseSale(Collection<TransactionTaxesPair> purchaseSaleTaxPairs)
 *           }
 *
 *           Always import the securities transaction and the taxes treatment for a correct transaction.
 *           Due to rounding differences, the correct gross amount is not always shown in the securities transaction.
 *
 *           In postProcessing, we always finally delete the taxes treatment.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class CommerzbankPDFExtractor extends AbstractPDFExtractor
{
    private static record TransactionTaxesPair(Item transaction, Item tax)
    {
    }

    private static final String ATTRIBUTE_GROSS_TAXES_TREATMENT = "gross_taxes_treatment";

    public CommerzbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("C O M M E R Z B A N K");
        addBankIdentifier("Commerzbank AG");

        addBuySellTransaction();
        addDividendeTransaction();
        addTaxesTreatmentTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Commerzbank AG";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("W[\\s]*e[\\s]*r[\\s]*t[\\s]*p[\\s]*a[\\s]*p[\\s]*i[\\s]*e[\\s]*r[\\s]*(k[\\s]*a[\\s]*u[\\s]*f|v[\\s]*e[\\s]*r[\\s]*k[\\s]*a[\\s]*u[\\s]*f)"); //
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^W[\\s]*e[\\s]*r[\\s]*t[\\s]*p[\\s]*a[\\s]*p[\\s]*i[\\s]*e[\\s]*r[\\s]*(k[\\s]*a[\\s]*u[\\s]*f|v[\\s]*e[\\s]*r[\\s]*k[\\s]*a[\\s]*u[\\s]*f).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Wertpapierkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(?<type>W[\\s]*e[\\s]*r[\\s]*t[\\s]*p[\\s]*a[\\s]*p[\\s]*i[\\s]*e[\\s]*r[\\s]*(k[\\s]*a[\\s]*u[\\s]*f|v[\\s]*e[\\s]*r[\\s]*k[\\s]*a[\\s]*u[\\s]*f)).*$") //
                        .assign((t, v) -> {
                            if ("Wertpapierverkauf".equals(stripBlanks(v.get("type"))))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // W e r t p a p i e r - B e z e i c h n u n g W e r t p a p i e r k e n n n u m m e r
                        // i S h s I I I - C o r e MSCI W o r l d U . E T F A0RPWH
                        // R e g i s t e r e d S h s USD ( A c c ) o . N .
                        // S t . 0 , 5 7 2 EUR 4 3 , 6 4
                        //
                        // W e r t p a p i e r - B e z e i c h n u n g W e r t p a p i e r k e n n n u m m e r
                        // A l l i a n z SE 8 4 0 4 0 0
                        // v i n k . N a m e n s - A k t i e n o . N .
                        // Summe S t . 2 5 0 EUR 1 9 1 , 0 0 8 6 4 EUR 4 7 . 7 5 2 , 1 6
                        // @formatter:on
                        .section("name", "wkn", "nameContinued", "currency") //
                        .find("W[\\s]*e[\\s]*r[\\s]*t[\\s]*p[\\s]*a[\\s]*p[\\s]*i[\\s]*e[\\s]*r[\\s]*([\\-\\s]+)?B[\\s]*e[\\s]*z[\\s]*e[\\s]*i[\\s]*c[\\s]*h[\\s]*n[\\s]*u[\\s]*n[\\s]*g[\\s]*W[\\s]*e[\\s]*r[\\s]*t[\\s]*p[\\s]*a[\\s]*p[\\s]*i[\\s]*e[\\s]*r[\\s]*k[\\s]*e[\\s]*n[\\s]*n[\\s]*n[\\s]*u[\\s]*m[\\s]*m[\\s]*e[\\s]*r.*") //
                        .match("^(?<name>.*)([,])?[\\s]{1,}(?<wkn>(?:[A-Z0-9][\\s]*){6})$") //
                        .match("^(?<nameContinued>.*)") //
                        .match("^(Summe[\\s]*)?S[\\s]*t[\\s]*\\.[\\s]{1,}[\\.,\\d\\s]+[\\s]{1,}(?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> {
                            v.put("name", replaceMultipleBlanks(v.get("name")));
                            v.put("nameContinued", replaceMultipleBlanks(v.get("nameContinued")));
                            v.put("wkn", stripBlanks(v.get("wkn")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // S t . 2 0 0 EUR 2 0 1 , 7 0
                        // Summe S t . 2 5 0 EUR 1 9 1 , 0 0 8 6 4 EUR 4 7 . 7 5 2 , 1 6
                        // @formatter:on
                        .section("shares") //
                        .match("^(Summe[\\s]*)?S[\\s]*t[\\s]*\\.[\\s]{1,}(?<shares>[\\.,\\d\\s]+)[\\s]{1,}[A-Z]{3}.*$") //
                        .assign((t, v) -> t.setShares(asShares(stripBlanks(v.get("shares")))))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // 1 2 : 0 6 S t . 2 3 0 EUR 1 8 4 , 1 6 EUR 4 2 . 3 5 6 , 8 0
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("time") //
                                                        .match("^(?<time>[\\d\\s]+:[\\d\\s]+)[\\s]{1,}(S[\\s]*t[\\s]*.|St.)[\\s]{1,}[\\.,\\d\\s]+[\\s]{1,}[A-Z]{3}[\\s]{1,}[\\.,\\d\\s]+[\\s]{1,}[A-Z]{3} [\\.,\\d\\s]+$") //
                                                        .assign((t, v) -> type.getCurrentContext().put("time", stripBlanks(v.get("time")))),
                                        // @formatter:off
                                        // H a n d e l s z e i t : 1 3 : 1 0 U h r (MEZ/MESZ)
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("time") //
                                                        .match("^H[\\s]*a[\\s]*n[\\s]*d[\\s]*e[\\s]*l[\\s]*s[\\s]*z[\\s]*e[\\s]*i[\\s]*t[\\s]*:[\\s]{1,}(?<time>[\\d\\s]+:[\\d\\s]+).*$") //
                                                        .assign((t, v) -> type.getCurrentContext().put("time", stripBlanks(v.get("time")))))

                        // @formatter:off
                        // G e s c h ä f t s t a g : 1 7 . 0 2 . 2 0 2 1 A b w i c k l u n g : F e s t p r e i s
                        // G e s c h ä f t s t a g : 3 1 . 0 1 . 2 0 2 1 A u s f ü h r u n g s p l a t z : FRANKFURT
                        // @formatter:on
                        .section("date") //
                        .match("^G[\\s]*e[\\s]*s[\\s]*c[\\s]*h[\\s]*.[\\s]*f[\\s]*t[\\s]*s[\\s]*t[\\s]*a[\\s]*g[\\s]*:[\\s]{1,}(?<date>[\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+).*$") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().get("time") != null)
                                t.setDate(asDate(stripBlanks(v.get("date")), type.getCurrentContext().get("time")));
                            else
                                t.setDate(asDate(stripBlanks(v.get("date"))));
                        })

                        // @formatter:off
                        // DE26 1 0 0 4 0 0 4 8 0 6 8 0 4 0 3 3 0 2 EUR 2 0 . 0 4 . 2 0 1 7 EUR 2 4 , 9 6
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^.*[A-Z]{3} [\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+ (?<currency>[A-Z]{3})(?<amount>[\\.,\\d\\s]+).*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                        })

                        // @formatter:off
                        // 1 0 8 0 4 3 1 3 7 2 7 0 Rechnungsnummer : 4 1 9 7 9 4 9 1 6 7 9 8 D 1 C 2
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*Rechnungsnummer[\\s]*:[\\s]{1,}(?<note>.*)$") //
                        .assign((t, v) -> t.setNote("R.-Nr.: " + stripBlanks(v.get("note"))))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final var type = new DocumentType("(D[\\s]*i[\\s]*v[\\s]*i[\\s]*d[\\s]*e[\\s]*n[\\s]*d[\\s]*e[\\s]*n[\\s]*g[\\s]*u[\\s]*t[\\s]*s[\\s]*c[\\s]*h[\\s]*r[\\s]*i[\\s]*f[\\s]*t|E[\\s]*r[\\s]*t[\\s]*r[\\s]*a[\\s]*g[\\s]*s[\\s]*g[\\s]*u[\\s]*t[\\s]*s[\\s]*c[\\s]*h[\\s]*r[\\s]*i[\\s]*f[\\s]*t)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^(D[\\s]*i[\\s]*v[\\s]*i[\\s]*d[\\s]*e[\\s]*n[\\s]*d[\\s]*e[\\s]*n[\\s]*g[\\s]*u[\\s]*t[\\s]*s[\\s]*c[\\s]*h[\\s]*r[\\s]*i[\\s]*f[\\s]*t|E[\\s]*r[\\s]*t[\\s]*r[\\s]*a[\\s]*g[\\s]*s[\\s]*g[\\s]*u[\\s]*t[\\s]*s[\\s]*c[\\s]*h[\\s]*r[\\s]*i[\\s]*f[\\s]*t).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // p e r 2 7 . 0 3 . 2 0 2 0 Samsung E l e c t r o n i c s Co. L t d . 881823
                        // STK 1 2 , 0 0 0 R.Shs(NV)Pf(GDR144A)/25 SW 100 US7960502018
                        // USD 7 ,219127 D i v i d e n d e p r o S t ü c k f ü r G e s c h ä f t s j a h r 0 1 . 0 1 . 2 0 b i s 3 1 . 1 2 . 2 0
                        //
                        // p e r  2 8 . 0 2 . 2 0 2 5                          A l l i a n z  R o h s t o f f f o n d s                  8 4 7 5 0 9
                        // S T K              3 2 9 , 8 1 7                 I n h a b e r - A n t e i l e  A  ( E U R )          D E 0 0 0 8 4 7 5 0 9 6
                        // EUR 1,372      Ausschüttung pro Stück für Geschäftsjahr     01.01.24 bis 31.12.24
                        // @formatter:on
                        .section("name", "wkn", "nameContinued", "isin", "currency") //
                        .match("^p[\\s]*e[\\s]*r[\\s]{1,}[\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+ (?<name>.*)([,])?[\\s]{1,}(?<wkn>(?:[A-Z0-9][\\s]*){6}).*$") //
                        .match("^S[\\s]*T[\\s]*K[\\s]{1,}[\\.,\\d\\s]+ (?<nameContinued>.*) (?<isin>(?:[A-Z0-9][\\s]*){12}).*$") //
                        .match("^(?<currency>[A-Z]{3})[\\s]{1,}[\\.,\\d\\s]+ (D[\\s]*i[\\s]*v[\\s]*i[\\s]*d[\\s]*e[\\s]*n[\\s]*d[\\s]*e|A[\\s]*u[\\s]*s[\\s]*s[\\s]*c[\\s]*h[\\s]*.[\\s]*t[\\s]*t[\\s]*u[\\s]*n[\\s]*g).*$") //
                        .assign((t, v) -> {
                            v.put("name", replaceMultipleBlanks(v.get("name")));
                            v.put("nameContinued", replaceMultipleBlanks(v.get("nameContinued")));
                            v.put("wkn", stripBlanks(v.get("wkn")));
                            v.put("isin", stripBlanks(v.get("isin")));

                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // STK 1 2 , 0 0 0 R.Shs(NV)Pf(GDR144A)/25 SW 100 US7960502018
                        // S T K              3 2 9 , 8 1 7                 I n h a b e r - A n t e i l e  A  ( E U R )          D E 0 0 0 8 4 7 5 0 9 6
                        // @formatter:on
                        .section("shares") //
                        .match("^S[\\s]*T[\\s]*K[\\s]{1,}(?<shares>[\\.,\\d\\s]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(stripBlanks(v.get("shares")))))

                        // @formatter:off
                        // z a h l b a r ab 1 8 . 0 6 . 2 0 1 5
                        // @formatter:on
                        .section("date") //
                        .match("^z[\\s]*a[\\s]*h[\\s]*l[\\s]*b[\\s]*a[\\s]*r[\\s]*a[\\s]*b[\\s]{1,}(?<date>[\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(stripBlanks(v.get("date")))))

                        // @formatter:off
                        // DE12 3456 7890 1234 5678 01 EUR 2 2 . 0 6 . 2 0 1 5 EUR 1 2 3 , 4 5
                        // @formatter:on
                        .section("currency", "amount") //
                        .match(".*[A-Z]{3} [\\d\\s]+\\.[\\d\\s]+\\.[\\d\\s]+ (?<currency>[A-Z]{3})(?<amount>[\\.,\\d\\s]+).*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                        })

                        // @formatter:off
                        // B r u t t o b e t r a g : USD 8 6 , 6 3
                        // 2 2 , 0 0 0 % Q u e l l e n s t e u e r USD 1 9 , 0 6 -
                        // Ausmachender B e t r a g USD 6 7 , 3 3
                        // zum D e v i s e n k u r s : EUR/USD 1 ,098400 EUR 6 1 , 3 0
                        // @formatter:on
                        .section("fxGross", "exchangeRate", "baseCurrency", "termCurrency").optional() //
                        .match("^B[\\s]*r[\\s]*u[\\s]*t[\\s]*t[\\s]*o[\\s]*b[\\s]*e[\\s]*t[\\s]*r[\\s]*a[\\s]*g[\\s]*:[\\s]{1,}[A-Z]{3}[\\s]{1,}(?<fxGross>[\\.,\\d\\s]+).*$") //
                        .match("^.*(?<baseCurrency>[A-Z]{3})\\/(?<termCurrency>[A-Z]{3})[\\s]{1,}(?<exchangeRate>[\\.,\\d\\s]+)[\\s]{1,}[A-Z]{3}[\\s]{1,}[\\.,\\d\\s]+.*$") //
                        .assign((t, v) -> {
                            v.put("exchangeRate", stripBlanks(v.get("exchangeRate")));

                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(stripBlanks(v.get("fxGross"))));
                            var gross = rate.convert(rate.getBaseCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Add withHolding tax and set correct gross amount
                        // @formatter:on
                        .optionalOneOf( //
                                        // @formatter:off
                                        // Bruttobetrag:                     USD               0,22
                                        // 15,000 % Quellensteuer            USD               0,03 -
                                        //     zum Devisenkurs: EUR/USD      1,084100                 EUR               4,65
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxCurrency", "fxGross", "currencyWithHoldingTax", "withHoldingTax", "baseCurrency", "termCurrency", "exchangeRate") //
                                                        .match("^Bruttobetrag:[\\s]{1,}(?<fxCurrency>[\\w]{3})[\\s]{1,}(?<fxGross>[\\.,\\d]+).*$") //
                                                        .match("^[\\.,\\d]+ % Quellensteuer[\\s]{1,}(?<currencyWithHoldingTax>[\\w]{3})[\\s]{1,}(?<withHoldingTax>[\\.,\\d]+) \\-.*$") //
                                                        .match("^.*zum Devisenkurs: (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3})[\\s]{1,}(?<exchangeRate>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var fxWithHoldingTax = Money.of(asCurrencyCode(v.get("currencyWithHoldingTax")), asAmount(v.get("withHoldingTax")));
                                                            var withHoldingTax = rate.convert(rate.getBaseCurrency(), fxWithHoldingTax);

                                                            t.setMonetaryAmount(t.getMonetaryAmount().add(withHoldingTax));

                                                            var fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                                                            checkAndSetGrossUnit(t.getMonetaryAmount(), fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // 15,000 % Quellensteuer                                     EUR               XX,XX -
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "withHoldingTax") //
                                                        .match("^[\\.,\\d]+ % Quellensteuer[\\s]{1,}(?<currencyWithHoldingTax>[\\w]{3})[\\s]{1,}(?<withHoldingTax>[\\.,\\d]+) \\-.*$") //
                                                        .assign((t, v) -> {
                                                            var withHoldingTax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("withHoldingTax")));

                                                            if (t.getMonetaryAmount().getCurrencyCode().equals(withHoldingTax.getCurrencyCode()))
                                                                t.setMonetaryAmount(t.getMonetaryAmount().add(withHoldingTax));
                                                        }))

                        // @formatter:off
                        // ( R e f e r e n z - N r . 3345AO12BC3D4445E).
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*R[\\s]*e[\\s]*f[\\s]*e[\\s]*r[\\s]*e[\\s]*n[\\s]*z[\\s]*\\-[\\s]*N[\\s]*r[\\s]*\\. (?<note>.*)\\).*$") //
                        .assign((t, v) -> t.setNote("Ref.-Nr.: " + trim(v.get("note"))))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addTaxesTreatmentTransaction()
    {
        final var type = new DocumentType("Steuerliche Behandlung: (Wertpapier(kauf|verkauf)|Verkauf|.*Dividende|.*Aussch.ttung)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Kundennr\\. \\/BLZ Bezeichnung$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Stk. 150 THE SOUTHERN CO. DL 5 , WKN / ISIN: 852523 / US8425871071
                                        // Zu Ihren Gunsten vor Steuern: EUR 1 2 . 2 0 4 , 2 8 USD 1 3 . 2 1 7 , 2 4
                                        //
                                        // Stk. 70 PEPSICO INC. DL-,0166 , WKN / ISIN: 851995 / US7134481081
                                        // Zu Ihren Lasten vor Steuern: EUR - 9 . 6 4 8 , 1 7 USD - 1 0 . 4 4 7 , 0 4
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .match("^Stk\\. (\\-)?[\\.,\\d]+ (?<name>.*), WKN \\/ ISIN: (?<wkn>(?:[A-Z0-9][\\s]*){6}) \\/ (?<isin>(?:[A-Z0-9][\\s]*){12}).*$") //
                                                        .match("^Zu Ihren (Gunsten|Lasten) vor Steuern:[\\s]{1,}[A-Z]{3}[\\-\\s]{1,}[\\.,\\d\\s]+[\\s]{1,}(?<currency>[A-Z]{3})[\\-\\s]{1,}[\\.,\\d\\s]+.*$") //
                                                        .assign((t, v) -> {
                                                            v.put("name", trim(replaceMultipleBlanks(v.get("name"))));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // Stk. -10,195 VERMOEGENSMA.BALANCE A EO , WKN / ISIN: A0M16S / LU0321021155
                                        // Zu Ihren Gunsten vor Steuern: EUR 1 . 4 3 9 , 1 3
                                        //
                                        // Stk.             126 NESTLE NAM.        SF-,10 , WKN / ISIN: A0Q4DC  / CH0038863350
                                        // Z u  Ih r e n G u n s t e n v o r S te u e r n :                                                                                                    E U R             265,1 4
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .match("^[\\s]*Stk\\.[\\s]*(\\-)?[\\.,\\d]+ (?<name>.*), WKN \\/ ISIN: (?<wkn>(?:[A-Z0-9][\\s]*){6}) \\/ (?<isin>(?:[A-Z0-9][\\s]*){12}).*$") //
                                                        .match("^[\\s]*Z[\\s]*u[\\s]*I[\\s]*h[\\s]*r[\\s]*e[\\s]*n[\\s]*(?:G[\\s]*u[\\s]*n[\\s]*s[\\s]*t[\\s]*e[\\s]*n|L[\\s]*a[\\s]*s[\\s]*t[\\s]*e[\\s]*n)[\\s]*v[\\s]*o[\\s]*r[\\s]*S[\\s]*t[\\s]*e[\\s]*u[\\s]*e[\\s]*r[\\s]*n[\\s]*:?[\\s]*?(?<currency>[A-Z\\s]{3,}).*$") //
                                                        .assign((t, v) -> {
                                                            v.put("name", trim(replaceMultipleBlanks(v.get("name"))));
                                                            v.put("currency", stripBlanks(v.get("currency")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // Stk. -10,195 VERMOEGENSMA.BALANCE A EO , WKN / ISIN: A0M16S / LU0321021155
                        // Stk.             126 NESTLE NAM.        SF-,10 , WKN / ISIN: A0Q4DC  / CH0038863350
                        // @formatter:on
                        .section("shares") //
                        .match("^Stk\\.[\\s]*(\\-)?(?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Steuerliche Behandlung: Wertpapierkauf Nr. 72006822 vom 17.02.2021
                        // Steuerliche Behandlung: Ausländische Dividende vom 24.04.2025
                        // @formatter:on
                        .section("date") //
                        .match("^Steuerliche Behandlung: .* vom (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Zu Ihren Gunsten vor Steuern: EUR 1 2 . 2 0 4 , 2 8 USD 1 3 . 2 1 7 , 2 4
                                        // Steuerbemessungsgrundlage (1) EUR 6 . 4 2 2 , 0 8
                                        // abgeführte Steuern EUR - 1 . 6 9 3 , 8 2 USD - 1 . 8 3 4 , 4 1
                                        // Umrechnungen zum Devisenkurs 1,083000
                                        //
                                        // Zu Ihren Lasten vor Steuern: EUR - 9 . 6 4 8 , 1 7 USD - 1 0 . 4 4 7 , 0 4
                                        // teuerbemessungsgrundlage EUR 0 , 0 0
                                        // abgeführte Steuern EUR 0 , 0 0 USD 0 , 0 0
                                        // Zu Ihren Lasten nach Steuern: USD - 1 0 . 4 4 7 , 0 4
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currencyBeforeTaxes", "grossBeforeTaxes", "fxCurrencyAssessmentBasis", "fxGrossAssessmentBasis", "currencyDeductedTaxes", "deductedTaxes", "exchangeRate") //
                                                        .match("^Zu Ihren (Gunsten|Lasten) vor Steuern:[\\s]{1,}[A-Z]{3}[\\-\\s]{1,}[\\.,\\d\\s]+[\\s]{1,}(?<currencyBeforeTaxes>[A-Z]{3})[\\-\\s]{1,}(?<grossBeforeTaxes>[\\.,\\d\\s]+).*$") //
                                                        .match("^Steuerbemessungsgrundlage[\\s]{1,}([\\(\\s\\d\\)]+)?(?<fxCurrencyAssessmentBasis>[A-Z]{3})[\\s]{1,}(?<fxGrossAssessmentBasis>[\\.,\\d\\s]+).*$") //
                                                        .match("^abgef.hrte Steuern[\\s]{1,}[A-Z]{3}[\\-\\s]{1,}[\\.,\\d\\s]+[\\s]{1,}(?<currencyDeductedTaxes>[A-Z]{3})[\\-\\s]{1,}(?<deductedTaxes>[\\.,\\d\\s]+).*$") //
                                                        .match("^Umrechnungen zum Devisenkurs[\\s]{1,}(?<exchangeRate>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            var grossBeforeTaxes = Money.of(asCurrencyCode(v.get("currencyBeforeTaxes")), asAmount(stripBlanks(v.get("grossBeforeTaxes"))));
                                                            var fxGrossAssessmentBasis = Money.of(asCurrencyCode(v.get("fxCurrencyAssessmentBasis")), asAmount(stripBlanks(v.get("fxGrossAssessmentBasis"))));
                                                            var deductedTaxes = Money.of(asCurrencyCode(v.get("currencyDeductedTaxes")), asAmount(stripBlanks(v.get("deductedTaxes"))));

                                                            var exchangeRate = asExchangeRate(v.get("exchangeRate"));
                                                            var inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                                                            var grossAssessmentBasis = Money.of(grossBeforeTaxes.getCurrencyCode(), BigDecimal.valueOf(fxGrossAssessmentBasis.getAmount())
                                                                            .multiply(inverseRate).setScale(0, RoundingMode.HALF_UP).longValue());

                                                            // Calculate the taxes and store gross amount
                                                            if (!grossBeforeTaxes.isZero() && grossAssessmentBasis.isGreaterThan(grossBeforeTaxes))
                                                            {
                                                                t.setMonetaryAmount(grossAssessmentBasis.subtract(grossBeforeTaxes).add(deductedTaxes));

                                                                // Store in transaction context
                                                                v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossAssessmentBasis);
                                                            }
                                                            else
                                                            {
                                                                // Store in transaction context
                                                                v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossBeforeTaxes);

                                                                t.setMonetaryAmount(deductedTaxes);
                                                            }
                                                        }),
                                        // @formatter:off
                                        // Zu Ihren Gunsten vor Steuern: EUR 4 5 2 , 5 1
                                        // Steuerbemessungsgrundlage vor Verlustverrechnung (1) EUR 3 1 6 , 7 6
                                        // abgeführte Steuern EUR 0 , 0 0
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currencyBeforeTaxes", "grossBeforeTaxes", "currencyTaxesBaseBeforeLossOffset", "sign", "grossTaxesBaseBeforeLossOffset", "currencyDeductedTaxes", "deductedTaxes") //
                                                        .match("^Zu Ihren (Gunsten|Lasten) vor Steuern:[\\s]{1,}(?<currencyBeforeTaxes>[A-Z]{3})[\\-\\s]{1,}(?<grossBeforeTaxes>[\\.,\\d\\s]+).*$") //
                                                        .match("^Steuerbemessungsgrundlage vor Verlustverrechnung[\\s]{1,}([\\(\\s\\d\\)]+)?(?<currencyTaxesBaseBeforeLossOffset>[A-Z]{3})(?<sign>[\\-\\s]{1,})(?<grossTaxesBaseBeforeLossOffset>[\\.,\\d\\s]+).*$") //
                                                        .match("^abgef.hrte Steuern[\\s]{1,}(?<currencyDeductedTaxes>[A-Z]{3})[\\-\\s]{1,}(?<deductedTaxes>[\\.,\\d\\s]+).*$") //
                                                        .assign((t, v) -> {
                                                            var grossBeforeTaxes = Money.of(asCurrencyCode(v.get("currencyBeforeTaxes")), asAmount(stripBlanks(v.get("grossBeforeTaxes"))));
                                                            var grossTaxesBaseBeforeLossOffset = Money.of(asCurrencyCode(v.get("currencyTaxesBaseBeforeLossOffset")), asAmount(stripBlanks(v.get("grossTaxesBaseBeforeLossOffset"))));
                                                            var deductedTaxes = Money.of(asCurrencyCode(v.get("currencyDeductedTaxes")), asAmount(stripBlanks(v.get("deductedTaxes"))));

                                                            // Calculate the taxes
                                                            if (!grossBeforeTaxes.isZero() && grossTaxesBaseBeforeLossOffset.isGreaterThan(grossBeforeTaxes) && !"-".equals(trim(v.get("sign"))))
                                                            {
                                                                t.setMonetaryAmount(grossTaxesBaseBeforeLossOffset.subtract(grossBeforeTaxes).add(deductedTaxes));

                                                                // Store in transaction context
                                                                v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossTaxesBaseBeforeLossOffset);
                                                            }
                                                            else
                                                            {
                                                                t.setMonetaryAmount(deductedTaxes);
                                                            }
                                                        }),
                                        // @formatter:off
                                        // Zu Ihren Gunsten vor Steuern: EUR 7 1 3 , 0 0
                                        // Steuerbemessungsgrundlage (1) EUR 4 9 9 , 1 0
                                        // abgeführte Steuern EUR - 1 3 1 , 6 4
                                        //
                                        // Zu Ihren Gunsten vor Steuern: EUR 8 2 , 4 4
                                        // Steuerbemessungsgrundlage EUR 9 6 , 9 9
                                        // abgeführte Steuern EUR - 1 0 , 2 3
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currencyBeforeTaxes", "grossBeforeTaxes", "currencyAssessmentBasis", "grossAssessmentBasis", "currencyDeductedTaxes", "deductedTaxes") //
                                                        .match("^Zu Ihren (Gunsten|Lasten) vor Steuern:[\\s]{1,}(?<currencyBeforeTaxes>[A-Z]{3})[\\-\\s]{1,}(?<grossBeforeTaxes>[\\.,\\d\\s]+).*$") //
                                                        .match("^Steuerbemessungsgrundlage[\\s]{1,}([\\(\\s\\d\\)]+)?(?<currencyAssessmentBasis>[A-Z]{3})[\\s]{1,}(?<grossAssessmentBasis>[\\.,\\d\\s]+).*$") //
                                                        .match("^abgef.hrte Steuern[\\s]{1,}(?<currencyDeductedTaxes>[A-Z]{3})[\\-\\s]{1,}(?<deductedTaxes>[\\.,\\d\\s]+).*$") //
                                                        .assign((t, v) -> {
                                                            var grossBeforeTaxes = Money.of(asCurrencyCode(v.get("currencyBeforeTaxes")), asAmount(stripBlanks(v.get("grossBeforeTaxes"))));
                                                            var grossAssessmentBasis = Money.of(asCurrencyCode(v.get("currencyAssessmentBasis")), asAmount(stripBlanks(v.get("grossAssessmentBasis"))));
                                                            var deductedTaxes = Money.of(asCurrencyCode(v.get("currencyDeductedTaxes")), asAmount(stripBlanks(v.get("deductedTaxes"))));

                                                            // Calculate the taxes and store gross amount
                                                            if (!grossBeforeTaxes.isZero() && grossAssessmentBasis.isGreaterThan(grossBeforeTaxes))
                                                            {
                                                                t.setMonetaryAmount(grossAssessmentBasis.subtract(grossBeforeTaxes).add(deductedTaxes));

                                                                // Store in transaction context
                                                                v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossAssessmentBasis);
                                                            }
                                                            else
                                                            {
                                                                // Store in transaction context
                                                                v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossBeforeTaxes);

                                                                t.setMonetaryAmount(deductedTaxes);
                                                            }
                                                        }),
                                        // @formatter:off
                                        // Z u  Ih r e n G u n s t e n v o r S te u e r n :                                                                                                    E U R             265,1 4
                                        // S  te u e rb e m  e ss u n g s g r u n d la g e                                                            E  U   R                             4   0  7 , 9 2
                                        // a b g e f ü h rt e S t e u er n                                                                                                                    E_ U_ R_ _ _ _ _ _ _ _  _ _ _ _ _-_4_3_,_0_ 3_
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currencyBeforeTaxes", "grossBeforeTaxes", "currencyAssessmentBasis", "grossAssessmentBasis", "currencyDeductedTaxes", "deductedTaxes") //
                                                        .match("^[\\s]*Z[\\s]*u[\\s]*I[\\s]*h[\\s]*r[\\s]*e[\\s]*n[\\s]*(?:G[\\s]*u[\\s]*n[\\s]*s[\\s]*t[\\s]*e[\\s]*n|L[\\s]*a[\\s]*s[\\s]*t[\\s]*e[\\s]*n)[\\s]*v[\\s]*o[\\s]*r[\\s]*S[\\s]*t[\\s]*e[\\s]*u[\\s]*e[\\s]*r[\\s]*n[\\s]*:?[\\s]*?(?<currencyBeforeTaxes>[A-Z\\s]{3,})[\\-\\s]{1,}(?<grossBeforeTaxes>[\\.,\\d\\s]+).*$") //
                                                        .match("^[\\s]*S[\\s]*t[\\s]*e[\\s]*u[\\s]*e[\\s]*r[\\s]*b[\\s]*e[\\s]*m[\\s]*e[\\s]*s[\\s]*s[\\s]*u[\\s]*n[\\s]*g[\\s]*s[\\s]*g[\\s]*r[\\s]*u[\\s]*n[\\s]*d[\\s]*l[\\s]*a[\\s]*g[\\s]*e[\\s]{1,}([\\(\\s\\d\\)]+)?(?<currencyAssessmentBasis>[A-Z\\s]{3,})[\\s]{1,}(?<grossAssessmentBasis>[\\.,\\d\\s]+).*$") //
                                                        .match("^[\\s]*a[\\s]*b[\\s]*g[\\s]*e[\\s]*f[\\s]*.[\\s]*h[\\s]*r[\\s]*t[\\s]*e[\\s]{1,}S[\\s]*t[\\s]*e[\\s]*u[\\s]*e[\\s]*r[\\s]*n[\\s]{1,}(?<currencyDeductedTaxes>[A-Z_\\s]+)[\\-\\s]{1,}(?<deductedTaxes>[\\.,\\d_\\s]+).*$") //
                                                        .assign((t, v) -> {
                                                            var grossBeforeTaxes = Money.of(asCurrencyCode(v.get("currencyBeforeTaxes")), asAmount(stripBlanks(v.get("grossBeforeTaxes"))));
                                                            var grossAssessmentBasis = Money.of(asCurrencyCode(v.get("currencyAssessmentBasis")), asAmount(stripBlanks(v.get("grossAssessmentBasis"))));
                                                            var deductedTaxes = Money.of(asCurrencyCode(stripBlanksAndUnderscores(v.get("currencyDeductedTaxes"))), asAmount(stripBlanksAndUnderscores(v.get("deductedTaxes"))));

                                                            // Calculate the taxes and store gross amount
                                                            if (!grossBeforeTaxes.isZero() && grossAssessmentBasis.isGreaterThan(grossBeforeTaxes))
                                                            {
                                                                t.setMonetaryAmount(grossAssessmentBasis.subtract(grossBeforeTaxes).add(deductedTaxes));

                                                                // Store in transaction context
                                                                v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossAssessmentBasis);
                                                            }
                                                            else
                                                            {
                                                                // Store in transaction context
                                                                v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossBeforeTaxes);

                                                                t.setMonetaryAmount(deductedTaxes);
                                                            }
                                                        }),
                                        // @formatter:off
                                        // Zu Ihren Gunsten vor Steuern: EUR 4 5 . 9 1 8 , 9 7
                                        // Steuerbemessungsgrundlage EUR - 1 . 9 5 9 , 4 0
                                        // erstattete Steuern EUR 5 4 8 , 5 1
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currencyRefundedTaxes", "refundedTaxes") //
                                                        .match("^Zu Ihren (Gunsten|Lasten) vor Steuern:[\\s]{1,}[A-Z]{3}[\\-\\s]{1,}[\\.,\\d\\s]+.*$") //
                                                        .match("^Steuerbemessungsgrundlage[\\s]{1,}[A-Z]{3}[\\-\\s]{1,}[\\.,\\d\\s]+.*$") //
                                                        .match("^erstattete Steuern[\\s]{1,}(?<currencyRefundedTaxes>[A-Z]{3})[\\s]{1,}(?<refundedTaxes>[\\.,\\d\\s]+).*$") //
                                                        .assign((t, v) -> {
                                                            t.setType(AccountTransaction.Type.TAX_REFUND);

                                                            t.setCurrencyCode(asCurrencyCode(v.get("currencyRefundedTaxes")));
                                                            t.setAmount(asAmount(stripBlanks(v.get("refundedTaxes"))));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Zu Ihren Gunsten vor Steuern: EUR 1 2 . 2 0 4 , 2 8 USD 1 3 . 2 1 7 , 2 4
                                        // Steuerbemessungsgrundlage (1) EUR 6 . 4 2 2 , 0 8
                                        // abgeführte Steuern EUR - 1 . 6 9 3 , 8 2 USD - 1 . 8 3 4 , 4 1
                                        // Umrechnungen zum Devisenkurs 1,083000
                                        //
                                        // Zu Ihren Lasten vor Steuern: EUR - 9 . 6 4 8 , 1 7 USD - 1 0 . 4 4 7 , 0 4
                                        // Steuerbemessungsgrundlage EUR 0 , 0 0
                                        // abgeführte Steuern EUR 0 , 0 0 USD 0 , 0 0
                                        // Zu Ihren Lasten nach Steuern: USD - 1 0 . 4 4 7 , 0 4
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "fxTaxes", "baseCurrency", "taxes", "exchangeRate") //
                                                        .match("^Zu Ihren (Gunsten|Lasten) vor Steuern:[\\s]{1,}[A-Z]{3}[\\-\\s]{1,}[\\.,\\d\\s]+[\\s]{1,}[A-Z]{3}[\\-\\s]{1,}[\\.,\\d\\s]+.*$") //
                                                        .match("^Steuerbemessungsgrundlage[\\s]{1,}([\\(\\s\\d\\)]+)?[A-Z]{3}[\\s]{1,}[\\.,\\d\\s]+.*$") //
                                                        .match("^abgef.hrte Steuern[\\s]{1,}(?<termCurrency>[A-Z]{3})[\\-\\s]{1,}(?<fxTaxes>[\\.,\\d\\s]+)[\\s]{1,}(?<baseCurrency>[A-Z]{3})[\\-\\s]{1,}(?<taxes>[\\.,\\d\\s]+).*$") //
                                                        .match("^Umrechnungen zum Devisenkurs[\\s]{1,}(?<exchangeRate>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var gross = Money.of(rate.getBaseCurrency(), asAmount(stripBlanks(v.get("taxes"))));
                                                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(stripBlanks(v.get("fxTaxes"))));

                                                            if (!t.getSecurity().getCurrencyCode().equals(t.getMonetaryAmount().getCurrencyCode()))
                                                                t.addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, fxGross, rate.getRate()));

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // 01111 City Referenz-Nummer: 0W7U3RJX11111111
                        // 3  5  5  1  0   N   u  t z  b  a  c h                  R   e  f e  r e  n  z  - N   u mmer:    1 C   7  Z  B   W   0  N Q14714E9
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*R[\\s]*e[\\s]*f[\\s]*e[\\s]*r[\\s]*e[\\s]*n[\\s]*z[\\s]*\\-[\\s]*N[\\s]*u[\\s]*m[\\s]*m[\\s]*e[\\s]*r[\\s]*:[\\s]{1,}(?<note>.*)$") //
                        .assign((t, v) -> t.setNote("Ref.-Nr.: " + trim(stripBlanks(v.get("note")))))

                        .wrap((t, ctx) -> {
                            var item = new TransactionItem(t);

                            // Store attribute in item data map
                            item.setData(ATTRIBUTE_GROSS_TAXES_TREATMENT, ctx.get(ATTRIBUTE_GROSS_TAXES_TREATMENT));

                            if (t.getCurrencyCode() != null && t.getAmount() == 0)
                                item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupported);

                            return item;
                        });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction

                        // @formatter:off
                        // f r emde Spesen USD 0 , 2 4 -
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.*Spesen (?<currency>[A-Z]{3}) (?<fee>[\\.,\\d\\s]+)( \\-)?.*$") //
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // F r e m d e S p e s e n : USD 1 , 6 5 -
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.*S[\\s]*p[\\s]*e[\\s]*s[\\s]*e[\\s]*n[\\s]*:[\\s]{1,}(?<currency>[A-Z]{3}) (?<fee>[\\.,\\d\\s]+( \\-)?).*$") //
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // P r o v i s i o n : EUR 5 , 6 1
                        // 0 , 2 5 0 0 0 % P r o v i s i o n : EUR 4 9 , 6 3
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.*P[\\s]*r[\\s]*o[\\s]*v[\\s]*i[\\s]*s[\\s]*i[\\s]*o[\\s]*n[\\s]*:[\\s]{1,}(?<currency>[A-Z]{3}) (?<fee>[\\.,\\d\\s]+( \\-)?).*$") //
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // 0 , 2 5 0 0 0 % G e s a m t p r o v i s i o n : EUR 1 1 9 , 3 8
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.* [\\.,\\d\\s]+[\\s]*%[\\s]*G[\\s]*e[\\s]*s[\\s]*a[\\s]*m[\\s]*t[\\s]*p[\\s]*r[\\s]*o[\\s]*v[\\s]*i[\\s]*s[\\s]*i[\\s]*o[\\s]*n[\\s]*:[\\s]{1,}(?<currency>[A-Z]{3}) (?<fee>[\\.,\\d\\s]+( \\-)?).*$") //
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // S o c k e l b e t r a g : EUR 4 , 9 0
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^S[\\s]*o[\\s]*c[\\s]*k[\\s]*e[\\s]*l[\\s]*b[\\s]*e[\\s]*t[\\s]*r[\\s]*a[\\s]*g[\\s]*:[\\s]{1,}(?<currency>[A-Z]{3}) (?<fee>[\\.,\\d\\s]+( \\-)?).*$") //
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // U m s c h r e i b e e n t g e l t : EUR 0 , 6 0
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^U[\\s]*m[\\s]*s[\\s]*c[\\s]*h[\\s]*r[\\s]*e[\\s]*i[\\s]*b[\\s]*e[\\s]*e[\\s]*n[\\s]*t[\\s]*g[\\s]*e[\\s]*l[\\s]*t[\\s]*:[\\s]{1,}(?<currency>[A-Z]{3}) (?<fee>[\\.,\\d\\s]+( \\-)?).*$") //
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // T r a n s a k t i o n s e n t g e l t : EUR 4 , 6 1 -
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^T[\\s]*r[\\s]*a[\\s]*n[\\s]*s[\\s]*a[\\s]*k[\\s]*t[\\s]*i[\\s]*o[\\s]*n[\\s]*s[\\s]*e[\\s]*n[\\s]*t[\\s]*g[\\s]*e[\\s]*l[\\s]*t[\\s]*:[\\s]{1,}(?<currency>[A-Z]{3}) (?<fee>[\\.,\\d\\s]+( \\-)?.*$)") //
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // X e t r a - E n t g e l t : EUR 2 , 7 3
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^X[\\s]*e[\\s]*t[\\s]*r[\\s]*a[\\s]*\\-[\\s]*E[\\s]*n[\\s]*t[\\s]*g[\\s]*e[\\s]*l[\\s]*t[\\s]*:[\\s]{1,}(?<currency>[A-Z]{3}) (?<fee>[\\.,\\d\\s]+)( \\-)?.*$") //
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // M i n i m u m p r o v i s i o n : EUR 9 , 9 0 -
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^M[\\s]*i[\\s]*n[\\s]*i[\\s]*m[\\s]*u[\\s]*m[\\s]*p[\\s]*r[\\s]*o[\\s]*v[\\s]*i[\\s]*s[\\s]*i[\\s]*o[\\s]*n[\\s]*:[\\s]{1,}(?<currency>[A-Z]{3}) (?<fee>[\\.,\\d\\s]+)( \\-)?.*$") //
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // V a r i a b l e B ö r s e n s p e s e n : EUR 3 , 2 5 -
                        // 0 , 2 5 0 0 0 % G e s a m t p r o v i s i o n : EUR 1 1 9 , 3 8
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^.*V[\\s]*a[\\s]*r[\\s]*i[\\s]*a[\\s]*b[\\s]*l[\\s]*e[\\s]*B[\\s]*.[\\s]*r[\\s]*s[\\s]*e[\\s]*n[\\s]*s[\\s]*p[\\s]*e[\\s]*s[\\s]*e[\\s]*n[\\s]*:[\\s]{1,}(?<currency>[A-Z]{3}) (?<fee>[\\.,\\d\\s]+)( \\-)?.*$") //
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // C l e a r s t r e a m - E n t g e l t : EUR 1 , 9 8 -
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^C[\\s]*l[\\s]*e[\\s]*a[\\s]*r[\\s]*s[\\s]*t[\\s]*r[\\s]*e[\\s]*a[\\s]*m[\\s]*\\-[\\s]*E[\\s]*n[\\s]*t[\\s]*g[\\s]*e[\\s]*l[\\s]*t[\\s]*:[\\s]{1,}(?<currency>[A-Z]{3}) (?<fee>[\\.,\\d\\s]+)( \\-)?.*$") //
                        .assign((t, v) -> {
                            v.put("fee", stripBlanks(v.get("fee")));
                            processFeeEntries(t, v, type);
                        })

                        // @formatter:off
                        // K u r s w e r t : EUR 4 9 , 9 2
                        // I n dem K u r s w e r t s i n d 2 , 4 3 9 0 2 % A u s g a b e a u f s c h l a g d e r B a n k e n t h a l t e n .
                        // @formatter:on
                        .section("currency", "amount", "percentageFee").optional() //
                        .match("^K[\\s]*u[\\s]*r[\\s]*s[\\s]*w[\\s]*e[\\s]*r[\\s]*t[\\s]*:[\\s]{1,}(?<currency>[A-Z]{3}) (?<amount>[\\.,\\d\\s]+)( \\-)?.*$") //
                        .match("^I[\\s]*n[\\s]*d[\\s]*e[\\s]*m[\\s]*[\\s]*K[\\s]*u[\\s]*r[\\s]*s[\\s]*w[\\s]*e[\\s]*r[\\s]*t[\\s]*s[\\s]*i[\\s]*n[\\s]*d[\\s]*(?<percentageFee>[\\.,\\d\\s]+)[\\s]*%[\\s]*A[\\s]*u[\\s]*s[\\s]*g[\\s]*a[\\s]*b[\\s]*e[\\s]*a[\\s]*u[\\s]*f[\\s]*s[\\s]*c[\\s]*h[\\s]*l[\\s]*a[\\s]*g[\\s]*d[\\s]*e[\\s]*r[\\s]*B[\\s]*a[\\s]*n[\\s]*k[\\s]*e[\\s]*n[\\s]*t[\\s]*h[\\s]*a[\\s]*l[\\s]*t[\\s]*e[\\s]*n.*$") //
                        .assign((t, v) -> {
                            var percentageFee = asBigDecimal(stripBlanks(v.get("percentageFee")));
                            var amount = asBigDecimal(stripBlanks(v.get("amount")));

                            if (percentageFee.compareTo(BigDecimal.ZERO) != 0)
                            {
                                // @formatter:off
                                // feeAmount = (amount / (1 + percentageFee / 100)) * (percentageFee / 100)
                                // @formatter:on
                                var fxFee = amount //
                                                .divide(percentageFee.divide(BigDecimal.valueOf(100)) //
                                                                .add(BigDecimal.ONE), Values.MC) //
                                                .multiply(percentageFee, Values.MC);

                                // fee = fee - discount
                                var fee = Money.of(asCurrencyCode(v.get("currency")), fxFee.setScale(0, Values.MC.getRoundingMode()).longValue());

                                // Assign the fee to the current context
                                checkAndSetFee(fee, t, type.getCurrentContext());
                            }
                        });
    }

    /**
     * @formatter:off
     * This method performs post-processing on a list transaction items, categorizing and
     * modifying them based on their types and associations. It follows several steps:
     *
     * 1. Filters the input list to isolate taxes treatment transactions, purchase/sale transactions, and dividend transactions.
     * 2. Matches purchase/sale transactions with their corresponding taxes treatment and dividend transactions with their corresponding taxes treatment.
     * 3. Adjusts purchase/sale transactions by adding/subtracting tax amounts, adding tax units, combining source information, appending tax-related notes,
     *    and removing taxes treatment's from the list of items.
     * 4. Adjusts dividend transactions by updating the gross amount if necessary, adding/subtracting tax amounts, adding tax units,
     *    combining source information, appending taxes treatment notes, and removing taxes treatment's from the list of items.
     *
     * The goal of this method is to process transactions and ensure that taxes treatment is accurately reflected
     * in purchase/sale and dividend transactions, making the transaction's more comprehensive and accurate.
     *
     * @param items The list of transaction items to be processed.
     * @return A modified list of transaction items after post-processing.
     * @formatter:on
     */
    @Override
    public void postProcessing(List<Item> items)
    {
        // Filter transactions by taxes treatment's
        var taxesTreatmentList = items.stream() //
                        .filter(TransactionItem.class::isInstance) //
                        .filter(i -> i.getSubject() instanceof AccountTransaction) //
                        .filter(i -> { //
                            var type = ((AccountTransaction) i.getSubject()).getType(); //
                            return type == AccountTransaction.Type.TAXES || type == AccountTransaction.Type.TAX_REFUND; //
                        }) //
                        .toList();

        // Filter transactions by buySell transactions
        var purchaseSaleTransactionList = items.stream() //
                        .filter(BuySellEntryItem.class::isInstance) //
                        .filter(i -> i.getSubject() instanceof BuySellEntry) //
                        .filter(i -> { //
                            var type = ((BuySellEntry) i.getSubject()).getPortfolioTransaction().getType(); //
                            return PortfolioTransaction.Type.SELL.equals(type)
                                            || PortfolioTransaction.Type.BUY.equals(type); //
                        }) //
                        .toList();

        // Filter transactions by dividend transactions
        var dividendTransactionList = items.stream() //
                        .filter(TransactionItem.class::isInstance) //
                        .filter(i -> i.getSubject() instanceof AccountTransaction) //
                        .filter(i -> AccountTransaction.Type.DIVIDENDS //
                                        .equals((((AccountTransaction) i.getSubject()).getType()))) //
                        .toList();

        var purchaseSaleListTaxPairs = matchTransactionPair(purchaseSaleTransactionList, taxesTreatmentList);
        var dividendTaxPairs = matchTransactionPair(dividendTransactionList, taxesTreatmentList);

        applyMissingCurrencyConversionBetweenTaxesAndPurchaseSale(purchaseSaleListTaxPairs);

        // @formatter:off
        // This loop iterates through a list of purchase/sale and tax pairs and processes them.
        //
        // For each pair, it adds/subtracts the tax amount from the purchase/sale transaction's total amount,
        // adds the tax as a tax unit to the purchase/sale transaction, combines source information if needed,
        // appends taxes treatment notes to the purchase/sale transaction, and removes the tax treatment from the 'items' list.
        //
        // It performs these operations when a valid tax transaction is found.
        // @formatter:on
        for (TransactionTaxesPair pair : purchaseSaleListTaxPairs)
        {
            var purchaseSaleTransaction = (BuySellEntry) pair.transaction.getSubject();
            var taxesTransaction = pair.tax() != null ? (AccountTransaction) pair.tax().getSubject() : null;

            if (taxesTransaction != null && taxesTransaction.getType() == AccountTransaction.Type.TAXES)
            {
                if (purchaseSaleTransaction.getPortfolioTransaction().getType().isLiquidation())
                {
                    purchaseSaleTransaction.setMonetaryAmount(purchaseSaleTransaction.getPortfolioTransaction()
                                    .getMonetaryAmount().subtract(taxesTransaction.getMonetaryAmount()));
                }
                else
                {
                    purchaseSaleTransaction.setMonetaryAmount(purchaseSaleTransaction.getPortfolioTransaction()
                                    .getMonetaryAmount().add(taxesTransaction.getMonetaryAmount()));
                }

                purchaseSaleTransaction.getPortfolioTransaction()
                                .addUnit(new Unit(Unit.Type.TAX, taxesTransaction.getMonetaryAmount()));

                purchaseSaleTransaction.setSource(
                                concatenate(purchaseSaleTransaction.getSource(), taxesTransaction.getSource(), "; "));

                purchaseSaleTransaction.setNote(
                                concatenate(purchaseSaleTransaction.getNote(), taxesTransaction.getNote(), " | "));

                ExtractorUtils.fixGrossValueBuySell().accept(purchaseSaleTransaction);

                items.remove(pair.tax());
            }
        }

        // @formatter:off
         // This loop processes a list of dividend and tax pairs, adjusting the gross amount of dividend transactions as needed.
         //
         // For each pair, it checks if there is a corresponding tax transaction. If present, it considers the gross taxes treatment,
         // adjusting the gross amount of the dividend transaction if necessary. If taxes amount is zero and gross taxes treatment
         // is less than the dividend amount, it adjusts the taxes and sets the dividend amount to the gross taxes treatment.
         // If taxes amount is not zero, it sets the dividend amount to the gross taxes treatment and fixes the gross value.
         //
         // If there is no tax transaction, it simply fixes the gross value of the dividend transaction.
         // @formatter:on
        for (TransactionTaxesPair pair : dividendTaxPairs)
        {
            var dividendTransaction = (AccountTransaction) pair.transaction().getSubject();
            var taxesTransaction = pair.tax() != null ? (AccountTransaction) pair.tax().getSubject() : null;

            if (taxesTransaction != null)
            {
                var grossTaxesTreatment = (Money) pair.tax().getData(ATTRIBUTE_GROSS_TAXES_TREATMENT);

                if (grossTaxesTreatment != null)
                {
                    var dividendAmount = dividendTransaction.getMonetaryAmount();
                    var taxesAmount = taxesTransaction.getMonetaryAmount();

                    if (taxesAmount.isZero() && grossTaxesTreatment.isLessThan(dividendAmount))
                    {
                        var adjustedTaxes = dividendAmount.subtract(grossTaxesTreatment);
                        dividendTransaction.addUnit(new Unit(Unit.Type.TAX, adjustedTaxes));
                        dividendTransaction.setMonetaryAmount(grossTaxesTreatment);
                    }
                    else
                    {
                        dividendTransaction.setMonetaryAmount(grossTaxesTreatment);
                    }
                }

                ExtractorUtils.fixGrossValue().accept(dividendTransaction);

                dividendTransaction.setMonetaryAmount(dividendTransaction.getMonetaryAmount() //
                                .subtract(taxesTransaction.getMonetaryAmount()));

                dividendTransaction.addUnit(new Unit(Unit.Type.TAX, taxesTransaction.getMonetaryAmount()));

                dividendTransaction.setSource(
                                concatenate(dividendTransaction.getSource(), taxesTransaction.getSource(), "; "));

                dividendTransaction
                                .setNote(concatenate(dividendTransaction.getNote(), taxesTransaction.getNote(), " | "));

                ExtractorUtils.fixGrossValue().accept(dividendTransaction);

                items.remove(pair.tax());
            }
            else
            {
                ExtractorUtils.fixGrossValue().accept(dividendTransaction);
            }
        }
    }

    /**
     * @formatter:off
     * Matches transactions and taxes treatment's, ensuring unique pairs based on date and security.
     *
     * This method matches transactions and taxes treatment's by creating a Pair consisting of the transaction's
     * date and security. It uses a Set called 'keys' to prevent duplicates based on these Pair keys,
     * ensuring that the same combination of date and security is not processed multiple times.
     * Duplicate transactions for the same security on the same day are avoided.
     *
     * @param transactionList      A list of transactions to be matched.
     * @param taxesTreatmentList   A list of taxes treatment's to be considered for matching.
     * @return A collection of TransactionTaxesPair objects representing matched transactions and taxes treatment's.
     * @formatter:on
     */
    private Collection<TransactionTaxesPair> matchTransactionPair(List<Item> transactionList,
                    List<Item> taxesTreatmentList)
    {
        // Use a Set to prevent duplicates
        Set<Pair<LocalDate, Security>> keys = new HashSet<>();
        Map<Pair<LocalDate, Security>, TransactionTaxesPair> pairs = new HashMap<>();

        // Match identified transactions and taxes treatment's
        transactionList.forEach( //
                        transaction -> {
                            var key = new Pair<>(transaction.getDate().toLocalDate(), transaction.getSecurity());

                            // Prevent duplicates
                            if (keys.add(key))
                                pairs.put(key, new TransactionTaxesPair(transaction, null));
                        } //
        );

        // Iterate through the list of taxes treatment's to match them with
        // transactions
        taxesTreatmentList.forEach( //
                        tax -> {
                            // Check if the taxes treatment has a security
                            if (tax.getSecurity() == null)
                                return;

                            // Create a key based on the taxes treatment date
                            // and security
                            var key = new Pair<>(tax.getDate().toLocalDate(), tax.getSecurity());

                            // Retrieve the TransactionTaxesPair associated with
                            // this key, if it exists
                            var pair = pairs.get(key);

                            // Skip if no transaction is found or if a taxes
                            // treatment already exists
                            if (pair != null && pair.tax() == null)
                                pairs.put(key, new TransactionTaxesPair(pair.transaction(), tax));
                        } //
        );

        return pairs.values();
    }

    /**
     * @formatter:off
     * Resolves missing currency conversions between taxes and purchase/sale transactions based on existing exchange rates.
     *
     * For each TransactionTaxesPair, this method checks for currency mismatches between:
     * - the monetary amount and security currency of the taxes transaction, and
     * - the monetary amount and security currency of the purchase/sale transaction.
     *
     * If either side shows a mismatch, and if the opposite side contains a valid exchange rate,
     * a corresponding GROSS_VALUE unit with the appropriate FX conversion will be added to ensure consistency.
     *
     * This helps ensure that both tax and purchase/sale transactions carry correct currency conversion data
     * when working across multi-currency portfolios.
     *
     * @param purchaseSaleTaxPairs A collection of TransactionTaxesPair objects containing associated taxes and purchase/sale transactions.
     * @formatter:on
     */
    private void applyMissingCurrencyConversionBetweenTaxesAndPurchaseSale(
                    Collection<TransactionTaxesPair> purchaseSaleTaxPairs)
    {
        purchaseSaleTaxPairs.forEach(pair -> {
            if (pair.tax != null && pair.transaction != null)
            {
                var tax = (AccountTransaction) pair.tax.getSubject();
                var purchaseSale = (BuySellEntry) pair.transaction.getSubject();
                var purchaseSalePortfolioTx = purchaseSale.getPortfolioTransaction();

                // Determine currency of monetary amounts and associated
                // securities
                var taxCurrency = tax.getMonetaryAmount().getCurrencyCode();
                var taxSecurityCurrency = tax.getSecurity().getCurrencyCode();

                var purchaseSaleCurrency = purchaseSalePortfolioTx.getMonetaryAmount().getCurrencyCode();
                var purchaseSaleSecurityCurrency = purchaseSalePortfolioTx.getSecurity().getCurrencyCode();

                var taxHasMismatch = !taxCurrency.equals(taxSecurityCurrency);
                var purchaseSaleHasMismatch = !purchaseSaleCurrency.equals(purchaseSaleSecurityCurrency);

                // Proceed only if at least one of the transactions has a
                // currency mismatch
                if (taxHasMismatch || purchaseSaleHasMismatch)
                {
                    var taxAmount = tax.getMonetaryAmount();

                    var taxGrossValue = tax.getUnit(Unit.Type.GROSS_VALUE);
                    var purchaseSaleGrossValue = purchaseSalePortfolioTx.getUnit(Unit.Type.GROSS_VALUE);

                    // If the taxes transaction contains a usable exchange rate,
                    // apply the conversion to the sales transaction. Otherwise,
                    // if the purchase/sales transaction contains a usable
                    // exchange rate,
                    // apply the conversion to the taxes transaction.
                    if (taxGrossValue.isPresent() && taxGrossValue.get().getExchangeRate() != null)
                    {
                        var rate = new ExtrExchangeRate(taxGrossValue.get().getExchangeRate(),
                                        purchaseSaleSecurityCurrency, taxCurrency);
                        var fxGross = rate.convert(purchaseSaleSecurityCurrency,
                                        purchaseSalePortfolioTx.getMonetaryAmount());

                        purchaseSalePortfolioTx.addUnit(new Unit(Unit.Type.GROSS_VALUE,
                                        purchaseSalePortfolioTx.getMonetaryAmount(), fxGross, rate.getRate()));
                    }
                    else if (purchaseSaleGrossValue.isPresent()
                                    && purchaseSaleGrossValue.get().getExchangeRate() != null)
                    {
                        var rate = new ExtrExchangeRate(purchaseSaleGrossValue.get().getExchangeRate(),
                                        purchaseSaleSecurityCurrency, taxCurrency);
                        var fxGross = rate.convert(purchaseSaleSecurityCurrency, taxAmount);

                        tax.addUnit(new Unit(Unit.Type.GROSS_VALUE, taxAmount, fxGross, rate.getRate()));
                    }
                }
            }
        });
    }
}
