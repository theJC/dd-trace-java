package datadog.trace.instrumentation.resteasy;

import datadog.trace.api.iast.IastContext;
import datadog.trace.api.iast.InstrumentationBridge;
import datadog.trace.api.iast.Source;
import datadog.trace.api.iast.SourceTypes;
import datadog.trace.api.iast.propagation.PropagationModule;
import java.util.Collection;
import net.bytebuddy.asm.Advice;

public class QueryParamInjectorAdvice {
  @Advice.OnMethodExit(suppress = Throwable.class)
  @Source(SourceTypes.REQUEST_PARAMETER_VALUE)
  public static void onExit(
      @Advice.Return Object result, @Advice.FieldValue("encodedName") String paramName) {
    if (result instanceof String || result instanceof Collection) {
      final PropagationModule module = InstrumentationBridge.PROPAGATION;
      if (module != null) {
        if (result instanceof Collection) {
          Collection<?> collection = (Collection<?>) result;
          final IastContext ctx = IastContext.Provider.get();
          for (Object o : collection) {
            if (o instanceof String) {
              module.taint(ctx, o, SourceTypes.REQUEST_PARAMETER_VALUE, paramName);
            }
          }
        } else {
          module.taint(result, SourceTypes.REQUEST_PARAMETER_VALUE, paramName);
        }
      }
    }
  }
}
