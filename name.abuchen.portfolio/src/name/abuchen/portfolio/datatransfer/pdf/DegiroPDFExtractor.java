package name.abuchen.portfolio.datatransfer.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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

public class DegiroPDFExtractor extends AbstractPDFExtractor
{

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.GERMANY); //$NON-NLS-1$
    
    public DegiroPDFExtractor(Client client)
    {
        super(client);
        addBankIdentifier("DEGIRO"); //$NON-NLS-1$
        addBankAccountTransactions();
        addPortfolioTransactions();
    }

    @SuppressWarnings("nls")
    private void addBankAccountTransactions()
    {
        DocumentType type = new DocumentType("Kontoauszug");
        this.addDocumentTyp(type);

        // 02-08-2017 00:00 Einzahlung EUR 350,00 EUR 350,00
        // 01-02-2019 11:44 01-02-2019 Einzahlung EUR 0,01 EUR 0,01
        // 22-02-2019 18:40 22-02-2019 SOFORT Einzahlung EUR 27,00 EUR 44,89
        Block blockDeposit = new Block("^.* Einzahlung .*$");
        type.addBlock(blockDeposit);
        blockDeposit.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.DEPOSIT);
            return t;
        })

                        .section("date", "currency", "amount")
                        .match("(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?(SOFORT )?Einzahlung (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d{2}) .*")
                        .assign((t, v) -> {
                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                t.setDateTime(asDate(v.get("date")));
                                t.setAmount(asAmount(v.get("amount")));
                        })
                        .wrap(t -> new TransactionItem(t)));
        
        // 05-08-2019 00:09 05-08-2019 Auszahlung EUR -1.000,00 EUR 1.445,06
        Block blockRemoval = new Block("^.* Auszahlung .*$");
        type.addBlock(blockRemoval);
        blockRemoval.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.REMOVAL);
            return t;
        })

                        .section("date", "currency", "amount")
                        .match("(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?Auszahlung (?<currency>\\w{3}) -?(?<amount>[\\d.]+,\\d{2}) .*")
                        .assign((t, v) -> {
                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                t.setDateTime(asDate(v.get("date")));
                                t.setAmount(asAmount(v.get("amount")));
                        })
                        .wrap(t -> new TransactionItem(t)));
        
        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividende EUR 2,07 EUR 521,41
        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividendensteuer EUR -0,55 EUR 519,34
        // 17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividende EUR 22,64 EUR 519,89
        Block blockDividendsEur = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ (\\d+-\\d+-\\d{4} )?.*Dividende EUR.*");
        type.addBlock(blockDividendsEur);
        blockDividendsEur.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.DIVIDENDS);
            return t;
        })

                        .section("date", "name", "isin", "currency", "amount")
                        .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?(?<name>.*) (?<isin>\\w{12}+) .*Dividende (?<currency>\\w{3}) -?(?<amount>[\\d.]+,\\d{2}) (\\w{3}).*$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                        })
                        
                        .section("currencyTax", "tax").optional()
                        .match("^(\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?(.*) (\\w{12}+) .*Dividendensteuer\\s(?<currencyTax>\\w{3}) -?(?<tax>[\\d.]+,\\d{2}) .*$")
                        .assign((t, v) -> {
                            t.addUnit(new Unit(Unit.Type.TAX, Money.of(asCurrencyCode(v.get("currencyTax")), asAmount(v.get("tax")))));
                        })
                        
                        .wrap(t -> {
                                return new TransactionItem(t);
                        }));
        
        //31-07-2017 00:00 Zinsen EUR -0,07 EUR -84,16
        Block blockInterest = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ (\\d+-\\d+-\\d{4} )?.*Zinsen .*");
        type.addBlock(blockInterest);
        blockInterest.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.INTEREST_CHARGE);
            return t;
        })

                        .section("date", "currency", "amount")
                        .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?.*Zinsen (?<currency>\\w{3}) -?(?<amount>[\\d.]+,\\d{2}).*$")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(t -> new TransactionItem(t)));
        
        //04-01-2019 20:14 04-01-2019 SOFORT Zahlungsgebühr EUR -1,00 EUR 150,00
        Block blockDepositFee = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ (\\d+-\\d+-\\d{4} )?SOFORT Zahlungsgeb.*hr.*$");
        type.addBlock(blockDepositFee);
        blockDepositFee.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.FEES);
            return t;
        })

                        .section("date", "currency", "amount")
                        .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?SOFORT Zahlungsgeb.*hr (?<currency>\\w{3}) -?(?<amount>[\\d.]+,\\d{2}) .*$")
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                        })
                        
                        .wrap(t -> new TransactionItem(t)));
        
        
        
        // 30-06-2017 00:00 Einrichtung von EUR -2,50 EUR 1.116,79
        // Handelsmodalitäten
        // 2017
        //03-07-2019 11:47 30-06-2019 Einrichtung von Handelsmodalitäten 2019 (New York Stock EUR -0,54 EUR 22,16
        Block blockTrademodalities = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ (\\d+-\\d+-\\d{4} )?.*Einrichtung von .*$");
        type.addBlock(blockTrademodalities);
        blockTrademodalities.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.FEES);
            return t;
        })
                        
                        .oneOf(
                                        section -> section.attributes("date", "currency", "amount").match(
                                        "^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?.*Einrichtung von (?<currency>\\w{3}) -?(?<amount>[\\d.]+,\\d{2}).*$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setDateTime(asDate(v.get("date")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        }),
                                        section -> section.attributes("date", "currency", "amount").match(
                                                        "^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?.*Einrichtung von Handelsmodalit.* (?<currency>\\w{3}) -(?<amount>[\\d.]+,\\d{2}).*$")
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setDateTime(asDate(v.get("date")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                              )

                        .wrap(t -> new TransactionItem(t)));
        
        //22-03-2019 14:10 22-03-2019 ODX4 P11550.00 22MAR19 DE000C2ZNL25 Gebühr für Ausübung/Zuteilung EUR -1,00 EUR 2.204,26
        Block blockFeeStrike = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ (\\d+-\\d+-\\d{4} )?.*Gebühr.* (Ausübung|Zuteilung).*$");
        type.addBlock(blockFeeStrike);
        blockFeeStrike.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.FEES);
            return t;
        })

                        .section("date", "name", "isin", "currency", "amount")
                        .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?(?<name>.*) (?<isin>\\w{12}+) .*Gebühr.* (Ausübung|Zuteilung).*(?<currency>\\w{3}) -?(?<amount>[\\d.]+,\\d{2}) .*$")
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                        })
                        
                        .wrap(t -> new TransactionItem(t)));
        
        // 05-03-2019 15:37 28-02-2019 Rabatt für 500 Euro Aktion EUR 18,00 EUR 1.960,64
        // 05-03-2019 15:37 28-02-2019 Gutschrift für die Neukundenaktion EUR 18,00 EUR 1.960,69
        Block blockFeeReturn = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ (\\d+-\\d+-\\d{4} )?(Rabatt|Gutschrift).*$");
        type.addBlock(blockFeeReturn);
        blockFeeReturn.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.FEES_REFUND);
            return t;
        })

                        .section("date", "currency", "amount")
                        .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?(Rabatt|Gutschrift) .* (?<currency>\\w{3}) (?<amount>[\\d.]+,\\d{2}) .*$")
                        .assign((t, v) -> {
                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                t.setDateTime(asDate(v.get("date")));
                                t.setAmount(asAmount(v.get("amount")));
                        })
                        .wrap(t -> new TransactionItem(t)));
        
        
        //        15-06-2019 06:44 14-06-2019 Währungswechsel (Einbuchung) EUR 0,30 EUR 23,09
        //        15-06-2019 06:44 14-06-2019 Währungswechsel (Ausbuchung) 1,1219 USD -0,34 USD 0,00
        //        ....
        //        14-06-2019 07:55 14-06-2019 THE KRAFT HEINZ COMPAN US5007541064 Dividende USD 0,40 USD 0,34
        //        14-06-2019 07:55 14-06-2019 THE KRAFT HEINZ COMPAN US5007541064 Dividendensteuer USD -0,06 USD -0,06
        
        //        19-07-2017 00:00 Währungswechsel USD -0,86 USD 0,00
        //        (Ausbuchung)
        //        19-07-2017 00:00 Währungswechsel 1,1528 EUR 0,75 EUR 1.783,89
        //        (Einbuchung)
        //        ....
        //        17-07-2017 00:00 IS.DJ U.S.SELEC.DIV.U.ETF DE000A0D8Q49 Dividende USD 0,86 USD 0,86
        
        //        17-05-2019 06:53 16-05-2019 Währungswechsel (Einbuchung) EUR 0,58 EUR 11,49
        //        17-05-2019 06:53 16-05-2019 Währungswechsel (Ausbuchung) 1,1186 USD -0,65 USD 0,00
        //        16-05-2019 08:21 16-05-2019 APPLE INC. - COMMON ST US0378331005 Dividende USD 0,77 USD 0,65
        //        16-05-2019 08:21 16-05-2019 APPLE INC. - COMMON ST US0378331005 Dividendensteuer USD -0,12 USD -0,12
        
        //        06-06-2019 09:00 05-06-2019 SONY CORPORATION COMMO US8356993076 ADR/GDR Weitergabegebühr USD -0,04 USD -0,04
        Block blockDividendForex = new Block("^(\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?Währungswechsel (\\(Einbuchung\\)|[.\\d]+,\\d+).*");
        type.addBlock(blockDividendForex);
        blockDividendForex.set(new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction t = new AccountTransaction();
            t.setType(AccountTransaction.Type.DIVIDENDS);
            return t;
        })
                        .oneOf(
                                        section -> section.attributes("currency", "amount")
                                        .match(".* Währungswechsel \\(Einbuchung\\) ?(?<currency>\\w{3}) (?<amount>[.\\d]+,\\d+) .*")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                        , 
                                        section -> section.attributes("exchangeRate", "currency", "amount")
                                        .match(".* Währungswechsel (?<exchangeRate>[.\\d]+,\\d+) (?<currency>\\w{3}) (?<amount>[.\\d]+,\\d+).*")
                                        .assign((t, v) -> {
                                            type.getCurrentContext().put("exchangeRate", v.get("exchangeRate"));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                        )

                        .section("exchangeRate", "currencyFx").optional()
                        .match(".* Währungswechsel \\(Ausbuchung\\) ?(?<exchangeRate>[.\\d]+,\\d+) (?<currencyFx>\\w{3}).*")
                        .assign((t, v) -> {
                            type.getCurrentContext().put("exchangeRate", v.get("exchangeRate"));
                            type.getCurrentContext().put("currencyFx", v.get("currencyFx"));
                        })
                        
                        .section("date", "name", "isin", "currencyFx", "amountFx")
                        .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?(?<name>.*) (?<isin>\\w{12}+) .*Dividende (?<currencyFx>\\w{3}) -?(?<amountFx>[\\d.]+,\\d{2}) (\\w{3}).*$")
                        .assign((t, v) -> {
                            
                            Map<String, String> context = type.getCurrentContext();

                            String currencyCodeFx = context.get("currencyFx");
                            if (currencyCodeFx == null)
                            {
                                currencyCodeFx = asCurrencyCode(v.get("currencyFx"));
                            }
                            else
                            {
                                currencyCodeFx = asCurrencyCode(currencyCodeFx);
                            }

                            BigDecimal exchangeRate = asExchangeRate(context.get("exchangeRate"));
                            BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10, RoundingMode.HALF_DOWN);

                            long partialAmountDividend = inverseRate
                                            .multiply(BigDecimal.valueOf(asAmount(v.get("amountFx"))))
                                            .setScale(0, RoundingMode.HALF_DOWN).longValue();

                            t.addUnit(new Unit(Unit.Type.GROSS_VALUE,
                                            Money.of(t.getCurrencyCode(), partialAmountDividend),
                                            Money.of(currencyCodeFx, asAmount(v.get("amountFx"))), inverseRate));

                            Security security = getOrCreateSecurity(v);
                            security.setCurrencyCode(currencyCodeFx);
                            t.setSecurity(security);
                            t.setDateTime(asDate(v.get("date")));
                        })

                        //        14-06-2019 07:55 14-06-2019 THE KRAFT HEINZ COMPAN US5007541064 Dividendensteuer USD -0,06 USD -0,06
                        // nicht  17-07-2017 00:00 ISH.S.EU.SEL.DIV.30 U.ETF DE0002635299 Dividendensteuer EUR -0,55 EUR 519,34
                        .section("date", "isin", "currencyTax", "taxFx").optional()
                        .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?(.*) (?<isin>\\w{12}+) .*Dividendensteuer (?<currencyTax>\\w{3}) -?(?<taxFx>[\\d.]+,\\d{2}) .*$")
                        .assign((t, v) -> {
                            if (v.get("isin").equalsIgnoreCase(t.getSecurity().getIsin()))
                            {
                                Map<String, String> context = type.getCurrentContext();

                                BigDecimal exchangeRate = asExchangeRate(context.get("exchangeRate"));
                                BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                                RoundingMode.HALF_DOWN);

                                String currencyCodeFx = asCurrencyCode(v.get("currencyTax"));

                                Money mTaxesFx = Money.of(currencyCodeFx, asAmount(v.get("taxFx")));

                                long taxesFxInEUR = BigDecimal.valueOf(mTaxesFx.getAmount())
                                                .divide(exchangeRate, 10, RoundingMode.HALF_DOWN)
                                                .setScale(0, RoundingMode.HALF_DOWN).longValue();

                                t.addUnit(new Unit(Unit.Type.TAX, Money.of(t.getCurrencyCode(), taxesFxInEUR), mTaxesFx,
                                                inverseRate));
                            }
                        })
                        
                        //        06-06-2019 09:00 05-06-2019 SONY CORPORATION COMMO US8356993076 ADR/GDR Weitergabegebühr USD -0,04 USD -0,04
                        .section("date", "currencyFee", "feeFx").optional()
                        .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (\\d+-\\d+-\\d{4} )?(.*) (\\w{12}+) ADR/GDR Weitergabegebühr (?<currencyFee>\\w{3}) -?(?<feeFx>[\\d.]+,\\d{2}) .*$")
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();

                            BigDecimal exchangeRate = asExchangeRate(context.get("exchangeRate"));
                            BigDecimal inverseRate = BigDecimal.ONE.divide(exchangeRate, 10,
                                            RoundingMode.HALF_DOWN);
                            
                            String currencyCodeFx = asCurrencyCode(v.get("currencyFee"));

                            Money mFeesFx = Money.of(currencyCodeFx, asAmount(v.get("feeFx")));
                            
                            long feesFxInEUR = BigDecimal.valueOf(mFeesFx.getAmount())
                                            .divide(exchangeRate, 10, RoundingMode.HALF_DOWN)
                                            .setScale(0, RoundingMode.HALF_DOWN).longValue();
                            
                            t.addUnit(new Unit(Unit.Type.FEE, Money.of(t.getCurrencyCode(), feesFxInEUR), mFeesFx, inverseRate));
                        })

                        .wrap(t -> {

                            // check if there is a delta between the gross
                            // amount and the sum of fees and taxs

                            Optional<Unit> grossValue = t.getUnit(Unit.Type.GROSS_VALUE);
                            if (grossValue.isPresent())
                            {
                                long net = t.getAmount();
                                long gross = grossValue.get().getAmount().getAmount();

                                long feesAndTaxes = t.getUnits().filter(
                                                u -> u.getType() == Unit.Type.TAX || u.getType() == Unit.Type.FEE)
                                                .mapToLong(u -> u.getAmount().getAmount()).sum();

                                long delta = gross - feesAndTaxes - net;

                                if (delta != 0)
                                {
                                    // pick the first unit and make it fit; see
                                    // discussion
                                    // https://github.com/buchen/portfolio/pull/1198

                                    Unit unit = t.getUnits().filter(
                                                    u -> u.getType() == Unit.Type.TAX || u.getType() == Unit.Type.FEE)
                                                    .filter(u -> u.getExchangeRate() != null)
                                                    .findFirst().orElseThrow(IllegalArgumentException::new);

                                    t.removeUnit(unit);

                                    long amountPlusDelta = unit.getAmount().getAmount() + delta;
                                    long forexPlusDelta = BigDecimal.ONE
                                                    .divide(unit.getExchangeRate(), 10, RoundingMode.HALF_DOWN)
                                                    .multiply(BigDecimal.valueOf(amountPlusDelta))
                                                    .setScale(0, RoundingMode.HALF_DOWN).longValue();

                                    Unit newUnit = new Unit(unit.getType(),
                                                    Money.of(unit.getAmount().getCurrencyCode(),
                                                                    amountPlusDelta),
                                                    Money.of(unit.getForex().getCurrencyCode(),
                                                                    forexPlusDelta),
                                                    unit.getExchangeRate());

                                    t.addUnit(newUnit);
                                }
                            }

                            return new TransactionItem(t);
                        }));
        
    }
    
    
    @SuppressWarnings("nls")
    private void addPortfolioTransactions()
    {
        DocumentType type = new DocumentType("Transaktionsübersicht");
        this.addDocumentTyp(type);
        
        DocumentType typeNL = new DocumentType("Transacties");
        this.addDocumentTyp(typeNL);

        Block blockBuy = new Block("^\\d+-\\d+-\\d{4} \\d+:\\d+ .* \\w{12}+ .* \\w{3}+ .*[.\\d]+,\\d{2}$");
        type.addBlock(blockBuy);
        typeNL.addBlock(blockBuy);

        blockBuy.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .oneOf(
                        // @formatter:off

                            // 08-02-2019 13:27 ODX2 C11000.00 08FEB19 DE000C25KFE8 ERX -3 EUR 0,00 EUR 0,00 EUR 0,00 EUR 0,00
                            // 09-07-2019 14:08 VANGUARD FTSE AW IE00B3RBWM25 EAM 3 EUR 77,10 EUR -231,30 EUR -231,30 EUR -231,30

                            section -> section
                                .attributes("date", "name", "isin", "shares", "currency", "amount")
                                .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (?<name>.*) (?<isin>\\w{12}+) \\w{3} (?<shares>[-]?[.\\d]+[,\\d]*)"
                                                // quote
                                                + " \\w{3} [-.,\\d]*"
                                                 // gross value transaction currency
                                                + " \\w{3} [-.,\\d]*"
                                                // gross value account currency
                                                + " \\w{3} [-.,\\d]*"
                                                // total amount
                                                + " (?<currency>\\w{3}) -?(?<amount>[.,\\d]*)$")
                                .assign((t, v) -> {
                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    if (v.get("shares").startsWith("-"))
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(
                                                        v.get("shares").replaceFirst("-", "")));
                                    }
                                    else
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }
                                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                    t.setAmount(asAmount(v.get("amount")));
                            }),

                            // 07-02-2019 12:22 ODX2 C11200.00 08FEB19 DE000C25KFN9 ERX 1 EUR 30,00 EUR -150,00 EUR -150,00 EUR -0,90 EUR -150,90
                            // 01-04-2019 15:35 DEUTSCHE BANK AG NA O.N DE0005140008 XET -136 EUR 7,45 EUR 1.013,20 EUR 1.013,20 EUR -2,26 EUR 1.010,94
                            // 01-04-2019 12:20 DEUTSCHE BANK AG NA O.N DE0005140008 XET 18 EUR 7,353 EUR -132,35 EUR -132,35 EUR -0,03 EUR -132,38

                            section -> section
                                .attributes("date", "name", "isin", "shares", "currencyFee", "fee", "currency", "amount")
                                .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (?<name>.*) (?<isin>\\w{12}+) \\w{3} (?<shares>[-]?[.\\d]+[,\\d]*)"
                                                // quote
                                                + " \\w{3} [-.,\\d]*"
                                                // gross value transaction currency
                                                + " \\w{3} [-.,\\d]*"
                                                // gross value local currency
                                                + " \\w{3} [-.,\\d]*"
                                                // fees
                                                + " (?<currencyFee>\\w{3}) -?(?<fee>[.,\\d]*)"
                                                // total amount
                                                + " (?<currency>\\w{3}) -?(?<amount>[.,\\d]*)$")
                                .assign((t, v) -> {
                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    if (v.get("shares").startsWith("-")) 
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    } 
                                    else 
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }
                                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                    t.setAmount(asAmount(v.get("amount")));  
                                    Money feeAmount = Money.of(asCurrencyCode(v.get("currencyFee")), asAmount(v.get("fee")));
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feeAmount));
                            }),

                            //26-04-2019 17:52 TESLA MOTORS INC. - C US88160R1014 NDQ 2 USD 240,00 USD -480,00 EUR -430,69 1,1145 EUR -0,51 EUR -431,20
                            //29-04-2019 16:11 TESLA MOTORS INC. - C US88160R1014 NDQ -3 USD 240,00 USD 720,00 EUR 645,04 1,1162 EUR -0,51 EUR 644,53

                            section -> section.attributes("date", "name", "isin", "shares", "currency", "amountFx", "exchangeRate", "currencyFee", "fee", "currencyAccount", "amount")
                            .match("^(?<date>\\d+-\\d+-\\d{4} \\d+:\\d+) (?<name>.*) (?<isin>\\w{12}+) \\w{3} (?<shares>[-]?[.\\d]+[,\\d]*)"
                                            + " \\w{3} -?[.\\d]+,\\d{2}"
                                            + " (?<currency>\\w{3}) -?(?<amountFx>[.\\d]+,\\d{2}).*"
                                            + " \\w{3} -?[.\\d]+,\\d{2}"
                                            + " (?<exchangeRate>[.\\d]+,\\d{1,6})"
                                            + " (?<currencyFee>\\w{3}) (?<fee>-?[.\\d]+,\\d{2})"
                                            + " (?<currencyAccount>\\w{3}) -?(?<amount>[.\\d]+,\\d{2})$")
                            .assign((t, v) -> {
                                    t.setSecurity(getOrCreateSecurity(v));
                                    t.setDate(asDate(v.get("date")));
                                    if (v.get("shares").startsWith("-")) 
                                    {
                                        t.setType(PortfolioTransaction.Type.SELL);
                                        t.setShares(asShares(v.get("shares").replaceFirst("-", "")));
                                    } 
                                    else 
                                    {
                                        t.setShares(asShares(v.get("shares")));
                                    }
                                    t.setCurrencyCode(asCurrencyCode(v.get("currencyAccount")));
                                    t.setAmount(asAmount(v.get("amount")));  
                                    Money feeAmount = Money.of(asCurrencyCode(v.get("currencyFee")), asAmount(v.get("fee")));
                                    t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, feeAmount));
                                    
                                    long amountFx = asAmount(v.get("amountFx"));
                                    String currencyFx = asCurrencyCode(v.get("currency"));

                                    if (currencyFx.equals(t.getPortfolioTransaction().getSecurity().getCurrencyCode()))
                                    {
                                        Money amount = Money.of(asCurrencyCode(v.get("currencyAccount")), asAmount(v.get("amount")));
                                        if (t.getPortfolioTransaction().getType() == PortfolioTransaction.Type.BUY)
                                        {
                                            amount = amount.subtract(feeAmount);
                                        }
                                        BigDecimal exchangeRate = BigDecimal.ONE.divide(asExchangeRate(v.get("exchangeRate")), 10, RoundingMode.HALF_DOWN);
                                        Money forex = Money.of(asCurrencyCode(v.get("currency")), amountFx);
                                        Unit grossValue = new Unit(Unit.Type.GROSS_VALUE, amount, forex, exchangeRate);
                                        t.getPortfolioTransaction().addUnit(grossValue);
                                    }
                            })

                            // @formatter:on
                        )

                        .wrap(t -> new BuySellEntryItem(t)));
    }
    
    @Override
    LocalDateTime asDate(String value)
    {
        return LocalDateTime.parse(value, DATE_FORMAT);
    }

    @Override
    public String getLabel()
    {
        return "DEGIRO"; //$NON-NLS-1$
    }
}
