package dagger.harness;

import java.net.URL;
import java.net.URLClassLoader;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

public abstract class AbstractRunnerWithClassLoader extends BlockJUnit4ClassRunner {

  public AbstractRunnerWithClassLoader(Class<?> klass) throws InitializationError {
    super(klass);
  }

  @Override
  protected void runChild(FrameworkMethod method, RunNotifier notifier) {
    Thread currentThread = Thread.currentThread();
    URLClassLoader oldCL = (URLClassLoader) currentThread.getContextClassLoader();
    TestClassLoader cl = createClassLoader(method, notifier, oldCL.getURLs());
    if (cl == null) {
      return;
    }
    currentThread.setContextClassLoader(cl);
    try {
      super.runChild(method, notifier);
      cl.validate();
    } finally {
      currentThread.setContextClassLoader(oldCL);
    }
  }

  /**
   * Creates the {@link ClassLoader} that the test method will run with.
   * <p>
   * Reports errors to the {@link RunNotifier} and returns {@code null} in case of error.
   */
  protected abstract TestClassLoader createClassLoader(FrameworkMethod method, RunNotifier notifier, URL[] urls);

  protected static abstract class TestClassLoader extends URLClassLoader {

    public TestClassLoader(URL[] urls) {
      super(urls, null);
    }

    public abstract void validate();
  }
}
