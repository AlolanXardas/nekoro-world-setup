package dev.xardas.nekoroworldsetup;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Saved with the world: whether the first start already ran, and which players have joined before. */
final class SetupData extends SavedData {
    boolean worldSetupDone;
    final Set<UUID> joined = new HashSet<>();

    static SetupData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SetupData::new, SetupData::load, null),
                // the file keeps its first name, so worlds remember what already ran
                "nekoro_world_setup");
    }

    private static SetupData load(CompoundTag tag, HolderLookup.Provider registries) {
        SetupData data = new SetupData();
        data.worldSetupDone = tag.getBoolean("world_setup_done");
        for (Tag t : tag.getList("joined", Tag.TAG_INT_ARRAY)) data.joined.add(NbtUtils.loadUUID(t));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("world_setup_done", worldSetupDone);
        ListTag list = new ListTag();
        for (UUID id : joined) list.add(NbtUtils.createUUID(id));
        tag.put("joined", list);
        return tag;
    }
}
