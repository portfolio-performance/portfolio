package name.abuchen.portfolio.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import name.abuchen.portfolio.math.Rebalancer.FixedSumRebalancer;
import name.abuchen.portfolio.math.Rebalancer.RebalancingConstraint;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

public class RebalancerTest
{
    private static final InvestmentVehicle A = new Security();
    private static final InvestmentVehicle B = new Security();
    private static final InvestmentVehicle C = new Security();
    private static final InvestmentVehicle D = new Security();

    private static final String CURRENCY_UNIT = CurrencyUnit.USD;
    private static final Money AMOUNT_0 = Money.of(CURRENCY_UNIT, 0);
    private static final Money AMOUNT_1 = Money.of(CURRENCY_UNIT, 100000);
    private static final Money AMOUNT_2 = Money.of(CURRENCY_UNIT, 200000);
    private static final Money AMOUNT_NEGATIVE_1 = Money.of(CURRENCY_UNIT, -100000);

    @Test
    public void testNoConstraints()
    {
        Rebalancer rebalancer = new Rebalancer();
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(Collections.emptySet(), solution.getInvestmentVehicles());
    }
    @Test
    public void testEmptyConstraints()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_0));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(Collections.emptySet(), solution.getInvestmentVehicles());
    }
    
    @Test
    public void test1Constraint1Security()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(Collections.singleton(A), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertEquals(AMOUNT_1, solution.getMoney(A));
    }
    
    @Test
    public void test1Constraint1SecurityNegative()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_NEGATIVE_1));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(Collections.singleton(A), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertEquals(AMOUNT_NEGATIVE_1, solution.getMoney(A));
    }
    
    @Test
    public void test1Constraint1SecurityZero()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_0));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(Collections.singleton(A), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertEquals(AMOUNT_0, solution.getMoney(A));
    }
    
    @Test
    public void test2Constraints2IndependentSecurities()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(B, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_2));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertEquals(AMOUNT_1, solution.getMoney(A));
        assertFalse(solution.isAmbigous(B));
        assertTrue(solution.isExact(B));
        assertEquals(AMOUNT_2, solution.getMoney(B));
    }
    
    @Test
    public void test2Constraints2DependentSecurities()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, .9d);
        map.put(B, .1d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(A, .1d);
        map2.put(B, .9d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_2));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertEquals(Money.of(CURRENCY_UNIT, 87500), solution.getMoney(A));
        assertFalse(solution.isAmbigous(B));
        assertTrue(solution.isExact(B));
        assertEquals(Money.of(CURRENCY_UNIT, 212500), solution.getMoney(B));
    }
    
    @Test
    public void test3Constraints3DependentSecurities()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, .9d);
        map.put(B, .1d);
        map.put(C, .5d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(A, .1d);
        map2.put(B, .9d);
        map2.put(C, .5d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_2));
        Map<InvestmentVehicle, Double> map3 = new HashMap<>();
        map3.put(C, .5d);
        rebalancer.addConstraint(new RebalancingConstraint(map3, AMOUNT_0));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B,C), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertEquals(Money.of(CURRENCY_UNIT, 87500), solution.getMoney(A));
        assertFalse(solution.isAmbigous(B));
        assertTrue(solution.isExact(B));
        assertEquals(Money.of(CURRENCY_UNIT, 212500), solution.getMoney(B));
        assertFalse(solution.isAmbigous(C));
        assertTrue(solution.isExact(C));
        assertEquals(AMOUNT_0, solution.getMoney(C));
    }
    
    @Test
    public void testDuplicatedConstraint()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_1));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(Collections.singleton(A), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertEquals(AMOUNT_1, solution.getMoney(A));
    }
    
    @Test
    public void testRedundantConstraint()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, .9d);
        map.put(B, .1d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(A, .1d);
        map2.put(B, .9d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_2));
        Map<InvestmentVehicle, Double> map3 = new HashMap<>();
        map3.put(A, .5d);
        map3.put(B, .5d);
        rebalancer.addConstraint(new RebalancingConstraint(map3, Money.of(CURRENCY_UNIT, 150000)));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertEquals(Money.of(CURRENCY_UNIT, 87500), solution.getMoney(A));
        assertFalse(solution.isAmbigous(B));
        assertTrue(solution.isExact(B));
        assertEquals(Money.of(CURRENCY_UNIT, 212500), solution.getMoney(B));
    }
    
    @Test
    public void testUnsolvableConstraint()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_2));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_0));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(Collections.singleton(A), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertFalse(solution.isExact(A));
        assertEquals(AMOUNT_1, solution.getMoney(A));
    }
    
    @Test
    public void testUnsolvableConstraint2()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(B, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_1));
        Map<InvestmentVehicle, Double> map3 = new HashMap<>();
        map3.put(A, 1d);
        map3.put(B, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map3, AMOUNT_1));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertFalse(solution.isExact(A));
        assertEquals(Money.of(CURRENCY_UNIT, Math.round(100000d * 2d/3d)), solution.getMoney(A));
        assertFalse(solution.isAmbigous(B));
        assertFalse(solution.isExact(B));
        assertEquals(Money.of(CURRENCY_UNIT, Math.round(100000d * 2d/3d)), solution.getMoney(B));
    }
    
    @Test
    public void testPartiallyUnsolvableConstraint()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_2));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_0));
        Map<InvestmentVehicle, Double> map3 = new HashMap<>();
        map3.put(C, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map3, AMOUNT_0));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,C), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertFalse(solution.isExact(A));
        assertEquals(AMOUNT_1, solution.getMoney(A));
        assertFalse(solution.isAmbigous(C));
        assertTrue(solution.isExact(C));
        assertEquals(AMOUNT_0, solution.getMoney(C));
    }
    
    @Test
    public void testPartiallyUnsolvableConstraint2()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(B, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_1));
        Map<InvestmentVehicle, Double> map3 = new HashMap<>();
        map3.put(A, 1d);
        map3.put(B, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map3, AMOUNT_1));
        Map<InvestmentVehicle, Double> map4 = new HashMap<>();
        map4.put(C, .1d);
        map4.put(D, .9d);
        rebalancer.addConstraint(new RebalancingConstraint(map4, AMOUNT_1));
        Map<InvestmentVehicle, Double> map5 = new HashMap<>();
        map5.put(C, .9d);
        map5.put(D, .1d);
        rebalancer.addConstraint(new RebalancingConstraint(map5, AMOUNT_1));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B,C,D), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertFalse(solution.isExact(A));
        assertEquals(Money.of(CURRENCY_UNIT, Math.round(100000d * 2d/3d)), solution.getMoney(A));
        assertFalse(solution.isAmbigous(B));
        assertFalse(solution.isExact(B));
        assertEquals(Money.of(CURRENCY_UNIT, Math.round(100000d * 2d/3d)), solution.getMoney(B));
        assertFalse(solution.isAmbigous(C));
        assertTrue(solution.isExact(C));
        assertEquals(AMOUNT_1, solution.getMoney(C));
        assertFalse(solution.isAmbigous(D));
        assertTrue(solution.isExact(D));
        assertEquals(AMOUNT_1, solution.getMoney(C));
    }
    
    @Test
    public void testAmbigous()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, .5d);
        map.put(B, .5d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B), solution.getInvestmentVehicles());
        assertTrue(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertTrue(solution.isAmbigous(B));
        assertTrue(solution.isExact(B));
        assertEquals(AMOUNT_2, solution.getMoney(A).add(solution.getMoney(B)));
    }
    
    @Test
    public void testAmbigousWithRedundantConstraint()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, .5d);
        map.put(B, .5d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(A, 1d);
        map2.put(B, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_2));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B), solution.getInvestmentVehicles());
        assertTrue(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertTrue(solution.isAmbigous(B));
        assertTrue(solution.isExact(B));
        assertEquals(AMOUNT_2, solution.getMoney(A).add(solution.getMoney(B)));
    }
    
    @Test
    public void testPartiallyAmbigous()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, .5d);
        map.put(B, .5d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(C, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_1));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B,C), solution.getInvestmentVehicles());
        assertTrue(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertTrue(solution.isAmbigous(B));
        assertTrue(solution.isExact(B));
        assertEquals(AMOUNT_2, solution.getMoney(A).add(solution.getMoney(B)));
        assertFalse(solution.isAmbigous(C));
        assertTrue(solution.isExact(C));
        assertEquals(AMOUNT_1, solution.getMoney(C));
    }
    
    @Test
    public void testPartiallyAmbigousWithRedundantConstraint()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, .5d);
        map.put(B, .5d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(C, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_1));
        Map<InvestmentVehicle, Double> map3 = new HashMap<>();
        map3.put(A, 1d);
        map3.put(B, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map3, AMOUNT_2));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B,C), solution.getInvestmentVehicles());
        assertTrue(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertTrue(solution.isAmbigous(B));
        assertTrue(solution.isExact(B));
        assertEquals(AMOUNT_2, solution.getMoney(A).add(solution.getMoney(B)));
        assertFalse(solution.isAmbigous(C));
        assertTrue(solution.isExact(C));
        assertEquals(AMOUNT_1, solution.getMoney(C));
    }
    
    @Test
    public void testPartiallyAmbigousWith2RedundantConstraints()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, .5d);
        map.put(B, .5d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(C, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_1));
        Map<InvestmentVehicle, Double> map3 = new HashMap<>();
        map3.put(A, 1d);
        map3.put(B, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map3, AMOUNT_2));
        Map<InvestmentVehicle, Double> map4 = new HashMap<>();
        map4.put(C, 2d);
        rebalancer.addConstraint(new RebalancingConstraint(map4, AMOUNT_2));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B,C), solution.getInvestmentVehicles());
        assertTrue(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertTrue(solution.isAmbigous(B));
        assertTrue(solution.isExact(B));
        assertEquals(AMOUNT_2, solution.getMoney(A).add(solution.getMoney(B)));
        assertFalse(solution.isAmbigous(C));
        assertTrue(solution.isExact(C));
        assertEquals(AMOUNT_1, solution.getMoney(C));
    }
    
    @Test
    public void testPartiallyAmbigousPartiallyUnsolvable()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, .5d);
        map.put(B, .5d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(C, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_1));
        Map<InvestmentVehicle, Double> map3 = new HashMap<>();
        map3.put(C, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map3, AMOUNT_2));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B,C), solution.getInvestmentVehicles());
        assertTrue(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertTrue(solution.isAmbigous(B));
        assertTrue(solution.isExact(B));
        assertEquals(AMOUNT_2, solution.getMoney(A).add(solution.getMoney(B)));
        assertFalse(solution.isAmbigous(C));
        assertFalse(solution.isExact(C));
        assertEquals(Money.of(CURRENCY_UNIT, 150000), solution.getMoney(C));
    }
    
    @Test
    public void testPartiallyAmbigousPartiallyUnsolvable2()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(C, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_1));
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 2d);
        map.put(B, 2d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map3 = new HashMap<>();
        map3.put(D, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map3, AMOUNT_1));
        Map<InvestmentVehicle, Double> map4 = new HashMap<>();
        map4.put(C, 1d);
        map4.put(D, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map4, AMOUNT_1));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B,C,D), solution.getInvestmentVehicles());
        assertTrue(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertTrue(solution.isAmbigous(B));
        assertTrue(solution.isExact(B));
        assertEquals(Money.of(CURRENCY_UNIT, 50000), solution.getMoney(A).add(solution.getMoney(B)));
        assertFalse(solution.isAmbigous(C));
        assertFalse(solution.isExact(C));
        assertEquals(Money.of(CURRENCY_UNIT, 66667), solution.getMoney(C));
        assertFalse(solution.isAmbigous(D));
        assertFalse(solution.isExact(D));
        assertEquals(Money.of(CURRENCY_UNIT, 66667), solution.getMoney(D));
    }
    
    @Test
    public void testPartiallyAmbigousPartiallyUnsolvablePartiallyExact()
    {
        Rebalancer rebalancer = new Rebalancer();
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(C, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_1));
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 2d);
        map.put(B, 2d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map3 = new HashMap<>();
        map3.put(D, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map3, AMOUNT_1));
        Map<InvestmentVehicle, Double> map4 = new HashMap<>();
        map4.put(C, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map4, AMOUNT_2));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B,C,D), solution.getInvestmentVehicles());
        assertTrue(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertTrue(solution.isAmbigous(B));
        assertTrue(solution.isExact(B));
        assertEquals(Money.of(CURRENCY_UNIT, 50000), solution.getMoney(A).add(solution.getMoney(B)));
        assertFalse(solution.isAmbigous(C));
        assertFalse(solution.isExact(C));
        assertEquals(Money.of(CURRENCY_UNIT, 150000), solution.getMoney(C));
        assertFalse(solution.isAmbigous(D));
        assertTrue(solution.isExact(D));
        assertEquals(AMOUNT_1, solution.getMoney(D));
    }
    
    // From here on tests for the fixedSumRebalancer
    
    @Test
    public void testFixedSumRebalancerUnsolvableConstraintSumZero()
    {
        FixedSumRebalancer rebalancer = new FixedSumRebalancer(AMOUNT_0);
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_2));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_0));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(Collections.singleton(A), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertFalse(solution.isExact(A));
        assertEquals(AMOUNT_0, solution.getMoney(A));
    }
    
    @Test
    public void testFixedSumRebalancerUnsolvableConstraintSumOne()
    {
        FixedSumRebalancer rebalancer = new FixedSumRebalancer(AMOUNT_1);
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_2));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_0));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(Collections.singleton(A), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertFalse(solution.isExact(A));
        assertEquals(AMOUNT_1, solution.getMoney(A));
    }
    
    @Test
    public void testFixedSumRebalancerUnsolvableConstraint2SumZero()
    {
        FixedSumRebalancer rebalancer = new FixedSumRebalancer(AMOUNT_0);
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(B, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_1));
        Map<InvestmentVehicle, Double> map3 = new HashMap<>();
        map3.put(A, 1d);
        map3.put(B, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map3, AMOUNT_1));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertFalse(solution.isExact(A));
        assertEquals(AMOUNT_0, solution.getMoney(A));
        assertFalse(solution.isAmbigous(B));
        assertFalse(solution.isExact(B));
        assertEquals(AMOUNT_0, solution.getMoney(B));
    }
    
    @Test
    public void testFixedSumRebalancerUnsolvableConstraint2SumTwo()
    {
        FixedSumRebalancer rebalancer = new FixedSumRebalancer(AMOUNT_2);
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(B, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_1));
        Map<InvestmentVehicle, Double> map3 = new HashMap<>();
        map3.put(A, 1d);
        map3.put(B, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map3, AMOUNT_1));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertFalse(solution.isExact(A));
        assertEquals(AMOUNT_1, solution.getMoney(A));
        assertFalse(solution.isAmbigous(B));
        assertFalse(solution.isExact(B));
        assertEquals(AMOUNT_1, solution.getMoney(B));
    }
    
    @Test
    public void testFixedSumRebalancerPartiallyUnsolvableConstraintSumZero()
    {
        FixedSumRebalancer rebalancer = new FixedSumRebalancer(AMOUNT_0);
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_2));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_0));
        Map<InvestmentVehicle, Double> map3 = new HashMap<>();
        map3.put(C, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map3, AMOUNT_0));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,C), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertFalse(solution.isExact(A));
        assertEquals(AMOUNT_0, solution.getMoney(A));
        assertFalse(solution.isAmbigous(C));
        assertTrue(solution.isExact(C));
        assertEquals(AMOUNT_0, solution.getMoney(C));
    }
    
    @Test
    public void testFixedSumRebalancerPartiallyUnsolvableConstraintSumOne()
    {
        FixedSumRebalancer rebalancer = new FixedSumRebalancer(AMOUNT_1);
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_2));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_0));
        Map<InvestmentVehicle, Double> map3 = new HashMap<>();
        map3.put(C, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map3, AMOUNT_0));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,C), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertFalse(solution.isExact(A));
        assertEquals(AMOUNT_1, solution.getMoney(A));
        assertFalse(solution.isAmbigous(C));
        assertTrue(solution.isExact(C));
        assertEquals(AMOUNT_0, solution.getMoney(C));
    }
    
    @Test
    public void testFixedSumRebalancerPartiallyUnsolvableConstraint2()
    {
        FixedSumRebalancer rebalancer = new FixedSumRebalancer(Money.of(CURRENCY_UNIT, 400000));
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(B, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_1));
        Map<InvestmentVehicle, Double> map3 = new HashMap<>();
        map3.put(A, 1d);
        map3.put(B, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map3, AMOUNT_1));
        Map<InvestmentVehicle, Double> map4 = new HashMap<>();
        map4.put(C, .1d);
        map4.put(D, .9d);
        rebalancer.addConstraint(new RebalancingConstraint(map4, AMOUNT_1));
        Map<InvestmentVehicle, Double> map5 = new HashMap<>();
        map5.put(C, .9d);
        map5.put(D, .1d);
        rebalancer.addConstraint(new RebalancingConstraint(map5, AMOUNT_1));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B,C,D), solution.getInvestmentVehicles());
        assertFalse(solution.isAmbigous(A));
        assertFalse(solution.isExact(A));
        assertEquals(AMOUNT_1, solution.getMoney(A));
        assertFalse(solution.isAmbigous(B));
        assertFalse(solution.isExact(B));
        assertEquals(AMOUNT_1, solution.getMoney(B));
        assertFalse(solution.isAmbigous(C));
        assertTrue(solution.isExact(C));
        assertEquals(AMOUNT_1, solution.getMoney(C));
        assertFalse(solution.isAmbigous(D));
        assertTrue(solution.isExact(D));
        assertEquals(AMOUNT_1, solution.getMoney(C));
    }
    
    @Test
    public void testFixedSumRebalancerPartiallyAmbigousPartiallyUnsolvable()
    {
        FixedSumRebalancer rebalancer = new FixedSumRebalancer(Money.of(CURRENCY_UNIT, 300000));
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, .5d);
        map.put(B, .5d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(C, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_1));
        Map<InvestmentVehicle, Double> map3 = new HashMap<>();
        map3.put(C, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map3, AMOUNT_2));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B,C), solution.getInvestmentVehicles());
        assertTrue(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertTrue(solution.isAmbigous(B));
        assertTrue(solution.isExact(B));
        assertEquals(AMOUNT_2, solution.getMoney(A).add(solution.getMoney(B)));
        assertFalse(solution.isAmbigous(C));
        assertFalse(solution.isExact(C));
        assertEquals(AMOUNT_1, solution.getMoney(C));
    }
    
    @Test
    public void testFixedSumRebalancerPartiallyAmbigousPartiallyUnsolvable2()
    {
        FixedSumRebalancer rebalancer = new FixedSumRebalancer(Money.of(CURRENCY_UNIT, 250000));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(C, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_1));
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 2d);
        map.put(B, 2d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map3 = new HashMap<>();
        map3.put(D, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map3, AMOUNT_1));
        Map<InvestmentVehicle, Double> map4 = new HashMap<>();
        map4.put(C, 1d);
        map4.put(D, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map4, AMOUNT_1));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B,C,D), solution.getInvestmentVehicles());
        assertTrue(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertTrue(solution.isAmbigous(B));
        assertTrue(solution.isExact(B));
        assertEquals(Money.of(CURRENCY_UNIT, 50000), solution.getMoney(A).add(solution.getMoney(B)));
        assertFalse(solution.isAmbigous(C));
        assertFalse(solution.isExact(C));
        assertEquals(AMOUNT_1, solution.getMoney(C));
        assertFalse(solution.isAmbigous(D));
        assertFalse(solution.isExact(D));
        assertEquals(AMOUNT_1, solution.getMoney(D));
    }
    
    @Test
    public void testFixedSumRebalancerPartiallyAmbigousPartiallyUnsolvablePartiallyExact()
    {
        FixedSumRebalancer rebalancer = new FixedSumRebalancer(Money.of(CURRENCY_UNIT, 250000));
        Map<InvestmentVehicle, Double> map2 = new HashMap<>();
        map2.put(C, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map2, AMOUNT_1));
        Map<InvestmentVehicle, Double> map = new HashMap<>();
        map.put(A, 2d);
        map.put(B, 2d);
        rebalancer.addConstraint(new RebalancingConstraint(map, AMOUNT_1));
        Map<InvestmentVehicle, Double> map3 = new HashMap<>();
        map3.put(D, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map3, AMOUNT_1));
        Map<InvestmentVehicle, Double> map4 = new HashMap<>();
        map4.put(C, 1d);
        rebalancer.addConstraint(new RebalancingConstraint(map4, AMOUNT_2));
        Rebalancer.RebalancingSolution solution = rebalancer.solve();
        
        assertEquals(asSet(A,B,C,D), solution.getInvestmentVehicles());
        assertTrue(solution.isAmbigous(A));
        assertTrue(solution.isExact(A));
        assertTrue(solution.isAmbigous(B));
        assertTrue(solution.isExact(B));
        assertEquals(Money.of(CURRENCY_UNIT, 50000), solution.getMoney(A).add(solution.getMoney(B)));
        assertFalse(solution.isAmbigous(C));
        assertFalse(solution.isExact(C));
        assertEquals(AMOUNT_1, solution.getMoney(C));
        assertFalse(solution.isAmbigous(D));
        assertTrue(solution.isExact(D));
        assertEquals(AMOUNT_1, solution.getMoney(D));
    }
    
    @SafeVarargs
    private static <T> Set<T> asSet(T... objects)
    {
       return new HashSet<T>(Arrays.asList(objects));
    }
}
