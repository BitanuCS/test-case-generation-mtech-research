package org.apache.commons.math3.genetics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;
import static org.junit.Assert.*;

public class ListPopulation_CandorTest {

    // Concrete subclass of ListPopulation for testing purposes
    private static class DummyChromosome extends Chromosome {
        private final double fitness;

        public DummyChromosome(double fitness) {
            this.fitness = fitness;
        }

        @Override
        public double fitness() {
            return fitness;
        }
    }

    private static class ConcreteListPopulation extends ListPopulation {
        public ConcreteListPopulation(int populationLimit) {
            super(populationLimit);
        }

        public ConcreteListPopulation(List<Chromosome> chromosomes, int populationLimit) {
            super(chromosomes, populationLimit);
        }

        @Override
        public Population nextGeneration() {
            return null;
        }
    }

    @Test
    public void iteratorOnEmptyPopulation() {
        ListPopulation population = new ConcreteListPopulation(10);
        Iterator<Chromosome> iterator = population.iterator();
        
        assertNotNull(iterator);
        assertFalse(iterator.hasNext());
        
        try {
            iterator.next();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    @Test
    public void iteratorOnPopulatedPopulation() {
        Chromosome c1 = new DummyChromosome(1.0);
        Chromosome c2 = new DummyChromosome(2.0);
        List<Chromosome> list = new ArrayList<Chromosome>();
        list.add(c1);
        list.add(c2);

        ListPopulation population = new ConcreteListPopulation(list, 10);
        Iterator<Chromosome> iterator = population.iterator();

        assertNotNull(iterator);
        assertTrue(iterator.hasNext());
        assertSame(c1, iterator.next());
        assertTrue(iterator.hasNext());
        assertSame(c2, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void iteratorRemoveUnsupported() {
        Chromosome c1 = new DummyChromosome(1.0);
        ListPopulation population = new ConcreteListPopulation(10);
        population.addChromosome(c1);

        Iterator<Chromosome> iterator = population.iterator();
        assertTrue(iterator.hasNext());
        iterator.next();
        iterator.remove();
    }

    @Test
    public void iteratorMultipleIndependentIterators() {
        Chromosome c1 = new DummyChromosome(1.0);
        ListPopulation population = new ConcreteListPopulation(10);
        population.addChromosome(c1);

        Iterator<Chromosome> it1 = population.iterator();
        Iterator<Chromosome> it2 = population.iterator();

        assertNotSame(it1, it2);
        assertTrue(it1.hasNext());
        assertTrue(it2.hasNext());
        assertSame(c1, it1.next());
        assertSame(c1, it2.next());
    }

    @Test
    public void iteratorAfterAddingChromosome() {
        ListPopulation population = new ConcreteListPopulation(10);
        Chromosome c1 = new DummyChromosome(5.0);
        population.addChromosome(c1);

        Iterator<Chromosome> iterator = population.iterator();
        assertTrue(iterator.hasNext());
        assertSame(c1, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void iteratorReflectsCurrentState() {
        ListPopulation population = new ConcreteListPopulation(10);
        Chromosome c1 = new DummyChromosome(1.0);
        population.addChromosome(c1);

        Iterator<Chromosome> iterator = population.iterator();

        // Modify population via setChromosomes
        Chromosome c2 = new DummyChromosome(2.0);
        List<Chromosome> newList = new ArrayList<Chromosome>();
        newList.add(c2);
        population.setChromosomes(newList);

        // A new iterator should reflect the new list
        Iterator<Chromosome> newIterator = population.iterator();
        assertTrue(newIterator.hasNext());
        assertSame(c2, newIterator.next());
        assertFalse(newIterator.hasNext());
    }

    @Test
    public void iteratorHasNextConsistency() {
        ListPopulation population = new ConcreteListPopulation(10);
        population.addChromosome(new DummyChromosome(1.0));

        Iterator<Chromosome> iterator = population.iterator();
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext());

        assertSame(c1OrDummy(iterator), iterator.next());
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext());
    }

    private Chromosome c1OrDummy(Iterator<Chromosome> it) {
        // Helper just to fetch the chromosome during consistency check
        // Actually we can just call it.next() directly in test, but keeping this is fine.
        return it.next();
    }

    @Test
    public void iteratorIterationCompleteness() {
        List<Chromosome> list = Arrays.asList(
            new DummyChromosome(1.0),
            new DummyChromosome(2.0),
            new DummyChromosome(3.0)
        );
        ListPopulation population = new ConcreteListPopulation(list, 10);

        Iterator<Chromosome> iterator = population.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            assertNotNull(iterator.next());
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void iteratorOnConstructorWithList() {
        List<Chromosome> list = Arrays.asList(
            new DummyChromosome(10.0),
            new DummyChromosome(20.0)
        );
        ListPopulation population = new ConcreteListPopulation(list, 5);

        Iterator<Chromosome> iterator = population.iterator();
        assertTrue(iterator.hasNext());
        assertSame(list.get(0), iterator.next());
        assertTrue(iterator.hasNext());
        assertSame(list.get(1), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void iteratorAfterAddChromosomesCollection() {
        ListPopulation population = new ConcreteListPopulation(10);
        List<Chromosome> collection = Arrays.asList(
            new DummyChromosome(1.1),
            new DummyChromosome(2.2)
        );
        population.addChromosomes(collection);

        Iterator<Chromosome> iterator = population.iterator();
        assertTrue(iterator.hasNext());
        assertSame(collection.get(0), iterator.next());
        assertTrue(iterator.hasNext());
        assertSame(collection.get(1), iterator.next());
        assertFalse(iterator.hasNext());
    }
}
