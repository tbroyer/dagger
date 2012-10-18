package dagger.harness.javac;

import dagger.internal.codegen.InjectProcessor;
import dagger.internal.codegen.ProvidesProcessor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

public class JavacHelper {

  private static final boolean DEBUG = Boolean.getBoolean("dagger.tests.debug");

  public static Map<String, byte[]> processAnnotations(Class<?>[] classes, Description description, RunNotifier notifier) {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      if (DEBUG) {
        System.err.println("Use a JDK, not a JRE");
      }
      notifier.fireTestAssumptionFailed(new Failure(description, new AssumptionViolatedException("Use a JDK, not a JRE")));
      return null;
    }

    // TODO: charset?
    InMemoryJavaFileManager fileManager = new InMemoryJavaFileManager(compiler.getStandardFileManager(null, null, null));
    List<String> classnames = new ArrayList<String>(classes.length);
    for (Class<?> cls : classes) {
      classnames.add(cls.getName());
    }
    DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<JavaFileObject>();

    CompilationTask task = compiler.getTask(null, fileManager, diagnosticCollector,
        Arrays.asList("-processor", InjectProcessor.class.getName() + "," + ProvidesProcessor.class.getName()),
        classnames, null);

    boolean success = task.call();

    try {
      fileManager.close();
    } catch (IOException e) {
      if (DEBUG) {
        e.printStackTrace();
      }
      notifier.fireTestAssumptionFailed(new Failure(description, new AssumptionViolatedException(e.getLocalizedMessage())));
      // fall-through
    }

    if (DEBUG) {
      File outDir = new File("target/tests/javac/" + description.getClassName() + "-" + description.getMethodName());
      outDir.mkdirs();
      for (Map.Entry<String, InMemoryJavaFileObject> entry : fileManager.getGeneratedFiles().entrySet()) {
        File outFile = new File(outDir, entry.getKey());
        outFile.getParentFile().mkdirs();
        try {
          FileOutputStream os = new FileOutputStream(outFile);
          os.write(entry.getValue().getBytesContent());
          os.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    if (!success) {
      String diagnostic = makeDiagnostic(diagnosticCollector.getDiagnostics());
      if (DEBUG) {
        System.err.println(description);
        System.err.println(diagnostic);
      }
      notifier.fireTestAssumptionFailed(new Failure(description, new AssumptionViolatedException(diagnostic)));
      return null;
    }

    Map<String, byte[]> generatedClasses = new HashMap<String, byte[]>();
    for (Map.Entry<String, InMemoryJavaFileObject> entry : fileManager.getGeneratedClasses().entrySet()) {
      generatedClasses.put(entry.getKey(), entry.getValue().getBytesContent());
    }
    return generatedClasses;
  }

  private static String makeDiagnostic(List<Diagnostic<? extends JavaFileObject>> diagnostics) {
    StringBuilder sb = new StringBuilder("Annotation processing failed:\n\n");
    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
      if (diagnostic.getKind() != Diagnostic.Kind.ERROR) {
        // TODO: log
        continue;
      }
      sb.append(" - ").append(diagnostic).append("\n");
    }
    return sb.toString();
  }
}
