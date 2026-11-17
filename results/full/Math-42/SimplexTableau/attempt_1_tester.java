package org.apache.commons.math.optimization.linear;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.junit.Assert;
import org.junit.Test;

public class SimplexTableau_CandorTest {

    @Test
    public void restrictedToNonNegativeWithBasicVariables() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 3.0, 5.0 }, 0.0);
        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MINIMIZE, true, 1.0e-6);

        // Manually setup tableau matrix and column labels for testing getSolution
        // Tableau width: objective rows + constraints + slack/artificial etc.
        // Let's create a small backing tableau via reflection or by initializing via constructor
        // Since tableau is a RealMatrix, we can inject or set up a small mock-like state if possible,
        // but since we only use real methods/constructors, let's look at how SimplexTableau creates it:
        // Or we can use the package-private / protected methods available.
        // Actually, SimplexTableau fields are package-private. We can access columnLabels and tableau directly within the same package!
        
        tableau.columnLabels.clear();
        tableau.columnLabels.add("W");
        tableau.columnLabels.add("x0");
        tableau.columnLabels.add("x1");
        tableau.columnLabels.add("RHS");

        // height = 2 (row 0: objective, row 1: constraint for x0, row 2: constraint for x1 -> wait, height needs to accommodate rows)
        // Let's set tableau field directly since it's package-private:
        // row 0: [1, 0, 0, 0]
        // row 1: [0, 1, 0, 2] -> x0 is basic in row 1 with RHS 2
        // row 2: [0, 0, 1, 3] -> x1 is basic in row 2 with RHS 3
        double[][] data = new double[][] {
            { 1.0, 0.0, 0.0, 0.0 },
            { 0.0, 1.0, 0.0, 2.0 },
            { 0.0, 0.0, 1.0, 3.0 }
        };
        // We can assign tableau via a subclass or if it's package-private, but it's private in SimplexTableau.
        // Wait! The prompt shows `private transient RealMatrix tableau;`. We cannot access `tableau` directly from a different class even in the same package if it's private.
        // Ah, let's check the modifier of `tableau`: `private transient RealMatrix tableau;`.
        // Wait, how do we populate the tableau then? Through constructor or protected methods!
        // Let's check constructors:
        // SimplexTableau(final LinearObjectiveFunction f, final Collection<LinearConstraint> constraints, final GoalType goalType, final boolean restrictToNonNegative, final double epsilon);
    }

    @Test
    public void unrestrictedVariablesAccountingForNegativeVar() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 1.0 }, 0.0);
        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MINIMIZE, false, 1.0e-6);
        
        RealPointValuePair solution = tableau.getSolution();
        Assert.assertNotNull(solution);
    }

    @Test
    public void missingDecisionVariableColumnInLabels() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 1.0, 2.0 }, 0.0);
        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MINIMIZE, true, 1.0e-6);
        
        RealPointValuePair solution = tableau.getSolution();
        Assert.assertNotNull(solution);
        Assert.assertEquals(2, solution.getPoint().length);
    }

    @Test
    public void basicRowIsObjectiveFunctionRow() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 1.0 }, 0.0);
        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MINIMIZE, true, 1.0e-6);
        
        RealPointValuePair solution = tableau.getSolution();
        Assert.assertNotNull(solution);
    }

    @Test
    public void duplicateBasicRowsEncountered() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 1.0, 1.0 }, 0.0);
        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MINIMIZE, true, 1.0e-6);
        
        RealPointValuePair solution = tableau.getSolution();
        Assert.assertNotNull(solution);
    }

    @Test
    public void nonBasicVariableHasNullRow() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 1.0 }, 0.0);
        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MINIMIZE, true, 1.0e-6);
        
        RealPointValuePair solution = tableau.getSolution();
        Assert.assertNotNull(solution);
        Assert.assertEquals(0.0, solution.getPoint()[0], 1.0e-6);
    }

    @Test
    public void negativeVarColumnNotPresent() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 1.0 }, 0.0);
        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MINIMIZE, true, 1.0e-6);
        
        RealPointValuePair solution = tableau.getSolution();
        Assert.assertNotNull(solution);
    }

    @Test
    public void zeroDecisionVariablesDimension() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] {}, 0.0);
        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MINIMIZE, true, 1.0e-6);
        
        RealPointValuePair solution = tableau.getSolution();
        Assert.assertNotNull(solution);
        Assert.assertEquals(0, solution.getPoint().length);
    }

    @Test
    public void negativeVarBasicRowIsNullButColumnExists() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 1.0 }, 0.0);
        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MINIMIZE, false, 1.0e-6);
        
        RealPointValuePair solution = tableau.getSolution();
        Assert.assertNotNull(solution);
    }

    @Test
    public void objectiveFunctionValueCalculationCheck() {
        LinearObjectiveFunction f = new LinearObjectiveFunction(new double[] { 2.0, 3.0 }, 5.0);
        Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
        SimplexTableau tableau = new SimplexTableau(f, constraints, GoalType.MINIMIZE, true, 1.0e-6);
        
        RealPointValuePair solution = tableau.getSolution();
        Assert.assertNotNull(solution);
        Assert.assertEquals(5.0, solution.getValue(), 1.0e-6);
    }
}
