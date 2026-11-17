package org.jfree.chart.block;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import org.jfree.chart.util.RectangleEdge;
import org.jfree.chart.util.Size2D;
import org.junit.Test;

/**
 * JUnit 4 test class for BorderArrangement.arrangeFF method.
 */
public class BorderArrangement_CandorTest {

    // Helper stub for Block
    private static class TestBlock implements Block {
        private Size2D arrangedSize = new Size2D(10.0, 10.0);
        private Rectangle2D bounds;
        private String id;

        public TestBlock() {}

        public TestBlock(double width, double height) {
            this.arrangedSize = new Size2D(width, height);
        }

        @Override
        public Size2D arrange(Graphics2D g2, RectangleConstraint constraint) {
            return arrangedSize;
        }

        @Override
        public Size2D arrange(Graphics2D g2) {
            return arrangedSize;
        }

        @Override
        public Rectangle2D getBounds() {
            return bounds;
        }

        @Override
        public void setBounds(Rectangle2D bounds) {
            this.bounds = bounds;
        }

        @Override
        public String getID() {
            return this.id;
        }

        @Override
        public void setID(String id) {
            this.id = id;
        }

        // Unused interface methods
        @Override
        public void draw(Graphics2D g2, Rectangle2D area) {}
        @Override
        public Object draw(Graphics2D g2, Rectangle2D area, Object params) {
            return null;
        }
        @Override
        public double getHeight() { return arrangedSize.height; }
        @Override
        public double getWidth() { return arrangedSize.width; }
    }

    // Helper subclass to expose protected arrangeFF method for testing
    private static class TestableBorderArrangement extends BorderArrangement {
        @Override
        public Size2D arrangeFF(BlockContainer container, Graphics2D g2, RectangleConstraint constraint) {
            return super.arrangeFF(container, g2, constraint);
        }
    }

    @Test
    public void arrangeFFWithAllBlocksPresent() {
        TestableBorderArrangement arrangement = new TestableBorderArrangement();
        BlockContainer container = new BlockContainer(arrangement);

        TestBlock top = new TestBlock(500.0, 50.0);
        TestBlock bottom = new TestBlock(500.0, 40.0);
        TestBlock left = new TestBlock(100.0, 310.0);
        TestBlock right = new TestBlock(80.0, 310.0);
        TestBlock center = new TestBlock(320.0, 310.0);

        container.add(top, RectangleEdge.TOP);
        container.add(bottom, RectangleEdge.BOTTOM);
        container.add(left, RectangleEdge.LEFT);
        container.add(right, RectangleEdge.RIGHT);
        container.add(center, null);

        RectangleConstraint constraint = new RectangleConstraint(500.0, 400.0);
        Size2D result = arrangement.arrangeFF(container, null, constraint);

        assertNotNull(result);
        assertEquals(500.0, result.width, 0.001);
        assertEquals(400.0, result.height, 0.001);

        assertEquals(0.0, top.getBounds().getX(), 0.001);
        assertEquals(0.0, top.getBounds().getY(), 0.001);
        assertEquals(500.0, top.getBounds().getWidth(), 0.001);
        assertEquals(50.0, top.getBounds().getHeight(), 0.001);
    }

    @Test
    public void arrangeFFWithOnlyCenterBlock() {
        TestableBorderArrangement arrangement = new TestableBorderArrangement();
        BlockContainer container = new BlockContainer(arrangement);

        TestBlock center = new TestBlock(300.0, 200.0);
        container.add(center, null);

        RectangleConstraint constraint = new RectangleConstraint(300.0, 200.0);
        Size2D result = arrangement.arrangeFF(container, null, constraint);

        assertNotNull(result);
        assertEquals(300.0, result.width, 0.001);
        assertEquals(200.0, result.height, 0.001);

        assertNotNull(center.getBounds());
        assertEquals(0.0, center.getBounds().getX(), 0.001);
        assertEquals(0.0, center.getBounds().getY(), 0.001);
        assertEquals(300.0, center.getBounds().getWidth(), 0.001);
        assertEquals(200.0, center.getBounds().getHeight(), 0.001);
    }

    @Test
    public void arrangeFFWithEmptyContainer() {
        TestableBorderArrangement arrangement = new TestableBorderArrangement();
        BlockContainer container = new BlockContainer(arrangement);

        RectangleConstraint constraint = new RectangleConstraint(100.0, 100.0);
        Size2D result = arrangement.arrangeFF(container, null, constraint);

        assertNotNull(result);
        assertEquals(100.0, result.width, 0.001);
        assertEquals(100.0, result.height, 0.001);
    }

    @Test
    public void arrangeFFWithTopAndBottomBlocksOnly() {
        TestableBorderArrangement arrangement = new TestableBorderArrangement();
        BlockContainer container = new BlockContainer(arrangement);

        TestBlock top = new TestBlock(400.0, 60.0);
        TestBlock bottom = new TestBlock(400.0, 40.0);

        container.add(top, RectangleEdge.TOP);
        container.add(bottom, RectangleEdge.BOTTOM);

        RectangleConstraint constraint = new RectangleConstraint(400.0, 300.0);
        Size2D result = arrangement.arrangeFF(container, null, constraint);

        assertNotNull(result);
        assertEquals(400.0, result.width, 0.001);
        assertEquals(300.0, result.height, 0.001);

        assertEquals(0.0, top.getBounds().getY(), 0.001);
        assertEquals(60.0 + (300.0 - 60.0 - 40.0), bottom.getBounds().getY(), 0.001);
    }

