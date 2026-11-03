package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.modules.auth.domain.entity.UserRank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Supplementary UT-15 coverage of the production rank policy, not the persistence query.
 * RefundMembershipService delegates net-paid/points recalculation to UserRepository;
 * verifying actual monetary deductions, rounding and rank persistence requires a database test.
 * These tests deliberately do not reproduce that SQL in a mock answer or test-side calculator.
 */
class ScopeMembershipDesignTest {
    @ParameterizedTest(name = "{0} points => {1}")
    @CsvSource({
            "0, SILVER",
            "499, SILVER",
            "500, SILVER",
            "501, GOLD",
            "502, GOLD",
            "1499, GOLD",
            "1500, GOLD",
            "1501, DIAMOND",
            "1502, DIAMOND"
    })
    @DisplayName("UT-15 supplement: current rank policy has exact 500/501 and 1500/1501 boundaries")
    void rankPolicyUsesConcretePointBoundaries(int points, UserRank expectedRank) {
        assertEquals(expectedRank, UserRank.fromPoints(points));
    }
}
