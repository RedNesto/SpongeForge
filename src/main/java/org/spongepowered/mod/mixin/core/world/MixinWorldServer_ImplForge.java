/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.mod.mixin.core.world;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.event.SpongeCommonEventFactory;
import org.spongepowered.common.bridge.world.ServerWorldBridge;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;
import org.spongepowered.common.mixin.core.world.MixinWorld;
import org.spongepowered.common.world.gen.SpongeChunkGenerator;
import org.spongepowered.common.world.gen.SpongeWorldGenerator;
import org.spongepowered.mod.world.gen.SpongeChunkGeneratorForge;

@Mixin(value = WorldServer.class, priority = 1001)
public abstract class MixinWorldServer_ImplForge extends MixinWorld implements ServerWorldBridge {

    @Shadow public abstract ChunkProviderServer getChunkProvider();

    /**
     * @author gabizou - May 23rd, 2018
     * @reason - Even though Dedicated server does handle this change, I'm inlining the
     * block check for the player since
     */
    @Redirect(
        method = "canMineBlockBody",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;isBlockProtected(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/EntityPlayer;)Z"
        )
    )
    private boolean isSpongeBlockProtected(MinecraftServer server, net.minecraft.world.World worldIn, BlockPos pos, EntityPlayer playerIn) {
        if (server.isBlockProtected(worldIn, pos, playerIn)) {
            return true;
        }
        if (!this.isFake() && SpongeImplHooks.isMainThread()) {
            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                // Might as well provide the active item in use.
                frame.addContext(EventContextKeys.USED_ITEM, ItemStackUtil.snapshotOf(playerIn.getActiveItemStack()));
                return SpongeCommonEventFactory.callChangeBlockEventPre(this, pos, playerIn).isCancelled();
            }
        }
        return false;
    }

    /**
     * @author gabizou - May 23rd, 2018
     * @reason - Since Forge adds the override to check for world providers whether
     * a block is modifiable, we have to provide the same contract to call "super".
     *
     * <p>Note: The event thrown here MUST be inverted for the {@link Cancellable#isCancelled()}
     * check, because if the block is modifiable, then the event should not be cancelled; however,
     * if the event is cancelled, then the block is not modifiable.
     * </p>
     */
    @Overwrite
    @Override
    public boolean isBlockModifiable(EntityPlayer player, BlockPos pos) {
        if (super.isBlockModifiable(player, pos)) {
            return true;
        }
        if (!this.isFake() && SpongeImplHooks.isMainThread()) {
            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                // Might as well provide the active item in use.
                frame.addContext(EventContextKeys.USED_ITEM, ItemStackUtil.snapshotOf(player.getActiveItemStack()));
                return !SpongeCommonEventFactory.callChangeBlockEventPre(this, pos, player).isCancelled();
            }
        }
        return false;
    }

    @Override
    public SpongeChunkGenerator bridge$createChunkGenerator(SpongeWorldGenerator newGenerator) {
        return new SpongeChunkGeneratorForge((net.minecraft.world.World) (Object) this, newGenerator.getBaseGenerationPopulator(),
                newGenerator.getBiomeGenerator());
    }


}
