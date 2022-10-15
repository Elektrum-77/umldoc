package com.github.pereiratostain;

import com.github.forax.umldoc.core.Entity;
import com.github.forax.umldoc.core.Entity.Stereotype;
import com.github.forax.umldoc.core.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.RecordComponentVisitor;


class Visitor extends ClassVisitor {
  private Entity entity = null;

  public Visitor(int api) {
    super(api);
  }

  public Entity getEntity() {
    return this.entity;
  }

  @Override
  public void visit(int version, int access, String name, String signature,
                    String superName, String[] interfaces) {

    var modif = new HashSet<com.github.forax.umldoc.core.Modifier>();
    modif.add(modifier(access));
    name = name.substring(name.lastIndexOf("/") + 1);
    name = name.replace('-', '_');
    name = name.replace('$', ' ');
    this.entity = new Entity(modif, name, Stereotype.CLASS,
            List.of(), List.of());
  }

  /**
   * Converts an int to a Modifier.
   *
   * @param modifier The int to convert
   * @return A value of the enum Modifier
   */
  private static com.github.forax.umldoc.core.Modifier modifier(int modifier) {
    return switch (modifier) {
      case Modifier.PUBLIC -> com.github.forax.umldoc.core.Modifier.PUBLIC;
      case Modifier.PRIVATE -> com.github.forax.umldoc.core.Modifier.PRIVATE;
      case Modifier.PROTECTED -> com.github.forax.umldoc.core.Modifier.PROTECTED;
      default -> com.github.forax.umldoc.core.Modifier.PACKAGE;
    };
  }

  @Override
  public RecordComponentVisitor visitRecordComponent(String name, String descriptor,
                                                     String signature) {
    return null;
  }

  @Override
  public FieldVisitor visitField(int access, String name, String descriptor, String signature,
                                 Object value) {
    var modifier = new HashSet<com.github.forax.umldoc.core.Modifier>();
    modifier.add(modifier(access));
    descriptor = descriptor.substring(descriptor.lastIndexOf("/") + 1);
    if (signature != null) {
      signature = signature.substring(signature.lastIndexOf("/") + 1);
      signature = signature.substring(0, signature.indexOf(';'));
      System.out.println(signature);
      descriptor = descriptor + "<" + signature + ">";
    }
    var fields = new ArrayList<>(this.entity.fields());
    var field = new Field(modifier, name, descriptor);
    fields.add(field);
    this.entity = new Entity(entity.modifiers(), entity.name(), entity.stereotype(), fields,
            List.of());
    return null;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                   String[] exceptions) {
    return null;
  }
}
