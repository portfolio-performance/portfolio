package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetTax;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.DocumentContext;
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
import name.abuchen.portfolio.util.AdditionalLocales;

/**
 * @formatter:off
 * @implNote GBM (Grupo Bursátil Mexicano, GBM+) is a Mexican broker.
 *           The currency is MXN (Mexican Peso).
 *
 * @implSpec The monthly account statement (Estado de Cuenta) contains the transactions
 *           in the "DESGLOSE DE MOVIMIENTOS" section.
 *           All security currencies are in MXN.
 *           No ISIN or WKN is provided. The security name is the ticker symbol (EMISORA)
 *           followed by the series. For securities of the international quotation system (SIC)
 *           the series is "*" and is not part of the security name.
 *           The ticker symbol is the EMISORA concatenated with the series (e.g. "GBMF2BF"),
 *           following the BMV listing convention, so that existing securities with an
 *           exchange suffix (e.g. "GBMF2BF.MX") are matched during import.
 *
 *           The transaction rows only contain the day of the operation and the day of the
 *           settlement (FECHA OPER/LIQ). The settlement day is used as the transaction date.
 *           Month and year are taken from the statement period
 *           (e.g. "DEL 1 AL 31 DE MAYO DE 2021").
 *           The statement is ordered by the operation day. If the settlement day is smaller
 *           than the operation day, the settlement falls into the following month.
 *
 *           The amount of every row is taken from the NETO column, which is the cash
 *           movement of the row. For buy, sell and dividend rows the commission (COMISIÓN)
 *           and tax (IMPUESTO) columns are added as fee and tax units.
 *
 *           Repurchase agreement rows (Compra en Reporto, Vencimiento de Reporto) are used by GBM
 *           to invest idle cash overnight. They are imported as buy and sell transactions.
 *           All reporto rows of the same issuer (e.g. "BI" = CETES, "LD" = BONDES D) are mapped
 *           to a single security using the EMISORA as name. No ticker symbol is set, because the
 *           EMISORA is not a tradable symbol and could match an unrelated existing security.
 *           The series (maturity date, e.g. "211104") changes with every roll-over and is
 *           therefore not part of the security, but kept in the note.
 *
 *           Cash rows (DEPOSITO DE EFECTIVO, INTERESES) use "Efec *" as EMISORA and are
 *           imported as deposit and interest transactions without a security.
 *
 *           CBFI share distributions (Distribución de CBFIs por Resultado Fiscal) are imported
 *           as delivery inbound transactions without a value. The description may be garbled as
 *           "DDeisptorsibituoi ddoe CBFIs por Resultado Fiscal" due to overlapping columns.
 *
 *           Tax withholding rows (RETENCION ISR POR RESULTADO FISCAL, ISR 10 % POR DIVIDENDOS SIC)
 *           are collected upfront and merged into the matching dividend transaction.
 *
 *           Since 2022 the statement uses a slightly different layout: transaction rows start
 *           with a leading space, the statement period is printed behind the RFC on the same
 *           line, and the text extraction can interleave overlapping description columns
 *           (e.g. "Abono efectivo" and "Distribuido" become "ADbisotnriob eufiedcotivo").
 *           The SALDO column can be negative.
 *
 * @formatter:on
 */
@SuppressWarnings("nls")
public class GBMPDFExtractor extends AbstractPDFExtractor
{
    private static final String MXN = "MXN";

    public GBMPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("GRUPO BURSATIL MEXICANO S.A. DE C.V., CASA DE BOLSA");
        addBankIdentifier("gbm.com.mx");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "GBM (Grupo Bursátil Mexicano)";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("DESGLOSE DE MOVIMIENTOS", this::parseDocumentContext);
        this.addDocumentTyp(type);

