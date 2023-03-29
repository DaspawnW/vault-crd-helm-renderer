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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertiesGenerator {

    private static final Pattern expressionRegex = Pattern.compile("\\{\\{\\s*?vault\\.lookup(V2)?\\(");
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
            } catch (FatalTemplateErrorsException | RenderingException ex) {
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

    private Map<String, String> renderFiles(Map<String, Object> context, Map<String, String> files) throws FatalTemplateErrorsException, RenderingException {
        Jinjava jinjava = new Jinjava();
        Map<String, String> targetFiles = new HashMap<>();

        for (Map.Entry<String, String> entry : files.entrySet()) {
            String renderedContent = jinjava.render(entry.getValue(), context);
            if (hasRenderingIssue(renderedContent)) {
                throw new RenderingException("Failed to render " + entry.getKey() + " in vault properties as it still contains expressions, probably some syntax issue occurred!");
            }

            targetFiles.put(entry.getKey(), Base64.getEncoder().encodeToString(renderedContent.getBytes()));
        }

        return targetFiles;
    }

    // add a self developed test to identify if jinjava rendering fails due to invalid syntax: https://github.com/HubSpot/jinjava/issues/1038
    private boolean hasRenderingIssue(String renderedContent) {
        Matcher matcher = expressionRegex.matcher(renderedContent);
        return matcher.find();
    }

}
