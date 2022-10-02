package com.github.kr328.gradle.golang;

import com.android.build.gradle.BaseExtension;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.gradle.api.GradleException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.apache.tools.ant.taskdefs.condition.Os.*;

@Data
@EqualsAndHashCode
public class Variant implements Serializable {
    public enum BuildMode {
        Executable, Shared, Archive
    }

    public enum Arch {
        Arm7, Arm8, I386, Amd64
    }

    public enum Os {
        Android, Windows, Linux, Darwin
    }

    private final String name;

    @Nullable
    private String fileName;
    @Nullable
    private BuildMode buildMode;
    @Nullable
    private Arch arch;
    @Nullable
    private Os os;
    @Nullable
    private String packageName;
    @Nullable
    private Cgo cgo;
    @Nullable
    private List<String> tags;
    @Nullable
    private List<String> flags;
    private boolean strip;

    public Variant(String name) {
        this.name = name;
    }

    @Data
    @EqualsAndHashCode
    @AllArgsConstructor
    public static class Cgo implements Serializable {
        @Nonnull
        private final File compiler;

        @Nonnull
        public static Cgo fromAndroid(@Nonnull BaseExtension extension, @Nonnull Arch arch) {
            final File ndkDir = extension.getNdkDirectory();
            final int minSdk = Optional.ofNullable(extension.getDefaultConfig().getMinSdk())
                    .orElseThrow(() -> new GradleException("minSdk required"));

            final ArrayList<String> compilerPath = new ArrayList<>();

            compilerPath.add(ndkDir.getAbsolutePath());
            compilerPath.add("toolchains");
            compilerPath.add("llvm");
            compilerPath.add("prebuilt");

            if (isFamily(FAMILY_WINDOWS)) {
                compilerPath.add("windows-x86_64");
            } else if (isFamily(FAMILY_MAC)) {
                compilerPath.add("darwin-x86_64");
            } else if (isFamily(FAMILY_UNIX)) {
                compilerPath.add("linux-x86_64");
            } else {
                throw new GradleException("Unsupported platform: " + System.getProperty("os.name"));
            }

            compilerPath.add("bin");

            final String compilerPrefix;
            switch (arch) {
                case Arm8:
                    compilerPrefix = "aarch64-linux-android";
                    break;
                case Arm7:
                    compilerPrefix = "armv7a-linux-androideabi";
                    break;
                case I386:
                    compilerPrefix = "i686-linux-android";
                    break;
                case Amd64:
                    compilerPrefix = "x86_64-linux-android";
                    break;
                default:
                    throw new GradleException("Unsupported abi: " + arch);
            }

            compilerPath.add(compilerPrefix + minSdk + "-clang");

            return new Cgo(new File(String.join(File.separator, compilerPath)));
        }
    }
}