        addBuySellBlock(type);
        addReportoBlock(type);
        addDividendBlock(type);
        addDepositAndInterestBlock(type);
        addDeliveryBlock(type);
    }

    private void addBuySellBlock(DocumentType type)
    {
        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^[\\s]*[\\d]{2}\\/[\\d]{2} [\\d]+ Compra Soc\\. de Inv\\. \\- Cliente .*$");
        firstRelevantLine.setMaxSize(1);
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // @formatter:off
                        // 04/04 52898941 Compra Soc. de Inv. - Cliente GBMF2 BF 2 37.159019 0.00 0.00 0.00 74.32 26.72
                        // @formatter:on
                        .section("oper", "date", "note", "name", "serie", "shares", "fee", "tax", "amount") //
                        .documentContext("month", "year") //
                        .match("^[\\s]*(?<oper>[\\d]{2})\\/(?<date>[\\d]{2}) (?<note>[\\d]+) " //
                                        + "Compra Soc\\. de Inv\\. \\- Cliente " //
                                        + "(?<name>[A-Z0-9]+) (?<serie>[A-Z0-9\\*]+) " //
                                        + "(?<shares>[\\.,\\d]+) [\\.,\\d]+ (?<fee>[\\.,\\d]+) [\\.,\\d]+ (?<tax>[\\.,\\d]+) " //
                                        + "(?<amount>[\\.,\\d]+) (\\-)?[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            v.put("currency", asCurrencyCode(MXN));
                            v.put("tickerSymbol", getSecurityTickerSymbol(v.get("name"), v.get("serie")));
                            v.put("name", getSecurityName(v.get("name"), v.get("serie")));
                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDate(asTransactionDate(v.get("oper"), v.get("date"), v.get("month"), v.get("year")));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(asCurrencyCode(MXN));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote("Folio: " + v.get("note"));

                            var fee = Money.of(asCurrencyCode(MXN), asAmount(v.get("fee")));
                            if (fee.isPositive())
                                checkAndSetFee(fee, t.getPortfolioTransaction(), type.getCurrentContext());

                            var tax = Money.of(asCurrencyCode(MXN), asAmount(v.get("tax")));
                            if (tax.isPositive())
                                checkAndSetTax(tax, t.getPortfolioTransaction(), type.getCurrentContext());
                        })

                        .wrap(BuySellEntryItem::new);
    }

    private void addReportoBlock(DocumentType type)
    {
        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^[\\s]*[\\d]{2}\\/[\\d]{2} [\\d]+ (Compra en Reporto|Vencimiento de Reporto) .*$");
        firstRelevantLine.setMaxSize(1);
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // @formatter:off
                        // 04/04 90565309 Compra en Reporto LD 260806 1 99.412030 1.00 1 0.00 0.00 0.00 99.41 61.25
                        // 05/05 90565310 Vencimiento de Reporto LD 260806 1 99.414800 1.00 1 0.00 0.00 0.00 99.41 160.66
                        // 11/11 93153110 Vencimiento de Reporto BI 211104 16 9.964150 1.00 3 0.00 0.01 0.01 159.41 160.66
                        //  29/29 241220673 Compra en Reporto BI 220630 107 9.872545 0.10 3 0.00 0.00 0.00 1,056.36 3.62
                        // @formatter:on
                        .section("oper", "date", "note", "type", "name", "serie", "shares", "fee", "tax", "amount") //
                        .documentContext("month", "year") //
                        .match("^[\\s]*(?<oper>[\\d]{2})\\/(?<date>[\\d]{2}) (?<note>[\\d]+) " //
                                        + "(?<type>Compra en Reporto|Vencimiento de Reporto) " //
                                        + "(?<name>[A-Z0-9]+) (?<serie>[A-Z0-9\\*]+) " //
                                        + "(?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+ [\\d]+ (?<fee>[\\.,\\d]+) [\\.,\\d]+ " //
                                        + "(?<tax>[\\.,\\d]+) (?<amount>[\\.,\\d]+) (\\-)?[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "Vencimiento de Reporto" change from BUY to SELL
                            // @formatter:on
                            if ("Vencimiento de Reporto".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);

                            // @formatter:off
                            // The series (maturity date) changes with every roll-over.
                            // Map all reporto rows of the same issuer to a single security
                            // and keep the series in the note.
                            // @formatter:on
                            v.put("currency", asCurrencyCode(MXN));
                            v.put("name", trim(v.get("name")));
                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDate(asTransactionDate(v.get("oper"), v.get("date"), v.get("month"), v.get("year")));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(asCurrencyCode(MXN));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote("Folio: " + v.get("note") + " | " + v.get("name") + " " + trim(v.get("serie")));

                            var fee = Money.of(asCurrencyCode(MXN), asAmount(v.get("fee")));
                            if (fee.isPositive())
                                checkAndSetFee(fee, t.getPortfolioTransaction(), type.getCurrentContext());

                            var tax = Money.of(asCurrencyCode(MXN), asAmount(v.get("tax")));
                            if (tax.isPositive())
                                checkAndSetTax(tax, t.getPortfolioTransaction(), type.getCurrentContext());
                        })

                        .wrap(BuySellEntryItem::new);
    }

    private void addDividendBlock(DocumentType type)
    {
        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^[\\s]*[\\d]{2}\\/[\\d]{2} [\\d]+ " //
                        + "(Abono efectivo Resultado Fiscal Distribuido" //
                        + "|ADbisotnriob eufiedcotivo Resultado Fiscal" //
                        + "|Abono Reembolso de Capital, Cust\\. Normal" //
                        + "|ABONO DIVIDENDO EMISORA EXTRANJERA" //
                        + "|AEXBTORNAON DJEIVRIADENDO EMISORA) .*$");
        firstRelevantLine.setMaxSize(1);
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        // @formatter:off
                        // 06/10 1671315 Abono efectivo Resultado Fiscal Distribuido FUNO 11 0 0.000000 0.00 0.00 0.00 47.61 74.33
                        // 06/10 1689408 Abono Reembolso de Capital, Cust. Normal FUNO 11 0 0.000000 0.00 0.00 0.00 24.63 98.96
                        // 23/24 30895065 ABONO DIVIDENDO EMISORA EXTRANJERA VWO * 0 0.000000 0.00 0.00 0.00 19.77 24.52
                        //  27/29 3978837 ADbisotnriob eufiedcotivo Resultado Fiscal FIBRAPL 14 0 0.000000 0.00 0.00 0.00 128.93 131.44
                        //  24/27 93960162 AEXBTORNAON DJEIVRIADENDO EMISORA VWO * 0 0.000000 0.00 0.00 0.00 21.22 22.88
                        // @formatter:on
                        .section("oper", "date", "note", "type", "name", "serie", "fee", "tax", "amount") //
                        .documentContext("month", "year") //
                        .match("^[\\s]*(?<oper>[\\d]{2})\\/(?<date>[\\d]{2}) (?<note>[\\d]+) " //
                                        + "(?<type>Abono efectivo Resultado Fiscal Distribuido" //
                                        + "|ADbisotnriob eufiedcotivo Resultado Fiscal" //
                                        + "|Abono Reembolso de Capital, Cust\\. Normal" //
                                        + "|ABONO DIVIDENDO EMISORA EXTRANJERA" //
                                        + "|AEXBTORNAON DJEIVRIADENDO EMISORA) " //
                                        + "(?<name>[A-Z0-9]+) (?<serie>[A-Z0-9\\*]+) " //
                                        + "[\\.,\\d]+ [\\.,\\d]+ (?<fee>[\\.,\\d]+) [\\.,\\d]+ (?<tax>[\\.,\\d]+) " //
                                        + "(?<amount>[\\.,\\d]+) (\\-)?[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            var context = type.getCurrentContext();
                            var security = v.get("name") + " " + v.get("serie");

                            v.put("currency", asCurrencyCode(MXN));
                            v.put("tickerSymbol", getSecurityTickerSymbol(v.get("name"), v.get("serie")));
                            v.put("name", getSecurityName(v.get("name"), v.get("serie")));
                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDateTime(asTransactionDate(v.get("oper"), v.get("date"), v.get("month"), v.get("year")));
                            t.setCurrencyCode(asCurrencyCode(MXN));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote("Folio: " + v.get("note"));

                            var fee = Money.of(asCurrencyCode(MXN), asAmount(v.get("fee")));
                            if (fee.isPositive())
                                checkAndSetFee(fee, t, context);

                            var tax = Money.of(asCurrencyCode(MXN), asAmount(v.get("tax")));
                            if (tax.isPositive())
                                checkAndSetTax(tax, t, context);

                            // @formatter:off
                            // The tax withholding is booked as a separate row in the statement.
                            // Merge it into the matching dividend transaction.
                            // @formatter:on
                            var taxType = getTaxType(v.get("type"));

                            if (taxType != null)
                            {
                                var taxAmountTransactionHelper = context.getType(TaxAmountTransactionHelper.class).orElseGet(TaxAmountTransactionHelper::new);
                                var item = taxAmountTransactionHelper.findItem(v.getStartLineNumber(), t.getDateTime(), security, taxType);

                                if (item.isPresent())
                                {
                                    var withholdingTax = Money.of(asCurrencyCode(MXN), item.get().tax);
                                    t.setMonetaryAmount(t.getMonetaryAmount().subtract(withholdingTax));

                                    checkAndSetTax(withholdingTax, t, context);
                                }
                            }
                        })

                        .wrap(TransactionItem::new);
    }

    private void addDepositAndInterestBlock(DocumentType type)
    {
        var firstRelevantLine = new Block("^[\\s]*[\\d]{2}\\/[\\d]{2} [\\d]+ (DEPOSITO DE EFECTIVO|INTERESES) Efec \\* .*$");
        firstRelevantLine.setMaxSize(1);
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        // @formatter:off
                        //  05/05 209452300 DEPOSITO DE EFECTIVO Efec * 0 0.000000 0.00 0.00 0.00 1,129.94 1,138.91
                        //  29/29 218952286 INTERESES Efec * 0 0.000000 0.00 0.00 0.00 0.05 20.40
                        // @formatter:on
                        .section("oper", "date", "note", "type", "amount") //
                        .documentContext("month", "year") //
                        .match("^[\\s]*(?<oper>[\\d]{2})\\/(?<date>[\\d]{2}) (?<note>[\\d]+) " //
                                        + "(?<type>DEPOSITO DE EFECTIVO|INTERESES) Efec \\* " //
                                        + "[\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ " //
                                        + "(?<amount>[\\.,\\d]+) (\\-)?[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "INTERESES" change from DEPOSIT to INTEREST
                            // @formatter:on
                            if ("INTERESES".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.INTEREST);

                            t.setDateTime(asTransactionDate(v.get("oper"), v.get("date"), v.get("month"), v.get("year")));
                            t.setCurrencyCode(asCurrencyCode(MXN));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote("Folio: " + v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }

    private void addDeliveryBlock(DocumentType type)
    {
        var firstRelevantLine = new Block("^[\\s]*[\\d]{2}\\/[\\d]{2} [\\d]+ DDeisptorsibituoi ddoe CBFIs por Resultado Fiscal .*$");
        firstRelevantLine.setMaxSize(1);
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(new Transaction<PortfolioTransaction>()

                        .subject(() -> new PortfolioTransaction(PortfolioTransaction.Type.DELIVERY_INBOUND))

                        // @formatter:off
                        //  01/01 10799687 DDeisptorsibituoi ddoe CBFIs por Resultado Fiscal FIBRAPL 14 4 0.000000 0.00 0.00 0.00 0.00 99.32
                        // @formatter:on
                        .section("oper", "date", "note", "name", "serie", "shares") //
                        .documentContext("month", "year") //
                        .match("^[\\s]*(?<oper>[\\d]{2})\\/(?<date>[\\d]{2}) (?<note>[\\d]+) " //
                                        + "DDeisptorsibituoi ddoe CBFIs por Resultado Fiscal " //
                                        + "(?<name>[A-Z0-9]+) (?<serie>[A-Z0-9\\*]+) " //
                                        + "(?<shares>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ " //
                                        + "[\\.,\\d]+ (\\-)?[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            v.put("currency", asCurrencyCode(MXN));
                            v.put("tickerSymbol", getSecurityTickerSymbol(v.get("name"), v.get("serie")));
                            v.put("name", getSecurityName(v.get("name"), v.get("serie")));
                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDateTime(asTransactionDate(v.get("oper"), v.get("date"), v.get("month"), v.get("year")));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(asCurrencyCode(MXN));
                            t.setAmount(0L);
                            t.setNote("Folio: " + v.get("note"));
                        })

                        // The amount of a CBFI distribution is always zero, therefore
                        // rows without shares are skipped instead of rows without amount.
                        .wrap(t -> {
                            if (t.getCurrencyCode() != null && t.getShares() == 0)
                                return new SkippedItem(new TransactionItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            return new TransactionItem(t);
                        }));
    }

    private void parseDocumentContext(DocumentContext context, String[] lines)
    {
        // @formatter:off
        // DEL 1 AL 31 DE MAYO DE 2021
        // RFC:XXXXXXXXXXXXXX DEL 1 AL 30 DE ABRIL DE 2022
        // @formatter:on
        var pPeriod = Pattern.compile("^.*DEL [\\d]{1,2} AL [\\d]{1,2} DE (?<month>[A-Z]+) DE (?<year>[\\d]{4}).*$");

        // @formatter:off
        // 06/10 26674354 RETENCION ISR POR RESULTADO FISCAL FUNO 11 0 0.000000 0.00 0.00 0.00 14.28 84.67
        // 23/24 30895067 ISR 10 % POR DIVIDENDOS SIC VWO * 0 0.000000 0.00 0.00 0.00 1.98 22.55
        //  27/29 81561904 RETENCION ISR POR RESULTADO FISCAL FIBRAPL 14 0 0.000000 0.00 0.00 0.00 38.68 92.76
        //  08/10 152765619 RDEISTTE.NCION ISR POR RESULTADO FISCAL TERRA 13 0 0.000000 0.00 0.00 0.00 17.73 184.95
        // @formatter:on
        var pTaxAmountTransaction = Pattern.compile("^[\\s]*(?<oper>[\\d]{2})\\/(?<date>[\\d]{2}) [\\d]+ " //
                        + "(?<type>RETENCION ISR POR RESULTADO FISCAL" //
                        + "|RDEISTTE\\.NCION ISR POR RESULTADO FISCAL" //
                        + "|ISR 10 % POR DIVIDENDOS SIC) " //
                        + "(?<name>[A-Z0-9]+) (?<serie>[A-Z0-9\\*]+) " //
                        + "[\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ " //
                        + "(?<tax>[\\.,\\d]+) (\\-)?[\\.,\\d]+$");

        var taxAmountTransactionHelper = new TaxAmountTransactionHelper();
        context.putType(taxAmountTransactionHelper);

        // The statement period is required to build the date of the
        // withholding rows, therefore it is searched in a first pass.
        for (String line : lines)
        {
            var mPeriod = pPeriod.matcher(line);
            if (mPeriod.matches())
            {
                context.put("month", mPeriod.group("month"));
                context.put("year", mPeriod.group("year"));
                break;
            }
        }

        if (!context.containsKey("month"))
            return;

        for (var i = 0; i < lines.length; i++)
        {
            var m = pTaxAmountTransaction.matcher(lines[i]);
            if (m.matches())
            {
                // @formatter:off
                // "RDEISTTE.NCION ISR POR RESULTADO FISCAL" is "RETENCION ISR POR RESULTADO FISCAL"
                // with the wrapped "DIST." of the description column interleaved by the text extraction.
                // @formatter:on
                var taxType = m.group("type");
                if ("RDEISTTE.NCION ISR POR RESULTADO FISCAL".equals(taxType))
                    taxType = "RETENCION ISR POR RESULTADO FISCAL";

                var item = new TaxAmountTransactionItem();
                item.line = i;
                item.dateTime = asTransactionDate(m.group("oper"), m.group("date"), context.get("month"), context.get("year"));
                item.type = taxType;
                item.security = m.group("name") + " " + m.group("serie");
                item.tax = asAmount(m.group("tax"));

                taxAmountTransactionHelper.items.add(item);
            }
        }
    }

    private LocalDateTime asTransactionDate(String operationDay, String settlementDay, String month, String year)
    {
        var dateTime = asDate(settlementDay + " " + month + " " + year, AdditionalLocales.MEXICO);

        // The statement is ordered by the operation day (OPER). If the
        // settlement day (LIQ) is smaller than the operation day, the
        // settlement falls into the following month (and year, for
        // December statements).
        if (Integer.parseInt(settlementDay) < Integer.parseInt(operationDay))
            dateTime = dateTime.withDayOfMonth(1).plusMonths(1).withDayOfMonth(Integer.parseInt(settlementDay));

        return dateTime;
    }

    private static String getSecurityName(String name, String serie)
    {
        // @formatter:off
        // For securities of the international quotation system (SIC)
        // the series is "*" and is not part of the security name.
        // @formatter:on
        return "*".equals(serie) ? trim(name) : trim(name) + " " + trim(serie);
    }

    private static String getSecurityTickerSymbol(String name, String serie)
    {
        // @formatter:off
        // The BMV listing symbol is the EMISORA concatenated with the series (e.g. "GBMF2BF").
        // For SIC securities the series is "*" and the EMISORA is already the ticker symbol.
        // @formatter:on
        return "*".equals(serie) ? trim(name) : trim(name) + trim(serie);
    }

    private static String getTaxType(String dividendType)
    {
        // @formatter:off
        // "ADbisotnriob eufiedcotivo Resultado Fiscal" is "Abono efectivo Resultado Fiscal
        // Distribuido" with the overlapping description columns interleaved by the text extraction.
        // @formatter:on
        if ("Abono efectivo Resultado Fiscal Distribuido".equals(dividendType)
                        || "ADbisotnriob eufiedcotivo Resultado Fiscal".equals(dividendType))
            return "RETENCION ISR POR RESULTADO FISCAL";

        // @formatter:off
        // "AEXBTORNAON DJEIVRIADENDO EMISORA" is "ABONO DIVIDENDO EMISORA EXTRANJERA"
        // with the overlapping description columns interleaved by the text extraction.
        // @formatter:on
        if ("ABONO DIVIDENDO EMISORA EXTRANJERA".equals(dividendType)
                        || "AEXBTORNAON DJEIVRIADENDO EMISORA".equals(dividendType))
            return "ISR 10 % POR DIVIDENDOS SIC";

        return null;
    }

    private static class TaxAmountTransactionHelper
    {
        private List<TaxAmountTransactionItem> items = new ArrayList<>();

        public Optional<TaxAmountTransactionItem> findItem(int line, LocalDateTime dateTime, String security, String type)
        {
            // The withholding row usually follows the dividend row. Search
            // forward first and fall back to the closest preceding row, in
            // case the text extraction changed the order of the rows.
            TaxAmountTransactionItem match = null;

            for (var item : items)
            {
                if (!item.dateTime.equals(dateTime) || !item.security.equals(security) || !item.type.equals(type))
                    continue;

                match = item;

                if (item.line >= line)
                    break;
            }

            if (match == null)
                return Optional.empty();

            // @formatter:off
            // A withholding row belongs to exactly one dividend row. Remove the item
            // so it is not merged into a second dividend of the same security and day.
            // @formatter:on
            items.remove(match);
            return Optional.of(match);
        }
    }

    private static class TaxAmountTransactionItem
    {
        int line;

        LocalDateTime dateTime;
        String type;
        String security;
        long tax;

        @Override
        public String toString()
        {
            return "TaxAmountTransactionItem [line=" + line + ", dateTime=" + dateTime + ", type=" + type + ", security=" + security + ", tax=" + tax + "]";
        }
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "es", "MX");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "es", "MX");
    }
}
