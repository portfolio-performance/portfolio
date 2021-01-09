package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

public class DABPDFExtractor extends AbstractPDFExtractor
{
    public DABPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("DAB Bank"); //$NON-NLS-1$
        addBankIdentifier("BNP Paribas S.A. Niederlassung Deutschland"); //$NON-NLS-1$

        addBuyTransaction();
        addSellTransaction();
        addDividendTransaction();
        addProceedsTransaction();
    }

    @Override
    public String getPDFAuthor()
    {
        return "Computershare Communication Services GmbH"; //$NON-NLS-1$
    }

    @Override
    public String getLabel()
    {
        return "DAB Bank"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Kauf");
        this.addDocumentTyp(type);

        Block block = new Block("^Kauf .*$", "Dieser Beleg wird .*$");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("isin", "name", "currency") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("^(?<name>.*) (?<isin>[^ ]*)$")
                        .match("STK [\\d.]+(,\\d+)? (?<currency>\\w{3}) ([\\d.]+,\\d+)$")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .find("Nominal Kurs") //
                        .match("^STK (?<shares>[\\d.]+(,\\d+)?) (\\w{3}) ([\\d.]+,\\d+)$")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency").optional() //
                        .find("Wert Konto-Nr\\. Betrag zu Ihren Lasten")
                        .match("^(\\d+\\.\\d+\\.\\d{4}) ([0-9]*) (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("amount", "currency", "exchangeRate").optional() //
                        .find("Wert Konto-Nr\\. Devisenkurs Betrag zu Ihren Lasten")
                        .match("^(\\d+\\.\\d+\\.\\d{4}) ([0-9]*) \\w{3}\\/\\w{3} (?<exchangeRate>[\\d.,]+) (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            type.getCurrentContext().put("exchangeRate", v.get("exchangeRate"));
                        })

                        .section("date") //
                        .match("^Handelstag (?<date>\\d+\\.\\d+\\.\\d{4}) .*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("date", "time").optional() //
                        .match("^Handelstag (?<date>\\d+\\.\\d+\\.\\d{4}) .*$") //
                        .match("^Handelszeit (?<time>\\d+:\\d+).*$").assign((t, v) -> {
                            if (v.get("time") != null)
                            {
                                t.setDate(asDate(v.get("date"), v.get("time")));
                            }
                            else
                            {
                                t.setDate(asDate(v.get("date")));
                            }
                        })

                        .section("fees", "currency").optional() //
                        .match("^.* Registrierungsspesen (?<currency>\\w{3}+) (?<fees>[\\d.]+,\\d+)-$")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("fees"))))))

                        .section("fees", "currency").optional() //
                        .match("^.* Provision (?<currency>\\w{3}+) (?<fees>[\\d.]+,\\d+)-$").assign((t, v) -> {
                            String currency = asCurrencyCode(v.get("currency"));

                            if (currency.equals(t.getAccountTransaction().getCurrencyCode()))
                            {
                                t.getPortfolioTransaction().addUnit(
                                                new Unit(Unit.Type.FEE, Money.of(currency, asAmount(v.get("fees")))));
                            }
                            else
                            {
                                BigDecimal exchangeRate = asExchangeRate(type.getCurrentContext().get("exchangeRate"));
                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                                RoundingMode.HALF_DOWN);

                                long fxfees = asAmount(v.get("fees"));
                                long fees = new BigDecimal(fxfees).divide(exchangeRate, 10, RoundingMode.HALF_DOWN).longValue();
                                t.getPortfolioTransaction().addUnit(
                                                new Unit(Unit.Type.FEE, Money.of(t.getAccountTransaction().getCurrencyCode(), fees), Money.of(currency, fxfees), inverseRate));
                            }
                        })

                        .section("fees", "currency").optional() //
                        .match("^.* Handelsplatzentgelt (?<currency>\\w{3}) (?<fees>[\\d.]+,\\d+)-$").assign((t, v) -> {
                            String currency = asCurrencyCode(v.get("currency"));

                            if (currency.equals(t.getAccountTransaction().getCurrencyCode()))
                            {
                                t.getPortfolioTransaction().addUnit(
                                                new Unit(Unit.Type.FEE, Money.of(currency, asAmount(v.get("fees")))));
                            }
                            else
                            {
                                BigDecimal exchangeRate = asExchangeRate(type.getCurrentContext().get("exchangeRate"));
                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                                RoundingMode.HALF_DOWN);

                                long fxfees = asAmount(v.get("fees"));
                                long fees = new BigDecimal(fxfees).divide(exchangeRate, 10, RoundingMode.HALF_DOWN).longValue();
                                t.getPortfolioTransaction().addUnit(
                                                new Unit(Unit.Type.FEE, Money.of(t.getAccountTransaction().getCurrencyCode(), fees), Money.of(currency, fxfees), inverseRate));
                            }
                        })

                        .section("amount", "currency", "exchangeRate", "forex", "forexCurrency", "curr").optional() //
                        .find("^Handel.* (Ausmachender Betrag|Kurswert) (?<forexCurrency>\\w{3}) (?<forex>[\\d.]+,\\d+)-")
                        .find("Wert Konto-Nr. Devisenkurs Betrag zu Ihren Lasten")
                        .match("^(\\d+\\.\\d+\\.\\d{4}) ([0-9]*) (?<curr>\\w{3})\\/\\w{3} (?<exchangeRate>[\\d.]+,\\d+) (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {

                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                            t.setMonetaryAmount(amount);

                            BigDecimal exchangeRate = BigDecimal.ONE.divide( //
                                            asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);

                            // if curr is not the transaction currency, we need
                            // to use the reverse rate
                            if (!amount.getCurrencyCode().equals(v.get("curr")))
                            {
                                exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                            }

                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forex")));

                            // the amount needs to be adjusted for fees already deducted
                            Money fees = t.getPortfolioTransaction().getUnitSum(Unit.Type.FEE);
                            
                            amount = amount.subtract(fees);
                            
                            Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                            t.getPortfolioTransaction().addUnit(grossValue);
                        })

                        .wrap(t -> {
                            if (t.getPortfolioTransaction().getAmount() == 0L)
                                throw new IllegalArgumentException("No amount found");

                            return new BuySellEntryItem(t);
                        }));
    }

    @SuppressWarnings("nls")
    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Verkauf");
        this.addDocumentTyp(type);

        Block block = new Block("^Verkauf .*$", "Dieser Beleg wird .*$");
        type.addBlock(block);
        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });
        block.set(pdfTransaction);

        pdfTransaction.section("isin", "name", "currency") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("^(?<name>.*) (?<isin>[^ ]*)$")
                        .match("STK [\\d.]+(,\\d+)? (?<currency>\\w{3}) ([\\d.]+,\\d+)$")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .find("Nominal Kurs") //
                        .match("^STK (?<shares>[\\d.]+(,\\d+)?) (\\w{3}) ([\\d.]+,\\d+)$")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("amount", "currency").optional() //
                        .find("Wert Konto-Nr. Betrag zu Ihren Gunsten")
                        .match("^(\\d+\\.\\d+\\.\\d{4}+) ([0-9]*) (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("amount", "currency", "exchangeRate", "forex", "forexCurrency").optional() //
                        .find(".* Ausmachender Betrag (?<forexCurrency>\\w{3}) (?<forex>[\\d.]+,\\d+)")
                        .find("Wert Konto-Nr. Devisenkurs Betrag zu Ihren Gunsten")
                        .match("^(\\d+\\.\\d+\\.\\d{4}+) ([0-9]*) .../... (?<exchangeRate>[\\d.]+,\\d+) (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {

                            Money amount = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("amount")));
                            t.setMonetaryAmount(amount);

                            BigDecimal exchangeRate = BigDecimal.ONE.divide( //
                                            asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forex")));

                            Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                            t.getPortfolioTransaction().addUnit(grossValue);
                        })

                        .section("date") //
                        .match("^Handelstag (?<date>\\d+\\.\\d+\\.\\d{4}+) .*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("date", "time").optional() //
                        .match("^Handelstag (?<date>\\d+\\.\\d+\\.\\d{4}+) .*$")
                        .match("^Handelszeit (?<time>\\d+:\\d+).*$").assign((t, v) -> {
                            if (v.get("time") != null)
                            {
                                t.setDate(asDate(v.get("date"), v.get("time")));
                            }
                        })

                        .section("fees", "currency").optional()
                        .match("^.*Provision (?<currency>\\w{3}) (?<fees>[\\d.]+,\\d+)-$").assign((t, v) -> {
                            String currency = asCurrencyCode(v.get("currency"));
                            if (currency.equals(t.getAccountTransaction().getCurrencyCode()))
                            {
                                t.getPortfolioTransaction().addUnit(
                                                new Unit(Unit.Type.FEE, Money.of(currency, asAmount(v.get("fees")))));
                            }
                        })

                        .wrap(t -> {
                            if (t.getPortfolioTransaction().getAmount() == 0L)
                                throw new IllegalArgumentException("No amount found");

                            return new BuySellEntryItem(t);
                        });

        addTaxesSectionsTransaction(type, pdfTransaction);

        Block taxBlock = new Block("zu versteuern \\(negativ\\).*");
        type.addBlock(taxBlock);
        taxBlock.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.TAX_REFUND);
            return t;
        })

                        .section("amount", "currency", "date") //
                        .match("Wert Konto-Nr. Abrechnungs-Nr. Betrag zu Ihren Gunsten")
                        .match("^(?<date>\\d+\\.\\d+\\.\\d{4}+) ([0-9]*) ([0-9]*) (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                        }).wrap(t -> new TransactionItem(t)));

    }

    @SuppressWarnings("nls")
    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Dividende");
        this.addDocumentTyp(type);

        Block block = new Block("^Dividendengutschrift.*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });
        block.set(pdfTransaction);

        pdfTransaction.section("isin", "name", "currency") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("^(?<name>.*) (?<isin>[^ ]*)$") //
                        .match("STK ([\\d.]+(,\\d+)?) (\\d{2}\\.\\d{2}\\.\\d{4}) (\\d{2}\\.\\d{2}\\.\\d{4}) (?<currency>\\w{3}) (\\d+,\\d+)")
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares") //
                        .find("Nominal Ex-Tag Zahltag .*") //
                        .match("^STK (?<shares>[\\d.]+(,\\d+)?) .*$")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .section("date", "amount", "currency") //
                        .optional() //
                        .find("Wert *Konto-Nr. *Betrag *zu *Ihren *Gunsten")
                        .match("^(?<date>\\d{2}\\.\\d{2}\\.\\d{4}) ([0-9]*) (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .section("date", "amount", "currency", "forexCurrency", "exchangeRate") //
                        .optional() //
                        .find("Wert Konto-Nr. Devisenkurs Betrag zu Ihren Gunsten")
                        .match("^(?<date>\\d{2}\\.\\d{2}\\.\\d{4}) ([0-9]*) \\w{3}/(?<forexCurrency>\\w{3}) (?<exchangeRate>[\\d.]+,\\d+) (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate")).setScale(10,
                                            RoundingMode.HALF_DOWN);
                            BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")),
                                            Math.round(t.getAmount() / inverseRate.doubleValue()));
                            Unit unit = new Unit(Unit.Type.GROSS_VALUE, t.getMonetaryAmount(), forex, inverseRate);
                            if (unit.getForex().getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                                t.addUnit(unit);
                        })

                        // this section is needed, if the dividend is payed in
                        // the forex currency to a account in forex curreny but
                        // the security is listed in local currency
                        .section("forex", "localCurrency", "forexCurrency", "exchangeRate") //
                        .optional() //
                        .find("Wert Konto-Nr. Betrag zu Ihren Gunsten")
                        .match("^(\\d{2}.\\d{2}\\.\\d{4}) ([0-9]*) (\\w{3}) (?<forex>[\\d.]+,\\d+)$")
                        .match("Devisenkurs: (?<localCurrency>\\w{3})/(?<forexCurrency>\\w{3}) (?<exchangeRate>[\\d.]+,\\d+)")
                        .assign((t, v) -> {
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate")).setScale(10,
                                            RoundingMode.HALF_DOWN);
                            Money forex = Money.of(asCurrencyCode(v.get("forexCurrency")), asAmount(v.get("forex")));
                            Money localAmount = Money.of(v.get("localCurrency"), Math.round(forex.getAmount()
                                            / Double.parseDouble(v.get("exchangeRate").replace(',', '.'))));
                            t.setAmount(forex.getAmount());
                            t.setCurrencyCode(forex.getCurrencyCode());
                            Unit unit = new Unit(Unit.Type.GROSS_VALUE, forex, localAmount, exchangeRate);
                            if (unit.getForex().getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                                t.addUnit(unit);
                        })

                        // if gross dividend is given in document, we need to
                        // fix the unit, if security currency and transaction
                        // currency differ
                        .section("fxCurrency", "fxAmount", "currency", "exchangeRate") //
                        .optional() //
                        // this line seems to give the gross dividend always in
                        // EUR
                        .match("ausl.ndische Dividende \\w{3} (?<fxAmount>[\\d.]+,\\d+)")
                        .match("Devisenkurs: (?<fxCurrency>\\w{3})/(?<currency>\\w{3}) (?<exchangeRate>[\\d.]+,\\d+)")
                        .assign((t, v) -> {

                            if (!t.getCurrencyCode().equals(t.getSecurity().getCurrencyCode()))
                            {
                                BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));

                                // check, if forex currency is transaction
                                // currency or not and swap amount, if necessary
                                Unit grossValue;
                                if (!asCurrencyCode(v.get("fxCurrency")).equals(t.getCurrencyCode()))
                                {
                                    Money fxAmount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                                    asAmount(v.get("fxAmount")));
                                    long localAmount = exchangeRate.multiply(BigDecimal.valueOf(fxAmount.getAmount()))
                                                    .longValue();
                                    Money amount = Money.of(asCurrencyCode(v.get("currency")), localAmount);
                                    grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, exchangeRate);
                                }
                                else
                                {
                                    Money amount = Money.of(asCurrencyCode(v.get("fxCurrency")),
                                                    asAmount(v.get("fxAmount")));
                                    long forexAmount = exchangeRate.multiply(BigDecimal.valueOf(amount.getAmount()))
                                                    .longValue();
                                    Money fxAmount = Money.of(asCurrencyCode(v.get("currency")), forexAmount);
                                    grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, fxAmount, exchangeRate);
                                }
                                // remove existing unit to replace with new one
                                Optional<Unit> grossUnit = t.getUnit(Unit.Type.GROSS_VALUE);
                                if (grossUnit.isPresent())
                                {
                                    t.removeUnit(grossUnit.get());
                                }
                                t.addUnit(grossValue);
                            }
                        })

                        .wrap(t -> {
                            if (t.getAmount() == 0)
                                throw new IllegalArgumentException("No dividend amount found.");
                            return new TransactionItem(t);
                        });

        addTaxesSectionsTransaction(type, pdfTransaction);
    }

    @SuppressWarnings("nls")
    private void addProceedsTransaction()
    {
        DocumentType type = new DocumentType("Erträgnisgutschrift");
        this.addDocumentTyp(type);

        // Block zweimal vorhanden, finde direkt 2. Block
        Block block = new Block("^Erträgnisgutschrift (?!aus).*$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });
        block.set(pdfTransaction);

        pdfTransaction.section("name", "isin").find("Gattungsbezeichnung ISIN").match("^(?<name>.*) (?<isin>[^ ]*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        .section("shares").find("Nominal Ex-Tag Zahltag .*")
                        .match("^STK (?<shares>[\\d.]+(,\\d+)?) .*$")
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // No exchange rate
                        .section("date", "amount", "currency").optional() //
                        .find("Wert\\s*Konto-Nr.\\s*Betrag\\s*zu\\s*Ihren\\s*Gunsten")
                        .match("^(?<date>\\d+\\.\\d+\\.\\d{4}+)\\s*([0-9]*) (?<currency>\\w{3})\\s*(?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // With exchange rate
                        .section("date", "amount", "currency") //
                        .optional() //
                        .find("Wert\\s*Konto-Nr.\\s*Devisenkurs\\s*Betrag\\s*zu\\s*Ihren\\s*Gunsten")
                        .match("^(?<date>\\d+\\.\\d+\\.\\d{4}+)\\s*([0-9]*)\\s*(\\w{3})\\/(\\w{3})\\s*([\\d.]+(,\\d+)?)\\s*(?<currency>\\w{3})\\s*(?<amount>[\\d.]+,\\d+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> {
                            if (t.getAmount() == 0)
                                throw new IllegalArgumentException("No dividend amount found.");
                            return new TransactionItem(t);
                        });

        addTaxesSectionsTransaction(type, pdfTransaction);
    }

    @SuppressWarnings("nls")
    private <T extends Transaction<?>> void addTaxesSectionsTransaction(DocumentType documentType, T pdfTransaction)
    {
        // if we have a tax return, we set a flag and don't book tax below
        pdfTransaction.section("n").optional() //
                        .match("zu versteuern \\(negativ\\) (?<n>.*)").assign((t, v) -> {
                            documentType.getCurrentContext().put("negative", "X");
                        });

        pdfTransaction.section("exchangeRate", "fxCurrency").optional() //
                        .match("Devisenkurs: (\\w{3})/(?<fxCurrency>\\w{3}) (?<exchangeRate>[\\d.]+,\\d+)")
                        .assign((t, v) -> {

                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            if (getTransaction(t).getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                            {
                                exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                            }
                            documentType.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                        })

                        .section("exchangeRate", "fxCurrency").optional() //
                        .match("^Wert Konto-Nr. Devisenkurs Betrag zu Ihren Gunsten$")
                        .match("\\d{2}\\.\\d{2}\\.\\d{4} \\d+ (\\w{3})/(?<fxCurrency>\\w{3}) (?<exchangeRate>[\\d.]+,\\d+) .*")
                        .assign((t, v) -> {
                            BigDecimal exchangeRate = asExchangeRate(v.get("exchangeRate"));
                            if (getTransaction(t).getCurrencyCode().contentEquals(asCurrencyCode(v.get("fxCurrency"))))
                            {
                                exchangeRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);
                            }
                            documentType.getCurrentContext().put("exchangeRate", exchangeRate.toPlainString());
                        })

                        .section("tax", "currency", "label").optional()
                        .match("^(?<label>.*)US-Quellensteuer.* (?<currency>\\w{3}) (?<tax>[\\d.]+,\\d+)-?$")
                        .assign((t, v) -> {
                            if (!"davon anrechenbare".equals(v.get("label").trim()))
                            {
                                Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                                PDFExtractorUtils.checkAndSetTax(tax, getTransaction(t), documentType);
                            }
                        })

                        .section("tax", "currency", "label").optional()
                        .match("^(?<label>.*) Kapitalertragsteuer (?<currency>\\w{3}) (?<tax>[\\d.]+,\\d+)-?$")
                        .assign((t, v) -> {
                            if (!"X".equals(documentType.getCurrentContext().get("negative"))
                                            && !"im laufenden Jahr einbehaltene".equals(v.get("label")))
                            {
                                Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                                PDFExtractorUtils.checkAndSetTax(tax, getTransaction(t), documentType);
                            }
                        })

                        .section("tax", "currency", "label").optional()
                        .match("^(?<label>.*) Solidaritätszuschlag (?<currency>\\w{3}) (?<tax>[\\d.]+,\\d+)-?$")
                        .assign((t, v) -> {
                            if (!"X".equals(documentType.getCurrentContext().get("negative"))
                                            && !"im laufenden Jahr einbehaltener".equals(v.get("label")))
                            {
                                Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                                PDFExtractorUtils.checkAndSetTax(tax, getTransaction(t), documentType);
                            }
                        })

                        .section("tax", "currency", "label").optional()
                        .match("^(?<label>.*) ?Kirchensteuer (?<currency>\\w{3}) (?<tax>[\\d.]+,\\d+)-?$")
                        .assign((t, v) -> {
                            if (!"X".equals(documentType.getCurrentContext().get("negative"))
                                            && !"im laufenden Jahr einbehaltene".equals(v.get("label")))
                            {
                                Money tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));
                                PDFExtractorUtils.checkAndSetTax(tax, getTransaction(t), documentType);
                            }
                        });
    }

    private name.abuchen.portfolio.model.Transaction getTransaction(Object t)
    {
        if (t instanceof name.abuchen.portfolio.model.Transaction)
        {
            return ((name.abuchen.portfolio.model.Transaction) t);
        }
        else
        {
            return ((name.abuchen.portfolio.model.BuySellEntry) t).getPortfolioTransaction();
        }
    }
}
