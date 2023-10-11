package com.thebuildingblocks.derec.v0_9.httpprototype;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class UtilTest {
    @Test
    public void verifyUuidToFromByte() {
        UUID u = UUID.randomUUID();
        byte[] uBytes = Util.asBytes(u);
        UUID u2 = Util.asUuid(uBytes);
        assertEquals(u, u2);
    }
}