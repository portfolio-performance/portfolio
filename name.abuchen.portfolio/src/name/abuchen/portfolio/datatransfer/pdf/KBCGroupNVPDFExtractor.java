package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

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
public class KBCGroupNVPDFExtractor extends AbstractPDFExtractor
{
    public KBCGroupNVPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("KBC BANK NV");

        addBuySellTransaction();
        addDividendeTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "KBC Group NV";
    }

    private void addBuySellTransaction()
    {
        var type = new DocumentType("Uw (Aankoop|Verkoop)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Borderel [\\d]+.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkoop" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Uw (?<type>(Aankoop|Verkoop)).*$") //
                        .assign((t, v) -> {
                            if ("Verkoop".equals(v.get("type"))) //
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Uw Aankoop Online van 15 PROSUS N.V. (AS) aan 57,2 EUR 858,00 EUR
                                        // Waardecode NL0013654783
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^.* van [\\.,\\d]+ (?<name>.*) aan [\\.,\\d]+ (?<currency>[A-Z]{3}) [\\.,\\d]+ [A-Z]{3}$") //
                                                        .match("^Waardecode (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Uw Verkoop Online van 1.070 ISHAR.III CORE EUR CORP BD UC ETF-D aan 127.005,21 EUR
                                        // 118,69646 EUR
                                        // Waardecode IE00B3F81R35
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^.* van [\\.,\\d]+ (?<name>.*) aan [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .match("^[\\.,\\d]+ [A-Z]{3}$") //
                                                        .match("^Waardecode (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Uw Verkoop Online van 94 IS CO S&P500 U.ETF USD(ACC-PTG.K aan 538,17 50.587,98 EUR
                                        // EUR
                                        // Waardecode IE00B5BMR087
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^.* van [\\.,\\d]+ (?<name>.*) aan [\\.,\\d]+ [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .match("^[A-Z]{3}$") //
                                                        .match("^Waardecode (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Uw Aankoop Online van EUR 40.000 GERMANY 21-26 0% 10/04 REGS aan 38.500,00 EUR
                                        // 96,25%
                                        // Waardecode DE0001141836
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^.* van (?<currency>[A-Z]{3}) [\\.,\\d]+ (?<name>.*) aan [\\.,\\d]+ [A-Z]{3}$") //
                                                        .match("^[\\.,\\d]+%$") //
                                                        .match("^Waardecode (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Uw Aankoop Online van EUR 40.000 EUROP UN 21-28 0% 04/10 REGS MTN 36.068,00 EUR
                                        // aan 90,17%
                                        // Waardecode EU000A3KWCF4
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^.* van (?<currency>[A-Z]{3}) [\\.,\\d]+ (?<name>.*) [\\.,\\d]+ [A-Z]{3}$") //
                                                        .match("^aan [\\.,\\d]+%$") //
                                                        .match("^Waardecode (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Uw Aankoop Online van EUR 40.000 SPAIN 21-28 0% 31/01 aan 91,68% 36.672,00 EUR
                                        // Waardecode ES0000012I08
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^.* van (?<currency>[A-Z]{3}) [\\.,\\d]+ (?<name>.*) aan [\\.,\\d]+% [\\.,\\d]+ [A-Z]{3}$") //
                                                        .match("^Waardecode (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Uw Aankoop Online van 15 PROSUS N.V. (AS) aan 57,2 EUR 858,00 EUR
                                        // Uw Verkoop Online van 1.070 ISHAR.III CORE EUR CORP BD UC ETF-D aan 127.005,21 EUR
                                        // Uw Verkoop Online van 94 IS CO S&P500 U.ETF USD(ACC-PTG.K aan 538,17 50.587,98 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* van (?<shares>[\\.,\\d]+) .* aan.*$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Uw Aankoop Online van EUR 40.000 SPAIN 21-28 0% 31/01 aan 91,68% 36.672,00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* van [A-Z]{3} (?<shares>[\\.,\\d]+) .* aan [\\.,\\d]+%.*$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            var shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }),
                                        // @formatter:off
                                        // Uw Aankoop Online van EUR 40.000 GERMANY 21-26 0% 10/04 REGS aan 38.500,00 EUR
                                        // 96,25%
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* van [A-Z]{3} (?<shares>[\\.,\\d]+) .* aan.*$") //
                                                        .match("^[\\.,\\d]+%$") //
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            var shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }),
                                        // @formatter:off
                                        // Uw Aankoop Online van EUR 40.000 EUROP UN 21-28 0% 04/10 REGS MTN 36.068,00 EUR
                                        // aan 90,17%
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^.* van [A-Z]{3} (?<shares>[\\.,\\d]+).*$") //
                                                        .match("^aan [\\.,\\d]+%$")
                                                        .assign((t, v) -> {
                                                            // @formatter:off
                                                            // Percentage quotation, workaround for bonds
                                                            // @formatter:on
                                                            var shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // 02/02/2022 14:50:02 Valuta 04/02/2022 Euronext A'dam
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) Valuta [\\d]{2}\\/[\\d]{2}\\/[\\d]{4}.*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Uitvoeringsdatum : 13/03/2025 Uitvoeringstijdstip : 10:33:16 Valutadatum : 17/03/2025
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Uitvoeringsdatum : (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) Uitvoeringstijdstip : (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))))

                        // @formatter:off
                        // Netto debit -868,50 EUR
                        // Netto credit 125.824,15 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Netto (debit|credit) (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Netto debit -10.718,20 USD
                                        // Wisselkoers 1 EUR = 1,065446 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("gross", "baseCurrency", "exchangeRate", "termCurrency") //
                                                        .match("^Netto (debit|credit) (\\-)?(?<gross>[\\.,\\d]+) [A-Z]{3}$") //
                                                        .match("^Wisselkoers [\\.,\\d]+ (?<baseCurrency>[A-Z]{3}) = (?<exchangeRate>[\\.,\\d]+) (?<termCurrency>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                                                            var fxGross = rate.convert(rate.getBaseCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // 1 USD = 0,932651 EUR
                                        // Netto credit 69.606,12 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("baseCurrency", "exchangeRate", "termCurrency", "gross") //
                                                        .match("^[\\.,\\d]+ (?<baseCurrency>[A-Z]{3}) = (?<exchangeRate>[\\.,\\d]+) (?<termCurrency>[A-Z]{3})$") //
                                                        .match("^Netto (debit|credit) (\\-)?(?<gross>[\\.,\\d]+) [A-Z]{3}$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            var fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // Borderel 275825809 Limit order
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Borderel [\\d]+).*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        var type = new DocumentType("Uw Uitbetaling dividenden");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Borderel [\\d]+.*$");
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
                                        // Uw Uitbetaling dividenden van 43 CP ALBEMARLE CORP (NY) 14.12.23 aan 0,4 17,20 USD
                                        // USD
                                        // Cash Dividend US0126531013ex 2023-12-14 pd 2024-01-02
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^.* van [\\.,\\d]+ (?<name>.*) aan [\\.,\\d]+ [\\.,\\d]+ [A-Z]{3}$") //
                                                        .match("^(?<currency>[A-Z]{3})$") //
                                                        .match("^Cash Dividend (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Uw Uitbetaling dividenden van 2.065 ISHAR.III CORE EUR CORP BD UC ETF-D 4.173,16 EUR
                                        // aan 2,020901 EUR
                                        // Cash Dividend IE00B3F81R35ex 2024-01-11 pd 2024-01-24
                                        //
                                        // Uw Uitbetaling dividenden van 912 ISHAR.III CORE EUR CORP BD UC 1.860,12 EUR
                                        // ETF-D aan 2,039605 EUR
                                        // Cash Dividend IE00B3F81R35ex 2025-01-16 pd 2025-01-29
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^.* van [\\.,\\d]+ (?<name>.*) [\\.,\\d]+ [A-Z]{3}$") //
                                                        .match("^.*aan [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .match("^Cash Dividend (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Uw Uitbetaling dividenden van 2.130 FIRST MAJESTIC SILVER CORP aan 0,0057 USD 12,14 USD
                                        // Cash Dividend CA32076V1031ex 2025-02-28 pd 2025-03-14
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^.* van [\\.,\\d]+ (?<name>.*) aan [\\.,\\d]+ (?<currency>[A-Z]{3}) [\\.,\\d]+ [A-Z]{3}$") //
                                                        .match("^Cash Dividend (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Uw Uitbetaling dividenden van 2.065 ISHAR.III CORE EUR CORP BD UC ETF-D 4.173,16 EUR
                        // @formatter:on
                        .section("shares") //
                        .match("^.* van (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 24/01/2024 Uitbetaling dividenden ISHAR.III CORE EUR Valuta 24/01/2024 2.862,79 EUR
                        // @formatter:on
                        .section("date") //
                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) Uitbetaling dividenden.*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Netto credit 2.862,79 EUR
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Netto credit (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Uw Uitbetaling dividenden van 2.130 FIRST MAJESTIC SILVER CORP aan 0,0057 USD 12,14 USD
                                        // Wisselkoers 1 EUR = 1,089923 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("gross", "baseCurrency", "exchangeRate", "termCurrency") //
                                                        .match("^.* aan [\\.,\\d]+ [A-Z]{3} (?<gross>[\\.,\\d]+) [A-Z]{3}$") //
                                                        .match("^Wisselkoers [\\.,\\d]+ (?<baseCurrency>[A-Z]{3}) = (?<exchangeRate>[\\.,\\d]+) (?<termCurrency>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                                                            var fxGross = rate.convert(rate.getBaseCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Uw Uitbetaling dividenden van 43 CP ALBEMARLE CORP (NY) 14.12.23 aan 0,4 17,20 USD
                                        // USD
                                        // Wisselkoers 1 EUR = 1,10952 USD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("gross", "baseCurrency", "exchangeRate", "termCurrency") //
                                                        .match("^.* aan [\\.,\\d]+ (?<gross>[\\.,\\d]+) [A-Z]{3}$") //
                                                        .match("^Wisselkoers [\\.,\\d]+ (?<baseCurrency>[A-Z]{3}) = (?<exchangeRate>[\\.,\\d]+) (?<termCurrency>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var gross = Money.of(rate.getTermCurrency(), asAmount(v.get("gross")));
                                                            var fxGross = rate.convert(rate.getBaseCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // Borderel 003308592
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Borderel [\\d]+)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Rekeninguittreksel Nr");
        this.addDocumentTyp(type);

        // @formatter:off
        // 18/08/2022 Provisionering rekening klant Valuta 17/08/2022 50.000,00 EUR
        // @formatter:on
        var depositBlock = new Block("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{4} Provisionering rekening klant.*$");
        type.addBlock(depositBlock);
        depositBlock.setMaxSize(1);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "currency") //
                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) " //
                                        + "(?<note>Provisionering rekening klant) " //
                                        + "Valuta[\\s]*[\\d]{2}\\/[\\d]{2}\\/[\\d]{4} " //
                                        + "(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 04/09/2024 Overschrijving naar klant Valuta 04/09/2024 -32.339,70 EUR
        // 30/01/2025 Overschrijving naar klant Valuta30/01/2025 -1.275,00 EUR
        // @formatter:on
        var removalBlock = new Block("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{4} Overschrijving naar klant.*$");
        type.addBlock(removalBlock);
        removalBlock.setMaxSize(1);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "currency") //
                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) " //
                                        + "(?<note>Overschrijving naar klant) " //
                                        + "Valuta[\\s]*[\\d]{2}\\/[\\d]{2}\\/[\\d]{4} " //
                                        + "\\-(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 05/08/2024 Bewaarloon Valuta 05/08/2024 -179,48 EUR
        // @formatter:on
        var feesBlock = new Block("^[\\d]{2}\\/[\\d]{2}\\/[\\d]{4} Bewaarloon.*$");
        type.addBlock(feesBlock);
        feesBlock.setMaxSize(1);
        feesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "currency") //
                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) " //
                                        + "(?<note>Bewaarloon) " //
                                        + "Valuta [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} " //
                                        + "\\-(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Buitenlandse bronheffing (basisbedrag 12,14 USD) 3,04 USD
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Buitenlandse bronheffing \\(basisbedrag [\\.,\\d]+ [A-Z]{3}\\) (?<withHoldingTax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Roerende voorheffing op fondsen 839,15 EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Roerende voorheffing op fondsen (?<withHoldingTax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Roerende voorheffing (basisbedrag 4.173,16 EUR) 1.251,95 EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Roerende voorheffing \\(basisbedrag [\\.,\\d]+ [A-Z]{3}\\) (?<withHoldingTax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // BTW (basisbedrag 48,28 EUR) 10,14 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^BTW \\(basisbedrag [\\.,\\d]+ [A-Z]{3}\\) (?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Beurstaks 3,00 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Beurstaks (?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Sec Fee USA 0,05 USD
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Sec Fee .* (?<fee>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Makelaarsloon 7,50 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Makelaarsloon (?<fee>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Makelaarsloon KBC 37,84 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Makelaarsloon .* (?<fee>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Kosten incassostelling (basisbedrag 2.921,21 EUR) 48,28 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Kosten incassostelling \\(basisbedrag [\\.,\\d]+ [A-Z]{3}\\) (?<fee>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
