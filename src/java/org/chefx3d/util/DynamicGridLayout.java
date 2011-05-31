/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2006
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.util;

// External imports
import java.awt.*;

// Local Imports
// None

/**
 * A layout manager that lays out a container's components in a rectangular
 * grid, with different sizes for each row/column.
 * <p>
 * As with GridLayout one component is placed in each rectangle, but the width
 * of columns and height of rows are not necessarily the same. By default each
 * row and column will be sized in proportion to the minimum size of the largest
 * component in that row or column. Alternatively, individual rows or columns
 * can be set to a fixed percentage of the container or their minimum size with
 * various options. Components can also be set to fill the assigned area or use
 * their minimum size and align themselves within that allocated space. If the
 * grid itself doesn't completely fill the area assigned by the container there
 * are methods to set this alignment too.
 * <p>
 * Here's a simple example of using a DynamicGridLayout.
 * <p>
 * <blockquote>
 * 
 * <pre>
 * //create the layout and set it
 * setLayout(dgl = new DynamicGridLayout(2, 2, 5, 5));
 * //set any styles, sizes or alignments
 * dgl.setRowSize(0, dgl.MINIMUM);
 * //add the components
 * add(new Button(&quot;Row One/Col One&quot;));
 * add(new Button(&quot;Row One/Col Two&quot;));
 * add(new Button(&quot;Row Two/Col One&quot;));
 * add(new Button(&quot;Row Two/Col Two&quot;));
 * </pre>
 * 
 * </blockquote>
 * 
 * @author Robert Nielsen
 * @version $Revision: 1.4 $
 */
public class DynamicGridLayout implements LayoutManager, java.io.Serializable {

    /** version id */
    private static final long serialVersionUID = 1L;

    // Constants for size
    public static final int DYNAMIC = 0;

    public static final int MINIMUM = -1;

    public static final int ABOVE_MINIMUM = -2;

    public static final int BELOW_MINIMUM = -3;

    private static final int ABOVE_MARKER = -5;

    private static final int BELOW_MARKER = -6;

    // Constants for alignment
    public static final int FILL = 0;

    public static final int CENTER = 1;

    public static final int LEFT = 2;

    public static final int RIGHT = 3;

    public static final int TOP = 4;

    public static final int BOTTOM = 5;

    // Constants for style
    public static final int LABEL_FILL = 6;

    public static final int LABEL_MIN = 7;

    private int hgap;

    private int vgap;

    private int rows;

    private int cols;

    private int[] row_align;

    private int[] col_align;

    private int[] row_size;

    private int[] col_size;

    private int[] row_height;

    private int[] col_width;

    private int vert_align = CENTER;

    private int horiz_align = CENTER;

    /**
     * Creates a grid layout with a default of one column per component, in a
     * single row.
     */
    public DynamicGridLayout() {
        this(1, 0, 0, 0);
    }

    /**
     * Creates a grid layout with the specified number of rows and columns. All
     * components in the layout are given equal size.
     * <p>
     * One, but not both, of <code>rows</code> and <code>cols</code> can be
     * zero, which means that any number of objects can be placed in a row or in
     * a column.
     * 
     * @param rows the rows, with the value zero meaning any number of rows.
     * @param cols the columns, with the value zero meaning any number of
     *        columns.
     */
    public DynamicGridLayout(int rows, int cols) {
        this(rows, cols, 0, 0);
    }

    /**
     * Creates a grid layout with the specified number of rows and columns.
     * <p>
     * In addition, the horizontal and vertical gaps are set to the specified
     * values. Horizontal gaps are placed at the left and right edges, and
     * between each of the columns. Vertical gaps are placed at the top and
     * bottom edges, and between each of the rows.
     * <p>
     * One, but not both, of <code>rows</code> and <code>cols</code> can be
     * zero, which means that any number of objects can be placed in a row or in
     * a column.
     * 
     * @param rows the rows, with the value zero meaning any number of rows.
     * @param cols the columns, with the value zero meaning any number of
     *        columns.
     * @param hgap the horizontal gap.
     * @param vgap the vertical gap.
     * @exception IllegalArgumentException if the of <code>rows</code> or
     *            <code>cols</code> is invalid.
     */
    public DynamicGridLayout(int rows, int cols, int hgap, int vgap) {
        if ((rows == 0) && (cols == 0))
            throw new IllegalArgumentException(
                    "rows and cols cannot both be zero");
        this.rows = rows;
        this.cols = cols;
        this.hgap = hgap;
        this.vgap = vgap;
        resetGrid();
    }

