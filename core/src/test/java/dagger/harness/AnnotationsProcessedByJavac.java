package dagger.harness;

import java.net.URL;
import java.util.Map;

import dagger.harness.javac.JavacHelper;

import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import static org.junit.Assert.fail;

public class AnnotationsProcessedByJavac extends AbstractRunnerWithClassLoader {

  public AnnotationsProcessedByJavac(Class<?> klass) throws InitializationError {
    super(klass);
  }

  @Override
  protected TestClassLoader createClassLoader(final FrameworkMethod method, final RunNotifier notifier,
      URL[] urls) {
    ProcessAnnotations annotation = method.getAnnotation(ProcessAnnotations.class);
    if (annotation == null) {
      notifier.fireTestAssumptionFailed(new Failure(describeChild(method),
          new AssumptionViolatedException(String.format("method '%s' must have a %s annotation",
              method.getName(), ProcessAnnotations.class.getCanonicalName()))));
      return null;
    }

    final Map<String, byte[]> bytecodes = JavacHelper.processAnnotations(annotation.value(), describeChild(method), notifier);
    if (bytecodes == null) {
      return null;
    }

    return new TestClassLoader(urls) {
      private boolean called;

      {
        for (Map.Entry<String, byte[]> bytecode : bytecodes.entrySet()) {
          byte[] bytes = bytecode.getValue();
          defineClass(bytecode.getKey(), bytes, 0, bytes.length);
        }
      }

      @Override
      public void validate() {
        // just check that we've actually been used
        if (!called) {
          notifier.fireTestAssumptionFailed(
              new Failure(describeChild(method), new AssumptionViolatedException("Custom classloader never used")));
        }
      }

      @Override
      protected Class<?> findClass(String name) throws ClassNotFoundException {
        called = true;
        try {
          return super.findClass(name);
        } catch (ClassNotFoundException cnfe) {
          if (name.endsWith("$ModuleAdapter")
              || name.endsWith("$InjectAdapter")
              || name.endsWith("$SingletonInject")) {
            fail();
          }
          throw cnfe;
        }
      }
    };
  }
}
