package de.koudingspawn.vaultcrdhelmrenderer.vault;

import org.springframework.vault.VaultException;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.VaultVersionedKeyValueOperations;
import org.springframework.vault.support.VaultResponseSupport;
import org.springframework.vault.support.Versioned;

import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Pattern;

public class VaultCommunication {

    private static final Pattern keyValuePattern = Pattern.compile("^.*?\\/.*?$");

    private final VaultTemplate vaultTemplate;

    public VaultCommunication(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    public HashMap getKeyValue(String path) throws SecretNotAccessibleException {
        return getRequest(path, HashMap.class);
    }

    private <T> T getRequest(String path, Class<T> clazz) throws SecretNotAccessibleException {
        try {
            VaultResponseSupport<T> response = vaultTemplate.read(path, clazz);
            if (response != null) {
                return response.getData();
            } else {
                throw new SecretNotAccessibleException(String.format("The secret %s is not available or in the wrong format.", path));
            }
        } catch (VaultException exception) {
            throw new SecretNotAccessibleException(
                    String.format("Couldn't load secret from vault path %s", path), exception);
        }
    }

    public HashMap getVersionedSecret(String path, Optional<Integer> version) throws SecretNotAccessibleException {
        return getVersionedSecret(path, version, HashMap.class);
    }

    private <T> T getVersionedSecret(String path, Optional<Integer> version, Class<T> clazz) throws SecretNotAccessibleException {
        String mountPoint = extractMountPoint(path);
        String extractedKey = extractKey(path);

        VaultVersionedKeyValueOperations versionedKV = vaultTemplate.opsForVersionedKeyValue(mountPoint);
        Versioned<T> versionedResponse;

        try {
            if (version.isPresent()) {
                versionedResponse = versionedKV.get(extractedKey, Versioned.Version.from(version.get()), clazz);
            } else {
                versionedResponse = versionedKV.get(extractedKey, clazz);
            }

            if (versionedResponse != null) {
                return versionedResponse.getData();
            }

            throw new SecretNotAccessibleException(String.format("The secret %s is not available or in the wrong format.", path));

        } catch (VaultException ex) {
            throw new SecretNotAccessibleException(
                    String.format("Couldn't load secret from vault path %s", path), ex);
        }
    }

    private String extractMountPoint(String path) throws SecretNotAccessibleException {
        if (keyValuePattern.matcher(path).matches()) {
            return path.split("/", 2)[0];
        }

        throw new SecretNotAccessibleException(String.format("Could not extract mountpoint from path: %s. A valid path looks like 'mountpoint/key'", path));
    }

    private String extractKey(String path) throws SecretNotAccessibleException {
        if (keyValuePattern.matcher(path).matches()) {
            return path.split("/", 2)[1];
        }

        throw new SecretNotAccessibleException(String.format("Could not extract key from path: %s. A valid path looks like 'mountpoint/key'", path));
    }
}