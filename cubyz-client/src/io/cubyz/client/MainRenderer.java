package io.cubyz.client;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL13C;

import io.cubyz.blocks.Block;
import io.cubyz.blocks.BlockInstance;
import io.cubyz.entity.Entity;
import io.cubyz.entity.Player;
import io.cubyz.math.CubyzMath;
import io.cubyz.math.Vector3fi;
import io.cubyz.world.Chunk;
import io.jungle.FrameBuffer;
import io.jungle.InstancedMesh;
import io.jungle.Mesh;
import io.jungle.ShadowMap;
import io.jungle.Spatial;
import io.jungle.Texture;
import io.jungle.Window;
import io.jungle.game.Context;
import io.jungle.renderers.IRenderer;
import io.jungle.renderers.Transformation;
import io.jungle.util.DirectionalLight;
import io.jungle.util.PointLight;
import io.jungle.util.ShaderProgram;
import io.jungle.util.SpotLight;
import io.jungle.util.Utils;

@SuppressWarnings("unchecked")
public class MainRenderer implements IRenderer {

	private ShaderProgram shaderProgram;
	private ShaderProgram depthShaderProgram;

	private static final float Z_NEAR = 0.01f;
	private static final float Z_FAR = 1000.0f;
	private boolean inited = false;
	private boolean doRender = true;
	public boolean orthogonal;
	private Transformation transformation;
	private String shaders = "";
	private Matrix4f prjViewMatrix = new Matrix4f();
	private FrustumIntersection frustumInt = new FrustumIntersection();
	public static ShadowMap shadowMap;

	public static final int MAX_POINT_LIGHTS = 0;
	public static final int MAX_SPOT_LIGHTS = 0;
	public static final Vector3f VECTOR3F_ZERO = new Vector3f(0, 0, 0);
	private float specularPower = 16f;

	public MainRenderer() {

	}

	public Transformation getTransformation() {
		return transformation;
	}

	public void setShaderFolder(String shaders) {
		this.shaders = shaders;
	}

	public void unloadShaders() throws Exception {
		shaderProgram.unbind();
		shaderProgram.cleanup();
		shaderProgram = null;
		System.gc();
	}

	public void setDoRender(boolean doRender) {
		this.doRender = doRender;
	}

	public void loadShaders() throws Exception {
		shaderProgram = new ShaderProgram();
		shaderProgram.createVertexShader(Utils.loadResource(shaders + "/vertex.vs"));
		shaderProgram.createFragmentShader(Utils.loadResource(shaders + "/fragment.fs"));
		shaderProgram.link();
		shaderProgram.createUniform("projectionMatrix");
		shaderProgram.createUniform("orthoProjectionMatrix");
		shaderProgram.createUniform("modelViewNonInstancedMatrix");
		shaderProgram.createUniform("viewMatrixInstanced");
		shaderProgram.createUniform("lightViewMatrixInstanced");
		shaderProgram.createUniform("texture_sampler");
		shaderProgram.createUniform("ambientLight");
		shaderProgram.createUniform("selectedNonInstanced");
		shaderProgram.createUniform("specularPower");
		shaderProgram.createUniform("isInstanced");
		shaderProgram.createUniform("shadowEnabled");
		shaderProgram.createMaterialUniform("material");
		shaderProgram.createPointLightListUniform("pointLights", MAX_POINT_LIGHTS);
		shaderProgram.createSpotLightListUniform("spotLights", MAX_SPOT_LIGHTS);
		shaderProgram.createDirectionalLightUniform("directionalLight");
		shaderProgram.createFogUniform("fog");
		shaderProgram.createUniform("shadowMap");
		shaderProgram.createUniform("cheapLighting");
		
		depthShaderProgram = new ShaderProgram();
		depthShaderProgram.createVertexShader(Utils.loadResource(shaders + "/depth_vertex.vs"));
		depthShaderProgram.createFragmentShader(Utils.loadResource(shaders + "/depth_fragment.fs"));
		depthShaderProgram.link();
		depthShaderProgram.createUniform("viewMatrixInstanced");
		depthShaderProgram.createUniform("modelLightViewNonInstancedMatrix");
		depthShaderProgram.createUniform("projectionMatrix");
		depthShaderProgram.createUniform("isInstanced");
		
		//shadowMap = new ShadowMap(1024, 1024);
		
		System.gc();
	}

