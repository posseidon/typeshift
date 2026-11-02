package hu.elte.ntb.typeshift.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MathExpressionEvaluatorTest {

  @Test
  void evaluatesLedgerArithmeticWithDecimalComma() {
    assertThat(MathExpressionEvaluator.evaluate("148,50 + 27,25 = ?")).isEqualTo("175,75");
    assertThat(MathExpressionEvaluator.evaluate("2 + 3 * 4 = ?")).isEqualTo("14");
  }

  @Test
  void rejectsUnsupportedAndNonTerminatingExpressions() {
    assertThatThrownBy(() -> MathExpressionEvaluator.evaluate("1 / 3 = ?"))
        .isInstanceOf(ArithmeticException.class);
    assertThatThrownBy(() -> MathExpressionEvaluator.evaluate("1 ^ 3 = ?"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MathExpressionEvaluator.evaluate("1, + 2 = ?"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
