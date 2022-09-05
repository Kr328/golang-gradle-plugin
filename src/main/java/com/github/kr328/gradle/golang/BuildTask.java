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

        final ArrayList<String> commands = new ArrayList<>(
                List.of("go", "build", "-trimpath", "-o", outputDir.getAbsolutePath())
        );
        if (variant.getBuildMode() != null) {
            switch (variant.getBuildMode()) {
                case Executable:
                    commands.add("-buildmode");
                    commands.add("exe");
                    break;
                case Library:
                    commands.add("-buildmode");
                    commands.add("c-library");
                    break;
            }
        }
        if (variant.getTags() != null) {
            commands.add("-tags");
            commands.add(String.join(",", variant.getTags()));
        }
        if (variant.isStrip()) {
            commands.add("-ldflags");
            commands.add("-s -w");
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
