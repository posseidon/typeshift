package hu.elte.ntb.typeshift.ai.service;

import java.math.BigDecimal;

public final class MathExpressionEvaluator {

  private MathExpressionEvaluator() {}

  public static String evaluate(String displayText) {
    if (displayText == null || !displayText.matches("[0-9, +*/-]+ = \\?")) {
      throw new IllegalArgumentException("Invalid ledger expression");
    }
    String expression = displayText.substring(0, displayText.lastIndexOf('=')).strip();
    Parser parser = new Parser(expression);
    BigDecimal result = parser.expression();
    parser.requireEnd();
    return result.stripTrailingZeros().toPlainString().replace('.', ',');
  }

  private static final class Parser {

    private final String input;
    private int position;

    private Parser(String input) {
      this.input = input;
    }

    private BigDecimal expression() {
      BigDecimal result = term();
      while (true) {
        skipSpaces();
        if (consume('+')) {
          result = result.add(term());
        } else if (consume('-')) {
          result = result.subtract(term());
        } else {
          return result;
        }
      }
    }

    private BigDecimal term() {
      BigDecimal result = number();
      while (true) {
        skipSpaces();
        if (consume('*')) {
          result = result.multiply(number());
        } else if (consume('/')) {
          result = result.divide(number());
        } else {
          return result;
        }
      }
    }

    private BigDecimal number() {
      skipSpaces();
      int start = position;
      while (position < input.length()
          && (Character.isDigit(input.charAt(position)) || input.charAt(position) == ',')) {
        position++;
      }
      if (start == position) {
        throw new IllegalArgumentException("Expected a decimal number");
      }
      String value = input.substring(start, position);
      int separator = value.indexOf(',');
      if (separator >= 0
          && (separator == 0
              || separator == value.length() - 1
              || value.indexOf(',', separator + 1) >= 0)) {
        throw new IllegalArgumentException("Invalid decimal number");
      }
      return new BigDecimal(value.replace(',', '.'));
    }

    private boolean consume(char expected) {
      if (position < input.length() && input.charAt(position) == expected) {
        position++;
        return true;
      }
      return false;
    }

    private void skipSpaces() {
      while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
        position++;
      }
    }

    private void requireEnd() {
      skipSpaces();
      if (position != input.length()) {
        throw new IllegalArgumentException("Unexpected math expression content");
      }
    }
  }
}