    /**
     * Gets the number of rows in this layout.
     * 
     * @return the number of rows in this layout.
     */
    public int getRows() {
        return rows;
    }

    /**
     * Sets the number of rows in this layout to the specified value.
     * 
     * @param rows the number of rows in this layout.
     * @exception IllegalArgumentException if the value of both
     *            <code>rows</code> and <code>cols</code> is set to zero.
     */
    public void setRows(int rows) {
        if ((rows == 0) && (this.cols == 0))
            throw new IllegalArgumentException(
                    "rows and cols cannot both be zero");
        this.rows = rows;
        resetGrid();
    }

    /**
     * Gets the number of columns in this layout.
     * 
     * @return the number of columns in this layout.
     * @since JDK1.1
     */
    public int getColumns() {
        return cols;
    }

    /**
     * Sets the number of columns in this layout to the specified value.
     * 
     * @param cols the number of columns in this layout.
     * @exception IllegalArgumentException if the value of both
     *            <code>rows</code> and <code>cols</code> is set to zero.
     */
    public void setColumns(int cols) {
        if ((cols == 0) && (this.rows == 0))
            throw new IllegalArgumentException(
                    "rows and cols cannot both be zero");
        this.cols = cols;
        resetGrid();
    }

    /**
     * Gets the horizontal gap between components.
     * 
     * @return the horizontal gap between components.
     */
    public int getHgap() {
        return hgap;
    }

    /**
     * Sets the horizontal gap between components to the specified value.
     * 
     * @param hgap the horizontal gap between components.
     */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /**
     * Gets the vertical gap between components.
     * 
     * @return the vertical gap between components.
     */
    public int getVgap() {
        return vgap;
    }

    /**
     * Sets the vertical gap between components to the specified value.
     * 
     * @param vgap the vertical gap between components.
     */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    /**
     * Resets the grid parameters. All styles, alignments and sizes are reset to
     * their default values.
     */
    public void resetGrid() {
        row_align = new int[rows];
        col_align = new int[cols];
        row_size = new int[rows];
        col_size = new int[cols];
    }

    /**
     * Sets the style of this grid. This provides a shortcut to producing
     * several standard styles of grid layout by setting various row alignments
     * and sizes. The style can be tweaked by using the set(Row|Column)Alignment
     * and set(Row|Column)Size methods after a call to this one.
     * <p>
     * The style parameter has the following parameters:
     * <ul>
     * <li>FILL to use all available space, with component heights and widths
     * relative to their preferred size.
     * <li>CENTER to use the same spacing as FILL but with each component being
     * it's preferred size and centered in the space.
     * <li>LABEL_FILL is a simple layout where the 1st, 3rd, etc. columns are
     * considered to be labels and the intervening ones are the data components
     * (TextFields, ComboBoxes). The labels are minimum size and right aligned
     * and the others fill all available space.
     * <li>LABEL_MIN is the same as LABEL_FILL but the data components are
     * minimum too and aligned left.
     * </ul>
     * 
     * @param style - the style to be set.
     */
    public void setStyle(int style) {
        switch (style) {
        case FILL:
            setColumnAlignments(0, cols - 1, FILL);
            setRowAlignments(0, rows - 1, FILL);
            setColumnSizes(0, cols - 1, DYNAMIC);
            setRowSizes(0, rows - 1, DYNAMIC);
            break;
        case CENTER:
            setColumnAlignments(0, cols - 1, CENTER);
            setRowAlignments(0, rows - 1, CENTER);
            setColumnSizes(0, cols - 1, DYNAMIC);
            setRowSizes(0, rows - 1, DYNAMIC);
            break;
        case LABEL_FILL:
        case LABEL_MIN:
            for (int i = 0; i < cols; i += 2) {
                setColumnAlignment(i, RIGHT);
                setColumnSize(i, MINIMUM);
                if (style == LABEL_FILL) {
                    setColumnAlignment(i + 1, DYNAMIC);
                    setColumnSize(i + 1, FILL);
                } else {
                    setColumnAlignment(i + 1, LEFT);
                    setColumnSize(i + 1, BELOW_MINIMUM);
                }
            }
            break;
        }
    }

