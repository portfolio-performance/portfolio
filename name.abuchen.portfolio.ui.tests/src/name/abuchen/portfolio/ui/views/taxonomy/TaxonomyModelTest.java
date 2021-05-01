package name.abuchen.portfolio.ui.views.taxonomy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import name.abuchen.portfolio.math.Rebalancer;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class TaxonomyModelTest
{
    private static final Security A;
    static
    {
        A = new Security();
        A.setLatest(new LatestSecurityPrice(LocalDate.now(), 100L * Values.Quote.factorToMoney()));
    }
    private static final Security B;
    static
    {
        B = new Security();
        B.setLatest(new LatestSecurityPrice(LocalDate.now(), 100L * Values.Quote.factorToMoney()));
    }
    private static final Security C;
    static
    {
        C = new Security();
        C.setLatest(new LatestSecurityPrice(LocalDate.now(), 100L * Values.Quote.factorToMoney()));
    }

    private static final String CURRENCY_UNIT = CurrencyUnit.EUR;
    private static final Money AMOUNT_0 = Money.of(CURRENCY_UNIT, 0);

    // SIMPLE_TAXONOMY
    // @formatter:off
    // - Root (weight: 100 %)
    //   - Classification X (50 %)
    //     - Security A (100 %)
    //   - Classification Y (50 %)
    //     - Security B (100 %)
    // @formatter:on
    private static final Taxonomy SIMPLE_TAXONOMY;
    static
    {
        SIMPLE_TAXONOMY = new Taxonomy();
        Classification root = new Classification();
        SIMPLE_TAXONOMY.setRootNode(root);
        root.setWeight(Classification.ONE_HUNDRED_PERCENT);

        Classification x = new Classification();
        root.addChild(x);
        x.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        Assignment a = new Assignment(A, Classification.ONE_HUNDRED_PERCENT);
        x.addAssignment(a);

        Classification y = new Classification();
        root.addChild(y);
        y.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        Assignment b = new Assignment(B, Classification.ONE_HUNDRED_PERCENT);
        y.addAssignment(b);
    }

    // MIXED_TAXONOMY
    // @formatter:off
    // - Root (weight: 100 %)
    //   - Classification X (50 %)
    //     - Security A (90 %)
    //     - Security B (10 %)
    //   - Classification Y (50 %)
    //     - Security A (10 %)
    //     - Security B (90 %)
    // @formatter:on
    private static final Taxonomy MIXED_TAXONOMY;
    static
    {
        MIXED_TAXONOMY = new Taxonomy();
        Classification root1 = new Classification();
        MIXED_TAXONOMY.setRootNode(root1);
        root1.setWeight(Classification.ONE_HUNDRED_PERCENT);

        Classification x1 = new Classification();
        root1.addChild(x1);
        x1.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        Assignment a1 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT * 9 / 10);
        x1.addAssignment(a1);
        Assignment b1 = new Assignment(B, Classification.ONE_HUNDRED_PERCENT * 1 / 10);
        x1.addAssignment(b1);

        Classification y1 = new Classification();
        root1.addChild(y1);
        y1.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        Assignment a2 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT * 1 / 10);
        y1.addAssignment(a2);
        Assignment b2 = new Assignment(B, Classification.ONE_HUNDRED_PERCENT * 9 / 10);
        y1.addAssignment(b2);
    }

    // HALF_HALF_TAXONOMY
    // @formatter:off
    // - Root (weight: 100 %)
    //   - Classification X (50 %)
    //     - Security A (50 %)
    //     - Security B (50 %)
    //   - Classification Y (50 %)
    //     - Security A (50 %)
    //     - Security B (50 %)
    // @formatter:on
    private static final Taxonomy HALF_HALF_TAXONOMY;
    static
    {
        HALF_HALF_TAXONOMY = new Taxonomy();
        Classification root1 = new Classification();
        HALF_HALF_TAXONOMY.setRootNode(root1);
        root1.setWeight(Classification.ONE_HUNDRED_PERCENT);

        Classification x1 = new Classification();
        root1.addChild(x1);
        x1.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        Assignment a1 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 2);
        x1.addAssignment(a1);
        Assignment b1 = new Assignment(B, Classification.ONE_HUNDRED_PERCENT / 2);
        x1.addAssignment(b1);

        Classification y1 = new Classification();
        root1.addChild(y1);
        y1.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        Assignment a2 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 2);
        y1.addAssignment(a2);
        Assignment b2 = new Assignment(B, Classification.ONE_HUNDRED_PERCENT / 2);
        y1.addAssignment(b2);
    }

    // MIXED_CLASSIFICATION_TAXONOMY
    // @formatter:off
    // - Root (weight: 100 %)
    //   - Classification X (50 %)
    //     - Security A (50 %)
    //     - Classification XY (100 %)
    //       - Security B (100 %)
    //   - Classification Y (50 %)
    //     - Security A (50 %)
    // @formatter:on
    private static final Taxonomy MIXED_CLASSIFICATION_TAXONOMY;
    static
    {
        MIXED_CLASSIFICATION_TAXONOMY = new Taxonomy();
        Classification root1 = new Classification();
        MIXED_CLASSIFICATION_TAXONOMY.setRootNode(root1);
        root1.setWeight(Classification.ONE_HUNDRED_PERCENT);

        Classification x1 = new Classification();
        root1.addChild(x1);
        x1.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        Assignment a1 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 2);
        x1.addAssignment(a1);

        Classification xy = new Classification();
        x1.addChild(xy);
        xy.setWeight(Classification.ONE_HUNDRED_PERCENT);

        Assignment b = new Assignment(B, Classification.ONE_HUNDRED_PERCENT);
        xy.addAssignment(b);

        Classification y1 = new Classification();
        root1.addChild(y1);
        y1.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        Assignment a2 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 2);
        y1.addAssignment(a2);
    }

    // MIXED_CLASSIFICATION_TAXONOMY2
    // @formatter:off
    // - Root (weight: 100 %)
    //   - Classification X (50 %)
    //     - Security A (50 %)
    //     - Security B (50 %)
    //     - Classification XY (100 %)
    //       - Security B (50 %)
    //   - Classification Y (50 %)
    //     - Security A (50 %)
    // @formatter:on
    private static final Taxonomy MIXED_CLASSIFICATION_TAXONOMY2;
    static
    {
        MIXED_CLASSIFICATION_TAXONOMY2 = new Taxonomy();
        Classification root1 = new Classification();
        MIXED_CLASSIFICATION_TAXONOMY2.setRootNode(root1);
        root1.setWeight(Classification.ONE_HUNDRED_PERCENT);

        Classification x1 = new Classification();
        root1.addChild(x1);
        x1.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        Assignment a1 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 2);
        x1.addAssignment(a1);
        Assignment b1 = new Assignment(B, Classification.ONE_HUNDRED_PERCENT / 2);
        x1.addAssignment(b1);

        Classification xy = new Classification();
        x1.addChild(xy);
        xy.setWeight(Classification.ONE_HUNDRED_PERCENT);

        Assignment b = new Assignment(B, Classification.ONE_HUNDRED_PERCENT / 2);
        xy.addAssignment(b);

        Classification y1 = new Classification();
        root1.addChild(y1);
        y1.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        Assignment a2 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 2);
        y1.addAssignment(a2);
    }

    // MIXED_CLASSIFICATION_TAXONOMY3
    // @formatter:off
    // - Root (weight: 100 %)
    //   - Classification X (75 %)
    //     - Security A (50 %)
    //     - Security B (50 %)
    //     - Classification XY (50 %)
    //       - Security B (50 %)
    //     - Classification XZ (50 %)
    //       - Security C (100 %)
    //   - Classification Y (25 %)
    //     - Security A (50 %)
    // @formatter:on
    private static final Taxonomy MIXED_CLASSIFICATION_TAXONOMY3;
    static
    {
        MIXED_CLASSIFICATION_TAXONOMY3 = new Taxonomy();
        Classification root1 = new Classification();
        MIXED_CLASSIFICATION_TAXONOMY3.setRootNode(root1);
        root1.setWeight(Classification.ONE_HUNDRED_PERCENT);

        Classification x1 = new Classification();
        root1.addChild(x1);
        x1.setWeight(Classification.ONE_HUNDRED_PERCENT * 3 / 4);

        Assignment a1 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 2);
        x1.addAssignment(a1);
        Assignment b1 = new Assignment(B, Classification.ONE_HUNDRED_PERCENT / 2);
        x1.addAssignment(b1);

        Classification xy = new Classification();
        x1.addChild(xy);
        xy.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        Assignment b = new Assignment(B, Classification.ONE_HUNDRED_PERCENT / 2);
        xy.addAssignment(b);

        Classification xz = new Classification();
        x1.addChild(xz);
        xz.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        Assignment c = new Assignment(C, Classification.ONE_HUNDRED_PERCENT);
        xz.addAssignment(c);

        Classification y1 = new Classification();
        root1.addChild(y1);
        y1.setWeight(Classification.ONE_HUNDRED_PERCENT / 4);

        Assignment a2 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 2);
        y1.addAssignment(a2);
    }

    // MIXED_CLASSIFICATION_TAXONOMY4
    // @formatter:off
    // - Root (weight: 100 %)
    //   - Security A (50 %)
    //   - Security B (50 %)
    //   - Classification X (75 %)
    //     - Classification XY (50 %)
    //       - Security B (50 %)
    //     - Classification XZ (50 %)
    //       - Security C (100 %)
    //   - Classification Y (25 %)
    //     - Security A (50 %)
    // @formatter:on
    // Rebalancing solution: 4/13*A, 6/13*B, 3/13*C
    private static final Taxonomy MIXED_CLASSIFICATION_TAXONOMY4;
    static
    {
        MIXED_CLASSIFICATION_TAXONOMY4 = new Taxonomy();
        Classification root1 = new Classification();
        MIXED_CLASSIFICATION_TAXONOMY4.setRootNode(root1);
        root1.setWeight(Classification.ONE_HUNDRED_PERCENT);

        Assignment a1 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 2);
        root1.addAssignment(a1);
        Assignment b1 = new Assignment(B, Classification.ONE_HUNDRED_PERCENT / 2);
        root1.addAssignment(b1);

        Classification x1 = new Classification();
        root1.addChild(x1);
        x1.setWeight(Classification.ONE_HUNDRED_PERCENT * 3 / 4);

        Classification xy = new Classification();
        x1.addChild(xy);
        xy.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        Assignment b = new Assignment(B, Classification.ONE_HUNDRED_PERCENT / 2);
        xy.addAssignment(b);

        Classification xz = new Classification();
        x1.addChild(xz);
        xz.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        Assignment c = new Assignment(C, Classification.ONE_HUNDRED_PERCENT);
        xz.addAssignment(c);

        Classification y1 = new Classification();
        root1.addChild(y1);
        y1.setWeight(Classification.ONE_HUNDRED_PERCENT / 4);

        Assignment a2 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 2);
        y1.addAssignment(a2);
    }

    // ABC_IN_ROOT_TAXONOMY
    // @formatter:off
    // - Root (weight: 100 %)
    //   - Classification X (50 %)
    //     - Security A (50 %)
    //     - Security B (50 %)
    //     - Classification XY (50 %)
    //       - Security B (50 %)
    //     - Classification XZ (50 %)
    //       - Security C (100 %)
    //   - Classification Y (50 %)
    //     - Security A (50 %)
    // @formatter:on
    private static final Taxonomy ABC_IN_ROOT_TAXONOMY;
    static
    {
        ABC_IN_ROOT_TAXONOMY = new Taxonomy();
        Classification root1 = new Classification();
        ABC_IN_ROOT_TAXONOMY.setRootNode(root1);
        root1.setWeight(Classification.ONE_HUNDRED_PERCENT);

        Assignment a = new Assignment(A, Classification.ONE_HUNDRED_PERCENT);
        root1.addAssignment(a);
        Assignment b = new Assignment(B, Classification.ONE_HUNDRED_PERCENT);
        root1.addAssignment(b);
        Assignment c = new Assignment(C, Classification.ONE_HUNDRED_PERCENT);
        root1.addAssignment(c);
    }

    // MIXED_AMBIGUOUS_TAXONOMY
    // @formatter:off
    // - Root (weight: 100 %)
    //   - Classification X (50 %)
    //     - Security A (25 %)
    //     - Security B (50 %)
    //     - Security C (75 %)
    //   - Classification Y (50 %)
    //     - Security A (75 %)
    //     - Security B (50 %)
    //     - Security C (25 %)
    // @formatter:on
    // Rebalancing: Any solution with A = C works.
    private static final Taxonomy MIXED_AMBIGUOUS_TAXONOMY;
    static
    {
        MIXED_AMBIGUOUS_TAXONOMY = new Taxonomy();
        Classification root1 = new Classification();
        MIXED_AMBIGUOUS_TAXONOMY.setRootNode(root1);
        root1.setWeight(Classification.ONE_HUNDRED_PERCENT);
        
        Classification x = new Classification();
        root1.addChild(x);
        x.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        Assignment a = new Assignment(A, Classification.ONE_HUNDRED_PERCENT * 1 / 4);
        x.addAssignment(a);
        Assignment b = new Assignment(B, Classification.ONE_HUNDRED_PERCENT * 2 / 4);
        x.addAssignment(b);
        Assignment c = new Assignment(C, Classification.ONE_HUNDRED_PERCENT * 3 / 4);
        x.addAssignment(c);
        
        Classification y = new Classification();
        root1.addChild(y);
        y.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        Assignment a2 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT * 3 / 4);
        y.addAssignment(a2);
        Assignment b2 = new Assignment(B, Classification.ONE_HUNDRED_PERCENT * 2 / 4);
        y.addAssignment(b2);
        Assignment c2 = new Assignment(C, Classification.ONE_HUNDRED_PERCENT * 1 / 4);
        y.addAssignment(c2);
    }

    // MIXED_AMBIGUOUS_TAXONOMY
    // @formatter:off
    // - Root (weight: 100 %)
    //   - Security A (100 %)
    //   - Classification X (100 %)
    //     - Security B (100 %)
    // @formatter:on
    private static final Taxonomy MIXED_CLASSIFICATION_AMBIGUOUS_TAXONOMY;
    static
    {
        MIXED_CLASSIFICATION_AMBIGUOUS_TAXONOMY = new Taxonomy();
        Classification root1 = new Classification();
        MIXED_CLASSIFICATION_AMBIGUOUS_TAXONOMY.setRootNode(root1);
        root1.setWeight(Classification.ONE_HUNDRED_PERCENT);
        
        Assignment a = new Assignment(A, Classification.ONE_HUNDRED_PERCENT);
        root1.addAssignment(a);
        
        Classification x = new Classification();
        root1.addChild(x);
        x.setWeight(Classification.ONE_HUNDRED_PERCENT);

        Assignment b = new Assignment(B, Classification.ONE_HUNDRED_PERCENT);
        x.addAssignment(b);
    }

    // MIXED_AMBIGUOUS_TAXONOMY
    // @formatter:off
    // - Root (weight: 100 %)
    //   - Classification X (50 %)
    //   - Security A (100 %)
    //     - Classification XY (100 %)
    //       - Security B (100 %)
    //   - Classification Y (50 %)
    //     - Security C (100 %)
    // 
    // @formatter:on
    private static final Taxonomy MIXED_CLASSIFICATION_PARTIALLY_AMBIGUOUS_TAXONOMY;
    static
    {
        MIXED_CLASSIFICATION_PARTIALLY_AMBIGUOUS_TAXONOMY = new Taxonomy();
        Classification root1 = new Classification();
        MIXED_CLASSIFICATION_PARTIALLY_AMBIGUOUS_TAXONOMY.setRootNode(root1);
        root1.setWeight(Classification.ONE_HUNDRED_PERCENT);
        
        Classification x = new Classification();
        root1.addChild(x);
        x.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);
        
        Assignment a = new Assignment(A, Classification.ONE_HUNDRED_PERCENT);
        x.addAssignment(a);
        
        Classification xy = new Classification();
        x.addChild(xy);
        xy.setWeight(Classification.ONE_HUNDRED_PERCENT);

        Assignment b = new Assignment(B, Classification.ONE_HUNDRED_PERCENT);
        xy.addAssignment(b);
        
        Classification y = new Classification();
        root1.addChild(y);
        y.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);
        
        Assignment c = new Assignment(C, Classification.ONE_HUNDRED_PERCENT);
        y.addAssignment(c);
    }
    

    // SIMPLE_OVERDETERMINED_TAXONOMY
    // @formatter:off
    // - Root (weight: 100 %)
    //   - Classification X (25 %)
    //     - Security A (50 %)
    //   - Classification Z (75 %)
    //     - Security A (50 %)
    // @formatter:on
    private static final Taxonomy SIMPLE_OVERDETERMINED_TAXONOMY;
    static
    {
        SIMPLE_OVERDETERMINED_TAXONOMY = new Taxonomy();
        Classification root = new Classification();
        SIMPLE_OVERDETERMINED_TAXONOMY.setRootNode(root);
        root.setWeight(Classification.ONE_HUNDRED_PERCENT);

        Classification x = new Classification();
        root.addChild(x);
        x.setWeight(Classification.ONE_HUNDRED_PERCENT / 4);

        Assignment a = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 2);
        x.addAssignment(a);

        Classification z = new Classification();
        root.addChild(z);
        z.setWeight(Classification.ONE_HUNDRED_PERCENT * 3 / 4);

        Assignment a2 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 2);
        z.addAssignment(a2);
    }
    

    // SIMPLE_PARTIALLY_OVERDETERMINED_TAXONOMY
    // @formatter:off
    // - Root (weight: 100 %)
    //   - Classification X (25 %)
    //     - Security A (50 %)
    //   - Classification Y (25 %)
    //     - Security B (100 %)
    //   - Classification Z (50 %)
    //     - Security A (50 %)
    // @formatter:on
    private static final Taxonomy SIMPLE_PARTIALLY_OVERDETERMINED_TAXONOMY;
    static
    {
        SIMPLE_PARTIALLY_OVERDETERMINED_TAXONOMY = new Taxonomy();
        Classification root = new Classification();
        SIMPLE_PARTIALLY_OVERDETERMINED_TAXONOMY.setRootNode(root);
        root.setWeight(Classification.ONE_HUNDRED_PERCENT);

        Classification x = new Classification();
        root.addChild(x);
        x.setWeight(Classification.ONE_HUNDRED_PERCENT / 4);

        Assignment a = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 2);
        x.addAssignment(a);

        Classification y = new Classification();
        root.addChild(y);
        y.setWeight(Classification.ONE_HUNDRED_PERCENT / 4);

        Assignment b = new Assignment(B, Classification.ONE_HUNDRED_PERCENT);
        y.addAssignment(b);

        Classification z = new Classification();
        root.addChild(z);
        z.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        Assignment a2 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 2);
        z.addAssignment(a2);
    }

    // MIXED_OVERDETERMINED_TAXONOMY
    // @formatter:off
    // - Root (weight: 100 %)
    //   - Classification X (25 %)
    //     - Security A (50 %)
    //     - Security B (10 %)
    //   - Classification Y (25 %)
    //     - Security A (25 %)
    //     - Security B (10 %)
    //   - Classification Z (50 %)
    //     - Security A (25 %)
    //     - Security B (80 %)
    // @formatter:on
    private static final Taxonomy MIXED_OVERDETERMINED_TAXONOMY;
    static
    {
        MIXED_OVERDETERMINED_TAXONOMY = new Taxonomy();
        Classification root1 = new Classification();
        MIXED_OVERDETERMINED_TAXONOMY.setRootNode(root1);
        root1.setWeight(Classification.ONE_HUNDRED_PERCENT);

        Classification x1 = new Classification();
        root1.addChild(x1);
        x1.setWeight(Classification.ONE_HUNDRED_PERCENT / 4);

        Assignment a1 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 2);
        x1.addAssignment(a1);
        Assignment b1 = new Assignment(B, Classification.ONE_HUNDRED_PERCENT * 1 / 10);
        x1.addAssignment(b1);

        Classification y1 = new Classification();
        root1.addChild(y1);
        y1.setWeight(Classification.ONE_HUNDRED_PERCENT / 4);

        Assignment a2 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 4);
        y1.addAssignment(a2);
        Assignment b2 = new Assignment(B, Classification.ONE_HUNDRED_PERCENT * 1 / 10);
        y1.addAssignment(b2);

        Classification z = new Classification();
        root1.addChild(z);
        z.setWeight(Classification.ONE_HUNDRED_PERCENT / 2);

        Assignment a3 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 4);
        z.addAssignment(a3);
        Assignment b3 = new Assignment(B, Classification.ONE_HUNDRED_PERCENT * 8 / 10);
        z.addAssignment(b3);
    }
    
    // MIXED_CLASSIFICATION_OVERDETERMINED_TAXONOMY
    // @formatter:off
    // - Root (weight: 100 %)
    //   - Classification X (25 %)
    //     - Security A (50 %)
    //     - Classification XY (100 %)
    //       - Security B (50 %)
    //   - Classification Z (75 %)
    //     - Security B (50 %)
    //     - Classification ZY (100 %)
    //       - Security A (50 %)
    // @formatter:on
    private static final Taxonomy MIXED_CLASSIFICATION_OVERDETERMINED_TAXONOMY;
    static
    {
        MIXED_CLASSIFICATION_OVERDETERMINED_TAXONOMY = new Taxonomy();
        Classification root = new Classification();
        MIXED_CLASSIFICATION_OVERDETERMINED_TAXONOMY.setRootNode(root);
        root.setWeight(Classification.ONE_HUNDRED_PERCENT);

        Classification x = new Classification();
        root.addChild(x);
        x.setWeight(Classification.ONE_HUNDRED_PERCENT / 4);

        Assignment a = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 2);
        x.addAssignment(a);

        Classification xy = new Classification();
        x.addChild(xy);
        xy.setWeight(Classification.ONE_HUNDRED_PERCENT);

        Assignment b = new Assignment(B, Classification.ONE_HUNDRED_PERCENT / 2);
        xy.addAssignment(b);

        Classification z = new Classification();
        root.addChild(z);
        z.setWeight(Classification.ONE_HUNDRED_PERCENT * 3 / 4);

        Assignment b2 = new Assignment(B, Classification.ONE_HUNDRED_PERCENT / 2);
        z.addAssignment(b2);
        
        Classification zy = new Classification();
        x.addChild(zy);
        zy.setWeight(Classification.ONE_HUNDRED_PERCENT);

        Assignment a2 = new Assignment(A, Classification.ONE_HUNDRED_PERCENT / 2);
        zy.addAssignment(a2);
    }

    private static final Client AB_ALL_ZERO_CLIENT;
    static
    {
        AB_ALL_ZERO_CLIENT = new Client();
        AB_ALL_ZERO_CLIENT.addSecurity(A);
        AB_ALL_ZERO_CLIENT.addSecurity(B);
    }

    private static final Client AB_EQUAL_WEIGHTED_CLIENT;
    static
    {
        AB_EQUAL_WEIGHTED_CLIENT = new Client();
        AB_EQUAL_WEIGHTED_CLIENT.addSecurity(A);
        AB_EQUAL_WEIGHTED_CLIENT.addSecurity(B);
        Portfolio portfolio = new Portfolio();
        AB_EQUAL_WEIGHTED_CLIENT.addPortfolio(portfolio);
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.now().minusDays(1), CURRENCY_UNIT,
                        10000L * Values.Quote.factorToMoney(), A, 100L * Values.Share.factor(), Type.DELIVERY_INBOUND,
                        0, 0));
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.now().minusDays(1), CURRENCY_UNIT,
                        10000L * Values.Quote.factorToMoney(), B, 100L * Values.Share.factor(), Type.DELIVERY_INBOUND,
                        0, 0));
    }

    private static final Client ONLY_A_CLIENT;
    static
    {
        ONLY_A_CLIENT = new Client();
        ONLY_A_CLIENT.addSecurity(A);
        ONLY_A_CLIENT.addSecurity(B);
        Portfolio portfolio = new Portfolio();
        ONLY_A_CLIENT.addPortfolio(portfolio);
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.now().minusDays(1), CURRENCY_UNIT,
                        10000L * Values.Quote.factorToMoney(), A, 100L * Values.Share.factor(), Type.DELIVERY_INBOUND,
                        0, 0));
    }

    private static final Client A_OVERWEIGHTED_CLIENT;
    static
    {
        A_OVERWEIGHTED_CLIENT = new Client();
        A_OVERWEIGHTED_CLIENT.addSecurity(A);
        A_OVERWEIGHTED_CLIENT.addSecurity(B);
        Portfolio portfolio = new Portfolio();
        A_OVERWEIGHTED_CLIENT.addPortfolio(portfolio);
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.now().minusDays(1), CURRENCY_UNIT,
                        10000L * Values.Quote.factorToMoney(), A, 200L * Values.Share.factor(), Type.DELIVERY_INBOUND,
                        0, 0));
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.now().minusDays(1), CURRENCY_UNIT,
                        10000L * Values.Quote.factorToMoney(), B, 100L * Values.Share.factor(), Type.DELIVERY_INBOUND,
                        0, 0));
    }
    
    private static final Client ABC_EQUAL_WEIGHTED_CLIENT;
    static
    {
        ABC_EQUAL_WEIGHTED_CLIENT = new Client();
        ABC_EQUAL_WEIGHTED_CLIENT.addSecurity(A);
        ABC_EQUAL_WEIGHTED_CLIENT.addSecurity(B);
        ABC_EQUAL_WEIGHTED_CLIENT.addSecurity(C);
        Portfolio portfolio = new Portfolio();
        ABC_EQUAL_WEIGHTED_CLIENT.addPortfolio(portfolio);
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.now().minusDays(1), CURRENCY_UNIT,
                        10000L * Values.Quote.factorToMoney(), A, 100L * Values.Share.factor(), Type.DELIVERY_INBOUND,
                        0, 0));
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.now().minusDays(1), CURRENCY_UNIT,
                        10000L * Values.Quote.factorToMoney(), B, 100L * Values.Share.factor(), Type.DELIVERY_INBOUND,
                        0, 0));
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.now().minusDays(1), CURRENCY_UNIT,
                        10000L * Values.Quote.factorToMoney(), C, 100L * Values.Share.factor(), Type.DELIVERY_INBOUND,
                        0, 0));
    }

    private static final Client ONLY_C_CLIENT;
    static
    {
        ONLY_C_CLIENT = new Client();
        ONLY_C_CLIENT.addSecurity(A);
        ONLY_C_CLIENT.addSecurity(B);
        ONLY_C_CLIENT.addSecurity(C);
        Portfolio portfolio = new Portfolio();
        ONLY_C_CLIENT.addPortfolio(portfolio);
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.now().minusDays(1), CURRENCY_UNIT,
                        10000L * Values.Quote.factorToMoney(), C, 300L * Values.Share.factor(), Type.DELIVERY_INBOUND,
                        0, 0));
    }

    private static final Client C_OVERWEIGHTED_CLIENT;
    static
    {
        C_OVERWEIGHTED_CLIENT = new Client();
        C_OVERWEIGHTED_CLIENT.addSecurity(A);
        C_OVERWEIGHTED_CLIENT.addSecurity(B);
        C_OVERWEIGHTED_CLIENT.addSecurity(C);
        Portfolio portfolio = new Portfolio();
        C_OVERWEIGHTED_CLIENT.addPortfolio(portfolio);
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.now().minusDays(1), CURRENCY_UNIT,
                        10000L * Values.Quote.factorToMoney(), A, 200L * Values.Share.factor(), Type.DELIVERY_INBOUND,
                        0, 0));
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.now().minusDays(1), CURRENCY_UNIT,
                        10000L * Values.Quote.factorToMoney(), B, 100L * Values.Share.factor(), Type.DELIVERY_INBOUND,
                        0, 0));
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.now().minusDays(1), CURRENCY_UNIT,
                        10000L * Values.Quote.factorToMoney(), C, 900L * Values.Share.factor(), Type.DELIVERY_INBOUND,
                        0, 0));
    }

    @Test
    public void testAllZero()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(AB_ALL_ZERO_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, AB_ALL_ZERO_CLIENT, SIMPLE_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(B));
    }

    @Test
    public void testAlreadyRebalanced()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        AB_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, AB_EQUAL_WEIGHTED_CLIENT, SIMPLE_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(B));
    }

    @Test
    public void testWithUnaasigned()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        ABC_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ABC_EQUAL_WEIGHTED_CLIENT, SIMPLE_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(B));
    }

    @Test
    public void testOnlyA()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(ONLY_A_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ONLY_A_CLIENT, SIMPLE_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, -5000), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, 5000), rebalancingResult.getMoney(B));
    }

    @Test
    public void testAOverweighted()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        A_OVERWEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, A_OVERWEIGHTED_CLIENT, SIMPLE_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, -5000), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, 5000), rebalancingResult.getMoney(B));
    }

    @Test
    public void testAllZeroMixed()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(AB_ALL_ZERO_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, AB_ALL_ZERO_CLIENT, MIXED_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(B));
    }

    @Test
    public void testAlreadyRebalancedMixed()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        AB_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, AB_EQUAL_WEIGHTED_CLIENT, MIXED_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(B));
    }

    @Test
    public void testAlreadyRebalancedMixedWithUnassigned()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        ABC_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ABC_EQUAL_WEIGHTED_CLIENT, MIXED_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(B));
    }

    @Test
    public void testOnlyAMixed()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(ONLY_A_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ONLY_A_CLIENT, MIXED_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, -5000), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, 5000), rebalancingResult.getMoney(B));
    }

    @Test
    public void testAOverweightedMixed()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        A_OVERWEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, A_OVERWEIGHTED_CLIENT, MIXED_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, -5000), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, 5000), rebalancingResult.getMoney(B));
    }

    @Test
    public void testAllZeroHalfHalf()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(AB_ALL_ZERO_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, AB_ALL_ZERO_CLIENT, HALF_HALF_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertTrue(rebalancingResult.isAmbigous(A));
        assertTrue(rebalancingResult.isExact(B));
        assertTrue(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(B).add(rebalancingResult.getMoney(A)));
    }

    @Test
    public void testAlreadyRebalancedHalfHalf()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        AB_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, AB_EQUAL_WEIGHTED_CLIENT,
                        HALF_HALF_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertTrue(rebalancingResult.isAmbigous(A));
        assertTrue(rebalancingResult.isExact(B));
        assertTrue(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(B).add(rebalancingResult.getMoney(A)));
    }

    @Test
    public void testAlreadyRebalancedHalfHalfWithUnassigned()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        ABC_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ABC_EQUAL_WEIGHTED_CLIENT,
                        HALF_HALF_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertTrue(rebalancingResult.isAmbigous(A));
        assertTrue(rebalancingResult.isExact(B));
        assertTrue(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(B).add(rebalancingResult.getMoney(A)));
    }

    @Test
    public void testOnlyAHalfHalf()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(ONLY_A_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ONLY_A_CLIENT, HALF_HALF_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertTrue(rebalancingResult.isAmbigous(A));
        assertTrue(rebalancingResult.isExact(B));
        assertTrue(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(B).add(rebalancingResult.getMoney(A)));
    }

    @Test
    public void testAOverweightedHalfHalf()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        A_OVERWEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, A_OVERWEIGHTED_CLIENT, HALF_HALF_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertTrue(rebalancingResult.isAmbigous(A));
        assertTrue(rebalancingResult.isExact(B));
        assertTrue(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(B).add(rebalancingResult.getMoney(A)));
    }

    @Test
    public void testMixedClassificationEqualWeightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        AB_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, AB_EQUAL_WEIGHTED_CLIENT,
                        MIXED_CLASSIFICATION_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, 10000), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, -10000), rebalancingResult.getMoney(B));
    }

    @Test
    public void testMixedClassificationOnlyAClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(ONLY_A_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ONLY_A_CLIENT,
                        MIXED_CLASSIFICATION_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(B));
    }

    @Test
    public void testMixedClassificationAOverweightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        A_OVERWEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, A_OVERWEIGHTED_CLIENT,
                        MIXED_CLASSIFICATION_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, 10000), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, -10000), rebalancingResult.getMoney(B));
    }

    @Test
    public void testMixedClassification2EqualWeightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        AB_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, AB_EQUAL_WEIGHTED_CLIENT,
                        MIXED_CLASSIFICATION_TAXONOMY2);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, 10000), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, -10000), rebalancingResult.getMoney(B));
    }

    @Test
    public void testMixedClassification2OnlyAClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(ONLY_A_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ONLY_A_CLIENT,
                        MIXED_CLASSIFICATION_TAXONOMY2);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(B));
    }

    @Test
    public void testMixedClassification2AOverweightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        A_OVERWEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, A_OVERWEIGHTED_CLIENT,
                        MIXED_CLASSIFICATION_TAXONOMY2);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, 10000), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, -10000), rebalancingResult.getMoney(B));
    }

    @Test
    public void testMixedClassification3EqualWeightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        ABC_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ABC_EQUAL_WEIGHTED_CLIENT,
                        MIXED_CLASSIFICATION_TAXONOMY3);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B, C), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, 5000), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(B));
        assertTrue(rebalancingResult.isExact(C));
        assertFalse(rebalancingResult.isAmbigous(C));
        assertEquals(Money.of(CURRENCY_UNIT, -5000), rebalancingResult.getMoney(C));
    }

    @Test
    public void testMixedClassification3OnlyCClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(ONLY_C_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ONLY_C_CLIENT,
                        MIXED_CLASSIFICATION_TAXONOMY3);
        Rebalancer.RebalancingSolution rebalancingResult =  model.getRebalancingSolution();

        assertEquals(asSet(A, B, C), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, 15000), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, 10000), rebalancingResult.getMoney(B));
        assertTrue(rebalancingResult.isExact(C));
        assertFalse(rebalancingResult.isAmbigous(C));
        assertEquals(Money.of(CURRENCY_UNIT, -25000), rebalancingResult.getMoney(C));
    }

    @Test
    public void testMixedClassification3COverweightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        C_OVERWEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, C_OVERWEIGHTED_CLIENT,
                        MIXED_CLASSIFICATION_TAXONOMY3);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B, C), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, 40000), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, 30000), rebalancingResult.getMoney(B));
        assertTrue(rebalancingResult.isExact(C));
        assertFalse(rebalancingResult.isAmbigous(C));
        assertEquals(Money.of(CURRENCY_UNIT, -70000), rebalancingResult.getMoney(C));
    }

    @Test
    public void testMixedClassification4EqualWeightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        ABC_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ABC_EQUAL_WEIGHTED_CLIENT,
                        MIXED_CLASSIFICATION_TAXONOMY4);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B, C), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, -769), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, 3846), rebalancingResult.getMoney(B));
        assertTrue(rebalancingResult.isExact(C));
        assertFalse(rebalancingResult.isAmbigous(C));
        assertEquals(Money.of(CURRENCY_UNIT, -3077), rebalancingResult.getMoney(C));
    }

    @Test
    public void testMixedClassification4OnlyCClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(ONLY_C_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ONLY_C_CLIENT,
                        MIXED_CLASSIFICATION_TAXONOMY4);
        Rebalancer.RebalancingSolution rebalancingResult =  model.getRebalancingSolution();

        assertEquals(asSet(A, B, C), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, 9231), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, 13846), rebalancingResult.getMoney(B));
        assertTrue(rebalancingResult.isExact(C));
        assertFalse(rebalancingResult.isAmbigous(C));
        assertEquals(Money.of(CURRENCY_UNIT, -23077), rebalancingResult.getMoney(C));
    }

    @Test
    public void testMixedClassification4COverweightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        C_OVERWEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, C_OVERWEIGHTED_CLIENT,
                        MIXED_CLASSIFICATION_TAXONOMY4);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B, C), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, 16923), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, 45385), rebalancingResult.getMoney(B));
        assertTrue(rebalancingResult.isExact(C));
        assertFalse(rebalancingResult.isAmbigous(C));
        assertEquals(Money.of(CURRENCY_UNIT, -62308), rebalancingResult.getMoney(C));
    }

    @Test
    public void testAllInRootClassificationEqualWeightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        ABC_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ABC_EQUAL_WEIGHTED_CLIENT,
                        ABC_IN_ROOT_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B, C), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertTrue(rebalancingResult.isAmbigous(A));
        assertTrue(rebalancingResult.isExact(B));
        assertTrue(rebalancingResult.isAmbigous(B));
        assertTrue(rebalancingResult.isExact(C));
        assertTrue(rebalancingResult.isAmbigous(C));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A).add(rebalancingResult.getMoney(B).add(rebalancingResult.getMoney(C))));
    }

    @Test
    public void tesAllInRootClassificationClassificationOnlyCClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(ONLY_C_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ONLY_C_CLIENT,
                        ABC_IN_ROOT_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B, C), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertTrue(rebalancingResult.isAmbigous(A));
        assertTrue(rebalancingResult.isExact(B));
        assertTrue(rebalancingResult.isAmbigous(B));
        assertTrue(rebalancingResult.isExact(C));
        assertTrue(rebalancingResult.isAmbigous(C));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A).add(rebalancingResult.getMoney(B).add(rebalancingResult.getMoney(C))));
    }

    @Test
    public void testAllInRootClassificationCOverweightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        C_OVERWEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, C_OVERWEIGHTED_CLIENT,
                        ABC_IN_ROOT_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B, C), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertTrue(rebalancingResult.isAmbigous(A));
        assertTrue(rebalancingResult.isExact(B));
        assertTrue(rebalancingResult.isAmbigous(B));
        assertTrue(rebalancingResult.isExact(C));
        assertTrue(rebalancingResult.isAmbigous(C));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A).add(rebalancingResult.getMoney(B).add(rebalancingResult.getMoney(C))));
    }

    @Test
    public void testMixedAmbigousEqualWeighted()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        ABC_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ABC_EQUAL_WEIGHTED_CLIENT,
                        MIXED_AMBIGUOUS_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B, C), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertTrue(rebalancingResult.isAmbigous(A));
        assertTrue(rebalancingResult.isExact(B));
        assertTrue(rebalancingResult.isAmbigous(B));
        assertTrue(rebalancingResult.isExact(C));
        assertTrue(rebalancingResult.isAmbigous(C));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A).add(rebalancingResult.getMoney(B).add(rebalancingResult.getMoney(C))));
        assertEquals(rebalancingResult.getMoney(A), rebalancingResult.getMoney(C));
    }

    @Test
    public void testMixedAmbigousOnlyCClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(ONLY_C_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ONLY_C_CLIENT,
                        MIXED_AMBIGUOUS_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B, C), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertTrue(rebalancingResult.isAmbigous(A));
        assertTrue(rebalancingResult.isExact(B));
        assertTrue(rebalancingResult.isAmbigous(B));
        assertTrue(rebalancingResult.isExact(C));
        assertTrue(rebalancingResult.isAmbigous(C));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A).add(rebalancingResult.getMoney(B).add(rebalancingResult.getMoney(C))));
        assertEquals(rebalancingResult.getMoney(A), rebalancingResult.getMoney(C).add(Money.of(CURRENCY_UNIT, 30000)));
    }

    @Test
    public void testMixedAmbigousCOverweightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        C_OVERWEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, C_OVERWEIGHTED_CLIENT,
                        MIXED_AMBIGUOUS_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B, C), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertTrue(rebalancingResult.isAmbigous(A));
        assertTrue(rebalancingResult.isExact(B));
        assertTrue(rebalancingResult.isAmbigous(B));
        assertTrue(rebalancingResult.isExact(C));
        assertTrue(rebalancingResult.isAmbigous(C));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A).add(rebalancingResult.getMoney(B).add(rebalancingResult.getMoney(C))));
        assertEquals(rebalancingResult.getMoney(A), rebalancingResult.getMoney(C).add(Money.of(CURRENCY_UNIT, 70000)));
    }
    
    @Test
    public void testMixedClassificationAmbiguousEqualWeightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        AB_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, AB_EQUAL_WEIGHTED_CLIENT,
                        MIXED_CLASSIFICATION_AMBIGUOUS_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertTrue(rebalancingResult.isAmbigous(A));
        assertTrue(rebalancingResult.isExact(B));
        assertTrue(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A).add(rebalancingResult.getMoney(B)));
    }

    @Test
    public void testMixedClassificationAmbiguousOnlyAClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(ONLY_A_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ONLY_A_CLIENT,
                        MIXED_CLASSIFICATION_AMBIGUOUS_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertTrue(rebalancingResult.isAmbigous(A));
        assertTrue(rebalancingResult.isExact(B));
        assertTrue(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A).add(rebalancingResult.getMoney(B)));
    }

    @Test
    public void testMixedClassificationAmbiguousAOverweightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        A_OVERWEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, A_OVERWEIGHTED_CLIENT,
                        MIXED_CLASSIFICATION_AMBIGUOUS_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertTrue(rebalancingResult.isAmbigous(A));
        assertTrue(rebalancingResult.isExact(B));
        assertTrue(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A).add(rebalancingResult.getMoney(B)));
    }

    @Test
    public void testMixedClassificationPartiallyAmbigousEqualWeighted()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        ABC_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ABC_EQUAL_WEIGHTED_CLIENT,
                        MIXED_CLASSIFICATION_PARTIALLY_AMBIGUOUS_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B, C), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertTrue(rebalancingResult.isAmbigous(A));
        assertTrue(rebalancingResult.isExact(B));
        assertTrue(rebalancingResult.isAmbigous(B));
        assertTrue(rebalancingResult.isExact(C));
        assertFalse(rebalancingResult.isAmbigous(C));
        assertEquals(Money.of(CURRENCY_UNIT, -5000), rebalancingResult.getMoney(A).add(rebalancingResult.getMoney(B)));
        assertEquals(Money.of(CURRENCY_UNIT, 5000), rebalancingResult.getMoney(C));
    }

    @Test
    public void testMixedClassificationPartiallyAmbigousOnlyCClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(ONLY_C_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ONLY_C_CLIENT,
                        MIXED_CLASSIFICATION_PARTIALLY_AMBIGUOUS_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B, C), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertTrue(rebalancingResult.isAmbigous(A));
        assertTrue(rebalancingResult.isExact(B));
        assertTrue(rebalancingResult.isAmbigous(B));
        assertTrue(rebalancingResult.isExact(C));
        assertFalse(rebalancingResult.isAmbigous(C));
        assertEquals(Money.of(CURRENCY_UNIT, 15000), rebalancingResult.getMoney(A).add(rebalancingResult.getMoney(B)));
        assertEquals(Money.of(CURRENCY_UNIT, -15000), rebalancingResult.getMoney(C));
    }

    @Test
    public void testMixedClassificationPartiallyAmbigousCOverweightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        C_OVERWEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, C_OVERWEIGHTED_CLIENT,
                        MIXED_CLASSIFICATION_PARTIALLY_AMBIGUOUS_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B, C), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertTrue(rebalancingResult.isAmbigous(A));
        assertTrue(rebalancingResult.isExact(B));
        assertTrue(rebalancingResult.isAmbigous(B));
        assertTrue(rebalancingResult.isExact(C));
        assertFalse(rebalancingResult.isAmbigous(C));
        assertEquals(Money.of(CURRENCY_UNIT, 30000), rebalancingResult.getMoney(A).add(rebalancingResult.getMoney(B)));
        assertEquals(Money.of(CURRENCY_UNIT, -30000), rebalancingResult.getMoney(C));
    }
    
    @Test
    public void testSimpleOverdeterminedEqualWeightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        AB_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, AB_EQUAL_WEIGHTED_CLIENT,
                        SIMPLE_OVERDETERMINED_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A), rebalancingResult.getInvestmentVehicles());
        assertFalse(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A));
    }

    @Test
    public void testSimpleOverdeterminedAmbiguousOnlyAClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(ONLY_A_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ONLY_A_CLIENT,
                        SIMPLE_OVERDETERMINED_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A), rebalancingResult.getInvestmentVehicles());
        assertFalse(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A));
    }

    @Test
    public void testSimpleOverdeterminedAmbiguousAOverweightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        A_OVERWEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, A_OVERWEIGHTED_CLIENT,
                        SIMPLE_OVERDETERMINED_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A), rebalancingResult.getInvestmentVehicles());
        assertFalse(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A));
    }
    
    @Test
    public void testSimplePartiallyOverdeterminedEqualWeightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        AB_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, AB_EQUAL_WEIGHTED_CLIENT,
                        SIMPLE_PARTIALLY_OVERDETERMINED_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A,B), rebalancingResult.getInvestmentVehicles());
        assertFalse(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, 5000), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, -5000), rebalancingResult.getMoney(B));
    }

    @Test
    public void testSimplePartiallyOverdeterminedAmbiguousOnlyAClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(ONLY_A_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ONLY_A_CLIENT,
                        SIMPLE_PARTIALLY_OVERDETERMINED_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A,B), rebalancingResult.getInvestmentVehicles());
        assertFalse(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, -2500), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, 2500), rebalancingResult.getMoney(B));
    }

    @Test
    public void testSimplePartiallyOverdeterminedAmbiguousAOverweightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        A_OVERWEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, A_OVERWEIGHTED_CLIENT,
                        SIMPLE_PARTIALLY_OVERDETERMINED_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A,B), rebalancingResult.getInvestmentVehicles());
        assertFalse(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, 2500), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, -2500), rebalancingResult.getMoney(B));
    }
    
    @Test
    public void testMixedOverdeterminedEqualWeightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        AB_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, AB_EQUAL_WEIGHTED_CLIENT,
                        MIXED_OVERDETERMINED_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A,B), rebalancingResult.getInvestmentVehicles());
        assertFalse(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, 206), rebalancingResult.getMoney(A));
        assertFalse(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, -206), rebalancingResult.getMoney(B));
    }

    @Test
    public void testMixedOverdeterminedAmbiguousOnlyAClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(ONLY_A_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ONLY_A_CLIENT,
                        MIXED_OVERDETERMINED_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A,B), rebalancingResult.getInvestmentVehicles());
        assertFalse(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, -4897), rebalancingResult.getMoney(A));
        assertFalse(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, 4897), rebalancingResult.getMoney(B));
    }

    @Test
    public void testMixedOverdeterminedAmbiguousAOverweightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        A_OVERWEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, A_OVERWEIGHTED_CLIENT,
                        MIXED_OVERDETERMINED_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A,B), rebalancingResult.getInvestmentVehicles());
        assertFalse(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, -4691), rebalancingResult.getMoney(A));
        assertFalse(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, 4691), rebalancingResult.getMoney(B));
    }

    @Test
    public void testMixedClassificationOverdeterminedEqualWeightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        AB_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, AB_EQUAL_WEIGHTED_CLIENT, MIXED_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(B));
    }

    @Test
    public void testMixedClassificationOverdeterminedEqualWeightedClientWithUnassigned()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        ABC_EQUAL_WEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ABC_EQUAL_WEIGHTED_CLIENT, MIXED_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(AMOUNT_0, rebalancingResult.getMoney(B));
    }

    @Test
    public void testMixedClassificationOverdeterminedAmbiguousOnlyAClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(ONLY_A_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, ONLY_A_CLIENT, MIXED_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, -5000), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, 5000), rebalancingResult.getMoney(B));
    }

    @Test
    public void testMixedClassificationOverdeterminedAmbiguousAOverweightedClient()
    {
        ExchangeRateProviderFactory exchangeRateProviderFactory = new ExchangeRateProviderFactory(
                        A_OVERWEIGHTED_CLIENT);
        TaxonomyModel model = new TaxonomyModel(exchangeRateProviderFactory, A_OVERWEIGHTED_CLIENT, MIXED_TAXONOMY);
        Rebalancer.RebalancingSolution rebalancingResult = model.getRebalancingSolution();

        assertEquals(asSet(A, B), rebalancingResult.getInvestmentVehicles());
        assertTrue(rebalancingResult.isExact(A));
        assertFalse(rebalancingResult.isAmbigous(A));
        assertEquals(Money.of(CURRENCY_UNIT, -5000), rebalancingResult.getMoney(A));
        assertTrue(rebalancingResult.isExact(B));
        assertFalse(rebalancingResult.isAmbigous(B));
        assertEquals(Money.of(CURRENCY_UNIT, 5000), rebalancingResult.getMoney(B));
    }

    @SafeVarargs
    private static <T> Set<T> asSet(T... objects)
    {
        return new HashSet<T>(Arrays.asList(objects));
    }
}
