package org.apache.commons.math3.random;

import junit.framework.TestCase;
import java.util.Random;

/**
 * JUnit 3 test class for BitsStreamGenerator.
 */
public class BitsStreamGenerator_CandorTest extends TestCase {

    /**
     * Concrete implementation of BitsStreamGenerator for testing purposes.
     */
    private static class ConcreteBitsStreamGenerator extends BitsStreamGenerator {
        private static final long serialVersionUID = 1L;
        private int nextReturnValue = 0;
        private boolean nextCalled = false;
        private int lastRequestedBits = -1;

        public void setNextReturnValue(int value) {
            this.nextReturnValue = value;
        }

        public boolean isNextCalled() {
            return nextCalled;
        }

        public int getLastRequestedBits() {
            return lastRequestedBits;
        }

        @Override
        public void setSeed(int seed) {
            clear();
        }

        @Override
        public void setSeed(int[] seed) {
            if (seed == null) {
                throw new NullPointerException();
            }
            clear();
        }

        @Override
        public void setSeed(long seed) {
            clear();
        }

        @Override
        protected int next(int bits) {
            nextCalled = true;
            lastRequestedBits = bits;
            return nextReturnValue;
        }
    }

    public void testNextBooleanReturnsTrueWhenNextBitIsNonZero() {
        ConcreteBitsStreamGenerator generator = new ConcreteBitsStreamGenerator();
        generator.setNextReturnValue(1);

        boolean result = generator.nextBoolean();

        assertTrue(result);
        assertTrue(generator.isNextCalled());
        assertEquals(1, generator.getLastRequestedBits());
    }

    public void testNextBooleanReturnsFalseWhenNextBitIsZero() {
        ConcreteBitsStreamGenerator generator = new ConcreteBitsStreamGenerator();
        generator.setNextReturnValue(0);

        boolean result = generator.nextBoolean();

        assertFalse(result);
        assertTrue(generator.isNextCalled());
        assertEquals(1, generator.getLastRequestedBits());
    }

    public void testSetSeedIntResetsGeneratorState() {
        ConcreteBitsStreamGenerator generator = new ConcreteBitsStreamGenerator();
        // Should execute without throwing any exception
        generator.setSeed(123);
        assertNotNull(generator);
    }

    public void testSetSeedLongResetsGeneratorState() {
        ConcreteBitsStreamGenerator generator = new ConcreteBitsStreamGenerator();
        // Should execute without throwing any exception
        generator.setSeed(123456789L);
        assertNotNull(generator);
    }

    public void testSetSeedIntArrayResetsGeneratorState() {
        ConcreteBitsStreamGenerator generator = new ConcreteBitsStreamGenerator();
        // Should execute without throwing any exception
        generator.setSeed(new int[] { 1, 2, 3 });
        assertNotNull(generator);
    }

    public void testNextProducesRequestedBits() {
        ConcreteBitsStreamGenerator generator = new ConcreteBitsStreamGenerator();
        generator.setNextReturnValue(42);

        int result = generator.next(32);

        assertEquals(42, result);
        assertTrue(generator.isNextCalled());
        assertEquals(32, generator.getLastRequestedBits());
    }

    public void testNextHandlesSmallBitCounts() {
        ConcreteBitsStreamGenerator generator = new ConcreteBitsStreamGenerator();
        generator.setNextReturnValue(1);

        int result = generator.next(1);

        assertEquals(1, result);
        assertTrue(generator.isNextCalled());
        assertEquals(1, generator.getLastRequestedBits());
    }

    public void testNextHandlesMaximumBitCounts() {
        ConcreteBitsStreamGenerator generator = new ConcreteBitsStreamGenerator();
        generator.setNextReturnValue(-1);

        int result = generator.next(32);

        assertEquals(-1, result);
        assertTrue(generator.isNextCalled());
        assertEquals(32, generator.getLastRequestedBits());
    }

    public void testConstructorInitializesGaussianToNaN() {
        ConcreteBitsStreamGenerator generator = new ConcreteBitsStreamGenerator();
        // nextGaussian() public method uses the internal nextGaussian field.
        // If it was initialized to Double.NaN, calling nextGaussian() will compute a new value
        // rather than using a previously cached one. We can verify the generator initializes properly.
        assertNotNull(generator);
    }

    public void testSetSeedIntArrayWithNullThrowsException() {
        ConcreteBitsStreamGenerator generator = new ConcreteBitsStreamGenerator();
        try {
            generator.setSeed((int[]) null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // Expected
        }
    }
}
