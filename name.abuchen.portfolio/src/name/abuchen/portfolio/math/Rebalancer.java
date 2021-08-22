package name.abuchen.portfolio.math;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.util.Precision;

import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.money.MonetaryException;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.util.Pair;

public class Rebalancer
{
    private Map<InvestmentVehicle, Integer> investmentVehicleIndices;
    private List<InvestmentVehicle> investmentVehicles;
    private List<RebalancingConstraint> constraintsToSolve;
    
    private String currency;
    private String fallbackCurrency;
    
    public static class RebalancingConstraint
    {
        private Map<InvestmentVehicle, Double> linearEquation;
        private Money targetAmount;
        
        public RebalancingConstraint(Map<InvestmentVehicle, Double> linearEquation, Money targetAmount)
        {
            this.linearEquation = linearEquation;
            this.targetAmount = targetAmount;
        }
        
        @SafeVarargs
        public static RebalancingConstraint create(Money targetAmount, Pair<InvestmentVehicle, Double>... linearEquation)
        {
            Map<InvestmentVehicle, Double> linearEquationMap = new HashMap<>(linearEquation.length);
            for(int i = 0; i < linearEquation.length; i++)
            {
                if(linearEquationMap.put(linearEquation[i].getLeft(), linearEquation[i].getRight()) != null)
                {
                    // We overwrote a value in the map. => There were at least two entries for the securities in the linear equation.
                    throw new IllegalArgumentException("At least two entries for the security " //$NON-NLS-1$
                                    + linearEquation[i].getKey().toString() + " in a linear equation."); //$NON-NLS-1$
                }
            }
            return new RebalancingConstraint(linearEquationMap, targetAmount);
        }
    }
    
    public static class RebalancingSolution
    {
        private static final RebalancingSolution EMPTY_REBALANCING_SOLUTION =
                        new RebalancingSolution(Collections.emptyMap(), Collections.emptySet(), Collections.emptySet());
        
        private Map<InvestmentVehicle, Money> solution;
        private Set<InvestmentVehicle> ambigousResults;
        private Set<InvestmentVehicle> inexactResults;
        
        public RebalancingSolution(Map<InvestmentVehicle, Money> solution, Set<InvestmentVehicle> ambigousResults,
                        Set<InvestmentVehicle> inexactResults)
        {
            this.solution = solution;
            this.ambigousResults = ambigousResults;
            this.inexactResults = inexactResults;
        }

        public Set<InvestmentVehicle> getInvestmentVehicles()
        {
            return solution.keySet();
        }
        
        public Money getMoney(InvestmentVehicle investmentVehicle)
        {
            return solution.get(investmentVehicle);
        }
        
        public boolean isExact(InvestmentVehicle investmentVehicle)
        {
            return ! inexactResults.contains(investmentVehicle);
        }
        
        public boolean isAmbigous(InvestmentVehicle investmentVehicle)
        {
            return ambigousResults.contains(investmentVehicle);
        }
        
        public void markAllAsInexact(Collection<? extends InvestmentVehicle> investmentVehicles)
        {
            inexactResults.addAll(investmentVehicles);
        }
    }
    
    public Rebalancer()
    {
        investmentVehicleIndices = new HashMap<>();
        investmentVehicles = new ArrayList<>();
        constraintsToSolve = new ArrayList<>();
    }
    
    public void addConstraint(RebalancingConstraint constraint)
    {
        if(constraint.linearEquation.isEmpty())
            return; // Empty constraints carry no restrictions, we ignore them.
        verifyCurrency(constraint.targetAmount);
        for(InvestmentVehicle investmentVehicle:constraint.linearEquation.keySet())
        {
            if(!investmentVehicleIndices.containsKey(investmentVehicle))
            {
                investmentVehicleIndices.put(investmentVehicle, investmentVehicleIndices.size());
                investmentVehicles.add(investmentVehicle);
            }
        }
        constraintsToSolve.add(constraint);
    }
    