    /**
     * This method is only required if there are no columns with dynamic widths
     * which may cause the layout to be smaller than the allocated space. The
     * horizontal alignment of the grid in this space can then be specified
     * using this method. The default is centered. The available alignments are:
     * <ul>
     * <li>LEFT - align the grid to the left of the space available.
     * <li>CENTER - center the grid in the space.
     * <li>RIGHT - align the grid to the right of the space available.
     * </ul>
     * 
     * @param value the alignment to set
     */
    public void setHorizontalAlignment(int value) {
        horiz_align = value;
    }

    /**
     * This method is only required if there are no rows with dynamic heights
     * which may cause the layout to be smaller than the allocated space. The
     * vertical alignment of the grid in this space can then be specified using
     * this method. The default is centered. The available alignments are:
     * <ul>
     * <li>TOP - align the grid to the top of the space available.
     * <li>CENTER - center the grid in the space.
     * <li>BOTTOM - align the grid to the bottom of the space available.
     * </ul>
     * 
     * @param value the alignment to set
     */
    public void setVerticalAlignment(int value) {
        vert_align = value;
    }

    /**
     * Sets the alignment for a particular row. If the alignment is not FILL,
     * the preferred size of the component will be used unless it is greater
     * than the available space, in which case it will be truncated. The default
     * alignment is FILL for all rows. The available alignments are:
     * <ul>
     * <li>FILL - fill the entire vertical space available.
     * <li>TOP - align to the top of the space available.
     * <li>CENTER - center the component in the space.
     * <li>BOTTOM - align with the bottom of the space available.
     * </ul>
     * 
     * @param row - the row to set the alignment for
     * @param value - the alignment to set.
     */
    public void setRowAlignment(int row, int value) {
        row_align[row] = value;
    }

    /**
     * Sets the alignment of multiple rows. See setRowAlignment for details of
     * alignment values.
     * 
     * @param start the inclusive start row to set
     * @param end the inclusive end row to set
     * @param value the alignment to set
     * @see #setRowAlignment
     */
    public void setRowAlignments(int start, int end, int value) {
        for (int i = start; i <= end; i++)
            row_align[i] = value;
    }

    /**
     * Sets the alignment for a particular column. If the alignment is not FILL,
     * the preferred size of the component will be used unless it is greater
     * than the available space, in which case it will be truncated. The default
     * alignment is FILL for all columns. The available alignments are:
     * <ul>
     * <li>FILL - fill the entire vertical space available.
     * <li>LEFT - align to the left of the space available.
     * <li>CENTER - center the component in the space.
     * <li>RIGHT - align to the right of the space available.
     * </ul>
     * 
     * @param col - the column to set the alignment for
     * @param value - the alignment to set.
     */
    public void setColumnAlignment(int col, int value) {
        col_align[col] = value;
    }

    /**
     * Sets the alignment of multiple columns. See setColumnAlignment for
     * details of alignment values.
     * 
     * @param start the inclusive start column to set
     * @param end the inclusive end column to set
     * @param value the alignment to set
     * @see #setColumnAlignment
     */
    public void setColumnAlignments(int start, int end, int value) {
        for (int i = start; i <= end; i++)
            col_align[i] = value;
    }

    /**
     * Sets the size of a row. The available sizes are:
     * <ul>
     * <li>percentage - a value between 1 and 100 for the percentage of the
     * width of the grid that this row should take up.
     * <li>MINIMUM - make sure that the height of this row is always the
     * minimum height of of the largest component in the row no matter what it
     * does to the formatting. It can look ugly if you resize the window too
     * small but is good for labels which must always display all their text.
     * <li>DYNAMIC - After the percentages and minumum components are removed
     * from the grid width the rows with dynamic sizes are allocated a
     * proportion of the remaining space based on the minimum height of the
     * largest component in the row. This is the default size for all rows.
     * <li>BELOW_MINIMUM - the height is set to MINIMUM unless this minimum
     * size would be smaller than a dynamically allocated one, in which case
     * DYNAMIC allocation is used. This setting is useful if you want a
     * component to stick to it's minimum size in most situations but go even
     * smaller if the panel size gets really small.
     * <li>ABOVE_MINIMUM - the height is allocated using the DYNAMIC setting
     * unless it would make the component smaller than it's minimum size, where
     * MINIMUM is used. This option is useful if you want a component to take up
     * any available space but never go smaller than it's minumum size.
     * </ul>
     */
    public void setRowSize(int row, int value) {
        row_size[row] = value;
    }

