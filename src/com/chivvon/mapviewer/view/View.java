package com.chivvon.mapviewer.view;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;

import com.chivvon.mapviewer.Configuration;
import com.chivvon.mapviewer.Constants;
import com.chivvon.mapviewer.component.Animation;
import com.chivvon.mapviewer.component.Buffer;
import com.chivvon.mapviewer.component.CollisionMap;
import com.chivvon.mapviewer.component.FloorDefinition;
import com.chivvon.mapviewer.component.Fog;
import com.chivvon.mapviewer.component.Frame;
import com.chivvon.mapviewer.component.GameApplet;
import com.chivvon.mapviewer.component.GameFont;
import com.chivvon.mapviewer.component.Graphic;
import com.chivvon.mapviewer.component.IdentityKit;
import com.chivvon.mapviewer.component.MapRegion;
import com.chivvon.mapviewer.component.Model;
import com.chivvon.mapviewer.component.ObjectDefinition;
import com.chivvon.mapviewer.component.ProducingGraphicsBuffer;
import com.chivvon.mapviewer.component.Rasterizer2D;
import com.chivvon.mapviewer.component.Rasterizer3D;
import com.chivvon.mapviewer.component.Resource;
import com.chivvon.mapviewer.component.ResourceProvider;
import com.chivvon.mapviewer.component.SceneGraph;
import com.chivvon.mapviewer.component.VariableBits;
import com.chivvon.mapviewer.component.VariablePlayer;
import com.chivvon.mapviewer.util.IntUtils;
import com.softgate.fs.FileStore;
import com.softgate.fs.IndexedFileSystem;
import com.softgate.fs.binary.Archive;
import com.softgate.util.CompressionUtil;

public class View extends GameApplet {

	private static final long serialVersionUID = 1L;

	private int heightLevel = 0;

	private int xCameraPos = Constants.MAP_WIDTH * 32 * 128;
	private int yCameraPos = Constants.MAP_HEIGHT * 32 * 128;
	private int zCameraPos = -540;

	private int xCameraCurve = (int) (Math.random() * 20D) - 10 & 0x7ff;
	private int yCameraCurve = 128;

	private int fieldJ;
	private int heightlevelChangeRequest = -1;

	private int lastMouseX = -1;
	private int lastMouseY = -1;

	private boolean renderSettingsChanged;
	private boolean showAllHLs;
	private boolean mapRefreshRequested;
	public boolean mapLoaded;

	private static int loadMapRequestX = -1;
	private static int loadMapRequestZ = -1;

	private ProducingGraphicsBuffer gameScreenIP;
	public static ResourceProvider resourceProvider;
	private CollisionMap[] collisionMaps;
	private MapRegion mapRegion;
	private int[][][] tileHeights;
	public static View instance;
	private byte[][][] tileFlags;
	private Fog fog = new Fog();
	private SceneGraph scene;

	protected int currentRegion;

	private GameFont regularText;

	private void drawUI() {
		this.gameScreenIP.initDrawingArea();

		int relX = Constants.APP_WIDTH / 2;
		int relY = Constants.APP_HEIGHT / 2;

		if (Configuration.debug) {
			regularText.drawText(0xffffff, "X Pos: " + Configuration.START_X, relX - 348, relY - 237);
			regularText.drawText(0xffffff, "Y Pos: " + Configuration.START_Y, relX - 348, relY - 222);
			regularText.drawText(0xffffff, "Region: " + currentRegion, relX - 339, relY - 207);
			regularText.drawText(0xffffff, "xCameraPos: " + xCameraPos, relX - 326, relY - 192);
			regularText.drawText(0xffffff, "yCameraPos: " + yCameraPos, relX - 326, relY - 177);
			regularText.drawText(0xffffff, "zCameraPos: " + zCameraPos, relX - 326, relY - 162);
			regularText.drawText(0xffffff, "xCameraCur: " + xCameraCurve, relX - 326, relY - 147);
			regularText.drawText(0xffffff, "yCameraCur: " + yCameraCurve, relX - 326, relY - 132);
		}
	}

	public static final IndexedFileSystem cache = IndexedFileSystem.init(Paths.get("./Cache/"));

	public View() {
		collisionMaps = new CollisionMap[4];
	}

	public void init() {
		SceneGraph.lowMem = false;
		Rasterizer3D.lowMem = false;
		MapRegion.lowMem = false;
		ObjectDefinition.lowMemory = false;
		initClientFrame(450, 300);
	}

