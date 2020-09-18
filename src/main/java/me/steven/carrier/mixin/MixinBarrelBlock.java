package me.steven.carrier.mixin;

import me.steven.carrier.Carrier;
import me.steven.carrier.api.Carriable;
import me.steven.carrier.api.CarriablePlacementContext;
import me.steven.carrier.api.Holder;
import me.steven.carrier.api.Holding;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.Lazy;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BarrelBlock.class)
public class MixinBarrelBlock implements Carriable {

    @Shadow
    @Final
    public static DirectionProperty FACING;

    private static final Lazy<BlockState> RENDER_STATE = new Lazy<>(() -> Blocks.BARREL.getDefaultState().with(FACING, Direction.SOUTH));

    @Override
    public @NotNull ActionResult tryPickup(@NotNull Holder holder, @NotNull World world, @NotNull BlockPos pos, @Nullable Entity entity) {
        if (world.isClient) return ActionResult.PASS;
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof BarrelBlockEntity)) return ActionResult.PASS;
        AccessorBarrelBlockEntity barrel = (AccessorBarrelBlockEntity) blockEntity;
        Holding holding = new Holding(new Identifier(Carrier.MOD_ID, "barrel"), Inventories.toTag(new CompoundTag(), barrel.getInventory()));
        holder.setHolding(holding);
        sync(holder);
        barrel.getInventory().clear();
        world.setBlockState(pos, Blocks.AIR.getDefaultState());
        return ActionResult.SUCCESS;
    }

    @Override
    public @NotNull ActionResult tryPlace(@NotNull Holder holder, @NotNull World world, @NotNull CarriablePlacementContext ctx) {
        if (world.isClient) return ActionResult.PASS;
        Holding holding = holder.getHolding();
        if (holding == null) return ActionResult.PASS;
        DefaultedList<ItemStack> invList = DefaultedList.ofSize(27, ItemStack.EMPTY);
        Inventories.fromTag(holding.getTag(), invList);
        PlayerEntity player = (PlayerEntity) holder;
        BlockPos pos = ctx.getBlockPos();
        world.setBlockState(pos, Blocks.BARREL.getDefaultState().with(FACING, player.getHorizontalFacing().getOpposite()));
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof BarrelBlockEntity)) return ActionResult.PASS;
        AccessorBarrelBlockEntity barrel = (AccessorBarrelBlockEntity) blockEntity;
        Inventories.fromTag(holding.getTag(), barrel.getInventory());
        holder.setHolding(null);
        sync(holder);
        return ActionResult.SUCCESS;
    }

    @Override
    public void render(@NotNull Holder holder, @NotNull MatrixStack matrices, @NotNull VertexConsumerProvider vcp, float tickDelta, int light) {
        if (holder instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) holder;
            matrices.push();
            matrices.scale(0.6f, 0.6f, 0.6f);
            matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-player.bodyYaw));
            matrices.translate(-0.5, 0.8, 0.2);
            MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(RENDER_STATE.get(), matrices, vcp, light, OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }
    }
}