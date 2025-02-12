import datadog.trace.agent.test.AgentTestRunner
import datadog.trace.api.iast.InstrumentationBridge
import datadog.trace.api.iast.SourceTypes
import datadog.trace.api.iast.propagation.PropagationModule
import datadog.trace.api.iast.sink.UnvalidatedRedirectModule
import foo.bar.smoketest.JakartaHttpServletRequestTestSuite
import foo.bar.smoketest.JakartaHttpServletRequestWrapperTestSuite
import foo.bar.smoketest.ServletRequestTestSuite
import jakarta.servlet.RequestDispatcher
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper

import datadog.trace.agent.tooling.iast.TaintableEnumeration

class JakartaHttpServletRequestInstrumentationTest extends AgentTestRunner {

  @Override
  protected void configurePreAgent() {
    injectSysConfig('dd.iast.enabled', 'true')
  }

  void cleanup() {
    InstrumentationBridge.clearIastModules()
  }

  void 'test getHeader'() {
    setup:
    final iastModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final mock = Mock(HttpServletRequest)
    final request = suite.call(mock)

    when:
    final result = request.getHeader('header')

    then:
    result == 'value'
    1 * mock.getHeader('header') >> 'value'
    1 * iastModule.taint('value', SourceTypes.REQUEST_HEADER_VALUE, 'header')
    0 * _

    where:
    suite << testSuite()
  }

  void 'test getHeaders'() {
    setup:
    final iastModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final headers = ['value1', 'value2']
    final mock = Mock(HttpServletRequest)
    final request = suite.call(mock)

    when:
    final result = request.getHeaders('headers').collect()

    then:
    result == headers
    1 * mock.getHeaders('headers') >> Collections.enumeration(headers)
    headers.each { 1 * iastModule.taint(_, it, SourceTypes.REQUEST_HEADER_VALUE, 'headers') }
    0 * _

    where:
    suite << testSuite()
  }

  void 'test getHeaderNames'() {
    setup:
    final iastModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final headers = ['header1', 'header2']
    final mock = Mock(HttpServletRequest)
    final request = suite.call(mock)

    when:
    final result = request.getHeaderNames().collect()

    then:
    result == headers
    1 * mock.getHeaderNames() >> Collections.enumeration(headers)
    headers.each { 1 * iastModule.taint(_, it, SourceTypes.REQUEST_HEADER_NAME, it) }
    0 * _

    where:
    suite << testSuite()
  }

  void 'test getParameter'() {
    setup:
    final iastModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final mock = Mock(HttpServletRequest)
    final request = suite.call(mock)

    when:
    final result = request.getParameter('parameter')

    then:
    result == 'value'
    1 * mock.getParameter('parameter') >> 'value'
    1 * iastModule.taint('value', SourceTypes.REQUEST_PARAMETER_VALUE, 'parameter')
    0 * _

    where:
    suite << testSuite()
  }

  void 'test getParameterValues'() {
    setup:
    final iastModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final values = ['value1', 'value2']
    final mock = Mock(HttpServletRequest)
    final request = suite.call(mock)

    when:
    final result = request.getParameterValues('parameter').collect()

    then:
    result == values
    1 * mock.getParameterValues('parameter') >> { values as String[] }
    values.each { 1 * iastModule.taint(_, it, SourceTypes.REQUEST_PARAMETER_VALUE, 'parameter') }
    0 * _

    where:
    suite << testSuite()
  }

  void 'test getParameterMap'() {
    setup:
    final iastModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final parameters = [parameter: ['header1', 'header2'] as String[]]
    final mock = Mock(HttpServletRequest)
    final request = suite.call(mock)

    when:
    final result = request.getParameterMap()

    then:
    result == parameters
    1 * mock.getParameterMap() >> parameters
    parameters.each { key, values ->
      1 * iastModule.taint(_, key, SourceTypes.REQUEST_PARAMETER_NAME, key)
      values.each { value ->
        1 * iastModule.taint(_, value, SourceTypes.REQUEST_PARAMETER_VALUE, key)
      }
    }
    0 * _

    where:
    suite << testSuite()
  }


  void 'test getParameterNames'() {
    setup:
    final iastModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final parameters = ['param1', 'param2']
    final mock = Mock(HttpServletRequest)
    final request = suite.call(mock)

    when:
    final result = request.getParameterNames().collect()

    then:
    result == parameters
    1 * mock.getParameterNames() >> Collections.enumeration(parameters)
    parameters.each { 1 * iastModule.taint(_, it, SourceTypes.REQUEST_PARAMETER_NAME, it) }
    0 * _

    where:
    suite << testSuite()
  }

  void 'test getCookies'() {
    setup:
    final iastModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final cookies = [new Cookie('name1', 'value1'), new Cookie('name2', 'value2')] as Cookie[]
    final mock = Mock(HttpServletRequest)
    final request = suite.call(mock)

    when:
    final result = request.getCookies()

    then:
    result == cookies
    1 * mock.getCookies() >> cookies
    cookies.each { 1 * iastModule.taint(_, it, SourceTypes.REQUEST_COOKIE_VALUE) }
    0 * _

    where:
    suite << testSuite()
  }

  void 'test that get headers does not fail when servlet related code fails'() {
    setup:
    final iastModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final enumeration = Mock(Enumeration) {
      hasMoreElements() >> { throw new NuclearBomb('Boom!!!') }
    }
    final mock = Mock(HttpServletRequest)
    final request = suite.call(mock)

    when:
    final headers = request.getHeaders('header')

    then:
    1 * mock.getHeaders('header') >> enumeration
    noExceptionThrown()

    when:
    headers.hasMoreElements()

    then:
    final bomb = thrown(NuclearBomb)
    bomb.stackTrace.find { it.className == TaintableEnumeration.name } == null

    where:
    suite << testSuite()
  }

