public class Settings {
    public const int ScreenWidth = 1000;
    public const int ScreenHeight = 1000;
    public const int EntitySize = 4;
    public const int EntitiesToSpawn = 10000;
    public const int EntitySpeedMin = -1;
    public const int EntitySpeedMax = 2;
}
public static class Colours {
    public static readonly Brush[] Palette = new Brush[] {
        Brushes.Red,
        Brushes.Green,
        Brushes.Blue,
        Brushes.Orange,
    };
}

public class Program {
    static void Main() {
        Application.EnableVisualStyles();
        Application.Run(new GameWindow());
    }
}

// -------------------- ECS STRUCTS --------------------

public struct Entity {
    public int EntityID;
}

public struct Position {
    public int x;
    public int y;
}

public struct Health {
    public int health;
}

public struct Colour {
    public byte colourIndex;
}


// -------------------- ENTITY MANAGER --------------------

public class EntityManager {
    private readonly int[] freeList;
    private int freeCount;
    public Entity[] Entities { get; }
    public int EntityCount { get; private set; }

    public EntityManager(int maxEntities) {
        freeList = new int[maxEntities];
        Entities = new Entity[maxEntities];
        EntityCount = 0;

        for (int i = 0; i < maxEntities; i++) {
            freeList[i] = maxEntities - i - 1;
        }
        freeCount = maxEntities;
    }

    public int GetFreeEntityID() {
        if (freeCount == 0) {
            Console.WriteLine("[SpawnEntity] No free entities!");
            return -1;
        }
        return freeList[--freeCount];
    }

    public void AddEntity(int id) {
        Entities[EntityCount++] = new Entity { EntityID = id };
    }
}

// -------------------- COMPONENT MANAGER --------------------

public class ComponentManager {
    private readonly Random rand;
    public readonly Position[] PositionArray;
    public readonly Health[] HealthArray;
    public readonly Colour[] ColourArray;

    public ComponentManager(int maxEntities) {
        rand = new Random();
        PositionArray = new Position[maxEntities];
        HealthArray = new Health[maxEntities];
        ColourArray = new Colour[maxEntities];
    }

    public void CreateEntityComponents(int entityID, int x, int y, int health) {
        PositionArray[entityID].x = x;
        PositionArray[entityID].y = y;
        HealthArray[entityID].health = health;
        ColourArray[entityID].colourIndex = (byte)rand.Next(0, Colours.Palette.Length);
    }
}

// -------------------- GAME CLASS --------------------

public class Game {
    public readonly EntityManager EntityManager;
    public readonly ComponentManager ComponentManager;
    private readonly Random rand;

    public Game() {
        EntityManager = new EntityManager(Settings.EntitiesToSpawn);
        ComponentManager = new ComponentManager(Settings.EntitiesToSpawn);
        rand = new Random();
    }

    public void SpawnEntity(int x, int y) {
        int freeID = EntityManager.GetFreeEntityID();
        if (freeID == -1) return;

        EntityManager.AddEntity(freeID);
        ComponentManager.CreateEntityComponents(freeID, x, y, 10);
    }
    public void MoveEntities() {
        for (int i = 0; i < EntityManager.EntityCount; i++) {
            int id = EntityManager.Entities[i].EntityID;
            ref var pos = ref ComponentManager.PositionArray[id];

            pos.x += rand.Next(Settings.EntitySpeedMin, Settings.EntitySpeedMax); 
            pos.y += rand.Next(Settings.EntitySpeedMin, Settings.EntitySpeedMax);

            if (pos.x < 0) pos.x = Settings.ScreenWidth - 1;
            if (pos.x > Settings.ScreenWidth - 1) pos.x = 0;

            if (pos.y < 0) pos.y = Settings.ScreenHeight - 1;
            if (pos.y > Settings.ScreenHeight - 1) pos.y = 0;
        }
    }
}

// -------------------- GAME WINDOW --------------------

public class GameWindow : Form {
    private readonly Game game;
    private readonly System.Windows.Forms.Timer timer;
    private readonly Random random;

    public GameWindow() {
        Text = "ECS Demo";
        DoubleBuffered = true;
        Width = Settings.ScreenWidth;
        Height = Settings.ScreenHeight;

        game = new Game();
        random = new Random();

        for (int i = 0; i < Settings.EntitiesToSpawn; i++) {
            game.SpawnEntity(random.Next(0, Settings.ScreenWidth), random.Next(0, Settings.ScreenHeight));

        }

        timer = new System.Windows.Forms.Timer();
        timer.Interval = 16;
        timer.Tick += Update;
        timer.Start();
    }

    private void Update(object sender, EventArgs e) {
        game.MoveEntities();
        Invalidate();
    }

    protected override void OnPaint(PaintEventArgs e) {
        base.OnPaint(e);

        for (int i = 0; i < game.EntityManager.EntityCount; i++) {
            int id = game.EntityManager.Entities[i].EntityID;
            ref var pos = ref game.ComponentManager.PositionArray[id];
            var brush = Colours.Palette[game.ComponentManager.ColourArray[id].colourIndex];

            e.Graphics.FillRectangle(brush, pos.x, pos.y, Settings.EntitySize, Settings.EntitySize);
        }
    }
}