    /**
     * Sets the size of multiple rows. See setRowSize for details of alignment
     * values.
     * 
     * @param start the inclusive start row to set
     * @param end the inclusive end row to set
     * @param value the size to set
     * @see #setRowSize
     */
    public void setRowSizes(int start, int end, int value) {
        for (int i = start; i <= end; i++)
            row_size[i] = value;
    }

    /**
     * Sets the size of a column. The available sizes are:
     * <ul>
     * <li>percentage - a value between 1 and 100 for the percentage of the
     * width of the grid that this column should take up.
     * <li>MINIMUM - make sure that the width of this column is always the
     * minimum width of the largest component in the column no matter what it
     * does to the formatting. It can look ugly if you resize the window too
     * small but is good for labels which must always display all their text.
     * <li>DYNAMIC - After the percentages and minumum components are removed
     * from the grid width the columns with dynamic sizes are allocated a
     * proportion of the remaining space based on the minimum width of the
     * largest component in the row. This is the default size for all column.
     * <li>BELOW_MINIMUM - the height is set to MINIMUM unless this minimum
     * size would be smaller than a dynamically allocated one, in which case
     * DYNAMIC allocation is used. This setting is useful if you want a
     * component to stick to it's minimum size in most situations but go even
     * smaller if the panel size gets really small.
     * <li>ABOVE_MINIMUM - the width is allocated using the DYNAMIC setting
     * unless it would make the component smaller than it's minimum size, where
     * MINIMUM is used. This option is useful if you want a component to take up
     * any available space but never go smaller than it's minumum size.
     * </ul>
     * 
     * @param col - the column to set the size for
     * @param value - the size to set.
     */
    public void setColumnSize(int col, int value) {
        col_size[col] = value;
    }

    /**
     * Sets the size of multiple columns. See setColumnSize for details of
     * alignment values.
     * 
     * @param start the inclusive start column to set
     * @param end the inclusive end column to set
     * @param value the size to set
     * @see #setColumnSize
     */
    public void setColumnSizes(int start, int end, int value) {
        for (int i = start; i <= end; i++)
            col_size[i] = value;
    }

    /**
     * Adds the specified component with the specified name to the layout.
     * 
     * @param name the name of the component.
     * @param comp the component to be added.
     */
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * Removes the specified component from the layout.
     * 
     * @param comp the component to be removed.
     */
    public void removeLayoutComponent(Component comp) {
    }

    /**
     * Calculate the largest either minimum or preferred size for the components
     * in each row and column.
     * 
     * @param parent the container of all the components to be laid out.
     * @param prefered TRUE to use preferred size, FALSE to use minimum size
     */
    private void getWidthsAndHeights(Container parent, boolean preferred) {
        int ncomponents = parent.getComponentCount();
        col_width = new int[cols];
        row_height = new int[rows];

        for (int i = 0; i < ncomponents && i < rows * cols; i++) {
            Component comp = parent.getComponent(i);
            Dimension d;
            if (preferred)
                d = comp.getPreferredSize();
            else
                d = comp.getMinimumSize();
            if (d.width > col_width[i % cols])
                col_width[i % cols] = d.width;
            if (d.height > row_height[i / cols])
                row_height[i / cols] = d.height;
        }
    }

    private int getMaximumSize(int[] perc, int[] min) {
        int perc_sum = 0;
        int leftover_sum = 0;
        int temp;
        int max = 0;
        int size = perc.length;
        for (int i = 0; i < size; i++) {
            if (perc[i] > 0) {
                temp = min[i] * 100 / perc[i];
                if (temp > max)
                    max = temp;
                perc_sum += min[i];
            } else
                leftover_sum += min[i];
        }
        temp = leftover_sum * 100 / (100 - perc_sum);
        if (temp > max)
            max = temp;
        return max;
    }