    @Test
    public void arrangeFFWithLeftAndRightBlocksOnly() {
        TestableBorderArrangement arrangement = new TestableBorderArrangement();
        BlockContainer container = new BlockContainer(arrangement);

        TestBlock left = new TestBlock(150.0, 400.0);
        TestBlock right = new TestBlock(100.0, 400.0);

        container.add(left, RectangleEdge.LEFT);
        container.add(right, RectangleEdge.RIGHT);

        RectangleConstraint constraint = new RectangleConstraint(600.0, 400.0);
        Size2D result = arrangement.arrangeFF(container, null, constraint);

        assertNotNull(result);
        assertEquals(600.0, result.width, 0.001);
        assertEquals(400.0, result.height, 0.001);

        assertEquals(0.0, left.getBounds().getX(), 0.001);
        assertEquals(500.0, right.getBounds().getX(), 0.001);
    }

    @Test
    public void arrangeFFWithZeroConstraintDimensions() {
        TestableBorderArrangement arrangement = new TestableBorderArrangement();
        BlockContainer container = new BlockContainer(arrangement);

        TestBlock center = new TestBlock(0.0, 0.0);
        container.add(center, null);

        RectangleConstraint constraint = new RectangleConstraint(0.0, 0.0);
        Size2D result = arrangement.arrangeFF(container, null, constraint);

        assertNotNull(result);
        assertEquals(0.0, result.width, 0.001);
        assertEquals(0.0, result.height, 0.001);
        assertEquals(0.0, center.getBounds().getWidth(), 0.001);
        assertEquals(0.0, center.getBounds().getHeight(), 0.001);
    }

    @Test
    public void arrangeFFWithRightBlockWidthExceedingRemainingWidth() {
        TestableBorderArrangement arrangement = new TestableBorderArrangement();
        BlockContainer container = new BlockContainer(arrangement);

        TestBlock left = new TestBlock(400.0, 300.0);
        TestBlock right = new TestBlock(300.0, 300.0);

        container.add(left, RectangleEdge.LEFT);
        container.add(right, RectangleEdge.RIGHT);

        RectangleConstraint constraint = new RectangleConstraint(500.0, 300.0);
        Size2D result = arrangement.arrangeFF(container, null, constraint);

        assertNotNull(result);
        assertEquals(500.0, result.width, 0.001);
        assertEquals(300.0, result.height, 0.001);
        assertEquals(100.0, right.getBounds().getWidth(), 0.001);
    }

    @Test
    public void arrangeFFWithTopBlockConsumingFullHeight() {
        TestableBorderArrangement arrangement = new TestableBorderArrangement();
        BlockContainer container = new BlockContainer(arrangement);

        TestBlock top = new TestBlock(400.0, 300.0);
        TestBlock bottom = new TestBlock(400.0, 0.0);

        container.add(top, RectangleEdge.TOP);
        container.add(bottom, RectangleEdge.BOTTOM);

        RectangleConstraint constraint = new RectangleConstraint(400.0, 300.0);
        Size2D result = arrangement.arrangeFF(container, null, constraint);

        assertNotNull(result);
        assertEquals(400.0, result.width, 0.001);
        assertEquals(300.0, result.height, 0.001);
        assertEquals(300.0, bottom.getBounds().getY(), 0.001);
    }

    @Test(expected = NullPointerException.class)
    public void arrangeFFWithNullConstraintThrowsException() {
        TestableBorderArrangement arrangement = new TestableBorderArrangement();
        BlockContainer container = new BlockContainer(arrangement);
        TestBlock center = new TestBlock(100.0, 100.0);
        container.add(center, null);

        arrangement.arrangeFF(container, null, null);
    }

    @Test
    public void arrangeFFVerifyCenterBlockBoundsPositioning() {
        TestableBorderArrangement arrangement = new TestableBorderArrangement();
        BlockContainer container = new BlockContainer(arrangement);

        TestBlock top = new TestBlock(400.0, 50.0);
        TestBlock left = new TestBlock(100.0, 250.0);
        TestBlock center = new TestBlock(300.0, 250.0);

        container.add(top, RectangleEdge.TOP);
        container.add(left, RectangleEdge.LEFT);
        container.add(center, null);

        RectangleConstraint constraint = new RectangleConstraint(400.0, 300.0);
        arrangement.arrangeFF(container, null, constraint);

        assertEquals(100.0, center.getBounds().getX(), 0.001);
        assertEquals(50.0, center.getBounds().getY(), 0.001);
        assertEquals(300.0, center.getBounds().getWidth(), 0.001);
        assertEquals(250.0, center.getBounds().getHeight(), 0.001);
    }

    @Test
    public void arrangeFFVerifyRightBlockBoundsPositioning() {
        TestableBorderArrangement arrangement = new TestableBorderArrangement();
        BlockContainer container = new BlockContainer(arrangement);

        TestBlock top = new TestBlock(500.0, 50.0);
        TestBlock left = new TestBlock(100.0, 250.0);
        TestBlock right = new TestBlock(80.0, 250.0);

        container.add(top, RectangleEdge.TOP);
        container.add(left, RectangleEdge.LEFT);
        container.add(right, RectangleEdge.RIGHT);

        RectangleConstraint constraint = new RectangleConstraint(500.0, 300.0);
        arrangement.arrangeFF(container, null, constraint);

        assertEquals(420.0, right.getBounds().getX(), 0.001);
        assertEquals(50.0, right.getBounds().getY(), 0.001);
        assertEquals(80.0, right.getBounds().getWidth(), 0.001);
        assertEquals(250.0, right.getBounds().getHeight(), 0.001);
    }
}
