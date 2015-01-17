package name.abuchen.portfolio.money;

import java.util.function.UnaryOperator;

/**
 * Represents an operation on a single {@link Money} that produces a result of
 * type {@link Money}.
 */
@FunctionalInterface
public interface MonetaryOperator extends UnaryOperator<Money>
{}
