package com.datadog.debugger.symbol;

public class SymbolExtraction07 {
  public static int main(String arg) {
    int i = 10;
    do {
      int j = i + 12;
      i--;
    } while (i > 0);
    return arg.length();
  }
}
