package com.github.forax.umldoc.classfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.forax.umldoc.classfile.ClassFileParser.ParsingResult;
import com.github.forax.umldoc.core.Call;
import com.github.forax.umldoc.core.Method;
import com.github.forax.umldoc.core.Modifier;
import com.github.forax.umldoc.core.Package;
import com.github.forax.umldoc.core.TypeInfo;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.forax.umldoc.gen.PlantUmlGenerator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ClassFileParserTest {
  private static ParsingResult parseClass(Class<?> clazz) throws IOException {
    try(var inputStream = ClassFileParserTest.class.getResourceAsStream("/"  + clazz.getName().replace('.', '/') + ".class")) {
      return ClassFileParser.parseClass(inputStream);
    }
  }

  static class EmptyClass { }

  @Test
  public void parseEmptyClass() throws IOException {
    var parsingResult = parseClass(EmptyClass.class);
    assertEquals(EmptyClass.class.getName(), parsingResult.entityBuilder().type().name());
  }

  static class Company {
    Employee employee;
  }
  static class Employee {
    String name;
  }

  @Test
  public void parseCompanyClass() throws IOException {
    var parsingResult = parseClass(Company.class);
    assertEquals(Company.class.getName(), parsingResult.entityBuilder().type().name());
    assertEquals(
            List.of(new Delegation(Set.of(), TypeInfo.of(Employee.class.getName()), "employee")),
            parsingResult.delegations());
  }

  /*
  static class Storage {
    List<Pet> petList;
    Map<String, Pet> petMap;
    Stream<Pet> petStream;
    Optional<Pet> petOptional;
  }
  static class Pet {
    String name;
  }

  @Test
  public void parseStorageClass() throws IOException {
    var parsingResult = parseClass(Storage.class);
    assertEquals(Storage.class.getName(), parsingResult.entityBuilder().name());
    assertEquals(
        Set.of(
            new Delegation(Set.of(), Storage.class.getName(), Employee.class.getName(), "employee", Cardinality.ONLY_ONE, Employee.class.getName()),

            ),
        Set.copyOf(parsingResult.delegations()));
  }*/

  static class Tv {
    public List<String> getChannel() {
      return List.of();
    }

    public boolean changeChannel(String channel) {
      return true;
    }

    private static void shutdown() {
      return;
    }
  }

  static class Remote {
    public void changeChannel(Tv tv) {
      tv.changeChannel("toto");
    }

    public int countChannel(Tv tv) {
      var channels = tv.getChannel();
      return channels.size();
    }
  }

  @Test
  public void parseTvClass() throws IOException {
    var parsingResult = parseClass(Tv.class).entityBuilder().build();
    var methods = List.of(
            new Method(
                    Set.of(Modifier.PUBLIC),
                    "getChannel",
                    new TypeInfo(Optional.empty(), List.class.getName(),List.of(TypeInfo.of(String.class.getName()))),
                    List.of(),
                    "()Ljava/util/List;",
                    Call.Group.EMPTY_GROUP
            ),
            new Method(
                    Set.of(Modifier.PUBLIC),
                    "changeChannel",
                    TypeInfo.of(boolean.class.getName()),
                    List.of(new Method.Parameter("", TypeInfo.of(String.class.getName()))),
                    "(Ljava/lang/String;)Z",
                    Call.Group.EMPTY_GROUP
            ),
            new Method(
                    Set.of(Modifier.PRIVATE, Modifier.STATIC),
                    "shutdown",
                    TypeInfo.of(void.class.getName()),
                    List.of(),
                    "()V",
                    Call.Group.EMPTY_GROUP
            )
    );
    assertEquals(Tv.class.getName(), parsingResult.type().name());
    var p = new Package(Tv.class.getPackageName(), List.of(), List.of());

    var tvMethods = parsingResult.methods().stream()
            .map(method -> new Method(method.modifiers(),
                method.name(),
                method.returnTypeInfo(), method.parameters(),
                method.descriptor(),
                new Call.Group(method.callGroup().kind(), PlantUmlGenerator.relevantCallsGroup(method.callGroup(), p).calls())))
            .toList();

    assertEquals(methods, tvMethods);

//    assertEquals(methods, parsingResult.methods());
  }

  @Test
  @Disabled
  public void parseRemote() throws IOException {
    var parsingResult = parseClass(Remote.class).entityBuilder().build();
    var methods = List.of(
            new Method(
                    Set.of(Modifier.PUBLIC),
                    "changeChannel",
                    TypeInfo.of(void.class.getName()),
                    List.of(new Method.Parameter("", TypeInfo.of(Tv.class.getName()))),
                    "(" + Tv.class.descriptorString() + ")V",
                    new Call.Group(Call.Group.Kind.NONE,
                            List.of(new Call.MethodCall(
                                    Tv.class.getName(),
                                    "changeChannel",
                                    "(Ljava/lang/String;)Z")
                            ))
            ),
            new Method(
                    Set.of(Modifier.PUBLIC),
                    "countChannel",
                    TypeInfo.of(int.class.getName()),
                    List.of(new Method.Parameter("", TypeInfo.of(Tv.class.getName()))),
                    "(" + Tv.class.descriptorString() + ")I",
                    new Call.Group(Call.Group.Kind.NONE,
                            List.of(new Call.MethodCall(
                                    Tv.class.getName(),
                                    "getChannel",
                                    "(Ljava/util/List;)V")
                            ))
            )
    );
    assertEquals(Remote.class.getName(), parsingResult.type().name());

    var p = new Package(Remote.class.getPackageName(), List.of(), List.of());
    var remoteMethods = parsingResult.methods().stream()
            .map(method -> new Method(method.modifiers(),
                method.name(),
                method.returnTypeInfo(),
                method.parameters(),
                method.descriptor(),
                new Call.Group(method.callGroup().kind(), PlantUmlGenerator.relevantCallsGroup(method.callGroup(), p).calls())
            ))
            .toList();

    assertEquals(methods, remoteMethods);
  }

  @Test
  public void canNotBeInstantiated() throws InstantiationException, IllegalAccessException {
    var constructor = ClassFileParser.class.getDeclaredConstructors()[0];
    constructor.setAccessible(true);
    try {
      constructor.newInstance();
      fail();
    } catch (InvocationTargetException e) {
      assertInstanceOf(AssertionError.class, e.getCause());
    }
  }
}