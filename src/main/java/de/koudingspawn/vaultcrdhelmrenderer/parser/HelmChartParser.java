package de.koudingspawn.vaultcrdhelmrenderer.parser;

import de.koudingspawn.vaultcrdhelmrenderer.crd.Vault;
import de.koudingspawn.vaultcrdhelmrenderer.crd.VaultType;
import de.koudingspawn.vaultcrdhelmrenderer.parser.rotation.DeploymentRotation;
import de.koudingspawn.vaultcrdhelmrenderer.parser.rotation.Rotation;
import de.koudingspawn.vaultcrdhelmrenderer.parser.rotation.StatefulsetRotation;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HelmChartParser {

    private static List<Rotation> rotationList = List.of(new DeploymentRotation(), new StatefulsetRotation());
    private final List<HasMetadata> resourceList;

    public HelmChartParser(String rawContent) {
        String kind = "koudingspawn.de/v1#Vault";
        KubernetesDeserializer.registerCustomKind(kind, Vault.class);

        this.resourceList = new ArrayList<>();
        for (String s : rawContent.split("---")) {
            HasMetadata resource = Serialization.unmarshal(s);
            if (resource != null) {
                resourceList.add(resource);
            }
        }
    }

    public List<Vault> findVaultPropertiesResources() {
        return resourceList.stream()
                .filter(HelmChartParser::isVaultResource)
                .map(Serialization::asYaml)
                .map(s -> Serialization.unmarshal(s, Vault.class))
                .filter(n -> n.getSpec().getType().equals(VaultType.PROPERTIES))
                .toList();
    }

    public void replaceVaultPropertyWithSecret(Vault oldResource, Secret newResource) {
        int indexOfOldResource = findIndexOfResource(oldResource);
        resourceList.set(indexOfOldResource, newResource);

        this.annotateReferencingResources(newResource);
    }

    private void annotateReferencingResources(Secret newResource) {
        for (int i = 0; i < resourceList.size(); i++) {
            HasMetadata hasMetadata = resourceList.get(i);
            Optional<Rotation> rotationMatch = rotationList
                    .stream()
                    .filter(r -> r.getApiVersion().equals(hasMetadata.getApiVersion()))
                    .filter(r -> r.getKind().equals(hasMetadata.getKind()))
                    .findAny();

            if (rotationMatch.isPresent()) {
                HasMetadata annotatedObj = rotationMatch.get().annotate(hasMetadata, newResource.getMetadata().getName(), newResource.getMetadata().getAnnotations().get("vault.koudingspawn.de/compare"));
                resourceList.set(i, annotatedObj);
            }
        }
    }

    public String toString() {
        return String.join(System.lineSeparator(),
                resourceList.stream().map(Serialization::asYaml).toList());
    }

    private int findIndexOfResource(Vault idxResource) {
        for (int i = 0; i < resourceList.size(); i++) {
            if (!isVaultResource(resourceList.get(i))) {
                continue;
            }

            if (resourceList.get(i).getMetadata().getName().equals(idxResource.getMetadata().getName())) {
                return i;
            }
        }

        return -1;
    }

    public static boolean isVaultResource(HasMetadata resource) {
        if (resource == null) {
            return false;
        }

        return resource.getKind().equalsIgnoreCase("vault") && resource.getApiVersion().equalsIgnoreCase("koudingspawn.de/v1");
    }

}
