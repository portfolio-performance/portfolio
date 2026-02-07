package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.Pair;

/**
 * @formatter:off
 * @implNote Arkéa Direct Bank / Fortuneo Banque provides two documents for the transaction.
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
 *              applyMissingCurrencyConversionBetweenTaxesAndPurchaseSale(Collection<TransactionTaxesPair> purchaseSaleTaxPairs)
 *           }
 *
 *           Always import the securities transaction and the taxes treatment for a correct transaction.
 *           Due to rounding differences, the correct gross amount is not always shown in the securities transaction.
 *
 *           In postProcessing, we always finally delete the taxes treatment.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class ArkeaDirectBankPDFExtractor extends AbstractPDFExtractor
{
    private static record TransactionTaxesPair(Item transaction, Item tax)
    {
    }

    public ArkeaDirectBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Arkéa Direct Bank");

        addBuySellTransaction_Format01();
        addBuySellTransaction_Format02();
        addDividendeTransaction();
        addTaxesTreatmentTransaction();
        addDepositTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Arkéa Direct Bank / Fortuneo Banque";
    }

    private void addBuySellTransaction_Format01()
    {
        var securityRange = new Block("^.* (ACTION|TRACKER|OPCVM) : .* \\([A-Z]{2}[A-Z0-9]{9}[0-9]\\)$") //
                        .asRange(section -> section //
                                        // @formatter:off
                                        // ¢ ACTION : ORANGE (FR0000133308)
                                        // Quantité 46 Cours 10,646 €
                                        //
                                        // ¢ TRACKER : AMUNDI MSCI WORLD UC.ETF EUR D (LU2655993207)
                                        // Quantité 7 Cours 30,24 €
                                        //
                                        // ¢ OPCVM : IND.ET EXP.EUROPE SM.XC EUR 4D (LU1832174889)
                                        // Quantité 24,5 Val. Liquidative 237,36 €
                                        // @formatter:on
                                        .attributes("name", "isin", "currency") //
                                        .match("^.* (ACTION|TRACKER|OPCVM) : (?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\)$") //
                                        .match("^Quantit. [\\,\\d\\s]+ (Cours|Val\\. Liquidative) [\\,\\d\\s]+ (?<currency>\\p{Sc})$"));

        final var type = new DocumentType("AVIS D.OP.RATIONS", securityRange);
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} R.f.rence .*$", "^Montant NET .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Vente" change from BUY to SELL
                        .section("type").optional() //
                        .match(".* Sens (?<type>(Achat|Vente|Souscription)).*$") //
                        .assign((t, v) -> {
                            if ("Vente".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // Quantité 46 Cours 10,646 €
                        // Quantité 1 450 Cours 5,4941 €
                        // Quantité 24,5 Val. Liquidative 237,36 €
                        // @formatter:on
                        .section("shares") //
                        .documentRange("name", "isin", "currency") //
                        .match("^Quantit. (?<shares>[\\,\\d\\s]+) (Cours|Val\\. Liquidative) [\\,\\d\\s]+ \\p{Sc}$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setShares(asShares(v.get("shares")));
                        })

                        .oneOf(
                                        // @formatter:off
                                        // 26-03-2024 Référence 94R6134018440990
                                        // 16:27:35 Sens Achat - Exécution unique
                                        // @formatter:on
                                        section -> section //
                                            .attributes("date", "time") //
                                            .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) R.f.rence .*$") //
                                            .match("^(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) .* \\- .*$") //
                                            .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),

                                         // 02-02-2026 Référence 94F7048294366804
                                         section -> section //
                                             .attributes("date") //
                                             .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) R.f.rence .*$") //
                                             .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // Montant NET 491,67 € 491,67 €
                        // Montant NET 5 068,28 € 5 068,28 €
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Montant NET [\\d\\s]+,[\\d]{2} \\p{Sc} (?<amount>[\\d\\s]+,[\\d]{2}) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // 26-03-2024 Référence 94R6134018440990
                        // @formatter:on
                        .section("note").optional() //
                        .match("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} (?<note>R.f.rence .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addBuySellTransaction_Format02()
    {
        final var type = new DocumentType("(Souscription . titre r.ductible|Souscription avec droits)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^RESULTAT D'OST.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // @formatter:off
                        // Nous vous informons que  nous avons procédé à la souscription de 1 titre(s) VEOLIA ENVIRON. (FR0000124141).
                        // montant net de  : - 22,70 EUR
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^.*titre\\(s\\) (?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\).*$") //
                        .match("^montant net.* \\- [\\,\\d\\s]+ (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Nous vous informons que  nous avons procédé à la souscription de 1 titre(s) VEOLIA ENVIRON. (FR0000124141).
                        // @formatter:on
                        .section("shares") //
                        .match("^.*la souscription de (?<shares>[\\,\\d]+) titre\\(s\\).*.*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // Au 8 octobre 2021 2 gst eBJu gAAqm ZZYZnPSIY
                        // @formatter:on
                        .section("date") //
                        .match("^Au (?<date>[\\d]{1,2} .* [\\d]{4}).*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        // @formatter:off
                        // montant net de  : - 22,70 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^montant net.* \\- (?<amount>[\\,\\d\\s]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(BuySellEntryItem::new);
    }

    private void addDepositTransaction()
    {
        var type = new DocumentType("LIQUIDITES sur PEA",
                        documentContext -> documentContext //
                        // @formatter:off
                        // Nouveau Solde au 31/12/2020 5 315,65
                        // @formatter:on
                        .section("year") //
                        .match("^Nouveau Solde au 31/12/(?<year>[\\d]{4}) .*$") //
                        .assign((ctx, v) -> {
                            ctx.put("year", v.get("year"));
                        })
                        // @formatter:off
                        // Date Libellé Débit € Crédit €
                        // @formatter:on
                        .section("currency") //
                                        .match("^Date Libell. D.bit (?<currency>\\p{Sc}) Cr.dit \\1$") //
                        .assign((ctx, v) -> {
                            ctx.put("currency", v.get("currency"));
                        }));
        this.addDocumentTyp(type);


        // @formatter:off
        // 02/03 VERSEMENT 3 000,00
        // 10/03 REGULARISATION 331,98
        // @formatter:on
        var depositBlock = new Block("^(?<day>[\\d]{2})\\/(?<month>[\\d]{2}) " //
                        + "(VERSEMENT|REGULARISATION) " //
                        + "(?<amount>[\\.,\\d\\\s]+)");
        type.addBlock(depositBlock);

        depositBlock.set(new Transaction<AccountTransaction>() //
                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })
                        .section("amount", "day", "month", "note") //
                        .documentContext("year", "currency") //
                        .match("^(?<day>[\\d]{2})\\/(?<month>[\\d]{2}) " //
                                        + "(?<note>VERSEMENT|REGULARISATION) " //
                                        + "(?<amount>[\\.,\\d\\\s]+)")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setDateTime(asDate(v.get("day") + "." + v.get("month") + "." + v.get("year")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }

    private void addDividendeTransaction()
    {
        var type = new DocumentType("AVIS D.ENCAISSEMENT COUPON");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^AVIS D.ENCAISSEMENT COUPON.*$");
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
                                        //   n       ACTION  ORANGE (FR0000133308)
                                        // Montant unitaire 0,30 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^.* (ACTION|OPCVM) (?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\).*$") //
                                                        .match("^Montant unitaire [\\,\\d\\s]+ (?<currency>\\p{Sc})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        //   n       OPCVM  AMUND.MSCI WORLD D (LU2655993207)
                                        // Montant unitaire 0,21 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^.* (ACTION|OPCVM) (?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\).*$") //
                                                        .match("^Montant unitaire [\\,\\d\\s]+ (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        //   n       ACTION  ENGIE (FR0010208488))
                                        // Montant Net 140,00 € 140,00 €
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^.* (ACTION|OPCVM) (?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\).*$") //
                                                        .match("^Montant Net [\\,\\d\\s]+ (?<currency>\\p{Sc}).*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Quantité 450
                        // 28/04/2023 Quantité 100
                        // @formatter:on
                        .section("shares") //
                        .match("^.*Quantit. (?<shares>[\\,\\d\\s]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 04/12/2023
                        // @formatter:on
                        .section("date") //
                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}\\/[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Montant Net 135,00 €
                        // Montant Net 140,00 € 140,00 €
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Montant Net (?<amount>[\\,\\d\\s]+) (?<currency>\\p{Sc}).*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addTaxesTreatmentTransaction()
    {
        var type = new DocumentType("AVIS RECAPITULATIF DE LA TAXE SUR LES");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^.* \\([A-Z]{2}[A-Z0-9]{9}[0-9]\\)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // ¾ ORANGE (FR0000133308)
                        // Montant global soumis à la TTF : 489,72 €
                        // @formatter:on
                        .section("name", "isin", "currency") //
                        .match("^. (?<name>.*) \\((?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])\\)$") //
                        .match("^Montant global soumis à la TTF : [\\,\\d\\s]+ (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Quantité compensée soumise à la TTF : 46
                        // @formatter:on
                        .section("shares") //
                        .match("^Quantit. compens.e soumise . la TTF : (?<shares>[\\,\\d\\s]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 26-03-2024 Taxe Transaction Financière
                        // @formatter:on
                        .section("date") //
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) Taxe Transaction Financi.re$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Montant global soumis à la TTF : 489,72 €
                        // Taux de TTF : 0,30 %
                        // 1,47
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Montant global soumis à la TTF : [\\,\\d\\s]+ (?<currency>\\p{Sc})$") //
                        .find("Taux de TTF : [\\,\\d\\s]+ %")
                        .match("(Montant de la TTF:[\\s]*|^)(?<amount>[\\,\\d\\s]+)$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // 26-03-2024 Référence 94R6134018440990
                        // @formatter:on
                        .section("note").optional() //
                        .match("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} (?<note>R.f.rence .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Prélèvement fiscal 0,00 €
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Pr.l.vement fiscal (?<tax>[\\,\\d\\s]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Prélèvements sociaux 0,00 €
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Pr.l.vements sociaux (?<tax>[\\,\\d\\s]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Courtage et Commission 1,95 €
                        // Droits entrée/sortie 116,31 €
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^(Courtage et Commission|Droits entr.e\\/sortie) (?<fee>[\\,\\d\\s]+) (?<currency>\\p{Sc})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "fr", "FR");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "fr", "FR");
    }

    /**
     * @formatter:off
     * This method performs post-processing on a list transaction items, categorizing and
     * modifying them based on their types and associations. It follows several steps:
     *
     * 1. Filters the input list to isolate taxes treatment transactions, purchase/sale transactions.
     * 2. Matches purchase/sale transactions with their corresponding taxes treatment.
     * 3. Adjusts purchase/sale transactions by adding/subtracting tax amounts, adding tax units, combining source information, appending tax-related notes,
     *    and removing taxes treatment's from the list of items.
     *
     * The goal of this method is to process transactions and ensure that taxes treatment is accurately reflected
     * in purchase/sale transactions, making the transaction's more comprehensive and accurate.
     *
     * @param items The list of transaction items to be processed.
     * @return A modified list of transaction items after post-processing.
     * @formatter:on
     */
    @Override
    public void postProcessing(List<Item> items)
    {
        // Filter transactions by taxes treatment's
        var taxesTreatmentList = items.stream() //
                        .filter(TransactionItem.class::isInstance) //
                        .filter(i -> i.getSubject() instanceof AccountTransaction) //
                        .filter(i -> { //
                            var type = ((AccountTransaction) i.getSubject()).getType(); //
                            return type == AccountTransaction.Type.TAXES || type == AccountTransaction.Type.TAX_REFUND; //
                        }) //
                        .toList();

        // Filter transactions by buySell transactions
        var purchaseSaleTransactionList = items.stream() //
                        .filter(BuySellEntryItem.class::isInstance) //
                        .filter(i -> i.getSubject() instanceof BuySellEntry) //
                        .filter(i -> { //
                            var type = ((BuySellEntry) i.getSubject()).getPortfolioTransaction().getType(); //
                            return PortfolioTransaction.Type.SELL.equals(type)
                                            || PortfolioTransaction.Type.BUY.equals(type); //
                        }) //
                        .toList();

        var purchaseSaleListTaxPairs = matchTransactionPair(purchaseSaleTransactionList, taxesTreatmentList);

        applyMissingCurrencyConversionBetweenTaxesAndPurchaseSale(purchaseSaleListTaxPairs);

        // @formatter:off
        // This loop iterates through a list of purchase/sale and tax pairs and processes them.
        //
        // For each pair, it adds/subtracts the tax amount from the purchase/sale transaction's total amount,
        // adds the tax as a tax unit to the purchase/sale transaction, combines source information if needed,
        // appends taxes treatment notes to the purchase/sale transaction, and removes the tax treatment from the 'items' list.
        //
        // It performs these operations when a valid tax transaction is found.
        // @formatter:on
        for (TransactionTaxesPair pair : purchaseSaleListTaxPairs)
        {
            var purchaseSaleTransaction = (BuySellEntry) pair.transaction.getSubject();
            var taxesTransaction = pair.tax() != null ? (AccountTransaction) pair.tax().getSubject() : null;

            if (taxesTransaction != null && taxesTransaction.getType() == AccountTransaction.Type.TAXES)
            {
                if (purchaseSaleTransaction.getPortfolioTransaction().getType().isLiquidation())
                {
                    purchaseSaleTransaction.setMonetaryAmount(purchaseSaleTransaction.getPortfolioTransaction()
                                    .getMonetaryAmount().subtract(taxesTransaction.getMonetaryAmount()));
                }
                else
                {
                    purchaseSaleTransaction.setMonetaryAmount(purchaseSaleTransaction.getPortfolioTransaction()
                                    .getMonetaryAmount().add(taxesTransaction.getMonetaryAmount()));
                }

                purchaseSaleTransaction.getPortfolioTransaction()
                                .addUnit(new Unit(Unit.Type.TAX, taxesTransaction.getMonetaryAmount()));

                purchaseSaleTransaction.setSource(
                                concatenate(purchaseSaleTransaction.getSource(), taxesTransaction.getSource(), "; "));

                purchaseSaleTransaction.setNote(
                                concatenate(purchaseSaleTransaction.getNote(), taxesTransaction.getNote(), " | "));

                ExtractorUtils.fixGrossValueBuySell().accept(purchaseSaleTransaction);

                items.remove(pair.tax());
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
    private Collection<TransactionTaxesPair> matchTransactionPair(List<Item> transactionList,
                    List<Item> taxesTreatmentList)
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

        // Iterate through the list of taxes treatment's to match them with
        // transactions
        taxesTreatmentList.forEach( //
                        tax -> {
                            // Check if the taxes treatment has a security
                            if (tax.getSecurity() == null)
                                return;

                            // Create a key based on the taxes treatment date
                            // and security
                            var key = new Pair<>(tax.getDate().toLocalDate(), tax.getSecurity());

                            // Retrieve the TransactionTaxesPair associated with
                            // this key, if it exists
                            var pair = pairs.get(key);

                            // Skip if no transaction is found or if a taxes
                            // treatment already exists
                            if (pair != null && pair.tax() == null)
                                pairs.put(key, new TransactionTaxesPair(pair.transaction(), tax));
                        } //
        );

        return pairs.values();
    }

    /**
     * @formatter:off
     * Resolves missing currency conversions between taxes and purchase/sale transactions based on existing exchange rates.
     *
     * For each TransactionTaxesPair, this method checks for currency mismatches between:
     * - the monetary amount and security currency of the taxes transaction, and
     * - the monetary amount and security currency of the purchase/sale transaction.
     *
     * If either side shows a mismatch, and if the opposite side contains a valid exchange rate,
     * a corresponding GROSS_VALUE unit with the appropriate FX conversion will be added to ensure consistency.
     *
     * This helps ensure that both tax and purchase/sale transactions carry correct currency conversion data
     * when working across multi-currency portfolios.
     *
     * @param purchaseSaleTaxPairs A collection of TransactionTaxesPair objects containing associated taxes and purchase/sale transactions.
     * @formatter:on
     */
    private void applyMissingCurrencyConversionBetweenTaxesAndPurchaseSale(
                    Collection<TransactionTaxesPair> purchaseSaleTaxPairs)
    {
        purchaseSaleTaxPairs.forEach(pair -> {
            if (pair.tax != null && pair.transaction != null)
            {
                var tax = (AccountTransaction) pair.tax.getSubject();
                var purchaseSale = (BuySellEntry) pair.transaction.getSubject();
                var purchaseSalePortfolioTx = purchaseSale.getPortfolioTransaction();

                // Determine currency of monetary amounts and associated
                // securities
                var taxCurrency = tax.getMonetaryAmount().getCurrencyCode();
                var taxSecurityCurrency = tax.getSecurity().getCurrencyCode();

                var purchaseSaleCurrency = purchaseSalePortfolioTx.getMonetaryAmount().getCurrencyCode();
                var purchaseSaleSecurityCurrency = purchaseSalePortfolioTx.getSecurity().getCurrencyCode();

                var taxHasMismatch = !taxCurrency.equals(taxSecurityCurrency);
                var purchaseSaleHasMismatch = !purchaseSaleCurrency.equals(purchaseSaleSecurityCurrency);

                // Proceed only if at least one of the transactions has a
                // currency mismatch
                if (taxHasMismatch || purchaseSaleHasMismatch)
                {
                    var taxAmount = tax.getMonetaryAmount();

                    var taxGrossValue = tax.getUnit(Unit.Type.GROSS_VALUE);
                    var purchaseSaleGrossValue = purchaseSalePortfolioTx.getUnit(Unit.Type.GROSS_VALUE);

                    // If the taxes transaction contains a usable exchange rate,
                    // apply the conversion to the sales transaction. Otherwise,
                    // if the purchase/sales transaction contains a usable
                    // exchange rate,
                    // apply the conversion to the taxes transaction.
                    if (taxGrossValue.isPresent() && taxGrossValue.get().getExchangeRate() != null)
                    {
                        var rate = new ExtrExchangeRate(taxGrossValue.get().getExchangeRate(),
                                        purchaseSaleSecurityCurrency, taxCurrency);
                        var fxGross = rate.convert(purchaseSaleSecurityCurrency,
                                        purchaseSalePortfolioTx.getMonetaryAmount());

                        purchaseSalePortfolioTx.addUnit(new Unit(Unit.Type.GROSS_VALUE,
                                        purchaseSalePortfolioTx.getMonetaryAmount(), fxGross, rate.getRate()));
                    }
                    else if (purchaseSaleGrossValue.isPresent()
                                    && purchaseSaleGrossValue.get().getExchangeRate() != null)
                    {
                        var rate = new ExtrExchangeRate(purchaseSaleGrossValue.get().getExchangeRate(),
                                        purchaseSaleSecurityCurrency, taxCurrency);
                        var fxGross = rate.convert(purchaseSaleSecurityCurrency, taxAmount);

                        tax.addUnit(new Unit(Unit.Type.GROSS_VALUE, taxAmount, fxGross, rate.getRate()));
                    }
                }
            }
        });
    }
}
