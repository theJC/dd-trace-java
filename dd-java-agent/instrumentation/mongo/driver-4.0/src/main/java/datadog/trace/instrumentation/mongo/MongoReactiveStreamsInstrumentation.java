package datadog.trace.instrumentation.mongo;

import static datadog.trace.agent.tooling.bytebuddy.matcher.DDElementMatchers.implementsInterface;
import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.named;
import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.namedOneOf;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.bootstrap.instrumentation.java.concurrent.State;
import java.util.Map;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public final class MongoReactiveStreamsInstrumentation extends Instrumenter.Tracing {

  private static final boolean MONGO_REACTIVESTREAMS_PROPAGATE_ALL_SINGLERESULTCALLBACK = true;

  public MongoReactiveStreamsInstrumentation() {
    super("mongo", "mongo-4.0");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    // Provide a mechanism to instrument all subclasses of SingleResultCallBack
    if (MONGO_REACTIVESTREAMS_PROPAGATE_ALL_SINGLERESULTCALLBACK) {
      return implementsInterface(named("com.mongodb.internal.async.SingleResultCallback"));
    } else {
      // This handles all of the current cases
      return namedOneOf(
          "com.mongodb.internal.async.ErrorHandlingResultCallback",
          // Required when a call is made before the connection is created
          "com.mongodb.internal.operation.OperationHelper$8",
          "com.mongodb.internal.operation.OperationHelper$9");
    }
  }

  @Override
  public Map<String, String> contextStore() {
    return singletonMap("com.mongodb.internal.async.SingleResultCallback", State.class.getName());
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {};
  }

  @Override
  public void adviceTransformations(AdviceTransformation transformation) {
    transformation.applyAdvice(isConstructor(), packageName + ".ConstructAdvice");
    transformation.applyAdvice(isMethod().and(named("onResult")), packageName + ".CompleteAdvice");
  }
}