  void 'test that get header names does not fail when servlet related code fails'() {
    setup:
    final iastModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final enumeration = Mock(Enumeration) {
      hasMoreElements() >> { throw new NuclearBomb('Boom!!!') }
    }
    final mock = Mock(HttpServletRequest)
    final request = suite.call(mock)

    when:
    final result = request.getHeaderNames()

    then:
    1 * mock.getHeaderNames() >> enumeration
    noExceptionThrown()

    when:
    result.hasMoreElements()

    then:
    final bomb = thrown(NuclearBomb)
    bomb.stackTrace.find { it.className == TaintableEnumeration.name } == null

    where:
    suite << testSuite()
  }

  void 'test get query string'() {
    setup:
    final iastModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final queryString = 'paramName=paramValue'
    final mock = Mock(HttpServletRequest)
    final request = suite.call(mock)

    when:
    final String result = request.getQueryString()

    then:
    result == queryString
    1 * mock.getQueryString() >> queryString
    1 * iastModule.taint(queryString, SourceTypes.REQUEST_QUERY)
    0 * _

    where:
    suite << testSuite()
  }

  void 'test getInputStream'() {
    setup:
    final iastModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final is = Mock(ServletInputStream)
    final mock = Mock(HttpServletRequest)
    final request = suite.call(mock)

    when:
    final result = request.getInputStream()

    then:
    result == is
    1 * mock.getInputStream() >> is
    1 * iastModule.taint(is, SourceTypes.REQUEST_BODY)
    0 * _

    where:
    suite << testSuite()
  }

  void 'test getReader'() {
    setup:
    final iastModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final reader = Mock(BufferedReader)
    final mock = Mock(HttpServletRequest)
    final request = suite.call(mock)

    when:
    final result = request.getReader()

    then:
    result == reader
    1 * mock.getReader() >> reader
    1 * iastModule.taint(reader, SourceTypes.REQUEST_BODY)
    0 * _

    where:
    suite << testSuite()
  }

  void 'test getRequestDispatcher'() {
    setup:
    final iastModule = Mock(UnvalidatedRedirectModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final path = 'http://dummy.location.com'
    final dispatcher = Mock(RequestDispatcher)
    final mock = Mock(HttpServletRequest)
    final request = suite.call(mock)

    when:
    final result = request.getRequestDispatcher(path)

    then:
    result == dispatcher
    1 * mock.getRequestDispatcher(path) >> dispatcher
    1 * iastModule.onRedirect(path)
    0 * _

    where:
    suite << testSuite()
  }

  void 'test getRequestURI'() {
    setup:
    final iastModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final uri = 'retValue'
    final mock = Mock(HttpServletRequest)
    final request = suite.call(mock)

    when:
    final result = request.getRequestURI()

    then:
    result == uri
    1 * mock.getRequestURI() >> uri
    1 * iastModule.taint(uri, SourceTypes.REQUEST_PATH)
    0 * _

    where:
    suite << testSuiteCallSites()
  }

  void 'test getPathInfo'() {
    setup:
    final iastModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final pathInfo = 'retValue'
    final mock = Mock(HttpServletRequest)
    final request = suite.call(mock)

    when:
    final result = request.getPathInfo()

    then:
    result == pathInfo
    1 * mock.getPathInfo() >> pathInfo
    1 * iastModule.taint(pathInfo, SourceTypes.REQUEST_PATH)
    0 * _

    where:
    suite << testSuiteCallSites()
  }

  void 'test getPathTranslated'() {
    setup:
    final iastModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final pathTranslated = 'retValue'
    final mock = Mock(HttpServletRequest)
    final request = suite.call(mock)

    when:
    final result = request.getPathTranslated()

    then:
    result == pathTranslated
    1 * mock.getPathTranslated() >> pathTranslated
    1 * iastModule.taint(pathTranslated, SourceTypes.REQUEST_PATH)
    0 * _

    where:
    suite << testSuiteCallSites()
  }

  void 'test getRequestURL'() {
    setup:
    final iastModule = Mock(PropagationModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final url = new StringBuffer('retValue')
    final mock = Mock(HttpServletRequest)
    final request = suite.call(mock)

    when:
    final result = request.getRequestURL()

    then:
    result == url
    1 * mock.getRequestURL() >> url
    1 * iastModule.taint(url, SourceTypes.REQUEST_URI)
    0 * _

    where:
    suite << testSuiteCallSites()
  }

  private List<Closure<? extends HttpServletRequest>> testSuite() {
    return [
      { HttpServletRequest request -> new CustomRequest(request: request) },
      { HttpServletRequest request -> new CustomRequestWrapper(new CustomRequest(request: request)) },
      { HttpServletRequest request ->
        new HttpServletRequestWrapper(new CustomRequest(request: request))
      }
    ]
  }

  private List<Closure<? extends ServletRequestTestSuite>> testSuiteCallSites() {
    return [
      { HttpServletRequest request -> new JakartaHttpServletRequestTestSuite(request) },
      { HttpServletRequest request -> new JakartaHttpServletRequestWrapperTestSuite(new CustomRequestWrapper(request)) },
    ]
  }

  private static class NuclearBomb extends RuntimeException {
    NuclearBomb(final String message) {
      super(message)
    }
  }

  private static class CustomRequest implements HttpServletRequest {
    @Delegate
    private HttpServletRequest request
  }

  private static class CustomRequestWrapper extends HttpServletRequestWrapper {

    CustomRequestWrapper(final HttpServletRequest request) {
      super(request)
    }
  }
}
