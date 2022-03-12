package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class DekaBankPDFExtractor extends AbstractPDFExtractor
{
    public DekaBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("DekaBank"); //$NON-NLS-1$

        addBuySellTransaction();
        addDepotStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "DekaBank Deutsche Girozentrale"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(LASTSCHRIFTEINZUG|TAUSCH/KAUF|TAUSCH/VERKAUF|VERKAUF)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(LASTSCHRIFTEINZUG|TAUSCH\\/KAUF|TAUSCH\\/VERKAUF|VERKAUF) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(?<type>(LASTSCHRIFTEINZUG|TAUSCH\\/KAUF|TAUSCH\\/VERKAUF|VERKAUF)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("TAUSCH/VERKAUF") || (v.get("type").equals("VERKAUF")))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Bezeichnung: Deka-UmweltInvest TF
                // ISIN: DE000DK0ECT0 Unterdepot: 00 Auftragsnummer: 8103 1017
                // =Abrechnungsbetrag EUR 4.000,00 EUR 4.000,00 EUR 494,260000 Anteilumsatz: 8,093
                .section("name", "isin", "currency").optional()
                .match("^Bezeichnung: (?<name>.*)$")
                .match("^ISIN: (?<isin>[\\w]{12})( .*)?$")
                .match("^.* (?<currency>[\\w]{3}) [\\.,\\d]+ Anteilumsatz: (\\-)?[\\.,\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Bezeichnung: Deka-Liquidität: EURO TF
                // ISIN: DE0009771824
                // Kurs
                // EUR 128,610000
                .section("name", "isin", "currency").optional()
                .match("^Bezeichnung: (?<name>.*)$")
                .match("^ISIN: (?<isin>[\\w]{12})( .*)?$")
                .find("Kurs(\\/Kaufpreis)?")
                .match("^(?<currency>[\\w]{3}) [\\.,\\d]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Anteilumsatz: 5,112
                // Anteilumsatz: -2,598
                // =Abrechnungsbetrag EUR 4.000,00 EUR 4.000,00 EUR 187,770000 Anteilumsatz: 21,303
                .section("shares")
                .match("(^|^.* )?Anteilumsatz: (\\-)?(?<shares>[\\.,\\d]+)$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Verwahrart: GiroSammel Abrechnungstag: 18.05.2021
                // Abrechnungstag: 06.06.2018
                .section("date")
                .match("(^|^.* )?Abrechnungstag: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                .oneOf(
                                // Einzugsbetrag EUR 4.000,00 Kurs/Kaufpreis Bestand alt: 11,291
                                // Abrechnungsbetrag EUR 332,31 EUR 332,
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^(Abrechnungsbetrag|Einzugsbetrag) (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)( .*)?$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // Auszahlungsbetrag EUR 332,31
                                // Einzugsbetrag EUR 4.000,00
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^(Auszahlungsbetrag|Einzugsbetrag) (?<currency>[\\w]{3}) (?<amount>[.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
    }

    public void addDepotStatementTransaction()
    {
        final DocumentType type = new DocumentType("Quartalsbericht per [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}", (context, lines) -> {
            Pattern pAccountCurrency = Pattern.compile("^depot in (?<accountCurrency>[\\w]{3}) in [\\w]{3}$");
            Pattern pSecurityCurrency = Pattern.compile("^(?<securityCurrency>[\\w]{3}) Fremdw.hrung [\\w]{3}$");
            Pattern pIsin = Pattern.compile("^ISIN: (?<isin>[\\w]{12}) .*$");

            int endBlock = lines.length;
            String securityCurrency = CurrencyUnit.EUR;

            for (int i = lines.length - 1; i >= 0; i--)
            {
                Matcher m = pAccountCurrency.matcher(lines[i]);
                if (m.matches())
                    context.put("accountCurrency", m.group("accountCurrency"));

                m = pIsin.matcher(lines[i]);
                if (m.matches())
                {
                    /***
                     * Search the security currency in the block
                     */
                    for (int ii = i; ii < endBlock; ii++)
                    {
                        Matcher m1 = pSecurityCurrency.matcher(lines[ii]);
                        if (m1.matches())
                            securityCurrency = m1.group("securityCurrency");
                    }

                    /***
                     * Stringbuilder:
                     * security_(security name)_(currency)_(start@line)_(end@line) = isin
                     * 
                     * Example:
                     * Deka-GlobalChampions TF
                     * ISIN: DE000DK0ECV6 Unterdepot: 00
                     * EUR Fremdwährung EUR Anteile tag nungstag
                     */
                    StringBuilder securityListKey = new StringBuilder("security_");
                    securityListKey.append(trim(lines[i - 1])).append("_");
                    securityListKey.append(securityCurrency).append("_");
                    securityListKey.append(Integer.toString(i)).append("_");
                    securityListKey.append(Integer.toString(endBlock));
                    context.put(securityListKey.toString(), m.group("isin"));

                    endBlock = i;
                }
            }
        });
        this.addDocumentTyp(type);

        Block buySellBlock = new Block("^(Lastschrifteinzug|Verkauf) .*$");
        type.addBlock(buySellBlock);
        buySellBlock.set(new Transaction<BuySellEntry>()
            .subject(() -> {
                BuySellEntry entry = new BuySellEntry();
                entry.setType(PortfolioTransaction.Type.BUY);
                return entry;
        })

                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(?<type>(Lastschrifteinzug|Verkauf)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // Lastschrifteinzug 250,00 198,660000 +1,258 01.04.2021 01.04.2021
                .section("amount", "shares", "date")
                .match("^(Lastschrifteinzug|Verkauf) (?<amount>[\\.,\\d]+) [\\.,\\d]+ [\\-|\\+](?<shares>[\\.,\\d]+) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    Security securityData = getSecurity(context, v.getStartLineNumber());
                    if (securityData != null)
                    {
                        v.put("name", securityData.getName());
                        v.put("isin", securityData.getIsin());
                        v.put("currency", asCurrencyCode(securityData.getCurrency()));
                    }
                    
                    t.setDate(asDate(v.get("date")));
                    t.setShares(asShares(v.get("shares")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("accountCurrency")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                .wrap(t -> {
                    if (t.getPortfolioTransaction().getCurrencyCode() != null)
                        return new BuySellEntryItem(t);
                    return null;
                }));

        Block deliveryInOutbondblock = new Block("^(Einbuchung|Ausbuchung) .*$");
        type.addBlock(deliveryInOutbondblock);
        deliveryInOutbondblock.set(new Transaction<PortfolioTransaction>()
            .subject(() -> {
                PortfolioTransaction transaction = new PortfolioTransaction();
                transaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                return transaction;
        })

                // Is type --> "Ausbuchung" change from DELIVERY_INBOUND to DELIVERY_OUTBOUND
                .section("type").optional()
                .match("^(?<type>(Einbuchung|Ausbuchung)) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Ausbuchung"))
                    {
                        t.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                    }
                })

                // Ausbuchung w/ Fusion -2,140 31.05.2021 28.05.2021
                // Einbuchung w/ Fusion +1,315 31.05.2021 28.05.2021
                .section("shares", "date")
                .match("^(Einbuchung|Ausbuchung) .* [\\-|\\+](?<shares>[\\.,\\d]+) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    Security securityData = getSecurity(context, v.getStartLineNumber());
                    if (securityData != null)
                    {
                        v.put("name", securityData.getName());
                        v.put("isin", securityData.getIsin());
                        v.put("currency", asCurrencyCode(securityData.getCurrency()));
                    }
                    
                    t.setDateTime(asDate(v.get("date")));
                    t.setShares(asShares(v.get("shares")));
                    t.setAmount(0L);
                    t.setCurrencyCode(asCurrencyCode(context.get("accountCurrency")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Ausbuchung w/ Fusion -2,140 31.05.2021 28.05.2021
                // Einbuchung w/ Fusion +1,315 31.05.2021 28.05.2021                          
                .section("note").optional()
                .match("^(Einbuchung|Ausbuchung) .* (?<note>.*) [\\-|\\+][\\.,\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(t -> {
                    if (t.getCurrencyCode() != null)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // + Verrechnete Steuern EUR 1,72
                .section("currency", "tax").optional()
                .match("^\\+ Verrechnete Steuern (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private Security getSecurity(Map<String, String> context, Integer entry)
    {
        for (String key : context.keySet())
        {
            String[] parts = key.split("_"); //$NON-NLS-1$
            if (parts[0].equalsIgnoreCase("security")) //$NON-NLS-1$
            {
                if (entry >= Integer.parseInt(parts[3]) && entry <= Integer.parseInt(parts[4]))
                {
                    // returns security name, isin, security currency
                    return new Security(parts[1], context.get(key), parts[2]);
                }
            }
        }
        return null;
    }

    private static class Security
    {
        public Security(String name, String isin, String currency)
        {
            this.name = name;
            this.isin = isin;
            this.currency = currency;
        }

        private String name;
        private String isin;
        private String currency;

        public String getName()
        {
            return name;
        }

        public String getIsin()
        {
            return isin;
        }

        public String getCurrency()
        {
            return currency;
        }
    }
}
