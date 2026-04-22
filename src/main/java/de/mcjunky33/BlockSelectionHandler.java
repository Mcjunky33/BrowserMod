package de.mcjunky33;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;

public class BlockSelectionHandler {

    public static void init() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {

            if (!StreamScreen.isSelecting) return InteractionResult.PASS;

            BlockPos hitPos = hitResult.getBlockPos();

            if (hitPos.getY() <= 0) {
                player.sendSystemMessage(Component.literal("§cError: Cannot select floor or blocks at/below Y=0."));
                return InteractionResult.FAIL;
            }

            if (StreamScreen.pos1 == null) {

                StreamScreen.pos1 = hitPos;
                StreamScreen.face = hitResult.getDirection();

                player.sendSystemMessage(Component.literal("Set Pos 1: " + StreamScreen.pos1.toShortString()));

            } else if (StreamScreen.pos2 == null) {

                if (hitPos.equals(StreamScreen.pos1)) {
                    player.sendSystemMessage(Component.literal("§cError: Pos 2 cannot be the same as Pos 1."));
                    return InteractionResult.FAIL;
                }

                boolean sameX = hitPos.getX() == StreamScreen.pos1.getX();
                boolean sameY = hitPos.getY() == StreamScreen.pos1.getY();
                boolean sameZ = hitPos.getZ() == StreamScreen.pos1.getZ();

                if (!sameX && !sameY && !sameZ) {
                    player.sendSystemMessage(Component.literal("§cError: Selection must be a flat surface (aligned on X, Y, or Z)."));
                    return InteractionResult.FAIL;
                }


                StreamScreen.pos2 = hitPos;
                StreamScreen.isSelecting = false;
                StreamScreen.isStreaming = true;

                player.sendSystemMessage(Component.literal("§aScreen created: " + StreamScreen.pos1.toShortString() + " to " + StreamScreen.pos2.toShortString()));


                Minecraft.getInstance().setScreen(new StreamScreen());
            }

            return InteractionResult.FAIL;
        });
    }
}