    /**
     * Given an array of sizes, calculate the grid positions. This method is
     * called both with the row sizes and the column sizes.
     * 
     * @param len the length of the space we need to allocate
     * @param perce the sizes of each row or column
     * @param value the array to place the calculated positions into.
     */
    private int spaceOut(int len, int[] perc, int[] value) {
        int sum = 0; // sum of all dynamic values
        int dyn_cnt = 0; // a count of the number of dynamic entries
        int size = perc.length; // the number of grid squares
        int space_left = len; // the remaining space to allocate between the
                                // dynamics
        boolean check_above_minimum = false; // efficiency flags
        boolean check_below_minimum = false;
        for (int i = 0; i < size; i++) {
            if (perc[i] > 0) // a percentage
            {
                value[i] = len * perc[i] / 100;
                space_left -= value[i];
            } else if (perc[i] == DYNAMIC || perc[i] == ABOVE_MINIMUM
                    || perc[i] == ABOVE_MARKER) {
                if (perc[i] == ABOVE_MARKER)
                    perc[i] = ABOVE_MINIMUM;
                if (perc[i] != DYNAMIC)
                    check_above_minimum = true;
                sum += value[i];
                dyn_cnt++;
            } else // minimum, below_minumum, or below_marker
            {
                if (perc[i] == BELOW_MARKER) // reset the marker
                    perc[i] = BELOW_MINIMUM;
                if (perc[i] != MINIMUM)
                    check_below_minimum = true;
                space_left -= value[i]; // lower the available space
            }
        }
        if (check_below_minimum)
            for (int i = 0; i < size; i++)
                if ((perc[i] == BELOW_MINIMUM)
                        && (space_left + value[i]) / (sum + value[i]) < 1) {
                    perc[i] = BELOW_MARKER;
                    sum += value[i];
                    space_left += value[i];
                    dyn_cnt++;
                }
        if (check_above_minimum)
            for (int i = 0; i < size; i++) {
                if ((perc[i] == ABOVE_MINIMUM)
                        && (space_left - value[i]) / (sum - value[i]) < 1) {
                    perc[i] = ABOVE_MARKER;
                    sum -= value[i];
                    space_left -= value[i];
                    dyn_cnt--;
                }
            }
        if (dyn_cnt > 0) {
            int leftover = space_left;
            int lastdyn = -1; // this had better be changed or there is
                                // something wrong with cnt
            for (int i = 0; i < size; i++)
                if (perc[i] == DYNAMIC || perc[i] == BELOW_MARKER
                        || perc[i] == ABOVE_MINIMUM) {
                    if (sum == 0)
                        value[i] = 0;
                    else
                        value[i] = Math.round(value[i] * space_left / sum);
                    leftover -= value[i];
                    lastdyn = i;

                }
            value[lastdyn] += leftover; // if there is any leftovers give it to
                                        // the last dynamic one
            return 0;
        }
        if (space_left < 0)
            return 0;
        else
            return space_left;
    }

    private void placeComponent(Component c, int x, int y, int w, int h,
            int ra, int ca) {
        Dimension pref = c.getPreferredSize();
        if (ra != FILL && pref.height < h) {
            if (ra == CENTER)
                y += Math.round((h - pref.height) / 2.0);
            else if (ra == BOTTOM)
                y += (h - pref.height);
            h = pref.height;
        }
        if (ca != FILL && pref.width < w) {
            if (ca == CENTER)
                x += Math.round((w - pref.width) / 2.0);
            else if (ca == RIGHT)
                x += (w - pref.width);
            w = pref.width;
        }
        c.setBounds(x, y, w, h);
    }

    /**
     * Determines the preferred size of the container argument using this grid
     * layout.
     * <p>
     * The preferred width of a grid layout is the largest preferred width of
     * any of the widths in the container times the number of columns, plus the
     * horizontal padding times the number of columns plus one, plus the left
     * and right insets of the target container.
     * <p>
     * The preferred height of a grid layout is the largest preferred height of
     * any of the heights in the container times the number of rows, plus the
     * vertical padding times the number of rows plus one, plus the top and
     * bottom insets of the target container.
     * 
     * @param parent the container in which to do the layout.
     * @return the preferred dimensions to lay out the subcomponents of the
     *         specified container.
     * @see java.awt.GridLayout#minimumLayoutSize
     * @see java.awt.Container#getPreferredSize()
     */
    public Dimension preferredLayoutSize(Container parent) {
        getWidthsAndHeights(parent, true);
        int w = getMaximumSize(col_size, col_width);
        int h = getMaximumSize(row_size, row_height);
        Insets insets = parent.getInsets();
        return new Dimension(
                insets.left + insets.right + w + (cols - 1) * hgap, insets.top
                        + insets.bottom + h + (rows - 1) * vgap);
    }

