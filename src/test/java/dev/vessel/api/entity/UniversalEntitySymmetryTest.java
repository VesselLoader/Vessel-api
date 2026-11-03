package dev.vessel.api.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exhaustive verification suite for Universal Entity & Multipart Substrate (Waves 3 & 4).
 * Validates multipart hierarchies, custom spawn serialization, teleportation safety,
 * mount/passenger topologies, and strict Rule 17 compliance.
 *
 * @version 1.0.0
 */
public final class UniversalEntitySymmetryTest {

    private TestLivingEntity bossEntity;
    private TestEntityPart wingPart;
    private TestEntityPart headPart;

    @BeforeEach
    public void setUp() {
        this.bossEntity = new TestLivingEntity(1001, 100.0f, 100.0f);
        this.wingPart = new TestEntityPart(1002, bossEntity);
        this.headPart = new TestEntityPart(1003, bossEntity);
        MultipartEntityPartRouter.getInstance().clear();
        EntityPassengerManager.getInstance().clear();
    }

    @Test
    public void testVesselEntityView() {
        assertThat(VesselEntityView.getEntityId(bossEntity)).isEqualTo(1001);
        assertThat(VesselEntityView.getHealth(bossEntity)).isEqualTo(100.0f);
        assertThat(VesselEntityView.getMaxHealth(bossEntity)).isEqualTo(100.0f);
        assertThat(VesselEntityView.isDead(bossEntity)).isFalse();

        // Damage until dead
        bossEntity.setHealth(0.0f);
        assertThat(VesselEntityView.isDead(bossEntity)).isTrue();

        // Null safety
        assertThat(VesselEntityView.getEntityId(null)).isEqualTo(VesselEntityView.DEFAULT_ENTITY_ID);
        assertThat(VesselEntityView.getHealth(null)).isEqualTo(VesselEntityView.DEFAULT_HEALTH);
        assertThat(VesselEntityView.getMaxHealth(null)).isEqualTo(VesselEntityView.DEFAULT_MAX_HEALTH);
        assertThat(VesselEntityView.isDead(null)).isTrue();
    }

    @Test
    public void testMultipartEntityHierarchyAndDamageRouting() {
        MultipartEntityPartRouter router = MultipartEntityPartRouter.getInstance();
        assertThat(router.isMultipartEntity(bossEntity)).isFalse();

        Object[] parts = new Object[]{wingPart, headPart};
        router.registerMultipart(bossEntity, parts);

        assertThat(router.isMultipartEntity(bossEntity)).isTrue();
        assertThat(router.getParts(bossEntity)).containsExactly(parts);
        assertThat(router.getParent(wingPart)).isEqualTo(bossEntity);
        assertThat(router.getParent(headPart)).isEqualTo(bossEntity);

        // Test damage routing with critical multiplier on head
        router.setPartDamageMultiplier(headPart, 2.0f);
        router.setPartDamageMultiplier(wingPart, 0.5f);

        float dealtWing = router.routePartDamage(bossEntity, wingPart, 10.0f);
        assertThat(dealtWing).isEqualTo(5.0f);
        assertThat(bossEntity.getHealth()).isEqualTo(95.0f);

        float dealtHead = router.routePartDamage(bossEntity, headPart, 10.0f);
        assertThat(dealtHead).isEqualTo(20.0f);
        assertThat(bossEntity.getHealth()).isEqualTo(75.0f);

        // Unregister
        router.unregisterMultipart(bossEntity);
        assertThat(router.isMultipartEntity(bossEntity)).isFalse();
        assertThat(router.getParts(bossEntity)).isEmpty();
    }

    @Test
    public void testEntitySpawnDataSerializer() {
        TestLivingEntity entity = new TestLivingEntity(2001, 20.0f, 20.0f);
        entity.setCustomSpawnData("CustomBossModelData:UUID=abc-123;Scale=2.5");

        TestSpawnDataBuffer buffer = new TestSpawnDataBuffer();
        boolean writeSuccess = EntitySpawnDataSerializer.writeSpawnData(entity, buffer);
        assertThat(writeSuccess).isTrue();
        assertThat(buffer.hasContent()).isTrue();

        TestLivingEntity clientEntity = new TestLivingEntity(2001, 20.0f, 20.0f);
        boolean readSuccess = EntitySpawnDataSerializer.readSpawnData(clientEntity, buffer);
        assertThat(readSuccess).isTrue();
        assertThat(clientEntity.getCustomSpawnData()).isEqualTo("CustomBossModelData:UUID=abc-123;Scale=2.5");
    }

