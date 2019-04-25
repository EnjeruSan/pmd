/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.rule.design;

import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.ast.ASTImportDeclaration;
import net.sourceforge.pmd.lang.java.ast.JavaNode;
import net.sourceforge.pmd.stat.DataPoint;
import net.sourceforge.pmd.util.NumericConstants;

/**
 * ExcessiveImports attempts to count all unique imports a class contains. This
 * rule will count a "import com.something.*;" as a single import. This is a
 * unqiue situation and I'd like to create an audit type rule that captures
 * those.
 *
 * @author aglover
 * @since Feb 21, 2003
 */
public class ExcessiveImportsRule extends ExcessiveNodeCountRule {

    public ExcessiveImportsRule() {
        super(ASTCompilationUnit.class);
        setProperty(MINIMUM_DESCRIPTOR, 1d);
    }

    @Override
    public Object visit(JavaNode node, Object data) {
        int numNodes = 0;

        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            Integer treeSize = (Integer) ((JavaNode) node.jjtGetChild(i)).jjtAccept(this, data);
            numNodes += treeSize;
        }

        if (nodeClass.isInstance(node)) {
            DataPoint point = new DataPoint();
            point.setNode(node);
            point.setScore(1.0 * numNodes);
            point.setMessage(getMessage());
            addDataPoint(point);
            Double result = point.getScore();

            addViolation(data, node, result.toString());
        }

        return Integer.valueOf(numNodes);
    }

    /**
     * Hook method to count imports. This is a user defined value.
     *
     * @param node
     * @param data
     * @return Object
     */
    @Override
    public Object visit(ASTImportDeclaration node, Object data) {
        return NumericConstants.ONE;
    }
}
