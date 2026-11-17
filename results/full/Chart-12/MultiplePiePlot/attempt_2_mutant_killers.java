package org.jfree.chart.plot;

import java.awt.Font;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.util.RectangleEdge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.util.TableOrder;
import org.jfree.data.category.DefaultCategoryDataset;
import org.junit.Test;

public class MultiplePiePlot_CandorTest {

    @Test
    public void constructorWithNullDataset() {
        MultiplePiePlot plot = new MultiplePiePlot(null);
        assertNull(plot.getDataset());
        assertEquals(TableOrder.BY_COLUMN, plot.getDataExtractOrder());
        assertEquals(0.0, plot.getLimit(), 0.0001);
        assertNotNull(plot.getPieChart());
        assertNull(plot.getPieChart().getLegend());
        assertNotNull(plot.getPieChart().getTitle());
    }

    @Test
    public void constructorWithValidDataset() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(1.0, "Row1", "Col1");
        
        MultiplePiePlot plot = new MultiplePiePlot(dataset);
        assertEquals(dataset, plot.getDataset());
        assertEquals(TableOrder.BY_COLUMN, plot.getDataExtractOrder());
        assertEquals(0.0, plot.getLimit(), 0.0001);
        assertNotNull(plot.getPieChart());
    }

    @Test
    public void getDatasetReturnsAssignedDataset() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(5.0, "R1", "C1");
        MultiplePiePlot plot = new MultiplePiePlot(dataset);
        
        assertEquals(dataset, plot.getDataset());
    }

    @Test
    public void getDatasetReturnsNullWhenUnassigned() {
        MultiplePiePlot plot = new MultiplePiePlot(null);
        assertNull(plot.getDataset());
    }

    @Test
    public void getPieChartReturnsNonNullJFreeChart() {
        MultiplePiePlot plot = new MultiplePiePlot(null);
        JFreeChart chart = plot.getPieChart();
        assertNotNull(chart);
        assertNotNull(chart.getPlot());
    }

    @Test
    public void setPieChartUpdatesChartInstance() {
        MultiplePiePlot plot = new MultiplePiePlot(null);
        PiePlot newPiePlot = new PiePlot(null);
        JFreeChart newChart = new JFreeChart(newPiePlot);
        
        plot.setPieChart(newChart);
        assertEquals(newChart, plot.getPieChart());
    }

    @Test
    public void getDataExtractOrderDefaultValue() {
        MultiplePiePlot plot = new MultiplePiePlot(null);
        assertEquals(TableOrder.BY_COLUMN, plot.getDataExtractOrder());
    }

    @Test
    public void setDataExtractOrderUpdatesOrder() {
        MultiplePiePlot plot = new MultiplePiePlot(null);
        plot.setDataExtractOrder(TableOrder.BY_ROW);
        assertEquals(TableOrder.BY_ROW, plot.getDataExtractOrder());
    }

    @Test
    public void getLimitDefaultValue() {
        MultiplePiePlot plot = new MultiplePiePlot(null);
        assertEquals(0.0, plot.getLimit(), 0.0001);
    }

    @Test
    public void setLimitUpdatesDoubleValue() {
        MultiplePiePlot plot = new MultiplePiePlot(null);
        plot.setLimit(0.05);
        assertEquals(0.05, plot.getLimit(), 0.0001);
    }

    @Test
    public void getAggregatedItemsKeyDefaultValue() {
        MultiplePiePlot plot = new MultiplePiePlot(null);
        assertEquals("Other", plot.getAggregatedItemsKey());
    }

    @Test
    public void setAggregatedItemsKeyUpdatesKey() {
        MultiplePiePlot plot = new MultiplePiePlot(null);
        plot.setAggregatedItemsKey("Rest");
        assertEquals("Rest", plot.getAggregatedItemsKey());
    }

    @Test
    public void getAggregatedItemsPaintDefaultValue() {
        MultiplePiePlot plot = new MultiplePiePlot(null);
        assertEquals(Color.lightGray, plot.getAggregatedItemsPaint());
    }

    @Test
    public void setAggregatedItemsPaintUpdatesPaint() {
        MultiplePiePlot plot = new MultiplePiePlot(null);
        plot.setAggregatedItemsPaint(Color.DARK_GRAY);
        assertEquals(Color.DARK_GRAY, plot.getAggregatedItemsPaint());
    }

    @Test
    public void getPlotTypeReturnsExpectedString() {
        MultiplePiePlot plot = new MultiplePiePlot(null);
        assertNotNull(plot.getPlotType());
    }

    @Test
    public void equalsWithSameInstance() {
        MultiplePiePlot plot = new MultiplePiePlot(null);
        assertTrue(plot.equals(plot));
    }

    @Test
    public void equalsWithNullObject() {
        MultiplePiePlot plot = new MultiplePiePlot(null);
        assertFalse(plot.equals(null));
    }

    @Test
        public void testPieChartBackgroundPaintIsInitializedToNull() {
            MultiplePiePlot plot = new MultiplePiePlot(null);
            assertNull("Pie chart background paint should be null by default",
                    plot.getPieChart().getBackgroundPaint());
        }

    @Test
        public void testSeriesTitlePositionIsBottom() {
            MultiplePiePlot plot = new MultiplePiePlot(null);
            TextTitle title = plot.getPieChart().getTitle();
            assertEquals("Series title position should be BOTTOM",
                    RectangleEdge.BOTTOM, title.getPosition());
        }

    @Test
        public void testSeriesTitleFontPointSizeIs12() {
            MultiplePiePlot plot = new MultiplePiePlot(null);
            TextTitle title = plot.getPieChart().getTitle();
            Font font = title.getFont();
            assertEquals("Series title font size should be 12", 12, font.getSize());
        }
}
