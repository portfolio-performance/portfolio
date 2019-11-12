package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.HashMap;
import java.util.Map;


import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Attributes;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.CurrencyToStringConverter;
import name.abuchen.portfolio.ui.util.NumberVerifyListener;
import name.abuchen.portfolio.ui.util.StringToCurrencyConverter;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.ValueEditingSupport;

public class ExpectedReturnsAttachedModel implements TaxonomyModel.AttachedModel
{
    public static final String KEY_EXPECTED_RETURN = "expected-return"; //$NON-NLS-1$
    public static final String KEY_ER_IN_USE = "expected-return-in-use"; //$NON-NLS-1$

    private TaxonomyModel taxonomyModel;

    private int getExpectedReturnFor(TaxonomyNode node)
    {
        Integer expectedReturn = (Integer) node.getData(KEY_EXPECTED_RETURN);
        return expectedReturn == null ? 0 : expectedReturn.intValue();
    }

    private void setExpectedReturnFor(TaxonomyNode node, int expectedReturn)
    {
        node.setData(KEY_EXPECTED_RETURN, Integer.valueOf(expectedReturn));
    }

    public Boolean getIsERinUse(TaxonomyNode node)
    {
        Boolean inUse = (Boolean) node.getData(KEY_ER_IN_USE); 
        return inUse == null ? false : inUse;
    }

    public void setIsERinUse(TaxonomyNode node, Boolean isERinUse)
    {
        node.setData(KEY_ER_IN_USE, isERinUse);
    }
 
   
    @Override
    public void setup(TaxonomyModel model)
    {
        taxonomyModel = model; 
    }
    
    // Method that is triggered when the user modifies a value in the "expected returns" column
    public void onERModified(Object element, Object newValue, Object oldValue)
    {
        TaxonomyNode node = (TaxonomyNode) element;
        // Trigger recalculation of affected expected returns, in the model
        recalcExpectedReturns(node);
    
        recalculate(this.taxonomyModel);
        taxonomyModel.fireTaxonomyModelChange(node);
        taxonomyModel.markDirty();
    }
    

    // Adds a column where you can enter your expected return for this asset class (or security)
    @Override
    public void addColumns(ShowHideColumnHelper columns)
    {
        Column column = new Column("expectedReturn", Messages.ColumnExpectedReturn, SWT.RIGHT, 100); //$NON-NLS-1$
        column.setMenuLabel(Messages.ColumnExpectedReturn_MenuLabel);
        column.setDescription(Messages.ColumnExpectedReturn_Description);
        column.setLabelProvider(new StyledCellLabelProvider()
        {
            private  Styler strikeoutStyler = new Styler()
            {
                @Override
                public void applyStyles(TextStyle textStyle)
                {
                    textStyle.strikeout = true;
                    textStyle.foreground = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
                }
            };

            @Override
            public void update(final ViewerCell cell) {
                TaxonomyNode node = (TaxonomyNode) cell.getElement();

                String erText = Values.Percent_ER.format(getExpectedReturnFor(node));
                // If node is not in use, print percentage in grey and strikethrough
                StyledString styledString = new StyledString(erText, getIsERinUse(node) ? null : strikeoutStyler);
                cell.setText(styledString.getString());
                cell.setStyleRanges(styledString.getStyleRanges());

                // Print overall portfolio performance in bold
                // @buchen: Is it acceptable here to identify this node by its name? Some example portfolios (DAX) don't seem to have "Asset Allocation".
                if (node.getName().equals("Asset Allocation")) {
                    // Set font to bold
                    FontData fontData = Display.getCurrent().getSystemFont().getFontData()[0];
                    fontData.setStyle(SWT.BOLD);
                    cell.setFont(new Font(Display.getDefault(), fontData));
                }
            }

            // This will still be used (for the CSV export)
            public String getText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;
                String prefix = ((getIsERinUse(node) ? "" : "(unused) "));
                return prefix + Values.Percent_ER.format(getExpectedReturnFor(node));
            }

            @Override
            public String getToolTipText(Object element)
            {
                TaxonomyNode node = (TaxonomyNode) element;

                String text = getIsERinUse(node) ? Messages.ColumnExpectedReturn_Tooltip_InUse : Messages.ColumnExpectedReturn_Tooltip_NotInUse;
                if (node.getName().equals("Asset Allocation")) {
                    text = Messages.ColumnExpectedReturn_Tooltip_TotalPortfolioReturn;
                }
                if (text == null)
                    return null;

                return text;
            }

            @Override
            public Point getToolTipShift(Object object) {   
                return new Point (0,15);
            }
        });

        EREditingSupport eres = new EREditingSupport();
        eres.addListener(this::onERModified).attachTo(column);

        column.setSorter(null);
        // Column is not visible by default, has to be added by user by using the menu
        column.setVisible(false);
        columns.addColumn(column);

