/*
 * Copyright (C) 2011 Google Inc.
 * Copyright (C) 2012 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dagger.harness.javac;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

class InMemoryJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

  private final Map<String, InMemoryJavaFileObject> generatedClasses = new HashMap<String, InMemoryJavaFileObject>();
  private final Map<String, InMemoryJavaFileObject> generatedFiles = new HashMap<String, InMemoryJavaFileObject>();

  public InMemoryJavaFileManager(JavaFileManager fileManager) {
    super(fileManager);
  }

  public Map<String, InMemoryJavaFileObject> getGeneratedClasses() {
    return generatedClasses;
  }

  public Map<String, InMemoryJavaFileObject> getGeneratedFiles() {
    return generatedFiles;
  }

  @Override
  public FileObject getFileForOutput(Location location, String packageName, String relativeName,
      FileObject sibling) throws IOException {
    String path = packageName.replace('.', '/') + "/" + relativeName;
    return getFileForOutput(path, null, getKind(relativeName));
  }

  private Kind getKind(String name) {
    for (Kind kind : Kind.values()) {
      if (name.endsWith(kind.extension) && kind == Kind.OTHER) {
        return kind;
      }
    }
    return Kind.OTHER;
  }

  @Override
  public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind,
      FileObject sibling) throws IOException {
    String path = className.replace('.', '/') + kind.extension;
    return getFileForOutput(path, className, kind);
  }

  private JavaFileObject getFileForOutput(String path, String className, Kind kind) {
    InMemoryJavaFileObject result = new InMemoryJavaFileObject(path, kind);
    if (kind == Kind.CLASS) {
      if (className == null) {
        className = inferBinaryName(path);
      }
      generatedClasses.put(className, result);
    }
    generatedFiles.put(path, result);
    return result;
  }

  private String inferBinaryName(String path) {
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    int extPos = path.lastIndexOf('.');
    if (extPos >= 0) {
      path = path.substring(0, extPos);
    }
    return path.replace('/', '.');
  }

  @Override
  public boolean isSameFile(FileObject a, FileObject b) {
    if (a instanceof InMemoryJavaFileObject && b instanceof InMemoryJavaFileObject) {
      InMemoryJavaFileObject memoryA = (InMemoryJavaFileObject) a;
      InMemoryJavaFileObject memoryB = (InMemoryJavaFileObject) b;
      return memoryA.getKind().equals(memoryB.getKind())
          && memoryA.toUri().equals(memoryB.toUri());
    }
    return super.isSameFile(a, b);
  }

  @Override
  public String inferBinaryName(Location location, JavaFileObject file) {
    if (location.isOutputLocation() && file instanceof InMemoryJavaFileObject) {
      return inferBinaryName(((InMemoryJavaFileObject) file).toUri().getRawSchemeSpecificPart());
    }
    return super.inferBinaryName(location, file);
  }
}
