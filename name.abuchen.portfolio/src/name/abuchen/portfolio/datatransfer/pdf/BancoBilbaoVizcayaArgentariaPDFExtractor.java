package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class BancoBilbaoVizcayaArgentariaPDFExtractor extends AbstractPDFExtractor
{

    public BancoBilbaoVizcayaArgentariaPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("BANCO BILBAO VIZCAYA ARGENTARIA");
        addBankIdentifier("CARTA DE AVISO POR OPERACIONES");
        addBankIdentifier("CARTA DE ABONO POR OPERACIONES");

        addBuySellTransaction_Format01();
        addBuySellTransaction_Format02();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Banco Bilbao Vizcaya Argentaria";
    }

    private void addBuySellTransaction_Format01()
    {
        var type = new DocumentType("(VENTA|COMPRA) DE VALORES");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Te facilitamos el detalle de la operaci.n (VENTA|COMPRA) DE VALORES  que hemos liquidado en tu cuenta.$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "VENTA" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Te facilitamos el detalle de la operaci.n (?<type>(VENTA|COMPRA)) DE VALORES  que hemos liquidado en tu cuenta.$") //
                        .assign((t, v) -> {
                            if ("VENTA".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Valor ACC.INTEL CORPORATION -USD-
                        // ISIN Valor US4581401001
                        // ORIGEN USD            2.100,00
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^Valor (?<name>.*)$") //
                        .match("^ISIN Valor (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^ORIGEN (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Nº de valores 100
                        // @formatter:on
                        .section("shares") //
                        .match("^N. de valores (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Hora ejecución 16.50.47
                        // @formatter:on
                        .section("time").optional() //
                        .match("^Hora ejecuci.n (?<time>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2})$") //
                        .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                        // @formatter:off
                        // Fecha ejecución 20/06/2025
                        // @formatter:on
                        .section("date") //
                        .match("^Fecha ejecuci.n (?<date>[\\d]{1,2}\\/[\\d]{1,2}\\/[\\d]{4})$") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().get("time") != null)
                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                            else
                                t.setDate(asDate(v.get("date")));
                        })

                        // @formatter:off
                        // CONCEPTO DIVISA BASE PRECIO IMPORTE
                        // IMPORTE TOTAL EUR            1.854,38
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^IMPORTE TOTAL (?<currency>[A-Z]{3})[\\s]{1,}(\\-)?(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Cambio divisa 1,1448 USD/EUR
                        // CONCEPTO DIVISA BASE PRECIO IMPORTE
                        // S/títulos USD                  100         21,00000000            2.100,00
                        // @formatter:on
                        .section("fxGross", "termCurrency", "baseCurrency", "exchangeRate") //
                        .match("^Cambio divisa (?<exchangeRate>[\\.,\\d]+) (?<termCurrency>[A-Z]{3})\\/(?<baseCurrency>[A-Z]{3})$") //
                        .match("^S/títulos [A-Z]{3}[\\s]{1,}[\\d]+[\\s]{1,}[\\.,\\d]+[\\s]{1,}(\\-)?(?<fxGross>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                            var gross = rate.convert(rate.getBaseCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addBuySellTransaction_Format02()
    {
        var type = new DocumentType("OPERACIONES DE FONDOS (SUSCRIPCI.N|REEMBOLSO) EN EFECTIVO");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^CARTA DE AVISO POR OPERACIONES DE FONDOS (SUSCRIPCI.N|REEMBOLSO) EN EFECTIVO$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction.subject(() -> {
            var portfolioTransaction = new BuySellEntry();
            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
            return portfolioTransaction;
        })

                        // Is type --> "REEMBOLSO" change from BUY to SELL
                        .section("type").optional() //
                        .match("^CARTA DE AVISO POR OPERACIONES DE FONDOS (?<type>(SUSCRIPCI.N|REEMBOLSO)) EN EFECTIVO$") //
                        .assign((t, v) -> {
                            if ("REEMBOLSO".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // CODIGO CUENTA VALOR: NOMBRE DEL FONDO ES0113925038 NUMERO PARTICIPACIONES CAMBIO DIVISA
                        // 1865 1752 777 888511536 BBVA BOLSA IND. USA CUBIERTO FI 483,2315919 EUR/EUR
                        // @formatter:on
                        .section("isin", "name", "currency") //
                        .match("^CODIGO CUENTA VALOR: NOMBRE DEL FONDO (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) NUMERO PARTICIPACIONES CAMBIO DIVISA$") //
                        .match("^[\\d]+ [\\d]+ [\\d]+ [\\d]+ (?<name>[\\p{L}0-9\\s.]+) [\\.,\\d]+ (?<currency>[A-Z]{3})\\/[A-Z]{3}$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 5197 8750 259 410574728 BBVA BOLSA IND. USA CUBIERTO FI 451,5550246 EUR/EUR
                        // @formatter:on
                        .section("shares") //
                        .match("^[\\d]+ [\\d]+ [\\d]+ [\\d]+ [\\p{L}0-9\\s.]+ (?<shares>[\\.,\\d]+) [A-Z]{3}/[A-Z]{3}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // FECHA EJECUCIÓN
                        // 02-01-2025
                        // @formatter:on
                        .section("date") //
                        .find("FECHA EJECUCI.N") //
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4})$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // CONCEPTO DIVISA PRECIO IMPORTE
                        // SUSCRIPCIÓN EFECTIVO EUR 33,218543 15.000,00
                        //
                        // CONCEPTO DIVISA PRECIO IMPORTE
                        // REEMBOLSO EFECTIVO EUR 33,110418 16.000,00
                        // @formatter:on
                        .section("amount", "currency") //
                        .find("CONCEPTO DIVISA PRECIO IMPORTE") //
                        .match("^(SUSCRIPCIÓN|REEMBOLSO) EFECTIVO (?<currency>[A-Z]{3}) [\\.,\\d]+ (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        var type = new DocumentType("ABONO DE DIVIDENDOS");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^ABONO DE DIVIDENDOS$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction.subject(() -> {
            var accountTransaction = new AccountTransaction();
            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
            return accountTransaction;
        })

                        // @formatter:off
                        // CODIGO CUENTA VALOR VALOR (US8299331004) TIPO INTERÉS FECHA VENCIMIENTO CAMBIO DIVISA
                        // 0182 2229 58 0018380492 ACC.SIRIUS XM HOLDINGS INC 25/02/2025 1,0517 USD/EUR
                        // IMPORTE EFECTIVO USD 33 0,27000000 8,91
                        // @formatter:on
                        .section("isin", "name", "currency") //
                        .match("^CODIGO CUENTA VALOR VALOR \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\).*$") //
                        .match("^[\\d]+ [\\d]+ [\\d]+ [\\d]+ (?<name>[\\p{L}0-9\\s.]+) [\\d]{2}\\/[\\d]{2}\\/[\\d]{4} [\\.,\\d]+ [A-Z]{3}/[A-Z]{3}$") //
                        .match("^IMPORTE EFECTIVO (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // NUMERO DE VALORES IMPORTE BRUTO UNITARIO IMPORTE NETO UNITARIO
                        // 33 0,27000000 USD 0,22950000 USD
                        // @formatter:on
                        .section("shares") //
                        .find("NUMERO DE VALORES IMPORTE BRUTO UNITARIO IMPORTE NETO UNITARIO") //
                        .match("^(?<shares>\\d+) [\\.,\\d]+ [A-Z]{3} [\\.,\\d]+ [A-Z]{3}$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 0182 2229 58 0018380492 ACC.SIRIUS XM HOLDINGS INC 28/05/2025 1,1382 USD/EUR
                        // @formatter:on
                        .section("date") //
                        .match("^[\\d]+ [\\d]+ [\\d]+ [\\d]+ .* (?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}) [\\.,\\d]+ [A-Z]{3}/[A-Z]{3}$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // IMPORTE TOTAL EUR 3,57
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^IMPORTE TOTAL (?<currency>[A-Z]{3}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // 0182 2229 58 0018380492 ACC.SIRIUS XM HOLDINGS INC 28/05/2025 1,1382 USD/EUR
                        // IMPORTE EFECTIVO USD 33 0,27000000 8,91
                        // @formatter:on
                        .section("exchangeRate", "termCurrency", "baseCurrency", "fxGross") //
                        .match("^[\\d]+ [\\d]+ [\\d]+ [\\d]+ .* (?<exchangeRate>[\\.,\\d]+) (?<termCurrency>[A-Z]{3})\\/(?<baseCurrency>[A-Z]{3})$") //
                        .match("^IMPORTE EFECTIVO [A-Z]{3} [\\d]+ [\\.,\\d]+ (?<fxGross>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                            var gross = rate.convert(rate.getBaseCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // RETENCION EN ORIGEN USD -1,34
                        // @formatter:on
                        .section("currency", "withHoldingTax").optional() //
                        .match("^RETENCION EN ORIGEN (?<currency>[A-Z]{3}) \\-(?<withHoldingTax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                        })

                        // @formatter:off
                        // RETENCION EUR 6,65 19,0000% -1,26
                        // @formatter:on
                        .section("currency", "withHoldingTax").optional() //
                        .match("^RETENCION (?<currency>[A-Z]{3}) [\\.,\\d]+ [\\.,\\d]+% \\-(?<withHoldingTax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                        });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // BOLSA (comisión fija) EUR         20,00000000               20,00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^BOLSA \\(comisi.n fija\\) (?<currency>[A-Z]{3})[\\s]{1,}[\\.,\\d]+[\\s]{1,}(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // SUBTOTAL EN ORIGEN) EUR                8,13
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^SUBTOTAL EN ORIGEN\\) (?<currency>[A-Z]{3})[\\s]{1,}(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // CANALIZADOR S/importe EUR             1.100,71             0,4500%               -4,95
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^CANALIZADOR S\\/importe (?<currency>[A-Z]{3})[\\s]{1,}[\\.,\\d]+[\\s]{1,}[\\.,\\d]+%[\\s]{1,}\\-(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // COMISION BANCARIA
                        // Mínimo EUR              -12,00
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^M.nimo (?<currency>[A-Z]{3})[\\s]{1,}\\-(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // COMISION BANCARIA Mínimo EUR -1,50
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^COMISION BANCARIA M.nimo (?<currency>[A-Z]{3})[\\s]{1,}\\-(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // I. V. A. % S/comisión bancaria EUR 1,50 21,0000% -0,32
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^I\\. V\\. A\\. % S\\/comisi.n bancaria (?<currency>[A-Z]{3}) [\\.,\\d]+ [\\.,\\d]+% \\-(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // COMUNICACION EUR               -2,40
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^COMUNICACION (?<currency>[A-Z]{3})[\\s]{1,}\\-(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

}
