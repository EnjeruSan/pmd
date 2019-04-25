/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.metrics;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.pmd.lang.ast.QualifiableNode;

/**
 * Base class for metrics computers. These objects compute a metric and memoize it.
 *
 * @param <T> Type of type declaration nodes of the language
 * @param <O> Type of operation declaration nodes of the language
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public abstract class AbstractMetricsComputer<T extends QualifiableNode, O extends QualifiableNode>
    implements MetricsComputer<T, O> {

    @Override
    public double computeForType(MetricKey<T> key, T node, boolean force,
                                 MetricOptions options, MetricMemoizer<T> memoizer) {

        ParameterizedMetricKey<T> paramKey = ParameterizedMetricKey.getInstance(key, options);
        // if memo.get(key) == null then the metric has never been computed. NaN is a valid value.
        Double prev = memoizer.getMemo(paramKey);
        if (!force && prev != null) {
            return prev;
        }

        double val = key.getCalculator().computeFor(node, options);
        memoizer.memoize(paramKey, val);

        return val;
    }


    @Override
    public double computeForOperation(MetricKey<O> key, O node, boolean force,
                                      MetricOptions options, MetricMemoizer<O> memoizer) {

        ParameterizedMetricKey<O> paramKey = ParameterizedMetricKey.getInstance(key, options);
        Double prev = memoizer.getMemo(paramKey);
        if (!force && prev != null) {
            return prev;
        }

        double val = key.getCalculator().computeFor(node, options);
        memoizer.memoize(paramKey, val);
        return val;
    }


    @Override
    public double computeWithResultOption(MetricKey<O> key, T node, boolean force, MetricOptions options,
                                          ResultOption option, ProjectMemoizer<T, O> stats) {

        List<O> ops = findOperations(node);

        List<Double> values = new ArrayList<>();
        for (O op : ops) {
            if (key.supports(op)) {
                MetricMemoizer<O> opStats = stats.getOperationMemoizer(op.getQualifiedName());
                double val = this.computeForOperation(key, op, force, options, opStats);
                if (val != Double.NaN) {
                    values.add(val);
                }
            }
        }

        // FUTURE use streams to do that when we upgrade the compiler to 1.8
        switch (option) {
        case SUM:
            return sum(values);
        case HIGHEST:
            return highest(values);
        case AVERAGE:
            return average(values);
        default:
            return Double.NaN;
        }
    }


    /**
     * Finds the declaration nodes of all methods or constructors that are declared inside a class. This is language
     * specific, as it depends on the AST.
     *
     * @param node The class in which to look for.
     *
     * @return The list of all operations declared inside the specified class.
     */
    protected abstract List<O> findOperations(T node); // TODO:cf this one is computed every time


    private static double sum(List<Double> values) {
        double sum = 0;
        for (double val : values) {
            sum += val;
        }
        return sum;
    }


    private static double highest(List<Double> values) {
        double highest = Double.NEGATIVE_INFINITY;
        for (double val : values) {
            if (val > highest) {
                highest = val;
            }
        }
        return highest == Double.NEGATIVE_INFINITY ? 0 : highest;
    }


    private static double average(List<Double> values) {
        return sum(values) / values.size();
    }


}
