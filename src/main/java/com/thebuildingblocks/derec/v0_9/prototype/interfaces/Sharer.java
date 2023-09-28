package com.thebuildingblocks.derec.v0_9.prototype.interfaces;

import com.thebuildingblocks.derec.v0_9.prototype.DeRecId;
import com.thebuildingblocks.derec.v0_9.prototype.Secret;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface Sharer {
    Secret newSecret(String description, byte[] bytesToProtect, List<DeRecId> helperIds) throws ExecutionException,
            InterruptedException;

    Secret newSecret(String secretId, String description, byte[] bytesToProtect, List<DeRecId> helperIds) throws ExecutionException, InterruptedException;

    Secret getSecret(String secretId);

    List<String> getSecrets();

    public interface Builder {
        Builder id(DeRecId id);

        Builder keyPair(KeyPair keyPair);

        Builder x509Certificate(X509Certificate x509Certificate);

        Sharer build();
    }

}
