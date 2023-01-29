package de.koudingspawn.vaultcrdhelmrenderer.parser.rotation;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface Rotation {

    String getKind();

    String getApiVersion();

    HasMetadata annotate(HasMetadata obj, String secretName, String annotation);

}
