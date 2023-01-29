package de.koudingspawn.vaultcrdhelmrenderer.parser.rotation;

import io.fabric8.kubernetes.api.model.Container;

import java.util.List;
import java.util.Objects;

public class ContainerReferencesSecretUtil {

    static boolean hasSecretInEnv(List<Container> containers, String secretName) {
        return containers
                .stream()
                .map(Container::getEnv)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(e -> e.getValueFrom() != null)
                .filter(e -> e.getValueFrom().getSecretKeyRef() != null)
                .filter(e -> e.getValueFrom().getSecretKeyRef().getName() != null)
                .anyMatch(e -> e.getValueFrom().getSecretKeyRef().getName().equals(secretName));
    }

    static boolean hasSecretInEnvFrom(List<Container> containers, String secretName) {
        return containers
                .stream()
                .map(Container::getEnvFrom)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(e -> e.getSecretRef() != null)
                .filter(e -> e.getSecretRef().getName() != null)
                .anyMatch(e -> e.getSecretRef().getName().equals(secretName));
    }

}
