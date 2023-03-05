package com.github.kr328.gradle.golang;

import org.gradle.api.GradleException;
import org.gradle.api.Project;

import javax.annotation.Nonnull;
import java.nio.file.Paths;

public class GoPlugin implements org.gradle.api.Plugin<Project> {
    @Nonnull
    private static String capitalize(@Nonnull final String input) {
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }

    @Override
    public void apply(@Nonnull final Project target) {
        final GoExtension extension = target.getExtensions().create("golang", GoExtension.class);

        extension.getVariants().all(variant -> {
            final String name = "compileGolang" + capitalize(variant.getName());

            target.getTasks().register(name, GoBuildTask.class, (task) -> {
                task.getModuleDirectory().set(extension.getModuleDirectory());
                task.getVariant().set(variant);
                task.getOutputDirectory().set(
                        Paths.get(target.getBuildDir().getAbsolutePath(), "outputs", "golang", variant.getName()).toAbsolutePath().toFile()
                );
            });
        });
        extension.getVariants().whenObjectRemoved(variant -> {
            throw new GradleException("Remove variant may cause tasks dependency issues", new UnsupportedOperationException());
        });
    }
}
