 package io.cubyz.world;

import java.util.ArrayList;

import org.joml.Vector3i;

import io.cubyz.api.CubzRegistries;
import io.cubyz.api.Registry;
import io.cubyz.blocks.Block;
import io.cubyz.blocks.BlockInstance;
import io.cubyz.blocks.Ore;

public class Chunk {

	private BlockInstance[][][] inst;
	private ArrayList<BlockInstance> list = new ArrayList<>();
	private ArrayList<BlockInstance> visibles = new ArrayList<>();
	private int ox, oy;
	private boolean generated;
	private boolean loaded;
	
	private static Registry<Block> br =  CubzRegistries.BLOCK_REGISTRY; // shortcut to BLOCK_REGISTRY
	
	// Normal:
	private static Block grass = br.getByID("cubyz:grass");
	private static Block sand = br.getByID("cubyz:sand");
	private static Block snow = br.getByID("cubyz:snow");
	private static Block dirt = br.getByID("cubyz:dirt");
	private static Block ice = br.getByID("cubyz:ice");
	private static Block stone = br.getByID("cubyz:stone");
	private static Block bedrock = br.getByID("cubyz:bedrock");
	
	// Ores:
	private static ArrayList<Ore> ores = new ArrayList<>();
	static {
		ores.add((Ore) br.getByID("cubyz:coal_ore"));
		ores.add((Ore) br.getByID("cubyz:iron_ore"));
		ores.add((Ore) br.getByID("cubyz:ruby_ore"));
		ores.add((Ore) br.getByID("cubyz:gold_ore"));
		ores.add((Ore) br.getByID("cubyz:diamond_ore"));
		ores.add((Ore) br.getByID("cubyz:emerald_ore"));
	}
	
	// Liquids:
	private static Block water = br.getByID("cubyz:water");
	
	public static final int SEA_LEVEL = 100;
	
	private World world;
	
