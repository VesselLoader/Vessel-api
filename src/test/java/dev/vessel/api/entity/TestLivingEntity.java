package dev.vessel.api.entity;

/**
 * Mock living entity fixture for testing universal entity view, damage, and teleportation symmetry.
 *
 * @version 1.0.0
 */
public final class TestLivingEntity {

    private final int id;
    private float health;
    private final float maxHealth;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private String customSpawnData;

    public TestLivingEntity(int id, float health, float maxHealth) {
        this.id = id;
        this.health = health;
        this.maxHealth = maxHealth;
        this.x = 0.0;
        this.y = 64.0;
        this.z = 0.0;
        this.yaw = 0.0f;
        this.pitch = 0.0f;
        this.customSpawnData = null;
    }

    public int getId() {
        return id;
    }

    public float getHealth() {
        return health;
    }

    public void setHealth(float health) {
        this.health = health;
    }

    public void setHealth(double health) {
        this.health = (float) health;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public boolean isDead() {
        return health <= 0.0f;
    }

    public boolean isAlive() {
        return health > 0.0f;
    }

    public void hurt(float amount) {
        this.health = Math.max(0.0f, this.health - amount);
    }

    public void damage(double amount) {
        hurt((float) amount);
    }

    public void teleportTo(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public String getCustomSpawnData() {
        return customSpawnData;
    }

    public void setCustomSpawnData(String customSpawnData) {
        this.customSpawnData = customSpawnData;
    }

    public void writeSpawnData(TestSpawnDataBuffer buffer) {
        if (customSpawnData != null) {
            buffer.writeString(customSpawnData);
        }
    }

    public void readSpawnData(TestSpawnDataBuffer buffer) {
        this.customSpawnData = buffer.readString();
    }
}
