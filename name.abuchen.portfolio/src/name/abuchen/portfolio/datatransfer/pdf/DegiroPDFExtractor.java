package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.DocumentContext;
import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.ParsedData;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class DegiroPDFExtractor extends AbstractPDFExtractor
{
    private static final DateTimeFormatter DATEFORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final Pattern TRANSACTION_REPORT_START = Pattern
                    .compile("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}:[\\d]{2} .* [A-Z]{2}[A-Z0-9]{9}[0-9] .*$");
    private static final Pattern TRANSACTION_REPORT_NAME_CONTINUATION = Pattern
                    .compile("^[A-Z0-9\\(][A-Z0-9 .,&'’()/+\\-]*$");

    public DegiroPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("DEGIRO");

        addAccountStatementTransactions();
        addDepotStatementTransactions();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "DEGIRO";
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Dividend note");
        this.addDocumentTyp(type);

        Block block = new Block("^Dividend note .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS));

        pdfTransaction
                // @formatter:off
                // Security name: ROYAL DUTCH SHELLA
                // Security ISIN: GB00B03MLX29
                // Number of shares Amount of dividend Gross amount of Amount of tax Net amount ofper share dividend withheld dividend Wäh.
                // 20 0,142 2,84 -0,43 2,41 EUR
                // @formatter:on
                .section("name", "isin", "shares", "currency")
                .match("^Security name: (?<name>.*)$")
                .match("^Security ISIN: (?<isin>.*)$")
                .find("Number of shares .*")
                .match("^(?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+ -[\\.,\\d]+ [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Dividend date (Pay date): 2020-06-22
                .section("date")
                .match("^Dividend date \\(Pay date\\): (?<date>[\\d]{4}-[\\d]{2}-[\\d]{2})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Ex date: 2020-05-14
                .section("exDate").optional()
                .match("^Ex date: (?<exDate>[\\d]{4}-[\\d]{2}-[\\d]{2})$")
                .assign((t, v) -> t.setExDate(asDate(v.get("exDate"))))

                // @formatter:off
                // Number of shares Amount of dividend Gross amount of Amount of tax Net amount ofper share dividend withheld dividend Wäh.
                // 20 0,142 2,84 -0,43 2,41 EUR
                // @formatter:on
                .section("amount", "currency")
                .find("Number of shares .*")
                .match("^[\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ -[\\.,\\d]+ (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addAccountStatementTransactions()
    {
        final DocumentType type = new DocumentType("(Kontoauszug"
                        + "|Account statement"
                        + "|Rekeningoverzicht"
                        + "|Estratto conto"
                        + "|Estado de cuenta"
                        + "|Přehled účtu)", (context, lines) -> {

            // @formatter:off
            // Formatting:
            // DateTime | Valutadatum | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
            // or
            // DateTime | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
            // or
            // DateTime | Value date | Product | ISIN | Description | FX Change | Balance
            // or
            // DatumTijd | Valutadatum | Product | ISIN | Omschrijving | FX Mutatie | Saldo
            // -------------------------------------
            // Einbuchung:
            // 19-07-2017 00:00 Währungswechsel 1,1528 EUR 0,75 EUR 1.783,89
            // 03-08-2019 06:55 02-08-2019 Währungswechsel (Ausbuchung) 1,1120 USD -3,84 USD -0,00
            //
            // 23-03-2021 08:17 22-03-2021 FX Debit EUR 9.12 EUR -2,681.14
            // 23-03-2021 08:17 22-03-2021 FX Debit 1.4958 CAD -13.65 CAD 0.00
            //
            // 17-11-2021 07:38 16-11-2021 Währungswechsel (Einbuchung) CHF 2.02 CHF 1'185.08
            // 17-11-2021 07:38 16-11-2021 Währungswechsel (Ausbuchung) 1.0760 USD -2.18 USD 0.00
            //
            // 13-11-2021 07:45 12-11-2021 Währungswechsel (Einbuchung) CHF 1.54 CHF 1'183.06
            // 13-11-2021 07:45 12-11-2021 Währungswechsel (Ausbuchung) 1.0866 USD -1.68 USD 0.00
            //
            // 27-05-2022 07:34 26-05-2022 Valuta Creditering EUR 0,53 EUR 0,53
            // 27-05-2022 07:34 26-05-2022 Valuta Debitering 1,0758 USD -0,57 USD 0,00
            //
            // 30-09-2022 07:52 29-09-2022 Credito FX EUR 68,23 EUR 762,14
            // 30-09-2022 07:52 29-09-2022 Prelievo FX 0,9840 USD -67,14 USD 0,00
            //
            // 01-10-2022 08:36 30-09-2022 Ingreso Cambio de Divisa EUR 26,99 EUR 119,99
            // 01-10-2022 08:36 30-09-2022 Retirada Cambio de Divisa 0,9827 USD -26,52 USD 0,00
            //
            // 30-12-2022 07:49 29-12-2022 FX vyučtování konverze měny EUR 37,88 EUR 217,97
            // 30-12-2022 07:49 29-12-2022 FX vyučtování konverze měny 1,0688 USD -40,49 USD 0,00
            // @formatter:on
            Pattern pCurrencyFx = Pattern.compile("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) [\\d]{2}:[\\d]{2} "
                            + "(?<valuta>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                            + "(W.hrungswechsel"
                            + "|FX Debit"
                                            + "|Operation de change \\- Cr.dit" + "|Op.ration de change \\- D.bit"
                            + "|Valuta Creditering"
                            + "|Valuta Debitering"
                            + "|Prelievo"
                            + "|Retirada Cambio de Divisa"
                            + "|FX vyu.tov.n. konverze m.ny).* "
                            + "(?<fxRate>[\\.,'\\d\\s]+) "
                            + "(?<currency>[\\w]{3}) "
                            + "(\\-)?(?<amount>[\\.,'\\d\\s]+) "
                            + "[\\w]{3}.*$");

            // @formatter:off
            // Formatting:
            // DateTime | Valutadatum | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
            // or
            // DateTime | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
            // or
            // DateTime | Value date | Product | ISIN | Description | FX Change | Balance
            // or
            // DatumTijd | Valutadatum | Product | ISIN | Omschrijving | FX Mutatie | Saldo
            // -------------------------------------
            // Ausbuchung:
            // 19-07-2017 00:00 Währungswechsel USD -0,86 USD 0,00
            // 03-08-2019 06:55 02-08-2019 Währungswechsel (Einbuchung) EUR 3,45 EUR 1.552,27
            //
            // 16-03-2021 09:24 15-03-2021 FX Debit EUR 31.66 EUR -1,542.30
            // 16-03-2021 09:24 15-03-2021 FX Debit 1.1942 USD -37.82 USD -0.00
            //
            // 28-06-2024 08:17 27-06-2024 FX Credit EUR 1.87 EUR 12.76
            // 28-06-2024 08:17 27-06-2024 FX Debit 4.3211 PLN -8.10 PLN 0.00
            //
            // 27-05-2022 07:34 26-05-2022 Valuta Creditering EUR 0,53 EUR 0,53
            // 27-05-2022 07:34 26-05-2022 Valuta Debitering 1,0758 USD -0,57 USD 0,00
            //
            // 30-09-2022 07:52 29-09-2022 Credito FX EUR 68,23 EUR 762,14
            // 30-09-2022 07:52 29-09-2022 Prelievo FX 0,9840 USD -67,14 USD 0,00
            //
            // 01-10-2022 08:36 30-09-2022 Ingreso Cambio de Divisa EUR 26,99 EUR 119,99
            // 01-10-2022 08:36 30-09-2022 Retirada Cambio de Divisa 0,9827 USD -26,52 USD 0,00
            //
            // 30-12-2022 07:49 29-12-2022 FX vyučtování konverze měny EUR 37,88 EUR 217,97
            // 30-12-2022 07:49 29-12-2022 FX vyučtování konverze měny 1,0688 USD -40,49 USD 0,00
            // @formatter:on
            Pattern pCurrencyBase = Pattern.compile("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}:[\\d]{2} "
                            + "([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                            + "(W.hrungswechsel"
                            + "|FX Credit"
                            + "|FX Debit"
                                            + "|Operation de change \\- Cr.dit" + "|Op.ration de change \\- D.bit"
                            + "|Valuta Creditering"
                            + "|Valuta Debitering"
                            + "|Credito"
                            + "|Ingreso Cambio de Divisa"
                            + "|Prelievo FX"
                            + "|FX vyu.tov.ní konverze m.ny).* "
                            + "(?<currency>[\\w]{3}) "
                            + "(\\-)?(?<amount>[\\.,'\\d\\s]+) "
                            + "[\\w]{3}.*$");

            ExchangeRateHelper exchangeRateHelper = new ExchangeRateHelper();
            context.putType(exchangeRateHelper);

            for (int i = 0; i < lines.length; i++)
            {
                Matcher mFx = pCurrencyFx.matcher(lines[i]);
                if (mFx.matches())
                {
                    CurrencyExchangeItem item = new CurrencyExchangeItem();
                    item.lineNo = i;
                    item.date = LocalDate.parse(mFx.group("date"), DATEFORMAT);

                    String valuta = mFx.group("valuta");
                    if (valuta != null)
                        item.valuta = LocalDate.parse(valuta.trim(), DATEFORMAT);

                    item.termCurrency = mFx.group("currency");
                    item.termAmount = asAmount(mFx.group("amount"));
                    item.rate = asExchangeRate(mFx.group("fxRate"));

                    // @formatter:off
                    // run backwards to find the corresponding entry
                    // @formatter:on
                    for (int ii = i - 1; ii >= 0; ii--)
                    {
                        Matcher mBase = pCurrencyBase.matcher(lines[ii]);
                        if (mBase.matches())
                        {
                            if (item.valuta != null)
                            {
                                item.baseCurrency = mBase.group("currency");
                                item.baseAmount = asAmount(mBase.group("amount"));
                            }
                            else
                            {
                                // @formatter:off
                                // older documents (before providing valuta date)
                                // provide the currency the other way around
                                // @formatter:on
                                item.baseCurrency = item.termCurrency;
                                item.baseAmount = item.termAmount;
                                item.termCurrency = mBase.group("currency");
                                item.termAmount = asAmount(mBase.group("amount"));
                            }
                            exchangeRateHelper.items.add(item);
                            break;
                        }
                    }
                }
            }

            Pattern pDividendeTransactions = Pattern.compile("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                            + "([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                            + ".* "
                            + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                            + "(Dividende"
                            + "|Dividend"
                            + "|Fondsaussch.ttung"
                            + "|Dividendo"
                            + "|Dividenda) "
                            + "[\\w]{3}"
                            + "(?<type>\\s(\\-)?)"
                            + "[\\.,'\\d\\s]+ "
                            + "[\\w]{3} "
                            + "(\\-)?[\\.,'\\d\\s]+$");

            // @formatter:off
            // Sometimes the dividend tax is booked before the dividend.
            //
            // 28-06-2024 04:43 27-06-2024 CD PROJEKT SA PLOPTTC00011 Dividend Tax PLN -1.90 PLN 8.10
            // 28-06-2024 04:43 27-06-2024 CD PROJEKT SA PLOPTTC00011 Dividend PLN 10.00 PLN 10.00
            //
            // The block of the dividend starts at the line of the dividend and does not
            // cover a tax above it. Therefore the dividend taxes are collected here.
            // @formatter:on
            Pattern pDividendeTaxTransactions = Pattern.compile("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                            + "([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                            + ".* "
                            + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*"
                            + "(Dividendensteuer"
                            + "|Dividend Tax"
                            + "|Dividendbelasting"
                            + "|Ritenuta sul dividendo"
                                            + "|Imp.ts sur dividende"
                            + "|Retenci.n del dividendo) "
                            + "(?<currency>[\\w]{3}) "
                            + "\\-(?<amount>[\\.,'\\d\\s]+) "
                            + "[\\w]{3} "
                            + "(\\-)?[\\.,'\\d\\s]+$");

            DividendTaxHelper dividendeTaxHelper = new DividendTaxHelper();
            context.putType(dividendeTaxHelper);

            // @formatter:off
            // The block of a dividend reaches from the line of the dividend down to the
            // next dividend and adds the first tax it finds within that range. Every other
            // tax cannot be reached and is collected here.
            // @formatter:on
                            LocalDate openDividendDate = null;
                            String openDividendIsin = null;
                            boolean isTaxAddedToDividendBlock = false;

            for (int i = 0; i < lines.length; i++)
            {
                                Matcher d = pDividendeTransactions.matcher(lines[i]);
                                if (d.matches())
                                {
                                    openDividendDate = asDate(d.group("date"), d.group("time")).toLocalDate();
                                    openDividendIsin = d.group("isin");
                                    isTaxAddedToDividendBlock = false;
                                    continue;
                                }

                Matcher m = pDividendeTaxTransactions.matcher(lines[i]);
                if (m.matches())
                {
                    // @formatter:off
                    // The block of the open dividend only adds a tax of the same security,
                    // therefore a tax of another security is collected here as well.
                    // @formatter:on
                                    if (!isTaxAddedToDividendBlock && openDividendIsin != null
                                                    && openDividendIsin.equals(m.group("isin"))
                                                    && openDividendDate.equals(asDate(m.group("date"), m.group("time"))
                                                                    .toLocalDate()))
                                    {
                                        isTaxAddedToDividendBlock = true;
                                        continue;
                                    }

                    DividendeTaxItem item = new DividendeTaxItem();
                    item.lineNo = i;
                                    item.date = asDate(m.group("date"), m.group("time")).toLocalDate();
                    item.isin = m.group("isin");
                    item.currency = m.group("currency");
                    item.amount = m.group("amount");

                    dividendeTaxHelper.items.add(item);
                }
            }

            DividendTransactionHelper dividendeTransactionHelper = new DividendTransactionHelper();
            context.putType(dividendeTransactionHelper);

            for (int i = 0; i < lines.length; i++)
            {
                Matcher m = pDividendeTransactions.matcher(lines[i]);
                if (m.matches())
                {
                    DividendeTransactionsItem item = new DividendeTransactionsItem();
                    item.dateTime = asDate(m.group("date"), m.group("time"));
                    item.isin = m.group("isin");

                    // A dividend with a negative sign is the cancellation of an
                    // earlier dividend payment.
                    item.cancellation = "-".equals(trim(m.group("type")));

                    dividendeTransactionHelper.items.add(item);
                }
            }
        });
        this.addDocumentTyp(type);

        // @formatter:off
        // Formatting:
        // DateTime | Valutadatum | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
        // or
        // DateTime | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
        // or
        // DateTime | Value date | Product | ISIN | Description | FX Change | Balance
        // or
        // DatumTijd | Valutadatum | Product | ISIN | Omschrijving | FX Mutatie | Saldo
        // or
        // Data Ora | Data Valore | Prodotto | ISIN | Descrizione  | Borsa Variazioni | Saldo
        // -------------------------------------
        // 07-02-2019 11:53 07-02-2019 Einzahlung EUR 1.000,00 EUR 1.000,01
        // 01-02-2019 11:44 01-02-2019 Einzahlung EUR 0,01 EUR 0,01
        // 02-08-2017 00:00 Einzahlung EUR 350,00 EUR 350,00
        // 22-02-2019 18:40 22-02-2019 SOFORT Einzahlung EUR 27,00 EUR 44,89
        // 26-10-2020 15:00 26-10-2020 flatex Einzahlung EUR 500,00 EUR 512,88
        // 25-08-2021 08:41 24-08-2021 iDEAL Deposit EUR 1.123,00 EUR 123,29
        // 27-07-2021 08:43 26-07-2021 iDEAL Deposit EUR 123,00 EUR 123,13
        // 26-11-2021 09:01 25-11-2021 Einzahlung CHF 569.00 CHF 1'754.08
        // 28-10-2021 08:50 28-10-2021 Deposito flatex EUR 200,00 EUR 215,77
        // 01-09-2021 02:44 31-08-2021 Deposito EUR 0,01 EUR 0,01
        // 25-07-2022 10:50 25-07-2022 flatex Storting EUR 123,45 EUR 123,47
        // 03-08-2022 02:24 03-08-2022 flatex Deposit EUR 2.000,00 EUR 2.032,74
        // 20-12-2021 02:53 18-12-2021 Ingreso EUR 1,00 EUR 1,00
        // 21-12-2021 16:59 21-12-2021 Vklad CZK 50 000,00 CZK 50 000,00
        // @formatter:on
        Block blockDeposit = new Block("^.*([\\d]{2}:[\\d]{2}|[\\d]{4}) "
                        + "(SOFORT |iDEAL |flatex )?"
                        + "(Einzahlung"
                        + "|D.p.t"
                        + "|Deposit"
                        + "|Deposito"
                        + "|Storting"
                        + "|Ingreso"
                        + "|Vklad)"
                        + "( flatex| iDEAL)? "
                        + "[\\w]{3} .*$");
        type.addBlock(blockDeposit);
        blockDeposit.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "time", "note", "currency", "amount")
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                        + "([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                                        + "(?<note>(SOFORT |iDEAL |flatex )?"
                                        + "(Einzahlung"
                                        + "|D.p.t"
                                        + "|Deposit"
                                        + "|Deposito"
                                        + "|Storting"
                                        + "|Ingreso"
                                        + "|Vklad)" + "( flatex| iDEAL)?) "
                                        + "(?<currency>[\\w]{3}) "
                                        + "(?<amount>[\\.,'\\d\\s]+) "
                                        + "[\\w]{3} "
                                        + "(\\-)?[\\.,'\\d\\s]+$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        // @formatter:off
        // Formatting:
        // DateTime | Valutadatum | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
        // or
        // DateTime | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
        // or
        // DateTime | Value date | Product | ISIN | Description | FX Change | Balance
        // or
        // DatumTijd | Valutadatum | Product | ISIN | Omschrijving | FX Mutatie | Saldo
        // or
        // Data Ora | Data Valore | Prodotto | ISIN | Descrizione  | Borsa Variazioni | Saldo
        // -------------------------------------
        // 05-08-2019 00:09 05-08-2019 Auszahlung EUR -1.000,00 EUR 1.445,06
        // 20-04-2022 17:30 21-04-2022 Prelievo flatex EUR -1.046,92 EUR 1.411,80
        // 23-06-2022 18:00 24-06-2022 Prelievo flatex EUR -26.600,00 EUR -24.176,26
        // 30-06-2022 18:20 01-07-2022 flatex terugstorting EUR -2.900,00 EUR -2.884,26
        // @formatter:on
        Block blockRemoval = new Block("^.*([\\d]{2}:[\\d]{2}|[\\d]{4}) (Auszahlung"
                        + "|Prelievo flatex"
                        + "|(flatex |iDEAL )?terugstorting) [\\w]{3} \\-[\\.,'\\d\\s]+ [\\w]{3} (\\-)?[\\.,'\\d\\s]+$");
        type.addBlock(blockRemoval);
        blockRemoval.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.REMOVAL))

                        .section("date", "time", "note", "currency", "amount")
                        .match("(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                        + "([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                                        + "(?<note>(Auszahlung"
                                        + "|Prelievo flatex"
                                        + "|(flatex |iDEAL )?terugstorting)) "
                                        + "(?<currency>[\\w]{3}) "
                                        + "\\-(?<amount>[\\.,'\\d\\s]+) "
                                        + "[\\w]{3} "
                                        + "(\\-)?[\\.,'\\d\\s]+$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        // @formatter:off
        // Formatting:
        // DateTime | Valutadatum | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
        // or
        // DateTime | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
        // or
        // DateTime | Value date | Product | ISIN | Description | FX Change | Balance
        // or
        // DatumTijd | Valutadatum | Product | ISIN | Omschrijving | FX Mutatie | Saldo
        // or
        // Data Ora | Data Valore | Prodotto | ISIN | Descrizione  | Borsa Variazioni | Saldo
        // -------------------------------------
        // 15-06-2019 06:44 14-06-2019 Währungswechsel (Einbuchung) EUR 0,30 EUR 23,09
        // 15-06-2019 06:44 14-06-2019 Währungswechsel (Ausbuchung) 1,1219 USD -0,34 USD 0,00
        // (...)
        // 14-06-2019 07:55 14-06-2019 THE KRAFT HEINZ COMPAN US5007541064 Dividende USD 0,40 USD 0,34
        // 14-06-2019 07:55 14-06-2019 THE KRAFT HEINZ COMPAN US5007541064 Dividendensteuer USD -0,06 USD -0,06
        // -------------------------------------
        // 05-07-2019 06:49 04-07-2019 Währungswechsel (Einbuchung) EUR 10,12 EUR 2.119,03
        // 05-07-2019 06:49 04-07-2019 Währungswechsel (Ausbuchung) 1,1297 USD -11,44 USD -0,00
        // 04-07-2019 10:22 30-06-2019 MORGAN STANLEY USD LIQUIDITY FUND LU0904783114 Fondsausschüttung USD 11,44 USD 11,44
        // -------------------------------------
        // 19-07-2017 00:00 Währungswechsel USD -0,86 USD 0,00
        // (Ausbuchung)
        // 19-07-2017 00:00 Währungswechsel 1,1528 EUR 0,75 EUR 1.783,89
        // (Einbuchung)
        // (...)
        // 17-07-2017 00:00 IS.DJ U.S.SELEC.DIV.U.ETF DE000A0D8Q49 Dividende USD 0,86 USD 0,86
        // -------------------------------------
        // 17-05-2019 06:53 16-05-2019 Währungswechsel (Einbuchung) EUR 0,58 EUR 11,49
        // 17-05-2019 06:53 16-05-2019 Währungswechsel (Ausbuchung) 1,1186 USD -0,65 USD 0,00
        // 16-05-2019 08:21 16-05-2019 APPLE INC. - COMMON ST US0378331005 Dividende USD 0,77 USD 0,65
        // 16-05-2019 08:21 16-05-2019 APPLE INC. - COMMON ST US0378331005 Dividendensteuer USD -0,12 USD -0,12
        // -------------------------------------
        // 07-06-2019 07:05 06-06-2019 Währungswechsel (Einbuchung) EUR 0,23 EUR 211,70
        // 07-06-2019 07:05 06-06-2019 Währungswechsel (Ausbuchung) 1,1287 USD -0,27 USD 0,00
        // (...)
        // 06-06-2019 09:00 05-06-2019 SONY CORPORATION COMMO US8356993076 Dividende USD 0,37 USD 0,27
        // 06-06-2019 09:00 05-06-2019 SONY CORPORATION COMMO US8356993076 Dividendensteuer USD -0,06 USD -0,10
        // 06-06-2019 09:00 05-06-2019 SONY CORPORATION COMMO US8356993076 ADR/GDR Weitergabegebühr USD -0,04 USD -0,04
        // -------------------------------------
        // 03-08-2019 06:09 02-08-2019 FOOT LOCKER INC. US3448491049 Dividende USD 1,52 USD 3,84
        // 03-08-2019 06:09 02-08-2019 FOOT LOCKER INC. US3448491049 Dividendensteuer USD -0,23 USD 2,32
        // -------------------------------------
        // 02-08-2019 07:38 02-08-2019 VODAFONE GROUP PLC GB00BH4HKS39 Dividende GBP 3,73 GBP 3,73
        // -------------------------------------
        // 22-03-2021 07:39 19-03-2021 MANULIFE FINANCIAL COR CA56501R1064 Dividend CAD 18.20 CAD 13.65
        // 22-03-2021 07:39 19-03-2021 MANULIFE FINANCIAL COR CA56501R1064 Dividend Tax CAD -4.55 CAD -4.55
        //
        // **********************************
        // Calculation of taxes:            *
        // **********************************
        // 14-06-2019 07:55 14-06-2019 THE KRAFT HEINZ COMPAN US5007541064 Dividende USD 0,40 USD 0,34
        // 14-06-2019 07:55 14-06-2019 THE KRAFT HEINZ COMPAN US5007541064 Dividendensteuer USD -0,06 USD -0,06
        //
        // Gross amount: 0,40 USD + 0,06 USD = 0,46 USD
        //
        // **********************************
        // Calculation of taxes and fee:    *
        // **********************************
        // 12-06-2019 09:12 11-06-2019 NOKIA CORPORATION SPON US6549022043 Dividende USD 1,74 USD 2,36
        // 12-06-2019 09:12 11-06-2019 NOKIA CORPORATION SPON US6549022043 Dividendensteuer USD -0,52 USD 0,62
        // 12-06-2019 09:12 11-06-2019 NOKIA CORPORATION SPON US6549022043 ADR/GDR Weitergabegebühr USD -0,08 USD 1,14
        //
        // ExchangeRage: 0,8851124093 --> 0,89
        // Gross amount in EUR: (1,74 * 0,89) + (0,52 * 0,89) + (0,08 * 0,89) = 2,0826 EUR
        // @formatter:on
        Block blockDividends = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}:[\\d]{2} ([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?.*"
                        + "(Dividende"
                        + "|Dividend(?! Tax)"
                        + "|Fondsaussch.ttung"
                        + "|Dividendo"
                        + "|Dividenda) "
                        + ".*$");
        type.addBlock(blockDividends);
        blockDividends.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        .optionalOneOf(
                                        // @formatter:off
                                        // 14-06-2019 07:55 14-06-2019 THE KRAFT HEINZ COMPAN US5007541064 Dividende USD 0,40 USD 0,34
                                        // 05-08-2019 14:12 31-07-2019 MORGAN STANLEY USD LIQUIDITY FUND LU0904783114 Fondsausschüttung USD 1,64 USD 6.383,23
                                        // 22-03-2021 07:39 19-03-2021 MANULIFE FINANCIAL COR CA56501R1064 Dividend CAD 18.20 CAD 13.65
                                        // 31-03-2022 07:36 30-03-2022 ISHARES GLOB HIG YLD CORP BOND UCITS IE00B74DQ490 Dividendo USD 24,19 USD 24,19
                                        // 30-09-2022 07:25 29-09-2022 T. ROWE PRICE GROUP I US74144T1088 Dividendo USD 31,20 USD 26,52
                                        //
                                        // -------------------------------------
                                        // Date of currency exchange is different from the dividend date
                                        //
                                        // 27-06-2022 07:30 24-06-2022 Valuta Creditering EUR 497,44 EUR 2.648,76
                                        // 27-06-2022 07:30 24-06-2022 Valuta Debitering 1,0582 USD -526,37 USD 0,00
                                        // 26-06-2022 06:41 24-06-2022 VANGUARD TOTAL INTERNA US9219097683 Dividend USD 751,95 USD 526,37
                                        // 26-06-2022 06:41 24-06-2022 VANGUARD TOTAL INTERNA US9219097683 Dividendbelasting USD -225,58 USD -225,58
                                        // -------------------------------------
                                        //
                                        // Dividend refund + tax refund
                                        //
                                        // 07-11-2022 09:59 02-11-2022 INDITEX ES0148396007 Dividendo EUR -317,70 EUR 534,43
                                        // 07-11-2022 09:59 02-11-2022 INDITEX ES0148396007 Retención del dividendo EUR 60,36 EUR 852,13
                                        // @formatter:on
                                        section -> section
                                        .attributes("date", "time", "valueDate", "name", "isin", "currency", "type", "amount")
                                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                        + "(?<valueDate>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) "
                                                        + "(?<name>.*) "
                                                        + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                        + "(Dividende"
                                                        + "|Dividend"
                                                        + "|Fondsaussch.ttung"
                                                        + "|Dividendo"
                                                        + "|Dividenda) "
                                                        + "(?<currency>[\\w]{3})"
                                                        + "(?<type>\\s(\\-)?)"
                                                        + "(?<amount>[\\.,'\\d\\s]+) "
                                                        + "[\\w]{3} "
                                                        + "(\\-)?[\\.,'\\d\\s]+$")
                                        .assign((t, v) -> {
                                            DocumentContext context = type.getCurrentContext();
                                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                                            t.setSecurity(getOrCreateSecurity(v));

                                            // Dividend refund
                                            if ("-".equals(trim(v.get("type"))))
                                                v.markAsFailure(Messages.MsgErrorTransactionOrderCancellationUnsupported);

                                            Money money = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                                            if (!money.getCurrencyCode().equals(getClient().getBaseCurrency()))
                                            {
                                                ExchangeRateHelper exchangeRateHelper = context.getType(ExchangeRateHelper.class)
                                                                .orElseGet(ExchangeRateHelper::new);

                                                Optional<CurrencyExchangeItem> item = exchangeRateHelper.findItem(v.getStartLineNumber(), money,
                                                                t.getDateTime().toLocalDate());

                                                // @formatter:off
                                                // If no exchange rate is found,
                                                // search for the exchange rate on the value date
                                                // @formatter:on
                                                if (!item.isPresent())
                                                {
                                                    LocalDate valuta = LocalDate.parse(v.get("valueDate"), DATEFORMAT);
                                                    item = exchangeRateHelper.findItem(v.getStartLineNumber(), money, valuta);
                                                }

                                                if (item.isPresent())
                                                {
                                                    v.put("currency", asCurrencyCode(v.get("currency")));
                                                    v.put("exchangeRate", item.get().rate.toString());
                                                    v.put("baseCurrency", asCurrencyCode(item.get().baseCurrency));
                                                    v.put("termCurrency", asCurrencyCode(item.get().termCurrency));

                                                    ExtrExchangeRate rate = asExchangeRate(v);
                                                    type.getCurrentContext().putType(rate);

                                                    Money fxGross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                                                    Money gross = rate.convert(asCurrencyCode(item.get().baseCurrency), fxGross);

                                                    t.setMonetaryAmount(gross);

                                                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());

                                                    context.putType(item.get());
                                                }
                                                else
                                                {
                                                    // @formatter:off
                                                    // Without an exchange rate in the document the amount cannot be
                                                    // converted into the account currency. The booking keeps the amount
                                                    // of the document and is marked as failure, so that it is not
                                                    // skipped without any notice. A cancellation already carries its
                                                    // own failure message.
                                                    // @formatter:on
                                                    if (!"-".equals(trim(v.get("type"))))
                                                        v.markAsFailure(Messages.MsgErrorTransactionMissingExchangeRateIfInForex);

                                                    t.setMonetaryAmount(money);
                                                }
                                            }
                                            else
                                            {
                                                t.setMonetaryAmount(money);
                                            }
                                        })
                                        ,
                                        // @formatter:off
                                        // 17-07-2017 00:00 IS.DJ U.S.SELEC.DIV.U.ETF DE000A0D8Q49 Dividende USD 0,86 USD 0,86
                                        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividende EUR 2,07 EUR 521,41
                                        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividendensteuer EUR -0,55 EUR 519,34
                                        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividende EUR 22,64 EUR 519,89
                                        // 17-07-2017 00:00 IS.S.GL.SE.D.100 U.ETF A DE000A0F5UH1 Dividende EUR 0,09 EUR 497,25
                                        // 17-07-2017 00:00 IS.S.GL.SE.D.100 U.ETF A DE000A0F5UH1 Dividendensteuer EUR -0,02 EUR 497,16
                                        // 17-07-2017 00:00 IS.S.GL.SE.D.100 U.ETF A DE000A0F5UH1 Dividende EUR 1,74 EUR 497,18
                                        // @formatter:on
                                        section -> section
                                                .attributes("date", "time", "name", "isin", "currency", "amount")
                                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                                + "(?<name>.*) "
                                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                                + "(Dividende"
                                                                + "|Dividend"
                                                                + "|Fondsaussch.ttung"
                                                                + "|Dividendo"
                                                                + "|Dividenda) "
                                                                + "(?<currency>[\\w]{3}) "
                                                                + "(?<amount>[\\.,'\\d\\s]+) "
                                                                + "[\\w]{3} "
                                                                + "(\\-)?[\\.,'\\d\\s]+$")
                                                .assign((t, v) -> {
                                                    DocumentContext context = type.getCurrentContext();
                                                    t.setDateTime(asDate(v.get("date"), v.get("time")));
                                                    t.setSecurity(getOrCreateSecurity(v));

                                                    Money money = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                                                    if (!money.getCurrencyCode().equals(getClient().getBaseCurrency()))
                                                    {
                                                        ExchangeRateHelper exchangeRateHelper = context.getType(ExchangeRateHelper.class)
                                                                        .orElseGet(ExchangeRateHelper::new);

                                                        Optional<CurrencyExchangeItem> item = exchangeRateHelper.findItem(v.getStartLineNumber(), money,
                                                                        t.getDateTime().toLocalDate());

                                                        if (item.isPresent())
                                                        {
                                                            v.put("currency", asCurrencyCode(v.get("currency")));
                                                            v.put("exchangeRate", item.get().rate.toString());
                                                            v.put("baseCurrency", asCurrencyCode(item.get().baseCurrency));
                                                            v.put("termCurrency", asCurrencyCode(item.get().termCurrency));

                                                            ExtrExchangeRate rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            Money fxGross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                                                            Money gross = rate.convert(asCurrencyCode(item.get().baseCurrency), fxGross);

                                                            t.setMonetaryAmount(gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());

                                                            context.putType(item.get());
                                                        }
                                                        else
                                                        {
                                                            // @formatter:off
                                                            // Without an exchange rate in the document the amount cannot be
                                                            // converted into the account currency. The booking keeps the amount
                                                            // of the document and is marked as failure, so that it is not
                                                            // skipped without any notice.
                                                            // @formatter:on
                                                            v.markAsFailure(Messages.MsgErrorTransactionMissingExchangeRateIfInForex);
                                                            t.setMonetaryAmount(money);
                                                        }
                                                    }
                                                    else
                                                    {
                                                        t.setMonetaryAmount(money);
                                                    }
                                                })
                                )

                        // @formatter:off
                        // 14-06-2019 07:55 14-06-2019 THE KRAFT HEINZ COMPAN US5007541064 Dividendensteuer USD -0,06 USD -0,06
                        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividendensteuer EUR -0,55 EUR 519,34
                        // 22-03-2021 07:39 19-03-2021 MANULIFE FINANCIAL COR CA56501R1064 Dividend Tax CAD -4.55 CAD -4.55
                        // 11-07-2022 07:45 08-07-2022 LYXOR ETF CAC 40 FR0007052782 Dividendbelasting EUR -0,38 EUR 1,71
                        // 12-11-2021 07:31 11-11-2021 APPLE INC. - COMMON ST US0378331005 Ritenuta sul dividendo USD -0,23 USD -0,23
                        // 30-09-2022 07:25 29-09-2022 T. ROWE PRICE GROUP I US74144T1088 Retención del dividendo USD -4,68 USD -4,68
                        // @formatter:on
                        .section("isin", "currencyTax", "tax").optional()
                        .match("^([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}:[\\d]{2}) "
                                        + "([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                                        + "(.*) "
                                        + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*"
                                        + "(Dividendensteuer"
                                        + "|Dividend Tax"
                                        + "|Dividendbelasting"
                                        + "|Ritenuta sul dividendo"
                                        + "|Imp.ts sur dividende" + "|Imp.ts sur dividende"
                                        + "|Retenci.n del dividendo) "
                                        + "(?<currencyTax>[\\w]{3}) "
                                        + "\\-(?<tax>[\\.,'\\d\\s]+) "
                                        + "[\\w]{3} "
                                        + "(\\-)?[\\.,'\\d\\s]+$")
                        .assign((t, v) -> {
                            DocumentContext context = type.getCurrentContext();
                            Money tax = Money.of(asCurrencyCode(v.get("currencyTax")), asAmount(v.get("tax")));

                            Optional<CurrencyExchangeItem> item = context.getType(CurrencyExchangeItem.class);
                            if (item.isPresent() && v.get("isin").equalsIgnoreCase(t.getSecurity().getIsin()))
                            {
                                Money converted = Money.of(item.get().baseCurrency,
                                                BigDecimal.valueOf(tax.getAmount()).divide(item.get().rate, Values.MC)
                                                                .setScale(0, RoundingMode.HALF_UP).longValue());

                        // @formatter:off
                                // A unit may only carry forex information if the currency of the
                                // security differs from the currency of the transaction and if the
                                // forex currency is the currency of the security. Otherwise the
                                // amount is added without forex information.
                                // @formatter:on
                                if (!converted.getCurrencyCode().equals(t.getSecurity().getCurrencyCode())
                                                && tax.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                                    t.addUnit(new Unit(Unit.Type.TAX, converted, tax,
                                                    BigDecimal.ONE.divide(item.get().rate, Values.MC)));
                                else
                                    t.addUnit(new Unit(Unit.Type.TAX, converted));

                                t.setAmount(t.getAmount() - converted.getAmount());
                            }
                            else if (tax.getCurrencyCode().equals(t.getCurrencyCode())
                                            && v.get("isin").equalsIgnoreCase(t.getSecurity().getIsin()))
                            {
                                t.addUnit(new Unit(Unit.Type.TAX, tax));
                                t.setAmount(t.getAmount() - tax.getAmount());
                            }
                        })

                        // @formatter:off
                        // A dividend tax that is booked before the dividend is not covered by the
                        // block of the dividend. It is taken from the document context and added
                        // here, so that dividend and tax remain one single transaction.
                        //
                        // 28-06-2024 04:43 27-06-2024 CD PROJEKT SA PLOPTTC00011 Dividend Tax PLN -1.90 PLN 8.10
                        // 28-06-2024 04:43 27-06-2024 CD PROJEKT SA PLOPTTC00011 Dividend PLN 10.00 PLN 10.00
                        // @formatter:on
                        .section("isin").optional()
                        .match("^([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}:[\\d]{2}) "
                                        + "([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                                        + "(.*) "
                                        + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                        + "(Dividende"
                                        + "|Dividend"
                                        + "|Fondsaussch.ttung"
                                        + "|Dividendo"
                                        + "|Dividenda) "
                                        + "[\\w]{3} "
                                        + "[\\.,'\\d\\s]+ "
                                        + "[\\w]{3} "
                                        + "(\\-)?[\\.,'\\d\\s]+$")
                        .assign((t, v) -> {
                            DocumentContext context = type.getCurrentContext();

                            DividendTaxHelper dividendTaxHelper = context.getType(DividendTaxHelper.class)
                                            .orElseGet(DividendTaxHelper::new);

                            Optional<DividendeTaxItem> taxTransaction = dividendTaxHelper.findItemAbove(
                                            t.getDateTime().toLocalDate(), v.get("isin"), v.getStartLineNumber());

                            if (!taxTransaction.isPresent())
                                return;

                            Money tax = Money.of(asCurrencyCode(taxTransaction.get().currency),
                                            asAmount(taxTransaction.get().amount));

                            Optional<CurrencyExchangeItem> item = context.getType(CurrencyExchangeItem.class);
                            if (item.isPresent() && v.get("isin").equalsIgnoreCase(t.getSecurity().getIsin()))
                            {
                                Money converted = Money.of(item.get().baseCurrency,
                                                BigDecimal.valueOf(tax.getAmount()).divide(item.get().rate, Values.MC)
                                                                .setScale(0, RoundingMode.HALF_UP).longValue());

                        // @formatter:off
                                // A unit may only carry forex information if the currency of the
                                // security differs from the currency of the transaction and if the
                                // forex currency is the currency of the security. Otherwise the
                                // amount is added without forex information.
                                // @formatter:on
                                if (!converted.getCurrencyCode().equals(t.getSecurity().getCurrencyCode())
                                                && tax.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                                    t.addUnit(new Unit(Unit.Type.TAX, converted, tax,
                                                    BigDecimal.ONE.divide(item.get().rate, Values.MC)));
                                else
                                    t.addUnit(new Unit(Unit.Type.TAX, converted));

                                t.setAmount(t.getAmount() - converted.getAmount());
                            }
                            else if (tax.getCurrencyCode().equals(t.getCurrencyCode())
                                            && v.get("isin").equalsIgnoreCase(t.getSecurity().getIsin()))
                            {
                                t.addUnit(new Unit(Unit.Type.TAX, tax));
                                t.setAmount(t.getAmount() - tax.getAmount());
                            }
                        })

                        // @formatter:off
                        //  06-06-2019 09:00 05-06-2019 SONY CORPORATION COMMO US8356993076 ADR/GDR Weitergabegebühr USD -0,04 USD -0,040
                        // @formatter:on
                        .section("isin", "currencyFee", "feeFx").optional()
                        .match("^([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}:[\\d]{2}) "
                                        + "([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                                        + "(.*) "
                                        + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                        + "ADR\\/GDR Weitergabegeb.hr "
                                        + "(?<currencyFee>[\\w]{3}) "
                                        + "\\-(?<feeFx>[\\.,'\\d\\s]+) "
                                        + "[\\w]{3} "
                                        + "(\\-)?[\\.,'\\d\\s]+$")
                        .assign((t, v) -> {
                            DocumentContext context = type.getCurrentContext();
                            Money fee = Money.of(asCurrencyCode(v.get("currencyFee")), asAmount(v.get("feeFx")));

                            Optional<CurrencyExchangeItem> item = context.getType(CurrencyExchangeItem.class);
                            if (item.isPresent() && v.get("isin").equalsIgnoreCase(t.getSecurity().getIsin()))
                            {
                                Money converted = Money.of(item.get().baseCurrency,
                                                BigDecimal.valueOf(fee.getAmount()).divide(item.get().rate, Values.MC)
                                                                .setScale(0, RoundingMode.HALF_UP).longValue());

                                Unit unit = new Unit(Unit.Type.FEE, converted, fee,
                                                BigDecimal.ONE.divide(item.get().rate, Values.MC));
                                t.addUnit(unit);
                                t.setAmount(t.getAmount() - converted.getAmount());
                            }
                            else if (fee.getCurrencyCode().equals(t.getCurrencyCode()))
                            {
                                t.addUnit(new Unit(Unit.Type.FEE, fee));
                                t.setAmount(t.getAmount() - fee.getAmount());
                            }
                        })

                        .wrap((t, ctx) -> {
                            Optional<CurrencyExchangeItem> currencyExchange = type.getCurrentContext().getType(CurrencyExchangeItem.class);

                            if (currencyExchange.isPresent())
                            {
                                long delta = t.getAmount() - currencyExchange.get().baseAmount;

                                if (Math.abs(delta) == 1)
                                {
                                    t.setAmount(currencyExchange.get().baseAmount);

                                    // pick the first unit and make it fit;
                                    // see discussion
                                    // https://github.com/portfolio-performance/portfolio/pull/1198
                                    Optional<Unit> candidate = t.getUnits()
                                                    .filter(u -> u.getType() == Unit.Type.TAX || u.getType() == Unit.Type.FEE)
                                                    .filter(u -> u.getExchangeRate() != null).findFirst();

                                    if (candidate.isPresent())
                                    {
                                        Unit unit = candidate.get();
                                        t.removeUnit(unit);

                                        long amountPlusDelta = unit.getAmount().getAmount() + delta;
                                        long forexPlusDelta = BigDecimal.ONE
                                                        .divide(unit.getExchangeRate(), 10, RoundingMode.HALF_DOWN)
                                                        .multiply(BigDecimal.valueOf(amountPlusDelta))
                                                        .setScale(0, RoundingMode.HALF_DOWN).longValue();

                                        Unit newUnit = new Unit(unit.getType(),
                                                        Money.of(unit.getAmount().getCurrencyCode(), amountPlusDelta),
                                                        Money.of(unit.getForex().getCurrencyCode(), forexPlusDelta), unit.getExchangeRate());

                                        t.addUnit(newUnit);
                                    }
                                }
                            }

                            type.getCurrentContext().removeType(CurrencyExchangeItem.class);

                            ExtractorUtils.fixGrossValueA().accept(t);

                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        Block blockDividendTax = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}:[\\d]{2} ([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?.*"
                        + "(Dividendensteuer"
                        + "|Dividend Tax"
                        + "|Dividendbelasting"
                        + "|Ritenuta sul dividendo"
                        + "|Retenci.n del dividendo) "
                        + ".*$");
        type.addBlock(blockDividendTax);
        blockDividendTax.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAX_REFUND))

                        // @formatter:off
                        // 16-12-2021 18:26 22-10-2021 VANGUARD TOTAL INTERNA US9219097683 Dividendbelasting USD -10,45 USD -22,78
                        // 16-12-2021 18:26 22-10-2021 VANGUARD TOTAL INTERNA US9219097683 Dividendbelasting USD -5,99 USD -12,33
                        // 16-12-2021 18:26 22-10-2021 VANGUARD TOTAL INTERNA US9219097683 Dividendbelasting USD -4,47 USD -6,34
                        // 16-12-2021 18:26 22-10-2021 VANGUARD TOTAL INTERNA US9219097683 Dividendbelasting USD -1,87 USD -1,87
                        //
                        // 02-05-2019 08:20 02-05-2019 ING GROEP NV EO -,01 NL0011821202 Dividendensteuer EUR 2,20 EUR 129,07
                        // 02-05-2019 08:20 02-05-2019 ING GROEP NV EO -,01 NL0011821202 Dividendensteuer EUR -1,25 EUR 126,87
                        // @formatter:on
                        .section("date", "time", "name", "isin", "note", "currency", "type", "amount").optional()
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                        + "([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                                        + "(?<name>.*) "
                                        + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*"
                                        + "(?<note>Dividendensteuer"
                                        + "|Dividend Tax"
                                        + "|Dividendbelasting"
                                        + "|Ritenuta sul dividendo"
                                        + "|Imp.ts sur dividende" + "|Imp.ts sur dividende"
                                        + "|Retenci.n del dividendo) "
                                        + "(?<currency>[\\w]{3})"
                                        + "(?<type>\\s(\\-)?)"
                                        + "(?<amount>[\\.,'\\d\\s]+) "
                                        + "[\\w]{3} "
                                        + "(\\-)?[\\.,'\\d\\s]+$")
                        .assign((t, v) -> {
                            DocumentContext context = type.getCurrentContext();
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setNote(v.get("note") + ": " + v.get("isin"));

                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.TAXES);

                            // @formatter:off
                            // Sometimes the dividend tax is settled without a dividend transaction.
                            //
                            // To capture this, we note all these dividend transactions and look for whether
                            // this tax belongs to a dividend transaction or not.
                            //
                            // If there is no dividend transaction, then we record this tax.
                            // @formatter:on

                            DividendTransactionHelper dividendTransactionHelper = context.getType(DividendTransactionHelper.class).orElseGet(DividendTransactionHelper::new);
                            Optional<DividendeTransactionsItem> dividendTransaction = dividendTransactionHelper.findItem(t.getDateTime(), t.getSecurity().getIsin());

                            if (!dividendTransaction.isPresent())
                            {
                                Money money = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                                // @formatter:off
                                // A dividend tax with a positive sign is the counter booking of a
                                // cancelled dividend, if that cancelled dividend is booked at the
                                // same time for the same security. Without such a cancellation the
                                // positive amount is a real tax refund and is imported as usual.
                                // @formatter:on
                                boolean isCancellation = !"-".equals(trim(v.get("type"))) && dividendTransactionHelper
                                                .findCancellation(t.getDateTime(), t.getSecurity().getIsin()).isPresent();

                                if (isCancellation)
                                    v.markAsFailure(Messages.MsgErrorTransactionOrderCancellationUnsupported);

                                if (!money.getCurrencyCode().equals(getClient().getBaseCurrency()))
                                {
                                    ExchangeRateHelper exchangeRateHelper = context.getType(ExchangeRateHelper.class)
                                                    .orElseGet(ExchangeRateHelper::new);

                                    Optional<CurrencyExchangeItem> item = exchangeRateHelper.findItem(v.getStartLineNumber(), money,
                                                    t.getDateTime().toLocalDate());

                                    if (item.isPresent())
                                    {
                                        Money converted = Money.of(item.get().baseCurrency,
                                                        BigDecimal.valueOf(money.getAmount())
                                                                        .divide(item.get().rate, Values.MC)
                                                                        .setScale(0, RoundingMode.HALF_UP).longValue());

                                        t.setMonetaryAmount(converted);
                                        t.addUnit(new Unit(Unit.Type.GROSS_VALUE, converted, money,
                                                        BigDecimal.ONE.divide(item.get().rate, Values.MC)));

                                        context.putType(item.get());
                                    }
                                    else
                                    {
                                        // @formatter:off
                                        // Without an exchange rate in the document the amount cannot
                                        // be converted into the account currency. The booking keeps
                                        // the amount of the document and is marked as failure, so
                                        // that it is not skipped without any notice. A cancellation
                                        // already carries its own failure message.
                                        // @formatter:on
                                        if (!isCancellation)
                                            v.markAsFailure(Messages.MsgErrorTransactionMissingExchangeRateIfInForex);

                                        t.setMonetaryAmount(money);
                                    }
                                }
                                else
                                {
                                    t.setMonetaryAmount(money);
                                }
                            }
                        })

                        .wrap(t -> {
                            type.getCurrentContext().removeType(DividendeTransactionsItem.class);
                            type.getCurrentContext().removeType(CurrencyExchangeItem.class);

                            if (t.getCurrencyCode() != null && t.getAmount() != 0L)
                                return new TransactionItem(t);
                            return null;
                        }));

        // @formatter:off
        // Formatting:
        // DateTime | Valutadatum | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
        // or
        // DateTime | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
        // or
        // DateTime | Value date | Product | ISIN | Description | FX Change | Balance
        // or
        // DatumTijd | Valutadatum | Product | ISIN | Omschrijving | FX Mutatie | Saldo
        // or
        // Data Ora | Data Valore | Prodotto | ISIN | Descrizione  | Borsa Variazioni | Saldo
        //  -------------------------------------
        // 31-07-2017 00:00 Zinsen EUR -0,07 EUR -84,16
        // 05-12-2018 16:07 30-11-2018 Zinsen für Leerverkauf EUR -0,04 EUR 220,63
        // 01-03-2021 18:47 28-02-2021 Interest EUR -0.88 EUR -1,644.64
        // 01-04-2021 21:00 31-03-2021 Flatex Interest EUR -0,15 EUR 117,98
        // 02-07-2021 07:41 30-06-2021 Flatex Interest Income EUR 0,00 EUR 0,00
        // 02-11-2020 16:06 31-10-2020 Interesse EUR -0,01 EUR -2,51
        // 01-10-2022 09:31 30-09-2022 Flatex Interest EUR -2,15 EUR 117,84
        // 02-07-2022 06:20 30-06-2022 Flatex Interest EUR -5,31 EUR 5.395,07
        // 02-01-2023 13:41 02-01-2023 Flatex Interest Income EUR 0,00 EUR 217,97
        // @formatter:on
        Block blockInterest = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}:[\\d]{2} ([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?.*"
                        + "(Zinsen"
                        + "|Interest"
                        + "|Interesse) "
                        + ".*$");
        type.addBlock(blockInterest);
        blockInterest.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        .section("date", "time", "note", "currency", "type", "amount")
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                        + "([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                                        + "(?<note>(Flatex )?"
                                        + "(Zinsen"
                                        + "|Interest"
                                        + "|Interesse)"
                                        + "( (f.r Leerverkauf|Income))?) "
                                        + "(?<currency>[\\w]{3})"
                                        + "(?<type>\\s(\\-)?)"
                                        + "(?<amount>[\\.,'\\d\\s]+) "
                                        + "[\\w]{3} "
                                        + "(\\-)?[\\.,'\\d\\s]+$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));

                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        // @formatter:off
        // Formatting:
        // DateTime | Valutadatum | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
        // or
        // DateTime | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
        // or
        // DateTime | Value date | Product | ISIN | Description | FX Change | Balance
        // or
        // DatumTijd | Valutadatum | Product | ISIN | Omschrijving | FX Mutatie | Saldo
        // or
        // Data Ora | Data Valore | Prodotto | ISIN | Descrizione  | Borsa Variazioni | Saldo
        // -------------------------------------
        // 04-01-2019 20:14 04-01-2019 SOFORT Zahlungsgebühr EUR -1,00 EUR 150,00
        // 29-06-2018 15:25 29-06-2018 SOFORT Zahlungsgebühr EUR -2,00 EUR 1.683,05
        // @formatter:on
        Block blockDepositFee = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}:[\\d]{2} ([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                        + "SOFORT Zahlungsgeb.*hr"
                        + ".*$");
        type.addBlock(blockDepositFee);
        blockDepositFee.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.FEES))

                        .section("date", "time", "note", "currency", "amount")
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                        + "([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                                        + "(?<note>SOFORT Zahlungsgeb.*hr) "
                                        + "(?<currency>[\\w]{3}) "
                                        + "(\\-)?(?<amount>[\\.,'\\d\\s]+) "
                                        + "[\\w]{3} "
                                        + "(\\-)?[\\.,'\\d\\s]+$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        // @formatter:off
        // Formatting:
        // DateTime | Valutadatum | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
        // or
        // DateTime | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
        // or
        // DateTime | Value date | Product | ISIN | Description | FX Change | Balance
        // or
        // DatumTijd | Valutadatum | Product | ISIN | Omschrijving | FX Mutatie | Saldo
        // or
        // Data Ora | Data Valore | Prodotto | ISIN | Descrizione  | Borsa Variazioni | Saldo
        // -------------------------------------
        // 03-07-2019 11:47 30-06-2019 Einrichtung von Handelsmodalitäten 2019 (New York Stock EUR -0,54 EUR 22,16
        // 01-02-2019 16:31 31-01-2019 Einrichtung von Handelsmodalitäten 2019 (NASDAQ - NDQ) EUR 2,50 EUR 108,21
        //
        // 30-06-2017 00:00 Einrichtung von EUR -2,50 EUR 1.116,79
        // Handelsmodalitäten
        // 2017
        //
        // 31-08-2017 00:00 Einrichtung von EUR -0,89 EUR 35,63
        // Handelsmodalitäten
        // 2017
        //
        // 02-09-2021 21:29 31-08-2021 DEGIRO Aansluitingskosten 2021 (Borsa Italiana S.p.A. - EUR -2,50 EUR 12,91
        // MIL)
        //
        // 01-06-2022 19:55 31-05-2022 Giro Exchange Connection Fee 2022 EUR -1,01 EUR -6,39
        // 02-10-2021 10:46 30-09-2021 DEGIRO Costi di connessione 2021 (Xetra - XET) EUR -1,24 EUR 206,99
        // 05-07-2022 08:32 30-06-2022 Giro Exchange Connection Fee 2022 EUR -2,50 EUR 7.390,07
        //
        // 28-04-2022 15:33 27-04-2022 JINKOSOLAR HOLDING COM US47759T1007 ADR/GDR Weitergabegebühr USD -0,01 USD 0,64
        // 29-04-2022 17:34 18-08-2021 GAZPROM PAO US3682872078 ADR/GDR Weitergabegebühr USD 7,12 USD 11,93
        //
        // 03-01-2023 14:01 31-12-2022 DEGIRO poplatek za Obchodování na zahraničních burzách EUR -2,50 EUR 212,97
        // 2023 (Euronext Amsterdam - EAM)
        //
        // 03-01-2023 14:00 31-12-2022 DEGIRO Aansluitingskosten 2023 (NYSE Arca - NYA) EUR -2,50 EUR 1.426,00
        // 03-01-2023 14:00 31-12-2022 DEGIRO Aansluitingskosten 2023 (NASDAQ - NDQ) EUR -2,50 EUR 1.428,50
        // @formatter:on
        Block blockTrademodalities = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}:[\\d]{2} ([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?.*"
                        + "(Einrichtung von"
                        + "|DEGIRO Aansluitingskosten"
                        + "|Giro Exchange Connection Fee"
                        + "|DEGIRO Costi di connessione"
                        + "|DEGIRO poplatek za Obchodování"
                        + "|Frais de connexion aux places boursi.res"
                        + "|ADR\\/GDR Weitergabegeb.hr) "
                        + ".*$");
        type.addBlock(blockTrademodalities);
        blockTrademodalities.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.FEES_REFUND))

                        .oneOf(
                                section -> section
                                    .attributes("date", "time", "note1", "note2", "note3", "currency", "type", "amount")
                                    .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                    + "([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                                                    + "(?<note1>Einrichtung von) "
                                                    + "(?<currency>[\\w]{3})"
                                                    + "(?<type>\\s(\\-)?)"
                                                    + "(?<amount>[\\.,'\\d\\s]+) "
                                                    + "[\\w]{3} "
                                                    + "(\\-)?[\\.,'\\d\\s]+$")
                                    .match("^(?<note2>Handelsmodalit.ten)$")
                                    .match("^(?<note3>[\\d]{4})$")
                                    .assign((t, v) -> {
                                        t.setDateTime(asDate(v.get("date"), v.get("time")));
                                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        t.setAmount(asAmount(v.get("amount")));
                                        t.setNote(v.get("note1") + " " + v.get("note2") + " " + v.get("note3"));

                                        if ("-".equals(trim(v.get("type"))))
                                            t.setType(AccountTransaction.Type.FEES);
                                    }),
                                section -> section
                                    .attributes("date", "time", "note1", "currency", "type", "amount", "note2")
                                    .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                    + "([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                                                    + "(?<note1>(Einrichtung von Handelsmodalit.ten"
                                                    + "|DEGIRO Aansluitingskosten"
                                                    + "|Giro Exchange Connection Fee"
                                                    + "|DEGIRO Costi di connessione"
                                                                        + "|DEGIRO poplatek za Obchodov.n."
                                                                        + "|Frais de connexion aux places boursi.res)) .* "
                                                    + "(?<currency>[\\w]{3})"
                                                    + "(?<type>\\s(\\-)?)"
                                                    + "(?<amount>[\\.,'\\d\\s]+) "
                                                    + "[\\w]{3} "
                                                    + "(\\-)?[\\.,'\\d\\s]+$")
                                    .match("^(?<note2>[\\d]{4} \\(.*\\)).*$")
                                    .assign((t, v) -> {
                                        t.setDateTime(asDate(v.get("date"), v.get("time")));
                                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        t.setAmount(asAmount(v.get("amount")));
                                        t.setNote(v.get("note1") + " " + v.get("note2"));

                                        if ("-".equals(trim(v.get("type"))))
                                            t.setType(AccountTransaction.Type.FEES);
                                    }),
                                        // @formatter:off
                                // 03-07-2019 11:47 30-06-2019 Einrichtung von Handelsmodalitäten 2019 (New York Stock EUR -0,54 EUR 22,16
                                // Exchange - NSY)
                                //
                                // 07-04-2026 09:44 31-03-2026 Frais de connexion aux places boursières 2026 (London EUR -2,50 EUR 5 487,72
                                // Stock Exchange (LSE) - LSE)
                                // @formatter:on
                                section -> section
                                                        .attributes("date", "time", "note1", "note2", "currency",
                                                                        "type", "amount", "note3")
                                                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                                        + "([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                                                                        + "(?<note1>(Einrichtung von Handelsmodalit.ten"
                                                                        + "|DEGIRO Aansluitingskosten"
                                                                        + "|Giro Exchange Connection Fee"
                                                                        + "|DEGIRO Costi di connessione"
                                                                        + "|DEGIRO poplatek za Obchodov.n."
                                                                        + "|Frais de connexion aux places boursi.res)( [\\d]{4})?) "
                                                                        + "(?<note2>\\([^\\)]*) "
                                                                        + "(?<currency>[\\w]{3})" + "(?<type>\\s(\\-)?)"
                                                                        + "(?<amount>[\\.,'\\d\\s]+) " + "[\\w]{3} "
                                                                        + "(\\-)?[\\.,'\\d\\s]+$")
                                                        .match("^(?<note3>.*\\))$").assign((t, v) -> {
                                                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setNote(v.get("note1") + " " + v.get("note2") + " "
                                                                            + v.get("note3"));

                                                            if ("-".equals(trim(v.get("type"))))
                                                                t.setType(AccountTransaction.Type.FEES);
                                                        }),
                                        section -> section
                                    .attributes("date", "time", "note", "currency", "type", "amount")
                                    .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                    + "([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                                                    + "(?<note>(Einrichtung von Handelsmodalit.ten"
                                                    + "|DEGIRO Aansluitingskosten"
                                                    + "|Giro Exchange Connection Fee"
                                                    + "|DEGIRO Costi di connessione"
                                                                        + "|DEGIRO poplatek za Obchodov.n."
                                                                        + "|Frais de connexion aux places boursi.res)( [\\d]{4})?( \\(.*\\))?).* "
                                                    + "(?<currency>[\\w]{3})"
                                                    + "(?<type>\\s(\\-)?)"
                                                    + "(?<amount>[\\.,'\\d\\s]+) "
                                                    + "[\\w]{3} "
                                                    + "(\\-)?[\\.,'\\d\\s]+$")
                                    .assign((t, v) -> {
                                        t.setDateTime(asDate(v.get("date"), v.get("time")));
                                        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        t.setAmount(asAmount(v.get("amount")));
                                        t.setNote(v.get("note"));

                                        if ("-".equals(trim(v.get("type"))))
                                            t.setType(AccountTransaction.Type.FEES);
                                    }),
                                section -> section
                                    .attributes("date", "time", "isin", "note", "currency", "type", "amount")
                                    .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                    + "([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                                                    + "(.*) "
                                                    + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                    + "(?<note>ADR\\/GDR Weitergabegeb.hr) "
                                                    + "(?<currency>[\\w]{3})"
                                                    + "(?<type>\\s(\\-)?)(?<amount>[\\.,'\\d\\s]+) "
                                                    + "[\\w]{3} "
                                                    + "(\\-)?[\\.,'\\d\\s]+$")
                                    .assign((t, v) -> {
                                        DocumentContext context = type.getCurrentContext();
                                        t.setSecurity(getOrCreateSecurity(v));
                                        t.setDateTime(asDate(v.get("date"), v.get("time")));
                                        t.setNote(t.getSecurity().getIsin() + ": " + v.get("note"));

                                        if ("-".equals(trim(v.get("type"))))
                                            t.setType(AccountTransaction.Type.FEES);

                                        // @formatter:off
                                        // Sometimes the ADR/GDR transfer fee is settled without a dividend transaction.
                                        //
                                        // To capture this, we remember all this dividend transactions and search for whether
                                        // these charges belong to a dividend transaction or not.
                                        //
                                        // If there is no dividend transaction, then we record this fee.
                                        // @formatter:on

                                        DividendTransactionHelper dividendTransactionHelper = context.getType(DividendTransactionHelper.class).orElseGet(DividendTransactionHelper::new);
                                        Optional<DividendeTransactionsItem> dividendTransaction = dividendTransactionHelper.findItem(t.getDateTime(), t.getSecurity().getIsin());

                                        if (!dividendTransaction.isPresent())
                                        {
                                            Money money = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));

                                            if (!money.getCurrencyCode().equals(getClient().getBaseCurrency()))
                                            {
                                                ExchangeRateHelper exchangeRateHelper = context.getType(ExchangeRateHelper.class)
                                                                .orElseGet(ExchangeRateHelper::new);

                                                Optional<CurrencyExchangeItem> item = exchangeRateHelper.findItem(v.getStartLineNumber(), money,
                                                                t.getDateTime().toLocalDate());

                                                if (item.isPresent())
                                                {
                                                    Money converted = Money.of(item.get().baseCurrency,
                                                                    BigDecimal.valueOf(money.getAmount())
                                                                                    .divide(item.get().rate, Values.MC)
                                                                                    .setScale(0, RoundingMode.HALF_UP).longValue());

                                                    t.setMonetaryAmount(converted);
                                                    t.addUnit(new Unit(Unit.Type.GROSS_VALUE, converted, money,
                                                                    BigDecimal.ONE.divide(item.get().rate, Values.MC)));

                                                    context.putType(item.get());
                                                }
                                                else
                                                {
                                                    // @formatter:off
                                                    // Without an exchange rate in the document the amount cannot be
                                                    // converted into the account currency. The booking keeps the amount
                                                    // of the document and is marked as failure, so that it is not
                                                    // skipped without any notice.
                                                    // @formatter:on
                                                    v.markAsFailure(Messages.MsgErrorTransactionMissingExchangeRateIfInForex);
                                                    t.setMonetaryAmount(money);
                                                }
                                            }
                                            else
                                            {
                                                t.setMonetaryAmount(money);
                                            }
                                        }
                                    })
                        )

                        .wrap(t -> {
                            type.getCurrentContext().removeType(DividendeTransactionsItem.class);
                            type.getCurrentContext().removeType(CurrencyExchangeItem.class);

                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        // @formatter:off
        // Formatting:
        // DateTime | Valutadatum | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
        // or
        // DateTime | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
        // or
        // DateTime | Value date | Product | ISIN | Description | FX Change | Balance
        // or
        // DatumTijd | Valutadatum | Product | ISIN | Omschrijving | FX Mutatie | Saldo
        // or
        // Data Ora | Data Valore | Prodotto | ISIN | Descrizione  | Borsa Variazioni | Saldo
        // -------------------------------------
        // 01-03-2019 13:21 01-03-2019 ODX1 C11500.00 01MAR19 DE000C27U079 Gebühr für Ausübung/Zuteilung EUR -1,00 EUR 1.942,75
        // 22-03-2019 14:10 22-03-2019 ODX4 P11550.00 22MAR19 DE000C2ZNL25 Gebühr für Ausübung/Zuteilung EUR -1,00 EUR 2.204,26
        // 26-07-2019 13:52 26-07-2019 ODX4 P12400.00 26JUL19 DE000C3727R2 Gebühr für Ausübung/Zuteilung EUR -2,00 EUR 1.225,59
        // 01-03-2019 13:21 01-03-2019 ODX1 C11500.00 01MAR19 DE000C27U079 Gebühr für Ausübung/Zuteilung EUR -1,00 EUR 1.942,75
        // @formatter:on
        Block blockFeeStrike = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}:[\\d]{2} ([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?.* "
                        + "Geb.hr für Aus.bung\\/Zuteilung "
                        + ".*$");
        type.addBlock(blockFeeStrike);
        blockFeeStrike.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.FEES))

                        .section("date", "time", "name", "isin", "note", "currency", "amount")
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                        + "([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                                        + "(?<name>.*) "
                                        + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                        + "(?<note>Geb.hr für Aus.bung\\/Zuteilung) "
                                        + "(?<currency>[\\w]{3}) "
                                        + "\\-(?<amount>[\\.,'\\d\\s]+) "
                                        + "[\\w]{3} "
                                        + "(\\-)?[\\.,'\\d\\s]+$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));

        // @formatter:off
        // Formatting:
        // DateTime | Valutadatum | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
        // or
        // DateTime | Produkt | ISIN | Beschreibung | FX Änderung | Saldo
        // or
        // DateTime | Value date | Product | ISIN | Description | FX Change | Balance
        // or
        // DatumTijd | Valutadatum | Product | ISIN | Omschrijving | FX Mutatie | Saldo
        // or
        // Data Ora | Data Valore | Prodotto | ISIN | Descrizione  | Borsa Variazioni | Saldo
        // -------------------------------------
        // 05-03-2019 15:37 28-02-2019 Gutschrift für die Neukundenaktion EUR 18,00 EUR 1.960,69
        // 05-03-2019 15:37 28-02-2019 Rabatt für 500 Euro Aktion EUR 18,00 EUR 1.960,64
        // @formatter:on
        Block blockFeeReturn = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}:[\\d]{2}( [\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                        + "(Rabatt|Gutschrift|Remboursement) "
                        + ".*$");
        type.addBlock(blockFeeReturn);
        blockFeeReturn.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.FEES_REFUND))

                        .section("date", "time", "note", "currency", "amount")
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                        + "([\\d]{2}\\-[\\d]{2}\\-[\\d]{4} )?"
                                        + "(?<note>(Rabatt|Gutschrift|Remboursement) .*) "
                                        + "(?<currency>[\\w]{3}) "
                                        + "(?<amount>[\\.,'\\d\\s]+) "
                                        + "[\\w]{3} "
                                        + "(\\-)?[\\.,'\\d\\s]+$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getAmount() != 0)
                                return new TransactionItem(t);
                            return null;
                        }));
    }

    /**
     * Information:
     *
     * If you make changes to the regEx, the examples will help you
     * to adjust it correctly. Always check where and on which side the
     * currency, amount, exchange rate and fees are!
     *
     * Please do not delete these examples!
     */
    private void addDepotStatementTransactions()
    {
        final var type = new DocumentType("(Transaktions.bersicht"
                        + "|Transacciones"
                        + "|Transacties"
                        + "|Transactions"
                        + "|Transakcje"
                        + "|Operazioni"
                        + "|Transakce"
                        + "|Transa..es)", (context, lines) -> {
            // @formatter:off
                            // The account currency is not part of the transaction line. It is
                            // taken from the table header of the report.
                            //
                            // exchange Venue Quantity Price Local value Value EUR Exchange AutoFX and/or third
                            // d'exécution locale CHF change conversion
                            // Lokalwährung Wert EUR Wec
                            // @formatter:on
                            Pattern pCurrencyAccount = Pattern.compile(
                                            "^.*(Local value Value|locale|Lokalw.hrung Wert) (?<currency>[A-Z]{3}) .*$");

            // @formatter:off
                            // The name of the security can be spread over several lines.
                            //
                            // 04-02-2026 10:32 WISDOMTREE PHYSICAL JE00B1VS3333 EAM XAMS 30 68,3760 EUR -2.051,28 EUR -2.051,28 0,00 -3,00 -2.054,28
                            // SILVER INDIVIDUAL
                            // SECURITIES ETC
                            //
                            // The continuation lines are collected here and are added to the name
                            // of the security afterwards.
                            // @formatter:on
                            TransactionReportNameContinuationHelper nameContinuationHelper = new TransactionReportNameContinuationHelper();

                            for (int i = 0; i < lines.length; i++)
                            {
                                Matcher m = pCurrencyAccount.matcher(lines[i]);
                                if (m.matches() && !context.containsKey("currencyAccount"))
                                    context.put("currencyAccount", asCurrencyCode(m.group("currency")));

                                if (!TRANSACTION_REPORT_START.matcher(lines[i]).matches())
                                    continue;

                                List<String> fragments = new ArrayList<>();
                                boolean skipPageHeaderOrFooter = false;

                                for (int j = i + 1; j < lines.length; j++)
                                {
                                    if (TRANSACTION_REPORT_START.matcher(lines[j]).matches())
                                        break;

                                    String line = trim(lines[j]);

                                    if (line != null && isTransactionReportHeaderOrFooter(line))
                                    {
                                        skipPageHeaderOrFooter = true;
                                        continue;
                                    }

                                    if (skipPageHeaderOrFooter)
                                    {
                                        if (isStandaloneTransactionReportHeaderCurrency(lines, j))
                                            skipPageHeaderOrFooter = false;

                                        continue;
                                    }

                                    if (!isTransactionReportNameContinuation(lines, j))
                                        break;

                                    fragments.add(line);
                                }

                                if (!fragments.isEmpty())
                                    nameContinuationHelper.put(i, String.join(" ", fragments));
                            }

                            context.putType(nameContinuationHelper);
                        });
        this.addDocumentTyp(type);

        Block blockBuy = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}:[\\d]{2} .* [A-Z]{2}[A-Z0-9]{9}[0-9] .* ([\\w]{3}|[\\w]{3} [\\w]{4}) .*([\\.\\d]+,[\\d]{2}|[\\w]{3})([\\s]+)?$");
        type.addBlock(blockBuy);

        blockBuy.set(new Transaction<BuySellEntry>()

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        .oneOf(
                            // @formatter:off
                            // without exchange rate
                            // with fee
                            // with stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Place | Shares | Quote | Local market value | Market value | AutoFX fee | Fee | Total amount
                            // -------------------------------------
                            // 04-02-2026 10:32 WISDOMTREE PHYSICAL JE00B1VS3333 EAM XAMS 30 68,3760 EUR -2.051,28 EUR -2.051,28 0,00 -3,00 -2.054,28
                            // 17-12-2024 11:08 AEGON LTD BMG0112X1056 EAM XAMS -211 5,6260 EUR 1.187,09 EUR 1.187,09 0,00 -3,00 1.184,09
                            // 10-11-2023 17:30 COMPAGNIE FINANCIERE CH0210483332 SWX XSWX 3 106,7000 CHF -320,10 CHF -320,10 0,00 -6,00 -326,10
                            // @formatter:on
                            section -> section
                                .id("documentContext-withoutExchangeRate-withFee-withStockExchangePlace")
                                .attributes("date", "time", "name", "isin", "shares", "currency", "fee", "amount")
                                .documentContext("currencyAccount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} [\\w]{4} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2,6} "
                                                + "(?<currency>[\\w]{3}) (\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} "
                                                + "[\\w]{3} (\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} "
                                                + "0[\\.,]00 "
                                                + "\\-(?<fee>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) "
                                                + "(\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|,][\\d]{2})$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(v.get("currencyAccount"));
                                    t.setAmount(asAmount(v.get("amount")));

                                    // This layout always uses the comma as decimal separator,
                                    // e.g. 1.500 are 1500 shares and not 1.5 shares.
                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", ""), Locale.GERMANY));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares"), Locale.GERMANY));
                                    }

                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, //
                                                    Money.of(v.get("currencyAccount"), asAmount(v.get("fee")))));
                                }),

                            // @formatter:off
                            // without exchange rate
                            // with fee
                            // without stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Place | Shares | Quote | Local market value | Market value | AutoFX fee | Fee | Total amount
                            // -------------------------------------
                            // 24-09-2020 09:05 ROYAL DUTCH SHELLA GB00B03MLX29 EAM 92 11,2200 EUR -1.032,24 EUR -1.032,24 0,00 -2,31 -1.034,55
                            // @formatter:on
                            section -> section
                                .id("documentContext-withoutExchangeRate-withFee-withoutStockExchangePlace")
                                .attributes("date", "time", "name", "isin", "shares", "currency", "fee", "amount")
                                .documentContext("currencyAccount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2,6} "
                                                + "(?<currency>[\\w]{3}) (\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} "
                                                + "[\\w]{3} (\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} "
                                                + "0[\\.,]00 "
                                                + "\\-(?<fee>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) "
                                                + "(\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|,][\\d]{2})$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(v.get("currencyAccount"));
                                    t.setAmount(asAmount(v.get("amount")));

                                    // This layout always uses the comma as decimal separator,
                                    // e.g. 1.500 are 1500 shares and not 1.5 shares.
                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", ""), Locale.GERMANY));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares"), Locale.GERMANY));
                                    }

                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, //
                                                    Money.of(v.get("currencyAccount"), asAmount(v.get("fee")))));
                                }),

                            // @formatter:off
                            // without exchange rate
                            // without fee
                            // with stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Place | Shares | Quote | Local market value | Market value | AutoFX fee | Total amount
                            // -------------------------------------
                            // 28-02-2024 09:07 SBM OFFSHORE NV NL0000360618 EAM XAMS 91 12,9500 EUR -1.178,45 EUR -1.178,45 0,00 -1.178,45
                            // @formatter:on
                            section -> section
                                .id("documentContext-withoutExchangeRate-withoutFee-withStockExchangePlace")
                                .attributes("date", "time", "name", "isin", "shares", "currency", "amount")
                                .documentContext("currencyAccount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} [\\w]{4} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2,6} "
                                                + "(?<currency>[\\w]{3}) (\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} "
                                                + "[\\w]{3} (\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} "
                                                + "0[\\.,]00 "
                                                + "(\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|,][\\d]{2})$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(v.get("currencyAccount"));
                                    t.setAmount(asAmount(v.get("amount")));

                                    // This layout always uses the comma as decimal separator,
                                    // e.g. 1.500 are 1500 shares and not 1.5 shares.
                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", ""), Locale.GERMANY));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares"), Locale.GERMANY));
                                    }
                                }),

                            // @formatter:off
                            // without exchange rate
                            // without fee
                            // without stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Place | Shares | Quote | Local market value | Market value | AutoFX fee | Total amount
                            // -------------------------------------
                            // 03-04-2024 10:10 CMB TECH NV BE0003816338 EBR -145 16,4100 EUR 2.379,45 EUR 2.379,45 0,00 2.379,45
                            // 25-03-2022 00:00 ATARI DS FR0014008D33 EPA -1.412 0,0000 EUR 0,00 EUR 0,00 0,00 0,00
                            // @formatter:on
                            section -> section
                                .id("documentContext-withoutExchangeRate-withoutFee-withoutStockExchangePlace")
                                .attributes("date", "time", "name", "isin", "shares", "currency", "amount")
                                .documentContext("currencyAccount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2,6} "
                                                + "(?<currency>[\\w]{3}) (\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} "
                                                + "[\\w]{3} (\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} "
                                                + "0[\\.,]00 "
                                                + "(\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|,][\\d]{2})$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(v.get("currencyAccount"));
                                    t.setAmount(asAmount(v.get("amount")));

                                    // This layout always uses the comma as decimal separator,
                                    // e.g. 1.500 are 1500 shares and not 1.5 shares.
                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", ""), Locale.GERMANY));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares"), Locale.GERMANY));
                                    }
                                }),

                            // @formatter:off
                            // with exchange rate
                            // with AutoFX fee
                            // with fee
                            // with stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Place | Shares | Quote | Local market value | Market value | Exchange rate | AutoFX fee | Fee | Total amount
                            // -------------------------------------
                            // 16-01-2026 21:59 LIGHTWAVE LOGIC INC US5322751042 NDQ XNAS 130 4,6000 USD -598,00 USD -515,52 1,1600 -1,29 -2,00 -518,81
                            // 16-10-2025 18:33 RIGETTI COMPUTING INC US76655K1034 NDQ JNST -100 48,3807 USD 4.838,07 USD 4.144,34 1,1674 -10,36 -2,00 4.131,98
                            // 01-12-2025 19:04 NORDEA BANK ABP FI4000297767 TDG XGAT -100 15,3650 EUR 1 536,50 EUR 1 434,95 1,0708 -3,59 -3,65 1 431,30
                            // @formatter:on
                            section -> section
                                .id("documentContext-withExchangeRate-withAutoFxFee-withFee-withStockExchangePlace")
                                                        .attributes("date", "time", "name", "isin", "shares",
                                                                        "currency", "amountFx", "gross", "exchangeRate",
                                                                        "fxFee", "fee", "amount")
                                .documentContext("currencyAccount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} [\\w]{4} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2,6} "
                                                + "(?<currency>[\\w]{3}) (\\-)?(?<amountFx>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) "
                                                                        + "[\\w]{3} (\\-)?(?<gross>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) "
                                                + "(?<exchangeRate>[\\.,'\\d]+[\\.|,][\\d]{1,4}) "
                                                + "\\-(?<fxFee>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) "
                                                + "\\-(?<fee>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) "
                                                + "(\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|,][\\d]{2})$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(v.get("currencyAccount"));
                                    t.setAmount(asAmount(v.get("amount")));

                                    // This layout always uses the comma as decimal separator,
                                    // e.g. 1.500 are 1500 shares and not 1.5 shares.
                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", ""), Locale.GERMANY));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares"), Locale.GERMANY));
                                    }

                                    var fxFeeAmount = Money.of(v.get("currencyAccount"), asAmount(v.get("fxFee")));
                                    var feeAmount = Money.of(v.get("currencyAccount"), asAmount(v.get("fee")));
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, fxFeeAmount));
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feeAmount));

                                    var currencyFx = asCurrencyCode(v.get("currency"));

                                    if (currencyFx.equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                                    {
                                                                var gross = Money.of(v.get("currencyAccount"),
                                                                                asAmount(v.get("gross")));

                                        var exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
                                        var forex = Money.of(currencyFx, asAmount(v.get("amountFx")));
                                        t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate));
                                    }
                                }),

                            // @formatter:off
                            // with exchange rate
                            // with AutoFX fee
                            // without fee
                            // with stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Place | Shares | Quote | Local market value | Market value | Exchange rate | AutoFX fee | Total amount
                            // -------------------------------------
                            // 07-01-2026 21:26 EURO SUN MINING INC CA29872L2066 TOR XTSE 1.500 0,3850 CAD -577,50 CAD -357,01 1,6176 -0,89 -357,91
                            // 22-04-2025 14:39 AMUNDI MSCI CHINA UCITS LU1841731745 XET XETA -20 16,3240 EUR 326,48 EUR 304,28 1,0730 -0,76 304,28
                            // @formatter:on
                            section -> section
                                .id("documentContext-withExchangeRate-withAutoFxFee-withStockExchangePlace")
                                                        .attributes("date", "time", "name", "isin", "shares",
                                                                        "currency", "amountFx", "gross", "exchangeRate",
                                                                        "fxFee", "amount")
                                .documentContext("currencyAccount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} [\\w]{4} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2,6} "
                                                + "(?<currency>[\\w]{3}) (\\-)?(?<amountFx>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) "
                                                                        + "[\\w]{3} (\\-)?(?<gross>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) "
                                                + "(?<exchangeRate>[\\.,'\\d]+[\\.|,][\\d]{1,4}) "
                                                + "\\-(?<fxFee>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) "
                                                + "(\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|,][\\d]{2})$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(v.get("currencyAccount"));
                                    t.setAmount(asAmount(v.get("amount")));

                                    // This layout always uses the comma as decimal separator,
                                    // e.g. 1.500 are 1500 shares and not 1.5 shares.
                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", ""), Locale.GERMANY));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares"), Locale.GERMANY));
                                    }

                                    var fxFeeAmount = Money.of(v.get("currencyAccount"), asAmount(v.get("fxFee")));
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, fxFeeAmount));

                                    var currencyFx = asCurrencyCode(v.get("currency"));

                                    if (currencyFx.equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                                    {
                                                                var gross = Money.of(v.get("currencyAccount"),
                                                                                asAmount(v.get("gross")));

                                        var exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
                                        var forex = Money.of(currencyFx, asAmount(v.get("amountFx")));
                                        t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate));
                                    }
                                }),

                            // @formatter:off
                            // with exchange rate
                            // without fee
                            // without stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Shares | Quote | Local market value | Market value | Exchange rate | AutoFX fee | Total amount
                            // -------------------------------------
                            // 30-01-2026 00:00 CIVITAS RESOURCES INC US17888H1032 NSY -40 27,3800 USD 1.095,20 USD 928,84 1,1791 0,00 928,84
                            // 13-06-2023 00:00 UBS GROUP AG REGISTERE CH0244767585 NSY 6 19,9128 USD -119,48 USD -108,16 1,1047 0,00 -108,16
                            // @formatter:on
                            section -> section
                                .id("documentContext-withExchangeRate-withoutFee-withoutStockExchangePlace")
                                                        .attributes("date", "time", "name", "isin", "shares",
                                                                        "currency", "amountFx", "gross", "exchangeRate",
                                                                        "amount")
                                .documentContext("currencyAccount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2,6} "
                                                + "(?<currency>[\\w]{3}) (\\-)?(?<amountFx>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) "
                                                                        + "[\\w]{3} (\\-)?(?<gross>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) "
                                                + "(?<exchangeRate>[\\.,'\\d]+[\\.|,][\\d]{1,4}) "
                                                + "0[\\.,]00 "
                                                + "(\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|,][\\d]{2})$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(v.get("currencyAccount"));
                                    t.setAmount(asAmount(v.get("amount")));

                                    // This layout always uses the comma as decimal separator,
                                    // e.g. 1.500 are 1500 shares and not 1.5 shares.
                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", ""), Locale.GERMANY));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares"), Locale.GERMANY));
                                    }

                                    var currencyFx = asCurrencyCode(v.get("currency"));

                                    if (currencyFx.equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                                    {
                                                                var gross = Money.of(v.get("currencyAccount"),
                                                                                asAmount(v.get("gross")));

                                        var exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
                                        var forex = Money.of(currencyFx, asAmount(v.get("amountFx")));
                                        t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate));
                                    }
                                }),

                            // @formatter:off
                            // with exchange rate
                            // with fee
                            // without stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Shares | Quote | Amount in exchange rate | Market value | Local market value | Fee | Total amount
                            // -------------------------------------
                            // 26-04-2019 17:52 TESLA MOTORS INC. - C US88160R1014 NDQ 2 USD 240,00 USD -480,00 EUR -430,69 1,1145 EUR -0,51 EUR -431,20
                            // 29-04-2019 16:11 TESLA MOTORS INC. - C US88160R1014 NDQ -3 USD 240,00 USD 720,00 EUR 645,04 1,1162 EUR -0,51 EUR 644,53
                            // 06-08-2019 20:20 WILLIAMS-SONOMA INC. US9699041011 NSY 1 USD 64,695 USD -64,70 EUR -57,85 1,1184 EUR -0,50 EUR -58,35
                            // 23-07-2019 15:30 RIO TINTO PLC COMMON S US7672041008 NSY -3 USD 61,04 USD 183,12 EUR 163,83 1,1177 EUR -0,51 EUR 163,32
                            // @formatter:on
                            section -> section
                                .id("withExchangeRate-withFee-withoutStockExchangePlace")
                                .attributes("date", "time", "name", "isin", "shares", "currency", "amountFx", "exchangeRate", "currencyFee", "fee", "currencyAccount", "amount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "[\\w]{3} (\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2,6} "
                                                + "(?<currency>[\\w]{3}) (\\-)?(?<amountFx>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) "
                                                + "[\\w]{3} (\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} "
                                                + "(?<exchangeRate>[\\.,'\\d\\s]+[\\.|,][\\d]{1,4}) "
                                                + "(?<currencyFee>[\\w]{3}) (\\-)?(?<fee>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) "
                                                + "([\\s]+)?(?<currencyAccount>[\\w]{3}) (\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|,][\\d]{2})([\\s]+)?$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currencyAccount")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }

                                    Money feeAmount = Money.of(asCurrencyCode(v.get("currencyFee")), asAmount(v.get("fee")));
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feeAmount));

                                    long amountFx = asAmount(v.get("amountFx"));
                                    String currencyFx = asCurrencyCode(v.get("currency"));

                                    if (currencyFx.equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                                    {
                                        Money amount = Money.of(asCurrencyCode(v.get("currencyAccount")), asAmount(v.get("amount")));
                                        if (t.getPortfolioTransaction().getType() == PortfolioTransaction.Type.BUY)
                                        {
                                            amount = amount.subtract(feeAmount);
                                        }
                                        else
                                        {
                                            amount = amount.add(feeAmount);
                                        }
                                        BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
                                        Money forex = Money.of(asCurrencyCode(v.get("currency")), amountFx);
                                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                                        t.getPortfolioTransaction().addUnit(grossValue);
                                    }
                            }),

                            // @formatter:off
                            // with exchange rate
                            // without fee
                            // with stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Place | Shares | Quote | Amount in exchange rate | Local Market value | Exchange rate | Total amount
                            // -------------------------------------
                            // 03-02-2021 21:18 UPWORK INC. CMN US91688F1049 NDQ SOHO -1 47,00 USD 47,00 USD 39,00 EUR 1,2038 39,00 EUR
                            // @formatter:on
                            section -> section
                                .id("withExchangeRate-withoutFee-withStockExchangePlace")
                                .attributes("date", "time", "name", "isin", "shares", "currency", "amountFx", "exchangeRate", "currencyAccount", "amount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} [\\w]{4} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2,6} [\\w]{3}.* "
                                                + "(\\-)?(?<amountFx>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) (?<currency>[\\w]{3}).* "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} [\\w]{3} "
                                                + "(?<exchangeRate>[\\.,'\\d\\s]+[\\.|,][\\d]{1,4}) "
                                                + "([\\s]+)?(\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) (?<currencyAccount>[\\w]{3})([\\s]+)?$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currencyAccount")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }

                                    long amountFx = asAmount(v.get("amountFx"));
                                    String currencyFx = asCurrencyCode(v.get("currency"));

                                    if (currencyFx.equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                                    {
                                        Money amount = Money.of(asCurrencyCode(v.get("currencyAccount")), asAmount(v.get("amount")));
                                        BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
                                        Money forex = Money.of(asCurrencyCode(v.get("currency")), amountFx);
                                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                                        t.getPortfolioTransaction().addUnit(grossValue);
                                    }
                            }),

                            // @formatter:off
                            // with exchange rate
                            // with fee
                            // without stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Shares | Quote | Amount in exchange rate | Market value | Local market value | Exchange rate | Fee | Total amount
                            // -------------------------------------
                            // 22-10-2020 16:35 THE KRAFT HEINZ COMPAN  US5007541064 NDQ -40 31,21 USD 1.248,40 USD 1.054,33 EUR 1,1829 -0,64 EUR 1.053,69 EUR
                            // 22-12-2020 04:41 TCL ELECTRONICS KYG8701T1388 HKS -8000 5,78 HKD 46 240,00 HKD 4 868,15 EUR 9,489 -2,93 EUR 4 865,22 EUR
                            // 26-08-2020 17:55 PLUG POWER INC. - COM US72919P2020 NDQ 50 13.50 USD -675.00 USD -613.18 CHF 1.0997 -0.72 CHF -613.90 CHF
                            // 14-08-2020 21:13 TESLA MOTORS INC. - C US88160R1014 NDQ 2 1'630.00 USD -3'260.00 USD -2'964.76 CHF 1.0996 -0.55 CHF -2'965.31 CHF
                            // @formatter:on
                            section -> section
                                .id("withExchangeRate-withFee-withoutStockExchangePlace - <currency> <amount>")
                                .attributes("date", "time", "name", "isin", "shares", "currency", "amountFx", "exchangeRate", "currencyFee", "fee", "currencyAccount", "amount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2,6} [\\w]{3} "
                                                + "(\\-)?(?<amountFx>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) (?<currency>[\\w]{3}).* "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} [\\w]{3} "
                                                + "(?<exchangeRate>[\\.,'\\d\\s]+[\\.|,][\\d]{1,4})"
                                                + "( )?(\\-)?(?<fee>[\\.,'\\d\\s]*[\\.|,]?[\\d]{0,2}) (?<currencyFee>[\\w]{3}) "
                                                + "([\\s]+)?(\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) (?<currencyAccount>[\\w]{3})([\\s]+)?$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currencyAccount")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }

                                    // At this stage, fee cannot be null
                                    String fee = v.get("fee").isEmpty()?"0":v.get("fee");
                                    Money feeAmount = Money.of(asCurrencyCode(v.get("currencyFee")), asAmount(fee));
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feeAmount));

                                    long amountFx = asAmount(v.get("amountFx"));
                                    String currencyFx = asCurrencyCode(v.get("currency"));

                                    if (currencyFx.equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                                    {
                                        Money amount = Money.of(asCurrencyCode(v.get("currencyAccount")), asAmount(v.get("amount")));
                                        if (t.getPortfolioTransaction().getType() == PortfolioTransaction.Type.BUY)
                                        {
                                            amount = amount.subtract(feeAmount);
                                        }
                                        else
                                        {
                                            amount = amount.add(feeAmount);
                                        }
                                        BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
                                        Money forex = Money.of(asCurrencyCode(v.get("currency")), amountFx);
                                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                                        t.getPortfolioTransaction().addUnit(grossValue);
                                    }
                            }),

                            // @formatter:off
                            // with exchange rate
                            // without fee
                            // without stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Shares | Quote | Amount in exchange rate | Local Market value | Exchange rate | Total amount
                            // -------------------------------------
                            // 22-07-2019 19:16 LPL FINANCIAL HOLDINGS US50212V1008 NDQ 1 USD 85,73 USD 85,73 EUR 76,42 1,1218 EUR 76,42
                            // @formatter:on
                            section -> section
                                .id("withExchangeRate-withoutFee-withoutStockExchangePlace - <currency> <amount>")
                                .attributes("date", "time", "name", "isin", "shares", "currency", "amountFx", "exchangeRate", "currencyAccount", "amount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "[\\w]{3} (\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2,6} "
                                                + "(?<currency>[\\w]{3}) (\\-)?(?<amountFx>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) "
                                                + "[\\w]{3} (\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} "
                                                + "(?<exchangeRate>[\\.,'\\d\\s]+[\\.|,][\\d]{1,4}) "
                                                + "([\\s]+)?(?<currencyAccount>[\\w]{3}) (\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|,][\\d]{2})([\\s]+)?$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currencyAccount")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }

                                    long amountFx = asAmount(v.get("amountFx"));
                                    String currencyFx = asCurrencyCode(v.get("currency"));

                                    if (currencyFx.equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                                    {
                                        Money amount = Money.of(asCurrencyCode(v.get("currencyAccount")), asAmount(v.get("amount")));
                                        BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
                                        Money forex = Money.of(asCurrencyCode(v.get("currency")), amountFx);
                                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                                        t.getPortfolioTransaction().addUnit(grossValue);
                                    }
                            }),

                            // @formatter:off
                            // with exchange rate
                            // without fee
                            // without stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Shares | Quote | Amount in exchange rate | Local Market value | Exchange rate | Total amount
                            // -------------------------------------
                            // 14-01-2021 15:36 ALTO INGREDIENTS INC US0215131063 NDQ 2048 7,30 USD -14 950,40 USD -12 245,87 EUR 1,2209 -12 245,87 EUR
                            // 14-12-2020 00:00 ASTON MARTIN GB00BN7CG237 LSE 51 1 417,00 GBX -72 267,00 GBX -791,47 EUR 91,3075 -791,47 EUR
                            // @formatter:on
                            section -> section
                                .id("withExchangeRate-withoutFee-withoutStockExchangePlace - <amount> <currency>")
                                .attributes("date", "time", "name", "isin", "shares", "currency", "amountFx", "exchangeRate", "currencyAccount", "amount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2,6} [\\w]{3} "
                                                + "(\\-)?(?<amountFx>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) (?<currency>[\\w]{3}) "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} [\\w]{3} "
                                                + "(?<exchangeRate>[\\.,'\\d\\s]+[\\.|,][\\d]{1,4}) "
                                                + "([\\s]+)?(\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) (?<currencyAccount>[\\w]{3})([\\s]+)?$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currencyAccount")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }

                                    long amountFx = asAmount(v.get("amountFx"));
                                    String currencyFx = asCurrencyCode(v.get("currency"));

                                    if (currencyFx.equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                                    {
                                        Money amount = Money.of(asCurrencyCode(v.get("currencyAccount")), asAmount(v.get("amount")));
                                        BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
                                        Money forex = Money.of(asCurrencyCode(v.get("currency")), amountFx);
                                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                                        t.getPortfolioTransaction().addUnit(grossValue);
                                    }
                            }),

                            // @formatter:off
                            // with exchange rate
                            // with fee
                            // with stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Place | Shares | Quote | Amount in exchange rate | Local Market value | Exchange rate | Fee | Total amount
                            // -------------------------------------
                            // 27-01-2021 20:55 APPLE INC. - COMMON ST US0378331005 NDQ XNAS 9 141,70 USD -1.275,30 USD -1.053,79 EUR 1,209 -0,53 EUR -1.054,32 EUR
                            // 27-01-2021 20:54 NIKOLA CORP US6541101050 NDQ XNAS -48 28,00 USD 1.344,00 USD 1.108,34 EUR 1,2114 -0,66 EUR 1.107,68 EUR
                            // 23-02-2021 15:30 ALTO INGREDIENTS INC US0215131063 NDQ SOHO -224 6,30 USD 1 411,20 USD 1 159,26 EUR 1,2161 -0,74 EUR 1 158,52 EUR
                            // 23-02-2021 09:42 ASTON MARTIN GB00BN7CG237 LSE MESI -128 2 105,00 GBX 269 440,00 GBX 3 113,71 EUR 86,4469 -5,56 EUR 3 108,15 EUR
                            // 26-04-2021 15:35 GOODFOOD MARKET CORP CA38217M1005 TOR NEOE 100 8,20 CAD -820,00 CAD -546,67 EUR 1,5 -2,67 EUR -549,34 EUR
                            // @formatter:on
                            section -> section
                                .id("withExchangeRate-withFee-withStockExchangePlace - <amount> <currency>")
                                .attributes("date", "time", "name", "isin", "shares", "currency", "amountFx", "exchangeRate", "currencyFee", "fee", "currencyAccount", "amount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} [\\w]{4} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,]?[\\d]{0,6} [\\w]{3} "
                                                + "(\\-)?(?<amountFx>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) (?<currency>[\\w]{3}) "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} [\\w]{3} "
                                                + "(?<exchangeRate>[\\.,'\\d\\s]+[\\.|,][\\d]{1,4}) "
                                                + "(\\-)?(?<fee>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) (?<currencyFee>[\\w]{3}) "
                                                + "([\\s]+)?(\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) (?<currencyAccount>[\\w]{3})([\\s]+)?$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currencyAccount")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }

                                    Money feeAmount = Money.of(asCurrencyCode(v.get("currencyFee")), asAmount(v.get("fee")));
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feeAmount));

                                    long amountFx = asAmount(v.get("amountFx"));
                                    String currencyFx = asCurrencyCode(v.get("currency"));

                                    if (currencyFx.equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                                    {
                                        Money amount = Money.of(asCurrencyCode(v.get("currencyAccount")), asAmount(v.get("amount")));
                                        if (t.getPortfolioTransaction().getType() == PortfolioTransaction.Type.BUY)
                                        {
                                            amount = amount.subtract(feeAmount);
                                        }
                                        else
                                        {
                                            amount = amount.add(feeAmount);
                                        }
                                        BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
                                        Money forex = Money.of(asCurrencyCode(v.get("currency")), amountFx);
                                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                                        t.getPortfolioTransaction().addUnit(grossValue);
                                    }
                            }),

                            // @formatter:off
                            // without exchange rate
                            // without fee
                            // without stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Shares | Quote | Market value | Local market value | Total amount
                            // -------------------------------------
                            // 08-02-2019 13:27 ODX2 C11000.00 08FEB19 DE000C25KFE8 ERX -3 EUR 0,00 EUR 0,00 EUR 0,00 EUR 0,00
                            // 09-07-2019 14:08 VANGUARD FTSE AW IE00B3RBWM25 EAM 3 EUR 77,10 EUR -231,30 EUR -231,30 EUR -231,30
                            // @formatter:on
                            section -> section
                                .id("withoutExchangeRate-withoutFee-withoutStockExchangePlace - <amount> <currency>")
                                .attributes("date", "time", "name", "isin", "shares", "currency", "amount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "[\\w]{3} (\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2,6} "
                                                + "[\\w]{3} (\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} "
                                                + "[\\w]{3} (\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} "
                                                + "([\\s]+)?(?<currency>[\\w]{3}) (\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|,][\\d]{2})([\\s]+)?$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }
                            }),

                            // @formatter:off
                            // without exchange rate
                            // without fee
                            // without stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Shares | Quote | Market value | Local market value | Total amount
                            // -------------------------------------
                            // 13-03-2018 00:00 BAUMOT GROUP AG O.N DE000A2DAM11 XET -350 1,84 EUR 644,00 EUR 644,00 EUR 644,00 EUR
                            // 22-07-2020 00:00 PHARMA MAR SA ES0169501022 MAD 21 114,66 EUR -2 407,86 EUR -2 407,86 EUR -2 407,86 EUR
                            // @formatter:on
                            section -> section
                                .id("withoutExchangeRate-withoutFee-withoutStockExchangePlace - <currency> <amount>")
                                .attributes("date", "time", "name", "isin", "shares", "currency", "amount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2,6} [\\w]{3} "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} [\\w]{3} "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} [\\w]{3} "
                                                + "([\\s]+)?(\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|\\,][\\d]{2}) (?<currency>[\\w]{3})([\\s]+)?$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }
                            }),

                            // @formatter:off
                            // without exchange rate
                            // with fee
                            // without stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Shares | Quote | Market value | Local market value | Fee | Total amount
                            // -------------------------------------
                            // 07-02-2019 12:22 ODX2 C11200.00 08FEB19 DE000C25KFN9 ERX 1 EUR 30,00 EUR -150,00 EUR -150,00 EUR -0,90 EUR -150,90
                            // 01-04-2019 15:35 DEUTSCHE BANK AG NA O.N DE0005140008 XET -136 EUR 7,45 EUR 1.013,20 EUR 1.013,20 EUR -2,26 EUR 1.010,94
                            // 01-04-2019 12:20 DEUTSCHE BANK AG NA O.N DE0005140008 XET 18 EUR 7,353 EUR -132,35 EUR -132,35 EUR -0,03 EUR -132,38
                            // @formatter:on
                            section -> section
                                .id("withoutExchangeRate-withFee-withoutStockExchangePlace - <amount> <currency>")
                                .attributes("date", "time", "name", "isin", "shares", "currencyFee", "fee", "currency", "amount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "[\\w]{3} (\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2,6} "
                                                + "[\\w]{3} (\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} "
                                                + "[\\w]{3} (\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} "
                                                + "(?<currencyFee>[\\w]{3}) (\\-)?(?<fee>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) "
                                                + "([\\s]+)?(?<currency>[\\w]{3}) (\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|,][\\d]{2})([\\s]+)?$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    Money feeAmount = Money.of(asCurrencyCode(v.get("currencyFee")), asAmount(v.get("fee")));
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feeAmount));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }
                            }),

                            // @formatter:off
                            // without exchange rate
                            // with fee
                            // without stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Shares | Quote | Market value | Local Market value | Fee | Total amount
                            // -------------------------------------
                            // 01-08-2017 17:21 DEUTSCHE TELEKOM AG DE0005557508 XET 50 15,61 EUR -780,50 EUR -780,50 EUR -2,06 EUR -782,56 EUR
                            // 01-08-2017 16:32 CECONOMY AG DE0007257503 XET 72 9,45 EUR -680,40 EUR -680,40 EUR -2,05 EUR -682,45 EUR
                            // @formatter:on
                            section -> section
                                .id("withoutExchangeRate-withFee-withoutStockExchangePlace - <currency> <amount>")
                                .attributes("date", "time", "name", "isin", "shares", "currencyFee", "fee", "currency", "amount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2,6} [\\w]{3} "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} [\\w]{3} "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} [\\w]{3} "
                                                + "(\\-)?(?<fee>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) (?<currencyFee>[\\w]{3}) "
                                                + "([\\s]+)?(\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) (?<currency>[\\w]{3})([\\s]+)?$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }

                                    Money feeAmount = Money.of(asCurrencyCode(v.get("currencyFee")), asAmount(v.get("fee")));
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feeAmount));
                            }),

                            // @formatter:off
                            // without exchange rate
                            // with fee
                            // with stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Place | Shares | Quote | Market value | Local Market value | Fee | Total amount
                            // -------------------------------------
                            // 07-01-2021 09:00 K&S AG DE000KSAG888 XET XETA 20 9,748 EUR -194,96 EUR -194,96 EUR -2,04 EUR -197,00 EUR
                            // 21-12-2020 10:33 VARTA AG DE000A0TGJ55 XET XETA -11 111,60 EUR 1.227,60 EUR 1.227,60 EUR -2,22 EUR 1.225,38 EUR
                            // 23-02-2021 15:56 INVESCO EQQQ NASDAQ-100 IE0032077012 XET XETA 8 260,00 EUR -2 080,00 EUR -2 080,00 EUR -2,62 EUR -2 082,62 EUR
                            // @formatter:on
                            section -> section
                                .id("withoutExchangeRate-withFee-withStockExchangePlace")
                                .attributes("date", "time", "name", "isin", "shares", "currencyFee", "fee", "currency", "amount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) "
                                                + "(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} [\\w]{4} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,]?[\\d]{0,6} [\\w]{3} "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} [\\w]{3} "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} [\\w]{3}"
                                                + "( )?(\\-)?(?<fee>[\\.,'\\d\\s]*[\\.|,]?[\\d]{0,2}) (?<currencyFee>[\\w]{3}) "
                                                + "([\\s]+)?(\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) (?<currency>[\\w]{3})([\\s]+)?$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }
                                    // At this stage, fee cannot be null
                                    String fee = v.get("fee").isEmpty()?"0":v.get("fee");
                                    Money feeAmount = Money.of(asCurrencyCode(v.get("currencyFee")), asAmount(fee));
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feeAmount));
                            }),

                            // @formatter:off
                            // without exchange rate
                            // without fee
                            // with stock exchange place
                            // -------------------------------------
                            // Formatting:
                            // DateTime | Name | ISIN | Stock Exchange | Place | Shares | Quote | Market value | Local Market value | Total amount
                            // -------------------------------------
                            // 28-02-2020 09:00 TOMTOM NL0013332471 EAM XAMS 1 8,933 EUR -8,93 EUR -8,93 EUR -8,93 EUR
                            // @formatter:on
                            section -> section
                                .id("withoutExchangeRate-withoutFee-withStockExchangePlace")
                                .attributes("date", "time", "name", "isin", "shares", "currency", "amount")
                                .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}) "
                                                + "(?<name>.*) (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) "
                                                + "[\\w]{3} [\\w]{4} ([\\s]+)?"
                                                + "(?<shares>[\\-\\.,'\\d]+) "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2,6} [\\w]{3} "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} [\\w]{3} "
                                                + "(\\-)?[\\.,'\\d\\s]+[\\.|,][\\d]{2} [\\w]{3} "
                                                + "([\\s]+)?(\\-)?(?<amount>[\\.,'\\d\\s]+[\\.|,][\\d]{2}) (?<currency>[\\w]{3})([\\s]+)?$")
                                .assign((t, v) -> {
                                                            setTransactionReportSecurity(type, t, v);
                                    t.setDate(asDate(v.get("date"), v.get("time")));
                                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                    t.setAmount(asAmount(v.get("amount")));

                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(
                                                        v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }
                                })

                        )

                        .wrap(t -> {
                            // Account statements that carry the account currency in the
                            // document context (the layout with the AutoFX column) also list
                            // corporate actions such as rights or non-tradeable positions with
                            // a total amount of zero. These are no transactions and are
                            // skipped. Older layouts keep their previous behavior.
                            if (type.getCurrentContext().containsKey("currencyAccount")
                                            && t.getPortfolioTransaction().getCurrencyCode() != null
                                            && t.getPortfolioTransaction().getAmount() == 0)
                                return new SkippedItem(new BuySellEntryItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            return new BuySellEntryItem(t);
                        }));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Number of shares Amount of dividend Gross amount of Amount of tax Net amount oper share dividend withheld dividend Wäh.
                // 20 0,142 2,84 -0,43 2,41 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .find("Number of shares .*")
                .match("^[\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ -(?<tax>[\\.,\\d]+) [\\.,\\d]+ (?<currency>[\\w]{3})$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private static class ExchangeRateHelper
    {
        private List<CurrencyExchangeItem> items = new ArrayList<>();

        public Optional<CurrencyExchangeItem> findItem(int lineNumber, Money money, LocalDate date)
        {
            // search backwards for the first items _before_ the given line
            // number with a currency exchange for the given currency

            for (int ii = items.size() - 1; ii >= 0; ii--) // NOSONAR
            {
                CurrencyExchangeItem item = items.get(ii);
                if (item.lineNo > lineNumber)
                    continue;

                if (!item.termCurrency.equals(money.getCurrencyCode()))
                    continue;

                if (item.valuta != null && (item.date.equals(date) || item.valuta.equals(date)))
                    return Optional.of(item);

                if (item.valuta == null && (date.equals(item.date) || money.getAmount() == item.termAmount))
                    return Optional.of(item);
            }

            return Optional.empty();
        }
    }

    private static class DividendTransactionHelper
    {
        private List<DividendeTransactionsItem> items = new ArrayList<>();

        public Optional<DividendeTransactionsItem> findItem(LocalDateTime dateTime, String isin)
        {
            // Search date and time of dividend transaction using date+time and
            // ISIN. Cancelled dividends are not taken into account.

            for (DividendeTransactionsItem item : items)
            {
                if (!item.cancellation && item.dateTime.equals(dateTime) && item.isin.equals(isin))
                    return Optional.of(item);
            }

            return Optional.empty();
        }

        public Optional<DividendeTransactionsItem> findCancellation(LocalDateTime dateTime, String isin)
        {
            // Search date and time of a cancelled dividend transaction using
            // date+time and ISIN.

            for (DividendeTransactionsItem item : items)
            {
                if (item.cancellation && item.dateTime.equals(dateTime) && item.isin.equals(isin))
                    return Optional.of(item);
            }

            return Optional.empty();
        }
    }

    /**
     * Represents two lines in the account statement for a currency exchange
     * ("Währungswechsel" or "FX Debit").
     *
     * <pre>
     *  amount in base currency x exchange rate = amount in term currency
     * </pre>
     */
    private boolean isTransactionReportNameContinuation(String[] lines, int index)
    {
        String line = trim(lines[index]);

        if (line == null || line.isBlank())
            return false;

        if (TRANSACTION_REPORT_START.matcher(line).matches())
            return false;

        if (isTransactionReportHeaderOrFooter(line))
            return false;

        if (isStandaloneTransactionReportHeaderCurrency(lines, index))
            return false;

        return TRANSACTION_REPORT_NAME_CONTINUATION.matcher(line).matches();
    }

    private boolean isStandaloneTransactionReportHeaderCurrency(String[] lines, int index)
    {
        String line = trim(lines[index]);

        if (line == null || !line.matches("[A-Z]{3}"))
            return false;

        String previous = previousNonBlankLine(lines, index);

        // @formatter:off
        // The table header ends with a standalone currency code below the
        // column titles. It must not be added to the name of a security.
        //
        // rate Fee party fees Total EUR
        // EUR
        // @formatter:on
        return previous != null && (previous.contains("AutoFX") //
                        || previous.contains("party fees") //
                        || previous.contains("Total") //
                        || previous.contains("parties") //
                        || previous.contains("Gesamt"));
    }

    private String previousNonBlankLine(String[] lines, int index)
    {
        for (int i = index - 1; i >= 0; i--)
        {
            String line = trim(lines[i]);
            if (line != null && !line.isBlank())
                return line;
        }

        return null;
    }

    private boolean isTransactionReportHeaderOrFooter(String line)
    {
        return line.equals("Transaction") //
                        || line.startsWith("Date Time Product ISIN") //
                        || line.startsWith("exchange Venue Quantity") //
                        || line.startsWith("rate Fee") //
                        || line.startsWith("Date Heure Produit Code ISIN") //
                        || line.startsWith("Datum Uhrzeit Produkt ISIN") //
                        || isCommonHeaderOrFooter(line);
    }

    private boolean isCommonHeaderOrFooter(String line)
    {
        return line.startsWith("User name:") //
                        || line.startsWith("Benutzername:") //
                        || line.startsWith("Page ") //
                        || line.startsWith("Seite ") //
                        || line.startsWith("flatexDEGIRO Bank") //
                        || line.startsWith("SE is primarily supervised") //
                        || line.startsWith("with DNB") //
                        || line.startsWith("DEGIRO B.V.") //
                        || isSalutationLine(line);
    }

    private boolean isSalutationLine(String line)
    {
        return line.startsWith("Mr ") //
                        || line.startsWith("Ms ") //
                        || line.startsWith("Mrs ") //
                        || line.startsWith("Herr ") //
                        || line.startsWith("Frau ");
    }

    private void setTransactionReportSecurity(DocumentType type, BuySellEntry transaction, ParsedData values)
    {
        type.getCurrentContext().getType(TransactionReportNameContinuationHelper.class)
                        .flatMap(helper -> helper.find(values.getStartLineNumber()))
                        .ifPresent(nameContinued -> values.put("nameContinued", nameContinued));

        transaction.setSecurity(getOrCreateSecurity(values));
    }

    private static class TransactionReportNameContinuationHelper
    {
        private Map<Integer, String> values = new HashMap<>();

        public void put(int lineNumber, String nameContinued)
        {
            values.put(lineNumber, nameContinued);
        }

        public Optional<String> find(int lineNumber)
        {
            return Optional.ofNullable(values.get(lineNumber));
        }
    }

    private static class CurrencyExchangeItem
    {
        int lineNo;
        LocalDate date;
        LocalDate valuta;

        String baseCurrency;
        long baseAmount;
        String termCurrency;
        long termAmount;
        BigDecimal rate;

        @Override
        public String toString()
        {
            return "CurrencyExchangeItem [lineNo=" + lineNo + ", date=" + date + ", valuta=" + valuta
                            + ", baseCurrency=" + baseCurrency + ", baseAmount=" + baseAmount + ", termCurrency="
                            + termCurrency + ", termAmount=" + termAmount + ", rate=" + rate + "]";
        }
    }

    private static class DividendTaxHelper
    {
        private List<DividendeTaxItem> items = new ArrayList<>();

        public Optional<DividendeTaxItem> findItemAbove(LocalDate date, String isin, int lineNo)
        {
            // @formatter:off
            // Search a dividend tax that is printed directly above its dividend and is
            // therefore not covered by the block of the dividend. Only the line directly
            // above is taken into account, otherwise a later correction of the tax would
            // be added to an earlier dividend. The booking time of dividend and tax may
            // differ, therefore only the date is compared.
            // @formatter:on

            for (DividendeTaxItem item : items)
            {
                if (item.lineNo == lineNo - 1 && item.date.equals(date) && item.isin.equals(isin))
                    return Optional.of(item);
            }

            return Optional.empty();
        }
    }

    private static class DividendeTaxItem
    {
        int lineNo;
        LocalDate date;
        String isin;
        String currency;
        String amount;

        @Override
        public String toString()
        {
            return "DividendeTaxItem [lineNo=" + lineNo + ", date=" + date + ", isin=" + isin + ", currency=" + currency
                            + ", amount=" + amount + "]";
        }
    }

    private static class DividendeTransactionsItem
    {
        LocalDateTime dateTime;
        String isin;
        boolean cancellation;

        @Override
        public String toString()
        {
            return "DividendeTransactionsItem [dateTime=" + dateTime + ", isin=" + isin + ", cancellation="
                            + cancellation + "]";
        }
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount,
                        ExtractorUtils.guessNumberLocale(value, Locale.GERMANY));
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share,
                        ExtractorUtils.guessNumberLocale(value, Locale.GERMANY));
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share,
                        ExtractorUtils.guessNumberLocale(value, Locale.GERMANY));
    }
}