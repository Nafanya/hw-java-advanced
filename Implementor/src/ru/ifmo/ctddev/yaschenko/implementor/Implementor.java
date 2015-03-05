package ru.ifmo.ctddev.yaschenko.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Implementor implements Impler {

    private static final Map<String, String> DEFAULT_VALUES;
    static {
        DEFAULT_VALUES = new HashMap<>();
        DEFAULT_VALUES.put("boolean", "false");
        DEFAULT_VALUES.put("byte", "0");
        DEFAULT_VALUES.put("char", "0");
        DEFAULT_VALUES.put("short", "0");
        DEFAULT_VALUES.put("int", "0");
        DEFAULT_VALUES.put("long", "0L");
        DEFAULT_VALUES.put("float", "0f");
        DEFAULT_VALUES.put("double", "0");

        DEFAULT_VALUES.put("void", "");
    }

    @Override
    public void implement(Class<?> token, File root) throws ImplerException {
        if (token == null || token.isPrimitive() || Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException();
        }

        final File file;
        try {
            file = createPackageDirs(token, root);
        } catch (IOException e) {
            System.out.println("Can't create file");
            throw new ImplerException("Can't create file");
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(file)))) {

            writePackage(writer, token);
            writeClassDefinition(writer, token);
            writeConstructors(writer, token);
            writeMethods(writer, token);

            writer.write("}\n");

        } catch (FileNotFoundException e) {
            System.out.print("File not found: " + e.getMessage());
        } catch (IOException e) {
            System.out.print("IO error: " + e.getMessage());
        }
    }

    private File createPackageDirs(final Class<?> token, File root) throws IOException {
        String packagePath = "/" + token.getPackage().getName().replace('.', '/');
        String classPath = root.getAbsoluteFile() + packagePath + "/" + token.getSimpleName() + "Impl.java";
        File f = new File(classPath);
        f.getParentFile().mkdirs();
        return f;
    }

    private void writePackage(final BufferedWriter writer, final Class<?> token) throws IOException {
        if (token.getPackage() == null) {
            return;
        }
        writer.write("package ");
        writer.write(token.getPackage().getName());
        writer.write(";\n\n");
    }

    private void writeClassDefinition(final BufferedWriter writer, final Class<?> token) throws IOException {
        final String name = token.getSimpleName();
        final String classOrInterface = token.isInterface() ? "implements" : "extends";

        writer.write(String.format("public class %sImpl %s %s ", name, classOrInterface, name));
        writer.write("{\n");
    }

    private void writeConstructors(final BufferedWriter writer, final Class<?> token) throws ImplerException, IOException {
        if (token.isInterface()) {
            return;
        }
        final Constructor<?>[] allConstructors = token.getDeclaredConstructors();

        for (Constructor c : allConstructors) {
            if (Modifier.isPublic(c.getModifiers()) || Modifier.isProtected(c.getModifiers())) {
                if (c.getParameterTypes().length == 0) {
                    return;
                }
            }
        }

        for (Constructor c : allConstructors) {
            if (Modifier.isPublic(c.getModifiers()) || Modifier.isProtected(c.getModifiers())) {
                final boolean throwsExceptions = c.getExceptionTypes().length > 0;
                final String mods = Modifier.toString(c.getModifiers() & Modifier.constructorModifiers());
                final String name = token.getSimpleName() + "Impl";
                final String args =
                        IntStream.range(0, c.getParameterCount())
                                .mapToObj(i -> c.getParameterTypes()[i].getTypeName() + " arg" + i)
                                .collect(Collectors.joining(", ", "", ""));

                final String argsNames =
                        IntStream.range(0, c.getParameterCount())
                                .mapToObj(i -> "arg" + i)
                                .collect(Collectors.joining(", ", "", ""));

                final String thrown = (!throwsExceptions) ? "" :
                        Arrays.asList(c.getExceptionTypes())
                                .stream()
                                .map(Class::getTypeName)
                                .collect(Collectors.joining(", ", "", ""));

                writer.write(String.format("\t%s %s(%s) ", mods, name, args));
                if (throwsExceptions) {
                    writer.write(String.format("throws %s ", thrown));
                }
                writer.write(String.format("{ super(%s); }\n", argsNames));

                return;
            }
        }
        throw new ImplerException("No public or protected constructors found");
    }

    private String getMethodHash(Method method) {
        return method.getReturnType().getCanonicalName() +
                method.getName() +
                Arrays.asList(method.getParameterTypes())
                        .stream()
                        .map(Class::getCanonicalName)
                        .collect(Collectors.joining(","));
    }

    private void writeMethods(final BufferedWriter writer, final Class<?> token) throws IOException {
        Map<String, Method> needImplementationMethods = new HashMap<>();

        recursiveMethodWalk(token, method -> {
            if (Modifier.isAbstract(method.getModifiers())) {
                final String hash = getMethodHash(method);
                if (!needImplementationMethods.containsKey(hash)) {
                    needImplementationMethods.put(hash, method);
                }
            }
        });

        recursiveMethodWalk(token, method -> {
            if (!Modifier.isAbstract(method.getModifiers())) {
                final String hash = getMethodHash(method);
                if (needImplementationMethods.containsKey(hash)) {
                    needImplementationMethods.remove(hash);
                }
            }
        });

        for (Method m : needImplementationMethods.values()) {

            if (Modifier.isAbstract(m.getModifiers())) {
                final String modifier;
                if (Modifier.isPublic(m.getModifiers())) {
                    modifier = "public";
                } else if (Modifier.isProtected(m.getModifiers())) {
                    modifier = "protected";
                } else {
                    modifier = "";
                }


                String returns = "";
                Type type = m.getGenericReturnType();
                if (type instanceof TypeVariable) {
                    returns = "<" + ((TypeVariable) type).getName() + ">";
                }
                returns = returns + m.getGenericReturnType().getTypeName();

                final String name = m.getName();
                final String params = Arrays.asList(m.getParameters())
                        .stream()
                        .map(Parameter::toString)
                        .collect(Collectors.joining(", "));

                writer.write("\t@Override\n");
                writer.write(String.format("\t%s %s %s(%s)", modifier, returns, name, params));

                final boolean throwsExceptions = m.getExceptionTypes().length > 0;
                if (throwsExceptions) {
                    final String thrown = Arrays.asList(m.getExceptionTypes())
                            .stream()
                            .map(Class::getCanonicalName)
                            .collect(Collectors.joining(", ", "", ""));

                    writer.write(String.format(" throws %s", thrown));
                }
                final String ret = "return " + getDefaultValueString(m.getReturnType()) + ";";
                writer.write(String.format("{ %s }\n\n", ret));
            }
        }
    }

    private void recursiveMethodWalk(Class<?> clazz, MethodVisitor visitor) {
        if (clazz == null) {
            return;
        }

        for (Method m : clazz.getDeclaredMethods()) {
            visitor.onVisitMethod(m);
        }
        for (Class<?> iface : clazz.getInterfaces()) {
            recursiveMethodWalk(iface, visitor);
        }
        recursiveMethodWalk(clazz.getSuperclass(), visitor);
    }

    private String getDefaultValueString(final Class<?> returnType) {
        if (returnType.isPrimitive()) {
            return DEFAULT_VALUES.get(returnType.getName());
        } else {
            return "null";
        }
    }

    private static interface MethodVisitor {
        public void onVisitMethod(Method method);
    }

}
