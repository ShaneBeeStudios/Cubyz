package io.cubyz.blocks;

import org.joml.Vector3f;
import org.joml.Vector3i;

import io.cubyz.IRenderablePair;
import io.cubyz.api.IRegistryElement;
import io.cubyz.api.Resource;
import io.cubyz.items.Item;
import io.cubyz.items.ItemBlock;
import io.cubyz.world.World;


public class Block implements IRegistryElement {
	
	public static enum BlockClass {
		WOOD, STONE, SAND, UNBREAKABLE, LEAF, FLUID
	};
	
	private static final Vector3f ONE = new Vector3f(1, 1, 1);

	IRenderablePair pair;
	boolean transparent;
	boolean texConverted;
	/**
	 * Used for rendering optimization.<br/>
	 * Do not edit or rely on, as it is not an ID to actually describe the block on a persistent state.
	 */
	public int ID;			// Stores the numerical ID. This ID is generated by the registry. There is no need to fill it manually.
	private Resource id = Resource.EMPTY;
	private float hardness; // Time in seconds to break this block by hand.
	private boolean solid = true;
	private boolean selectable = true;
	private Item blockDrop;
	protected boolean degradable = false; // Meaning undegradable parts of trees or other structures can grow through this block.
	protected BlockClass bc;
	
	public Block() {}
	
	public Block(String id, float hardness, BlockClass bc) {
		setID(id);
		this.bc = bc;
		ItemBlock bd = new ItemBlock(this);
		bd.setTexture("blocks/"+this.id.getID()+".png");
		setBlockDrop(bd);
		this.hardness = hardness;
	}
	
	public boolean isDegradable() {
		return degradable;
	}
	
	public boolean isTransparent() {
		return transparent;
	}
	
	public Block setSolid(boolean solid) {
		this.solid = solid;
		return this;
	}
	
	public boolean isSolid() {
		return solid;
	}
	
	public Block setSelectable(boolean selectable) {
		this.selectable = selectable;
		return this;
	}
	
	public boolean isSelectable() {
		return selectable;
	}
	
	public IRenderablePair getBlockPair() {
		return pair;
	}
	
	public void setBlockPair(IRenderablePair pair) {
		this.pair = pair;
	}
	
	public boolean isTextureConverted() {
		return texConverted;
	}
	
	public void init() {}
	
	public Resource getRegistryID() {
		return id;
	}
	
	public void setID(int ID) {
		this.ID = ID;
	}
	
	/**
	 * The ID can only be changed <b>BEFORE</b> registering the block.
	 * @param id
	 */
	public Block setID(String id) {
		return setID(new Resource(id));
	}
	
	public Block setID(Resource id) {
		this.id = id;
		return this;
	}
	
	public void setBlockDrop(Item bd) {
		blockDrop = bd;
	}
	
	public Item getBlockDrop() {
		return blockDrop;
	}
	
	public float getHardness() {
		return hardness;
	}
	
	public Block setHardness(float hardness) {
		this.hardness = hardness;
		return this;
	}
	
	public BlockEntity createBlockEntity(BlockInstance bi) {
		return null;
	}
	
	public boolean hasBlockEntity() {
		return false;
	}
	
	public BlockClass getBlockClass() {
		return bc;
	}
	
	public Vector3f getLightAdjust() {
		return ONE;
	}
	
	public boolean onClick(World world, Vector3i pos, BlockInstance bi) {
		return false; // returns true if the block was did something while clicked
	}
	
}
