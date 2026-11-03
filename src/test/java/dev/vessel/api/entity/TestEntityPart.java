package dev.vessel.api.entity;

/**
 * Mock entity part fixture representing a child hitbox in a multipart entity hierarchy.
 *
 * @version 1.0.0
 */
public final class TestEntityPart {

    private final int partId;
    private final Object parent;

    public TestEntityPart(int partId, Object parent) {
        this.partId = partId;
        this.parent = parent;
    }

    public int getPartId() {
        return partId;
    }

    public Object getParent() {
        return parent;
    }
}
