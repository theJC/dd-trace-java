package datadog.trace.instrumentation.json;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.api.iast.InstrumentationBridge;
import datadog.trace.api.iast.Propagation;
import datadog.trace.api.iast.propagation.PropagationModule;
import net.bytebuddy.asm.Advice;

@AutoService(Instrumenter.class)
public class JSONTokenerInstrumentation extends Instrumenter.Iast
    implements Instrumenter.ForSingleType {

  public JSONTokenerInstrumentation() {
    super("org-json");
  }

  @Override
  public String instrumentedType() {
    return "org.json.JSONTokener";
  }

  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
    transformation.applyAdvice(
        isConstructor().and(takesArguments(String.class)),
        getClass().getName() + "$ConstructorAdvice");
  }

  public static class ConstructorAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    @Propagation
    public static void afterInit(@Advice.This Object self, @Advice.Argument(0) final Object input) {
      final PropagationModule iastModule = InstrumentationBridge.PROPAGATION;
      if (iastModule != null && input != null) {
        iastModule.taintIfTainted(self, input);
      }
    }
  }
}
