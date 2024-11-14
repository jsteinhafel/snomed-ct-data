package dev.ikm.maven;

import dev.ikm.tinkar.composer.Composer;

import java.io.File;
import java.util.UUID;

public class AbstractTransformer implements Transformer {
    final UUID namespace;
    AbstractTransformer(UUID namespace) {
        this.namespace = namespace;
    }
    @Override
    public void transform(File file, Composer composer) {
    }
    @Override
    public UUID getNamespace() {
        return namespace;
    }
}
