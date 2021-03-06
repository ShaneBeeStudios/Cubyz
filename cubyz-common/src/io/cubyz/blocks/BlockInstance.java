package io.cubyz.blocks;

import org.joml.Vector3i;

import io.cubyz.ClientOnly;
import io.cubyz.world.Chunk;
import io.cubyz.world.TorusSurface;
import io.cubyz.world.World;

public class BlockInstance {

	private Block block;
	private IBlockSpatial spatial;
	private Vector3i pos;
	private TorusSurface surface;
	public boolean neighborUp, neighborDown, neighborEast, neighborWest, neighborNorth, neighborSouth;
	public int light;
	
	public TorusSurface getStellarTorus() {
		return surface;
	}
	
	public void setStellarTorus(TorusSurface world) {
		this.surface = world;
	}
	
	public BlockInstance(Block block) {
		this.block = block;
		this.light = block.getLight();
	}
	
	public int getID() {
		return block.ID;
	}
	
	public Vector3i getPosition() {
		return pos;
	}
	
	public int getX() {
		return pos.x;
	}
	
	public int getY() {
		return pos.y;
	}
	
	public int getZ() {
		return pos.z;
	}
	
	public Block getBlock() {
		return block;
	}
	
	public void setBlock(Block b) {
		block = b;
	}
	
	public void setPosition(Vector3i pos) {
		this.pos = pos;
	}
	
	public BlockInstance[] getNeighbors(Chunk ch) {
		BlockInstance[] inst = new BlockInstance[6];
		// 0 = EAST  (x - 1)
		// 1 = WEST  (x + 1)
		// 2 = NORTH (z + 1)
		// 3 = SOUTH (z - 1)
		// 4 = DOWN
		// 5 = UP
		if(pos.y+1 < World.WORLD_HEIGHT)
			inst[5] = ch.getBlockInstanceAt(pos.x & 15, pos.y + 1, pos.z & 15);
		if(pos.y > 0)
			inst[4] = ch.getBlockInstanceAt(pos.x & 15, pos.y - 1, pos.z & 15);
		if((pos.z & 15) != 0)
			inst[3] = ch.getBlockInstanceAt(pos.x & 15, pos.y, (pos.z - 1) & 15);
		else
			inst[3] = surface.getBlockInstance(pos.x, pos.y, pos.z - 1);
		if((pos.z & 15) != 15)
			inst[2] = ch.getBlockInstanceAt(pos.x & 15, pos.y, (pos.z + 1) & 15);
		else
			inst[2] = surface.getBlockInstance(pos.x, pos.y, pos.z + 1);
		if((pos.x & 15) != 15)
			inst[1] = ch.getBlockInstanceAt((pos.x + 1) & 15, pos.y, pos.z & 15);
		else
			inst[1] = surface.getBlockInstance(pos.x + 1, pos.y, pos.z);
		if((pos.x & 15) != 0)
			inst[0] = ch.getBlockInstanceAt((pos.x - 1) & 15, pos.y, pos.z & 15);
		else
			inst[0] = surface.getBlockInstance(pos.x - 1, pos.y, pos.z);
		return inst;
	}
	
	public BlockInstance getNeighbor(int i, Chunk ch) {
		// 0 = EAST  (x - 1)
		// 1 = WEST  (x + 1)
		// 2 = NORTH (z + 1)
		// 3 = SOUTH (z - 1)
		// 4 = DOWN
		// 5 = UP
		switch(i) {
			case 5:
				if(pos.y+1 < World.WORLD_HEIGHT)
					return ch.getBlockInstanceAt(pos.x & 15, pos.y + 1, pos.z & 15);
				return null;
			case 4:
				if(pos.y > 0)
					return ch.getBlockInstanceAt(pos.x & 15, pos.y - 1, pos.z & 15);
				return null;
			case 3:
				if((pos.z & 15) != 0)
					return ch.getBlockInstanceAt(pos.x & 15, pos.y, (pos.z - 1) & 15);
				return surface.getBlockInstance(pos.x, pos.y, pos.z - 1);
			case 2:
				if((pos.z & 15) != 15)
					return surface.getBlockInstance(pos.x & 15, pos.y, (pos.z - 1) & 15);
				return surface.getBlockInstance(pos.x, pos.y, pos.z + 1);
			case 1:
				if((pos.x & 15) != 0)
					return surface.getBlockInstance((pos.x + 1) & 15, pos.y, pos.z & 15);
				return surface.getBlockInstance(pos.x + 1, pos.y, pos.z);
			case 0:
				if((pos.x & 15) != 0)
					return ch.getBlockInstanceAt((pos.x - 1) & 15, pos.y, pos.z & 15);
				return surface.getBlockInstance(pos.x - 1, pos.y, pos.z);
		}
		return null;
	}
	
	public IBlockSpatial getSpatial() {
		if (spatial == null) {
			spatial = ClientOnly.createBlockSpatial.apply(this);
		}
		return spatial;
	}

	public void setBreakingAnimation(float f) { // 0 <= f < 1
		// TODO Overlay the block with the corresponding block break graphics.
	}
	
}
