package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetTax;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

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
public class SaxoBankPDFExtractor extends AbstractPDFExtractor
{
    public SaxoBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Saxo Bank");

        addBuySellTransaction();
        addDividendeTransaction();
        addInterestTransaction();
        addDepositTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Saxo Bank";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("Trade details,", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Währung: CHF 05-Dez-2024 - 05-Dez-2024
                                        // / Phone No.: +45 39 77 40 00 / Fax No.: +45 39 77 42 00 / Email: info@saxobank.com Currency: USD 09-Apr-2025 - 09-Apr-2025
                                        // Currency: CHF 05-Aug-2025 - 05-Aug-2025
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^.*(W.hrung|Currency): (?<currency>[A-Z]{3}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Instrument.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Is type --> "Verkauf" change from BUY to SELL
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type") //
                                                        .match("^K\\/V (?<type>(Kauf|Buy|Verkauf)) Menge (\\-)?[\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if ("Verkauf".equals(v.get("type"))) //
                                                                t.setType(PortfolioTransaction.Type.SELL);
                                                        }),
                                        // @formatter:off
                                        // Is type --> "-" change from BUY to SELL
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type") //
                                                        .match("^.*(Quantity|Menge)(?<type>([\\s|\\-]{1,}))[\\.,\\d]+$") //
                                                        .assign((t, v) -> {
                                                            if ("-".equals(trim(v.get("type"))))
                                                                t.setType(PortfolioTransaction.Type.SELL);
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Instrument Republic of France 3.75% 25 May 2056, EUR Trade time 20-Jun-2025 08:45:38
                                        // ISIN FR001400XJJ3 Value Date 24-Jun-2025
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^Instrument (?<name>.*), (?<currency>[A-Z]{3}) Trade time.*$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Value.*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Instrument iShares Core MSCI World UCITS ETF Handelszeit 05-Dez-2024 11:21:27
                                        // ISIN IE00B4L5Y983 Valuta 09-Dez-2024
                                        // Symbol SWDA:xswx Order-ID 5236807355
                                        // Ordertyp Marktorder Gehandelter Wert -5.480,43 USD
                                        //
                                        // Instrument iShares Core MSCI World UCITS ETF Handelszeit 05-Dez-2024 11:21:27
                                        // ISIN IE00B4L5Y983 Valuta 09-Dez-2024
                                        // Symbol SWDA:xswx Order-ID 5236807355
                                        // Ordertyp Marktorder Preis 111,8455 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "tickerSymbol", "currency") //
                                                        .match("^Instrument (?<name>.*) Handelszeit.*$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Valuta.*$") //
                                                        .match("^Symbol (?<tickerSymbol>[A-Z0-9\\._-]{1,10}(?:\\.[A-Z]{1,4})?):.*$") //
                                                        .match("^Ordertyp .* (\\-)?[\\.,'\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Instrument iShares Core MSCI World UCITS ETF Handelszeit 05-Aug-2025 12:47:54
                                        // ISIN IE00B4L5Y983 Valuta 07-Aug-2025
                                        // Symbol SWDA:xswx Order-ID 5311230162
                                        // Hauptbörse SIX Swiss Exchange (ETFs) Gehandelter Wert -2.394,19 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "tickerSymbol", "currency") //
                                                        .match("^Instrument (?<name>.*) Handelszeit.*$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Valuta.*$") //
                                                        .match("^Symbol (?<tickerSymbol>[A-Z0-9\\._-]{1,10}(?:\\.[A-Z]{1,4})?):.*$") //
                                                        .match("^.* Gehandelter Wert (\\-)?[\\.,'\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Berichtszeitraum
                                        // 05-Jun-2025 bis 05-Jun-2025
                                        // ETF
                                        // iShares Core Euro STOXX 50 (DE) UCITS
                                        // Instrument Handelszeit 05-Jun-2025 12:56:00
                                        // ETF
                                        // ISIN DE0005933956 Valuta 10-Jun-2025
                                        // Symbol DJSXE:xswx Order-ID 8482757546
                                        // Hauptbörse SIX Swiss Exchange (ETFs) Gehandelter Wert -16'997.84 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "tickerSymbol", "currency") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Valuta.*$") //
                                                        .match("^Symbol (?<tickerSymbol>[A-Z0-9\\._-]{1,10}(?:\\.[A-Z]{1,4})?):.*$") //
                                                        .match("^.* Gehandelter Wert (\\-)?[\\.,'\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Instrument Virtus Infracap US Preferred Stock ETF Trade time 09-Apr-2025 19:47:57
                                        // ISIN US26923G8226 Value Date 10-Apr-2025
                                        // Symbol PFFA:arcx Order ID 5276831204
                                        // Venue Exchange Traded Value -980,98 USD
                                        //
                                        // Instrument iShares MSCI ACWI USD Acc UCITS ETF Trade time 05-Aug-2025 12:46:23
                                        // ISIN IE00B6R52259 Value Date 07-Aug-2025
                                        // Symbol Symbol SSAC_CHF:xswx Order ID 5311227095
                                        // Venue Exchange Price 80,4666 CHF
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "tickerSymbol", "currency") //
                                                        .match("^Instrument (?<name>.*) Trade time.*$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Value.*$") //
                                                        .match("^Symbol (?<tickerSymbol>[A-Z0-9\\._-]{1,10}(?:\\.[A-Z]{1,4})?):.*$") //
                                                        .match("^.*Traded Value (\\-)?[\\.,'\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off

                                        // ISIN CH0016999846 Value Date 07-Aug-2025
                                        // Symbol CSBGC7:xswx Order ID 5311259140
                                        // Venue Exchange Price 75,19880769 CHF
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "tickerSymbol", "currency") ///
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) Value.*$") //
                                                        .match("^Symbol (?<tickerSymbol>[A-Z0-9\\._-]{1,10}(?:\\.[A-Z]{1,4})?):.*$") //
                                                        .match("^.*Traded Value (\\-)?[\\.,'\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Eröffnung/Schluss To-Open Menge 49,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Er.ffnung\\/Schluss To-Open Menge (?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // K/V Kauf Menge 49,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^K\\/V (Kauf|Verkauf) Menge (\\-)?(?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Order Type Limit Order Quantity 10.000,00
                                        // Bond Traded Value
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.*Quantity (?<shares>[\\.,\\d]+)$") //
                                                        .find("Bond Traded Value") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            var shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }),
                                        // @formatter:off
                                        // B/S Buy Quantity 49,00
                                        // Order Type Limit Order Quantity 49,00
                                        // Ordertyp Marktorder Menge 306.00
                                        // Ordertyp Marktorder Menge -15,00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.*(Quantity|Menge) (\\-)?(?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Instrument iShares Core MSCI World UCITS ETF Handelszeit 05-Dez-2024 11:21:27
                                        // Instrument Virtus Infracap US Preferred Stock ETF Trade time 09-Apr-2025 19:47:57
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^.*(Handelszeit|Trade time) (?<date>[\\d]{2}\\-[\\w]+\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Nettobetrag - - - - - -12,14 -4.869,43
                                        // Nettobetrag - - - - - 0,00 -3.057,58
                                        // Net Amount - - - - - 0,00 -981,98
                                        // Nettobetrag - - - - - -39.83 -15'972.49
                                        // Nettobetrag - - - - - 0,00 1.130,17
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount") //
                                                        .documentContext("currency") //
                                                        .match("^(Nettobetrag|Net Amount) .* \\-[\\s]*[\\.,'\\d]+ (\\-)?(?<amount>[\\.,'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // ID USD CHF CHF
                                        // Aktienbetrag 39679982249 05-Dez-2024 09-Dez-2024 -5.480,43 0,887182 -12,12 -4.862,14
                                        // Nettobetrag - - - - - -12,14 -4.869,43
                                        //
                                        // ID CHF CHF CHF
                                        // Aktienbetrag 40112732021 20-Dez-2024 24-Dez-2024 -3.050,00 1,000000 0,00 -3.050,00
                                        // Nettobetrag - - - - - 0,00 -3.057,58
                                        //
                                        // ID USD USD USD
                                        // Share Amount 43080952371 09-Apr-2025 10-Apr-2025 -980,98 1,000000 0,00 -980,98
                                        // Net Amount - - - - - 0,00 -981,98
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "termCurrency", "exchangeRate", "gross") //
                                                        .match("^ID (?<baseCurrency>[A-Z]{3}) [A-Z]{3} (?<termCurrency>[A-Z]{3})$") //
                                                        .match("^(Aktienbetrag|Share Amount) .* (?<exchangeRate>[\\.,'\\d]+) (\\-)?[\\.,'\\d]+ (\\-)?[\\.,'\\d]+$") //
                                                        .match("^(Nettobetrag|Net Amount) .* (\\-)?[\\s]*[\\.,'\\d]+ (\\-)?(?<gross>[\\.,'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                                                            var fxGross = rate.convert(rate.getBaseCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Symbol SWDA:xswx Order-ID 5236807355
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*(?<note>Order\\-ID [\\d]+).*$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))),
                                        // @formatter:off
                                        // Symbol PFFA:arcx Order ID 5276831204
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*(?<note>Order ID [\\d]+).*$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Handelsplatz Exchange Trade-ID 6093088529
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*(?<note>Trade\\-ID [\\d]+).*$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | "))),
                                        // @formatter:off
                                        // Exchange Description KNIGHT LINK (KNLI) Trade ID 6236413100
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*(?<note>Trade ID [\\d]+).*$") //
                                                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), v.get("note"), " | "))))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Bond Accrued
                                        // 45126758574 20-Jun-2025 24-Jun-2025 -30,82 1,000000 0,00 -30,82
                                        // Interest
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note1", "note2") //
                                                        .match("^(?<note1>Bond Accrued).*$") //
                                                        .match("[\\d]+ [\\d]{2}\\-[\\w]+\\-[\\d]{4} [\\d]{2}\\-[\\w]+\\-[\\d]{4} \\-(?<note2>[\\.,'\\d]+) [\\.,'\\d]+ [\\.,'\\d]+ \\-[\\.,'\\d]+$") //
                                                        .find("Interest.*") //
                                                        .assign((t, v) -> {

                                                            var note = v.get("note1") + " " + trim(v.get("note2")) + " " + t.getPortfolioTransaction().getCurrencyCode();
                                                            t.setNote(concatenate(t.getNote(), note, " | "));
                                                        }))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final var type = new DocumentType("Corporate Action Detail Report", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Währung: CHF 05-Dez-2024 - 05-Dez-2024
                                        // / Phone No.: +45 39 77 40 00 / Fax No.: +45 39 77 42 00 / Email: info@saxobank.com Currency: USD 09-Apr-2025 - 09-Apr-2025
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^.*(W.hrung|Currency): (?<currency>[A-Z]{3}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Event (Dividend|Cash dividend|Bardividende).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Description BlackRock Taxable Municipal Bond Trust Dividend per share 0,09 USD
                                        // Symbol BBN:xnys Conversion Rate 1,000000
                                        // ISIN US09248X1000 Corporate Actions - Withholding Tax -8,42 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "tickerSymbol", "isin") //
                                                        .match("^Description (?<name>.*) Dividend per share [\\.,'\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .match("^Symbol (?<tickerSymbol>[A-Z0-9\\._-]{1,10}(?:\\.[A-Z]{1,4})?):.*$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Description iShares (CH) Swiss Dividend ETF Dividende pro Aktie 0.26 CHF
                                        // Symbol CHDVD:xswx Umrechnungskurs 1.000000
                                        // ISIN CH0237935637 Kapitalmaßnahmen - Bardividenden 19.50 CHF
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "tickerSymbol", "isin") //
                                                        .match("^Description (?<name>.*) Dividende pro Aktie [\\.,'\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .match("^Symbol (?<tickerSymbol>[A-Z0-9\\._-]{1,10}(?:\\.[A-Z]{1,4})?):.*$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Event Dividend reinvestment Eligible quantity 604
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.*Eligible quantity (?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Event Bardividende Geeignete Menge 75
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.*Geeignete Menge (?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        // @formatter:off
                        // 43640515029 15-Apr-2025 15-Apr-2025 02-Apr-2025 30-Apr-2025 56,11 1,000000 56,11
                        // @formatter:on
                        .section("date") //
                        .match("^.*(?<date>[\\d]{2}\\-[\\w]+\\-[\\d]{4}) [\\.,'\\d]+ [\\.,'\\d]+ [\\.,'\\d]+$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Net Amount - - - - - 47,69 1,000000 47,69
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount") //
                                                        .documentContext("currency") //
                                                        .match("^Net Amount .* [\\.,'\\d]+ [\\.,'\\d]+ (?<amount>[\\.,'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Nettobetrag - - - - - 12.67 1.000000 12.67
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount") //
                                                        .documentContext("currency") //
                                                        .match("^Nettobetrag .* [\\.,'\\d]+ [\\.,'\\d]+ (?<amount>[\\.,'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))


                        .optionalOneOf( //
                                        // @formatter:off
                                        // Transaction description Event Id 9369584
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*(?<note>Event Id [\\d]+).*$") //
                                                        .assign((t, v) -> t.setNote(trim(v.get("note")))))

                        .wrap(TransactionItem::new);

                addTaxesSectionsTransaction(pdfTransaction, type);
                addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addInterestTransaction()
    {
        final var type = new DocumentType("Cash Amount Detail Report");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Event Interest.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Interest 43681798738 01-Mai-2025 3,32 USD 1,000000 3,32
                        // @formatter:on
                        .section("date") //
                        .match("^Interest .* (?<date>[\\d]{2}\\-[\\w]+\\-[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Net Amount - - 3,32 USD 1,000000 3,32
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Net Amount .* (?<currency>[A-Z]{3}) [\\.,'\\d]+ (?<amount>[\\.,'\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .wrap(TransactionItem::new);

                addTaxesSectionsTransaction(pdfTransaction, type);
                addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDepositTransaction()
    {
        var type = new DocumentType("Cash Transfer Detail Report");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Cash Transfer Detail Report$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Bargeldtransfer
                                        // CHF CHF
                                        // Einlage 39482097030 28-Nov-2024 28-Nov-2024 4.600,00 1,000000 0,00 4.600,00
                                        // Währung: CHF 28-Nov-2024 - 28-Nov-2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note", "date", "amount", "currency") //
                                                        .find("Bargeldtransfer") //
                                                        .match("^Einlage (?<note>[\\d]+) [\\d]{2}\\-[\\w]+\\-[\\d]{4} (?<date>[\\d]{2}\\-[\\w]+\\-[\\d]{4}) .* (?<amount>[\\.,'\\d]+)$") //
                                                        .match("^W.hrung: (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setNote(trim(v.get("note")));
                                                        }),
                                        // @formatter:off
                                        // Cash Transfer
                                        // EUR EUR
                                        // Deposit 45108148786 20-Jun-2025 20-Jun-2025 10,00 1,000000 0,00 10,00
                                        // / Phone No.: +45 39 77 40 00 / Fax No.: +45 39 77 42 00 / Email: info@saxobank.com Currency: EUR 20-Jun-2025 - 20-Jun-2025
                                        //
                                        // EUR EUR
                                        // Withdrawal 46769031349 18-Aug-2025 18-Aug-2025 -3.000,00 1,000000 0,00 -3.000,00
                                        // / Phone No.: +45 39 77 40 00 / Fax No.: +45 39 77 42 00 / Email: info@saxobank.com Currency: EUR 18-Aug-2025 - 18-Aug-2025
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type", "note", "date", "amount", "currency") //
                                                        .find("Cash Transfer") //
                                                        .match("^(?<type>(Deposit|Withdrawal)) (?<note>[\\d]+) [\\d]{2}\\-[\\w]+\\-[\\d]{4} (?<date>[\\d]{2}\\-[\\w]+\\-[\\d]{4}) .* (\\-)?(?<amount>[\\.,'\\d]+)$") //
                                                        .match("^.*Currency: (?<currency>[A-Z]{3}).*$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Is type is "Withdrawal" change from DEPOSIT to REMOVAL
                                                            // @formatter:on
                                                            if ("Withdrawal".equals(trim(v.get("type"))))
                                                                t.setType(AccountTransaction.Type.REMOVAL);

                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setNote(trim(v.get("note")));
                                                        }))

                        .wrap(TransactionItem::new);
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Kontoauszugsbericht", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Währung : CHF
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^W.hrung : (?<currency>[A-Z]{3}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 26-Nov-2024 26-Nov-2024 DEPOSIT (6980803089, 6083903733) 700,00 700,00
        // @formatter:on
        var depositBlock = new Block("^[\\d]{2}\\-[\\w]+\\-[\\d]{4} [\\d]{2}\\-[\\w]+\\-[\\d]{4} (DEPOSIT) .* [\\.,'\\d]+ [\\.,'\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\-[\\w]+\\-[\\d]{4} " //
                                        + "(?<date>[\\d]{2}\\-[\\w]+\\-[\\d]{4}) " //
                                        + "DEPOSIT .* " //
                                        + "(?<amount>[\\.,'\\d]+)" //
                                        + "[\\.,'\\d]+$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Stempelgebühr 39683058642 05-Dez-2024 09-Dez-2024 -8,22 0,887182 -0,02 -7,29
                        // Stempelgebühr 40118366990 20-Dez-2024 24-Dez-2024 -4,58 1,000000 0,00 -4,58
                        // @formatter:on
                        .section("currencyConversionFee", "tax").optional() //
                        .documentContext("currency") //
                        .match("^Stempelgeb.hr .* (\\-)?(?<currencyConversionFee>[\\.,'\\d]+) \\-(?<tax>[\\.,'\\d]+)$") //
                        .assign((t, v) -> {
                            var taxes = Money.of(v.get("currency"), asAmount(v.get("tax")));
                            var currencyConversionFee = Money.of(v.get("currency"), asAmount(v.get("currencyConversionFee")));

                            // Subtract currency conversion fee from taxes
                            taxes = taxes.subtract(currencyConversionFee);

                            checkAndSetTax(taxes, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // 45865563190 15-Jul-2025 16-Jul-2025 - 17-Jul-2025 -6.83 1.000000 -6.83
                        // Quellensteuer
                        // @formatter:on
                        .section("withHoldingTax").optional() //
                        .documentContext("currency") //
                        .match("^.*[\\.,'\\d]+ [\\.,'\\d]+ \\-(?<withHoldingTax>[\\.,'\\d]+)$") //
                        .match("^Quellensteuer$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // 43640515030 15-Apr-2025 15-Apr-2025 02-Apr-2025 30-Apr-2025 -8,42 1,000000 -8,42
                        // Withholding Tax
                        // @formatter:on
                        .section("withHoldingTax").optional() //
                        .documentContext("currency") //
                        .match("^.*[\\.,'\\d]+ [\\.,'\\d]+ \\-(?<withHoldingTax>[\\.,'\\d]+)$") //
                        .match("^Withholding Tax$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Provision 4555555555 07-Apr-2025 08-Apr-2025 -1,77 0,860862 -0,01 -1,52
                        // Provision 40107905938 20-Dez-2024 24-Dez-2024 -3,00 1,000000 0,00 -3,00
                        // Commission 43073716378 09-Apr-2025 10-Apr-2025 -1,00 1,000000 0,00 -1,00
                        // @formatter:on
                        .section("currencyConversionFee", "fee").optional() //
                        .documentContext("currency") //
                        .match("^(Provision|Commission) .* (\\-)?(?<currencyConversionFee>[\\.,'\\d]+) \\-(?<fee>[\\.,'\\d]+)$") //
                        .assign((t, v) -> {
                            var fees = Money.of(v.get("currency"), asAmount(v.get("fee")));
                            var currencyConversionFee = Money.of(v.get("currency"), asAmount(v.get("currencyConversionFee")));

                            // Add currency conversion fee from fees
                            fees = fees.add(currencyConversionFee);

                            checkAndSetFee(fees, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Bond Accrued
                        // 45126758574 20-Jun-2025 24-Jun-2025 -30,82 1,000000 0,00 -30,82
                        // Interest
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .find("Bond Accrued.*") //
                        .match("^[\\d]+ [\\d]{2}\\-[\\w]+\\-[\\d]{4} [\\d]{2}\\-[\\w]+\\-[\\d]{4} \\-(?<fee>[\\.,'\\d]+) [\\.,'\\d]+ [\\.,'\\d]+ \\-[\\.,'\\d]+$") //
                        .find("Interest.*") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Stempelgebühr 39683058642 05-Dez-2024 09-Dez-2024 -8,22 0,887182 -0,02 -7,29
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Stempelgeb.hr .* \\-(?<fee>[\\.,'\\d]+) \\-[\\.,'\\d]+$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Swiss Stamp Duty
                        // 46406463093 05-Aug-2025 07-Aug-2025 -1,69 1,000000 0,00 -1,69
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .find("Swiss Stamp Duty.*") //
                        .match("^[\\d]+ [\\d]{2}\\-[\\w]+\\-[\\d]{4} [\\d]{2}\\-[\\w]+\\-[\\d]{4} \\-(?<fee>[\\.,'\\d]+) [\\.,'\\d]+ [\\.,'\\d]+ \\-[\\.,'\\d]+$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Aktienbetrag 41308584615 05-Feb-2025 07-Feb-2025 -23.714,22 0,904466 -53,48 -21.448,69
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Aktienbetrag .* \\-(?<fee>[\\.,'\\d]+) \\-[\\.,'\\d]+$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Belgium Stock
                        // 47351983134 05-sep-2025 09-sep-2025 -0,10 1,000000 0,00 -0,10
                        // @formatter: on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .find(".*Stock.*") //
                        .match("^[\\d]+ [\\d]{2}\\-[\\w]+\\-[\\d]{4} [\\d]{2}\\-[\\w]+\\-[\\d]{4} \\-(?<fee>[\\.,'\\d]+) [\\.,'\\d]+ [\\.,'\\d]+ \\-[\\.,'\\d]+$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }


    @Override
    protected long asAmount(String value)
    {
        var language = "de";
        var country = "DE";

        var apostrophe = value.indexOf("\'");
        if (apostrophe >= 0)
        {
            language = "de";
            country = "CH";
        }
        else
        {
            var lastDot = value.lastIndexOf(".");
            var lastComma = value.lastIndexOf(",");

            // returns the greater of two int values
            if (Math.max(lastDot, lastComma) == lastDot)
            {
                language = "en";
                country = "US";
            }
        }

        return ExtractorUtils.convertToNumberLong(value, Values.Amount, language, country);
    }

    @Override
    protected long asShares(String value)
    {
        var language = "de";
        var country = "DE";

        var apostrophe = value.indexOf("\'");
        if (apostrophe >= 0)
        {
            language = "de";
            country = "CH";
        }
        else
        {
            var lastDot = value.lastIndexOf(".");
            var lastComma = value.lastIndexOf(",");

            // returns the greater of two int values
            if (Math.max(lastDot, lastComma) == lastDot)
            {
                language = "en";
                country = "US";
            }
        }

        return ExtractorUtils.convertToNumberLong(value, Values.Share, language, country);
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        var language = "de";
        var country = "DE";

        var apostrophe = value.indexOf("\'");
        if (apostrophe >= 0)
        {
            language = "de";
            country = "CH";
        }
        else
        {
            var lastDot = value.lastIndexOf(".");
            var lastComma = value.lastIndexOf(",");

            // returns the greater of two int values
            if (Math.max(lastDot, lastComma) == lastDot)
            {
                language = "en";
                country = "US";
            }
        }

        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, language, country);
    }
}
