package com.github.kr328.gradle.golang;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class BuildTask extends DefaultTask {
    @InputDirectory
    @Nonnull
    public abstract DirectoryProperty getModuleDirectory();

    @Input
    @Nonnull
    public abstract Property<Variant> getVariant();

    @OutputDirectory
    @Nonnull
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void build() {
        final File moduleDir = getModuleDirectory().getAsFile().get();
        final File outputDir = getOutputDirectory().getAsFile().get();
        final Variant variant = getVariant().get();

        try (final DirectoryStream<Path> files = Files.newDirectoryStream(outputDir.toPath())) {
            for (final Path file : files) {
                Files.delete(file);
            }
        } catch (IOException e) {
            // ignore
        }

        final HashMap<String, String> environment = new HashMap<>(System.getenv());
        if (variant.getOs() != null) {
            switch (variant.getOs()) {
                case Android:
                    environment.put("GOOS", "android");
                    break;
                case Windows:
                    environment.put("GOOS", "windows");
                    break;
                case Linux:
                    environment.put("GOOS", "linux");
                    break;
                case Darwin:
                    environment.put("GOOS", "darwin");
                    break;
            }
        }
        if (variant.getArch() != null) {
            switch (variant.getArch()) {
                case Arm7:
                    environment.put("GOARCH", "arm");
                    environment.put("GOARM", "7");
                    break;
                case Arm8:
                    environment.put("GOARCH", "arm64");
                    break;
                case I386:
                    environment.put("GOARCH", "386");
                    break;
                case Amd64:
                    environment.put("GOARCH", "amd64");
                    break;
            }
        }
        if (variant.getCgo() != null) {
            environment.put("CGO_ENABLED", "1");
            environment.put("CC", variant.getCgo().getCompiler().getPath());
            environment.put("CFLAGS", "-O3 -Werror");
        } else {
            environment.put("CGO_ENABLED", "0");
        }

        final String outputPath;
        if (variant.getFileName() != null) {
            outputPath = String.join(File.separator, outputDir.getAbsolutePath(), variant.getFileName());
        } else {
            outputPath = outputDir.getAbsolutePath() + File.separator;
        }
        final ArrayList<String> commands = new ArrayList<>(
                List.of("go", "build", "-trimpath", "-o", outputPath)
        );
        if (variant.getBuildMode() != null) {
            switch (variant.getBuildMode()) {
                case Executable:
                    commands.add("-buildmode");
                    commands.add("exe");
                    break;
                case Shared:
                    commands.add("-buildmode");
                    commands.add("c-shared");
                    break;
                case Archive:
                    commands.add("-buildmode");
                    commands.add("c-archive");
            }
        }
        if (variant.getTags() != null) {
            commands.add("-tags");
            commands.add(String.join(",", variant.getTags()));
        }
        if (variant.getFlags() != null) {
            commands.addAll(variant.getFlags());
        }
        if (variant.getPackageName() != null) {
            commands.add(variant.getPackageName());
        }

        getProject().exec((spec) -> {
            spec.environment(environment);
            spec.workingDir(moduleDir);
            spec.commandLine(commands);
        }).assertNormalExitValue();
    }
}
