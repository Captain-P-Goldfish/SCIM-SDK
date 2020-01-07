package de.captaingoldfish.scim.sdk.server.filter.antlr;

import java.util.BitSet;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

import de.captaingoldfish.scim.sdk.common.exceptions.InvalidFilterException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 16.10.2019 - 09:06 <br>
 * <br>
 * checks for errors during filter parsing and wraps the error into an {@link InvalidFilterException}
 */
@Slf4j
@NoArgsConstructor
public class FilterRuleErrorListener implements ANTLRErrorListener
{

  /**
   * throws an {@link InvalidFilterException} if a parser error occured
   */
  @Override
  public void syntaxError(Recognizer<?, ?> recognizer, Object o, int i, int i1, String message, RecognitionException e)
  {
    throw new InvalidFilterException("The specified filter syntax was invalid, or the specified attribute and filter "
                                     + "comparison combination is not supported: " + message, e);
  }

  @Override
  public void reportAmbiguity(Parser parser,
                              DFA dfa,
                              int i,
                              int i1,
                              boolean b,
                              BitSet bitSet,
                              ATNConfigSet atnConfigSet)
  {}

  @Override
  public void reportAttemptingFullContext(Parser parser,
                                          DFA dfa,
                                          int i,
                                          int i1,
                                          BitSet bitSet,
                                          ATNConfigSet atnConfigSet)
  {}

  @Override
  public void reportContextSensitivity(Parser parser, DFA dfa, int i, int i1, int i2, ATNConfigSet atnConfigSet)
  {}
}
