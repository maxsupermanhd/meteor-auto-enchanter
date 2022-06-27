package maxsuperman.addon.enchanter.modules;

import maxsuperman.addon.enchanter.mixin.ScreenHandlerAccessor;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class AutoEnchant extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private boolean indexed = false;
    private BlockPos lastIndexedPos = null;
    private String lastIndexedDimension = null;
    private Queue<IndexedBlock> toIndexPos = new ArrayDeque<>();
    private List<IndexedBlock> cachedAnvils = new ArrayList<>(16);
    private List<ItemInABox> cachedItems = new ArrayList<>(16);
    private List<EnchantmentAction> process = new ArrayList<>();
    private State state = State.Disabled;
    private int lastInteraction = 0;

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
    private final Setting<Integer> interactionDelay = sgGeneral.add(new IntSetting.Builder()
        .name("interaction-delay")
        .description("How much to wait before sending next interaction (milliseconds)")
        .defaultValue(250)
        .min(0)
        .sliderRange(0, 1000)
        .build());
    private final Setting<Mode> opMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("How enchanting will be performed")
        .defaultValue(Mode.SingleDumb)
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
    public enum Mode {
        SingleDumb
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        var l = theme.verticalList();
        fillWidget(theme, l);
        return l;
    }

    private void fillWidget(GuiTheme theme, WVerticalList list) {
        WSection loadDataSection = list.add(theme.section("Config Saving")).expandX().widget();

        WTable control = loadDataSection.add(theme.table()).expandX().widget();

        WTextBox nfname = control.add(theme.textBox("default")).expandWidgetX().expandCellX().expandX().widget();
        WButton save = control.add(theme.button("Save")).expandX().widget();
        save.action = () -> {
            if (saveProcessToFile(new File(new File(MeteorClient.FOLDER, "AutoEnchanter"), nfname.get()+".nbt"))) {
                info("Saved successfully");
            } else {
                info("Save failed");
            }
            list.clear();
            fillWidget(theme, list);
        };
        control.row();

        ArrayList<String> fnames = new ArrayList<>();
        try {
            Files.list(MeteorClient.FOLDER.toPath().resolve("AutoEnchanter")).forEach(path -> {
                String name = path.getFileName().toString();
                fnames.add(name.substring(0, name.length() - 4));
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (fnames.size() == 0) {
            fnames.add("default");
        }
        WDropdown<String> lfname = control.add(theme.dropdown(fnames.toArray(new String[0]), "default"))
            .expandWidgetX().expandCellX().expandX().widget();
        WButton load = control.add(theme.button("Load")).expandX().widget();
        load.action = () -> {
            loadProcessFromFile(new File(new File(MeteorClient.FOLDER, "AutoEnchanter"), lfname.get()+".nbt"));
            list.clear();
            fillWidget(theme, list);
        };

        WSection p = list.add(theme.section("Process")).expandX().widget();

        WTable table = p.add(theme.table()).expandX().widget();

        table.add(theme.label("A"));
        table.add(theme.label("Ae"));
        table.add(theme.label("B"));
        table.add(theme.label("Be"));
        table.add(theme.label("E"));
        table.add(theme.label("D"));
        table.row();
        for (EnchantmentAction action : process) {
            table.add(theme.item(action.A.getDefaultStack()));
            var ael = theme.verticalList();
            for (EnchantmentWithLevel ae : action.Ae) {
                ael.add(theme.label(ae.enchantment.getName(ae.level).getString()));
            }
            table.add(ael);
            table.add(theme.item(action.B.getDefaultStack()));
            var bel = theme.verticalList();
            for (EnchantmentWithLevel be : action.Be) {
                bel.add(theme.label(be.enchantment.getName(be.level).getString()));
            }
            table.add(bel);
            var edit = table.add(theme.button("E")).widget();
            edit.action = () -> {
                var s = new ProcessStepScreen(theme, action);
                s.onClosed(() -> {
                    list.clear();
                    fillWidget(theme, list);
                });
                mc.setScreen(s);
            };
            var delete = table.add(theme.button("D")).widget();
            delete.action = () -> {
                process.remove(action);
                list.clear();
                fillWidget(theme, list);
            };

            table.row();
        }
        var addb = table.add(theme.button("Add")).widget();
        addb.action = () -> {
            process.add(new EnchantmentAction());
            list.clear();
            fillWidget(theme, list);
        };
    }

    private boolean saveProcessToFile(File f) {
        NbtList l = new NbtList();
        for (EnchantmentAction a : process) {
            l.add(a.toTag());
        }
        NbtCompound c = new NbtCompound();
        c.put("process", l);
        f.getParentFile().mkdirs();
        try {
            NbtIo.write(c, f);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean loadProcessFromFile(File f) {
        if (!f.exists() || !f.canRead()) {
            info("File does not exist or can not be loaded");
            return false;
        }
        NbtCompound r = null;
        try {
            r = NbtIo.read(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (r == null) {
            info("Failed to load nbt from file");
            return false;
        }
        NbtList l = r.getList("process", NbtElement.COMPOUND_TYPE);
        process.clear();
        for (NbtElement e : l) {
            if (e.getType() != NbtElement.COMPOUND_TYPE) {
                info("Invalid list element");
                return false;
            }
            process.add(new EnchantmentAction().fromTag((NbtCompound) e));
        }
        return true;
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
    private IndexedBlock interactedBlock = null;
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (mc.player == null) {return;}
        if (event.screen == null) {return;}
        info("Opened screen: " + event.screen.getClass().getName());
        if (!(event.screen instanceof ShulkerBoxScreen) && !(event.screen instanceof GenericContainerScreen)) {return;}
        var screen = event.screen;
        ScreenHandler container = ((ScreenHandlerProvider<?>)screen).getScreenHandler();
        state = State.IndexingWaitingForContents;
        mc.player.currentScreenHandler = new ScreenHandler(((ScreenHandlerAccessor) container).getNullableType(), container.syncId) {
            @Override
            public boolean canUse(PlayerEntity var1) {
                return true;
            }
            @Override
            public ItemStack transferSlot(PlayerEntity player, int index) {
                return ItemStack.EMPTY;
            }
            @Override
            public void updateSlotStacks(int revision, List<ItemStack> stacks, ItemStack cursorStack) {
                info("Got {} slots revision {}", stacks.size(), revision);
                if (mc.player == null) {
                    error("Player is null!");
                    return;
                }
                try {
                    if(state != State.IndexingWaitingForContents) {throw new Exception("??? got screen without waiting for contents");}
                    if(interactedBlock == null) {throw new Exception("??? interacted block in null");}
                    for (int slot = 0; slot < stacks.size(); slot++) {
                        ItemStack stack = stacks.get(slot);
                        if(stack.isEmpty()) {continue;}
                        for (EnchantmentAction p : process) {
                            boolean found;
                            if(stack.isOf(p.A)) {
                                found = checkMatchingEnchantments(p.Ae, stack);
                            } else if(stack.isOf(p.B)) {
                                found = checkMatchingEnchantments(p.Be, stack);
                            } else {
                                continue;
                            }
                            if(found) {
                                info("Found {} in {} at {}", stack.getName().getString(), interactedBlock.pos, slot);
                                cachedItems.add(new ItemInABox(interactedBlock, stack, slot));
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    error(e.getMessage());
                }
                mc.player.closeHandledScreen();
                state = State.Indexing;
            }
        };
    }
    private boolean checkMatchingEnchantments(List<EnchantmentWithLevel> req, ItemStack stack) {
        var e = EnchantmentHelper.get(stack);
        if(e.size() != req.size()) {
            return false;
        }
        boolean matches = true;
        for (EnchantmentWithLevel c : req) {
            var cc = e.get(c.enchantment);
            if(cc == null || c.level != cc) {
                matches = false;
            }
        }
        return matches;
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (state == State.Indexing) {
            var t = toIndexPos.peek();
            if (t == null) {
                state = State.Planning;
                return;
            }
            if (mc.player == null) {
                error("player is null");
                state = State.Disabled;
                return;
            }
            if (mc.world == null) {
                error("player is null");
                state = State.Disabled;
                return;
            }
            Vec3d epos = mc.player.getEyePos();
            if (mc.interactionManager == null) {
                error("interaction manager is null");
                state = State.Disabled;
                return;
            }
            if (mc.currentScreen == null) {
                state = State.IndexingWaitingForScreen;
                interactedBlock = t;
                mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(t.hit,
                    Direction.getFacing((float) (t.hit.x - epos.x), (float) (t.hit.y - epos.y), (float) (t.hit.z - epos.z)),
                    t.pos, false));
            }
            toIndexPos.remove();
        } else if(state == State.Planning) {
            if(cachedAnvils.size() == 0) {
                error("No anvils around?");
                state = State.Disabled;
                return;
            }
            var m = opMode.get();
            switch (m) {
                case SingleDumb -> {

                }
                default -> error("Not implemented enchanting mode");
            }
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
        return r;
    }
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
    public static class ClosestPosResult {
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

        public ItemInABox(IndexedBlock container, ItemStack item, int slot) {
            this.container = container;
            this.item = item;
            this.slot = slot;
        }
    }
}
