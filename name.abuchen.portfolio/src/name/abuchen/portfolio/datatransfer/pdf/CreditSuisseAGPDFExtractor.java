package name.abuchen.portfolio.datatransfer.pdf;

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
public class CreditSuisseAGPDFExtractor extends AbstractPDFExtractor
{

    public CreditSuisseAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("CREDIT SUISSE"); //$NON-NLS-1$

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "Credit Suisse AG"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("Ihr Kauf|Ihr Verkauf");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^Ihr (Kauf|Verkauf) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^Ihr (?<type>Verkauf) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                    {
                        t.setType(PortfolioTransaction.Type.SELL);
                    }
                })

                // 900 Registered Shs Iron Mountain Inc USD 0.01
                // Valor 26754105, IRM, ISIN US46284V1017
                // Kurswert USD 27,270.00
                .section("shares", "name", "isin", "currency").optional()
                .match("^(?<shares>[.,\\d]+) (?<name>.*) [\\w]{3} [.,\\d]+$")
                .match("^.* ISIN (?<isin>[\\w]{12})$")
                .match("^Kurswert (?<currency>[\\w]{3}) [.,\\d]+$")
                .assign((t, v) -> {
                    v.put("shares", convertAmount(v.get("shares")));

                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // USD 200,000 6.25 % Fixed Rate Notes Norddeutsche
                // Landesbank Girozentrale 2014-10.4.24 Reg-S
                // Subord.
                // Valor 24160639, NDKH, ISIN XS1055787680
                // Kurswert USD 183,000.00
                .section("shares", "name", "isin", "currency").optional()
                .match("^[\\w]{3} (?<shares>[.,\\d]+) (?<name>.*)$")
                .match("^.* ISIN (?<isin>[\\w]{12})$")
                .match("^Kurswert (?<currency>[\\w]{3}) [.,\\d]+$")
                .assign((t, v) -> {
                    v.put("shares", convertAmount(v.get("shares")));

                    /***
                     * Workaround for bonds 
                     */
                    t.setShares((asShares(v.get("shares")) / 100));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Datum 08.06.2020
                .section("date")
                .match("^Datum (?<date>\\d+.\\d+.\\d{4})$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // Belastung USD 27,734.70
                // Gutschrift EUR 28,744.30
                .section("currency", "amount")
                .match("^(Belastung|Gutschrift) (?<currency>[\\w]{3}) (?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(convertAmount(v.get("amount"))));
                })

                /***
                 * If we have the "Internet-Vergünstigung",
                 * we add up it from the amount and reset it.
                 * The "Internet discount" is then posted as a fee refund.
                 * 
                 * If changes are made in this area, 
                 * the fee refund function must be adjusted.
                 * addFeeReturnBlock(type);
                 */
                // Internet-Vergünstigung USD - 41.81
                // Internet-Vergünstigung USD 44.69
                .section("feeRefund", "currency").optional()
                .match("^Internet-Verg.nstigung (?<currency>[\\w]{3}) ([-\\s]+)?(?<feeRefund>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setAmount(t.getPortfolioTransaction().getAmount() + asAmount(convertAmount(v.get("feeRefund"))));
                })

                .wrap(t -> new BuySellEntryItem(t));

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
        addFeeReturnBlock(type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Ertragsabrechnung");
        this.addDocumentTyp(type);

        Block block = new Block("^Ertragsabrechnung .*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction
                // USD 200,000 6.25 % FIXED RATE NOTES NORDDEUTSCHE
                // LANDESBANK GIROZENTRALE 2014-10.4.24 REG-S
                // SUBORD.
                // Valor 24160639, NDKH, ISIN XS1055787680
                // Bruttoertrag USD 6,250.00
                .section("shares", "name", "isin", "currency")
                .match("^[\\w]{3} (?<shares>[.,\\d]+) (?<name>.*)$")
                .match("^.* ISIN (?<isin>[\\w]{12})$")
                .match("^Bruttoertrag (?<currency>[\\w]{3}) [.,\\d]+$")
                .assign((t, v) -> {
                    v.put("shares", convertAmount(v.get("shares")));

                    /***
                     * Workaround for bonds 
                     */
                    t.setShares((asShares(v.get("shares")) / 100));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Coupon-Verfall 10.04.2021
                .section("date")
                .match("^Coupon-Verfall (?<date>\\d+.\\d+.\\d{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Bruttoertrag USD 6,250.00
                .section("currency", "amount")
                .match("^Gutschrift (?<currency>[\\w]{3}) (?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(convertAmount(v.get("amount"))));
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addFeeReturnBlock(DocumentType type)
    {
        /***
         * If changes are made in this area,
         * the buy/sell transaction function must be adjusted.
         * addBuySellTransaction();
         */
        Block block = new Block("^Ihr (Kauf|Verkauf) .*$");
        type.addBlock(block);
        block.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction t = new AccountTransaction();
                    t.setType(AccountTransaction.Type.FEES_REFUND);
                    return t;
                })

                // 900 Registered Shs Iron Mountain Inc USD 0.01
                // Valor 26754105, IRM, ISIN US46284V1017
                // Kurswert USD 27,270.00
                .section("shares", "name", "currency", "isin").optional()
                .match("^(?<shares>[.,\\d]+) (?<name>.*) [\\w]{3} [.,\\d]+$")
                .match("^.* ISIN (?<isin>[\\w]{12})$")
                .match("^Kurswert (?<currency>[\\w]{3}) [.,\\d]+$")
                .assign((t, v) -> {
                    t.setShares(asShares(v.get("shares")));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // USD 200,000 6.25 % Fixed Rate Notes Norddeutsche
                // Landesbank Girozentrale 2014-10.4.24 Reg-S
                // Subord.
                // Valor 24160639, NDKH, ISIN XS1055787680
                // Kurswert USD 183,000.00
                .section("shares", "name", "isin", "currency").optional()
                .match("^[\\w]{3} (?<shares>[.,\\d]+) (?<name>.*)$")
                .match("^.* ISIN (?<isin>[\\w]{12})$")
                .match("^Kurswert (?<currency>[\\w]{3}) [.,\\d]+$")
                .assign((t, v) -> {
                    v.put("shares", convertAmount(v.get("shares")));

                    t.setShares((asShares(v.get("shares")) / 100));
                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Datum 08.06.2020
                .section("date")
                .match("^Datum (?<date>\\d+.\\d+.\\d{4})$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // Internet-Vergünstigung USD - 41.81
                // Internet-Vergünstigung USD 44.69
                .section("currency", "amount").optional()
                .match("^Internet-Verg.nstigung (?<currency>[\\w]{3}) ([-\\s]+)?(?<amount>[.,\\d]+)$")
                .assign((t, v) -> {
                    v.put("amount", convertAmount(v.get("amount")));

                    t.setCurrencyCode(v.get("currency"));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Eidgenössische Umsatzabgabe USD 40.91
                .section("tax", "currency").optional()
                .match("^Eidgen.ssische Umsatzabgabe (?<currency>[\\w]{3}) ([-\\s]+)?(?<tax>[.,\\d]+)$")
                .assign((t, v) -> {
                    v.put("tax", convertAmount(v.get("tax")));
                    processTaxEntries(t, v, type);   
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Kommission Schweiz/Ausland USD 463.60
                .section("currency", "fee").optional()
                .match("^Kommission Schweiz\\/Ausland (?<currency>[\\w]{3}) ([-\\s]+)?(?<fee>[.,\\d]+)$")
                .assign((t, v) -> {
                    v.put("fee", convertAmount(v.get("fee")));
                    processFeeEntries(t, v, type);   
                })

                // Kommission Schweiz USD 1,413.65
                .section("currency", "fee").optional()
                .match("^Kommission Schweiz (?<currency>[\\w]{3}) ([-\\s]+)?(?<fee>[.,\\d]+)$")
                .assign((t, v) -> {
                    v.put("fee", convertAmount(v.get("fee")));
                    processFeeEntries(t, v, type);   
                })

                // Kosten und Abgaben Ausland USD 2.00
                .section("currency", "fee").optional()
                .match("^Kosten und Abgaben (?<currency>[\\w]{3}) ([-\\s]+)?(?<fee>[.,\\d]+)$")
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
        String amount = inputAmount.replace(",", "");
        return amount.replace(".", ",");
    }
}
