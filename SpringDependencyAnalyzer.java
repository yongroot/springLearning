package org.example;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class SpringDependencyAnalyzer {

    private static final Set<String> BEAN_ANNOTATIONS = Set.of("Component", "Service", "Repository");
    private static final String AUTOWIRED_ANNOTATION = "Autowired";

    private static final Map<String, Set<String>> dependencyGraph = new LinkedHashMap<>();
    private static Map<String, Set<String>> interfaceImplementations = new HashMap<>();

    public static void main(String[] args) throws IOException {
        String projectDir = "/Users/tofi/Documents/code/xianliu"; // 修改为你的项目路径
        Files.walk(Paths.get(projectDir))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        analyzeFile(path.toFile());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });


        // Detect circular dependencies
//        for (String className : dependencyGraph.keySet()) {
//            detectCircularDependency(className, new HashSet<>(), new Stack<>());
//        }

        dependencyGraph.forEach((cln, dSet) -> {
            Stack<String>stack = new Stack<>();
            stack.push(cln);
            for (String c : dSet) {
                checkAc(cln, c, stack);

            }
        });

        System.out.println(coll);
    }

    private static final List<List<String>> coll  = new ArrayList<>();


    private static void checkAc(final String cln, String d, Stack<String> stack) {
        if (cln.equals(d)) {
//            System.out.println(stack);
            coll.add(new ArrayList<>(stack));
            stack.pop();
            return;
        }
        stack.push(d);

        Set<String> children = dependencyGraph.get(d);
        if (children != null) {
            for (String d2 : children) {
                checkAc(cln, d2, stack);
            }
        }
        stack.pop();
    }

    private static void analyzeFile(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        CompilationUnit cu = StaticJavaParser.parse(in);

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(c -> {
            final String className = c.getNameAsString();
            c.getAnnotations().forEach(a -> {
                if (BEAN_ANNOTATIONS.contains(a.getNameAsString())) {
                    dependencyGraph.putIfAbsent(className, new HashSet<>());
                    c.findAll(FieldDeclaration.class).forEach(f -> {

                        if (f.getVariables().get(0).getInitializer().isEmpty()) {
                            String dependencyType = f.getElementType().asString();
                            dependencyGraph.get(className).add(dependencyType);
                        }
                    });
                }
            });

            if (c.isInterface()) {
                interfaceImplementations.put(className, new HashSet<>());
            } else {
                c.getImplementedTypes().forEach(impl -> {
                    interfaceImplementations.computeIfAbsent(impl.getNameAsString(), k -> new HashSet<>()).add(className);
                });
            }
        });

        // Resolve interface dependencies
        for (String className : new HashSet<>(dependencyGraph.keySet())) {
            Set<String> dependencies = dependencyGraph.get(className);
            Set<String> resolvedDependencies = new HashSet<>();
            for (String dependency : dependencies) {
                if (interfaceImplementations.containsKey(dependency)) {
                    resolvedDependencies.addAll(interfaceImplementations.get(dependency));
                } else {
                    resolvedDependencies.add(dependency);
                }
            }
            dependencyGraph.put(className, resolvedDependencies);
        }



    }


    private static void detectCircularDependency(String className, Set<String> visited, Stack<String> stack) {
        // 跳过已经检查的
        if (visited.contains(className)) {
            return;
        }
        if (stack.contains(className)) {
            System.out.println("Circular dependency detected: " + stack + " -> " + className);
            return;
        }
        stack.push(className);
        Set<String> dependencies = dependencyGraph.get(className);
        if (dependencies != null) {
            for (String dependency : dependencies) {
                detectCircularDependency(dependency, visited, stack);
            }
        }
        stack.pop();
        visited.add(className);
    }
}