    @Test
    public void testEntityTeleportationGovernorBounds() {
        // Valid coordinate teleportation
        boolean success = EntityTeleportationGovernor.teleport(bossEntity, "world", 100.0, 70.0, -200.0, 90.0f, 0.0f);
        assertThat(success).isTrue();
        assertThat(bossEntity.getX()).isEqualTo(100.0);
        assertThat(bossEntity.getY()).isEqualTo(70.0);
        assertThat(bossEntity.getZ()).isEqualTo(-200.0);

        // Out-of-bounds rejection
        boolean outOfBoundsX = EntityTeleportationGovernor.teleport(bossEntity, "world", 40000000.0, 70.0, 0.0, 0.0f, 0.0f);
        assertThat(outOfBoundsX).isFalse();

        boolean nanCoord = EntityTeleportationGovernor.teleport(bossEntity, "world", Double.NaN, 70.0, 0.0, 0.0f, 0.0f);
        assertThat(nanCoord).isFalse();

        boolean infCoord = EntityTeleportationGovernor.teleport(bossEntity, "world", 0.0, Double.POSITIVE_INFINITY, 0.0, 0.0f, 0.0f);
        assertThat(infCoord).isFalse();
    }

    @Test
    public void testPassengerAttachmentAndCircularProtection() {
        EntityPassengerManager manager = EntityPassengerManager.getInstance();

        TestLivingEntity horse = new TestLivingEntity(3001, 30.0f, 30.0f);
        TestLivingEntity player = new TestLivingEntity(3002, 20.0f, 20.0f);
        TestLivingEntity pet = new TestLivingEntity(3003, 10.0f, 10.0f);

        assertThat(manager.addPassenger(horse, player)).isTrue();
        assertThat(manager.isPassenger(horse, player)).isTrue();
        assertThat(manager.getPassengers(horse)).containsExactly(player);
        assertThat(manager.getVehicle(player)).isEqualTo(horse);

        // Player carries pet
        assertThat(manager.addPassenger(player, pet)).isTrue();
        assertThat(manager.getPassengers(player)).containsExactly(pet);

        // Circular mounting attempt: horse cannot ride pet!
        boolean circularAttempt = manager.addPassenger(pet, horse);
        assertThat(circularAttempt).isFalse();

        // Dismount
        assertThat(manager.removePassenger(horse, player)).isTrue();
        assertThat(manager.isPassenger(horse, player)).isFalse();
        assertThat(manager.getPassengers(horse)).isEmpty();
    }

    @Test
    public void testForgeEntitySymmetryAdapter() {
        MultipartEntityPartRouter.getInstance().registerMultipart(bossEntity, new Object[]{wingPart});

        assertThat(ForgeEntitySymmetryAdapter.isMultipartEntity(bossEntity)).isTrue();
        assertThat(ForgeEntitySymmetryAdapter.getParts(bossEntity)).containsExactly(wingPart);
        assertThat(ForgeEntitySymmetryAdapter.canRiderInteract(bossEntity)).isTrue();
        assertThat(ForgeEntitySymmetryAdapter.shouldRiderSit(bossEntity)).isTrue();
        assertThat(ForgeEntitySymmetryAdapter.canTrample(bossEntity, null, null, 1.0f)).isTrue();
        assertThat(ForgeEntitySymmetryAdapter.canTrample(bossEntity, null, null, 0.2f)).isFalse();
    }

    @Test
    public void testBukkitLivingEntitySymmetryAdapter() {
        assertThat(BukkitLivingEntitySymmetryAdapter.getHealth(bossEntity)).isEqualTo(100.0);
        assertThat(BukkitLivingEntitySymmetryAdapter.getMaxHealth(bossEntity)).isEqualTo(100.0);

        BukkitLivingEntitySymmetryAdapter.damage(bossEntity, 25.0);
        assertThat(BukkitLivingEntitySymmetryAdapter.getHealth(bossEntity)).isEqualTo(75.0);

        BukkitLivingEntitySymmetryAdapter.setHealth(bossEntity, 50.0);
        assertThat(BukkitLivingEntitySymmetryAdapter.getHealth(bossEntity)).isEqualTo(50.0);

        boolean teleported = BukkitLivingEntitySymmetryAdapter.teleport(bossEntity, "world", 50.0, 65.0, 50.0, 0.0f, 0.0f);
        assertThat(teleported).isTrue();
        assertThat(bossEntity.getX()).isEqualTo(50.0);
    }

    @Test
    public void testRule17ComplianceZeroInnerClasses() {
        Class<?>[] classesToVerify = new Class<?>[]{
                VesselEntityView.class,
                MultipartEntityPartRouter.class,
                EntitySpawnDataSerializer.class,
                EntityTeleportationGovernor.class,
                EntityPassengerManager.class,
                ForgeEntitySymmetryAdapter.class,
                BukkitLivingEntitySymmetryAdapter.class,
                UniversalEntitySymmetryTest.class,
                TestLivingEntity.class,
                TestEntityPart.class,
                TestSpawnDataBuffer.class
        };

        for (Class<?> clazz : classesToVerify) {
            assertThat(clazz.getDeclaredClasses())
                    .as("Class %s must have 0 declared inner or nested classes", clazz.getName())
                    .isEmpty();
            assertThat(clazz.isAnonymousClass())
                    .as("Class %s must not be anonymous", clazz.getName())
                    .isFalse();
            assertThat(clazz.isLocalClass())
                    .as("Class %s must not be local", clazz.getName())
                    .isFalse();
            assertThat(clazz.isMemberClass())
                    .as("Class %s must not be member class", clazz.getName())
                    .isFalse();
        }
    }
}
