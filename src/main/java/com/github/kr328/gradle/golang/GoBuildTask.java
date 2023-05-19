package com.github.kr328.gradle.golang;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public abstract class GoBuildTask extends DefaultTask {
    @InputDirectory
    @Nonnull
    public abstract DirectoryProperty getModuleDirectory();

    @Input
    @Nonnull
    public abstract Property<GoVariant> getVariant();

    @OutputDirectory
    @Nonnull
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void build() {
        final File moduleDir = getModuleDirectory().getAsFile().get();
        final File outputDir = getOutputDirectory().getAsFile().get();
        final GoVariant variant = getVariant().get();

        try (final DirectoryStream<Path> files = Files.newDirectoryStream(outputDir.toPath())) {
            for (final Path file : files) {
                Files.delete(file);
            }
        } catch (final IOException e) {
            // ignore
        }

        final HashMap<String, String> environment = new HashMap<>(System.getenv());

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

        switch (variant.getBuildMode()) {
            case Executable:
                commands.add("-buildmode");
                commands.add("exe");
                break;
            case PIE:
                commands.add("-buildmode");
                commands.add("pie");
            case Shared:
                commands.add("-buildmode");
                commands.add("c-shared");
                break;
            case Archive:
                commands.add("-buildmode");
                commands.add("c-archive");
            default:
                throw new GradleException("Unsupported build mode " + variant.getBuildMode());
        }

        commands.add("-tags");
        commands.add(String.join(",", variant.getTags()));

        final List<String> asmFlags = new ArrayList<>();
        final List<String> gcFlags = new ArrayList<>();
        final List<String> ldFlags = new ArrayList<>();

        final Iterator<String> flagIterator = variant.getFlags().iterator();
        while (flagIterator.hasNext()) {
            final String flag = flagIterator.next();
            switch (flag) {
                case "-asmflags":
                    asmFlags.add(flagIterator.next());
                    break;
                case "-gcflags":
                    gcFlags.add(flagIterator.next());
                    break;
                case "-ldflags":
                    ldFlags.add(flagIterator.next());
                    break;
                default:
                    commands.add(flag);
                    break;
            }
        }

        if (!asmFlags.isEmpty()) {
            commands.add("-asmflags");
            commands.add(String.join(" ", asmFlags));
        }

        if (!gcFlags.isEmpty()) {
            commands.add("-gcflags");
            commands.add(String.join(" ", gcFlags));
        }

        if (!ldFlags.isEmpty()) {
            commands.add("-ldflags");
            commands.add(String.join(" ", ldFlags));
        }

        commands.add(variant.getPackageName());

        getProject().exec((spec) -> {
            spec.environment(environment);
            spec.workingDir(moduleDir);
            spec.commandLine(commands);
        }).assertNormalExitValue();
    }
}
