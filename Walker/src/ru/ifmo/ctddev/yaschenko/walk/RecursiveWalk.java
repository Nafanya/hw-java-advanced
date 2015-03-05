package ru.ifmo.ctddev.yaschenko.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;


public class RecursiveWalk {
    static final int PRIME = 0x01000193;
    static final int BUFF_SIZE = 4096;
    static final int INIT_HASH = 0x811c9dc5;
    static final String ERROR_HASH = String.format("%08x", 0x0);

    public static void main(String[] args) {
        if ((args == null) || (args.length != 2) || (args[0] == null) || (args[1] == null)) {
            System.out.println("Usage: java RecursiveWalk <input> <output>");
            return;
        }
        String inputName = args[0];
        String outputName = args[1];
        long startTime = System.nanoTime();
        try (InputStreamReader isReader = new InputStreamReader(new FileInputStream(inputName), StandardCharsets.UTF_8);
             OutputStreamWriter osWriter = new OutputStreamWriter(new FileOutputStream(outputName), StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isReader, BUFF_SIZE);
             BufferedWriter writer = new BufferedWriter(osWriter, BUFF_SIZE)) {
            String path;
            while ((path = reader.readLine()) != null) {
                fileVisitor(Paths.get(path), writer);
            }
        } catch (IOException e) {
            System.out.println("IO error");
        }
        long finishTime = System.nanoTime();
        System.out.println((finishTime - startTime) / 1e9);
    }

    private static void fileVisitor(Path startPath, final BufferedWriter writer) {
        try {
            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    print(getHash(file.toFile()) + " " + file + '\n', writer);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    print(ERROR_HASH + " " + file.toString() + '\n', writer);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            print(ERROR_HASH + " " + startPath.toString() + '\n', writer);
        }
    }

    private static String getHash(File file) {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file), BUFF_SIZE)) {
            int hash = INIT_HASH;
            byte[] bytes = new byte[BUFF_SIZE];
            int nBytes;
            while ((nBytes = bis.read(bytes, 0, BUFF_SIZE)) != -1) {
                for (int i = 0; i < nBytes; i++) {
                    hash = (hash * PRIME) ^ (bytes[i] & 0xff);
                }
            }
            return String.format("%08x", hash);
        } catch (IOException e) {
            return ERROR_HASH;
        }
    }

    private static void print(String s, final BufferedWriter writer) {
        try {
            writer.write(s);
        } catch (IOException e) {
            System.out.println("Error while writing result");
        }
    }
}