        // Initial calculation of overall portfolio expected return. Calculate all expected returns from root downwards
        calcFullERTree(taxonomyModel.getVirtualRootNode());
    }



    @Override
    public void recalculate(TaxonomyModel model)
    {
        // Recalculate full expected returns tree
        calcFullERTree(model.getVirtualRootNode());
    }

    // Model calculations for expected returns feature.
    // Recursively calculate the expected returns for the full ER (expected returns) tree below (and including) 'node'. This is used both
    // upon init/update of the asset allocation page, and also after modifying an expected return field.
    public void calcFullERTree(TaxonomyNode node) {
        // Recurse over all children. Recursion will stop when node has no children, i.e. is leaf
        node.getChildren().forEach(child -> calcFullERTree(child));

        // If this node is an assignment (=a security), there is nothing to do - it either has an expected return assigned or not,
        // but we don't need to calculate anything.  And if this node is not in use, nothing needs to be done either.
        if (node.isAssignment() || !getIsERinUse(node)) {
            return;
        }
        // If one of the children is marked as unused, don't calculate this node either
        for (TaxonomyNode child : node.getChildren()) {
            if (!getIsERinUse(child)) {
                return;
            }
        }
        // Finally, calc and update
        // Rounding here seems to help in avoiding rounding errors 
        setExpectedReturnFor(node, (int)Math.round(calcERForNode(node, false)));
    }

    // Calculate the expected return for a given node, as a weighted average of the node's children's ER's.
    // markAsUsed: if True, mark this node's children as in use
    private double calcERForNode(TaxonomyNode parent, boolean markChildrenAsUsed)
    {
        double portfolioER = 0;
        // Calculate parent's expected return as weighted average of expected returns of all 
        //   siblings (all children of the parent) of the node. (Does not consider whether nodes are in use or not.)
        for (TaxonomyNode node : parent.getChildren()) {
            // Divide amount in this asset class by amount of total assets (root of asset class tree)
            long base = node.getParent() == null ? node.getActual().getAmount() : node.getParent().getActual().getAmount();
            double pctOfCategory = node.getActual().getAmount() / (double) base;
            portfolioER += pctOfCategory * getExpectedReturnFor(node);
            if (markChildrenAsUsed) {
                setIsERinUse(node, true);  // Mark node as 'used' in portfolio return calculation
            }
        }
        return portfolioER;
    }


    // This is called after manually changing a ER field (i.e. from onERModified()).
    // In order to make a calculation possible, mark the required nodes as "in use" or "not in use" and finally
    // re-calculate the modified tree from the root downwards.
    public void recalcExpectedReturns(TaxonomyNode currentNode) {
        // Mark as in use: 1. this node and all its siblings, 2. all parents and their siblings up to root
        markParentER(currentNode);

        // In the special situation where we have only one child, and that child is an assignment (=security), we can 
        // assign the new expected return to that child as well, since it logically must have the same ER
        if (currentNode.getChildren().size() == 1 && currentNode.getChildren().get(0).isAssignment()) {
            // Set expected return of child to that of current node
            setExpectedReturnFor(currentNode.getChildren().get(0), getExpectedReturnFor(currentNode)); 
            // and mark child as in use
            setIsERinUse(currentNode.getChildren().get(0), true);
        } else {
            // Normal situation:
            // Children below this modified node need to be marked as "not in use for the calculation of overall portfolio expected return".
            // Mark as not in use: all children and their children (if I change ER of an asset class, obviously the assigned securities' ER cannot be used in the calculation anymore)
            markChildrenAsERUnused(currentNode);
        }
        // Recalculate the full tree
        calcFullERTree(currentNode);
    }

    // Recursively mark as in use: 1. this node and all its siblings, 2. all parents and their siblings up to root
    private void markParentER(TaxonomyNode currentNode)
    {
        TaxonomyNode parent = currentNode.getParent();
        for (TaxonomyNode node : parent.getChildren()) {
            setIsERinUse(node, true);
        }
        setIsERinUse(parent, true);

        // Continue updating up the tree (further upwards until root)
        if (!parent.isRoot()) {
            markParentER(parent);
        }
    }

    private void markChildrenAsERUnused(TaxonomyNode currentNode) {
        for (TaxonomyNode child : currentNode.getChildren()) {
            setIsERinUse(child, false);
            if (child.getChildren() != null) {
                markChildrenAsERUnused(child);
            }
        }
    }
    // END Model calculations for expected returns feature.
    

    // Class for column editor support. Differs from ValueEditingSupport in that it stores its value in a Hashmap in the TaxonomyNode with set/getData(). 
    class EREditingSupport extends ColumnEditingSupport
    {
        private StringToCurrencyConverter stringToLong;
        private CurrencyToStringConverter longToString;

        public EREditingSupport()
        {
            // StringToCurrencyConverter that allows negative numbers
            this.stringToLong = new StringToCurrencyConverter(Values.Percent_ER, true);
            this.longToString = new CurrencyToStringConverter(Values.Percent_ER);
        }

        @Override
        public CellEditor createEditor(Composite composite)
        {
            TextCellEditor textEditor = new TextCellEditor(composite);
            ((Text) textEditor.getControl()).setTextLimit(20);
            // 'true' in NumberVerifyListener() to allow negative values (for negative interest rates)
            ((Text) textEditor.getControl()).addVerifyListener(new NumberVerifyListener(true));
            return textEditor;
        }

        @Override
        public Object getValue(Object element) throws Exception
        {
            TaxonomyNode node = (TaxonomyNode) element;
            return Values.Percent_ER.format(getExpectedReturnFor(node));
        }

        @Override
        public void setValue(Object element, Object value) throws Exception
        {
            TaxonomyNode node = (TaxonomyNode) element;
            String str = String.valueOf(value);
            // If there is a trailing '%' in the user input, remove it so we can correctly convert it later on 
            str = (str.length() > 0 && str.charAt(str.length() - 1) == '%') ? str.substring(0, str.length() - 1) : str;
            Number newValue = stringToLong.convert(str);
            newValue = Integer.valueOf(newValue.intValue());
            Object oldValue = getExpectedReturnFor(node);
            // We do not check here whether oldValue == newValue because it may be desirable
            // to update the field even if the newly entered value is the same as before (in order to switch the 'in use' flag)
            if (newValue != null) {
                setExpectedReturnFor(node, (int) newValue);
                setIsERinUse(node, true);
                notify(element, newValue, oldValue);
            }
        }
   }
}