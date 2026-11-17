package org.apache.commons.math3.distribution;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * JUnit test class for HypergeometricDistribution method getNumericalMean().
 */
public class HypergeometricDistribution_CandorTest {

    private static final double TOLERANCE = 1e-12;

    @Test
    public void testStandardParameters() {
        HypergeometricDistribution dist = new HypergeometricDistribution(10, 5, 5);
        assertEquals(2.5, dist.getNumericalMean(), TOLERANCE);
    }

    @Test
    public void testPopulationSizeEqualsOne() {
        HypergeometricDistribution dist = new HypergeometricDistribution(1, 1, 1);
        assertEquals(1.0, dist.getNumericalMean(), TOLERANCE);
    }

    @Test
    public void testZeroSuccesses() {
        HypergeometricDistribution dist = new HypergeometricDistribution(100, 0, 10);
        assertEquals(0.0, dist.getNumericalMean(), TOLERANCE);
    }

    @Test
    public void testSampleSizeEqualsPopulationSize() {
        HypergeometricDistribution dist = new HypergeometricDistribution(20, 8, 20);
        assertEquals(8.0, dist.getNumericalMean(), TOLERANCE);
    }

    @Test
    public void testLargePopulationAndSample() {
        HypergeometricDistribution dist = new HypergeometricDistribution(10000, 2500, 100);
        assertEquals(25.0, dist.getNumericalMean(), TOLERANCE);
    }

    @Test
    public void testFractionalResult() {
        HypergeometricDistribution dist = new HypergeometricDistribution(3, 1, 2);
        assertEquals(2.0 / 3.0, dist.getNumericalMean(), TOLERANCE);
    }

    @Test
    public void testMaxPossibleSuccesses() {
        HypergeometricDistribution dist = new HypergeometricDistribution(50, 50, 25);
        assertEquals(25.0, dist.getNumericalMean(), TOLERANCE);
    }

    @Test
    public void testSmallSampleSize() {
        HypergeometricDistribution dist = new HypergeometricDistribution(100, 50, 1);
        assertEquals(0.5, dist.getNumericalMean(), TOLERANCE);
    }

    @Test
    public void testSymmetricDistribution() {
        HypergeometricDistribution dist = new HypergeometricDistribution(100, 50, 50);
        assertEquals(25.0, dist.getNumericalMean(), TOLERANCE);
    }

    @Test
    public void testAsymmetricPopulation() {
        HypergeometricDistribution dist = new HypergeometricDistribution(1000, 100, 500);
        assertEquals(50.0, dist.getNumericalMean(), TOLERANCE);
    }

    @Test
    public void testPrimePopulationSize() {
        HypergeometricDistribution dist = new HypergeometricDistribution(17, 6, 5);
        double expected = 5 * (6 / (double) 17);
        assertEquals(expected, dist.getNumericalMean(), TOLERANCE);
    }
}
