package datadog.trace.instrumentation.akkahttp.iast.helpers;

import datadog.trace.api.iast.IastContext;
import datadog.trace.api.iast.InstrumentationBridge;
import datadog.trace.api.iast.SourceTypes;
import datadog.trace.api.iast.propagation.PropagationModule;
import scala.Tuple1;
import scala.Tuple2;
import scala.collection.Iterator;
import scala.collection.immutable.Map;
import scala.compat.java8.JFunction1;

public class TaintMapFunction
    implements JFunction1<Tuple1<Map<String, String>>, Tuple1<Map<String, String>>> {
  public static final TaintMapFunction INSTANCE = new TaintMapFunction();

  @Override
  public Tuple1<Map<String, String>> apply(Tuple1<Map<String, String>> v1) {
    Map<String, String> m = v1._1;

    PropagationModule prop = InstrumentationBridge.PROPAGATION;
    if (prop == null || m == null || m.isEmpty()) {
      return v1;
    }

    final IastContext ctx = IastContext.Provider.get();
    Iterator<Tuple2<String, String>> iterator = m.iterator();
    while (iterator.hasNext()) {
      Tuple2<String, String> e = iterator.next();
      final String name = e._1(), value = e._2();
      prop.taint(ctx, name, SourceTypes.REQUEST_PARAMETER_NAME, name);
      prop.taint(ctx, value, SourceTypes.REQUEST_PARAMETER_VALUE, name);
    }

    return v1;
  }
}
