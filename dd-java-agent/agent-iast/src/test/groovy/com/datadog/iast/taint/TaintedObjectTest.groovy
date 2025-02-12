package com.datadog.iast.taint

import com.datadog.iast.model.Source
import datadog.trace.api.Config
import spock.lang.Specification
import com.datadog.iast.model.Range

import java.lang.ref.ReferenceQueue

import static datadog.trace.api.iast.VulnerabilityMarks.NOT_MARKED
import static datadog.trace.api.iast.SourceTypes.REQUEST_HEADER_NAME

class TaintedObjectTest extends Specification {

  void 'test that tainted objects never go over the limit'() {
    given:
    final max = Config.get().iastMaxRangeCount
    final ranges = (0..max + 1)
      .collect { index -> new Range(index, 1, new Source(REQUEST_HEADER_NAME, 'a', 'b'), NOT_MARKED) }

    when:
    final tainted = new TaintedObject('test', ranges.toArray(new Range[0]), new ReferenceQueue<Object>())

    then:
    ranges.size() > max
    tainted.ranges.size() == max
  }
}
