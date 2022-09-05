package com.github.kr328.gradle.golang;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;

import javax.annotation.Nonnull;

public abstract class Extension {
    @Nonnull
    public abstract DirectoryProperty getModuleDirectory();

    @Nonnull
    public abstract NamedDomainObjectContainer<Variant> getVariants();

    public void variants(@Nonnull Action<NamedDomainObjectContainer<Variant>> block) {
        block.execute(getVariants());
    }
}
