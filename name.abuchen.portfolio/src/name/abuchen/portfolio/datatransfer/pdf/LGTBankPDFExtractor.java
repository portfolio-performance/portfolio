package name.abuchen.portfolio.datatransfer.pdf;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class LGTBankPDFExtractor extends AbstractPDFExtractor
{
    public LGTBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("LGT Bank AG"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return "LGT Bank AG"; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "LGT Bank AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType(".*Abrechnung Kauf.*");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block(".*Abrechnung Kauf.*");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Titel A.P. Moeller - Maersk A/S
                // Namen- und Inhaber-Aktien -B-
                // ISIN DK0010244508
                // Valorennummer 906020
                // Wertpapierkennnummer 861837
                .section("isin", "wkn", "name", "nameContinued")
                .match("^(Titel) (?<name>.*)$")
                .match("(?<nameContinued>.*)")
                .match("^(ISIN) (?<isin>[\\w]{12}.*)$")
                .match("^(Wertpapierkennnummer) (?<wkn>.*)$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                })
    
                // Abschlussdatum 14.04.2020 09:00:02
                .section("date", "time")
                .match("^(Abschlussdatum) (?<date>\\d+.\\d+.\\d{4}+) (?<time>\\d+:\\d+:\\d+)$")
                .assign((t, v) -> {
                    if (v.get("time") != null)
                        t.setDate(asDate(v.get("date"), v.get("time")));
                    else
                        t.setDate(asDate(v.get("date")));
                })
                
                // Anzahl 12 Stück
                .section("shares")
                .match("^(Anzahl) (?<shares>[\\d.,]+) (Stück)$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                })
    
                // Belastung DKK Konto 0037156.021 DKK 82'452.21
                .section("currency", "amount")
                .match("^(Belastung.* Konto) .* (?<currency>[\\w]{3}) (?<amount>['.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(convertAmount(v.get("amount"))));
                })
    
                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }


    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Barausschüttung .*");
        this.addDocumentTyp(type);

        Block block = new Block("Barausschüttung .*");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // 551 Veolia Environnement SA
                // Namen- und Inhaber-Aktien
                // ISIN: FR0000124141, Valoren-Nr.: 1098758
                .section("shares", "isin", "wkn", "name", "nameContinued")
                .match("^(?<shares>[\\d.,]+) (?<name>.*)$")
                .match("(?<nameContinued>.*)")
                .match("^(ISIN:) (?<isin>[\\w]{12}), .* (?<wkn>.*)$")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })
    
                // Ex-Datum 12. Mai 2020
                .section("date")
                .match("^(Ex-Datum) (?<date>\\d+. \\w+ \\d{4})$")
                .assign((t, v) -> {
                    // Formate the date from 14. Mai 2020 to 14.05.2020
                    v.put("date", DateTimeFormatter.ofPattern("dd.MM.yyyy").format(LocalDate.parse(v.get("date"), DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMANY))));
                    t.setDateTime(asDate(v.get("date")));
                })
    
                // Netto EUR 198.36
                .section("currency", "amount")
                .match("^(Netto) (?<currency>[\\w]{3}) *(?<amount>['.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(convertAmount(v.get("amount"))));
                })
    
                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        
        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Eidg. Umsatzabgabe  DKK 121.19
                .section("tax", "currency").optional()
                .match("^(Eidg. Umsatzabgabe)\\s+(?<currency>[\\w]{3}) (?<tax>['.,\\d]+)$")
                .assign((t, v) -> {
                    v.put("tax", convertAmount(v.get("tax")));
                    processTaxEntries(t, v, type);
                })

                // Quellensteuer 28 % EUR -77.14
                .section("tax", "currency").optional()
                .match("^(Quellensteuer) .* (?<currency>[\\w]{3}) (?<tax>-['.,\\d]+)$")
                .assign((t, v) -> {
                    v.put("tax", convertAmount(v.get("tax")));
                    processTaxEntries(t, v, type);
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Courtage  DKK 1'534.90
                .section("fee", "currency").optional()
                .match("^(Courtage)\\s+(?<currency>[\\w]{3}) (?<fee>['.,\\d]+)$")
                .assign((t, v) -> {
                    v.put("fee", convertAmount(v.get("fee")));
                    processFeeEntries(t, v, type);
                })
        
                // Broker Kommission  DKK 12.12
                .section("fee", "currency").optional()
                .match("^(Broker Kommission)\\s+(?<currency>[\\w]{3}) (?<fee>['.,\\d]+)$")
                .assign((t, v) -> {
                    v.put("fee", convertAmount(v.get("fee")));
                    processFeeEntries(t, v, type);
                });
    }

    private void processTaxEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax, (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
            PDFExtractorUtils.checkAndSetTax(tax, ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }

    private void processFeeEntries(Object t, Map<String, String> v, DocumentType type)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee, 
                            (name.abuchen.portfolio.model.Transaction) t, type);
        }
        else
        {
            Money fee = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee")));
            PDFExtractorUtils.checkAndSetFee(fee,
                            ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction(), type);
        }
    }
    
    private String convertAmount(String inputAmount)
    {
        String amount = inputAmount.replace("'", "");
        return amount.replace(".", ",");
    }
}
