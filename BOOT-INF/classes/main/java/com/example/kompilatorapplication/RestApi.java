package com.example.kompilatorapplication;
import com.example.kompilatorapplication.kody.Zadanie;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;

@RestController
@CrossOrigin("*")
public class RestApi {
    private static String wynik;
    private static Stack<String> sciezkaPliku = new Stack<>();

    public record WynikUzytkownika(String string) {
        public String getWynik() {
            return string;
        }
    }

    @PostMapping("/wynik")
    public ResponseEntity<String> odczytanieKlasy() {
        File classFile = new File("src/main/java/com/example/kompilatorapplication/kody/Zadanie.class");
        URL classUrl = null;
        try {
            classUrl = classFile.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        URL[] classUrls = {classUrl};
        ClassLoader cl = new URLClassLoader(classUrls);
        Class<?> clazz = null;

        try{
            clazz = cl.loadClass("com.example.kompilatorapplication.kody.Zadanie");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Method mainMethod = null;
        try {
            mainMethod = clazz.getMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream old = System.out;
        System.setOut(ps);
        try {
            mainMethod.invoke(null, (Object) new String[]{});
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        System.out.flush();
        System.setOut(old);
        wynik = baos.toString();
        return ResponseEntity.ok(baos.toString());
    }
    @PostMapping("/kompilator2")
    public void getResult(@RequestBody WynikUzytkownika wynikUzytkownika) {

        Path path = FileSystems.getDefault().getPath("src/main/java/com/example/kompilatorapplication/kody/Zadanie.java");
        Path path2 = FileSystems.getDefault().getPath("src/main/java/com/example/kompilatorapplication/kody/Zadanie.class");
        try {
            Files.delete(path);
            Files.delete(path2);
            System.out.println("File deleted");
        } catch (Exception e) {
            System.out.println("zła scieżka pliku");
        }
        FileWriter writer = null;
        try {
            writer = new FileWriter("src/main/java/com/example/kompilatorapplication/kody/Zadanie.java");
            byte[] decoded = Base64.getDecoder().decode(wynikUzytkownika.getWynik());
            String decodedStr = new String(decoded, StandardCharsets.UTF_8);

            writer.write(decodedStr);
            writer.close();
            System.out.println("String written to file successfully.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        File file = new File("src/main/java/com/example/kompilatorapplication/kody/Zadanie.java");
        try {
            try {
                kompilator();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }

        String wynikZapytania = wynik;

        System.exit(1);


    }





    public void kompilator() throws
            ClassNotFoundException,
            IllegalAccessException,
            InstantiationException, NoSuchMethodException {
        String fileToCompile = "src/main/java/com/example/kompilatorapplication/kody/Zadanie.java";
        sciezkaPliku.push(fileToCompile);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int compilationResult = compiler.run(null, null, null, sciezkaPliku.peek());
        if (compilationResult == 0) {
            System.out.println("Compilation is successful");
        } else {
            System.out.println("Compilation Failed");
            Path path = FileSystems.getDefault().getPath("src/main/java/com/example/kompilatorapplication/kody/Zadanie.java");
            try {
                Files.delete(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public class CustomClassLoader extends ClassLoader {
        private final Map<String, Class<?>> classes = new HashMap<>();

        public CustomClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Class<?> loadClass(String s) {
            try {
                return findClass(s);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }


        @Override
        public Class findClass(String name) throws ClassNotFoundException {
            try {
                byte[] bytes = loadClassFromFile(name);
                return defineClass(name, bytes, 0, bytes.length);
            } finally{
                try {
                    return super.loadClass(name);
                } catch (ClassNotFoundException ignore) {
                    ignore.printStackTrace(System.out);
                }

                return null;
            }
        }

        private byte[] loadClassFromFile(String fileName) {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(
                    fileName.replace('.', File.separatorChar) + ".class");
            byte[] buffer;
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            int nextValue = 0;
            try {
                while ((nextValue = inputStream.read()) != -1) {
                    byteStream.write(nextValue);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            buffer = byteStream.toByteArray();
            return buffer;
        }
    }
}

