package de.koudingspawn.vaultcrdhelmrenderer.vault;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.FatalTemplateErrorsException;
import de.koudingspawn.vaultcrdhelmrenderer.crd.Vault;
import de.koudingspawn.vaultcrdhelmrenderer.crd.VaultPropertiesConfiguration;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class PropertiesGenerator {

    private final VaultCommunication vaultCommunication;

    public PropertiesGenerator(VaultCommunication vaultCommunication) {
        this.vaultCommunication = vaultCommunication;
    }

    public Secret generateSecret(Vault resource) throws SecretNotAccessibleException {
        VaultPropertiesConfiguration propertiesConfiguration = resource.getSpec().getPropertiesConfiguration();

        if (propertiesConfiguration != null && propertiesConfiguration.getFiles() != null) {
            HashMap<String, Object> context = new HashMap<>();
            context.put("vault", new VaultJinjaLookup(vaultCommunication));
            if (propertiesConfiguration.getContext() != null) {
                context.putAll(propertiesConfiguration.getContext());
            }

            try {
                Map<String, String> renderedFiles = renderFiles(context, propertiesConfiguration.getFiles());
                return renderSecret(resource, renderedFiles);
            } catch (FatalTemplateErrorsException ex) {
                throw new SecretNotAccessibleException(ex.getMessage(), ex);
            }
        }

        throw new SecretNotAccessibleException("Does not contain the required Files to render");
    }

    private Secret renderSecret(Vault resource, Map<String, String> data) {
        ObjectMeta objectMeta = attachUpdateAnnotations(resource.getMetadata(), data);

        return new SecretBuilder()
                .withMetadata(objectMeta)
                .withData(data)
                .build();
    }

    private ObjectMeta attachUpdateAnnotations(ObjectMeta meta, Map<String, String> data) {
        if (meta.getAnnotations() == null) {
            meta.setAnnotations(new HashMap<>());
        }

        meta.getAnnotations().put("vault.koudingspawn.de/lastUpdated", LocalDateTime.now().toString());
        meta.getAnnotations().put("vault.koudingspawn.de/compare", hashOfData(data));

        return meta;
    }

    private String hashOfData(Map<String, String> data) {
        return Sha256.generateSha256(data.values().toArray(new String[0]));
    }

    private Map<String, String> renderFiles(Map<String, Object> context, Map<String, String> files) throws FatalTemplateErrorsException {
        Jinjava jinjava = new Jinjava();
        Map<String, String> targetFiles = new HashMap<>();

        files.forEach((key, value) -> {
            String renderedContent = jinjava.render(value, context);
            targetFiles.put(key, Base64.getEncoder().encodeToString(renderedContent.getBytes()));
        });

        return targetFiles;
    }

}
