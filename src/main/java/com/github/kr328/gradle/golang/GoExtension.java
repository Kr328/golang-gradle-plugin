package com.github.kr328.gradle.golang;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;

import javax.annotation.Nonnull;

public abstract class GoExtension {
    @Nonnull
    public abstract DirectoryProperty getModuleDirectory();

    @Nonnull
    public abstract NamedDomainObjectContainer<GoVariant> getVariants();

    public void variants(@Nonnull final Action<NamedDomainObjectContainer<GoVariant>> block) {
        block.execute(getVariants());
    }
}