    /**
     * Determines the minimum size of the container argument using this grid
     * layout.
     * <p>
     * The minimum width of a grid layout is the largest minimum width of any of
     * the widths in the container times the number of columns, plus the
     * horizontal padding times the number of columns plus one, plus the left
     * and right insets of the target container.
     * <p>
     * The minimum height of a grid layout is the largest minimum height of any
     * of the heights in the container times the number of rows, plus the
     * vertical padding times the number of rows plus one, plus the top and
     * bottom insets of the target container.
     * 
     * @param parent the container in which to do the layout.
     * @return the minimum dimensions needed to lay out the subcomponents of the
     *         specified container.
     * @see java.awt.GridLayout#preferredLayoutSize
     * @see java.awt.Container#doLayout
     */
    public Dimension minimumLayoutSize(Container parent) {
        getWidthsAndHeights(parent, false);
        int w = getMaximumSize(col_size, col_width);
        int h = getMaximumSize(row_size, row_height);
        Insets insets = parent.getInsets();
        return new Dimension(
                insets.left + insets.right + w + (cols - 1) * hgap, insets.top
                        + insets.bottom + h + (rows - 1) * vgap);
    }

    /**
     * Lays out the specified container using this layout.
     * <p>
     * This method reshapes the components in the specified target container in
     * order to satisfy the constraints of the <code>GridLayout</code> object.
     * <p>
     * The grid layout manager determines the size of individual components by
     * dividing the free space in the container into equal-sized portions
     * according to the number of rows and columns in the layout. The
     * container's free space equals the container's size minus any insets and
     * any specified horizontal or vertical gap. All components in a grid layout
     * are given the same size.
     * 
     * @param parent the container in which to do the layout.
     * @see java.awt.Container
     * @see java.awt.Container#doLayout
     */
    public void layoutContainer(Container parent) {
        Insets insets = parent.getInsets();
        int ncomponents = parent.getComponentCount();
        Dimension p = parent.getSize();

        int w = p.width - (insets.left + insets.right);
        int h = p.height - (insets.top + insets.bottom);
        w = (w - (cols - 1) * hgap);
        h = (h - (rows - 1) * vgap);
        getWidthsAndHeights(parent, true);

        int row_leftover = spaceOut(h, row_size, row_height);
        int col_leftover = spaceOut(w, col_size, col_width);
        int col_indent = 0;
        switch (horiz_align) {
        case CENTER:
            col_indent = (int) Math.round(col_leftover / 2.0);
            break;
        case RIGHT:
            col_indent = col_leftover;
            break;
        }

        int row_indent = 0;
        switch (vert_align) {
        case CENTER:
            row_indent = (int) Math.round(row_leftover / 2.0);
            break;
        case BOTTOM:
            row_indent = row_leftover;
            break;
        }

        for (int c = 0, x = insets.left + col_indent; c < cols; x += col_width[c++]
                + hgap) {
            for (int r = 0, y = insets.top + row_indent; r < rows; y += row_height[r++]
                    + vgap) {
                int i = r * cols + c;
                if (i < ncomponents)
                    placeComponent(parent.getComponent(i), x, y, col_width[c],
                            row_height[r], row_align[r], col_align[c]);
            }
        }
    }

    /*
     * public void dump() { System.out.print("Rows: "); for(int i=0;i<rows;i++)
     * System.out.print(row_height[i]+","); System.out.print("\nCols: ");
     * for(int i=0;i<cols;i++) System.out.print(col_width[i]+",");
     * System.out.println(); }
     */

    /**
     * Returns the string representation of this grid layout's values.
     * 
     * @return a string representation of this grid layout.
     */
    public String toString() {
        return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap
                + ",rows=" + rows + ",cols=" + cols + "]";
    }
}
