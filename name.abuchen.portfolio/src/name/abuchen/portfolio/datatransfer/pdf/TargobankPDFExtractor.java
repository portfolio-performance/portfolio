package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

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
import name.abuchen.portfolio.util.Pair;

/**
 * @formatter:off
 * @implNote Targo bank provides two documents for the transaction.
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
 *              fixMissingCurrencyConversionForSaleTaxesTransactions(Collection<TransactionTaxesPair>)
 *           }
 *
 *           Always import the securities transaction and the taxes treatment for a correct transaction.
 *           Due to rounding differences, the correct gross amount is not always shown in the securities transaction.
 *
 *           In postProcessing, we always finally delete the taxes treatment.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class TargobankPDFExtractor extends AbstractPDFExtractor
{
    private static record TransactionTaxesPair(Item transaction, Item tax)
    {
    }

    private static final String ATTRIBUTE_GROSS_TAXES_TREATMENT = "gross_taxes_treatment";

    public TargobankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("TARGO");
        addBankIdentifier("Targobank");
        addBankIdentifier("TARGOBANK AG");

        addBuySellTransaction();
        addDividendeTransaction();
        addTaxesTreatmentTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Targobank AG";
    }

    private void addBuySellTransaction()
    {
        final DocumentType type = new DocumentType("Effektenabrechnung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^Effektenabrechnung .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            BuySellEntry portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Transaktionstyp (?<type>(Kauf|Verkauf))$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Wertpapier FanCy shaRe. nAmE X0-X0
                        // WKN / ISIN ABC123 / DE0000ABC123
                        // Kurs 12,34 EUR
                        // @formatter:on
                        .section("name", "wkn", "isin", "currency") //
                        .match("^Wertpapier (?<name>.*)$") //
                        .match("^WKN \\/ ISIN (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^(Kurs|Preis vom) .* (?<currency>[\\w]{3})$")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück 987,654
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Schlusstag / Handelszeit 02.01.2020 / 13:01:00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Schlusstag \\/ Handelszeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\/ (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Schlusstag 10.01.2020
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // Konto-Nr. 0101753165 1.008,91 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Konto\\-Nr\\. .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Rechnungsnummer: BOE-2020-0223620085-0000068
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Rechnungsnummer: (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("R.-Nr.: " + trim(v.get("note"))))

                        // @formatter:off
                        // Referenznummer 555666777888
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Referenznummer (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), "Ref.-Nr.: " + trim(v.get("note")), " | ")))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final DocumentType type = new DocumentType("(Ertragsgutschrift|Dividendengutschrift) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}");
        this.addDocumentTyp(type);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();

        Block firstRelevantLine = new Block("^(Ertragsgutschrift|Dividendengutschrift) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Wertpapier Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN
                        // WKN / ISIN A12CX1 / IE00BKX55T58
                        // Ausschüttung pro Stück 0,293466 USD
                        // @formatter:on
                        .section("name", "wkn", "isin", "currency") //
                        .match("^Wertpapier (?<name>.*)$") //
                        .match("^WKN \\/ ISIN (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                        .match("^(Aussch.ttung|Dividende) pro St.ck [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Stück 81
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Zahlbar 24.06.2020
                        // @formatter:on
                        .section("date") //
                        .match("^Zahlbar (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Konto-Nr. 1234567890 21,18 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Konto\\-Nr\\. .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // Bruttoertrag 23,77 USD
                        // Devisenkurs zur Handelswährung USD/EUR 1,1223
                        // Bruttoertrag in EUR 21,18 EUR
                        // @formatter:on
                        .section("fxGross", "fxCurrency", "termCurrency", "baseCurrency", "exchangeRate", "gross", "currency").optional() //
                        .match("^Bruttoertrag (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$") //
                        .match("^Devisenkurs zur Handelsw.hrung (?<termCurrency>[\\w]{3})\\/(?<baseCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                        .match("^Bruttoertrag in [\\w]{3} (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            Money gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                            Money fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Rechnungsnummer: CPS-2020-0123456789-0001234
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Rechnungsnummer: (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("R.-Nr.: " + trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addTaxesTreatmentTransaction()
    {
        final DocumentType type = new DocumentType("\\(Steuerbeilage\\)");
        this.addDocumentTyp(type);

        Block block = new Block("^.* \\(Steuerbeilage\\) .*$");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        block.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Wertpapier FanCy shaRe. nAmE X0-X0
                                        // WKN / ISIN ABC123 / DE0000ABC123
                                        // Kurs 12,34 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .match("^Wertpapier (?<name>.*)$") //
                                                        .match("^WKN \\/ ISIN (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(Kurs|Preis vom) .* (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Wertpapier Vang.FTSE Develop.World U.ETF - Registered Shares USD Dis.oN
                                        // WKN / ISIN A12CX1 / IE00BKX55T58
                                        // Gesamtsumme Steuern 5,59 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "wkn", "isin", "currency") //
                                                        .match("^Wertpapier (?<name>.*)$") //
                                                        .match("^WKN \\/ ISIN (?<wkn>[A-Z0-9]{6}) \\/ (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Gesamtsumme Steuern [\\.,\\d]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Stück 987,654
                        // @formatter:on
                        .section("shares") //
                        .match("^St.ck (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Schlusstag / Handelszeit 26.05.2020 / 20:32:00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Schlusstag \\/ Handelszeit (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\/ (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Schlusstag 10.01.2020
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Belastung Ihres Kontos NUMMER mit Wertstellung zum 24. Juni 2020.
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Belastung Ihres Kontos .* (?<date>[\\d]{2}\\. .* [\\d]{4})\\.$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))),
                                        // @formatter:off
                                        // Only use for dividend transactions, when the pay date is missing
                                        // Ex-Tag 11.06.2020
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Ex-Tag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Teilfreistellung (§ 20 InvStG) 30,00 % - 6,35 EUR
                                        // Erträge/Verluste 14,83 EUR
                                        // Bemessungsgrundlage 0,00 EUR
                                        // Gesamtsumme Steuern 5,59 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("partialExemptionTaxes", "currencypartialExemptionTaxes", "grossBeforeTaxes", "currencyBeforeTaxes", "grossAssessmentBasis", "currencyAssessmentBasis", "deductedTaxes", "currencyDeductedTaxes") //
                                                        .match("^Teilfreistellung \\(§ 20 InvStG\\) [\\.,\\d]+ % \\- (?<partialExemptionTaxes>[\\.,\\d]+) (?<currencypartialExemptionTaxes>[\\w]{3})$") //
                                                        .match("^Ertr.ge\\/Verluste (?<grossBeforeTaxes>[\\.,\\d]+) (?<currencyBeforeTaxes>[\\w]{3})$") //
                                                        .match("^Bemessungsgrundlage (?<grossAssessmentBasis>[\\.,\\d]+) (?<currencyAssessmentBasis>[\\w]{3})$") //
                                                        .match("^Gesamtsumme Steuern (?<deductedTaxes>[\\.,\\d]+) (?<currencyDeductedTaxes>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            Money partialExemptionTaxes = Money.of(asCurrencyCode(v.get("currencypartialExemptionTaxes")), asAmount(v.get("partialExemptionTaxes")));
                                                            Money grossBeforeTaxes = Money.of(asCurrencyCode(v.get("currencyBeforeTaxes")), asAmount(v.get("grossBeforeTaxes")));
                                                            grossBeforeTaxes = grossBeforeTaxes.add(partialExemptionTaxes);

                                                            Money grossAssessmentBasis = Money.of(asCurrencyCode(v.get("currencyAssessmentBasis")), asAmount(v.get("grossAssessmentBasis")));
                                                            Money deductedTaxes = Money.of(asCurrencyCode(v.get("currencyDeductedTaxes")), asAmount(v.get("deductedTaxes")));

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
                                        // Erträge/Verluste 3.123,25 EUR
                                        // Bemessungsgrundlage 3.123,25 EUR
                                        // Gesamtsumme Steuern 823,76 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("grossBeforeTaxes", "currencyBeforeTaxes", "grossAssessmentBasis", "currencyAssessmentBasis", "deductedTaxes", "currencyDeductedTaxes") //
                                                        .match("^Ertr.ge\\/Verluste (?<grossBeforeTaxes>[\\.,\\d]+) (?<currencyBeforeTaxes>[\\w]{3})$") //
                                                        .match("^Bemessungsgrundlage (?<grossAssessmentBasis>[\\.,\\d]+) (?<currencyAssessmentBasis>[\\w]{3})$") //
                                                        .match("^Gesamtsumme Steuern (?<deductedTaxes>[\\.,\\d]+) (?<currencyDeductedTaxes>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            Money grossBeforeTaxes = Money.of(asCurrencyCode(v.get("currencyBeforeTaxes")), asAmount(v.get("grossBeforeTaxes")));
                                                            Money grossAssessmentBasis = Money.of(asCurrencyCode(v.get("currencyAssessmentBasis")), asAmount(v.get("grossAssessmentBasis")));
                                                            Money deductedTaxes = Money.of(asCurrencyCode(v.get("currencyDeductedTaxes")), asAmount(v.get("deductedTaxes")));

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
                                        // We don't have to pay taxes
                                        //
                                        // Veräußerungsverlust - 1.311,10 EUR
                                        // Erträge/Verluste - 1.311,10 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("grossLoss", "currencyLoss", "grossBeforeTaxes", "currencyBeforeTaxes") //
                                                        .match("^Ver.u.erungsverlust \\- (?<grossLoss>[\\.,\\d]+) (?<currencyLoss>[\\w]{3})$") //
                                                        .match("^Ertr.ge\\/Verluste \\- (?<grossBeforeTaxes>[\\.,\\d]+) (?<currencyBeforeTaxes>[\\w]{3})$") //
                                                        .assign((t, v) -> {
                                                            Money grossLoss = Money.of(asCurrencyCode(v.get("currencyLoss")), asAmount(v.get("grossLoss")));
                                                            Money grossBeforeTaxes = Money.of(asCurrencyCode(v.get("currencyBeforeTaxes")), asAmount(v.get("grossBeforeTaxes")));
                                                            grossBeforeTaxes = grossLoss.subtract(grossBeforeTaxes);

                                                            // There is no taxes to pay
                                                            if (grossBeforeTaxes.isZero())
                                                            {
                                                                // Store in transaction context
                                                                v.getTransactionContext().put(ATTRIBUTE_GROSS_TAXES_TREATMENT, grossBeforeTaxes);
                                                                t.setMonetaryAmount(grossBeforeTaxes);
                                                            }
                                                        }))

                        // @formatter:off
                        // Transaktionsreferenz TR TBK14720B024746O001
                        // Transaktionsreferenz INDTBK1234567890
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Transaktionsreferenz( TR)? (?<note>.*)$") //
                        .assign((t, v) -> t.setNote("Tr.-Nr.: " + trim(v.get("note"))))

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

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
                // Provision 8,90 EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^Provision (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }


    /**
     * @formatter:off
     * This method performs post-processing on a list transaction items, categorizing and
     * modifying them based on their types and associations. It follows several steps:
     *
     * 1. Filters the input list to isolate taxes treatment transactions, sale transactions, and dividend transactions.
     * 2. Matches sale transactions with their corresponding taxes treatment and dividend transactions with their corresponding taxes treatment.
     * 3. Adjusts sale transactions by subtracting tax amounts, adding tax units, combining source information, appending tax-related notes,
     *    and removing taxes treatment's from the list of items.
     * 4. Adjusts dividend transactions by updating the gross amount if necessary, subtracting tax amounts, adding tax units,
     *    combining source information, appending taxes treatment notes, and removing taxes treatment's from the list of items.
     *
     * The goal of this method is to process transactions and ensure that taxes treatment is accurately reflected
     * in sale and dividend transactions, making the transaction's more comprehensive and accurate.
     *
     * @param items The list of transaction items to be processed.
     * @return A modified list of transaction items after post-processing.
     * @formatter:on
     */
    @Override
    public void postProcessing(List<Item> items)
    {
        // Filter transactions by taxes treatment's
        List<Item> taxesTreatmentList = items.stream() //
                        .filter(TransactionItem.class::isInstance) //
                        .filter(i -> i.getSubject() instanceof AccountTransaction) //
                        .filter(i -> { //
                            var type = ((AccountTransaction) i.getSubject()).getType(); //
                            return type == AccountTransaction.Type.TAXES || type == AccountTransaction.Type.TAX_REFUND; //
                        }) //
                        .toList();

        // Filter transactions by buySell transactions
        List<Item> saleTransactionList = items.stream() //
                        .filter(BuySellEntryItem.class::isInstance) //
                        .filter(i -> i.getSubject() instanceof BuySellEntry) //
                        .filter(i -> PortfolioTransaction.Type.SELL //
                                        .equals((((BuySellEntry) i.getSubject()).getPortfolioTransaction().getType()))) //
                        .toList();

        // Filter transactions by dividend transactions
        List<Item> dividendTransactionList = items.stream() //
                        .filter(TransactionItem.class::isInstance) //
                        .filter(i -> i.getSubject() instanceof AccountTransaction) //
                        .filter(i -> AccountTransaction.Type.DIVIDENDS //
                                        .equals((((AccountTransaction) i.getSubject()).getType()))) //
                        .toList();

        var saleTaxPairs = matchTransactionPair(saleTransactionList, taxesTreatmentList);
        var dividendTaxPairs = matchTransactionPair(dividendTransactionList, taxesTreatmentList);

        fixMissingCurrencyConversionForSaleTaxesTransactions(saleTaxPairs);

        // @formatter:off
        // This loop iterates through a list of sale and tax pairs and processes them.
        //
        // For each pair, it subtracts the tax amount from the sale transaction's total amount,
        // adds the tax as a tax unit to the sale transaction, combines source information if needed,
        // appends taxes treatment notes to the sale transaction, and removes the tax treatment from the 'items' list.
        //
        // It performs these operations when a valid tax transaction is found.
        // @formatter:on
        for (TransactionTaxesPair pair : saleTaxPairs)
        {
            var saleTransaction = (BuySellEntry) pair.transaction.getSubject();
            var taxesTransaction = pair.tax() != null ? (AccountTransaction) pair.tax().getSubject() : null;

            if (taxesTransaction != null && taxesTransaction.getType() == AccountTransaction.Type.TAXES)
            {
                saleTransaction.setMonetaryAmount(saleTransaction.getPortfolioTransaction().getMonetaryAmount()
                                .subtract(taxesTransaction.getMonetaryAmount()));

                saleTransaction.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, taxesTransaction.getMonetaryAmount()));

                saleTransaction.setSource(concatenate(saleTransaction.getSource(), taxesTransaction.getSource(), "; "));

                saleTransaction.setNote(concatenate(saleTransaction.getNote(), taxesTransaction.getNote(), " | "));

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
                Money grossTaxesTreatment = (Money) pair.tax().getData(ATTRIBUTE_GROSS_TAXES_TREATMENT);

                if (grossTaxesTreatment != null)
                {
                    Money dividendAmount = dividendTransaction.getMonetaryAmount();
                    Money taxesAmount = taxesTransaction.getMonetaryAmount();

                    if (taxesAmount.isZero() && grossTaxesTreatment.isLessThan(dividendAmount))
                    {
                        Money adjustedTaxes  = dividendAmount.subtract(grossTaxesTreatment);
                        dividendTransaction.addUnit(new Unit(Unit.Type.TAX, adjustedTaxes ));
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

                dividendTransaction.setSource(concatenate(dividendTransaction.getSource(), taxesTransaction.getSource(), "; "));

                dividendTransaction.setNote(concatenate(dividendTransaction.getNote(), taxesTransaction.getNote(), " | "));

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
    private Collection<TransactionTaxesPair> matchTransactionPair(List<Item> transactionList, List<Item> taxesTreatmentList)
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

        // Iterate through the list of taxes treatment's to match them with transactions
        taxesTreatmentList.forEach( //
                        tax -> {
                            // Check if the taxes treatment has a security
                            if (tax.getSecurity() == null)
                                return;

                            // Create a key based on the taxes treatment date and security
                            var key = new Pair<>(tax.getDate().toLocalDate(), tax.getSecurity());

                            // Retrieve the TransactionTaxesPair associated with this key, if it exists
                            var pair = pairs.get(key);

                            // Skip if no transaction is found or if a taxes treatment already exists
                            if (pair != null && pair.tax() == null)
                                pairs.put(key, new TransactionTaxesPair(pair.transaction(), tax));
                        } //
        );

        return pairs.values();
    }

    /**
     * @formatter:off
     * This method fixes missing currency conversion for taxes transactions.
     *
     * It iterates through a collection of TransactionTaxesPair objects and performs the necessary currency conversions
     * if required based on the currency codes of the involved transactions.
     *
     * @param saleTaxPairs A collection of TransactionTaxesPair objects containing taxes and sale transactions.
     * @formatter:on
     */
    private void fixMissingCurrencyConversionForSaleTaxesTransactions(Collection<TransactionTaxesPair> saleTaxPairs)
    {
        saleTaxPairs.forEach( //
                        pair -> { //
                            if (pair.tax != null)
                            {
                                // Get the taxes treatment from the SaleTaxPair
                                var tax = (AccountTransaction) pair.tax.getSubject();

                                // Check if currency conversion is needed
                                if (!tax.getSecurity().getCurrencyCode().equals(tax.getMonetaryAmount().getCurrencyCode()))
                                {
                                    // Get the sale transaction from the SaleTaxPair
                                    var sale = (BuySellEntry) pair.transaction.getSubject();

                                    // Check if we have an exchange rate available from the sale transaction
                                    var grossValue = sale.getPortfolioTransaction().getUnit(Unit.Type.GROSS_VALUE);

                                    if (grossValue.isPresent() && grossValue.get().getExchangeRate() != null)
                                    {
                                        // Create and set the required grossUnit to the taxes treatment
                                        var rate = new ExtrExchangeRate(grossValue.get().getExchangeRate(),
                                                        sale.getPortfolioTransaction().getSecurity().getCurrencyCode(),
                                                        tax.getCurrencyCode());

                                        String termCurrency = sale.getPortfolioTransaction().getSecurity().getCurrencyCode();
                                        Money fxGross = rate.convert(termCurrency, tax.getMonetaryAmount());

                                        // Add the converted gross value unit to the taxes transaction
                                        tax.addUnit(new Unit(Unit.Type.GROSS_VALUE, tax.getMonetaryAmount(), fxGross, rate.getRate()));
                                    }
                                }
                            }
                        } //
        );
    }
}
