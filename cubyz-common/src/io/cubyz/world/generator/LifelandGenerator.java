package io.cubyz.world.generator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

import org.joml.Vector3i;

import io.cubyz.api.IRegistryElement;
import io.cubyz.api.Registry;
import io.cubyz.api.Resource;
import io.cubyz.blocks.Block;
import io.cubyz.blocks.Block.BlockClass;
import io.cubyz.blocks.BlockInstance;
import io.cubyz.blocks.Ore;
import io.cubyz.world.Chunk;
import io.cubyz.world.LocalTorusSurface;
import io.cubyz.world.TorusSurface;
import io.cubyz.world.World;
import io.cubyz.world.cubyzgenerators.*;
import io.cubyz.world.cubyzgenerators.biomes.Biome;

//TODO: Add more diversity
public class LifelandGenerator extends StellarTorusGenerator {
	
	public static void init() {
		GENERATORS.registerAll(new TerrainGenerator(), new RiverGenerator(), new OreGenerator(), new CaveGenerator(), new VegetationGenerator(), new GrassGenerator());
	}
	
	public static void initOres(Ore[] ores) {
		OreGenerator.ores = ores;
	}
	
	public static final Registry<Generator> GENERATORS = new Registry<>();
	ArrayList<Generator> sortedGenerators = new ArrayList<>();
	
	public void sortGenerators() {
		sortedGenerators.clear();
		for (IRegistryElement elem : GENERATORS.registered()) {
			Generator g = (Generator) elem;
			sortedGenerators.add(g);
		}
		sortedGenerators.sort(new Comparator<Generator>() {
			@Override
			public int compare(Generator a, Generator b) {
				if (a.getPriority() > b.getPriority()) {
					return 1;
				} else if (a.getPriority() < b.getPriority()) {
					return -1;
				} else {
					return 0;
				}
			}
		});
	}
	
	@Override
	public void generate(Chunk ch, TorusSurface surface) {
		int ox = ch.getX();
		int oy = ch.getZ();
		int wx = ox << 4;
		int wy = oy << 4;
		long seed = surface.getStellarTorus().getLocalSeed();
		// Generate some maps:
		float[][] heightMap = ((LocalTorusSurface)surface).getHeightMapData(wx-8, wy-8, 32, 32);
		float[][] heatMap = ((LocalTorusSurface)surface).getHeatMapData(wx-8, wy-8, 32, 32);
		Biome[][] biomeMap = ((LocalTorusSurface)surface).getBiomeMapData(wx-8, wy-8, 32, 32);
		boolean[][] vegetationIgnoreMap = new boolean[32][32]; // Stores places where vegetation should not grow, like caves and rivers.
		int[][] realHeight = new int[32][32];
		for(int px = 0; px < 32; px++) {
			for(int py = 0; py < 32; py++) {
				int h = (int)(heightMap[px][py]*World.WORLD_HEIGHT);
				if(h > World.WORLD_HEIGHT)
					h = World.WORLD_HEIGHT;
				realHeight[px][py] = h;
				
				heatMap[px][py] = ((2 - heightMap[px][py] + TerrainGenerator.SEA_LEVEL/(float)World.WORLD_HEIGHT)*heatMap[px][py]*120) - 100;
			}
		}
		
		Random r = new Random(seed);
		Block[][][] chunk = new Block[16][16][World.WORLD_HEIGHT];
		
		for (Generator g : sortedGenerators) {
			if (g instanceof FancyGenerator) {
				((FancyGenerator) g).generate(r.nextLong(), ox, oy, chunk, vegetationIgnoreMap, heatMap, realHeight, biomeMap);
			} else if (g instanceof BigGenerator) {
				((BigGenerator) g).generate(r.nextLong(), ox*16, oy*16, chunk, vegetationIgnoreMap, (LocalTorusSurface)surface);
			} else {
				g.generate(r.nextLong(), ox, oy, chunk, vegetationIgnoreMap);
			}
		}

		// Place the blocks in the chunk:
		for(int px = 0; px < 16; px++) {
			for(int py = 0; py < 16; py++) {
				for(int h = 0; h < World.WORLD_HEIGHT; h++) {
					Block b = chunk[px][py][h];
					if(b != null) {
						BlockInstance bi = new BlockInstance(b);
						bi.setPosition(new Vector3i(wx + px, h, wy + py));
						ch.rawAddBlock(px, h, py, bi);
						if(bi.getBlock() != null) {
							if (bi.getBlock().hasBlockEntity())
								ch.blockEntities().put(bi, bi.getBlock().createBlockEntity(bi.getPosition()));
							if (bi.getBlock().getBlockClass() == BlockClass.FLUID)
								ch.updatingLiquids().add(bi);
						}
					}
				}
			}
		}

		ch.applyBlockChanges();
	}

	@Override
	public Resource getRegistryID() {
		return new Resource("cubyz", "lifeland");
	}
}