	@Override
	public void init(Window window) throws Exception {
		transformation = new Transformation();
		window.setProjectionMatrix(transformation.getProjectionMatrix((float) Math.toRadians(70.0f), window.getWidth(),
				window.getHeight(), Z_NEAR, Z_FAR));
		loadShaders();

		inited = true;
	}

	public void clear() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
	}

	RenderList<Spatial>[] map = (RenderList<Spatial>[]) new RenderList[0];
	
	
	/**
	 * Renders a Cubyz world.
	 * @param window the window to render in
	 * @param ctx the Context object (will soon be replaced)
	 * @param ambientLight the ambient light to use
	 * @param directionalLight the directional light to use
	 * @param chunks the chunks being displayed
	 * @param blocks the type of blocks used (or availble) in the displayed chunks
	 * @param entities the entities to render
	 * @param spatials the special objects to render (that are neither entity, neither blocks, like sun and moon, or rain)
	 * @param localPlayer The world's local player
	 */
	public void render(Window window, Context ctx, Vector3f ambientLight, DirectionalLight directionalLight,
			Chunk[] chunks, Block[] blocks, Entity[] entities, Spatial[] spatials, Player localPlayer, int worldAnd) {
		if (window.isResized()) {
			glViewport(0, 0, window.getWidth(), window.getHeight());
			window.setResized(false);
			
			if (orthogonal) {
				window.setProjectionMatrix(transformation.getOrthoProjectionMatrix(1f, -1f, -1f, 1f, Z_NEAR, Z_FAR));
			} else {
				window.setProjectionMatrix(transformation.getProjectionMatrix(ctx.getCamera().getFov(), window.getWidth(),
						window.getHeight(), Z_NEAR, Z_FAR));
			}
		}
		if (!doRender)
			return;
		clear();
		ctx.getCamera().setViewMatrix(transformation.getViewMatrix(ctx.getCamera()));
		
		Spatial selected = null;
		int selectedBlock = -1;
		if (blocks.length != map.length) {
			map = (RenderList<Spatial>[]) new RenderList[blocks.length];
			int arrayListCapacity = 10;
			for (int i = 0; i < map.length; i++) {
				map[i] = new RenderList<Spatial>(arrayListCapacity);
			}
		}
		// Don't create a new ArrayList every time to reduce re-allocations:
		for (int i = 0; i < map.length; i++) {
			map[i].clear();
		}
		
		
		// Uses FrustumCulling on the chunks.
		prjViewMatrix.set(window.getProjectionMatrix());
		prjViewMatrix.mul(ctx.getCamera().getViewMatrix());
		// TODO: RayAabIntersection
		
		frustumInt.set(prjViewMatrix);
		if(localPlayer != null) {
			// Store the position locally to prevent glitches when the updateThread changes the position.
			Vector3fi pos = localPlayer.getPosition();
			int x0 = pos.x;
			float relX = pos.relX;
			int z0 = pos.z;
			float relZ = pos.relZ;
			float y0 = pos.y+1.5f;
			for (Chunk ch : chunks) {
				if (!frustumInt.testAab(ch.getMin(localPlayer, worldAnd), ch.getMax(localPlayer, worldAnd)))
					continue;
				BlockInstance[] vis = ch.getVisibles();
				for (int i = 0; vis[i] != null; i++) {
					BlockInstance bi = vis[i];
					float x = CubyzMath.matchSign((bi.getX() - x0) & worldAnd, worldAnd) - relX;
					float y = bi.getY() - y0;
					float z = CubyzMath.matchSign((bi.getZ() - z0) & worldAnd, worldAnd) - relZ;
					// Do the frustum culling directly here.
					if(frustumInt.testSphere(x, y, z, 0.866025f)) {
						// Only draw blocks that have at least one face facing the player.
						if(bi.getBlock().isTransparent() || // Ignore transparent blocks in the process, so the surface of water can still be seen from below.
								(x > 0.5001f && !bi.neighborEast) ||
								(x < -0.5001f && !bi.neighborWest) ||
								(y > 0.5001f && !bi.neighborDown) ||
								(y < -0.5001f && !bi.neighborUp) ||
								(z > 0.5001f && !bi.neighborSouth) ||
								(z < -0.5001f && !bi.neighborNorth)) {
							Spatial tmp = (Spatial) bi.getSpatial();
							tmp.setPosition(x, y, z);
							if(Chunk.easyLighting)
								ch.getCornerLight(bi.getX() & 15, bi.getY(), bi.getZ() & 15, ambientLight, tmp.light);
							if (tmp.isSelected()) {
								selected = tmp;
								selectedBlock = bi.getID();
								continue;
							}
							map[bi.getID()].add(tmp);
						}
					}
				}
			}
		}
		
		// sort distances for correct render of transparent blocks
		Vector3f tmpa = new Vector3f();
		Vector3f tmpb = new Vector3f();
		for (int i = 0; i < blocks.length; i++) {
			Block b = blocks[i];
			if (b != null && b.isTransparent()) {
				map[b.ID].sort((sa, sb) -> {
					ctx.getCamera().getPosition().sub(sa.getPosition(), tmpa);
					ctx.getCamera().getPosition().sub(sb.getPosition(), tmpb);
					float lenA = tmpa.lengthSquared();
					float lenB = tmpb.lengthSquared();
					if (lenA > lenB) {
						return 1;
					} else if (lenA == lenB) {
						return 0;
					} else {
						return -1;
					}
				});
			}
		}
		
		if (shadowMap != null) { // remember it will be disableable
			renderDepthMap(directionalLight, blocks, selected, selectedBlock);
			glViewport(0, 0, window.getWidth(), window.getHeight()); // reset viewport
			if (orthogonal) {
				window.setProjectionMatrix(transformation.getOrthoProjectionMatrix(1f, -1f, -1f, 1f, Z_NEAR, Z_FAR));
			} else {
				window.setProjectionMatrix(transformation.getProjectionMatrix(ctx.getCamera().getFov(), window.getWidth(),
						window.getHeight(), Z_NEAR, Z_FAR));
			}
			ctx.getCamera().setViewMatrix(transformation.getViewMatrix(ctx.getCamera()));
		}
		renderScene(ctx, ambientLight, null /* point light */, null /* spot light */, directionalLight, map, blocks, entities, spatials,
				localPlayer, selected, selectedBlock);
		if (ctx.getHud() != null) {
			ctx.getHud().render(window);
		}
	}
	
	public Matrix4f getLightViewMatrix(DirectionalLight light) {
		float lightAngleX = (float) Math.acos(light.getDirection().z);
		float lightAngleY = (float) Math.asin(light.getDirection().x);
		float lightAngleZ = 0f;
		return transformation.getLightViewMatrix(
				new Vector3f(light.getDirection()).mul(5f),
				new Vector3f(lightAngleX, lightAngleY, lightAngleZ));
	}
	
	public Matrix4f getShadowProjectionMatrix() {
		return transformation.getOrthoProjectionMatrix(-10f, 10f, -10f, 10f, 1f, 50f);
	}
	
	// for shadow map
	public void renderDepthMap(DirectionalLight light, Block[] blocks, Spatial selected, int selectedBlock) {
		FrameBuffer fbo = shadowMap.getDepthMapFBO();
		fbo.bind();
		Texture depthTexture = fbo.getDepthTexture();
		glViewport(0, 0, depthTexture.getWidth(), depthTexture.getHeight());
		glClear(GL_DEPTH_BUFFER_BIT);
		depthShaderProgram.bind();
		
		Matrix4f lightViewMatrix = getLightViewMatrix(light);
		// TODO: only create new vector if changed
		depthShaderProgram.setUniform("projectionMatrix", getShadowProjectionMatrix());
		depthShaderProgram.setUniform("viewMatrixInstanced", lightViewMatrix);
		
		for (int i = 0; i < blocks.length; i++) {
			if (map[i] == null)
				continue;
			Mesh mesh = Meshes.blockMeshes.get(blocks[i]);
			if (selectedBlock == i) {
				map[i].add(selected);
			}
			if (mesh.isInstanced()) {
				InstancedMesh ins = (InstancedMesh) mesh;
				depthShaderProgram.setUniform("isInstanced", 1);
				ins.renderListInstanced(map[i], transformation);
			} else {
				depthShaderProgram.setUniform("isInstanced", 0);
				mesh.renderList(map[i], (Spatial gameItem) -> {
					Matrix4f modelViewMatrix = transformation.getModelViewMatrix(gameItem, lightViewMatrix);
					if (orthogonal) {
						modelViewMatrix = transformation.getOrtoProjModelMatrix(gameItem);
					}
					if (gameItem.isSelected())
						depthShaderProgram.setUniform("selectedNonInstanced", 1f);
					depthShaderProgram.setUniform("modelViewNonInstancedMatrix", modelViewMatrix);
				});
				if (selectedBlock == i) {
					depthShaderProgram.setUniform("selectedNonInstanced", 0f);
				}
			}
		}
		// TODO: render entities
		depthShaderProgram.unbind();
		fbo.unbind();
	}
	
	public void renderScene(Context ctx, Vector3f ambientLight, PointLight[] pointLightList, SpotLight[] spotLightList,
			DirectionalLight directionalLight, RenderList<Spatial>[] map, Block[] blocks, Entity[] entities, Spatial[] spatials, Player p, Spatial selected,
			int selectedBlock) {
		shaderProgram.bind();
		
		shaderProgram.setUniform("fog", ctx.getFog());
		shaderProgram.setUniform("projectionMatrix", ctx.getWindow().getProjectionMatrix());
		shaderProgram.setUniform("texture_sampler", 0);
		if (shadowMap != null) {
			shaderProgram.setUniform("orthoProjectionMatrix", getShadowProjectionMatrix());
			shaderProgram.setUniform("lightViewMatrixInstanced", getLightViewMatrix(directionalLight));
			shaderProgram.setUniform("shadowMap", 1);
			shaderProgram.setUniform("shadowEnabled", true);
		} else {
			shaderProgram.setUniform("shadowEnabled", false);
			if (Chunk.easyLighting) {
				shaderProgram.setUniform("cheapLighting", true);
			}
		}
		
		Matrix4f viewMatrix = ctx.getCamera().getViewMatrix();
		shaderProgram.setUniform("viewMatrixInstanced", viewMatrix);
		
		renderLights(viewMatrix, ambientLight, pointLightList, spotLightList, directionalLight);
		
		for (int i = 0; i < blocks.length; i++) {
			if (map[i] == null)
				continue;
			Mesh mesh = Meshes.blockMeshes.get(blocks[i]);
			if (mesh == null) { // TODO: remove, prob related to custom ores
				return;
			}
			shaderProgram.setUniform("material", mesh.getMaterial());
			if (selectedBlock == i) {
				map[i].add(selected);
			}
			if (mesh.isInstanced()) {
				if (shadowMap != null) {
					glActiveTexture(GL13C.GL_TEXTURE1);
					glBindTexture(GL_TEXTURE_2D, shadowMap.getDepthMapFBO().getDepthTexture().getId());
				}
				InstancedMesh ins = (InstancedMesh) mesh;
				shaderProgram.setUniform("isInstanced", 1);
				ins.renderListInstanced(map[i], transformation);
			} else {
				shaderProgram.setUniform("isInstanced", 0);
				mesh.renderList(map[i], (Spatial gameItem) -> {
					Matrix4f modelViewMatrix = transformation.getModelViewMatrix(gameItem, viewMatrix);
					if (orthogonal) {
						modelViewMatrix = transformation.getOrtoProjModelMatrix(gameItem, viewMatrix);
					}
					if (gameItem.isSelected())
						shaderProgram.setUniform("selectedNonInstanced", 1f);
					shaderProgram.setUniform("modelViewNonInstancedMatrix", modelViewMatrix);
				});
				if (selectedBlock == i) {
					shaderProgram.setUniform("selectedNonInstanced", 0f);
				}
			}
		}
		
		for (int i = 0; i < entities.length; i++) {
			Entity ent = entities[i];
			if (ent != null && ent != p && Meshes.entityMeshes.get(ent.getType()) != null) { // don't render local player
				Mesh mesh = Meshes.entityMeshes.get(ent.getType());
				shaderProgram.setUniform("material", mesh.getMaterial());
				
				mesh.renderOne(() -> {
					Vector3f position = ent.getRenderPosition(p.getPosition());
					Matrix4f modelViewMatrix = transformation.getModelViewMatrix(transformation.getModelMatrix(position, ent.getRotation(), 1f), viewMatrix);
					shaderProgram.setUniform("isInstanced", 0);
					shaderProgram.setUniform("selectedNonInstanced", 0f);
					shaderProgram.setUniform("modelViewNonInstancedMatrix", modelViewMatrix);
				});
			}
		}
		
		shaderProgram.setUniform("fog.activ", 0); // manually disable the fog
		
		for (int i = 0; i < spatials.length; i++) {
			Spatial spatial = spatials[i];
			Mesh mesh = spatial.getMesh();
			mesh.renderOne(() -> {
				Matrix4f modelViewMatrix = transformation.getModelViewMatrix(
						transformation.getModelMatrix(spatial.getPosition(), spatial.getRotation(), spatial.getScale()),
						viewMatrix);
				shaderProgram.setUniform("isInstanced", 0);
				shaderProgram.setUniform("selectedNonInstanced", 0f);
				shaderProgram.setUniform("modelViewNonInstancedMatrix", modelViewMatrix);
			});
		}
		
		shaderProgram.unbind();
	}

	private void renderLights(Matrix4f viewMatrix, Vector3f ambientLight, PointLight[] pointLightList,
			SpotLight[] spotLightList, DirectionalLight directionalLight) {

		shaderProgram.setUniform("ambientLight", ambientLight);
		shaderProgram.setUniform("specularPower", specularPower);

		if(!Chunk.easyLighting) {
			// Process Point Lights
			int numLights = pointLightList != null ? pointLightList.length : 0;
			for (int i = 0; i < numLights; i++) {
				// Get a copy of the point light object and transform its position to view
				// coordinates
				PointLight currPointLight = new PointLight(pointLightList[i]);
				Vector3f lightPos = currPointLight.getPosition();
				Vector4f aux = new Vector4f(lightPos, 1);
				aux.mul(viewMatrix);
				lightPos.x = aux.x;
				lightPos.y = aux.y;
				lightPos.z = aux.z;
				shaderProgram.setUniform("pointLights", currPointLight, i);
			}
	
			// Process Spot Ligths
			numLights = spotLightList != null ? spotLightList.length : 0;
			for (int i = 0; i < numLights; i++) {
				// Get a copy of the spot light object and transform its position and cone
				// direction to view coordinates
				SpotLight currSpotLight = new SpotLight(spotLightList[i]);
				Vector4f dir = new Vector4f(currSpotLight.getConeDirection(), 0);
				dir.mul(viewMatrix);
				currSpotLight.setConeDirection(new Vector3f(dir.x, dir.y, dir.z));
				Vector3f lightPos = currSpotLight.getPointLight().getPosition();
	
				Vector4f aux = new Vector4f(lightPos, 1);
				aux.mul(viewMatrix);
				lightPos.x = aux.x;
				lightPos.y = aux.y;
				lightPos.z = aux.z;
	
				shaderProgram.setUniform("spotLights", currSpotLight, i);
			}
			// Get a copy of the directional light object and transform its position to view
			// coordinates
			DirectionalLight currDirLight = new DirectionalLight(directionalLight);
			Vector4f dir = new Vector4f(currDirLight.getDirection(), 0);
			dir.mul(viewMatrix);
			currDirLight.setDirection(new Vector3f(dir.x, dir.y, dir.z));
			shaderProgram.setUniform("directionalLight", currDirLight);
		}
	}

	@Override
	public void cleanup() {
		if (shaderProgram != null) {
			shaderProgram.cleanup();
		}
		if (depthShaderProgram != null) {
			depthShaderProgram.cleanup();
		}
		if (shadowMap != null) {
			shadowMap.getDepthMapFBO().cleanup();
		}
	}

	@Override
	public void render(Window win, Context ctx, Vector3f ambientLight, PointLight[] pointLightList,
			SpotLight[] spotLightList, DirectionalLight directionalLight) {
		throw new UnsupportedOperationException("Cubyz Renderer doesn't support this method.");
	}

	@Override
	public void setPath(String dataName, String path) {
		if (dataName.equals("shaders") || dataName.equals("shadersFolder")) {
			if (inited) {
				try {
					doRender = false;
					unloadShaders();
					shaders = path;
					loadShaders();
					doRender = true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				shaders = path;
			}
		}
	}

}
