package kg.fastfood.crm.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PoissonDistributionTest {

    @Test
    void calculatesLambdaAndProbabilities() {
        // 300 заказов за 100 часов => λ = 3.0
        double lambda = PoissonDistribution.lambda(300, 100);
        assertEquals(3.0, lambda, 1e-9);

        // P(X = 0) = e^-3 ≈ 0.049787
        assertEquals(Math.exp(-3.0), PoissonDistribution.probability(0, lambda), 1e-6);
        // P(X = 3) = (27 * e^-3) / 6 = 4.5 * e^-3 ≈ 0.2240418
        assertEquals(4.5 * Math.exp(-3.0), PoissonDistribution.probability(3, lambda), 1e-6);

        // P(X <= 2) + P(X >= 3) = 1
        assertEquals(1.0, PoissonDistribution.cumulative(2, lambda) + PoissonDistribution.atLeast(3, lambda), 1e-9);
        assertTrue(PoissonDistribution.quantile(0.95, lambda) >= 6);
    }

    @Test
    void handlesLargeKWithoutOverflow() {
        double p = PoissonDistribution.probability(150, 140.0);
        assertTrue(p > 0.0 && p < 1.0 && !Double.isNaN(p));
    }

    @Test
    void guardsInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> PoissonDistribution.lambda(10, 0));
        assertThrows(IllegalArgumentException.class, () -> PoissonDistribution.probability(-1, 2.0));
    }
}
