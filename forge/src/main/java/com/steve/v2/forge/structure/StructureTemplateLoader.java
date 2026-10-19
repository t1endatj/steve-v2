package com.steve.v2.forge.structure;

import com.steve.v2.forge.SteveMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import java.util.*;

/** Uses the native structure manager, including data packs. Block states are preserved; block entity NBT is not imported. */
public final class StructureTemplateLoader {
    public static Optional<List<BlockPlacement>> load(ServerLevel level, String name, BlockPos anchor) {
        ResourceLocation id = ResourceLocation.tryParse(name.contains(":") ? name : SteveMod.MODID + ":" + name);
        if (id == null) throw new IllegalArgumentException("Invalid structure ID: " + name);
        return level.getStructureManager().get(id).map(template -> {
            CompoundTag nbt = template.save(new CompoundTag());
            ListTag palette = nbt.getList("palette", Tag.TAG_COMPOUND);
            ListTag blocks = nbt.getList("blocks", Tag.TAG_COMPOUND);
            if (blocks.size() > 65536) throw new IllegalArgumentException("Template exceeds 65,536 blocks");
            var lookup = level.holderLookup(Registries.BLOCK);
            List<BlockPlacement> plan = new ArrayList<>();
            for (int i = 0; i < blocks.size(); i++) {
                var block = blocks.getCompound(i);
                var pos = block.getList("pos", Tag.TAG_INT);
                int state = block.getInt("state");
                if (pos.size() != 3 || state < 0 || state >= palette.size()) throw new IllegalArgumentException("Invalid template block");
                var blockState = NbtUtils.readBlockState(lookup, palette.getCompound(state));
                if (!blockState.isAir()) plan.add(new BlockPlacement(anchor.offset(pos.getInt(0), pos.getInt(1), pos.getInt(2)), blockState));
            }
            return plan;
        });
    }
    private StructureTemplateLoader() {}
}
