package com.thebuildingblocks.derec.v0_9.httpprototype;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class HelpersTest {
    @Test
    public void verifyUuidToFromByte() {
        UUID u = UUID.randomUUID();
        byte[] uBytes = Helpers.asBytes(u);
        UUID u2 = Helpers.asUuid(uBytes);
        assertEquals(u, u2);
    }
}