	public Chunk(int ox, int oy, World world) {
		this.ox = ox;
		this.oy = oy;
		this.world = world;
	}
	
	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}
	
	public boolean isLoaded() {
		return loaded;
	}
	
	public int getX() {
		return ox;
	}
	
	public int getZ() {
		return oy;
	}
	
	public ArrayList<BlockInstance> list() {
		return list;
	}
	
	public ArrayList<BlockInstance> getVisibles() {
		return visibles;
	}
	
	/**
	 * Add the <code>Block</code> b at relative space defined by X, Y, and Z, and if out of bounds, call this method from the other chunk (only work for 1 chunk radius)<br/>
	 * Meaning that if x or z are out of bounds, this method will call the same method from other chunks to add it.
	 * @param b
	 * @param x
	 * @param y
	 * @param z
	 */
	public void addBlock(Block b, int x, int y, int z) {
		if(y >= World.WORLD_HEIGHT)
			return;
		int rx = x - (ox << 4);
		// Determines if the block is part of another chunk.
		if (rx < 0) {
			world.getChunk(ox - 1, oy).addBlock(b, x, y, z);
			return;
		}
		if (rx > 15) {
			world.getChunk(ox + 1, oy).addBlock(b, x, y, z);
			return;
		}
		int rz = z - (oy << 4);
		if (rz < 0) {
			world.getChunk(ox, oy - 1).addBlock(b, x, y, z);
			return;
		}
		if (rz > 15) {
			world.getChunk(ox, oy + 1).addBlock(b, x, y, z);
			return;
		}
		if(inst == null) {
			inst = new BlockInstance[16][World.WORLD_HEIGHT][16];
		} else { // Checks if there is a block on that position and deposits it if degradable.
			BlockInstance bi = inst[rx][y][rz];
			if(bi != null) {
				if(!bi.getBlock().isDegradable() || b.isDegradable()) {
					return;
				}
				removeBlockAt(rx, y, rz);
			}
		}
		BlockInstance inst0 = new BlockInstance(b);
		inst0.setPosition(new Vector3i(x, y, z));
		inst0.setWorld(world);
		list.add(inst0);
		inst[rx][y][rz] = inst0;
		if(generated) {
			BlockInstance[] neighbors = inst0.getNeighbors();
			for (int i = 0; i < neighbors.length; i++) {
				if (blocksLight(neighbors[i], inst0.getBlock().isTransparent())) {
					visibles.add(inst0);
					break;
				}
			}
			for (int i = 0; i < neighbors.length; i++) {
				if(neighbors[i] != null) {
					Chunk ch = getChunk(neighbors[i].getX(), neighbors[i].getZ());
					if (ch.contains(neighbors[i])) {
						BlockInstance[] neighbors1 = neighbors[i].getNeighbors();
						boolean vis = true;
						for (int j = 0; j < neighbors1.length; j++) {
							if (blocksLight(neighbors1[j], neighbors[i].getBlock().isTransparent())) {
								vis = false;
								break;
							}
						}
						if(vis) {
							ch.hideBlock(neighbors[i]);
						}
					}
				}
			}
		}
	}
	
	//TODO: Take in consideration caves.
	//TODO: Ore Clusters
	//TODO: Finish vegetation
	//TODO: Clean this method
	//TODO: Add more diversity
	public void generateFrom(float[][] map, float[][] vegetation, float[][] oreMap, float[][] heatMap) {
		if(inst == null) {
			inst = new BlockInstance[16][World.WORLD_HEIGHT][16];
		}
		int wx = ox << 4;
		int wy = oy << 4;
		
		// heightmap pass
		for (int px = 0; px < 16; px++) {
			for (int py = 0; py < 16; py++) {
				float value = map[px][py];
				int y = (int) (value * World.WORLD_HEIGHT);
				if(y == World.WORLD_HEIGHT)
					y--;
				int temperature = (int)((2-value+SEA_LEVEL/(float)World.WORLD_HEIGHT)*heatMap[px][py]*120) - 100;
				for (int j = y > SEA_LEVEL ? y : SEA_LEVEL; j >= 0; j--) {
					BlockInstance bi = null;
					if(j > y) {
						if (temperature <= 0 && j == SEA_LEVEL) {
							bi = new BlockInstance(ice);
						} else {
							bi = new BlockInstance(water);
						}
					}else if (((y < SEA_LEVEL + 4 && temperature > 5) || temperature > 40 || y < SEA_LEVEL) && j > y - 3) {
						bi = new BlockInstance(sand);
					} else if (j == y) {
						if(temperature > 0) {
							bi = new BlockInstance(grass);
						} else {
							bi = new BlockInstance(snow);
						}
					} else if (j > y - 3) {
						bi = new BlockInstance(dirt);
					} else if (j > 0) {
						float rand = oreMap[px][py] * j * (256 - j) * (128 - j) * 6741;
						rand = (((int) rand) & 8191) / 8191.0F;
						bi = selectOre(rand, j);
					} else {
						bi = new BlockInstance(bedrock);
					}
					bi.setPosition(new Vector3i(wx + px, j, wy + py));
					bi.setWorld(world);
					//world.blocks().add(bi);
					list.add(bi);
					inst[px][j][py] = bi;
					/*if (bi.getBlock() instanceof IBlockEntity) {
						updatables.add(bi);
					}*/
				}
			}
		}
		
		// Vegetation pass
		for (int px = 0; px < 16; px++) {
			for (int py = 0; py < 16; py++) {
				float value = vegetation[px][py];
				int incx = px == 0 ? 1 : -1;
				int incy = py == 0 ? 1 : -1;
				int temperature = (int)((2-map[px][py]+SEA_LEVEL/(float)World.WORLD_HEIGHT)*heatMap[px][py]*120) - 100;
				if (map[px][py] * World.WORLD_HEIGHT >= SEA_LEVEL + 4) {
					Structures.generateVegetation(this, wx + px, (int) (map[px][py] * World.WORLD_HEIGHT) + 1, wy + py, value, temperature, (int)((vegetation[px][py]-vegetation[px+incx][py+incy]) * 100000000 + incx + incy));
				}
			}
		}
		generated = true;
	}
	
	// Loads the chunk
	public void load() {
		loaded = true;
		int wx = ox << 4;
		int wy = oy << 4;
		boolean chx0 = world.getChunk(ox - 1, oy).isGenerated();
		boolean chx1 = world.getChunk(ox + 1, oy).isGenerated();
		boolean chy0 = world.getChunk(ox, oy - 1).isGenerated();
		boolean chy1 = world.getChunk(ox, oy + 1).isGenerated();
		for(BlockInstance bi : list) {
			BlockInstance[] neighbors = bi.getNeighbors();
			int j = bi.getY();
			int px = bi.getX()&15;
			int py = bi.getZ()&15;
			for (int i = 0; i < neighbors.length; i++) {
				if (blocksLight(neighbors[i], bi.getBlock().isTransparent())
											&& (j != 0 || i != 4)
											&& (px != 0 || i != 0 || chx0)
											&& (px != 15 || i != 1 || chx1)
											&& (py != 0 || i != 3 || chy0)
											&& (py != 15 || i != 2 || chy1)) {
					visibles.add(bi);
					break;
				}
			}
		}
		for (int i = 0; i < 16; i++) {
			// Checks if blocks from neighboring chunks are changed
			int [] neighbor = {1, 0, 2, 3};
			int [] dx = {-1, 16, i, i};
			int [] dy = {i, i, -1, 16};
			boolean [] toCheck = {chx0, chx1, chy0, chy1};
			for(int k = 0; k < 4; k++) {
				if (toCheck[k]) {
					for (int j = World.WORLD_HEIGHT - 1; j >= 0; j--) {
						BlockInstance inst0 = world.getBlock(wx + dx[k], j, wy + dy[k]);
						if(inst0 == null) {
							continue;
						}
						Chunk ch = getChunk(inst0.getX(), inst0.getZ());
						if(ch.contains(inst0)) {
							continue;
						}
						if (blocksLight(inst0.getNeighbor(neighbor[k]), inst0.getBlock().isTransparent())) {
							ch.revealBlock(inst0);
							continue;
						}
					}
				}
			}
		}
	}
	
	public boolean blocksLight(BlockInstance bi, boolean transparent) {
		if(bi == null || (bi.getBlock().isTransparent() && !transparent)) {
			return true;
		}
		return false;
	}
	
	// This function only allows a less than 50% of the underground to be ores.
	public BlockInstance selectOre(float rand, int height) {
		float chance1 = 0.0F;
		float chance2 = 0.0F;
		for (Ore ore : ores) {
			chance2 += ore.getChance();
			if(height < ore.getHeight() && rand > chance1 && rand < chance2)
				return new BlockInstance(ore);
			chance1 += ore.getChance();
		}
		return new BlockInstance(stone);
	}
	
	public boolean isGenerated() {
		return generated;
	}
	
	public BlockInstance getBlockInstanceAt(int x, int y, int z) {
		try {
			return inst[x][y][z];
		} catch (Exception e) {
			return null;
		}
	}
	
	// This function is here because it is mostly used by addBlock, where the neighbors to the added block usually are in the same chunk.
	public Chunk getChunk(int x, int y) {

		int cx = x;
		if(cx < 0)
			cx -= 15;
		cx = cx / 16;
		int cz = y;
		if(cz < 0)
			cz -= 15;
		cz = cz / 16;
		if(ox != cx || oy != cz)
			return world.getChunk(cx, cz);
		return this;
	}
	
	public void hideBlock(BlockInstance bi) {
		visibles.remove(bi);
	}
	
	public void revealBlock(BlockInstance bi) {
		visibles.add(bi);
	}
	
	public boolean contains(BlockInstance bi) {
		return visibles.contains(bi);
	}
	
	public void removeBlockAt(int x, int y, int z) {
		BlockInstance bi = getBlockInstanceAt(x, y, z);
		if (bi != null) {
			list.remove(bi);
			visibles.remove(bi);
			inst[x][y][z] = null;
			BlockInstance[] neighbors = bi.getNeighbors();
			for (int i = 0; i < neighbors.length; i++) {
				BlockInstance inst = neighbors[i];
				if (inst != null && inst != bi) {
					Chunk ch = getChunk(inst.getX(), inst.getZ());
					if (!ch.contains(inst)) {
						ch.revealBlock(inst);
					}
				}
			}
			inst[x][y][z] = null;
		}
	}
	
}