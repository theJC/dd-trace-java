package datadog.trace.instrumentation.mongo;

import static datadog.trace.bootstrap.instrumentation.java.concurrent.AdviceUtils.endTaskScope;
import static datadog.trace.bootstrap.instrumentation.java.concurrent.AdviceUtils.startTaskScope;

import com.mongodb.internal.async.SingleResultCallback;
import datadog.trace.bootstrap.InstrumentationContext;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.java.concurrent.State;
import net.bytebuddy.asm.Advice;

public class CompleteAdvice {
  @Advice.OnMethodEnter(suppress = Throwable.class)
  public static <T> AgentScope activate(@Advice.This SingleResultCallback<T> task) {
    return startTaskScope(
        InstrumentationContext.get(SingleResultCallback.class, State.class), task);
  }

  @Advice.OnMethodExit(onThrowable = Throwable.class)
  public static void close(@Advice.Enter AgentScope scope) {
    endTaskScope(scope);
  }
}
