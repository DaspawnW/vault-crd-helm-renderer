package de.koudingspawn.vaultcrdhelmrenderer.vault;

import de.koudingspawn.vaultcrdhelmrenderer.crd.Vault;
import de.koudingspawn.vaultcrdhelmrenderer.utils.FileUtils;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class PropertiesGeneratorTest {

    @Mock
    VaultCommunication vaultCommunication;

    @Test
    void generateSecretFromVaultResource() throws SecretNotAccessibleException {
        String kind = "koudingspawn.de/v1#Vault";
        KubernetesDeserializer.registerCustomKind(kind, Vault.class);

        String s = FileUtils.fileAsString("/yamls/vault-crd.yaml");
        Vault vaultResource = Serialization.unmarshal(s, Vault.class);

        HashMap<String, String> objectObjectHashMap = new HashMap<>();
        objectObjectHashMap.put("username", "username");
        objectObjectHashMap.put("password", "password");
        when(vaultCommunication.getVersionedSecret(eq("kv-test/qwe/asd"), eq(Optional.empty())))
                .thenReturn(objectObjectHashMap);
        when(vaultCommunication.getKeyValue("kv-1-test/qwe/asd"))
                .thenReturn(objectObjectHashMap);


        PropertiesGenerator propertiesGenerator = new PropertiesGenerator(vaultCommunication);
        Secret secret = propertiesGenerator.generateSecret(vaultResource);


        Assertions.assertNotNull(secret);
        Assertions.assertEquals("example-properties", secret.getMetadata().getName());
        Assertions.assertEquals("RELEASE-NAME", secret.getMetadata().getLabels().get("app.kubernetes.io/instance"));

        Assertions.assertTrue(secret.getData().containsKey("simple-string"));
        String decodedSimpleString = new String(Base64.getDecoder().decode(secret.getData().get("simple-string")));
        Assertions.assertEquals("nothing-here", decodedSimpleString);

        Assertions.assertTrue(secret.getData().containsKey("rendered-secret"));
        String decodedRenderedSecret = new String(Base64.getDecoder().decode(secret.getData().get("rendered-secret")));
        Assertions.assertEquals("username", decodedRenderedSecret);

        Assertions.assertTrue(secret.getData().containsKey("render-context"));
        String decodedRenderedContext = new String(Base64.getDecoder().decode(secret.getData().get("render-context")));
        Assertions.assertEquals("example-namespace", decodedRenderedContext);

        Assertions.assertTrue(secret.getData().containsKey("v1-lookup"));
        String decodedV1Lookup = new String(Base64.getDecoder().decode(secret.getData().get("v1-lookup")));
        Assertions.assertEquals("password", decodedV1Lookup);

        Assertions.assertTrue(secret.getData().containsKey("mix-multiline"));
        String decodedMixMultiline = new String(Base64.getDecoder().decode(secret.getData().get("mix-multiline")));
        Assertions.assertEquals("simple-string: \"nothing-here\"\n" +
                "rendered-secret: \"username\"\n" +
                "render-context: \"example-namespace\"\n", decodedMixMultiline);
    }

    @Test
    void shouldFailRenderingWithException() throws SecretNotAccessibleException {
        String kind = "koudingspawn.de/v1#Vault";
        KubernetesDeserializer.registerCustomKind(kind, Vault.class);

        String s = FileUtils.fileAsString("/yamls/vault-crd.yaml");
        Vault vaultResource = Serialization.unmarshal(s, Vault.class);

        HashMap<String, String> objectObjectHashMap = new HashMap<>();
        objectObjectHashMap.put("password", "password");
        when(vaultCommunication.getVersionedSecret(eq("kv-test/qwe/asd"), eq(Optional.empty())))
                .thenReturn(objectObjectHashMap);

        PropertiesGenerator propertiesGenerator = new PropertiesGenerator(vaultCommunication);
        Assertions.assertThrows(SecretNotAccessibleException.class, () -> propertiesGenerator.generateSecret(vaultResource));
    }

    @Test
    void shouldFailWithInvalidSyntax() {
        String kind = "koudingspawn.de/v1#Vault";
        KubernetesDeserializer.registerCustomKind(kind, Vault.class);

        String s = FileUtils.fileAsString("/yamls/vault-crd-invalid-syntax.yaml");
        Vault vaultResource = Serialization.unmarshal(s, Vault.class);

        PropertiesGenerator propertiesGenerator = new PropertiesGenerator(vaultCommunication);

        Assertions.assertThrows(SecretNotAccessibleException.class, () ->
                propertiesGenerator.generateSecret(vaultResource));
    }

}