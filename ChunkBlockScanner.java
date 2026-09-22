package com.vice.addon.utils;

import it.unimi.dsi.fastutil.longs.*;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Keeps a running list of block positions matching a predicate, grouped by chunk.
 * Scans a whole chunk column when it loads, then stays up to date one block at a
 * time from block-update events. Shared by every "find me X" module in this addon
 * so we don't scan the world separately for each one.
 *
 * Not thread-safe on purpose - only ever touch this from the client thread
 * (i.e. from inside Meteor event handlers), same as the rest of the addon.
 */
public class ChunkBlockScanner {
    private final Predicate<BlockState> matches;
    private final Long2ObjectMap<LongSet> chunks = new Long2ObjectOpenHashMap<>();

    public ChunkBlockScanner(Predicate<BlockState> matches) {
        this.matches = matches;
    }

    /** Wipes everything and rescans every currently loaded chunk. Call from onActivate(). */
    public void rescanAll() {
        chunks.clear();
        for (Chunk chunk : Utils.chunks()) scanChunk(chunk);
    }

    public void clear() {
        chunks.clear();
    }

    public void scanChunk(Chunk chunk) {
        long key = ChunkPos.toLong(chunk.getPos().x, chunk.getPos().z);

        int minY = mc.world.getBottomY();
        int maxY = minY + mc.world.getHeight();

        LongSet found = null;
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int x = chunk.getPos().getStartX(); x <= chunk.getPos().getEndX(); x++) {
            for (int z = chunk.getPos().getStartZ(); z <= chunk.getPos().getEndZ(); z++) {
                for (int y = minY; y < maxY; y++) {
                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);

                    if (matches.test(state)) {
                        if (found == null) found = new LongOpenHashSet();
                        found.add(pos.asLong());
                    }
                }
            }
        }

        if (found != null && !found.isEmpty()) chunks.put(key, found);
        else chunks.remove(key);
    }

    /** Call from a BlockUpdateEvent handler with the new state of the block that changed. */
    public void onBlockUpdate(BlockPos pos, BlockState newState) {
        long chunkKey = ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4);
        boolean isMatch = matches.test(newState);
        long posKey = pos.asLong();

        LongSet set = chunks.get(chunkKey);

        if (isMatch) {
            if (set == null) {
                set = new LongOpenHashSet();
                chunks.put(chunkKey, set);
            }
            set.add(posKey);
        } else if (set != null) {
            set.remove(posKey);
            if (set.isEmpty()) chunks.remove(chunkKey);
        }
    }

    public void removeChunk(int chunkX, int chunkZ) {
        chunks.remove(ChunkPos.toLong(chunkX, chunkZ));
    }

    /** All matching block positions (packed - use BlockPos.fromLong to unpack) across every scanned chunk. */
    public LongCollection allPositions() {
        LongList all = new LongArrayList();
        for (LongSet set : chunks.values()) all.addAll(set);
        return all;
    }

    /** Number of matching blocks in one chunk, or 0 if none. */
    public int countInChunk(int chunkX, int chunkZ) {
        LongSet set = chunks.get(ChunkPos.toLong(chunkX, chunkZ));
        return set == null ? 0 : set.size();
    }

    /** Packed keys (ChunkPos.toLong) of every chunk that currently has at least one match. */
    public LongSet chunkKeys() {
        return chunks.keySet();
    }

    /** Unpacks a chunk X coordinate from a key produced by ChunkPos.toLong. */
    public static int chunkKeyX(long key) {
        return (int) (key & 0xFFFFFFFFL);
    }

    /** Unpacks a chunk Z coordinate from a key produced by ChunkPos.toLong. */
    public static int chunkKeyZ(long key) {
        return (int) (key >> 32);
    }
}
