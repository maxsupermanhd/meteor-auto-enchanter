package maxsuperman.addon.enchanter.modules;

import com.google.common.collect.ForwardingQueue;
import maxsuperman.addon.enchanter.GenericSelectionCallback;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.util.*;

public class AutoEnchant extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private boolean indexed = false;
    private BlockPos lastIndexedPos = null;
    private String lastIndexedDimension = null;
    private Queue<IndexedBlock> toIndexPos = new ArrayDeque<>();
    private List<IndexedBlock> cachedAnvils = new ArrayList<>();
    private Map<String, List<ItemInABox>> cachedBooks = new TreeMap<>();
    private Map<String, List<ItemInABox>> cachedItems = new TreeMap<>();

    private List<EnchantmentAction> rules = new ArrayList<>();

    private State state = State.Disabled;

    private final Setting<Double> reachRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("reach-distance")
        .description("Overrides reach distance.")
        .defaultValue(4.5)
        .min(0)
        .sliderRange(0, 11)
        .build());
    private final Setting<Boolean> doReindex = sgGeneral.add(new BoolSetting.Builder()
        .name("reindex")
        .description("Do storage indexing each time module activates.")
        .defaultValue(true)
        .build());
    private final Setting<List<BlockEntityType<?>>> storageBlocks = sgGeneral.add(new StorageBlockListSetting.Builder()
        .name("storage-blocks")
        .description("Select the storage blocks to consider.")
        .defaultValue(new BlockEntityType[]{BlockEntityType.CHEST, BlockEntityType.TRAPPED_CHEST, BlockEntityType.ENDER_CHEST, BlockEntityType.SHULKER_BOX, BlockEntityType.BARREL})
        .build());

    public AutoEnchant() {
        super(Categories.Misc, "auto-enchanter", "Automatically enchants stuff for you.");
    }

    public enum State {
        Disabled,
        Indexing,
        IndexingWaitingForScreen,
        IndexingWaitingForContents,
        Planning,
    }
    @Override
    public void onActivate() {
        if (doReindex.get() || !indexed) {
            indexed = false;
            if (mc.world != null && mc.player != null) {
                lastIndexedDimension = mc.world.asString();
                lastIndexedPos = mc.player.getBlockPos();
            }
            toIndexPos.addAll(indexSurroundings());
            state = State.Indexing;
        }
        super.onActivate();
    }

    @Override
    public void onDeactivate() {
        state = State.Disabled;
        super.onDeactivate();
    }
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        info("Opened screen: " + event.screen.toString());
//        if (event.screen instanceof ShulkerBoxScreen) {
//            toggle();
//        } else if (event.screen instanceof AnvilScreen) {
//
//        } else if (event.screen instanceof GenericContainerScreen) {
//
//        }
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (state == State.Indexing) {
            var t = toIndexPos.poll();
            if (t == null) {
                state = State.Planning;
                return;
            }
            if (mc.player == null) {
                error("player is null");
                return;
            }
            Vec3d epos = mc.player.getEyePos();
            if (mc.interactionManager == null) {
                error("interaction manager is null");
                return;
            }
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(t.hit));
        }
    }

    public List<IndexedBlock> indexSurroundings() {
        List<IndexedBlock> ret = new ArrayList<>(8);
        if (mc.world == null) {
            error("Unable to load surrounding anvils because world is null (?!?!)");
            return ret;
        }
        if (mc.player == null) {
            error("Unable to load surrounding anvils because player is null (?!?!)");
            return ret;
        }
        Vec3d epos = mc.player.getEyePos();
        double reach = getReachRange();
        int bReach = (int) (Math.round(reach)+3);
        for(int ox = -bReach; ox <= bReach; ox++) {
            for(int oy = -bReach; oy <= bReach; oy++) {
                for(int oz = -bReach; oz <= bReach; oz++) {
                    BlockPos p = mc.player.getBlockPos().add(ox, oy, oz);
                    BlockEntity e = mc.world.getBlockEntity(p);
                    if (e == null) {
                        continue;
                    }
                    BlockState s = e.getCachedState();
                    if(!isStorageContainer(e) || !canSearch(mc.world, p)) {
                        continue;
                    }
                    Vec3d closestPos = getClosestPoint(p, s.getOutlineShape(mc.world, p), epos, null);
                    if(closestPos.squaredDistanceTo(epos) > reach*reach) {
                        continue;
                    }
                    if(s.isOf(Blocks.ANVIL) || s.isOf(Blocks.CHIPPED_ANVIL) || s.isOf(Blocks.DAMAGED_ANVIL)) {
                        cachedAnvils.add(new IndexedBlock(p, closestPos));
                    } else {
                        ret.add(new IndexedBlock(p, closestPos));
                    }
                }
            }
        }
        return ret;
    }
    public boolean isStorageContainer(BlockEntity b) {
        return storageBlocks.get().contains(b.getType());
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (IndexedBlock p : toIndexPos) {
            event.renderer.blockLines(p.pos.getX(), p.pos.getY(), p.pos.getZ(), Color.GREEN, 0);
        }
    }

    public double getReachRange() {
        double r = reachRange.get();
        if (r == 0.0) {
            if (mc.interactionManager != null) {
                r = mc.interactionManager.getReachDistance();
            } else {
                error("Reach distance is set to 0 and interaction manager is null, defaulting to 4.5");

            }
        }
        return reachRange.get();
    }

    // ty earthcomputer <3
    public Vec3d getClosestPoint(BlockPos blockPos, VoxelShape voxel, Vec3d pos, Direction dir) {
        ClosestPosResult result = new ClosestPosResult();
        Direction[] dirs = dir == null ? Direction.values() : new Direction[] {dir};
        voxel.forEachBox((x1, y1, z1, x2, y2, z2) -> {
            Box box = new Box(x1, y1, z1, x2, y2, z2).offset(blockPos);
            for (Direction face : dirs) {
                Box faceBox = switch (face) {
                    case WEST -> new Box(box.minX, box.minY, box.minZ, box.minX, box.maxY, box.maxZ);
                    case EAST -> new Box(box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
                    case DOWN -> new Box(box.minX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ);
                    case UP -> new Box(box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ);
                    case NORTH -> new Box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ);
                    case SOUTH -> new Box(box.minX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ);
                };
                // Since the faces are axis aligned, it's a simple clamp operation
                Vec3d val = new Vec3d(MathHelper.clamp(pos.x, faceBox.minX, faceBox.maxX),
                    MathHelper.clamp(pos.y, faceBox.minY, faceBox.maxY),
                    MathHelper.clamp(pos.z, faceBox.minZ, faceBox.maxZ));
                double distanceSq = val.squaredDistanceTo(pos);
                if (distanceSq < result.distanceSq) {
                    result.val = val;
                    result.distanceSq = distanceSq;
                }
            }
        });
        return result.val;
    }
    public class ClosestPosResult {
        Vec3d val;
        double distanceSq = Double.POSITIVE_INFINITY;
    }
    private boolean canSearch(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof Inventory) && state.getBlock() != Blocks.ENDER_CHEST)
            return false;
        if (state.getBlock() instanceof ChestBlock || state.getBlock() == Blocks.ENDER_CHEST) {
            if (ChestBlock.isChestBlocked(world, pos))
                return false;
            if (state.getBlock() instanceof ChestBlock && state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
                BlockPos offsetPos = pos.offset(ChestBlock.getFacing(state));
                return world.getBlockState(offsetPos).getBlock() != state.getBlock() || !ChestBlock.isChestBlocked(world, offsetPos);
            }
        }
        return true;
    }

    public class IndexedBlock {
        IndexedBlock(BlockPos pos1, Vec3d hit1) {
            pos = pos1;
            hit = hit1;
        }
        public BlockPos pos;
        public Vec3d hit;
    }
    public class ItemInABox {
        public IndexedBlock container;
        public ItemStack item;
        public int slot;
    }

    public class EnchantmentAction {
        public Item A = Items.BOOK;
        public List<Enchantment> Ae = new ArrayList<>(2);
        public Item B = Items.BOOK;
        public List<Enchantment> Be = new ArrayList<>(2);
        public boolean isFinal = false;
    }
}
