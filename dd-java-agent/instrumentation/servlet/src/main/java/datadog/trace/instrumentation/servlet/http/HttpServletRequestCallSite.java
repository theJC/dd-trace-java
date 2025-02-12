package datadog.trace.instrumentation.servlet.http;

import datadog.trace.agent.tooling.csi.CallSite;
import datadog.trace.api.iast.IastCallSites;
import datadog.trace.api.iast.InstrumentationBridge;
import datadog.trace.api.iast.Source;
import datadog.trace.api.iast.SourceTypes;
import datadog.trace.api.iast.propagation.PropagationModule;
import javax.servlet.http.HttpServletRequest;

/**
 * Calls to these methods are often triggered outside of customer code, we use call sites to avoid
 * all these unwanted tainting operations
 */
@CallSite(spi = IastCallSites.class)
public class HttpServletRequestCallSite {

  @Source(SourceTypes.REQUEST_PATH)
  @CallSite.After("java.lang.String javax.servlet.http.HttpServletRequest.getRequestURI()")
  @CallSite.After("java.lang.String javax.servlet.http.HttpServletRequestWrapper.getRequestURI()")
  @CallSite.After("java.lang.String javax.servlet.http.HttpServletRequest.getPathInfo()")
  @CallSite.After("java.lang.String javax.servlet.http.HttpServletRequestWrapper.getPathInfo()")
  @CallSite.After("java.lang.String javax.servlet.http.HttpServletRequest.getPathTranslated()")
  @CallSite.After(
      "java.lang.String javax.servlet.http.HttpServletRequestWrapper.getPathTranslated()")
  public static String afterPath(
      @CallSite.This final HttpServletRequest self, @CallSite.Return final String retValue) {
    if (null != retValue && !retValue.isEmpty()) {
      final PropagationModule module = InstrumentationBridge.PROPAGATION;
      if (module != null) {
        try {
          module.taint(retValue, SourceTypes.REQUEST_PATH);
        } catch (final Throwable e) {
          module.onUnexpectedException("afterPath threw", e);
        }
      }
    }
    return retValue;
  }

  @Source(SourceTypes.REQUEST_URI)
  @CallSite.After("java.lang.StringBuffer javax.servlet.http.HttpServletRequest.getRequestURL()")
  @CallSite.After(
      "java.lang.StringBuffer javax.servlet.http.HttpServletRequestWrapper.getRequestURL()")
  public static StringBuffer afterGetRequestURL(
      @CallSite.This final HttpServletRequest self, @CallSite.Return final StringBuffer retValue) {
    if (null != retValue && retValue.length() > 0) {
      final PropagationModule module = InstrumentationBridge.PROPAGATION;
      if (module != null) {
        try {
          module.taint(retValue, SourceTypes.REQUEST_URI);
        } catch (final Throwable e) {
          module.onUnexpectedException("afterGetRequestURL threw", e);
        }
      }
    }
    return retValue;
  }
}
