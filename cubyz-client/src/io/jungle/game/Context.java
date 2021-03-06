package io.jungle.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.jungle.Camera;
import io.jungle.Fog;
import io.jungle.Mesh;
import io.jungle.Spatial;
import io.jungle.Window;
import io.jungle.hud.Hud;

public class Context {

	private Window win;
	private Game game;
	private Camera camera;
	private Hud hud;
	private Map<Mesh, List<Spatial>> meshMap;
	private Fog fog;
	
	public Context(Game g, Camera c) {
		camera = c;
		game = g;
		win = g.win;
		meshMap = new HashMap<>();
		hud = new Hud();
	}
	
	public Fog getFog() {
		return fog;
	}

	public void setFog(Fog fog) {
		this.fog = fog;
	}

	public void setSpatials(Spatial[] gameItems) {
		meshMap.clear();
		if(gameItems != null) {
			int numGameItems = gameItems.length;
			for (int i = 0; i < numGameItems; i++) {
				Spatial gameItem = gameItems[i];
				for (Mesh mesh : gameItem.getMeshes()) {
					List<Spatial> list = meshMap.get(mesh);
					if (list == null) {
						list = new ArrayList<>();
						meshMap.put(mesh, list);
					}
					list.add(gameItem);
				}
	        }
	    }
	}
	
	public Map<Mesh, List<Spatial>> getMeshMap() {
		return meshMap;
	}
	
	public Hud getHud() {
		return hud;
	}
	
	public void setHud(Hud hud) {
		this.hud = hud;
	}
	
	public Spatial[] getSpatials() {
		ArrayList<Spatial> spl = new ArrayList<>();
		for (Mesh mesh : meshMap.keySet()) {
			List<Spatial> sp = meshMap.get(mesh);
			for (Spatial s : sp) {
				spl.add(s);
			}
		}
		return spl.toArray(new Spatial[spl.size()]);
	}
	
	public void addSpatial(Spatial s) {
		for (Mesh mesh : s.getMeshes()) {
	        List<Spatial> list = meshMap.get(mesh);
	        if ( list == null ) {
	            list = new ArrayList<>();
	            meshMap.put(mesh, list);
	        }
	        list.add(s);
		}
	}
	
	public void removeSpatial(Spatial s) {
		for (Mesh mesh : meshMap.keySet()) {
			List<Spatial> sp = meshMap.get(mesh);
			if (sp.contains(s)) {
				sp.remove(s);
			}
		}
	}

	public Window getWindow() {
		return win;
	}

	public Game getGame() {
		return game;
	}

	public Camera getCamera() {
		return camera;
	}

}
