package com.github.kr328.gradle.golang;

import com.android.build.gradle.BaseExtension;
import lombok.*;
import org.gradle.api.GradleException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@EqualsAndHashCode
public class GoVariant implements Serializable {
    private final String name;

    @Nullable
    private String fileName;
    @Nonnull
    private BuildMode buildMode = BuildMode.Executable;
    @Nonnull
    private Arch arch = Arch.current;
    @Nonnull
    private Os os = Os.current;
    @Nonnull
    private String packageName = "";
    @Nonnull
    private List<String> tags = new ArrayList<>();
    @Nonnull
    private List<String> flags = new ArrayList<>();
    @Nullable
    private Cgo cgo = null;

    public GoVariant(final String name) {
        this.name = name;
    }

    public enum BuildMode {
        Executable, PIE, Shared, Archive
    }

    public enum Arch {
        Arm7, Arm8, I386, Amd64;

        public static final Arch current;

        static {
            final String osArch = System.getProperty("os.arch").toLowerCase();

            if (osArch.contains("amd64") || osArch.contains("x86_64")) {
                current = Amd64;
            } else if (osArch.contains("x86") || osArch.contains("386") || osArch.contains("686")) {
                current = I386;
            } else if (osArch.contains("aarch64") || osArch.contains("armv8")) {
                current = Arm8;
            } else if (osArch.contains("armv7")) {
                current = Arm7;
            } else {
                current = Amd64;
            }
        }
    }

    public enum Os {
        Android, Windows, Linux, Darwin;

        public static final Os current;

        static {
            final String osName = System.getProperty("os.name").toLowerCase();

            if (osName.contains("windows")) {
                current = Windows;
            } else if (osName.contains("linux")) {
                current = Linux;
            } else if (osName.contains("macos") || osName.contains("osx") || osName.contains("darwin")) {
                current = Darwin;
            } else {
                current = Linux;
            }
        }
    }

    @Data
    @EqualsAndHashCode
    @AllArgsConstructor
    public static class Cgo implements Serializable {
        @Nonnull
        private final String cc;

        @Nonnull
        public static Cgo fromAndroid(
                @Nonnull final BaseExtension extension,
                @Nonnull final Arch arch,
                @Nullable final String flags
        ) {
            final File ndkDir = extension.getNdkDirectory();
            final int minSdk = Optional.ofNullable(extension.getDefaultConfig().getMinSdk())
                    .orElseThrow(() -> new GradleException("minSdk required"));

            final ArrayList<String> compilerPath = new ArrayList<>();

            compilerPath.add(ndkDir.getAbsolutePath());
            compilerPath.add("toolchains");
            compilerPath.add("llvm");
            compilerPath.add("prebuilt");

            switch (Os.current) {
                case Windows -> compilerPath.add("windows-x86_64");
                case Linux -> compilerPath.add("linux-x86_64");
                case Darwin -> compilerPath.add("darwin-x86_64");
                default -> throw new GradleException("Unsupported platform: " + Os.current);
            }

            compilerPath.add("bin");

            final String compilerPrefix = switch (arch) {
                case Arm8 -> "aarch64-linux-android";
                case Arm7 -> "armv7a-linux-androideabi";
                case I386 -> "i686-linux-android";
                case Amd64 -> "x86_64-linux-android";
            };

            compilerPath.add(compilerPrefix + minSdk + "-clang");

            return new Cgo("\"" +String.join(File.separator, compilerPath) + "\" " + flags);
        }
    }
}
