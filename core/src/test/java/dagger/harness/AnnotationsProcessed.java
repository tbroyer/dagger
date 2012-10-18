package dagger.harness;

import java.util.Arrays;
import org.junit.runner.Runner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;

public class AnnotationsProcessed extends Suite {

  public AnnotationsProcessed(Class<?> klass) throws InitializationError {
    super(klass, Arrays.<Runner> asList(
        new NoCodeGen(klass) {
          @Override
          protected String getName() {
            return "Reflection";
          }
        },
        new AnnotationsProcessedByJavac(klass) {
          @Override
          protected String getName() {
            return "Javac";
          }
        }));
  }
}
