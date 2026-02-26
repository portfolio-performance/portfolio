package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class DirectaSimPDFExtractor extends AbstractPDFExtractor
{
    public DirectaSimPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("DIRECTA SIM");

        addBuySellTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Directa SIM";
    }

    private void addBuySellTransaction()
    {
        final DocumentType type = new DocumentType("(acquisto|vendita) di", //
                        documentContext -> documentContext //
                                        .oneOf( //
                                                        // @formatter:off
                                                        //                                               Quantita'                USD              Euro     Prezzo         Valuta
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("currency") //
                                                                        .match("^[\\s]{1,}Quantita.[\\s]{1,}([A-Z]{3}|[A-Za-z\\s\\-\\'\\.,]+)[\\s]{1,}(?<currency>([A-Z]{3}|[A-Za-z\\s\\-\\'\\.,]+))[\\s]{1,}Prezzo.*$") //
                                                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get( "currency")))),
                                                        // @formatter:off
                                                        //                                      Quantita'                 Euro               Prezzo      Valuta
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("currency") //
                                                                        .match("^[\\s]{1,}Quantita.[\\s]{1,}(?<currency>([A-Z]{3}|[A-Za-z\\s\\-\\'\\.,]+))[\\s]{1,}Prezzo.*$") //
                                                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get( "currency"))))));

        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Nota Informativa per l'ordine.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Tipo di Operazione: Vendita     Mercato di esecuzione: Borsa - XETRA
                        // @formatter:on
                        .section("type").optional() //
                        .match("^Tipo di Operazione: (?<type>(Vendita|Acquisto)).*$") //
                        .assign((t, v) -> {
                            if ("Vendita".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // per l'acquisto di: 9  CROCS INC ISIN US2270461096
                                        //                                               Quantita'                USD              Euro     Prezzo         Valuta
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^.*: [\\.,\\d]+[\\s]{1,}(?<name>.*) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^[\\s]{1,}Quantita.[\\s]{1,}(?<currency>([A-Z]{3}|[A-Za-z\\s\\-\\'\\.,]+))[\\s]{1,}([A-Z]{3}|[A-Za-z\\s\\-\\'\\.,]+)[\\s]{1,}Prezzo.*$") //
                                                        .assign((t, v) -> {
                                                            v.put("currency", asCurrencyCode(v.get("currency")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        // @formatter:off
                                        // per l'acquisto di: 29  VANGUARD FTSE ALL-WORLD UCITS ISIN IE00BK5BQT80
                                        //                                                   Quantita'                 Euro               Prezzo      Valuta
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^.*: [\\.,\\d]+[\\s]{1,}(?<name>.*) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^[\\s]{1,}Quantita.[\\s]{1,}(?<currency>([A-Z]{3}|[A-Za-z\\s\\-\\'\\.,]+))[\\s]{1,}Prezzo.*$") //
                                                        .assign((t, v) -> {
                                                            v.put("currency", asCurrencyCode(v.get("currency")));

                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // per l'acquisto di: 9  CROCS INC ISIN US2270461096
                        // @formatter:on
                        .section("shares") //
                        .match("^.*: (?<shares>[\\.,\\d]+)[\\s]{1,}.* ISIN [A-Z]{2}[A-Z0-9]{9}[0-9].*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        //  5.01.2024  14:02:36  Eseguito                           29             3.074,29             106,0100  09.01.2024
                        // 12.04.2024  09:03:49  Richiesta Immissione                7                                  118,5000
                        // @formatter:on
                        .section("date", "time") //
                        .match("^[\\s]*(?<date>[\\d]{1,2}\\.[\\d]{2}\\.[\\d]{4})[\\s]{1,}(?<time>[\\d]{2}\\:[\\d]{2}\\:[\\d]{2})[\\s]{1,}Eseguito.*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        .oneOf( //
                                        // @formatter:off
                                        //                                      Totale a Vs. Debito                               3.079,29
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount") //
                                                        .documentContext("currency") //
                                                        .match("^.*Totale a Vs\\. Debito[\\s]{1,}(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        //                     Totale a Vs. Debito    :                      778,7700           731,44
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount") //
                                                        .documentContext("currency") //
                                                        .match("^.*Totale a Vs\\. Debito[\\s]{1,}:[\\s]{1,}[\\.,\\d]+[\\s]{1,}(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        //                      Totale a Vs. Credito                             10.910,19
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount") //
                                                        .documentContext("currency") //
                                                        .match("^.*Totale a Vs\\. Credito[\\s]{1,}(?<amount>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        //                                               Quantita'                USD              Euro     Prezzo         Valuta
                        //                     Totale a Vs. Debito    :                      778,7700           731,44
                        // =C/$=  1,06470
                        // @formatter:on
                        .section("termCurrency", "baseCurrency", "fxGross", "exchangeRate").optional() //
                        .match("^[\\s]{1,}Quantita.[\\s]{1,}(?<termCurrency>([A-Z]{3}|[A-Za-z\\s\\-\\'\\.,]+))[\\s]{1,}(?<baseCurrency>([A-Z]{3}|[A-Za-z\\s\\-\\'\\.,]+))[\\s]{1,}Prezzo.*$") //
                        .match("^.*Totale a Vs\\. Debito[\\s]{1,}:[\\s]{1,}(?<fxGross>[\\.,\\d]+)[\\s]{1,}[\\.,\\d]+$") //
                        .match("^.*\\/.*=[\\s]{1,}(?<exchangeRate>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("baseCurrency", asCurrencyCode(v.get("baseCurrency")));
                            v.put("termCurrency", asCurrencyCode(v.get("termCurrency")));

                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                            Money gross = rate.convert(rate.getBaseCurrency(), fxGross);

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Nota Informativa per l'ordine T1673620593440
                        // @formatter:on
                        .section("note") //
                        .match("^Nota Informativa per l.ordine (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("Ordine " + trim(v.get("note"))))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        //                       Commissioni:                                          5,00
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^.*Commissioni:[\\s]{1,}(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        //                     Commissioni:                                    9,0000             8,45*
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^.*Commissioni:[\\s]{1,}[\\.,\\d]+[\\s]{1,}(?<fee>[\\.,\\d]+)\\*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        //                      Commissioni:                                         -9,50
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^.*Commissioni:[\\s]{1,}\\-(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        //                      Capital Gain *                                     -101,51               5,3106
                        // @formatter:on
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^.*Capital Gain[\\s]{1,}\\*[\\s]{1,}\\-(?<tax>[\\.,\\d]+).*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }
}