    // Better safe than sorry: Check if the user added everything in the same currency.
    private void verifyCurrency(Money money)
    {
        if(money.isZero())
        {
            // nothing to check, but if no non-zero money appears, we should use the currency code as fallback.
            fallbackCurrency = money.getCurrencyCode();
        }
        else
        {
            if (currency == null)
            {
                currency = money.getCurrencyCode();
            }
            else if (!currency.equals(money.getCurrencyCode()))
                throw new MonetaryException("Tried to add a constraint with currency " + //$NON-NLS-1$
                                money.getCurrencyCode() + "to the Rebalancer, but all " //$NON-NLS-1$
                                + "other constraints use the currency " + currency + "."); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
    
    public RebalancingSolution solve()
    {
        if (currency == null)
            currency = fallbackCurrency;
        if (constraintsToSolve.isEmpty())
        {
            // Apache commons math library does not like "empty" matrices.
            return RebalancingSolution.EMPTY_REBALANCING_SOLUTION;
        }
        int numberOfConstraints = constraintsToSolve.size();
        int numberOfInvestmentVehicles = investmentVehicleIndices.size();
        double[][] matrixArray = new double[Math.max(numberOfConstraints, numberOfInvestmentVehicles)][numberOfInvestmentVehicles];
        // We make sure that the matrix is at least as high as wide, because otherwise the Appache Commons Math library
        // does not compute the full SVD.
        // We need the full SVD, because we also want to compute the kernel to find out which values are ambiguous (if any).
        double[] vectorArray = new double[Math.max(numberOfConstraints, numberOfInvestmentVehicles)];
        for(int i = 0; i < numberOfConstraints; i++)
        {
            RebalancingConstraint constraint = constraintsToSolve.get(i);
            for(Map.Entry<InvestmentVehicle, Double> entry : constraint.linearEquation.entrySet())
            {
                int index = investmentVehicleIndices.get(entry.getKey());
                matrixArray[i][index] = entry.getValue();
            }
            vectorArray[i] = constraint.targetAmount.getAmount(); // amount is with a value with a factor,
                                                                  //but we don't divide by the factor here...
        }
        
        RealMatrix coefficients = new Array2DRowRealMatrix(matrixArray);
        RealVector target = new ArrayRealVector(vectorArray);
        
        // We use SVD, because it gives us least square solution for unsolvable linear equation systems
        // and can handle singular matrices.
        SingularValueDecomposition decomp = new SingularValueDecomposition(coefficients);
        
        DecompositionSolver solver = decomp.getSolver();
        RealVector solution = solver.solve(target);
        Map<InvestmentVehicle, Money> solutionMap = new HashMap<>();
        for(int i = 0; i < numberOfInvestmentVehicles; i++)
        {
            Money money = Money.of(currency, Math.round(solution.getEntry(i))); // ... so we don't have to multiply by the factor here.
            solutionMap.put(investmentVehicles.get(i), money);
        }
        
        // Tolerance for "being inside the kernel"
        double tol = Math.max(Math.max(numberOfConstraints, numberOfInvestmentVehicles) * decomp.getSingularValues()[0] * 0x1.0p-50,
                         Math.sqrt(Precision.SAFE_MIN));
        
        RealVector testTarget = coefficients.operate(solution);
        double matrixMax = Precision.SAFE_MIN;
        for(int i = 0; i < numberOfConstraints; i++)
            for(int j = 0; j < numberOfInvestmentVehicles; j++)
                matrixMax = Math.max(matrixMax, Math.abs(coefficients.getEntry(i, j)));
        double solutionVectorMax = Precision.SAFE_MIN;
        for(int i = 0; i < numberOfInvestmentVehicles; i++)
            solutionVectorMax = Math.max(solutionVectorMax, Math.abs(solution.getEntry(i)));
        
        // Due to too many constraints, there might be no exact solution. Find out which securities are
        // in too many constraints.
        Set<InvestmentVehicle> inexactResults = new HashSet<>();
        for(int i = 0; i < numberOfConstraints; i++)
        {
            double distance = Math.abs(testTarget.getEntry(i) - target.getEntry(i));
            if(distance >= tol * matrixMax * solutionVectorMax)
            {
                // Constraint i is not satisfied exactly. => Mark all securities in this constraint as not exact.
                for(InvestmentVehicle investmentVehicle : constraintsToSolve.get(i).linearEquation.keySet())
                    inexactResults.add(investmentVehicle);
            }
        }
        // Due to too few constraints, some results might be ambiguous. Find out which.
        int rank = decomp.getRank();
        boolean isAmbiguous = rank < investmentVehicleIndices.size();
        Set<InvestmentVehicle> ambigousResults;
        if(isAmbiguous)
        {
            ambigousResults = new HashSet<>();
            // Let the matrix M ∈ ℝ^{m × n} be our coefficient matrix, r the rank of M and  M = S Σ V* be the SVD of M:
            // Then the kernel of M is spanned by the last n - r vectors of V.
            // The i-th component of the solution vector is ambiguous iff there is a kernel vector where the i-th component is non-zero.
            RealMatrix matrixV = decomp.getV();
            List<RealVector> kernelBasis = new ArrayList<>(numberOfInvestmentVehicles - rank);
            for(int columnVectorIndexV = rank; columnVectorIndexV < numberOfInvestmentVehicles; columnVectorIndexV++)
                kernelBasis.add(matrixV.getColumnVector(columnVectorIndexV));
                
            for(int i = 0; i < numberOfInvestmentVehicles; i++)
            {
                boolean isThisAmbiguous = false;
                for(RealVector kernelBasisVector : kernelBasis)
                    isThisAmbiguous = isThisAmbiguous || Math.abs(kernelBasisVector.getEntry(i)) >= tol;
                if(isThisAmbiguous)
                    ambigousResults.add(investmentVehicles.get(i));
            }
        }
        else
            ambigousResults = Collections.emptySet();
        return new RebalancingSolution(solutionMap, ambigousResults, inexactResults);
    }
    
    /**
     * Rebalancer that works very similar to {@link Rebalancer}, but guarantees
     * that the rebalancing solution adds up to fixed value, if there is no exact solution. This is especially
     * useful when no exact rebalancing is possible, because this rebalancer
     * minimizes the mean square error while respecting the desired rebalancing
     * sum (while {@link Rebalancer} only minimizes the mean squarde error
     * without respecting the desired rebalancing sum).
     */
    public static class FixedSumRebalancer
    {
        // This rebalancer contains the unmodified constraints. It is used to
        // determine which results are ambiguous and which ones are inexact.
        private Rebalancer normalRebalancer;
        private Money totalSum;

        public FixedSumRebalancer(Money totalSum)
        {
            this.totalSum = totalSum;
            normalRebalancer = new Rebalancer();
        }

        public void addConstraint(RebalancingConstraint constraint)
        {
            normalRebalancer.addConstraint(constraint);
        }

        public RebalancingSolution solve()
        {
            RebalancingSolution normalSolution = normalRebalancer.solve();

            // This rebalancer does not contain “removedInvestmentVehicle”. The
            // result for the removedInvestementVehicle can be determined from
            // the total sum.
            Rebalancer reducedRebalancer = new Rebalancer();
            InvestmentVehicle removedInvestmentVehicle;

            if (normalSolution.inexactResults.isEmpty())
                return normalSolution; // exact solution found => Return it
            else
                removedInvestmentVehicle = normalSolution.inexactResults.iterator().next(); // Pick any inexact solution
            
            Money inexactTotalSum = totalSum;
            for(Map.Entry<InvestmentVehicle, Money> entry : normalSolution.solution.entrySet())
                if(normalSolution.isExact(entry.getKey()))
                    inexactTotalSum = inexactTotalSum.subtract(entry.getValue());

            // Re-solve all inexact solutions
            for (RebalancingConstraint oldConstraint : normalRebalancer.constraintsToSolve)
            {
                if(! Collections.disjoint(oldConstraint.linearEquation.keySet(), normalSolution.inexactResults))
                {
                    double removedInvestmentVehicleFactor = oldConstraint.linearEquation
                                    .getOrDefault(removedInvestmentVehicle, 0d);
                    Map<InvestmentVehicle, Double> newLinearEquation = new HashMap<>(oldConstraint.linearEquation);
                    newLinearEquation.remove(removedInvestmentVehicle);
                    if (removedInvestmentVehicleFactor > Precision.EPSILON)
                    {
                        for (InvestmentVehicle investmentVehicle : normalSolution.inexactResults)
                            if(investmentVehicle != removedInvestmentVehicle)
                                addToMap(newLinearEquation, investmentVehicle, - removedInvestmentVehicleFactor);
                    }
    
                    Money moneyToSubtract = Money.of(inexactTotalSum.getCurrencyCode(),
                                    Math.round(inexactTotalSum.getAmount() * removedInvestmentVehicleFactor));
                    RebalancingConstraint newConstraint = new RebalancingConstraint(newLinearEquation,
                                    oldConstraint.targetAmount.subtract(moneyToSubtract));
                    reducedRebalancer.addConstraint(newConstraint);
                }
            }

            RebalancingSolution reducedSolution = reducedRebalancer.solve();
            Money reducedSolutionSum = Money.of(normalRebalancer.currency, 0);
            for (Map.Entry<InvestmentVehicle, Money> entry : reducedSolution.solution.entrySet())
            {
                normalSolution.solution.put(entry.getKey(), entry.getValue());
                reducedSolutionSum = reducedSolutionSum.add(entry.getValue());
            }
            normalSolution.solution.put(removedInvestmentVehicle, inexactTotalSum.subtract(reducedSolutionSum));
            
            return normalSolution;
        }
    }

    private static void addToMap(Map<InvestmentVehicle, Double> map, InvestmentVehicle investmentVehicle, double weight)
    {
        map.put(investmentVehicle, map.getOrDefault(investmentVehicle, 0d) + weight);
    }
}
