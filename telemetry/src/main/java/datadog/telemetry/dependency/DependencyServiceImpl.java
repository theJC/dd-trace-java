package datadog.telemetry.dependency;

import datadog.trace.util.AgentTaskScheduler;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that detects app dependencies from classloading by using a no-op class-file transformer
 */
public class DependencyServiceImpl implements DependencyService, Runnable {

  private static final Logger log = LoggerFactory.getLogger(DependencyServiceImpl.class);

  private final DependencyResolverQueue resolverQueue = new DependencyResolverQueue();

  private final BlockingQueue<Dependency> newDependencies = new LinkedBlockingQueue<>();

  private AgentTaskScheduler.Scheduled<Runnable> scheduledTask;

  public void schedulePeriodicResolution() {
    scheduledTask =
        AgentTaskScheduler.INSTANCE.scheduleAtFixedRate(
            AgentTaskScheduler.RunnableTask.INSTANCE, this, 0, 1000L, TimeUnit.MILLISECONDS);
  }

  public void resolveOneDependency() {
    Dependency dep = resolverQueue.pollDependency();
    if (dep != null) {
      log.info("Resolved dependency {}", dep.getName());
      newDependencies.add(dep);
    }
  }

  /**
   * Registers this service as a no-op class file transformer.
   *
   * @param instrumentation instrumentation instance to register on
   */
  public void installOn(Instrumentation instrumentation) {
    instrumentation.addTransformer(new LocationsCollectingTransformer(this));
  }

  @Override
  public Collection<Dependency> drainDeterminedDependencies() {
    List<Dependency> list = new LinkedList<>();
    int drained = newDependencies.drainTo(list);
    if (drained > 0) {
      return list;
    }
    return Collections.emptyList();
  }

  @Override
  public void addURL(URL url) {
    resolverQueue.queueURI(convertToURI(url));
  }

  private URI convertToURI(URL location) {
    URI uri = null;

    if (location.getProtocol().equals("vfs")) {
      // resolve jboss virtual file system
      try {
        uri = JbossVirtualFileHelper.getJbossVfsPath(location);
      } catch (RuntimeException rte) {
        log.debug("Error in call to getJbossVfsPath", rte);
        return null;
      }
    }

    if (uri == null) {
      try {
        uri = location.toURI();
      } catch (URISyntaxException e) {
        log.warn("Error converting URL to URI", e);
        // silently ignored
      }
    }

    return uri;
  }

  @Override
  public void run() {
    resolveOneDependency();
  }

  @Override
  public void stop() {
    if (scheduledTask != null) {
      scheduledTask.cancel();
      scheduledTask = null;
    }
  }
}