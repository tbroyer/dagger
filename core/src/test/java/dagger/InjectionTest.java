/*
 * Copyright (C) 2010 Google Inc.
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
package dagger;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.RandomAccess;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("unused")
@RunWith(Enclosed.class)
public final class InjectionTest {
  public static class BasicInjection {
    static class TestEntryPoint {
      @Inject Provider<G> gProvider;
    }

    @Module(entryPoints = TestEntryPoint.class)
    static class TestModule {
      @Provides E provideE(F f) {
        return new E(f);
      }
      @Provides F provideF() {
        return new F();
      }
    }

    @Test public void test() {
      TestEntryPoint entryPoint = new TestEntryPoint();
      ObjectGraph.get(new TestModule()).inject(entryPoint);
      G g = entryPoint.gProvider.get();
      assertThat(g.a).isNotNull();
      assertThat(g.b).isNotNull();
      assertThat(g.c).isNotNull();
      assertThat(g.d).isNotNull();
      assertThat(g.e).isNotNull();
      assertThat(g.e.f).isNotNull();
    }
  }

  static class A {
    @Inject A() {}
  }

  static class B {
    @Inject B() {}
  }

  @Singleton
  static class C {
    @Inject C() {}
  }

  @Singleton
  static class D {
    @Inject D() {}
  }

  static class E {
    F f;
    E(F f) {
      this.f = f;
    }
  }

  static class F {}

  static class G {
    @Inject A a;
    @Inject B b;
    C c;
    D d;
    @Inject E e;
    @Inject G(C c, D d) {
      this.c = c;
      this.d = d;
    }
  }

  public static class ProviderInjection {
    static class TestEntryPoint {
      @Inject Provider<A> aProvider;
    }

    @Module(entryPoints = TestEntryPoint.class)
    static class TestModule {
    }

    @Test public void test() {
      TestEntryPoint entryPoint = new TestEntryPoint();
      ObjectGraph.get(new TestModule()).inject(entryPoint);

      assertThat(entryPoint.aProvider.get()).isNotNull();
      assertThat(entryPoint.aProvider.get()).isNotNull();
      assertThat(entryPoint.aProvider.get()).isNotSameAs(entryPoint.aProvider.get());
    }
  }

  public static class Singletons {
    static class TestEntryPoint {
      @Inject Provider<F> fProvider;
      @Inject Provider<I> iProvider;
    }

    @Module(entryPoints = TestEntryPoint.class)
    static class TestModule {
      @Provides @Singleton F provideF() {
        return new F();
      }
    }

    @Test public void test() {
      TestEntryPoint entryPoint = new TestEntryPoint();
      ObjectGraph.get(new TestModule()).inject(entryPoint);
      assertThat(entryPoint.fProvider.get()).isSameAs(entryPoint.fProvider.get());
      assertThat(entryPoint.iProvider.get()).isSameAs(entryPoint.iProvider.get());
    }
  }

  @Singleton
  static class I {
    @Inject I() {}
  }

  public static class BindingAnnotations {
    static class TestEntryPoint {
      @Inject A a;
      @Inject @Named("one") A aOne;
      @Inject @Named("two") A aTwo;
    }

    @Module(entryPoints = TestEntryPoint.class)
    static class TestModule {
      final A one = new A();
      final A two = new A();

      @Provides @Named("one") A getOne() {
        return one;
      }
      @Provides @Named("two") A getTwo() {
        return two;
      }
    }

    @Test public void test() {
      TestEntryPoint entryPoint = new TestEntryPoint();
      TestModule module = new TestModule();
      ObjectGraph.get(module).inject(entryPoint);
      assertThat(entryPoint.a).isNotNull();
      assertThat(module.one).isSameAs(entryPoint.aOne);
      assertThat(module.two).isSameAs(entryPoint.aTwo);
    }
  }

  public static class SingletonBindingAnnotationAndProvider {
    static class TestEntryPoint {
      @Inject Provider<L> lProvider;
    }

    @Module(entryPoints = TestEntryPoint.class)
    static class TestModule {
      A a1;
      A a2;

      @Provides @Singleton @Named("one") F provideF(Provider<A> aProvider) {
        a1 = aProvider.get();
        a2 = aProvider.get();
        return new F();
      }
    }

    @Test public void test() {
      TestEntryPoint entryPoint = new TestEntryPoint();
      TestModule module = new TestModule();
      ObjectGraph.get(module).inject(entryPoint);
      entryPoint.lProvider.get();

      assertThat(module.a1).isNotNull();
      assertThat(module.a2).isNotNull();
      assertThat(module.a1).isNotSameAs(module.a2);
      assertThat(entryPoint.lProvider.get()).isSameAs(entryPoint.lProvider.get());
    }
  }

  @Singleton
  static class L {
    @Inject @Named("one") F f;
    @Inject Provider<L> lProvider;
  }

  public static class SingletonInGraph {
    static class TestEntryPoint {
      @Inject N n1;
      @Inject N n2;
      @Inject F f1;
      @Inject F f2;
    }

    @Module(entryPoints = TestEntryPoint.class)
    static class TestModule {
      @Provides @Singleton F provideF() {
        return new F();
      }
    }

    @Test public void test() {
      TestEntryPoint entryPoint = new TestEntryPoint();
      ObjectGraph.get(new TestModule()).inject(entryPoint);

      assertThat(entryPoint.f1).isSameAs(entryPoint.f2);
      assertThat(entryPoint.f1).isSameAs(entryPoint.n1.f1);
      assertThat(entryPoint.f1).isSameAs(entryPoint.n1.f2);
      assertThat(entryPoint.f1).isSameAs(entryPoint.n2.f1);
      assertThat(entryPoint.f1).isSameAs(entryPoint.n2.f2);
      assertThat(entryPoint.f1).isSameAs(entryPoint.n1.fProvider.get());
      assertThat(entryPoint.f1).isSameAs(entryPoint.n2.fProvider.get());
    }
  }

  static class N {
    @Inject F f1;
    @Inject F f2;
    @Inject Provider<F> fProvider;
  }

  public static class NoJitBindingsForAnnotations {
    static class TestEntryPoint {
      @Inject @Named("a") A a;
    }

    @Module(entryPoints = TestEntryPoint.class)
    static class TestModule {
    }

    private ObjectGraph graph;

    @Before
    public void setup() {
      graph = ObjectGraph.get(new TestModule());
    }

    @Test(expected = IllegalStateException.class)
    public void test() {
      graph.validate();
    }
  }

  public static class Subclasses {
    static class TestEntryPoint {
      @Inject Q q;
    }

    @Module(entryPoints = TestEntryPoint.class)
    static class TestModule {
      @Provides F provideF() {
        return new F();
      }
    }

    @Test public void test() {
      TestEntryPoint entryPoint = new TestEntryPoint();
      ObjectGraph.get(new TestModule()).inject(entryPoint);
      assertThat(entryPoint.q.f).isNotNull();
    }
  }

  static class P {
    @Inject F f;
  }

  static class Q extends P {
    @Inject Q() {}
  }

  public static class SingletonsAreNotEager {
    static class TestEntryPoint {
      @Inject Provider<A> aProvider;
    }

    @Module(entryPoints = TestEntryPoint.class)
    static class TestModule {
      boolean sInjected = false;

      @Provides F provideF(R r) {
        return new F();
      }

      @Provides @Singleton S provideS() {
        sInjected = true;
        return new S();
      }
    }

    @Test public void test() {
      R.injected = false;
      TestEntryPoint entryPoint = new TestEntryPoint();
      TestModule module = new TestModule();
      ObjectGraph.get(module).inject(entryPoint);
  
      assertThat(R.injected).isFalse();
      assertThat(module.sInjected).isFalse();
    }
  }

  @Singleton
  static class R {
    static boolean injected = false;
    @Inject R() {
      injected = true;
    }
  }

  static class S {}

  public static class ProviderMethodsConflict {
    @Module
    static class TestModule {
      @Provides A provideA1() {
        throw new AssertionError();
      }
      @Provides A provideA2() {
        throw new AssertionError();
      }
    }

    @Test(expected = IllegalArgumentException.class)
    public void test() {
      ObjectGraph.get(new TestModule());
    }
  }

  public static class SingletonsInjectedOnlyIntoProviders {
    static class TestEntryPoint {
      @Inject Provider<A> aProvider;
    }

    @Module(entryPoints = TestEntryPoint.class)
    static class TestModule {
      @Provides @Singleton A provideA() {
        return new A();
      }
    }

    @Test public void test() {
      TestEntryPoint entryPoint = new TestEntryPoint();
      ObjectGraph.get(new TestModule()).inject(entryPoint);
      assertThat(entryPoint.aProvider.get()).isSameAs(entryPoint.aProvider.get());
    }
  }

  public static class ModuleOverrides {
    static class TestEntryPoint {
      @Inject Provider<E> eProvider;
    }

    @Module(entryPoints = TestEntryPoint.class)
    static class BaseModule {
      @Provides F provideF() {
        throw new AssertionError();
      }
      @Provides E provideE(F f) {
        return new E(f);
      }
    }

    @Module(overrides = true)
    static class OverridesModule {
      @Provides F provideF() {
        return new F();
      }
    }

    @Test public void test() {
      TestEntryPoint entryPoint = new TestEntryPoint();
      ObjectGraph.get(new BaseModule(), new OverridesModule()).inject(entryPoint);
      E e = entryPoint.eProvider.get();
      assertThat(e).isNotNull();
      assertThat(e.f).isNotNull();
    }
  }

  public static class NoJitBindingsForInterfaces {
    static class TestEntryPoint {
      @Inject RandomAccess randomAccess;
    }

    @Module(entryPoints = TestEntryPoint.class)
    static class TestModule {
    }

    private ObjectGraph graph;

    @Before public void setup() {
      graph = ObjectGraph.get(new TestModule());
    }

    @Test(expected = IllegalStateException.class)
    public void test() {
      graph.validate();
    }
  }

  public static class NoProvideBindingsForAbstractClasses {
    static class TestEntryPoint {
      @Inject AbstractList abstractList;
    }

    @Module(entryPoints = TestEntryPoint.class)
    static class TestModule {
    }

    private ObjectGraph graph;

    @Before public void setup() {
      graph = ObjectGraph.get(new TestModule());
    }

    @Test(expected = IllegalStateException.class)
    public void test() {
      graph.validate();
    }
  }

  static class ExtendsParameterizedType extends AbstractList<Integer> {
    @Inject String string;
    @Override public Integer get(int i) {
      throw new AssertionError();
    }
    @Override public int size() {
      throw new AssertionError();
    }
  }

  /**
   * We've had bugs where we look for the wrong keys when a class extends a
   * parameterized class. Explicitly test that we can inject such classes.
   */
  public static class ExtendsParameterizedTypeTest {
    static class TestEntryPoint {
      @Inject ExtendsParameterizedType extendsParameterizedType;
    }

    @Module(entryPoints = TestEntryPoint.class)
    static class TestModule {
      @Provides String provideString() {
        return "injected";
      }
    }

    @Test public void test() {
      TestEntryPoint entryPoint = new TestEntryPoint();
      ObjectGraph.get(new TestModule()).inject(entryPoint);
      assertThat(entryPoint.extendsParameterizedType.string).isEqualTo("injected");
    }
  }

  public static class InjectParameterizedType {
    static class TestEntryPoint {
      @Inject List<String> listOfStrings;
    }

    @Module(entryPoints = TestEntryPoint.class)
    static class TestModule {
      @Provides List<String> provideList() {
        return Arrays.asList("a", "b");
      }
    }

    @Test public void test() {
      TestEntryPoint entryPoint = new TestEntryPoint();
      ObjectGraph.get(new TestModule()).inject(entryPoint);
      assertThat(entryPoint.listOfStrings).isEqualTo(Arrays.asList("a", "b"));
    }
  }

  public static class InjectWilcardType {
    static class TestEntryPoint {
      @Inject List<? extends Number> listOfNumbers;
    }

    @Module(entryPoints = TestEntryPoint.class)
    static class TestModule {
      @Provides List<? extends Number> provideList() {
        return Arrays.asList(1, 2);
      }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test() {
      ObjectGraph.get(new TestModule());
    }
  }

  public static class NoConstructorInjectionsForClassesWithTypeParameters {
    static class Parameterized<T> {
      @Inject String string;
    }

    static class TestEntryPoint {
      @Inject Parameterized<Long> parameterized;
    }

    @Module(entryPoints = TestEntryPoint.class)
    static class TestModule {
      @Provides String provideString() {
        return "injected";
      }
    }

    private ObjectGraph graph;

    @Before public void setup() {
      graph = ObjectGraph.get(new TestModule());
    }

    @Test(expected = IllegalStateException.class)
    public void test() {
      graph.validate();
    }
  }

  public static class ModuleWithNoProvidesMethods {
    @Module
    static class TestModule {
    }

    @Test public void test() {
      ObjectGraph.get(new TestModule());
    }
  }

  public static class GetInstance {

    @Module(entryPoints = Integer.class)
    static class TestModule {

      final AtomicInteger next = new AtomicInteger(0);

      @Provides Integer provideInteger() {
        return next.getAndIncrement();
      }
    }

    @Test public void test() {
      ObjectGraph graph = ObjectGraph.get(new TestModule());
      assertEquals(0, (int) graph.getInstance(Integer.class));
      assertEquals(1, (int) graph.getInstance(Integer.class));
    }
  }

  public static class GetInstanceRequiresEntryPoint {
    @Module
    static class TestModule {
      @Provides Integer provideInteger() {
        throw new AssertionError();
      }
    }

    private ObjectGraph graph;

    @Before public void setup() {
      graph = ObjectGraph.get(new TestModule());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test() {
      graph.getInstance(Integer.class);
    }
  }

  public static class GetInstanceOfPrimitive {
    @Module(entryPoints = int.class)
    static class TestModule {
      @Provides int provideInt() {
        return 1;
      }
    }

    @Test public void test() {
      ObjectGraph graph = ObjectGraph.get(new TestModule());
      assertEquals(1, (int) graph.getInstance(int.class));
    }
  }

  public static class GetInstanceOfArray {
    @Module(entryPoints = int[].class)
    static class TestModule {
      @Provides int[] provideIntArray() {
        return new int[] { 1, 2, 3 };
      }
    }

    @Test public void test() {
      ObjectGraph graph = ObjectGraph.get(new TestModule());
      assertEquals("[1, 2, 3]", Arrays.toString(graph.getInstance(int[].class)));
    }
  }

  public static class GetInstanceAndInjectMembersUseDifferentKeys {
    static class BoundTwoWays {
      @Inject String s;
    }

    @Module(entryPoints = BoundTwoWays.class)
    static class TestModule {
      @Provides
      BoundTwoWays provideBoundTwoWays() {
        BoundTwoWays result = new BoundTwoWays();
        result.s = "Pepsi";
        return result;
      }

      @Provides String provideString() {
        return "Coke";
      }
    }

    private ObjectGraph graph;

    @Before public void setup() {
      graph = ObjectGraph.get(new TestModule());
    }

    @Test public void testGetInstance() {
      BoundTwoWays provided = graph.getInstance(BoundTwoWays.class);
      assertEquals("Pepsi", provided.s);
    }

    @Test public void testInjectMembers() {
      BoundTwoWays membersInjected = new BoundTwoWays();
      graph.inject(membersInjected);
      assertEquals("Coke", membersInjected.s);
    }
  }

  static class NoInjections {
  }

  public static class EntryPointNeedsNoInjectAnnotation {
    @Module(entryPoints = NoInjections.class)
    static class TestModule {
    }

    @Test public void test() {
      ObjectGraph.get(new TestModule()).validate();
    }
  }

  public static class NonEntryPointNeedsInjectAnnotation {
    @Module
    static class TestModule {
      @Provides String provideString(NoInjections noInjections) {
        throw new AssertionError();
      }
    }

    ObjectGraph graph;

    @Before public void steup() {
      graph = ObjectGraph.get(new TestModule());
    }

    @Test(expected = IllegalStateException.class)
    public void test() {
      graph.validate();
    }
  }
}