	@Override
	protected void startUp() {
		try {
			System.out.println("started");
			this.drawLoadingText(5, "Grabbing files from cache 5/100");
			Thread.sleep(175);

			FileStore archiveStore = cache.getStore(0);

			this.drawLoadingText(5, "Decoding archives 5/100");
			Thread.sleep(175);

			Archive titleArchive = Archive.decode(archiveStore.readFile(Constants.TITLE_CRC));
			Archive configArchive = Archive.decode(archiveStore.readFile(Constants.CONFIG_CRC));
			Archive crcArchive = Archive.decode(archiveStore.readFile(Constants.UPDATE_CRC));
			Archive textureArchive = Archive.decode(archiveStore.readFile(Constants.TEXTURES_CRC));

			regularText = new GameFont(false, "p12_full", titleArchive);

			this.drawLoadingText(10, "Decoded archives 10/100");
			Thread.sleep(175);

			this.drawLoadingText(15, "Initializing tile flags 15/100");
			Thread.sleep(175);

			tileFlags = new byte[4][Constants.MAP_TILE_WIDTH][Constants.MAP_TILE_HEIGHT];
			tileHeights = new int[4][Constants.MAP_TILE_WIDTH + 1][Constants.MAP_TILE_HEIGHT + 1];

			this.drawLoadingText(20, "Initializing scene graph 20/100");
			Thread.sleep(175);

			scene = new SceneGraph(tileHeights);

			this.drawLoadingText(25, "Initializing collision map 25/100");
			Thread.sleep(175);

			for (int j = 0; j < 4; j++) {
				collisionMaps[j] = new CollisionMap(Constants.MAP_TILE_WIDTH, Constants.MAP_TILE_HEIGHT);
			}

			this.drawLoadingText(30, "Initializing frames 30/100");
			Thread.sleep(175);

			Frame.animationlist = new Frame[3000][0];

			this.drawLoadingText(35, "Unpacking resources 35/100");
			Thread.sleep(175);

			resourceProvider = new ResourceProvider();
			resourceProvider.initialize(crcArchive, this);

			Model.method459(resourceProvider.getModelCount(), resourceProvider);

			this.drawLoadingText(40, "Unpacking textures 40/100");
			Thread.sleep(175);

			Rasterizer3D.loadTextures(textureArchive);
			Rasterizer3D.setBrightness(0.80000000000000004D);
			Rasterizer3D.initiateRequestBuffers();

			this.drawLoadingText(45, "Unpacking animations 45/100");
			Thread.sleep(175);

			Animation.init(configArchive);

			this.drawLoadingText(50, "Unpacking objects definitions 50/100");
			Thread.sleep(175);

			ObjectDefinition.init(configArchive);

			this.drawLoadingText(55, "Unpacking tiles definitions 55/100");
			Thread.sleep(175);

			FloorDefinition.init(configArchive);

			this.drawLoadingText(60, "Unpacking character models 60/100");
			Thread.sleep(175);

			IdentityKit.init(configArchive);

			this.drawLoadingText(65, "Unpacking graphics 65/100");
			Thread.sleep(175);

			Graphic.init(configArchive);

			this.drawLoadingText(70, "Unpacking player configs 70/100");
			Thread.sleep(175);

			VariablePlayer.init(configArchive);

			this.drawLoadingText(75, "Unpacking interface configs 75/100");
			Thread.sleep(175);

			VariableBits.init(configArchive);

			this.drawLoadingText(80, "Initializing game screen 80/100");
			Thread.sleep(175);

			gameScreenIP = new ProducingGraphicsBuffer(765, 503);

			Rasterizer3D.reposition(765, 503);

			int isOnScreen[] = new int[9];

			for (int i8 = 0; i8 < 9; i8++) {
				int k8 = 128 + i8 * 32 + 15;
				int l8 = 600 + k8 * 3;
				int i9 = Rasterizer3D.anIntArray1470[k8];
				isOnScreen[i8] = l8 * i9 >> 16;
			}

			this.drawLoadingText(85, "Initializing viewport 85/100");
			Thread.sleep(175);

			SceneGraph.setupViewport(500, 800, 765, 503, isOnScreen);

			this.drawLoadingText(95, "Generating map 95/100");
			Thread.sleep(175);

			loadMapCoordinates(Configuration.START_X, Configuration.START_Y);

			loadMap(Configuration.START_X, Configuration.START_Y);

			this.drawLoadingText(100, "Complete 100/100");
			Thread.sleep(175);

			currentRegion = IntUtils.coordinateToRegionId(Configuration.START_X, Configuration.START_Y);
			System.out.println("finished");
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	@Override
	protected void processDrawing() {
		try {

			if (mapLoaded) {
				drawScene();
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void drawScene() {
		int j = showAllHLs ? 3 : heightLevel;
		int l = xCameraPos;
		int i1 = zCameraPos;
		int j1 = yCameraPos;
		int k1 = yCameraCurve;
		int l1 = xCameraCurve;
		Model.aBoolean1684 = true;
		Model.anInt1687 = 0;
		Model.anInt1685 = super.mouseX - 4;
		Model.anInt1686 = super.mouseY - 4;
		gameScreenIP.initDrawingArea();
		Rasterizer2D.clear();
		fieldJ = j;
		scene.render(xCameraPos, yCameraPos, xCameraCurve, zCameraPos, fieldJ, yCameraCurve);
		scene.clearGameObjectCache();

		if (Configuration.enableFog) {
			int baseFogDistance = (int) Math.sqrt(Math.pow(zCameraPos, 2));
			int fogStart = baseFogDistance + 1700;
			int fogEnd = baseFogDistance + 2100;
			fog.renderFog(false, fogStart, fogEnd, 2);
		}

		drawUI();

		gameScreenIP.drawGraphics(0, super.graphics, 0);

		xCameraPos = l;
		zCameraPos = i1;
		yCameraPos = j1;
		yCameraCurve = k1;
		xCameraCurve = l1;
	}

	@Override
	protected void processGameLoop() {

		if (resourceProvider != null) {
			processOnDemandQueue();
		}
		
		if ((loadMapRequestX != -1 && loadMapRequestZ != -1)) {
			try {				
				loadMap(loadMapRequestX, loadMapRequestZ);
			
				loadMapRequestX = -1;
				loadMapRequestZ = -1;
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		if (mapLoaded) {

			if (mapRefreshRequested) {
				scene.clearCullingClusters();
				scene.clearTiles();
				setHeightLevel(heightLevel);
				mapRefreshRequested = false;
			}

			if (heightlevelChangeRequest != -1) {				
				heightLevel = heightlevelChangeRequest;
				scene.method275(heightlevelChangeRequest);
				repaint();
				heightlevelChangeRequest = -1;
			}
			
			if (renderSettingsChanged) {
				refreshMap();
				renderSettingsChanged = false;
			}

			processInput();

			if (clickMode2 == 1) {
				// left click
			}

		}

	}

	private void processOnDemandQueue() {
		do {
			Resource resource;
			do {
				resource = resourceProvider.next();
				if (resource == null)
					return;
				if (resource.dataType == 0)
					Model.method460(resource.buffer, resource.ID);
				if (resource.dataType == 1)
					Frame.load(resource.ID, resource.buffer);
			} while (resource.dataType != 93 || !resourceProvider.landscapePresent(resource.ID));
			MapRegion.passiveRequestGameObjectModels(new Buffer(resource.buffer),
					resourceProvider);
		} while (true);
	}

	private void processInput() {
		try {
			if (clickMode2 == 2 && lastMouseX != -1) {
				int mouseDeltaX = mouseX - lastMouseX;
				int mouseDeltaY = mouseY - lastMouseY;
				lastMouseX = mouseX;
				lastMouseY = mouseY;
				xCameraCurve -= mouseDeltaX;
				yCameraCurve += mouseDeltaY;
			}
			if (clickMode2 == 0 && lastMouseX != -1) {
				lastMouseX = -1;
				lastMouseY = -1;
			}
			if (clickMode3 == 2 && lastMouseX == -1) {
				lastMouseX = saveClickX;
				lastMouseY = saveClickY;
			}
			if (xCameraPos < 0) {
				xCameraPos = 0;
			}
			if (yCameraPos <= -1) {
				yCameraPos = 0;
			}
			if (xCameraCurve < 0) {
				xCameraCurve = 2047;
			}
			if (yCameraCurve < 0) {
				yCameraCurve = 2047;
			}
			if (xCameraCurve / 64 >= 32) {
				xCameraCurve = 0;
			}
			if (yCameraCurve > 2047) {
				yCameraCurve = 0;
			}
			if (keyArray['w'] == 1) {
				yCameraPos += Rasterizer3D.COSINE[xCameraCurve] >> 11;
				xCameraPos -= Rasterizer3D.anIntArray1470[xCameraCurve] >> 11;
			}
			if (keyArray['s'] == 1) {
				yCameraPos -= Rasterizer3D.COSINE[xCameraCurve] >> 11;
				xCameraPos += Rasterizer3D.anIntArray1470[xCameraCurve] >> 11;
			}
			if (keyArray['d'] == 1) {
				yCameraPos += Rasterizer3D.anIntArray1470[xCameraCurve] >> 11;
				xCameraPos += Rasterizer3D.COSINE[xCameraCurve] >> 11;
			}
			if (keyArray['a'] == 1) {
				yCameraPos -= Rasterizer3D.anIntArray1470[xCameraCurve] >> 11;
				xCameraPos -= Rasterizer3D.COSINE[xCameraCurve] >> 11;
			}
			if (keyArray['q'] == 1) {
				if (zCameraPos > -4250) {
					zCameraPos -= Rasterizer3D.COSINE[yCameraCurve] >> 11;
				}
			}
			if (keyArray['z'] == 1) {
				if (zCameraPos < -400) {
					zCameraPos += Rasterizer3D.COSINE[yCameraCurve] >> 11;
				}
			}
		} catch (Exception error) {
			error.printStackTrace();
		}
	}

	public void loadMap(int x, int z) throws IOException {
		x /= 64;
		z /= 64;
		Rasterizer3D.clearTextureCache();
		scene.initToNull();
		ObjectDefinition.baseModels.clear();
		ObjectDefinition.models.clear();		
		System.gc();

		for (int i = 0; i < 4; i++) {
			collisionMaps[i].initialize();
		}

		for (int l = 0; l < 4; l++) {
			for (int k1 = 0; k1 < Constants.MAP_TILE_WIDTH; k1++) {
				for (int j2 = 0; j2 < Constants.MAP_TILE_HEIGHT; j2++)
					tileFlags[l][k1][j2] = 0;
			}
		}

		mapRegion = new MapRegion(Constants.MAP_TILE_WIDTH, Constants.MAP_TILE_HEIGHT, tileFlags, tileHeights);

		for (int _x = 0; _x < Constants.MAP_WIDTH; _x++)
			for (int _z = 0; _z < Constants.MAP_HEIGHT; _z++) {

				int terrainIdx = resourceProvider.resolve(0, z + _z, x + _x);

				if (terrainIdx == -1) {
					mapRegion.clear_region(_z * 64, 64, 64, _x * 64);
					continue;
				}
				byte[] terrainData = CompressionUtil.degzip(ByteBuffer.wrap(cache.getStore(4).readFile(terrainIdx)));

				if (terrainData == null) {
					mapRegion.clear_region(_z * 64, 64, 64, _x * 64);
					continue;
				}
				mapRegion.method180(terrainData, _z * 64, _x * 64, x * 64, z * 64, collisionMaps);
			}
		for (int _x = 0; _x < Constants.MAP_WIDTH; _x++)
			for (int _z = 0; _z < Constants.MAP_HEIGHT; _z++) {
				int objectIdx = resourceProvider.resolve(1, z + _z, x + _x);

				if (objectIdx == -1) {
					continue;
				}

				byte[] objectData = cache.getStore(4).readFile(objectIdx);

				if (objectData == null) {
					continue;
				}

				byte[] tempData = CompressionUtil.degzip(ByteBuffer.wrap(objectData));

				if (!Configuration.tilesOnly) {
					mapRegion.method190(_x * 64, collisionMaps, _z * 64, scene, tempData);
				}

			}

		mapRegion.createRegionScene(collisionMaps, scene);
		scene.method275(0);
		System.gc();
		Rasterizer3D.initiateRequestBuffers();
		resourceProvider.clearExtras();
		mapLoaded = true;
		setHeightLevel(0);
	}

	public static void loadMapCoordinates(int x, int z) {
		loadMapRequestX = x;
		loadMapRequestZ = z;
	}

	private void refreshMap() {
		mapRefreshRequested = true;
	}

	public void setHeightLevel(int hL) {
		heightlevelChangeRequest = hL;
	}

}
