package dagger.harness;

import java.net.URL;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

public class NoCodeGen extends AbstractRunnerWithClassLoader {

  public NoCodeGen(Class<?> klass) throws InitializationError {
    super(klass);
  }

  @Override
  protected TestClassLoader createClassLoader(final FrameworkMethod method, final RunNotifier notifier, URL[] urls) {
    return new TestClassLoader(urls) {
      private boolean called;

      @Override
      public void validate() {
        // just check that we've actually been used
        if (!called) {
          notifier.fireTestAssumptionFailed(
              new Failure(describeChild(method), new AssumptionViolatedException("Custom classloader never used")));
        }
      }

      @Override
      public Class<?> loadClass(String name) throws ClassNotFoundException {
        return this.loadClass(name, false);
      }

      @Override
      protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        called = true;
        if (name.endsWith("$ModuleAdapter") 
            || name.endsWith("$InjectAdapter")
            || name.endsWith("$StaticInjection")) {
          throw new ClassNotFoundException("Dagger codegen disabled; always use reflection-based fallback");
        }
        return super.loadClass(name, resolve);
      }
    };
  }
}
