package datadog.trace.instrumentation.mongo;

import static datadog.trace.bootstrap.instrumentation.java.concurrent.AdviceUtils.capture;

import com.mongodb.internal.async.SingleResultCallback;
import datadog.trace.bootstrap.InstrumentationContext;
import datadog.trace.bootstrap.instrumentation.java.concurrent.State;
import net.bytebuddy.asm.Advice;

public class ConstructAdvice {
  @Advice.OnMethodExit(suppress = Throwable.class)
  public static <T> void captureScope(@Advice.This SingleResultCallback<T> task) {
    capture(InstrumentationContext.get(SingleResultCallback.class, State.class), task, true);
  }
}
