package com.thinkaurelius.titan.graphdb.tinkerpop.optimize;

import com.google.common.collect.Iterables;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanGraphQuery;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.graphdb.query.BaseQuery;
import com.thinkaurelius.titan.graphdb.query.TitanPredicate;
import com.tinkerpop.gremlin.process.graph.step.sideEffect.GraphStep;
import com.tinkerpop.gremlin.process.graph.util.HasContainer;
import com.tinkerpop.gremlin.process.util.TraversalHelper;
import com.tinkerpop.gremlin.structure.Contains;
import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.structure.Order;
import com.tinkerpop.gremlin.structure.Vertex;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Matthias Broecheler (me@matthiasb.com)
 */
public class TitanGraphStep<E extends Element> extends GraphStep<E> implements HasStepFolder<E,E> {

    private final List<HasContainer> hasContainers = new ArrayList<>();
    private int limit = BaseQuery.NO_LIMIT;
    private List<OrderEntry> orders = new ArrayList<>();

    public TitanGraphStep(final GraphStep<E> originalGraphStep) {
        super(originalGraphStep.getTraversal(), originalGraphStep.getGraph(TitanGraph.class), originalGraphStep.getReturnClass(), originalGraphStep.getIds());
        if (TraversalHelper.isLabeled(originalGraphStep))
            this.setLabel(originalGraphStep.getLabel());
        this.setIteratorSupplier(() -> {
            TitanTransaction tx = TitanTraversalUtil.getTx(traversal);
            TitanGraphQuery query = tx.query();
            for (HasContainer condition : hasContainers) {
                if (condition.predicate instanceof Contains && condition.value==null) {
                    if (condition.predicate==Contains.within) query.has(condition.key);
                    else query.hasNot(condition.key);
                } else {
                    query.has(condition.key, TitanPredicate.Converter.convert(condition.predicate), condition.value);
                }
            }
            for (OrderEntry order : orders) query.orderBy(order.key,order.order);
            if (limit!=BaseQuery.NO_LIMIT) query.limit(limit);
            return Vertex.class.isAssignableFrom(this.returnClass) ? query.vertices().iterator() : query.edges().iterator();
        });
    }

    @Override
    public String toString() {
        return this.hasContainers.isEmpty() ? super.toString() : TraversalHelper.makeStepString(this, this.hasContainers);
    }

    @Override
    public void addAll(Iterable<HasContainer> has) {
        Iterables.addAll(hasContainers, has);
    }

    @Override
    public void orderBy(String key, Order order) {
        orders.add(new OrderEntry(key,order));
    }

    @Override
    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public int getLimit() {
        return this.limit;
    }
}

