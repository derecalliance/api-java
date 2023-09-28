package com.thebuildingblocks.derec.v0_9.prototype.interfaces;

import com.thebuildingblocks.derec.v0_9.prototype.DeRecId;
import com.thebuildingblocks.derec.v0_9.prototype.HelperClient;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public interface Secret {
    List<HelperClient> addHelpers(List<DeRecId> helperIds) throws ExecutionException, InterruptedException;

    List<HelperClient> addHelpers(List<DeRecId> helperIds, boolean async) throws ExecutionException,
            InterruptedException;

    void removeHelpers(HelperClient... helper);

    com.thebuildingblocks.derec.v0_9.prototype.Secret.Version update(byte[] bytesToProtect);

    Future<com.thebuildingblocks.derec.v0_9.prototype.Secret.Version> updateAsync(byte[] bytesToProtect);

    void close();

    boolean isAvailable();

    String listHelpers();
}
