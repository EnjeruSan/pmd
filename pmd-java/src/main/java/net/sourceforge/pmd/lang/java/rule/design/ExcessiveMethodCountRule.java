package net.sourceforge.pmd.lang.java.rule.design;

import net.sourceforge.pmd.lang.java.ast.ASTMethodOrConstructorDeclaration;
import net.sourceforge.pmd.lang.java.ast.JavaNode;
import net.sourceforge.pmd.stat.DataPoint;
import net.sourceforge.pmd.util.NumericConstants;

public class ExcessiveMethodCountRule extends ExcessiveNodeCountRule {

    public ExcessiveMethodCountRule() {
        super(ASTMethodOrConstructorDeclaration.class);
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

    public Object visit(ASTMethodOrConstructorDeclaration node, Object data) {
        return NumericConstants.ONE;
    }
}
