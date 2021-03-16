package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class PostbankPDFExtractor extends AbstractPDFExtractor
{
    public PostbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Postbank"); //$NON-NLS-1$

        addDividendeTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return "Deutsche Postbank AG"; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "Postbank"; //$NON-NLS-1$
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("Dividendengutschrift");
        this.addDocumentTyp(type);

        Block block = new Block("Dividendengutschrift");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>()
            .subject(() -> {
                AccountTransaction entry = new AccountTransaction();
                entry.setType(AccountTransaction.Type.DIVIDENDS);
                return entry;
            });

        pdfTransaction

                // Stück 12 JOHNSON & JOHNSON  SHARES US4781601046 (853260)
                // REGISTERED SHARES DL 1
                .section("isin", "wkn", "name", "shares", "nameContinued")
                .match("^St.ck (?<shares>[\\d.,]+) (?<name>.*) (?<isin>[\\w]{12}.*) (\\((?<wkn>.*)\\).*)")
                .match("(?<nameContinued>.*)")
                .assign((t, v) -> {
                    t.setSecurity(getOrCreateSecurity(v));
                    t.setShares(asShares(v.get("shares")));
                })

                // Ex-Tag 22.02.2021 Art der Dividende Quartalsdividende
                .section("date")
                .match("^Ex-Tag (?<date>\\d+.\\d+.\\d{4}) .*")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                })

                // Ex-Tag 22.02.2021 Art der Dividende Quartalsdividende
                // Geschäftsjahr 01.01.2021 - 31.12.2021
                .section("note").optional()
                .match("^Ex-Tag \\d+.\\d+.\\d{4} Art der Dividende (?<note>.*)")
                .assign((t, v) -> {
                    t.setNote(v.get("note"));
                })

                // Ausmachender Betrag 8,64+ EUR
                .section("currency", "amount")
                .match("^Ausmachender Betrag (?<amount>[\\d.]+,\\d+)\\+ (?<currency>\\w{3})")
                .assign((t, v) -> {
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(v.get("currency"));
                })

                // Devisenkurs EUR / USD  1,1920
                // Devisenkursdatum 09.03.2021
                // Dividendengutschrift 12,12 USD 10,17+ EUR
                .section("exchangeRate", "fxAmount", "fxCurrency", "amount", "currency").optional()
                .match("^Devisenkurs .* (?<exchangeRate>[\\d.]+,\\d+)$")
                .match("^Devisenkursdatum .*")
                .match("^Dividendengutschrift (?<fxAmount>[\\d.]+,\\d+) (?<fxCurrency>\\w{3}) (?<amount>[\\d.]+,\\d+)\\+ (?<currency>\\w{3})$")                        
                .assign((t, v) -> {
                    BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                    if (t.getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                    {
                        exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                    }
                    type.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());

                    if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                    {
                        BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                        RoundingMode.HALF_DOWN);

                        // check, if forex currency is transaction
                        // currency or not and swap amount, if necessary
                        Unit grossValue;
                        if (!asCurrencyCode(v.get("fxCurrency")).equals(t.getCurrencyCode()))
                        {
                            Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(v.get("fxAmount")));
                            Money amount = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        else
                        {
                            Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                            asAmount(v.get("fxAmount")));
                            Money fxAmount = Money.of(asCurrencyCode(v.get("currency")),
                                            asAmount(v.get("amount")));
                            grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, inverseRate);
                        }
                        t.addUnit(grossValue);
                    }
                })

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction

            // Anrechenbare Quellensteuer 15 % auf 10,17 EUR 1,53 EUR
            .section("tax", "currency").optional()
            .match("^Anrechenbare Quellensteuer [.,\\d]+ % .* [.,\\d]+ \\w{3} (?<tax>[.,\\d]+) (?<currency>\\w{3})$")
            .assign((t, v) -> processTaxEntries(t, v, type));
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
}
